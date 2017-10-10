package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import org.junit.Test;

import eu.itesla_project.online.StatusSynthesis;
import eu.itesla_project.online.WorkflowStatusEnum;

public class StatusMessageTest {

    @Test
    public void testToJson() {
      WorkflowStatusEnum status = WorkflowStatusEnum.DONE;
      StatusSynthesis body = new StatusSynthesis("id", status);
      StatusMessage message = new StatusMessage(body);
      assertEquals("{\"body\":{\"workflowId\":\"id\",\"status\":\"DONE\"},\"type\":\"status\"}", message.toJson());

    }

}
