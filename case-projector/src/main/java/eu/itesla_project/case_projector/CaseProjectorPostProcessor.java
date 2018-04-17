/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.itesla_project.case_projector;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.import_.ImportPostProcessor;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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

    LoadFlowFactory loadFlowFactory;
    LoadFlowParameters loadFlowParameters;

    private final CaseProjectorConfig config;

    private final Path generatorsDomains;


    public CaseProjectorPostProcessor() {
        this(PlatformConfig.defaultConfig());
    }

    public CaseProjectorPostProcessor(PlatformConfig platformConfig) {
        ComponentDefaultConfig defaultConfig = ComponentDefaultConfig.load();
        loadFlowFactory = defaultConfig.newFactoryImpl(LoadFlowFactory.class);
        loadFlowParameters = new LoadFlowParameters(LoadFlowParameters.VoltageInitMode.UNIFORM_VALUES);
        config = CaseProjectorConfig.load();
        //retrieve the generators domains file from the caseProjector config section
        ModuleConfig config = platformConfig.getModuleConfig("caseProjector");
        generatorsDomains = config.getPathProperty("generatorsDomainsFile");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void process(Network network, ComputationManager computationManager) throws Exception {
        LoadFlow loadFlow = loadFlowFactory.create(network, computationManager, 0);
        project(computationManager, network, loadFlow, network.getStateManager().getWorkingStateId()).join();
    }

    private static final LoadFlowParameters LOAD_FLOW_PARAMETERS = LoadFlowParameters.load();

    private static final LoadFlowParameters LOAD_FLOW_PARAMETERS2 = LoadFlowParameters.load().setNoGeneratorReactiveLimits(true);

    public CompletableFuture<Boolean> project(ComputationManager computationManager, Network network, LoadFlow loadFlow, String workingStateId) throws Exception {
        return loadFlow.runAsync(workingStateId, LOAD_FLOW_PARAMETERS)
                .thenComposeAsync(loadFlowResult -> {
                    LOGGER.debug("Pre-projector load flow metrics: {}", loadFlowResult.getMetrics());
                    if (!loadFlowResult.isOk()) {
                        throw new CaseProjectorUtils.StopException("Pre-projector load flow diverged");
                    }
                    return CaseProjectorUtils.createAmplTask(computationManager, network, workingStateId, config, generatorsDomains);
                }, computationManager.getExecutor())
                .thenComposeAsync(ok -> {
                    if (!Boolean.TRUE.equals(ok)) {
                        throw new CaseProjectorUtils.StopException("Projector failed");
                    }
                    return loadFlow.runAsync(workingStateId, LOAD_FLOW_PARAMETERS2);
                }, computationManager.getExecutor())
                .thenApplyAsync(loadFlowResult -> {
                    LOGGER.debug("Post-projector load flow metrics: {}", loadFlowResult.getMetrics());
                    if (!loadFlowResult.isOk()) {
                        throw new CaseProjectorUtils.StopException("Post-projector load flow diverged");
                    }
                    CaseProjectorUtils.reintegrateLfState(network, workingStateId);
                    return Boolean.TRUE;
                }, computationManager.getExecutor())
                .exceptionally(throwable -> {
                    if (!(throwable instanceof CompletionException && throwable.getCause() instanceof CaseProjectorUtils.StopException)) {
                        LOGGER.error(throwable.toString(), throwable);
                    }
                    return Boolean.FALSE;
                });
    }


}
