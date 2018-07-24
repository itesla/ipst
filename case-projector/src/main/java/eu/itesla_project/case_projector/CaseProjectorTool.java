/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.case_projector;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.StateManager;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.simulation.SimulatorFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
@AutoService(Tool.class)
public class CaseProjectorTool implements Tool {

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "case-projector";
            }

            @Override
            public String getTheme() {
                return "Computation";
            }

            @Override
            public String getDescription() {
                return "run case projector";
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
                options.addOption(Option.builder().longOpt("generators-domains-file")
                        .desc("the generators domains file path")
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
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        Path caseFile = Paths.get(line.getOptionValue("case-file"));

        Network network = Importers.loadNetwork(caseFile);
        if (network == null) {
            throw new RuntimeException("Case " + caseFile + " not found");
        }
        ComponentDefaultConfig config = ComponentDefaultConfig.load();
        LoadFlowFactory loadFlowFactory = config.newFactoryImpl(LoadFlowFactory.class);
        SimulatorFactory simulatorFactory = config.newFactoryImpl(SimulatorFactory.class);
        CaseProjectorConfig caseProjectorConfig = CaseProjectorConfig.load();
        if (line.hasOption("generators-domains-file")) {
            caseProjectorConfig = new CaseProjectorConfig(caseProjectorConfig.getAmplHomeDir(), Paths.get(line.getOptionValue("generators-domains-file")), caseProjectorConfig.isDebug());
        }
        new CaseProjector(network, LocalComputationManager.getDefault(), loadFlowFactory, simulatorFactory, caseProjectorConfig)
                .project(StateManager.INITIAL_STATE_ID).join();
    }
}
