/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.powsybl;

import com.google.auto.service.AutoService;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import py4j.GatewayServer;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
@AutoService(Tool.class)
public class PyPowsybl implements Tool {

    @Override
    public Command getCommand() {
        return new Command() {

            @Override
            public String getName() {
                return "py-powsybl";
            }

            @Override
            public String getTheme() {
                return "Misc";
            }

            @Override
            public String getDescription() {
                return "run py-powsybl gateway";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
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

        GatewayServer server = new GatewayServer();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println();
                System.out.println("Shutting down py-powsybl server");
                server.shutdown();
            }
        });

        // execute the py4j Gateway service
        //TODO customize service parameters: address, port, .....
        server.start();
        System.out.println("py-powsybl server started; CTRL+C to stop it");

    }
}
