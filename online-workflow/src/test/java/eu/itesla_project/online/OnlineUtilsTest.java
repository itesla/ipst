/**
 * Copyright (c) 2017-2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.security.LimitViolationType;
import com.powsybl.simulation.securityindexes.SecurityIndex;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
public class OnlineUtilsTest {

    private final Contingency contingency1 = new Contingency("id1", Collections.emptyList());
    private final Contingency contingency2 = new Contingency("id2", Collections.emptyList());
    private final List<Contingency> contingencies = Arrays.asList(contingency1, contingency2);

    @Test
    public void filterContingenciesTest() {
        Collection<Contingency> contingencyList = OnlineUtils.filterContingencies(contingencies, Collections.emptyList());
        assertEquals(0, contingencyList.size());

        contingencyList = OnlineUtils.filterContingencies(contingencies, Arrays.asList("id1", "id2"));
        assertEquals(2, contingencyList.size());
        assertEquals(contingency1, contingencyList.iterator().next());

        contingencyList = OnlineUtils.filterContingencies(contingencies, Collections.singletonList("id1"));
        assertEquals(1, contingencyList.size());
        assertEquals(contingency1, contingencyList.iterator().next());

        contingencyList = OnlineUtils.filterContingencies(contingencies, Collections.singletonList("id2"));
        assertEquals(1, contingencyList.size());
        assertEquals(contingency2, contingencyList.iterator().next());

        contingencyList = OnlineUtils.filterContingencies(contingencies, Collections.singletonList("id3"));
        assertEquals(0, contingencyList.size());
    }

    @Test
    public void getContingenciesIdTest() {
        Set<String> contingenciesId = OnlineUtils.getContingencyIds(contingencies);
        assertEquals(2, contingenciesId.size());
        assertTrue(contingenciesId.contains("id1"));
        assertTrue(contingenciesId.contains("id2"));
        assertFalse(contingenciesId.contains("id3"));
    }

    @Test
    public void isSafeTest() {
        SecurityIndex securityIndex1 = Mockito.mock(SecurityIndex.class);
        Mockito.when(securityIndex1.isOk()).thenReturn(true);

        SecurityIndex securityIndex2 = Mockito.mock(SecurityIndex.class);
        Mockito.when(securityIndex2.isOk()).thenReturn(false);

        assertTrue(OnlineUtils.isSafe(Collections.emptyList()));
        assertTrue(OnlineUtils.isSafe(Collections.singletonList(securityIndex1)));
        assertTrue(OnlineUtils.isSafe(Arrays.asList(securityIndex1, securityIndex1)));
        assertFalse(OnlineUtils.isSafe(Collections.singletonList(securityIndex2)));
        assertFalse(OnlineUtils.isSafe(Arrays.asList(securityIndex1, securityIndex2)));
    }

    @Test
    public void getPostContingencyIdTest() {
        assertEquals("state-post-contingency", OnlineUtils.getPostContingencyId("state", "contingency"));
    }
    
    @Test
    public void getUnitEnumTest() {
        assertEquals(UnitEnum.MW, OnlineUtils.getUnit(LimitViolationType.CURRENT));
        assertEquals(UnitEnum.KV, OnlineUtils.getUnit(LimitViolationType.LOW_VOLTAGE));
        assertEquals(UnitEnum.KV, OnlineUtils.getUnit(LimitViolationType.HIGH_VOLTAGE));
    }

    @Test
    public void getBranchesDataTest() {
        Network network = EurostagTutorialExample1Factory.createWithCurrentLimits();
        Map<String, Double> expectedBranchesData = ImmutableMap.<String, Double>builder()
                                                              .put("NHV1_NHV2_1__TO__VLHV1_I", 1192.5631358010583)
                                                              .put("NHV1_NHV2_1__TO__VLHV1_P", 560.0)
                                                              .put("NHV1_NHV2_1__TO__VLHV1_IMAX", 500.0)
                                                              .put("NHV1_NHV2_1__TO__VLHV2_I", 1192.5631358010583)
                                                              .put("NHV1_NHV2_1__TO__VLHV2_P", 560.0)
                                                              .put("NHV1_NHV2_1__TO__VLHV2_IMAX", 1100.0)
                                                              .put("NHV1_NHV2_2__TO__VLHV1_I", 1192.5631358010583)
                                                              .put("NHV1_NHV2_2__TO__VLHV1_P", 560.0)
                                                              .put("NHV1_NHV2_2__TO__VLHV1_IMAX", 1100.0)
                                                              .put("NHV1_NHV2_2__TO__VLHV2_I", 1192.5631358010583)
                                                              .put("NHV1_NHV2_2__TO__VLHV2_P", 560.0)
                                                              .put("NHV1_NHV2_2__TO__VLHV2_IMAX", 500.0)
                                                              .put("NGEN_NHV1__TO__VLGEN_I", Double.NaN)
                                                              .put("NGEN_NHV1__TO__VLGEN_P", Double.NaN)
                                                              .put("NGEN_NHV1__TO__VLGEN_IMAX", Double.NaN)
                                                              .put("NGEN_NHV1__TO__VLHV1_I", Double.NaN)
                                                              .put("NGEN_NHV1__TO__VLHV1_P", Double.NaN)
                                                              .put("NGEN_NHV1__TO__VLHV1_IMAX", Double.NaN)
                                                              .put("NHV2_NLOAD__TO__VLHV2_I", Double.NaN)
                                                              .put("NHV2_NLOAD__TO__VLHV2_P", Double.NaN)
                                                              .put("NHV2_NLOAD__TO__VLHV2_IMAX", Double.NaN)
                                                              .put("NHV2_NLOAD__TO__VLLOAD_I", Double.NaN)
                                                              .put("NHV2_NLOAD__TO__VLLOAD_P", Double.NaN)
                                                              .put("NHV2_NLOAD__TO__VLLOAD_IMAX", Double.NaN)
                                                              .build();
        LinkedHashMap<String, Double> branchesData = OnlineUtils.getBranchesData(network);
        assertEquals(24, branchesData.values().size(), 0);
        expectedBranchesData.keySet().forEach(attribute -> {
            assertTrue(branchesData.containsKey(attribute));
            assertEquals(expectedBranchesData.get(attribute), branchesData.get(attribute), 0f);
        });
    }

}
