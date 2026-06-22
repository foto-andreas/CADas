package de.schrell.cadas.ui;

import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.LineSegment;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.PipePath;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.PipePrimitive;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.QuarterArc;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingPoint;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingResult;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.Turn;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.Window;

final class HeatingCircuitRoutingWindow {

    private static final double CANVAS_WIDTH = 980.0;
    private static final double CANVAS_HEIGHT = 620.0;
    private static final double VIEW_PADDING = 46.0;
    private static final Color SUPPLY_COLOR = Color.web("#d33b32");
    private static final Color RETURN_COLOR = Color.web("#1f62d0");
    private static final Color GROOVE_COLOR = Color.web("#d8dee3");
    private static final Color FIELD_COLOR = Color.web("#f7f3eb");
    private static final Color FIELD_BORDER_COLOR = Color.web("#6f7f8a");
    private static final double MINIMUM_ZOOM = 0.25;
    private static final double MAXIMUM_ZOOM = 6.0;
    private static final double ZOOM_STEP = 1.2;

    private final HeatingCircuitCommandRouter router = new HeatingCircuitCommandRouter();
    private final Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
    private final TextField areaSizeField = new TextField("200x300");
    private final TextField spacingField = new TextField("10");
    private final TextArea protocolArea = new TextArea();
    private final CheckBox flowInvertedCheckBox = new CheckBox("V/R tauschen");
    private final Button undoButton = new Button("Rückgängig");
    private final Button redoButton = new Button("Wiederherstellen");
    private final Button generateVarioButton = new Button("Vario erzeugen");
    private final Label statusLabel = new Label();
    private final Deque<RoutingState> undoStack = new ArrayDeque<>();
    private final Deque<RoutingState> redoStack = new ArrayDeque<>();
    private final StringBuilder protocol = new StringBuilder();
    private final StringBuilder commands = new StringBuilder();
    private double zoomFactor = 1.0;
    private int rotationQuarterTurns;

    void show(Window owner) {
        Stage stage = new Stage();
        stage.setTitle("Heizkreis-Router Vario");
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setScene(new Scene(buildContent(), 1100, 820));
        stage.setMinWidth(900);
        stage.setMinHeight(700);
        stage.show();
        Platform.runLater(protocolArea::requestFocus);
    }

