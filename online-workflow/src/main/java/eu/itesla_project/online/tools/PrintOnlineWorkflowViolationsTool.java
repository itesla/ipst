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
import eu.itesla_project.modules.online.OnlineConfig;
import eu.itesla_project.modules.online.OnlineDb;
import eu.itesla_project.modules.online.OnlineStep;
import eu.itesla_project.security.LimitViolation;
import eu.itesla_project.security.LimitViolationFilter;
import eu.itesla_project.security.LimitViolationType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Quinary <itesla@quinary.com>
 */
@AutoService(Tool.class)
public class PrintOnlineWorkflowViolationsTool implements Tool {

    private static final String TABLE_TITLE = "online-workflow-violations";
    private static Command COMMAND = new Command() {

        @Override
        public String getName() {
            return "print-online-workflow-violations";
        }

        @Override
        public String getTheme() {
            return Themes.ONLINE_WORKFLOW;
        }

        @Override
        public String getDescription() {
            return "Print violations in the network data of an online workflow";
        }

        @Override
        public Options getOptions() {
            Options options = new Options();
            options.addOption(Option.builder().longOpt("workflow")
                    .desc("the workflow id")
                    .hasArg()
                    .required()
                    .argName("ID")
                    .build());
            options.addOption(Option.builder().longOpt("state")
                    .desc("the state id")
                    .hasArg()
                    .argName("STATE")
                    .build());
            options.addOption(Option.builder().longOpt("step")
                    .desc("the online step (FORECAST_ERRORS_ANALYSIS,MERGING,WORST_CASE_APPROACH,MONTE_CARLO_SAMPLING,LOAD_FLOW,SECURITY_RULES_ASSESSMENT,CONTROL_ACTION_OPTIMIZATION,TIME_DOMAIN_SIMULATION)")
                    .hasArg()
                    .argName("STEP")
                    .build());
            options.addOption(Option.builder().longOpt("type")
                    .desc("sub list of violations types (CURRENT, HIGH_VOLTAGE, LOW_VOLTAGE) to use")
                    .hasArg()
                    .argName("VIOLATION_TYPE,VIOLATION_TYPE,...")
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
        String workflowId = line.getOptionValue("workflow");
        final LimitViolationFilter violationsFilter = (line.hasOption("type")) ?
                new LimitViolationFilter(Arrays.stream(line.getOptionValue("type")
                        .split(","))
                        .map(LimitViolationType::valueOf)
                        .collect(Collectors.toSet()), 0)
                : null;
        TableFormatterConfig tableFormatterConfig = TableFormatterConfig.load();
        Column[] tableColumns = {
                new Column("State"),
                new Column("Step"),
                new Column("Equipment"),
                new Column("Type"),
                new Column("Value"),
                new Column("Limit"),
                new Column("Limit reduction"),
                new Column("Voltage Level")
        };
        Path outputFile = (line.hasOption("output-file")) ? Paths.get(line.getOptionValue("output-file")) : null;
        String outputFormat = (line.hasOption("output-format")) ? line.getOptionValue("output-format") : "ascii";
        try (OnlineDb onlinedb = config.getOnlineDbFactoryClass().newInstance().create()) {
            if (line.hasOption("state") && line.hasOption("step")) {
                Integer stateId = Integer.parseInt(line.getOptionValue("state"));
                OnlineStep step = OnlineStep.valueOf(line.getOptionValue("step"));
                List<LimitViolation> violationsByStateAndStep = onlinedb.getViolations(workflowId, stateId, step);
                if (violationsByStateAndStep != null && !violationsByStateAndStep.isEmpty()) {
                    try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                        printStateStepViolations(formatter, stateId, step, violationsByStateAndStep, violationsFilter);
                    }
                } else {
                    System.err.println("\nNo violations for workflow " + workflowId + ", step " + step.name() + " and state " + stateId);
                }
            } else if (line.hasOption("state")) {
                Integer stateId = Integer.parseInt(line.getOptionValue("state"));
                Map<OnlineStep, List<LimitViolation>> stateViolations = onlinedb.getViolations(workflowId, stateId);
                if (stateViolations != null && !stateViolations.keySet().isEmpty()) {
                    try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                        new TreeMap<>(stateViolations).forEach((onlineStep, violations) ->
                                printStateStepViolations(formatter, stateId, onlineStep, violations, violationsFilter));
                    }
                } else {
                    System.err.println("\nNo violations for workflow " + workflowId + " and state " + stateId);
                }
            } else if (line.hasOption("step")) {
                OnlineStep step = OnlineStep.valueOf(line.getOptionValue("step"));
                Map<Integer, List<LimitViolation>> stepViolations = onlinedb.getViolations(workflowId, step);
                if (stepViolations != null && !stepViolations.keySet().isEmpty()) {
                    try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                        new TreeMap<>(stepViolations).forEach((stateId, violations) ->
                                printStateStepViolations(formatter, stateId, step, violations, violationsFilter));
                    }
                } else {
                    System.err.println("\nNo violations for workflow " + workflowId + " and step " + step);
                }
            } else {
                Map<Integer, Map<OnlineStep, List<LimitViolation>>> workflowViolations = onlinedb.getViolations(workflowId);
                if (workflowViolations != null && !workflowViolations.keySet().isEmpty()) {
                    try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                        new TreeMap<>(workflowViolations).forEach((stateId, stateViolations) -> {
                            if (stateViolations != null) {
                                new TreeMap<>(stateViolations).forEach((step, violations) ->
                                        printStateStepViolations(formatter, stateId, step, violations, violationsFilter));
                            }
                        });
                    }
                } else {
                    System.err.println("\nNo violations for workflow " + workflowId);
                }
            }
        }
    }

    private void printStateStepViolations(TableFormatter formatter, Integer stateId, OnlineStep step, List<LimitViolation> violations,
                                          LimitViolationFilter violationsFilter) {
        if (violations != null) {
            if (violationsFilter != null) {
                violations = violationsFilter.apply(violations);
            }
            violations
                    .stream()
                    .sorted((o1, o2) -> o1.getSubject().getId().compareTo(o2.getSubject().getId()))
                    .forEach(violation -> {
                        try {
                            formatter.writeCell(stateId);
                            formatter.writeCell(step.name());
                            formatter.writeCell(violation.getSubject().getId());
                            formatter.writeCell(violation.getLimitType().name());
                            formatter.writeCell(violation.getValue());
                            formatter.writeCell(violation.getLimit());
                            formatter.writeCell(violation.getLimitReduction());
                            formatter.writeCell(violation.getBaseVoltage());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
