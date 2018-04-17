/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.security.rest.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import eu.itesla_project.security.rest.api.factories.SecurityAnalysisServiceFactory;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.it>
 */
@Path("/")
public class SecurityAnalysisApi {
    private final SecurityAnalysisService delegate = SecurityAnalysisServiceFactory.getSecurityServiceApi();

    @POST
    @Path("/security-analysis")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response process(MultipartFormDataInput form, @Context SecurityContext securityContext) {
        return delegate.analyze(form);
    }

    @POST
    @Path("/action-simulator")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response actionSimulator(MultipartFormDataInput form, @Context SecurityContext securityContext) {
        return delegate.actionSimulator(form);
    }
}
