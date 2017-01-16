/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
public abstract class ProcessApiService {
    public abstract Response getProcessList(String owner, String basecase, String name, DateTimeParameter date,
            DateTimeParameter creationDate, SecurityContext securityContext) throws ApiException;

    public abstract Response getProcessById(String processId, SecurityContext securityContext) throws ApiException;

    public abstract Response getWorkflowResult(String processId, String workflowId, SecurityContext securityContext)
            throws ApiException;
}
