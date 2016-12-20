/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.tools;

import com.google.auto.service.AutoService;
import eu.itesla_project.commons.io.table.Column;
import eu.itesla_project.commons.io.table.TableFormatter;
import eu.itesla_project.commons.io.table.TableFormatterConfig;
import eu.itesla_project.commons.tools.Command;
import eu.itesla_project.commons.tools.Tool;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.modules.online.*;
import eu.itesla_project.online.OnlineTaskStatus;
import eu.itesla_project.security.LimitViolation;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Quinary <itesla@quinary.com>
 */
@AutoService(Tool.class)
public class PrintOnlineWorkflowSummaryTable implements Tool {

    private static final String EMPTY_CONTINGENCY_ID = "Empty-Contingency";
    private static final String TABLE_TITLE = "online-workflow-summary";

    private static Command COMMAND = new Command() {

        @Override
        public String getName() {
            return "print-online-workflow-summary";
        }

        @Override
        public String getTheme() {
            return Themes.ONLINE_WORKFLOW;
        }

        @Override
        public String getDescription() {
            return "Print a summary table containing the data of an online workflow";
        }

        @Override
        public Options getOptions() {
            Options options = new Options();
            options.addOption(Option.builder().longOpt("workflow")
                    .desc("the workflow id")
                    .hasArg()
                    .argName("ID")
                    .build());
            options.addOption(Option.builder().longOpt("workflows")
                    .desc("the workflow ids, separated by ,")
                    .hasArg()
                    .argName("IDS")
                    .build());
            options.addOption(Option.builder().longOpt("basecase")
                    .desc("the basecase")
                    .hasArg()
                    .argName("BASECASE")
                    .build());
            options.addOption(Option.builder().longOpt("basecases-interval")
                    .desc("the base cases interval")
                    .hasArg()
                    .argName("BASECASE")
                    .build());
            options.addOption(Option.builder().longOpt("output-file")
                    .desc("export to a file")
                    .hasArg()
                    .argName("FILE")
                    .build());
            options.addOption(Option.builder().longOpt("output-format")
                    .desc("output formats available: " + PrintOnlineWorkflowUtils.availableTableFormatterFormats() + " (default is ascii)")
                    .hasArg()
                    .argName("FORMAT")
                    .build());
            return options;
        }

        @Override
        public String getUsageFooter() {
            return null;
        }

    };

    @Override
    public Command getCommand() {
        return COMMAND;
    }

