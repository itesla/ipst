/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.ExecutionEnvironment;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.tasks.ModificationTask;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.StateManager;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import eu.itesla_project.modules.contingencies.Action;
import eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation;
import eu.itesla_project.modules.contingencies.Constraint;
import eu.itesla_project.modules.contingencies.ConstraintType;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.contingencies.impl.ActionsContingenciesAssociationImpl;
import eu.itesla_project.modules.contingencies.impl.ConstraintImpl;
import eu.itesla_project.modules.histo.HistoDbAttr;
import eu.itesla_project.modules.histo.HistoDbClient;
import eu.itesla_project.modules.histo.HistoDbHorizon;
import eu.itesla_project.modules.histo.HistoDbNetworkAttributeId;
import eu.itesla_project.modules.histo.HistoDbStats;
import eu.itesla_project.modules.histo.HistoDbStatsType;
import eu.itesla_project.modules.rules.RuleAttributeSet;
import eu.itesla_project.modules.rules.RuleId;
import eu.itesla_project.modules.rules.RulesDbClient;
import eu.itesla_project.modules.rules.SecurityRule;
import eu.itesla_project.modules.rules.SecurityRuleExpression;
import eu.itesla_project.modules.rules.SecurityRuleStatus;
import eu.itesla_project.modules.rules.expr.AndOperator;
import eu.itesla_project.modules.rules.expr.Attribute;
import eu.itesla_project.modules.rules.expr.ComparisonOperator;
import eu.itesla_project.modules.rules.expr.ExpressionNode;
import eu.itesla_project.modules.rules.expr.Litteral;
import eu.itesla_project.modules.wca.UncertaintiesAnalyserFactory;
import eu.itesla_project.modules.wca.WCA;
import eu.itesla_project.modules.wca.WCAClusterNum;
import eu.itesla_project.modules.wca.WCAParameters;
import eu.itesla_project.modules.wca.WCAResult;
import eu.itesla_project.modules.wca.report.WCAActionApplication;
import eu.itesla_project.modules.wca.report.WCAPostContingencyStatus;
import eu.itesla_project.modules.wca.report.WCAReport;
import eu.itesla_project.modules.wca.report.WCARuleViolationType;
import com.powsybl.security.LimitViolationType;
import com.powsybl.simulation.securityindexes.SecurityIndexId;
import com.powsybl.simulation.securityindexes.SecurityIndexType;
import eu.itesla_project.wca.uncertainties.UncertaintiesAnalyserFactoryTestImpl;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAImplTest {

    private Interval histoInterval;
    private Contingency contingency;
    private Action action;
    private Network network;
    private ComputationManager computationManager;
    private HistoDbClient histoDbClient;
    private RulesDbClient rulesDbClient;
    private ContingenciesAndActionsDatabaseClient contingenciesActionsDbClient;
    private UncertaintiesAnalyserFactory uncertaintiesAnalyserFactory;
    private LoadFlowFactory loadFlowFactory;

    @Before
    public void setUp() throws Exception {
        histoInterval = Interval.parse("2013-01-01T00:00:00+01:00/2013-01-31T23:59:00+01:00");

        network = EurostagTutorialExample1Factory.create();
        ((Bus) network.getIdentifiable("NHV1")).setV(380f);
        ((Bus) network.getIdentifiable("NHV2")).setV(380f);
        network.getLine("NHV1_NHV2_1").getTerminal1().setP(560f).setQ(550f);
        network.getLine("NHV1_NHV2_1").getTerminal2().setP(560f).setQ(550f);
        network.getLine("NHV1_NHV2_1").newCurrentLimits1().setPermanentLimit(1500f).add();
        network.getLine("NHV1_NHV2_1").newCurrentLimits2().setPermanentLimit(1500f).add();
        network.getLine("NHV1_NHV2_2").getTerminal1().setP(560f).setQ(550f);
        network.getLine("NHV1_NHV2_2").getTerminal2().setP(560f).setQ(550f);
        network.getLine("NHV1_NHV2_2").newCurrentLimits1().setPermanentLimit(1500f).add();
        network.getLine("NHV1_NHV2_2").newCurrentLimits2().setPermanentLimit(1500f).add();
        network.getStateManager().allowStateMultiThreadAccess(true);

        computationManager = Mockito.mock(ComputationManager.class);
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable r) {
                r.run();
            }
        };
        Mockito.when(computationManager.getExecutor()).thenReturn(executor);

        histoDbClient = Mockito.mock(HistoDbClient.class);
        HistoDbStats histoDbStats = new HistoDbStats();
        network.getLoads().forEach( load -> {
            histoDbStats.setValue(HistoDbStatsType.MIN, new HistoDbNetworkAttributeId(load.getId(), HistoDbAttr.P), load.getP0() - (load.getP0()*20/110));
            histoDbStats.setValue(HistoDbStatsType.MAX, new HistoDbNetworkAttributeId(load.getId(), HistoDbAttr.P), load.getP0() + (load.getP0()*20/110));
        });
        network.getGenerators().forEach( generator -> {
            histoDbStats.setValue(HistoDbStatsType.MIN, new HistoDbNetworkAttributeId(generator.getId(), HistoDbAttr.P), generator.getTargetP() - (generator.getTargetP()*20/110));
            histoDbStats.setValue(HistoDbStatsType.MAX, new HistoDbNetworkAttributeId(generator.getId(), HistoDbAttr.P), generator.getTargetP() + (generator.getTargetP()*20/110));
        });        
        Mockito.when(histoDbClient.queryStats(Matchers.anySet(), Matchers.eq(histoInterval), Matchers.eq(HistoDbHorizon.SN), Matchers.eq(true)))
               .thenReturn(histoDbStats);

        rulesDbClient = Mockito.mock(RulesDbClient.class);

        contingenciesActionsDbClient = Mockito.mock(ContingenciesAndActionsDatabaseClient.class);
        contingency = Mockito.mock(Contingency.class);
        Mockito.when(contingency.getId()).thenReturn("NHV1_NHV2_1_contingency");
        Mockito.when(contingency.toTask()).thenReturn(new ModificationTask() {
            @Override
            public void modify(Network network, ComputationManager computationManager) {
                network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
                network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();
                network.getLine("NHV1_NHV2_2").getTerminal1().setP(860f);
            }
        });
        action =  Mockito.mock(Action.class);
        Mockito.when(action.getId()).thenReturn("NHV1_NHV2_2_curative_action");
        Mockito.when(action.toTask()).thenReturn(new ModificationTask() {
            @Override
            public void modify(Network network, ComputationManager computationManager) {
                network.getLine("NHV1_NHV2_1").getTerminal1().connect();
                network.getLine("NHV1_NHV2_1").getTerminal2().connect();
                network.getLine("NHV1_NHV2_2").getTerminal1().setP(560f);
            }
        });
        Constraint constraint = new ConstraintImpl("NHV1_NHV2_2", 0, ConstraintType.BRANCH_OVERLOAD);
        ActionsContingenciesAssociation association = new ActionsContingenciesAssociationImpl(
                Arrays.asList(contingency.getId()), 
                Arrays.asList(constraint), 
                Arrays.asList(action.getId()));
        Mockito.when(contingenciesActionsDbClient.getContingencies(network)).thenReturn(Arrays.asList(contingency));
        Mockito.when(contingenciesActionsDbClient.getAction(action.getId(), network)).thenReturn(action);
        Mockito.when(contingenciesActionsDbClient.getActionsCtgAssociations(network)).thenReturn(Arrays.asList(association));

        uncertaintiesAnalyserFactory = new UncertaintiesAnalyserFactoryTestImpl();

        loadFlowFactory = Mockito.mock(LoadFlowFactory.class);
        LoadFlowResult loadFlowResult = new LoadFlowResult() {
            @Override
            public boolean isOk() {
                return true;
            }

            @Override
            public Map<String, String> getMetrics() {
                return Collections.emptyMap();
            }

            @Override
            public String getLogs() {
                return null;
            }
        };
        LoadFlow loadFlow = Mockito.mock(LoadFlow.class);
        Mockito.when(loadFlow.run(Matchers.any(LoadFlowParameters.class))).thenReturn(loadFlowResult);
        Mockito.when(loadFlow.runAsync(Matchers.any(String.class), Matchers.any(LoadFlowParameters.class)))
               .thenReturn(CompletableFuture.completedFuture(loadFlowResult));
        Mockito.when(loadFlowFactory.create(network, computationManager, 0)).thenReturn(loadFlow);
    }

    @Test
    public void testRunWithCurativeActions() throws Exception {
        WCAClustersResult clustersResult = new WCAClustersResult(WCAClusterNum.TWO, true, 1, Collections.emptyMap());
        Mockito.when(computationManager.execute(Mockito.any(ExecutionEnvironment.class), Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(clustersResult));

        WCAConfig config = new WCAConfig(Paths.get("/xpress-home"), 1f, true, false, EnumSet.noneOf(WCARestrictingThresholdLevel.class), 
                                         0f, true, false, WCAPreventiveActionsFilter.LF, WCAPreventiveActionsOptimizer.NONE, false, 
                                         WCACurativeActionsOptimizer.CLUSTERS, 0f, EnumSet.noneOf(Country.class), true, true, false);
        WCAParameters parameters = new WCAParameters(histoInterval, null, null, 1);
        WCA wca = new WCAImpl(network, computationManager, histoDbClient, rulesDbClient, uncertaintiesAnalyserFactory, 
                              contingenciesActionsDbClient, loadFlowFactory, config);
        WCAResult result = wca.run(parameters);
        WCAReport report = wca.getReport();
        
        assertEquals(WCAClusterNum.TWO, result.getClusters().iterator().next().getNum());
        assertTrue(result.getClusters().iterator().next().getOrigin().contains(WCAClusterOrigin.LF_POST_CONTINGENCY_VIOLATION.name()));
        assertTrue(result.getClusters().iterator().next().getOrigin().contains(WCAClusterOrigin.CLUSTERS_ANALYSIS.name()));

        assertTrue(report.getBaseStateLoadflowResult().loadflowConverged());
        assertTrue(report.getPreContingencyViolationsWithoutUncertainties().isEmpty());
        assertTrue(report.getPreContingencyViolationsWithUncertainties().isEmpty());
        assertEquals(1, report.getPostContingenciesStatus().size());
        WCAPostContingencyStatus postContingencyStatus = report.getPostContingenciesStatus().iterator().next();
        assertEquals(contingency.getId(), postContingencyStatus.getContingencyId());
        assertEquals(1, postContingencyStatus.getPostContingencyViolationsWithoutUncertainties().size());
        assertEquals("NHV1_NHV2_2", postContingencyStatus.getPostContingencyViolationsWithoutUncertainties().iterator().next().getSubjectId());
        assertEquals(LimitViolationType.CURRENT, postContingencyStatus.getPostContingencyViolationsWithoutUncertainties().iterator().next().getLimitType());
        assertEquals(1, postContingencyStatus.getPostContingencyViolationsWithUncertainties().size());
        assertEquals("NHV1_NHV2_2", postContingencyStatus.getPostContingencyViolationsWithUncertainties().iterator().next().getSubjectId());
        assertEquals(LimitViolationType.CURRENT, postContingencyStatus.getPostContingencyViolationsWithUncertainties().iterator().next().getLimitType());
        assertEquals(1, postContingencyStatus.getCurativeActionsApplication().size());
        WCAActionApplication actionApplication = postContingencyStatus.getCurativeActionsApplication().iterator().next();
        assertEquals(action.getId(), actionApplication.getActionId());
        assertTrue(actionApplication.getLoadflowResult().loadflowConverged());
        assertTrue(actionApplication.areViolationsRemoved());
        assertTrue(actionApplication.isActionApplied());
    }

    @Test
    public void testRunWithLoadflowDivergence() throws Exception {
        LoadFlowResult loadFlowResult = new LoadFlowResult() {
            @Override
            public boolean isOk() {
                return false;
            }

            @Override
            public Map<String, String> getMetrics() {
                return Collections.emptyMap();
            }

            @Override
            public String getLogs() {
                return null;
            }
        };
        LoadFlow loadFlow = Mockito.mock(LoadFlow.class);
        Mockito.when(loadFlow.run(Matchers.any(LoadFlowParameters.class)))
               .thenReturn(loadFlowResult);
        Mockito.when(loadFlow.runAsync(Matchers.any(String.class), Matchers.any(LoadFlowParameters.class)))
               .thenReturn(CompletableFuture.completedFuture(loadFlowResult));
        Mockito.when(loadFlowFactory.create(network, computationManager, 0))
               .thenReturn(loadFlow);

        WCAConfig config = new WCAConfig(Paths.get("/xpress-home"), 1f, true, false, EnumSet.noneOf(WCARestrictingThresholdLevel.class), 
                                         0f, true, false, WCAPreventiveActionsFilter.LF, WCAPreventiveActionsOptimizer.NONE, false, 
                                         WCACurativeActionsOptimizer.NONE, 0f, EnumSet.noneOf(Country.class), true, true, false);
        WCAParameters parameters = new WCAParameters(histoInterval, null, null, 1);
        WCA wca = new WCAImpl(network, computationManager, histoDbClient, rulesDbClient, uncertaintiesAnalyserFactory, 
                              contingenciesActionsDbClient, loadFlowFactory, config);
        WCAResult result = wca.run(parameters);
        WCAReport report = wca.getReport();

        assertEquals(WCAClusterNum.FOUR, result.getClusters().iterator().next().getNum());
        assertTrue(result.getClusters().iterator().next().getOrigin().contains(WCAClusterOrigin.LF_DIVERGENCE.name()));

        assertFalse(report.getBaseStateLoadflowResult().loadflowConverged());
    }

    @Test
    public void testRunWithPreventiveActions() throws Exception {
        network.getLine("NHV1_NHV2_2").getTerminal1().setP(860f);
        network.getLine("NHV1_NHV2_1").getTerminal1().setP(860f);

        Action action1 =  Mockito.mock(Action.class);
        Mockito.when(action1.getId()).thenReturn("NHV1_NHV2_2_preventive_action");
        Mockito.when(action1.toTask()).thenReturn(new ModificationTask() {
            @Override
            public void modify(Network network, ComputationManager computationManager) {
                network.getLine("NHV1_NHV2_2").getTerminal1().setP(560f);
            }
        });
        Action action2 =  Mockito.mock(Action.class);
        Mockito.when(action2.getId()).thenReturn("NHV1_NHV2_1_preventive_action");
        Mockito.when(action2.toTask()).thenReturn(new ModificationTask() {
            @Override
            public void modify(Network network, ComputationManager computationManager) {
                network.getLine("NHV1_NHV2_1").getTerminal1().setP(850f);
            }
        });
        Constraint constraint1 = new ConstraintImpl("NHV1_NHV2_2", 0, ConstraintType.BRANCH_OVERLOAD);
        Constraint constraint2 = new ConstraintImpl("NHV1_NHV2_1", 0, ConstraintType.BRANCH_OVERLOAD);
        ActionsContingenciesAssociation association1 = new ActionsContingenciesAssociationImpl(
                Collections.emptyList(), 
                Arrays.asList(constraint1), 
                Arrays.asList(action1.getId()));
        ActionsContingenciesAssociation association2 = new ActionsContingenciesAssociationImpl(
                Collections.emptyList(), 
                Arrays.asList(constraint2), 
                Arrays.asList(action2.getId()));
        Mockito.when(contingenciesActionsDbClient.getAction(action1.getId(), network)).thenReturn(action1);
        Mockito.when(contingenciesActionsDbClient.getActionsCtgAssociationsByConstraint(constraint1.getEquipment(), constraint1.getType()))
               .thenReturn(Arrays.asList(association1));
        Mockito.when(contingenciesActionsDbClient.getAction(action2.getId(), network)).thenReturn(action2);
        Mockito.when(contingenciesActionsDbClient.getActionsCtgAssociationsByConstraint(constraint2.getEquipment(), constraint2.getType()))
               .thenReturn(Arrays.asList(association2));

        WCADomainsResult domainsResult = new WCADomainsResult(true, false, 1, Collections.emptyMap());
        Mockito.when(computationManager.execute(Mockito.any(ExecutionEnvironment.class), Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(domainsResult));

        WCAConfig config = new WCAConfig(Paths.get("/xpress-home"), 1f, true, false, EnumSet.noneOf(WCARestrictingThresholdLevel.class), 
                                         0f, true, false, WCAPreventiveActionsFilter.DOMAINS, WCAPreventiveActionsOptimizer.DOMAINS, true, 
                                         WCACurativeActionsOptimizer.NONE, 0f, EnumSet.noneOf(Country.class), true, true, true);
        WCAParameters parameters = new WCAParameters(histoInterval, null, null, 1);
        WCA wca = new WCAImpl(network, computationManager, histoDbClient, rulesDbClient, uncertaintiesAnalyserFactory, 
                              contingenciesActionsDbClient, loadFlowFactory, config);
        WCAResult result = wca.run(parameters);
        WCAReport report = wca.getReport();
        
        assertTrue(result.getClusters().iterator().next().getOrigin().contains(WCAClusterOrigin.LF_BASIC_VIOLATION.name()));
        assertTrue(result.getClusters().iterator().next().getOrigin().contains(WCAClusterOrigin.DOMAINS_BASIC_VIOLATION.name()));
        assertTrue(result.getClusters().iterator().next().getOrigin().contains(WCAClusterOrigin.DOMAINS_SPECIFIC_PREVENTIVE_ACTION_FOUND.name()));

        assertTrue(report.getBaseStateLoadflowResult().loadflowConverged());
        assertEquals(2, report.getPreContingencyViolationsWithoutUncertainties().size());
        report.getPreContingencyViolationsWithoutUncertainties().forEach( violation -> 
        {
            assertTrue(Arrays.asList("NHV1_NHV2_2","NHV1_NHV2_1").contains(violation.getSubjectId()));
            assertEquals(LimitViolationType.CURRENT, violation.getLimitType());
        });
        assertEquals(2, report.getPreContingencyViolationsWithUncertainties().size());
        report.getPreContingencyViolationsWithUncertainties().forEach( violation -> 
        {
            assertTrue(Arrays.asList("NHV1_NHV2_2","NHV1_NHV2_1").contains(violation.getSubjectId()));
            assertEquals(LimitViolationType.CURRENT, violation.getLimitType());
        });
        assertEquals(2, report.getPreventiveActionsApplication().size());
        report.getPreventiveActionsApplication().forEach( actionApplication -> 
        {
            assertTrue(Arrays.asList(action1.getId(),action2.getId()).contains(actionApplication.getActionId()));
            assertEquals(LimitViolationType.CURRENT, actionApplication.getViolation().getLimitType());
            if ( action1.getId().equals(actionApplication.getActionId()) ) {
                assertEquals("NHV1_NHV2_2", actionApplication.getViolation().getSubjectId());
                assertTrue(actionApplication.areViolationsRemoved());
                assertTrue(actionApplication.isActionApplied());
            }
            if ( action2.getId().equals(actionApplication.getActionId()) ) {
                assertEquals("NHV1_NHV2_1", actionApplication.getViolation().getSubjectId());
                assertFalse(actionApplication.areViolationsRemoved());
                assertFalse(actionApplication.isActionApplied());
            }
        });
        assertEquals(1, report.getPostPreventiveActionsViolationsWithUncertainties().size());
        assertEquals("NHV1_NHV2_1", report.getPostPreventiveActionsViolationsWithUncertainties().iterator().next().getSubjectId());
        assertEquals(LimitViolationType.CURRENT, report.getPostPreventiveActionsViolationsWithUncertainties().iterator().next().getLimitType());
    }

    @Test
    public void testRunWithPostcontingencyLoadflowDivergence() throws Exception {
        LoadFlowResult loadFlowResult = new LoadFlowResult() {
            @Override
            public boolean isOk() {
                if ( StateManager.INITIAL_STATE_ID.equals(network.getStateManager().getWorkingStateId()) )
                    return true;
                return false;
            }

            @Override
            public Map<String, String> getMetrics() {
                return Collections.emptyMap();
            }

            @Override
            public String getLogs() {
                return null;
            }
        };
        LoadFlow loadFlow = Mockito.mock(LoadFlow.class);
        Mockito.when(loadFlow.run(Matchers.any(LoadFlowParameters.class)))
               .thenReturn(loadFlowResult);
        Mockito.when(loadFlow.runAsync(Matchers.any(String.class), Matchers.any(LoadFlowParameters.class)))
               .thenReturn(CompletableFuture.completedFuture(loadFlowResult));
        Mockito.when(loadFlowFactory.create(network, computationManager, 0))
               .thenReturn(loadFlow);

        WCAConfig config = new WCAConfig(Paths.get("/xpress-home"), 1f, true, false, EnumSet.noneOf(WCARestrictingThresholdLevel.class), 
                                         0f, true, false, WCAPreventiveActionsFilter.LF, WCAPreventiveActionsOptimizer.NONE, false, 
                                         WCACurativeActionsOptimizer.NONE, 0f, EnumSet.noneOf(Country.class), true, true, false);
        WCAParameters parameters = new WCAParameters(histoInterval, null, null, 1);
        WCA wca = new WCAImpl(network, computationManager, histoDbClient, rulesDbClient, uncertaintiesAnalyserFactory, 
                              contingenciesActionsDbClient, loadFlowFactory, config);
        WCAResult result = wca.run(parameters);
        WCAReport report = wca.getReport();

        assertEquals(WCAClusterNum.FOUR, result.getClusters().iterator().next().getNum());
        assertTrue(result.getClusters().iterator().next().getOrigin().contains(WCAClusterOrigin.LF_POST_CONTINGENCY_DIVERGENCE.name()));

        assertTrue(report.getBaseStateLoadflowResult().loadflowConverged());
        assertTrue(report.getPreContingencyViolationsWithoutUncertainties().isEmpty());
        assertTrue(report.getPreContingencyViolationsWithUncertainties().isEmpty());
        assertFalse(report.getPostContingenciesStatus().iterator().next().getPostContingencyLoadflowResult().loadflowConverged());
    }
    
    @Test
    public void testRunWithSecurityRules() throws Exception {
        String workflowId = "workflowId";
        RuleId ruleId = new RuleId(RuleAttributeSet.WORST_CASE, new SecurityIndexId(contingency.getId(), SecurityIndexType.TSO_OVERLOAD));
        ExpressionNode condition = new AndOperator(
                new ComparisonOperator(
                        new Attribute(new HistoDbNetworkAttributeId("LOAD", HistoDbAttr.P)),
                        new Litteral(500d),
                        ComparisonOperator.Type.GREATER_EQUAL), 
                new ComparisonOperator(
                        new Attribute(new HistoDbNetworkAttributeId("GEN", HistoDbAttr.Q)),
                        new Litteral(400d),
                        ComparisonOperator.Type.LESS));
        SecurityRule rule = Mockito.mock(SecurityRule.class);
        Mockito.when(rule.getId()).thenReturn(ruleId);
        Mockito.when(rule.getWorkflowId()).thenReturn(workflowId);
        Mockito.when(rule.toExpression(Mockito.anyFloat())).thenReturn(new SecurityRuleExpression(ruleId, SecurityRuleStatus.SECURE_IF, condition));
        Mockito.when(rulesDbClient.getRules(workflowId, RuleAttributeSet.WORST_CASE, contingency.getId(), SecurityIndexType.TSO_OVERLOAD))
               .thenReturn(Collections.singletonList(rule));
        Mockito.when(rulesDbClient.getRules(workflowId, RuleAttributeSet.MONTE_CARLO, contingency.getId(), SecurityIndexType.TSO_OVERLOAD))
               .thenReturn(Collections.emptyList());

        WCADomainsResult domainsResult = new WCADomainsResult(false, true, 0, Collections.emptyMap());
        Mockito.when(computationManager.execute(Mockito.any(ExecutionEnvironment.class), Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(domainsResult));

        WCAConfig config = new WCAConfig(Paths.get("/xpress-home"), 1f, true, false, EnumSet.noneOf(WCARestrictingThresholdLevel.class), 
                                         0f, true, false, WCAPreventiveActionsFilter.LF, WCAPreventiveActionsOptimizer.NONE, false, 
                                         WCACurativeActionsOptimizer.NONE, 0f, EnumSet.noneOf(Country.class), true, true, false);
        WCAParameters parameters = new WCAParameters(histoInterval, workflowId, Collections.singleton(SecurityIndexType.TSO_OVERLOAD), 1);
        WCA wca = new WCAImpl(network, computationManager, histoDbClient, rulesDbClient, uncertaintiesAnalyserFactory, 
                              contingenciesActionsDbClient, loadFlowFactory, config);
        WCAResult result = wca.run(parameters);
        WCAReport report = wca.getReport();

        assertTrue(result.getClusters().iterator().next().getOrigin().contains(WCAClusterOrigin.LF_RULE_VIOLATION.name()));
        assertTrue(result.getClusters().iterator().next().getOrigin().contains(WCAClusterOrigin.DOMAINS_RULE_VIOLATION.name()));

        assertEquals(2, report.getSecurityRulesApplication().size());
        report.getSecurityRulesApplication().forEach( ruleApplication -> 
        {
            assertEquals(contingency.getId(), ruleApplication.getContingencyId());
            if ( ruleApplication.getSecurityRule() != null ) {
                assertEquals(ruleId, ruleApplication.getSecurityRule().getId());
                assertEquals(workflowId, ruleApplication.getSecurityRule().getWorkflowId());
                assertFalse(ruleApplication.isRuleViolated());
                assertEquals(WCARuleViolationType.NO_VIOLATION, ruleApplication.getRuleViolationType());
            } else {
                assertTrue(ruleApplication.isRuleViolated());
                assertEquals(WCARuleViolationType.MISSING_RULE, ruleApplication.getRuleViolationType());
            }
        });
    }

}

