/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.actions_contingencies.xml.test;

import eu.itesla_project.iidm.actions_contingencies.xml.XmlFileContingenciesAndActionsDatabaseClient;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.test.PhaseShifterTestCaseFactory;
import eu.itesla_project.modules.contingencies.Action;
import eu.itesla_project.modules.contingencies.ActionElement;
import eu.itesla_project.modules.contingencies.ActionElementType;
import eu.itesla_project.modules.contingencies.TapChangeAction;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
public class PstOperationTest {

    @Test
    public void test() throws JAXBException, SAXException {
        Network network = PhaseShifterTestCaseFactory.create();

        Path path = Paths.get("src/test/resources/pstOperations.xml");
        XmlFileContingenciesAndActionsDatabaseClient client = new XmlFileContingenciesAndActionsDatabaseClient(path);

        List<Action> actions = client.getActions(network);
        assertEquals(1, actions.size());

        Action action = actions.get(0);
        assertEquals("tapChange", action.getId());
        assertTrue(action.isCurative());
        assertFalse(action.isPreventive());
        assertEquals(123, action.getStartTime().intValue());
        assertEquals(1, action.getElements().size());

        ActionElement actionElement = action.getElements().iterator().next();
        assertEquals(ActionElementType.TAP_CHANGE, actionElement.getType());
        TapChangeAction tapChangeAction = (TapChangeAction) actionElement;
        assertEquals("PS1", tapChangeAction.getEquipmentId());
        assertEquals(0, tapChangeAction.getAchievmentIndex().intValue());
        assertEquals(150, tapChangeAction.getImplementationTime().intValue());
        assertEquals(2, tapChangeAction.getTapPosition());
    }
}
