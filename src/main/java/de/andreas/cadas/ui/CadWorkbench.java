package de.andreas.cadas.ui;

import de.andreas.cadas.application.drawing.DraftingConstraints;
import de.andreas.cadas.application.drawing.DraftingService;
import de.andreas.cadas.application.drawing.OpeningPlacementService;
import de.andreas.cadas.application.drawing.SnapService;
import de.andreas.cadas.application.drawing.WallEditingService;
import de.andreas.cadas.application.drawing.WallEndpointSelection;
import de.andreas.cadas.application.exchange.LevelExchangeService;
import de.andreas.cadas.application.parts.DoorPreset;
import de.andreas.cadas.application.parts.PartLibraryImportService;
import de.andreas.cadas.application.parts.StairPreset;
import de.andreas.cadas.application.parts.StandardPartLibrary;
import de.andreas.cadas.application.parts.StandardPartLibraryService;
import de.andreas.cadas.application.parts.WindowPreset;
import de.andreas.cadas.application.view.RenderableKind;
import de.andreas.cadas.application.view.SelectionKey;
import de.andreas.cadas.domain.geometry.Angle;
import de.andreas.cadas.domain.geometry.Grid;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;
import de.andreas.cadas.infrastructure.dxf.DxfLevelExchangeService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
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
import javafx.stage.FileChooser;
import javafx.stage.Window;

public final class CadWorkbench extends BorderPane {

    private static final double BASE_PIXELS_PER_MILLIMETER = 0.10;
    private static final double RULER_SIZE = 32.0;
    private static final Length DEFAULT_GRID = Length.of(25, LengthUnit.CENTIMETER);
    private static final Length DEFAULT_WALL_THICKNESS = Length.of(17.5, LengthUnit.CENTIMETER);
    private static final Length DEFAULT_WALL_HEIGHT = Length.of(2.75, LengthUnit.METER);
    private static final Length DEFAULT_ROOM_HEIGHT = Length.of(2.60, LengthUnit.METER);
    private static final Length DEFAULT_FLOOR_THICKNESS = Length.of(18, LengthUnit.CENTIMETER);
    private static final Length DEFAULT_CEILING_THICKNESS = Length.of(20, LengthUnit.CENTIMETER);
    private static final Length DEFAULT_DOOR_WIDTH = Length.of(1.01, LengthUnit.METER);
    private static final Length DEFAULT_DOOR_HEIGHT = Length.of(2.01, LengthUnit.METER);
    private static final Length DEFAULT_WINDOW_WIDTH = Length.of(1.20, LengthUnit.METER);
    private static final Length DEFAULT_WINDOW_HEIGHT = Length.of(1.20, LengthUnit.METER);
    private static final Length DEFAULT_WINDOW_SILL = Length.of(90, LengthUnit.CENTIMETER);
    private static final Length DEFAULT_STAIR_HEIGHT = Length.of(2.80, LengthUnit.METER);
    private static final Length SNAP_TOLERANCE = Length.of(12, LengthUnit.CENTIMETER);

    private final StandardPartLibrary partLibrary = new StandardPartLibraryService().load();
    private final PartLibraryImportService partLibraryImportService = new PartLibraryImportService();
    private final DraftingService draftingService = new DraftingService();
    private final SnapService snapService = new SnapService();
    private final OpeningPlacementService openingPlacementService = new OpeningPlacementService();
    private final WallEditingService wallEditingService = new WallEditingService();
    private final LevelExchangeService levelExchangeService = new DxfLevelExchangeService();
    private final ProjectModel project = ProjectModel.withDefaultLevel("Neues Projekt", "Erdgeschoss");

    private final ObjectProperty<Level> activeLevel = new SimpleObjectProperty<>(project.primaryLevel());
    private final ObjectProperty<ViewOrientation> activeView = new SimpleObjectProperty<>(ViewOrientation.TOP);
    private final BooleanProperty showGrid = new SimpleBooleanProperty(true);
    private final BooleanProperty snapToGrid = new SimpleBooleanProperty(true);
    private final BooleanProperty snapToEndpoints = new SimpleBooleanProperty(true);
    private final BooleanProperty showCompass = new SimpleBooleanProperty(true);
    private final BooleanProperty showDimensions = new SimpleBooleanProperty(true);
    private final BooleanProperty showAreaVolume = new SimpleBooleanProperty(true);
    private final BooleanProperty showGuides = new SimpleBooleanProperty(true);

    private final Canvas drawingCanvas = new Canvas();
    private final Canvas horizontalRuler = new Canvas();
    private final Canvas verticalRuler = new Canvas();
    private final Pane drawingPane = new Pane(drawingCanvas);
    private final ObservableList<Level> availableLevels = FXCollections.observableArrayList(project.levels());

    private final TextField gridField = new TextField("25");
    private final ComboBox<LengthUnit> gridUnit = new ComboBox<>();
    private final TextField lengthField = new TextField();
    private final ComboBox<LengthUnit> lengthUnit = new ComboBox<>();
    private final TextField angleField = new TextField();
    private final TextField wallThicknessField = new TextField("17,5");
    private final ComboBox<LengthUnit> wallThicknessUnit = new ComboBox<>();
    private final TextField wallHeightField = new TextField("2,75");
    private final ComboBox<LengthUnit> wallHeightUnit = new ComboBox<>();
    private final TextField roomNameField = new TextField("Raum");
    private final TextField roomHeightField = new TextField("2,60");
    private final ComboBox<LengthUnit> roomHeightUnit = new ComboBox<>();
    private final TextField floorThicknessField = new TextField("18");
    private final ComboBox<LengthUnit> floorThicknessUnit = new ComboBox<>();
    private final TextField ceilingThicknessField = new TextField("20");
    private final ComboBox<LengthUnit> ceilingThicknessUnit = new ComboBox<>();
    private final TextField doorWidthField = new TextField("1,01");
    private final ComboBox<LengthUnit> doorWidthUnit = new ComboBox<>();
    private final TextField doorHeightField = new TextField("2,01");
    private final ComboBox<LengthUnit> doorHeightUnit = new ComboBox<>();
    private final TextField thresholdField = new TextField("0");
    private final ComboBox<LengthUnit> thresholdUnit = new ComboBox<>();
    private final TextField windowWidthField = new TextField("1,20");
    private final ComboBox<LengthUnit> windowWidthUnit = new ComboBox<>();
    private final TextField windowHeightField = new TextField("1,20");
    private final ComboBox<LengthUnit> windowHeightUnit = new ComboBox<>();
    private final TextField sillHeightField = new TextField("90");
    private final ComboBox<LengthUnit> sillHeightUnit = new ComboBox<>();
    private final ComboBox<DoorPreset> doorPresetSelector = new ComboBox<>();
    private final ComboBox<WindowPreset> windowPresetSelector = new ComboBox<>();
    private final ComboBox<StairPreset> stairPresetSelector = new ComboBox<>();
    private final TextField stairHeightField = new TextField("2,80");
    private final ComboBox<LengthUnit> stairHeightUnit = new ComboBox<>();
    private final TextField stairStepsField = new TextField("16");
    private final ComboBox<Level> levelSelector = new ComboBox<>();
    private final ComboBox<DrawingTool> toolSelector = new ComboBox<>();
    private final ObservableList<DoorPreset> availableDoorPresets = FXCollections.observableArrayList();
    private final ObservableList<WindowPreset> availableWindowPresets = FXCollections.observableArrayList();
    private final ObservableList<StairPreset> availableStairPresets = FXCollections.observableArrayList();
    private final ThreeDViewport threeDViewport = new ThreeDViewport(this::handleThreeDSelection);

