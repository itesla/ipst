/**
 * Copyright (c) 2017-2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.db;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;

import org.junit.Test;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;

import eu.itesla_project.online.OnlineUtils;

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

    @Test
    public void branchesDataToCsv() {
        String networkId = "network1";
        Integer stateId = 1;
        String contingencyId = "contingency1";
        String csvHeaders = String.join(";", 
                                        "networkId",
                                        "stateId",
                                        "contingencyId",
                                        "NHV1_NHV2_1__TO__VLHV1_I",
                                        "NHV1_NHV2_1__TO__VLHV1_P",
                                        "NHV1_NHV2_1__TO__VLHV1_IMAX",
                                        "NHV1_NHV2_1__TO__VLHV2_I",
                                        "NHV1_NHV2_1__TO__VLHV2_P",
                                        "NHV1_NHV2_1__TO__VLHV2_IMAX",
                                        "NHV1_NHV2_2__TO__VLHV1_I",
                                        "NHV1_NHV2_2__TO__VLHV1_P",
                                        "NHV1_NHV2_2__TO__VLHV1_IMAX",
                                        "NHV1_NHV2_2__TO__VLHV2_I",
                                        "NHV1_NHV2_2__TO__VLHV2_P",
                                        "NHV1_NHV2_2__TO__VLHV2_IMAX",
                                        "NGEN_NHV1__TO__VLGEN_I",
                                        "NGEN_NHV1__TO__VLGEN_P",
                                        "NGEN_NHV1__TO__VLGEN_IMAX",
                                        "NGEN_NHV1__TO__VLHV1_I",
                                        "NGEN_NHV1__TO__VLHV1_P",
                                        "NGEN_NHV1__TO__VLHV1_IMAX",
                                        "NHV2_NLOAD__TO__VLHV2_I",
                                        "NHV2_NLOAD__TO__VLHV2_P",
                                        "NHV2_NLOAD__TO__VLHV2_IMAX",
                                        "NHV2_NLOAD__TO__VLLOAD_I",
                                        "NHV2_NLOAD__TO__VLLOAD_P",
                                        "NHV2_NLOAD__TO__VLLOAD_IMAX");
        String csvValues = String.join(";",
                                       networkId,
                                       Integer.toString(stateId),
                                       contingencyId,
                                       Float.toString(1192.5631f),
                                       Float.toString(560f),
                                       Float.toString(500f),
                                       Float.toString(1192.5631f),
                                       Float.toString(560f),
                                       Float.toString(1100f),
                                       Float.toString(1192.5631f),
                                       Float.toString(560f),
                                       Float.toString(1100f),
                                       Float.toString(1192.5631f),
                                       Float.toString(560f),
                                       Float.toString(500f),
                                       Float.toString(Float.NaN),
                                       Float.toString(Float.NaN),
                                       Float.toString(Float.NaN),
                                       Float.toString(Float.NaN),
                                       Float.toString(Float.NaN),
                                       Float.toString(Float.NaN),
                                       Float.toString(Float.NaN),
                                       Float.toString(Float.NaN),
                                       Float.toString(Float.NaN),
                                       Float.toString(Float.NaN),
                                       Float.toString(Float.NaN),
                                       Float.toString(Float.NaN));
        // check to obtain the expected results
        Network network1 = EurostagTutorialExample1Factory.createWithCurrentLimits();
        LinkedHashMap<String, Float> branchesData1 = OnlineUtils.getBranchesData(network1);
        String csvHeaders1 = OnlineDbMVStoreUtils.branchesDataToCsvHeaders(branchesData1);
        assertEquals(csvHeaders, csvHeaders1);
        String csvValues1 = OnlineDbMVStoreUtils.branchesDataToCsv(networkId, stateId, contingencyId, branchesData1);
        assertEquals(csvValues, csvValues1);
        // check that with the same network you get the same results (same order)
        Network network2 = EurostagTutorialExample1Factory.createWithCurrentLimits();
        LinkedHashMap<String, Float> branchesData2 = OnlineUtils.getBranchesData(network2);
        String csvHeaders2 = OnlineDbMVStoreUtils.branchesDataToCsvHeaders(branchesData2);
        assertEquals(csvHeaders1, csvHeaders2);
        String csvValues2 = OnlineDbMVStoreUtils.branchesDataToCsv(networkId, stateId, contingencyId, branchesData2);
        assertEquals(csvValues1, csvValues2);
    }

    @Test
    public void postContingencyStateKey() {
        assertEquals("001_contingency", OnlineDbMVStoreUtils.postContingencyStateKey(1, "contingency"));
        assertEquals("020_contingency", OnlineDbMVStoreUtils.postContingencyStateKey(20, "contingency"));
        assertEquals("240_contingency", OnlineDbMVStoreUtils.postContingencyStateKey(240, "contingency"));
    }
}
