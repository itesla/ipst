/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import java.nio.file.Path;
import java.util.Properties;

import eu.itesla_project.iidm.datasource.DataSource;
import eu.itesla_project.iidm.datasource.GzFileDataSource;
import eu.itesla_project.iidm.export.Exporters;
import eu.itesla_project.iidm.network.Network;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAStateExporter {

    private final Network network;
    private final Path folder;
    private final int faultNum;
    private final int actionNum;

    public WCAStateExporter(Network network, Path folder, int faultNum, int actionNum) {
        this.network = network;
        this.folder = folder;
        this.faultNum = faultNum;
        this.actionNum = actionNum;
    }

    public void export() {
        Properties parameters = new Properties();
        parameters.setProperty("iidm.export.xml.indent", "true");
        parameters.setProperty("iidm.export.xml.with-branch-state-variables", "true");
        DataSource dataSource = new GzFileDataSource(folder, network.getId() + "_" + faultNum + "_" + actionNum);
        Exporters.export("XIIDM", network, parameters, dataSource);
    }

}
