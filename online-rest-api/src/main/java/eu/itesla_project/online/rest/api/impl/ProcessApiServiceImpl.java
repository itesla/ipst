/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.api.impl;


import java.text.SimpleDateFormat;
import eu.itesla_project.online.rest.model.Process;
import eu.itesla_project.online.rest.model.WorkflowResult;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.core.SecurityContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import eu.itesla_project.online.rest.api.ApiException;
import eu.itesla_project.online.rest.api.DateTimeParameter;
import eu.itesla_project.online.rest.api.ProcessApiService;
import eu.itesla_project.online.rest.api.util.OnlineDBUtils;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2016-10-06T14:01:02.692Z")
public class ProcessApiServiceImpl extends ProcessApiService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessApiServiceImpl.class);

	private OnlineDBUtils utils;
	private ObjectMapper objectMapper;
	private final SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	public ProcessApiServiceImpl() {

			utils= new OnlineDBUtils();
			objectMapper = new ObjectMapper();
			objectMapper.registerModule(new JodaModule());
			objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.
				    WRITE_DATES_AS_TIMESTAMPS , false);
			objectMapper.setDateFormat(sdf);		
	}
	
	
      @Override
      public Response processGet(String user,String basecase,String name,DateTimeParameter date,DateTimeParameter creationDate,SecurityContext securityContext)
      throws ApiException {
    	 LOGGER.info("Get process list: user="+user+", basecase="+basecase+", name="+name+", date="+date+", creationDate="+creationDate);
    	 System.out.println("Get process list: user="+user+", basecase="+basecase+", name="+name+", date="+date+", creationDate="+creationDate);
    	
			String res=null;
			try {
				res=objectMapper.writer().writeValueAsString(utils.listProcesses( user, basecase, name, date, creationDate));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				LOGGER.error(e.getMessage(),e);
				throw new ApiException(500,e.getMessage());
			}
			
      return Response.ok().entity(res).build();
  }
      @Override
      public Response processProcessIdGet(String processId,SecurityContext securityContext)
      throws ApiException {
     	 LOGGER.info("Get process : processId="+processId);

    	  Process entity= utils.getProcess(processId);
    	  if(entity==null)
    		  return Response.status(Status.NOT_FOUND).entity("Process not found").build();
    	  String res=null;
			try {
				res=objectMapper.writer().writeValueAsString(entity);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				LOGGER.error(e.getMessage(),e);
				throw new ApiException(500,e.getMessage());
			}
      return Response.ok().entity(res).build();
  }
      @Override
      public Response processProcessIdWorkflowIdGet(String processId,String workflowId,SecurityContext securityContext)
      throws ApiException {
    	  LOGGER.info("Get workflow result : processId="+processId +" ,workflowId="+workflowId);
    	  WorkflowResult entity= utils.getWorkflowResult(processId,workflowId);
    	  if(entity==null)
    		  return Response.status(Status.NOT_FOUND).entity("Workflow not found").build();
    	  
    	  String res=null;
			try {
				res=objectMapper.writer().writeValueAsString(entity);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				LOGGER.error(e.getMessage(),e);
				throw new ApiException(500,e.getMessage());
			}
    	  
      return Response.ok().entity(res).build();
  }
}
