package eu.itesla_project.online.server.message;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import eu.itesla_project.modules.wca.WCACluster;
import eu.itesla_project.online.UnstableContingenciesSynthesis;

public class UnstableContingenciesMessageTest {

    @Test
    public void testToJson() {
      Map<String, WCACluster> map = new HashMap<>();
      UnstableContingenciesSynthesis unstableContingencies = new UnstableContingenciesSynthesis("str", map);
      UnstableContingenciesMessage contingenciesMessage = new UnstableContingenciesMessage(unstableContingencies);
      assertEquals(
            "{\"body\":{\"workflowId\":\"str\",\"contingenciesCluster\":{}},\"type\":\"unstableContingencies\"}",
            contingenciesMessage.toJson());

    }

}
