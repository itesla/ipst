package eu.itesla_project.online.server.message;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConnectionMessageTest {

    @Test
    public void testToJson() {
        ConnectionMessage connectionMessage = new ConnectionMessage(true);
        assertEquals("{\"body\":true,\"type\":\"connection\"}", connectionMessage.toJson());

    }

}
