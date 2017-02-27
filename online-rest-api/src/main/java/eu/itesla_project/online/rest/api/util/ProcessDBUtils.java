package eu.itesla_project.online.rest.api.util;

import java.util.List;
import java.util.Map;

import eu.itesla_project.online.rest.api.DateTimeParameter;
import eu.itesla_project.online.rest.model.Process;
import eu.itesla_project.online.rest.model.ProcessSynthesis;
import eu.itesla_project.online.rest.model.WorkflowResult;

public interface ProcessDBUtils {

    List<Process> getProcessList(String owner, String basecase, String name, DateTimeParameter date,
            DateTimeParameter creationDate) throws Exception;

    Process getProcess(String processId) throws Exception;

    WorkflowResult getWorkflowResult(String processId, String workflowId) throws Exception;

    ProcessSynthesis getSynthesis(String processId) throws Exception;

}
