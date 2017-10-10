package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import org.junit.Test;

import eu.itesla_project.online.IndexSecurityRulesResultsSynthesis;

public class StatesWithSecurityRulesResultSynthesisMessageTest {

    @Test
    public void testToJson() {
      IndexSecurityRulesResultsSynthesis contingencyIndexesResultsSynthesis = new IndexSecurityRulesResultsSynthesis(
            "id");
      StatesWithSecurityRulesResultSynthesisMessage message = new StatesWithSecurityRulesResultSynthesisMessage(
            contingencyIndexesResultsSynthesis);
      assertEquals(
            "{\"body\":{\"workflowId\":\"id\",\"contingencySecurityRulesMap\":{}},\"type\":\"statesWithSecurityRulesResultSyntesis\"}",
            message.toJson());

    }

}
