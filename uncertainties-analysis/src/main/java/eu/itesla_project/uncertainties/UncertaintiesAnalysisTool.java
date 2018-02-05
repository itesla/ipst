/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.uncertainties;

import com.google.auto.service.AutoService;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.DefaultDataSourceObserver;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import eu.itesla_project.histodb.client.impl.HistoDbCacheImpl;
import eu.itesla_project.histodb.client.impl.HistoDbClientImpl;
import eu.itesla_project.histodb.client.impl.HistoDbConfig;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.converter.AmplUtil;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.wca.Uncertainties;
import eu.itesla_project.wca.uncertainties.UncertaintiesAmplWriter;
import org.apache.commons.cli.CommandLine;
import org.joda.time.Interval;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
@AutoService(Tool.class)
public class UncertaintiesAnalysisTool implements Tool {

    @Override
    public Command getCommand() {
        return UncertaintiesAnalysisCommand.INSTANCE;
    }

    @Override
    public void run(CommandLine line) throws Exception {
        Path caseFile = Paths.get(line.getOptionValue("case-file"));
        Path outputDir = Paths.get(line.getOptionValue("output-dir"));
        Interval histoInterval = Interval.parse(line.getOptionValue("history-interval"));

        try (ComputationManager computationManager = new LocalComputationManager()) {

            System.out.println("loading case file: " + caseFile);

            // load the network

            if (Files.isRegularFile(caseFile)) {
                // load the network
                Network network = Importers.loadNetwork(caseFile);
                if (network == null) {
                    throw new RuntimeException("Case '" + caseFile + "' not found");
                }

                try (HistoDbClient histoDbClient = new HistoDbClientImpl(HistoDbConfig.load(), new HistoDbCacheImpl())) {

                    System.out.println("compute uncertainties...");

                    Uncertainties uncertainties = new UncertaintiesAnalyserImpl(network, histoDbClient, computationManager)
                            .analyse(histoInterval).join();

                    StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);
                    DataSource ds = new FileDataSource(outputDir, caseFile.getFileName().toString(), new DefaultDataSourceObserver() {
                        @Override
                        public void opened(String streamName) {
                            System.out.println("writing " + streamName);
                        }
                    });

                    new UncertaintiesAmplWriter(uncertainties, ds, mapper).write();
                }
            } else {
                throw new RuntimeException("Not a regular file '" + caseFile + "'");
            }
        }
    }

}
