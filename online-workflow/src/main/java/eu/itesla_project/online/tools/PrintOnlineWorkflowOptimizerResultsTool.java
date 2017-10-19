/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.tools;

import com.csvreader.CsvWriter;
import com.google.auto.service.AutoService;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import eu.itesla_project.modules.online.OnlineConfig;
import eu.itesla_project.modules.online.OnlineDb;
import eu.itesla_project.modules.online.OnlineWorkflowResults;
import eu.itesla_project.online.Utils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.nocrala.tools.texttablefmt.BorderStyle;
import org.nocrala.tools.texttablefmt.CellStyle;
import org.nocrala.tools.texttablefmt.Table;

import java.io.StringWriter;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
@AutoService(Tool.class)
public class PrintOnlineWorkflowOptimizerResultsTool implements Tool {

    private static Command COMMAND = new Command() {

        @Override
        public String getName() {
            return "print-online-workflow-optimizer-results";
        }

        @Override
        public String getTheme() {
            return Themes.ONLINE_WORKFLOW;
        }

        @Override
        public String getDescription() {
            return "Print stored results of corrective control optimizer for an online workflow";
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
            options.addOption(Option.builder().longOpt("csv")
                    .desc("export in csv format")
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
        OnlineDb onlinedb = config.getOnlineDbFactoryClass().newInstance().create();
        String workflowId = line.getOptionValue("workflow");
        OnlineWorkflowResults wfResults = onlinedb.getResults(workflowId);
        if (wfResults != null) {
            if (!wfResults.getContingenciesWithActions().isEmpty()) {
                Table table = new Table(5, BorderStyle.CLASSIC_WIDE);
                StringWriter content = new StringWriter();
                CsvWriter cvsWriter = new CsvWriter(content, ',');
                String[] headers = new String[5];
                int i = 0;
                table.addCell("Contingency", new CellStyle(CellStyle.HorizontalAlign.center));
                headers[i++] = "Contingency";
                table.addCell("State", new CellStyle(CellStyle.HorizontalAlign.center));
                headers[i++] = "State";
                table.addCell("Actions Found", new CellStyle(CellStyle.HorizontalAlign.center));
                headers[i++] = "Actions Found";
                table.addCell("Status", new CellStyle(CellStyle.HorizontalAlign.center));
                headers[i++] = "Status";
                table.addCell("Actions", new CellStyle(CellStyle.HorizontalAlign.center));
                headers[i++] = "Actions";
                cvsWriter.writeRecord(headers);
                for (String contingencyId : wfResults.getContingenciesWithActions()) {
                    for (Integer stateId : wfResults.getUnsafeStatesWithActions(contingencyId).keySet()) {
                        String[] values = new String[5];
                        i = 0;
                        table.addCell(contingencyId);
                        values[i++] = contingencyId;
                        table.addCell(stateId.toString(), new CellStyle(CellStyle.HorizontalAlign.right));
                        values[i++] = stateId.toString();
                        table.addCell(Boolean.toString(wfResults.getUnsafeStatesWithActions(contingencyId).get(stateId)), new CellStyle(CellStyle.HorizontalAlign.right));
                        values[i++] = Boolean.toString(wfResults.getUnsafeStatesWithActions(contingencyId).get(stateId));
                        table.addCell(wfResults.getStateStatus(contingencyId, stateId).name());
                        values[i++] = wfResults.getStateStatus(contingencyId, stateId).name();
                        String json = "-";
                        if (wfResults.getActionsIds(contingencyId, stateId) != null) {
//                            json = Utils.actionsToJson(wfResults, contingencyId, stateId);
                            json = Utils.actionsToJsonExtended(wfResults, contingencyId, stateId);
                        }
                        table.addCell(json);
                        values[i++] = json;
                        cvsWriter.writeRecord(values);
                    }
                }
                cvsWriter.flush();
                if (line.hasOption("csv")) {
                    context.getOutputStream().println(content.toString());
                } else {
                    context.getOutputStream().println(table.render());
                }
                cvsWriter.close();
            } else {
                context.getOutputStream().println("\nNo contingencies requiring corrective actions");
            }
        } else {
            context.getOutputStream().println("No results for this workflow");
        }
        onlinedb.close();
    }

}
