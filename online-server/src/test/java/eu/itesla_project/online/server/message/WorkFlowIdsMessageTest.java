package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

public class WorkFlowIdsMessageTest {

    @Test
    public void testToJson() {
      Collection<String> workflowIds = new ArrayList<>();
      WorkFlowIdsMessage flowIdsMessage = new WorkFlowIdsMessage(workflowIds);
      assertEquals("{\"body\":[],\"type\":\"workflowIds\"}", flowIdsMessage.toJson());

    }

}
