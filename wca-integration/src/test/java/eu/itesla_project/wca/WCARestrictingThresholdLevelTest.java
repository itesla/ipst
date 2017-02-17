/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class WCARestrictingThresholdLevelTest {

    @Test
    public void testThresholdLevels() {
        assertEquals(0, WCARestrictingThresholdLevel.getLevel(EnumSet.noneOf(WCARestrictingThresholdLevel.class)));
        assertEquals(1, WCARestrictingThresholdLevel.getLevel(EnumSet.of(WCARestrictingThresholdLevel.NO_HV_THRESHOLDS)));
        assertEquals(2, WCARestrictingThresholdLevel.getLevel(EnumSet.of(WCARestrictingThresholdLevel.NO_FOREIGN_THRESHOLDS)));
        assertEquals(3, WCARestrictingThresholdLevel.getLevel(EnumSet.of(WCARestrictingThresholdLevel.NO_FOREIGN_THRESHOLDS, WCARestrictingThresholdLevel.NO_HV_THRESHOLDS)));
        assertEquals(3, WCARestrictingThresholdLevel.getLevel(EnumSet.of(WCARestrictingThresholdLevel.NO_HV_THRESHOLDS, WCARestrictingThresholdLevel.NO_FOREIGN_THRESHOLDS)));
        assertEquals(1, WCARestrictingThresholdLevel.getLevel(EnumSet.of(WCARestrictingThresholdLevel.NO_HV_THRESHOLDS, WCARestrictingThresholdLevel.NO_HV_THRESHOLDS)));
        assertEquals(2, WCARestrictingThresholdLevel.getLevel(EnumSet.of(WCARestrictingThresholdLevel.NO_FOREIGN_THRESHOLDS, WCARestrictingThresholdLevel.NO_FOREIGN_THRESHOLDS)));
        assertEquals(3, WCARestrictingThresholdLevel.getLevel(EnumSet.allOf(WCARestrictingThresholdLevel.class)));
    }

}
