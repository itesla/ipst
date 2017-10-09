package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import org.junit.Test;

import eu.itesla_project.online.RunningSynthesis;

public class WcaRunningMessageTest {

    @Test
    public void testToJson() {
      RunningSynthesis wcaRunning = new RunningSynthesis();
      WcaRunningMessage message = new WcaRunningMessage(wcaRunning);
      assertEquals("{\"body\":{\"workflowId\":null,\"running\":false},\"type\":\"wcaRunning\"}", message.toJson());

    }

}
