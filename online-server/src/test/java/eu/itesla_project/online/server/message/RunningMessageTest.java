package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import org.junit.Test;

public class RunningMessageTest {

    @Test
    public void testToJson() {
        RunningMessage message = new RunningMessage(true);
        assertEquals("{\"body\":true,\"type\":\"running\"}", message.toJson());
    }

}
