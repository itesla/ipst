package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import org.junit.Test;

import eu.itesla_project.online.ContingencyStatesActionsSynthesis;
import eu.itesla_project.online.IndexSecurityRulesResultsSynthesis;
import eu.itesla_project.online.WorkflowStatusEnum;
import eu.itesla_project.online.server.OnlineWorkflowInfo;

public class SelectedWorkFlowInfoMessageTest {

@Test
public void testToJson() {
      OnlineWorkflowInfo workflow = new OnlineWorkflowInfo("id");
      workflow.setWcaRunning(true);
      IndexSecurityRulesResultsSynthesis indexesSecurityRulesApplicatio = new IndexSecurityRulesResultsSynthesis("id");
      workflow.setSecurityRulesIndexesApplication(indexesSecurityRulesApplicatio);
      ContingencyStatesActionsSynthesis statesActions = new ContingencyStatesActionsSynthesis("id");
      workflow.setStatesActions(statesActions);
      WorkflowStatusEnum workflowStatusEnum = WorkflowStatusEnum.DONE;
      workflow.setStatus(workflowStatusEnum);
      // fail("Not yet implemented");
      SelectedWorkFlowInfoMessage flowInfoMessage = new SelectedWorkFlowInfoMessage(workflow);

      assertEquals(
            "{\"body\":{\"worflowId\":\"id\",\"status\":\"DONE\",\"wcaRunning\":true,\"workStatus\":{},\"statesActions\":{\"contingencyMap\":{},\"workflowId\":\"id\"},\"unsafeContingencies\":null,\"securityRulesIndexesApplication\":{\"workflowId\":\"id\",\"contingencySecurityRulesMap\":{}},\"wcaContingencies\":null},\"type\":\"selectedWorkFlowInfo\"}",
            flowInfoMessage.toJson());

       }

}
