/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import com.google.common.collect.Lists;
import eu.itesla_project.cta.model.IpsoBoundsEvaluator;
import eu.itesla_project.cta.model.IpsoConstraint;
import eu.itesla_project.cta.model.IpsoProblemDefinition;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class IpsoConstraintFinder {

    /**
     * Constructor
     */
    public IpsoConstraintFinder() {
    }

    /**
     * @return violated constraints
     */
    public List<IpsoConstraint> findAllViolatedConstraintsIn(IpsoProblemDefinition ipsoProblemDefinition) {
        checkArgument(ipsoProblemDefinition != null, "ipsoProblemDefinition must not be null");

        // 1. find all constraints
        List<IpsoConstraint> constraints = findAllConstraintsOf(ipsoProblemDefinition);

        // 2. filter on violated constraint
        return constraints.stream()
                .filter(IpsoConstraint::isViolated)
                .collect(toList());
    }

    /**
     * @return violated constraints from a list of constraints
     */
    public <T extends IpsoConstraint<?>> List<T> findViolatedConstraintsIn(List<T> constraints) {
        return constraints.stream()
                .filter(T::isViolated)
                .collect(toList());
    }

    /**
     * @return violated setpoint constraints from a list of constraints
     */
    public <T extends IpsoConstraint<?>> List<T> findViolatedSetpointConstraintsIn(List<T> constraints) {
        return constraints.stream()
                .filter(T::isViolated)
                .filter(T::isSetpointConstraint)
                .collect(toList());
    }

    /**
     * @return violated bounds constraints from a list of constraints
     */
    public <T extends IpsoConstraint<?>> List<T> findViolatedBoundsConstraintsIn(List<T> constraints) {
        return constraints.stream()
                .filter(T::isViolated)
                .filter(T::isNotSetpointConstraint)
                .collect(toList());
    }

    /**
     * @return not violated constraints from a list of constraints
     */
    public <T extends IpsoConstraint<?>> List<T> findNotViolatedSetpointConstraintsIn(List<T> constraints) {
        return constraints.stream()
                .filter(T::isNotViolated)
                .filter(T::isSetpointConstraint)
                .collect(toList());
    }

    public <T extends IpsoConstraint> List<T> findConstraintsJustOnTheBounds(Collection<T> constraints) {
        return constraints.stream()
                .filter(c -> IpsoBoundsEvaluator.isJustOnTheBounds(c.getConstrainedValue(), c.getBoundsMin(), c.getBoundsMax()))
                .collect(toList());
    }

    /**
     *
     * @return all constraints defined to the Ipso problem definition
     */
    private List<IpsoConstraint> findAllConstraintsOf(IpsoProblemDefinition ipsoProblemDefinition) {
        List<IpsoConstraint> ipsoConstraints = Lists.newArrayList( ipsoProblemDefinition.getConstraintNodeAcVoltageBounds());
        ipsoConstraints.addAll(ipsoProblemDefinition.getConstraintNodeAcAngleBounds());
        ipsoConstraints.addAll(ipsoProblemDefinition.getConstraintGeneratorPBounds());
        ipsoConstraints.addAll(ipsoProblemDefinition.getConstraintGeneratorQBounds());
        ipsoConstraints.addAll(ipsoProblemDefinition.getConstraint2WTransformerFlows());
        ipsoConstraints.addAll(ipsoProblemDefinition.getConstraint2WTransformerTaps());
        ipsoConstraints.addAll(ipsoProblemDefinition.getConstraintLineFlows());
        ipsoConstraints.addAll(ipsoProblemDefinition.getConstraintBankStepBounds());
        return ipsoConstraints;
    }
}
