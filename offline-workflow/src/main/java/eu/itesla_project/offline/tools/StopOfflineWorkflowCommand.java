/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.offline.tools;

import com.powsybl.tools.Command;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class StopOfflineWorkflowCommand implements Command {

    public static final StopOfflineWorkflowCommand INSTANCE = new StopOfflineWorkflowCommand();

    @Override
    public String getName() {
        return "stop-offline-workflow";
    }

    @Override
    public String getTheme() {
        return Themes.OFFLINE_APPLICATION_REMOTE_CONTROL;
    }

    @Override
    public String getDescription() {
        return "stop an offline workflow";
    }

    @Override
    @SuppressWarnings("static-access")
    public Options getOptions() {
        Options options = new Options();
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

}
