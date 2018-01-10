/*
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.test;

import com.powsybl.simulation.securityindexes.SecurityIndexType;
import eu.itesla_project.modules.rules.RuleAttributeSet;
import eu.itesla_project.modules.rules.RuleId;
import eu.itesla_project.modules.rules.RulesDbClient;
import eu.itesla_project.modules.rules.SecurityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class RulesDbClientTestImpl implements RulesDbClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RulesDbClientTestImpl.class);


    @Override
    public void close() throws Exception {
    }

    @Override
    public List<String> listWorkflows() {
        return Collections.emptyList();
    }

    @Override
    public void updateRule(SecurityRule rule) {
        LOGGER.info("updateRule ({})", rule);
    }

    @Override
    public List<SecurityRule> getRules(String workflowId, RuleAttributeSet attributeSet, String contingencyId, SecurityIndexType securityIndexType) {
        return Collections.emptyList();
    }

    @Override
    public Collection<RuleId> listRules(String workflowId, RuleAttributeSet attributeSet) {
        return Collections.emptyList();
    }

}
