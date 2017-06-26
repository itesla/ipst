/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.offline.tools;

import com.google.auto.service.AutoService;
import eu.itesla_project.commons.tools.Command;
import eu.itesla_project.commons.tools.Tool;
import eu.itesla_project.commons.tools.ToolRunningContext;
import eu.itesla_project.offline.OfflineApplication;
import eu.itesla_project.offline.RemoteOfflineApplicationImpl;
import org.apache.commons.cli.CommandLine;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(Tool.class)
public class RemoveOfflineWorkflowTool implements Tool {

    @Override
    public Command getCommand() {
        return RemoveOfflineWorkflowCommand.INSTANCE;
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        String workflowId = line.getOptionValue("workflow");        
        try (OfflineApplication app = new RemoteOfflineApplicationImpl()) {
            app.removeWorkflow(workflowId);
        }
    }

}
