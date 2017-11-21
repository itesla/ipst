/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.contingencies.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import eu.itesla_project.modules.contingencies.ActionPlan;
import eu.itesla_project.modules.contingencies.ActionPlanOption;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class ActionPlanImpl implements ActionPlan {

    private String name;

    private String description;

    private List<String> zones = new ArrayList<String>();

    private Map<BigInteger, ActionPlanOption> priorityOption = new HashMap<BigInteger, ActionPlanOption>();


    public ActionPlanImpl(String name, String description, List<String> zones, Map<BigInteger, ActionPlanOption> priorityOptions) {
        this.name = name;
        this.description = description;
        this.zones = zones;
        this.priorityOption = priorityOptions;
    }



    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public List<String> getZones() {
        return this.zones;

    }

    @Override
    public Map<BigInteger, ActionPlanOption> getPriorityOption() {
        return this.priorityOption;
    }


}

