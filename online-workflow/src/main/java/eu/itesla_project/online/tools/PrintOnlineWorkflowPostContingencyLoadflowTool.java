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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

/**
 *
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
		TableFormatterConfig tableFormatterConfig=TableFormatterConfig.load();
		Writer writer=null;
		Path csvFile = null;
		TableFormatterFactory formatterFactory=null;
		if (line.hasOption("csv")) {
			formatterFactory=new CsvTableFormatterFactory();
			csvFile = Paths.get(line.getOptionValue("csv"));
			writer=Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8);
		} else {
			formatterFactory=new AsciiTableFormatterFactory();
			writer=new SystemOutStreamWriter();
		}
		try (OnlineDb onlinedb = config.getOnlineDbFactoryClass().newInstance().create()) {
            if (line.hasOption("state")) {
                Integer stateId = Integer.parseInt(line.getOptionValue("state"));
                Map<String, Boolean> loadflowConvergence = onlinedb.getPostContingencyLoadflowConvergence(workflowId, stateId);
                if (loadflowConvergence != null && !loadflowConvergence.keySet().isEmpty()) {
                    try (TableFormatter formatter = createFormatter(formatterFactory, tableFormatterConfig, writer)) {
                        String[] contingencyIds = loadflowConvergence.keySet().toArray(new String[loadflowConvergence.keySet().size()]);
                        Arrays.sort(contingencyIds);
                        for (String contingencyId : contingencyIds) {
                            Boolean loadflowConverge = loadflowConvergence.get(contingencyId);
                            printValues(formatter, stateId, contingencyId, loadflowConverge);
                        }
                    }
                } else
                    System.out.println("\nNo post contingency loadflow data for workflow " + workflowId + " and state " + stateId);
            } else if (line.hasOption("contingency")) {
                String contingencyId = line.getOptionValue("contingency");
                Map<Integer, Boolean> loadflowConvergence = onlinedb.getPostContingencyLoadflowConvergence(workflowId, contingencyId);
                if (loadflowConvergence != null && !loadflowConvergence.keySet().isEmpty()) {
                    try (TableFormatter formatter = createFormatter(formatterFactory, tableFormatterConfig, writer)) {
                        Integer[] stateIds = loadflowConvergence.keySet().toArray(new Integer[loadflowConvergence.keySet().size()]);
                        Arrays.sort(stateIds);
                        for (Integer stateId : stateIds) {
                            Boolean loadflowConverge = loadflowConvergence.get(stateId);
                            printValues(formatter, stateId, contingencyId, loadflowConverge);
                        }
                    }
                } else
                    System.out.println("\nNo post contingency loadflow data for workflow " + workflowId + " and contingency " + contingencyId);
            } else {
                Map<Integer, Map<String, Boolean>> loadflowConvergence = onlinedb.getPostContingencyLoadflowConvergence(workflowId);
                if (loadflowConvergence != null && !loadflowConvergence.keySet().isEmpty()) {
                    try (TableFormatter formatter = createFormatter(formatterFactory, tableFormatterConfig, writer)) {
                        Integer[] stateIds = loadflowConvergence.keySet().toArray(new Integer[loadflowConvergence.keySet().size()]);
                        Arrays.sort(stateIds);
                        for (Integer stateId : stateIds) {
                            Map<String, Boolean> stateLoadflowConvergence = loadflowConvergence.get(stateId);
                            if (stateLoadflowConvergence != null && !stateLoadflowConvergence.keySet().isEmpty()) {
                                String[] contingencyIds = stateLoadflowConvergence.keySet().toArray(new String[stateLoadflowConvergence.keySet().size()]);
                                Arrays.sort(contingencyIds);
                                for (String contingencyId : contingencyIds) {
                                    Boolean loadflowConverge = stateLoadflowConvergence.get(contingencyId);
                                    printValues(formatter, stateId, contingencyId, loadflowConverge);
                                }
                            }
                        }
                    }
                } else
                    System.out.println("\nNo post contingency loadflow data for workflow " + workflowId);
            }
        }
	}

	private TableFormatter createFormatter(TableFormatterFactory formatterFactory, TableFormatterConfig config, Writer writer) throws IOException {
		TableFormatter formatter = formatterFactory.create(writer,
				TABLE_TITLE,
				config,
				new Column("State"),
				new Column("Contingency"),
				new Column("Loadflow Convergence"));
		return formatter;
	}

	private void printValues(TableFormatter formatter, Integer stateId, String contingencyId, Boolean loadflowConverge) throws IOException {
		formatter.writeCell(stateId);
		formatter.writeCell(contingencyId);
		formatter.writeCell(loadflowConverge);
	}
}
