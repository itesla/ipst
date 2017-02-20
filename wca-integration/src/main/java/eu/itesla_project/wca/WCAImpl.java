/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016-2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.gdata.util.common.base.Pair;
import eu.itesla_project.commons.io.table.Column;
import eu.itesla_project.commons.io.table.TableFormatter;
import eu.itesla_project.commons.util.StringToIntMapper;
import eu.itesla_project.computation.*;
import eu.itesla_project.contingency.Contingency;
import eu.itesla_project.iidm.datasource.DataSource;
import eu.itesla_project.iidm.datasource.FileDataSource;
import eu.itesla_project.iidm.export.ampl.*;
import eu.itesla_project.iidm.export.ampl.util.AmplDatTableFormatter;
import eu.itesla_project.iidm.network.Identifiable;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.StateManager;
import eu.itesla_project.loadflow.api.LoadFlow;
import eu.itesla_project.loadflow.api.LoadFlowFactory;
import eu.itesla_project.loadflow.api.LoadFlowParameters;
import eu.itesla_project.modules.contingencies.Action;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.histo.HistoDbAttributeId;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.histo.IIDM2DB;
import eu.itesla_project.modules.rules.*;
import eu.itesla_project.modules.wca.*;
import eu.itesla_project.security.LimitViolation;
import eu.itesla_project.security.LimitViolationFilter;
import eu.itesla_project.security.LimitViolationType;
import eu.itesla_project.security.Security;
import eu.itesla_project.simulation.securityindexes.SecurityIndexId;
import eu.itesla_project.simulation.securityindexes.SecurityIndexType;
import eu.itesla_project.wca.uncertainties.UncertaintiesAmplWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class WCAImpl implements WCA, WCAConstants, AmplConstants {

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

    private static final float UNCERTAINTY_RATIO = 1f;
    private static final float UNCERTAINTY_THRESHOLD = 50f;
    private static final int DETAILS_LEVEL_NORMAL = 2;
    private static final int DETAILS_LEVEL_DEBUG = 4;

    private static final Pattern CLUSTER_INDEX_PATTERN = Pattern.compile(" WCA Result : contingency_index (\\d*) contingency_cluster_index (\\d*) curative_action_index (\\d*)");
    private static final Pattern DOMAINS_RESULTS_PATTERN = Pattern.compile(" WCA Result : basic_violation (\\d*) rule_violation (\\d*) preventive_action_index (\\d*)");

    private final Network network;

    private final ComputationManager computationManager;

    private final HistoDbClient histoDbClient;

    private final RulesDbClient rulesDbClient;

    private final UncertaintiesAnalyserFactory uncertaintiesAnalyserFactory;

    private final ContingenciesAndActionsDatabaseClient contingenciesActionsDbClient;

    private final LoadFlowFactory loadFlowFactory;

    private final WCAConfig config;

    private final Map<String, String> env;

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

        env = ImmutableMap.of("XPRESS", config.getXpressHome().resolve("bin").toString(),
                "LD_LIBRARY_PATH", config.getXpressHome().resolve("lib").toString());
    }

    private Matcher parseOutFile(String cmdId, Pattern pattern, Path workingDir) throws IOException {

        Path out = workingDir.resolve(cmdId + "_0.out");
        Path outGz = workingDir.resolve(cmdId + "_0.out.gz");
        if (Files.exists(out) || Files.exists(outGz)) {
            try (BufferedReader reader = (Files.exists(out)) ? Files.newBufferedReader(out, StandardCharsets.UTF_8)
                    : new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(outGz.toFile()))))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        return matcher;
                    }
                }
            }
        } else {
            LOGGER.error("WCA output file {} or {} not found !!", out.toFile().getAbsolutePath(), outGz.toFile().getAbsolutePath());
        }
        return null;
    }

    private int parseClusterNum(Path workingDir) throws IOException {
        int clusterNum = -1;
        Matcher matcher = parseOutFile(CLUSTERS_CMD_ID, CLUSTER_INDEX_PATTERN, workingDir);
        if (matcher != null) {
            clusterNum = Integer.parseInt(matcher.group(2));
        }
        return clusterNum;
    }

    private WCAClusterNum readClusterNum(Path workingDir) throws IOException {
        WCAClusterNum clusterNum;
        int clusterValue = parseClusterNum(workingDir);
        switch (clusterValue) {
            case 1:
                clusterNum = WCAClusterNum.ONE;
                break;
            case 2:
                clusterNum = WCAClusterNum.TWO;
                break;
            case 3:
                clusterNum = WCAClusterNum.THREE;
                break;
            case 4:
                clusterNum = WCAClusterNum.FOUR;
                break;
            case -1:
                clusterNum = WCAClusterNum.UNDEFINED;
                break;
            default:
                throw new AssertionError("Undefined cluster value" + clusterValue);
        }
        return clusterNum;
    }

    private WCAClusterOrigin readDomainsResult(Path workingDir) throws IOException {
        Matcher matcher = parseOutFile(DOMAINS_CMD_ID, DOMAINS_RESULTS_PATTERN, workingDir);
        if (matcher != null) {
            int basicViolation = Integer.parseInt(matcher.group(1));
            int ruleViolation = Integer.parseInt(matcher.group(2));
            if (basicViolation == 1) {
                return WCAClusterOrigin.DOMAIN_LIMIT;
            } else if (ruleViolation == 1) {
                return WCAClusterOrigin.DOMAIN_OFFLINE_RULE;
            } else {
                return null;
            }
        }
        return null;
    }

    private static final AmplExportConfig DOMAINS_AMPL_EXPORT_CONFIG = new AmplExportConfig(AmplExportConfig.ExportScope.ONLY_MAIN_CC_AND_CONNECTABLE_GENERATORS_AND_SHUNTS_AND_ALL_LOADS, false, AmplExportConfig.ExportActionType.PREVENTIVE);
    private static final AmplExportConfig CLUSTERS_AMPL_EXPORT_CONFIG = new AmplExportConfig(AmplExportConfig.ExportScope.ONLY_MAIN_CC_AND_CONNECTABLE_GENERATORS_AND_SHUNTS_AND_ALL_LOADS, false, AmplExportConfig.ExportActionType.CURATIVE);

    private static final LoadFlowParameters LOAD_FLOW_PARAMETERS = new LoadFlowParameters()
            .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.UNIFORM_VALUES)
            .setTransformerVoltageControlOn(false)
            .setNoGeneratorReactiveLimits(false)
            .setPhaseShifterRegulationOn(false);

    private static final LimitViolationFilter CURRENT_FILTER = LimitViolationFilter.load()
            .setViolationTypes(EnumSet.of(LimitViolationType.CURRENT));

    private SecurityIndexType[] getSecurityIndexTypes(WCAParameters parameters) {
        return parameters.getSecurityIndexTypes() != null
                ? parameters.getSecurityIndexTypes().toArray(new SecurityIndexType[parameters.getSecurityIndexTypes().size()])
                : SecurityIndexType.values();
    }

    private static void writeContingencies(Collection<Contingency> contingencies, DataSource dataSource, StringToIntMapper<AmplSubset> mapper) {
        try (TableFormatter formatter = new AmplDatTableFormatter(
                new OutputStreamWriter(dataSource.newOutputStream(FAULTS_FILE_SUFFIX, TXT_EXT, false), StandardCharsets.UTF_8),
                "Contingencies",
                INVALID_FLOAT_VALUE,
                true,
                LOCALE,
                new Column("num"),
                new Column("id"))) {
            for (Contingency contingency : contingencies) {
                int contingencyNum = mapper.getInt(AmplSubset.FAULT, contingency.getId());
                formatter.writeCell(contingencyNum)
                        .writeCell(contingency.getId());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeActions(Collection<String> actionIds, DataSource dataSource, StringToIntMapper<AmplSubset> mapper,
                                     String title, AmplSubset amplSubset) {
        try (TableFormatter formatter = new AmplDatTableFormatter(
                new OutputStreamWriter(dataSource.newOutputStream(ACTIONS_FILE_SUFFIX, TXT_EXT, false), StandardCharsets.UTF_8),
                title,
                INVALID_FLOAT_VALUE,
                true,
                LOCALE,
                new Column("num"),
                new Column("id"))) {
            for (String actionId : actionIds) {
                int actionNum = mapper.getInt(amplSubset, actionId);
                formatter.writeCell(actionNum)
                        .writeCell(actionId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private CompletableFuture<WCACluster> createClusterTask(Contingency contingency, List<String> curativeActionIds,
                                                            String baseStateId, String contingencyStateId,
                                                            List<String> curativeStateIds, List<SecurityRuleExpression> securityRuleExpressions,
                                                            Uncertainties uncertainties, WCAHistoLimits histoLimits,
                                                            StringToIntMapper<AmplSubset> mapper,
                                                            boolean stopWcaOnViolations) {
        return computationManager.execute(new ExecutionEnvironment(env, CLUSTERS_WORKING_DIR_PREFIX, config.isDebug()),
                new DefaultExecutionHandler<WCACluster>() {

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
                        new AmplNetworkWriter(network, dataSource, 0, 0, false, mapper, CLUSTERS_AMPL_EXPORT_CONFIG).write();
                        if (config.isExportStates()) {
                            WCAUtils.exportState(network, workingDir, 0, 0);
                        }

                        // write post contingency state
                        network.getStateManager().setWorkingState(contingencyStateId);
                        AmplUtil.fillMapper(mapper, network); // because action can create a new bus
                        new AmplNetworkWriter(network, dataSource, contingencyNum, 0, true, mapper, CLUSTERS_AMPL_EXPORT_CONFIG).write();
                        if (config.isExportStates()) {
                            WCAUtils.exportState(network, workingDir, contingencyNum, 0);
                        }

                        // write contingency description
                        writeContingencies(Collections.singleton(contingency), dataSource, mapper);

                        // write security rules corresponding to the contingency
                        new WCASecurityRulesWriter(network, securityRuleExpressions, dataSource, mapper, false, stopWcaOnViolations).write();

                        // write post curative state
                        for (int i = 0; i < curativeActionIds.size(); i++) {
                            String curativeActionId = curativeActionIds.get(i);
                            int curativeActionNum = mapper.newInt(AmplSubset.CURATIVE_ACTION, curativeActionId);

                            String curativeStateId = curativeStateIds.get(i);
                            network.getStateManager().setWorkingState(curativeStateId);
                            AmplUtil.fillMapper(mapper, network); // because action can create a new bus
                            new AmplNetworkWriter(network, dataSource, contingencyNum, curativeActionNum, true, mapper, CLUSTERS_AMPL_EXPORT_CONFIG).write();
                            if (config.isExportStates()) {
                                WCAUtils.exportState(network, workingDir, contingencyNum, curativeActionNum);
                            }
                        }

                        // write curatives action description associated to the contingency
                        writeActions(curativeActionIds, dataSource, mapper, "Curative actions", AmplSubset.CURATIVE_ACTION);

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
                                        Integer.toString(WCARestrictingThresholdLevel.getLevel(config.getRestrictingThresholdLevels())))
                                .build();
                        return Arrays.asList(new CommandExecution(cmd, 1));
                    }

                    @Override
                    public WCACluster after(Path workingDir, ExecutionReport report) throws IOException {
                        report.log();
                        WCAClusterNum clusterNum = readClusterNum(workingDir);
                        return new WCAClusterImpl(contingency, clusterNum, WCAClusterOrigin.CLUSTER,
                                Collections.singletonList("Actions tested by clusters: " + curativeActionIds));
                    }
                });
    }

    private CompletableFuture<WCACluster> createDomainTask(Contingency contingency, String baseStateId,
                                                           List<SecurityRuleExpression> securityRuleExpressions,
                                                           Uncertainties uncertainties, WCAHistoLimits histoLimits,
                                                           StringToIntMapper<AmplSubset> mapper,
                                                           List<String> preventiveStateIds,
                                                           List<String> preventiveActionIds,
                                                           boolean stopWcaOnViolations) {
        return computationManager.execute(new ExecutionEnvironment(env, DOMAINS_WORKING_DIR_PREFIX, config.isDebug()),
                new DefaultExecutionHandler<WCACluster>() {

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
                        new AmplNetworkWriter(network, dataSource, 0, 0, false, mapper, DOMAINS_AMPL_EXPORT_CONFIG).write();
                        if (config.isExportStates()) {
                            WCAUtils.exportState(network, workingDir, 0, 0);
                        }

                        // write contingency description
                        writeContingencies(Collections.singleton(contingency), dataSource, mapper);

                        // write post preventive state
                        for (int i = 0; i < preventiveActionIds.size(); i++) {
                            String preventiveActionId = preventiveActionIds.get(i);
                            int preventiveActionNum = mapper.newInt(AmplSubset.PREVENTIVE_ACTION, preventiveActionId);

                            String preventiveStateId = preventiveStateIds.get(i);
                            network.getStateManager().setWorkingState(preventiveStateId);
                            AmplUtil.fillMapper(mapper, network); // because action can create a new bus
                            new AmplNetworkWriter(network, dataSource, 0, preventiveActionNum, true, mapper, DOMAINS_AMPL_EXPORT_CONFIG).write();
                            if (config.isExportStates()) {
                                WCAUtils.exportState(network, workingDir, 0, preventiveActionNum);
                            }
                        }

                        // write preventive action description
                        writeActions(preventiveActionIds, dataSource, mapper, "Preventive actions", AmplSubset.PREVENTIVE_ACTION);

                        // write security rules corresponding to the contingency
                        new WCASecurityRulesWriter(network, securityRuleExpressions, dataSource, mapper, false, stopWcaOnViolations).write();

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
                                        Integer.toString(WCARestrictingThresholdLevel.getLevel(config.getRestrictingThresholdLevels())))
                                .build();
                        return Arrays.asList(new CommandExecution(cmd, 1));
                    }

                    @Override
                    public WCACluster after(Path workingDir, ExecutionReport report) throws IOException {
                        report.log();
                        WCAClusterOrigin origin = readDomainsResult(workingDir);
                        if (origin != null) {
                            return new WCAClusterImpl(contingency, WCAClusterNum.FOUR, origin, Collections.emptyList());
                        }
                        return null;
                    }
                });
    }

    private CompletableFuture<WCACluster> createClusterTaskWithDeps(Contingency contingency, List<String> curativeActionIds,
                                                                    String baseStateId, String contingencyStateId,
                                                                    List<String> curativeStateIds, List<SecurityRuleExpression> securityRuleExpressions,
                                                                    Supplier<CompletableFuture<Uncertainties>> memoizedUncertaintiesFuture,
                                                                    Supplier<CompletableFuture<WCAHistoLimits>> histoLimitsFuture,
                                                                    StringToIntMapper<AmplSubset> mapper,
                                                                    boolean stopWcaOnViolations) {
        return memoizedUncertaintiesFuture.get()
                .thenCombine(histoLimitsFuture.get(), (uncertainties, histoLimits) -> Pair.of(uncertainties, histoLimits))
                .thenCompose(p -> createClusterTask(contingency, curativeActionIds, baseStateId, contingencyStateId,
                        curativeStateIds, securityRuleExpressions, p.getFirst(), p.getSecond(), mapper, stopWcaOnViolations));
    }

    private CompletableFuture<WCACluster> createDomainTaskWithDeps(Contingency contingency, String baseStateId,
                                                                   List<SecurityRuleExpression> securityRuleExpressions,
                                                                   Supplier<CompletableFuture<Uncertainties>> memoizedUncertaintiesFuture,
                                                                   Supplier<CompletableFuture<WCAHistoLimits>> histoLimitsFuture,
                                                                   StringToIntMapper<AmplSubset> mapper,
                                                                   List<String> preventiveStateIds,
                                                                   List<String> preventiveActionIds,
                                                                   boolean stopWcaOnViolations) {
        return memoizedUncertaintiesFuture.get()
                .thenCombine(histoLimitsFuture.get(), (uncertainties, histoLimits) -> Pair.of(uncertainties, histoLimits))
                .thenCompose(p -> createDomainTask(contingency, baseStateId, securityRuleExpressions, p.getFirst(), p.getSecond(), mapper,
                        preventiveStateIds, preventiveActionIds, stopWcaOnViolations));
    }

    private CompletableFuture<WCACluster> createClusterWorkflowTask(Contingency contingency, String baseStateId,
                                                                    ContingencyDbFacade contingencyDbFacade,
                                                                    List<SecurityRuleExpression> securityRuleExpressions,
                                                                    Supplier<CompletableFuture<Uncertainties>> memoizedUncertaintiesFuture,
                                                                    Supplier<CompletableFuture<WCAHistoLimits>> histoLimitsFuture,
                                                                    StringToIntMapper<AmplSubset> mapper,
                                                                    LoadFlow loadFlow,
                                                                    boolean stopWcaOnViolations) {
        String[] contingencyStateId = new String[1];
        List<String> curativeStateIds = Collections.synchronizedList(new ArrayList<>());
        List<String> curativeActionIds = Collections.synchronizedList(new ArrayList<>());

        return CompletableFuture
                .runAsync(() -> {
                    // create post contingency state
                    contingencyStateId[0] = baseStateId + "_" + contingency.getId();
                    network.getStateManager().cloneState(baseStateId, contingencyStateId[0]);
                    network.getStateManager().setWorkingState(contingencyStateId[0]);

                    // apply contingency to the network
                    contingency.toTask().modify(network);

                }, computationManager.getExecutor())
                .thenCompose(aVoid -> loadFlow.runAsync(contingencyStateId[0], LOAD_FLOW_PARAMETERS))
                .thenCompose(loadFlowResult -> {
                    if (!loadFlowResult.isOk()) {
                        return CompletableFuture.completedFuture(new WCAClusterImpl(contingency,
                                WCAClusterNum.FOUR,
                                WCAClusterOrigin.HADES_POST_CONTINGENCY_DIVERGENCE,
                                Collections.singletonList(contingency.getId())));
                    } else {
                        network.getStateManager().setWorkingState(contingencyStateId[0]);

                        List<LimitViolation> contingencyStateLimitViolations = CURRENT_FILTER.apply(Security.checkLimits(network));

                        List<List<Action>> curativeActions = contingencyDbFacade.getCurativeActions(contingency, null); // TODO pass post contingency violations?

                        if (curativeActions.isEmpty()) {
                            // check limits on contingency state
                            if (contingencyStateLimitViolations.size() > 0 && stopWcaOnViolations) {
                                return CompletableFuture.completedFuture(new WCAClusterImpl(contingency,
                                        WCAClusterNum.FOUR,
                                        WCAClusterOrigin.HADES_POST_CONTINGENCY_LIMIT,
                                        Collections.singletonList(contingency.getId())));
                            } else {
                                return createClusterTaskWithDeps(contingency, Collections.emptyList(), baseStateId, contingencyStateId[0],
                                        Collections.emptyList(), securityRuleExpressions, memoizedUncertaintiesFuture,
                                        histoLimitsFuture, mapper, stopWcaOnViolations);
                            }
                        } else {
                            CompletableFuture<?>[] curativeActionTasks = new CompletableFuture[curativeActions.size()];
                            List<String> curativeStateIdsForClusters = Collections.synchronizedList(new ArrayList<>());
                            List<String> curativeActionIdsForClusters = Collections.synchronizedList(new ArrayList<>());

                            // create post curative states
                            for (int i = 0; i < curativeActions.size(); i++) {
                                List<Action> curativeAction = curativeActions.get(i);

                                String curativeActionId = curativeAction.stream().map(Action::getId).collect(Collectors.joining("+"));
                                curativeActionIds.add(curativeActionId);
                                String curativeStateId = contingencyStateId[0] + "_" + curativeActionId;
                                curativeStateIds.add(curativeStateId);

                                curativeActionTasks[i] = CompletableFuture.runAsync(() -> {
                                    network.getStateManager().cloneState(contingencyStateId[0], curativeStateId);
                                    network.getStateManager().setWorkingState(curativeStateId);

                                    // apply curative actions to the network
                                    for (Action subAction : curativeAction) {
                                        subAction.toTask().modify(network);
                                    }
                                }, computationManager.getExecutor())
                                        .thenCompose(ignored -> loadFlow.runAsync(curativeStateId, LOAD_FLOW_PARAMETERS))
                                        .thenAccept(loadFlowResult1 -> {
                                            if (loadFlowResult1.isOk()) {
                                                network.getStateManager().setWorkingState(curativeStateId);

                                                List<LimitViolation> curativeStateLimitViolations = CURRENT_FILTER.apply(Security.checkLimits(network));
                                                if (curativeStateLimitViolations.isEmpty() || !stopWcaOnViolations) {
                                                    curativeStateIdsForClusters.add(curativeStateId);
                                                    curativeActionIdsForClusters.add(curativeActionId);
                                                }
                                            }
                                        })
                                        .exceptionally(throwable -> {
                                            if (throwable != null) {
                                                LOGGER.error(throwable.toString(), throwable);
                                            }
                                            return null;
                                        });
                            }

                            return CompletableFuture.allOf(curativeActionTasks)
                                    .thenComposeAsync(aVoid -> createClusterTaskWithDeps(contingency, curativeActionIds, baseStateId, contingencyStateId[0],
                                            curativeStateIds, securityRuleExpressions, memoizedUncertaintiesFuture,
                                            histoLimitsFuture, mapper, stopWcaOnViolations));
                        }
                    }
                })
                .handle((cluster, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error(throwable.toString(), throwable);
                    }

                    // cleanup working states
                    network.getStateManager().removeState(contingencyStateId[0]);
                    for (String curativeStateId : curativeStateIds) {
                        network.getStateManager().removeState(curativeStateId);
                    }
                    return cluster;
                });
    }

    private CompletableFuture<List<CompletableFuture<WCACluster>>> createWcaTask(String baseStateId, WCAParameters parameters) throws Exception {
        if (!network.getStateManager().isStateMultiThreadAccessAllowed()) {
            throw new IllegalArgumentException("State multi thread access has to be activated");
        }

        LoadFlow loadFlow = loadFlowFactory.create(network, computationManager, 0);

        // run the LF on N state
        return loadFlow
                .runAsync(baseStateId, LOAD_FLOW_PARAMETERS)
                .thenApply(loadFlowInBaseStateResult -> {

                    network.getStateManager().setWorkingState(baseStateId);

                    ContingencyDbFacade contingencyDbFacade = new SimpleContingencyDbFacade(contingenciesActionsDbClient, network);

                    StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

                    Collection<Contingency> contingencies = contingencyDbFacade.getContingencies();

                    List<CompletableFuture<WCACluster>> clusters = new ArrayList<>(contingencies.size());

                    // check base load flow divergence
                    if (!loadFlowInBaseStateResult.isOk()) {
                        for (Contingency contingency : contingencies) {
                            clusters.add(CompletableFuture.completedFuture(new WCAClusterImpl(contingency,
                                    WCAClusterNum.FOUR,
                                    WCAClusterOrigin.HADES_BASE_DIVERGENCE,
                                    Collections.emptyList())));
                        }
                    } else {
                        // check limits on base state
                        List<LimitViolation> baseStateLimitViolations = CURRENT_FILTER.apply(Security.checkLimits(network));
                        if (baseStateLimitViolations.size() > 0 && parameters.stopWcaOnViolations()) {
                            for (Contingency contingency : contingencies) {
                                clusters.add(CompletableFuture.completedFuture(new WCAClusterImpl(contingency,
                                        WCAClusterNum.FOUR,
                                        WCAClusterOrigin.HADES_BASE_LIMIT,
                                        baseStateLimitViolations.stream().map(LimitViolation::getSubject)
                                                .map(Identifiable::getId)
                                                .distinct()
                                                .collect(Collectors.toList()))));
                            }
                        } else {
                            // commons tasks to all contingency task, a supplier is used to cache to completable future
                            // result
                            Supplier<CompletableFuture<Uncertainties>> uncertainties = Suppliers.memoize(() -> {
                                network.getStateManager().setWorkingState(baseStateId);
                                try {
                                    return uncertaintiesAnalyserFactory.create(network, histoDbClient, computationManager)
                                            .analyse(parameters.getHistoInterval());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });

                            Supplier<CompletableFuture<WCAHistoLimits>> histoLimits
                                    = Suppliers.memoize(() -> CompletableFuture.supplyAsync(() -> {
                                network.getStateManager().setWorkingState(baseStateId);
                                try {
                                    WCAHistoLimits limits = new WCAHistoLimits(parameters.getHistoInterval());
                                    limits.load(network, histoDbClient);
                                    return limits;
                                } catch (InterruptedException | IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }, computationManager.getExecutor()));

                            // get preventive actions for domains task
                            List<String> preventiveStateIdsForDomains = Collections.synchronizedList(new ArrayList<>());
                            List<String> preventiveActionIdsForDomains = Collections.synchronizedList(new ArrayList<>());
                            if (baseStateLimitViolations.size() > 0) {
                                LOGGER.info("Network {}: getting preventive actions for 'domains' task", network.getId());
                                List<CompletableFuture<?>> preventiveActionTasks = Collections.synchronizedList(new ArrayList<>());
                                ;
                                for (LimitViolation baseStateLimitViolation : baseStateLimitViolations) {
                                    List<List<Action>> preventiveActions = contingencyDbFacade.getPreventiveActions(baseStateLimitViolation);
                                    if (preventiveActions.isEmpty())
                                        continue;
                                    for (int i = 0; i < preventiveActions.size(); i++) {
                                        List<Action> preventiveAction = preventiveActions.get(i);
                                        String preventiveActionId = preventiveAction.stream().map(Action::getId).collect(Collectors.joining("+"));
                                        if (preventiveStateIdsForDomains.contains(preventiveActionId))
                                            continue;
                                        String preventiveStateId = StateManager.INITIAL_STATE_ID + "_" + preventiveActionId;
                                        preventiveActionTasks.add(CompletableFuture.runAsync(() -> {
                                            LOGGER.info("Network {}, Preventive Action {}: starting analysis for {} violation on equipment {}",
                                                    network.getId(),
                                                    preventiveActionId,
                                                    baseStateLimitViolation.getLimitType(),
                                                    baseStateLimitViolation.getSubject().getId());
                                            network.getStateManager().cloneState(StateManager.INITIAL_STATE_ID, preventiveStateId);
                                            network.getStateManager().setWorkingState(preventiveStateId);
                                            for (Action subAction : preventiveAction) {
                                                subAction.toTask().modify(network);
                                            }
                                        }, computationManager.getExecutor())
                                                .thenCompose(ignored -> loadFlow.runAsync(preventiveStateId, LOAD_FLOW_PARAMETERS))
                                                .thenAccept(loadFlowResult1 -> {
                                                    if (loadFlowResult1.isOk()) {
                                                        network.getStateManager().setWorkingState(preventiveStateId);
                                                        List<LimitViolation> preventiveStateLimitViolations = CURRENT_FILTER.apply(Security.checkLimits(network));
                                                        Optional<LimitViolation> notSolvedLimitViolation = preventiveStateLimitViolations
                                                                .stream()
                                                                .filter(preventiveStateLimitViolation -> preventiveStateLimitViolation.getSubject().getId().equals(baseStateLimitViolation.getSubject().getId()))
                                                                .findAny();
                                                        if (notSolvedLimitViolation.isPresent() && parameters.stopWcaOnViolations()) {
                                                            LOGGER.warn("Network {}, Preventive Action {}: post action state still contains {} violation on equiment {}",
                                                                    network.getId(),
                                                                    preventiveActionId,
                                                                    baseStateLimitViolation.getLimitType(),
                                                                    baseStateLimitViolation.getSubject().getId());
                                                        } else {
                                                            LOGGER.info("Network {}, Preventive Action {}: adding action to list for 'domains' task", network.getId(), preventiveActionId);
                                                            preventiveStateIdsForDomains.add(preventiveStateId);
                                                            preventiveActionIdsForDomains.add(preventiveActionId);
                                                        }
                                                    } else
                                                        LOGGER.warn("Network {}, Preventive Action {}: loadflow on post action state diverged", network.getId(), preventiveActionId);
                                                })
                                                .exceptionally(throwable -> {
                                                    if (throwable != null) {
                                                        LOGGER.error(throwable.toString(), throwable);
                                                    }
                                                    return null;
                                                }));
                                    }

                                }
                                try {
                                    CompletableFuture.allOf(preventiveActionTasks.toArray(new CompletableFuture<?>[preventiveActionTasks.size()])).get();
                                } catch (Exception e) {
                                    LOGGER.error("Network {}: error getting preventive actions for 'domains' task: {}", network.getId(), e.getMessage(), e);
                                }
                                LOGGER.info("Network {}: found {} preventive actions for 'domains' task", network.getId(), preventiveActionIdsForDomains.size());
                            }

                            // check offline rules
                            for (Contingency contingency : contingencies) {
                                clusters.add(CompletableFuture
                                        .completedFuture(null)
                                        .thenComposeAsync(ignored -> {
                                            network.getStateManager().setWorkingState(baseStateId);

                                            List<String> causes = new ArrayList<>();
                                            List<SecurityRuleExpression> securityRuleExpressions = new ArrayList<>();
                                            if (parameters.getOfflineWorkflowId() != null) {
                                                Supplier<Map<HistoDbAttributeId, Object>> baseStateAttributeValues = Suppliers.memoize((Supplier<Map<HistoDbAttributeId, Object>>) () -> IIDM2DB.extractCimValues(network, new IIDM2DB.Config(null, false)).getSingleValueMap());

                                                for (RuleAttributeSet attributeSet : RuleAttributeSet.values()) {
                                                    for (SecurityIndexType securityIndexType : getSecurityIndexTypes(parameters)) {
                                                        List<SecurityRule> securityRules = rulesDbClient.getRules(parameters.getOfflineWorkflowId(), attributeSet, contingency.getId(), securityIndexType);
                                                        if (securityRules.isEmpty()) {
                                                            causes.add("Missing rule " + new RuleId(attributeSet, new SecurityIndexId(contingency.getId(), securityIndexType)));
                                                        } else {
                                                            for (SecurityRule securityRule : securityRules) {
                                                                SecurityRuleExpression expression = securityRule.toExpression(parameters.getPurityThreshold());
                                                                securityRuleExpressions.add(expression);
                                                                SecurityRuleCheckReport checkReport = expression.check(baseStateAttributeValues.get());
                                                                if (checkReport.getMissingAttributes().size() > 0) {
                                                                    causes.add("Missing attributes for rule " + securityRule.getId()
                                                                            + ": " + checkReport.getMissingAttributes().stream().map(Object::toString).collect(Collectors.joining(",")));
                                                                } else if (!checkReport.isSafe()) {
                                                                    causes.add("Rule " + securityRule.getId() + " not verified");
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if (causes.size() > 0 && parameters.stopWcaOnViolations()) {
                                                return CompletableFuture.completedFuture(new WCAClusterImpl(contingency,
                                                        WCAClusterNum.FOUR,
                                                        WCAClusterOrigin.HADES_BASE_OFFLINE_RULE,
                                                        causes));
                                            } else {
                                                return createDomainTaskWithDeps(contingency, baseStateId, securityRuleExpressions, uncertainties, histoLimits, mapper,
                                                        preventiveStateIdsForDomains, preventiveActionIdsForDomains, parameters.stopWcaOnViolations())
                                                        .thenCompose(cluster -> {
                                                            if (cluster != null && parameters.stopWcaOnViolations()) {
                                                                return CompletableFuture.completedFuture(cluster);
                                                            }
                                                            return createClusterWorkflowTask(contingency, baseStateId, contingencyDbFacade, securityRuleExpressions,
                                                                    uncertainties, histoLimits, mapper, loadFlow, parameters.stopWcaOnViolations());
                                                        });
                                            }
                                        }, computationManager.getExecutor()));
                            }
                        }
                    }

                    return clusters;
                });
    }

    @Override
    public CompletableFuture<WCAAsyncResult> runAsync(String baseStateId, WCAParameters parameters) throws Exception {
        LOGGER.info(parameters.toString());
        LOGGER.info("Starting WCA...");
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

}
