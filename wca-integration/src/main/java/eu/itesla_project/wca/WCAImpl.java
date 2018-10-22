/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016-2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.powsybl.computation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.gdata.util.common.base.Pair;

import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.contingency.Contingency;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.ampl.converter.AmplExportConfig;
import com.powsybl.ampl.converter.AmplNetworkWriter;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.converter.AmplUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import eu.itesla_project.modules.constraints.ConstraintsModifier;
import eu.itesla_project.modules.constraints.ConstraintsModifierConfig;
import eu.itesla_project.modules.contingencies.Action;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.histo.HistoDbAttributeId;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.histo.IIDM2DB;
import eu.itesla_project.modules.rules.RuleAttributeSet;
import eu.itesla_project.modules.rules.RuleId;
import eu.itesla_project.modules.rules.RulesDbClient;
import eu.itesla_project.modules.rules.SecurityRule;
import eu.itesla_project.modules.rules.SecurityRuleCheckReport;
import eu.itesla_project.modules.rules.SecurityRuleExpression;
import eu.itesla_project.modules.wca.Uncertainties;
import eu.itesla_project.modules.wca.UncertaintiesAnalyserFactory;
import eu.itesla_project.modules.wca.WCA;
import eu.itesla_project.modules.wca.WCAAsyncResult;
import eu.itesla_project.modules.wca.WCACluster;
import eu.itesla_project.modules.wca.WCAClusterNum;
import eu.itesla_project.modules.wca.WCAParameters;
import eu.itesla_project.modules.wca.WCAResult;
import eu.itesla_project.modules.wca.report.WCAActionApplication;
import eu.itesla_project.modules.wca.report.WCALoadflowResult;
import eu.itesla_project.modules.wca.report.WCAPostContingencyStatus;
import eu.itesla_project.modules.wca.report.WCAReport;
import eu.itesla_project.modules.wca.report.WCARuleViolationType;
import eu.itesla_project.modules.wca.report.WCASecurityRuleApplication;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationFilter;
import com.powsybl.security.LimitViolationType;
import com.powsybl.security.Security;
import com.powsybl.simulation.securityindexes.SecurityIndexId;
import com.powsybl.simulation.securityindexes.SecurityIndexType;
import eu.itesla_project.wca.report.WCAReportImpl;
import eu.itesla_project.wca.uncertainties.UncertaintiesAmplWriter;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class WCAImpl implements WCA, WCAConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(WCAImpl.class);

    private static final String CLUSTERS_CMD_ID = "wca_clusters";
    private static final String DOMAINS_CMD_ID = "wca_domains";

    private static final String CLUSTERS_WORKING_DIR_PREFIX = "itesla_wca_clusters_";
    private static final String DOMAINS_WORKING_DIR_PREFIX = "itesla_wca_domains_";

    private static final String REQUIRED_FILE_NAME = "required";

    private static final String[] COMMON_INPUT_FILE_NAMES = {
        MEANS_FILE_SUFFIX + "." + TXT_EXT,
        TRUST_INTERVAL_FILE_SUFFIX + "." + TXT_EXT,
        REDUCTION_MATRIX_FILE_SUFFIX + "." + TXT_EXT,
        HISTO_LOADS_FILE_SUFFIX + "." + TXT_EXT,
        HISTO_GENERATORS_FILE_SUFFIX + "." + TXT_EXT
    };

    private static final String[] INPUT_FILE_NAMES = {
        FAULTS_FILE_SUFFIX + "." + TXT_EXT,
        ACTIONS_FILE_SUFFIX + "." + TXT_EXT,
        SECURITY_RULES_FILE_SUFFIX + "." + TXT_EXT,
        "_network_generators.txt",
        "_network_ptc.txt",
        "_network_substations.txt",
        "_network_branches.txt",
        "_network_limits.txt",
        "_network_rtc.txt",
        "_network_tct.txt",
        "_network_buses.txt",
        "_network_loads.txt",
        "_network_shunts.txt",
    };

    private static final int THREADS = 1;

    private static final float UNCERTAINTY_THRESHOLD = 50f;
    private static final int DETAILS_LEVEL_NORMAL = 2;
    private static final int DETAILS_LEVEL_DEBUG = 4;

    private static final String WCA_FLOWS_FILE = "wca_flows.txt";
    private static final String WCA_UNCERTAINTIES_FILE = "wca_uncertainties.txt";
    private static final String WCA_UNDEFINED_PST_FILE = "wca_undefined_pst.txt";
    private static final String WCA_SENSIBILITIES_FILE = "wca_sensibilities.txt";
    private static final String WCA_INFLUENCE_PST_FILE = "wca_influence_pst.txt";

    private final Network network;

    private final ComputationManager computationManager;

    private final HistoDbClient histoDbClient;

    private final RulesDbClient rulesDbClient;

    private final UncertaintiesAnalyserFactory uncertaintiesAnalyserFactory;

    private final ContingenciesAndActionsDatabaseClient contingenciesActionsDbClient;

    private final LoadFlowFactory loadFlowFactory;

    private final WCAConfig config;

    private final Map<String, String> env;

    private final LimitViolationFilter violationsFilter;

    private WCAReportImpl wcaReport;

    public WCAImpl(Network network, ComputationManager computationManager, HistoDbClient histoDbClient,
                   RulesDbClient rulesDbClient, UncertaintiesAnalyserFactory uncertaintiesAnalyserFactory,
                   ContingenciesAndActionsDatabaseClient contingenciesActionsDbClient, LoadFlowFactory loadFlowFactory,
                   WCAConfig config) {
        this.network = Objects.requireNonNull(network, "network is null");
        this.computationManager = Objects.requireNonNull(computationManager, "computationManager is null");
        this.histoDbClient = Objects.requireNonNull(histoDbClient, "histoDbClient is null");
        this.rulesDbClient = Objects.requireNonNull(rulesDbClient, "rulesDbClient is null");
        this.uncertaintiesAnalyserFactory = Objects.requireNonNull(uncertaintiesAnalyserFactory, "uncertaintiesAnalyserFactory is null");
        this.contingenciesActionsDbClient = Objects.requireNonNull(contingenciesActionsDbClient, "contingenciesActionsDbClient is null");
        this.loadFlowFactory = Objects.requireNonNull(loadFlowFactory, "loadFlowFactory is null");
        this.config = Objects.requireNonNull(config, "config is null");

        LOGGER.info(config.toString());

        this.violationsFilter = new LimitViolationFilter().setViolationTypes(config.ignoreVoltageConstraints() ? EnumSet.of(LimitViolationType.CURRENT) : null)
                                                          .setMinBaseVoltage(config.getVoltageLevelConstraintFilter())
                                                          .setCountries(config.getCountryConstraintFilter().isEmpty() ? null : config.getCountryConstraintFilter());

        this.wcaReport = new WCAReportImpl(network);

        env = ImmutableMap.of("XPRESS",  config.getXpressHome().resolve("bin").toString(),
                              "LD_LIBRARY_PATH", config.getXpressHome().resolve("lib").toString());
    }

    private static final AmplExportConfig DOMAINS_AMPL_EXPORT_CONFIG = new AmplExportConfig(AmplExportConfig.ExportScope.ONLY_MAIN_CC_AND_CONNECTABLE_GENERATORS_AND_SHUNTS_AND_ALL_LOADS,
                                                                                            false,
                                                                                            AmplExportConfig.ExportActionType.PREVENTIVE);
    private static final AmplExportConfig CLUSTERS_AMPL_EXPORT_CONFIG = new AmplExportConfig(AmplExportConfig.ExportScope.ONLY_MAIN_CC_AND_CONNECTABLE_GENERATORS_AND_SHUNTS_AND_ALL_LOADS,
                                                                                             false,
                                                                                             AmplExportConfig.ExportActionType.CURATIVE);

    private static final LoadFlowParameters LOAD_FLOW_PARAMETERS = new LoadFlowParameters()
            .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.UNIFORM_VALUES)
            .setTransformerVoltageControlOn(false)
            .setNoGeneratorReactiveLimits(false)
            .setPhaseShifterRegulationOn(false);

    private SecurityIndexType[] getSecurityIndexTypes(WCAParameters parameters) {
        return parameters.getSecurityIndexTypes() != null
                ? parameters.getSecurityIndexTypes().toArray(new SecurityIndexType[parameters.getSecurityIndexTypes().size()])
                : SecurityIndexType.values();
    }

    private List<InputFile> inputFiles(int dataSetNum) {
        List<InputFile> inputFiles = new ArrayList<>(INPUT_FILE_NAMES.length + 1);
        inputFiles.add(new InputFile(REQUIRED_FILE_NAME));
        for (String inputFileName : COMMON_INPUT_FILE_NAMES) {
            inputFiles.add(new InputFile(COMMONE_FILE_PREFIX + inputFileName));
        }
        for (String inputFileName : INPUT_FILE_NAMES) {
            inputFiles.add(new InputFile(FILE_PREFIX + dataSetNum + inputFileName));
        }
        return inputFiles;
    }

    private static void copyRequired(Path workingDir) throws IOException {
        Files.copy(WCAImpl.class.getResourceAsStream("/" + REQUIRED_FILE_NAME), workingDir.resolve(REQUIRED_FILE_NAME));
    }

    private CompletableFuture<WCAClustersResult> createClustersTask(Contingency contingency,
                                                                    List<String> curativeActionIds,
                                                                    String baseStateId,
                                                                    String contingencyStateId,
                                                                    List<String> curativeStateIds,
                                                                    List<SecurityRuleExpression> securityRuleExpressions,
                                                                    Uncertainties uncertainties,
                                                                    WCAHistoLimits histoLimits,
                                                                    StringToIntMapper<AmplSubset> mapper,
                                                                    boolean activateFiltering) {
        return computationManager.execute(new ExecutionEnvironment(env, CLUSTERS_WORKING_DIR_PREFIX, config.isDebug()),
                new AbstractExecutionHandler<WCAClustersResult>() {

                    @Override
                    public List<CommandExecution> before(Path workingDir) throws IOException {

                        network.getStateManager().setWorkingState(baseStateId);

                        copyRequired(workingDir);

                        DataSource commonDataSource = new FileDataSource(workingDir, COMMONE_FILE_PREFIX);

                        // write uncertainies
                        new UncertaintiesAmplWriter(uncertainties, commonDataSource, mapper).write();

                        // write historical interval
                        histoLimits.write(commonDataSource, mapper);

                        int contingencyNum = mapper.newInt(AmplSubset.FAULT, contingency.getId());
                        int dataSetNum = contingencyNum - 1;

                        DataSource dataSource = new FileDataSource(workingDir, FILE_PREFIX + dataSetNum);

                        // write base state
                        AmplUtil.resetNetworkMapping(mapper);
                        AmplUtil.fillMapper(mapper, network);
                        new AmplNetworkWriter(network, dataSource, 0, 0, false, mapper, CLUSTERS_AMPL_EXPORT_CONFIG).write();
                        if (config.isExportStates()) {
                            WCAUtils.exportState(network, workingDir, 0, 0);
                        }

                        // write post contingency state
                        network.getStateManager().setWorkingState(contingencyStateId);
                        AmplUtil.resetNetworkMapping(mapper);
                        AmplUtil.fillMapper(mapper, network); // because action can create a new bus
                        new AmplNetworkWriter(network, dataSource, contingencyNum, 0, true, mapper, CLUSTERS_AMPL_EXPORT_CONFIG).write();
                        if (config.isExportStates()) {
                            WCAUtils.exportState(network, workingDir, contingencyNum, 0);
                        }

                        // write contingency description
                        WCAUtils.writeContingencies(Collections.singleton(contingency), dataSource, mapper);

                        // write security rules corresponding to the contingency
                        new WCASecurityRulesWriter(network, securityRuleExpressions, dataSource, mapper, false, activateFiltering).write();

                        // write post curative state
                        for (int i = 0; i < curativeActionIds.size(); i++) {
                            String curativeActionId = curativeActionIds.get(i);
                            int curativeActionNum = mapper.newInt(AmplSubset.CURATIVE_ACTION, curativeActionId);

                            String curativeStateId = curativeStateIds.get(i);
                            network.getStateManager().setWorkingState(curativeStateId);
                            AmplUtil.resetNetworkMapping(mapper);
                            AmplUtil.fillMapper(mapper, network); // because action can create a new bus
                            new AmplNetworkWriter(network, dataSource, contingencyNum, curativeActionNum, true, mapper, CLUSTERS_AMPL_EXPORT_CONFIG).write();
                            if (config.isExportStates()) {
                                WCAUtils.exportState(network, workingDir, contingencyNum, curativeActionNum);
                            }
                        }

                        // write curatives action description associated to the contingency
                        WCAUtils.writeActions(curativeActionIds, dataSource, mapper, "Curative actions", AmplSubset.CURATIVE_ACTION);

                        Command cmd = new SimpleCommandBuilder()
                                .id(CLUSTERS_CMD_ID)
                                .program("clusters")
                                .inputFiles(inputFiles(dataSetNum))
                                .args(COMMONE_FILE_PREFIX,
                                        FILE_PREFIX + dataSetNum,
                                        REQUIRED_FILE_NAME,
                                        "" + dataSetNum,
                                        Integer.toString(THREADS),
                                        Float.toString(config.getReducedVariableRatio()),
                                        Float.toString(UNCERTAINTY_THRESHOLD),
                                        Integer.toString(config.isDebug() ? DETAILS_LEVEL_DEBUG : DETAILS_LEVEL_NORMAL),
                                        Integer.toString(WCARestrictingThresholdLevel.getLevel(config.getRestrictingThresholdLevels())),
                                        WCA_FLOWS_FILE,
                                        WCA_UNCERTAINTIES_FILE,
                                        WCA_UNDEFINED_PST_FILE,
                                        WCA_SENSIBILITIES_FILE,
                                        WCA_INFLUENCE_PST_FILE)
                                .build();
                        return Collections.singletonList(new CommandExecution(cmd, 1));
                    }

                    @Override
                    public WCAClustersResult after(Path workingDir, ExecutionReport report) throws IOException {
                        report.log();
                        WCAClustersResult clustersResult = WCAUtils.readClustersResult(CLUSTERS_CMD_ID, workingDir, WCA_FLOWS_FILE, WCA_UNCERTAINTIES_FILE);
                        LOGGER.info("Network {}, contingency {}: 'clusters' result = {}", network.getId(), contingency.getId(), clustersResult.toString());
                        return clustersResult;
                    }
                });
    }

    private CompletableFuture<WCADomainsResult> createDomainsTask(Contingency contingency,
                                                                  String baseStateId,
                                                                  List<SecurityRuleExpression> securityRuleExpressions,
                                                                  Uncertainties uncertainties,
                                                                  WCAHistoLimits histoLimits,
                                                                  StringToIntMapper<AmplSubset> mapper,
                                                                  List<String> preventiveStateIds,
                                                                  List<String> preventiveActionIds,
                                                                  boolean activateFiltering) {
        return computationManager.execute(new ExecutionEnvironment(env, DOMAINS_WORKING_DIR_PREFIX, config.isDebug()),
                new AbstractExecutionHandler<WCADomainsResult>() {

                    @Override
                    public List<CommandExecution> before(Path workingDir) throws IOException {

                        network.getStateManager().setWorkingState(baseStateId);

                        copyRequired(workingDir);

                        DataSource commonDataSource = new FileDataSource(workingDir, COMMONE_FILE_PREFIX);

                        // write uncertainies
                        new UncertaintiesAmplWriter(uncertainties, commonDataSource, mapper).write();

                        // write historical interval
                        histoLimits.write(commonDataSource, mapper);

                        int contingencyNum = contingency == null ? 1 : mapper.newInt(AmplSubset.FAULT, contingency.getId());
                        int dataSetNum = contingencyNum - 1;

                        DataSource dataSource = new FileDataSource(workingDir, FILE_PREFIX + dataSetNum);

                        // write base state
                        AmplUtil.resetNetworkMapping(mapper);
                        AmplUtil.fillMapper(mapper, network);
                        new AmplNetworkWriter(network, dataSource, 0, 0, false, mapper, DOMAINS_AMPL_EXPORT_CONFIG).write();
                        if (config.isExportStates()) {
                            WCAUtils.exportState(network, workingDir, 0, 0);
                        }

                        // write contingency description
                        WCAUtils.writeContingencies(contingency == null ? Collections.emptyList() : Collections.singleton(contingency), dataSource, mapper);

                        // write post preventive state
                        for (int i = 0; i < preventiveActionIds.size(); i++) {
                            String preventiveActionId = preventiveActionIds.get(i);
                            int preventiveActionNum = mapper.newInt(AmplSubset.PREVENTIVE_ACTION, preventiveActionId);

                            String preventiveStateId = preventiveStateIds.get(i);
                            network.getStateManager().setWorkingState(preventiveStateId);
                            AmplUtil.resetNetworkMapping(mapper);
                            AmplUtil.fillMapper(mapper, network); // because action can create a new bus
                            new AmplNetworkWriter(network, dataSource, 0, preventiveActionNum, true, mapper, DOMAINS_AMPL_EXPORT_CONFIG).write();
                            if (config.isExportStates()) {
                                WCAUtils.exportState(network, workingDir, 0, preventiveActionNum);
                            }
                        }

                        // write preventive action description
                        WCAUtils.writeActions(preventiveActionIds, dataSource, mapper, "Preventive actions", AmplSubset.PREVENTIVE_ACTION);

                        // write security rules corresponding to the contingency
                        new WCASecurityRulesWriter(network, securityRuleExpressions, dataSource, mapper, false, activateFiltering).write();

                        Command cmd = new SimpleCommandBuilder()
                                .id(DOMAINS_CMD_ID)
                                .program("domains")
                                .inputFiles(inputFiles(dataSetNum))
                                .args(COMMONE_FILE_PREFIX,
                                        FILE_PREFIX + dataSetNum,
                                        REQUIRED_FILE_NAME,
                                        "" + dataSetNum,
                                        Integer.toString(THREADS),
                                        Float.toString(config.getReducedVariableRatio()),
                                        Float.toString(UNCERTAINTY_THRESHOLD),
                                        Integer.toString(SecurityIndexType.TSO_OVERLOAD.ordinal()),
                                        Integer.toString(config.isDebug() ? DETAILS_LEVEL_DEBUG : DETAILS_LEVEL_NORMAL),
                                        Integer.toString(WCARestrictingThresholdLevel.getLevel(config.getRestrictingThresholdLevels())),
                                        WCA_FLOWS_FILE,
                                        WCA_UNCERTAINTIES_FILE,
                                        WCA_UNDEFINED_PST_FILE,
                                        WCA_SENSIBILITIES_FILE,
                                        WCA_INFLUENCE_PST_FILE)
                                .build();
                        return Collections.singletonList(new CommandExecution(cmd, 1));
                    }

                    @Override
                    public WCADomainsResult after(Path workingDir, ExecutionReport report) throws IOException {
                        report.log();
                        WCADomainsResult domainsResult = WCAUtils.readDomainsResult(DOMAINS_CMD_ID, workingDir, WCA_UNCERTAINTIES_FILE);
                        LOGGER.info("Network {}, {}: 'domains' result = {}", network.getId(), contingency == null ? "pre-contingency" : "contingency " + contingency.getId(), domainsResult.toString());
                        return domainsResult;
                    }
                });
    }

    private CompletableFuture<WCAClustersResult> createClustersTaskWithDeps(Contingency contingency,
                                                                            List<String> curativeActionIds,
                                                                            String baseStateId,
                                                                            String contingencyStateId,
                                                                            List<String> curativeStateIds,
                                                                            List<SecurityRuleExpression> securityRuleExpressions,
                                                                            Supplier<CompletableFuture<Uncertainties>> memoizedUncertaintiesFuture,
                                                                            Supplier<CompletableFuture<WCAHistoLimits>> histoLimitsFuture,
                                                                            StringToIntMapper<AmplSubset> mapper,
                                                                            boolean activateFiltering) {
        return memoizedUncertaintiesFuture.get()
                .thenCombine(histoLimitsFuture.get(), (uncertainties, histoLimits) -> Pair.of(uncertainties, histoLimits))
                .thenCompose(p -> createClustersTask(contingency, curativeActionIds, baseStateId, contingencyStateId, curativeStateIds,
                                                     securityRuleExpressions, p.getFirst(), p.getSecond(), mapper, activateFiltering));
    }

    private CompletableFuture<WCADomainsResult> createDomainsTaskWithDeps(Contingency contingency,
                                                                          String baseStateId,
                                                                          List<SecurityRuleExpression> securityRuleExpressions,
                                                                          Supplier<CompletableFuture<Uncertainties>> memoizedUncertaintiesFuture,
                                                                          Supplier<CompletableFuture<WCAHistoLimits>> histoLimitsFuture,
                                                                          StringToIntMapper<AmplSubset> mapper,
                                                                          List<String> preventiveStateIds,
                                                                          List<String> preventiveActionIds,
                                                                          boolean activateFiltering) {
        return memoizedUncertaintiesFuture.get()
                .thenCombine(histoLimitsFuture.get(), (uncertainties, histoLimits) -> Pair.of(uncertainties, histoLimits))
                .thenCompose(p -> createDomainsTask(contingency, baseStateId, securityRuleExpressions, p.getFirst(), p.getSecond(), mapper,
                                                    preventiveStateIds, preventiveActionIds, activateFiltering));
    }

    private CompletableFuture<WCAClusterNum> createClustersWorkflowTask(Contingency contingency, String baseStateId,
                                                                        ContingencyDbFacade contingencyDbFacade,
                                                                        List<SecurityRuleExpression> securityRuleExpressions,
                                                                        Supplier<CompletableFuture<Uncertainties>> memoizedUncertaintiesFuture,
                                                                        Supplier<CompletableFuture<WCAHistoLimits>> histoLimitsFuture,
                                                                        StringToIntMapper<AmplSubset> mapper,
                                                                        LoadFlow loadFlow,
                                                                        boolean activateFiltering,
                                                                        boolean filterCurativeActions,
                                                                        WCAFilteredClusters filteredClusters) {
        String[] contingencyStateId = new String[1];
        List<String> curativeStateIds = Collections.synchronizedList(new ArrayList<>());
        List<String> curativeActionIds = Collections.synchronizedList(new ArrayList<>());

        return CompletableFuture
                .runAsync(() -> {
                    LOGGER.info("Network {}, contingency {}: computing post contingency state", network.getId(), contingency.getId());
                    contingencyStateId[0] = baseStateId + "_" + contingency.getId();
                    network.getStateManager().cloneState(baseStateId, contingencyStateId[0]);
                    network.getStateManager().setWorkingState(contingencyStateId[0]);

                    contingency.toTask().modify(network, computationManager);

                }, computationManager.getExecutor())
                .thenCompose(aVoid -> loadFlow.run(contingencyStateId[0], LOAD_FLOW_PARAMETERS))
                .thenCompose(loadFlowResult -> {
                    if (!loadFlowResult.isOk()) {
                        LOGGER.warn("Network {}, contingency {}: load flow on post contingency state diverged, metrics = {}",
                                    network.getId(), contingency.getId(), loadFlowResult.getMetrics());
                        filteredClusters.removeClusters(contingency.getId(),
                                                        EnumSet.of(WCAClusterNum.ONE, WCAClusterNum.TWO, WCAClusterNum.THREE),
                                                        WCAClusterOrigin.LF_POST_CONTINGENCY_DIVERGENCE);
                        wcaReport.addPostContingencyStatus(new WCAPostContingencyStatus(
                                contingency.getId(),
                                new WCALoadflowResult(false, "load flow on post contingency state diverged: metrics = " + loadFlowResult.getMetrics())
                                ));
                        return CompletableFuture.completedFuture(WCAClusterNum.FOUR);
                    } else {
                        network.getStateManager().setWorkingState(contingencyStateId[0]);

                        WCAPostContingencyStatus postContingencyStatus = new WCAPostContingencyStatus(contingency.getId(), new WCALoadflowResult(true, null));

                        List<LimitViolation> contingencyStateLimitViolations = violationsFilter.apply(Security.checkLimits(network), network);
                        if (contingencyStateLimitViolations.size() > 0) {
                            LOGGER.warn("Network {}, contingency {}: constraint violantions found in post contingency state:\n{}",
                                        network.getId(), contingency.getId(), Security.printLimitsViolations(contingencyStateLimitViolations, network, violationsFilter));
                            postContingencyStatus.setPostContingencyViolationsWithoutUncertainties(contingencyStateLimitViolations);
                            filteredClusters.removeClusters(contingency.getId(),
                                                            EnumSet.of(WCAClusterNum.ONE),
                                                            WCAClusterOrigin.LF_POST_CONTINGENCY_VIOLATION);
                        }

                        if (WCACurativeActionsOptimizer.CLUSTERS.equals(config.getCurativeActionsOptimizer())
                                || WCACurativeActionsOptimizer.LF_HEURISTIC.equals(config.getCurativeActionsOptimizer())) {
                            LOGGER.info("Network {}, contingency {}: getting curative actions", network.getId(), contingency.getId());
                            List<List<Action>> curativeActions = contingencyDbFacade.getCurativeActions(contingency, null); // pass post contingency violations?
                            LOGGER.info("Network {}, contingency {}: found {} curative actions", network.getId(), contingency.getId(), curativeActions.size());
                            if (curativeActions.isEmpty()) {
                                LOGGER.warn("Network {}, contingency {}: found no curative actions", network.getId(), contingency.getId());
                                filteredClusters.removeClusters(contingency.getId(),
                                                                EnumSet.of(WCAClusterNum.ONE),
                                                                WCAClusterOrigin.LF_POST_SPECIFIC_CURATIVE_ACTION_VIOLATION);
                                postContingencyStatus.setCurativeActionsAvailable(false);
                                wcaReport.addPostContingencyStatus(postContingencyStatus);
                                if (WCACurativeActionsOptimizer.CLUSTERS.equals(config.getCurativeActionsOptimizer())) {
                                    return createClustersTaskWithDeps(contingency, Collections.emptyList(), baseStateId, contingencyStateId[0],
                                                                      Collections.emptyList(), securityRuleExpressions, memoizedUncertaintiesFuture,
                                                                      histoLimitsFuture, mapper, activateFiltering)
                                                                      .thenCompose(clusterResults -> {
                                                                          return CompletableFuture.completedFuture(clusterResults.getClusterNum());
                                                                      });
                                } else {
                                    return CompletableFuture.completedFuture(WCAClusterNum.UNDEFINED);
                                }
                            } else {
                                List<String> curativeStateIdsForClusters = Collections.synchronizedList(new ArrayList<>());
                                List<String> curativeActionIdsForClusters  = Collections.synchronizedList(new ArrayList<>());
                                List<WCAActionApplication> curativeActionsApplication = Collections.synchronizedList(new ArrayList<>());
                                curativeActions.sort((o1, o2) -> o1.stream().map(Action::getId).collect(Collectors.joining("+")).compareTo(o2.stream().map(Action::getId).collect(Collectors.joining("+"))));
                                String previousState = contingencyStateId[0];
                                for (int i = 0; i < curativeActions.size(); i++) {
                                    List<Action> curativeAction = curativeActions.get(i);
                                    String curativeActionId = curativeAction.stream().map(Action::getId).collect(Collectors.joining("+"));
                                    curativeActionIds.add(curativeActionId);
                                    String curativeStateId = previousState + "_" + curativeActionId;
                                    curativeStateIds.add(curativeStateId);
                                    LOGGER.info("Network {}, contingency {}, curative action {}: starting analysis",
                                                network.getId(), contingency.getId(), curativeActionId);
                                    network.getStateManager().cloneState(previousState, curativeStateId);
                                    network.getStateManager().setWorkingState(curativeStateId);
                                    LOGGER.info("Network {}, contingency {}, curative action {}: computing post curative action state",
                                                network.getId(), contingency.getId(), curativeActionId);
                                    for (Action subAction : curativeAction) {
                                        subAction.toTask().modify(network, computationManager);
                                    }
                                    LoadFlowResult loadFlowResult1;
                                    try {
                                        loadFlowResult1 = loadFlow.run(network.getStateManager().getWorkingStateId(), LOAD_FLOW_PARAMETERS).join();
                                        if (loadFlowResult1.isOk()) {
                                            boolean violationsRemoved = false;
                                            boolean actionApplied = false;
                                            String comment = null;
                                            List<LimitViolation> curativeStateLimitViolations = violationsFilter.apply(Security.checkLimits(network), network);
                                            if (curativeStateLimitViolations.isEmpty()) {
                                                LOGGER.info("Network {}, contingency {}, curative action {} solves violations: adding curative action to list for 'clusters' task",
                                                            network.getId(), contingency.getId(), curativeActionId);
                                                curativeStateIdsForClusters.add(curativeStateId);
                                                curativeActionIdsForClusters.add(curativeActionId);
                                                violationsRemoved = true;
                                                actionApplied = true;
                                                previousState = curativeStateId;
                                            } else {
                                                LOGGER.warn("Network {}, contingency {}, curative action {}: violantions found in post curative action state:\n{}",
                                                            network.getId(), contingency.getId(), curativeActionId, Security.printLimitsViolations(curativeStateLimitViolations, network, violationsFilter));
                                                comment = "violantions found in post curative action state";
                                                if (!filterCurativeActions) {
                                                    LOGGER.info("Network {}, contingency {}, curative action {}: adding anyway curative action to list for 'clusters' task (config filterCurativeActions = false)",
                                                                network.getId(), contingency.getId(), curativeActionId);
                                                    curativeStateIdsForClusters.add(curativeStateId);
                                                    curativeActionIdsForClusters.add(curativeActionId);
                                                    actionApplied = true;
                                                    previousState = curativeStateId;
                                                }
                                            }
                                            curativeActionsApplication.add(new WCAActionApplication(curativeActionId,
                                                                                                    null,
                                                                                                    new WCALoadflowResult(true, null),
                                                                                                    violationsRemoved,
                                                                                                    actionApplied,
                                                                                                    comment));
                                        } else {
                                            LOGGER.warn("Network {}, contingency {}, curative action {}: load flow on post curative action state diverged, metrics = {}",
                                                        network.getId(), contingency.getId(), curativeActionId, loadFlowResult1.getMetrics());
                                            curativeActionsApplication.add(new WCAActionApplication(curativeActionId,
                                                                                                    null,
                                                                                                    new WCALoadflowResult(false, "load flow on post curative action state diverged: metrics = " + loadFlowResult1.getMetrics()),
                                                                                                    false,
                                                                                                    false,
                                                                                                    null));
                                        }
                                    } catch (Exception e) {
                                        LOGGER.warn("Network {}, contingency {}, curative action {}: load flow on post curative action state failed: {}",
                                                    network.getId(), contingency.getId(), curativeActionId, e.getMessage(), e);
                                        curativeActionsApplication.add(new WCAActionApplication(curativeActionId,
                                                                                            null,
                                                                                            new WCALoadflowResult(false, "load flow on post curative action state failed: " + e.getMessage()),
                                                                                            false,
                                                                                            false,
                                                                                            null));
                                    }
                                }
                                postContingencyStatus.setCurativeActionsApplication(curativeActionsApplication);
                                if (curativeActionIdsForClusters.isEmpty()) {
                                    LOGGER.warn("Network {}, contingency {}: no available curative actions", network.getId(), contingency.getId());
                                    filteredClusters.removeClusters(contingency.getId(),
                                                                    EnumSet.of(WCAClusterNum.ONE),
                                                                    WCAClusterOrigin.LF_POST_SPECIFIC_CURATIVE_ACTION_VIOLATION);
                                }
                                return CompletableFuture.completedFuture(new WCAClustersResult())
                                        .thenCompose(clusterResults -> {
                                            if (WCACurativeActionsOptimizer.CLUSTERS.equals(config.getCurativeActionsOptimizer())) {
                                                LOGGER.info("Network {}, contingency {}: running 'clusters' curative action optimizer", network.getId(), contingency.getId());
                                                return createClustersTaskWithDeps(contingency, curativeActionIdsForClusters, baseStateId, contingencyStateId[0],
                                                                  curativeStateIdsForClusters, securityRuleExpressions, memoizedUncertaintiesFuture,
                                                                  histoLimitsFuture, mapper, activateFiltering);
                                            } else {
                                                return CompletableFuture.completedFuture(clusterResults);
                                            }
                                        })
                                        .thenCompose(clusterResults2 -> {
                                            if (clusterResults2.foundViolations()) {
                                                LOGGER.info("Network {}, contingency {}: 'clusters' found violations", network.getId(), contingency.getId());
                                                String clustersUncertaintiesState = contingencyStateId[0] + "_clustersUncertaintiesState";
                                                LOGGER.info("Network {}, contingency {}: creating post contingency state with 'clusters' uncertainties",
                                                            network.getId(), contingency.getId());
                                                network.getStateManager().cloneState(contingencyStateId[0], clustersUncertaintiesState);
                                                network.getStateManager().setWorkingState(clustersUncertaintiesState);
                                                WCAUtils.applyInjections(network, clustersUncertaintiesState, clusterResults2.getInjections());
                                                LOGGER.info("Network {}, contingency {}: running loadflow on post contingency state with 'clusters' uncertainties",
                                                            network.getId(), contingency.getId());
                                                loadFlow.run(clustersUncertaintiesState, LOAD_FLOW_PARAMETERS)
                                                    .thenAccept(loadFlowResult2 -> {
                                                        if (!loadFlowResult2.isOk()) {
                                                            LOGGER.info("Network {}, contingency {}: loadflow on state with 'clusters' uncertainties diverged: metrics = {}",
                                                                        network.getId(), contingency.getId(), loadFlowResult.getMetrics());
                                                            postContingencyStatus.setPostContingencyWithUncertaintiesLoadflowResult(
                                                                    new WCALoadflowResult(false,
                                                                                          "load flow on post contingency state with 'clusters' uncertainties diverged: metrics = " + loadFlowResult.getMetrics())
                                                            );
                                                        } else {
                                                            postContingencyStatus.setPostContingencyWithUncertaintiesLoadflowResult(new WCALoadflowResult(true, null));
                                                            List<LimitViolation> clustersLimitViolations = violationsFilter.apply(Security.checkLimits(network), network);
                                                            if (clustersLimitViolations.size() > 0) {
                                                                LOGGER.warn("Network {}, contingency {}: constraint violantions found in state with 'clusters' uncertainties:\n{}",
                                                                            network.getId(), contingency.getId(), Security.printLimitsViolations(clustersLimitViolations, network, violationsFilter));
                                                                postContingencyStatus.setPostContingencyViolationsWithUncertainties(clustersLimitViolations);
                                                            } else {
                                                                LOGGER.warn("Network {}, contingency {}: no violations found in state with 'clusters' uncertainties",
                                                                            network.getId(), contingency.getId());
                                                            }
                                                        }
                                                    }).join();
                                            }
                                            wcaReport.addPostContingencyStatus(postContingencyStatus);
                                            return CompletableFuture.completedFuture(clusterResults2.getClusterNum());
                                        });
                            }
                        } else {
                            return CompletableFuture.completedFuture(WCAClusterNum.UNDEFINED);
                        }
                    }
                })
                .handle((clusterNumber, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error(throwable.toString(), throwable);
                    }

                    // cleanup working states
                    network.getStateManager().removeState(contingencyStateId[0]);
                    for (String curativeStateId : curativeStateIds) {
                        network.getStateManager().removeState(curativeStateId);
                    }
                    return clusterNumber;
                });
    }

    private CompletableFuture<List<CompletableFuture<WCACluster>>> createWcaTask(String baseStateId, WCAParameters parameters) throws Exception {
        if (!network.getStateManager().isStateMultiThreadAccessAllowed()) {
            throw new IllegalArgumentException("State multi thread access has to be activated");
        }

        LoadFlow loadFlow = loadFlowFactory.create(network, computationManager, 0);

        return loadFlow
                .run(baseStateId, LOAD_FLOW_PARAMETERS)
                .thenApply(loadFlowInBaseStateResult -> {

                    network.getStateManager().setWorkingState(baseStateId);

                    ContingencyDbFacade contingencyDbFacade = new SimpleContingencyDbFacade(contingenciesActionsDbClient, network);

                    StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

                    Collection<Contingency> contingencies = contingencyDbFacade.getContingencies();
                    LOGGER.info("Network {}: working on {} contingencies", network.getId(), contingencies.size());

                    List<CompletableFuture<WCACluster>> clusters = new ArrayList<>(contingencies.size());

                    WCAFilteredClusters filteredClusters = new WCAFilteredClusters(network.getId(), contingencies.stream().map(Contingency::getId).collect(Collectors.toList()));

                    if (!loadFlowInBaseStateResult.isOk()) {
                        LOGGER.error("Network {}: load flow on base state diverged, metrics = {}", network.getId(), loadFlowInBaseStateResult.getMetrics());
                        wcaReport.setBaseStateLoadflowResult(new WCALoadflowResult(false, "load flow on base state diverged: metrics = " + loadFlowInBaseStateResult.getMetrics()));
                        contingencies.forEach(contingency -> {
                            filteredClusters.removeClusters(contingency.getId(),
                                                            EnumSet.of(WCAClusterNum.ONE, WCAClusterNum.TWO, WCAClusterNum.THREE),
                                                            WCAClusterOrigin.LF_DIVERGENCE);
                            clusters.add(CompletableFuture.completedFuture(new WCAClusterImpl(contingency,
                                                                                              WCAClusterNum.FOUR,
                                                                                              EnumSet.of(WCAClusterOrigin.LF_DIVERGENCE),
                                                                                              Collections.emptyList())));
                        });
                    } else {
                        wcaReport.setBaseStateLoadflowResult(new WCALoadflowResult(true, null));
                        List<LimitViolation> baseStateLimitViolations = violationsFilter.apply(Security.checkLimits(network), network);
                        if (baseStateLimitViolations.size() > 0) {
                            LOGGER.warn("Network {}: constraint violantions found in base state:\n{}",
                                        network.getId(), Security.printLimitsViolations(baseStateLimitViolations, network, violationsFilter));
                            wcaReport.setPreContingencyViolationsWithoutUncertainties(baseStateLimitViolations);
                            contingencies.forEach(contingency -> filteredClusters.removeClusters(contingency.getId(),
                                                                                                 EnumSet.of(WCAClusterNum.ONE, WCAClusterNum.TWO),
                                                                                                 WCAClusterOrigin.LF_BASIC_VIOLATION));
                        }
                        Supplier<CompletableFuture<Uncertainties>> uncertainties = Suppliers.memoize(() -> {
                            network.getStateManager().setWorkingState(baseStateId);
                            try {
                                if (!(config.getPreventiveActionsFilter() == WCAPreventiveActionsFilter.DOMAINS)
                                        && !(config.getPreventiveActionsOptimizer() == WCAPreventiveActionsOptimizer.DOMAINS)
                                        && !(parameters.getOfflineWorkflowId() != null)
                                        && !(config.getCurativeActionsOptimizer() == WCACurativeActionsOptimizer.CLUSTERS)) {
                                    return CompletableFuture.completedFuture(new Uncertainties(Collections.emptyList(), 0));
                                }
                                LOGGER.info("Network {}: computing uncertainities", network.getId());
                                return uncertaintiesAnalyserFactory.create(network, histoDbClient, computationManager).analyse(parameters.getHistoInterval());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                        Supplier<CompletableFuture<WCAHistoLimits>> histoLimits
                                = Suppliers.memoize(() -> CompletableFuture.supplyAsync(() -> {
                                    network.getStateManager().setWorkingState(baseStateId);
                                    try {
                                        WCAHistoLimits limits = new WCAHistoLimits(parameters.getHistoInterval());
                                        if (config.getPreventiveActionsFilter() == WCAPreventiveActionsFilter.DOMAINS
                                                || config.getPreventiveActionsOptimizer() == WCAPreventiveActionsOptimizer.DOMAINS
                                                || parameters.getOfflineWorkflowId() != null
                                                || config.getCurativeActionsOptimizer() == WCACurativeActionsOptimizer.CLUSTERS) {
                                            LOGGER.info("Network {}: computing historical limits", network.getId());
                                            limits.load(network, histoDbClient);
                                        }
                                        return limits;
                                    } catch (InterruptedException | IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }, computationManager.getExecutor()));
                        List<LimitViolation> violationsToBePrevented = baseStateLimitViolations;
                        if (WCAPreventiveActionsFilter.DOMAINS.equals(config.getPreventiveActionsFilter())) {
                            LOGGER.info("Network {}: running 'domains' preventive actions filter", network.getId());
                            violationsToBePrevented = createDomainsTaskWithDeps(null, baseStateId, Collections.emptyList(), uncertainties,
                                                                                histoLimits, mapper, Collections.emptyList(), Collections.emptyList(),
                                                                                config.activateFiltering())
                                    .thenCompose(domainsResult -> {
                                        if (domainsResult.foundBasicViolations()) {
                                            LOGGER.info("Network {}: 'domains' found basic violations", network.getId());
                                            String domainsUncertaintiesState = "domainsUncertaintiesState";
                                            LOGGER.info("Network {}: creating state with 'domains' uncertainties", network.getId());
                                            network.getStateManager().cloneState(baseStateId, domainsUncertaintiesState);
                                            network.getStateManager().setWorkingState(domainsUncertaintiesState);
                                            WCAUtils.applyInjections(network, domainsUncertaintiesState, domainsResult.getInjections());
                                            LOGGER.info("Network {}: running loadflow on state with 'domains' uncertainties", network.getId());
                                            return loadFlow.run(domainsUncertaintiesState, LOAD_FLOW_PARAMETERS)
                                                    .thenApply(loadFlowResult -> {
                                                        if (!loadFlowResult.isOk()) {
                                                            LOGGER.info("Network {}: loadflow on state with 'domains' uncertainties diverged, metrics = {}", network.getId(), loadFlowResult.getMetrics());
                                                            wcaReport.setBaseStateWithUncertaintiesLoadflowResult(new WCALoadflowResult(false, "load flow on state with 'domains' uncertainties diverged: metrics = " + loadFlowResult.getMetrics()));
                                                            return CompletableFuture.completedFuture(baseStateLimitViolations);
                                                        } else {
                                                            List<LimitViolation> domainsLimitViolations = violationsFilter.apply(Security.checkLimits(network), network);
                                                            if (domainsLimitViolations.size() > 0) {
                                                                LOGGER.warn("Network {}: constraint violantions found in state with 'domains' uncertainties:\n{}",
                                                                            network.getId(), Security.printLimitsViolations(domainsLimitViolations, network, violationsFilter));
                                                                wcaReport.setPreContingencyViolationsWithUncertainties(domainsLimitViolations);
                                                                contingencies.forEach(contingency -> filteredClusters.removeClusters(contingency.getId(),
                                                                                                                                     EnumSet.of(WCAClusterNum.ONE, WCAClusterNum.TWO),
                                                                                                                                     WCAClusterOrigin.DOMAINS_BASIC_VIOLATION));
                                                                return CompletableFuture.completedFuture(domainsLimitViolations);
                                                            } else {
                                                                LOGGER.warn("Network {}: no violations found in state with 'domains' uncertainties", network.getId());
                                                            }
                                                            return CompletableFuture.completedFuture(baseStateLimitViolations);
                                                        }
                                                    }).join();
                                        }
                                        return CompletableFuture.completedFuture(baseStateLimitViolations);
                                    })
                                    .join();
                            network.getStateManager().setWorkingState(baseStateId);
                        }
                        LOGGER.info("Network {}: {} violations to be prevented:\n{}",
                                    network.getId(), violationsToBePrevented.size(), Security.printLimitsViolations(violationsToBePrevented, network, violationsFilter));
                        List<String> preventiveStateIdsForDomains = Collections.synchronizedList(new ArrayList<>());
                        List<String> preventiveActionIdsForDomains  = Collections.synchronizedList(new ArrayList<>());
                        Map<String, List<Action>> possibleActionsToApply = Collections.synchronizedMap(new HashMap<String, List<Action>>());
                        wcaReport.setBaseStateRemainingViolations(violationsToBePrevented); // should I use these violations (they could come from 'domains' worst uncertainties state) or the original basecase violations?
                        if (violationsToBePrevented.size() > 0) {
                            if (config.getPreventiveActionsOptimizer().equals(WCAPreventiveActionsOptimizer.NONE)) {
                                contingencies.forEach(contingency -> filteredClusters.removeClusters(contingency.getId(),
                                                                                                     EnumSet.of(WCAClusterNum.ONE, WCAClusterNum.TWO, WCAClusterNum.THREE),
                                                                                                     null));
                            } else {
                                LOGGER.info("Network {}: getting preventive actions", network.getId());
                                violationsToBePrevented.sort((o1, o2) -> (int) Math.ceil(((o2.getValue() - o2.getLimit()) / o2.getValue()) - ((o1.getValue() - o1.getLimit()) / o1.getValue())));
                                String previousState = baseStateId;
                                List<LimitViolation> solvedViolations = new ArrayList<>();
                                List<LimitViolation> originalViolations = violationsToBePrevented;
                                violationsLoop: for (LimitViolation violationToBePrevented : violationsToBePrevented) {
                                    List<List<Action>> preventiveActions = contingencyDbFacade.getPreventiveActions(violationToBePrevented);
                                    if (preventiveActions.isEmpty()) {
                                        continue;
                                    }
                                    for (int i = 0; i < preventiveActions.size(); i++) {
                                        List<Action> preventiveAction = preventiveActions.get(i);
                                        String preventiveActionId = preventiveAction.stream().map(Action::getId).collect(Collectors.joining("+"));
                                        if (preventiveStateIdsForDomains.contains(preventiveActionId)) {
                                            continue;
                                        }
                                        String preventiveStateId = previousState + "_" + preventiveActionId;
                                        LOGGER.info("Network {}, preventive action {}: starting analysis for {} violation on equipment {}",
                                                    network.getId(), preventiveActionId, violationToBePrevented.getLimitType(), violationToBePrevented.getSubjectId());
                                        possibleActionsToApply.put(preventiveActionId, preventiveAction);
                                        network.getStateManager().cloneState(previousState, preventiveStateId);
                                        network.getStateManager().setWorkingState(preventiveStateId);
                                        for (Action subAction : preventiveAction) {
                                            subAction.toTask().modify(network, computationManager);
                                        }
                                        LoadFlowResult loadFlowResult1;
                                        try {
                                            loadFlowResult1 = loadFlow.run(network.getStateManager().getWorkingStateId(), LOAD_FLOW_PARAMETERS).join();
                                            if (loadFlowResult1.isOk()) {
                                                List<LimitViolation> preventiveStateLimitViolations = violationsFilter.apply(Security.checkLimits(network), network);
                                                Optional<LimitViolation> notSolvedLimitViolation = preventiveStateLimitViolations
                                                        .stream()
                                                        .filter(preventiveStateLimitViolation -> preventiveStateLimitViolation.getSubjectId().equals(violationToBePrevented.getSubjectId()))
                                                        .findAny();
                                                Optional<LimitViolation> previouslySolvedLimitViolation = preventiveStateLimitViolations
                                                        .stream()
                                                        .filter(preventiveStateLimitViolation -> WCAUtils.containsViolation(solvedViolations, preventiveStateLimitViolation))
                                                        .findAny();
                                                Optional<LimitViolation> newLimitViolation = preventiveStateLimitViolations
                                                        .stream()
                                                        .filter(preventiveStateLimitViolation -> !WCAUtils.containsViolation(originalViolations, preventiveStateLimitViolation))
                                                        .findAny();
                                                if (notSolvedLimitViolation.isPresent() || previouslySolvedLimitViolation.isPresent() || newLimitViolation.isPresent()) {
                                                    String message = null;
                                                    if (notSolvedLimitViolation.isPresent()) {
                                                        message = "post preventive action state still contains " + violationToBePrevented.getLimitType()
                                                                + " violation on equiment " + violationToBePrevented.getSubjectId();
                                                    } else if (previouslySolvedLimitViolation.isPresent()) {
                                                        message = "post preventive action state contains previously solved violations";
                                                    } else if (newLimitViolation.isPresent()) {
                                                        message = "post preventive action state contains new violations";
                                                    }
                                                    LOGGER.warn("Network {}, preventive action {}: {}:\n{}",
                                                                network.getId(), preventiveActionId, message, Security.printLimitsViolations(preventiveStateLimitViolations, network, violationsFilter));
                                                    if (!config.filterPreventiveActions()) {
                                                        LOGGER.info("Network {}, preventive action {}: adding anyway preventive action to list (config filterPreventiveActions = false)",
                                                                    network.getId(), preventiveActionId);
                                                        preventiveStateIdsForDomains.add(preventiveStateId);
                                                        preventiveActionIdsForDomains.add(preventiveActionId);
                                                        previousState = preventiveStateId;
                                                        solvedViolations.add(violationToBePrevented);
                                                    }
                                                    WCAActionApplication actionApplication = new WCAActionApplication(preventiveActionId,
                                                            violationToBePrevented,
                                                            new WCALoadflowResult(true, null),
                                                            false,
                                                            false,
                                                            message);
                                                    actionApplication.setPostActionViolations(preventiveStateLimitViolations);
                                                    wcaReport.addPreventiveActionApplication(actionApplication);
                                                } else {
                                                    LOGGER.info("Network {}, preventive action {} solves {} violation on equiment {}: adding preventive action to list",
                                                                network.getId(), preventiveActionId, violationToBePrevented.getLimitType(), violationToBePrevented.getSubjectId());
                                                    preventiveStateIdsForDomains.add(preventiveStateId);
                                                    preventiveActionIdsForDomains.add(preventiveActionId);
                                                    previousState = preventiveStateId;
                                                    solvedViolations.add(violationToBePrevented);
                                                    WCAActionApplication actionApplication = new WCAActionApplication(preventiveActionId,
                                                                                                                      violationToBePrevented,
                                                                                                                      new WCALoadflowResult(true, null),
                                                                                                                      true,
                                                                                                                      false,
                                                                                                                      null);
                                                    actionApplication.setPostActionViolations(preventiveStateLimitViolations);
                                                    wcaReport.addPreventiveActionApplication(actionApplication);
                                                    if (preventiveStateLimitViolations.isEmpty()) {
                                                        LOGGER.info("Network {}, preventive action {} solved all the remaining violations: stopping preventive actions analysis",
                                                                    network.getId(), preventiveActionId);
                                                        break violationsLoop;
                                                    }
                                                    break;
                                                }
                                            } else {
                                                LOGGER.warn("Network {}, preventive action {}: loadflow on post preventive action state diverged, metrics = {}",
                                                            network.getId(), preventiveActionId, loadFlowResult1.getMetrics());
                                                wcaReport.addPreventiveActionApplication(new WCAActionApplication(preventiveActionId,
                                                                                                                  violationToBePrevented,
                                                                                                                  new WCALoadflowResult(false, "loadflow on post preventive action state diverged: metrics = " + loadFlowResult1.getMetrics()),
                                                                                                                  false,
                                                                                                                  false,
                                                                                                                  null));
                                            }
                                        } catch (Exception e) {
                                            LOGGER.error("Network {}, preventive action {}: loadflow on post preventive action state failed: {}",
                                                         network.getId(), preventiveActionId, e.getMessage(), e);
                                            wcaReport.addPreventiveActionApplication(new WCAActionApplication(preventiveActionId,
                                                                                     violationToBePrevented,
                                                                                     new WCALoadflowResult(false, "loadflow on post preventive action state failed: " + e.getMessage()),
                                                                                     false,
                                                                                     false,
                                                                                     null));
                                        }
                                    }
                                }
                                LOGGER.info("Network {}: found {} preventive actions", network.getId(), preventiveActionIdsForDomains.size());
                                Collection<String> preventiveActionsToApply = CompletableFuture.completedFuture(new ArrayList<String>())
                                        .thenCompose(ignored -> {
                                            if (config.getPreventiveActionsOptimizer().equals(WCAPreventiveActionsOptimizer.DOMAINS)
                                                    && !preventiveActionIdsForDomains.isEmpty()) {
                                                LOGGER.info("Network {}: running 'domains' preventive actions optimizer", network.getId());
                                                return createDomainsTaskWithDeps(null, baseStateId, Collections.emptyList(), uncertainties, histoLimits, mapper,
                                                                                 preventiveStateIdsForDomains, preventiveActionIdsForDomains, config.activateFiltering())
                                                                                 .thenCompose(domainsResult -> {
                                                                                     if (domainsResult.getPreventiveActionIndex() > 0) {
                                                                                         LOGGER.info("Network {}: 'domains' found {} preventive actions solved the violations",
                                                                                                     network.getId(), preventiveActionIdsForDomains.subList(0, domainsResult.getPreventiveActionIndex()));
                                                                                         contingencies.forEach(contingency -> filteredClusters.removeClusters(contingency.getId(),
                                                                                                                                                              EnumSet.of(WCAClusterNum.ONE, WCAClusterNum.TWO),
                                                                                                                                                              WCAClusterOrigin.DOMAINS_SPECIFIC_PREVENTIVE_ACTION_FOUND));
                                                                                     } else {
                                                                                         LOGGER.info("Network {}: 'domains' found no preventive actions solving the violations", network.getId());
                                                                                         contingencies.forEach(contingency -> filteredClusters.removeClusters(contingency.getId(),
                                                                                                                                                              EnumSet.of(WCAClusterNum.ONE, WCAClusterNum.TWO, WCAClusterNum.THREE),
                                                                                                                                                              WCAClusterOrigin.DOMAINS_NO_PREVENTIVE_ACTION_FOUND));
                                                                                     }
                                                                                     return CompletableFuture.completedFuture(new ArrayList<String>(preventiveActionIdsForDomains.subList(0, domainsResult.getPreventiveActionIndex())));
                                                                                 });
                                            }
                                            return CompletableFuture.completedFuture(new ArrayList<String>());
                                        })
                                        .join();
                                if (preventiveActionsToApply.size() > 0 && config.applyPreventiveActions()) {
                                    LOGGER.info("Network {}: applying preventive actions", network.getId());
                                    network.getStateManager().setWorkingState(baseStateId);
                                    CompletableFuture.runAsync(() -> {
                                        preventiveActionsToApply.forEach(actionId -> {
                                            if (possibleActionsToApply.containsKey(actionId)) {
                                                possibleActionsToApply.get(actionId).forEach(action -> {
                                                    LOGGER.info("Network {}: applying preventive action {}, elementary action {}", network.getId(), actionId, action.getId());
                                                    action.toTask().modify(network, computationManager);
                                                });
                                                wcaReport.setPreventiveActionAsApplied(actionId);
                                            } else {
                                                LOGGER.warn("Network {}: action {} not found in preventive actions", network.getId(), actionId);
                                            }
                                        });
                                    }, computationManager.getExecutor())
                                    .thenCompose(ignored -> loadFlow.run(baseStateId, LOAD_FLOW_PARAMETERS))
                                    .thenAccept(ignored -> {
                                        // check that the violations have disappeared? this check has already been done previously
                                        // update basecase remaining violations
                                        wcaReport.setBaseStateRemainingViolations(violationsFilter.apply(Security.checkLimits(network), network));
                                    })
                                    .exceptionally(throwable -> {
                                        if (throwable != null) {
                                            LOGGER.error(throwable.toString(), throwable);
                                        }
                                        return null;
                                    })
                                        .join();
                                }
                                if (contingencies.stream().allMatch(
                                    contingency -> filteredClusters.getClusters(contingency.getId()).size() == 1
                                                         && WCAClusterNum.FOUR.equals(filteredClusters.getCluster(contingency.getId()))
                                        ) || config.loosenConstraints()) {
                                    wcaReport.setPostPreventiveActionsViolationsWithUncertainties(wcaReport.getBaseStateRemainingViolations());
                                    if (!wcaReport.getBaseStateRemainingViolations().isEmpty()) {
                                        LOGGER.warn("Network {}: loosening the basecase constraints for remaining violations:\n{}",
                                                    network.getId(), Security.printLimitsViolations(new ArrayList<>(wcaReport.getBaseStateRemainingViolations()), network, violationsFilter));
                                        ConstraintsModifierConfig constraintsModifierConfig = new ConstraintsModifierConfig(
                                                config.getCountryConstraintFilter(),
                                                config.ignoreVoltageConstraints() ? EnumSet.of(LimitViolationType.CURRENT) : EnumSet.allOf(LimitViolationType.class)
                                                );
                                        ConstraintsModifier constraintsModifier = new ConstraintsModifier(network, constraintsModifierConfig);
                                        constraintsModifier.looseConstraints(baseStateId,
                                                                             new ArrayList<>(wcaReport.getBaseStateRemainingViolations()),
                                                                             config.getMargin());
                                    }
                                }

                            }
                        }
                        for (Contingency contingency : contingencies) {
                            LOGGER.info("Network {}, contingency {}: starting analysis", network.getId(), contingency.getId());
                            clusters.add(CompletableFuture
                                    .completedFuture(null)
                                    .thenComposeAsync(ignored -> {
                                        network.getStateManager().setWorkingState(baseStateId);
                                        boolean rulesViolated = false;
                                        List<SecurityRuleExpression> securityRuleExpressions = new ArrayList<>();
                                        if (parameters.getOfflineWorkflowId() != null) {
                                            LOGGER.info("Network {}, contingency {}: starting security rules analysis", network.getId(), contingency.getId());
                                            Supplier<Map<HistoDbAttributeId, Object>> baseStateAttributeValues = Suppliers
                                                    .memoize((Supplier<Map<HistoDbAttributeId, Object>>) () -> IIDM2DB
                                                            .extractCimValues(network, new IIDM2DB.Config(null, false))
                                                            .getSingleValueMap());
                                            for (RuleAttributeSet attributeSet : RuleAttributeSet.values()) {
                                                for (SecurityIndexType securityIndexType : getSecurityIndexTypes(parameters)) {
                                                    List<SecurityRule> securityRules = rulesDbClient.getRules(parameters.getOfflineWorkflowId(),
                                                                                                              attributeSet,
                                                                                                              contingency.getId(),
                                                                                                              securityIndexType);
                                                    if (securityRules.isEmpty()) {
                                                        rulesViolated = true;
                                                        String cause = "Missing rule " + new RuleId(attributeSet, new SecurityIndexId(contingency.getId(), securityIndexType));
                                                        LOGGER.warn("Network {}, contingency {}: {}", network.getId(), contingency.getId(), cause);
                                                        wcaReport.addSecurityRulesApplication(new WCASecurityRuleApplication(contingency.getId(),
                                                                                                                             null,
                                                                                                                             true,
                                                                                                                             WCARuleViolationType.MISSING_RULE,
                                                                                                                             cause));
                                                    } else {
                                                        for (SecurityRule securityRule : securityRules) {
                                                            LOGGER.info("Network {}, contingency {}: checking rule {}",
                                                                        network.getId(), contingency.getId(), securityRule.getId());
                                                            String cause = "Rule " + securityRule.getId() + " verified";
                                                            boolean isRuleViolated = false;
                                                            WCARuleViolationType violationType = WCARuleViolationType.NO_VIOLATION;
                                                            SecurityRuleExpression expression = securityRule.toExpression(parameters.getPurityThreshold());
                                                            SecurityRuleCheckReport checkReport = expression.check(baseStateAttributeValues.get());
                                                            if (checkReport.getMissingAttributes().size() > 0) {
                                                                rulesViolated = true;
                                                                isRuleViolated = true;
                                                                violationType = WCARuleViolationType.MISSING_ATTRIBUTE;
                                                                cause = "Missing attributes for rule " + securityRule.getId()
                                                                        + ": " + checkReport.getMissingAttributes().stream().map(Object::toString).collect(Collectors.joining(","));
                                                                LOGGER.warn("Network {}, contingency {}: {}", network.getId(), contingency.getId(), cause);
                                                            } else if (!checkReport.isSafe()) {
                                                                rulesViolated = true;
                                                                isRuleViolated = true;
                                                                violationType = WCARuleViolationType.RULE_NOT_SAFE;
                                                                cause = "Rule " + securityRule.getId() + " not verified";
                                                                LOGGER.warn("Network {}, contingency {}: {}", network.getId(), contingency.getId(), cause);
                                                            } else {
                                                                LOGGER.info("Network {}, contingency {}: adding rule {} to the list for analysis",
                                                                            network.getId(), contingency.getId(), securityRule.getId());
                                                                securityRuleExpressions.add(expression);
                                                            }
                                                            wcaReport.addSecurityRulesApplication(new WCASecurityRuleApplication(contingency.getId(),
                                                                                                                                 securityRule,
                                                                                                                                 isRuleViolated,
                                                                                                                                 violationType,
                                                                                                                                 cause));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (rulesViolated) {
                                            filteredClusters.removeClusters(contingency.getId(),
                                                                            EnumSet.of(WCAClusterNum.ONE),
                                                                            WCAClusterOrigin.LF_RULE_VIOLATION);
                                        }
                                        return CompletableFuture
                                                .completedFuture(rulesViolated)
                                                .thenCompose(rulesViolationsFound -> {
                                                    if (securityRuleExpressions.isEmpty() || (rulesViolationsFound && config.activateFiltering())) {
                                                        return CompletableFuture.completedFuture(new WCADomainsResult());
                                                    }
                                                    LOGGER.info("Network {}: running 'domains' with security rules for contingency {}", network.getId(), contingency.getId());
                                                    return createDomainsTaskWithDeps(contingency, baseStateId, securityRuleExpressions, uncertainties, histoLimits,
                                                                                     mapper, preventiveStateIdsForDomains, preventiveActionIdsForDomains,
                                                                                     config.activateFiltering());
                                                })
                                                .thenCompose(domainsResult -> {
                                                    if (domainsResult.areRulesViolated()) {
                                                        filteredClusters.removeClusters(contingency.getId(),
                                                                                        EnumSet.of(WCAClusterNum.ONE),
                                                                                        WCAClusterOrigin.DOMAINS_RULE_VIOLATION);
                                                    }
                                                    if (domainsResult.areRulesViolated() && config.activateFiltering()) {
                                                        return CompletableFuture.completedFuture(WCAClusterNum.UNDEFINED);
                                                    }
                                                    return createClustersWorkflowTask(contingency, baseStateId, contingencyDbFacade, securityRuleExpressions,
                                                                                      uncertainties, histoLimits, mapper, loadFlow, config.activateFiltering(),
                                                                                      config.filterCurativeActions(), filteredClusters);
                                                })
                                                .thenCompose(clusterNum -> {
                                                    LOGGER.info("Network {}, contingency {}: assigned cluster number {}",
                                                                network.getId(), contingency.getId(), clusterNum);
                                                    if (!clusterNum.equals(WCAClusterNum.UNDEFINED)
                                                            && !filteredClusters.getFlags(contingency.getId()).contains(WCAClusterOrigin.LF_POST_CONTINGENCY_DIVERGENCE)) {
                                                        if (filteredClusters.hasCluster(contingency.getId(), clusterNum)) {
                                                            filteredClusters.removeAllButCluster(contingency.getId(),
                                                                                                 clusterNum,
                                                                                                 WCAClusterOrigin.CLUSTERS_ANALYSIS);
                                                        } else {
                                                            filteredClusters.addClusters(contingency.getId(),
                                                                                         EnumSet.of(clusterNum),
                                                                                         WCAClusterOrigin.CLUSTERS_ANALYSIS);
                                                        }
                                                    }
                                                    LOGGER.info("Network {}, contingency {}: final cluster number {}",
                                                                network.getId(), contingency.getId(), filteredClusters.getCluster(contingency.getId()));
                                                    LOGGER.info("Network {}, contingency {}: final flags {}",
                                                                network.getId(), contingency.getId(), filteredClusters.getFlags(contingency.getId()));
                                                    return CompletableFuture.completedFuture(new WCAClusterImpl(contingency,
                                                                                                                filteredClusters.getCluster(contingency.getId()),
                                                                                                                filteredClusters.getFlags(contingency.getId()),
                                                                                                                Collections.emptyList()));
                                                });
                                    }, computationManager.getExecutor()));
                        }
                    }

                    return clusters;
                });
    }

    @Override
    public CompletableFuture<WCAAsyncResult> runAsync(String baseStateId, WCAParameters parameters) throws Exception {
        LOGGER.info(parameters.toString());
        LOGGER.info("Network {}: starting WCA...", network.getId());
        return createWcaTask(baseStateId, parameters)
                .thenApply(clusters -> () -> clusters);
    }

    @Override
    public WCAResult run(WCAParameters parameters) throws Exception {
        WCAAsyncResult asyncResult = runAsync(network.getStateManager().getWorkingStateId(), parameters)
                .join();
        List<WCACluster> clusters = new ArrayList<>();
        for (CompletableFuture<WCACluster> cluster : asyncResult.getClusters()) {
            clusters.add(cluster.join());
        }
        return () -> clusters;
    }

    @Override
    public WCAReport getReport() {
        return wcaReport;
    }

}
