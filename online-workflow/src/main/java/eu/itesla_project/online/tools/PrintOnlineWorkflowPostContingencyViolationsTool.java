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
import eu.itesla_project.security.LimitViolation;
import eu.itesla_project.security.LimitViolationFilter;
import eu.itesla_project.security.LimitViolationType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Quinary <itesla@quinary.com>
 */
@AutoService(Tool.class)
public class PrintOnlineWorkflowPostContingencyViolationsTool implements Tool {

    private static final String TABLE_TITLE = "online-workflow-postcontingency-violations";
    private static Command COMMAND = new Command() {

        @Override
        public String getName() {
            return "print-online-workflow-postcontingency-violations";
        }

        @Override
        public String getTheme() {
            return Themes.ONLINE_WORKFLOW;
        }

        @Override
        public String getDescription() {
            return "Print post contingency violations in the network data of an online workflow";
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
            options.addOption(Option.builder().longOpt("contingency")
                    .desc("the contingency id")
                    .hasArg()
                    .argName("CONTINGENCY")
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
                new Column("Contingency"),
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
            if (line.hasOption("state") && line.hasOption("contingency")) {
                Integer stateId = Integer.parseInt(line.getOptionValue("state"));
                String contingencyId = line.getOptionValue("contingency");
                List<LimitViolation> violations = onlinedb.getPostContingencyViolations(workflowId, stateId, contingencyId);
                if (violations != null && !violations.isEmpty()) {
                    try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                        printStateContingencyViolations(formatter, stateId, contingencyId, violations, violationsFilter);
                    }
                } else {
                    System.err.println("\nNo post contingency violations for workflow " + workflowId + ", contingency " + contingencyId + " and state " + stateId);
                }
            } else if (line.hasOption("state")) {
                Integer stateId = Integer.parseInt(line.getOptionValue("state"));
                Map<String, List<LimitViolation>> stateViolationsByStateId = onlinedb.getPostContingencyViolations(workflowId, stateId);
                if (stateViolationsByStateId != null && !stateViolationsByStateId.keySet().isEmpty()) {
                    try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                        new TreeMap<>(stateViolationsByStateId).forEach((contingencyId, violations) ->
                                printStateContingencyViolations(formatter, stateId, contingencyId, violations, violationsFilter));
                    }
                } else {
                    System.err.println("\nNo post contingency violations for workflow " + workflowId + " and state " + stateId);
                }
            } else {
                if (line.hasOption("contingency")) {
                    String contingencyId = line.getOptionValue("contingency");
                    Map<Integer, List<LimitViolation>> contingencyViolationsByContingencyId = onlinedb.getPostContingencyViolations(workflowId, contingencyId);
                    if (contingencyViolationsByContingencyId != null && !contingencyViolationsByContingencyId.keySet().isEmpty()) {
                        try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                            new TreeMap<>(contingencyViolationsByContingencyId).forEach((stateId, violations) ->
                                    printStateContingencyViolations(formatter, stateId, contingencyId, violations, violationsFilter));
                        }
                    } else {
                        System.err.println("\nNo post contingency violations for workflow " + workflowId + " and contingency " + contingencyId);
                    }
                } else {
                    Map<Integer, Map<String, List<LimitViolation>>> wfViolations = onlinedb.getPostContingencyViolations(workflowId);
                    if (wfViolations != null && !wfViolations.keySet().isEmpty()) {
                        try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                            new TreeMap<>(wfViolations).forEach((stateId, stateViolations) -> {
                                if (stateViolations != null) {
                                    new TreeMap<>(stateViolations).forEach((contingencyId, violations) -> {
                                        printStateContingencyViolations(formatter, stateId, contingencyId, violations, violationsFilter);
                                    });
                                }
                            });

                        }
                    } else {
                        System.err.println("\nNo post contingency violations for workflow " + workflowId);
                    }
                }
            }
        }
    }

    private void printStateContingencyViolations(TableFormatter formatter, Integer stateId, String contingencyId, List<LimitViolation> violations,
                                                 LimitViolationFilter violationsFilter) {
        if (violations != null) {
            if (violationsFilter != null) {
                violations = violationsFilter.apply(violations);
            }
            violations
                    .stream()
                    .sorted(Comparator.comparing(o -> o.getSubjectId()))
                    .forEach(violation -> {
                        try {
                            formatter.writeCell(stateId);
                            formatter.writeCell(contingencyId);
                            formatter.writeCell(violation.getSubjectId());
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
