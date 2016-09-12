/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import com.google.common.collect.Lists;
import eu.itesla_project.cta.model.IpsoConstraint;
import eu.itesla_project.cta.model.IpsoControlVariable;
import eu.itesla_project.cta.model.TopologicalAction;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class CorrectiveActions {

    private final List<IpsoControlVariable> controlVariables;
    private final List<IpsoConstraint> constraints;
    private final List<TopologicalAction> topologicalActions;
    private String actionPlanId = "";

    public CorrectiveActions(List<IpsoControlVariable> controlVariables, List<IpsoConstraint> constraints, List<TopologicalAction> topologicalActions) {
        checkArgument(controlVariables != null, "controlVariables must not be null");
        checkArgument(constraints != null, "constraints must not be null");
        checkArgument(topologicalActions != null, "topologicalActions must not be null");
        this.controlVariables = controlVariables;
        this.constraints = constraints;
        this.topologicalActions = topologicalActions;
    }

    public CorrectiveActions() {
        controlVariables = Lists.newArrayList();
        constraints = Lists.newArrayList();
        this.topologicalActions = Lists.newArrayList();
    }

    public List<IpsoControlVariable> getControlVariables() {
        return controlVariables;
    }

    public List<IpsoConstraint> getConstraints() {
        return constraints;
    }

    public List<TopologicalAction> getTopologicalActions() {
        return topologicalActions;
    }

    public boolean addTopologicalAction(TopologicalAction topologicalAction) {
        return topologicalActions.add(topologicalAction);
    }

    public boolean hasTopologicalActions() {
        checkState(topologicalActions != null, "topologicalActions must not be null");
        return topologicalActions.size() > 0;
    }

    public String getActionPlanId() {
        return actionPlanId;
    }
}
