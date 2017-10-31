/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules;

import com.google.auto.service.AutoService;
import com.powsybl.security.LimitViolationFilter;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.offline.OfflineConfig;
import eu.itesla_project.modules.sampling.SampleCharacteritics;
import eu.itesla_project.modules.topo.TopologyContext;
import eu.itesla_project.modules.topo.TopologyMiner;
import com.powsybl.security.Security;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.joda.time.Interval;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(Tool.class)
public class OptimizerTool implements Tool {

    private static Command COMMAND = new Command() {

        @Override
        public String getName() {
            return "wp42-run";
        }

        @Override
        public String getTheme() {
            return "WP42";
        }

        @Override
        public String getDescription() {
            return "run WP42 optimizer + load flow";
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
            options.addOption(Option.builder().longOpt("history-interval")
                    .desc("history time interval (example 2013-01-01T00:00:00+01:00/2013-01-31T23:59:00+01:00)")
                    .hasArg()
                    .argName("DATE1/DATE2")
                    .required()
                    .build());
            options.addOption(Option.builder().longOpt("correlation-threshold")
                    .desc("correlation threshold")
                    .hasArg()
                    .argName("VALUE")
                    .required()
                    .build());
            options.addOption(Option.builder().longOpt("probability-threshold")
                    .desc("probability threshold")
                    .hasArg()
                    .argName("VALUE")
                    .required()
                    .build());
            options.addOption(Option.builder().longOpt("generation-sampled")
                    .desc("generation sampled")
                    .build());
            options.addOption(Option.builder().longOpt("boundaries-sampled")
                    .desc("boundaries sampled")
                    .build());
            options.addOption(Option.builder().longOpt("check-constraints")
                    .desc("check static contraints (voltage and current limits)")
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
        Interval histoInterval = Interval.parse(line.getOptionValue("history-interval"));
        boolean checkConstraints = line.hasOption("check-constraints");
        double correlationThreshold = Double.parseDouble(line.getOptionValue("correlation-threshold"));
        double probabilityThreshold = Double.parseDouble(line.getOptionValue("probability-threshold"));
        boolean generationSampled = line.hasOption("generation-sampled");
        boolean boundariesSampled = line.hasOption("boundaries-sampled");

        context.getOutputStream().println("loading case...");
        // load the network
        Network network = Importers.loadNetwork(caseFile);
        if (network == null) {
            throw new RuntimeException("Case '" + caseFile + "' not found");
        }
        network.getStateManager().allowStateMultiThreadAccess(true);

        context.getOutputStream().println("sample characteristics: " + SampleCharacteritics.fromNetwork(network, generationSampled, boundariesSampled));

        OfflineConfig config = OfflineConfig.load();
        try (HistoDbClient histoDbClient = config.getHistoDbClientFactoryClass().newInstance().create();
             TopologyMiner topologyMiner = config.getTopologyMinerFactoryClass().newInstance().create()) {

            Optimizer optimizer = config.getOptimizerFactoryClass().newInstance().create(network, context.getComputationManager(), 0, histoDbClient, topologyMiner);
            LoadFlow loadFlow = config.getLoadFlowFactoryClass().newInstance().create(network, context.getComputationManager(), 0);

            context.getOutputStream().println("initializing optimizer...");

            TopologyContext topologyContext = TopologyContext.create(network, topologyMiner, histoDbClient, context.getComputationManager(), histoInterval, correlationThreshold, probabilityThreshold);

            optimizer.init(new OptimizerParameters(histoInterval), topologyContext);

            context.getOutputStream().println("running optimizer...");

            OptimizerResult result = optimizer.run();

            context.getOutputStream().println("optimizer status is " + (result.isFeasible() ? "feasible" : "unfeasible") + " (" + result.getMetrics() + ")");

            if (result.isFeasible()) {
                context.getOutputStream().println("running loadflow...");

                LoadFlowResult result2 = loadFlow.run();

                context.getOutputStream().println("loadflow status is " + (result2.isOk() ? "ok" : "nok") + " (" + result2.getMetrics() + ")");

                if (result2.isOk() && checkConstraints) {
                    String report = Security.printLimitsViolations(network, LimitViolationFilter.load());
                    if (report != null) {
                        context.getOutputStream().println(report);
                    }
                }
            }
        }
    }

}
