/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.eurostag;

import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.simulation.ImpactAnalysis;
import com.powsybl.simulation.SimulatorFactory;
import com.powsybl.simulation.Stabilization;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EurostagFactory implements SimulatorFactory {

    @Override
    public Stabilization createStabilization(Network network, ComputationManager computationManager, int priority) {
        return new EurostagStabilization(network, computationManager, priority);
    }

    @Override
    public ImpactAnalysis createImpactAnalysis(Network network, ComputationManager computationManager, int priority, ContingenciesProvider contingenciesProvider) {
        return new EurostagImpactAnalysis(network, computationManager, priority, contingenciesProvider);
    }

}
