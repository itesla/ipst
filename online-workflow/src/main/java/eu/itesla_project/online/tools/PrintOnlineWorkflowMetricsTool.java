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
import eu.itesla_project.commons.tools.ToolRunningContext;
import eu.itesla_project.modules.online.OnlineConfig;
import eu.itesla_project.modules.online.OnlineDb;
import eu.itesla_project.modules.online.OnlineStep;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Quinary <itesla@quinary.com>
 *
 */
@AutoService(Tool.class)
public class PrintOnlineWorkflowMetricsTool implements Tool {

    private static final String TABLE_TITLE = "online-workflow-metrics";

    private static Command COMMAND = new Command() {

        @Override
        public String getName() {
            return "print-online-workflow-metrics";
        }

        @Override
        public String getTheme() {
            return Themes.ONLINE_WORKFLOW;
        }

        @Override
        public String getDescription() {
            return "Print stored metrics of a step of an online workflow";
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
            options.addOption(Option.builder().longOpt("step")
                    .desc("the online step (FORECAST_ERRORS_ANALYSIS,MERGING,WORST_CASE_APPROACH,MONTE_CARLO_SAMPLING,LOAD_FLOW,SECURITY_RULES_ASSESSMENT,CONTROL_ACTION_OPTIMIZATION,TIME_DOMAIN_SIMULATION,STABILIZATION,IMPACT_ANALYSIS)")
                    .hasArg()
                    .required()
                    .argName("STEP")
                    .build());
            options.addOption(Option.builder().longOpt("state")
                    .desc("the state id (if not specified all the metrics of all the states are printed, in CSV format)")
                    .hasArg()
                    .argName("STATE")
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
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        OnlineConfig config = OnlineConfig.load();
        try (OnlineDb onlinedb = config.getOnlineDbFactoryClass().newInstance().create()) {
            String workflowId = line.getOptionValue("workflow");
            OnlineStep step = OnlineStep.valueOf(line.getOptionValue("step"));

            Path outputFile = (line.hasOption("output-file")) ? Paths.get(line.getOptionValue("output-file")) : null;
            String outputFormat = (line.hasOption("output-format")) ? line.getOptionValue("output-format") : "ascii";

            TableFormatterConfig tableFormatterConfig = TableFormatterConfig.load();

            if (line.hasOption("state")) {
                Integer stateId = Integer.parseInt(line.getOptionValue("state"));
                Map<String, String> metrics = onlinedb.getMetrics(workflowId, stateId, step);
                if ((metrics == null) || (metrics.keySet().isEmpty())) {
                    context.getErrorStream().println("\nNo metrics for workflow " + workflowId + ", step " + step.name() + " and state " + stateId);
                } else {
                    String[] headers = new String[metrics.keySet().size() + 1];
                    headers[0] = "state";
                    int i = 1;
                    for (String parameter : metrics.keySet()) {
                        headers[i] = parameter;
                        i++;
                    }
                    List<Column> columns = Arrays.stream(headers)
                            .map(Column::new)
                            .collect(Collectors.toList());
                    try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile,
                            TABLE_TITLE, columns.toArray(new Column[0]))) {
                        formatter.writeCell(stateId);
                        columns.stream().skip(1).forEach(x -> {
                            try {
                                formatter.writeCell(metrics.get(x.getName()));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
            } else {
                List<String[]> metricsTableList = onlinedb.getAllMetrics(workflowId, step);
                if (metricsTableList.isEmpty()) {
                    context.getErrorStream().println("\nNo metrics for workflow " + workflowId + " and step " + step.name());
                } else {
                    //first row is metrics' header
                    List<Column> columns = Arrays.stream(metricsTableList.get(0))
                            .map(Column::new)
                            .collect(Collectors.toList());
                    try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile,
                            TABLE_TITLE, columns.toArray(new Column[0]))) {
                        metricsTableList.stream().skip(1).forEach(row -> Arrays.stream(row).forEach(colValue -> {
                            try {
                                formatter.writeCell(colValue);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));
                    }
                }
            }
        }
    }


}
