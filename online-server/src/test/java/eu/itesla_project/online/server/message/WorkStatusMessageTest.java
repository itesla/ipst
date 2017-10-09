package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import org.junit.Test;

import eu.itesla_project.online.WorkSynthesis;

public class WorkStatusMessageTest {

    @Test
    public void testToJson() {
      WorkSynthesis status = new WorkSynthesis();
      WorkStatusMessage message = new WorkStatusMessage(status);
      assertEquals("{\"body\":{\"workflowId\":null,\"status\":null},\"type\":\"workStatus\"}", message.toJson());
    }

}
