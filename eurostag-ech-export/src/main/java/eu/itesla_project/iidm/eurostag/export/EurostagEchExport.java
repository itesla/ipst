/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import eu.itesla_project.eurostag.network.EsgGeneralParameters;
import eu.itesla_project.eurostag.network.EsgNetwork;
import eu.itesla_project.eurostag.network.EsgSpecialParameters;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public interface EurostagEchExport {
    EsgNetwork createNetwork(EsgGeneralParameters parameters);

    void write(Path file, EsgGeneralParameters parameters, EsgSpecialParameters specialParameters) throws IOException;
}
