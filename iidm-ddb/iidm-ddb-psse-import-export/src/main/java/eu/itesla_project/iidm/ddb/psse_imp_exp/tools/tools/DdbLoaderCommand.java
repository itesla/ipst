/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.ddb.psse_imp_exp.tools.tools;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.powsybl.tools.Command;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class DdbLoaderCommand implements Command {

    public static final DdbLoaderCommand INSTANCE = new DdbLoaderCommand();
    public static final String PSSE_VERSION = "psse-version";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String PSSE_DYRFILEPATH = "dyr-file";
    public static final String PSSE_MAPPINGFILEPATH = "mapping-file";
    public static final String OPTION_REMOVE = "remove-data-flag";

    @Override
    public String getName() {
        return "ddb-load-psse";
    }

    @Override
    public String getTheme() {
        return "Dynamic Database";
    }

    @Override
    public String getDescription() {
        return "load dynamic database from PSSE data";
    }

    @Override
     @SuppressWarnings("static-access")
    public Options getOptions() {
        Options opts = new Options();

        opts.addOption(Option.builder().longOpt(PSSE_DYRFILEPATH)
                .desc(".dyr input file path")
                .hasArg()
                .argName("DYRFILEPATH")
                .required()
                .build());

        opts.addOption(Option.builder().longOpt(PSSE_MAPPINGFILEPATH)
                .desc("mapping file path")
                .hasArg()
                .argName("MAPPINGFILEPATH")
                .required()
                .build());


        opts.addOption(Option.builder().longOpt(PSSE_VERSION)
                .desc("PSSE  version ( i.e 32.1)")
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

        opts.addOption(Option.builder().longOpt(OPTION_REMOVE)
                .desc("remove data")
                .hasArg()
                .argName("OPTION_REMOVE")
                .build());

        return opts;
    }

    @Override
    public String getUsageFooter() {
        // TODO Auto-generated method stub
        return null;
    }

}
