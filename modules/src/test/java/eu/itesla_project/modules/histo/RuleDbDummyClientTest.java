/*
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.histo;

import com.powsybl.simulation.securityindexes.SecurityIndexType;
import eu.itesla_project.modules.rules.RuleAttributeSet;
import eu.itesla_project.modules.rules.RulesDbClient;
import eu.itesla_project.modules.test.RulesDbClientTestFactoryImpl;
import org.joda.time.Interval;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class RuleDbDummyClientTest {

    @Test
    public void testCreateClient() {
        RulesDbClient ruleDbClient = new RulesDbClientTestFactoryImpl().create("RULEDBNAME");
        assertNotNull(ruleDbClient);
    }

    @Test
    public void testmethods() throws IOException, InterruptedException {
        Interval interval = Interval.parse("2013-01-14T00:00:00+01:00/2013-01-14T01:00:00+01:00");
        String rulesDbName = "DUMMYRULEDBNAMW";
        String wfId = "DUMMYWORKFLOWID";
        String contingencyId = "DUMMYCONTINGENCYID";

        RulesDbClient ruleDbClient = new RulesDbClientTestFactoryImpl().create(rulesDbName);
        assertEquals(ruleDbClient.listWorkflows(), Collections.emptyList());
        assertEquals(ruleDbClient.getRules(wfId, RuleAttributeSet.WORST_CASE, contingencyId, SecurityIndexType.TSO_OVERLOAD), Collections.emptyList());
        assertEquals(ruleDbClient.listRules(wfId, RuleAttributeSet.WORST_CASE), Collections.emptyList());
    }

}