package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import eu.itesla_project.modules.wca.WCACluster;
import eu.itesla_project.online.WcaContingenciesSynthesis;

public class WcaContingenciesMessageTest {

    @Test
    public void testToJson() {
      Map<String, WCACluster> arg1 = new HashMap<String, WCACluster>();
      WcaContingenciesSynthesis wcaContingencies = new WcaContingenciesSynthesis("id", arg1);
      WcaContingenciesMessage contingenciesMessage = new WcaContingenciesMessage(wcaContingencies);
      assertEquals("{\"body\":{\"workflowId\":\"id\",\"contingenciesCluster\":{}},\"type\":\"wcaContingencies\"}",
            contingenciesMessage.toJson());
    }

}
