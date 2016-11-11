/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException>{

	@Override
	public Response toResponse(ApiException exception) {
		return Response.status(exception.getCode()).entity(exception.getMessage()).build();
	}

}