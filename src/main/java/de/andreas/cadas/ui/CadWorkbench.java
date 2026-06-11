package de.andreas.cadas.ui;

import de.andreas.cadas.application.drawing.DraftingConstraints;
import de.andreas.cadas.application.drawing.DraftingService;
import de.andreas.cadas.application.exchange.ExchangeFileNameService;
import de.andreas.cadas.application.history.UndoRedoStack;
import de.andreas.cadas.application.drawing.OpeningPlacementService;
import de.andreas.cadas.application.drawing.QuarterTurnRotationService;
import de.andreas.cadas.application.drawing.SelectionQueryService;
import de.andreas.cadas.application.drawing.SnapService;
import de.andreas.cadas.application.drawing.WallEditingService;
import de.andreas.cadas.application.drawing.WallEndpointSelection;
import de.andreas.cadas.application.exchange.LevelExchangeService;
import de.andreas.cadas.application.exchange.ProjectExchangeService;
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
import de.andreas.cadas.infrastructure.dxf.DxfProjectExchangeService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
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
    private final SelectionQueryService selectionQueryService = new SelectionQueryService();
    private final ExchangeFileNameService exchangeFileNameService = new ExchangeFileNameService();
    private final OpeningPlacementService openingPlacementService = new OpeningPlacementService();
    private final WallEditingService wallEditingService = new WallEditingService();
    private final QuarterTurnRotationService quarterTurnRotationService = new QuarterTurnRotationService();
    private final LevelExchangeService levelExchangeService = new DxfLevelExchangeService();
    private final ProjectExchangeService projectExchangeService = new DxfProjectExchangeService();
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
    private final TextField northAngleField = new TextField("0");
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
    private final ViewProjectionService projectionService = new ViewProjectionService();
    private final UndoRedoStack<WorkbenchSnapshot> history = new UndoRedoStack<>();
    private final VBox propertySections = new VBox(12.0);
    private final Label selectionSummaryLabel = new Label("Keine Auswahl");
    private final Button undoButton = new Button("Rückgängig");
    private final Button redoButton = new Button("Wiederherstellen");
    private final Button deleteSelectionButton = new Button("Auswahl löschen");
    private final Button clearSelectionButton = new Button("Auswahl aufheben");
    private final Button applySelectionPropertiesButton = new Button("Werte auf Auswahl anwenden");
    private final ContextMenu selectionContextMenu = new ContextMenu();
    private final Label cadLibrarySummaryLabel = new Label("Keine externen CAD-Bibliotheken registriert.");

    private final Label zoomLabel = new Label();
    private final Label cursorLabel = new Label();
    private final Label draftLabel = new Label();
    private final Label viewLabel = new Label();

    private final ObservableList<GuideLine> guideLines = FXCollections.observableArrayList();
    private final ObservableList<Path> cadLibraryReferences = FXCollections.observableArrayList();
    private final LinkedHashSet<SelectionKey> selectedSelections = new LinkedHashSet<>();

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
    private boolean historyCapturedForDrag;

    public CadWorkbench() {
        setPadding(new Insets(12));
        setStyle("-fx-background-color: linear-gradient(to bottom, #f6f1e8, #ece5d8);");

        configureControls();
        configureLayout();
        configureCanvas();
        threeDViewport.syncLevels(availableLevels, activeLevel.get().name());
        threeDViewport.applyViewOrientation(activeView.get());
        selectedSelection.addListener((ignored, oldValue, newValue) -> {
            threeDViewport.setSelectedSelection(newValue);
            threeDViewport.setSelectedSelections(Set.copyOf(selectedSelections));
            updatePropertySectionVisibility();
            updateActionButtons();
            syncInputsFromPrimarySelection();
            render();
        });
        sceneProperty().addListener((ignored, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.ESCAPE),
                        this::clearSelection
                );
            }
        });
        updatePropertySectionVisibility();
        updateActionButtons();
        updateStatus();
        render();
    }

    private void configureControls() {
        initializeUnitSelectors();
        levelSelector.setItems(availableLevels);
        levelSelector.setValue(activeLevel.get());
        toolSelector.getItems().addAll(DrawingTool.values());
        toolSelector.setValue(DrawingTool.WALL);
        initializePresetSelectors();
        levelSelector.valueProperty().addListener((ignored, oldValue, newValue) -> {
            if (newValue != null) {
                activateLevel(newValue);
            }
        });

        applyFormTooltips();

        registerRenderListener(showGrid);
        registerRenderListener(snapToGrid);
        registerRenderListener(snapToEndpoints);
        registerRenderListener(showCompass);
        registerRenderListener(showDimensions);
        registerRenderListener(showAreaVolume);
        registerRenderListener(showGuides);
        activeView.addListener((ignored, oldValue, newValue) -> {
            threeDViewport.applyViewOrientation(newValue);
            render();
        });
        toolSelector.valueProperty().addListener((ignored, oldValue, newValue) -> {
            updatePropertySectionVisibility();
            updateActionButtons();
        });
        configureActionButtons();
    }

    private void configureLayout() {
        MenuBar menuBar = buildMenuBar();
        ToolBar settingsBar = buildSettingsBar();
        HBox viewBar = buildViewBar();
        VBox topArea = new VBox(8.0, menuBar, settingsBar, viewBar);
        topArea.setPadding(new Insets(0, 0, 12, 0));
        setTop(topArea);

        BorderPane drawingArea = new BorderPane();
        Region rulerCorner = new Region();
        rulerCorner.setPrefSize(RULER_SIZE, RULER_SIZE);
        rulerCorner.setStyle("-fx-background-color: #e7decd;");
        BorderPane rulerHeader = new BorderPane();
        rulerHeader.setLeft(rulerCorner);
        rulerHeader.setCenter(horizontalRuler);
        drawingArea.setTop(rulerHeader);
        drawingArea.setLeft(verticalRuler);
        drawingArea.setCenter(new StackPane(drawingPane));
        drawingArea.setStyle("-fx-background-color: rgba(255,255,255,0.55); -fx-background-radius: 16;");
        SplitPane splitPane = new SplitPane(buildPropertyPane(), drawingArea, threeDViewport);
        splitPane.setDividerPositions(0.19, 0.69);
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

        Button resetViewButton = createActionButton(
                "2D zentrieren",
                null,
                this::resetTwoDView,
                "Setzt Zoom und Verschiebung der Zeichenfläche auf die Startansicht zurück."
        );

        Button addLevelButton = createActionButton(
                "Etage hinzufügen",
                null,
                this::createLevel,
                "Legt eine neue Etage für den aktuellen Grundriss an und wechselt direkt in diese Etage."
        );

        settingsBarStyling();
        return new ToolBar(
                labelledNode("Werkzeug", toolSelector),
                new Separator(Orientation.VERTICAL),
                labelledNode("Etage", levelSelector),
                addLevelButton,
                undoButton,
                redoButton,
                deleteSelectionButton,
                clearSelectionButton,
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
        HBox box = new HBox(8.0);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(new Label("Ansichten:"));
        for (ViewOrientation viewOrientation : ViewOrientation.values()) {
            Button button = new Button(viewOrientation.buttonLabel());
            button.setOnAction(event -> activeView.set(viewOrientation));
            button.setStyle("-fx-background-radius: 999; -fx-padding: 8 14 8 14;");
            applyTooltip(button, "Schaltet die aktuelle orthogonale Ansicht auf " + viewOrientation.label() + " um.");
            box.getChildren().add(button);
        }
        return box;
    }

    private void settingsBarStyling() {
        gridField.setPrefColumnCount(5);
        lengthField.setPrefColumnCount(6);
        angleField.setPrefColumnCount(5);
        northAngleField.setPrefColumnCount(5);
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

    private MenuBar buildMenuBar() {
        Menu dateiMenu = new Menu("Datei");
        dateiMenu.getItems().addAll(
                menuItem("Etage hinzufügen", this::createLevel, shortcutKey(KeyCode.N)),
                menuItem("Projekt leeren", this::clearProject, shortcutKey(KeyCode.L)),
                menuItem("Gebäude als DXF exportieren", this::exportProjectAsDxf, shortcutShiftKey(KeyCode.E)),
                menuItem("Gebäude aus DXF importieren", this::importProjectFromDxf, shortcutShiftKey(KeyCode.I)),
                menuItem("Aktive Etage als DXF exportieren", this::exportCurrentLevel, null),
                menuItem("DXF als neue Etage importieren", this::importLevel, null),
                menuItem("Teilebibliothek laden", this::importPartLibrary, shortcutShiftKey(KeyCode.B)),
                menuItem("Beenden", Platform::exit, shortcutKey(KeyCode.Q))
        );

        Menu bearbeitenMenu = new Menu("Bearbeiten");
        bearbeitenMenu.getItems().addAll(
                menuItem("Rückgängig", this::undo, shortcutKey(KeyCode.Z)),
                menuItem("Wiederherstellen", this::redo, shortcutShiftKey(KeyCode.Z)),
                menuItem("Eigenschaften auf Auswahl anwenden", this::applyCurrentInputsToSelection, shortcutShiftKey(KeyCode.P)),
                menuItem("Auswahl löschen", this::deleteSelection, new KeyCodeCombination(KeyCode.DELETE)),
                menuItem("Auswahl aufheben", this::clearSelection, new KeyCodeCombination(KeyCode.ESCAPE))
        );

        Menu ansichtMenu = new Menu("Ansicht");
        for (ViewOrientation viewOrientation : ViewOrientation.values()) {
            ansichtMenu.getItems().add(menuItem(
                    "Zu " + viewOrientation.label(),
                    () -> activeView.set(viewOrientation),
                    null
            ));
        }
        ansichtMenu.getItems().addAll(
                menuItem("2D-Ansicht zentrieren", this::resetTwoDView, shortcutKey(KeyCode.DIGIT0)),
                menuItem("3D-Ansicht zentrieren", threeDViewport::resetToCurrentOrientation, shortcutShiftKey(KeyCode.DIGIT0))
        );

        Menu werkzeugMenu = new Menu("Werkzeuge");
        werkzeugMenu.getItems().addAll(
                toolMenuItem(DrawingTool.EDIT, KeyCode.E),
                toolMenuItem(DrawingTool.WALL, KeyCode.W),
                toolMenuItem(DrawingTool.ROOM, KeyCode.R),
                toolMenuItem(DrawingTool.STAIR, KeyCode.T),
                toolMenuItem(DrawingTool.DOOR, KeyCode.D),
                toolMenuItem(DrawingTool.WINDOW, KeyCode.F),
                menuItem("Ausgewählte Bauteile 90° rechts drehen", this::rotateSelectedComponentsClockwise, shortcutShiftKey(KeyCode.RIGHT)),
                menuItem("Ausgewählte Bauteile 90° links drehen", this::rotateSelectedComponentsCounterClockwise, shortcutShiftKey(KeyCode.LEFT))
        );

        Menu optionenMenu = new Menu("Optionen");
        optionenMenu.getItems().addAll(
                checkMenuItem("Raster anzeigen", showGrid),
                checkMenuItem("Auf Raster einrasten", snapToGrid),
                checkMenuItem("Auf Punkte einrasten", snapToEndpoints),
                checkMenuItem("Hilfslinien anzeigen", showGuides),
                checkMenuItem("Bemaßung anzeigen", showDimensions),
                checkMenuItem("Fläche und Volumen anzeigen", showAreaVolume),
                checkMenuItem("Nordpfeil anzeigen", showCompass)
        );

        MenuBar menuBar = new MenuBar(dateiMenu, bearbeitenMenu, ansichtMenu, werkzeugMenu, optionenMenu);
        applyTooltip(menuBar, "Bietet Datei-, Bearbeitungs-, Ansichts- und Werkzeugfunktionen mit passenden Tastaturkürzeln an.");
        return menuBar;
    }

    private ScrollPane buildPropertyPane() {
        selectionSummaryLabel.setWrapText(true);
        selectionSummaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        cadLibrarySummaryLabel.setWrapText(true);
        cadLibrarySummaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        propertySections.getChildren().setAll(
                createPropertySection("Auswahl", selectionSummaryLabel, applySelectionPropertiesButton),
                createPropertySection(
                        "Zeichnen",
                        propertyRow("Rasterweite", gridField, gridUnit),
                        propertyRow("Länge", lengthField, lengthUnit),
                        propertyRow("Winkel", angleField),
                        propertyRow("Nordwinkel", northAngleField)
                ),
                createPropertySection(
                        "Wand",
                        propertyRow("Wandstärke", wallThicknessField, wallThicknessUnit),
                        propertyRow("Wandhöhe", wallHeightField, wallHeightUnit)
                ),
                createPropertySection(
                        "Raum",
                        propertyRow("Name", roomNameField),
                        propertyRow("Raumhöhe", roomHeightField, roomHeightUnit),
                        propertyRow("Boden", floorThicknessField, floorThicknessUnit),
                        propertyRow("Decke", ceilingThicknessField, ceilingThicknessUnit)
                ),
                createPropertySection(
                        "Tür",
                        propertyRow("Preset", doorPresetSelector),
                        propertyRow("Breite", doorWidthField, doorWidthUnit),
                        propertyRow("Höhe", doorHeightField, doorHeightUnit),
                        propertyRow("Schwelle", thresholdField, thresholdUnit)
                ),
                createPropertySection(
                        "Fenster",
                        propertyRow("Preset", windowPresetSelector),
                        propertyRow("Breite", windowWidthField, windowWidthUnit),
                        propertyRow("Höhe", windowHeightField, windowHeightUnit),
                        propertyRow("Brüstung", sillHeightField, sillHeightUnit)
                ),
                createPropertySection(
                        "Treppe",
                        propertyRow("Preset", stairPresetSelector),
                        propertyRow("Höhe", stairHeightField, stairHeightUnit),
                        propertyRow("Stufen", stairStepsField)
                ),
                createPropertySection(
                        "CAD-Bibliotheken",
                        cadLibrarySummaryLabel
                )
        );
        propertySections.setPadding(new Insets(4, 0, 4, 0));

        VBox container = new VBox(10.0, new Label("Properties"), propertySections);
        container.setPadding(new Insets(12));
        container.setStyle("-fx-background-color: rgba(255,255,255,0.62); -fx-background-radius: 16;");

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefWidth(290);
        scrollPane.setStyle("-fx-background-color: transparent;");
        applyTooltip(scrollPane, "Zeigt alle für Werkzeug oder Auswahl passenden Eigenschaften in einer permanent sichtbaren, vertikalen Liste an.");
        return scrollPane;
    }

    private VBox createPropertySection(String title, Node... nodes) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        VBox section = new VBox(8.0, titleLabel);
        section.getChildren().addAll(nodes);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: rgba(242,236,226,0.92); -fx-background-radius: 12;");
        return section;
    }

    private VBox propertyRow(String label, Node... controls) {
        Label fieldLabel = new Label(label);
        fieldLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4d4135;");
        HBox controlRow = new HBox(6.0, controls);
        controlRow.setAlignment(Pos.CENTER_LEFT);
        return new VBox(4.0, fieldLabel, controlRow);
    }

    private void configureActionButtons() {
        undoButton.setOnAction(event -> undo());
        redoButton.setOnAction(event -> redo());
        deleteSelectionButton.setOnAction(event -> deleteSelection());
        clearSelectionButton.setOnAction(event -> clearSelection());
        applySelectionPropertiesButton.setOnAction(event -> applyCurrentInputsToSelection());
        rebuildSelectionContextMenu();
        applyTooltip(undoButton, "Stellt den letzten fachlichen Bearbeitungsschritt des Projekts wieder her.");
        applyTooltip(redoButton, "Stellt einen zuvor rückgängig gemachten Bearbeitungsschritt erneut her.");
        applyTooltip(deleteSelectionButton, "Löscht das aktuell ausgewählte Bauteil aus der aktiven Etage.");
        applyTooltip(clearSelectionButton, "Hebt die aktuelle Auswahl auf und entfernt die Hervorhebung in 2D und 3D.");
        applyTooltip(applySelectionPropertiesButton, "Übernimmt die aktuell sichtbaren Eingabewerte auf alle passenden, ausgewählten Bauteile.");
        applyTooltip(cadLibrarySummaryLabel, "Listet registrierte externe CAD-Bibliotheken wie `.dwg` oder `.cadasparts` auf, die für spätere Teileverwendung vorgemerkt sind.");
    }

    private void updatePropertySectionVisibility() {
        for (int index = 0; index < propertySections.getChildren().size(); index++) {
            Node node = propertySections.getChildren().get(index);
            boolean visible = switch (index) {
                case 0, 1 -> true;
                case 2 -> shouldShowSection(DrawingTool.WALL, RenderableKind.WALL);
                case 3 -> shouldShowSection(DrawingTool.ROOM, RenderableKind.ROOM_VOLUME, RenderableKind.ROOM_FLOOR, RenderableKind.ROOM_CEILING);
                case 4 -> shouldShowSection(DrawingTool.DOOR, RenderableKind.DOOR);
                case 5 -> shouldShowSection(DrawingTool.WINDOW, RenderableKind.WINDOW);
                case 6 -> shouldShowSection(DrawingTool.STAIR, RenderableKind.STAIR);
                default -> true;
            };
            node.setVisible(visible);
            node.setManaged(visible);
        }
        selectionSummaryLabel.setText(selectionSummary());
    }

    private boolean shouldShowSection(DrawingTool tool, RenderableKind... kinds) {
        if (currentTool() == tool) {
            return true;
        }
        if (selectedSelection.get() == null) {
            return false;
        }
        for (RenderableKind kind : kinds) {
            if (selectedSelection.get().kind() == kind) {
                return true;
            }
        }
        return false;
    }

    private String selectionSummary() {
        if (selectedSelections.isEmpty()) {
            return "Keine Auswahl. Wähle ein Bauteil im Werkzeug `Bearbeiten` aus oder nutze direkt die Werkzeuge in der Zeichenfläche.";
        }
        if (selectedSelections.size() > 1) {
            return selectedSelections.size() + " Bauteile ausgewählt. Änderungen über die sichtbaren Eigenschaften werden auf passende Auswahlen gemeinsam angewendet.";
        }
        return "Ausgewählt: " + switch (selectedSelection.get().kind()) {
            case WALL -> "Wand";
            case ROOM_VOLUME, ROOM_FLOOR, ROOM_CEILING -> "Raum";
            case DOOR -> "Tür";
            case WINDOW -> "Fenster";
            case STAIR -> "Treppe";
            default -> selectedSelection.get().kind().name();
        } + " auf Etage `" + selectedSelection.get().levelName() + "`.";
    }

    private void updateActionButtons() {
        undoButton.setDisable(!history.canUndo());
        redoButton.setDisable(!history.canRedo());
        boolean hasSelection = !selectedSelections.isEmpty();
        deleteSelectionButton.setDisable(!hasSelection);
        clearSelectionButton.setDisable(!hasSelection && selectedEndpointGroup == null);
        applySelectionPropertiesButton.setDisable(!hasSelection);
    }

    private MenuItem menuItem(String label, Runnable action, KeyCombination accelerator) {
        MenuItem menuItem = new MenuItem(label);
        menuItem.setOnAction(event -> action.run());
        if (accelerator != null) {
            menuItem.setAccelerator(accelerator);
        }
        return menuItem;
    }

    private MenuItem toolMenuItem(DrawingTool tool, KeyCode keyCode) {
        return menuItem(tool.label(), () -> toolSelector.setValue(tool), shortcutKey(keyCode));
    }

    private CheckMenuItem checkMenuItem(String label, BooleanProperty property) {
        CheckMenuItem menuItem = new CheckMenuItem(label);
        menuItem.selectedProperty().bindBidirectional(property);
        return menuItem;
    }

    private KeyCombination shortcutKey(KeyCode keyCode) {
        return new KeyCodeCombination(keyCode, KeyCombination.SHORTCUT_DOWN);
    }

    private KeyCombination shortcutShiftKey(KeyCode keyCode) {
        return new KeyCodeCombination(keyCode, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    }

    private HBox labelledNode(String label, Node node) {
        HBox box = new HBox(6.0, new Label(label), node);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void initializeUnitSelectors() {
        initializeUnitSelector(gridUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(lengthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(wallThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(wallHeightUnit, LengthUnit.METER);
        initializeUnitSelector(roomHeightUnit, LengthUnit.METER);
        initializeUnitSelector(floorThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(ceilingThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(doorWidthUnit, LengthUnit.METER);
        initializeUnitSelector(doorHeightUnit, LengthUnit.METER);
        initializeUnitSelector(thresholdUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(windowWidthUnit, LengthUnit.METER);
        initializeUnitSelector(windowHeightUnit, LengthUnit.METER);
        initializeUnitSelector(sillHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairHeightUnit, LengthUnit.METER);
    }

    private void initializeUnitSelector(ComboBox<LengthUnit> selector, LengthUnit defaultUnit) {
        selector.getItems().addAll(LengthUnit.values());
        selector.setValue(defaultUnit);
    }

    private void initializePresetSelectors() {
        availableDoorPresets.setAll(partLibrary.doorPresets());
        availableWindowPresets.setAll(partLibrary.windowPresets());
        availableStairPresets.setAll(partLibrary.stairPresets());
        doorPresetSelector.setItems(availableDoorPresets);
        windowPresetSelector.setItems(availableWindowPresets);
        stairPresetSelector.setItems(availableStairPresets);
        selectFirstIfAvailable(doorPresetSelector, availableDoorPresets);
        selectFirstIfAvailable(windowPresetSelector, availableWindowPresets);
        selectFirstIfAvailable(stairPresetSelector, availableStairPresets);
        applyDoorPreset(doorPresetSelector.getValue());
        applyWindowPreset(windowPresetSelector.getValue());
        applyStairPreset(stairPresetSelector.getValue());
        doorPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyDoorPreset(newValue));
        windowPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyWindowPreset(newValue));
        stairPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyStairPreset(newValue));
    }

    private <T> void selectFirstIfAvailable(ComboBox<T> selector, ObservableList<T> values) {
        if (!values.isEmpty()) {
            selector.setValue(values.getFirst());
        }
    }

    private void applyFormTooltips() {
        applyTooltip(toolSelector, "Wählt das aktuelle Zeichenwerkzeug aus. Je nach Werkzeug werden Wände, Räume, Türen oder Fenster platziert.");
        applyTooltip(gridField, "Legt die Rasterweite für die Zeichenfläche fest. Werte werden mit der gewählten Einheit interpretiert.");
        applyTooltip(gridUnit, "Bestimmt die Einheit für die Rasterweite, damit Eingaben in Millimeter, Zentimeter oder Meter erfolgen können.");
        applyTooltip(lengthField, "Optionaler Längenwert für die gerade gezeichnete Wand. Wenn ein Wert eingetragen ist, wird die Wand auf diese Länge gesetzt.");
        applyTooltip(lengthUnit, "Bestimmt die Einheit für die manuelle Längeneingabe während des Zeichnens.");
        applyTooltip(angleField, "Optionaler Winkel in Grad für die aktuelle Wand. Ohne Eingabe bleibt der orthogonale 90°-Modus aktiv.");
        applyTooltip(northAngleField, "Definiert die Nordausrichtung des Gebäudes in Grad. Die Kompassanzeige richtet sich danach aus.");
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
    }

    private void registerRenderListener(BooleanProperty property) {
        property.addListener((ignored, oldValue, newValue) -> render());
    }

    private Button createActionButton(String label, String style, Runnable action, String tooltipText) {
        Button button = new Button(label);
        button.setOnAction(event -> action.run());
        if (style != null) {
            button.setStyle(style);
        }
        applyTooltip(button, tooltipText);
        return button;
    }

    private void configureCanvas() {
        horizontalRuler.setHeight(RULER_SIZE);
        verticalRuler.setWidth(RULER_SIZE);

        drawingPane.widthProperty().addListener((ignored, oldValue, newValue) -> resizeCanvases());
        drawingPane.heightProperty().addListener((ignored, oldValue, newValue) -> resizeCanvases());
        horizontalRuler.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> startGuideDrag(GuideOrientation.HORIZONTAL, guideWorldPositionFromHorizontalRuler(event)));
        horizontalRuler.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> updateGuideDrag(GuideOrientation.HORIZONTAL, guideWorldPositionFromHorizontalRuler(event)));
        horizontalRuler.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> finishGuideDrag(GuideOrientation.HORIZONTAL, guideWorldPositionFromHorizontalRuler(event)));
        verticalRuler.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> startGuideDrag(GuideOrientation.VERTICAL, guideWorldPositionFromVerticalRuler(event)));
        verticalRuler.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> updateGuideDrag(GuideOrientation.VERTICAL, guideWorldPositionFromVerticalRuler(event)));
        verticalRuler.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> finishGuideDrag(GuideOrientation.VERTICAL, guideWorldPositionFromVerticalRuler(event)));

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

        if (event.getButton() == MouseButton.SECONDARY && currentTool() == DrawingTool.EDIT) {
            DraftingConstraints constraints = currentConstraints(false);
            PlanPoint editPoint = snapService.snap(screenToWorld(event.getX(), event.getY()), constraints, activeLevel.get().walls());
            SelectionKey contextSelection = selectionQueryService.findSelection(activeLevel.get(), editPoint, SNAP_TOLERANCE).orElse(null);
            if (contextSelection != null) {
                if (!selectedSelections.contains(contextSelection)) {
                    selectSingle(contextSelection);
                }
                selectionContextMenu.show(drawingCanvas, event.getScreenX(), event.getScreenY());
                return;
            }
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

        if (!isDirectEditingView()) {
            render();
            draftLabel.setText("Direktes Zeichnen und Bearbeiten ist aktuell nur in der Draufsicht verfügbar.");
            return;
        }

        if (currentTool() == DrawingTool.EDIT) {
            DraftingConstraints constraints = currentConstraints(false);
            PlanPoint editPoint = snapService.snap(screenToWorld(event.getX(), event.getY()), constraints, activeLevel.get().walls());
            selectedEndpointGroup = wallEditingService.findConnectedEndpoint(activeLevel.get().walls(), editPoint, SNAP_TOLERANCE).orElse(null);
            historyCapturedForDrag = false;
            if (selectedEndpointGroup != null) {
                activeLevel.get().walls().stream()
                        .filter(wall -> selectedEndpointGroup.startWallIds().contains(wall.id()) || selectedEndpointGroup.endWallIds().contains(wall.id()))
                        .findFirst()
                        .ifPresent(wall -> updateSelection(
                                new SelectionKey(RenderableKind.WALL, activeLevel.get().name(), wall.id().toString()),
                                event.isShortcutDown() || event.isShiftDown()
                        ));
            } else {
                updateSelection(
                        selectionQueryService.findSelection(activeLevel.get(), editPoint, SNAP_TOLERANCE).orElse(null),
                        event.isShortcutDown() || event.isShiftDown()
                );
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
                if (!historyCapturedForDrag) {
                    rememberStateForUndo();
                    historyCapturedForDrag = true;
                }
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
            historyCapturedForDrag = false;
            updateActionButtons();
            render();
            return;
        }

        if (event.getButton() != MouseButton.PRIMARY || draftStart == null || previewSegment == null) {
            return;
        }

        if (previewSegment.length().toMillimeters() > 1.0) {
            if (currentTool() == DrawingTool.WALL) {
                rememberStateForUndo();
                Wall wall = Wall.create(previewSegment, currentWallThickness(), currentWallHeight());
                activeLevel.get().addWall(wall);
                selectSingle(new SelectionKey(RenderableKind.WALL, activeLevel.get().name(), wall.id().toString()));
            } else if (currentTool() == DrawingTool.ROOM) {
                rememberStateForUndo();
                Room room = Room.rectangular(
                        currentRoomName(),
                        previewSegment.start(),
                        previewSegment.end(),
                        currentRoomHeight(),
                        currentFloorThickness(),
                        currentCeilingThickness()
                );
                activeLevel.get().addRoom(room);
                selectSingle(new SelectionKey(RenderableKind.ROOM_VOLUME, activeLevel.get().name(), room.id().toString()));
            } else if (currentTool() == DrawingTool.STAIR) {
                rememberStateForUndo();
                Staircase staircase = Staircase.create(
                        currentStairType(),
                        previewSegment.start(),
                        previewSegment.end(),
                        currentStairHeight(),
                        currentStairSteps()
                );
                activeLevel.get().addStaircase(staircase);
                selectSingle(new SelectionKey(RenderableKind.STAIR, activeLevel.get().name(), staircase.id().toString()));
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
        if (!projectionService.isPlanView(activeView.get())) {
            return;
        }
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
        if (!projectionService.isPlanView(activeView.get())) {
            return;
        }
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
            if (projectionService.isPlanView(activeView.get())) {
                drawWall(graphics, wall.axis(), wall.thickness(), selected ? Color.web("#d97f2f") : Color.web("#274c77"), selected ? 1.18 : 1.0);
            } else {
                drawWallElevation(graphics, wall, selected);
            }
            if (showDimensions.get() && projectionService.isPlanView(activeView.get())) {
                drawDimensionLabel(graphics, wall.axis(), wall.axis().length().format(LengthUnit.METER, 2));
            }
        }
    }

    private void drawRooms(GraphicsContext graphics) {
        for (Room room : activeLevel.get().rooms()) {
            if (!projectionService.isPlanView(activeView.get())) {
                drawRoomElevation(graphics, room);
                continue;
            }
            double[] xPoints = room.outline().stream().mapToDouble(point -> toScreenProjectedX(point, 0.0)).toArray();
            double[] yPoints = room.outline().stream().mapToDouble(point -> toScreenProjectedY(point, 0.0)).toArray();
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
        graphics.fillText(room.name(), toScreenProjectedX(center, 0.0) - 26, toScreenProjectedY(center, 0.0) - 6);
        graphics.setFont(Font.font("Menlo", 11));
        graphics.fillText(
                String.format(Locale.GERMAN, "%.2f m² | %.2f m³", room.areaSquareMeters(), room.volumeCubicMeters()),
                toScreenProjectedX(center, 0.0) - 42,
                toScreenProjectedY(center, 0.0) + 12
        );
    }

    private void drawDoors(GraphicsContext graphics) {
        for (Door door : activeLevel.get().doors()) {
            Wall hostWall = activeLevel.get().findWall(door.wallId());
            PlanPoint openingStart = hostWall.axis().pointAt(door.offsetFromStart());
            PlanPoint openingEnd = hostWall.axis().pointAt(door.offsetFromStart().add(door.width()));
            boolean selected = isSelected(RenderableKind.DOOR, door.id().toString());
            if (!projectionService.isPlanView(activeView.get())) {
                drawOpeningElevation(graphics, openingStart, openingEnd, door.thresholdHeight().toMillimeters(), door.height().toMillimeters(), selected ? Color.web("#f08f3c") : Color.web("#d66b2d"));
                continue;
            }
            graphics.setStroke(selected ? Color.web("#f08f3c") : Color.web("#d66b2d"));
            graphics.setLineWidth(Math.max(hostWall.thickness().toMillimeters() * scale() * (selected ? 0.72 : 0.55), 3.0));
            graphics.strokeLine(
                    toScreenProjectedX(openingStart, 0.0),
                    toScreenProjectedY(openingStart, 0.0),
                    toScreenProjectedX(openingEnd, 0.0),
                    toScreenProjectedY(openingEnd, 0.0)
            );
        }
    }

    private void drawWindows(GraphicsContext graphics) {
        for (WindowElement window : activeLevel.get().windows()) {
            Wall hostWall = activeLevel.get().findWall(window.wallId());
            PlanPoint openingStart = hostWall.axis().pointAt(window.offsetFromStart());
            PlanPoint openingEnd = hostWall.axis().pointAt(window.offsetFromStart().add(window.width()));
            boolean selected = isSelected(RenderableKind.WINDOW, window.id().toString());
            if (!projectionService.isPlanView(activeView.get())) {
                drawOpeningElevation(graphics, openingStart, openingEnd, window.sillHeight().toMillimeters(), window.windowHeight().toMillimeters(), selected ? Color.web("#7bc8eb") : Color.web("#4da8da"));
                continue;
            }
            graphics.setStroke(selected ? Color.web("#7bc8eb") : Color.web("#4da8da"));
            graphics.setLineWidth(Math.max(hostWall.thickness().toMillimeters() * scale() * (selected ? 0.48 : 0.35), 3.0));
            graphics.strokeLine(
                    toScreenProjectedX(openingStart, 0.0),
                    toScreenProjectedY(openingStart, 0.0),
                    toScreenProjectedX(openingEnd, 0.0),
                    toScreenProjectedY(openingEnd, 0.0)
            );
        }
    }

    private void drawStaircases(GraphicsContext graphics) {
        for (Staircase staircase : activeLevel.get().staircases()) {
            boolean selected = isSelected(RenderableKind.STAIR, staircase.id().toString());
            graphics.setStroke(selected ? Color.web("#8a6848") : Color.web("#5e503f"));
            graphics.setFill(selected ? Color.color(0.63, 0.47, 0.27, 0.24) : Color.color(0.52, 0.46, 0.37, 0.16));
            graphics.setLineWidth(selected ? 2.8 : 2.0);
            if (!projectionService.isPlanView(activeView.get())) {
                drawStairElevation(graphics, staircase);
                continue;
            }
            drawStairOutline(graphics, staircase);
            switch (staircase.stairType()) {
                case STRAIGHT -> drawStraightStairTreads(graphics, staircase);
                case HALF_TURN -> drawHalfTurnStair(graphics, staircase);
                case SWITCHBACK -> drawSwitchbackStair(graphics, staircase);
                case SPIRAL -> drawSpiralStair(graphics, staircase);
            }
        }
    }

    private void drawStraightStairTreads(GraphicsContext graphics, Staircase staircase) {
        double stepHeight = staircase.heightMillimeters() / staircase.stepCount();
        for (int step = 1; step < staircase.stepCount(); step++) {
            strokeLocalLine(graphics, staircase, 0, stepHeight * step, staircase.widthMillimeters(), stepHeight * step);
        }
    }

    private void drawHalfTurnStair(GraphicsContext graphics, Staircase staircase) {
        double totalWidth = staircase.widthMillimeters();
        double totalHeight = staircase.heightMillimeters();
        double landingDepth = totalHeight * 0.22;
        double flightDepth = (totalHeight - landingDepth) / 2.0;
        double firstFlightWidth = totalWidth * 0.48;
        int firstFlightSteps = staircase.stepCount() / 2;
        int secondFlightSteps = staircase.stepCount() - firstFlightSteps;

        strokeLocalRect(graphics, staircase, 0, flightDepth, totalWidth, landingDepth);
        strokeLocalLine(graphics, staircase, firstFlightWidth, 0, firstFlightWidth, flightDepth);
        for (int step = 1; step < firstFlightSteps; step++) {
            double localY = (flightDepth / firstFlightSteps) * step;
            strokeLocalLine(graphics, staircase, 0, localY, firstFlightWidth, localY);
        }
        for (int step = 1; step < secondFlightSteps; step++) {
            double localY = flightDepth + landingDepth + (flightDepth / secondFlightSteps) * step;
            strokeLocalLine(graphics, staircase, firstFlightWidth, localY, totalWidth, localY);
        }
    }

    private void drawSwitchbackStair(GraphicsContext graphics, Staircase staircase) {
        double totalWidth = staircase.widthMillimeters();
        double totalHeight = staircase.heightMillimeters();
        double turnZoneDepth = totalHeight * 0.18;
        double flightDepth = totalHeight - turnZoneDepth;
        double flightWidth = totalWidth / 2.0;
        int firstFlightSteps = staircase.stepCount() / 2;
        int secondFlightSteps = staircase.stepCount() - firstFlightSteps;

        strokeLocalRect(graphics, staircase, 0, flightDepth, totalWidth, turnZoneDepth);
        strokeLocalLine(graphics, staircase, flightWidth, 0, flightWidth, flightDepth);
        for (int step = 1; step < firstFlightSteps; step++) {
            double localY = (flightDepth / firstFlightSteps) * step;
            strokeLocalLine(graphics, staircase, 0, localY, flightWidth, localY);
        }
        for (int step = 1; step < secondFlightSteps; step++) {
            double localY = flightDepth - (flightDepth / secondFlightSteps) * step;
            strokeLocalLine(graphics, staircase, flightWidth, localY, totalWidth, localY);
        }
    }

    private void drawSpiralStair(GraphicsContext graphics, Staircase staircase) {
        graphics.strokeOval(
                toScreenProjectedX(new PlanPoint(staircase.minX(), staircase.minY()), 0.0),
                toScreenProjectedY(new PlanPoint(staircase.minX(), staircase.minY()), 0.0),
                staircase.widthMillimeters() * scale(),
                staircase.heightMillimeters() * scale()
        );
        graphics.strokeOval(
                toScreenProjectedX(new PlanPoint(staircase.minX() + staircase.widthMillimeters() * 0.25, staircase.minY() + staircase.heightMillimeters() * 0.25), 0.0),
                toScreenProjectedY(new PlanPoint(staircase.minX() + staircase.widthMillimeters() * 0.25, staircase.minY() + staircase.heightMillimeters() * 0.25), 0.0),
                staircase.widthMillimeters() * scale() * 0.5,
                staircase.heightMillimeters() * scale() * 0.5
        );
        for (int step = 0; step < staircase.stepCount(); step++) {
            double angle = (360.0 / staircase.stepCount()) * step;
            double radius = Math.min(staircase.widthMillimeters(), staircase.heightMillimeters()) * 0.45;
            PlanPoint center = staircase.pointAtLocalPosition(staircase.widthMillimeters() / 2.0, staircase.heightMillimeters() / 2.0);
            PlanPoint outer = new PlanPoint(
                    center.xMillimeters() + Math.cos(Math.toRadians(angle)) * radius,
                    center.yMillimeters() + Math.sin(Math.toRadians(angle)) * radius
            );
            graphics.strokeLine(
                    toScreenProjectedX(center, 0.0),
                    toScreenProjectedY(center, 0.0),
                    toScreenProjectedX(outer, 0.0),
                    toScreenProjectedY(outer, 0.0)
            );
        }
    }

    private void drawStairOutline(GraphicsContext graphics, Staircase staircase) {
        double[] xPoints = {
                toScreenProjectedX(staircase.pointAtLocalPosition(0, 0), 0.0),
                toScreenProjectedX(staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0), 0.0),
                toScreenProjectedX(staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()), 0.0),
                toScreenProjectedX(staircase.pointAtLocalPosition(0, staircase.heightMillimeters()), 0.0)
        };
        double[] yPoints = {
                toScreenProjectedY(staircase.pointAtLocalPosition(0, 0), 0.0),
                toScreenProjectedY(staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0), 0.0),
                toScreenProjectedY(staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()), 0.0),
                toScreenProjectedY(staircase.pointAtLocalPosition(0, staircase.heightMillimeters()), 0.0)
        };
        graphics.fillPolygon(xPoints, yPoints, 4);
        graphics.strokePolygon(xPoints, yPoints, 4);
    }

    private void strokeLocalRect(GraphicsContext graphics, Staircase staircase, double localX, double localY, double localWidth, double localHeight) {
        strokeLocalLine(graphics, staircase, localX, localY, localX + localWidth, localY);
        strokeLocalLine(graphics, staircase, localX + localWidth, localY, localX + localWidth, localY + localHeight);
        strokeLocalLine(graphics, staircase, localX + localWidth, localY + localHeight, localX, localY + localHeight);
        strokeLocalLine(graphics, staircase, localX, localY + localHeight, localX, localY);
    }

    private void strokeLocalLine(GraphicsContext graphics, Staircase staircase, double startLocalX, double startLocalY, double endLocalX, double endLocalY) {
        PlanPoint start = staircase.pointAtLocalPosition(startLocalX, startLocalY);
        PlanPoint end = staircase.pointAtLocalPosition(endLocalX, endLocalY);
        graphics.strokeLine(
                toScreenProjectedX(start, 0.0),
                toScreenProjectedY(start, 0.0),
                toScreenProjectedX(end, 0.0),
                toScreenProjectedY(end, 0.0)
        );
    }

    private void drawWall(GraphicsContext graphics, PlanSegment segment, Length thickness, Color color, double widthFactor) {
        double screenStartX = toScreenProjectedX(segment.start(), 0.0);
        double screenStartY = toScreenProjectedY(segment.start(), 0.0);
        double screenEndX = toScreenProjectedX(segment.end(), 0.0);
        double screenEndY = toScreenProjectedY(segment.end(), 0.0);
        graphics.setStroke(color);
        graphics.setLineWidth(Math.max(thickness.toMillimeters() * scale() * widthFactor, 2.0));
        graphics.strokeLine(screenStartX, screenStartY, screenEndX, screenEndY);
    }

    private void drawWallElevation(GraphicsContext graphics, Wall wall, boolean selected) {
        double startX = toScreenProjectedX(wall.axis().start(), 0.0);
        double endX = toScreenProjectedX(wall.axis().end(), 0.0);
        double floorY = toScreenProjectedY(wall.axis().start(), 0.0);
        double topY = toScreenProjectedY(wall.axis().start(), wall.height().toMillimeters());
        double left = Math.min(startX, endX);
        double width = Math.max(Math.abs(endX - startX), 3.0);
        double top = Math.min(floorY, topY);
        double height = Math.max(Math.abs(floorY - topY), 3.0);
        graphics.setFill(selected ? Color.color(0.85, 0.57, 0.22, 0.24) : Color.color(0.23, 0.39, 0.54, 0.18));
        graphics.fillRect(left, top, width, height);
        graphics.setStroke(selected ? Color.web("#d97f2f") : Color.web("#274c77"));
        graphics.setLineWidth(selected ? 2.8 : 2.0);
        graphics.strokeRect(left, top, width, height);
    }

    private void drawRoomElevation(GraphicsContext graphics, Room room) {
        double minProjectedX = room.outline().stream()
                .mapToDouble(point -> projectHorizontal(point, 0.0))
                .min()
                .orElse(0.0);
        double maxProjectedX = room.outline().stream()
                .mapToDouble(point -> projectHorizontal(point, 0.0))
                .max()
                .orElse(0.0);
        double left = toScreenHorizontal(minProjectedX);
        double right = toScreenHorizontal(maxProjectedX);
        double floorY = toScreenVertical(0.0);
        double topY = toScreenVertical(-room.roomHeight().toMillimeters());
        graphics.setFill(Color.color(0.77, 0.64, 0.45, 0.16));
        graphics.fillRect(Math.min(left, right), Math.min(floorY, topY), Math.max(Math.abs(right - left), 3.0), Math.max(Math.abs(floorY - topY), 3.0));
        graphics.setStroke(Color.color(0.55, 0.43, 0.25, 0.65));
        graphics.setLineWidth(1.6);
        graphics.strokeRect(Math.min(left, right), Math.min(floorY, topY), Math.max(Math.abs(right - left), 3.0), Math.max(Math.abs(floorY - topY), 3.0));
    }

    private void drawStairElevation(GraphicsContext graphics, Staircase staircase) {
        double[] projectedHorizontals = {
                projectHorizontal(staircase.pointAtLocalPosition(0, 0), 0.0),
                projectHorizontal(staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0), 0.0),
                projectHorizontal(staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()), 0.0),
                projectHorizontal(staircase.pointAtLocalPosition(0, staircase.heightMillimeters()), 0.0)
        };
        double minHorizontal = java.util.Arrays.stream(projectedHorizontals).min().orElse(0.0);
        double maxHorizontal = java.util.Arrays.stream(projectedHorizontals).max().orElse(0.0);
        double left = toScreenHorizontal(minHorizontal);
        double right = toScreenHorizontal(maxHorizontal);
        double floorY = toScreenVertical(0.0);
        double topY = toScreenVertical(-staircase.totalHeight().toMillimeters());
        graphics.fillRect(Math.min(left, right), Math.min(floorY, topY), Math.max(Math.abs(right - left), 3.0), Math.max(Math.abs(floorY - topY), 3.0));
        graphics.strokeRect(Math.min(left, right), Math.min(floorY, topY), Math.max(Math.abs(right - left), 3.0), Math.max(Math.abs(floorY - topY), 3.0));
    }

    private void drawOpeningElevation(GraphicsContext graphics, PlanPoint openingStart, PlanPoint openingEnd, double baseHeightMillimeters, double openingHeightMillimeters, Color color) {
        double startX = toScreenProjectedX(openingStart, 0.0);
        double endX = toScreenProjectedX(openingEnd, 0.0);
        double bottomY = toScreenVertical(-baseHeightMillimeters);
        double topY = toScreenVertical(-(baseHeightMillimeters + openingHeightMillimeters));
        graphics.setStroke(color);
        graphics.setLineWidth(2.8);
        graphics.strokeRect(Math.min(startX, endX), Math.min(bottomY, topY), Math.max(Math.abs(endX - startX), 3.0), Math.max(Math.abs(bottomY - topY), 3.0));
    }

    private void drawPreview(GraphicsContext graphics) {
        if (!projectionService.isPlanView(activeView.get())) {
            return;
        }
        double startX = Math.min(previewSegment.start().xMillimeters(), previewSegment.end().xMillimeters());
        double startY = Math.min(previewSegment.start().yMillimeters(), previewSegment.end().yMillimeters());
        double endX = Math.max(previewSegment.start().xMillimeters(), previewSegment.end().xMillimeters());
        double endY = Math.max(previewSegment.start().yMillimeters(), previewSegment.end().yMillimeters());
        if (currentTool() == DrawingTool.ROOM) {
            graphics.setFill(Color.color(0.76, 0.49, 0.27, 0.18));
            graphics.fillRect(
                    toScreenProjectedX(new PlanPoint(startX, startY), 0.0),
                    toScreenProjectedY(new PlanPoint(startX, startY), 0.0),
                    (endX - startX) * scale(),
                    (endY - startY) * scale()
            );
            graphics.setStroke(Color.web("#c26d32"));
            graphics.setLineWidth(2.0);
            graphics.strokeRect(
                    toScreenProjectedX(new PlanPoint(startX, startY), 0.0),
                    toScreenProjectedY(new PlanPoint(startX, startY), 0.0),
                    (endX - startX) * scale(),
                    (endY - startY) * scale()
            );
        } else if (currentTool() == DrawingTool.STAIR) {
            graphics.setFill(Color.color(0.45, 0.37, 0.29, 0.18));
            graphics.setStroke(Color.web("#7f6a55"));
            graphics.setLineWidth(2.0);
            graphics.fillRect(
                    toScreenProjectedX(new PlanPoint(startX, startY), 0.0),
                    toScreenProjectedY(new PlanPoint(startX, startY), 0.0),
                    (endX - startX) * scale(),
                    (endY - startY) * scale()
            );
            graphics.strokeRect(
                    toScreenProjectedX(new PlanPoint(startX, startY), 0.0),
                    toScreenProjectedY(new PlanPoint(startX, startY), 0.0),
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
        double midX = (toScreenProjectedX(segment.start(), 0.0) + toScreenProjectedX(segment.end(), 0.0)) / 2.0;
        double midY = (toScreenProjectedY(segment.start(), 0.0) + toScreenProjectedY(segment.end(), 0.0)) / 2.0;
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
        graphics.fillText(activeView.get().overlayDescription(), 28, 62);
    }

    private void drawCompass(GraphicsContext graphics) {
        if (!projectionService.isPlanView(activeView.get())) {
            return;
        }
        double x = drawingCanvas.getWidth() - 78;
        double y = 34;
        double angle = Math.toRadians(currentNorthAngleDegrees() - activeView.get().cameraAzimuthDegrees());
        double arrowLength = 14.0;
        double arrowX = Math.sin(angle) * arrowLength;
        double arrowY = -Math.cos(angle) * arrowLength;
        graphics.setStroke(Color.web("#4b6a88"));
        graphics.setFill(Color.web("#4b6a88"));
        graphics.setLineWidth(2);
        graphics.strokeOval(x - 18, y - 18, 36, 36);
        graphics.strokeLine(x - arrowX, y - arrowY, x + arrowX, y + arrowY);
        graphics.strokeLine(x + arrowX, y + arrowY, x + arrowX - 5 * Math.cos(angle), y + arrowY + 5 * Math.sin(angle));
        graphics.strokeLine(x + arrowX, y + arrowY, x + arrowX + 5 * Math.cos(angle), y + arrowY - 5 * Math.sin(angle));
        graphics.fillText("N", x + arrowX - 4, y + arrowY - 8);
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
        if (!projectionService.isPlanView(activeView.get())) {
            graphics.fillText("Achse", 6, 12);
            return;
        }

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
        if (!projectionService.isPlanView(activeView.get())) {
            graphics.fillText("H", 6, 12);
            return;
        }

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

    private double currentNorthAngleDegrees() {
        return parseAngle(northAngleField).map(Angle::degrees).orElse(0.0);
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
                    rememberStateForUndo();
                    Level level = project.createLevel(levelName);
                    availableLevels.add(level);
                    activateLevel(level);
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
                    rememberStateForUndo();
                    activeLevel.get().addDoor(door);
                    selectSingle(new SelectionKey(RenderableKind.DOOR, activeLevel.get().name(), door.id().toString()));
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
                    rememberStateForUndo();
                    activeLevel.get().addWindow(window);
                    selectSingle(new SelectionKey(RenderableKind.WINDOW, activeLevel.get().name(), window.id().toString()));
                    markThreeDDirty();
                });
    }

    private void startGuideDrag(GuideOrientation orientation, double worldMillimeters) {
        if (!isDirectEditingView()) {
            render();
            draftLabel.setText("Hilfslinien lassen sich aktuell nur in der Draufsicht platzieren.");
            return;
        }
        if (pendingGuideOrientation == null) {
            rememberStateForUndo();
        }
        pendingGuideOrientation = orientation;
        pendingGuideWorldMillimeters = worldMillimeters;
        draftLabel.setText("Hilfslinie: " + formatGuidePosition(orientation, worldMillimeters));
        render();
    }

    private void updateGuideDrag(GuideOrientation orientation, double worldMillimeters) {
        if (pendingGuideOrientation == orientation) {
            pendingGuideWorldMillimeters = worldMillimeters;
            draftLabel.setText("Hilfslinie: " + formatGuidePosition(orientation, worldMillimeters));
            render();
        }
    }

    private void finishGuideDrag(GuideOrientation orientation, double worldMillimeters) {
        if (pendingGuideOrientation != orientation) {
            return;
        }
        guideLines.add(new GuideLine(orientation, worldMillimeters));
        pendingGuideOrientation = null;
        draftLabel.setText("Hilfslinie platziert: " + formatGuidePosition(orientation, worldMillimeters));
        render();
    }

    private void removeNearestGuide(PlanPoint clickPoint) {
        guideLines.stream()
                .filter(guideLine -> guideDistance(guideLine, clickPoint) <= SNAP_TOLERANCE.toMillimeters())
                .findFirst()
                .ifPresent(guideLine -> {
                    rememberStateForUndo();
                    guideLines.remove(guideLine);
                });
        render();
    }

    private double guideDistance(GuideLine guideLine, PlanPoint clickPoint) {
        if (guideLine.orientation() == GuideOrientation.VERTICAL) {
            return Math.abs(guideLine.worldMillimeters() - clickPoint.xMillimeters());
        }
        return Math.abs(guideLine.worldMillimeters() - clickPoint.yMillimeters());
    }

    private double guideWorldPositionFromHorizontalRuler(MouseEvent event) {
        return snapGuidePoint(projectedPointInDrawingPane(event)).yMillimeters();
    }

    private double guideWorldPositionFromVerticalRuler(MouseEvent event) {
        return snapGuidePoint(projectedPointInDrawingPane(event)).xMillimeters();
    }

    private Point2D projectedPointInDrawingPane(MouseEvent event) {
        Point2D localPoint = drawingPane.sceneToLocal(event.getSceneX(), event.getSceneY());
        double x = clamp(localPoint.getX(), 0.0, drawingCanvas.getWidth());
        double y = clamp(localPoint.getY(), 0.0, drawingCanvas.getHeight());
        return new Point2D(x, y);
    }

    private PlanPoint snapGuidePoint(Point2D point) {
        return snapService.snap(
                screenToWorld(point.getX(), point.getY()),
                currentConstraints(false),
                activeLevel.get().walls()
        );
    }

    private String formatGuidePosition(GuideOrientation orientation, double worldMillimeters) {
        String axis = orientation == GuideOrientation.VERTICAL ? "X" : "Y";
        return axis + "=" + String.format(Locale.GERMAN, "%.2f m", worldMillimeters / 1000.0);
    }

    private void exportCurrentLevel() {
        FileChooser fileChooser = createDxfFileChooser();
        fileChooser.setInitialFileName(activeLevel.get().name().replace(' ', '_') + ".dxf");
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            return;
        }
        exportCurrentLevel(file.toPath());
    }

    private void exportCurrentLevel(Path targetFile) {
        try {
            Path exportPath = exchangeFileNameService.ensureSingleExtension(targetFile, ".dxf");
            levelExchangeService.exportLevel(activeLevel.get(), exportPath);
            draftLabel.setText("DXF exportiert: " + exportPath.getFileName());
        } catch (IOException exception) {
            draftLabel.setText("DXF-Export fehlgeschlagen: " + exception.getMessage());
        }
    }

    private void exportProjectAsDxf() {
        FileChooser fileChooser = createDxfFileChooser();
        fileChooser.setInitialFileName(project.name().replace(' ', '_') + "_Gebaeude.dxf");
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            return;
        }
        exportProjectAsDxf(file.toPath());
    }

    private void exportProjectAsDxf(Path targetFile) {
        try {
            Path exportPath = exchangeFileNameService.ensureSingleExtension(targetFile, ".dxf");
            projectExchangeService.exportProject(project, exportPath);
            draftLabel.setText("Gebäude-DXF exportiert: " + exportPath.getFileName());
        } catch (IOException exception) {
            draftLabel.setText("Gebäude-DXF-Export fehlgeschlagen: " + exception.getMessage());
        }
    }

    private void importLevel() {
        FileChooser fileChooser = createDxfFileChooser();
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        importLevel(file.toPath());
    }

    private void importLevel(Path sourceFile) {
        try {
            rememberStateForUndo();
            String levelName = uniqueLevelName(exchangeFileNameService.stripRepeatedExtension(sourceFile, ".dxf"));
            Level importedLevel = levelExchangeService.importLevel(sourceFile, levelName);
            project.addLevel(importedLevel);
            availableLevels.add(importedLevel);
            activateLevel(importedLevel);
            draftLabel.setText("DXF importiert: " + sourceFile.getFileName());
        } catch (IOException exception) {
            draftLabel.setText("DXF-Import fehlgeschlagen: " + exception.getMessage());
        }
    }

    private void importProjectFromDxf() {
        FileChooser fileChooser = createDxfFileChooser();
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        importProjectFromDxf(file.toPath());
    }

    private void importProjectFromDxf(Path sourceFile) {
        try {
            rememberStateForUndo();
            ProjectModel importedProject = projectExchangeService.importProject(sourceFile, project.name());
            project.replaceWith(importedProject);
            availableLevels.setAll(project.levels());
            guideLines.clear();
            clearSelectionsInternal();
            activateLevel(project.primaryLevel());
            draftLabel.setText("Gebäude-DXF importiert: " + sourceFile.getFileName());
        } catch (IOException exception) {
            draftLabel.setText("Gebäude-DXF-Import fehlgeschlagen: " + exception.getMessage());
        }
    }

    private FileChooser createDxfFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("DXF-Datei auswählen");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DXF-Dateien", "*.dxf"));
        return fileChooser;
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
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CADas Teilebibliothek", "*.cadasparts"),
                new FileChooser.ExtensionFilter("AutoCAD-Bibliothek", "*.dwg")
        );
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        importPartLibrary(file.toPath());
    }

    private void importPartLibrary(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".dwg")) {
            if (!cadLibraryReferences.contains(sourceFile)) {
                cadLibraryReferences.add(sourceFile);
            }
            updateCadLibrarySummary();
            draftLabel.setText("DWG-Bibliothek registriert: " + fileName);
            return;
        }
        try {
            StandardPartLibrary importedLibrary = partLibraryImportService.importLibrary(sourceFile);
            availableDoorPresets.addAll(importedLibrary.doorPresets());
            availableWindowPresets.addAll(importedLibrary.windowPresets());
            availableStairPresets.addAll(importedLibrary.stairPresets());
            if (!cadLibraryReferences.contains(sourceFile)) {
                cadLibraryReferences.add(sourceFile);
            }
            updateCadLibrarySummary();
            if (!importedLibrary.doorPresets().isEmpty()) {
                doorPresetSelector.setValue(importedLibrary.doorPresets().getFirst());
            }
            if (!importedLibrary.windowPresets().isEmpty()) {
                windowPresetSelector.setValue(importedLibrary.windowPresets().getFirst());
            }
            if (!importedLibrary.stairPresets().isEmpty()) {
                stairPresetSelector.setValue(importedLibrary.stairPresets().getFirst());
            }
            draftLabel.setText("Teilebibliothek geladen: " + fileName);
        } catch (IOException exception) {
            draftLabel.setText("Teilebibliothek fehlgeschlagen: " + exception.getMessage());
        }
    }

    private void updateCadLibrarySummary() {
        if (cadLibraryReferences.isEmpty()) {
            cadLibrarySummaryLabel.setText("Keine externen CAD-Bibliotheken registriert.");
            return;
        }
        cadLibrarySummaryLabel.setText(cadLibraryReferences.stream()
                .map(path -> "• " + path.getFileName())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("Keine externen CAD-Bibliotheken registriert."));
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

    private double toScreenProjectedX(PlanPoint point, double heightMillimeters) {
        return toScreenHorizontal(projectHorizontal(point, heightMillimeters));
    }

    private double toScreenProjectedY(PlanPoint point, double heightMillimeters) {
        return toScreenVertical(projectVertical(point, heightMillimeters));
    }

    private double toScreenHorizontal(double projectedMillimeters) {
        return offsetX + projectedMillimeters * scale();
    }

    private double toScreenVertical(double projectedMillimeters) {
        return offsetY + projectedMillimeters * scale();
    }

    private double projectHorizontal(PlanPoint point, double heightMillimeters) {
        return projectionService.project(point, heightMillimeters, activeView.get()).horizontalMillimeters();
    }

    private double projectVertical(PlanPoint point, double heightMillimeters) {
        return projectionService.project(point, heightMillimeters, activeView.get()).verticalMillimeters();
    }

    private double scale() {
        return BASE_PIXELS_PER_MILLIMETER * zoom;
    }

    private boolean isDirectEditingView() {
        return activeView.get() == ViewOrientation.TOP;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void resetTwoDView() {
        zoom = 1.0;
        offsetX = 240.0;
        offsetY = 160.0;
        render();
    }

    private void clearProject() {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Alle Etagen, Bauteile, Hilfslinien und Dachinformationen des aktuellen Projekts werden entfernt. Dieser Schritt kann derzeit nicht rückgängig gemacht werden.",
                ButtonType.OK,
                ButtonType.CANCEL
        );
        alert.setTitle("Projekt leeren");
        alert.setHeaderText("Projekt wirklich leeren?");
        alert.getDialogPane().setPrefWidth(520);
        Window window = getScene() != null ? getScene().getWindow() : null;
        if (window != null) {
            alert.initOwner(window);
        }
        alert.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(ignored -> {
                    rememberStateForUndo();
                    Level level = project.resetToSingleLevel("Erdgeschoss");
                    availableLevels.setAll(project.levels());
                    guideLines.clear();
                    clearSelectionsInternal();
                    selectedEndpointGroup = null;
                    draftStart = null;
                    previewSegment = null;
                    pendingGuideOrientation = null;
                    activateLevel(level);
                    draftLabel.setText("Projekt geleert.");
                });
    }

    private void undo() {
        history.undo(captureSnapshot())
                .ifPresent(snapshot -> {
                    restoreSnapshot(snapshot);
                    draftLabel.setText("Letzte Änderung rückgängig gemacht.");
                });
        updateActionButtons();
    }

    private void redo() {
        history.redo(captureSnapshot())
                .ifPresent(snapshot -> {
                    restoreSnapshot(snapshot);
                    draftLabel.setText("Änderung wiederhergestellt.");
                });
        updateActionButtons();
    }

    private void rememberStateForUndo() {
        history.remember(captureSnapshot());
        updateActionButtons();
    }

    private WorkbenchSnapshot captureSnapshot() {
        return new WorkbenchSnapshot(
                project.copy(),
                guideLines,
                activeLevel.get().name(),
                List.copyOf(selectedSelections),
                selectedSelection.get()
        );
    }

    private void restoreSnapshot(WorkbenchSnapshot snapshot) {
        project.replaceWith(snapshot.project());
        availableLevels.setAll(project.levels());
        guideLines.setAll(snapshot.guideLines());
        selectedEndpointGroup = null;
        draftStart = null;
        previewSegment = null;
        pendingGuideOrientation = null;
        historyCapturedForDrag = false;
        selectedSelections.clear();
        selectedSelections.addAll(snapshot.selectedSelections());
        selectedSelection.set(snapshot.primarySelection());
        Level level = project.levels().stream()
                .filter(candidate -> candidate.name().equals(snapshot.activeLevelName()))
                .findFirst()
                .orElse(project.primaryLevel());
        activateLevel(level);
    }

    private void clearSelection() {
        clearSelectionsInternal();
        selectedEndpointGroup = null;
        updateActionButtons();
        render();
    }

    private void deleteSelection() {
        if (selectedSelections.isEmpty()) {
            return;
        }
        rememberStateForUndo();
        boolean removed = false;
        for (SelectionKey selectionKey : List.copyOf(selectedSelections)) {
            UUID id = UUID.fromString(selectionKey.elementId());
            removed |= switch (selectionKey.kind()) {
                case WALL -> activeLevel.get().removeWall(id);
                case ROOM_VOLUME, ROOM_FLOOR, ROOM_CEILING -> activeLevel.get().removeRoom(id);
                case DOOR -> activeLevel.get().removeDoor(id);
                case WINDOW -> activeLevel.get().removeWindow(id);
                case STAIR -> activeLevel.get().removeStaircase(id);
                default -> false;
            };
        }
        if (removed) {
            clearSelectionsInternal();
            markThreeDDirty();
            draftLabel.setText("Ausgewählte Bauteile gelöscht.");
            render();
            return;
        }
        draftLabel.setText("Auswahl konnte nicht gelöscht werden.");
        updateActionButtons();
    }

    private void updateSelection(SelectionKey selectionKey, boolean toggleSelection) {
        if (selectionKey == null) {
            if (!toggleSelection) {
                clearSelectionsInternal();
            }
            syncSelectionState();
            return;
        }
        if (toggleSelection) {
            if (!selectedSelections.add(selectionKey)) {
                selectedSelections.remove(selectionKey);
                selectedSelection.set(selectedSelections.stream().reduce((first, second) -> second).orElse(null));
                syncSelectionState();
                return;
            }
            selectedSelection.set(selectionKey);
            syncSelectionState();
            return;
        }
        selectSingle(selectionKey);
        syncSelectionState();
    }

    private void selectSingle(SelectionKey selectionKey) {
        selectedSelections.clear();
        if (selectionKey != null) {
            selectedSelections.add(selectionKey);
        }
        selectedSelection.set(selectionKey);
        syncSelectionState();
    }

    private void clearSelectionsInternal() {
        selectedSelections.clear();
        selectedSelection.set(null);
        syncSelectionState();
    }

    private void syncSelectionState() {
        threeDViewport.setSelectedSelections(Set.copyOf(selectedSelections));
        rebuildSelectionContextMenu();
        updatePropertySectionVisibility();
        updateActionButtons();
    }

    private void rebuildSelectionContextMenu() {
        selectionContextMenu.getItems().setAll(
                menuItem("Eigenschaften auf Auswahl anwenden", this::applyCurrentInputsToSelection, null),
                menuItem("Auswahl löschen", this::deleteSelection, null),
                menuItem("Auswahl aufheben", this::clearSelection, null)
        );
        if (selectedSelections.stream().anyMatch(this::isRotatableSelection)) {
            selectionContextMenu.getItems().addAll(
                    menuItem("Bauteile 90° im Uhrzeigersinn drehen", this::rotateSelectedComponentsClockwise, null),
                    menuItem("Bauteile 90° gegen den Uhrzeigersinn drehen", this::rotateSelectedComponentsCounterClockwise, null)
            );
        }
    }

    private void syncInputsFromPrimarySelection() {
        if (selectedSelection.get() == null) {
            return;
        }
        switch (selectedSelection.get().kind()) {
            case WALL -> activeLevel.get().walls().stream()
                    .filter(wall -> wall.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(wall -> {
                        wallThicknessField.setText(formatValue(wall.thickness(), LengthUnit.CENTIMETER, 1));
                        wallHeightField.setText(formatValue(wall.height(), LengthUnit.METER, 2));
                    });
            case ROOM_VOLUME, ROOM_FLOOR, ROOM_CEILING -> activeLevel.get().rooms().stream()
                    .filter(room -> room.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(room -> {
                        roomNameField.setText(room.name());
                        roomHeightField.setText(formatValue(room.roomHeight(), LengthUnit.METER, 2));
                        floorThicknessField.setText(formatValue(room.floorThickness(), LengthUnit.CENTIMETER, 1));
                        ceilingThicknessField.setText(formatValue(room.ceilingThickness(), LengthUnit.CENTIMETER, 1));
                    });
            case DOOR -> activeLevel.get().doors().stream()
                    .filter(door -> door.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(door -> {
                        doorWidthField.setText(formatValue(door.width(), LengthUnit.METER, 2));
                        doorHeightField.setText(formatValue(door.height(), LengthUnit.METER, 2));
                        thresholdField.setText(formatValue(door.thresholdHeight(), LengthUnit.CENTIMETER, 1));
                    });
            case WINDOW -> activeLevel.get().windows().stream()
                    .filter(window -> window.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(window -> {
                        windowWidthField.setText(formatValue(window.width(), LengthUnit.METER, 2));
                        windowHeightField.setText(formatValue(window.windowHeight(), LengthUnit.METER, 2));
                        sillHeightField.setText(formatValue(window.sillHeight(), LengthUnit.CENTIMETER, 1));
                    });
            case STAIR -> activeLevel.get().staircases().stream()
                    .filter(stair -> stair.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(stair -> {
                        stairHeightField.setText(formatValue(stair.totalHeight(), LengthUnit.METER, 2));
                        stairStepsField.setText(Integer.toString(stair.stepCount()));
                    });
            default -> {
            }
        }
    }

    private void applyCurrentInputsToSelection() {
        if (selectedSelections.isEmpty() || selectedSelection.get() == null) {
            return;
        }
        rememberStateForUndo();
        switch (selectedSelection.get().kind()) {
            case WALL -> activeLevel.get().replaceWalls(activeLevel.get().walls().stream()
                    .map(wall -> selectedIds().contains(wall.id().toString())
                            ? new Wall(wall.id(), wall.axis(), currentWallThickness(), currentWallHeight())
                            : wall)
                    .toList());
            case ROOM_VOLUME, ROOM_FLOOR, ROOM_CEILING -> activeLevel.get().replaceRooms(activeLevel.get().rooms().stream()
                    .map(room -> selectedIds().contains(room.id().toString())
                            ? new Room(room.id(), currentRoomName(), room.outline(), currentRoomHeight(), currentFloorThickness(), currentCeilingThickness())
                            : room)
                    .toList());
            case DOOR -> activeLevel.get().replaceDoors(activeLevel.get().doors().stream()
                    .map(door -> selectedIds().contains(door.id().toString())
                            ? new Door(door.id(), door.wallId(), door.offsetFromStart(), currentDoorWidth(), currentDoorHeight(), currentThresholdHeight())
                            : door)
                    .toList());
            case WINDOW -> activeLevel.get().replaceWindows(activeLevel.get().windows().stream()
                    .map(window -> selectedIds().contains(window.id().toString())
                            ? new WindowElement(window.id(), window.wallId(), window.offsetFromStart(), currentWindowWidth(), currentSillHeight(), currentWindowHeight())
                            : window)
                    .toList());
            case STAIR -> activeLevel.get().replaceStaircases(activeLevel.get().staircases().stream()
                    .map(stair -> selectedIds().contains(stair.id().toString())
                            ? new Staircase(stair.id(), stair.stairType(), stair.firstCorner(), stair.oppositeCorner(), currentStairHeight(), currentStairSteps(), stair.rotationQuarterTurns())
                            : stair)
                    .toList());
            default -> {
            }
        }
        markThreeDDirty();
        draftLabel.setText("Eigenschaften auf Auswahl angewendet.");
        render();
    }

    private Set<String> selectedIds() {
        return selectedSelections.stream()
                .map(SelectionKey::elementId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private String formatValue(Length length, LengthUnit unit, int decimals) {
        return length.format(unit, decimals)
                .replace(" " + unit.symbol(), "")
                .replace('.', ',');
    }

    private void rotateSelectedComponentsClockwise() {
        rotateSelectedComponents(true);
    }

    private void rotateSelectedComponentsCounterClockwise() {
        rotateSelectedComponents(false);
    }

    private void rotateSelectedComponents(boolean clockwise) {
        if (selectedSelections.stream().noneMatch(this::isRotatableSelection)) {
            return;
        }
        rememberStateForUndo();
        QuarterTurnRotationService.RotationResult rotationResult = quarterTurnRotationService.rotate(activeLevel.get(), Set.copyOf(selectedSelections), clockwise);
        activeLevel.get().replaceWalls(rotationResult.walls());
        activeLevel.get().replaceRooms(rotationResult.rooms());
        activeLevel.get().replaceStaircases(rotationResult.staircases());
        markThreeDDirty();
        draftLabel.setText("Ausgewählte Bauteile gedreht.");
        render();
    }

    private void activateLevel(Level level) {
        if (levelSelector.getValue() != level) {
            levelSelector.setValue(level);
            return;
        }
        activeLevel.set(level);
        threeDViewport.syncLevels(availableLevels, level.name());
        markThreeDDirty();
        updatePropertySectionVisibility();
        updateActionButtons();
        render();
    }

    private void handleThreeDSelection(SelectionKey selectionKey) {
        if (selectionKey == null) {
            return;
        }
        availableLevels.stream()
                .filter(level -> level.name().equals(selectionKey.levelName()))
                .findFirst()
                .ifPresent(this::activateLevel);
        selectSingle(selectionKey);
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
        return selectedSelections.stream().anyMatch(selection ->
                selection.kind() == kind
                        && selection.levelName().equals(activeLevel.get().name())
                        && selection.elementId().equals(elementId)
        );
    }

    public WorkbenchAutomationSnapshot automationSnapshot() {
        refreshThreeDIfNeeded();
        return new WorkbenchAutomationSnapshot(
                project.name(),
                activeLevel.get().name(),
                activeView.get().name(),
                currentTool().name(),
                activeLevel.get().walls().size(),
                activeLevel.get().rooms().size(),
                activeLevel.get().doors().size(),
                activeLevel.get().windows().size(),
                activeLevel.get().staircases().size(),
                selectedSelections.size(),
                cadLibraryReferences.size(),
                draftLabel.getText()
        );
    }

    public void automationSetTool(String toolName) {
        toolSelector.setValue(DrawingTool.valueOf(toolName.trim().toUpperCase(Locale.ROOT)));
    }

    public void automationSelectLevel(String levelName) {
        availableLevels.stream()
                .filter(level -> level.name().equals(levelName))
                .findFirst()
                .ifPresentOrElse(this::activateLevel, () -> {
                    throw new IllegalArgumentException("Etage `" + levelName + "` ist nicht vorhanden.");
                });
    }

    public void automationSetField(String fieldName, String value) {
        textFieldByName(fieldName).setText(value);
        updatePropertySectionVisibility();
        render();
    }

    public void automationSetUnit(String fieldName, String unitName) {
        unitSelectorByName(fieldName).setValue(LengthUnit.valueOf(unitName.trim().toUpperCase(Locale.ROOT)));
        render();
    }

    public void automationPlaceGuide(String orientationName, double worldMillimeters) {
        GuideOrientation orientation = GuideOrientation.valueOf(orientationName.trim().toUpperCase(Locale.ROOT));
        startGuideDrag(orientation, worldMillimeters);
        finishGuideDrag(orientation, worldMillimeters);
    }

    public void automationCanvasClick(double x, double y, MouseButton button, boolean shiftDown, boolean shortcutDown, boolean altDown) {
        ensureCanvasReady();
        drawingCanvas.fireEvent(mouseEvent(MouseEvent.MOUSE_PRESSED, x, y, button, shiftDown, shortcutDown, altDown, false));
        drawingCanvas.fireEvent(mouseEvent(MouseEvent.MOUSE_RELEASED, x, y, button, shiftDown, shortcutDown, altDown, false));
    }

    public void automationCanvasDrag(double fromX, double fromY, double toX, double toY, MouseButton button, boolean shiftDown, boolean shortcutDown, boolean altDown) {
        ensureCanvasReady();
        drawingCanvas.fireEvent(mouseEvent(MouseEvent.MOUSE_PRESSED, fromX, fromY, button, shiftDown, shortcutDown, altDown, false));
        drawingCanvas.fireEvent(mouseEvent(MouseEvent.MOUSE_DRAGGED, toX, toY, button, shiftDown, shortcutDown, altDown, true));
        drawingCanvas.fireEvent(mouseEvent(MouseEvent.MOUSE_RELEASED, toX, toY, button, shiftDown, shortcutDown, altDown, false));
    }

    public void automationInvoke(String actionName, Path path) {
        switch (actionName) {
            case "undo" -> undo();
            case "redo" -> redo();
            case "clearSelection" -> clearSelection();
            case "deleteSelection" -> deleteSelection();
            case "applySelectionProperties" -> applyCurrentInputsToSelection();
            case "rotateSelectedComponentsClockwise", "rotateSelectedStairsClockwise" -> rotateSelectedComponentsClockwise();
            case "rotateSelectedComponentsCounterClockwise", "rotateSelectedStairsCounterClockwise" -> rotateSelectedComponentsCounterClockwise();
            case "exportProjectDxf" -> exportProjectAsDxf(requirePath(path, actionName));
            case "importProjectDxf" -> importProjectFromDxf(requirePath(path, actionName));
            case "exportLevelDxf" -> exportCurrentLevel(requirePath(path, actionName));
            case "importLevelDxf" -> importLevel(requirePath(path, actionName));
            case "importPartLibrary" -> importPartLibrary(requirePath(path, actionName));
            case "clearProject" -> clearProjectWithoutDialog();
            default -> throw new IllegalArgumentException("Automatisierungsaktion `" + actionName + "` ist unbekannt.");
        }
    }

    public void automationSetStatusText(String text) {
        draftLabel.setText(text);
    }

    private void clearProjectWithoutDialog() {
        rememberStateForUndo();
        Level level = project.resetToSingleLevel("Erdgeschoss");
        availableLevels.setAll(project.levels());
        guideLines.clear();
        clearSelectionsInternal();
        selectedEndpointGroup = null;
        draftStart = null;
        previewSegment = null;
        pendingGuideOrientation = null;
        activateLevel(level);
        draftLabel.setText("Projekt geleert.");
    }

    private Path requirePath(Path path, String actionName) {
        if (path == null) {
            throw new IllegalArgumentException("Für `" + actionName + "` wird ein Parameter `path` benötigt.");
        }
        return path;
    }

    private TextField textFieldByName(String fieldName) {
        return switch (fieldName) {
            case "grid" -> gridField;
            case "length" -> lengthField;
            case "angle" -> angleField;
            case "northAngle" -> northAngleField;
            case "wallThickness" -> wallThicknessField;
            case "wallHeight" -> wallHeightField;
            case "roomName" -> roomNameField;
            case "roomHeight" -> roomHeightField;
            case "floorThickness" -> floorThicknessField;
            case "ceilingThickness" -> ceilingThicknessField;
            case "doorWidth" -> doorWidthField;
            case "doorHeight" -> doorHeightField;
            case "threshold" -> thresholdField;
            case "windowWidth" -> windowWidthField;
            case "windowHeight" -> windowHeightField;
            case "sillHeight" -> sillHeightField;
            case "stairHeight" -> stairHeightField;
            case "stairSteps" -> stairStepsField;
            default -> throw new IllegalArgumentException("Eingabefeld `" + fieldName + "` ist unbekannt.");
        };
    }

    private ComboBox<LengthUnit> unitSelectorByName(String fieldName) {
        return switch (fieldName) {
            case "grid" -> gridUnit;
            case "length" -> lengthUnit;
            case "wallThickness" -> wallThicknessUnit;
            case "wallHeight" -> wallHeightUnit;
            case "roomHeight" -> roomHeightUnit;
            case "floorThickness" -> floorThicknessUnit;
            case "ceilingThickness" -> ceilingThicknessUnit;
            case "doorWidth" -> doorWidthUnit;
            case "doorHeight" -> doorHeightUnit;
            case "threshold" -> thresholdUnit;
            case "windowWidth" -> windowWidthUnit;
            case "windowHeight" -> windowHeightUnit;
            case "sillHeight" -> sillHeightUnit;
            case "stairHeight" -> stairHeightUnit;
            default -> throw new IllegalArgumentException("Einheitenselektor `" + fieldName + "` ist unbekannt.");
        };
    }

    private void ensureCanvasReady() {
        if (drawingCanvas.getWidth() <= 0 || drawingCanvas.getHeight() <= 0) {
            resizeCanvases();
        }
    }

    private boolean isRotatableSelection(SelectionKey selectionKey) {
        return selectionKey.kind() == RenderableKind.WALL
                || selectionKey.kind() == RenderableKind.ROOM_VOLUME
                || selectionKey.kind() == RenderableKind.ROOM_FLOOR
                || selectionKey.kind() == RenderableKind.ROOM_CEILING
                || selectionKey.kind() == RenderableKind.STAIR;
    }

    private MouseEvent mouseEvent(javafx.event.EventType<MouseEvent> type,
                                  double x,
                                  double y,
                                  MouseButton button,
                                  boolean shiftDown,
                                  boolean shortcutDown,
                                  boolean altDown,
                                  boolean buttonDown) {
        boolean primaryDown = button == MouseButton.PRIMARY && buttonDown;
        boolean middleDown = button == MouseButton.MIDDLE && buttonDown;
        boolean secondaryDown = button == MouseButton.SECONDARY && buttonDown;
        return new MouseEvent(
                type,
                x,
                y,
                x,
                y,
                button,
                1,
                shiftDown,
                false,
                altDown,
                shortcutDown,
                primaryDown,
                middleDown,
                secondaryDown,
                false,
                false,
                type != MouseEvent.MOUSE_DRAGGED,
                new PickResult(drawingCanvas, x, y)
        );
    }

}
