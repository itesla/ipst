package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

import eu.itesla_project.online.server.OnlineWorkflowInfo;

public class WorkflowListMessageTest {

    @Test
    public void testToJson() {
      HashMap<String, OnlineWorkflowInfo> workflows = new HashMap<>();
      WorkflowListMessage listMessage = new WorkflowListMessage(workflows);
      assertEquals("{\"body\":{},\"type\":\"workflows\"}", listMessage.toJson());
    }

}
