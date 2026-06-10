package de.andreas.cadas.ui;

import de.andreas.cadas.application.drawing.DraftingConstraints;
import de.andreas.cadas.application.drawing.DraftingService;
import de.andreas.cadas.application.drawing.SnapService;
import de.andreas.cadas.domain.geometry.Angle;
import de.andreas.cadas.domain.geometry.Grid;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Wall;

import java.util.Locale;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public final class CadWorkbench extends BorderPane {

    private static final double BASE_PIXELS_PER_MILLIMETER = 0.10;
    private static final double RULER_SIZE = 32.0;
    private static final Length DEFAULT_GRID = Length.of(25, LengthUnit.CENTIMETER);
    private static final Length DEFAULT_WALL_THICKNESS = Length.of(17.5, LengthUnit.CENTIMETER);
    private static final Length SNAP_TOLERANCE = Length.of(12, LengthUnit.CENTIMETER);

    private final DraftingService draftingService = new DraftingService();
    private final SnapService snapService = new SnapService();
    private final ProjectModel project = ProjectModel.withDefaultLevel("Neues Projekt", "Erdgeschoss");

    private final ObjectProperty<Level> activeLevel = new SimpleObjectProperty<>(project.primaryLevel());
    private final ObjectProperty<ViewOrientation> activeView = new SimpleObjectProperty<>(ViewOrientation.TOP);
    private final BooleanProperty showGrid = new SimpleBooleanProperty(true);
    private final BooleanProperty snapToGrid = new SimpleBooleanProperty(true);
    private final BooleanProperty snapToEndpoints = new SimpleBooleanProperty(true);
    private final BooleanProperty showCompass = new SimpleBooleanProperty(true);
    private final BooleanProperty showDimensions = new SimpleBooleanProperty(true);

    private final Canvas drawingCanvas = new Canvas();
    private final Canvas horizontalRuler = new Canvas();
    private final Canvas verticalRuler = new Canvas();
    private final Pane drawingPane = new Pane(drawingCanvas);

    private final TextField gridField = new TextField("25");
    private final ComboBox<LengthUnit> gridUnit = new ComboBox<>();
    private final TextField lengthField = new TextField();
    private final ComboBox<LengthUnit> lengthUnit = new ComboBox<>();
    private final TextField angleField = new TextField();
    private final TextField wallThicknessField = new TextField("17,5");
    private final ComboBox<LengthUnit> wallThicknessUnit = new ComboBox<>();

    private final Label zoomLabel = new Label();
    private final Label cursorLabel = new Label();
    private final Label draftLabel = new Label();
    private final Label viewLabel = new Label();

    private double zoom = 1.0;
    private double offsetX = 240.0;
    private double offsetY = 160.0;
    private double panStartX;
    private double panStartY;
    private double panOriginX;
    private double panOriginY;
    private boolean panning;
    private PlanPoint draftStart;
    private PlanSegment previewSegment;
    private PlanPoint lastCursor = new PlanPoint(0.0, 0.0);

    public CadWorkbench() {
        setPadding(new Insets(12));
        setStyle("-fx-background-color: linear-gradient(to bottom, #f6f1e8, #ece5d8);");

        configureControls();
        configureLayout();
        configureCanvas();
        updateStatus();
        render();
    }

    private void configureControls() {
        gridUnit.getItems().addAll(LengthUnit.values());
        gridUnit.setValue(LengthUnit.CENTIMETER);
        lengthUnit.getItems().addAll(LengthUnit.values());
        lengthUnit.setValue(LengthUnit.CENTIMETER);
        wallThicknessUnit.getItems().addAll(LengthUnit.values());
        wallThicknessUnit.setValue(LengthUnit.CENTIMETER);

        applyTooltip(gridField, "Legt die Rasterweite für die Zeichenfläche fest. Werte werden mit der gewählten Einheit interpretiert.");
        applyTooltip(gridUnit, "Bestimmt die Einheit für die Rasterweite, damit Eingaben in Millimeter, Zentimeter oder Meter erfolgen können.");
        applyTooltip(lengthField, "Optionaler Längenwert für die gerade gezeichnete Wand. Wenn ein Wert eingetragen ist, wird die Wand auf diese Länge gesetzt.");
        applyTooltip(lengthUnit, "Bestimmt die Einheit für die manuelle Längeneingabe während des Zeichnens.");
        applyTooltip(angleField, "Optionaler Winkel in Grad für die aktuelle Wand. Ohne Eingabe bleibt der orthogonale 90°-Modus aktiv.");
        applyTooltip(wallThicknessField, "Definiert die Wandstärke für neu gezeichnete Wände.");
        applyTooltip(wallThicknessUnit, "Bestimmt die Einheit für die Wandstärke.");

        showGrid.addListener((ignored, oldValue, newValue) -> render());
        snapToGrid.addListener((ignored, oldValue, newValue) -> render());
        snapToEndpoints.addListener((ignored, oldValue, newValue) -> render());
        showCompass.addListener((ignored, oldValue, newValue) -> render());
        showDimensions.addListener((ignored, oldValue, newValue) -> render());
        activeView.addListener((ignored, oldValue, newValue) -> render());
    }

    private void configureLayout() {
        ToolBar settingsBar = buildSettingsBar();
        HBox viewBar = buildViewBar();
        VBox topArea = new VBox(10.0, settingsBar, viewBar);
        topArea.setPadding(new Insets(0, 0, 12, 0));
        setTop(topArea);

        BorderPane drawingArea = new BorderPane();
        drawingArea.setTop(horizontalRuler);
        drawingArea.setLeft(verticalRuler);
        drawingArea.setCenter(new StackPane(drawingPane));
        drawingArea.setStyle("-fx-background-color: rgba(255,255,255,0.55); -fx-background-radius: 16;");
        setCenter(drawingArea);

        HBox statusBar = new HBox(18.0, viewLabel, zoomLabel, cursorLabel, draftLabel);
        statusBar.setPadding(new Insets(10, 16, 0, 16));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-font-size: 12px; -fx-text-fill: #2f2a24;");
        setBottom(statusBar);
    }

    private ToolBar buildSettingsBar() {
        CheckBox rasterBox = new CheckBox("Raster");
        rasterBox.selectedProperty().bindBidirectional(showGrid);
        applyTooltip(rasterBox, "Blendet das maßstäbliche Raster der Zeichenfläche ein oder aus.");

        CheckBox snapRasterBox = new CheckBox("Snap Raster");
        snapRasterBox.selectedProperty().bindBidirectional(snapToGrid);
        applyTooltip(snapRasterBox, "Aktiviert das magnetische Einrasten auf das konfigurierte Raster.");

        CheckBox snapPointsBox = new CheckBox("Snap Punkte");
        snapPointsBox.selectedProperty().bindBidirectional(snapToEndpoints);
        applyTooltip(snapPointsBox, "Aktiviert das magnetische Einrasten auf vorhandene Linien-Endpunkte.");

        CheckBox compassBox = new CheckBox("Nordpfeil");
        compassBox.selectedProperty().bindBidirectional(showCompass);
        applyTooltip(compassBox, "Blendet einen Nordpfeil in der Zeichenfläche ein oder aus.");

        CheckBox dimensionsBox = new CheckBox("Bemaßung");
        dimensionsBox.selectedProperty().bindBidirectional(showDimensions);
        applyTooltip(dimensionsBox, "Blendet die Längenbeschriftung der gezeichneten Wände ein oder aus.");

        Button resetViewButton = new Button("Ansicht zentrieren");
        resetViewButton.setOnAction(event -> {
            zoom = 1.0;
            offsetX = 240.0;
            offsetY = 160.0;
            render();
        });
        applyTooltip(resetViewButton, "Setzt Zoom und Verschiebung der Zeichenfläche auf die Startansicht zurück.");

        settingsBarStyling(resetViewButton);
        return new ToolBar(
                labelledNode("Rasterweite", gridField),
                gridUnit,
                new Separator(Orientation.VERTICAL),
                labelledNode("Länge", lengthField),
                lengthUnit,
                labelledNode("Winkel", angleField),
                labelledNode("Wandstärke", wallThicknessField),
                wallThicknessUnit,
                new Separator(Orientation.VERTICAL),
                rasterBox,
                snapRasterBox,
                snapPointsBox,
                dimensionsBox,
                compassBox,
                new Separator(Orientation.VERTICAL),
                resetViewButton
        );
    }

    private HBox buildViewBar() {
        ToggleGroup group = new ToggleGroup();
        HBox box = new HBox(8.0);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(new Label("Ansichten:"));
        for (ViewOrientation viewOrientation : ViewOrientation.values()) {
            ToggleButton button = new ToggleButton(viewOrientation.label());
            button.setUserData(viewOrientation);
            button.setToggleGroup(group);
            button.setSelected(viewOrientation == ViewOrientation.TOP);
            button.setOnAction(event -> activeView.set(viewOrientation));
            button.setStyle("-fx-background-radius: 999; -fx-padding: 8 14 8 14;");
            applyTooltip(button, "Schaltet die aktuelle orthogonale Ansicht auf " + viewOrientation.label() + " um.");
            box.getChildren().add(button);
        }
        return box;
    }

    private void settingsBarStyling(Button resetViewButton) {
        resetViewButton.setStyle("-fx-background-color: #4b6a88; -fx-text-fill: white; -fx-background-radius: 999;");
        gridField.setPrefColumnCount(5);
        lengthField.setPrefColumnCount(6);
        angleField.setPrefColumnCount(5);
        wallThicknessField.setPrefColumnCount(5);
    }

    private HBox labelledNode(String label, javafx.scene.Node node) {
        HBox box = new HBox(6.0, new Label(label), node);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void configureCanvas() {
        horizontalRuler.setHeight(RULER_SIZE);
        verticalRuler.setWidth(RULER_SIZE);

        drawingPane.widthProperty().addListener((ignored, oldValue, newValue) -> resizeCanvases());
        drawingPane.heightProperty().addListener((ignored, oldValue, newValue) -> resizeCanvases());

        drawingCanvas.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            lastCursor = screenToWorld(event.getX(), event.getY());
            updateStatus();
            render();
        });
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        drawingCanvas.setOnScroll(event -> {
            double oldScale = scale();
            double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
            zoom = clamp(zoom * zoomFactor, 0.25, 8.0);
            double newScale = scale();
            offsetX = event.getX() - ((event.getX() - offsetX) / oldScale) * newScale;
            offsetY = event.getY() - ((event.getY() - offsetY) / oldScale) * newScale;
            render();
        });
    }

    private void handleMousePressed(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY || event.getButton() == MouseButton.MIDDLE) {
            panning = true;
            panStartX = event.getX();
            panStartY = event.getY();
            panOriginX = offsetX;
            panOriginY = offsetY;
            return;
        }

        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        DraftingConstraints constraints = currentConstraints(!event.isShiftDown());
        draftStart = snapService.snap(screenToWorld(event.getX(), event.getY()), constraints, activeLevel.get().walls());
        previewSegment = new PlanSegment(draftStart, draftStart);
        render();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (panning) {
            offsetX = panOriginX + (event.getX() - panStartX);
            offsetY = panOriginY + (event.getY() - panStartY);
            render();
            return;
        }

        if (draftStart == null) {
            return;
        }

        DraftingConstraints constraints = currentConstraints(!event.isShiftDown());
        PlanPoint snappedPoint = snapService.snap(screenToWorld(event.getX(), event.getY()), constraints, activeLevel.get().walls());
        previewSegment = draftingService.createSegment(draftStart, snappedPoint, constraints);
        lastCursor = previewSegment.end();
        updateStatus();
        render();
    }

    private void handleMouseReleased(MouseEvent event) {
        if (panning) {
            panning = false;
            return;
        }

        if (event.getButton() != MouseButton.PRIMARY || draftStart == null || previewSegment == null) {
            return;
        }

        if (previewSegment.length().toMillimeters() > 1.0) {
            activeLevel.get().addWall(Wall.create(previewSegment, currentWallThickness()));
        }
        draftStart = null;
        previewSegment = null;
        render();
    }

    private void resizeCanvases() {
        double width = Math.max(drawingPane.getWidth(), 200.0);
        double height = Math.max(drawingPane.getHeight(), 200.0);
        drawingCanvas.setWidth(width);
        drawingCanvas.setHeight(height);
        horizontalRuler.setWidth(width);
        verticalRuler.setHeight(height);
        render();
    }

    private void render() {
        GraphicsContext graphics = drawingCanvas.getGraphicsContext2D();
        graphics.setFill(Color.web("#fcfaf5"));
        graphics.fillRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());

        if (showGrid.get()) {
            drawGrid(graphics);
        }
        drawWalls(graphics);
        if (previewSegment != null) {
            drawPreview(graphics);
        }
        drawViewOverlay(graphics);
        if (showCompass.get()) {
            drawCompass(graphics);
        }
        drawRulers();
        updateStatus();
    }

    private void drawGrid(GraphicsContext graphics) {
        double spacingMillimeters = currentGrid().spacing().toMillimeters();
        double spacingPixels = spacingMillimeters * scale();
        if (spacingPixels < 8.0) {
            return;
        }

        graphics.setStroke(Color.web("#d6d0c4"));
        graphics.setLineWidth(1.0);
        double startX = offsetX % spacingPixels;
        double startY = offsetY % spacingPixels;
        for (double x = startX; x <= drawingCanvas.getWidth(); x += spacingPixels) {
            graphics.strokeLine(x, 0, x, drawingCanvas.getHeight());
        }
        for (double y = startY; y <= drawingCanvas.getHeight(); y += spacingPixels) {
            graphics.strokeLine(0, y, drawingCanvas.getWidth(), y);
        }
    }

    private void drawWalls(GraphicsContext graphics) {
        graphics.setStroke(Color.web("#2f2a24"));
        graphics.setFill(Color.web("#2f2a24"));
        graphics.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        for (Wall wall : activeLevel.get().walls()) {
            drawWall(graphics, wall.axis(), wall.thickness(), Color.web("#274c77"));
            if (showDimensions.get()) {
                drawDimensionLabel(graphics, wall.axis(), wall.axis().length().format(LengthUnit.METER, 2));
            }
        }
    }

    private void drawWall(GraphicsContext graphics, PlanSegment segment, Length thickness, Color color) {
        double screenStartX = toScreenX(segment.start().xMillimeters());
        double screenStartY = toScreenY(segment.start().yMillimeters());
        double screenEndX = toScreenX(segment.end().xMillimeters());
        double screenEndY = toScreenY(segment.end().yMillimeters());
        graphics.setStroke(color);
        graphics.setLineWidth(Math.max(thickness.toMillimeters() * scale(), 2.0));
        graphics.strokeLine(screenStartX, screenStartY, screenEndX, screenEndY);
    }

    private void drawPreview(GraphicsContext graphics) {
        drawWall(graphics, previewSegment, currentWallThickness(), Color.web("#c26d32"));
        drawDimensionLabel(
                graphics,
                previewSegment,
                previewSegment.length().format(LengthUnit.METER, 2) + " | " + previewSegment.angle().format()
        );
    }

    private void drawDimensionLabel(GraphicsContext graphics, PlanSegment segment, String text) {
        double midX = (toScreenX(segment.start().xMillimeters()) + toScreenX(segment.end().xMillimeters())) / 2.0;
        double midY = (toScreenY(segment.start().yMillimeters()) + toScreenY(segment.end().yMillimeters())) / 2.0;
        graphics.setFill(Color.web("#1d1b18"));
        graphics.setFont(Font.font("Menlo", 12));
        graphics.fillText(text, midX + 8.0, midY - 8.0);
    }

    private void drawViewOverlay(GraphicsContext graphics) {
        graphics.setFill(Color.color(0.18, 0.16, 0.13, 0.78));
        graphics.fillRoundRect(16, 16, 230, 62, 18, 18);
        graphics.setFill(Color.WHITE);
        graphics.setFont(Font.font("Menlo", 15));
        graphics.fillText("Ansicht: " + activeView.get().label(), 28, 40);
        graphics.setFont(Font.font("Menlo", 11));
        graphics.fillText("Top-Down-Grundriss mit orthogonaler Projektion", 28, 62);
    }

    private void drawCompass(GraphicsContext graphics) {
        double x = drawingCanvas.getWidth() - 78;
        double y = 34;
        graphics.setStroke(Color.web("#4b6a88"));
        graphics.setFill(Color.web("#4b6a88"));
        graphics.setLineWidth(2);
        graphics.strokeOval(x - 18, y - 18, 36, 36);
        graphics.strokeLine(x, y + 14, x, y - 12);
        graphics.strokeLine(x, y - 12, x - 6, y - 2);
        graphics.strokeLine(x, y - 12, x + 6, y - 2);
        graphics.fillText("N", x - 4, y - 22);
    }

    private void drawRulers() {
        drawHorizontalRuler(horizontalRuler.getGraphicsContext2D());
        drawVerticalRuler(verticalRuler.getGraphicsContext2D());
    }

    private void drawHorizontalRuler(GraphicsContext graphics) {
        graphics.setFill(Color.web("#e7decd"));
        graphics.fillRect(0, 0, horizontalRuler.getWidth(), horizontalRuler.getHeight());
        graphics.setStroke(Color.web("#7d7365"));
        graphics.setFill(Color.web("#4a433b"));
        graphics.setFont(Font.font("Menlo", 10));

        double stepMillimeters = chooseRulerStep();
        double stepPixels = stepMillimeters * scale();
        double start = offsetX % stepPixels;
        double worldStart = -offsetX / scale();
        for (double x = start; x <= horizontalRuler.getWidth(); x += stepPixels) {
            graphics.strokeLine(x, horizontalRuler.getHeight(), x, horizontalRuler.getHeight() - 10);
            double worldX = worldStart + (x / scale());
            graphics.fillText(formatRuler(worldX), x + 3, 12);
        }
    }

    private void drawVerticalRuler(GraphicsContext graphics) {
        graphics.setFill(Color.web("#e7decd"));
        graphics.fillRect(0, 0, verticalRuler.getWidth(), verticalRuler.getHeight());
        graphics.setStroke(Color.web("#7d7365"));
        graphics.setFill(Color.web("#4a433b"));
        graphics.setFont(Font.font("Menlo", 10));

        double stepMillimeters = chooseRulerStep();
        double stepPixels = stepMillimeters * scale();
        double start = offsetY % stepPixels;
        double worldStart = -offsetY / scale();
        for (double y = start; y <= verticalRuler.getHeight(); y += stepPixels) {
            graphics.strokeLine(verticalRuler.getWidth(), y, verticalRuler.getWidth() - 10, y);
            double worldY = worldStart + (y / scale());
            graphics.fillText(formatRuler(worldY), 2, y - 3);
        }
    }

    private double chooseRulerStep() {
        double[] candidates = {100, 250, 500, 1000, 2000, 5000};
        for (double candidate : candidates) {
            if (candidate * scale() >= 60.0) {
                return candidate;
            }
        }
        return candidates[candidates.length - 1];
    }

    private String formatRuler(double worldMillimeters) {
        return String.format(Locale.GERMAN, "%.1f m", worldMillimeters / 1000.0);
    }

    private DraftingConstraints currentConstraints(boolean orthogonalMode) {
        return new DraftingConstraints(
                orthogonalMode,
                snapToGrid.get(),
                snapToEndpoints.get(),
                currentGrid(),
                currentWallThickness(),
                SNAP_TOLERANCE,
                parseLength(lengthField, lengthUnit.getValue()),
                parseAngle(angleField)
        );
    }

    private Grid currentGrid() {
        return new Grid(parseLength(gridField, gridUnit.getValue()).orElse(DEFAULT_GRID));
    }

    private Length currentWallThickness() {
        return parseLength(wallThicknessField, wallThicknessUnit.getValue()).orElse(DEFAULT_WALL_THICKNESS);
    }

    private Optional<Length> parseLength(TextField field, LengthUnit unit) {
        String text = field.getText();
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Length.of(Double.parseDouble(text.replace(',', '.')), unit));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Angle> parseAngle(TextField field) {
        String text = field.getText();
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Angle.ofDegrees(Double.parseDouble(text.replace(',', '.'))));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private void updateStatus() {
        viewLabel.setText("Aktive Ansicht: " + activeView.get().label() + " | Etage: " + activeLevel.get().name());
        zoomLabel.setText(String.format(Locale.GERMAN, "Zoom: %.2f x", zoom));
        cursorLabel.setText(String.format(Locale.GERMAN, "Cursor: %.2f m / %.2f m", lastCursor.xMillimeters() / 1000.0, lastCursor.yMillimeters() / 1000.0));
        if (previewSegment == null) {
            draftLabel.setText("Zeichnen: Linke Maustaste für Wände, Shift für freie Winkel, rechte Maustaste zum Verschieben.");
        } else {
            draftLabel.setText("Zeichnen: " + previewSegment.length().format(LengthUnit.METER, 2) + " | " + previewSegment.angle().format());
        }
    }

    private void applyTooltip(javafx.scene.Node node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(320);
        Tooltip.install(node, tooltip);
    }

    private PlanPoint screenToWorld(double screenX, double screenY) {
        return new PlanPoint((screenX - offsetX) / scale(), (screenY - offsetY) / scale());
    }

    private double toScreenX(double worldMillimeters) {
        return offsetX + worldMillimeters * scale();
    }

    private double toScreenY(double worldMillimeters) {
        return offsetY + worldMillimeters * scale();
    }

    private double scale() {
        return BASE_PIXELS_PER_MILLIMETER * zoom;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

