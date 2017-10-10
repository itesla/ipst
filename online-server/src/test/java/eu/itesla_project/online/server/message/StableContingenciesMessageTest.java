package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import org.junit.Test;

import eu.itesla_project.online.StableContingenciesSynthesis;

public class StableContingenciesMessageTest {

    @Test
    public void testToJson() {
      StableContingenciesSynthesis stableContingencies = new StableContingenciesSynthesis();
      StableContingenciesMessage contingenciesMessage = new StableContingenciesMessage(stableContingencies);
      assertEquals("{\"body\":{\"workflowId\":null,\"contingencies\":null},\"type\":\"stableContingencies\"}",
            contingenciesMessage.toJson());

   }

}
