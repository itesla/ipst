/**
 * Copyright (c) 2016-2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.actions_contingencies.xml.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Test;
import org.xml.sax.SAXException;

import eu.itesla_project.contingency.Contingency;
import eu.itesla_project.contingency.ContingencyElement;
import eu.itesla_project.contingency.ContingencyElementType;
import eu.itesla_project.iidm.actions_contingencies.xml.XmlFileContingenciesAndActionsDatabaseClientFactory;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.modules.contingencies.Action;
import eu.itesla_project.modules.contingencies.ActionElement;
import eu.itesla_project.modules.contingencies.ActionElementType;
import eu.itesla_project.modules.contingencies.ActionPlan;
import eu.itesla_project.modules.contingencies.ActionPlanOption;
import eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation;
import eu.itesla_project.modules.contingencies.ConstraintType;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.contingencies.Zone;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class ACXmlClientTest {

    @Test
    public void test() throws JAXBException, SAXException, IOException, URISyntaxException  {

        URL testActionsUrl = getClass().getResource("/test-ac.xml");
        ContingenciesAndActionsDatabaseClient cadbClient = new XmlFileContingenciesAndActionsDatabaseClientFactory().create(Paths.get(testActionsUrl.toURI()));
        Network network = ACXmlClientTestUtils.getNetwork();

        // Collection<Zone> getZones();
        Collection<Zone> zones = cadbClient.getZones();
        assertEquals(1, zones.size());
        checkZone(zones.iterator().next(), BigInteger.ONE, "ZONE1", "zone one", 2);
        // Collection<Zone> getZones(Network network);
        zones = cadbClient.getZones(network);
        assertEquals(1, zones.size());
        checkZone(zones.iterator().next(), BigInteger.ONE, "ZONE1", "zone one", 2);
        // Zone getZone(String id);
        Zone zone = cadbClient.getZone("ZONE1");
        checkZone(zone, BigInteger.ONE, "ZONE1", "zone one", 2);

        // List<Contingency> getContingencies(Network network);
        List<Contingency> contingencies = cadbClient.getContingencies(network);
        assertEquals(1, contingencies.size());
        checkContingency(contingencies.iterator().next(), "N-1_Contingency", 1, "LINE1_ACLS", ContingencyElementType.BRANCH);
        // Contingency getContingency(String id, Network network);
        Contingency contingency = cadbClient.getContingency("N-1_Contingency", network);
        checkContingency(contingency, "N-1_Contingency", 1, "LINE1_ACLS", ContingencyElementType.BRANCH);

        // Collection<Action> getActions(Network network);
        Collection<Action> actions = cadbClient.getActions(network);
        assertEquals(2, actions.size());
        checkAction(actions.iterator().next(), "Action1", 120, 1, "LINE1_ACLS", ActionElementType.LINE_TRIPPING);
        // Action getAction(String id, Network network);
        Action action = cadbClient.getAction("Action1", network);
        checkAction(action, "Action1", 120, 1, "LINE1_ACLS", ActionElementType.LINE_TRIPPING);

        // Collection<ActionPlan> getActionPlans();
        Collection<ActionPlan> actionPlans = cadbClient.getActionPlans();
        assertEquals(1, actionPlans.size());
        checkActionPlan(actionPlans.iterator().next(), "Plan1", 1, 2, "Action1");
        // Collection<ActionPlaactionPlans = client.getActionPlans();
        actionPlans = cadbClient.getActionPlans(network);
        assertEquals(1, actionPlans.size());
        checkActionPlan(actionPlans.iterator().next(), "Plan1", 1, 2, "Action1");
        // ActionPlan getActionPlan(String id);
        ActionPlan actionPlan = cadbClient.getActionPlan("Plan1");
        checkActionPlan(actionPlan, "Plan1", 1, 2, "Action1");

        // Collection<ActionsContingenciesAssociation> getActionsCtgAssociations();
        Collection<ActionsContingenciesAssociation> actionCtgAssociations = cadbClient.getActionsCtgAssociations();
        assertEquals(2, actionCtgAssociations.size());
        checkAssociation(actionCtgAssociations.iterator().next(), 1, "N-1_Contingency", "Action2", "LINE1_ACLS", ConstraintType.BRANCH_OVERLOAD);
        // List<ActionsContingenciesAssociation> getActionsCtgAssociations(Network network);
        actionCtgAssociations = cadbClient.getActionsCtgAssociations(network);
        assertEquals(2, actionCtgAssociations.size());
        checkAssociation(actionCtgAssociations.iterator().next(), 1, "N-1_Contingency", "Action2", "LINE1_ACLS", ConstraintType.BRANCH_OVERLOAD);
        // Collection<ActionsContingenciesAssociation> getActionsCtgAssociationsByContingency(String contingencyId);
        actionCtgAssociations = cadbClient.getActionsCtgAssociationsByContingency("N-1_Contingency");
        assertEquals(1, actionCtgAssociations.size());
        checkAssociation(actionCtgAssociations.iterator().next(), 1, "N-1_Contingency", "Action2", "LINE1_ACLS", ConstraintType.BRANCH_OVERLOAD);
        // Collection<ActionsContingenciesAssociation> getActionsCtgAssociationsByConstraint(String equipmentId, ConstraintType constraintType);
        actionCtgAssociations = cadbClient.getActionsCtgAssociationsByConstraint("LINE1_ACLS", ConstraintType.BRANCH_OVERLOAD);
        assertEquals(1, actionCtgAssociations.size());
        checkAssociation(actionCtgAssociations.iterator().next(), 1, "N-1_Contingency", "Action2", "LINE1_ACLS", ConstraintType.BRANCH_OVERLOAD);
        actionCtgAssociations = cadbClient.getActionsCtgAssociationsByConstraint("LINE2_ACLS", ConstraintType.BRANCH_OVERLOAD);
        assertEquals(1, actionCtgAssociations.size());
        checkAssociation(actionCtgAssociations.iterator().next(), 0, null, "Action1", "LINE2_ACLS", ConstraintType.BRANCH_OVERLOAD);
    }

    private void checkZone(Zone zone, BigInteger number, String name, String description, int numVoltageLevels) {
        assertNotNull(zone);
        assertEquals(number, zone.getNumber());
        assertEquals(name, zone.getName());
        assertEquals(description, zone.getDescription());
        assertEquals(numVoltageLevels, zone.getVoltageLevels().size());
    }

    private void checkContingency(Contingency contingency, String contingencyId, int numEquipments, String equipmentId, ContingencyElementType equipmentType) {
        assertNotNull(contingency);
        assertEquals(contingencyId, contingency.getId());
        assertEquals(numEquipments, contingency.getElements().size());
        ContingencyElement contingencyElement = contingency.getElements().iterator().next();
        assertEquals(equipmentId, contingencyElement.getId());
        assertEquals(equipmentType, contingencyElement.getType());
    }

    private void checkAction(Action action, String actionId, int startTime, int numEquipments, String equipmentId, ActionElementType equipmentType) {
        assertNotNull(action);
        assertEquals(actionId, action.getId());
        assertTrue(action.isCurative());
        assertTrue(action.isPreventive());
        assertEquals(BigInteger.valueOf(startTime), action.getStartTime());
        assertEquals(numEquipments, action.getElements().size());
        ActionElement actionElement = action.getElements().iterator().next();
        assertEquals(equipmentId, actionElement.getEquipmentId());
        assertEquals(equipmentType, actionElement.getType());
    }

    private void checkActionPlan(ActionPlan actionPlan, String name, int numOptions, int numActions, String actionId) {
        assertNotNull(actionPlan);
        assertEquals(name, actionPlan.getName());
        assertEquals(numOptions, actionPlan.getPriorityOption().size());
        ActionPlanOption option = actionPlan.getPriorityOption().values().iterator().next();
        assertEquals(numActions, option.getActions().size());
        assertEquals(actionId, option.getActions().values().iterator().next());
    }

    private void checkAssociation(ActionsContingenciesAssociation actionCtgAssociation, int numContingencies, String contingencyId, String actionId, 
            String equipmentId, ConstraintType constraintType) {
        assertNotNull(actionCtgAssociation);
        assertEquals(numContingencies, actionCtgAssociation.getContingenciesId().size());
        if ( numContingencies > 0 )
            assertEquals(contingencyId, actionCtgAssociation.getContingenciesId().iterator().next());
        assertEquals(1, actionCtgAssociation.getActionsId().size());
        assertEquals(actionId, actionCtgAssociation.getActionsId().iterator().next());
        assertEquals(1, actionCtgAssociation.getConstraints().size());
        assertEquals(equipmentId, actionCtgAssociation.getConstraints().iterator().next().getEquipment());
        assertEquals(constraintType, actionCtgAssociation.getConstraints().iterator().next().getType());
    }

}
