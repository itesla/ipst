/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
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
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyImpl;
import com.powsybl.security.LimitViolationType;
import com.powsybl.simulation.securityindexes.SecurityIndex;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
public class OnlineUtilsTest {

    private final Contingency contingency1 = new ContingencyImpl("id1", Collections.emptyList());
    private final Contingency contingency2 = new ContingencyImpl("id2", Collections.emptyList());
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
}
