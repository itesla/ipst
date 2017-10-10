package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import org.junit.Test;

import eu.itesla_project.online.ContingencyStatesActionsSynthesis;

public class StatesWithActionsSynthesisMessageTest {

    @Test
    public void testToJson() {
      ContingencyStatesActionsSynthesis contingecyStatesActions = new ContingencyStatesActionsSynthesis();
      StatesWithActionsSynthesisMessage actionsSynthesisMessage = new StatesWithActionsSynthesisMessage(
            contingecyStatesActions);
      assertEquals("{\"body\":{\"contingencyMap\":{},\"workflowId\":null},\"type\":\"statesWithActionsSyntesis\"}",
            actionsSynthesisMessage.toJson());
    }

}
