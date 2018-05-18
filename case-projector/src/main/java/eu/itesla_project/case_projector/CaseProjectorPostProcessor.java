/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.itesla_project.case_projector;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.import_.ImportPostProcessor;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * implements post processor 'case-proj'.
 * It's based on the current case projector logic, without the final stabilization step.
 * It executes, in sequence: LF, ampl script, LF, finally reintegrate LF state.
 * Requires new parameter generatorsDomainsFile in the caseProjector config section
 *
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
@AutoService(ImportPostProcessor.class)
public class CaseProjectorPostProcessor implements ImportPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProjectorPostProcessor.class);

    public static final String NAME = "case-proj";

    private final LoadFlowFactory loadFlowFactory;

    private final CaseProjectorConfig config;

    public CaseProjectorPostProcessor() {
        this(PlatformConfig.defaultConfig());
    }

    public CaseProjectorPostProcessor(PlatformConfig platformConfig) {
        ComponentDefaultConfig defaultConfig = ComponentDefaultConfig.load();
        loadFlowFactory = defaultConfig.newFactoryImpl(LoadFlowFactory.class);
        config = CaseProjectorConfig.load();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void process(Network network, ComputationManager computationManager) throws Exception {
        LoadFlow loadFlow = loadFlowFactory.create(network, computationManager, 0);
        CaseProjectorUtils.project(computationManager, network, loadFlow, network.getStateManager().getWorkingStateId(), config).join();
    }

}
