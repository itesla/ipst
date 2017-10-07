/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.offline;

import com.google.auto.service.AutoService;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(Tool.class)
public class SimulationsDbListSecurityIndexes implements Tool {

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "simulations-db-list-security-indexes";
            }

            @Override
            public String getTheme() {
                return "Simulation DB";
            }

            @Override
            public String getDescription() {
                return "list simulations db security indexes";
            }

            @Override
            @SuppressWarnings("static-access")
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt("simulation-db-name")
                        .desc("the simulation db name (default is " + OfflineConfig.DEFAULT_SIMULATION_DB_NAME + ")")
                        .hasArg()
                        .argName("NAME")
                        .build());
                options.addOption(Option.builder().longOpt("workflow")
                        .desc("the workflow id")
                        .hasArg()
                        .required()
                        .argName("ID")
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
        String simulationDbName = line.hasOption("simulation-db-name") ? line.getOptionValue("simulation-db-name") : OfflineConfig.DEFAULT_SIMULATION_DB_NAME;
        String workflowId = line.getOptionValue("workflow");
        OfflineConfig config = OfflineConfig.load();
        OfflineDb offlineDb = config.getOfflineDbFactoryClass().newInstance().create(simulationDbName);
        offlineDb.getSecurityIndexIds(workflowId).forEach(securityIndexId -> context.getOutputStream().println(securityIndexId));
    }

}
