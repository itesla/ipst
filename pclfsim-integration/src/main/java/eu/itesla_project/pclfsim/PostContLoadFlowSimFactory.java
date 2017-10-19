/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pclfsim;

import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.simulation.ImpactAnalysis;
import com.powsybl.simulation.SimulatorFactory;
import com.powsybl.simulation.Stabilization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Quinary <itesla@quinary.com>
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PostContLoadFlowSimFactory implements SimulatorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostContLoadFlowSimFactory.class);

    private final PostContLoadFlowSimConfig config = PostContLoadFlowSimConfig.load();

    private final LoadFlowFactory loadFlowFactory;

    public PostContLoadFlowSimFactory() {
        LOGGER.info(config.toString());
        try {
            loadFlowFactory = config.getLoadFlowFactoryClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Stabilization createStabilization(Network network, ComputationManager computationManager, int priority) {
        return new PostContLoadFlowSimStabilization(network, config);
    }

    @Override
    public ImpactAnalysis createImpactAnalysis(Network network, ComputationManager computationManager, int priority, ContingenciesProvider contingenciesProvider) {
        return new PostContLoadFlowSimImpactAnalysis(network, computationManager, priority, contingenciesProvider, config, loadFlowFactory);
    }

}
