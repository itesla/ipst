/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.histo.tools;

import com.google.auto.service.AutoService;
import eu.itesla_project.commons.tools.Command;
import eu.itesla_project.commons.tools.Tool;
import eu.itesla_project.commons.tools.ToolRunningContext;
import eu.itesla_project.modules.histo.*;
import eu.itesla_project.modules.offline.OfflineConfig;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.joda.time.Interval;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(Tool.class)
public class HistoDbCountAttributesTool implements Tool {

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "histodb-count-attributes";
            }

            @Override
            public String getTheme() {
                return "Histo DB";
            }

            @Override
            public String getDescription() {
                return "count attributes instances";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt("interval")
                        .desc("time interval (example 2013-01-01T00:00:00+01:00/2013-01-31T23:59:00+01:00)")
                        .hasArg()
                        .required()
                        .argName("DATE1/DATE2")
                        .build());
                options.addOption(Option.builder().longOpt("horizon")
                        .desc("SN/DACF (default SN)")
                        .hasArg()
                        .argName("HORIZON")
                        .build());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return "Where HORIZON is one of " + Arrays.toString(HistoDbHorizon.values());
            }
        };
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        Interval interval = Interval.parse(line.getOptionValue("interval"));
        HistoDbHorizon horizon = HistoDbHorizon.SN;
        if (line.hasOption("horizon")) {
            horizon = HistoDbHorizon.valueOf(line.getOptionValue("horizon"));
        }
        OfflineConfig config = OfflineConfig.load();
        try (HistoDbClient histoDbClient = config.getHistoDbClientFactoryClass().newInstance().create(true)) {
            Set<HistoDbAttributeId> attributeIds = new LinkedHashSet<>(histoDbClient.listAttributes());
            HistoDbStats stats = histoDbClient.queryStats(attributeIds, interval, horizon, true);
            for (HistoDbAttributeId attributeId : attributeIds) {
                context.getOutputStream().println(attributeId + ";" + (int) stats.getValue(HistoDbStatsType.COUNT, attributeId, -1));
            }
        }
    }
}
