/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.api.impl;

import java.text.SimpleDateFormat;
import java.util.Objects;

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
import eu.itesla_project.online.rest.api.ApiResponseCodeEnum;
import eu.itesla_project.online.rest.api.ApiResponseMessage;
import eu.itesla_project.online.rest.api.DateTimeParameter;
import eu.itesla_project.online.rest.api.ProcessApiService;
import eu.itesla_project.online.rest.api.util.ProcessDBUtils;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
public class ProcessApiServiceImpl extends ProcessApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessApiServiceImpl.class);

    private final ProcessDBUtils utils;
    private final ObjectMapper objectMapper;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public ProcessApiServiceImpl(ProcessDBUtils utils) {
        this.utils = Objects.requireNonNull(utils);
        ;
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(sdf);
    }

    @Override
    public Response getProcessList(String owner, String basecase, String name, DateTimeParameter date,
            DateTimeParameter creationDate, SecurityContext securityContext) throws ApiException {
        LOGGER.info("Get process list: owner=" + owner + ", basecase=" + basecase + ", name=" + name + ", date=" + date
                + ", creationDate=" + creationDate);
        String res = null;
        try {
            res = objectMapper.writer()
                    .writeValueAsString(utils.getProcessList(owner, basecase, name, date, creationDate));
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            return Response.serverError().entity(new ApiResponseMessage(ApiResponseCodeEnum.ERROR, e.getMessage()))
                    .build();
        }
        return Response.ok().entity(res).build();
    }

    @Override
    public Response getProcessById(String processId, SecurityContext securityContext) throws ApiException {
        LOGGER.info("Get process : processId=" + processId);
        Process entity = utils.getProcess(processId);
        if (entity == null)
            return Response.status(Status.NOT_FOUND).entity("Process not found").build();
        String res = null;
        try {
            res = objectMapper.writer().writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            return Response.serverError().entity(new ApiResponseMessage(ApiResponseCodeEnum.ERROR, e.getMessage()))
                    .build();
        }
        return Response.ok().entity(res).build();
    }

    @Override
    public Response getWorkflowResult(String processId, String workflowId, SecurityContext securityContext)
            throws ApiException {
        LOGGER.info("Get workflow result : processId=" + processId + " ,workflowId=" + workflowId);
        WorkflowResult entity = utils.getWorkflowResult(processId, workflowId);
        if (entity == null)
            return Response.status(Status.NOT_FOUND).entity("Workflow not found").build();

        String res = null;
        try {
            res = objectMapper.writer().writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
            return Response.serverError().entity(new ApiResponseMessage(ApiResponseCodeEnum.ERROR, e.getMessage()))
                    .build();
        }

        return Response.ok().entity(res).build();
    }
}
