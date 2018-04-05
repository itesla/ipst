/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.eurostag;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.ContingenciesProviderFactory;
import eu.itesla_project.eurostag.network.EsgGeneralParameters;
import eu.itesla_project.eurostag.network.EsgNetwork;
import eu.itesla_project.eurostag.network.EsgSpecialParameters;
import eu.itesla_project.eurostag.network.io.EsgWriter;
import eu.itesla_project.eurostag.tools.EurostagNetworkModifier;
import eu.itesla_project.iidm.ddb.eurostag_imp_exp.DynamicDatabaseClient;
import eu.itesla_project.iidm.ddb.eurostag_imp_exp.DynamicDatabaseClientFactory;
import eu.itesla_project.iidm.eurostag.export.*;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.simulation.SimulationParameters;
import org.apache.commons.cli.CommandLine;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(Tool.class)
public class EurostagExportTool implements Tool, EurostagConstants {

    @Override
    public Command getCommand() {
        return EurostagExportCommand.INSTANCE;
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        ComponentDefaultConfig defaultConfig = ComponentDefaultConfig.load();
        EurostagConfig eurostagConfig = EurostagConfig.load();
        Path caseFile = Paths.get(line.getOptionValue("case-file"));
        Path outputDir = Paths.get(line.getOptionValue("output-dir"));
        if (!Files.isDirectory(outputDir)) {
            throw new RuntimeException(outputDir + " is not a directory");
        }
        DynamicDatabaseClient ddbClient = defaultConfig.newFactoryImpl(DynamicDatabaseClientFactory.class).create(eurostagConfig.isDdbCaching());

        context.getOutputStream().println("loading case...");
        // load network
        Network network = Importers.loadNetwork(caseFile);
        if (network == null) {
            throw new RuntimeException("Case '" + caseFile + "' not found");
        }
        network.getStateManager().allowStateMultiThreadAccess(true);

        context.getOutputStream().println("exporting ech...");
        // export .ech and dictionary
        EurostagEchExportConfig exportConfig = EurostagEchExportConfig.load();
        EurostagFakeNodes fakeNodes = EurostagFakeNodes.build(network, exportConfig);
        BranchParallelIndexes parallelIndexes = BranchParallelIndexes.build(network, exportConfig, fakeNodes);
        EurostagDictionary dictionary = EurostagDictionary.create(network, parallelIndexes, exportConfig, fakeNodes);

        try (Writer writer = Files.newBufferedWriter(outputDir.resolve("sim.ech"), StandardCharsets.UTF_8)) {
            EsgGeneralParameters parameters = new EsgGeneralParameters();
            parameters.setTransformerVoltageControl(false);
            parameters.setSvcVoltageControl(false);
            parameters.setMaxNumIteration(eurostagConfig.getLfMaxNumIteration());
            EsgSpecialParameters specialParameters = null;
            if (exportConfig.isSpecificCompatibility()) {
                context.getOutputStream().println("specificCompatibility=true: forces start mode to WARM and write the special parameters section in ech file");
                parameters.setStartMode(EsgGeneralParameters.StartMode.WARM_START);
                specialParameters = new EsgSpecialParameters();
                //WARM START: ZMIN_LOW
                specialParameters.setZmin(EsgSpecialParameters.ZMIN_LOW);
            } else {
                parameters.setStartMode(eurostagConfig.isLfWarmStart() ? EsgGeneralParameters.StartMode.WARM_START : EsgGeneralParameters.StartMode.FLAT_START);
            }

            EurostagEchExporterFactory echExportFactory = defaultConfig.newFactoryImpl(EurostagEchExporterFactory.class, EurostagEchExporterFactoryImpl.class);
            EsgNetwork networkEch = echExportFactory.createEchExporter(network, exportConfig, parallelIndexes, dictionary, fakeNodes).createNetwork(parameters);
            new EurostagNetworkModifier().hvLoadModelling(networkEch);
            new EsgWriter(networkEch, parameters, specialParameters).write(writer, network.getId() + "/" + network.getStateManager().getWorkingStateId());
        }
        dictionary.dump(outputDir.resolve("dict.csv"));
        context.getOutputStream().println("exporting dta...");

        // export .dta
        ddbClient.dumpDtaFile(outputDir, "sim.dta", network, parallelIndexes.toMap(), EurostagUtil.VERSION, dictionary.toMap());

        context.getOutputStream().println("exporting seq...");

        // export .seq
        EurostagScenario scenario = new EurostagScenario(SimulationParameters.load(), eurostagConfig);
        try (BufferedWriter writer = Files.newBufferedWriter(outputDir.resolve(PRE_FAULT_SEQ_FILE_NAME), StandardCharsets.UTF_8)) {
            scenario.writePreFaultSeq(writer, PRE_FAULT_SAC_FILE_NAME);
        }
        ContingenciesProvider contingenciesProvider = defaultConfig.newFactoryImpl(ContingenciesProviderFactory.class).create();
        scenario.writeFaultSeqArchive(contingenciesProvider.getContingencies(network), network, dictionary, faultNum -> FAULT_SEQ_FILE_NAME.replace(com.powsybl.computation.Command.EXECUTION_NUMBER_PATTERN, Integer.toString(faultNum)))
                .as(ZipExporter.class).exportTo(outputDir.resolve(ALL_SCENARIOS_ZIP_FILE_NAME).toFile());

        // export limits
        try (OutputStream os = Files.newOutputStream(outputDir.resolve(LIMITS_ZIP_FILE_NAME))) {
            EurostagImpactAnalysis.writeLimits(network, dictionary, os, exportConfig);
        }
    }

}
