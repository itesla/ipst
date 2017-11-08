/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.uncertainties;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.wca.UncertaintiesAnalyser;
import eu.itesla_project.modules.wca.UncertaintiesAnalyserFactory;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class UncertaintiesAnalyserFactoryImpl implements UncertaintiesAnalyserFactory {
    @Override
    public UncertaintiesAnalyser create(Network network, HistoDbClient histoDbClient, ComputationManager computationManager) {
        return new UncertaintiesAnalyserImpl(network, histoDbClient, computationManager);
    }

}
