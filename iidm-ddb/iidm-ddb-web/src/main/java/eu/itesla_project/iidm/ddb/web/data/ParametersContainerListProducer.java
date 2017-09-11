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

import eu.itesla_project.iidm.ddb.model.ParametersContainer;
import eu.itesla_project.iidm.ddb.service.DDBManager;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
@RequestScoped
public class ParametersContainerListProducer {

    @Inject
    private Logger log;
    
    @EJB
    private DDBManager pmanager;

    private List<ParametersContainer> parametersContainerAll;
    
    
    //private List<String> parametersContainerDdbids;

    // @Named provides access the return value via the EL variable name "members" in the UI (e.g.
    // Facelets or JSP view)
    @Produces
    @Named
    public List<ParametersContainer> getParametersContainerAll() {
        return parametersContainerAll;
    }    

    
    @PostConstruct
    public void retrieveAllParametersContainer() {
        log.log(Level.INFO, " create parametersContainerAll  list ");

        this.parametersContainerAll = pmanager.findParametersContainerAll();
        log.log(Level.INFO, " DONE create parametersContainerAll  list ");
    /*    this.parametersContainerDdbids  = new ArrayList<String>();

        for (ParametersContainer pc :parametersContainerAll) {
            parametersContainerDdbids.add(pc.getDdbId());
        }*/
    }
    
   /* public List<String> geetParametersContainerDdbids(){
        return this.parametersContainerDdbids;
    }*/
    
}
