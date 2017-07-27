/**
 * Copyright (c) 2016-2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.tools;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import eu.itesla_project.commons.tools.Command;
import eu.itesla_project.commons.tools.Tool;
import eu.itesla_project.commons.tools.ToolRunningContext;
import eu.itesla_project.commons.datasource.DataSource;
import eu.itesla_project.commons.datasource.FileDataSource;
import eu.itesla_project.iidm.export.Exporters;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.modules.online.OnlineConfig;
import eu.itesla_project.modules.online.OnlineDb;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
@AutoService(Tool.class)
public class AmplExportOnlineWorkflowStatesTool implements Tool {

    private static Command COMMAND = new Command() {

        @Override
        public String getName() {
            return "ampl-export-online-workflow-states";
        }

        @Override
        public String getTheme() {
            return Themes.ONLINE_WORKFLOW;
        }

        @Override
        public String getDescription() {
            return "Export network data of stored states of an online workflow, in AMPL format";
        }

        @Override
        public Options getOptions() {
            Options options = new Options();
            options.addOption(Option.builder().longOpt("workflow")
                    .desc("the workflow id")
                    .hasArg()
                    .required()
                    .argName("ID")
                    .build());
            options.addOption(Option.builder().longOpt("state")
                    .desc("the state id; all states if not specified")
                    .hasArg()
                    .argName("STATE")
                    .build());
            options.addOption(Option.builder().longOpt("contingency")
                    .desc("the contingency id; all contingencies if not specified")
                    .hasArg()
                    .argName("CONTINGENCY")
                    .build());
            options.addOption(Option.builder().longOpt("folder")
                    .desc("the folder where to export the network data")
                    .hasArg()
                    .required()
                    .argName("FOLDER")
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
        OnlineConfig config = OnlineConfig.load();
        OnlineDb onlinedb = config.getOnlineDbFactoryClass().newInstance().create();
        String workflowId = line.getOptionValue("workflow");
        if (line.hasOption("contingency") && !line.hasOption("state")) {
            throw new RuntimeException("state needed, when a contingency is specified !");
        }
        //pre-contingency state
        List<Integer> states = line.hasOption("state") ? Arrays.asList(Integer.valueOf(line.getOptionValue("state"))) : onlinedb.listStoredStates(workflowId);
        //post-contingencies states
        Map<Integer, Set<String>> postContingenciesStates = line.hasOption("contingency")
                ? ImmutableMap.of(Integer.valueOf(line.getOptionValue("state")), ImmutableSet.of(line.getOptionValue("contingency")))
                : onlinedb.listStoredPostContingencyStates(workflowId);
        //total number of post-contingencis states
        int postcontingenciesStatesSize = postContingenciesStates
                .keySet()
                .stream()
                .mapToInt(key -> postContingenciesStates.get(key).size())
                .sum();

        Path folder = Paths.get(line.getOptionValue("folder"));
        context.getOutputStream().println("Exporting in AMPL format network data of workflow " + workflowId + ", " + states.size() + " state(s), " + postcontingenciesStatesSize + " post-contingencies states, to folder " + folder);
        states.forEach(state -> {
            //exports pre-contingency state
            exportState(onlinedb, workflowId, state, null, folder, context.getOutputStream());
            //exports post-contingencies states
            Set<String> contingenciesIds = postContingenciesStates.get(state);
            if (contingenciesIds != null) {
                contingenciesIds.forEach(contingencyId -> {
                    exportState(onlinedb, workflowId, state, contingencyId, folder, context.getOutputStream());
                });
            }
        });
        onlinedb.close();
    }

    private void exportState(OnlineDb onlinedb, String workflowId, Integer stateId, String contingencyId, Path folder,
                             PrintStream out) {
        String wfInfo = "workflow " + workflowId + ", state " + stateId + ((contingencyId != null) ? ", contingency " + contingencyId : "");
        Network network = onlinedb.getState(workflowId, stateId, contingencyId);
        if (network == null) {
            out.println("Cannot export network data: no stored state for " + wfInfo);
            return;
        }
        String baseName = "wf_" + workflowId + "_state_" + stateId + ((contingencyId != null) ? "_cont_" + contingencyId : "");
        Path stateFolder = Paths.get(folder.toString(), baseName);
        out.println("Exporting network data of " + wfInfo + " to folder " + stateFolder);
        if (stateFolder.toFile().exists()) {
            out.println("Cannot export network data of " + wfInfo + ": folder " + stateFolder + " already exists");
            return;
        }
        if (!stateFolder.toFile().mkdirs()) {
            out.println("Cannot export network data of " + wfInfo + ": unable to create " + stateFolder + " folder");
            return;
        }
        DataSource dataSource = new FileDataSource(stateFolder, baseName);
        Exporters.export("AMPL", network, new Properties(), dataSource);
    }

}
