/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.itesla_project.online.tools;

import com.google.auto.service.AutoService;
import eu.itesla_project.commons.config.ComponentDefaultConfig;
import eu.itesla_project.commons.io.table.Column;
import eu.itesla_project.commons.io.table.TableFormatter;
import eu.itesla_project.commons.io.table.TableFormatterConfig;
import eu.itesla_project.commons.tools.Command;
import eu.itesla_project.commons.tools.Tool;
import eu.itesla_project.computation.ComputationManager;
import eu.itesla_project.computation.local.LocalComputationManager;
import eu.itesla_project.iidm.import_.ImportConfig;
import eu.itesla_project.iidm.import_.Importers;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.loadflow.api.LoadFlow;
import eu.itesla_project.loadflow.api.LoadFlowFactory;
import eu.itesla_project.loadflow.api.LoadFlowResult;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
@AutoService(Tool.class)
public class RunLoadFlowTool implements Tool {

    private static final String TABLE_TITLE = "loadflow";
    private static Command COMMAND = new Command() {

        @Override
        public String getName() {
            return "run-loadflow";
        }

        @Override
        public String getTheme() {
            return "Computation";
        }

        @Override
        public String getDescription() {
            return "Run loadflow";
        }

        @Override
        public Options getOptions() {
            Options options = new Options();
            options.addOption(Option.builder().longOpt("case-file")
                    .desc("the case path")
                    .hasArg()
                    .argName("FILE")
                    .required()
                    .build());
            options.addOption(Option.builder().longOpt("skip-postproc")
                    .desc("skip network importer post processors (when configured)")
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

    private TableFormatter getFormatter(String outputFormat, Path outputFile) throws IOException {
        TableFormatterConfig tableFormatterConfig = TableFormatterConfig.load();
        Column[] tableColumns = {
                new Column("Network"),
                new Column("Result"),
                new Column("Metrics"),
        };
        return PrintOnlineWorkflowUtils.createFormatter(tableFormatterConfig, outputFormat, outputFile, TABLE_TITLE, tableColumns);

    }

    private void printTableEntry(TableFormatter formatter, Network network, LoadFlowResult result) {
        try {
            formatter.writeCell(network.getId());
            formatter.writeCell(result.isOk() ? "ok" : "nok");
            formatter.writeCell(result.getMetrics().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void run(CommandLine line) throws Exception {
        Path caseFile = Paths.get(line.getOptionValue("case-file"));
        Path outputFile = (line.hasOption("output-file")) ? Paths.get(line.getOptionValue("output-file")) : null;
        String outputFormat = (line.hasOption("output-format")) ? line.getOptionValue("output-format") : "ascii";
        boolean skipPostProc = line.hasOption("skip-postproc");
        ComponentDefaultConfig defaultConfig = ComponentDefaultConfig.load();

        try (ComputationManager computationManager = new LocalComputationManager(); TableFormatter formatter = getFormatter(outputFormat, outputFile)) {
            ImportConfig importConfig = (skipPostProc == false) ? ImportConfig.load() : new ImportConfig();
            if (Files.isRegularFile(caseFile)) {
                Network network = Importers.loadNetwork(caseFile, computationManager, importConfig, null);
                if (network == null) {
                    throw new RuntimeException("Case '" + caseFile + "' not found");
                }
                LoadFlow loadFlow = defaultConfig.newFactoryImpl(LoadFlowFactory.class).create(network, computationManager, 0);
                printTableEntry(formatter, network, loadFlow.run());
            } else if (Files.isDirectory(caseFile)) {
                Importers.loadNetworks(caseFile, false, computationManager, importConfig, network -> {
                    try {
                        LoadFlow loadFlow = defaultConfig.newFactoryImpl(LoadFlowFactory.class).create(network, computationManager, 0);
                        printTableEntry(formatter, network, loadFlow.run());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, dataSource -> System.out.println("loading case " + dataSource.getBaseName()));
            }
        }
    }
}
