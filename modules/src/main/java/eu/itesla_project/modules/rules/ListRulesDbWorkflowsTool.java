/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.rules;

import com.google.auto.service.AutoService;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import eu.itesla_project.modules.offline.OfflineConfig;
import org.apache.commons.cli.CommandLine;
import org.nocrala.tools.texttablefmt.BorderStyle;
import org.nocrala.tools.texttablefmt.Table;

import java.util.List;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(Tool.class)
public class ListRulesDbWorkflowsTool implements Tool {

    @Override
    public Command getCommand() {
        return ListRulesDbWorkflowsCommand.INSTANCE;
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        OfflineConfig config = OfflineConfig.load();
        String rulesDbName = line.hasOption("rules-db-name") ? line.getOptionValue("rules-db-name") : OfflineConfig.DEFAULT_RULES_DB_NAME;
        RulesDbClient rulesDb = config.getRulesDbClientFactoryClass().newInstance().create(rulesDbName);
        List<String> workflowIds = rulesDb.listWorkflows();
        Table table = new Table(1, BorderStyle.CLASSIC_WIDE);
        table.addCell("ID");
        for (String workflowId : workflowIds) {
            table.addCell(workflowId);
        }
        context.getOutputStream().println(table.render());
    }

}