    @Override
    public void run(CommandLine line) throws Exception {
        OnlineConfig config = OnlineConfig.load();
        try (OnlineDb onlinedb = config.getOnlineDbFactoryClass().newInstance().create()) {
            List<String> workflowsIds = new ArrayList<String>();
            if (line.hasOption("workflow"))
                workflowsIds.add(line.getOptionValue("workflow"));
            else if (line.hasOption("workflows"))
                workflowsIds = Arrays.asList(line.getOptionValue("workflows").split(","));
            else if (line.hasOption("basecase")) {
                DateTime basecaseDate = DateTime.parse(line.getOptionValue("basecase"));
                workflowsIds = onlinedb.listWorkflows(basecaseDate).stream().map(OnlineWorkflowDetails::getWorkflowId).collect(Collectors.toList());
            } else if (line.hasOption("basecases-interval")) {
                Interval basecasesInterval = Interval.parse(line.getOptionValue("basecases-interval"));
                workflowsIds = onlinedb.listWorkflows(basecasesInterval).stream().map(OnlineWorkflowDetails::getWorkflowId).collect(Collectors.toList());
            } else {
                System.err.println("You must specify workflow(s) or basecase(s)");
                return;
            }
            TableFormatterConfig tableFormatterConfig = TableFormatterConfig.load();
            try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig,
                    (line.hasOption("output-format")) ? line.getOptionValue("output-format") : "ascii",
                    (line.hasOption("output-file")) ? Paths.get(line.getOptionValue("output-file")) : null,
                    TABLE_TITLE,
                    new Column("WorkflowId"),
                    new Column("Basecase"),
                    new Column("Contingency"),
                    new Column("State"),
                    new Column("FailureStep"),
                    new Column("FailureDescription"),
                    new Column("ViolationType"),
                    new Column("Violation"),
                    new Column("ViolationStep"),
                    new Column("Equipment"),
                    new Column("Value"),
                    new Column("Limit"))) {

                workflowsIds.sort((o1, o2) -> (o1.compareTo(o2)));
                workflowsIds.forEach(workflowId -> {
                    Network basecase = onlinedb.getState(workflowId, 0);
                    String basecaseId = basecase.getId();
                    printPrecontingencyViolations(workflowId, basecaseId, onlinedb, formatter);
                    printContingenciesViolations(workflowId, basecaseId, onlinedb, formatter);
                });
            }
        }
    }

    private void printPrecontingencyViolations(String workflowId, String basecaseId, OnlineDb onlinedb, TableFormatter formatter) {
        Map<Integer, Map<OnlineStep, List<LimitViolation>>> wfViolations = onlinedb.getViolations(workflowId);
        Map<Integer, ? extends StateProcessingStatus> statesProcessingStatus = onlinedb.getStatesProcessingStatus(workflowId);
        if (wfViolations != null && !wfViolations.keySet().isEmpty()) {
            new TreeMap<>(statesProcessingStatus).forEach((stateId, stateprocessingStatus) -> {
                if (stateprocessingStatus != null && stateprocessingStatus.getStatus() != null
                        && !stateprocessingStatus.getStatus().isEmpty()) {
                    for (String step : stateprocessingStatus.getStatus().keySet()) {
                        if (OnlineTaskStatus.valueOf(stateprocessingStatus.getStatus().get(step)) == OnlineTaskStatus.FAILED) {
                            try {
                                formatter.writeCell(workflowId);
                                formatter.writeCell(basecaseId);
                                formatter.writeCell(EMPTY_CONTINGENCY_ID);
                                formatter.writeCell(stateId);
                                formatter.writeCell(step);
                                formatter.writeCell(stateprocessingStatus.getDetail());
                                formatter.writeEmptyCell();
                                formatter.writeEmptyCell();
                                formatter.writeEmptyCell();
                                formatter.writeEmptyCell();
                                formatter.writeEmptyCell();
                                formatter.writeEmptyCell();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        }
                    }
                }
                Map<OnlineStep, List<LimitViolation>> stateViolations = wfViolations.get(stateId);
                if (stateViolations != null && !stateViolations.keySet().isEmpty()) {
                    stateViolations.entrySet().stream()
                            .sorted(Comparator.comparing(Map.Entry::getKey))
                            .forEach(entry -> printViolations(workflowId, basecaseId, EMPTY_CONTINGENCY_ID, stateId, entry.getKey(), entry.getValue(), formatter));
                }
            });
        }
    }

    private void printViolations(String workflowId, String basecaseId, String contingencyId, Integer stateId, OnlineStep step,
                                 List<LimitViolation> violations, TableFormatter formatter) {
        if (violations != null && !violations.isEmpty()) {
            violations.stream().sorted(Comparator.comparing(LimitViolation::getLimitType)).forEach(violation -> {
                if (violation != null) {
                    try {
                        formatter.writeCell(workflowId);
                        formatter.writeCell(basecaseId);
                        formatter.writeCell(contingencyId);
                        formatter.writeCell(stateId);
                        formatter.writeEmptyCell();
                        formatter.writeEmptyCell();
                        formatter.writeCell(ViolationType.STEADY_STATE.name());
                        formatter.writeCell(violation.getLimitType().name());
                        formatter.writeCell(step.name());
                        formatter.writeCell(violation.getSubject().getId());
                        formatter.writeCell(violation.getValue());
                        formatter.writeCell(violation.getLimit());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    private void printContingenciesViolations(String workflowId, String basecaseId, OnlineDb onlinedb, TableFormatter formatter) {
        int states = onlinedb.getWorkflowParameters(workflowId).getStates();
        OnlineWorkflowRulesResults wfWcaRulesResults = onlinedb.getWcaRulesResults(workflowId);
        OnlineWorkflowRulesResults wfMclaRulesResults = onlinedb.getRulesResults(workflowId);
        OnlineWorkflowResults wfResults = onlinedb.getResults(workflowId);
        Map<Integer, Map<String, List<LimitViolation>>> wfViolations = onlinedb.getPostContingencyViolations(workflowId);
        Map<Integer, Map<String, Boolean>> loadflowConvergence = onlinedb.getPostContingencyLoadflowConvergence(workflowId);
        List<String> contingencies = new ArrayList<>();
        OnlineWorkflowWcaResults wcaResults = onlinedb.getWcaResults(workflowId);
        if (wcaResults != null && wcaResults.getContingencies() != null)
            contingencies.addAll(wcaResults.getContingencies());
        contingencies.stream().sorted((x, y) -> x.compareTo(y)).forEach(contingency -> {
            for (int stateId = 0; stateId < states; stateId++) {
                printFailures(loadflowConvergence, workflowId, basecaseId, contingency, stateId, formatter);
                printRulesResults(wfWcaRulesResults, workflowId, basecaseId, contingency, stateId, ViolationType.WCA_RULE, formatter);
                printRulesResults(wfMclaRulesResults, workflowId, basecaseId, contingency, stateId, ViolationType.MCLA_RULE, formatter);
                printSimulationResults(wfResults, workflowId, basecaseId, contingency, stateId, ViolationType.SECURITY_INDEX, formatter);
                printPostcontingencyViolations(workflowId, basecaseId, contingency, stateId, wfViolations, formatter);
            }
        });
    }

    private void printFailures(Map<Integer, Map<String, Boolean>> loadflowConvergence, String workflowId, String basecaseId,
                               String contingencyId, Integer stateId, TableFormatter formatter) {
        if (loadflowConvergence != null
                && !loadflowConvergence.isEmpty()
                && loadflowConvergence.containsKey(stateId)
                && !loadflowConvergence.get(stateId).isEmpty()
                && loadflowConvergence.get(stateId).containsKey(contingencyId)) {
            if (!loadflowConvergence.get(stateId).get(contingencyId)) {
                try {
                    formatter.writeCell(workflowId);
                    formatter.writeCell(basecaseId);
                    formatter.writeCell(contingencyId);
                    formatter.writeCell(stateId);
                    formatter.writeCell(OnlineStep.POSTCONTINGENCY_LOAD_FLOW.name());
                    formatter.writeCell("Post contingency load flow does not converge");
                    formatter.writeEmptyCell();
                    formatter.writeEmptyCell();
                    formatter.writeEmptyCell();
                    formatter.writeEmptyCell();
                    formatter.writeEmptyCell();
                    formatter.writeEmptyCell();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void printRulesResults(OnlineWorkflowRulesResults wfMclaRulesResults, String workflowId, String basecaseId, String contingencyId,
                                   Integer stateId, ViolationType violationType, TableFormatter formatter) {
        if (wfMclaRulesResults != null
                && wfMclaRulesResults.getContingenciesWithSecurityRulesResults() != null
                && !wfMclaRulesResults.getContingenciesWithSecurityRulesResults().isEmpty()
                && wfMclaRulesResults.getContingenciesWithSecurityRulesResults().contains(contingencyId)
                && wfMclaRulesResults.getStateResults(contingencyId, stateId) != null
                && !wfMclaRulesResults.getStateResults(contingencyId, stateId).isEmpty()) {
            for (String index : wfMclaRulesResults.getStateResults(contingencyId, stateId).keySet()) {
                if (!wfMclaRulesResults.getStateResults(contingencyId, stateId).get(index)) {
                    try {
                        formatter.writeCell(workflowId);
                        formatter.writeCell(basecaseId);
                        formatter.writeCell(contingencyId);
                        formatter.writeCell(stateId);
                        formatter.writeEmptyCell();
                        formatter.writeEmptyCell();
                        formatter.writeCell(violationType.name());
                        formatter.writeCell(index);
                        formatter.writeCell(OnlineStep.MONTE_CARLO_LIKE_APPROACH.name());
                        formatter.writeEmptyCell();
                        formatter.writeEmptyCell();
                        formatter.writeEmptyCell();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }
    }

    private void printSimulationResults(OnlineWorkflowResults wfResults, String workflowId, String basecaseId, String contingencyId,
                                        Integer stateId, ViolationType violationType, TableFormatter formatter) {
        if (wfResults != null
                && wfResults.getUnsafeContingencies() != null
                && !wfResults.getUnsafeContingencies().isEmpty()
                && wfResults.getUnsafeContingencies().contains(contingencyId)
                && wfResults.getIndexesData(contingencyId, stateId) != null
                && !wfResults.getIndexesData(contingencyId, stateId).isEmpty()) {
            for (String index : wfResults.getIndexesData(contingencyId, stateId).keySet()) {
                if (!wfResults.getIndexesData(contingencyId, stateId).get(index)) {
                    try {
                        formatter.writeCell(workflowId);
                        formatter.writeCell(basecaseId);
                        formatter.writeCell(contingencyId);
                        formatter.writeCell(stateId);
                        formatter.writeEmptyCell();
                        formatter.writeEmptyCell();
                        formatter.writeCell(violationType.name());
                        formatter.writeCell(index);
                        formatter.writeCell(OnlineStep.TIME_DOMAIN_SIMULATION.name());
                        formatter.writeEmptyCell();
                        formatter.writeEmptyCell();
                        formatter.writeEmptyCell();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void printPostcontingencyViolations(String workflowId, String basecaseId, String contingencyId, Integer stateId,
                                                Map<Integer, Map<String, List<LimitViolation>>> wfViolations, TableFormatter formatter) {
        if (wfViolations != null
                && !wfViolations.isEmpty()
                && wfViolations.containsKey(stateId)
                && wfViolations.get(stateId) != null
                && !wfViolations.get(stateId).isEmpty()
                && wfViolations.get(stateId).containsKey(contingencyId)) {
            List<LimitViolation> violations = wfViolations.get(stateId).get(contingencyId);
            printViolations(workflowId, basecaseId, contingencyId, stateId, OnlineStep.POSTCONTINGENCY_LOAD_FLOW, violations, formatter);
        }
    }
}

enum ViolationType {
    STEADY_STATE,
    WCA_RULE,
    MCLA_RULE,
    SECURITY_INDEX
}
