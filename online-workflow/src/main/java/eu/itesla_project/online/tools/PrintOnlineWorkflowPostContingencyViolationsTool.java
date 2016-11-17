/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.tools;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import eu.itesla_project.commons.io.SystemOutStreamWriter;
import eu.itesla_project.commons.io.table.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import com.google.auto.service.AutoService;

import eu.itesla_project.commons.tools.Command;
import eu.itesla_project.commons.tools.Tool;
import eu.itesla_project.modules.online.OnlineConfig;
import eu.itesla_project.modules.online.OnlineDb;
import eu.itesla_project.security.LimitViolation;
import eu.itesla_project.security.LimitViolationFilter;
import eu.itesla_project.security.LimitViolationType;

/**
 *
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
            options.addOption(Option.builder().longOpt("csv")
                    .desc("export in csv format to a file")
                    .hasArg()
                    .argName("FILE")
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
        LimitViolationFilter violationsFilter = null;
        if (line.hasOption("type")) {
            Set<LimitViolationType> limitViolationTypes = Arrays.stream(line.getOptionValue("type").split(","))
                    .map(LimitViolationType::valueOf)
                    .collect(Collectors.toSet());
            violationsFilter = new LimitViolationFilter(limitViolationTypes, 0);
        }
        TableFormatterConfig tableFormatterConfig=TableFormatterConfig.load();
        Writer writer=null;
        Path csvFile = null;
        TableFormatterFactory formatterFactory=null;
        try (OnlineDb onlinedb = config.getOnlineDbFactoryClass().newInstance().create()) {
            if (line.hasOption("csv")) {
                formatterFactory = new CsvTableFormatterFactory();
                csvFile = Paths.get(line.getOptionValue("csv"));
                writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8);
            } else {
                formatterFactory = new AsciiTableFormatterFactory();
                writer = new SystemOutStreamWriter();
            }
            if (line.hasOption("state") && line.hasOption("contingency")) {
                Integer stateId = Integer.parseInt(line.getOptionValue("state"));
                String contingencyId = line.getOptionValue("contingency");
                List<LimitViolation> violations = onlinedb.getPostContingencyViolations(workflowId, stateId, contingencyId);
                if (violations != null && !violations.isEmpty()) {
                    try (TableFormatter formatter = createFormatter(formatterFactory, tableFormatterConfig, writer)) {
                        printValues(formatter, stateId, contingencyId, violations, violationsFilter);
                    }
                } else
                    System.out.println("\nNo post contingency violations for workflow " + workflowId + ", contingency " + contingencyId + " and state " + stateId);
            } else if (line.hasOption("state")) {
                Integer stateId = Integer.parseInt(line.getOptionValue("state"));
                Map<String, List<LimitViolation>> stateViolations = onlinedb.getPostContingencyViolations(workflowId, stateId);
                if (stateViolations != null && !stateViolations.keySet().isEmpty()) {
                    try (TableFormatter formatter = createFormatter(formatterFactory, tableFormatterConfig, writer)) {
                        String[] contingencyIds = stateViolations.keySet().toArray(new String[stateViolations.keySet().size()]);
                        Arrays.sort(contingencyIds);
                        for (String contingencyId : contingencyIds) {
                            List<LimitViolation> violations = stateViolations.get(contingencyId);
                            if (violations != null && !violations.isEmpty()) {
                                printValues(formatter, stateId, contingencyId, violations, violationsFilter);
                            }
                        }
                    }
                } else
                    System.out.println("\nNo post contingency violations for workflow " + workflowId + " and state " + stateId);
            } else if (line.hasOption("contingency")) {
                String contingencyId = line.getOptionValue("contingency");
                Map<Integer, List<LimitViolation>> contingencyViolations = onlinedb.getPostContingencyViolations(workflowId, contingencyId);
                if (contingencyViolations != null && !contingencyViolations.keySet().isEmpty()) {
                    try (TableFormatter formatter = createFormatter(formatterFactory, tableFormatterConfig, writer)) {
                        Integer[] stateIds = contingencyViolations.keySet().toArray(new Integer[contingencyViolations.keySet().size()]);
                        Arrays.sort(stateIds);
                        for (Integer stateId : stateIds) {
                            List<LimitViolation> violations = contingencyViolations.get(stateId);
                            if (violations != null && !violations.isEmpty()) {
                                printValues(formatter, stateId, contingencyId, violations, violationsFilter);
                            }
                        }
                    }
                } else
                    System.out.println("\nNo post contingency violations for workflow " + workflowId + " and contingency " + contingencyId);
            } else {
                Map<Integer, Map<String, List<LimitViolation>>> wfViolations = onlinedb.getPostContingencyViolations(workflowId);
                if (wfViolations != null && !wfViolations.keySet().isEmpty()) {
                    try (TableFormatter formatter = createFormatter(formatterFactory, tableFormatterConfig, writer)) {
                        Integer[] stateIds = wfViolations.keySet().toArray(new Integer[wfViolations.keySet().size()]);
                        Arrays.sort(stateIds);
                        for (Integer stateId : stateIds) {
                            Map<String, List<LimitViolation>> stateViolations = wfViolations.get(stateId);
                            if (stateViolations != null && !stateViolations.keySet().isEmpty()) {
                                String[] contingencyIds = stateViolations.keySet().toArray(new String[stateViolations.keySet().size()]);
                                Arrays.sort(contingencyIds);
                                for (String contingencyId : contingencyIds) {
                                    List<LimitViolation> violations = stateViolations.get(contingencyId);
                                    if (violations != null && !violations.isEmpty()) {
                                        printValues(formatter, stateId, contingencyId, violations, violationsFilter);
                                    }
                                }
                            }
                        }
                    }
                } else
                    System.out.println("\nNo post contingency violations for workflow " + workflowId);
            }
        }
    }

    private TableFormatter createFormatter(TableFormatterFactory formatterFactory, TableFormatterConfig config, Writer writer) throws IOException {
        TableFormatter formatter = formatterFactory.create(writer,
                TABLE_TITLE,
                config,
                new Column("State"),
                new Column("Contingency"),
                new Column("Equipment"),
                new Column("Type"),
                new Column("Value"),
                new Column("Limit"),
                new Column("Limit reduction"),
                new Column("Voltage Level"));
        return formatter;
    }


    private void printValues(TableFormatter formatter, Integer stateId, String contingencyId, List<LimitViolation> violations,
            LimitViolationFilter violationsFilter) throws IOException {
        if ( violationsFilter != null )
            violations = violationsFilter.apply(violations);
        Collections.sort(violations, new Comparator<LimitViolation>() {
            public int compare(LimitViolation o1, LimitViolation o2) {
                return o1.getSubject().getId().compareTo(o2.getSubject().getId());
            }
        });
        for (LimitViolation violation : violations) {
            formatter.writeCell(stateId);
            formatter.writeCell(contingencyId);
            formatter.writeCell(violation.getSubject().getId());
            formatter.writeCell(violation.getLimitType().name());
            formatter.writeCell(violation.getValue());
            formatter.writeCell(violation.getLimit());
            formatter.writeCell(violation.getLimitReduction());
            formatter.writeCell(violation.getBaseVoltage());
        }
    }
}
