/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.modules.mock;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import eu.itesla_project.modules.mcla.ForecastErrorsDataStorage;
import eu.itesla_project.modules.mcla.MontecarloSampler;
import eu.itesla_project.modules.mcla.MontecarloSamplerFactory;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class MontecarloSamplerFactoryMock implements MontecarloSamplerFactory {
    @Override
    public MontecarloSampler create(Network network, ComputationManager computationManager, ForecastErrorsDataStorage feDataStorage) {
        return new MontecarloSamplerMock(network, computationManager, feDataStorage);
    }
}

