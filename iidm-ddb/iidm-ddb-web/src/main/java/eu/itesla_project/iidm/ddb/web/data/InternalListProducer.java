/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.ddb.web.data;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import eu.itesla_project.iidm.ddb.model.Internal;
import eu.itesla_project.iidm.ddb.service.DDBManager;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
@RequestScoped
public class InternalListProducer {

    @Inject
    private Logger log;
    
    @EJB
    private DDBManager pmanager;

    private List<Internal> internals;
    
    
    // @Named provides access the return value via the EL variable name "members" in the UI (e.g.
    // Facelets or JSP view)
    @Produces
    @Named
    public List<Internal> getInternals() {
        return internals;
    }    

    @PostConstruct
    public void retrieveAllInternals() {
        log.log(Level.INFO, " produces internals list ");
        this.internals = pmanager.findInternalsAll();
        log.log(Level.INFO, " produced internals list size " + internals.size());
    }
}
