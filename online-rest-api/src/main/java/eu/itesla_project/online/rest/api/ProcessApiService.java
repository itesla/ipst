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
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2016-10-06T14:01:02.692Z")
public abstract class ProcessApiService {
      public abstract Response processGet(String owner,String basecase,String name,DateTimeParameter date,DateTimeParameter creationDate,SecurityContext securityContext)
      throws ApiException;
      public abstract Response processProcessIdGet(String processId,SecurityContext securityContext)
      throws ApiException;
      public abstract Response processProcessIdWorkflowIdGet(String processId,String workflowId,SecurityContext securityContext)
      throws ApiException;
}
