package de.schrell.cadas.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.scene.input.MouseButton;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Stellt ausschließlich für ausdrücklich aktivierte Testläufe eine lokale HTTP-Brücke zur JavaFX-Oberfläche bereit.
 * Lesende Zustandsabfragen und verändernde Aktionen sind methodisch getrennt. Bis auf den inhaltsarmen Health-Check
 * benötigen alle Endpunkte ein starkes Sitzungstoken; Browseranfragen fremder Herkunft werden unabhängig davon
 * abgewiesen. Dateiaktionen bleiben auf eine beim Start kanonisch festgelegte Wurzel begrenzt.
 */
public final class AutomationBridgeServer {

    private static final Duration FX_ACTION_TIMEOUT = Duration.ofSeconds(30);
    private static final int MINIMUM_TOKEN_LENGTH = 32;
    private static final int MAXIMUM_QUERY_LENGTH = 65_536;
    private final CadWorkbench workbench;
    private final HttpServer server;
    private final String bearerToken;
    private final Path automationRoot;
    private final String serverOrigin;

    AutomationBridgeServer(CadWorkbench workbench, HttpServer server, String bearerToken, Path automationRoot) throws IOException {
        this.workbench = workbench;
        this.server = server;
        this.bearerToken = validateToken(bearerToken);
        this.automationRoot = automationRoot.toAbsolutePath().normalize().toRealPath();
        this.serverOrigin = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public static Optional<AutomationBridgeServer> startIfEnabled(CadWorkbench workbench) {
        boolean enabled = Boolean.parseBoolean(System.getProperty("cadas.automation.enabled", "false"))
                || "1".equals(System.getenv("CADAS_AUTOMATION"));
        if (!enabled) {
            return Optional.empty();
        }
        int port = Integer.getInteger("cadas.automation.port", 17845);
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            AutomationBridgeServer bridge = new AutomationBridgeServer(
                    workbench,
                    server,
                    configuredToken(),
                    configuredAutomationRoot()
            );
            bridge.registerContexts();
            server.start();
            workbench.automationSetErrorDialogsEnabled(false);
            workbench.automationSetStatusText("Geschützter Automatisierungszugriff aktiv auf http://127.0.0.1:" + port);
            return Optional.of(bridge);
        } catch (IOException exception) {
            throw new IllegalStateException("Automatisierungsserver konnte nicht gestartet werden.", exception);
        }
    }

    public void stop() {
        server.stop(0);
    }

    void registerContexts() {
        server.createContext("/health", exchange -> handleRead(exchange, false, () -> "{\"status\":\"ok\"}"));
        server.createContext("/state", exchange -> handleRead(
                exchange,
                true,
                () -> snapshotJson(callOnFx(workbench::automationSnapshot))
        ));
        server.createContext("/tool", exchange -> handleMutation(exchange, query -> {
            workbench.automationSetTool(required(query, "value"));
            return workbench.automationSnapshot();
        }));
        server.createContext("/level", exchange -> handleMutation(exchange, query -> {
            workbench.automationSelectLevel(required(query, "value"));
            return workbench.automationSnapshot();
        }));
        server.createContext("/workspace", exchange -> handleMutation(exchange, query -> {
            workbench.automationSetWorkspace(required(query, "value"));
            return workbench.automationSnapshot();
        }));
        server.createContext("/surfaceType", exchange -> handleMutation(exchange, query -> {
            workbench.automationSetSurfaceType(required(query, "value"));
            return workbench.automationSnapshot();
        }));
        server.createContext("/select", exchange -> handleMutation(exchange, query -> {
            workbench.automationSelect(
                    required(query, "kind"),
                    Integer.parseInt(query.getOrDefault("index", "0")),
                    parseBoolean(query.get("toggle"))
            );
            return workbench.automationSnapshot();
        }));
        server.createContext("/surfaceLayer", exchange -> handleMutation(exchange, query -> {
            workbench.automationSelectSurfaceLayer(Integer.parseInt(query.getOrDefault("index", "0")));
            return workbench.automationSnapshot();
        }));
        server.createContext("/field", exchange -> handleMutation(exchange, query -> {
            workbench.automationSetField(required(query, "name"), required(query, "value"));
            return workbench.automationSnapshot();
        }));
        server.createContext("/unit", exchange -> handleMutation(exchange, query -> {
            workbench.automationSetUnit(required(query, "name"), required(query, "value"));
            return workbench.automationSnapshot();
        }));
        server.createContext("/guide", exchange -> handleMutation(exchange, query -> {
            workbench.automationPlaceGuide(required(query, "orientation"), Double.parseDouble(required(query, "millimeters")));
            return workbench.automationSnapshot();
        }));
        server.createContext("/canvas/click", exchange -> handleMutation(exchange, query -> {
            workbench.automationCanvasClick(
                    Double.parseDouble(required(query, "x")),
                    Double.parseDouble(required(query, "y")),
                    mouseButton(query.getOrDefault("button", "PRIMARY")),
                    parseBoolean(query.get("shift")),
                    parseBoolean(query.get("shortcut")),
                    parseBoolean(query.get("alt"))
            );
            return workbench.automationSnapshot();
        }));
        server.createContext("/canvas/drag", exchange -> handleMutation(exchange, query -> {
            workbench.automationCanvasDrag(
                    Double.parseDouble(required(query, "fromX")),
                    Double.parseDouble(required(query, "fromY")),
                    Double.parseDouble(required(query, "toX")),
                    Double.parseDouble(required(query, "toY")),
                    mouseButton(query.getOrDefault("button", "PRIMARY")),
                    parseBoolean(query.get("shift")),
                    parseBoolean(query.get("shortcut")),
                    parseBoolean(query.get("alt"))
            );
            return workbench.automationSnapshot();
        }));
        server.createContext("/invoke", exchange -> handleMutation(exchange, query -> {
            WorkbenchAutomationSnapshot direct = workbench.automationInvoke(
                    required(query, "action"),
                    Optional.ofNullable(query.get("path")).map(this::validatedAutomationPath).orElse(null)
            );
            return direct != null ? direct : workbench.automationSnapshot();
        }));
    }

