/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.powsybl.iidm.network.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.BranchContingency;
import eu.itesla_project.modules.contingencies.Action;
import eu.itesla_project.modules.contingencies.ActionPlan;
import eu.itesla_project.modules.contingencies.ActionPlanOption;
import eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation;
import eu.itesla_project.modules.contingencies.Constraint;
import eu.itesla_project.modules.contingencies.ConstraintType;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.contingencies.SwitchClosingAction;
import eu.itesla_project.modules.contingencies.UnaryOperator;
import eu.itesla_project.modules.contingencies.impl.ActionImpl;
import eu.itesla_project.modules.contingencies.impl.ActionPlanImpl;
import eu.itesla_project.modules.contingencies.impl.ActionsContingenciesAssociationImpl;
import eu.itesla_project.modules.contingencies.impl.ConstraintImpl;
import eu.itesla_project.modules.contingencies.impl.LogicalExpressionImpl;
import eu.itesla_project.modules.contingencies.impl.OptionImpl;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;
import eu.itesla_project.wca.ContingencyDbFacade;
import eu.itesla_project.wca.SimpleContingencyDbFacade;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleContingencyDbFacadeTest {

    private static LimitViolation createMockLimitViolation(String lineId) {
        return new LimitViolation(lineId, LimitViolationType.CURRENT,null, 0, Float.NaN, Float.NaN, Float.NaN, Branch.Side.ONE);
    }

    @Test
    public void testGetContingencies() throws Exception {

        // network
        Network mockNetwork = NetworkFactory.create("mockNetwork", "test");
        // contingency
        Contingency mockContingency = new Contingency("mockContingency", new BranchContingency("mockLine"));
        List<Contingency> mockContingencies = Arrays.asList(mockContingency);
        // contingency and actions db client
        ContingenciesAndActionsDatabaseClient mockContingenciesActionsDbClient = Mockito.mock(ContingenciesAndActionsDatabaseClient.class);
        Mockito.when(mockContingenciesActionsDbClient.getContingencies(mockNetwork)).thenReturn(mockContingencies);

        ContingencyDbFacade contingencyDbFacade = new SimpleContingencyDbFacade(mockContingenciesActionsDbClient, mockNetwork);
        assertEquals(mockContingencies, contingencyDbFacade.getContingencies());

    }

    @Test
    public void testGetCurativeActions() throws Exception {

        // network
        Network mockNetwork = NetworkFactory.create("mockNetwork", "test");
        // contingency
        String mockContingencyId = "mockContingency";
        Contingency mockContingency = new Contingency(mockContingencyId, new BranchContingency("mockLine"));
        // action
        String mockActionId = "mockAction";
        Action mockAction = new ActionImpl(mockActionId, true, true, new SwitchClosingAction("mockVoltageLevel", "mockSwitch"));
        // constraint
        Constraint mockConstraint = new ConstraintImpl("mockline", 0, ConstraintType.BRANCH_OVERLOAD);
        // association
        ActionsContingenciesAssociation mockAssociation = new ActionsContingenciesAssociationImpl(
                Arrays.asList(mockContingencyId), 
                Arrays.asList(mockConstraint), 
                Arrays.asList(mockActionId));
        // contingency and actions db client
        ContingenciesAndActionsDatabaseClient mockContingenciesActionsDbClient = Mockito.mock(ContingenciesAndActionsDatabaseClient.class);
        Mockito.when(mockContingenciesActionsDbClient.getActionsCtgAssociations(mockNetwork)).thenReturn(Arrays.asList(mockAssociation));
        Mockito.when(mockContingenciesActionsDbClient.getAction(mockActionId, mockNetwork)).thenReturn(mockAction);

        ContingencyDbFacade contingencyDbFacade = new SimpleContingencyDbFacade(mockContingenciesActionsDbClient, mockNetwork);
        assertEquals(Arrays.asList(Collections.singletonList(mockAction)), contingencyDbFacade.getCurativeActions(mockContingency, null));

    }

    @Test
    public void testGetCurativeActionsWithActionPlans() throws Exception {

        // network
        Network mockNetwork = NetworkFactory.create("mockNetwork", "test");
        // contingency
        String mockContingencyId = "mockContingency";
        Contingency mockContingency = new Contingency(mockContingencyId, new BranchContingency("mockLine"));
        // action
        String mockActionId = "mockAction";
        Action mockAction = new ActionImpl(mockActionId, true, true, new SwitchClosingAction("mockVoltageLevel", "mockSwitch"));
        // action plan
        Map<BigInteger, String> mockOptionActions =  ImmutableMap.of(new BigInteger("1"), mockActionId);
        LogicalExpressionImpl mockLogicalExpression = new LogicalExpressionImpl();
        mockLogicalExpression.setOperator(new UnaryOperator(mockActionId));
        ActionPlanOption mockActionPlanOption = new OptionImpl(new BigInteger("1"), mockLogicalExpression, mockOptionActions);
        Map<BigInteger, ActionPlanOption> mockPriorityOptions = ImmutableMap.of(new BigInteger("1"), mockActionPlanOption);
        String mockActionPlanId = "mockActionPlan";
        ActionPlan mockActionPlan = new ActionPlanImpl(mockActionPlanId, "", Collections.emptyList(), mockPriorityOptions);
        // constraint
        Constraint mockConstraint = new ConstraintImpl("mockline", 0, ConstraintType.BRANCH_OVERLOAD);
        // association
        ActionsContingenciesAssociation mockAssociation = new ActionsContingenciesAssociationImpl(
                Arrays.asList(mockContingencyId), 
                Arrays.asList(mockConstraint), 
                Arrays.asList(mockActionPlanId));
        // contingency and actions db client
        ContingenciesAndActionsDatabaseClient mockContingenciesActionsDbClient = Mockito.mock(ContingenciesAndActionsDatabaseClient.class);
        Mockito.when(mockContingenciesActionsDbClient.getActionsCtgAssociations(mockNetwork)).thenReturn(Arrays.asList(mockAssociation));
        Mockito.when(mockContingenciesActionsDbClient.getAction(mockActionPlanId, mockNetwork)).thenReturn(null);
        Mockito.when(mockContingenciesActionsDbClient.getActionPlan(mockActionPlanId)).thenReturn(mockActionPlan);
        Mockito.when(mockContingenciesActionsDbClient.getAction(mockActionId, mockNetwork)).thenReturn(mockAction);

        ContingencyDbFacade contingencyDbFacade = new SimpleContingencyDbFacade(mockContingenciesActionsDbClient, mockNetwork);
        assertEquals(Arrays.asList(Collections.singletonList(mockAction)), contingencyDbFacade.getCurativeActions(mockContingency, null));

    }

    @Test
    public void testGetCurativeActionsWithViolations() throws Exception {

        // network
        Network mockNetwork = NetworkFactory.create("mockNetwork", "test");
        // line
        String mockLineId = "mockline";
        mockNetwork.newSubstation()
        .setId("mockSubstation1")
        .setCountry(Country.FR)
        .add()
        .newVoltageLevel()
        .setId("mockVoltageLevel1")
        .setNominalV(380)
        .setTopologyKind(TopologyKind.BUS_BREAKER)
        .setHighVoltageLimit(400)
        .setLowVoltageLimit(300)
        .add()
        .getBusBreakerView()
        .newBus()
        .setId("mockBus1")
        .add();
        mockNetwork.newSubstation()
        .setId("mockSubstation2")
        .setCountry(Country.FR)
        .add()
        .newVoltageLevel()
        .setId("mockVoltageLevel2")
        .setNominalV(380)
        .setTopologyKind(TopologyKind.BUS_BREAKER)
        .setHighVoltageLimit(400)
        .setLowVoltageLimit(300)
        .add().getBusBreakerView()
        .newBus()
        .setId("mockBus2")
        .add();
        Line mockLine = mockNetwork.newLine()
                .setId(mockLineId)
                .setVoltageLevel1("mockVoltageLevel1")
                .setBus1("mockBus1")
                .setConnectableBus1("mockBus1")
                .setVoltageLevel2("mockVoltageLevel2")
                .setBus2("mockBus2")
                .setConnectableBus2("mockBus2")
                .setR(0)
                .setX(0)
                .setG1(0)
                .setB1(0)
                .setG2(0)
                .setB2(0)
                .add();
        // limit violation
        LimitViolation mockLimitViolation = createMockLimitViolation(mockLineId);
        List<LimitViolation> mockLimitViolations = Arrays.asList(mockLimitViolation);
        // contingency
        String mockContingencyId = "mockContingency";
        Contingency mockContingency = new Contingency(mockContingencyId, new BranchContingency("mockLine"));
        // action
        String mockActionId = "mockAction";
        Action mockAction = new ActionImpl(mockActionId, true, true, new SwitchClosingAction("mockVoltageLevel", "mockSwitch"));
        // constraint
        Constraint mockConstraint = new ConstraintImpl(mockLineId, 0, ConstraintType.BRANCH_OVERLOAD);
        // association
        ActionsContingenciesAssociation mockAssociation = new ActionsContingenciesAssociationImpl(
                Arrays.asList(mockContingencyId), 
                Arrays.asList(mockConstraint), 
                Arrays.asList(mockActionId));
        // contingency and actions db client
        ContingenciesAndActionsDatabaseClient mockContingenciesActionsDbClient = Mockito.mock(ContingenciesAndActionsDatabaseClient.class);
        Mockito.when(mockContingenciesActionsDbClient.getActionsCtgAssociations(mockNetwork)).thenReturn(Arrays.asList(mockAssociation));
        Mockito.when(mockContingenciesActionsDbClient.getAction(mockActionId, mockNetwork)).thenReturn(mockAction);

        ContingencyDbFacade contingencyDbFacade = new SimpleContingencyDbFacade(mockContingenciesActionsDbClient, mockNetwork);
        assertEquals(Arrays.asList(Collections.singletonList(mockAction)), contingencyDbFacade.getCurativeActions(mockContingency, mockLimitViolations));

    }

    @Test
    public void testGetPreventiveActions() throws Exception {

        // network
        Network mockNetwork = NetworkFactory.create("mockNetwork", "test");
        // line
        String mockLineId = "mockline";
        mockNetwork.newSubstation()
        .setId("mockSubstation1")
        .setCountry(Country.FR)
        .add()
        .newVoltageLevel()
        .setId("mockVoltageLevel1")
        .setNominalV(380)
        .setTopologyKind(TopologyKind.BUS_BREAKER)
        .setHighVoltageLimit(400)
        .setLowVoltageLimit(300)
        .add()
        .getBusBreakerView()
        .newBus()
        .setId("mockBus1")
        .add();
        mockNetwork.newSubstation()
        .setId("mockSubstation2")
        .setCountry(Country.FR)
        .add()
        .newVoltageLevel()
        .setId("mockVoltageLevel2")
        .setNominalV(380)
        .setTopologyKind(TopologyKind.BUS_BREAKER)
        .setHighVoltageLimit(400)
        .setLowVoltageLimit(300)
        .add().getBusBreakerView()
        .newBus()
        .setId("mockBus2")
        .add();
        Line mockLine = mockNetwork.newLine()
                .setId(mockLineId)
                .setVoltageLevel1("mockVoltageLevel1")
                .setBus1("mockBus1")
                .setConnectableBus1("mockBus1")
                .setVoltageLevel2("mockVoltageLevel2")
                .setBus2("mockBus2")
                .setConnectableBus2("mockBus2")
                .setR(0)
                .setX(0)
                .setG1(0)
                .setB1(0)
                .setG2(0)
                .setB2(0)
                .add();
        // limit violation
        LimitViolation mockLimitViolation = createMockLimitViolation(mockLineId);
        // action
        String mockActionId = "mockAction";
        Action mockAction = new ActionImpl(mockActionId, true, true, new SwitchClosingAction("mockVoltageLevel", "mockSwitch"));
        // constraint
        Constraint mockConstraint = new ConstraintImpl(mockLineId, 0, ConstraintType.BRANCH_OVERLOAD);
        // association
        ActionsContingenciesAssociation mockAssociation = new ActionsContingenciesAssociationImpl(
                Collections.emptyList(), 
                Arrays.asList(mockConstraint), 
                Arrays.asList(mockActionId));
        // contingency and actions db client
        ContingenciesAndActionsDatabaseClient mockContingenciesActionsDbClient = Mockito.mock(ContingenciesAndActionsDatabaseClient.class);
        Mockito.when(mockContingenciesActionsDbClient.getActionsCtgAssociationsByConstraint(mockLineId, ConstraintType.BRANCH_OVERLOAD))
        .thenReturn(Arrays.asList(mockAssociation));
        Mockito.when(mockContingenciesActionsDbClient.getAction(mockActionId, mockNetwork)).thenReturn(mockAction);

        ContingencyDbFacade contingencyDbFacade = new SimpleContingencyDbFacade(mockContingenciesActionsDbClient, mockNetwork);
        assertEquals(Arrays.asList(Collections.singletonList(mockAction)), contingencyDbFacade.getPreventiveActions(mockLimitViolation));

    }

}
