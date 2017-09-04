/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.contigencies;

import eu.itesla_project.contingency.tasks.ModificationTask;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.PhaseTapChanger;
import eu.itesla_project.iidm.network.test.PhaseShifterTestCaseFactory;
import eu.itesla_project.modules.contingencies.ActionElementType;
import eu.itesla_project.modules.contingencies.TapChangeAction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
public class TapChangeActionTest {

    @Test
    public void testContructors() {
        TapChangeAction action = new TapChangeAction("equipmentId", 1);
        assertEquals("equipmentId", action.getEquipmentId());
        assertEquals(1, action.getTapPosition());
        assertNull(action.getAchievmentIndex());
        assertNull(action.getImplementationTime());
        assertEquals(ActionElementType.TAP_CHANGE, action.getType());

        action = new TapChangeAction("equipmentId2", 2, 150, 0);
        assertEquals("equipmentId2", action.getEquipmentId());
        assertEquals(2, action.getTapPosition());
        assertEquals(0, action.getAchievmentIndex());
        assertEquals(150, action.getImplementationTime());
    }

    @Test
    public void testToTask() {
        Network network = PhaseShifterTestCaseFactory.create();
        PhaseTapChanger tapChanger = network.getTwoWindingsTransformer("PS1").getPhaseTapChanger();
        assertEquals(1, tapChanger.getTapPosition());

        TapChangeAction action = new TapChangeAction("PS1", 2);
        ModificationTask task = action.toTask();
        task.modify(network, null);
        assertEquals(2, tapChanger.getTapPosition());

        try {
            action.toTask(null);
            fail();
        } catch (UnsupportedOperationException exc) {
        }
    }
}
