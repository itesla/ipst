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
import eu.itesla_project.modules.online.*;
import eu.itesla_project.simulation.securityindexes.SecurityIndexType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Quinary <itesla@quinary.com>
 */
@AutoService(Tool.class)
public class PrintOnlineWorkflowPerformances implements Tool {

    private static final String TABLE_TITLE = "online-workflow-performances";

    private static Command COMMAND = new Command() {

        @Override
        public String getName() {
            return "print-online-workflow-performances";
        }

        @Override
        public String getTheme() {
            return Themes.ONLINE_WORKFLOW;
        }

        @Override
        public String getDescription() {
            return "Print the performances of an online workflow";
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
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        OnlineConfig config = OnlineConfig.load();
        try (OnlineDb onlinedb = config.getOnlineDbFactoryClass().newInstance().create()) {
            List<String> workflowsIds = new ArrayList<>();
            if (line.hasOption("workflow")) {
                workflowsIds.add(line.getOptionValue("workflow"));
            } else if (line.hasOption("workflows")) {
                workflowsIds = Arrays.asList(line.getOptionValue("workflows").split(","));
            } else if (line.hasOption("basecase")) {
                DateTime basecaseDate = DateTime.parse(line.getOptionValue("basecase"));
                workflowsIds = onlinedb.listWorkflows(basecaseDate).stream().map(OnlineWorkflowDetails::getWorkflowId).collect(Collectors.toList());
            } else if (line.hasOption("basecases-interval")) {
                Interval basecasesInterval = Interval.parse(line.getOptionValue("basecases-interval"));
                workflowsIds = onlinedb.listWorkflows(basecasesInterval).stream().map(OnlineWorkflowDetails::getWorkflowId).collect(Collectors.toList());
            } else {
                System.err.println("You must specify workflow(s) or basecase(s)");
                return;
            }

            Path outputFile = (line.hasOption("output-file")) ? Paths.get(line.getOptionValue("output-file")) : null;
            String outputFormat = (line.hasOption("output-format")) ? line.getOptionValue("output-format") : "ascii";

            TableFormatterConfig tableFormatterConfig = TableFormatterConfig.load();
            Column[] tableColumns = {
                new Column("workflow_id"),
                new Column("basecase"),
                new Column("secure_contingencies"),
                new Column("unsecure_contingencies"),
                new Column("unsecure_contingencies_ratio"),
                new Column("secure_contingencies_ratio"),
                new Column("unsecure_secure_contingencies_ratio"),
                new Column("wca_missed_alarms"),
                new Column("wca_missed_alarms_lst"),
                new Column("wca_false_alarms"),
                new Column("wca_false_alarms_lst"),
                new Column("wca_accuracy"),
                new Column("wca_efficiency"),
                new Column("mcla_missed_alarms"),
                new Column("mcla_missed_alarms_lst"),
                new Column("mcla_false_alarms"),
                new Column("mcla_false_alarms_lst"),
                new Column("mcla_accuracy"),
                new Column("mcla_efficiency"),
                new Column("wf_missed_alarms"),
                new Column("wf_missed_alarms_lst"),
                new Column("wf_false_alarms"),
                new Column("wf_false_alarms_lst"),
                new Column("wf_accuracy"),
                new Column("wf_efficiency")
            };

            try (TableFormatter formatter = PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns)) {
                workflowsIds.stream()
                        .sorted((o1, o2) -> (o1.compareTo(o2)))
                        .forEach(workflowId -> {
                            OnlineWorkflowParameters parameters = onlinedb.getWorkflowParameters(workflowId);
                            if ((parameters == null) || (!parameters.validation())) {
                                context.getErrorStream().println("No data for validation: skipping wf " + workflowId);
                                return;
                            }
                            OnlineWorkflowResults wfResults = onlinedb.getResults(workflowId);
                            if (wfResults == null) {
                                context.getErrorStream().println("No results: skipping wf " + workflowId);
                                return;
                            }
                            if (wfResults.getUnsafeContingencies().isEmpty()) {
                                context.getErrorStream().println("No data for benchmark: skipping wf " + workflowId);
                                return;
                            }

                            SecurityIndexType[] securityIndexTypes = (parameters.getSecurityIndexes() == null) ? SecurityIndexType.values()
                                    : parameters.getSecurityIndexes().toArray(new SecurityIndexType[0]);

                            String basecaseId = onlinedb.getState(workflowId, 0).getId();
                            OnlineWorkflowWcaResults wfWcaResults = onlinedb.getWcaResults(workflowId);
                            OnlineWorkflowRulesResults wfRulesResults = onlinedb.getRulesResults(workflowId);

                            Map<String, Boolean> contingencySecure = new HashMap<>();
                            Map<String, Boolean> contingencyWCASecure = new HashMap<>();
                            Map<String, Boolean> contingencyMCLASecure = new HashMap<>();
                            Map<String, Boolean> contingencyWfSecure = new HashMap<>();
                            Map<String, Map<SecurityIndexType, Boolean>> contingencyPhenomenaSecure = new HashMap<>();
                            Map<String, Map<SecurityIndexType, Boolean>> contingencyPhenomenaMCLASecure = new HashMap<>();
                            int unsecureContingencies = 0;
                            int secureContingencies = 0;
                            int wcaFalseAlarms = 0;
                            List<String> wcaFalseAlarmsList = new ArrayList<>();
                            List<String> wcaMissedAlarmsList = new ArrayList<>();
                            List<String> mclaFalseAlarmsList = new ArrayList<>();
                            List<String> mclaMissedAlarmsList = new ArrayList<>();
                            List<String> wfFalseAlarmsList = new ArrayList<>();
                            List<String> wfMissedAlarmsList = new ArrayList<>();

                            for (String contingencyId : wfResults.getUnsafeContingencies()) {

                                // initialize values
                                contingencySecure.put(contingencyId, true);
                                contingencyWCASecure.put(contingencyId, wfWcaResults.getClusterIndex(contingencyId) == 1);
                                contingencyMCLASecure.put(contingencyId, true);

                                Map<SecurityIndexType, Boolean> phenomenaSecure = new HashMap<>();
                                Map<SecurityIndexType, Boolean> phenomenaMCLASecure = new HashMap<>();
                                for (SecurityIndexType securityIndexType : securityIndexTypes) {
                                    phenomenaSecure.put(securityIndexType, true);
                                    phenomenaMCLASecure.put(securityIndexType, true);
                                }
                                contingencyPhenomenaSecure.put(contingencyId, phenomenaSecure);
                                contingencyPhenomenaMCLASecure.put(contingencyId, phenomenaMCLASecure);

                                // compute values
                                for (Integer stateId : wfResults.getUnstableStates(contingencyId)) {
                                    Map<String, Boolean> securityIndexes = wfResults.getIndexesData(contingencyId, stateId);
                                    Map<String, Boolean> securityRules = wfRulesResults.getStateResults(contingencyId, stateId);
                                    for (SecurityIndexType securityIndexType : securityIndexTypes) {
                                        if (securityIndexes.containsKey(securityIndexType.getLabel()) && !securityIndexes.get(securityIndexType.getLabel())) {
                                            contingencySecure.put(contingencyId, false);
                                            contingencyPhenomenaSecure.get(contingencyId).put(securityIndexType, false);
                                        }
                                        if (securityRules.containsKey(securityIndexType.getLabel()) && !securityRules.get(securityIndexType.getLabel())) {
                                            contingencyMCLASecure.put(contingencyId, false);
                                            contingencyPhenomenaMCLASecure.get(contingencyId).put(securityIndexType, false);
                                        }
                                    }
                                }
                                contingencyWfSecure.put(contingencyId, contingencyWCASecure.get(contingencyId)
                                        || (!contingencyWCASecure.get(contingencyId) && contingencyMCLASecure.get(contingencyId)));

                                // compute data for performances
                                if (contingencySecure.get(contingencyId)) {
                                    secureContingencies++;
                                } else {
                                    unsecureContingencies++;
                                }

                                if (!contingencySecure.get(contingencyId) && contingencyWCASecure.get(contingencyId)) {
                                    wcaMissedAlarmsList.add(contingencyId);
                                }
                                if (contingencySecure.get(contingencyId) && !contingencyWCASecure.get(contingencyId)) {
                                    wcaFalseAlarmsList.add(contingencyId);
                                }
                                if (!contingencySecure.get(contingencyId) && contingencyMCLASecure.get(contingencyId)) {
                                    mclaMissedAlarmsList.add(contingencyId);
                                }
                                if (contingencySecure.get(contingencyId) && !contingencyMCLASecure.get(contingencyId)) {
                                    mclaFalseAlarmsList.add(contingencyId);
                                }
                                if (!contingencySecure.get(contingencyId) && contingencyWfSecure.get(contingencyId)) {
                                    wfMissedAlarmsList.add(contingencyId);
                                }
                                if (contingencySecure.get(contingencyId) && !contingencyWfSecure.get(contingencyId)) {
                                    wfFalseAlarmsList.add(contingencyId);
                                }
                            }

                            // compute performances
                            float wcaAccuracy = (unsecureContingencies == 0) ? 100 : (1f - ((float) wcaMissedAlarmsList.size() / (float) unsecureContingencies)) * 100f;
                            float wcaEfficiency = (secureContingencies == 0) ? 100 : (1f - ((float) wcaFalseAlarms / (float) secureContingencies)) * 100f;
                            float mclaAccuracy = (unsecureContingencies == 0) ? 100 : (1f - ((float) mclaMissedAlarmsList.size() / (float) unsecureContingencies)) * 100f;
                            float mclaEfficiency = (secureContingencies == 0) ? 100 : (1f - ((float) mclaFalseAlarmsList.size() / (float) secureContingencies)) * 100f;
                            float wfAccuracy = (unsecureContingencies == 0) ? 100 : (1f - ((float) wfMissedAlarmsList.size() / (float) unsecureContingencies)) * 100f;
                            float wfEfficiency = (secureContingencies == 0) ? 100 : (1f - ((float) wfFalseAlarmsList.size() / (float) secureContingencies)) * 100f;
                            float unsecureRatio = (float) unsecureContingencies / (float) (secureContingencies + unsecureContingencies);
                            float secureRatio = (float) secureContingencies / (float) (secureContingencies + unsecureContingencies);
                            float secureUnsecureRatio = (secureContingencies == 0) ? Float.NaN : (float) unsecureContingencies / (float) secureContingencies;

                            // print performances table
                            try {
                                formatter.writeCell(workflowId);
                                formatter.writeCell(basecaseId);
                                formatter.writeCell(secureContingencies);
                                formatter.writeCell(unsecureContingencies);
                                formatter.writeCell(unsecureRatio);
                                formatter.writeCell(secureRatio);
                                formatter.writeCell(secureUnsecureRatio);
                                formatter.writeCell(wcaMissedAlarmsList.size());
                                formatter.writeCell(wcaMissedAlarmsList.toString());
                                formatter.writeCell(wcaFalseAlarmsList.size());
                                formatter.writeCell(wcaFalseAlarmsList.toString());
                                formatter.writeCell(wcaAccuracy);
                                formatter.writeCell(wcaEfficiency);
                                formatter.writeCell(mclaMissedAlarmsList.size());
                                formatter.writeCell(mclaMissedAlarmsList.toString());
                                formatter.writeCell(mclaFalseAlarmsList.size());
                                formatter.writeCell(mclaFalseAlarmsList.toString());
                                formatter.writeCell(mclaAccuracy);
                                formatter.writeCell(mclaEfficiency);
                                formatter.writeCell(wfMissedAlarmsList.size());
                                formatter.writeCell(wfMissedAlarmsList.toString());
                                formatter.writeCell(wfFalseAlarmsList.size());
                                formatter.writeCell(wfFalseAlarmsList.toString());
                                formatter.writeCell(wfAccuracy);
                                formatter.writeCell(wfEfficiency);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }
    }

}
