package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import org.junit.Test;

import eu.itesla_project.online.ContingencyStatesIndexesSynthesis;

public class StatesWithIndexesSynthesisMessageTest {

    @Test
    public void testToJson() {

      ContingencyStatesIndexesSynthesis contingencyStatesIndexesSynthesis = new ContingencyStatesIndexesSynthesis();
      StatesWithIndexesSynthesisMessage indexesSynthesisMessage = new StatesWithIndexesSynthesisMessage(
            contingencyStatesIndexesSynthesis);
      assertEquals("{\"body\":{\"contingencyMap\":{},\"workflowId\":null},\"type\":\"statesWithIndexesSyntesis\"}",
            indexesSynthesisMessage.toJson());
    }

}
