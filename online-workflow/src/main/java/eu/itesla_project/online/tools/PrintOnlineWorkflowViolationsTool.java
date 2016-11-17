/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.tools;

import com.google.auto.service.AutoService;
import eu.itesla_project.commons.io.SystemOutStreamWriter;
import eu.itesla_project.commons.io.table.*;
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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

        TableFormatterConfig tableFormatterConfig = TableFormatterConfig.load();
        Writer writer = null;
        Path csvFile = null;
        TableFormatterFactory formatterFactory = null;
        if (line.hasOption("csv")) {
            formatterFactory = new CsvTableFormatterFactory();
            csvFile = Paths.get(line.getOptionValue("csv"));
            writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8);
        } else {
            formatterFactory = new AsciiTableFormatterFactory();
            writer = new SystemOutStreamWriter();
        }
        try (OnlineDb onlinedb = config.getOnlineDbFactoryClass().newInstance().create()) {
            if (line.hasOption("state") && line.hasOption("step")) {
                Integer stateId = Integer.parseInt(line.getOptionValue("state"));
                OnlineStep step = OnlineStep.valueOf(line.getOptionValue("step"));
                List<LimitViolation> violations = onlinedb.getViolations(workflowId, stateId, step);
                if (violations != null && !violations.isEmpty()) {
                    try (TableFormatter formatter = createFormatter(formatterFactory, tableFormatterConfig, writer)) {
                        printValues(formatter, stateId, step, violations, violationsFilter);
                    }
                } else
                    System.out.println("\nNo violations for workflow " + workflowId + ", step " + step.name() + " and state " + stateId);
            } else if (line.hasOption("state")) {
                Integer stateId = Integer.parseInt(line.getOptionValue("state"));
                Map<OnlineStep, List<LimitViolation>> stateViolations = onlinedb.getViolations(workflowId, stateId);
                if (stateViolations != null && !stateViolations.keySet().isEmpty()) {
                    try (TableFormatter formatter = createFormatter(formatterFactory, tableFormatterConfig, writer)) {
                        OnlineStep[] steps = stateViolations.keySet().toArray(new OnlineStep[stateViolations.keySet().size()]);
                        Arrays.sort(steps);
                        for (OnlineStep step : steps) {
                            List<LimitViolation> violations = stateViolations.get(step);
                            if (violations != null && !violations.isEmpty()) {
                                printValues(formatter, stateId, step, violations, violationsFilter);
                            }
                        }
                    }
                } else
                    System.out.println("\nNo violations for workflow " + workflowId + " and state " + stateId);
            } else if (line.hasOption("step")) {
                OnlineStep step = OnlineStep.valueOf(line.getOptionValue("step"));
                Map<Integer, List<LimitViolation>> stepViolations = onlinedb.getViolations(workflowId, step);
                if (stepViolations != null && !stepViolations.keySet().isEmpty()) {
                    try (TableFormatter formatter = createFormatter(formatterFactory, tableFormatterConfig, writer)) {
                        Integer[] stateIds = stepViolations.keySet().toArray(new Integer[stepViolations.keySet().size()]);
                        Arrays.sort(stateIds);
                        for (Integer stateId : stateIds) {
                            List<LimitViolation> violations = stepViolations.get(stateId);
                            if (violations != null && !violations.isEmpty()) {
                                printValues(formatter, stateId, step, violations, violationsFilter);
                            }
                        }
                    }
                } else
                    System.out.println("\nNo violations for workflow " + workflowId + " and step " + step);
            } else {
                Map<Integer, Map<OnlineStep, List<LimitViolation>>> wfViolations = onlinedb.getViolations(workflowId);
                if (wfViolations != null && !wfViolations.keySet().isEmpty()) {
                    try (TableFormatter formatter = createFormatter(formatterFactory, tableFormatterConfig, writer)) {
                        Integer[] stateIds = wfViolations.keySet().toArray(new Integer[wfViolations.keySet().size()]);
                        Arrays.sort(stateIds);
                        for (Integer stateId : stateIds) {
                            Map<OnlineStep, List<LimitViolation>> stateViolations = wfViolations.get(stateId);
                            if (stateViolations != null && !stateViolations.keySet().isEmpty()) {
                                OnlineStep[] steps = stateViolations.keySet().toArray(new OnlineStep[stateViolations.keySet().size()]);
                                Arrays.sort(steps);
                                for (OnlineStep step : steps) {
                                    List<LimitViolation> violations = stateViolations.get(step);
                                    if (violations != null && !violations.isEmpty()) {
                                        printValues(formatter, stateId, step, violations, violationsFilter);
                                    }
                                }
                            }
                        }
                    }
                } else
                    System.out.println("\nNo violations for workflow " + workflowId);
            }
        }
    }

    private TableFormatter createFormatter(TableFormatterFactory formatterFactory, TableFormatterConfig config, Writer writer) throws IOException {
        TableFormatter formatter = formatterFactory.create(writer,
                TABLE_TITLE,
                config,
                new Column("State"),
                new Column("Step"),
                new Column("Equipment"),
                new Column("Type"),
                new Column("Value"),
                new Column("Limit"),
                new Column("Limit reduction"),
                new Column("Voltage Level"));
        return formatter;
    }

    private void printValues(TableFormatter formatter, Integer stateId, OnlineStep step, List<LimitViolation> violations,
                             LimitViolationFilter violationsFilter) throws IOException {
        if (violationsFilter != null)
            violations = violationsFilter.apply(violations);
        Collections.sort(violations, new Comparator<LimitViolation>() {
            public int compare(LimitViolation o1, LimitViolation o2) {
                return o1.getSubject().getId().compareTo(o2.getSubject().getId());
            }
        });
        for (LimitViolation violation : violations) {
            formatter.writeCell(stateId);
            formatter.writeCell(step.name());
            formatter.writeCell(violation.getSubject().getId());
            formatter.writeCell(violation.getLimitType().name());
            formatter.writeCell(violation.getValue());
            formatter.writeCell(violation.getLimit());
            formatter.writeCell(violation.getLimitReduction());
            formatter.writeCell(violation.getBaseVoltage());
        }
    }
}
