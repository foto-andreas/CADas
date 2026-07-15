package de.schrell.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AutomationBridgeServerTest {

    private static final String TOKEN = "0123456789abcdef0123456789abcdef";

    @TempDir
    Path tempDir;

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

    @Test
    void begrenztWartenAufDieJavaFxAktion() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> AutomationBridgeServer.awaitAutomationResult(
                        new CompletableFuture<>(),
                        Duration.ofMillis(10)
                )
        );

        assertTrue(exception.getMessage().contains("10 ms"));
    }

    @Test
    void erzwingtHttpMethodenTokenUndBrowserOrigin() throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AutomationBridgeServer bridge = new AutomationBridgeServer(null, httpServer, TOKEN, tempDir);
        bridge.registerContexts();
        httpServer.start();
        URI baseUri = URI.create("http://127.0.0.1:" + httpServer.getAddress().getPort());
        try {
            assertEquals(200, request(baseUri.resolve("/health"), "GET", Map.of()));
            assertEquals(401, request(baseUri.resolve("/state"), "GET", Map.of()));
            assertEquals(405, request(baseUri.resolve("/tool?value=WALL"), "GET", Map.of(
                    "Authorization", "Bearer " + TOKEN
            )));
            assertEquals(401, request(baseUri.resolve("/tool?value=WALL"), "POST", Map.of()));
            assertEquals(403, rawRequest(baseUri.resolve("/tool?value=WALL"), "POST", Map.of(
                    "Authorization", "Bearer " + TOKEN,
                    "Origin", "https://angreifer.example"
            )));
        } finally {
            bridge.stop();
        }
    }

    @Test
    void begrenztDateipfadeKanonischAufDieAutomatisierungswurzel() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("freigegeben"));
        Path outside = Files.createDirectory(tempDir.resolve("ausserhalb"));
        Path link = root.resolve("verknuepfung");
        Files.createSymbolicLink(link, outside);
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AutomationBridgeServer bridge = new AutomationBridgeServer(null, httpServer, TOKEN, root);
        httpServer.start();
        try {
            assertEquals(root.resolve("neu.cadas"), bridge.validatedAutomationPath(root.resolve("neu.cadas").toString()));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> bridge.validatedAutomationPath(link.resolve("angriff.cadas").toString())
            );
        } finally {
            bridge.stop();
        }
    }

    @Test
    void lehntZuKurzeSitzungstokenAb() throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.start();
        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new AutomationBridgeServer(null, httpServer, "zu-kurz", tempDir)
            );
        } finally {
            httpServer.stop(0);
        }
    }

    private int request(URI uri, String method, Map<String, String> headers) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(2_000);
        connection.setReadTimeout(2_000);
        headers.forEach(connection::setRequestProperty);
        if (method.equals("POST")) {
            connection.setDoOutput(true);
            connection.getOutputStream().close();
        }
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }

    private int rawRequest(URI uri, String method, Map<String, String> headers) throws Exception {
        try (Socket socket = new Socket(uri.getHost(), uri.getPort())) {
            StringBuilder request = new StringBuilder(method)
                    .append(' ')
                    .append(uri.getRawPath());
            if (uri.getRawQuery() != null) {
                request.append('?').append(uri.getRawQuery());
            }
            request.append(" HTTP/1.1\r\nHost: ").append(uri.getHost()).append(':').append(uri.getPort()).append("\r\n");
            headers.forEach((name, value) -> request.append(name).append(": ").append(value).append("\r\n"));
            request.append("Content-Length: 0\r\nConnection: close\r\n\r\n");
            socket.getOutputStream().write(request.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String statusLine = new java.io.BufferedReader(
                    new java.io.InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)
            ).readLine();
            return Integer.parseInt(statusLine.split(" ")[1]);
        }
    }
}