    private BorderPane buildContent() {
        configureControls();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setTop(buildInputRow());
        root.setCenter(new StackPane(canvas));
        root.setBottom(buildProtocolArea());
        root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleWindowShortcut);
        redraw();
        return root;
    }

    private HBox buildInputRow() {
        undoButton.setOnAction(event -> undo());
        applyTooltip(undoButton, "Nimmt die letzte protokollierte Routingeingabe im Testfenster zurück.");

        redoButton.setOnAction(event -> redo());
        applyTooltip(redoButton, "Stellt eine zuvor zurückgenommene Routingeingabe im Testfenster wieder her.");

        Button rotateButton = new Button("90° drehen");
        rotateButton.setOnAction(event -> rotateArea());
        applyTooltip(rotateButton, "Vertauscht Länge und Breite des Test-Heizbereichs, um eine 90°-Drehung der Vario-Geometrie zu prüfen.");

        generateVarioButton.setOnAction(event -> generateVario());
        applyTooltip(generateVarioButton, "Erzeugt aus dem aktuellen Heizbereich eine Vario-Doppelspirale. Rechtecke werden auf schmale Seite mal lange Seite normalisiert.");

        Button clearButton = new Button("Kommandos löschen");
        clearButton.setOnAction(event -> clearCommands());
        applyTooltip(clearButton, "Löscht das Protokoll und startet die Routingeingabe wieder im Mittelpunkt des Heizbereichs.");

        HBox row = new HBox(
                10.0,
                new Label("Heizbereich cm"),
                areaSizeField,
                new Label("Verlegeabstand cm"),
                spacingField,
                undoButton,
                redoButton,
                rotateButton,
                generateVarioButton,
                flowInvertedCheckBox,
                clearButton,
                statusLabel
        );
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 10, 0));
        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        return row;
    }

    private VBox buildProtocolArea() {
        Label label = new Label("Kommandoprotokoll");
        VBox box = new VBox(5.0, label, protocolArea);
        box.setPadding(new Insets(10, 0, 0, 0));
        return box;
    }

    private void configureControls() {
        areaSizeField.setPrefColumnCount(10);
        spacingField.setPrefColumnCount(5);
        protocolArea.setPrefRowCount(3);
        protocolArea.setWrapText(true);
        protocolArea.setEditable(false);
        protocolArea.setFocusTraversable(true);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle("-fx-text-fill: #5c5146;");

        areaSizeField.textProperty().addListener((ignored, oldValue, newValue) -> redraw());
        spacingField.textProperty().addListener((ignored, oldValue, newValue) -> redraw());
        flowInvertedCheckBox.selectedProperty().addListener((ignored, oldValue, newValue) -> redraw());
        protocolArea.addEventFilter(KeyEvent.KEY_TYPED, this::handleCommandInput);
        protocolArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleControlKey);
        canvas.setOnMouseClicked(event -> protocolArea.requestFocus());

        applyTooltip(areaSizeField, "Erfasst Breite und Länge des rechteckigen Heizbereichs in Zentimetern, zum Beispiel `200x300`.");
        applyTooltip(spacingField, "Legt den Verlegeabstand `v` in Zentimetern fest. Geraden sind `v` lang, Bögen besitzen den Durchmesser `v`.");
        applyTooltip(protocolArea, "Nimmt Routingkommandos buchstabenweise an: `I/R/L` für Vorlauf und `i/r/l` für Rücklauf. Leerzeichen und Enter werden ignoriert, ungültige Zeichen erscheinen als `x`.");
        applyTooltip(flowInvertedCheckBox, "Tauscht die Darstellung von Vorlauf und Rücklauf, ohne eine HKV-Verbindung zu erzeugen.");
        updateUndoRedoButtons();
    }

    private void handleCommandInput(KeyEvent event) {
        applyInput(event.getCharacter());
        event.consume();
    }

    void automationInput(String text) {
        applyInput(text);
    }

    void automationUndo() {
        undo();
    }

    void automationRedo() {
        redo();
    }

    void automationZoomIn() {
        zoomIn();
    }

    void automationZoomOut() {
        zoomOut();
    }

    double automationZoomFactor() {
        return zoomFactor;
    }

    void automationRotateArea() {
        rotateArea();
    }

    RoutingPoint automationSupplyEndPoint() {
        AreaSize size = parseAreaSize();
        double spacingMillimeters = parsePositiveCentimeters(spacingField.getText(), "Der Verlegeabstand") * 10.0;
        return currentRoutingResult(size, spacingMillimeters).supplyPath().endPoint();
    }

    void automationGenerateVario() {
        generateVario();
    }

    String automationProtocol() {
        return protocol.toString();
    }

    String automationCommands() {
        return commands.toString();
    }

    String automationAreaSizeText() {
        return areaSizeField.getText();
    }

    private void applyInput(String text) {
        boolean changed = false;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (router.isIgnoredCharacter(character)) {
                continue;
            }
            if (!changed) {
                rememberUndoState();
                redoStack.clear();
                changed = true;
            }
            if (router.isCommandCharacter(character)) {
                commands.append(character);
                protocol.append(character);
            } else {
                protocol.append('x');
            }
        }
        if (changed) {
            updateProtocolArea();
            redraw();
        }
    }

    private void handleControlKey(KeyEvent event) {
        if (event.isShortcutDown() && event.getCode() == KeyCode.Z) {
            if (event.isShiftDown()) {
                redo();
            } else {
                undo();
            }
            event.consume();
            return;
        }
        if (event.isShortcutDown() && event.getCode() == KeyCode.Y) {
            redo();
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            event.consume();
        }
    }

    private void handleWindowShortcut(KeyEvent event) {
        if (!event.isShortcutDown() && !event.isControlDown()) {
            return;
        }
        if (isZoomInKey(event)) {
            zoomIn();
            event.consume();
            return;
        }
        if (isZoomOutKey(event)) {
            zoomOut();
            event.consume();
        }
    }

    private boolean isZoomInKey(KeyEvent event) {
        return event.getCode() == KeyCode.PLUS
                || event.getCode() == KeyCode.ADD
                || event.getCode() == KeyCode.EQUALS;
    }

    private boolean isZoomOutKey(KeyEvent event) {
        return event.getCode() == KeyCode.MINUS
                || event.getCode() == KeyCode.SUBTRACT;
    }

    private void zoomIn() {
        setZoom(zoomFactor * ZOOM_STEP);
    }

    private void zoomOut() {
        setZoom(zoomFactor / ZOOM_STEP);
    }

    private void setZoom(double newZoomFactor) {
        zoomFactor = Math.max(MINIMUM_ZOOM, Math.min(MAXIMUM_ZOOM, newZoomFactor));
        redraw();
    }

    private void generateVario() {
        try {
            AreaSize size = parseAreaSize();
            double spacingMillimeters = parsePositiveCentimeters(spacingField.getText(), "Der Verlegeabstand") * 10.0;
            double sideMillimeters = Math.min(size.widthMillimeters(), size.heightMillimeters());
            double longSideMillimeters = Math.max(size.widthMillimeters(), size.heightMillimeters());
            String generatedCommands = router.rectangularVarioCommands(
                    size.widthMillimeters(),
                    size.heightMillimeters(),
                    spacingMillimeters
            );
            rememberUndoState();
            redoStack.clear();
            commands.setLength(0);
            commands.append(generatedCommands);
            protocol.setLength(0);
            protocol.append(generatedCommands);
            rotationQuarterTurns = 0;
            areaSizeField.setText(formatCentimeters(sideMillimeters) + "x" + formatCentimeters(longSideMillimeters));
            updateProtocolArea();
            redraw();
            statusLabel.setText("Vario für " + formatCentimeters(sideMillimeters) + "x"
                    + formatCentimeters(longSideMillimeters) + " cm erzeugt.");
            Platform.runLater(protocolArea::requestFocus);
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private void clearCommands() {
        if (protocol.isEmpty() && commands.isEmpty()) {
            Platform.runLater(protocolArea::requestFocus);
            return;
        }
        rememberUndoState();
        redoStack.clear();
        commands.setLength(0);
        protocol.setLength(0);
        updateProtocolArea();
        redraw();
        Platform.runLater(protocolArea::requestFocus);
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        redoStack.push(currentState());
        restoreState(undoStack.pop());
        Platform.runLater(protocolArea::requestFocus);
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        undoStack.push(currentState());
        restoreState(redoStack.pop());
        Platform.runLater(protocolArea::requestFocus);
    }

    private void rememberUndoState() {
        undoStack.push(currentState());
        updateUndoRedoButtons();
    }

    private RoutingState currentState() {
        return new RoutingState(protocol.toString(), commands.toString());
    }

    private void restoreState(RoutingState state) {
        protocol.setLength(0);
        protocol.append(state.protocol());
        commands.setLength(0);
        commands.append(state.commands());
        updateProtocolArea();
        redraw();
    }

    private void updateProtocolArea() {
        protocolArea.setText(protocol.toString());
        protocolArea.positionCaret(protocolArea.getText().length());
        updateUndoRedoButtons();
    }

    private void updateUndoRedoButtons() {
        undoButton.setDisable(undoStack.isEmpty());
        redoButton.setDisable(redoStack.isEmpty());
    }

    private void rotateArea() {
        try {
            AreaSize size = parseAreaSize();
            rotationQuarterTurns = (rotationQuarterTurns + 1) % 4;
            areaSizeField.setText(formatCentimeters(size.heightMillimeters()) + "x" + formatCentimeters(size.widthMillimeters()));
            redraw();
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
        Platform.runLater(protocolArea::requestFocus);
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());
        try {
            AreaSize size = parseAreaSize();
            double spacingMillimeters = parsePositiveCentimeters(spacingField.getText(), "Der Verlegeabstand") * 10.0;
            RoutingResult result = currentRoutingResult(size, spacingMillimeters);
            ViewTransform transform = transformFor(size, result);
            drawField(gc, size, spacingMillimeters, transform);
            drawPath(gc, result.supplyPath(), SUPPLY_COLOR, transform);
            drawPath(gc, result.returnPath(), RETURN_COLOR, transform);
            drawEndpoint(gc, result.supplyPath().endPoint(), "VL", SUPPLY_COLOR, transform);
            drawEndpoint(gc, result.returnPath().endPoint(), "RL", RETURN_COLOR, transform);
            drawStartPoint(gc, result.supplyPath().startPoint(), transform);
            statusLabel.setText(String.format(
                    Locale.GERMANY,
                    "Bereit. Gültige Befehle: I/R/L und i/r/l. Zoom %.0f %%.",
                    zoomFactor * 100.0
            ));
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private RoutingResult currentRoutingResult(AreaSize size, double spacingMillimeters) {
        RoutingResult result = router.route(
                size.widthMillimeters(),
                size.heightMillimeters(),
                spacingMillimeters,
                commands.toString()
        ).withFlowInverted(flowInvertedCheckBox.isSelected());
        for (int index = 0; index < rotationQuarterTurns; index++) {
            result = result.rotatedClockwise();
        }
        return alignVerticallyToGrid(result, size, spacingMillimeters);
    }

    private void drawField(GraphicsContext gc, AreaSize size, double spacingMillimeters, ViewTransform transform) {
        double left = -size.widthMillimeters() / 2.0;
        double right = size.widthMillimeters() / 2.0;
        double bottom = -size.heightMillimeters() / 2.0;
        double top = size.heightMillimeters() / 2.0;
        Point2D topLeft = transform.screen(left, top);
        Point2D bottomRight = transform.screen(right, bottom);

        gc.setFill(FIELD_COLOR);
        gc.fillRect(topLeft.getX(), topLeft.getY(), bottomRight.getX() - topLeft.getX(), bottomRight.getY() - topLeft.getY());
        gc.setStroke(GROOVE_COLOR);
        gc.setLineWidth(1.0);
        for (double x = -snapDown(size.widthMillimeters() / 2.0, spacingMillimeters);
             x <= size.widthMillimeters() / 2.0 + 0.001;
             x += spacingMillimeters) {
            Point2D start = transform.screen(x, bottom);
            Point2D end = transform.screen(x, top);
            gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
        }
        for (double y = -snapDown(size.heightMillimeters() / 2.0, spacingMillimeters);
             y <= size.heightMillimeters() / 2.0 + 0.001;
             y += spacingMillimeters) {
            Point2D start = transform.screen(left, y);
            Point2D end = transform.screen(right, y);
            gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
        }
        gc.setStroke(FIELD_BORDER_COLOR);
        gc.setLineWidth(2.0);
        gc.strokeRect(topLeft.getX(), topLeft.getY(), bottomRight.getX() - topLeft.getX(), bottomRight.getY() - topLeft.getY());
    }

    private double snapDown(double value, double step) {
        return Math.floor(value / step) * step;
    }

    private void drawPath(GraphicsContext gc, PipePath path, Color color, ViewTransform transform) {
        gc.setStroke(color);
        gc.setLineWidth(3.0);
        for (PipePrimitive primitive : path.primitives()) {
            if (primitive instanceof LineSegment line) {
                Point2D start = transform.screen(line.startPoint());
                Point2D end = transform.screen(line.endPoint());
                gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
            } else if (primitive instanceof QuarterArc arc) {
                drawArc(gc, arc, transform);
            }
        }
    }

    private void drawArc(GraphicsContext gc, QuarterArc arc, ViewTransform transform) {
        List<RoutingPoint> points = sampledArc(arc);
        for (int index = 1; index < points.size(); index++) {
            Point2D start = transform.screen(points.get(index - 1));
            Point2D end = transform.screen(points.get(index));
            gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
        }
    }

    private List<RoutingPoint> sampledArc(QuarterArc arc) {
        double startAngle = Math.atan2(
                arc.startPoint().yMillimeters() - arc.centerPoint().yMillimeters(),
                arc.startPoint().xMillimeters() - arc.centerPoint().xMillimeters()
        );
        double sweep = arc.turn() == Turn.RIGHT ? -Math.PI / 2.0 : Math.PI / 2.0;
        List<RoutingPoint> points = new ArrayList<>();
        for (int index = 0; index <= 16; index++) {
            double angle = startAngle + sweep * index / 16.0;
            points.add(new RoutingPoint(
                    arc.centerPoint().xMillimeters() + Math.cos(angle) * arc.radiusMillimeters(),
                    arc.centerPoint().yMillimeters() + Math.sin(angle) * arc.radiusMillimeters()
            ));
        }
        return points;
    }

    private RoutingResult alignVerticallyToGrid(RoutingResult result, AreaSize size, double spacingMillimeters) {
        if (result.supplyPath().primitives().isEmpty() && result.returnPath().primitives().isEmpty()) {
            return result;
        }
        Bounds bounds = routeBounds(result);
        double halfSpacing = spacingMillimeters / 2.0;
        double upOffset = halfSpacing;
        double downOffset = -halfSpacing;
        double offset = verticalAlignmentScore(bounds, size.heightMillimeters(), downOffset)
                <= verticalAlignmentScore(bounds, size.heightMillimeters(), upOffset)
                ? downOffset
                : upOffset;
        return result.translatedBy(0.0, offset);
    }

    private double verticalAlignmentScore(Bounds bounds, double heightMillimeters, double offsetMillimeters) {
        double bottom = -heightMillimeters / 2.0;
        double top = heightMillimeters / 2.0;
        double shiftedMinY = bounds.minY() + offsetMillimeters;
        double shiftedMaxY = bounds.maxY() + offsetMillimeters;
        double overflow = Math.max(0.0, bottom - shiftedMinY) + Math.max(0.0, shiftedMaxY - top);
        double edgeDistance = Math.min(Math.abs(shiftedMinY - bottom), Math.abs(top - shiftedMaxY));
        return overflow * 1_000.0 + edgeDistance;
    }

    private Bounds routeBounds(RoutingResult result) {
        return new Bounds(0.0, 0.0, 0.0, 0.0)
                .include(result.supplyPath())
                .include(result.returnPath());
    }

    private void drawStartPoint(GraphicsContext gc, RoutingPoint startPoint, ViewTransform transform) {
        Point2D point = transform.screen(startPoint);
        gc.setFill(Color.web("#222222"));
        gc.fillOval(point.getX() - 4.0, point.getY() - 4.0, 8.0, 8.0);
    }

    private void drawEndpoint(GraphicsContext gc, RoutingPoint point, String label, Color color, ViewTransform transform) {
        Point2D screen = transform.screen(point);
        gc.setFill(color);
        gc.fillOval(screen.getX() - 5.0, screen.getY() - 5.0, 10.0, 10.0);
        gc.setFont(Font.font("Menlo", 12.0));
        gc.fillText(label, screen.getX() + 8.0, screen.getY() - 8.0);
    }

    private ViewTransform transformFor(AreaSize size, RoutingResult result) {
        Bounds bounds = new Bounds(
                -size.widthMillimeters() / 2.0,
                size.widthMillimeters() / 2.0,
                -size.heightMillimeters() / 2.0,
                size.heightMillimeters() / 2.0
        );
        bounds = bounds.include(result.supplyPath());
        bounds = bounds.include(result.returnPath());
        double width = Math.max(bounds.width(), 1.0);
        double height = Math.max(bounds.height(), 1.0);
        double scale = Math.min(
                (canvas.getWidth() - VIEW_PADDING * 2.0) / width,
                (canvas.getHeight() - VIEW_PADDING * 2.0) / height
        ) * zoomFactor;
        return new ViewTransform(
                (bounds.minX() + bounds.maxX()) / 2.0,
                (bounds.minY() + bounds.maxY()) / 2.0,
                scale,
                canvas.getWidth(),
                canvas.getHeight()
        );
    }

    private AreaSize parseAreaSize() {
        String normalized = areaSizeField.getText().trim().toLowerCase(Locale.ROOT).replace('×', 'x');
        String[] parts = normalized.split("x");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Das Heizbereichsmaß muss wie `200x300` eingegeben werden.");
        }
        double widthCentimeters = parsePositiveCentimeters(parts[0], "Die Heizbereichsbreite");
        double heightCentimeters = parsePositiveCentimeters(parts[1], "Die Heizbereichslänge");
        return new AreaSize(widthCentimeters * 10.0, heightCentimeters * 10.0);
    }

    private double parsePositiveCentimeters(String text, String label) {
        try {
            double value = Double.parseDouble(text.trim().replace(',', '.'));
            if (value <= 0.0) {
                throw new IllegalArgumentException(label + " muss größer als null sein.");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " muss eine gültige Zentimeterzahl sein.", exception);
        }
    }

    private String formatCentimeters(double millimeters) {
        double centimeters = millimeters / 10.0;
        if (Math.abs(centimeters - Math.rint(centimeters)) < 0.0001) {
            return String.format(Locale.GERMANY, "%.0f", centimeters);
        }
        return String.format(Locale.GERMANY, "%.2f", centimeters);
    }

    private void applyTooltip(javafx.scene.Node node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(360);
        Tooltip.install(node, tooltip);
    }

    private record AreaSize(double widthMillimeters, double heightMillimeters) {
    }

    private record RoutingState(String protocol, String commands) {
    }

    private record ViewTransform(
            double centerX,
            double centerY,
            double scale,
            double viewportWidth,
            double viewportHeight
    ) {

        Point2D screen(RoutingPoint point) {
            return screen(point.xMillimeters(), point.yMillimeters());
        }

        Point2D screen(double xMillimeters, double yMillimeters) {
            return new Point2D(
                    viewportWidth / 2.0 + (xMillimeters - centerX) * scale,
                    viewportHeight / 2.0 - (yMillimeters - centerY) * scale
            );
        }
    }

    private record Bounds(double minX, double maxX, double minY, double maxY) {

        Bounds include(PipePath path) {
            Bounds result = this;
            for (PipePrimitive primitive : path.primitives()) {
                result = result.include(primitive.startPoint()).include(primitive.endPoint());
                if (primitive instanceof QuarterArc arc) {
                    result = result.include(new RoutingPoint(
                            arc.centerPoint().xMillimeters() - arc.radiusMillimeters(),
                            arc.centerPoint().yMillimeters() - arc.radiusMillimeters()
                    )).include(new RoutingPoint(
                            arc.centerPoint().xMillimeters() + arc.radiusMillimeters(),
                            arc.centerPoint().yMillimeters() + arc.radiusMillimeters()
                    ));
                }
            }
            return result;
        }

        Bounds include(RoutingPoint point) {
            return new Bounds(
                    Math.min(minX, point.xMillimeters()),
                    Math.max(maxX, point.xMillimeters()),
                    Math.min(minY, point.yMillimeters()),
                    Math.max(maxY, point.yMillimeters())
            );
        }

        double width() {
            return maxX - minX;
        }

        double height() {
            return maxY - minY;
        }
    }
}
