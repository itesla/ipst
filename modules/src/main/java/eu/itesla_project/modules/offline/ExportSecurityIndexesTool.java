/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.offline;

import com.powsybl.tools.Tool;
import com.powsybl.tools.Command;
import com.google.auto.service.AutoService;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(Tool.class)
public class ExportSecurityIndexesTool implements Tool {

    @Override
    public Command getCommand() {
        return ExportSecurityIndexesCommand.INSTANCE;
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        String simulationDbName = line.hasOption("simulation-db-name") ? line.getOptionValue("simulation-db-name") : OfflineConfig.DEFAULT_SIMULATION_DB_NAME;
        OfflineConfig config = OfflineConfig.load();
        OfflineDb offlineDb = config.getOfflineDbFactoryClass().newInstance().create(simulationDbName);
        String workflowId = line.getOptionValue("workflow");
        Path outputFile = Paths.get(line.getOptionValue("output-file"));
        char delimiter = ';';
        if (line.hasOption("delimiter")) {
            String value = line.getOptionValue("delimiter");
            if (value.length() != 1) {
                throw new RuntimeException("A character is expected");
            }
            delimiter = value.charAt(0);
        }
        OfflineAttributesFilter stateAttrFilter = OfflineAttributesFilter.ALL;
        if (line.hasOption("attributes-filter")) {
            stateAttrFilter = OfflineAttributesFilter.valueOf(line.getOptionValue("attributes-filter"));
        }
        boolean addSampleColumn = line.hasOption("add-sample-column");
        boolean keepAllSamples = line.hasOption("keep-all-samples");
        try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            offlineDb.exportCsv(workflowId, writer, new OfflineDbCsvExportConfig(delimiter, stateAttrFilter, addSampleColumn, keepAllSamples));
        }
    }

}
