/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.joda.time.Interval;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;

import eu.itesla_project.mcla.NetworkUtils;
import eu.itesla_project.mcla.forecast_errors.FEAHistoDBFacade;
import eu.itesla_project.mcla.forecast_errors.FEAMatFileWriter;
import eu.itesla_project.mcla.forecast_errors.HistoricalDataCreator;
import eu.itesla_project.mcla.forecast_errors.data.ForecastErrorsHistoricalData;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.online.OnlineConfig;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
@AutoService(Tool.class)
public class CreateFeaMat implements Tool {

    private static Command COMMAND = new Command() {

        @Override
        public String getName() {
            return "create-fea-mat";
        }

        @Override
        public String getTheme() {
            return Themes.MCLA;
        }

        @Override
        public String getDescription() {
            return "Create FEA MAT file";
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
            options.addOption(Option.builder().longOpt("histo-interval")
                    .desc("interval of historical data")
                    .hasArg()
                    .argName("INTERVAL")
                    .required()
                    .build());
            options.addOption(Option.builder().longOpt("output-folder")
                    .desc("the folder where to store the data")
                    .hasArg()
                    .argName("FOLDER")
                    .required()
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
        Path caseFile = Paths.get(line.getOptionValue("case-file"));
        Path outputFolder = Paths.get(line.getOptionValue("output-folder"));
        Interval histoInterval = Interval.parse(line.getOptionValue("histo-interval"));

        if (Files.isRegularFile(caseFile)) {
            System.out.println("loading case " + caseFile);
            // load the network
            Network network = Importers.loadNetwork(caseFile);
            if (network == null) {
                throw new RuntimeException("Case '" + caseFile + "' not found");
            }
            network.getStateManager().allowStateMultiThreadAccess(true);
            createMat(network, histoInterval, outputFolder);
        } else {
            throw new RuntimeException("Case '" + caseFile + "' is not a valid basecase file");
        }
    }

    private void createMat(Network network, Interval histoInterval, Path outputFolder) throws Exception {
        System.out.println("creating mat file for network " + network.getId() + " and historical interval " + histoInterval);
        String histoFileName = "histoData_" + network.getId() + "_" + histoInterval.toString().replaceAll("/", "_");
        Path historicalDataCsvFile = outputFolder.resolve(histoFileName + ".csv");
        Path historicalDataMatFile = outputFolder.resolve(histoFileName + ".mat");
        try {
            ArrayList<String> generatorsIds = NetworkUtils.getGeneratorsIds(network);
            ArrayList<String> loadsIds = NetworkUtils.getLoadsIds(network);
            OnlineConfig config = OnlineConfig.load();
            HistoDbClient histoDbClient = config.getHistoDbClientFactoryClass().newInstance().create();
            System.out.println("downloading data from histodb for historical interval " + histoInterval);
            FEAHistoDBFacade.historicalDataToCsvFile(histoDbClient,
                                                     generatorsIds,
                                                     loadsIds,
                                                     histoInterval,
                                                     historicalDataCsvFile);
            ForecastErrorsHistoricalData forecastErrorsHistoricalData = new HistoricalDataCreator(network, generatorsIds, loadsIds)
                    .createForecastErrorsHistoricalData(historicalDataCsvFile);
            System.out.println("creating mat file " + historicalDataMatFile);
            new FEAMatFileWriter(historicalDataMatFile).writeHistoricalData(forecastErrorsHistoricalData);
        } finally {
            if (Files.exists(historicalDataCsvFile)) {
                Files.delete(historicalDataCsvFile);
            }
        }
    }

}
