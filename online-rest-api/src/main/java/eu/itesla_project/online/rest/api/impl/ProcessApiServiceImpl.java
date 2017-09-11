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
import eu.itesla_project.online.rest.model.ProcessSynthesis;
import eu.itesla_project.online.rest.model.WorkflowResult;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.core.SecurityContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import eu.itesla_project.online.rest.api.DateTimeParameter;
import eu.itesla_project.online.rest.api.ProcessApiService;
import eu.itesla_project.online.rest.api.util.ProcessDBUtils;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
public class ProcessApiServiceImpl implements ProcessApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessApiServiceImpl.class);

    private final ProcessDBUtils utils;
    private final ObjectMapper objectMapper;

    public ProcessApiServiceImpl(ProcessDBUtils utils) {
        this.utils = Objects.requireNonNull(utils);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
    }

    @Override
    public Response getProcessList(String owner, String basecase, String name, DateTimeParameter date,
            DateTimeParameter creationDate, SecurityContext securityContext) {
        LOGGER.info("Get process list: owner=" + owner + ", basecase=" + basecase + ", name=" + name + ", date=" + date
                + ", creationDate=" + creationDate);
        String res = null;
        try {
            res = objectMapper.writer()
                    .writeValueAsString(utils.getProcessList(owner, basecase, name, date, creationDate));
            return Response.ok().entity(res).build();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response getProcessById(String processId, SecurityContext securityContext) {
        LOGGER.info("Get process : processId=" + processId);
        if (processId == null) {
            return Response.status(Status.BAD_REQUEST).entity("null proceesId parameter").build();
        }
        try {
            Process entity = utils.getProcess(processId);
            if (entity == null) {
                return Response.status(Status.NOT_FOUND).entity("Process not found").build();
            }
            String res = objectMapper.writer().writeValueAsString(entity);
            return Response.ok().entity(res).build();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

    }

    @Override
    public Response getWorkflowResult(String processId, String workflowId, SecurityContext securityContext) {
        LOGGER.info("Get workflow result : processId=" + processId + " ,workflowId=" + workflowId);
        if (processId == null) {
            return Response.status(Status.BAD_REQUEST).entity("null proceesId parameter").build();
        }
        if (workflowId == null) {
            return Response.status(Status.BAD_REQUEST).entity("null workflowId parameter").build();
        }

        try {
            WorkflowResult entity = utils.getWorkflowResult(processId, workflowId);
            if (entity == null) {
                return Response.status(Status.NOT_FOUND).entity("Workflow not found").build();
            }

            String res = objectMapper.writer().writeValueAsString(entity);
            return Response.ok().entity(res).build();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response getProcessSynthesis(String processId, SecurityContext securityContext) {
        LOGGER.info("Get process synthesis : processId=" + processId);
        if (processId == null) {
            return Response.status(Status.BAD_REQUEST).entity("Null proceesId parameter").build();
        }
        try {
            ProcessSynthesis entity = utils.getSynthesis(processId);
            if (entity == null) {
                return Response.status(Status.NOT_FOUND).entity("Process not found").build();
            }
            String res = objectMapper.writer().writeValueAsString(entity);
            return Response.ok().entity(res).build();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

    }

}
