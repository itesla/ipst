/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.uncertainties;

import com.powsybl.tools.Command;
import com.powsybl.iidm.import_.Importers;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class UncertaintiesAnalysisCommand implements Command {

    public static final UncertaintiesAnalysisCommand INSTANCE = new UncertaintiesAnalysisCommand();

    @Override
    public String getName() {
        return "analyse-uncertainties";
    }

    @Override
    public String getTheme() {
        return "Uncertainties";
    }

    @Override
    public String getDescription() {
        return "analyse stochastic injections uncertainties";
    }

    @Override
    @SuppressWarnings("static-access")
    public Options getOptions() {
        Options options = new Options();
        options.addOption(Option.builder().longOpt("case-file")
                .desc("the case path")
                .hasArg()
                .argName("FILE")
                .required()
                .build());
        options.addOption(OptionBuilder.withLongOpt("output-dir")
                .withDescription("output directory path")
                .hasArg()
                .withArgName("DIR")
                .isRequired()
                .create());
        options.addOption(OptionBuilder.withLongOpt("history-interval")
                .withDescription("history time interval (example 2013-01-01T00:00:00+01:00/2013-01-31T23:59:00+01:00)")
                .hasArg()
                .withArgName("DATE1/DATE2")
                .isRequired()
                .create());
        return options;
    }

    @Override
    public String getUsageFooter() {
        return "Where FORMAT is one of " + Importers.getFormats();
    }

}
