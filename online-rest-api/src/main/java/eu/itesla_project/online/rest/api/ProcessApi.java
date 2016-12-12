/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import eu.itesla_project.online.rest.api.factories.ProcessApiServiceFactory;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
@Path("/process")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2016-10-06T14:01:02.692Z")
public class ProcessApi {
    private final ProcessApiService delegate = ProcessApiServiceFactory.getProcessApi();

    @GET
    public Response processGet(@QueryParam("owner") String owner, @QueryParam("basecase") String basecase,
            @QueryParam("name") String name, @QueryParam("date") DateTimeParameter date,
            @QueryParam("creationDate") DateTimeParameter creationDate, @Context SecurityContext securityContext)
                    throws ApiException {
        return delegate.processGet(owner, basecase, name, date, creationDate, securityContext);
    }

    @GET
    @Path("/{processId}")
    public Response processProcessIdGet(@PathParam("processId") String processId,
            @Context SecurityContext securityContext) throws ApiException {
        return delegate.processProcessIdGet(processId, securityContext);
    }

    @GET
    @Path("/{processId}/{workflowId}")
    public Response processProcessIdWorkflowIdGet(@PathParam("processId") String processId,
            @PathParam("workflowId") String workflowId, @Context SecurityContext securityContext) throws ApiException {
        return delegate.processProcessIdWorkflowIdGet(processId, workflowId, securityContext);
    }
}