    private final Label zoomLabel = new Label();
    private final Label cursorLabel = new Label();
    private final Label draftLabel = new Label();
    private final Label viewLabel = new Label();

    private final ObservableList<GuideLine> guideLines = FXCollections.observableArrayList();

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
    private WallEndpointSelection selectedEndpointGroup;
    private final ObjectProperty<SelectionKey> selectedSelection = new SimpleObjectProperty<>();
    private GuideOrientation pendingGuideOrientation;
    private double pendingGuideWorldMillimeters;
    private boolean threeDDirty = true;

    public CadWorkbench() {
        setPadding(new Insets(12));
        setStyle("-fx-background-color: linear-gradient(to bottom, #f6f1e8, #ece5d8);");

        configureControls();
        configureLayout();
        configureCanvas();
        threeDViewport.syncLevels(availableLevels, activeLevel.get().name());
        selectedSelection.addListener((ignored, oldValue, newValue) -> {
            threeDViewport.setSelectedSelection(newValue);
            render();
        });
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
        wallHeightUnit.getItems().addAll(LengthUnit.values());
        wallHeightUnit.setValue(LengthUnit.METER);
        roomHeightUnit.getItems().addAll(LengthUnit.values());
        roomHeightUnit.setValue(LengthUnit.METER);
        floorThicknessUnit.getItems().addAll(LengthUnit.values());
        floorThicknessUnit.setValue(LengthUnit.CENTIMETER);
        ceilingThicknessUnit.getItems().addAll(LengthUnit.values());
        ceilingThicknessUnit.setValue(LengthUnit.CENTIMETER);
        doorWidthUnit.getItems().addAll(LengthUnit.values());
        doorWidthUnit.setValue(LengthUnit.METER);
        doorHeightUnit.getItems().addAll(LengthUnit.values());
        doorHeightUnit.setValue(LengthUnit.METER);
        thresholdUnit.getItems().addAll(LengthUnit.values());
        thresholdUnit.setValue(LengthUnit.CENTIMETER);
        windowWidthUnit.getItems().addAll(LengthUnit.values());
        windowWidthUnit.setValue(LengthUnit.METER);
        windowHeightUnit.getItems().addAll(LengthUnit.values());
        windowHeightUnit.setValue(LengthUnit.METER);
        sillHeightUnit.getItems().addAll(LengthUnit.values());
        sillHeightUnit.setValue(LengthUnit.CENTIMETER);
        stairHeightUnit.getItems().addAll(LengthUnit.values());
        stairHeightUnit.setValue(LengthUnit.METER);
        levelSelector.setItems(availableLevels);
        levelSelector.setValue(activeLevel.get());
        toolSelector.getItems().addAll(DrawingTool.values());
        toolSelector.setValue(DrawingTool.WALL);
        availableDoorPresets.setAll(partLibrary.doorPresets());
        availableWindowPresets.setAll(partLibrary.windowPresets());
        availableStairPresets.setAll(partLibrary.stairPresets());
        doorPresetSelector.setItems(availableDoorPresets);
        windowPresetSelector.setItems(availableWindowPresets);
        stairPresetSelector.setItems(availableStairPresets);
        doorPresetSelector.setValue(partLibrary.doorPresets().getFirst());
        windowPresetSelector.setValue(partLibrary.windowPresets().getFirst());
        stairPresetSelector.setValue(partLibrary.stairPresets().getFirst());
        applyDoorPreset(doorPresetSelector.getValue());
        applyWindowPreset(windowPresetSelector.getValue());
        applyStairPreset(stairPresetSelector.getValue());
        doorPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyDoorPreset(newValue));
        windowPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyWindowPreset(newValue));
        stairPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyStairPreset(newValue));
        levelSelector.valueProperty().addListener((ignored, oldValue, newValue) -> {
            if (newValue != null) {
                activeLevel.set(newValue);
                threeDViewport.syncLevels(availableLevels, newValue.name());
                markThreeDDirty();
                render();
            }
        });

        applyTooltip(toolSelector, "Wählt das aktuelle Zeichenwerkzeug aus. Je nach Werkzeug werden Wände, Räume, Türen oder Fenster platziert.");
        applyTooltip(gridField, "Legt die Rasterweite für die Zeichenfläche fest. Werte werden mit der gewählten Einheit interpretiert.");
        applyTooltip(gridUnit, "Bestimmt die Einheit für die Rasterweite, damit Eingaben in Millimeter, Zentimeter oder Meter erfolgen können.");
        applyTooltip(lengthField, "Optionaler Längenwert für die gerade gezeichnete Wand. Wenn ein Wert eingetragen ist, wird die Wand auf diese Länge gesetzt.");
        applyTooltip(lengthUnit, "Bestimmt die Einheit für die manuelle Längeneingabe während des Zeichnens.");
        applyTooltip(angleField, "Optionaler Winkel in Grad für die aktuelle Wand. Ohne Eingabe bleibt der orthogonale 90°-Modus aktiv.");
        applyTooltip(wallThicknessField, "Definiert die Wandstärke für neu gezeichnete Wände.");
        applyTooltip(wallThicknessUnit, "Bestimmt die Einheit für die Wandstärke.");
        applyTooltip(wallHeightField, "Legt die Raum- beziehungsweise Wandhöhe für neu gezeichnete Wände fest.");
        applyTooltip(wallHeightUnit, "Bestimmt die Einheit für die Wandhöhe.");
        applyTooltip(roomNameField, "Legt den Namen für den nächsten anzulegenden Raum fest.");
        applyTooltip(roomHeightField, "Legt die lichte Raumhöhe für den nächsten Raum fest.");
        applyTooltip(roomHeightUnit, "Bestimmt die Einheit für die Raumhöhe.");
        applyTooltip(floorThicknessField, "Legt die Boden- oder Fußbodenstärke des nächsten Raums fest.");
        applyTooltip(floorThicknessUnit, "Bestimmt die Einheit für die Bodenstärke.");
        applyTooltip(ceilingThicknessField, "Legt die Deckenstärke des nächsten Raums fest.");
        applyTooltip(ceilingThicknessUnit, "Bestimmt die Einheit für die Deckenstärke.");
        applyTooltip(doorWidthField, "Legt die Breite der nächsten Tür fest.");
        applyTooltip(doorWidthUnit, "Bestimmt die Einheit für die Türbreite.");
        applyTooltip(doorHeightField, "Legt die Höhe der nächsten Tür fest.");
        applyTooltip(doorHeightUnit, "Bestimmt die Einheit für die Türhöhe.");
        applyTooltip(thresholdField, "Legt den Höhenversatz der Türschwelle für die nächste Tür fest.");
        applyTooltip(thresholdUnit, "Bestimmt die Einheit für die Türschwellenhöhe.");
        applyTooltip(doorPresetSelector, "Wählt eine Standardtür aus der internen Teilebibliothek und übernimmt deren Maße.");
        applyTooltip(windowWidthField, "Legt die Breite des nächsten Fensters fest.");
        applyTooltip(windowWidthUnit, "Bestimmt die Einheit für die Fensterbreite.");
        applyTooltip(windowHeightField, "Legt die Höhe des nächsten Fensters fest.");
        applyTooltip(windowHeightUnit, "Bestimmt die Einheit für die Fensterhöhe.");
        applyTooltip(sillHeightField, "Legt die Brüstungshöhe des nächsten Fensters fest.");
        applyTooltip(sillHeightUnit, "Bestimmt die Einheit für die Brüstungshöhe.");
        applyTooltip(windowPresetSelector, "Wählt ein Standardfenster aus der internen Teilebibliothek und übernimmt dessen Maße.");
        applyTooltip(stairPresetSelector, "Wählt eine Standardtreppe aus der internen Teilebibliothek und übernimmt Typ, Höhe und Stufenanzahl.");
        applyTooltip(stairHeightField, "Legt die Gesamthöhe der nächsten Treppe fest.");
        applyTooltip(stairHeightUnit, "Bestimmt die Einheit für die Treppenhöhe.");
        applyTooltip(stairStepsField, "Legt die Stufenanzahl der nächsten Treppe fest.");
        applyTooltip(levelSelector, "Wechselt zwischen den vorhandenen Etagen des aktuellen Projekts. Jede Etage besitzt ihren eigenen Wandbestand.");

        showGrid.addListener((ignored, oldValue, newValue) -> render());
        snapToGrid.addListener((ignored, oldValue, newValue) -> render());
        snapToEndpoints.addListener((ignored, oldValue, newValue) -> render());
        showCompass.addListener((ignored, oldValue, newValue) -> render());
        showDimensions.addListener((ignored, oldValue, newValue) -> render());
        showAreaVolume.addListener((ignored, oldValue, newValue) -> render());
        showGuides.addListener((ignored, oldValue, newValue) -> render());
        activeView.addListener((ignored, oldValue, newValue) -> {
            markThreeDDirty();
            render();
        });
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
        SplitPane splitPane = new SplitPane(drawingArea, threeDViewport);
        splitPane.setDividerPositions(0.64);
        setCenter(splitPane);

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

        CheckBox areaVolumeBox = new CheckBox("Fläche & Volumen");
        areaVolumeBox.selectedProperty().bindBidirectional(showAreaVolume);
        applyTooltip(areaVolumeBox, "Blendet Flächen- und Volumenwerte der Räume ein oder aus.");

        CheckBox guideBox = new CheckBox("Hilfslinien");
        guideBox.selectedProperty().bindBidirectional(showGuides);
        applyTooltip(guideBox, "Blendet gezogene Hilfslinien aus den Linealen ein oder aus.");

        Button resetViewButton = new Button("Ansicht zentrieren");
        resetViewButton.setOnAction(event -> {
            zoom = 1.0;
            offsetX = 240.0;
            offsetY = 160.0;
            render();
        });
        applyTooltip(resetViewButton, "Setzt Zoom und Verschiebung der Zeichenfläche auf die Startansicht zurück.");

        Button addLevelButton = new Button("Etage hinzufügen");
        addLevelButton.setOnAction(event -> createLevel());
        applyTooltip(addLevelButton, "Legt eine neue Etage für den aktuellen Grundriss an und wechselt direkt in diese Etage.");

        Button exportDxfButton = new Button("DXF exportieren");
        exportDxfButton.setOnAction(event -> exportCurrentLevel());
        applyTooltip(exportDxfButton, "Exportiert die aktuell aktive Etage als DXF-Datei. Räume, Wände, Türen und Fenster werden einschließlich CADas-Metadaten geschrieben.");

        Button importDxfButton = new Button("DXF importieren");
        importDxfButton.setOnAction(event -> importLevel());
        applyTooltip(importDxfButton, "Importiert eine DXF-Datei als neue Etage. CADas-Metadaten werden bevorzugt ausgewertet, einfache Geometrien werden ersatzweise als Grundriss übernommen.");

        Button importLibraryButton = new Button("Teilebibliothek laden");
        importLibraryButton.setOnAction(event -> importPartLibrary());
        applyTooltip(importLibraryButton, "Importiert zusätzliche Tür-, Fenster- und Treppen-Presets aus einer externen `.cadasparts`-Datei und stellt sie direkt in den Auswahllisten bereit.");

        settingsBarStyling(resetViewButton, addLevelButton, exportDxfButton, importDxfButton, importLibraryButton);
        return new ToolBar(
                labelledNode("Werkzeug", toolSelector),
                new Separator(Orientation.VERTICAL),
                labelledNode("Etage", levelSelector),
                addLevelButton,
                exportDxfButton,
                importDxfButton,
                importLibraryButton,
                new Separator(Orientation.VERTICAL),
                labelledNode("Rasterweite", gridField),
                gridUnit,
                new Separator(Orientation.VERTICAL),
                labelledNode("Länge", lengthField),
                lengthUnit,
                labelledNode("Winkel", angleField),
                labelledNode("Wandstärke", wallThicknessField),
                wallThicknessUnit,
                labelledNode("Wandhöhe", wallHeightField),
                wallHeightUnit,
                new Separator(Orientation.VERTICAL),
                labelledNode("Raum", roomNameField),
                labelledNode("Raumhöhe", roomHeightField),
                roomHeightUnit,
                labelledNode("Boden", floorThicknessField),
                floorThicknessUnit,
                labelledNode("Decke", ceilingThicknessField),
                ceilingThicknessUnit,
                new Separator(Orientation.VERTICAL),
                labelledNode("Türbreite", doorWidthField),
                doorWidthUnit,
                labelledNode("Türhöhe", doorHeightField),
                doorHeightUnit,
                labelledNode("Schwelle", thresholdField),
                thresholdUnit,
                labelledNode("Tür-Preset", doorPresetSelector),
                new Separator(Orientation.VERTICAL),
                labelledNode("Fensterbreite", windowWidthField),
                windowWidthUnit,
                labelledNode("Fensterhöhe", windowHeightField),
                windowHeightUnit,
                labelledNode("Brüstung", sillHeightField),
                sillHeightUnit,
                labelledNode("Fenster-Preset", windowPresetSelector),
                new Separator(Orientation.VERTICAL),
                labelledNode("Treppen-Preset", stairPresetSelector),
                labelledNode("Treppenhöhe", stairHeightField),
                stairHeightUnit,
                labelledNode("Stufen", stairStepsField),
                new Separator(Orientation.VERTICAL),
                rasterBox,
                snapRasterBox,
                snapPointsBox,
                dimensionsBox,
                areaVolumeBox,
                guideBox,
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

    private void settingsBarStyling(Button resetViewButton, Button addLevelButton, Button exportDxfButton, Button importDxfButton, Button importLibraryButton) {
        resetViewButton.setStyle("-fx-background-color: #4b6a88; -fx-text-fill: white; -fx-background-radius: 999;");
        addLevelButton.setStyle("-fx-background-color: #7f5539; -fx-text-fill: white; -fx-background-radius: 999;");
        exportDxfButton.setStyle("-fx-background-color: #3b7b58; -fx-text-fill: white; -fx-background-radius: 999;");
        importDxfButton.setStyle("-fx-background-color: #5d648f; -fx-text-fill: white; -fx-background-radius: 999;");
        importLibraryButton.setStyle("-fx-background-color: #8a5f8f; -fx-text-fill: white; -fx-background-radius: 999;");
        gridField.setPrefColumnCount(5);
        lengthField.setPrefColumnCount(6);
        angleField.setPrefColumnCount(5);
        wallThicknessField.setPrefColumnCount(5);
        wallHeightField.setPrefColumnCount(5);
        roomNameField.setPrefColumnCount(8);
        roomHeightField.setPrefColumnCount(5);
        floorThicknessField.setPrefColumnCount(4);
        ceilingThicknessField.setPrefColumnCount(4);
        doorWidthField.setPrefColumnCount(5);
        doorHeightField.setPrefColumnCount(5);
        thresholdField.setPrefColumnCount(4);
        windowWidthField.setPrefColumnCount(5);
        windowHeightField.setPrefColumnCount(5);
        sillHeightField.setPrefColumnCount(4);
        stairHeightField.setPrefColumnCount(5);
        stairStepsField.setPrefColumnCount(3);
        levelSelector.setPrefWidth(180);
        toolSelector.setPrefWidth(140);
        doorPresetSelector.setPrefWidth(190);
        windowPresetSelector.setPrefWidth(210);
        stairPresetSelector.setPrefWidth(190);
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
        horizontalRuler.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> startGuideDrag(GuideOrientation.VERTICAL, screenToWorld(event.getX(), 0).xMillimeters()));
        horizontalRuler.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> updateGuideDrag(GuideOrientation.VERTICAL, screenToWorld(event.getX(), 0).xMillimeters()));
        horizontalRuler.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> finishGuideDrag(GuideOrientation.VERTICAL, screenToWorld(event.getX(), 0).xMillimeters()));
        verticalRuler.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> startGuideDrag(GuideOrientation.HORIZONTAL, screenToWorld(0, event.getY()).yMillimeters()));
        verticalRuler.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> updateGuideDrag(GuideOrientation.HORIZONTAL, screenToWorld(0, event.getY()).yMillimeters()));
        verticalRuler.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> finishGuideDrag(GuideOrientation.HORIZONTAL, screenToWorld(0, event.getY()).yMillimeters()));

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
        if (event.getButton() == MouseButton.SECONDARY && event.isAltDown()) {
            removeNearestGuide(screenToWorld(event.getX(), event.getY()));
            return;
        }

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

        if (currentTool() == DrawingTool.EDIT) {
            DraftingConstraints constraints = currentConstraints(false);
            PlanPoint editPoint = snapService.snap(screenToWorld(event.getX(), event.getY()), constraints, activeLevel.get().walls());
            selectedEndpointGroup = wallEditingService.findConnectedEndpoint(activeLevel.get().walls(), editPoint, SNAP_TOLERANCE).orElse(null);
            if (selectedEndpointGroup != null) {
                activeLevel.get().walls().stream()
                        .filter(wall -> selectedEndpointGroup.startWallIds().contains(wall.id()) || selectedEndpointGroup.endWallIds().contains(wall.id()))
                        .findFirst()
                        .ifPresent(wall -> selectedSelection.set(new SelectionKey(RenderableKind.WALL, activeLevel.get().name(), wall.id().toString())));
            } else {
                selectedSelection.set(findSelectionAt(editPoint));
            }
            render();
            return;
        }

        DraftingConstraints constraints = currentConstraints(!event.isShiftDown());
        draftStart = snapService.snap(screenToWorld(event.getX(), event.getY()), constraints, activeLevel.get().walls());
        previewSegment = new PlanSegment(draftStart, draftStart);
        if (currentTool() == DrawingTool.DOOR) {
            placeDoor(draftStart);
            draftStart = null;
            previewSegment = null;
        } else if (currentTool() == DrawingTool.WINDOW) {
            placeWindow(draftStart);
            draftStart = null;
            previewSegment = null;
        }
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
            if (selectedEndpointGroup != null) {
                DraftingConstraints constraints = currentConstraints(false);
                PlanPoint snappedPoint = snapService.snap(screenToWorld(event.getX(), event.getY()), constraints, activeLevel.get().walls());
                activeLevel.get().replaceWalls(wallEditingService.moveEndpointGroup(activeLevel.get().walls(), selectedEndpointGroup, snappedPoint));
                markThreeDDirty();
                render();
            }
            return;
        }

        if (currentTool().isPointTool()) {
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

        if (selectedEndpointGroup != null) {
            selectedEndpointGroup = null;
            render();
            return;
        }

        if (event.getButton() != MouseButton.PRIMARY || draftStart == null || previewSegment == null) {
            return;
        }

        if (previewSegment.length().toMillimeters() > 1.0) {
            if (currentTool() == DrawingTool.WALL) {
                Wall wall = Wall.create(previewSegment, currentWallThickness(), currentWallHeight());
                activeLevel.get().addWall(wall);
                selectedSelection.set(new SelectionKey(RenderableKind.WALL, activeLevel.get().name(), wall.id().toString()));
            } else if (currentTool() == DrawingTool.ROOM) {
                Room room = Room.rectangular(
                        currentRoomName(),
                        previewSegment.start(),
                        previewSegment.end(),
                        currentRoomHeight(),
                        currentFloorThickness(),
                        currentCeilingThickness()
                );
                activeLevel.get().addRoom(room);
                selectedSelection.set(new SelectionKey(RenderableKind.ROOM_VOLUME, activeLevel.get().name(), room.id().toString()));
            } else if (currentTool() == DrawingTool.STAIR) {
                Staircase staircase = Staircase.create(
                        currentStairType(),
                        previewSegment.start(),
                        previewSegment.end(),
                        currentStairHeight(),
                        currentStairSteps()
                );
                activeLevel.get().addStaircase(staircase);
                selectedSelection.set(new SelectionKey(RenderableKind.STAIR, activeLevel.get().name(), staircase.id().toString()));
            }
            markThreeDDirty();
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
        if (showGuides.get()) {
            drawGuides(graphics);
        }
        drawRooms(graphics);
        drawWalls(graphics);
        drawStaircases(graphics);
        drawDoors(graphics);
        drawWindows(graphics);
        if (previewSegment != null) {
            drawPreview(graphics);
        }
        drawViewOverlay(graphics);
        if (showCompass.get()) {
            drawCompass(graphics);
        }
        drawRulers();
        refreshThreeDIfNeeded();
        updateStatus();
    }

    private void drawGuides(GraphicsContext graphics) {
        graphics.setStroke(Color.color(0.73, 0.2, 0.2, 0.75));
        graphics.setLineWidth(1.2);
        for (GuideLine guideLine : guideLines) {
            if (guideLine.orientation() == GuideOrientation.VERTICAL) {
                double x = toScreenX(guideLine.worldMillimeters());
                graphics.strokeLine(x, 0, x, drawingCanvas.getHeight());
            } else {
                double y = toScreenY(guideLine.worldMillimeters());
                graphics.strokeLine(0, y, drawingCanvas.getWidth(), y);
            }
        }
        if (pendingGuideOrientation != null) {
            if (pendingGuideOrientation == GuideOrientation.VERTICAL) {
                double x = toScreenX(pendingGuideWorldMillimeters);
                graphics.strokeLine(x, 0, x, drawingCanvas.getHeight());
            } else {
                double y = toScreenY(pendingGuideWorldMillimeters);
                graphics.strokeLine(0, y, drawingCanvas.getWidth(), y);
            }
        }
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
        graphics.setFill(Color.web("#2f2a24"));
        graphics.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        for (Wall wall : activeLevel.get().walls()) {
            boolean selected = isSelected(RenderableKind.WALL, wall.id().toString());
            drawWall(graphics, wall.axis(), wall.thickness(), selected ? Color.web("#d97f2f") : Color.web("#274c77"), selected ? 1.18 : 1.0);
            if (showDimensions.get()) {
                drawDimensionLabel(graphics, wall.axis(), wall.axis().length().format(LengthUnit.METER, 2));
            }
        }
    }

    private void drawRooms(GraphicsContext graphics) {
        for (Room room : activeLevel.get().rooms()) {
            double[] xPoints = room.outline().stream().mapToDouble(point -> toScreenX(point.xMillimeters())).toArray();
            double[] yPoints = room.outline().stream().mapToDouble(point -> toScreenY(point.yMillimeters())).toArray();
            boolean selected = isSelected(RenderableKind.ROOM_VOLUME, room.id().toString())
                    || isSelected(RenderableKind.ROOM_FLOOR, room.id().toString())
                    || isSelected(RenderableKind.ROOM_CEILING, room.id().toString());
            graphics.setFill(selected ? Color.color(0.87, 0.58, 0.24, 0.30) : Color.color(0.77, 0.64, 0.45, 0.22));
            graphics.fillPolygon(xPoints, yPoints, xPoints.length);
            graphics.setStroke(selected ? Color.color(0.78, 0.42, 0.14, 0.96) : Color.color(0.55, 0.43, 0.25, 0.8));
            graphics.setLineWidth(selected ? 2.8 : 2.0);
            graphics.strokePolygon(xPoints, yPoints, xPoints.length);
            if (showAreaVolume.get()) {
                PlanPoint center = room.centerPoint();
                drawRoomLabel(graphics, room, center);
            }
        }
    }

    private void drawRoomLabel(GraphicsContext graphics, Room room, PlanPoint center) {
        graphics.setFill(Color.web("#5d4527"));
        graphics.setFont(Font.font("Menlo", 12));
        graphics.fillText(room.name(), toScreenX(center.xMillimeters()) - 26, toScreenY(center.yMillimeters()) - 6);
        graphics.setFont(Font.font("Menlo", 11));
        graphics.fillText(
                String.format(Locale.GERMAN, "%.2f m² | %.2f m³", room.areaSquareMeters(), room.volumeCubicMeters()),
                toScreenX(center.xMillimeters()) - 42,
                toScreenY(center.yMillimeters()) + 12
        );
    }

    private void drawDoors(GraphicsContext graphics) {
        for (Door door : activeLevel.get().doors()) {
            Wall hostWall = activeLevel.get().findWall(door.wallId());
            PlanPoint openingStart = hostWall.axis().pointAt(door.offsetFromStart());
            PlanPoint openingEnd = hostWall.axis().pointAt(door.offsetFromStart().add(door.width()));
            boolean selected = isSelected(RenderableKind.DOOR, door.id().toString());
            graphics.setStroke(selected ? Color.web("#f08f3c") : Color.web("#d66b2d"));
            graphics.setLineWidth(Math.max(hostWall.thickness().toMillimeters() * scale() * (selected ? 0.72 : 0.55), 3.0));
            graphics.strokeLine(
                    toScreenX(openingStart.xMillimeters()),
                    toScreenY(openingStart.yMillimeters()),
                    toScreenX(openingEnd.xMillimeters()),
                    toScreenY(openingEnd.yMillimeters())
            );
        }
    }

    private void drawWindows(GraphicsContext graphics) {
        for (WindowElement window : activeLevel.get().windows()) {
            Wall hostWall = activeLevel.get().findWall(window.wallId());
            PlanPoint openingStart = hostWall.axis().pointAt(window.offsetFromStart());
            PlanPoint openingEnd = hostWall.axis().pointAt(window.offsetFromStart().add(window.width()));
            boolean selected = isSelected(RenderableKind.WINDOW, window.id().toString());
            graphics.setStroke(selected ? Color.web("#7bc8eb") : Color.web("#4da8da"));
            graphics.setLineWidth(Math.max(hostWall.thickness().toMillimeters() * scale() * (selected ? 0.48 : 0.35), 3.0));
            graphics.strokeLine(
                    toScreenX(openingStart.xMillimeters()),
                    toScreenY(openingStart.yMillimeters()),
                    toScreenX(openingEnd.xMillimeters()),
                    toScreenY(openingEnd.yMillimeters())
            );
        }
    }

    private void drawStaircases(GraphicsContext graphics) {
        for (Staircase staircase : activeLevel.get().staircases()) {
            double x = toScreenX(staircase.minX());
            double y = toScreenY(staircase.minY());
            double width = staircase.widthMillimeters() * scale();
            double height = staircase.heightMillimeters() * scale();
            boolean selected = isSelected(RenderableKind.STAIR, staircase.id().toString());
            graphics.setStroke(selected ? Color.web("#8a6848") : Color.web("#5e503f"));
            graphics.setFill(selected ? Color.color(0.63, 0.47, 0.27, 0.24) : Color.color(0.52, 0.46, 0.37, 0.16));
            graphics.setLineWidth(selected ? 2.8 : 2.0);
            graphics.fillRect(x, y, width, height);
            graphics.strokeRect(x, y, width, height);
            switch (staircase.stairType()) {
                case STRAIGHT -> drawStraightStairTreads(graphics, staircase, x, y, width, height);
                case HALF_TURN -> drawHalfTurnStair(graphics, staircase, x, y, width, height);
                case SPIRAL -> drawSpiralStair(graphics, x, y, width, height);
            }
        }
    }

    private void drawStraightStairTreads(GraphicsContext graphics, Staircase staircase, double x, double y, double width, double height) {
        double stepHeight = height / staircase.stepCount();
        for (int step = 1; step < staircase.stepCount(); step++) {
            double yStep = y + stepHeight * step;
            graphics.strokeLine(x, yStep, x + width, yStep);
        }
    }

    private void drawHalfTurnStair(GraphicsContext graphics, Staircase staircase, double x, double y, double width, double height) {
        double halfHeight = height / 2.0;
        double flightWidth = width / 2.0;
        graphics.strokeLine(x + flightWidth, y, x + flightWidth, y + halfHeight);
        for (int step = 1; step < staircase.stepCount() / 2; step++) {
            double localY = y + (halfHeight / (staircase.stepCount() / 2.0)) * step;
            graphics.strokeLine(x, localY, x + flightWidth, localY);
            graphics.strokeLine(x + flightWidth, y + height - (localY - y), x + width, y + height - (localY - y));
        }
    }

    private void drawSpiralStair(GraphicsContext graphics, double x, double y, double width, double height) {
        graphics.strokeOval(x, y, width, height);
        graphics.strokeOval(x + width * 0.25, y + height * 0.25, width * 0.5, height * 0.5);
    }

    private void drawWall(GraphicsContext graphics, PlanSegment segment, Length thickness, Color color, double widthFactor) {
        double screenStartX = toScreenX(segment.start().xMillimeters());
        double screenStartY = toScreenY(segment.start().yMillimeters());
        double screenEndX = toScreenX(segment.end().xMillimeters());
        double screenEndY = toScreenY(segment.end().yMillimeters());
        graphics.setStroke(color);
        graphics.setLineWidth(Math.max(thickness.toMillimeters() * scale() * widthFactor, 2.0));
        graphics.strokeLine(screenStartX, screenStartY, screenEndX, screenEndY);
    }

    private void drawPreview(GraphicsContext graphics) {
        double startX = Math.min(previewSegment.start().xMillimeters(), previewSegment.end().xMillimeters());
        double startY = Math.min(previewSegment.start().yMillimeters(), previewSegment.end().yMillimeters());
        double endX = Math.max(previewSegment.start().xMillimeters(), previewSegment.end().xMillimeters());
        double endY = Math.max(previewSegment.start().yMillimeters(), previewSegment.end().yMillimeters());
        if (currentTool() == DrawingTool.ROOM) {
            graphics.setFill(Color.color(0.76, 0.49, 0.27, 0.18));
            graphics.fillRect(
                    toScreenX(startX),
                    toScreenY(startY),
                    (endX - startX) * scale(),
                    (endY - startY) * scale()
            );
            graphics.setStroke(Color.web("#c26d32"));
            graphics.setLineWidth(2.0);
            graphics.strokeRect(
                    toScreenX(startX),
                    toScreenY(startY),
                    (endX - startX) * scale(),
                    (endY - startY) * scale()
            );
        } else if (currentTool() == DrawingTool.STAIR) {
            graphics.setFill(Color.color(0.45, 0.37, 0.29, 0.18));
            graphics.setStroke(Color.web("#7f6a55"));
            graphics.setLineWidth(2.0);
            graphics.fillRect(
                    toScreenX(startX),
                    toScreenY(startY),
                    (endX - startX) * scale(),
                    (endY - startY) * scale()
            );
            graphics.strokeRect(
                    toScreenX(startX),
                    toScreenY(startY),
                    (endX - startX) * scale(),
                    (endY - startY) * scale()
            );
        } else {
            drawWall(graphics, previewSegment, currentWallThickness(), Color.web("#c26d32"), 1.0);
        }
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

    private Length currentWallHeight() {
        return parseLength(wallHeightField, wallHeightUnit.getValue()).orElse(DEFAULT_WALL_HEIGHT);
    }

    private String currentRoomName() {
        String roomName = roomNameField.getText();
        if (roomName == null || roomName.isBlank()) {
            return "Raum";
        }
        return roomName.trim();
    }

    private Length currentRoomHeight() {
        return parseLength(roomHeightField, roomHeightUnit.getValue()).orElse(DEFAULT_ROOM_HEIGHT);
    }

    private Length currentFloorThickness() {
        return parseLength(floorThicknessField, floorThicknessUnit.getValue()).orElse(DEFAULT_FLOOR_THICKNESS);
    }

    private Length currentCeilingThickness() {
        return parseLength(ceilingThicknessField, ceilingThicknessUnit.getValue()).orElse(DEFAULT_CEILING_THICKNESS);
    }

    private Length currentDoorWidth() {
        return parseLength(doorWidthField, doorWidthUnit.getValue()).orElse(DEFAULT_DOOR_WIDTH);
    }

    private Length currentDoorHeight() {
        return parseLength(doorHeightField, doorHeightUnit.getValue()).orElse(DEFAULT_DOOR_HEIGHT);
    }

    private Length currentThresholdHeight() {
        return parseLength(thresholdField, thresholdUnit.getValue()).orElse(Length.zero());
    }

    private Length currentWindowWidth() {
        return parseLength(windowWidthField, windowWidthUnit.getValue()).orElse(DEFAULT_WINDOW_WIDTH);
    }

    private Length currentWindowHeight() {
        return parseLength(windowHeightField, windowHeightUnit.getValue()).orElse(DEFAULT_WINDOW_HEIGHT);
    }

    private Length currentSillHeight() {
        return parseLength(sillHeightField, sillHeightUnit.getValue()).orElse(DEFAULT_WINDOW_SILL);
    }

    private StairType currentStairType() {
        return Optional.ofNullable(stairPresetSelector.getValue())
                .map(StairPreset::stairType)
                .orElse(StairType.STRAIGHT);
    }

    private Length currentStairHeight() {
        return parseLength(stairHeightField, stairHeightUnit.getValue()).orElse(DEFAULT_STAIR_HEIGHT);
    }

    private int currentStairSteps() {
        try {
            return Math.max(1, Integer.parseInt(stairStepsField.getText()));
        } catch (NumberFormatException ignored) {
            return 16;
        }
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
            draftLabel.setText("Werkzeug: " + currentTool().label() + " | Linke Maustaste platziert, rechte Maustaste verschiebt, Alt+Rechtsklick entfernt Hilfslinien.");
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

    private void createLevel() {
        TextInputDialog dialog = new TextInputDialog("Etage " + (availableLevels.size() + 1));
        dialog.setTitle("Neue Etage");
        dialog.setHeaderText("Neue Etage anlegen");
        dialog.setContentText("Name der Etage:");
        dialog.getDialogPane().setPrefWidth(420);
        dialog.showAndWait()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .ifPresent(levelName -> {
                    Level level = project.createLevel(levelName);
                    availableLevels.add(level);
                    levelSelector.setValue(level);
                    threeDViewport.syncLevels(availableLevels, level.name());
                    markThreeDDirty();
                });
    }

    private DrawingTool currentTool() {
        return Optional.ofNullable(toolSelector.getValue()).orElse(DrawingTool.WALL);
    }

    private void placeDoor(PlanPoint clickPoint) {
        openingPlacementService.placeDoor(
                        clickPoint,
                        activeLevel.get().walls(),
                        currentDoorWidth(),
                currentDoorHeight(),
                currentThresholdHeight(),
                SNAP_TOLERANCE)
                .ifPresent(door -> {
                    activeLevel.get().addDoor(door);
                    selectedSelection.set(new SelectionKey(RenderableKind.DOOR, activeLevel.get().name(), door.id().toString()));
                    markThreeDDirty();
                });
    }

    private void placeWindow(PlanPoint clickPoint) {
        openingPlacementService.placeWindow(
                        clickPoint,
                        activeLevel.get().walls(),
                currentWindowWidth(),
                currentSillHeight(),
                currentWindowHeight(),
                SNAP_TOLERANCE)
                .ifPresent(window -> {
                    activeLevel.get().addWindow(window);
                    selectedSelection.set(new SelectionKey(RenderableKind.WINDOW, activeLevel.get().name(), window.id().toString()));
                    markThreeDDirty();
                });
    }

    private void startGuideDrag(GuideOrientation orientation, double worldMillimeters) {
        pendingGuideOrientation = orientation;
        pendingGuideWorldMillimeters = worldMillimeters;
        render();
    }

    private void updateGuideDrag(GuideOrientation orientation, double worldMillimeters) {
        if (pendingGuideOrientation == orientation) {
            pendingGuideWorldMillimeters = worldMillimeters;
            render();
        }
    }

    private void finishGuideDrag(GuideOrientation orientation, double worldMillimeters) {
        if (pendingGuideOrientation != orientation) {
            return;
        }
        guideLines.add(new GuideLine(orientation, worldMillimeters));
        pendingGuideOrientation = null;
        render();
    }

    private void removeNearestGuide(PlanPoint clickPoint) {
        guideLines.stream()
                .filter(guideLine -> guideDistance(guideLine, clickPoint) <= SNAP_TOLERANCE.toMillimeters())
                .findFirst()
                .ifPresent(guideLines::remove);
        render();
    }

    private double guideDistance(GuideLine guideLine, PlanPoint clickPoint) {
        if (guideLine.orientation() == GuideOrientation.VERTICAL) {
            return Math.abs(guideLine.worldMillimeters() - clickPoint.xMillimeters());
        }
        return Math.abs(guideLine.worldMillimeters() - clickPoint.yMillimeters());
    }

    private void exportCurrentLevel() {
        FileChooser fileChooser = createDxfFileChooser();
        fileChooser.setInitialFileName(activeLevel.get().name().replace(' ', '_') + ".dxf");
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            return;
        }
        try {
            levelExchangeService.exportLevel(activeLevel.get(), file.toPath());
            draftLabel.setText("DXF exportiert: " + file.getName());
        } catch (IOException exception) {
            draftLabel.setText("DXF-Export fehlgeschlagen: " + exception.getMessage());
        }
    }

    private void importLevel() {
        FileChooser fileChooser = createDxfFileChooser();
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        try {
            String levelName = uniqueLevelName(stripExtension(file.toPath()));
            Level importedLevel = levelExchangeService.importLevel(file.toPath(), levelName);
            project.addLevel(importedLevel);
            availableLevels.add(importedLevel);
            levelSelector.setValue(importedLevel);
            threeDViewport.syncLevels(availableLevels, importedLevel.name());
            markThreeDDirty();
            draftLabel.setText("DXF importiert: " + file.getName());
        } catch (IOException exception) {
            draftLabel.setText("DXF-Import fehlgeschlagen: " + exception.getMessage());
        }
    }

    private FileChooser createDxfFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("DXF-Datei auswählen");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DXF-Dateien", "*.dxf"));
        return fileChooser;
    }

    private String stripExtension(Path path) {
        String filename = path.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0) {
            return filename;
        }
        return filename.substring(0, dotIndex);
    }

    private String uniqueLevelName(String baseName) {
        String candidate = baseName;
        int suffix = 2;
        while (containsLevelName(candidate)) {
            candidate = baseName + " " + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean containsLevelName(String candidate) {
        return availableLevels.stream().anyMatch(level -> level.name().equalsIgnoreCase(candidate));
    }

    private void importPartLibrary() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Teilebibliothek auswählen");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CADas Teilebibliothek", "*.cadasparts"));
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        try {
            StandardPartLibrary importedLibrary = partLibraryImportService.importLibrary(file.toPath());
            availableDoorPresets.addAll(importedLibrary.doorPresets());
            availableWindowPresets.addAll(importedLibrary.windowPresets());
            availableStairPresets.addAll(importedLibrary.stairPresets());
            if (!importedLibrary.doorPresets().isEmpty()) {
                doorPresetSelector.setValue(importedLibrary.doorPresets().getFirst());
            }
            if (!importedLibrary.windowPresets().isEmpty()) {
                windowPresetSelector.setValue(importedLibrary.windowPresets().getFirst());
            }
            if (!importedLibrary.stairPresets().isEmpty()) {
                stairPresetSelector.setValue(importedLibrary.stairPresets().getFirst());
            }
            draftLabel.setText("Teilebibliothek geladen: " + file.getName());
        } catch (IOException exception) {
            draftLabel.setText("Teilebibliothek fehlgeschlagen: " + exception.getMessage());
        }
    }

    private void applyDoorPreset(DoorPreset preset) {
        if (preset == null) {
            return;
        }
        doorWidthField.setText(preset.width().format(LengthUnit.METER, 2).replace(" m", "").replace('.', ','));
        doorHeightField.setText(preset.height().format(LengthUnit.METER, 2).replace(" m", "").replace('.', ','));
        thresholdField.setText(preset.thresholdHeight().format(LengthUnit.CENTIMETER, 0).replace(" cm", "").replace('.', ','));
    }

    private void applyWindowPreset(WindowPreset preset) {
        if (preset == null) {
            return;
        }
        windowWidthField.setText(preset.width().format(LengthUnit.METER, 2).replace(" m", "").replace('.', ','));
        windowHeightField.setText(preset.height().format(LengthUnit.METER, 2).replace(" m", "").replace('.', ','));
        sillHeightField.setText(preset.sillHeight().format(LengthUnit.CENTIMETER, 0).replace(" cm", "").replace('.', ','));
    }

    private void applyStairPreset(StairPreset preset) {
        if (preset == null) {
            return;
        }
        stairHeightField.setText(preset.totalHeight().format(LengthUnit.METER, 2).replace(" m", "").replace('.', ','));
        stairStepsField.setText(Integer.toString(preset.stepCount()));
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

    private void handleThreeDSelection(SelectionKey selectionKey) {
        if (selectionKey == null) {
            return;
        }
        availableLevels.stream()
                .filter(level -> level.name().equals(selectionKey.levelName()))
                .findFirst()
                .ifPresent(levelSelector::setValue);
        selectedSelection.set(selectionKey);
    }

    private void markThreeDDirty() {
        threeDDirty = true;
    }

    private void refreshThreeDIfNeeded() {
        if (!threeDDirty) {
            return;
        }
        threeDViewport.syncLevels(availableLevels, activeLevel.get().name());
        threeDViewport.refresh(project);
        threeDDirty = false;
    }

    private boolean isSelected(RenderableKind kind, String elementId) {
        return selectedSelection.get() != null
                && selectedSelection.get().kind() == kind
                && selectedSelection.get().levelName().equals(activeLevel.get().name())
                && selectedSelection.get().elementId().equals(elementId);
    }

    private SelectionKey findSelectionAt(PlanPoint point) {
        for (Door door : activeLevel.get().doors()) {
            Wall wall = activeLevel.get().findWall(door.wallId());
            PlanSegment segment = new PlanSegment(
                    wall.axis().pointAt(door.offsetFromStart()),
                    wall.axis().pointAt(door.offsetFromStart().add(door.width()))
            );
            if (segment.distanceTo(point).compareTo(SNAP_TOLERANCE) <= 0) {
                return new SelectionKey(RenderableKind.DOOR, activeLevel.get().name(), door.id().toString());
            }
        }
        for (WindowElement window : activeLevel.get().windows()) {
            Wall wall = activeLevel.get().findWall(window.wallId());
            PlanSegment segment = new PlanSegment(
                    wall.axis().pointAt(window.offsetFromStart()),
                    wall.axis().pointAt(window.offsetFromStart().add(window.width()))
            );
            if (segment.distanceTo(point).compareTo(SNAP_TOLERANCE) <= 0) {
                return new SelectionKey(RenderableKind.WINDOW, activeLevel.get().name(), window.id().toString());
            }
        }
        for (Room room : activeLevel.get().rooms()) {
            if (containsPoint(room, point)) {
                return new SelectionKey(RenderableKind.ROOM_VOLUME, activeLevel.get().name(), room.id().toString());
            }
        }
        for (Staircase staircase : activeLevel.get().staircases()) {
            if (point.xMillimeters() >= staircase.minX()
                    && point.xMillimeters() <= staircase.maxX()
                    && point.yMillimeters() >= staircase.minY()
                    && point.yMillimeters() <= staircase.maxY()) {
                return new SelectionKey(RenderableKind.STAIR, activeLevel.get().name(), staircase.id().toString());
            }
        }
        return activeLevel.get().walls().stream()
                .filter(wall -> wall.axis().distanceTo(point).compareTo(SNAP_TOLERANCE) <= 0)
                .min((left, right) -> Double.compare(
                        left.axis().distanceTo(point).toMillimeters(),
                        right.axis().distanceTo(point).toMillimeters()))
                .map(wall -> new SelectionKey(RenderableKind.WALL, activeLevel.get().name(), wall.id().toString()))
                .orElse(null);
    }

    private boolean containsPoint(Room room, PlanPoint point) {
        boolean inside = false;
        int lastIndex = room.outline().size() - 1;
        for (int currentIndex = 0; currentIndex < room.outline().size(); currentIndex++) {
            PlanPoint current = room.outline().get(currentIndex);
            PlanPoint previous = room.outline().get(lastIndex);
            boolean intersects = (current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters())
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / (previous.yMillimeters() - current.yMillimeters())
                    + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            lastIndex = currentIndex;
        }
        return inside;
    }
}
