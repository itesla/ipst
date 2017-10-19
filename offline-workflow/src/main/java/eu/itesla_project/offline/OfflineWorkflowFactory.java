/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.offline;

import com.powsybl.computation.ComputationManager;
import com.powsybl.loadflow.LoadFlowFactory;
import eu.itesla_project.merge.MergeOptimizerFactory;
import eu.itesla_project.modules.OptimizerFactory;
import eu.itesla_project.cases.CaseRepository;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClientFactory;
import eu.itesla_project.modules.histo.HistoDbClientFactory;
import eu.itesla_project.modules.offline.MetricsDb;
import eu.itesla_project.modules.offline.OfflineDb;
import eu.itesla_project.modules.offline.OfflineWorkflowCreationParameters;
import eu.itesla_project.modules.rules.RulesBuilder;
import eu.itesla_project.modules.sampling.SamplerFactory;
import com.powsybl.simulation.SimulatorFactory;
import eu.itesla_project.modules.topo.TopologyMinerFactory;
import eu.itesla_project.modules.validation.ValidationDb;

import java.util.concurrent.ExecutorService;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface OfflineWorkflowFactory {

    OfflineWorkflow create(String id, OfflineWorkflowCreationParameters creationParameters, ComputationManager computationManager,
                           ContingenciesAndActionsDatabaseClientFactory cadbClientFactory,
                           HistoDbClientFactory histoDbClientFactory, TopologyMinerFactory topologyMinerFactory,
                           RulesBuilder rulesBuilder, OfflineDb offlineDb, ValidationDb validationDb, CaseRepository caseRepository,
                           SamplerFactory samplerFactory, LoadFlowFactory loadFlowFactory, OptimizerFactory optimizerFactory,
                           SimulatorFactory simulatorFactory, MergeOptimizerFactory mergeOptimizerFactory, MetricsDb metricsDb, ExecutorService executorService);
}
