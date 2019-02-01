/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.case_projector;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.powsybl.ampl.converter.*;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.computation.*;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;

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
public final class CaseProjectorUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProjectorUtils.class);

    private static final Set<String> AMPL_MODEL_FILE_NAMES = ImmutableSet.<String>builder()
            .add("projector.run")
            .add("projector.mod")
            .add("projector.dat")
            .add("projectorOutput.run")
            .add("ampl_network_coupledgen.txt")
            .build();

    private static final String WORKING_DIR_PREFIX = "itesla_projector_";

    private static final String AMPL_GENERATORS_DOMAINS_FILE_NAME = "ampl_generators_domains.txt";

    private CaseProjectorUtils() {

    }

    protected static CompletableFuture<Boolean> createAmplTask(ComputationManager computationManager, Network network, String workingStateId, CaseProjectorConfig config) {
        return computationManager.execute(new ExecutionEnvironment(ImmutableMap.of("PATH", config.getAmplHomeDir().toString()), WORKING_DIR_PREFIX, config.isDebug()),
                new AbstractExecutionHandler<Boolean>() {

                    private StringToIntMapper<AmplSubset> mapper;

                    @Override
                    public List<CommandExecution> before(Path workingDir) throws IOException {
                        network.getVariantManager().setWorkingVariant(workingStateId);

                        // copy AMPL model
                        for (String amplModelFileName : AMPL_MODEL_FILE_NAMES) {
                            Files.copy(getClass().getResourceAsStream("/ampl/projector/" + amplModelFileName), workingDir.resolve(amplModelFileName));
                        }

                        // copy the generators domains file
                        Files.copy(config.getGeneratorsDomainsFile(), workingDir.resolve(AMPL_GENERATORS_DOMAINS_FILE_NAME));

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
                        return Collections.singletonList(new CommandExecution(command, 1, 0));
                    }

                    @Override
                    public Boolean after(Path workingDir, ExecutionReport report) throws IOException {
                        report.log();

                        if (report.getErrors().isEmpty()) {
                            network.getVariantManager().setWorkingVariant(workingStateId);

                            Map<String, String> metrics = new HashMap<>();
                            new AmplNetworkReader(new FileDataSource(workingDir, "projector_results"/*"ampl_network_"*/), network, mapper)
                                .readBuses()
                                .readGenerators()
                                .readBranches()
                                .readLoads()
                                .readPhaseTapChangers()
                                .readRatioTapChangers()
                                .readShunts()
                                .readStaticVarcompensator()
                                .readHvdcLines()
                                .readLccConverterStations()
                                .readVscConverterStations()
                                .readMetrics(metrics);

                            LOGGER.debug("Projector metrics: {}", metrics);
                        }

                        return report.getErrors().isEmpty();
                    }
                });
    }

    protected static class StopException extends RuntimeException {
        public StopException(String message) {
            super(message);
        }
    }

    protected static CompletableFuture<Boolean> project(ComputationManager computationManager, Network network, LoadFlow loadFlow, String workingStateId, CaseProjectorConfig config) throws Exception {
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        LoadFlowParameters loadFlowParameters2 = LoadFlowParameters.load().setNoGeneratorReactiveLimits(true);

        return loadFlow.run(workingStateId, loadFlowParameters)
                .thenComposeAsync(loadFlowResult -> {
                    LOGGER.debug("Pre-projector load flow metrics: {}", loadFlowResult.getMetrics());
                    if (!loadFlowResult.isOk()) {
                        throw new StopException("Pre-projector load flow diverged");
                    }
                    return createAmplTask(computationManager, network, workingStateId, config);
                }, computationManager.getExecutor())
                .thenComposeAsync(ok -> {
                    if (!Boolean.TRUE.equals(ok)) {
                        throw new StopException("Projector failed");
                    }
                    return loadFlow.run(workingStateId, loadFlowParameters2);
                }, computationManager.getExecutor())
                .thenApplyAsync(loadFlowResult -> {
                    LOGGER.debug("Post-projector load flow metrics: {}", loadFlowResult.getMetrics());
                    if (!loadFlowResult.isOk()) {
                        throw new StopException("Post-projector load flow diverged");
                    }
                    CaseProjectorUtils.reintegrateLfState(network, workingStateId);
                    return Boolean.TRUE;
                }, computationManager.getExecutor())
                .exceptionally(throwable -> {
                    if (!(throwable instanceof CompletionException && throwable.getCause() instanceof StopException)) {
                        LOGGER.error(throwable.toString(), throwable);
                    }
                    return Boolean.FALSE;
                });
    }


    protected static void reintegrateLfState(Network network, String workingStateId) {
        reintegrateLfState(network, workingStateId, false);
    }

    protected static void reintegrateLfState(Network network, String workingStateId, boolean onlyVoltage) {
        network.getVariantManager().setWorkingVariant(workingStateId);
        for (Generator g : network.getGenerators()) {
            Terminal t = g.getTerminal();
            if (!onlyVoltage) {
                if (!Double.isNaN(t.getP())) {
                    double oldTargetP = g.getTargetP();
                    double newTargetP = -t.getP();
                    if (oldTargetP != newTargetP) {
                        g.setTargetP(newTargetP);
                        LOGGER.debug("LF result reintegration: targetP {} -> {}", oldTargetP, newTargetP);
                    }
                }
                if (!Double.isNaN(t.getQ())) {
                    double oldTargetQ = g.getTargetQ();
                    double newTargetQ = -t.getQ();
                    if (oldTargetQ != newTargetQ) {
                        g.setTargetQ(newTargetQ);
                        LOGGER.debug("LF result reintegration: targetQ {} -> {}", oldTargetQ, newTargetQ);
                    }
                }
            }
            Bus b = t.getBusView().getBus();
            if (b != null) {
                if (!Double.isNaN(b.getV())) {
                    double oldV = g.getTargetV();
                    double newV = b.getV();
                    if (oldV != newV) {
                        g.setTargetV(newV);
                        LOGGER.debug("LF result reintegration: targetV {} -> {}", oldV, newV);
                    }
                }
            }
        }
    }

}
