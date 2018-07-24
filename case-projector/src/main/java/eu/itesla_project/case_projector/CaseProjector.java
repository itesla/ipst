/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.case_projector;

import com.powsybl.computation.*;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.simulation.SimulationParameters;
import com.powsybl.simulation.SimulatorFactory;
import com.powsybl.simulation.Stabilization;
import com.powsybl.simulation.StabilizationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class CaseProjector {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProjector.class);

    private final Network network;

    private final ComputationManager computationManager;

    private final LoadFlowFactory loadFlowFactory;

    private final SimulatorFactory simulatorFactory;

    private final CaseProjectorConfig config;

    private final LoadFlow loadFlow;

    private final Stabilization stabilization;


    public CaseProjector(Network network, ComputationManager computationManager, LoadFlowFactory loadFlowFactory,
                         SimulatorFactory simulatorFactory, CaseProjectorConfig config) throws Exception {
        this.network = Objects.requireNonNull(network);
        this.computationManager = Objects.requireNonNull(computationManager);
        this.loadFlowFactory = Objects.requireNonNull(loadFlowFactory);
        this.simulatorFactory = Objects.requireNonNull(simulatorFactory);
        this.config = Objects.requireNonNull(config);
        loadFlow = loadFlowFactory.create(network, computationManager, 0);
        stabilization = simulatorFactory.createStabilization(network, computationManager, 0);
        stabilization.init(SimulationParameters.load(), new HashMap<>());
    }

    public CompletableFuture<Boolean> project(String workingStateId) throws Exception {
        return CaseProjectorUtils.project(computationManager, network, loadFlow, workingStateId, config)
                .thenComposeAsync(aVoid -> stabilization.runAsync(workingStateId), computationManager.getExecutor())
                .thenApplyAsync(stabilizationResult -> {
                    if (stabilizationResult.getStatus() == StabilizationStatus.COMPLETED) {
                        LOGGER.debug("Stabilization metrics: {}", stabilizationResult.getMetrics());
                        return Boolean.TRUE;
                    } else {
                        return Boolean.FALSE;
                    }
                }, computationManager.getExecutor())
                .exceptionally(throwable -> {
                    if (!(throwable instanceof CompletionException && throwable.getCause() instanceof CaseProjectorUtils.StopException)) {
                        LOGGER.error(throwable.toString(), throwable);
                    }
                    return Boolean.FALSE;
                });
    }

}
