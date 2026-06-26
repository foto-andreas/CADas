package de.schrell.cadas.ui;

import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.LineSegment;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.PipePath;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.PipePrimitive;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.QuarterArc;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingPoint;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingResult;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.Turn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final int TRAILING_CONNECTOR_PRIMITIVES = 2;
    private static final Path ROUTING_TEST_DIRECTORY = Path.of("src/test/resources/heizkreise");
    private static final DateTimeFormatter TEST_FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final HeatingCircuitCommandRouter router = new HeatingCircuitCommandRouter();
    private final Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
    private final TextField areaSizeField = new TextField("200x300");
    private final TextField spacingField = new TextField("10");
    private final TextArea protocolArea = new TextArea();
    private final CheckBox flowInvertedCheckBox = new CheckBox("V/R tauschen");
    private final CheckBox serpentineMiddleLineCheckBox = new CheckBox("Mittellinie schlängeln");
    private final Button undoButton = new Button("Rückgängig");
    private final Button redoButton = new Button("Wiederherstellen");
    private final Button generateVarioButton = new Button("Vario erzeugen");
    private final Button generateMeanderButton = new Button("Meander erzeugen");
    private final Button renderButton = new Button("Rendern");
    private final Button extendSupplyButton = new Button("VL +");
    private final Button shortenSupplyButton = new Button("VL -");
    private final Button extendReturnButton = new Button("RL +");
    private final Button shortenReturnButton = new Button("RL -");
    private final Button saveTestFileButton = new Button("Sichern");
    private final Label statusLabel = new Label();
    private final Deque<RoutingState> undoStack = new ArrayDeque<>();
    private final Deque<RoutingState> redoStack = new ArrayDeque<>();
    private final StringBuilder protocol = new StringBuilder();
    private final StringBuilder commands = new StringBuilder();
    private RoutingVariant routingVariant = RoutingVariant.MANUELL;
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
        stage.setMaximized(true);
        stage.show();
        Platform.runLater(protocolArea::requestFocus);
    }

    private BorderPane buildContent() {
        configureControls();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setTop(buildInputRow());
        StackPane canvasPane = new StackPane(canvas);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvas.widthProperty().addListener((ignored, oldValue, newValue) -> redraw());
        canvas.heightProperty().addListener((ignored, oldValue, newValue) -> redraw());
        root.setCenter(canvasPane);
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

        generateMeanderButton.setOnAction(event -> generateMeander());
        applyTooltip(generateMeanderButton, "Erzeugt aus dem aktuellen Heizbereich einen Meander-Verlauf. Der Schalter `Mittellinie schlängeln` ergänzt optional eine zweireihige Schlangenlinie in der Mitte.");

        extendSupplyButton.setOnAction(event -> extendPipeEnd('I', "Vorlauf"));
        applyTooltip(extendSupplyButton, "Verlängert das Vorlauf-Ende um ein gerades Rastersegment, indem ein `I` an die Routingsprache angehängt wird.");

        shortenSupplyButton.setOnAction(event -> shortenPipeEnd('I', "Vorlauf"));
        applyTooltip(shortenSupplyButton, "Kürzt das Vorlauf-Ende um ein gerades Rastersegment, wenn das letzte Vorlauf-Kommando ein `I` ist.");

        extendReturnButton.setOnAction(event -> extendPipeEnd('i', "Rücklauf"));
        applyTooltip(extendReturnButton, "Verlängert das Rücklauf-Ende um ein gerades Rastersegment, indem ein `i` an die Routingsprache angehängt wird.");

        shortenReturnButton.setOnAction(event -> shortenPipeEnd('i', "Rücklauf"));
        applyTooltip(shortenReturnButton, "Kürzt das Rücklauf-Ende um ein gerades Rastersegment, wenn das letzte Rücklauf-Kommando ein `i` ist.");

        saveTestFileButton.setOnAction(event -> saveRoutingTestFile());
        applyTooltip(saveTestFileButton, "Sichert den aktuellen Heizkreis als `.cadasfbh`-Testdatei unter `src/test/resources/heizkreise`, inklusive Maße, FBH-Parametern und Routing-Kommandos.");

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
                generateMeanderButton,
                extendSupplyButton,
                shortenSupplyButton,
                extendReturnButton,
                shortenReturnButton,
                saveTestFileButton,
                serpentineMiddleLineCheckBox,
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
        renderButton.setOnAction(event -> renderProtocolText());
        applyTooltip(renderButton, "Übernimmt den editierbaren Text aus dem Kommandoprotokoll, filtert gültige Routingbefehle und zeichnet den Heizkreis neu.");
        HBox protocolRow = new HBox(8.0, protocolArea, renderButton);
        HBox.setHgrow(protocolArea, Priority.ALWAYS);
        protocolRow.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(5.0, label, protocolRow);
        box.setPadding(new Insets(10, 0, 0, 0));
        return box;
    }

    private void configureControls() {
        areaSizeField.setPrefColumnCount(10);
        spacingField.setPrefColumnCount(5);
        protocolArea.setPrefRowCount(3);
        protocolArea.setWrapText(true);
        protocolArea.setEditable(true);
        protocolArea.setFocusTraversable(true);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle("-fx-text-fill: #5c5146;");

        areaSizeField.textProperty().addListener((ignored, oldValue, newValue) -> redraw());
        spacingField.textProperty().addListener((ignored, oldValue, newValue) -> redraw());
        flowInvertedCheckBox.selectedProperty().addListener((ignored, oldValue, newValue) -> redraw());
        canvas.setOnMouseClicked(event -> protocolArea.requestFocus());

        applyTooltip(areaSizeField, "Erfasst Breite und Länge des rechteckigen Heizbereichs in Zentimetern, zum Beispiel `200x300`.");
        applyTooltip(spacingField, "Legt den Verlegeabstand `v` in Zentimetern fest. Geraden sind `v` lang, Bögen besitzen den Durchmesser `v`.");
        applyTooltip(protocolArea, "Editierbarer Text der Routingkommandos: `I/R/L` für Vorlauf und `i/r/l` für Rücklauf. Ausschneiden, Kopieren und Einfügen funktionieren über die normalen Tastenkürzel; `Rendern` übernimmt den Text in die Zeichnung.");
        applyTooltip(serpentineMiddleLineCheckBox, "Erzeugt die Mitte beim nächsten Klick auf `Vario erzeugen` oder `Meander erzeugen` schlangenförmig. Die Schlangenlänge wird aus der Rasterdifferenz zwischen langer und kurzer Seite berechnet.");
        applyTooltip(flowInvertedCheckBox, "Tauscht die Darstellung von Vorlauf und Rücklauf, ohne eine HKV-Verbindung zu erzeugen.");
        updateUndoRedoButtons();
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

    RoutingPoint automationReturnEndPoint() {
        AreaSize size = parseAreaSize();
        double spacingMillimeters = parsePositiveCentimeters(spacingField.getText(), "Der Verlegeabstand") * 10.0;
        return currentRoutingResult(size, spacingMillimeters).returnPath().endPoint();
    }

    void automationGenerateVario() {
        generateVario();
    }

    void automationGenerateMeander() {
        generateMeander();
    }

    void automationExtendSupply() {
        extendPipeEnd('I', "Vorlauf");
    }

    void automationShortenSupply() {
        shortenPipeEnd('I', "Vorlauf");
    }

    void automationExtendReturn() {
        extendPipeEnd('i', "Rücklauf");
    }

    void automationShortenReturn() {
        shortenPipeEnd('i', "Rücklauf");
    }

    Path automationSaveRoutingTestFile() {
        return saveRoutingTestFile();
    }

    void automationSetAreaSize(String areaSizeText) {
        areaSizeField.setText(areaSizeText);
    }

    void automationSetSerpentineMiddleLine(boolean selected) {
        serpentineMiddleLineCheckBox.setSelected(selected);
    }

    void automationSetProtocolText(String text) {
        protocolArea.setText(text);
    }

    void automationRenderProtocolText() {
        renderProtocolText();
    }

    boolean automationProtocolEditable() {
        return protocolArea.isEditable();
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
            if (character == '+') {
                paintNextVarioEdge();
                continue;
            }
            if (character == '-') {
                removeLastVarioEdge();
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

    private void renderProtocolText() {
        String editedText = protocolArea.getText();
        StringBuilder renderedProtocol = new StringBuilder();
        StringBuilder renderedCommands = new StringBuilder();
        for (int index = 0; index < editedText.length(); index++) {
            char character = editedText.charAt(index);
            if (router.isIgnoredCharacter(character)) {
                continue;
            }
            if (router.isCommandCharacter(character)) {
                renderedProtocol.append(character);
                renderedCommands.append(character);
            } else {
                renderedProtocol.append('x');
            }
        }
        if (renderedProtocol.toString().contentEquals(protocol) && renderedCommands.toString().contentEquals(commands)) {
            redraw();
            return;
        }
        rememberUndoState();
        redoStack.clear();
        protocol.setLength(0);
        protocol.append(renderedProtocol);
        commands.setLength(0);
        commands.append(renderedCommands);
        routingVariant = RoutingVariant.MANUELL;
        updateProtocolArea();
        redraw();
        statusLabel.setText("Kommandotext gerendert.");
        Platform.runLater(protocolArea::requestFocus);
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
                    spacingMillimeters,
                    serpentineMiddleLineCheckBox.isSelected()
            );
            rememberUndoState();
            redoStack.clear();
            commands.setLength(0);
            commands.append(generatedCommands);
            protocol.setLength(0);
            protocol.append(generatedCommands);
            routingVariant = RoutingVariant.VARIO;
            rotationQuarterTurns = 0;
            areaSizeField.setText(formatCentimeters(sideMillimeters) + "x" + formatCentimeters(longSideMillimeters));
            updateProtocolArea();
            redraw();
            String serpentineText = serpentineMiddleLineCheckBox.isSelected()
                    ? " mit schlangenförmiger Mittellinie"
                    : "";
            statusLabel.setText("Vario" + serpentineText + " für " + formatCentimeters(sideMillimeters) + "x"
                    + formatCentimeters(longSideMillimeters) + " cm erzeugt.");
            Platform.runLater(protocolArea::requestFocus);
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private void generateMeander() {
        try {
            AreaSize size = parseAreaSize();
            double spacingMillimeters = parsePositiveCentimeters(spacingField.getText(), "Der Verlegeabstand") * 10.0;
            double sideMillimeters = Math.min(size.widthMillimeters(), size.heightMillimeters());
            double longSideMillimeters = Math.max(size.widthMillimeters(), size.heightMillimeters());
            String generatedCommands = router.meanderCommands(
                    size.widthMillimeters(),
                    size.heightMillimeters(),
                    spacingMillimeters,
                    serpentineMiddleLineCheckBox.isSelected()
            );
            rememberUndoState();
            redoStack.clear();
            commands.setLength(0);
            commands.append(generatedCommands);
            protocol.setLength(0);
            protocol.append(generatedCommands);
            routingVariant = RoutingVariant.MEANDER;
            rotationQuarterTurns = 0;
            areaSizeField.setText(formatCentimeters(sideMillimeters) + "x" + formatCentimeters(longSideMillimeters));
            updateProtocolArea();
            redraw();
            String serpentineText = serpentineMiddleLineCheckBox.isSelected()
                    ? " mit schlangenförmiger Mittellinie"
                    : "";
            statusLabel.setText("Meander" + serpentineText + " für " + formatCentimeters(sideMillimeters) + "x"
                    + formatCentimeters(longSideMillimeters) + " cm erzeugt.");
            Platform.runLater(protocolArea::requestFocus);
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private void paintNextVarioEdge() {
        try {
            VarioPaintState paintState = currentVarioPaintState();
            rememberUndoState();
            redoStack.clear();
            String step = paintState.nextStep();
            commands.append(step);
            protocol.append(step);
            routingVariant = RoutingVariant.VARIO;
            rotationQuarterTurns = 0;
            updateProtocolArea();
            redraw();
            statusLabel.setText("Vario-Seite gemalt.");
            Platform.runLater(protocolArea::requestFocus);
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private void removeLastVarioEdge() {
        try {
            VarioPaintState paintState = currentVarioPaintState();
            String step = paintState.lastStep();
            if (step.isEmpty()) {
                statusLabel.setText("Keine gemalte Vario-Seite zum Entfernen vorhanden.");
                Platform.runLater(protocolArea::requestFocus);
                return;
            }
            rememberUndoState();
            redoStack.clear();
            commands.delete(commands.length() - step.length(), commands.length());
            if (protocol.toString().endsWith(step)) {
                protocol.delete(protocol.length() - step.length(), protocol.length());
            } else {
                protocol.setLength(0);
                protocol.append(commands);
            }
            updateProtocolArea();
            redraw();
            statusLabel.setText("Vario-Seite entfernt.");
            Platform.runLater(protocolArea::requestFocus);
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private VarioPaintState currentVarioPaintState() {
        AreaSize size = parseAreaSize();
        double spacingMillimeters = parsePositiveCentimeters(spacingField.getText(), "Der Verlegeabstand") * 10.0;
        String generatedCommands = router.rectangularVarioCommands(
                size.widthMillimeters(),
                size.heightMillimeters(),
                spacingMillimeters,
                serpentineMiddleLineCheckBox.isSelected()
        );
        return VarioPaintState.of(splitVarioSteps(generatedCommands), commands.toString());
    }

    private List<String> splitVarioSteps(String generatedCommands) {
        List<String> edges = new ArrayList<>();
        int index = 0;
        while (index < generatedCommands.length()) {
            int startIndex = index;
            char command = generatedCommands.charAt(index);
            if (command == 'I' || command == 'i') {
                char lineCommand = command;
                while (index < generatedCommands.length() && generatedCommands.charAt(index) == lineCommand) {
                    index++;
                }
                if (index < generatedCommands.length() && samePipe(lineCommand, generatedCommands.charAt(index))) {
                    index++;
                }
            } else {
                while (index < generatedCommands.length()
                        && samePipe(command, generatedCommands.charAt(index))
                        && generatedCommands.charAt(index) != 'I'
                        && generatedCommands.charAt(index) != 'i') {
                    index++;
                }
            }
            edges.add(generatedCommands.substring(startIndex, index));
        }
        return groupVarioEdges(edges);
    }

    private List<String> groupVarioEdges(List<String> edges) {
        List<String> steps = new ArrayList<>();
        StringBuilder step = new StringBuilder();
        boolean hasSupply = false;
        boolean hasReturn = false;
        for (String edge : edges) {
            step.append(edge);
            if (edge.chars().anyMatch(Character::isUpperCase)) {
                hasSupply = true;
            } else {
                hasReturn = true;
            }
            if (hasSupply && hasReturn) {
                steps.add(step.toString());
                step.setLength(0);
                hasSupply = false;
                hasReturn = false;
            }
        }
        if (!step.isEmpty()) {
            if (steps.isEmpty()) {
                steps.add(step.toString());
            } else {
                int lastIndex = steps.size() - 1;
                steps.set(lastIndex, steps.get(lastIndex) + step);
            }
        }
        return steps;
    }

    private boolean samePipe(char leftCommand, char rightCommand) {
        return Character.isUpperCase(leftCommand) == Character.isUpperCase(rightCommand);
    }

    private record VarioPaintState(List<String> generatedSteps, int generatedStepCount, int additionalStepCount) {

        private static final String ADDITIONAL_STEP = "Ii";

        static VarioPaintState of(List<String> generatedSteps, String currentCommands) {
            StringBuilder prefix = new StringBuilder();
            int generatedStepCount = 0;
            for (String step : generatedSteps) {
                if (currentCommands.contentEquals(prefix)) {
                    break;
                }
                prefix.append(step);
                if (!currentCommands.startsWith(prefix.toString())) {
                    throw new IllegalArgumentException("Die aktuelle Eingabe ist kein Prefix des berechneten Vario-Routers.");
                }
                generatedStepCount++;
            }
            if (currentCommands.contentEquals(prefix)) {
                return new VarioPaintState(generatedSteps, generatedStepCount, 0);
            }
            if (generatedStepCount != generatedSteps.size()) {
                throw new IllegalArgumentException("Die aktuelle Eingabe ist kein Prefix des berechneten Vario-Routers.");
            }
            String extraCommands = currentCommands.substring(prefix.length());
            if (extraCommands.length() % ADDITIONAL_STEP.length() != 0) {
                throw new IllegalArgumentException("Die zusätzlichen Vario-Seiten sind nicht vollständig.");
            }
            for (int index = 0; index < extraCommands.length(); index += ADDITIONAL_STEP.length()) {
                if (!extraCommands.startsWith(ADDITIONAL_STEP, index)) {
                    throw new IllegalArgumentException("Die zusätzlichen Vario-Seiten passen nicht zum gemeinsamen VL/RL-Schritt.");
                }
            }
            return new VarioPaintState(
                    generatedSteps,
                    generatedStepCount,
                    extraCommands.length() / ADDITIONAL_STEP.length()
            );
        }

        String nextStep() {
            if (generatedStepCount < generatedSteps.size()) {
                return generatedSteps.get(generatedStepCount);
            }
            return ADDITIONAL_STEP;
        }

        String lastStep() {
            if (additionalStepCount > 0) {
                return ADDITIONAL_STEP;
            }
            if (generatedStepCount <= 0) {
                return "";
            }
            return generatedSteps.get(generatedStepCount - 1);
        }
    }

    private void extendPipeEnd(char command, String pipeName) {
        rememberUndoState();
        redoStack.clear();
        commands.append(command);
        protocol.append(command);
        updateProtocolArea();
        redraw();
        statusLabel.setText(pipeName + " um ein gerades Rastersegment verlängert.");
        Platform.runLater(protocolArea::requestFocus);
    }

    private void shortenPipeEnd(char command, String pipeName) {
        int commandIndex = lastPipeCommandIndex(command);
        if (commandIndex < 0 || commands.charAt(commandIndex) != command) {
            statusLabel.setText(pipeName + " kann nicht gekürzt werden, weil sein letztes Kommando kein gerades Segment ist.");
            Platform.runLater(protocolArea::requestFocus);
            return;
        }
        rememberUndoState();
        redoStack.clear();
        commands.deleteCharAt(commandIndex);
        int protocolIndex = lastIndexOf(protocol, command);
        if (protocolIndex >= 0) {
            protocol.deleteCharAt(protocolIndex);
        }
        updateProtocolArea();
        redraw();
        statusLabel.setText(pipeName + " um ein gerades Rastersegment gekürzt.");
        Platform.runLater(protocolArea::requestFocus);
    }

    private int lastPipeCommandIndex(char lineCommand) {
        boolean supply = Character.isUpperCase(lineCommand);
        for (int index = commands.length() - 1; index >= 0; index--) {
            if (Character.isUpperCase(commands.charAt(index)) == supply) {
                return index;
            }
        }
        return -1;
    }

    private int lastIndexOf(StringBuilder text, char character) {
        for (int index = text.length() - 1; index >= 0; index--) {
            if (text.charAt(index) == character) {
                return index;
            }
        }
        return -1;
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
        routingVariant = RoutingVariant.MANUELL;
        updateProtocolArea();
        redraw();
        Platform.runLater(protocolArea::requestFocus);
    }

    private Path saveRoutingTestFile() {
        try {
            if (commands.isEmpty()) {
                throw new IllegalArgumentException("Es gibt keine Routing-Kommandos zum Sichern.");
            }
            AreaSize size = parseAreaSize();
            double spacingMillimeters = parsePositiveCentimeters(spacingField.getText(), "Der Verlegeabstand") * 10.0;
            router.route(size.widthMillimeters(), size.heightMillimeters(), spacingMillimeters, commands.toString());
            Files.createDirectories(ROUTING_TEST_DIRECTORY);
            Path target = nextRoutingTestFile(size, spacingMillimeters);
            Files.writeString(target, routingTestFileContent(size, spacingMillimeters), StandardCharsets.UTF_8);
            statusLabel.setText("Heizkreis-Testdatei gesichert: " + target);
            Platform.runLater(protocolArea::requestFocus);
            return target;
        } catch (IOException exception) {
            statusLabel.setText("Heizkreis-Testdatei konnte nicht gesichert werden: " + exception.getMessage());
            Platform.runLater(protocolArea::requestFocus);
            return null;
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
            Platform.runLater(protocolArea::requestFocus);
            return null;
        }
    }

    private Path nextRoutingTestFile(AreaSize size, double spacingMillimeters) throws IOException {
        String sizePart = formatFileCentimeters(size.widthMillimeters())
                + "x" + formatFileCentimeters(size.heightMillimeters())
                + "_v" + formatFileCentimeters(spacingMillimeters);
        String baseName = "fbh_" + routingVariant.fileNamePart() + "_" + sizePart + "_"
                + LocalDateTime.now().format(TEST_FILE_TIMESTAMP_FORMAT);
        Path target = ROUTING_TEST_DIRECTORY.resolve(baseName + ".cadasfbh");
        int suffix = 2;
        while (Files.exists(target)) {
            target = ROUTING_TEST_DIRECTORY.resolve(baseName + "-" + suffix + ".cadasfbh");
            suffix++;
        }
        return target;
    }

    private String routingTestFileContent(AreaSize size, double spacingMillimeters) {
        return String.join(System.lineSeparator(),
                "# CADas FBH-Routing-Testdatei",
                "format=cadas-fbh-routing-v1",
                "breiteCm=" + formatFileCentimeters(size.widthMillimeters()),
                "höheCm=" + formatFileCentimeters(size.heightMillimeters()),
                "verlegeabstandCm=" + formatFileCentimeters(spacingMillimeters),
                "variante=" + routingVariant.fileValue(),
                "schlangenMittellinie=" + serpentineMiddleLineCheckBox.isSelected(),
                "vorlaufRücklaufGetauscht=" + flowInvertedCheckBox.isSelected(),
                "rotationViertel=" + rotationQuarterTurns,
                "generatorVergleich=" + (routingVariant != RoutingVariant.MANUELL),
                "kanonischeKommandos=" + canonicalCommands(commands.toString()),
                "kommandos=" + commands
        ) + System.lineSeparator();
    }

    private String canonicalCommands(String commandText) {
        return pipeCommands(commandText, true) + "|" + pipeCommands(commandText, false);
    }

    private String pipeCommands(String commandText, boolean supply) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < commandText.length(); index++) {
            char command = commandText.charAt(index);
            if (Character.isUpperCase(command) == supply) {
                result.append(command);
            }
        }
        return result.toString();
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
        double offset = verticalAlignmentScore(bounds, size.heightMillimeters())
                <= verticalAlignmentScore(bounds, size.heightMillimeters())
                ? 0
                : 0;
        return result.translatedBy(0.0, offset);
    }

    private double verticalAlignmentScore(Bounds bounds, double heightMillimeters) {
        double bottom = -heightMillimeters / 2.0;
        double top = heightMillimeters / 2.0;
        double shiftedMinY = bounds.minY() ;
        double shiftedMaxY = bounds.maxY();
        double overflow = Math.max(0.0, bottom - shiftedMinY) + Math.max(0.0, shiftedMaxY - top);
        double edgeDistance = Math.min(Math.abs(shiftedMinY - bottom), Math.abs(top - shiftedMaxY));
        return overflow * 1_000.0 + edgeDistance;
    }

    Bounds routeBounds(RoutingResult result) {
        return routeBounds(result, TRAILING_CONNECTOR_PRIMITIVES);
    }

    Bounds routeBounds(RoutingResult result, int trailingPrimitivesToIgnore) {
        return new Bounds(0.0, 0.0, 0.0, 0.0)
                .include(result.supplyPath(), trailingPrimitivesToIgnore)
                .include(result.returnPath(), trailingPrimitivesToIgnore);
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
        bounds = bounds.include(result.supplyPath(), TRAILING_CONNECTOR_PRIMITIVES);
        bounds = bounds.include(result.returnPath(), TRAILING_CONNECTOR_PRIMITIVES);
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

    private String formatFileCentimeters(double millimeters) {
        double centimeters = millimeters / 10.0;
        if (Math.abs(centimeters - Math.rint(centimeters)) < 0.0001) {
            return String.format(Locale.ROOT, "%.0f", centimeters);
        }
        return String.format(Locale.ROOT, "%.3f", centimeters)
                .replaceFirst("0+$", "")
                .replaceFirst("\\.$", "");
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

    private enum RoutingVariant {
        MANUELL("manuell", "Manuell"),
        VARIO("vario", "Vario"),
        MEANDER("meander", "Meander");

        private final String fileNamePart;
        private final String fileValue;

        RoutingVariant(String fileNamePart, String fileValue) {
            this.fileNamePart = fileNamePart;
            this.fileValue = fileValue;
        }

        private String fileNamePart() {
            return fileNamePart;
        }

        private String fileValue() {
            return fileValue;
        }
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

    record Bounds(double minX, double maxX, double minY, double maxY) {

        Bounds include(PipePath path) {
            return include(path, 0);
        }

        Bounds include(PipePath path, int trailingPrimitivesToIgnore) {
            if (trailingPrimitivesToIgnore <= 0 || path.primitives().size() <= trailingPrimitivesToIgnore) {
                Bounds result = this;
                for (PipePrimitive primitive : path.primitives()) {
                    result = result.include(primitive.startPoint()).include(primitive.endPoint());
                    if (primitive instanceof QuarterArc arc) {
                        result = result.include(arc);
                    }
                }
                return result;
            }
            Bounds result = include(path.startPoint());
            int includedPrimitiveCount = path.primitives().size() - trailingPrimitivesToIgnore;
            for (int index = 0; index < includedPrimitiveCount; index++) {
                PipePrimitive primitive = path.primitives().get(index);
                result = result.include(primitive.startPoint()).include(primitive.endPoint());
                if (primitive instanceof QuarterArc arc) {
                    result = result.include(arc);
                }
            }
            return result;
        }

        Bounds include(QuarterArc arc) {
            Bounds result = include(arc.startPoint()).include(arc.endPoint());
            double startAngle = Math.atan2(
                    arc.startPoint().yMillimeters() - arc.centerPoint().yMillimeters(),
                    arc.startPoint().xMillimeters() - arc.centerPoint().xMillimeters()
            );
            double endAngle = Math.atan2(
                    arc.endPoint().yMillimeters() - arc.centerPoint().yMillimeters(),
                    arc.endPoint().xMillimeters() - arc.centerPoint().xMillimeters()
            );
            if (startAngle < 0.0) {
                startAngle += Math.PI * 2.0;
            }
            if (endAngle < 0.0) {
                endAngle += Math.PI * 2.0;
            }
            for (double candidateAngle : new double[]{0.0, Math.PI / 2.0, Math.PI, Math.PI * 1.5}) {
                if (angleLiesOnArc(startAngle, endAngle, candidateAngle, arc.turn())) {
                    result = result.include(new RoutingPoint(
                            arc.centerPoint().xMillimeters() + Math.cos(candidateAngle) * arc.radiusMillimeters(),
                            arc.centerPoint().yMillimeters() + Math.sin(candidateAngle) * arc.radiusMillimeters()
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

        private boolean angleLiesOnArc(double startAngle, double endAngle, double candidateAngle, Turn turn) {
            double fullTurn = Math.PI * 2.0;
            double adjustedCandidate = candidateAngle;
            if (turn == Turn.RIGHT) {
                double adjustedEnd = endAngle > startAngle ? endAngle - fullTurn : endAngle;
                if (adjustedCandidate > startAngle) {
                    adjustedCandidate -= fullTurn;
                }
                return adjustedCandidate <= startAngle + 0.000_001 && adjustedCandidate >= adjustedEnd - 0.000_001;
            }
            double adjustedEnd = endAngle < startAngle ? endAngle + fullTurn : endAngle;
            if (adjustedCandidate < startAngle) {
                adjustedCandidate += fullTurn;
            }
            return adjustedCandidate >= startAngle - 0.000_001 && adjustedCandidate <= adjustedEnd + 0.000_001;
        }

        double width() {
            return maxX - minX;
        }

        double height() {
            return maxY - minY;
        }
    }
}
