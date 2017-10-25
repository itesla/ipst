/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca.report;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;
import eu.itesla_project.modules.wca.report.WCAActionApplication;
import eu.itesla_project.modules.wca.report.WCALoadflowResult;
import eu.itesla_project.modules.wca.report.WCAPostContingencyStatus;
import eu.itesla_project.modules.wca.report.WCAReport;
import eu.itesla_project.modules.wca.report.WCASecurityRuleApplication;
import com.powsybl.security.LimitViolation;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAReportImpl implements WCAReport {

    private final static Logger LOGGER = LoggerFactory.getLogger(WCAReportImpl.class);
    public static String PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE = "pre-contigency-violations-without-uncertainties-report.csv";
    public static String PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE = "pre-contigency-violations-without-uncertainties";
    public static String PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE = "pre-contigency-violations-with-uncertainties-report.csv";
    public static String PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_TITLE = "pre-contigency-violations-with-uncertainties";
    public static String POST_PREVENTIVE_ACTIONS_FILE = "post-preventive-actions-report.csv";
    public static String POST_PREVENTIVE_ACTIONS_TITLE = "post-preventive-actions";
    public static String POST_PREVENTIVE_ACTIONS_VIOLATIONS_WITH_UNCERTAINTIES_FILE = "post-preventive-actions-violations-with-uncertainties-report.csv";
    public static String POST_PREVENTIVE_ACTIONS_VIOLATIONS_WITH_UNCERTAINTIES_TITLE = "post-preventive-actions-violations-with-uncertainties";
    public static String SECURITY_RULES_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE = "security-rules-violations-without-uncertainties-report.csv";
    public static String SECURITY_RULES_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE = "security-rules-violations-without-uncertainties";
    public static String POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE = "post-contigency-violations-without-uncertainties-report.csv";
    public static String POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE = "post-contigency-violations-without-uncertainties";
    public static String POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE = "post-contigency-violations-with-uncertainties-report.csv";
    public static String POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_TITLE = "post-contigency-violations-with-uncertainties";
    public static String POST_CURATIVE_ACTIONS_FILE = "post-curative-actions-report.csv";
    public static String POST_CURATIVE_ACTIONS_TITLE = "post-curative-actions";
    private static TableFormatterConfig TABLE_FORMATTER_CONFIG = TableFormatterConfig.load();
    private static String LOADFLOW_STEP = "Loadflow";

    private final String basecase;
    private WCALoadflowResult baseStateLoadflowResult;
    private List<LimitViolation> preContingencyViolationsWithoutUncertainties = Collections.synchronizedList(new ArrayList<>());
    private WCALoadflowResult baseStateWithUncertaintiesLoadflowResult;
    private List<LimitViolation> preContingencyViolationsWithUncertainties = Collections.synchronizedList(new ArrayList<>());
    private Map<String, WCAActionApplication> preventiveActionsApplication = Collections.synchronizedMap(new HashMap<String, WCAActionApplication>());
    private List<LimitViolation> postPreventiveActionsViolationsWithUncertainties = Collections.synchronizedList(new ArrayList<>());
    private List<LimitViolation> baseStateRemainingViolations = Collections.synchronizedList(new ArrayList<>());
    private List<WCASecurityRuleApplication> securityRulesApplication = Collections.synchronizedList(new ArrayList<>());
    private List<WCAPostContingencyStatus> postContingenciesStatus = Collections.synchronizedList(new ArrayList<>());

    public WCAReportImpl(String basecase) {
        this.basecase = Objects.requireNonNull(basecase);
    }

    @Override
    public String getBasecase() {
        return basecase;
    }

    @Override
    public WCALoadflowResult getBaseStateLoadflowResult() {
        return baseStateLoadflowResult;
    }

    @Override
    public List<LimitViolation> getPreContingencyViolationsWithoutUncertainties() {
        return preContingencyViolationsWithoutUncertainties;
    }

    @Override
    public WCALoadflowResult getBaseStateWithUncertaintiesLoadflowResult() {
        return baseStateWithUncertaintiesLoadflowResult;
    }

    @Override
    public List<LimitViolation> getPreContingencyViolationsWithUncertainties() {
        return preContingencyViolationsWithUncertainties;
    }

    @Override
    public List<WCAActionApplication> getPreventiveActionsApplication() {
        return new ArrayList<>(preventiveActionsApplication.values());
    }

    @Override
    public List<LimitViolation> getPostPreventiveActionsViolationsWithUncertainties() {
        return postPreventiveActionsViolationsWithUncertainties;
    }

    @Override
    public List<LimitViolation> getBaseStateRemainingViolations() {
        return baseStateRemainingViolations;
    }

    @Override
    public List<WCASecurityRuleApplication> getSecurityRulesApplication() {
        return securityRulesApplication;
    }

    @Override
    public List<WCAPostContingencyStatus> getPostContingenciesStatus() {
        return postContingenciesStatus;
    }

    @Override
    public boolean exportCsv(Path folder) {
        Objects.requireNonNull(folder);
        try {
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            } else if (!Files.isDirectory(folder)) {
                throw new RuntimeException(folder + " is a file, not a folder");
            }
            exportPreContingencyViolationsWithoutUncertainties(folder);
            exportPreContingencyViolationsWithUncertainties(folder);
            exportPreventiveActionsApplication(folder);
            exportPostPreventiveActionsViolationsWithUncertainties(folder);
            exportSecurityRulesApplication(folder);
            exportPostContingencyViolations(folder);
            exportCurativeActionsApplication(folder);
            return true;
        } catch (Throwable e) {
            LOGGER.error("Error exporting WCA report of basecase {}: {}", basecase, e.getMessage(), e);
            return false;
        }
    }

    public void setBaseStateLoadflowResult(WCALoadflowResult wcaLoadflowResult) {
        this.baseStateLoadflowResult = wcaLoadflowResult;
    }

    public void setPreContingencyViolationsWithoutUncertainties(List<LimitViolation> preContingencyViolations) {
        this.preContingencyViolationsWithoutUncertainties = Objects.requireNonNull(preContingencyViolations);
    }

    public void setBaseStateWithUncertaintiesLoadflowResult(WCALoadflowResult wcaLoadflowResult) {
        this.baseStateWithUncertaintiesLoadflowResult = wcaLoadflowResult;
    }

    public void setPreContingencyViolationsWithUncertainties(List<LimitViolation> preContingencyViolations) {
        this.preContingencyViolationsWithUncertainties = Objects.requireNonNull(preContingencyViolations);
    }

    public void addPreventiveActionApplication(WCAActionApplication actionApplication) {
        Objects.requireNonNull(actionApplication);
        preventiveActionsApplication.put(actionApplication.getActionId(), actionApplication);
    }

    public void setPreventiveActionAsApplied(String actionId) {
        Objects.requireNonNull(actionId);
        if (preventiveActionsApplication.containsKey(actionId)) {
            preventiveActionsApplication.get(actionId).setActionApplied(true);
        }
    }

    public void setPostPreventiveActionsViolationsWithUncertainties(List<LimitViolation> postPreventiveActionsViolations) {
        this.postPreventiveActionsViolationsWithUncertainties = Objects.requireNonNull(postPreventiveActionsViolations);
    }

    public void setBaseStateRemainingViolations(List<LimitViolation> baseStateRemainingViolations) {
        this.baseStateRemainingViolations = Objects.requireNonNull(baseStateRemainingViolations);
    }

    public void addSecurityRulesApplication(WCASecurityRuleApplication securityRuleApplication) {
        Objects.requireNonNull(securityRuleApplication);
        securityRulesApplication.add(securityRuleApplication);
    }

    public void addPostContingencyStatus(WCAPostContingencyStatus postContingencyStatus) {
        Objects.requireNonNull(postContingencyStatus);
        postContingenciesStatus.add(postContingencyStatus);
    }

    private void writeViolations(TableFormatter formatter, String contingencyId, WCALoadflowResult loadflowResult,
                                 List<LimitViolation> violations) {
        if (loadflowResult != null && !loadflowResult.loadflowConverged()) {
            try {
                formatter.writeCell(basecase);
                if (contingencyId != null) {
                    formatter.writeCell(contingencyId);
                }
                formatter.writeCell(LOADFLOW_STEP)
                         .writeCell(loadflowResult.getComment())
                         .writeEmptyCell()
                         .writeEmptyCell()
                         .writeEmptyCell()
                         .writeEmptyCell()
                         .writeEmptyCell()
                         .writeEmptyCell();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (!violations.isEmpty()) {
            violations.forEach(violation -> {
                try {
                    formatter.writeCell(basecase);
                    if (contingencyId != null) {
                        formatter.writeCell(contingencyId);
                    }
                    formatter.writeEmptyCell()
                             .writeEmptyCell()
                             .writeCell(violation.getLimitType().name())
                             .writeCell(violation.getSubjectId())
                             .writeCell(violation.getValue())
                             .writeCell(violation.getLimit())
                             .writeCell(violation.getCountry().name())
                             .writeCell(violation.getBaseVoltage());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void exportViolations(Path folder, String file, String title, WCALoadflowResult loadflowResult,
                                  List<LimitViolation> violations) throws IOException {
        Path violationsPath = folder.resolve(file);
        Column[] columns = {
            new Column("Basecase"),
            new Column("FailureStep"),
            new Column("FailureDescription"),
            new Column("ViolationType"),
            new Column("Equipment"),
            new Column("Value"),
            new Column("Limit"),
            new Column("Country"),
            new Column("BaseVoltage")
        };
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        try (Writer writer = Files.newBufferedWriter(violationsPath, StandardCharsets.UTF_8);
             TableFormatter formatter = factory.create(writer, title, TABLE_FORMATTER_CONFIG, columns)) {
            writeViolations(formatter, null, loadflowResult, violations);
        }
    }
    private void exportPreContingencyViolationsWithoutUncertainties(Path folder) throws IOException {
        LOGGER.info("Exporting pre-contingency violations without uncertainties report of basecase {} to file {}",
                    basecase, folder + File.separator + PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        exportViolations(folder,
                         PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE,
                         PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE,
                         baseStateLoadflowResult,
                         preContingencyViolationsWithoutUncertainties);
    }

    private void exportPreContingencyViolationsWithUncertainties(Path folder) throws IOException {
        LOGGER.info("Exporting pre-contingency violations with uncertainties report of basecase {} to file {}",
                    basecase, folder + File.separator + PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        exportViolations(folder,
                         PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE,
                         PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_TITLE,
                         baseStateWithUncertaintiesLoadflowResult,
                         preContingencyViolationsWithUncertainties);
    }

    private void writeActionsApplications(TableFormatter formatter, String contingencyId,
                                          List<WCAActionApplication> actionsApplication) {
        if (!actionsApplication.isEmpty()) {
            actionsApplication.forEach(actionApplication -> {
                try {
                    formatter.writeCell(basecase);
                    if (contingencyId != null) {
                        formatter.writeCell(contingencyId);
                    }
                    formatter.writeCell(actionApplication.getActionId());
                    if (actionApplication.getViolation() != null) {
                        formatter.writeCell(actionApplication.getViolation().getSubjectId())
                                 .writeCell(actionApplication.getViolation().getLimitType().name());
                    } else {
                        formatter.writeEmptyCell()
                                 .writeEmptyCell();
                    }
                    if (!actionApplication.getLoadflowResult().loadflowConverged()) {
                        formatter.writeCell(LOADFLOW_STEP)
                                 .writeCell(actionApplication.getLoadflowResult().getComment());
                    } else {
                        formatter.writeEmptyCell()
                                 .writeEmptyCell();
                    }
                    formatter.writeCell(actionApplication.areViolationsRemoved())
                             .writeCell(actionApplication.isActionApplied());
                    if (actionApplication.getComment() != null) {
                        formatter.writeCell(actionApplication.getComment());
                    } else {
                        formatter.writeEmptyCell();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void exportPreventiveActionsApplication(Path folder) throws IOException {
        LOGGER.info("Exporting preventive action application report of basecase {} to file {}",
                    basecase, folder + File.separator + POST_PREVENTIVE_ACTIONS_FILE);
        Path violationsPath = folder.resolve(POST_PREVENTIVE_ACTIONS_FILE);
        Column[] columns = {
            new Column("Basecase"),
            new Column("ActionId"),
            new Column("ViolatedEquipment"),
            new Column("ViolationType"),
            new Column("FailureStep"),
            new Column("FailureDescription"),
            new Column("ViolationRemoved"),
            new Column("ActionApplied"),
            new Column("Comment")
        };
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        try (Writer writer = Files.newBufferedWriter(violationsPath, StandardCharsets.UTF_8);
             TableFormatter formatter = factory.create(writer, POST_PREVENTIVE_ACTIONS_TITLE, TABLE_FORMATTER_CONFIG, columns)) {
            writeActionsApplications(formatter, null, new ArrayList<>(preventiveActionsApplication.values()));
        }
    }

    private void exportPostPreventiveActionsViolationsWithUncertainties(Path folder) throws IOException {
        LOGGER.info("Exporting post preventive actions violations without uncertainties report of basecase {} to file {}",
                    basecase, folder + File.separator + POST_PREVENTIVE_ACTIONS_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        exportViolations(folder,
                         POST_PREVENTIVE_ACTIONS_VIOLATIONS_WITH_UNCERTAINTIES_FILE,
                         POST_PREVENTIVE_ACTIONS_VIOLATIONS_WITH_UNCERTAINTIES_TITLE,
                         null,
                         postPreventiveActionsViolationsWithUncertainties);
    }

    private void exportSecurityRulesApplication(Path folder) throws IOException {
        LOGGER.info("Exporting security rules application report of basecase {} to file {}",
                    basecase, folder + File.separator + SECURITY_RULES_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        Path violationsPath = folder.resolve(SECURITY_RULES_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        Column[] columns = {
            new Column("Basecase"),
            new Column("ContingencyId"),
            new Column("SecurityRule"),
            new Column("WorkflowId"),
            new Column("RuleViolated"),
            new Column("ViolationType"),
            new Column("Cause")
        };
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        try (Writer writer = Files.newBufferedWriter(violationsPath, StandardCharsets.UTF_8);
             TableFormatter formatter = factory.create(writer, SECURITY_RULES_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE, TABLE_FORMATTER_CONFIG, columns)) {
            if (!securityRulesApplication.isEmpty()) {
                securityRulesApplication.forEach(ruleApplication -> {
                    try {
                        formatter.writeCell(basecase)
                                 .writeCell(ruleApplication.getContingencyId());
                        if (ruleApplication.getSecurityRule() != null) {
                            formatter.writeCell(ruleApplication.getSecurityRule().getId().toString())
                                     .writeCell(ruleApplication.getSecurityRule().getWorkflowId());
                        } else {
                            formatter.writeEmptyCell()
                                     .writeEmptyCell();
                        }
                        formatter.writeCell(ruleApplication.isRuleViolated())
                                 .writeCell(ruleApplication.getRuleViolationType().name());
                        if (ruleApplication.getCause() != null) {
                            formatter.writeCell(ruleApplication.getCause());
                        } else {
                            formatter.writeEmptyCell();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private void exportPostContingencyViolations(Path folder) throws IOException {
        LOGGER.info("Exporting post-contingency violations without uncertainties report of basecase {} to file {}",
                    basecase, folder + File.separator + POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        LOGGER.info("Exporting post-contingency violations with uncertainties report of basecase {} to file {}",
                    basecase, folder + File.separator + POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        Path violationsPath1 = folder.resolve(POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        Path violationsPath2 = folder.resolve(POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        Column[] columns = {
            new Column("Basecase"),
            new Column("Contingency"),
            new Column("FailureStep"),
            new Column("FailureDescription"),
            new Column("ViolationType"),
            new Column("Equipment"),
            new Column("Value"),
            new Column("Limit"),
            new Column("Country"),
            new Column("BaseVoltage")
        };
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        try (Writer writer1 = Files.newBufferedWriter(violationsPath1, StandardCharsets.UTF_8);
             TableFormatter formatter1 = factory.create(writer1, POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE, TABLE_FORMATTER_CONFIG, columns);
             Writer writer2 = Files.newBufferedWriter(violationsPath2, StandardCharsets.UTF_8);
             TableFormatter formatter2 = factory.create(writer2, POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_TITLE, TABLE_FORMATTER_CONFIG, columns)) {
            if (!postContingenciesStatus.isEmpty()) {
                postContingenciesStatus.forEach(postContingencyStatus -> {
                    // post-contingency violations without uncertainties
                    writeViolations(formatter1,
                            postContingencyStatus.getContingencyId(),
                            postContingencyStatus.getPostContingencyLoadflowResult(),
                            postContingencyStatus.getPostContingencyViolationsWithoutUncertainties());
                    // post-contingency violations with uncertainties
                    writeViolations(formatter2,
                            postContingencyStatus.getContingencyId(),
                            postContingencyStatus.getPostContingencyWithUncertaintiesLoadflowResult(),
                            postContingencyStatus.getPostContingencyViolationsWithUncertainties());
                });
            }
        }
    }

    private void exportCurativeActionsApplication(Path folder) throws IOException {
        LOGGER.info("Exporting curative action application report of basecase {} to file {}",
                    basecase, folder + File.separator + POST_CURATIVE_ACTIONS_FILE);
        Path violationsPath = folder.resolve(POST_CURATIVE_ACTIONS_FILE);
        Column[] columns = {
            new Column("Basecase"),
            new Column("Contingency"),
            new Column("ActionId"),
            new Column("ViolatedEquipment"),
            new Column("ViolationType"),
            new Column("FailureStep"),
            new Column("FailureDescription"),
            new Column("ViolationRemoved"),
            new Column("ActionApplied"),
            new Column("Comment")
        };
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        try (Writer writer = Files.newBufferedWriter(violationsPath, StandardCharsets.UTF_8);
             TableFormatter formatter = factory.create(writer, POST_CURATIVE_ACTIONS_TITLE, TABLE_FORMATTER_CONFIG, columns)) {
            if (!postContingenciesStatus.isEmpty()) {
                postContingenciesStatus.forEach(postContingencyStatus -> {
                    writeActionsApplications(formatter,
                                             postContingencyStatus.getContingencyId(),
                                             postContingencyStatus.getCurativeActionsApplication());
                });
            }
        }
    }

}
