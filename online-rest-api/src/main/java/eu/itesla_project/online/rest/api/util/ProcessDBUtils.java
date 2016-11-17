package eu.itesla_project.online.rest.api.util;

import java.util.List;

import eu.itesla_project.online.rest.api.DateTimeParameter;
import eu.itesla_project.online.rest.model.Process;
import eu.itesla_project.online.rest.model.WorkflowResult;

public interface ProcessDBUtils {

	List<Process> listProcesses(String owner, String basecase, String name, DateTimeParameter date,
			DateTimeParameter creationDate);

	Process getProcess(String processId);

	WorkflowResult getWorkflowResult(String processId, String workflowId);

}