/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pclfsim;

import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.tasks.ModificationTask;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.mock.LoadFlowFactoryMock;
import com.powsybl.security.Security;
import com.powsybl.simulation.ImpactAnalysis;
import com.powsybl.simulation.ImpactAnalysisResult;
import com.powsybl.simulation.SimulationParameters;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class PostContLoadFlowSimTest {

    @BeforeClass
    public static void init() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR");
    }

    private Network createNetwork() {
        Network network = EurostagTutorialExample1Factory.create();
        network.getVariantManager().allowVariantMultiThreadAccess(true);
        ((Bus) network.getIdentifiable("NHV1")).setV(380.0);
        ((Bus) network.getIdentifiable("NHV2")).setV(380.0);
        network.getLine("NHV1_NHV2_1").getTerminal1().setP(560.0).setQ(550.0);
        network.getLine("NHV1_NHV2_1").getTerminal2().setP(560.0).setQ(550.0);
        network.getLine("NHV1_NHV2_1").newCurrentLimits1().setPermanentLimit(1500.0).add();
        network.getLine("NHV1_NHV2_1").newCurrentLimits2()
                .setPermanentLimit(1200.0)
                .beginTemporaryLimit()
                .setName("10'")
                .setAcceptableDuration(10 * 60)
                .setValue(1300.0)
                .endTemporaryLimit()
                .add();
        return network;
    }

    private ContingenciesProvider createContingenciesProvider(Network network, int contingenciesNumber) {
        Objects.requireNonNull(network);
        ContingenciesProvider contingenciesProvider = Mockito.mock(ContingenciesProvider.class);
        List<Contingency> contingencies = IntStream.range(1, contingenciesNumber).mapToObj(i -> {
            Contingency cnt = Mockito.mock(Contingency.class);
            Mockito.when(cnt.getId()).thenReturn("NHV1_NHV2_2_contingency_" + i);
            Mockito.when(cnt.getElements()).thenReturn(Collections.singletonList(new BranchContingency("NHV1_NHV2_2")));
            Mockito.when(cnt.toTask()).thenReturn(new ModificationTask() {
                @Override
                public void modify(Network network, ComputationManager computationManager) {
                    network.getLine("NHV1_NHV2_2").getTerminal1().disconnect();
                    network.getLine("NHV1_NHV2_2").getTerminal2().disconnect();
                    network.getLine("NHV1_NHV2_1").getTerminal2().setP(600.0);
                }
            });
            return cnt;
        }).collect(Collectors.toList());
        Mockito.when(contingenciesProvider.getContingencies(network)).thenReturn(contingencies);
        return contingenciesProvider;

    }

    @Test
    public void impactAnalysis() throws Exception {
        Network network = createNetwork();
        ContingenciesProvider contingenciesProvider = createContingenciesProvider(network, 500);

        LoadFlowFactory loadflowFactory = new LoadFlowFactoryMock();

        ComputationManager computationManager = Mockito.mock(ComputationManager.class);
        Executor executor = Executors.newCachedThreadPool();
        Mockito.when(computationManager.getExecutor()).thenReturn(executor);


        SimulationParameters simulationParameters = new SimulationParameters(5, 7, 0.8, 0.8, 0.3, 9);
        PostContLoadFlowSimConfig config = new PostContLoadFlowSimConfig(LoadFlowFactoryMock.class, false, false, 0, Security.CurrentLimitType.PATL, 0.5f);

        ImpactAnalysis impactAnalysis = new PostContLoadFlowSimImpactAnalysis(network, computationManager, 1, contingenciesProvider, config, loadflowFactory);
        impactAnalysis.init(simulationParameters, new HashMap<>());
        ImpactAnalysisResult result = impactAnalysis.runAsync(new PostContLoadFlowSimState(VariantManagerConstants.INITIAL_VARIANT_ID, Collections.EMPTY_LIST), null, null).join();

        assertNotNull(result);
        assertTrue(result.getMetrics().size() > 0);
    }
}
