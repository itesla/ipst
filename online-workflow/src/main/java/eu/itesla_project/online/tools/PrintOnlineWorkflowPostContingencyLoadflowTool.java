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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Quinary <itesla@quinary.com>
 */
@AutoService(Tool.class)
public class PrintOnlineWorkflowPostContingencyLoadflowTool implements Tool {

    private static final String TABLE_TITLE = "online-workflow-postcontingency-loadflow";
    private static Command COMMAND = new Command() {

        @Override
        public String getName() {
            return "print-online-workflow-postcontingency-loadflow";
        }

        @Override
        public String getTheme() {
            return Themes.ONLINE_WORKFLOW;
        }

        @Override
        public String getDescription() {
            return "Print convergence of post contingencies loadflow of an online workflow";
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
        Column[] tableColumns = {
                new Column("State"),
                new Column("Contingency"),
                new Column("Loadflow Convergence")
        };
        String workflowId = line.getOptionValue("workflow");
        TableFormatterConfig tableFormatterConfig = TableFormatterConfig.load();
        Path outputFile = (line.hasOption("output-file")) ? Paths.get(line.getOptionValue("output-file")) : null;
        String outputFormat = (line.hasOption("output-format")) ? line.getOptionValue("output-format") : "ascii";
        try (OnlineDb onlinedb = config.getOnlineDbFactoryClass().newInstance().create()) {
            if (line.hasOption("state")) {
                Integer stateId = Integer.parseInt(line.getOptionValue("state"));
                Map<String, Boolean> loadflowConvergenceByStateId = onlinedb.getPostContingencyLoadflowConvergence(workflowId, stateId);
                if (loadflowConvergenceByStateId != null && !loadflowConvergenceByStateId.keySet().isEmpty()) {
                    try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                        new TreeMap<>(loadflowConvergenceByStateId).forEach((contingencyId, loadflowConverge) ->
                                printValues(formatter, stateId, contingencyId, loadflowConverge));
                    }
                } else {
                    context.getErrorStream().println("\nNo post contingency loadflow data for workflow " + workflowId + " and state " + stateId);
                }
            } else if (line.hasOption("contingency")) {
                String contingencyId = line.getOptionValue("contingency");
                Map<Integer, Boolean> loadflowConvergenceByContingencyId = onlinedb.getPostContingencyLoadflowConvergence(workflowId, contingencyId);
                if (loadflowConvergenceByContingencyId != null && !loadflowConvergenceByContingencyId.keySet().isEmpty()) {
                    try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                        new TreeMap<>(loadflowConvergenceByContingencyId).forEach((stateId, loadflowConverge) ->
                                printValues(formatter, stateId, contingencyId, loadflowConverge));
                    }
                } else {
                    context.getErrorStream().println("\nNo post contingency loadflow data for workflow " + workflowId + " and contingency " + contingencyId);
                }
            } else {
                Map<Integer, Map<String, Boolean>> loadflowConvergence = onlinedb.getPostContingencyLoadflowConvergence(workflowId);
                if (loadflowConvergence != null && !loadflowConvergence.keySet().isEmpty()) {
                    try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                        new TreeMap<>(loadflowConvergence).forEach((stateId, stateLoadflowConvergence) -> {
                            if (stateLoadflowConvergence != null && !stateLoadflowConvergence.keySet().isEmpty()) {
                                new TreeMap<>(stateLoadflowConvergence).forEach((contingencyId, loadflowConverge) ->
                                        printValues(formatter, stateId, contingencyId, loadflowConverge));
                            }
                        });
                    }
                } else {
                    context.getErrorStream().println("\nNo post contingency loadflow data for workflow " + workflowId);
                }
            }
        }
    }

    private void printValues(TableFormatter formatter, Integer stateId, String contingencyId, Boolean loadflowConverge) {
        try {
            formatter.writeCell(stateId);
            formatter.writeCell(contingencyId);
            formatter.writeCell(loadflowConverge);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
