/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online;

import com.powsybl.computation.ComputationManager;
import com.powsybl.loadflow.LoadFlowFactory;
import eu.itesla_project.merge.MergeOptimizerFactory;
import eu.itesla_project.cases.CaseRepository;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.mcla.ForecastErrorsDataStorage;
import eu.itesla_project.modules.mcla.MontecarloSamplerFactory;
import eu.itesla_project.modules.online.OnlineDb;
import eu.itesla_project.modules.online.OnlineWorkflowParameters;
import eu.itesla_project.modules.online.RulesFacadeFactory;
import eu.itesla_project.modules.optimizer.CorrectiveControlOptimizerFactory;
import eu.itesla_project.modules.rules.RulesDbClient;
import com.powsybl.simulation.SimulatorFactory;
import eu.itesla_project.modules.wca.UncertaintiesAnalyserFactory;
import eu.itesla_project.modules.wca.WCAFactory;

/**
*
* @author Quinary <itesla@quinary.com>
*/
public interface OnlineWorkflowFactory {

    OnlineWorkflow create(ComputationManager computationManager,
            ContingenciesAndActionsDatabaseClient cadbClient,
            HistoDbClient histoDbClient, RulesDbClient rulesDbClient, WCAFactory wcaFactory,
            LoadFlowFactory loadFlowFactory, ForecastErrorsDataStorage feDataStorage, OnlineDb onlineDB,
            UncertaintiesAnalyserFactory uncertaintiesAnalyserFactory,
            CorrectiveControlOptimizerFactory optimizerFactory, SimulatorFactory simulatorFactory,
            CaseRepository caseRepository, MontecarloSamplerFactory montecarloSamplerFactory,
            MergeOptimizerFactory mergeOptimizerFactory, RulesFacadeFactory rulesFacadeFactory,
            OnlineWorkflowParameters parameters, OnlineWorkflowStartParameters startParameters);

}
