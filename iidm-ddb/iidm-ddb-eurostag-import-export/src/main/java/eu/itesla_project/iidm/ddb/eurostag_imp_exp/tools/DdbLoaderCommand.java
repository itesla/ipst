/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.ddb.eurostag_imp_exp.tools;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.powsybl.tools.Command;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class DdbLoaderCommand implements Command {

    public static final DdbLoaderCommand INSTANCE = new DdbLoaderCommand();

    public static final String DATA_DIR = "data-dir";
    public static final String EUROSTAG_VERSION = "eurostag-version";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String USER = "user";
    public static final String PASSWORD = "password";

    @Override
    public String getName() {
        return "ddb-load-eurostag";
    }

    @Override
    public String getTheme() {
        return "Dynamic Database";
    }

    @Override
    public String getDescription() {
        return "load dynamic database data from Eurostag data";
    }

    @Override
     @SuppressWarnings("static-access")
    public Options getOptions() {
        Options opts = new Options();


        opts.addOption(Option.builder().longOpt(DATA_DIR)
                .desc("data directory")
                .hasArg()
                .argName("DIR")
                .required()
                .build());

        opts.addOption(Option.builder().longOpt(EUROSTAG_VERSION)
                .desc("eurostag Version ( i.e 5.1.1)")
                .hasArg()
                .argName("VERSION")
                .required()
                .build());

        opts.addOption(Option.builder().longOpt(HOST)
                .desc("jboss host")
                .hasArg()
                .argName("HOST")
                .required()
                .build());

        opts.addOption(Option.builder().longOpt(PORT)
                .desc("jboss port")
                .hasArg()
                .argName("PORT")
                .required()
                .build());


        opts.addOption(Option.builder().longOpt(USER)
                .desc("jboss username")
                .hasArg()
                .argName("USER")
                .required()
                .build());

        opts.addOption(Option.builder().longOpt(PASSWORD)
                .desc("jboss password")
                .hasArg()
                .argName("PASSWORD")
                .required()
                .build());






        return opts;
    }

    @Override
    public String getUsageFooter() {
        // TODO Auto-generated method stub
        return null;
    }

}
