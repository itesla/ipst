/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.modules.mock;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.rules.RulesDbClient;
import eu.itesla_project.modules.wca.UncertaintiesAnalyserFactory;
import eu.itesla_project.modules.wca.WCA;
import eu.itesla_project.modules.wca.WCAFactory;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class WCAMockFactoryImpl implements WCAFactory {
    @Override
    public WCA create(Network network, ComputationManager computationManager, HistoDbClient histoDbClient, RulesDbClient rulesDbClient, UncertaintiesAnalyserFactory uncertaintiesAnalyserFactory, ContingenciesAndActionsDatabaseClient contingenciesActionsDbClient, LoadFlowFactory loadFlowFactory) {
        return new WCAMock(network, contingenciesActionsDbClient);
    }
}
