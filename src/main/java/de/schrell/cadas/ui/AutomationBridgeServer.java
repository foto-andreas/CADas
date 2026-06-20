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
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class AutomationBridgeServer {

    private final CadWorkbench workbench;
    private final HttpServer server;

    private AutomationBridgeServer(CadWorkbench workbench, HttpServer server) {
        this.workbench = workbench;
        this.server = server;
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
            AutomationBridgeServer bridge = new AutomationBridgeServer(workbench, server);
            bridge.registerContexts();
            server.start();
            workbench.automationSetErrorDialogsEnabled(false);
            workbench.automationSetStatusText("Automatisierungszugriff aktiv auf http://127.0.0.1:" + port);
            return Optional.of(bridge);
        } catch (IOException exception) {
            throw new IllegalStateException("Automatisierungsserver konnte nicht gestartet werden.", exception);
        }
    }

    public void stop() {
        server.stop(0);
    }

    private void registerContexts() {
        server.createContext("/health", exchange -> writeJson(exchange, 200, "{\"status\":\"ok\"}"));
        server.createContext("/state", exchange -> writeJson(exchange, 200, snapshotJson(callOnFx(workbench::automationSnapshot))));
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
                    Optional.ofNullable(query.get("path")).map(Path::of).orElse(null)
            );
            return direct != null ? direct : workbench.automationSnapshot();
        }));
    }

    private void handleMutation(HttpExchange exchange, FxSnapshotAction action) throws IOException {
        try {
            WorkbenchAutomationSnapshot snapshot = callOnFx(() -> action.run(parseQuery(exchange.getRequestURI())));
            writeJson(exchange, 200, snapshotJson(snapshot));
        } catch (IllegalArgumentException exception) {
            writeJson(exchange, 400, errorJson(exception.getMessage()));
        } catch (Exception exception) {
            writeJson(exchange, 500, errorJson(exception.getMessage()));
        }
    }

    private <T> T callOnFx(FxCallable<T> action) {
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
        try {
            return future.get();
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

    @FunctionalInterface
    private interface FxCallable<T> {
        T call() throws Exception;
    }

    @FunctionalInterface
    private interface FxSnapshotAction {
        WorkbenchAutomationSnapshot run(Map<String, String> query) throws Exception;
    }
}
