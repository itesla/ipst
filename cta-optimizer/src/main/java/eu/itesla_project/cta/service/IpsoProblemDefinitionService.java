/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import eu.itesla_project.cta.model.IpsoConstraint;
import eu.itesla_project.cta.model.IpsoProblemDefinition;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
 class IpsoProblemDefinitionService {

    private static final Logger LOG = getLogger(IpsoProblemDefinitionService.class);

    public IpsoProblemDefinitionService() {
    }

    /**
     * @return violated constraints defined within a problem definition
     */
    public List<IpsoConstraint> findViolatedConstraintsIn(IpsoProblemDefinition problemDefinition) {
        IpsoConstraintFinder ipsoConstraintFinder = new IpsoConstraintFinder();
        return ipsoConstraintFinder.findAllViolatedConstraintsIn(problemDefinition);
    }
}


