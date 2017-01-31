/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class WCARestrictingThresholdLevelTest {

    @Test
    public void testIntToEnum() {
        assertEquals(WCARestrictingThresholdLevel.fromLevel(0), WCARestrictingThresholdLevel.ZERO);
        assertEquals(WCARestrictingThresholdLevel.fromLevel(1), WCARestrictingThresholdLevel.ONE);
        assertEquals(WCARestrictingThresholdLevel.fromLevel(2), WCARestrictingThresholdLevel.TWO);
        assertEquals(WCARestrictingThresholdLevel.fromLevel(3), WCARestrictingThresholdLevel.THREE);
        assertEquals(WCARestrictingThresholdLevel.fromLevel(4), null);
    }

    @Test
    public void testEnumToInt() {
        assertEquals(WCARestrictingThresholdLevel.ZERO.getLevel(), 0);
        assertEquals(WCARestrictingThresholdLevel.ONE.getLevel(), 1);
        assertEquals(WCARestrictingThresholdLevel.TWO.getLevel(), 2);
        assertEquals(WCARestrictingThresholdLevel.THREE.getLevel(), 3);

    }


}
