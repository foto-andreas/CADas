package de.andreas.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AutomationBridgeServerTest {

    @Test
    void maskiertAlleJsonSteuerzeichen() {
        String value = "Anführungszeichen \" und \\ sowie \b\f\n\r\t\u0001";

        assertEquals(
                "Anführungszeichen \\\" und \\\\ sowie \\b\\f\\n\\r\\t\\u0001",
                AutomationBridgeServer.escapeJson(value)
        );
    }
}
