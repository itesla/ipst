/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.db;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.powsybl.iidm.network.Branch;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class OnlineDbMVStoreUtilsTest {

    @Test
    public void jsonLimitViolation() {
        LimitViolation lineViolation = new LimitViolation("LINE", LimitViolationType.CURRENT, "10", 1000f, 1, 1100f, Branch.Side.ONE);
        String jsonLineViolation = OnlineDbMVStoreUtils.limitViolationToJson(lineViolation);
        LimitViolation storedLineViolation = OnlineDbMVStoreUtils.jsonToLimitViolation(jsonLineViolation, null);
        checkViolation(lineViolation, storedLineViolation);
        
        LimitViolation vlViolation = new LimitViolation("VL", LimitViolationType.HIGH_VOLTAGE, 200f, 1, 250f);
        String jsonVlViolation = OnlineDbMVStoreUtils.limitViolationToJson(vlViolation);
        LimitViolation storedVlViolation = OnlineDbMVStoreUtils.jsonToLimitViolation(jsonVlViolation, null);
        checkViolation(vlViolation, storedVlViolation);
        
    }

    private void checkViolation(LimitViolation expectedViolation, LimitViolation actualViolation) {
        assertEquals(expectedViolation.getSubjectId(), actualViolation.getSubjectId());
        assertEquals(expectedViolation.getLimitType(), actualViolation.getLimitType());
        assertEquals(expectedViolation.getLimitName(), actualViolation.getLimitName());
        assertEquals(expectedViolation.getLimit(), actualViolation.getLimit(), 0);
        assertEquals(expectedViolation.getLimitReduction(), actualViolation.getLimitReduction(), 0);
        assertEquals(expectedViolation.getValue(), actualViolation.getValue(), 0);
        assertEquals(expectedViolation.getSide(), actualViolation.getSide());
    }

}