    private void handleMutation(HttpExchange exchange, FxSnapshotAction action) throws IOException {
        if (!acceptRequest(exchange, "POST", true)) {
            return;
        }
        try {
            WorkbenchAutomationSnapshot snapshot = callOnFx(() -> action.run(parseQuery(exchange.getRequestURI())));
            writeJson(exchange, 200, snapshotJson(snapshot));
        } catch (IllegalArgumentException exception) {
            writeJson(exchange, 400, errorJson(exception.getMessage()));
        } catch (Exception exception) {
            writeJson(exchange, 500, errorJson(exception.getMessage()));
        }
    }

    private void handleRead(HttpExchange exchange, boolean authenticationRequired, Supplier<String> response) throws IOException {
        if (!acceptRequest(exchange, "GET", authenticationRequired)) {
            return;
        }
        try {
            writeJson(exchange, 200, response.get());
        } catch (Exception exception) {
            writeJson(exchange, 500, errorJson(exception.getMessage()));
        }
    }

    private boolean acceptRequest(HttpExchange exchange, String expectedMethod, boolean authenticationRequired) throws IOException {
        if (!expectedMethod.equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", expectedMethod);
            writeJson(exchange, 405, errorJson("HTTP-Methode nicht erlaubt."));
            return false;
        }
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null && !MessageDigest.isEqual(
                origin.getBytes(StandardCharsets.UTF_8),
                serverOrigin.getBytes(StandardCharsets.UTF_8)
        )) {
            writeJson(exchange, 403, errorJson("Browser-Origin ist für die Automatisierung nicht zugelassen."));
            return false;
        }
        if (authenticationRequired && !authorized(exchange.getRequestHeaders().getFirst("Authorization"))) {
            exchange.getResponseHeaders().set("WWW-Authenticate", "Bearer");
            writeJson(exchange, 401, errorJson("Gültiges Automatisierungstoken erforderlich."));
            return false;
        }
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery != null && rawQuery.length() > MAXIMUM_QUERY_LENGTH) {
            writeJson(exchange, 414, errorJson("Abfrage ist zu lang."));
            return false;
        }
        return true;
    }

    private boolean authorized(String authorizationHeader) {
        String expected = "Bearer " + bearerToken;
        return authorizationHeader != null && MessageDigest.isEqual(
                authorizationHeader.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    private <T> T callOnFx(Callable<T> action) {
        if (Platform.isFxApplicationThread()) {
            try {
                return action.call();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                future.complete(action.call());
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return awaitAutomationResult(future);
    }

    static <T> T awaitAutomationResult(CompletableFuture<T> future) {
        return awaitAutomationResult(future, FX_ACTION_TIMEOUT);
    }

    static <T> T awaitAutomationResult(CompletableFuture<T> future, Duration timeout) {
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Warten auf Automatisierungsaktion wurde unterbrochen.", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Automatisierungsaktion konnte nicht abgeschlossen werden.", cause);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new IllegalStateException("Automatisierungsaktion wurde nach " + timeout.toMillis() + " ms abgebrochen.", exception);
        }
    }

    Path validatedAutomationPath(String value) {
        Path candidate = Path.of(value).toAbsolutePath().normalize();
        Path existingAncestor = candidate;
        while (existingAncestor != null && !Files.exists(existingAncestor)) {
            existingAncestor = existingAncestor.getParent();
        }
        try {
            if (existingAncestor == null || !existingAncestor.toRealPath().startsWith(automationRoot)) {
                throw new IllegalArgumentException("Dateipfad liegt außerhalb der Automatisierungswurzel.");
            }
            if (Files.exists(candidate) && !candidate.toRealPath().startsWith(automationRoot)) {
                throw new IllegalArgumentException("Dateipfad verweist außerhalb der Automatisierungswurzel.");
            }
            return candidate;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Dateipfad konnte nicht sicher geprüft werden.", exception);
        }
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> values = new LinkedHashMap<>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return values;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private String required(Map<String, String> query, String key) {
        String value = query.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Parameter `" + key + "` fehlt.");
        }
        return value;
    }

    private boolean parseBoolean(String value) {
        return Boolean.parseBoolean(Optional.ofNullable(value).orElse("false"));
    }

    private MouseButton mouseButton(String value) {
        return MouseButton.valueOf(value.toUpperCase());
    }

    private void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private String snapshotJson(WorkbenchAutomationSnapshot snapshot) {
        return "{"
                + "\"timestamp\":\"" + escapeJson(Instant.now().toString()) + "\","
                + "\"projectName\":\"" + escapeJson(snapshot.projectName()) + "\","
                + "\"activeLevel\":\"" + escapeJson(snapshot.activeLevel()) + "\","
                + "\"activeView\":\"" + escapeJson(snapshot.activeView()) + "\","
                + "\"activeTool\":\"" + escapeJson(snapshot.activeTool()) + "\","
                + "\"wallCount\":" + snapshot.wallCount() + ","
                + "\"roomCount\":" + snapshot.roomCount() + ","
                + "\"doorCount\":" + snapshot.doorCount() + ","
                + "\"windowCount\":" + snapshot.windowCount() + ","
                + "\"stairCount\":" + snapshot.stairCount() + ","
                + "\"selectionCount\":" + snapshot.selectionCount() + ","
                + "\"registeredCadLibraries\":" + snapshot.registeredCadLibraries() + ","
                + "\"threeDBodyCount\":" + snapshot.threeDBodyCount() + ","
                + "\"threeDHasContent\":" + snapshot.threeDHasContent() + ","
                + "\"threeDCameraStatus\":\"" + escapeJson(snapshot.threeDCameraStatus()) + "\","
                + "\"surfaceType\":\"" + escapeJson(snapshot.surfaceType()) + "\","
                + "\"surfaceTypeOptions\":\"" + escapeJson(snapshot.surfaceTypeOptions()) + "\","
                + "\"surfaceTargetLabel\":\"" + escapeJson(snapshot.surfaceTargetLabel()) + "\","
                + "\"surfaceSelectionHint\":\"" + escapeJson(snapshot.surfaceSelectionHint()) + "\","
                + "\"surfaceCoverageLabel\":\"" + escapeJson(snapshot.surfaceCoverageLabel()) + "\","
                + "\"selectedRoomMetrics\":\"" + escapeJson(snapshot.selectedRoomMetrics()) + "\","
                + "\"statusText\":\"" + escapeJson(snapshot.statusText()) + "\","
                + "\"zoom\":" + snapshot.zoom() + ","
                + "\"offsetX\":" + snapshot.offsetX() + ","
                + "\"offsetY\":" + snapshot.offsetY()
                + "}";
    }

    private String errorJson(String message) {
        return "{\"status\":\"error\",\"message\":\"" + escapeJson(Optional.ofNullable(message).orElse("Unbekannter Fehler")) + "\"}";
    }

    static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                default -> {
                    if (character < 0x20) {
                        escaped.append("\\u").append(String.format("%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static String configuredToken() {
        return Optional.ofNullable(System.getProperty("cadas.automation.token"))
                .or(() -> Optional.ofNullable(System.getenv("CADAS_AUTOMATION_TOKEN")))
                .orElseThrow(() -> new IllegalStateException(
                        "Für den Automatisierungsserver muss `cadas.automation.token` oder `CADAS_AUTOMATION_TOKEN` gesetzt sein."
                ));
    }

    private static Path configuredAutomationRoot() {
        String value = Optional.ofNullable(System.getProperty("cadas.automation.root"))
                .or(() -> Optional.ofNullable(System.getenv("CADAS_AUTOMATION_ROOT")))
                .orElse(System.getProperty("user.dir"));
        return Path.of(value);
    }

    private static String validateToken(String token) {
        if (token == null || token.length() < MINIMUM_TOKEN_LENGTH) {
            throw new IllegalArgumentException("Das Automatisierungstoken muss mindestens 32 Zeichen lang sein.");
        }
        return token;
    }

    @FunctionalInterface
    private interface FxSnapshotAction {
        WorkbenchAutomationSnapshot run(Map<String, String> query) throws Exception;
    }
}
