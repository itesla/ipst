/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.ddb.eurostag_imp_exp.tools;

import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;

import com.google.auto.service.AutoService;

import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import eu.itesla_project.iidm.ddb.eurostag_imp_exp.DdbConfig;
import eu.itesla_project.iidm.ddb.eurostag_imp_exp.DynDataUnloader;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
@AutoService(Tool.class)
public class DdbUnloaderTool implements Tool {

    @Override
    public Command getCommand() {
        return DdbUnloaderCommand.INSTANCE;
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        String jbossHost = line.getOptionValue(DdbLoaderCommand.HOST);
        String jbossPort = line.getOptionValue(DdbLoaderCommand.PORT);
        String jbossUser = line.getOptionValue(DdbLoaderCommand.USER);
        String jbossPassword = line.getOptionValue(DdbLoaderCommand.PASSWORD);

        DdbConfig ddbConfig = new DdbConfig(jbossHost, jbossPort, jbossUser, jbossPassword);
        DynDataUnloader dn = new DynDataUnloader(
                ddbConfig.getJbossHost(), ddbConfig.getJbossPort(), ddbConfig.getJbossUser(), ddbConfig.getJbossPassword()
                );
        dn.unloadDynData();
    }

}
