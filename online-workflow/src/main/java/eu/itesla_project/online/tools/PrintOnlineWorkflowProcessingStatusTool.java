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
import eu.itesla_project.modules.online.StateProcessingStatus;
import eu.itesla_project.online.OnlineTaskStatus;
import eu.itesla_project.online.OnlineTaskType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Quinary <itesla@quinary.com>
 */
@AutoService(Tool.class)
public class PrintOnlineWorkflowProcessingStatusTool implements Tool {

    private static final String TABLE_TITLE = "online-workflow-processing-status";

    private static Command COMMAND = new Command() {

        @Override
        public String getName() {
            return "print-online-workflow-processing-status";
        }

        @Override
        public String getTheme() {
            return Themes.ONLINE_WORKFLOW;
        }

        @Override
        public String getDescription() {
            return "Print processing status (failed, success) for the different states of an online workflow";
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
            String workflowId = line.getOptionValue("workflow");
            Map<Integer, ? extends StateProcessingStatus> statesProcessingStatus = onlinedb.getStatesProcessingStatus(workflowId);
            if (statesProcessingStatus != null) {

                Path outputFile = (line.hasOption("output-file")) ? Paths.get(line.getOptionValue("output-file")) : null;
                String outputFormat = (line.hasOption("output-format")) ? line.getOptionValue("output-format") : "ascii";

                TableFormatterConfig tableFormatterConfig = TableFormatterConfig.load();

                Column[] tableColumns = new Column[OnlineTaskType.values().length + 2];
                tableColumns[0] = new Column("State");
                int i = 1;
                for (OnlineTaskType taskType : OnlineTaskType.values()) {
                    tableColumns[i++] = new Column(taskTypeLabel(taskType));
                }
                tableColumns[i] = new Column("Detail");

                try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                    for (Integer stateId : statesProcessingStatus.keySet()) {
                        formatter.writeCell(stateId);
                        HashMap<String, String> stateProcessingStatus = getProcessingStatus(statesProcessingStatus.get(stateId).getStatus());
                        for (OnlineTaskType taskType : OnlineTaskType.values()) {
                            formatter.writeCell(stateProcessingStatus.get(taskType.name()));
                        }
                        formatter.writeCell(statesProcessingStatus.get(stateId).getDetail().isEmpty() ? "-" : statesProcessingStatus.get(stateId).getDetail());
                    }
                }
            } else {
                System.err.println("No status of the processing steps for this workflow: " + workflowId);
            }
        }
    }

    private HashMap<String, String> getProcessingStatus(Map<String, String> processingStatus) {
        HashMap<String, String> completeProcessingStatus = new HashMap<>();
        for (OnlineTaskType taskType : OnlineTaskType.values()) {
            if (processingStatus.containsKey(taskType.name()))
                switch (OnlineTaskStatus.valueOf(processingStatus.get(taskType.name()))) {
                    case SUCCESS:
                        completeProcessingStatus.put(taskType.name(), "OK");
                        break;
                    case FAILED:
                        completeProcessingStatus.put(taskType.name(), "FAILED");
                        break;
                    default:
                        completeProcessingStatus.put(taskType.name(), "-");
                        break;
                }
            else
                completeProcessingStatus.put(taskType.name(), "-");
        }
        return completeProcessingStatus;
    }

    private String taskTypeLabel(OnlineTaskType taskType) {
        switch (taskType) {
            case SAMPLING:
                return "Montecarlo Sampling";
            case LOAD_FLOW:
                return "Loadflow";
            case SECURITY_RULES:
                return "Security Rules";
            case OPTIMIZER:
                return "Optimizer";
            case TIME_DOMAIN_SIM:
                return "T-D Simulation";
            default:
                return "-";
        }
    }

}
