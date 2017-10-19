/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.case_projector;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.computation.*;
import eu.itesla_project.iidm.export.ampl.*;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.simulation.SimulationParameters;
import com.powsybl.simulation.SimulatorFactory;
import com.powsybl.simulation.Stabilization;
import com.powsybl.simulation.StabilizationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class CaseProjector {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProjector.class);

    private static final Set<String> AMPL_MODEL_FILE_NAMES = ImmutableSet.<String>builder()
            .add("projector.run")
            .add("projector.mod")
            .add("projector.dat")
            .add("projectorOutput.run")
            .build();

    private static final String WORKING_DIR_PREFIX = "itesla_projector_";

    private static final LoadFlowParameters LOAD_FLOW_PARAMETERS = LoadFlowParameters.load();

    private static final LoadFlowParameters LOAD_FLOW_PARAMETERS2 = LoadFlowParameters.load().setNoGeneratorReactiveLimits(true);

    private static final String AMPL_GENERATORS_DOMAINS_FILE_NAME = "ampl_generators_domains.txt";

    private final Network network;

    private final ComputationManager computationManager;

    private final LoadFlowFactory loadFlowFactory;

    private final SimulatorFactory simulatorFactory;

    private final CaseProjectorConfig config;

    private final LoadFlow loadFlow;

    private final Stabilization stabilization;

    private final Path generatorsDomains;

    public CaseProjector(Network network, ComputationManager computationManager, LoadFlowFactory loadFlowFactory,
                         SimulatorFactory simulatorFactory, CaseProjectorConfig config, Path generatorsDomains) throws Exception {
        this.network = Objects.requireNonNull(network);
        this.computationManager = Objects.requireNonNull(computationManager);
        this.loadFlowFactory = Objects.requireNonNull(loadFlowFactory);
        this.simulatorFactory = Objects.requireNonNull(simulatorFactory);
        this.config = Objects.requireNonNull(config);
        this.generatorsDomains = Objects.requireNonNull(generatorsDomains);
        loadFlow = loadFlowFactory.create(network, computationManager, 0);
        stabilization = simulatorFactory.createStabilization(network, computationManager, 0);
        stabilization.init(SimulationParameters.load(), new HashMap<>());
    }

    private CompletableFuture<Boolean> createAmplTask(String workingStateId) {
        return computationManager.execute(new ExecutionEnvironment(ImmutableMap.of("PATH", config.getAmplHomeDir().toString()), WORKING_DIR_PREFIX, config.isDebug()),
                new AbstractExecutionHandler<Boolean>() {

                    private StringToIntMapper<AmplSubset> mapper;

                    @Override
                    public List<CommandExecution> before(Path workingDir) throws IOException {
                        network.getStateManager().setWorkingState(workingStateId);

                        // copy AMPL model
                        for (String amplModelFileName : AMPL_MODEL_FILE_NAMES) {
                            Files.copy(getClass().getResourceAsStream("/ampl/projector/" + amplModelFileName), workingDir.resolve(amplModelFileName));
                        }

                        // copy the generators domains file
                        Files.copy(generatorsDomains, workingDir.resolve(AMPL_GENERATORS_DOMAINS_FILE_NAME));

                        // write input data
                        mapper = AmplUtil.createMapper(network);
                        mapper.dump(workingDir.resolve("mapper.csv"));

                        new AmplNetworkWriter(network,
                                new FileDataSource(workingDir, "ampl"), // "ampl_network_"
                                mapper,
                                new AmplExportConfig(AmplExportConfig.ExportScope.ALL, true, AmplExportConfig.ExportActionType.CURATIVE))
                                .write();

                        Command command = new SimpleCommandBuilder()
                                .id("projector")
                                .program(config.getAmplHomeDir().resolve("ampl").toString())
                                .args("projector.run")
                                .build();
                        return Arrays.asList(new CommandExecution(command, 1, 0));
                    }

                    @Override
                    public Boolean after(Path workingDir, ExecutionReport report) throws IOException {
                        report.log();

                        if (report.getErrors().isEmpty()) {
                            network.getStateManager().setWorkingState(workingStateId);

                            Map<String, String> metrics = new HashMap<>();
                            new AmplNetworkReader(new FileDataSource(workingDir, "projector_results"/*"ampl_network_"*/), network, mapper)
                                    .readGenerators()
                                    .readMetrics(metrics);

                            LOGGER.debug("Projector metrics: {}", metrics);
                        }

                        return report.getErrors().isEmpty();
                    }
                });
    }

    static class StopException extends RuntimeException {
        public StopException(String message) {
            super(message);
        }
    }

    private void reintegrateLfState(String workingStateId) {
        reintegrateLfState(workingStateId, false);
    }

    private void reintegrateLfState(String workingStateId, boolean onlyVoltage) {
        network.getStateManager().setWorkingState(workingStateId);
        for (Generator g : network.getGenerators()) {
            Terminal t = g.getTerminal();
            if (!onlyVoltage) {
                if (!Float.isNaN(t.getP())) {
                    float oldTargetP = g.getTargetP();
                    float newTargetP = -t.getP();
                    if (oldTargetP != newTargetP) {
                        g.setTargetP(newTargetP);
                        LOGGER.debug("LF result reintegration: targetP {} -> {}", oldTargetP, newTargetP);
                    }
                }
                if (!Float.isNaN(t.getQ())) {
                    float oldTargetQ = g.getTargetQ();
                    float newTargetQ = -t.getQ();
                    if (oldTargetQ != newTargetQ) {
                        g.setTargetQ(newTargetQ);
                        LOGGER.debug("LF result reintegration: targetQ {} -> {}", oldTargetQ, newTargetQ);
                    }
                }
            }
            Bus b = t.getBusView().getBus();
            if (b != null) {
                if (!Float.isNaN(b.getV())) {
                    float oldV = g.getTargetV();
                    float newV = b.getV();
                    if (oldV != newV) {
                        g.setTargetV(newV);
                        LOGGER.debug("LF result reintegration: targetV {} -> {}", oldV, newV);
                    }
                }
            }
        }
    }

    public CompletableFuture<Boolean> project(String workingStateId) throws Exception {
        return loadFlow.runAsync(workingStateId, LOAD_FLOW_PARAMETERS)
                .thenComposeAsync(loadFlowResult -> {
                    LOGGER.debug("Pre-projector load flow metrics: {}", loadFlowResult.getMetrics());
                    if (!loadFlowResult.isOk()) {
                        throw new StopException("Pre-projector load flow diverged");
                    }
                    return createAmplTask(workingStateId);
                }, computationManager.getExecutor())
                .thenComposeAsync(ok -> {
                    if (!Boolean.TRUE.equals(ok)) {
                        throw new StopException("Projector failed");
                    }
                    return loadFlow.runAsync(workingStateId, LOAD_FLOW_PARAMETERS2);
                }, computationManager.getExecutor())
                .thenAcceptAsync(loadFlowResult -> {
                    LOGGER.debug("Post-projector load flow metrics: {}", loadFlowResult.getMetrics());
                    if (!loadFlowResult.isOk()) {
                        throw new StopException("Post-projector load flow diverged");
                    }
                    reintegrateLfState(workingStateId);
                }, computationManager.getExecutor())
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
                    if (!(throwable instanceof CompletionException && throwable.getCause() instanceof StopException)) {
                        LOGGER.error(throwable.toString(), throwable);
                    }
                    return Boolean.FALSE;
                });
    }

}
