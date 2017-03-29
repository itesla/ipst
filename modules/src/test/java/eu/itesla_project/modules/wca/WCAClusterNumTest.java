/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.wca;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAClusterNumTest {

    @Test
    public void testFromInt() {
        assertEquals(WCAClusterNum.UNDEFINED, WCAClusterNum.fromInt(-1));
        assertEquals(WCAClusterNum.ONE, WCAClusterNum.fromInt(1));
        assertEquals(WCAClusterNum.FOUR, WCAClusterNum.fromInt(4));
        try {
            WCAClusterNum.fromInt(-2);
            fail();
        } catch(Exception ignored) {
        }
    }

}
