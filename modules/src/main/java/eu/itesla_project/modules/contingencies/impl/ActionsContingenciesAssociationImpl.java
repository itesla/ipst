/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.contingencies.impl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation;
import eu.itesla_project.modules.contingencies.Constraint;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class ActionsContingenciesAssociationImpl implements ActionsContingenciesAssociation {

    Collection<String> contingencies = new ArrayList<String>();
    Collection<Constraint> constraints = new ArrayList<Constraint>();
    Collection<String> actions = new ArrayList<String>(); // could be ActionPlan or ElementaryAction


    public ActionsContingenciesAssociationImpl(List<String> contingencies, List<Constraint> constraints, List<String> actions) {
        this.contingencies = contingencies;
        this.constraints = constraints;
        this.actions = actions;
    }

    @Override
    public Collection<String> getContingenciesId() {
        return contingencies;
    }

    @Override
    public Collection<Constraint> getConstraints() {
        return constraints;
    }

    @Override
    public Collection<String> getActionsId() {
        return actions;
    }

}
