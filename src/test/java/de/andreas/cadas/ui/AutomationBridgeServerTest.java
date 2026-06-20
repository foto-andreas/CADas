package de.andreas.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
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

    @Test
    void reichtFachlicheLaufzeitfehlerUnverändertWeiter() {
        IllegalArgumentException expected = new IllegalArgumentException("Ungültiger Parameter");
        CompletableFuture<String> future = CompletableFuture.failedFuture(expected);

        IllegalArgumentException actual = assertThrows(
                IllegalArgumentException.class,
                () -> AutomationBridgeServer.awaitAutomationResult(future)
        );

        assertSame(expected, actual);
    }

    @Test
    void bewahrtInterruptStatusBeimAbbruch() throws InterruptedException {
        AtomicBoolean interruptPreserved = new AtomicBoolean();
        Thread waitingThread = new Thread(() -> {
            Thread.currentThread().interrupt();
            assertThrows(
                    IllegalStateException.class,
                    () -> AutomationBridgeServer.awaitAutomationResult(new CompletableFuture<>())
            );
            interruptPreserved.set(Thread.currentThread().isInterrupted());
        });

        waitingThread.start();
        waitingThread.join();

        assertTrue(interruptPreserved.get());
    }
}
