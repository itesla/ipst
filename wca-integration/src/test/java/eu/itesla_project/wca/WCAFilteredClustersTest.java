/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.EnumSet;

import org.junit.Test;

import eu.itesla_project.modules.wca.WCAClusterNum;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAFilteredClustersTest {

    @Test
    public void test() throws Exception {
        String contingencyId = "contigencyId";
        WCAFilteredClusters filteredClusters = new WCAFilteredClusters("networkId", Collections.singletonList(contingencyId));
        checkValues(filteredClusters, 
                    contingencyId, 
                    WCAClusterNum.FOUR, 
                    EnumSet.of(WCAClusterNum.ONE, WCAClusterNum.TWO, WCAClusterNum.THREE, WCAClusterNum.FOUR), 
                    EnumSet.noneOf(WCAClusterOrigin.class));

        filteredClusters.removeClusters(contingencyId, EnumSet.of(WCAClusterNum.ONE,WCAClusterNum.FOUR), WCAClusterOrigin.LF_BASIC_VIOLATION);
        checkValues(filteredClusters, 
                    contingencyId, 
                    WCAClusterNum.THREE, 
                    EnumSet.of(WCAClusterNum.TWO, WCAClusterNum.THREE), 
                    EnumSet.of(WCAClusterOrigin.LF_BASIC_VIOLATION));

        filteredClusters.addClusters(contingencyId, EnumSet.of(WCAClusterNum.FOUR), WCAClusterOrigin.CLUSTERS_ANALYSIS);
        checkValues(filteredClusters, 
                    contingencyId, 
                    WCAClusterNum.FOUR, 
                    EnumSet.of(WCAClusterNum.TWO, WCAClusterNum.THREE, WCAClusterNum.FOUR), 
                    EnumSet.of(WCAClusterOrigin.LF_BASIC_VIOLATION, WCAClusterOrigin.CLUSTERS_ANALYSIS));

        filteredClusters.removeClusters(contingencyId, EnumSet.of(WCAClusterNum.TWO,WCAClusterNum.THREE,WCAClusterNum.FOUR), WCAClusterOrigin.LF_DIVERGENCE);
        checkValues(filteredClusters, 
                    contingencyId, 
                    WCAClusterNum.UNDEFINED, 
                    EnumSet.noneOf(WCAClusterNum.class), 
                    EnumSet.of(WCAClusterOrigin.LF_BASIC_VIOLATION, WCAClusterOrigin.CLUSTERS_ANALYSIS, WCAClusterOrigin.LF_DIVERGENCE));
        
        filteredClusters.addClusters(contingencyId, EnumSet.of(WCAClusterNum.ONE, WCAClusterNum.TWO, WCAClusterNum.THREE, WCAClusterNum.FOUR), WCAClusterOrigin.LF_RULE_VIOLATION);
        checkValues(filteredClusters, 
                    contingencyId, 
                    WCAClusterNum.FOUR, 
                    EnumSet.of(WCAClusterNum.ONE, WCAClusterNum.TWO, WCAClusterNum.THREE, WCAClusterNum.FOUR), 
                    EnumSet.of(WCAClusterOrigin.LF_BASIC_VIOLATION, WCAClusterOrigin.CLUSTERS_ANALYSIS, WCAClusterOrigin.LF_DIVERGENCE, WCAClusterOrigin.LF_RULE_VIOLATION));
        
        filteredClusters.removeAllButCluster(contingencyId, WCAClusterNum.TWO, WCAClusterOrigin.LF_DIVERGENCE);
        checkValues(filteredClusters, 
                    contingencyId, 
                    WCAClusterNum.TWO, 
                    EnumSet.of(WCAClusterNum.TWO), 
                    EnumSet.of(WCAClusterOrigin.LF_BASIC_VIOLATION, WCAClusterOrigin.CLUSTERS_ANALYSIS, WCAClusterOrigin.LF_DIVERGENCE, WCAClusterOrigin.LF_RULE_VIOLATION));
        
        filteredClusters.removeAllButCluster(contingencyId, WCAClusterNum.ONE, WCAClusterOrigin.CLUSTERS_ANALYSIS);
        checkValues(filteredClusters, 
                    contingencyId, 
                    WCAClusterNum.TWO, 
                    EnumSet.of(WCAClusterNum.TWO), 
                    EnumSet.of(WCAClusterOrigin.LF_BASIC_VIOLATION, WCAClusterOrigin.CLUSTERS_ANALYSIS, WCAClusterOrigin.LF_DIVERGENCE, WCAClusterOrigin.LF_RULE_VIOLATION));
    }

    private void checkValues(WCAFilteredClusters filteredClusters, String contingencyId, WCAClusterNum expectedClusterNum,
                             EnumSet<WCAClusterNum> expectedClusterNums, EnumSet<WCAClusterOrigin> expectedFlags) {

        assertEquals(expectedClusterNum, filteredClusters.getCluster(contingencyId));

        if ( !expectedClusterNum.equals(WCAClusterNum.UNDEFINED) ) {
            assertTrue(filteredClusters.hasCluster(contingencyId, expectedClusterNum));
        }

        assertEquals(expectedClusterNums, filteredClusters.getClusters(contingencyId));

        assertEquals(expectedFlags, filteredClusters.getFlags(contingencyId));
    }

}
