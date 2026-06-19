package de.andreas.cadas.ui;

import de.andreas.cadas.application.drawing.DraftingConstraints;
import de.andreas.cadas.application.drawing.DraftingService;
import de.andreas.cadas.application.drawing.DimensionLineLayoutService;
import de.andreas.cadas.application.drawing.DimensionStandard;
import de.andreas.cadas.application.drawing.EdgeResizeService;
import de.andreas.cadas.application.drawing.GuideSnapService;
import de.andreas.cadas.application.drawing.GuideSnapTargets;
import de.andreas.cadas.application.exchange.ExchangeFileNameService;
import de.andreas.cadas.application.history.UndoRedoStack;
import de.andreas.cadas.application.help.HelpContentService;
import de.andreas.cadas.application.drawing.OpeningPlacementService;
import de.andreas.cadas.application.drawing.QuarterTurnRotationService;
import de.andreas.cadas.application.drawing.SelectionQueryService;
import de.andreas.cadas.application.drawing.SelectionTranslationService;
import de.andreas.cadas.application.drawing.SnapService;
import de.andreas.cadas.application.drawing.WallEditingService;
import de.andreas.cadas.application.drawing.WallDimensionService;
import de.andreas.cadas.application.drawing.WallSnapService;
import de.andreas.cadas.application.drawing.WallEndpointSelection;
import de.andreas.cadas.application.dwg.DwgBlockDefinition;
import de.andreas.cadas.application.dwg.DwgConversionAvailability;
import de.andreas.cadas.application.dwg.DwgLibraryAnalysis;
import de.andreas.cadas.application.dwg.DwgLibraryAnalyzer;
import de.andreas.cadas.application.exchange.LevelExchangeService;
import de.andreas.cadas.application.exchange.ProjectExchangeService;
import de.andreas.cadas.application.layers.SurfaceCoveringPreset;
import de.andreas.cadas.application.layers.SurfaceCoveringPresetService;
import de.andreas.cadas.application.layers.DwgBlockCatalogService;
import de.andreas.cadas.application.layers.SurfaceLayerEffectService;
import de.andreas.cadas.application.layers.SurfaceLayerConsistencyService;
import de.andreas.cadas.application.layers.TileLayoutRequest;
import de.andreas.cadas.application.layers.TileLayoutService;
import de.andreas.cadas.application.layers.TilePlacement;
import de.andreas.cadas.application.layers.UserSurfaceCoveringPresetLibrary;
import de.andreas.cadas.application.layers.WallSurfaceSideService;
import de.andreas.cadas.application.layers.WallSurfaceTargetKey;
import de.andreas.cadas.application.objects.RoomObjectPreset;
import de.andreas.cadas.application.objects.RoomObjectPresetService;
import de.andreas.cadas.application.parts.DoorPreset;
import de.andreas.cadas.application.parts.PartLibraryImportService;
import de.andreas.cadas.application.parts.StairPreset;
import de.andreas.cadas.application.parts.StandardPartLibrary;
import de.andreas.cadas.application.parts.StandardPartLibraryService;
import de.andreas.cadas.application.parts.WindowPreset;
import de.andreas.cadas.application.reports.MarkdownHtmlRenderer;
import de.andreas.cadas.application.reports.ConstructionDrawingPdfService;
import de.andreas.cadas.application.reports.SurfaceMaterialListService;
import de.andreas.cadas.application.room.AutoRoomGenerationService;
import de.andreas.cadas.application.view.RenderableKind;
import de.andreas.cadas.application.view.SelectionKey;
import de.andreas.cadas.application.view.WallSurfaceOpeningService;
import de.andreas.cadas.application.view.WallSurfaceOpeningService.WallSurfaceInterval;
import de.andreas.cadas.application.view.WallSurfaceOpeningService.WallSurfaceRectangle;
import de.andreas.cadas.application.view.WallSurfacePlanGeometryService;
import de.andreas.cadas.application.view.WallSurfacePlanGeometryService.WallSurfacePlanPolygon;
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
import de.andreas.cadas.domain.model.RoomObject;
import de.andreas.cadas.domain.model.RoomObjectMountingMode;
import de.andreas.cadas.domain.model.SurfaceCutRestriction;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceLayoutMode;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.SlopedCeilingProfile;
import de.andreas.cadas.domain.model.SlopedCeilingSide;
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;
import de.andreas.cadas.infrastructure.dxf.DxfLevelExchangeService;
import de.andreas.cadas.infrastructure.dxf.DxfProjectExchangeService;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;

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
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.print.PrinterJob;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.embed.swing.SwingFXUtils;

public final class CadWorkbench extends BorderPane {

    private enum WorkspaceMode {
        TWO_D("2D"),
        THREE_D("3D"),
        INTERIOR("Innen");

        private final String label;

        WorkspaceMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final double BASE_PIXELS_PER_MILLIMETER = 0.10;
    private static final double RULER_SIZE = 32.0;
    private static final Length DEFAULT_GRID = Length.of(25, LengthUnit.CENTIMETER);
    private static final Length DEFAULT_WALL_THICKNESS = Length.of(17.5, LengthUnit.CENTIMETER);
    private static final Length DEFAULT_WALL_HEIGHT = Length.of(2.75, LengthUnit.METER);
    private static final Length DEFAULT_ROOM_HEIGHT = Length.of(2.60, LengthUnit.METER);
    private static final Length DEFAULT_FLOOR_THICKNESS = Length.of(18, LengthUnit.CENTIMETER);
    private static final Length DEFAULT_CEILING_THICKNESS = Length.of(1, LengthUnit.MILLIMETER);
    private static final Length DEFAULT_DOOR_WIDTH = Length.of(1.01, LengthUnit.METER);
    private static final Length DEFAULT_DOOR_HEIGHT = Length.of(2.01, LengthUnit.METER);
    private static final Length DEFAULT_WINDOW_WIDTH = Length.of(1.20, LengthUnit.METER);
    private static final Length DEFAULT_WINDOW_HEIGHT = Length.of(1.20, LengthUnit.METER);
    private static final Length DEFAULT_WINDOW_SILL = Length.of(90, LengthUnit.CENTIMETER);
    private static final Length DEFAULT_STAIR_HEIGHT = Length.of(2.80, LengthUnit.METER);
    private static final Length SNAP_TOLERANCE = Length.of(12, LengthUnit.CENTIMETER);
    private static final int LENGTH_INPUT_DECIMALS = 3;

    private final StandardPartLibrary partLibrary = new StandardPartLibraryService().load();
    private final PartLibraryImportService partLibraryImportService = new PartLibraryImportService();
    private final AutoRoomGenerationService autoRoomGenerationService = new AutoRoomGenerationService();
    private final DraftingService draftingService = new DraftingService();
    private final EdgeResizeService edgeResizeService = new EdgeResizeService();
    private final SnapService snapService = new SnapService();
    private final GuideSnapService guideSnapService = new GuideSnapService();
    private final WallSnapService wallSnapService = new WallSnapService();
    private final SelectionQueryService selectionQueryService = new SelectionQueryService();
    private final ExchangeFileNameService exchangeFileNameService = new ExchangeFileNameService();
    private final OpeningPlacementService openingPlacementService = new OpeningPlacementService();
    private final WallEditingService wallEditingService = new WallEditingService();
    private final WallDimensionService wallDimensionService = new WallDimensionService();
    private final DimensionLineLayoutService dimensionLineLayoutService = new DimensionLineLayoutService();
    private final QuarterTurnRotationService quarterTurnRotationService = new QuarterTurnRotationService();
    private final SelectionTranslationService selectionTranslationService = new SelectionTranslationService();
    private final LevelExchangeService levelExchangeService = new DxfLevelExchangeService();
    private final ProjectExchangeService projectExchangeService = new DxfProjectExchangeService();
    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();
    private final TileLayoutService tileLayoutService = new TileLayoutService();
    private final SurfaceLayerConsistencyService surfaceLayerConsistencyService = new SurfaceLayerConsistencyService();
    private final WallSurfaceSideService wallSurfaceSideService = new WallSurfaceSideService();
    private final WallSurfaceOpeningService wallSurfaceOpeningService = new WallSurfaceOpeningService();
    private final WallSurfacePlanGeometryService wallSurfacePlanGeometryService = new WallSurfacePlanGeometryService();
    private final GuideDistanceService guideDistanceService = new GuideDistanceService();
    private final PointerCursorService pointerCursorService = new PointerCursorService();
    private final TwoDZoomRange twoDZoomRange = new TwoDZoomRange();
    private final SurfaceCoveringPresetService surfaceCoveringPresetService = new SurfaceCoveringPresetService();
    private final UserSurfaceCoveringPresetLibrary userSurfacePresetLibrary = new UserSurfaceCoveringPresetLibrary();
    private final SurfaceMaterialListService surfaceMaterialListService = new SurfaceMaterialListService();
    private final ConstructionDrawingPdfService constructionDrawingPdfService = new ConstructionDrawingPdfService();
    private final HelpContentService helpContentService = new HelpContentService();
    private final MarkdownHtmlRenderer markdownHtmlRenderer = new MarkdownHtmlRenderer();
    private final DwgBlockCatalogService dwgBlockCatalogService = new DwgBlockCatalogService();
    private final RoomObjectPresetService roomObjectPresetService = new RoomObjectPresetService();
    private final DwgLibraryAnalyzer dwgLibraryAnalyzer = new DwgLibraryAnalyzer();
    private SurfaceType preferredRoomSurfaceType = SurfaceType.FLOOR;
    private final ProjectModel project = ProjectModel.withDefaultLevel("Neues Projekt", "Erdgeschoss");

    private final ObjectProperty<Level> activeLevel = new SimpleObjectProperty<>(project.primaryLevel());
    private final ObjectProperty<ViewOrientation> activeView = new SimpleObjectProperty<>(ViewOrientation.TOP);
    private final ObjectProperty<WorkspaceMode> activeWorkspaceMode = new SimpleObjectProperty<>(WorkspaceMode.TWO_D);
    private final BooleanProperty showGrid = new SimpleBooleanProperty(true);
    private final BooleanProperty snapToGrid = new SimpleBooleanProperty(true);
    private final BooleanProperty snapToEndpoints = new SimpleBooleanProperty(true);
    private final BooleanProperty showCompass = new SimpleBooleanProperty(true);
    private final BooleanProperty showDimensions = new SimpleBooleanProperty(true);
    private final BooleanProperty useIsoDimensions = new SimpleBooleanProperty(false);
    private final BooleanProperty showAreaVolume = new SimpleBooleanProperty(true);
    private final BooleanProperty showRoomObjects = new SimpleBooleanProperty(true);
    private final BooleanProperty showGuides = new SimpleBooleanProperty(true);
    private final BooleanProperty showGuideDistances = new SimpleBooleanProperty(true);
    private final BooleanProperty snapToGuides = new SimpleBooleanProperty(true);
    private final BooleanProperty snapToWalls = new SimpleBooleanProperty(true);

    private final Canvas drawingCanvas = new Canvas();
    private final Canvas horizontalRuler = new Canvas();
    private final Canvas verticalRuler = new Canvas();
    private final Pane drawingPane = new Pane(drawingCanvas);
    private final BorderPane drawingArea = new BorderPane();
    private final StackPane workspacePane = new StackPane();
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
    private final TextField endpointHeightField = new TextField("2,75");
    private final ComboBox<LengthUnit> endpointHeightUnit = new ComboBox<>();
    private final TextField roomNameField = new TextField("Raum");
    private final TextField roomHeightField = new TextField("2,60");
    private final ComboBox<LengthUnit> roomHeightUnit = new ComboBox<>();
    private final TextField floorThicknessField = new TextField("18");
    private final ComboBox<LengthUnit> floorThicknessUnit = new ComboBox<>();
    private final TextField ceilingThicknessField = new TextField("1");
    private final ComboBox<LengthUnit> ceilingThicknessUnit = new ComboBox<>();
    private final ComboBox<String> slopedCeilingModeSelector = new ComboBox<>();
    private final ComboBox<SlopedCeilingSide> slopedCeilingSideSelector = new ComboBox<>();
    private final TextField kneeWallHeightField = new TextField("1,00");
    private final ComboBox<LengthUnit> kneeWallHeightUnit = new ComboBox<>();
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
    private final ComboBox<RoomObjectPreset> roomObjectPresetSelector = new ComboBox<>();
    private final TextField stairHeightField = new TextField("2,80");
    private final ComboBox<LengthUnit> stairHeightUnit = new ComboBox<>();
    private final TextField stairStepsField = new TextField("16");
    private final TextField stairStartLandingField = new TextField("0");
    private final ComboBox<LengthUnit> stairStartLandingUnit = new ComboBox<>();
    private final TextField stairEndLandingField = new TextField("0");
    private final ComboBox<LengthUnit> stairEndLandingUnit = new ComboBox<>();
    private final ComboBox<SurfaceType> surfaceTypeSelector = new ComboBox<>();
    private final ComboBox<SurfaceCoveringPreset> surfacePresetSelector = new ComboBox<>();
    private final ListView<String> surfaceLayerList = new ListView<>();
    private final TextField surfaceLayerNameField = new TextField("Belag");
    private final TextField surfaceLayerThicknessField = new TextField("1,2");
    private final ComboBox<LengthUnit> surfaceLayerThicknessUnit = new ComboBox<>();
    private final TextField surfaceTileWidthField = new TextField("60");
    private final ComboBox<LengthUnit> surfaceTileWidthUnit = new ComboBox<>();
    private final TextField surfaceTileHeightField = new TextField("30");
    private final ComboBox<LengthUnit> surfaceTileHeightUnit = new ComboBox<>();
    private final ComboBox<SurfaceLayoutMode> surfaceLayoutModeSelector = new ComboBox<>();
    private final TextField surfaceLayoutOffsetField = new TextField("0");
    private final ComboBox<LengthUnit> surfaceLayoutOffsetUnit = new ComboBox<>();
    private final TextField surfaceMinimumOffsetField = new TextField("10");
    private final ComboBox<LengthUnit> surfaceMinimumOffsetUnit = new ComboBox<>();
    private final TextField surfaceMinimumEdgeWidthField = new TextField("8");
    private final ComboBox<LengthUnit> surfaceMinimumEdgeWidthUnit = new ComboBox<>();
    private final TextField surfaceMinimumStartEndMarginField = new TextField("8");
    private final ComboBox<LengthUnit> surfaceMinimumStartEndMarginUnit = new ComboBox<>();
    private final TextField surfaceJointWidthField = new TextField("2");
    private final ComboBox<LengthUnit> surfaceJointWidthUnit = new ComboBox<>();
    private final ComboBox<SurfaceCutRestriction> surfaceCutRestrictionSelector = new ComboBox<>();
    private final TextField dwgBlockNameField = new TextField();
    private final TextField dwgBlockSearchField = new TextField();
    private final ComboBox<DwgBlockDefinition> dwgBlockSelector = new ComboBox<>();
    private final ComboBox<RoomObjectMountingMode> dwgObjectFloorModeSelector = new ComboBox<>();
    private final Label dwgStatusLabel = new Label("Noch keine DWG-Bibliothek analysiert.");
    private final Label dwgBlockDetailLabel = new Label("Kein DWG-Block ausgewählt.");
    private final Canvas dwgPreviewCanvas = new Canvas(220, 150);
    private final Label surfaceLayerTargetLabel = new Label("Keine Fläche ausgewählt.");
    private final Label surfaceLayerSelectionHintLabel = new Label("Für Beläge zuerst eine passende Fläche auswählen.");
    private final Label surfaceLayerCoverageLabel = new Label("Keine Ebenen ausgewählt.");
    private final ComboBox<Level> levelSelector = new ComboBox<>();
    private final ComboBox<DrawingTool> toolSelector = new ComboBox<>();
    private final ObservableList<DoorPreset> availableDoorPresets = FXCollections.observableArrayList();
    private final ObservableList<WindowPreset> availableWindowPresets = FXCollections.observableArrayList();
    private final ObservableList<StairPreset> availableStairPresets = FXCollections.observableArrayList();
    private final ObservableList<RoomObjectPreset> availableRoomObjectPresets = FXCollections.observableArrayList();
    private final ObservableList<SurfaceCoveringPreset> availableSurfacePresets = FXCollections.observableArrayList();
    private final ObservableList<DwgBlockDefinition> availableDwgBlocks = FXCollections.observableArrayList();
    private final ThreeDViewport threeDViewport = new ThreeDViewport(this::handleThreeDSelection, this::switchToThreeDWorkspaceFromViewport);
    private final ViewProjectionService projectionService = new ViewProjectionService();
    private final ProjectedModelBoundsService projectedBoundsService = new ProjectedModelBoundsService();
    private final UndoRedoStack<WorkbenchSnapshot> history = new UndoRedoStack<>();
    private final VBox propertySections = new VBox(12.0);
    private final Label selectionSummaryLabel = new Label("Keine Auswahl");
    private final Button undoButton = new Button("Rückgängig");
    private final Button redoButton = new Button("Wiederherstellen");
    private final Button deleteSelectionButton = new Button("Auswahl löschen");
    private final Button clearSelectionButton = new Button("Auswahl aufheben");
    private final Button applySelectionPropertiesButton = new Button("Werte auf Auswahl anwenden");
    private final Button applyEndpointHeightButton = new Button("Eckhöhe anwenden");
    private final Button addSurfaceLayerButton = new Button("Ebene hinzufügen");
    private final Button updateSurfaceLayerButton = new Button("Ebene aktualisieren");
    private final Button removeSurfaceLayerButton = new Button("Ebene entfernen");
    private final Button toggleSurfaceLayerVisibilityButton = new Button("Sichtbarkeit umschalten");
    private final Button moveSurfaceLayerUpButton = new Button("Nach oben");
    private final Button moveSurfaceLayerDownButton = new Button("Nach unten");
    private final Button saveSurfacePresetButton = new Button("Speichern");
    private final Button addDwgBlockPresetButton = new Button("DWG-Block hinzufügen");
    private final Button refreshDwgLibraryButton = new Button("DWG prüfen");
    private final Button addDwgBlockAsSurfaceButton = new Button("Als Belag");
    private final Button addDwgBlockAsObjectButton = new Button("Als Objekt");
    private final ContextMenu selectionContextMenu = new ContextMenu();
    private final Label cadLibrarySummaryLabel = new Label("Keine externen CAD-Bibliotheken registriert.");

    private final Label zoomLabel = new Label();
    private final Label cursorLabel = new Label();
    private final Label draftLabel = new Label();
    private final Label viewLabel = new Label();

    private final ObservableList<GuideLine> guideLines = FXCollections.observableArrayList();
    private final ObservableList<Path> cadLibraryReferences = FXCollections.observableArrayList();
    private final Map<Path, DwgLibraryAnalysis> dwgAnalysesByPath = new LinkedHashMap<>();
    private final LinkedHashSet<SelectionKey> selectedSelections = new LinkedHashSet<>();

    private double zoom = 1.0;
    private double offsetX = 240.0;
    private double offsetY = 160.0;
    private double panStartX;
    private double panStartY;
    private double panOriginX;
    private double panOriginY;
    private boolean panning;
    private boolean panningMoved;
    private SelectionKey pendingContextSelection;
    private boolean updatingLengthInput;
    private PlanPoint draftStart;
    private PlanSegment previewSegment;
    private PlanPoint lastCursor = new PlanPoint(0.0, 0.0);
    private WallEndpointSelection selectedEndpointGroup;
    private final ObjectProperty<SelectionKey> selectedSelection = new SimpleObjectProperty<>();
    private GuideOrientation pendingGuideOrientation;
    private double pendingGuideWorldMillimeters;
    private boolean threeDDirty = true;
    private boolean keepViewportOrbitPoseOnNextThreeDActivation;
    private boolean historyCapturedForDrag;
    private PlanPoint selectionDragAnchor;
    private List<Wall> selectionDragBaseWalls = List.of();
    private List<Staircase> selectionDragBaseStaircases = List.of();
    private List<RoomObject> selectionDragBaseRoomObjects = List.of();
    private UUID openingDragId;
    private PlanSegment openingDragWallAxis;
    private double openingDragWidth;
    private double openingDragOffsetDelta;
    private EdgeResizeService.EdgeHandle activeEdgeHandle;
    private List<Wall> edgeResizeBaseWalls = List.of();
    private List<Door> edgeResizeBaseDoors = List.of();
    private List<WindowElement> edgeResizeBaseWindows = List.of();
    private double lastMouseX;
    private double lastMouseY;
    private boolean altPressed;
    private boolean spacePressed;

    public CadWorkbench() {
        setPadding(new Insets(12));
        setStyle("-fx-background-color: linear-gradient(to bottom, #f6f1e8, #ece5d8);");

        configureControls();
        configureLayout();
        configureCanvas();
        threeDViewport.syncLevels(availableLevels, activeLevel.get().name());
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
                newScene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), this::showHelpWindow);
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::updateModifierState);
                newScene.addEventFilter(KeyEvent.KEY_RELEASED, this::updateModifierState);
            }
        });
        updatePropertySectionVisibility();
        updateActionButtons();
        updateWorkspaceMode();
        fitCurrentViewToContent();
        updateStatus();
        render();
    }

    private void configureControls() {
        initializeUnitSelectors();
        initializeSlopedCeilingControls();
        levelSelector.setItems(availableLevels);
        levelSelector.setValue(activeLevel.get());
        toolSelector.getItems().addAll(DrawingTool.values());
        toolSelector.setValue(DrawingTool.WALL);
        initializePresetSelectors();
        initializeSurfaceLayerControls();
        initializeDwgLibraryControls();
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
        registerRenderListener(useIsoDimensions);
        registerRenderListener(showAreaVolume);
        registerRenderListener(showGuides);
        registerRenderListener(showGuideDistances);
        registerRenderListener(snapToGuides);
        registerRenderListener(snapToWalls);
        showRoomObjects.addListener((ignored, oldValue, newValue) -> {
            threeDViewport.setRoomObjectsVisible(newValue);
            markThreeDDirty();
            render();
        });
        activeView.addListener((ignored, oldValue, newValue) -> {
            fitCurrentViewToContent();
            render();
        });
        activeWorkspaceMode.addListener((ignored, oldValue, newValue) -> {
            updateWorkspaceMode();
            if (newValue == WorkspaceMode.THREE_D) {
                if (keepViewportOrbitPoseOnNextThreeDActivation) {
                    keepViewportOrbitPoseOnNextThreeDActivation = false;
                } else {
                    threeDViewport.activateOrbitView();
                }
                refreshThreeDIfNeeded();
            } else if (newValue == WorkspaceMode.INTERIOR) {
                refreshThreeDIfNeeded();
            }
            render();
        });
        toolSelector.valueProperty().addListener((ignored, oldValue, newValue) -> {
            updatePropertySectionVisibility();
            updateActionButtons();
            updateStatus();
            updateMouseCursor();
            render();
        });
        configureActionButtons();
        registerConfiguredDwgLibraries();
    }

    private void configureLayout() {
        MenuBar menuBar = buildMenuBar();
        ToolBar settingsBar = buildSettingsBar();
        HBox viewBar = buildViewBar();
        VBox topArea = new VBox(8.0, menuBar, settingsBar, viewBar);
        topArea.setPadding(new Insets(0, 0, 12, 0));
        setTop(topArea);

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
        workspacePane.getChildren().setAll(drawingArea, threeDViewport);
        SplitPane splitPane = new SplitPane(buildPropertyPane(), workspacePane);
        splitPane.setDividerPositions(0.22);
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

        CheckBox snapRasterBox = new CheckBox("Raster-Snap");
        snapRasterBox.selectedProperty().bindBidirectional(snapToGrid);
        applyTooltip(snapRasterBox, "Aktiviert das magnetische Einrasten auf das konfigurierte Raster.");

        CheckBox snapPointsBox = new CheckBox("Punkt-Snap");
        snapPointsBox.selectedProperty().bindBidirectional(snapToEndpoints);
        applyTooltip(snapPointsBox, "Aktiviert das magnetische Einrasten auf vorhandene Linien-Endpunkte.");

        CheckBox guideDistancesBox = new CheckBox("Hilfslinienabstände");
        guideDistancesBox.selectedProperty().bindBidirectional(showGuideDistances);
        applyTooltip(guideDistancesBox, "Zeigt beim Herausziehen einer Hilfslinie die Abstände zu allen vorhandenen parallelen Hilfslinien an.");

        CheckBox snapGuidesBox = new CheckBox("Hilfslinien-Snap");
        snapGuidesBox.selectedProperty().bindBidirectional(snapToGuides);
        applyTooltip(snapGuidesBox, "Lässt Wände, Türen und Fenster mit Kanten oder Mittellinie magnetisch an sichtbaren Hilfslinien einrasten.");

        CheckBox snapWallsBox = new CheckBox("Wand-Snap");
        snapWallsBox.selectedProperty().bindBidirectional(snapToWalls);
        applyTooltip(snapWallsBox, "Lässt neue oder verschobene Wände an Achsen, Außenkanten und Endkanten anderer Wände einrasten.");

        CheckBox dimensionsBox = new CheckBox("Bemaßung");
        dimensionsBox.selectedProperty().bindBidirectional(showDimensions);
        applyTooltip(dimensionsBox, "Blendet die Längenbeschriftung der gezeichneten Wände ein oder aus.");

        CheckBox isoDimensionsBox = new CheckBox("ISO 7519");
        isoDimensionsBox.selectedProperty().bindBidirectional(useIsoDimensions);
        applyTooltip(isoDimensionsBox, "Schaltet die sichtbare Bemaßung zwischen der bisherigen Beschriftung und dem Darstellungsprofil DIN EN ISO 7519 | 2025-01 mit Maß-, Maßhilfs- und Begrenzungslinien um.");

        CheckBox objectsBox = new CheckBox("Objekte");
        objectsBox.selectedProperty().bindBidirectional(showRoomObjects);
        applyTooltip(objectsBox, "Blendet platzierte Raumobjekte gemeinsam in 2D, Innenansicht und 3D ein oder aus.");

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
                new Separator(Orientation.VERTICAL),
                undoButton,
                redoButton,
                deleteSelectionButton,
                clearSelectionButton,
                new Separator(Orientation.VERTICAL),
                rasterBox,
                snapRasterBox,
                snapPointsBox,
                guideDistancesBox,
                snapGuidesBox,
                snapWallsBox,
                new Separator(Orientation.VERTICAL),
                dimensionsBox,
                isoDimensionsBox,
                objectsBox
        );
    }

    private HBox buildViewBar() {
        HBox box = new HBox(8.0);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(new Label("Arbeitsbereich:"));
        box.getChildren().add(workspaceModeButton(WorkspaceMode.TWO_D));
        box.getChildren().add(workspaceModeButton(WorkspaceMode.THREE_D));
        box.getChildren().add(workspaceModeButton(WorkspaceMode.INTERIOR));
        box.getChildren().add(new Separator(Orientation.VERTICAL));
        box.getChildren().add(new Label("2D-Ansichten:"));
        box.getChildren().add(viewButton("⤒ Oben", () -> activeView.set(ViewOrientation.TOP), "Schaltet auf die feste Draufsicht um."));
        box.getChildren().add(viewButton("⤓ Unten", () -> activeView.set(ViewOrientation.BOTTOM), "Schaltet auf die feste Untersicht um."));
        box.getChildren().add(viewButton("↑", this::rotateViewUp, "Dreht das Modell aus der aktuellen 2D-Ansicht nach oben."));
        box.getChildren().add(viewButton("↓", this::rotateViewDown, "Dreht das Modell aus der aktuellen 2D-Ansicht nach unten."));
        box.getChildren().add(viewButton("←", this::rotateViewLeft, "Dreht das Modell aus der aktuellen 2D-Ansicht nach links."));
        box.getChildren().add(viewButton("→", this::rotateViewRight, "Dreht das Modell aus der aktuellen 2D-Ansicht nach rechts."));
        box.getChildren().add(viewButton(
                "2D zentrieren",
                this::resetTwoDView,
                "Setzt Zoom und Verschiebung der Zeichenfläche auf die Startansicht zurück."
        ));
        return box;
    }

    private Button viewButton(String label, Runnable action, String tooltipText) {
        Button button = new Button(label);
        button.setOnAction(event -> action.run());
        button.setStyle("-fx-background-radius: 999; -fx-padding: 8 14 8 14;");
        applyTooltip(button, tooltipText);
        return button;
    }

    private void rotateViewLeft() {
        activeView.set(switch (activeView.get()) {
            case TOP -> ViewOrientation.WEST;
            case BOTTOM -> ViewOrientation.WEST;
            case NORTH -> ViewOrientation.WEST;
            case SOUTH -> ViewOrientation.EAST;
            case EAST -> ViewOrientation.NORTH;
            case WEST -> ViewOrientation.SOUTH;
        });
    }

    private void rotateViewRight() {
        activeView.set(switch (activeView.get()) {
            case TOP -> ViewOrientation.EAST;
            case BOTTOM -> ViewOrientation.EAST;
            case NORTH -> ViewOrientation.EAST;
            case SOUTH -> ViewOrientation.WEST;
            case EAST -> ViewOrientation.SOUTH;
            case WEST -> ViewOrientation.NORTH;
        });
    }

    private void rotateViewUp() {
        activeView.set(switch (activeView.get()) {
            case TOP -> ViewOrientation.NORTH;
            case BOTTOM -> ViewOrientation.SOUTH;
            case NORTH, SOUTH, EAST, WEST -> ViewOrientation.TOP;
        });
    }

    private void rotateViewDown() {
        activeView.set(switch (activeView.get()) {
            case TOP -> ViewOrientation.SOUTH;
            case BOTTOM -> ViewOrientation.NORTH;
            case NORTH, SOUTH, EAST, WEST -> ViewOrientation.BOTTOM;
        });
    }

    private Button workspaceModeButton(WorkspaceMode workspaceMode) {
        Button button = new Button(workspaceMode.label());
        button.setOnAction(event -> selectWorkspaceMode(workspaceMode, true));
        button.setStyle(workspaceModeButtonStyle(workspaceMode == activeWorkspaceMode.get()));
        activeWorkspaceMode.addListener((ignored, oldValue, newValue) ->
                button.setStyle(workspaceModeButtonStyle(workspaceMode == newValue)));
        applyTooltip(button, switch (workspaceMode) {
            case TWO_D -> "Zeigt die 2D-Zeichenfläche im großen Mittelbereich an.";
            case THREE_D -> "Zeigt die 3D-Orbitansicht im großen Mittelbereich an und spart Platz gegenüber der Parallelansicht.";
            case INTERIOR -> "Öffnet die 3D-Innenansicht im aktuell ausgewählten Raum oder im ersten Raum der aktiven Etage.";
        });
        return button;
    }

    private String workspaceModeButtonStyle(boolean active) {
        return active
                ? "-fx-background-color: #4b6a88; -fx-text-fill: white; -fx-background-radius: 999; -fx-padding: 8 16 8 16;"
                : "-fx-background-radius: 999; -fx-padding: 8 16 8 16;";
    }

    private void settingsBarStyling() {
        gridField.setPrefColumnCount(6);
        lengthField.setPrefColumnCount(6);
        angleField.setPrefColumnCount(6);
        northAngleField.setPrefColumnCount(6);
        wallThicknessField.setPrefColumnCount(6);
        wallHeightField.setPrefColumnCount(6);
        roomNameField.setPrefColumnCount(8);
        roomHeightField.setPrefColumnCount(6);
        floorThicknessField.setPrefColumnCount(6);
        ceilingThicknessField.setPrefColumnCount(6);
        endpointHeightField.setPrefColumnCount(6);
        slopedCeilingModeSelector.setPrefWidth(160);
        slopedCeilingSideSelector.setPrefWidth(160);
        kneeWallHeightField.setPrefColumnCount(6);
        doorWidthField.setPrefColumnCount(6);
        doorHeightField.setPrefColumnCount(6);
        thresholdField.setPrefColumnCount(6);
        windowWidthField.setPrefColumnCount(6);
        windowHeightField.setPrefColumnCount(6);
        sillHeightField.setPrefColumnCount(6);
        stairHeightField.setPrefColumnCount(6);
        stairStepsField.setPrefColumnCount(6);
        surfaceLayerThicknessField.setPrefColumnCount(6);
        surfaceTileWidthField.setPrefColumnCount(6);
        surfaceTileHeightField.setPrefColumnCount(6);
        surfaceLayoutOffsetField.setPrefColumnCount(6);
        surfaceMinimumOffsetField.setPrefColumnCount(6);
        surfaceMinimumEdgeWidthField.setPrefColumnCount(6);
        surfaceMinimumStartEndMarginField.setPrefColumnCount(6);
        surfaceJointWidthField.setPrefColumnCount(6);
        levelSelector.setPrefWidth(180);
        toolSelector.setPrefWidth(140);
        doorPresetSelector.setPrefWidth(190);
        windowPresetSelector.setPrefWidth(210);
        stairPresetSelector.setPrefWidth(190);
        roomObjectPresetSelector.setPrefWidth(210);
        dwgBlockNameField.setPrefColumnCount(14);
    }

    private void updateWorkspaceMode() {
        boolean showTwoD = activeWorkspaceMode.get() == WorkspaceMode.TWO_D;
        drawingArea.setVisible(showTwoD);
        drawingArea.setManaged(showTwoD);
        threeDViewport.setVisible(!showTwoD);
        threeDViewport.setManaged(!showTwoD);
    }

    private void switchToThreeDWorkspaceFromViewport() {
        if (activeWorkspaceMode.get() == WorkspaceMode.THREE_D) {
            return;
        }
        keepViewportOrbitPoseOnNextThreeDActivation = true;
        activeWorkspaceMode.set(WorkspaceMode.THREE_D);
    }

    private boolean activateInteriorViewForCurrentRoom() {
        Optional<Room> targetRoom = selectedSurfaceRoom()
                .or(this::selectedRoom)
                .or(() -> activeLevel.get().rooms().stream().findFirst());
        if (targetRoom.isEmpty()) {
            draftLabel.setText("Innenansicht braucht einen Raum auf der aktiven Etage.");
            return false;
        }
        threeDViewport.activateInteriorView(project, activeLevel.get(), targetRoom.get());
        return true;
    }

    private void selectWorkspaceMode(WorkspaceMode workspaceMode, boolean showErrorDialog) {
        if (workspaceMode == WorkspaceMode.INTERIOR && !activateInteriorViewForCurrentRoom()) {
            if (showErrorDialog) {
                showInteriorViewUnavailableError();
            }
            return;
        }
        activeWorkspaceMode.set(workspaceMode);
        updateWorkspaceMode();
        refreshThreeDIfNeeded();
    }

    private void showInteriorViewUnavailableError() {
        Alert alert = new Alert(
                Alert.AlertType.WARNING,
                "Auf der aktiven Etage ist kein Raum vorhanden. Schließe zuerst einen Wandzug, damit CADas einen Raum ableiten kann.",
                ButtonType.OK
        );
        alert.setTitle("Innenansicht nicht verfügbar");
        alert.setHeaderText("Für die Innenansicht wird ein Raum benötigt.");
        Window window = getScene() != null ? getScene().getWindow() : null;
        if (window != null) {
            alert.initOwner(window);
        }
        alert.showAndWait();
    }

    private MenuBar buildMenuBar() {
        Menu dateiMenu = new Menu("Datei");
        dateiMenu.getItems().addAll(
                menuItem("Etage hinzufügen", this::createLevel, shortcutKey(KeyCode.N)),
                menuItem("Projekt leeren", this::clearProject, shortcutKey(KeyCode.L)),
                menuItem("Gebäude als DXF exportieren", this::exportProjectAsDxf, shortcutShiftKey(KeyCode.E)),
                menuItem("Gebäude aus DXF importieren", this::importProjectFromDxf, shortcutShiftKey(KeyCode.I)),
                menuItem("Bauzeichnung als PDF exportieren", this::exportConstructionDrawingPdf, shortcutShiftKey(KeyCode.P)),
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
        ansichtMenu.getItems().addAll(
                menuItem("2D-Arbeitsbereich", () -> selectWorkspaceMode(WorkspaceMode.TWO_D, true), null),
                menuItem("3D-Arbeitsbereich", () -> selectWorkspaceMode(WorkspaceMode.THREE_D, true), null),
                menuItem("3D-Innenansicht", () -> selectWorkspaceMode(WorkspaceMode.INTERIOR, true), null),
                new SeparatorMenuItem()
        );
        for (ViewOrientation viewOrientation : ViewOrientation.values()) {
            ansichtMenu.getItems().add(menuItem(
                    "Zu " + viewOrientation.label(),
                    () -> activeView.set(viewOrientation),
                    null
            ));
        }
        ansichtMenu.getItems().addAll(
                menuItem("2D-Ansicht zentrieren", this::resetTwoDView, shortcutKey(KeyCode.DIGIT0)),
                menuItem("3D-Ansicht zentrieren", threeDViewport::centerCurrentView, shortcutShiftKey(KeyCode.DIGIT0))
        );

        Menu werkzeugMenu = new Menu("Werkzeuge");
        werkzeugMenu.getItems().addAll(
                toolMenuItem(DrawingTool.EDIT, KeyCode.E),
                toolMenuItem(DrawingTool.WALL, KeyCode.W),
                toolMenuItem(DrawingTool.STAIR, KeyCode.T),
                toolMenuItem(DrawingTool.DOOR, KeyCode.D),
                toolMenuItem(DrawingTool.WINDOW, KeyCode.F),
                toolMenuItem(DrawingTool.OBJECT, KeyCode.O),
                menuItem("Ausgewählte Bauteile 90° rechts drehen", this::rotateSelectedComponentsClockwise, shortcutShiftKey(KeyCode.RIGHT)),
                menuItem("Ausgewählte Bauteile 90° links drehen", this::rotateSelectedComponentsCounterClockwise, shortcutShiftKey(KeyCode.LEFT))
        );

        Menu optionenMenu = new Menu("Optionen");
        optionenMenu.getItems().addAll(
                checkMenuItem("Raster anzeigen", showGrid),
                checkMenuItem("Auf Raster einrasten", snapToGrid),
                checkMenuItem("Auf Punkte einrasten", snapToEndpoints),
                checkMenuItem("Hilfslinien anzeigen", showGuides),
                checkMenuItem("Hilfslinienabstände anzeigen", showGuideDistances),
                checkMenuItem("An Hilfslinien einrasten", snapToGuides),
                checkMenuItem("An anderen Wänden einrasten", snapToWalls),
                checkMenuItem("Bemaßung anzeigen", showDimensions),
                checkMenuItem("Bemaßung nach DIN EN ISO 7519 | 2025-01", useIsoDimensions),
                checkMenuItem("Objekte anzeigen", showRoomObjects),
                checkMenuItem("Fläche und Volumen anzeigen", showAreaVolume),
                checkMenuItem("Nordpfeil anzeigen", showCompass)
        );

        Menu berichteMenu = new Menu("Berichte");
        berichteMenu.getItems().addAll(
                menuItem("Materialliste Beläge anzeigen", this::showSurfaceMaterialReportWindow, null),
                menuItem("Materialliste Beläge als Markdown exportieren", this::exportSurfaceMaterialReportMarkdown, null)
        );

        Menu hilfeMenu = new Menu("Hilfe");
        hilfeMenu.getItems().add(menuItem(
                "Hilfe und Keymap",
                this::showHelpWindow,
                new KeyCodeCombination(KeyCode.F1)
        ));

        MenuBar menuBar = new MenuBar(dateiMenu, bearbeitenMenu, ansichtMenu, werkzeugMenu, optionenMenu, berichteMenu, hilfeMenu);
        applyTooltip(menuBar, "Bietet Datei-, Bearbeitungs-, Ansichts- und Werkzeugfunktionen mit passenden Tastaturkürzeln an.");
        return menuBar;
    }

    private ScrollPane buildPropertyPane() {
        selectionSummaryLabel.setWrapText(true);
        selectionSummaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        surfaceLayerTargetLabel.setWrapText(true);
        surfaceLayerTargetLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #5c5146;");
        surfaceLayerSelectionHintLabel.setWrapText(true);
        surfaceLayerSelectionHintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b6258;");
        surfaceLayerCoverageLabel.setWrapText(true);
        surfaceLayerCoverageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        cadLibrarySummaryLabel.setWrapText(true);
        cadLibrarySummaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        dwgStatusLabel.setWrapText(true);
        dwgStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        dwgBlockDetailLabel.setWrapText(true);
        dwgBlockDetailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
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
                        propertyRow("Wandhöhe", wallHeightField, wallHeightUnit),
                        propertyRow("Eckhöhe", endpointHeightField, endpointHeightUnit),
                        applyEndpointHeightButton
                ),
                createPropertySection(
                        "Raum",
                        propertyRow("Name", roomNameField),
                        propertyRow("Raumhöhe", roomHeightField, roomHeightUnit),
                        propertyRow("Boden", floorThicknessField, floorThicknessUnit),
                        propertyRow("Decke", ceilingThicknessField, ceilingThicknessUnit),
                        propertyRow("Dachschräge", slopedCeilingModeSelector),
                        propertyRow("Niedrige Seite", slopedCeilingSideSelector),
                        propertyRow("Sockelhöhe", kneeWallHeightField, kneeWallHeightUnit)
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
                        propertyRow("Stufen inkl. Absätze", stairStepsField),
                        propertyRow("Absatz Anfang", stairStartLandingField, stairStartLandingUnit),
                        propertyRow("Absatz Ende", stairEndLandingField, stairEndLandingUnit)
                ),
                createPropertySection(
                        "Objekt",
                        propertyRow("Preset", roomObjectPresetSelector)
                ),
                createPropertySection(
                        "Ebenen",
                        surfaceLayerTargetLabel,
                        surfaceLayerSelectionHintLabel,
                        propertyRow("Belagstyp", surfaceTypeSelector),
                        propertyRow("Preset", surfacePresetSelector),
                        surfaceLayerList,
                        surfaceLayerCoverageLabel,
                        new Separator(),
                        propertyRow("Name", surfaceLayerNameField, saveSurfacePresetButton),
                        propertyRow("Dicke", surfaceLayerThicknessField, surfaceLayerThicknessUnit),
                        propertyRow("Modulbreite", surfaceTileWidthField, surfaceTileWidthUnit),
                        propertyRow("Modulhöhe", surfaceTileHeightField, surfaceTileHeightUnit),
                        propertyRow("Versatzmodus", surfaceLayoutModeSelector),
                        propertyRow("Versatz", surfaceLayoutOffsetField, surfaceLayoutOffsetUnit),
                        propertyRow("Mindestversatz", surfaceMinimumOffsetField, surfaceMinimumOffsetUnit),
                        propertyRow("Mindestrand links/rechts", surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit),
                        propertyRow("Mindestbreite Anfang/Ende", surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit),
                        propertyRow("Fugenbreite", surfaceJointWidthField, surfaceJointWidthUnit),
                        propertyRow("Schnittbeschränkung", surfaceCutRestrictionSelector),
                        propertyRow("DWG-Block", dwgBlockNameField),
                        new HBox(6.0, addSurfaceLayerButton, updateSurfaceLayerButton),
                        new HBox(6.0, removeSurfaceLayerButton, toggleSurfaceLayerVisibilityButton),
                        new HBox(6.0, moveSurfaceLayerUpButton, moveSurfaceLayerDownButton),
                        addDwgBlockPresetButton
                ),
                createPropertySection(
                        "CAD-Bibliotheken",
                        cadLibrarySummaryLabel,
                        dwgStatusLabel,
                        propertyRow("DWG-Suche", dwgBlockSearchField),
                        propertyRow("DWG-Block", dwgBlockSelector),
                        dwgPreviewCanvas,
                        dwgBlockDetailLabel,
                        propertyRow("Objektnutzung", dwgObjectFloorModeSelector),
                        new HBox(6.0, refreshDwgLibraryButton, addDwgBlockAsSurfaceButton, addDwgBlockAsObjectButton)
                )
        );
        propertySections.setPadding(new Insets(4, 0, 4, 0));

        VBox container = new VBox(10.0, new Label("Eigenschaften"), propertySections);
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
        applyEndpointHeightButton.setOnAction(event -> applyEndpointHeightToSelection());
        addSurfaceLayerButton.setOnAction(event -> addSurfaceLayer());
        updateSurfaceLayerButton.setOnAction(event -> updateSurfaceLayer());
        removeSurfaceLayerButton.setOnAction(event -> removeSurfaceLayer());
        toggleSurfaceLayerVisibilityButton.setOnAction(event -> toggleSurfaceLayerVisibility());
        moveSurfaceLayerUpButton.setOnAction(event -> moveSurfaceLayer(-1));
        moveSurfaceLayerDownButton.setOnAction(event -> moveSurfaceLayer(1));
        saveSurfacePresetButton.setOnAction(event -> saveCurrentSurfacePreset());
        addDwgBlockPresetButton.setOnAction(event -> addDwgBlockPreset());
        refreshDwgLibraryButton.setOnAction(event -> refreshCurrentDwgLibraryAnalysis());
        addDwgBlockAsSurfaceButton.setOnAction(event -> addSelectedDwgBlockAsSurfacePreset());
        addDwgBlockAsObjectButton.setOnAction(event -> addSelectedDwgBlockAsObjectPreset());
        rebuildSelectionContextMenu();
        applyTooltip(undoButton, "Stellt den letzten fachlichen Bearbeitungsschritt des Projekts wieder her.");
        applyTooltip(redoButton, "Stellt einen zuvor rückgängig gemachten Bearbeitungsschritt erneut her.");
        applyTooltip(deleteSelectionButton, "Löscht das aktuell ausgewählte Bauteil aus der aktiven Etage.");
        applyTooltip(clearSelectionButton, "Hebt die aktuelle Auswahl auf und entfernt die Hervorhebung in 2D und 3D.");
        applyTooltip(applySelectionPropertiesButton, "Übernimmt die aktuell sichtbaren Eingabewerte auf alle passenden, ausgewählten Bauteile.");
        applyTooltip(applyEndpointHeightButton, "Übernimmt die eingetragene Höhe auf den aktuell ausgewählten Wand-Endpunkt und aktualisiert daraus die angrenzenden Räume.");
        applyTooltip(addSurfaceLayerButton, "Legt auf der aktuell ausgewählten Wand- oder Raumfläche eine neue Ebene mit den eingetragenen Maßen an.");
        applyTooltip(updateSurfaceLayerButton, "Übernimmt die aktuellen Ebenenwerte auf den in der Liste markierten Belag.");
        applyTooltip(removeSurfaceLayerButton, "Entfernt den in der Liste markierten Belag von der aktuell ausgewählten Fläche.");
        applyTooltip(toggleSurfaceLayerVisibilityButton, "Schaltet die Sichtbarkeit des markierten Belags um und passt Raumwirkung sowie 3D-Darstellung direkt an.");
        applyTooltip(moveSurfaceLayerUpButton, "Verschiebt den markierten Belag in der Stapelreihenfolge nach oben.");
        applyTooltip(moveSurfaceLayerDownButton, "Verschiebt den markierten Belag in der Stapelreihenfolge nach unten.");
        applyTooltip(saveSurfacePresetButton, "Speichert die aktuell eingetragenen Belagswerte als eigenes Preset unter `~/.config/CADas/Belag`, fragt vor dem Überschreiben nach und fügt das Preset der Auswahl hinzu.");
        applyTooltip(addDwgBlockPresetButton, "Registriert den manuell eingetragenen Blocknamen aus der aktuell ausgewählten DWG-Bibliothek als Belags-Preset. Wenn die DWG analysiert wurde, werden echte Blockmaße übernommen.");
        applyTooltip(refreshDwgLibraryButton, "Analysiert die aktuell geladene DWG-Bibliothek erneut über einen externen Konverter wie `dwg2dxf` oder `dwgread`.");
        applyTooltip(addDwgBlockAsSurfaceButton, "Übernimmt den ausgewählten DWG-Block mit echten Blockmaßen als Belags-Preset.");
        applyTooltip(addDwgBlockAsObjectButton, "Übernimmt den ausgewählten DWG-Block mit echtem Footprint als Objekt-Preset.");
        applyTooltip(cadLibrarySummaryLabel, "Listet registrierte externe CAD-Bibliotheken wie `.dwg` oder `.cadasparts` auf.");
        applyTooltip(dwgStatusLabel, "Zeigt, welcher externe DWG-Konverter gefunden wurde und ob die letzte Analyse erfolgreich war.");
        applyTooltip(dwgBlockSearchField, "Filtert die analysierten DWG-Blöcke nach Blockname, Layer oder Dateiname.");
        applyTooltip(dwgBlockSelector, "Wählt einen aus der DWG-Geometrie analysierten Block aus, der als Belag oder Objekt übernommen werden kann.");
        applyTooltip(dwgPreviewCanvas, "Zeigt eine maßstäbliche Draufsicht-Vorschau der aus dem DWG-Block abgeleiteten 2D-Grenzen.");
        applyTooltip(dwgBlockDetailLabel, "Zeigt Maße, Ursprung, Layer, Handles, Einheiten und Hinweise zum ausgewählten DWG-Block.");
        applyTooltip(dwgObjectFloorModeSelector, "Legt fest, ob das aus dem DWG-Block erzeugte Objekt auf dem Bodenbelag steht, den Bodenbelag ausschneidet oder wandmontiert ohne Bodenausschnitt geführt wird.");
    }

    private void updatePropertySectionVisibility() {
        for (int index = 0; index < propertySections.getChildren().size(); index++) {
            Node node = propertySections.getChildren().get(index);
            boolean visible = switch (index) {
                case 0, 1 -> true;
                case 2 -> shouldShowSection(DrawingTool.WALL, RenderableKind.WALL);
                case 3 -> shouldShowRoomSection();
                case 4 -> shouldShowSection(DrawingTool.DOOR, RenderableKind.DOOR);
                case 5 -> shouldShowSection(DrawingTool.WINDOW, RenderableKind.WINDOW);
                case 6 -> shouldShowSection(DrawingTool.STAIR, RenderableKind.STAIR);
                case 7 -> shouldShowSection(DrawingTool.OBJECT, RenderableKind.ROOM_OBJECT);
                case 8 -> shouldShowLayerSection();
                default -> true;
            };
            node.setVisible(visible);
            node.setManaged(visible);
        }
        selectionSummaryLabel.setText(selectionSummary());
        refreshSurfaceTypeSelector();
        refreshSurfaceLayerSection();
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

    private boolean shouldShowRoomSection() {
        if (currentTool() == DrawingTool.WALL) {
            return true;
        }
        if (selectedSelection.get() == null) {
            return false;
        }
        return selectedSelection.get().kind() == RenderableKind.ROOM_VOLUME
                || selectedSelection.get().kind() == RenderableKind.ROOM_FLOOR
                || selectedSelection.get().kind() == RenderableKind.ROOM_CEILING;
    }

    private boolean shouldShowLayerSection() {
        if (selectedSelection.get() == null) {
            return false;
        }
        return selectedSelection.get().kind() == RenderableKind.WALL
                || selectedSelection.get().kind() == RenderableKind.ROOM_VOLUME
                || selectedSelection.get().kind() == RenderableKind.ROOM_FLOOR
                || selectedSelection.get().kind() == RenderableKind.ROOM_CEILING;
    }

    private String selectionSummary() {
        if (selectedEndpointGroup != null) {
            return "Ausgewählt: gemeinsame Wandecke auf Etage `" + activeLevel.get().name()
                    + "`. Die Eckhöhe wirkt auf alle verbundenen Wandenden und leitet daraus die Raumdecke neu ab.";
        }
        if (selectedSelections.isEmpty()) {
            return "Keine Auswahl. Wähle ein Bauteil im Werkzeug `Bearbeiten` aus oder nutze direkt die Werkzeuge in der Zeichenfläche.";
        }
        if (selectedSelections.size() > 1) {
            return selectedSelections.size() + " Bauteile ausgewählt. Änderungen über die sichtbaren Eigenschaften werden auf passende Auswahlen gemeinsam angewendet.";
        }
        return "Ausgewählt: " + selectionLabel(selectedSelection.get()) + " auf Etage `" + selectedSelection.get().levelName() + "`.";
    }

    private String selectionLabel(SelectionKey selection) {
        return switch (selection.kind()) {
            case WALL -> "Wand";
            case ROOM_VOLUME, ROOM_FLOOR, ROOM_CEILING -> "automatisch abgeleiteter Raum";
            case DOOR -> "Tür";
            case WINDOW -> "Fenster";
            case STAIR -> "Treppe";
            case ROOM_OBJECT -> "Objekt";
            default -> selection.kind().name();
        };
    }

    private void updateActionButtons() {
        undoButton.setDisable(!history.canUndo());
        redoButton.setDisable(!history.canRedo());
        boolean hasSelection = !selectedSelections.isEmpty();
        boolean hasDeletableSelection = selectedSelections.stream().anyMatch(selection -> selection.kind() != RenderableKind.ROOM_VOLUME
                && selection.kind() != RenderableKind.ROOM_FLOOR
                && selection.kind() != RenderableKind.ROOM_CEILING);
        deleteSelectionButton.setDisable(!hasDeletableSelection);
        clearSelectionButton.setDisable(!hasSelection && selectedEndpointGroup == null);
        applySelectionPropertiesButton.setDisable(!hasSelection);
        applyEndpointHeightButton.setDisable(selectedEndpointGroup == null);
        boolean hasSurfaceTarget = currentSurfaceSelectionContext().isPresent();
        boolean hasSurfaceSelection = selectedSurfaceLayer().isPresent();
        addSurfaceLayerButton.setDisable(!hasSurfaceTarget);
        updateSurfaceLayerButton.setDisable(!hasSurfaceTarget || !hasSurfaceSelection);
        removeSurfaceLayerButton.setDisable(!hasSurfaceTarget || !hasSurfaceSelection);
        toggleSurfaceLayerVisibilityButton.setDisable(!hasSurfaceTarget || !hasSurfaceSelection);
        moveSurfaceLayerUpButton.setDisable(!hasSurfaceTarget || !hasSurfaceSelection || surfaceLayerList.getSelectionModel().getSelectedIndex() <= 0);
        moveSurfaceLayerDownButton.setDisable(!hasSurfaceTarget || !hasSurfaceSelection || surfaceLayerList.getSelectionModel().getSelectedIndex() >= surfaceLayerList.getItems().size() - 1);
        boolean hasDwgBlock = Optional.ofNullable(dwgBlockSelector.getValue()).filter(DwgBlockDefinition::hasGeometry).isPresent();
        refreshDwgLibraryButton.setDisable(currentDwgLibraryPath().isEmpty());
        addDwgBlockAsSurfaceButton.setDisable(!hasDwgBlock);
        addDwgBlockAsObjectButton.setDisable(!hasDwgBlock);
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
        initializeUnitSelector(gridField, gridUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(lengthField, lengthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(wallThicknessField, wallThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(wallHeightField, wallHeightUnit, LengthUnit.METER);
        initializeUnitSelector(endpointHeightField, endpointHeightUnit, LengthUnit.METER);
        initializeUnitSelector(roomHeightField, roomHeightUnit, LengthUnit.METER);
        initializeUnitSelector(floorThicknessField, floorThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(ceilingThicknessField, ceilingThicknessUnit, LengthUnit.MILLIMETER);
        initializeUnitSelector(kneeWallHeightField, kneeWallHeightUnit, LengthUnit.METER);
        initializeUnitSelector(doorWidthField, doorWidthUnit, LengthUnit.METER);
        initializeUnitSelector(doorHeightField, doorHeightUnit, LengthUnit.METER);
        initializeUnitSelector(thresholdField, thresholdUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(windowWidthField, windowWidthUnit, LengthUnit.METER);
        initializeUnitSelector(windowHeightField, windowHeightUnit, LengthUnit.METER);
        initializeUnitSelector(sillHeightField, sillHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairHeightField, stairHeightUnit, LengthUnit.METER);
        initializeUnitSelector(stairStartLandingField, stairStartLandingUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairEndLandingField, stairEndLandingUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceLayerThicknessField, surfaceLayerThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceTileWidthField, surfaceTileWidthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceTileHeightField, surfaceTileHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceLayoutOffsetField, surfaceLayoutOffsetUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceMinimumOffsetField, surfaceMinimumOffsetUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceJointWidthField, surfaceJointWidthUnit, LengthUnit.MILLIMETER);
    }

    private void initializeUnitSelector(TextField field, ComboBox<LengthUnit> selector, LengthUnit defaultUnit) {
        selector.getItems().addAll(LengthUnit.values());
        selector.setValue(defaultUnit);
        selector.valueProperty().addListener((ignored, oldUnit, newUnit) -> convertLengthInputOnUnitChange(field, oldUnit, newUnit));
    }

    private void convertLengthInputOnUnitChange(TextField field, LengthUnit oldUnit, LengthUnit newUnit) {
        if (updatingLengthInput || oldUnit == null || newUnit == null || oldUnit == newUnit) {
            return;
        }
        parseLength(field, oldUnit)
                .ifPresent(length -> field.setText(formatValue(length, newUnit, LENGTH_INPUT_DECIMALS)));
    }

    private void initializeSlopedCeilingControls() {
        slopedCeilingModeSelector.getItems().setAll("Ohne Dachschräge", "Mit Dachschräge");
        slopedCeilingModeSelector.setValue("Ohne Dachschräge");
        slopedCeilingSideSelector.getItems().setAll(SlopedCeilingSide.values());
        slopedCeilingSideSelector.setValue(SlopedCeilingSide.NORTH);
    }

    private void initializePresetSelectors() {
        availableDoorPresets.setAll(partLibrary.doorPresets());
        availableWindowPresets.setAll(partLibrary.windowPresets());
        availableStairPresets.setAll(partLibrary.stairPresets());
        availableRoomObjectPresets.setAll(roomObjectPresetService.presets());
        doorPresetSelector.setItems(availableDoorPresets);
        windowPresetSelector.setItems(availableWindowPresets);
        stairPresetSelector.setItems(availableStairPresets);
        roomObjectPresetSelector.setItems(availableRoomObjectPresets);
        selectFirstIfAvailable(doorPresetSelector, availableDoorPresets);
        selectFirstIfAvailable(windowPresetSelector, availableWindowPresets);
        selectFirstIfAvailable(stairPresetSelector, availableStairPresets);
        selectFirstIfAvailable(roomObjectPresetSelector, availableRoomObjectPresets);
        applyDoorPreset(doorPresetSelector.getValue());
        applyWindowPreset(windowPresetSelector.getValue());
        applyStairPreset(stairPresetSelector.getValue());
        doorPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyDoorPreset(newValue));
        windowPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyWindowPreset(newValue));
        stairPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyStairPreset(newValue));
    }

    private void initializeSurfaceLayerControls() {
        availableSurfacePresets.setAll(surfaceCoveringPresetService.defaults());
        loadUserSurfacePresets();
        surfacePresetSelector.setItems(availableSurfacePresets);
        if (!availableSurfacePresets.isEmpty()) {
            surfacePresetSelector.setValue(availableSurfacePresets.getFirst());
        }
        surfaceLayoutModeSelector.getItems().setAll(SurfaceLayoutMode.values());
        surfaceLayoutModeSelector.setValue(SurfaceLayoutMode.AUTOMATIC);
        surfaceCutRestrictionSelector.getItems().setAll(SurfaceCutRestriction.values());
        surfaceCutRestrictionSelector.setValue(SurfaceCutRestriction.fallback());
        surfaceLayerList.setPrefHeight(120);
        surfaceLayerList.getSelectionModel().selectedIndexProperty().addListener((ignored, oldValue, newValue) -> syncInputsFromSelectedSurfaceLayer());
        surfaceTypeSelector.valueProperty().addListener((ignored, oldValue, newValue) -> {
            if (newValue == SurfaceType.FLOOR || newValue == SurfaceType.CEILING) {
                preferredRoomSurfaceType = newValue;
            }
            refreshSurfaceLayerSection();
        });
        surfacePresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applySurfacePreset(newValue));
        refreshSurfaceTypeSelector();
        applySurfacePreset(surfacePresetSelector.getValue());
    }

    private void initializeDwgLibraryControls() {
        dwgBlockSelector.setItems(availableDwgBlocks);
        dwgBlockSelector.setPrefWidth(220);
        dwgBlockSearchField.setPromptText("Block, Layer oder Datei");
        dwgObjectFloorModeSelector.getItems().setAll(RoomObjectMountingMode.values());
        dwgObjectFloorModeSelector.setValue(RoomObjectMountingMode.STANDS_ON_COVERING);
        dwgBlockSearchField.textProperty().addListener((ignored, oldValue, newValue) -> applyDwgBlockFilter());
        dwgBlockSelector.valueProperty().addListener((ignored, oldValue, newValue) -> refreshDwgBlockPreviewAndDetails());
        DwgConversionAvailability availability = dwgLibraryAnalyzer.availability();
        dwgStatusLabel.setText(availability.message());
        drawEmptyDwgPreview("Keine DWG");
    }

    private void loadUserSurfacePresets() {
        try {
            userSurfacePresetLibrary.loadPresets().forEach(this::registerSurfacePreset);
        } catch (RuntimeException | IOException exception) {
            draftLabel.setText("Eigene Belagspresets konnten nicht geladen werden: " + exception.getMessage());
        }
    }

    private void registerConfiguredDwgLibraries() {
        try {
            userSurfacePresetLibrary.loadCadLibraries().forEach(this::registerConfiguredDwgLibraryReference);
        } catch (IOException exception) {
            draftLabel.setText("Gespeicherte DWG-Bibliotheken konnten nicht geladen werden: " + exception.getMessage());
        }
    }

    private void registerConfiguredDwgLibraryReference(Path sourceFile) {
        Path normalizedSource = sourceFile.toAbsolutePath().normalize();
        if (!cadLibraryReferences.contains(normalizedSource)) {
            cadLibraryReferences.add(normalizedSource);
        }
        registerSurfacePreset(surfaceCoveringPresetService.fromDwg(normalizedSource));
        dwgBlockCatalogService.loadCatalog(normalizedSource)
                .forEach(blockName -> registerSurfacePreset(surfaceCoveringPresetService.fromDwgBlock(normalizedSource, blockName)));
        updateCadLibrarySummary();
    }

    private <T> void selectFirstIfAvailable(ComboBox<T> selector, ObservableList<T> values) {
        if (!values.isEmpty()) {
            selector.setValue(values.getFirst());
        }
    }

    private void applyFormTooltips() {
        applyTooltip(toolSelector, "Wählt das aktuelle Zeichenwerkzeug aus. Räume werden aus geschlossenen Wandzügen automatisch abgeleitet, im Werkzeug `Bearbeiten` ausgewählt und beim Zeichnen von Wänden über die sichtbaren Standardwerte links mitgesteuert.");
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
        applyTooltip(endpointHeightField, "Legt die Höhe für den aktuell ausgewählten gemeinsamen Wand-Endpunkt fest. Daraus wird bei geschlossenen Wandzügen eine schräge Decke des betroffenen Raums abgeleitet.");
        applyTooltip(endpointHeightUnit, "Bestimmt die Einheit für die Endpunkthöhe einer ausgewählten Wandecke.");
        applyTooltip(roomNameField, "Legt den Namen für automatisch erkannte Räume oder für die aktuell ausgewählte Raumauswahl fest.");
        applyTooltip(roomHeightField, "Legt die lichte Raumhöhe für automatisch erkannte Räume oder die aktuell ausgewählte Raumauswahl fest.");
        applyTooltip(roomHeightUnit, "Bestimmt die Einheit für die Raumhöhe.");
        applyTooltip(floorThicknessField, "Legt die Boden- oder Fußbodenstärke für automatisch erkannte Räume oder die aktuell ausgewählte Raumauswahl fest.");
        applyTooltip(floorThicknessUnit, "Bestimmt die Einheit für die Bodenstärke.");
        applyTooltip(ceilingThicknessField, "Legt die Deckenstärke für automatisch erkannte Räume oder die aktuell ausgewählte Raumauswahl fest.");
        applyTooltip(ceilingThicknessUnit, "Bestimmt die Einheit für die Deckenstärke.");
        applyTooltip(slopedCeilingModeSelector, "Aktiviert optional eine innere Dachschräge beziehungsweise schräge Decke für rechteckige Räume. Ohne Aktivierung bleibt die Decke waagerecht.");
        applyTooltip(slopedCeilingSideSelector, "Legt fest, an welcher Raumkante die niedrige Sockelhöhe liegt. Die Schräge steigt immer zur gegenüberliegenden Kante an.");
        applyTooltip(kneeWallHeightField, "Legt die Sockel- beziehungsweise Kniestockhöhe der Dachschräge an der niedrigen Raumkante fest.");
        applyTooltip(kneeWallHeightUnit, "Bestimmt die Einheit für die Sockelhöhe der Dachschräge.");
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
        applyTooltip(stairStepsField, "Legt die gesamte Stufenanzahl fest. Konfigurierte Anfangs- und Endabsätze zählen jeweils als eine Stufe mit.");
        applyTooltip(stairStartLandingField, "Legt die Tiefe des ebenen Absatzes am Anfang der Treppe in Laufrichtung fest. Null deaktiviert den Absatz.");
        applyTooltip(stairStartLandingUnit, "Bestimmt die Einheit für die Tiefe des Anfangsabsatzes.");
        applyTooltip(stairEndLandingField, "Legt die Tiefe des ebenen Absatzes am Ende der Treppe in Laufrichtung fest. Null deaktiviert den Absatz.");
        applyTooltip(stairEndLandingUnit, "Bestimmt die Einheit für die Tiefe des Endabsatzes.");
        applyTooltip(roomObjectPresetSelector, "Wählt ein Raumobjekt zum Platzieren aus. DWG-Dateien unter `~/.config/CADas/Objekte` erscheinen hier zusätzlich als Objekt-Presets.");
        applyTooltip(surfaceTypeSelector, "Zeigt nur die Belagstypen an, die zur aktuellen Auswahl passen. Raum allein erlaubt Boden oder Decke, Raum plus Wand erlaubt Innenwand, Wand allein erlaubt Außenwand.");
        applyTooltip(surfacePresetSelector, "Wählt einen Beispielbelag oder eine DWG-Referenz aus und übernimmt deren Standardwerte in die Ebenenfelder.");
        applyTooltip(surfaceLayerList, "Zeigt die Ebenen der aktuell ausgewählten Fläche in ihrer Stapelreihenfolge an.");
        applyTooltip(surfaceLayerNameField, "Legt den Namen der Ebene fest, etwa Fliese, Rigips, Dämmplatte oder eine DWG-Referenz.");
        applyTooltip(surfaceLayerThicknessField, "Legt die Dicke der Ebene fest. Innenwand- und Deckenbeläge wirken direkt auf Raumgeometrie und Volumen.");
        applyTooltip(surfaceLayerThicknessUnit, "Bestimmt die Einheit für die Dicke des ausgewählten Belags.");
        applyTooltip(surfaceTileWidthField, "Legt die Breite einer Fliese oder Platte für die Belegungsbasis fest.");
        applyTooltip(surfaceTileWidthUnit, "Bestimmt die Einheit für die Breite der Fliese oder Platte.");
        applyTooltip(surfaceTileHeightField, "Legt die Höhe beziehungsweise Länge einer Fliese oder Platte für die Belegungsbasis fest.");
        applyTooltip(surfaceTileHeightUnit, "Bestimmt die Einheit für die Höhe oder Länge des Belags.");
        applyTooltip(surfaceLayoutModeSelector, "Bestimmt, ob ohne Versatz, mit automatischem Versatz oder mit festem Reihenversatz belegt wird.");
        applyTooltip(surfaceLayoutOffsetField, "Legt bei festem Versatz den horizontalen Reihenversatz fest.");
        applyTooltip(surfaceLayoutOffsetUnit, "Bestimmt die Einheit für den festen Reihenversatz.");
        applyTooltip(surfaceMinimumOffsetField, "Legt den kleinsten zulässigen automatischen Versatz zwischen zwei Reihen fest.");
        applyTooltip(surfaceMinimumOffsetUnit, "Bestimmt die Einheit für den Mindestversatz.");
        applyTooltip(surfaceMinimumEdgeWidthField, "Legt die kleinste zulässige Restbreite links und rechts innerhalb einer Reihe fest.");
        applyTooltip(surfaceMinimumEdgeWidthUnit, "Bestimmt die Einheit für die seitliche Mindestbreite an den Rändern.");
        applyTooltip(surfaceMinimumStartEndMarginField, "Legt die kleinste zulässige Breite der Anfangs- und Endreihe in Verlegerichtung fest. Wenn die Endreihe zu schmal würde, wird die Anfangsreihe entsprechend beschnitten, bleibt aber direkt an der Wand.");
        applyTooltip(surfaceMinimumStartEndMarginUnit, "Bestimmt die Einheit für die Mindestbreite der Anfangs- und Endreihe.");
        applyTooltip(surfaceJointWidthField, "Legt die Breite der Fugen zwischen den Fliesen oder Platten fest.");
        applyTooltip(surfaceJointWidthUnit, "Bestimmt die Einheit für die Fugenbreite.");
        applyTooltip(surfaceCutRestrictionSelector, "Legt fest, ob Zuschnitte beliebig frei verwendet werden dürfen, ob Schnittkanten nur an Außenkanten liegen dürfen oder ob zusätzlich die Verlegerichtung ohne Drehung eingehalten werden muss.");
        applyTooltip(dwgBlockNameField, "Erfasst einen konkreten Blocknamen aus einer geladenen DWG-Bibliothek, damit daraus ein auswählbares Oberflächen-Preset wird.");
        applyTooltip(surfaceLayerTargetLabel, "Zeigt, auf welcher Wand- oder Raumfläche die aktuellen Ebenen bearbeitet werden.");
        applyTooltip(surfaceLayerSelectionHintLabel, "Erklärt, welche Kombination aus Raum- und Wandauswahl für den aktuell sichtbaren Belagstyp erforderlich ist.");
        applyTooltip(surfaceLayerCoverageLabel, "Zeigt eine Kurzbewertung der aktuellen Platten- oder Fliesenbelegung der markierten Ebene.");
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
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            lastCursor = screenToWorld(event.getX(), event.getY());
            altPressed = event.isAltDown();
            updateMouseCursor();
            updateStatus();
            render();
        });
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_EXITED, event -> drawingCanvas.setCursor(Cursor.DEFAULT));
        drawingCanvas.setOnScroll(event -> {
            double oldScale = scale();
            double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
            zoom = twoDZoomRange.clamp(zoom * zoomFactor);
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

        if (event.getButton() == MouseButton.SECONDARY
                || event.getButton() == MouseButton.MIDDLE
                || event.getButton() == MouseButton.PRIMARY && spacePressed) {
            panning = true;
            panningMoved = false;
            pendingContextSelection = event.getButton() == MouseButton.SECONDARY && currentTool() == DrawingTool.EDIT
                    ? contextSelectionAt(event)
                    : null;
            panStartX = event.getX();
            panStartY = event.getY();
            panOriginX = offsetX;
            panOriginY = offsetY;
            updateMouseCursor();
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
            PlanPoint rawEditPoint = screenToWorld(event.getX(), event.getY());
            activeEdgeHandle = edgeResizeService.findHandle(
                    activeLevel.get(),
                    Set.copyOf(selectedSelections),
                    rawEditPoint,
                    Length.ofMillimeters(8.0 / scale())
            ).orElse(null);
            if (activeEdgeHandle != null) {
                edgeResizeBaseWalls = List.copyOf(activeLevel.get().walls());
                edgeResizeBaseDoors = List.copyOf(activeLevel.get().doors());
                edgeResizeBaseWindows = List.copyOf(activeLevel.get().windows());
                selectedEndpointGroup = null;
                selectionDragAnchor = null;
                openingDragId = null;
                historyCapturedForDrag = false;
                draftLabel.setText("Kanten-Handle ausgewählt: Ziehen verlängert oder kürzt das Bauteil entlang seiner Wandachse.");
                render();
                return;
            }
            DraftingConstraints constraints = currentConstraints(false);
            PlanPoint editPoint = snapService.snap(screenToWorld(event.getX(), event.getY()), constraints, activeLevel.get().walls());
            selectedEndpointGroup = wallEditingService.findConnectedEndpoint(activeLevel.get().walls(), editPoint, SNAP_TOLERANCE).orElse(null);
            selectionDragAnchor = null;
            selectionDragBaseWalls = List.of();
            selectionDragBaseStaircases = List.of();
            selectionDragBaseRoomObjects = List.of();
            historyCapturedForDrag = false;
            if (selectedEndpointGroup != null) {
                syncEndpointHeightInputFromSelection();
                activeLevel.get().walls().stream()
                        .filter(wall -> selectedEndpointGroup.startWallIds().contains(wall.id()) || selectedEndpointGroup.endWallIds().contains(wall.id()))
                        .findFirst()
                        .ifPresent(wall -> updateSelection(
                                new SelectionKey(RenderableKind.WALL, activeLevel.get().name(), wall.id().toString()),
                                event.isShortcutDown() || event.isShiftDown()
                        ));
                draftLabel.setText("Wandecke ausgewählt. `Eckhöhe anwenden` setzt die Höhe auf alle verbundenen Wandenden.");
            } else {
                SelectionKey editSelection = editSelectionAt(editPoint, event.isAltDown());
                updateSelection(editSelection, event.isShortcutDown() || event.isShiftDown());
                prepareSelectionDrag(editSelection, editPoint);
                openingDragId = null;
                openingDragWallAxis = null;
                openingDragWidth = 0;
                openingDragOffsetDelta = 0;
                boolean onlyDoorOrWindow = selectedSelections.stream().allMatch(
                        s -> s.kind() == RenderableKind.DOOR || s.kind() == RenderableKind.WINDOW);
                if (onlyDoorOrWindow && editSelection != null && (editSelection.kind() == RenderableKind.DOOR || editSelection.kind() == RenderableKind.WINDOW)) {
                    UUID elementId = UUID.fromString(editSelection.elementId());
                    if (editSelection.kind() == RenderableKind.DOOR) {
                        activeLevel.get().doors().stream()
                                .filter(door -> door.id().equals(elementId))
                                .findFirst()
                                .ifPresent(door -> {
                                    Wall wall = activeLevel.get().findWall(door.wallId());
                                    openingDragId = door.id();
                                    openingDragWallAxis = wall.axis();
                                    openingDragWidth = door.width().toMillimeters();
                                    openingDragOffsetDelta = door.offsetFromStart().toMillimeters() - openingDragWallAxis.projectedLength(editPoint).toMillimeters();
                                });
                    } else {
                        activeLevel.get().windows().stream()
                                .filter(window -> window.id().equals(elementId))
                                .findFirst()
                                .ifPresent(window -> {
                                    Wall wall = activeLevel.get().findWall(window.wallId());
                                    openingDragId = window.id();
                                    openingDragWallAxis = wall.axis();
                                    openingDragWidth = window.width().toMillimeters();
                                    openingDragOffsetDelta = window.offsetFromStart().toMillimeters() - openingDragWallAxis.projectedLength(editPoint).toMillimeters();
                                });
                    }
                }
            }
            render();
            return;
        }

        DraftingConstraints constraints = currentConstraints(!event.isShiftDown());
        draftStart = snapDrawingPoint(screenToWorld(event.getX(), event.getY()), constraints);
        previewSegment = new PlanSegment(draftStart, draftStart);
        if (currentTool() == DrawingTool.DOOR) {
            placeDoor(draftStart);
            draftStart = null;
            previewSegment = null;
        } else if (currentTool() == DrawingTool.WINDOW) {
            placeWindow(draftStart);
            draftStart = null;
            previewSegment = null;
        } else if (currentTool() == DrawingTool.OBJECT) {
            placeRoomObject(draftStart);
            draftStart = null;
            previewSegment = null;
        }
        render();
    }

    private SelectionKey editSelectionAt(PlanPoint editPoint, boolean cycleSelection) {
        List<SelectionKey> candidates = selectionQueryService.findSelections(activeLevel.get(), editPoint, SNAP_TOLERANCE);
        if (candidates.isEmpty()) {
            return null;
        }
        if (!cycleSelection || candidates.size() == 1) {
            return candidates.getFirst();
        }
        SelectionKey currentSelection = selectedSelection.get();
        int currentIndex = candidates.indexOf(currentSelection);
        SelectionKey nextSelection = candidates.get((currentIndex + 1) % candidates.size());
        draftLabel.setText("Auswahl umgeschaltet: " + selectionLabel(nextSelection) + ".");
        return nextSelection;
    }

    private void handleMouseDragged(MouseEvent event) {
        if (panning) {
            panningMoved |= Math.hypot(event.getX() - panStartX, event.getY() - panStartY) > 3.0;
            offsetX = panOriginX + (event.getX() - panStartX);
            offsetY = panOriginY + (event.getY() - panStartY);
            render();
            updateMouseCursor();
            return;
        }

        if (draftStart == null) {
            if (activeEdgeHandle != null) {
                if (!historyCapturedForDrag) {
                    rememberStateForUndo();
                    historyCapturedForDrag = true;
                }
                Level baseLevel = new Level(activeLevel.get().name());
                baseLevel.replaceWalls(edgeResizeBaseWalls);
                baseLevel.replaceDoors(edgeResizeBaseDoors);
                baseLevel.replaceWindows(edgeResizeBaseWindows);
                Set<UUID> excludedWallIds = Set.of(activeEdgeHandle.hostWallId());
                List<Wall> snapWalls = edgeResizeBaseWalls.stream()
                        .filter(wall -> activeEdgeHandle.kind() != EdgeResizeService.EdgeHandleKind.WALL_START
                                && activeEdgeHandle.kind() != EdgeResizeService.EdgeHandleKind.WALL_END
                                || !wall.id().equals(activeEdgeHandle.hostWallId()))
                        .toList();
                PlanPoint snappedPoint = snapService.snap(
                        screenToWorld(event.getX(), event.getY()),
                        currentConstraints(false),
                        snapWalls,
                        currentAlignmentSnapTargets(excludedWallIds)
                );
                EdgeResizeService.ResizeResult result = edgeResizeService.resize(baseLevel, activeEdgeHandle, snappedPoint);
                activeLevel.get().replaceWalls(result.walls());
                activeLevel.get().replaceDoors(result.doors());
                activeLevel.get().replaceWindows(result.windows());
                synchronizeRoomsFromWalls(activeLevel.get());
                markThreeDDirty();
                render();
                return;
            }
            if (selectedEndpointGroup != null) {
                if (!historyCapturedForDrag) {
                    rememberStateForUndo();
                    historyCapturedForDrag = true;
                }
                DraftingConstraints constraints = currentConstraints(false);
                Set<UUID> endpointWallIds = java.util.stream.Stream.concat(
                        selectedEndpointGroup.startWallIds().stream(),
                        selectedEndpointGroup.endWallIds().stream()
                ).collect(java.util.stream.Collectors.toSet());
                PlanPoint snappedPoint = snapService.snap(
                        screenToWorld(event.getX(), event.getY()),
                        constraints,
                        activeLevel.get().walls(),
                        currentAlignmentSnapTargets(endpointWallIds)
                );
                activeLevel.get().replaceWalls(wallEditingService.moveEndpointGroup(
                        activeLevel.get().walls(),
                        selectedEndpointGroup,
                        snappedPoint,
                        !event.isShiftDown()
                ));
                synchronizeRoomsFromWalls(activeLevel.get());
                markThreeDDirty();
                render();
            }
            if (openingDragId != null) {
                if (!historyCapturedForDrag) {
                    rememberStateForUndo();
                    historyCapturedForDrag = true;
                }
                DraftingConstraints constraints = currentConstraints(false);
                PlanPoint snappedPoint = snapService.snap(
                        screenToWorld(event.getX(), event.getY()),
                        constraints,
                        activeLevel.get().walls(),
                        currentGuideSnapTargets()
                );
                double wallLength = openingDragWallAxis.length().toMillimeters();
                double rawOffset = openingDragWallAxis.projectedLength(snappedPoint).toMillimeters() + openingDragOffsetDelta;
                double clampedOffset = Math.max(0.0, Math.min(wallLength - openingDragWidth, rawOffset));
                Wall openingWall = openingDragWall();
                Length newOffset = snapToGuides.get()
                        ? guideSnapService.snapOpeningOffset(
                                openingWall,
                                Length.ofMillimeters(clampedOffset),
                                Length.ofMillimeters(openingDragWidth),
                                currentGuideSnapTargets(),
                                SNAP_TOLERANCE
                        )
                        : Length.ofMillimeters(clampedOffset);
                activeLevel.get().replaceDoors(activeLevel.get().doors().stream()
                        .map(door -> door.id().equals(openingDragId) ? door.withOffset(newOffset) : door)
                        .toList());
                activeLevel.get().replaceWindows(activeLevel.get().windows().stream()
                        .map(window -> window.id().equals(openingDragId) ? window.withOffset(newOffset) : window)
                        .toList());
                synchronizeRoomsFromWalls(activeLevel.get());
                markThreeDDirty();
                render();
                return;
            }
            if (selectionDragAnchor != null) {
                if (!historyCapturedForDrag) {
                    rememberStateForUndo();
                    historyCapturedForDrag = true;
                }
                DraftingConstraints constraints = currentConstraints(false);
                PlanPoint snappedPoint = snapService.snap(screenToWorld(event.getX(), event.getY()), constraints, activeLevel.get().walls());
                translateSelectedComponents(snappedPoint);
                render();
            }
            return;
        }

        if (currentTool().isPointTool()) {
            return;
        }

        DraftingConstraints constraints = currentConstraints(!event.isShiftDown());
        PlanPoint snappedPoint = snapDrawingPoint(screenToWorld(event.getX(), event.getY()), constraints);
        previewSegment = draftingService.createSegment(draftStart, snappedPoint, constraints);
        if ((snapToGuides.get() || snapToWalls.get()) && constraints.manualLength().isEmpty() && constraints.manualAngle().isEmpty()) {
            previewSegment = guideSnapService.snapWallSegment(
                    previewSegment,
                    currentWallThickness(),
                    currentAlignmentSnapTargets(Set.of()),
                    SNAP_TOLERANCE
            );
            draftStart = previewSegment.start();
        }
        lastCursor = previewSegment.end();
        updateStatus();
        render();
    }

    private void handleMouseReleased(MouseEvent event) {
        if (panning) {
            panning = false;
            if (!panningMoved && pendingContextSelection != null && event.getButton() == MouseButton.SECONDARY) {
                if (!selectedSelections.contains(pendingContextSelection)) {
                    selectSingle(pendingContextSelection);
                }
                selectionContextMenu.show(drawingCanvas, event.getScreenX(), event.getScreenY());
            }
            pendingContextSelection = null;
            updateMouseCursor();
            return;
        }

        if (activeEdgeHandle != null) {
            activeEdgeHandle = null;
            edgeResizeBaseWalls = List.of();
            edgeResizeBaseDoors = List.of();
            edgeResizeBaseWindows = List.of();
            historyCapturedForDrag = false;
            updatePropertySectionVisibility();
            updateActionButtons();
            render();
            return;
        }

        if (selectedEndpointGroup != null) {
            historyCapturedForDrag = false;
            updatePropertySectionVisibility();
            updateActionButtons();
            render();
            return;
        }

        if (openingDragId != null) {
            openingDragId = null;
            openingDragWallAxis = null;
            openingDragWidth = 0;
            openingDragOffsetDelta = 0;
            historyCapturedForDrag = false;
            updatePropertySectionVisibility();
            updateActionButtons();
            render();
            return;
        }

        if (selectionDragAnchor != null) {
            selectionDragAnchor = null;
            selectionDragBaseWalls = List.of();
            selectionDragBaseStaircases = List.of();
            selectionDragBaseRoomObjects = List.of();
            historyCapturedForDrag = false;
            updatePropertySectionVisibility();
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
                synchronizeRoomsFromWalls(activeLevel.get());
                selectSingle(new SelectionKey(RenderableKind.WALL, activeLevel.get().name(), wall.id().toString()));
            } else if (currentTool() == DrawingTool.STAIR) {
                rememberStateForUndo();
                Staircase staircase = Staircase.create(
                        currentStairType(),
                        previewSegment.start(),
                        previewSegment.end(),
                        currentStairHeight(),
                        currentStairSteps(),
                        currentStairStartLanding(),
                        currentStairEndLanding()
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

    private SelectionKey contextSelectionAt(MouseEvent event) {
        DraftingConstraints constraints = currentConstraints(false);
        PlanPoint editPoint = snapService.snap(
                screenToWorld(event.getX(), event.getY()),
                constraints,
                activeLevel.get().walls()
        );
        return selectionQueryService.findSelection(activeLevel.get(), editPoint, SNAP_TOLERANCE).orElse(null);
    }

    private void updateModifierState(KeyEvent event) {
        altPressed = event.isAltDown();
        if (event.getCode() == KeyCode.SPACE) {
            spacePressed = event.getEventType() == KeyEvent.KEY_PRESSED;
        }
        updateMouseCursor();
    }

    private void updateMouseCursor() {
        PointerCursorService.PointerTarget target = pointerTargetAtLastPosition();
        PointerCursorService.CursorType cursorType = pointerCursorService.cursor(new PointerCursorService.PointerContext(
                currentTool(),
                target,
                panning,
                spacePressed,
                altPressed
        ));
        drawingCanvas.setCursor(switch (cursorType) {
            case DEFAULT -> Cursor.DEFAULT;
            case CROSSHAIR -> Cursor.CROSSHAIR;
            case HAND -> Cursor.HAND;
            case OPEN_HAND -> Cursor.OPEN_HAND;
            case CLOSED_HAND -> Cursor.CLOSED_HAND;
            case MOVE -> Cursor.MOVE;
            case HORIZONTAL_RESIZE -> Cursor.H_RESIZE;
            case VERTICAL_RESIZE -> Cursor.V_RESIZE;
        });
    }

    private PointerCursorService.PointerTarget pointerTargetAtLastPosition() {
        PlanPoint point = screenToWorld(lastMouseX, lastMouseY);
        Optional<EdgeResizeService.EdgeHandle> handle = edgeResizeService.findHandle(
                activeLevel.get(),
                Set.copyOf(selectedSelections),
                point,
                Length.ofMillimeters(8.0 / scale())
        );
        if (handle.isPresent()) {
            Wall wall = activeLevel.get().findWall(handle.orElseThrow().hostWallId());
            double deltaX = Math.abs(wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters());
            double deltaY = Math.abs(wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters());
            return deltaX >= deltaY
                    ? PointerCursorService.PointerTarget.HORIZONTAL_EDGE
                    : PointerCursorService.PointerTarget.VERTICAL_EDGE;
        }
        if (currentTool() == DrawingTool.EDIT
                && wallEditingService.findConnectedEndpoint(activeLevel.get().walls(), point, Length.ofMillimeters(8.0 / scale())).isPresent()) {
            return PointerCursorService.PointerTarget.ENDPOINT;
        }
        if (currentTool() == DrawingTool.EDIT
                && selectionQueryService.findSelection(activeLevel.get(), point, Length.ofMillimeters(8.0 / scale())).isPresent()) {
            return PointerCursorService.PointerTarget.ELEMENT;
        }
        return PointerCursorService.PointerTarget.EMPTY;
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

        drawLowerLevel(graphics);

        if (showGrid.get()) {
            drawGrid(graphics);
        }
        if (showGuides.get()) {
            drawGuides(graphics);
        }
        drawRooms(graphics);
        drawWalls(graphics);
        drawWallSurfaceLayers(graphics);
        drawStaircases(graphics);
        drawDoors(graphics);
        drawWindows(graphics);
        drawRoomObjects(graphics);
        drawEditablePoints(graphics);
        drawEdgeResizeHandles(graphics);
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
            drawGuideDistances(graphics);
        }
    }

    private void drawGuideDistances(GraphicsContext graphics) {
        if (!showGuideDistances.get()) {
            return;
        }
        List<GuideDistanceService.GuideDistance> distances = guideDistanceService.distancesToParallelGuides(
                guideLines,
                pendingGuideOrientation,
                pendingGuideWorldMillimeters
        );
        graphics.setFill(Color.color(0.35, 0.08, 0.08, 0.92));
        graphics.setFont(Font.font("Menlo", 11));
        for (int index = 0; index < distances.size(); index++) {
            GuideDistanceService.GuideDistance distance = distances.get(index);
            double midpoint = (pendingGuideWorldMillimeters + distance.guideWorldMillimeters()) / 2.0;
            String text = distance.distance().format(LengthUnit.METER, 2);
            if (pendingGuideOrientation == GuideOrientation.VERTICAL) {
                graphics.fillText(text, toScreenX(midpoint) - 22.0, 20.0 + index % 4 * 16.0);
            } else {
                graphics.fillText(text, 10.0 + index % 3 * 76.0, toScreenY(midpoint) - 5.0);
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

    private void drawLowerLevel(GraphicsContext graphics) {
        if (!projectionService.isPlanView(activeView.get())) return;
        int index = availableLevels.indexOf(activeLevel.get());
        if (index <= 0) return;
        Level lowerLevel = availableLevels.get(index - 1);
        graphics.setGlobalAlpha(0.2);
        Color gray = Color.gray(0.2);
        for (Room room : lowerLevel.rooms()) {
            double[] xPoints = room.outline().stream().mapToDouble(p -> toScreenProjectedX(p, 0.0)).toArray();
            double[] yPoints = room.outline().stream().mapToDouble(p -> toScreenProjectedY(p, 0.0)).toArray();
            graphics.setFill(gray);
            graphics.fillPolygon(xPoints, yPoints, xPoints.length);
            graphics.setStroke(gray);
            graphics.setLineWidth(2.0);
            graphics.strokePolygon(xPoints, yPoints, xPoints.length);
        }
        graphics.setLineCap(javafx.scene.shape.StrokeLineCap.SQUARE);
        for (Wall wall : lowerLevel.walls()) {
            drawWall(graphics, wall.axis(), wall.thickness(), gray, 1.0);
        }
        graphics.setGlobalAlpha(1.0);
    }

    private void drawWalls(GraphicsContext graphics) {
        graphics.setFill(Color.web("#2f2a24"));
        graphics.setLineCap(javafx.scene.shape.StrokeLineCap.SQUARE);

        for (Wall wall : activeLevel.get().walls()) {
            boolean selected = isSelected(RenderableKind.WALL, wall.id().toString());
            if (projectionService.isPlanView(activeView.get())) {
                drawWall(graphics, wall.axis(), wall.thickness(), selected ? Color.web("#d97f2f") : CadColorPalette.WALL, 1.0);
            } else {
                drawWallElevation(graphics, wall, selected);
            }
            if (showDimensions.get() && projectionService.isPlanView(activeView.get())) {
                if (selected) {
                    drawSelectedWallDimensions(graphics, wall);
                } else {
                    drawDimensionLabel(graphics, wall.axis(), wall.axis().length().format(LengthUnit.METER, 2));
                }
            }
        }
    }

    private void drawSelectedWallDimensions(GraphicsContext graphics, Wall wall) {
        WallDimensionService.WallDimensions dimensions = wallDimensionService.dimensions(activeLevel.get(), wall);
        double offset = Math.max(wall.thickness().toMillimeters() * scale() / 2.0 + 16.0, 24.0);
        for (WallDimensionService.SideDimension roomDimension : dimensions.roomDimensions()) {
            drawDimensionLabel(
                    graphics,
                    wall.axis(),
                    roomDimension.name() + ": Raummaß " + roomDimension.length().format(LengthUnit.METER, 2),
                    offset * roomDimension.sideSign()
            );
        }
        dimensions.exteriorDimension().ifPresent(exteriorDimension -> drawDimensionLabel(
                graphics,
                wall.axis(),
                "Außenmaß " + exteriorDimension.length().format(LengthUnit.METER, 2),
                offset * exteriorDimension.sideSign()
        ));
        if (dimensions.roomDimensions().isEmpty() && dimensions.exteriorDimension().isEmpty()) {
            drawDimensionLabel(graphics, wall.axis(), "Achsmaß " + wall.axis().length().format(LengthUnit.METER, 2));
        }
    }

    private void drawWallSurfaceLayers(GraphicsContext graphics) {
        if (projectionService.isPlanView(activeView.get())) {
            drawWallSurfaceLayersInPlan(graphics);
        } else {
            drawWallSurfaceLayersInElevation(graphics);
        }
    }

    private void drawWallSurfaceLayersInPlan(GraphicsContext graphics) {
        for (Wall wall : activeLevel.get().walls()) {
            activeLevel.get().surfaceLayerStacks().stream()
                    .filter(stack -> isWallSurfaceType(stack.surfaceType()))
                    .filter(stack -> WallSurfaceTargetKey.matchesWall(stack.targetKey(), wall.id()))
                    .forEach(stack -> drawWallSurfaceStackInPlan(graphics, wall, stack));
        }
    }

    private boolean isWallSurfaceType(SurfaceType surfaceType) {
        return surfaceType == SurfaceType.WALL_INTERIOR || surfaceType == SurfaceType.WALL_EXTERIOR;
    }

    private void drawWallSurfaceStackInPlan(GraphicsContext graphics, Wall wall, SurfaceLayerStack stack) {
        double cumulativeThickness = wall.thickness().toMillimeters() / 2.0;
        WallSurfaceSideService.WallLayerSides sides = wallSurfaceSideService.resolve(activeLevel.get(), wall, stack.surfaceType(), stack.targetKey());
        for (int layerIndex = 0; layerIndex < stack.layers().size(); layerIndex++) {
            SurfaceLayer layer = stack.layers().get(layerIndex);
            double layerThickness = layer.thickness().toMillimeters();
            if (layer.visible() && layerThickness > 0.0) {
                double centerOffset = cumulativeThickness + layerThickness / 2.0;
                if (sides.positiveSide()) {
                    drawWallSurfaceLayerInPlan(graphics, wall, stack, layer, layerIndex, centerOffset);
                }
                if (sides.negativeSide()) {
                    drawWallSurfaceLayerInPlan(graphics, wall, stack, layer, layerIndex, -centerOffset);
                }
            }
            cumulativeThickness += layerThickness;
        }
    }

    private void drawWallSurfaceLayerInPlan(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayerStack stack,
            SurfaceLayer layer,
            int layerIndex,
            double centerOffset
    ) {
        double wallLength = wall.axis().length().toMillimeters();
        if (wallLength <= 0.0) {
            return;
        }
        double sideSign = centerOffset < 0.0 ? -1.0 : 1.0;
        List<WallSurfaceInterval> visibleIntervals = wallSurfaceOpeningService.visiblePlanIntervals(activeLevel.get(), wall, sideSign);
        if (visibleIntervals.isEmpty()) {
            return;
        }
        graphics.save();
        graphics.setFill(Color.color(0.72, 0.58, 0.34, 0.82));
        for (WallSurfaceInterval interval : visibleIntervals) {
            fillWallSurfaceIntervalInPlan(graphics, wall, stack, layer, layerIndex, centerOffset, interval);
        }
        drawWallSurfaceJointsInPlan(graphics, wall, layer, centerOffset, visibleIntervals);
        graphics.restore();
    }

    private void fillWallSurfaceIntervalInPlan(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayerStack stack,
            SurfaceLayer layer,
            int layerIndex,
            double centerOffset,
            WallSurfaceInterval interval
    ) {
        WallSurfacePlanPolygon polygon = wallSurfacePlanGeometryService.surfacePolygon(
                activeLevel.get(),
                wall,
                stack,
                layer,
                layerIndex,
                centerOffset,
                interval
        );
        graphics.fillPolygon(
                polygon.points().stream().mapToDouble(point -> toScreenProjectedX(point, 0.0)).toArray(),
                polygon.points().stream().mapToDouble(point -> toScreenProjectedY(point, 0.0)).toArray(),
                polygon.points().size()
        );
    }

    private void drawWallSurfaceJointsInPlan(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayer layer,
            double centerOffset,
            List<WallSurfaceInterval> visibleIntervals
    ) {
        double wallLength = wall.axis().length().toMillimeters();
        double jointWidth = layer.jointWidth().toMillimeters();
        if (jointWidth < 0.001 || layer.tileWidth().toMillimeters() * scale() < 14.0) {
            return;
        }
        TileLayoutRequest request = new TileLayoutRequest(
                Length.ofMillimeters(wallLength),
                Length.ofMillimeters(wall.maximumHeightMillimeters()),
                layer.tileWidth(),
                layer.tileHeight(),
                layer.layoutMode(),
                layer.layoutOffset(),
                layer.minimumOffset(),
                layer.minimumEdgeWidth(),
                layer.minimumStartEndMargin()
        );
        graphics.setStroke(Color.color(0.20, 0.15, 0.09, 0.72));
        graphics.setLineWidth(Math.max(0.8, jointWidth * scale()));
        double sideSign = centerOffset < 0.0 ? -1.0 : 1.0;
        var jointPositions = new java.util.HashSet<String>();
        for (TilePlacement tile : tileLayoutService.fillSurface(request)) {
            double jointPosition = tile.xOffset().toMillimeters() + tile.width().toMillimeters();
            if (jointPosition <= 0.001 || jointPosition >= wallLength - 0.001) {
                continue;
            }
            if (!isVisiblePlanJoint(jointPosition, visibleIntervals)) {
                continue;
            }
            String key = String.format(Locale.US, "%.3f", jointPosition);
            if (!jointPositions.add(key)) {
                continue;
            }
            PlanPoint from = wallOffsetPoint(wall, jointPosition, centerOffset - sideSign * layer.thickness().toMillimeters() / 2.0);
            PlanPoint to = wallOffsetPoint(wall, jointPosition, centerOffset + sideSign * layer.thickness().toMillimeters() / 2.0);
            graphics.strokeLine(
                    toScreenProjectedX(from, 0.0),
                    toScreenProjectedY(from, 0.0),
                    toScreenProjectedX(to, 0.0),
                    toScreenProjectedY(to, 0.0)
            );
        }
    }

    private boolean isVisiblePlanJoint(double jointPosition, List<WallSurfaceInterval> visibleIntervals) {
        return visibleIntervals.stream()
                .anyMatch(interval -> jointPosition > interval.startMillimeters() + 0.001
                        && jointPosition < interval.endMillimeters() - 0.001);
    }

    private void drawWallSurfaceLayersInElevation(GraphicsContext graphics) {
        for (Wall wall : activeLevel.get().walls()) {
            activeLevel.get().surfaceLayerStacks().stream()
                    .filter(stack -> isWallSurfaceType(stack.surfaceType()))
                    .filter(stack -> WallSurfaceTargetKey.matchesWall(stack.targetKey(), wall.id()))
                    .forEach(stack -> drawWallSurfaceStackInElevation(graphics, wall, stack));
        }
    }

    private void drawWallSurfaceStackInElevation(GraphicsContext graphics, Wall wall, SurfaceLayerStack stack) {
        WallSurfaceSideService.WallLayerSides sides = wallSurfaceSideService.resolve(activeLevel.get(), wall, stack.surfaceType(), stack.targetKey());
        if (!sides.positiveSide() && !sides.negativeSide()) {
            return;
        }
        for (SurfaceLayer layer : stack.layers()) {
            if (layer.visible()) {
                if (sides.positiveSide()) {
                    drawWallSurfaceLayerInElevation(graphics, wall, layer, 1.0);
                }
                if (sides.negativeSide()) {
                    drawWallSurfaceLayerInElevation(graphics, wall, layer, -1.0);
                }
            }
        }
    }

    private void drawWallSurfaceLayerInElevation(GraphicsContext graphics, Wall wall, SurfaceLayer layer, double sideSign) {
        double wallLength = wall.axis().length().toMillimeters();
        double startHorizontal = projectHorizontal(wall.axis().start(), 0.0);
        double endHorizontal = projectHorizontal(wall.axis().end(), 0.0);
        if (wallLength <= 0.0 || Math.abs(endHorizontal - startHorizontal) < 10.0) {
            return;
        }
        List<WallSurfaceRectangle> visibleRectangles = wallSurfaceOpeningService.visibleRectangles(activeLevel.get(), wall, sideSign);
        if (visibleRectangles.isEmpty()) {
            return;
        }
        double startX = toScreenHorizontal(startHorizontal);
        double endX = toScreenHorizontal(endHorizontal);
        graphics.save();
        graphics.setFill(Color.color(0.72, 0.58, 0.34, 0.26));
        graphics.setStroke(Color.color(0.47, 0.36, 0.20, 0.80));
        graphics.setLineWidth(1.2);
        for (WallSurfaceRectangle rectangle : visibleRectangles) {
            double startRatio = rectangle.startMillimeters() / wallLength;
            double endRatio = rectangle.endMillimeters() / wallLength;
            double rectStartX = interpolateScreen(startX, endX, startRatio);
            double rectEndX = interpolateScreen(startX, endX, endRatio);
            double startTop = Math.min(rectangle.upperHeightMillimeters(), wall.heightAt(rectangle.startMillimeters()));
            double endTop = Math.min(rectangle.upperHeightMillimeters(), wall.heightAt(rectangle.endMillimeters()));
            if (startTop <= rectangle.lowerHeightMillimeters() && endTop <= rectangle.lowerHeightMillimeters()) {
                continue;
            }
            double bottomY = toScreenVertical(-rectangle.lowerHeightMillimeters());
            double startTopY = toScreenVertical(-startTop);
            double endTopY = toScreenVertical(-endTop);
            graphics.fillPolygon(
                    new double[]{rectStartX, rectEndX, rectEndX, rectStartX},
                    new double[]{bottomY, bottomY, endTopY, startTopY},
                    4
            );
            graphics.strokePolygon(
                    new double[]{rectStartX, rectEndX, rectEndX, rectStartX},
                    new double[]{bottomY, bottomY, endTopY, startTopY},
                    4
            );
        }
        drawWallSurfaceJointsInElevation(graphics, wall, layer, startX, endX, visibleRectangles);
        graphics.restore();
    }

    private void drawWallSurfaceJointsInElevation(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayer layer,
            double startX,
            double endX,
            List<WallSurfaceRectangle> visibleRectangles
    ) {
        double jointWidth = layer.jointWidth().toMillimeters();
        double wallLength = wall.axis().length().toMillimeters();
        double wallHeight = wall.maximumHeightMillimeters();
        if (jointWidth < 0.001 || wallLength <= 0.0 || wallHeight <= 0.0 || visibleRectangles.isEmpty()) {
            return;
        }
        TileLayoutRequest request = new TileLayoutRequest(
                Length.ofMillimeters(wallLength),
                Length.ofMillimeters(wallHeight),
                layer.tileWidth(),
                layer.tileHeight(),
                layer.layoutMode(),
                layer.layoutOffset(),
                layer.minimumOffset(),
                layer.minimumEdgeWidth(),
                layer.minimumStartEndMargin()
        );
        graphics.setStroke(Color.color(0.16, 0.12, 0.08, 0.78));
        graphics.setLineWidth(Math.max(0.7, jointWidth * scale()));
        var horizontalKeys = new java.util.HashSet<String>();
        var verticalKeys = new java.util.HashSet<String>();
        for (TilePlacement tile : tileLayoutService.fillSurface(request)) {
            double localStart = tile.xOffset().toMillimeters();
            double localEnd = localStart + tile.width().toMillimeters();
            double rowTop = tile.yOffset().toMillimeters() + tile.height().toMillimeters();
            drawClippedWallSurfaceJointsInElevation(
                    graphics,
                    horizontalKeys,
                    "h",
                    wallLength,
                    startX,
                    endX,
                    visibleRectangles,
                    localStart,
                    localEnd,
                    rowTop - jointWidth,
                    rowTop
            );
            drawClippedWallSurfaceJointsInElevation(
                    graphics,
                    verticalKeys,
                    "v",
                    wallLength,
                    startX,
                    endX,
                    visibleRectangles,
                    localEnd - jointWidth,
                    localEnd,
                    tile.yOffset().toMillimeters(),
                    tile.yOffset().toMillimeters() + tile.height().toMillimeters()
            );
        }
    }

    private void drawClippedWallSurfaceJointsInElevation(
            GraphicsContext graphics,
            Set<String> keys,
            String prefix,
            double wallLength,
            double startX,
            double endX,
            List<WallSurfaceRectangle> visibleRectangles,
            double localStartX,
            double localEndX,
            double localLowerY,
            double localUpperY
    ) {
        for (WallSurfaceRectangle rectangle : visibleRectangles) {
            double clippedStartX = Math.max(localStartX, rectangle.startMillimeters());
            double clippedEndX = Math.min(localEndX, rectangle.endMillimeters());
            double clippedLowerY = Math.max(localLowerY, rectangle.lowerHeightMillimeters());
            double clippedUpperY = Math.min(localUpperY, rectangle.upperHeightMillimeters());
            if (clippedEndX - clippedStartX <= 0.001 || clippedUpperY - clippedLowerY <= 0.001) {
                continue;
            }
            String key = String.format(Locale.US, "%s:%.3f:%.3f:%.3f:%.3f", prefix, clippedStartX, clippedEndX, clippedLowerY, clippedUpperY);
            if (!keys.add(key)) {
                continue;
            }
            double centerX = interpolateScreen(startX, endX, ((clippedStartX + clippedEndX) / 2.0) / wallLength);
            double centerY = toScreenVertical(-((clippedLowerY + clippedUpperY) / 2.0));
            if ("h".equals(prefix)) {
                graphics.strokeLine(
                        interpolateScreen(startX, endX, clippedStartX / wallLength),
                        centerY,
                        interpolateScreen(startX, endX, clippedEndX / wallLength),
                        centerY
                );
            } else {
                graphics.strokeLine(
                        centerX,
                        toScreenVertical(-clippedLowerY),
                        centerX,
                        toScreenVertical(-clippedUpperY)
                );
            }
        }
    }

    private PlanPoint wallOffsetPoint(Wall wall, double localDistance, double normalOffset) {
        double wallLength = wall.axis().length().toMillimeters();
        PlanPoint axisPoint = wall.axis().pointAt(Length.ofMillimeters(clamp(localDistance, 0.0, wallLength)));
        double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double length = Math.max(1.0, Math.hypot(dx, dy));
        return new PlanPoint(
                axisPoint.xMillimeters() - dy / length * normalOffset,
                axisPoint.yMillimeters() + dx / length * normalOffset
        );
    }

    private double interpolateScreen(double start, double end, double ratio) {
        return start + (end - start) * clamp(ratio, 0.0, 1.0);
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
            graphics.setLineWidth(2.0);
            graphics.strokePolygon(xPoints, yPoints, xPoints.length);
            if (showAreaVolume.get()) {
                PlanPoint center = room.centerPoint();
                drawRoomLabel(graphics, room, center);
            }
            drawRoomSlopeMarker(graphics, room);
            drawRoomTileGrid(graphics, room);
        }
    }

    private void drawRoomTileGrid(GraphicsContext graphics, Room room) {
        if (!projectionService.isPlanView(activeView.get())) {
            return;
        }
        SurfaceLayerStack stack = activeLevel.get().findSurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        if (stack == null || stack.layers().isEmpty()) {
            return;
        }
        SurfaceLayer layer = stack.layers().getFirst();
        if (!layer.visible()) {
            return;
        }
        TileLayoutRequest request = new TileLayoutRequest(
                Length.ofMillimeters(room.widthMillimeters()),
                Length.ofMillimeters(room.depthMillimeters()),
                layer.tileWidth(),
                layer.tileHeight(),
                layer.layoutMode(),
                layer.layoutOffset(),
                layer.minimumOffset(),
                layer.minimumEdgeWidth(),
                layer.minimumStartEndMargin()
        );
        List<TilePlacement> tiles = tileLayoutService.fillSurface(request);
        if (tiles.isEmpty()) {
            return;
        }
        double roomMinX = room.minXMillimeters();
        double roomMinY = room.minYMillimeters();
        double jointPx = Math.max(0.4, layer.jointWidth().toMillimeters() * scale());
        graphics.save();
        graphics.beginPath();
        PlanPoint[] outline = room.outline().toArray(new PlanPoint[0]);
        graphics.moveTo(toScreenX(outline[0].xMillimeters()), toScreenY(outline[0].yMillimeters()));
        for (int i = 1; i < outline.length; i++) {
            graphics.lineTo(toScreenX(outline[i].xMillimeters()), toScreenY(outline[i].yMillimeters()));
        }
        graphics.closePath();
        graphics.clip();
        graphics.setFill(Color.color(0.35, 0.25, 0.12, 0.55));
        var horizontalKeys = new java.util.HashSet<String>();
        var verticalKeys = new java.util.HashSet<String>();
        for (TilePlacement tile : tiles) {
            double tx = roomMinX + tile.xOffset().toMillimeters();
            double ty = roomMinY + tile.yOffset().toMillimeters();
            double tw = tile.width().toMillimeters();
            double th = tile.height().toMillimeters();
            String hKey = String.format(Locale.US, "h:%.3f:%.3f:%.3f", ty + th, tx, tx + tw);
            if (horizontalKeys.add(hKey)) {
                double screenX = toScreenX(tx);
                double screenY = toScreenY(ty + th - layer.jointWidth().toMillimeters() / 2.0);
                graphics.fillRect(screenX, screenY, tw * scale(), jointPx);
            }
            String vKey = String.format(Locale.US, "v:%.3f:%.3f:%.3f", tx + tw, ty, ty + th);
            if (verticalKeys.add(vKey)) {
                double screenX = toScreenX(tx + tw - layer.jointWidth().toMillimeters() / 2.0);
                double screenY = toScreenY(ty);
                graphics.fillRect(screenX, screenY, jointPx, th * scale());
            }
        }
        graphics.restore();
    }

    private void drawRoomLabel(GraphicsContext graphics, Room room, PlanPoint center) {
        graphics.setFill(Color.web("#5d4527"));
        graphics.setFont(Font.font("Menlo", 12));
        graphics.fillText(room.name(), toScreenProjectedX(center, 0.0) - 26, toScreenProjectedY(center, 0.0) - 6);
        graphics.setFont(Font.font("Menlo", 11));
        graphics.fillText(
                String.format(
                        Locale.GERMAN,
                        "%.2f m² | %.2f m³",
                        surfaceLayerEffectService.effectiveAreaSquareMeters(activeLevel.get(), room),
                        surfaceLayerEffectService.effectiveVolumeCubicMeters(activeLevel.get(), room)
                ),
                toScreenProjectedX(center, 0.0) - 42,
                toScreenProjectedY(center, 0.0) + 12
        );
    }

    private void drawRoomSlopeMarker(GraphicsContext graphics, Room room) {
        if (!projectionService.isPlanView(activeView.get()) || room.slopedCeilingProfile().isEmpty() || room.ceilingVertexHeightsProfile().isPresent()) {
            return;
        }
        SlopedCeilingProfile profile = room.slopedCeilingProfile().orElseThrow();
        graphics.setStroke(Color.color(0.52, 0.29, 0.14, 0.9));
        graphics.setLineWidth(1.6);
        graphics.setLineDashes(10.0, 8.0);
        PlanPoint start = room.centerPoint();
        PlanPoint end = room.centerPoint();
        PlanPoint arrowCenter = room.centerPoint();
        switch (profile.lowSide()) {
            case NORTH -> {
                start = new PlanPoint(room.minXMillimeters(), room.minYMillimeters());
                end = new PlanPoint(room.maxXMillimeters(), room.minYMillimeters());
                arrowCenter = new PlanPoint(room.centerPoint().xMillimeters(), room.minYMillimeters() + room.depthMillimeters() * 0.18);
            }
            case SOUTH -> {
                start = new PlanPoint(room.minXMillimeters(), room.maxYMillimeters());
                end = new PlanPoint(room.maxXMillimeters(), room.maxYMillimeters());
                arrowCenter = new PlanPoint(room.centerPoint().xMillimeters(), room.maxYMillimeters() - room.depthMillimeters() * 0.18);
            }
            case EAST -> {
                start = new PlanPoint(room.maxXMillimeters(), room.minYMillimeters());
                end = new PlanPoint(room.maxXMillimeters(), room.maxYMillimeters());
                arrowCenter = new PlanPoint(room.maxXMillimeters() - room.widthMillimeters() * 0.18, room.centerPoint().yMillimeters());
            }
            case WEST -> {
                start = new PlanPoint(room.minXMillimeters(), room.minYMillimeters());
                end = new PlanPoint(room.minXMillimeters(), room.maxYMillimeters());
                arrowCenter = new PlanPoint(room.minXMillimeters() + room.widthMillimeters() * 0.18, room.centerPoint().yMillimeters());
            }
        }
        graphics.strokeLine(
                toScreenProjectedX(start, 0.0),
                toScreenProjectedY(start, 0.0),
                toScreenProjectedX(end, 0.0),
                toScreenProjectedY(end, 0.0)
        );
        graphics.setLineDashes();
        drawSlopeArrow(graphics, arrowCenter, profile.lowSide());
        graphics.setFill(Color.web("#6b4627"));
        graphics.setFont(Font.font("Menlo", 10));
        graphics.fillText(
                String.format(Locale.GERMAN, "Schräge %.2f m → %.2f m | %.1f°",
                        profile.kneeWallHeight().toMillimeters() / 1000.0,
                        surfaceLayerEffectService.effectiveMaximumCeilingHeightMillimeters(activeLevel.get(), room) / 1000.0,
                        room.slopeAngleDegrees()),
                toScreenProjectedX(room.centerPoint(), 0.0) - 72,
                toScreenProjectedY(room.centerPoint(), 0.0) + 28
        );
    }

    private void drawSlopeArrow(GraphicsContext graphics, PlanPoint arrowCenter, SlopedCeilingSide lowSide) {
        double arrowLength = 28.0;
        double startX = toScreenProjectedX(arrowCenter, 0.0);
        double startY = toScreenProjectedY(arrowCenter, 0.0);
        double endX = startX;
        double endY = startY;
        switch (lowSide) {
            case NORTH -> endY += arrowLength;
            case SOUTH -> endY -= arrowLength;
            case EAST -> endX -= arrowLength;
            case WEST -> endX += arrowLength;
        }
        graphics.strokeLine(startX, startY, endX, endY);
        switch (lowSide) {
            case NORTH -> {
                graphics.strokeLine(endX, endY, endX - 5, endY - 6);
                graphics.strokeLine(endX, endY, endX + 5, endY - 6);
            }
            case SOUTH -> {
                graphics.strokeLine(endX, endY, endX - 5, endY + 6);
                graphics.strokeLine(endX, endY, endX + 5, endY + 6);
            }
            case EAST -> {
                graphics.strokeLine(endX, endY, endX + 6, endY - 5);
                graphics.strokeLine(endX, endY, endX + 6, endY + 5);
            }
            case WEST -> {
                graphics.strokeLine(endX, endY, endX - 6, endY - 5);
                graphics.strokeLine(endX, endY, endX - 6, endY + 5);
            }
        }
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
            graphics.save();
            graphics.setLineCap(javafx.scene.shape.StrokeLineCap.BUTT);
            graphics.setStroke(selected ? Color.web("#f08f3c") : Color.web("#d66b2d"));
            graphics.setLineWidth(Math.max(hostWall.thickness().toMillimeters() * scale() * 0.55, 3.0));
            graphics.strokeLine(
                    toScreenProjectedX(openingStart, 0.0),
                    toScreenProjectedY(openingStart, 0.0),
                    toScreenProjectedX(openingEnd, 0.0),
                    toScreenProjectedY(openingEnd, 0.0)
            );
            graphics.restore();
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
            graphics.save();
            graphics.setLineCap(javafx.scene.shape.StrokeLineCap.BUTT);
            graphics.setStroke(selected ? Color.web("#7bc8eb") : Color.web("#4da8da"));
            graphics.setLineWidth(Math.max(hostWall.thickness().toMillimeters() * scale() * 0.35, 3.0));
            graphics.strokeLine(
                    toScreenProjectedX(openingStart, 0.0),
                    toScreenProjectedY(openingStart, 0.0),
                    toScreenProjectedX(openingEnd, 0.0),
                    toScreenProjectedY(openingEnd, 0.0)
            );
            graphics.restore();
        }
    }

    private void drawEditablePoints(GraphicsContext graphics) {
        if (currentTool() != DrawingTool.EDIT || !projectionService.isPlanView(activeView.get())) {
            return;
        }
        Map<PlanPoint, Integer> wallEndpointCounts = new LinkedHashMap<>();
        for (Wall wall : activeLevel.get().walls()) {
            wallEndpointCounts.merge(wall.axis().start(), 1, Integer::sum);
            wallEndpointCounts.merge(wall.axis().end(), 1, Integer::sum);
        }
        for (Map.Entry<PlanPoint, Integer> entry : wallEndpointCounts.entrySet()) {
            boolean connected = entry.getValue() > 1;
            boolean active = selectedEndpointGroup != null && samePlanPoint(selectedEndpointGroup.anchorPoint(), entry.getKey());
            drawEditablePoint(
                    graphics,
                    entry.getKey(),
                    active ? Color.web("#d97f2f") : CadColorPalette.WALL,
                    active ? 0.68 : connected ? 0.42 : 0.18,
                    active ? 6.5 : 5.0
            );
        }
        for (Door door : activeLevel.get().doors()) {
            Wall wall = activeLevel.get().findWall(door.wallId());
            boolean selected = isSelected(RenderableKind.DOOR, door.id().toString());
            drawOpeningEditablePoints(graphics, wall, door.offsetFromStart(), door.width(), Color.web("#d66b2d"), selected);
        }
        for (WindowElement window : activeLevel.get().windows()) {
            Wall wall = activeLevel.get().findWall(window.wallId());
            boolean selected = isSelected(RenderableKind.WINDOW, window.id().toString());
            drawOpeningEditablePoints(graphics, wall, window.offsetFromStart(), window.width(), Color.web("#4da8da"), selected);
        }
    }

    private void drawEdgeResizeHandles(GraphicsContext graphics) {
        if (currentTool() != DrawingTool.EDIT || !projectionService.isPlanView(activeView.get())) {
            return;
        }
        for (EdgeResizeService.EdgeHandle handle : edgeResizeService.handles(activeLevel.get(), Set.copyOf(selectedSelections))) {
            double x = toScreenProjectedX(handle.position(), 0.0);
            double y = toScreenProjectedY(handle.position(), 0.0);
            boolean active = activeEdgeHandle != null
                    && activeEdgeHandle.kind() == handle.kind()
                    && activeEdgeHandle.elementId().equals(handle.elementId());
            double size = active ? 11.0 : 8.0;
            graphics.save();
            graphics.setFill(active ? Color.web("#d97f2f") : Color.web("#fffaf1"));
            graphics.setStroke(Color.web("#201c18"));
            graphics.setLineWidth(1.4);
            graphics.fillRect(x - size / 2.0, y - size / 2.0, size, size);
            graphics.strokeRect(x - size / 2.0, y - size / 2.0, size, size);
            graphics.restore();
        }
    }

    private void drawOpeningEditablePoints(GraphicsContext graphics, Wall wall, Length offset, Length width, Color color, boolean selected) {
        drawEditablePoint(graphics, wall.axis().pointAt(offset), color, selected ? 0.52 : 0.24, selected ? 6.0 : 5.0);
        drawEditablePoint(graphics, wall.axis().pointAt(offset.add(width)), color, selected ? 0.52 : 0.24, selected ? 6.0 : 5.0);
    }

    private void drawEditablePoint(GraphicsContext graphics, PlanPoint point, Color color, double fillOpacity, double radius) {
        double centerX = toScreenProjectedX(point, 0.0);
        double centerY = toScreenProjectedY(point, 0.0);
        graphics.save();
        graphics.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), fillOpacity));
        graphics.setStroke(Color.web("#201c18"));
        graphics.setLineWidth(1.2);
        graphics.fillOval(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0);
        graphics.strokeOval(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0);
        graphics.restore();
    }

    private boolean samePlanPoint(PlanPoint first, PlanPoint second) {
        return first.distanceTo(second).toMillimeters() < 0.001;
    }

    private void drawRoomObjects(GraphicsContext graphics) {
        if (!showRoomObjects.get()) {
            return;
        }
        for (RoomObject roomObject : activeLevel.get().roomObjects()) {
            if (!roomObject.visible()) {
                continue;
            }
            boolean selected = isSelected(RenderableKind.ROOM_OBJECT, roomObject.id().toString());
            graphics.setStroke(selected ? Color.web("#d97f2f") : Color.web("#356f62"));
            graphics.setFill(selected ? Color.color(0.85, 0.50, 0.18, 0.30) : Color.color(0.22, 0.44, 0.39, 0.22));
            graphics.setLineWidth(1.8);
            double width = roomObject.footprintWidthMillimeters();
            double depth = roomObject.footprintDepthMillimeters();
            if (projectionService.isPlanView(activeView.get())) {
                drawRoomObjectPlan(graphics, roomObject, width, depth);
            } else {
                double x = toScreenProjectedX(roomObject.center(), 0.0) - width * scale() / 2.0;
                double y = toScreenProjectedY(roomObject.center(), roomObject.height().toMillimeters());
                graphics.strokeRect(x, y, width * scale(), roomObject.height().toMillimeters() * scale());
            }
        }
    }

    private void drawRoomObjectPlan(GraphicsContext graphics, RoomObject roomObject, double width, double depth) {
        double x = toScreenX(roomObject.minXMillimeters());
        double y = toScreenY(roomObject.minYMillimeters());
        double w = width * scale();
        double h = depth * scale();
        switch (roomObject.shape()) {
            case CIRCLE, OVAL -> {
                graphics.fillOval(x, y, w, h);
                graphics.strokeOval(x, y, w, h);
            }
            case HALF_ROUND -> {
                graphics.fillRect(x, y + h / 2.0, w, h / 2.0);
                graphics.strokeRect(x, y + h / 2.0, w, h / 2.0);
                graphics.strokeArc(x, y, w, h, 0.0, 180.0, javafx.scene.shape.ArcType.OPEN);
            }
            case QUARTER_CIRCLE -> {
                graphics.fillRect(x, y, w, h);
                graphics.strokeArc(x, y, w * 2.0, h * 2.0, 180.0, 90.0, javafx.scene.shape.ArcType.OPEN);
                graphics.strokeLine(x, y + h, x, y);
                graphics.strokeLine(x, y + h, x + w, y + h);
            }
            case RECTANGLE -> {
                graphics.fillRect(x, y, w, h);
                graphics.strokeRect(x, y, w, h);
            }
        }
    }

    private void drawStaircases(GraphicsContext graphics) {
        for (Staircase staircase : activeLevel.get().staircases()) {
            boolean selected = isSelected(RenderableKind.STAIR, staircase.id().toString());
            graphics.setStroke(selected ? Color.web("#8a6848") : Color.web("#5e503f"));
            graphics.setFill(selected ? Color.color(0.63, 0.47, 0.27, 0.24) : Color.color(0.52, 0.46, 0.37, 0.16));
            graphics.setLineWidth(2.0);
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
        double startLanding = staircase.startLandingWidth().toMillimeters();
        double endLanding = staircase.endLandingWidth().toMillimeters();
        double runLength = staircase.heightMillimeters() - startLanding - endLanding;
        double stepDepth = runLength / staircase.regularStepCount();
        if (startLanding > 0) {
            strokeLocalLine(graphics, staircase, 0, startLanding, staircase.widthMillimeters(), startLanding);
        }
        for (int step = 1; step < staircase.regularStepCount(); step++) {
            double localY = startLanding + stepDepth * step;
            strokeLocalLine(graphics, staircase, 0, localY, staircase.widthMillimeters(), localY);
        }
        if (endLanding > 0) {
            double localY = staircase.heightMillimeters() - endLanding;
            strokeLocalLine(graphics, staircase, 0, localY, staircase.widthMillimeters(), localY);
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
        double startTopY = toScreenProjectedY(wall.axis().start(), wall.heightAtStart());
        double endTopY = toScreenProjectedY(wall.axis().end(), wall.heightAtEnd());
        double[] xPoints = {startX, endX, endX, startX};
        double[] yPoints = {floorY, floorY, endTopY, startTopY};
        graphics.setFill(selected ? Color.color(0.85, 0.57, 0.22, 0.24) : Color.color(0.23, 0.39, 0.54, 0.18));
        graphics.fillPolygon(xPoints, yPoints, xPoints.length);
        graphics.setStroke(selected ? Color.web("#d97f2f") : CadColorPalette.WALL);
        graphics.setLineWidth(2.0);
        graphics.strokePolygon(xPoints, yPoints, xPoints.length);
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
        double topY = toScreenVertical(-surfaceLayerEffectService.effectiveMaximumCeilingHeightMillimeters(activeLevel.get(), room));
        if (isSlopeVisibleInCurrentElevation(room)) {
            drawSlopedRoomElevation(graphics, room, left, right, floorY, topY);
            return;
        }
        graphics.setFill(Color.color(0.77, 0.64, 0.45, 0.16));
        graphics.fillRect(Math.min(left, right), Math.min(floorY, topY), Math.max(Math.abs(right - left), 3.0), Math.max(Math.abs(floorY - topY), 3.0));
        graphics.setStroke(Color.color(0.55, 0.43, 0.25, 0.65));
        graphics.setLineWidth(1.6);
        graphics.strokeRect(Math.min(left, right), Math.min(floorY, topY), Math.max(Math.abs(right - left), 3.0), Math.max(Math.abs(floorY - topY), 3.0));
    }

    private boolean isSlopeVisibleInCurrentElevation(Room room) {
        return switch (activeView.get()) {
            case EAST, WEST -> room.slopeVisibleInEastWestView();
            case NORTH, SOUTH -> room.slopeVisibleInNorthSouthView();
            default -> false;
        };
    }

    private void drawSlopedRoomElevation(GraphicsContext graphics, Room room, double left, double right, double floorY, double topY) {
        if (room.ceilingVertexHeightsProfile().isPresent()) {
            drawPolygonalRoomElevation(graphics, room, floorY);
            return;
        }
        double lowY = toScreenVertical(-surfaceLayerEffectService.effectiveMinimumCeilingHeightMillimeters(activeLevel.get(), room));
        boolean risesToRight = switch (activeView.get()) {
            case EAST -> room.slopedCeilingProfile().map(profile -> profile.lowSide() == SlopedCeilingSide.NORTH).orElse(false);
            case WEST -> room.slopedCeilingProfile().map(profile -> profile.lowSide() == SlopedCeilingSide.SOUTH).orElse(false);
            case NORTH -> room.slopedCeilingProfile().map(profile -> profile.lowSide() == SlopedCeilingSide.WEST).orElse(false);
            case SOUTH -> room.slopedCeilingProfile().map(profile -> profile.lowSide() == SlopedCeilingSide.EAST).orElse(false);
            default -> false;
        };
        double firstTopY = risesToRight ? lowY : topY;
        double secondTopY = risesToRight ? topY : lowY;
        double[] xPoints = {left, right, right, left};
        double[] yPoints = {floorY, floorY, secondTopY, firstTopY};
        graphics.setFill(Color.color(0.77, 0.64, 0.45, 0.16));
        graphics.fillPolygon(xPoints, yPoints, xPoints.length);
        graphics.setStroke(Color.color(0.55, 0.43, 0.25, 0.72));
        graphics.setLineWidth(1.8);
        graphics.strokePolygon(xPoints, yPoints, xPoints.length);
    }

    private void drawPolygonalRoomElevation(GraphicsContext graphics, Room room, double floorY) {
        java.util.TreeMap<Long, Double> topProfile = new java.util.TreeMap<>();
        for (int index = 0; index < room.outline().size(); index++) {
            PlanPoint point = room.outline().get(index);
            addElevationSample(topProfile, point, surfaceLayerEffectService.effectiveHeightAt(activeLevel.get(), room, point));
            PlanPoint next = room.outline().get((index + 1) % room.outline().size());
            PlanPoint midpoint = new PlanPoint(
                    (point.xMillimeters() + next.xMillimeters()) / 2.0,
                    (point.yMillimeters() + next.yMillimeters()) / 2.0
            );
            addElevationSample(topProfile, midpoint, surfaceLayerEffectService.effectiveHeightAt(activeLevel.get(), room, midpoint));
        }
        if (topProfile.size() < 2) {
            return;
        }
        double[] xPoints = new double[topProfile.size() * 2];
        double[] yPoints = new double[topProfile.size() * 2];
        int pointIndex = 0;
        for (Map.Entry<Long, Double> entry : topProfile.entrySet()) {
            xPoints[pointIndex] = toScreenHorizontal(entry.getKey());
            yPoints[pointIndex] = toScreenVertical(-entry.getValue());
            pointIndex++;
        }
        List<Map.Entry<Long, Double>> entries = new ArrayList<>(topProfile.entrySet());
        for (int reverseIndex = entries.size() - 1; reverseIndex >= 0; reverseIndex--) {
            xPoints[pointIndex] = toScreenHorizontal(entries.get(reverseIndex).getKey());
            yPoints[pointIndex] = floorY;
            pointIndex++;
        }
        graphics.setFill(Color.color(0.77, 0.64, 0.45, 0.16));
        graphics.fillPolygon(xPoints, yPoints, xPoints.length);
        graphics.setStroke(Color.color(0.55, 0.43, 0.25, 0.72));
        graphics.setLineWidth(1.8);
        graphics.strokePolygon(xPoints, yPoints, xPoints.length);
    }

    private void addElevationSample(java.util.TreeMap<Long, Double> topProfile, PlanPoint point, double ceilingHeightMillimeters) {
        long horizontal = Math.round(projectHorizontal(point, 0.0));
        topProfile.merge(horizontal, ceilingHeightMillimeters, Math::max);
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
        if (currentTool() == DrawingTool.STAIR) {
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
        drawDimensionLabel(graphics, segment, text, 0.0);
    }

    private void drawDimensionLabel(GraphicsContext graphics, PlanSegment segment, String text, double normalOffset) {
        double startX = toScreenProjectedX(segment.start(), 0.0);
        double startY = toScreenProjectedY(segment.start(), 0.0);
        double endX = toScreenProjectedX(segment.end(), 0.0);
        double endY = toScreenProjectedY(segment.end(), 0.0);
        double midX = (startX + endX) / 2.0;
        double midY = (startY + endY) / 2.0;
        double directionX = endX - startX;
        double directionY = endY - startY;
        double directionLength = Math.max(1.0, Math.hypot(directionX, directionY));
        if (currentDimensionStandard() == DimensionStandard.DIN_EN_ISO_7519_2025_01) {
            double isoOffset = Math.abs(normalOffset) < 0.001 ? 24.0 : normalOffset;
            var layout = dimensionLineLayoutService.layout(startX, startY, endX, endY, isoOffset);
            drawIsoDimensionLines(graphics, layout, directionX / directionLength, directionY / directionLength);
            midX = layout.textX();
            midY = layout.textY();
        } else {
            midX += -directionY / directionLength * normalOffset;
            midY += directionX / directionLength * normalOffset;
        }
        graphics.setFill(CadColorPalette.DIMENSION_TEXT);
        graphics.setFont(Font.font("Menlo", 12));
        graphics.fillText(text, midX + 8.0, midY - 8.0);
    }

    private DimensionStandard currentDimensionStandard() {
        return useIsoDimensions.get()
                ? DimensionStandard.DIN_EN_ISO_7519_2025_01
                : DimensionStandard.EXISTING;
    }

    private void drawIsoDimensionLines(
            GraphicsContext graphics,
            DimensionLineLayoutService.DimensionLineLayout layout,
            double directionX,
            double directionY
    ) {
        graphics.save();
        graphics.setStroke(CadColorPalette.DIMENSION_TEXT);
        graphics.setLineWidth(0.8);
        graphics.strokeLine(layout.firstExtensionStartX(), layout.firstExtensionStartY(), layout.firstExtensionEndX(), layout.firstExtensionEndY());
        graphics.strokeLine(layout.secondExtensionStartX(), layout.secondExtensionStartY(), layout.secondExtensionEndX(), layout.secondExtensionEndY());
        graphics.strokeLine(layout.lineStartX(), layout.lineStartY(), layout.lineEndX(), layout.lineEndY());
        double tickX = (directionX - directionY) * 4.0;
        double tickY = (directionY + directionX) * 4.0;
        graphics.strokeLine(layout.lineStartX() - tickX, layout.lineStartY() - tickY, layout.lineStartX() + tickX, layout.lineStartY() + tickY);
        graphics.strokeLine(layout.lineEndX() - tickX, layout.lineEndY() - tickY, layout.lineEndX() + tickX, layout.lineEndY() + tickY);
        graphics.restore();
    }

    private void drawViewOverlay(GraphicsContext graphics) {
        Font titleFont = Font.font("Menlo", 15);
        Font descFont = Font.font("Menlo", 11);
        String titleText = "Ansicht: " + activeView.get().label();
        String descText = activeView.get().overlayDescription();
        javafx.scene.text.Text titleMeasure = new javafx.scene.text.Text(titleText);
        titleMeasure.setFont(titleFont);
        javafx.scene.text.Text descMeasure = new javafx.scene.text.Text(descText);
        descMeasure.setFont(descFont);
        double titleWidth = titleMeasure.getLayoutBounds().getWidth();
        double descWidth = descMeasure.getLayoutBounds().getWidth();
        double maxWidth = Math.max(titleWidth, descWidth) + 32.0;
        double height = 52.0;
        graphics.setFill(Color.color(0.18, 0.16, 0.13, 0.78));
        graphics.fillRoundRect(16, 16, maxWidth, height, 18, 18);
        graphics.setFill(Color.WHITE);
        graphics.setFont(titleFont);
        graphics.fillText(titleText, 28, 40);
        graphics.setFont(descFont);
        graphics.fillText(descText, 28, 58);
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

    private Length currentEndpointHeight() {
        return parseLength(endpointHeightField, endpointHeightUnit.getValue()).orElse(currentWallHeight());
    }

    private String currentRoomName() {
        String roomName = roomNameField.getText();
        if (roomName == null || roomName.isBlank()) {
            return "Raum";
        }
        return roomName.trim();
    }

    private AutoRoomGenerationService.RoomDefaults currentRoomDefaults() {
        return new AutoRoomGenerationService.RoomDefaults(
                currentRoomName(),
                currentRoomHeight(),
                currentFloorThickness(),
                currentCeilingThickness(),
                currentSlopedCeilingProfile()
        );
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

    private SlopedCeilingProfile currentSlopedCeilingProfile() {
        if (!"Mit Dachschräge".equals(slopedCeilingModeSelector.getValue())) {
            return null;
        }
        return new SlopedCeilingProfile(
                Optional.ofNullable(slopedCeilingSideSelector.getValue()).orElse(SlopedCeilingSide.NORTH),
                parseLength(kneeWallHeightField, kneeWallHeightUnit.getValue()).orElse(Length.of(1.0, LengthUnit.METER))
        );
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

    private Length currentStairStartLanding() {
        return parseLength(stairStartLandingField, stairStartLandingUnit.getValue()).orElse(Length.ofMillimeters(0));
    }

    private Length currentStairEndLanding() {
        return parseLength(stairEndLandingField, stairEndLandingUnit.getValue()).orElse(Length.ofMillimeters(0));
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
        viewLabel.setText("Arbeitsbereich: " + activeWorkspaceMode.get().label() + " | 2D-Ansicht: " + activeView.get().label() + " | Etage: " + activeLevel.get().name());
        zoomLabel.setText(String.format(Locale.GERMAN, "Zoom: %.2f x", zoom));
        cursorLabel.setText(String.format(Locale.GERMAN, "Cursor: %.2f m / %.2f m", lastCursor.xMillimeters() / 1000.0, lastCursor.yMillimeters() / 1000.0));
        if (previewSegment == null) {
            if (selectedEndpointGroup != null) {
                draftLabel.setText("Werkzeug: " + currentTool().label() + " | Wandecke ausgewählt: Ziehen verschiebt sie gemeinsam, `Eckhöhe anwenden` setzt ihre Höhe.");
            } else {
                draftLabel.setText(statusHintForCurrentTool());
            }
        } else {
            draftLabel.setText("Zeichnen: " + previewSegment.length().format(LengthUnit.METER, 2) + " | " + previewSegment.angle().format());
        }
    }

    private String statusHintForCurrentTool() {
        return switch (currentTool()) {
            case EDIT -> "Werkzeug: Bearbeiten | Linksklick wählt aus, Alt+Linksklick schaltet überdeckte Treffer durch, Rechtsziehen verschiebt die Ansicht, Alt+Rechtsklick entfernt Hilfslinien.";
            case WALL -> "Werkzeug: Wand | Linksklick startet und beendet Wände, Shift erlaubt freie Winkel.";
            case DOOR -> "Werkzeug: Tür | Linksklick auf eine Wand platziert die Tür mit den aktuellen Maßen.";
            case WINDOW -> "Werkzeug: Fenster | Linksklick auf eine Wand platziert das Fenster mit den aktuellen Maßen.";
            case STAIR -> "Werkzeug: Treppe | Rechteck aufziehen platziert die Treppe mit dem gewählten Preset.";
            case OBJECT -> "Werkzeug: Objekt | Linksklick in einen Raum platziert das ausgewählte Objekt-Preset.";
        };
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
                    fitCurrentViewToContent();
                });
    }

    private DrawingTool currentTool() {
        return Optional.ofNullable(toolSelector.getValue()).orElse(DrawingTool.WALL);
    }

    private PlanPoint snapDrawingPoint(PlanPoint point, DraftingConstraints constraints) {
        GuideSnapTargets targets = currentTool() == DrawingTool.WALL || currentTool() == DrawingTool.EDIT
                ? currentAlignmentSnapTargets(Set.of())
                : currentGuideSnapTargets();
        return snapService.snap(point, constraints, activeLevel.get().walls(), targets);
    }

    private GuideSnapTargets currentGuideSnapTargets() {
        if (!snapToGuides.get()) {
            return GuideSnapTargets.empty();
        }
        return new GuideSnapTargets(
                guideLines.stream()
                        .filter(guideLine -> guideLine.orientation() == GuideOrientation.VERTICAL)
                        .map(GuideLine::worldMillimeters)
                        .toList(),
                guideLines.stream()
                        .filter(guideLine -> guideLine.orientation() == GuideOrientation.HORIZONTAL)
                        .map(GuideLine::worldMillimeters)
                        .toList()
        );
    }

    private GuideSnapTargets currentAlignmentSnapTargets(Set<UUID> excludedWallIds) {
        GuideSnapTargets guideTargets = currentGuideSnapTargets();
        GuideSnapTargets wallTargets = snapToWalls.get()
                ? wallSnapService.targets(activeLevel.get().walls(), excludedWallIds)
                : GuideSnapTargets.empty();
        return new GuideSnapTargets(
                java.util.stream.Stream.concat(guideTargets.verticalGuides().stream(), wallTargets.verticalGuides().stream()).distinct().toList(),
                java.util.stream.Stream.concat(guideTargets.horizontalGuides().stream(), wallTargets.horizontalGuides().stream()).distinct().toList()
        );
    }

    private Door snapDoorToGuides(Door door) {
        if (!snapToGuides.get()) {
            return door;
        }
        Wall wall = activeLevel.get().findWall(door.wallId());
        return door.withOffset(guideSnapService.snapOpeningOffset(
                wall,
                door.offsetFromStart(),
                door.width(),
                currentGuideSnapTargets(),
                SNAP_TOLERANCE
        ));
    }

    private WindowElement snapWindowToGuides(WindowElement window) {
        if (!snapToGuides.get()) {
            return window;
        }
        Wall wall = activeLevel.get().findWall(window.wallId());
        return window.withOffset(guideSnapService.snapOpeningOffset(
                wall,
                window.offsetFromStart(),
                window.width(),
                currentGuideSnapTargets(),
                SNAP_TOLERANCE
        ));
    }

    private Wall openingDragWall() {
        Optional<UUID> wallId = activeLevel.get().doors().stream()
                .filter(door -> door.id().equals(openingDragId))
                .map(Door::wallId)
                .findFirst()
                .or(() -> activeLevel.get().windows().stream()
                        .filter(window -> window.id().equals(openingDragId))
                        .map(WindowElement::wallId)
                        .findFirst());
        return activeLevel.get().findWall(wallId.orElseThrow());
    }

    private void placeDoor(PlanPoint clickPoint) {
        openingPlacementService.placeDoor(
                        clickPoint,
                        activeLevel.get().walls(),
                        currentDoorWidth(),
                currentDoorHeight(),
                currentThresholdHeight(),
                SNAP_TOLERANCE)
                .map(this::snapDoorToGuides)
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
                .map(this::snapWindowToGuides)
                .ifPresent(window -> {
                    rememberStateForUndo();
                    activeLevel.get().addWindow(window);
                    selectSingle(new SelectionKey(RenderableKind.WINDOW, activeLevel.get().name(), window.id().toString()));
                    markThreeDDirty();
                });
    }

    private void placeRoomObject(PlanPoint clickPoint) {
        RoomObjectPreset preset = roomObjectPresetSelector.getValue();
        if (preset == null) {
            draftLabel.setText("Kein Objekt-Preset ausgewählt.");
            return;
        }
        if (activeLevel.get().rooms().stream().noneMatch(room -> containsPoint(room, clickPoint))) {
            draftLabel.setText("Objekte können nur innerhalb eines Raums platziert werden.");
            return;
        }
        rememberStateForUndo();
        RoomObject roomObject = RoomObject.create(
                preset.id(),
                preset.name(),
                preset.type(),
                preset.shape(),
                clickPoint,
                preset.width(),
                preset.depth(),
                preset.height(),
                preset.mountingMode(),
                preset.source()
        );
        activeLevel.get().addRoomObject(roomObject);
        selectSingle(new SelectionKey(RenderableKind.ROOM_OBJECT, activeLevel.get().name(), roomObject.id().toString()));
        markThreeDDirty();
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
        guideDistanceService.nearestGuide(guideLines, clickPoint, SNAP_TOLERANCE)
                .ifPresent(guideLine -> {
                    rememberStateForUndo();
                    guideLines.remove(guideLine);
                });
        render();
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
        String levelName = exchangeFileNameService.stripRepeatedExtension(Path.of(activeLevel.get().name().replace(' ', '_')), ".dxf");
        fileChooser.setInitialFileName(levelName);
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
        } catch (Exception exception) {
            draftLabel.setText("DXF-Export fehlgeschlagen: " + exception.getMessage());
        }
    }

    private void exportProjectAsDxf() {
        FileChooser fileChooser = createDxfFileChooser();
        String projectName = exchangeFileNameService.stripRepeatedExtension(Path.of(project.name().replace(' ', '_')), ".dxf");
        fileChooser.setInitialFileName(projectName + "_Gebäude");
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
        } catch (Exception exception) {
            draftLabel.setText("Gebäude-DXF-Export fehlgeschlagen: " + exception.getMessage());
        }
    }

    private void showSurfaceMaterialReportWindow() {
        String markdown = surfaceMaterialListService.create(project).toMarkdown();
        WebView reportView = new WebView();
        reportView.getEngine().loadContent(markdownHtmlRenderer.renderDocument(markdown));
        VBox.setVgrow(reportView, Priority.ALWAYS);
        Button exportButton = new Button("Markdown exportieren");
        exportButton.setOnAction(event -> exportSurfaceMaterialReportMarkdown());
        applyTooltip(exportButton, "Exportiert genau diese Materialliste als Markdown-Datei.");
        Button printButton = new Button("Drucken");
        printButton.setOnAction(event -> printSurfaceMaterialReport(reportView));
        applyTooltip(printButton, "Druckt die gerenderte Materialliste so, wie sie in diesem Fenster angezeigt wird.");
        HBox actions = new HBox(8.0, printButton, exportButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox container = new VBox(10.0, reportView, actions);
        container.setPadding(new Insets(12));
        Stage stage = new Stage();
        stage.setTitle("Materialliste Beläge");
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setScene(new Scene(container, 920, 680));
        stage.show();
    }

    private void exportConstructionDrawingPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Maßstabgerechte Bauzeichnung als PDF speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Dateien", "*.pdf"));
        String projectName = exchangeFileNameService.stripRepeatedExtension(Path.of(project.name().replace(' ', '_')), ".dxf");
        fileChooser.setInitialFileName(projectName + "_Bauzeichnung.pdf");
        Window owner = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        try {
            Path target = exchangeFileNameService.ensureSingleExtension(file.toPath(), ".pdf");
            constructionDrawingPdfService.export(project, target);
            draftLabel.setText("Bauzeichnungs-PDF exportiert: " + target.getFileName());
        } catch (Exception exception) {
            draftLabel.setText("PDF-Export fehlgeschlagen: " + exception.getMessage());
        }
    }

    private void showHelpWindow() {
        WebView helpView = new WebView();
        helpView.getEngine().loadContent(markdownHtmlRenderer.renderDocument(helpContentService.createMarkdown()));
        VBox.setVgrow(helpView, Priority.ALWAYS);
        Button printButton = new Button("Drucken");
        printButton.setOnAction(event -> printWebView(helpView, "Hilfe und Keymap"));
        applyTooltip(printButton, "Druckt die vollständige Hilfe und Keymap. Im Druckdialog kann auch ein PDF-Drucker gewählt werden.");
        HBox actions = new HBox(8.0, printButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox container = new VBox(10.0, helpView, actions);
        container.setPadding(new Insets(12));
        Stage stage = new Stage();
        stage.setTitle("CADas-Hilfe und Keymap");
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setScene(new Scene(container, 920, 680));
        stage.show();
    }

    private void printSurfaceMaterialReport(WebView reportView) {
        printWebView(reportView, "Materialliste");
    }

    private void printWebView(WebView reportView, String documentName) {
        PrinterJob printerJob = PrinterJob.createPrinterJob();
        if (printerJob == null) {
            draftLabel.setText("Kein Drucker verfügbar.");
            return;
        }
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (!printerJob.showPrintDialog(owner)) {
            draftLabel.setText("Druck abgebrochen.");
            return;
        }
        reportView.getEngine().print(printerJob);
        printerJob.endJob();
        draftLabel.setText(documentName + " an Drucker übergeben.");
    }

    private void exportSurfaceMaterialReportMarkdown() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Materialliste als Markdown speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown-Dateien", "*.md"));
        String projectName = exchangeFileNameService.stripRepeatedExtension(Path.of(project.name().replace(' ', '_')), ".dxf");
        fileChooser.setInitialFileName(projectName + "_Materialliste_Beläge");
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            return;
        }
        exportSurfaceMaterialReportMarkdown(file.toPath());
    }

    private void exportSurfaceMaterialReportMarkdown(Path targetFile) {
        try {
            Path exportPath = exchangeFileNameService.ensureSingleExtension(targetFile, ".md");
            Files.writeString(exportPath, surfaceMaterialListService.create(project).toMarkdown());
            draftLabel.setText("Materialliste exportiert: " + exportPath.getFileName());
        } catch (Exception exception) {
            draftLabel.setText("Materiallisten-Export fehlgeschlagen: " + exception.getMessage());
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
            importedLevel.replaceRooms(autoRoomGenerationService.synchronize(importedLevel, currentRoomDefaults()));
            project.addLevel(importedLevel);
            availableLevels.add(importedLevel);
            activateLevel(importedLevel);
            fitCurrentViewToContent();
            draftLabel.setText("DXF importiert: " + sourceFile.getFileName());
        } catch (Exception exception) {
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
            String projectName = exchangeFileNameService.stripRepeatedExtension(sourceFile, ".dxf");
            ProjectModel importedProject = projectExchangeService.importProject(sourceFile, projectName);
            importedProject.levels().forEach(level -> level.replaceRooms(autoRoomGenerationService.synchronize(level, currentRoomDefaults())));
            project.replaceWith(importedProject);
            availableLevels.setAll(project.levels());
            guideLines.clear();
            clearSelectionsInternal();
            activateLevel(project.primaryLevel());
            markThreeDDirty();
            fitCurrentViewToContent();
            draftLabel.setText("Gebäude-DXF importiert: " + sourceFile.getFileName());
        } catch (Exception exception) {
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
        FileChooser fileChooser = createPartLibraryFileChooser();
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        importPartLibrary(file.toPath());
    }

    private FileChooser createPartLibraryFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Teilebibliothek auswählen");
        fileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Unterstützte Bibliotheken", "*.cadasparts", "*.dwg", "*.DWG"),
                new FileChooser.ExtensionFilter("CADas Teilebibliothek", "*.cadasparts"),
                new FileChooser.ExtensionFilter("DWG-Bibliothek", "*.dwg", "*.DWG"),
                new FileChooser.ExtensionFilter("Alle Dateien", "*.*")
        );
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().getFirst());
        return fileChooser;
    }

    private void importPartLibrary(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".dwg")) {
            registerDwgLibrary(sourceFile, true);
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
        } catch (Exception exception) {
            draftLabel.setText("Teilebibliothek fehlgeschlagen: " + exception.getMessage());
        }
    }

    private void updateCadLibrarySummary() {
        if (cadLibraryReferences.isEmpty()) {
            cadLibrarySummaryLabel.setText("Keine externen CAD-Bibliotheken registriert.");
            return;
        }
        cadLibrarySummaryLabel.setText(cadLibraryReferences.stream()
                .map(this::cadLibrarySummaryLine)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("Keine externen CAD-Bibliotheken registriert."));
    }

    private String cadLibrarySummaryLine(Path path) {
        DwgLibraryAnalysis analysis = dwgAnalysesByPath.get(path.toAbsolutePath().normalize());
        if (analysis == null) {
            return "• " + path.getFileName();
        }
        if (!analysis.successful()) {
            return "• " + path.getFileName() + " | nicht lesbar: " + analysis.summary();
        }
        long usableBlocks = analysis.blocks().stream()
                .filter(DwgBlockDefinition::hasGeometry)
                .count();
        return "• " + path.getFileName() + " | " + usableBlocks + " Blöcke";
    }

    private void registerDwgLibrary(Path sourceFile, boolean askBeforeOverwrite) {
        Path registeredFile = configuredCadLibraryPath(sourceFile, askBeforeOverwrite);
        Path normalizedRegisteredFile = registeredFile.toAbsolutePath().normalize();
        if (!cadLibraryReferences.contains(normalizedRegisteredFile)) {
            cadLibraryReferences.add(normalizedRegisteredFile);
        }
        DwgLibraryAnalysis analysis = analyzeDwgLibrary(normalizedRegisteredFile, false);
        SurfaceCoveringPreset dwgPreset = surfaceCoveringPresetService.fromDwg(normalizedRegisteredFile);
        registerSurfacePreset(dwgPreset);
        dwgBlockCatalogService.loadCatalog(normalizedRegisteredFile).forEach(blockName -> registerDwgBlockPreset(normalizedRegisteredFile, blockName));
        applyDwgBlockFilter();
        updateCadLibrarySummary();
        if (surfacePresetSelector.getValue() == null) {
            surfacePresetSelector.setValue(dwgPreset);
        }
        draftLabel.setText("DWG-Bibliothek geladen: " + normalizedRegisteredFile.getFileName() + " | " + analysis.summary());
    }

    private DwgLibraryAnalysis analyzeDwgLibrary(Path sourceFile, boolean force) {
        Path normalizedSource = sourceFile.toAbsolutePath().normalize();
        if (!force && dwgAnalysesByPath.containsKey(normalizedSource)) {
            return dwgAnalysesByPath.get(normalizedSource);
        }
        DwgLibraryAnalysis analysis = dwgLibraryAnalyzer.analyze(normalizedSource);
        dwgAnalysesByPath.put(normalizedSource, analysis);
        dwgStatusLabel.setText(normalizedSource.getFileName() + ": " + analysis.summary());
        return analysis;
    }

    private Path configuredCadLibraryPath(Path sourceFile, boolean askBeforeOverwrite) {
        boolean overwrite = askBeforeOverwrite && shouldOverwriteConfiguredCadLibrary(sourceFile);
        try {
            return userSurfacePresetLibrary.copyCadLibrary(sourceFile, overwrite);
        } catch (IOException exception) {
            draftLabel.setText("DWG-Bibliothek konnte nicht in das Belagsverzeichnis übernommen werden: " + exception.getMessage());
            return sourceFile.toAbsolutePath().normalize();
        }
    }

    private boolean shouldOverwriteConfiguredCadLibrary(Path sourceFile) {
        Path targetFile = userSurfacePresetLibrary.libraryDirectory().resolve(sourceFile.getFileName()).toAbsolutePath().normalize();
        if (!Files.exists(targetFile) || isSameFile(sourceFile, targetFile)) {
            return false;
        }
        return confirmOverwrite(
                "DWG-Bibliothek überschreiben",
                "Die DWG-Bibliothek `" + sourceFile.getFileName() + "` ist im Belagsverzeichnis bereits vorhanden.",
                "Soll die vorhandene Datei durch die neu gewählte DWG ersetzt werden?"
        );
    }

    private boolean isSameFile(Path first, Path second) {
        try {
            return Files.exists(first) && Files.exists(second) && Files.isSameFile(first, second);
        } catch (IOException exception) {
            return false;
        }
    }

    private void addDwgBlockPreset() {
        String blockName = dwgBlockNameField.getText() == null ? "" : dwgBlockNameField.getText().trim();
        if (blockName.isBlank()) {
            draftLabel.setText("Bitte zuerst einen DWG-Blocknamen eintragen.");
            return;
        }
        Path dwgLibrary = currentDwgLibraryPath().orElse(null);
        if (dwgLibrary == null) {
            draftLabel.setText("Bitte zuerst eine DWG-Bibliothek laden oder ein DWG-Preset auswählen.");
            return;
        }
        SurfaceCoveringPreset preset = registerDwgBlockPreset(dwgLibrary, blockName);
        surfacePresetSelector.setValue(preset);
        draftLabel.setText("DWG-Block als Oberflächen-Preset registriert: " + blockName);
    }

    private SurfaceCoveringPreset registerDwgBlockPreset(Path sourceFile, String blockName) {
        SurfaceCoveringPreset preset = findAnalyzedDwgBlock(sourceFile, blockName)
                .map(surfaceCoveringPresetService::fromDwgBlock)
                .orElseGet(() -> surfaceCoveringPresetService.fromDwgBlock(sourceFile, blockName));
        registerSurfacePreset(preset);
        return availableSurfacePresets.stream()
                .filter(candidate -> candidate.coveringSource().equals(preset.coveringSource()))
                .findFirst()
                .orElse(preset);
    }

    private Optional<DwgBlockDefinition> findAnalyzedDwgBlock(Path sourceFile, String blockName) {
        Path normalizedSource = sourceFile.toAbsolutePath().normalize();
        DwgLibraryAnalysis analysis = dwgAnalysesByPath.get(normalizedSource);
        if (analysis == null) {
            analysis = analyzeDwgLibrary(normalizedSource, false);
        }
        String normalizedBlockName = blockName == null ? "" : blockName.trim();
        return analysis.blocks().stream()
                .filter(block -> block.name().equalsIgnoreCase(normalizedBlockName))
                .findFirst();
    }

    private void addSelectedDwgBlockAsSurfacePreset() {
        DwgBlockDefinition block = dwgBlockSelector.getValue();
        if (block == null || !block.hasGeometry()) {
            draftLabel.setText("Bitte zuerst einen DWG-Block mit auswertbarer Geometrie auswählen.");
            return;
        }
        SurfaceCoveringPreset preset = surfaceCoveringPresetService.fromDwgBlock(block);
        registerSurfacePreset(preset);
        surfacePresetSelector.setValue(preset);
        dwgBlockNameField.setText(block.name());
        applySurfacePreset(preset);
        draftLabel.setText("DWG-Block als Belag übernommen: " + block.name());
    }

    private void addSelectedDwgBlockAsObjectPreset() {
        DwgBlockDefinition block = dwgBlockSelector.getValue();
        if (block == null || !block.hasGeometry()) {
            draftLabel.setText("Bitte zuerst einen DWG-Block mit auswertbarer Geometrie auswählen.");
            return;
        }
        RoomObjectMountingMode mountingMode = Optional.ofNullable(dwgObjectFloorModeSelector.getValue()).orElse(RoomObjectMountingMode.STANDS_ON_COVERING);
        RoomObjectPreset preset = roomObjectPresetService.fromDwgBlock(block, mountingMode);
        registerRoomObjectPreset(preset);
        roomObjectPresetSelector.setValue(preset);
        toolSelector.setValue(DrawingTool.OBJECT);
        draftLabel.setText("DWG-Block als Objekt übernommen: " + block.name());
    }

    private void registerRoomObjectPreset(RoomObjectPreset preset) {
        for (int index = 0; index < availableRoomObjectPresets.size(); index++) {
            RoomObjectPreset existing = availableRoomObjectPresets.get(index);
            if (existing.source().equals(preset.source()) || existing.id().equals(preset.id())) {
                availableRoomObjectPresets.set(index, preset);
                return;
            }
        }
        availableRoomObjectPresets.add(preset);
    }

    private void refreshCurrentDwgLibraryAnalysis() {
        Optional<Path> currentLibrary = currentDwgLibraryPath();
        if (currentLibrary.isEmpty()) {
            draftLabel.setText("Bitte zuerst eine DWG-Bibliothek laden oder auswählen.");
            return;
        }
        DwgLibraryAnalysis analysis = analyzeDwgLibrary(currentLibrary.get(), true);
        applyDwgBlockFilter();
        updateCadLibrarySummary();
        draftLabel.setText("DWG-Bibliothek geprüft: " + analysis.summary());
    }

    private void applyDwgBlockFilter() {
        String filter = Optional.ofNullable(dwgBlockSearchField.getText()).orElse("").trim().toLowerCase(Locale.GERMAN);
        DwgBlockDefinition previousSelection = dwgBlockSelector.getValue();
        List<DwgBlockDefinition> filteredBlocks = dwgAnalysesByPath.values().stream()
                .flatMap(analysis -> analysis.blocks().stream())
                .filter(block -> blockMatchesFilter(block, filter))
                .toList();
        availableDwgBlocks.setAll(filteredBlocks);
        if (previousSelection != null && filteredBlocks.stream().anyMatch(block -> block.sourceReference().equals(previousSelection.sourceReference()))) {
            dwgBlockSelector.setValue(filteredBlocks.stream()
                    .filter(block -> block.sourceReference().equals(previousSelection.sourceReference()))
                    .findFirst()
                    .orElse(null));
        } else if (!filteredBlocks.isEmpty()) {
            dwgBlockSelector.setValue(filteredBlocks.getFirst());
        } else {
            dwgBlockSelector.setValue(null);
        }
        refreshDwgBlockPreviewAndDetails();
        updateActionButtons();
    }

    private boolean blockMatchesFilter(DwgBlockDefinition block, String filter) {
        if (filter.isBlank()) {
            return true;
        }
        return block.name().toLowerCase(Locale.GERMAN).contains(filter)
                || block.sourceFile().getFileName().toString().toLowerCase(Locale.GERMAN).contains(filter)
                || block.layers().stream().anyMatch(layer -> layer.toLowerCase(Locale.GERMAN).contains(filter));
    }

    private void refreshDwgBlockPreviewAndDetails() {
        DwgBlockDefinition block = dwgBlockSelector.getValue();
        if (block == null) {
            dwgBlockDetailLabel.setText("Kein DWG-Block ausgewählt.");
            drawEmptyDwgPreview("Kein Block");
            return;
        }
        drawDwgPreview(block);
        String layerText = block.layers().isEmpty() ? "keine Layer" : String.join(", ", block.layers());
        String warningText = block.warnings().isEmpty() ? "" : "\nHinweise: " + String.join(" ", block.warnings());
        dwgBlockDetailLabel.setText(String.format(
                Locale.GERMAN,
                "%s%nDatei: %s%nMaße: %.1f x %.1f mm%nUrsprung: %.1f / %.1f mm%nEinheit: %s%nLayer: %s%nElemente: %d | Handles: %d | Inserts: %d%s",
                block.name(),
                block.sourceFile().getFileName(),
                block.widthMillimeters(),
                block.heightMillimeters(),
                block.originXMillimeters(),
                block.originYMillimeters(),
                block.unit(),
                layerText,
                block.entityCount(),
                block.handles().size(),
                block.inserts().size(),
                warningText
        ));
    }

    private void drawEmptyDwgPreview(String text) {
        GraphicsContext graphics = dwgPreviewCanvas.getGraphicsContext2D();
        graphics.setFill(Color.web("#f7f3eb"));
        graphics.fillRect(0, 0, dwgPreviewCanvas.getWidth(), dwgPreviewCanvas.getHeight());
        graphics.setStroke(Color.web("#b8ac9c"));
        graphics.strokeRect(0.5, 0.5, dwgPreviewCanvas.getWidth() - 1.0, dwgPreviewCanvas.getHeight() - 1.0);
        graphics.setFill(Color.web("#6b6258"));
        graphics.fillText(text, 12, dwgPreviewCanvas.getHeight() / 2.0);
    }

    private void drawDwgPreview(DwgBlockDefinition block) {
        if (!block.hasGeometry()) {
            drawEmptyDwgPreview("Keine Geometrie");
            return;
        }
        GraphicsContext graphics = dwgPreviewCanvas.getGraphicsContext2D();
        double width = dwgPreviewCanvas.getWidth();
        double height = dwgPreviewCanvas.getHeight();
        graphics.setFill(Color.web("#f7f3eb"));
        graphics.fillRect(0, 0, width, height);
        graphics.setStroke(Color.web("#b8ac9c"));
        graphics.strokeRect(0.5, 0.5, width - 1.0, height - 1.0);

        DwgBlockDefinition safeBlock = block;
        double padding = 16.0;
        double scale = Math.min(
                (width - padding * 2.0) / Math.max(1.0, safeBlock.widthMillimeters()),
                (height - padding * 2.0) / Math.max(1.0, safeBlock.heightMillimeters())
        );
        double x = padding;
        double y = padding;
        double previewWidth = safeBlock.widthMillimeters() * scale;
        double previewHeight = safeBlock.heightMillimeters() * scale;
        double offsetX = x + (width - padding * 2.0 - previewWidth) / 2.0;
        double offsetY = y + (height - padding * 2.0 - previewHeight) / 2.0;
        graphics.setFill(Color.web("#d8c6aa"));
        graphics.fillRect(offsetX, offsetY, previewWidth, previewHeight);
        graphics.setStroke(Color.web("#2f2a24"));
        graphics.strokeRect(offsetX, offsetY, previewWidth, previewHeight);

        double originX = offsetX + (safeBlock.originXMillimeters() - safeBlock.bounds().minXMillimeters()) * scale;
        double originY = offsetY + previewHeight - (safeBlock.originYMillimeters() - safeBlock.bounds().minYMillimeters()) * scale;
        graphics.setStroke(Color.web("#b3412f"));
        graphics.strokeLine(originX - 5.0, originY, originX + 5.0, originY);
        graphics.strokeLine(originX, originY - 5.0, originX, originY + 5.0);
        graphics.setFill(Color.web("#2f2a24"));
        graphics.fillText(String.format(Locale.GERMAN, "%.0f x %.0f mm", safeBlock.widthMillimeters(), safeBlock.heightMillimeters()), 10.0, height - 10.0);
    }

    private void registerSurfacePreset(SurfaceCoveringPreset preset) {
        for (int index = 0; index < availableSurfacePresets.size(); index++) {
            if (availableSurfacePresets.get(index).coveringSource().equals(preset.coveringSource())) {
                availableSurfacePresets.set(index, preset);
                return;
            }
        }
        availableSurfacePresets.add(preset);
    }

    private void saveCurrentSurfacePreset() {
        SurfaceCoveringPreset preset = currentSurfacePresetFromInputs();
        boolean overwrite = false;
        if (userSurfacePresetLibrary.containsPresetName(preset.name())) {
            overwrite = confirmOverwrite(
                    "Belagspreset überschreiben",
                    "Der Belag `" + preset.name() + "` ist in der Benutzerbibliothek bereits vorhanden.",
                    "Soll das vorhandene Preset durch die aktuell eingetragenen Werte ersetzt werden?"
            );
            if (!overwrite) {
                draftLabel.setText("Belagspreset nicht gespeichert.");
                return;
            }
        }
        try {
            SurfaceCoveringPreset savedPreset = userSurfacePresetLibrary.savePreset(preset, overwrite);
            registerSurfacePreset(savedPreset);
            surfacePresetSelector.setValue(savedPreset);
            draftLabel.setText("Belagspreset gespeichert: " + savedPreset.name());
        } catch (FileAlreadyExistsException exception) {
            draftLabel.setText("Belagspreset existiert bereits und wurde nicht überschrieben.");
        } catch (IOException exception) {
            draftLabel.setText("Belagspreset konnte nicht gespeichert werden: " + exception.getMessage());
        }
    }

    private SurfaceCoveringPreset currentSurfacePresetFromInputs() {
        return new SurfaceCoveringPreset(
                "",
                currentSurfaceLayerName(),
                currentSurfaceLayerThickness(),
                currentSurfaceTileWidth(),
                currentSurfaceTileHeight(),
                currentSurfaceLayoutMode(),
                currentSurfaceLayoutOffset(),
                currentSurfaceMinimumOffset(),
                currentSurfaceMinimumEdgeWidth(),
                currentSurfaceMinimumStartEndMargin(),
                currentSurfaceJointWidth(),
                currentSurfaceCutRestriction(),
                currentSurfaceCoveringSource()
        );
    }

    private boolean confirmOverwrite(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(header);
        return alert.showAndWait()
                .filter(ButtonType.OK::equals)
                .isPresent();
    }

    private void applyDoorPreset(DoorPreset preset) {
        if (preset == null) {
            return;
        }
        setLengthInput(doorWidthField, doorWidthUnit, preset.width(), LengthUnit.METER);
        setLengthInput(doorHeightField, doorHeightUnit, preset.height(), LengthUnit.METER);
        setLengthInput(thresholdField, thresholdUnit, preset.thresholdHeight(), LengthUnit.CENTIMETER);
    }

    private void applyWindowPreset(WindowPreset preset) {
        if (preset == null) {
            return;
        }
        setLengthInput(windowWidthField, windowWidthUnit, preset.width(), LengthUnit.METER);
        setLengthInput(windowHeightField, windowHeightUnit, preset.height(), LengthUnit.METER);
        setLengthInput(sillHeightField, sillHeightUnit, preset.sillHeight(), LengthUnit.CENTIMETER);
    }

    private void applyStairPreset(StairPreset preset) {
        if (preset == null) {
            return;
        }
        setLengthInput(stairHeightField, stairHeightUnit, preset.totalHeight(), LengthUnit.METER);
        stairStepsField.setText(Integer.toString(preset.stepCount()));
    }

    private void applySurfacePreset(SurfaceCoveringPreset preset) {
        if (preset == null) {
            return;
        }
        surfaceLayerNameField.setText(preset.name().replace("DWG-Referenz: ", "").replace("DWG-Block: ", ""));
        setLengthInput(surfaceLayerThicknessField, surfaceLayerThicknessUnit, preset.thickness(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceTileWidthField, surfaceTileWidthUnit, preset.tileWidth(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceTileHeightField, surfaceTileHeightUnit, preset.tileHeight(), LengthUnit.CENTIMETER);
        surfaceLayoutModeSelector.setValue(preset.layoutMode());
        setLengthInput(surfaceLayoutOffsetField, surfaceLayoutOffsetUnit, preset.offset(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceMinimumOffsetField, surfaceMinimumOffsetUnit, preset.minimumOffset(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit, preset.minimumEdgeWidth(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit, preset.minimumStartEndMargin(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceJointWidthField, surfaceJointWidthUnit, preset.jointWidth(), LengthUnit.MILLIMETER);
        surfaceCutRestrictionSelector.setValue(preset.cutRestriction());
        dwgBlockNameField.setText(extractDwgBlockName(preset.coveringSource()).orElse(""));
    }

    private void refreshSurfaceLayerSection() {
        Optional<SurfaceSelectionContext> selectionContext = currentSurfaceSelectionContext();
        if (selectionContext.isEmpty()) {
            surfaceLayerTargetLabel.setText("Keine passende Belagsfläche ausgewählt.");
            surfaceLayerSelectionHintLabel.setText(currentSurfaceSelectionHint());
            surfaceLayerList.getItems().clear();
            surfaceLayerCoverageLabel.setText("Keine Ebenen ausgewählt.");
            updateActionButtons();
            return;
        }
        SurfaceSelectionContext context = selectionContext.get();
        surfaceLayerTargetLabel.setText(context.label());
        surfaceLayerSelectionHintLabel.setText(context.hint());
        Optional<SurfaceLayerStack> stack = currentDisplaySurfaceLayerStack();
        if (stack.isEmpty()) {
            surfaceLayerList.getItems().clear();
            surfaceLayerCoverageLabel.setText(context.targetKeys().size() > 1
                    ? "Ausgewählte Wände haben noch keine gemeinsame Belagsfolge."
                    : "Noch keine Ebene auf dieser Fläche.");
            updateActionButtons();
            return;
        }
        surfaceLayerList.getItems().setAll(stack.get().layers().stream().map(this::describeSurfaceLayer).toList());
        if (!surfaceLayerList.getItems().isEmpty() && surfaceLayerList.getSelectionModel().getSelectedIndex() < 0) {
            surfaceLayerList.getSelectionModel().selectFirst();
        }
        syncInputsFromSelectedSurfaceLayer();
        updateActionButtons();
    }

    private String describeSurfaceLayer(SurfaceLayer layer) {
        String visibility = layer.visible() ? "sichtbar" : "aus";
        int tileCount = estimatedTileCount(layer);
        String sourceLabel = formatCoveringSourceLabel(layer.coveringSource());
        String source = sourceLabel.isBlank() ? "" : " | Quelle: " + sourceLabel;
        return layer.name() + " | " + layer.thickness().format(LengthUnit.MILLIMETER, 1) + " | " + visibility + " | " + tileCount + " Elemente | " + layer.cutRestriction().label() + source;
    }

    private int estimatedTileCount(SurfaceLayer layer) {
        Optional<Room> room = selectedRoom();
        if (room.isEmpty() || currentSurfaceType() == SurfaceType.WALL_INTERIOR || currentSurfaceType() == SurfaceType.WALL_EXTERIOR) {
            return 0;
        }
        TileLayoutRequest request = new TileLayoutRequest(
                Length.ofMillimeters(room.get().widthMillimeters()),
                Length.ofMillimeters(room.get().depthMillimeters()),
                layer.tileWidth(),
                layer.tileHeight(),
                layer.layoutMode(),
                layer.layoutOffset(),
                layer.minimumOffset(),
                layer.minimumEdgeWidth(),
                layer.minimumStartEndMargin()
        );
        return tileLayoutService.fillSurface(request).size();
    }

    private void syncInputsFromSelectedSurfaceLayer() {
        SurfaceLayer selectedLayer = selectedSurfaceLayer().orElse(null);
        if (selectedLayer == null) {
            updateActionButtons();
            return;
        }
        surfaceLayerNameField.setText(selectedLayer.name());
        syncLengthInput(surfaceLayerThicknessField, surfaceLayerThicknessUnit, selectedLayer.thickness(), LengthUnit.CENTIMETER);
        syncLengthInput(surfaceTileWidthField, surfaceTileWidthUnit, selectedLayer.tileWidth(), LengthUnit.CENTIMETER);
        syncLengthInput(surfaceTileHeightField, surfaceTileHeightUnit, selectedLayer.tileHeight(), LengthUnit.CENTIMETER);
        surfaceLayoutModeSelector.setValue(selectedLayer.layoutMode());
        syncLengthInput(surfaceLayoutOffsetField, surfaceLayoutOffsetUnit, selectedLayer.layoutOffset(), LengthUnit.CENTIMETER);
        syncLengthInput(surfaceMinimumOffsetField, surfaceMinimumOffsetUnit, selectedLayer.minimumOffset(), LengthUnit.CENTIMETER);
        syncLengthInput(surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit, selectedLayer.minimumEdgeWidth(), LengthUnit.CENTIMETER);
        syncLengthInput(surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit, selectedLayer.minimumStartEndMargin(), LengthUnit.CENTIMETER);
        syncLengthInput(surfaceJointWidthField, surfaceJointWidthUnit, selectedLayer.jointWidth(), LengthUnit.MILLIMETER);
        surfaceCutRestrictionSelector.setValue(selectedLayer.cutRestriction());
        surfaceLayerCoverageLabel.setText(describeSurfaceLayer(selectedLayer));
        updateActionButtons();
    }

    private void addSurfaceLayer() {
        Optional<SurfaceSelectionContext> selectionContext = currentSurfaceSelectionContext();
        if (selectionContext.isEmpty()) {
            showSurfaceLayerError("Belag kann nicht angelegt werden.", currentSurfaceSelectionHint());
            return;
        }
        if (!validateSurfaceLayerSelection(selectionContext.get())) {
            return;
        }
        rememberStateForUndo();
        for (String targetKey : selectionContext.get().targetKeys()) {
            SurfaceLayerStack stack = activeLevel.get().findSurfaceLayerStack(selectionContext.get().surfaceType(), targetKey);
            if (stack == null) {
                stack = new SurfaceLayerStack(selectionContext.get().surfaceType(), targetKey);
                activeLevel.get().addSurfaceLayerStack(stack);
            }
            stack.addLayer(buildSurfaceLayerFromInputs());
        }
        afterSurfaceLayerMutation(selectionContext.get().targetKeys().size() > 1
                ? "Belag auf ausgewählte Wände angewendet."
                : "Ebene hinzugefügt.");
    }

    private void updateSurfaceLayer() {
        List<SurfaceLayerStack> stacks = currentSurfaceLayerStacks();
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        if (stacks.isEmpty() || selectedIndex < 0) {
            return;
        }
        rememberStateForUndo();
        for (SurfaceLayerStack stack : stacks) {
            SurfaceLayer selectedLayer = stack.layers().get(selectedIndex);
            replaceSurfaceLayer(stack, selectedLayer.id(), new SurfaceLayer(
                    selectedLayer.id(),
                    currentSurfaceLayerName(),
                    currentSurfaceLayerThickness(),
                    selectedLayer.visible(),
                    currentSurfaceTileWidth(),
                    currentSurfaceTileHeight(),
                    currentSurfaceLayoutMode(),
                    currentSurfaceLayoutOffset(),
                    currentSurfaceMinimumOffset(),
                    currentSurfaceMinimumEdgeWidth(),
                    currentSurfaceMinimumStartEndMargin(),
                    currentSurfaceJointWidth(),
                    currentSurfaceCutRestriction(),
                    currentSurfaceCoveringSource()
            ));
        }
        afterSurfaceLayerMutation("Ebene aktualisiert.");
    }

    private void removeSurfaceLayer() {
        List<SurfaceLayerStack> stacks = currentSurfaceLayerStacks();
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        if (stacks.isEmpty() || selectedIndex < 0) {
            return;
        }
        rememberStateForUndo();
        for (SurfaceLayerStack stack : stacks) {
            SurfaceLayer selectedLayer = stack.layers().get(selectedIndex);
            stack.removeLayer(selectedLayer.id());
            if (stack.layers().isEmpty()) {
                activeLevel.get().removeSurfaceLayerStack(stack.id());
            }
        }
        afterSurfaceLayerMutation("Ebene entfernt.");
    }

    private void toggleSurfaceLayerVisibility() {
        List<SurfaceLayerStack> stacks = currentSurfaceLayerStacks();
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        if (stacks.isEmpty() || selectedIndex < 0) {
            return;
        }
        rememberStateForUndo();
        for (SurfaceLayerStack stack : stacks) {
            SurfaceLayer selectedLayer = stack.layers().get(selectedIndex);
            stack.setVisibility(selectedLayer.id(), !selectedLayer.visible());
        }
        afterSurfaceLayerMutation("Ebenensichtbarkeit umgeschaltet.");
    }

    private void moveSurfaceLayer(int direction) {
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        List<SurfaceLayerStack> stacks = currentSurfaceLayerStacks();
        if (stacks.isEmpty() || selectedIndex < 0) {
            return;
        }
        rememberStateForUndo();
        for (SurfaceLayerStack stack : stacks) {
            SurfaceLayer selectedLayer = stack.layers().get(selectedIndex);
            stack.moveLayer(selectedLayer.id(), selectedIndex + direction);
        }
        afterSurfaceLayerMutation("Ebenenreihenfolge geändert.");
        int newIndex = Math.max(0, Math.min(selectedIndex + direction, stacks.getFirst().layers().size() - 1));
        surfaceLayerList.getSelectionModel().select(newIndex);
    }

    private void afterSurfaceLayerMutation(String message) {
        synchronizeRoomsFromWalls(activeLevel.get());
        markThreeDDirty();
        refreshSurfaceLayerSection();
        draftLabel.setText(message);
        render();
    }

    private SurfaceLayer buildSurfaceLayerFromInputs() {
        return SurfaceLayer.create(
                currentSurfaceLayerName(),
                currentSurfaceLayerThickness(),
                currentSurfaceTileWidth(),
                currentSurfaceTileHeight(),
                currentSurfaceLayoutMode(),
                currentSurfaceLayoutOffset(),
                currentSurfaceMinimumOffset(),
                currentSurfaceMinimumEdgeWidth(),
                currentSurfaceMinimumStartEndMargin(),
                currentSurfaceJointWidth(),
                currentSurfaceCutRestriction(),
                currentSurfaceCoveringSource()
        );
    }

    private String currentSurfaceLayerName() {
        String name = surfaceLayerNameField.getText();
        return name == null || name.isBlank() ? "Belag" : name.trim();
    }

    private Length currentSurfaceLayerThickness() {
        return parseLength(surfaceLayerThicknessField, surfaceLayerThicknessUnit.getValue()).orElse(Length.of(1.2, LengthUnit.CENTIMETER));
    }

    private Length currentSurfaceTileWidth() {
        return parseLength(surfaceTileWidthField, surfaceTileWidthUnit.getValue()).orElse(Length.of(60, LengthUnit.CENTIMETER));
    }

    private Length currentSurfaceTileHeight() {
        return parseLength(surfaceTileHeightField, surfaceTileHeightUnit.getValue()).orElse(Length.of(30, LengthUnit.CENTIMETER));
    }

    private SurfaceLayoutMode currentSurfaceLayoutMode() {
        return Optional.ofNullable(surfaceLayoutModeSelector.getValue()).orElse(SurfaceLayoutMode.AUTOMATIC);
    }

    private Length currentSurfaceLayoutOffset() {
        return parseLength(surfaceLayoutOffsetField, surfaceLayoutOffsetUnit.getValue()).orElse(Length.zero());
    }

    private Length currentSurfaceMinimumOffset() {
        return parseLength(surfaceMinimumOffsetField, surfaceMinimumOffsetUnit.getValue()).orElse(Length.zero());
    }

    private Length currentSurfaceMinimumEdgeWidth() {
        return parseLength(surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit.getValue()).orElse(Length.zero());
    }

    private Length currentSurfaceMinimumStartEndMargin() {
        return parseLength(surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit.getValue()).orElse(Length.zero());
    }

    private Length currentSurfaceJointWidth() {
        return parseLength(surfaceJointWidthField, surfaceJointWidthUnit.getValue()).orElse(Length.ofMillimeters(2));
    }

    private SurfaceCutRestriction currentSurfaceCutRestriction() {
        return Optional.ofNullable(surfaceCutRestrictionSelector.getValue()).orElse(SurfaceCutRestriction.fallback());
    }

    private String currentSurfaceCoveringSource() {
        return Optional.ofNullable(surfacePresetSelector.getValue())
                .map(SurfaceCoveringPreset::coveringSource)
                .orElse("");
    }

    private Optional<Path> currentDwgLibraryPath() {
        return Optional.ofNullable(dwgBlockSelector.getValue())
                .map(DwgBlockDefinition::sourceFile)
                .or(() -> Optional.ofNullable(surfacePresetSelector.getValue())
                .map(SurfaceCoveringPreset::coveringSource)
                .flatMap(this::extractDwgLibraryPath))
                .or(() -> cadLibraryReferences.stream()
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dwg"))
                        .findFirst())
                .map(path -> path.toAbsolutePath().normalize());
    }

    private Optional<Path> extractDwgLibraryPath(String coveringSource) {
        if (coveringSource == null || coveringSource.isBlank()) {
            return Optional.empty();
        }
        String pathPart = coveringSource.contains("#")
                ? coveringSource.substring(0, coveringSource.indexOf('#'))
                : coveringSource;
        if (!pathPart.toLowerCase(Locale.ROOT).endsWith(".dwg")) {
            return Optional.empty();
        }
        return Optional.of(Path.of(pathPart));
    }

    private Optional<String> extractDwgBlockName(String coveringSource) {
        if (coveringSource == null || !coveringSource.contains("#")) {
            return Optional.empty();
        }
        return Optional.of(coveringSource.substring(coveringSource.indexOf('#') + 1));
    }

    private String formatCoveringSourceLabel(String coveringSource) {
        if (coveringSource == null || coveringSource.isBlank()) {
            return "";
        }
        Optional<Path> dwgPath = extractDwgLibraryPath(coveringSource);
        if (dwgPath.isPresent()) {
            String fileName = dwgPath.get().getFileName().toString();
            return extractDwgBlockName(coveringSource)
                    .map(blockName -> fileName + " → " + blockName)
                    .orElse(fileName);
        }
        if (coveringSource.toLowerCase(Locale.ROOT).endsWith(".cadasbelag")) {
            String fileName = Path.of(coveringSource).getFileName().toString();
            return "Eigenes Preset: " + fileName.substring(0, fileName.length() - ".cadasbelag".length());
        }
        return coveringSource;
    }

    private Optional<SurfaceLayerStack> currentSurfaceLayerStack() {
        return currentDisplaySurfaceLayerStack();
    }

    private Optional<SurfaceLayer> selectedSurfaceLayer() {
        SurfaceLayerStack stack = currentDisplaySurfaceLayerStack().orElse(null);
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        if (stack == null || selectedIndex < 0 || selectedIndex >= stack.layers().size()) {
            return Optional.empty();
        }
        return Optional.of(stack.layers().get(selectedIndex));
    }

    private Optional<String> currentSurfaceTargetKey() {
        return currentSurfaceSelectionContext()
                .filter(context -> context.targetKeys().size() == 1)
                .map(context -> context.targetKeys().getFirst());
    }

    private SurfaceType currentSurfaceType() {
        List<SurfaceType> availableTypes = availableSurfaceTypesForSelection();
        if (availableTypes.isEmpty()) {
            return SurfaceType.WALL_INTERIOR;
        }
        SurfaceType selectedType = surfaceTypeSelector.getValue();
        if (selectedType != null && availableTypes.contains(selectedType)) {
            return selectedType;
        }
        if (availableTypes.contains(preferredRoomSurfaceType)) {
            return preferredRoomSurfaceType;
        }
        return availableTypes.getFirst();
    }

    private Optional<Room> selectedRoom() {
        if (selectedSelection.get() == null) {
            return Optional.empty();
        }
        if (selectedSelection.get().kind() != RenderableKind.ROOM_VOLUME
                && selectedSelection.get().kind() != RenderableKind.ROOM_FLOOR
                && selectedSelection.get().kind() != RenderableKind.ROOM_CEILING) {
            return Optional.empty();
        }
        return activeLevel.get().rooms().stream()
                .filter(room -> room.id().toString().equals(selectedSelection.get().elementId()))
                .findFirst();
    }

    private List<SurfaceType> availableSurfaceTypesForSelection() {
        boolean hasWalls = !selectedWalls().isEmpty();
        boolean hasSingleRoom = selectedSurfaceRoom().isPresent();
        if (hasWalls && hasSingleRoom) {
            return List.of(SurfaceType.WALL_INTERIOR);
        }
        if (hasWalls) {
            return List.of(SurfaceType.WALL_EXTERIOR);
        }
        if (hasSingleRoom) {
            return List.of(SurfaceType.FLOOR, SurfaceType.CEILING);
        }
        return List.of();
    }

    private void refreshSurfaceTypeSelector() {
        List<SurfaceType> availableTypes = availableSurfaceTypesForSelection();
        SurfaceType currentValue = surfaceTypeSelector.getValue();
        if (!surfaceTypeSelector.getItems().equals(availableTypes)) {
            surfaceTypeSelector.getItems().setAll(availableTypes);
        }
        SurfaceType preferredType = availableTypes.contains(preferredRoomSurfaceType)
                ? preferredRoomSurfaceType
                : availableTypes.stream().findFirst().orElse(null);
        SurfaceType nextValue = currentValue != null && availableTypes.contains(currentValue) ? currentValue : preferredType;
        if (surfaceTypeSelector.getValue() != nextValue) {
            surfaceTypeSelector.setValue(nextValue);
        }
        surfaceTypeSelector.setDisable(availableTypes.size() <= 1);
    }

    private Optional<Room> selectedSurfaceRoom() {
        List<String> roomIds = selectedSelections.stream()
                .filter(selection -> selection.kind() == RenderableKind.ROOM_VOLUME
                        || selection.kind() == RenderableKind.ROOM_FLOOR
                        || selection.kind() == RenderableKind.ROOM_CEILING)
                .map(SelectionKey::elementId)
                .distinct()
                .toList();
        if (roomIds.size() != 1) {
            return Optional.empty();
        }
        return activeLevel.get().rooms().stream()
                .filter(room -> room.id().toString().equals(roomIds.getFirst()))
                .findFirst();
    }

    private List<Wall> selectedWalls() {
        Set<String> wallIds = selectedSelections.stream()
                .filter(selection -> selection.kind() == RenderableKind.WALL)
                .map(SelectionKey::elementId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (wallIds.isEmpty()) {
            return List.of();
        }
        return activeLevel.get().walls().stream()
                .filter(wall -> wallIds.contains(wall.id().toString()))
                .toList();
    }

    private Optional<SurfaceSelectionContext> currentSurfaceSelectionContext() {
        SurfaceType surfaceType = currentSurfaceType();
        if (surfaceType == SurfaceType.WALL_INTERIOR || surfaceType == SurfaceType.WALL_EXTERIOR) {
            List<Wall> walls = selectedWalls();
            if (walls.isEmpty()) {
                return Optional.empty();
            }
            if (surfaceType == SurfaceType.WALL_INTERIOR) {
                Optional<Room> room = selectedSurfaceRoom();
                if (room.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(new SurfaceSelectionContext(
                        surfaceType,
                        walls.stream().map(wall -> WallSurfaceTargetKey.interior(wall.id(), room.get().id())).toList(),
                        "Fläche: Innenwand auf Raum `" + room.get().name() + "` und " + walls.size() + " Wand/Wände",
                        "Innenwand-Beläge werden aus dem ausgewählten Raum auf die angrenzende Wandseite gelegt."
                ));
            }
            if (selectedSurfaceRoom().isPresent()) {
                return Optional.empty();
            }
            return Optional.of(new SurfaceSelectionContext(
                    surfaceType,
                    walls.stream().map(wall -> wall.id().toString()).toList(),
                    "Fläche: Außenwand auf " + walls.size() + " Wand/Wände",
                    "Außenwand-Beläge werden nur auf raumfreie Wandseiten gelegt."
            ));
        }
        Optional<Room> room = selectedSurfaceRoom();
        if (room.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SurfaceSelectionContext(
                surfaceType,
                List.of(room.get().id().toString()),
                "Fläche: " + (surfaceType == SurfaceType.CEILING ? "Decke" : "Boden") + " auf Raum `" + room.get().name() + "`",
                surfaceType == SurfaceType.CEILING
                        ? "Deckenbeläge wirken auf die Unterseite der Raumdecke."
                        : "Bodenbeläge liegen oberhalb des Rohbodens innerhalb des ausgewählten Raums."
        ));
    }

    private String currentSurfaceSelectionHint() {
        return switch (currentSurfaceType()) {
            case WALL_INTERIOR -> "Für Innenwand-Beläge genau einen Raum und mindestens eine Wand auswählen.";
            case WALL_EXTERIOR -> "Für Außenwand-Beläge eine oder mehrere Wände ohne Raumauswahl auswählen.";
            case FLOOR, CEILING -> "Für Boden- oder Deckenbeläge genau einen Raum auswählen.";
            default -> "Keine passende Fläche ausgewählt.";
        };
    }

    private Optional<SurfaceLayerStack> currentDisplaySurfaceLayerStack() {
        List<SurfaceLayerStack> stacks = currentSurfaceLayerStacks();
        if (stacks.isEmpty()) {
            return Optional.empty();
        }
        if (stacks.size() == 1) {
            return Optional.of(stacks.getFirst());
        }
        SurfaceLayerStack reference = stacks.getFirst();
        boolean allSelectedWallsCovered = currentSurfaceSelectionContext()
                .map(context -> context.targetKeys().size() == stacks.size())
                .orElse(false);
        if (!allSelectedWallsCovered) {
            return Optional.empty();
        }
        boolean equalSequence = stacks.stream()
                .skip(1)
                .allMatch(stack -> surfaceLayerConsistencyService.haveEqualSequence(reference, stack));
        return equalSequence ? Optional.of(reference) : Optional.empty();
    }

    private List<SurfaceLayerStack> currentSurfaceLayerStacks() {
        return currentSurfaceSelectionContext()
                .map(context -> context.targetKeys().stream()
                        .map(targetKey -> activeLevel.get().findSurfaceLayerStack(context.surfaceType(), targetKey))
                        .filter(stack -> stack != null)
                        .toList())
                .orElseGet(List::of);
    }

    private boolean validateSurfaceLayerSelection(SurfaceSelectionContext context) {
        if (context.surfaceType() == SurfaceType.WALL_INTERIOR) {
            Room room = selectedSurfaceRoom().orElseThrow();
            boolean invalidWallSelected = selectedWalls().stream()
                    .anyMatch(wall -> !wallSurfaceSideService.hasInteriorSide(activeLevel.get(), wall, room.id()));
            if (invalidWallSelected) {
                showSurfaceLayerError(
                        "Innenwand-Belag kann nicht angelegt werden.",
                        "Der ausgewählte Raum grenzt nicht an alle ausgewählten Wände."
                );
                return false;
            }
        }
        if (context.surfaceType() == SurfaceType.WALL_EXTERIOR) {
            boolean invalidWallSelected = selectedWalls().stream()
                    .anyMatch(wall -> !wallSurfaceSideService.hasExteriorSide(activeLevel.get(), wall));
            if (invalidWallSelected) {
                showSurfaceLayerError(
                        "Außenwand-Belag kann nicht angelegt werden.",
                        "Mindestens eine ausgewählte Wand hat keine raumfreie Seite. Wähle für Innenwände zusätzlich den passenden Raum aus."
                );
                return false;
            }
        }
        return true;
    }

    private void showSurfaceLayerError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
        alert.setTitle("Belag");
        alert.setHeaderText(header);
        alert.getDialogPane().setPrefWidth(520);
        Window window = getScene() != null ? getScene().getWindow() : null;
        if (window != null) {
            alert.initOwner(window);
        }
        alert.showAndWait();
    }

    private void replaceSurfaceLayer(SurfaceLayerStack stack, UUID layerId, SurfaceLayer replacement) {
        stack.replaceLayer(layerId, replacement);
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
        fitCurrentViewToContent();
        render();
    }

    private void fitCurrentViewToContent() {
        double viewportWidth = Math.max(drawingPane.getWidth(), 640.0);
        double viewportHeight = Math.max(drawingPane.getHeight(), 420.0);
        projectedBoundsService.bounds(activeLevel.get(), activeView.get()).ifPresentOrElse(bounds -> {
            double contentWidth = Math.max(bounds.widthMillimeters(), 1_000.0);
            double contentHeight = Math.max(bounds.heightMillimeters(), 1_000.0);
            double horizontalPadding = projectionService.isPlanView(activeView.get()) ? 80.0 : 64.0;
            double verticalPadding = projectionService.isPlanView(activeView.get()) ? 96.0 : 72.0;
            double availableWidth = Math.max(220.0, viewportWidth - horizontalPadding);
            double availableHeight = Math.max(180.0, viewportHeight - verticalPadding);
            double fitScale = Math.min(availableWidth / contentWidth, availableHeight / contentHeight);
            zoom = twoDZoomRange.clamp(fitScale / BASE_PIXELS_PER_MILLIMETER);
            offsetX = viewportWidth / 2.0 - bounds.centerHorizontalMillimeters() * scale();
            offsetY = viewportHeight / 2.0 - bounds.centerVerticalMillimeters() * scale();
        }, () -> {
            zoom = 1.0;
            offsetX = viewportWidth / 2.0;
            offsetY = viewportHeight / 2.0;
        });
    }

    private void clearProject() {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Alle Etagen, Bauteile, Hilfslinien und Dachinformationen des aktuellen Projekts werden entfernt. Dieser Schritt kann über Rückgängig wiederhergestellt werden, solange der Verlauf erhalten bleibt.",
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
                    openingDragId = null;
                    openingDragWallAxis = null;
                    openingDragWidth = 0;
                    openingDragOffsetDelta = 0;
                    draftStart = null;
                    previewSegment = null;
                    pendingGuideOrientation = null;
                    activateLevel(level);
                    fitCurrentViewToContent();
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
                selectedSelection.get(),
                zoom,
                offsetX,
                offsetY
        );
    }

    private void restoreSnapshot(WorkbenchSnapshot snapshot) {
        project.replaceWith(snapshot.project());
        project.levels().forEach(level -> level.replaceRooms(autoRoomGenerationService.synchronize(level, currentRoomDefaults())));
        availableLevels.setAll(project.levels());
        guideLines.setAll(snapshot.guideLines());
        selectedEndpointGroup = null;
        selectionDragAnchor = null;
        selectionDragBaseWalls = List.of();
        selectionDragBaseStaircases = List.of();
        selectionDragBaseRoomObjects = List.of();
        openingDragId = null;
        openingDragWallAxis = null;
        openingDragWidth = 0;
        openingDragOffsetDelta = 0;
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
        zoom = snapshot.zoom();
        offsetX = snapshot.offsetX();
        offsetY = snapshot.offsetY();
        updateStatus();
        render();
    }

    private void clearSelection() {
        clearSelectionsInternal();
        selectedEndpointGroup = null;
        selectionDragAnchor = null;
        selectionDragBaseWalls = List.of();
        selectionDragBaseStaircases = List.of();
        selectionDragBaseRoomObjects = List.of();
        openingDragId = null;
        openingDragWallAxis = null;
        openingDragWidth = 0;
        openingDragOffsetDelta = 0;
        historyCapturedForDrag = false;
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
                case ROOM_VOLUME, ROOM_FLOOR, ROOM_CEILING -> false;
                case DOOR -> activeLevel.get().removeDoor(id);
                case WINDOW -> activeLevel.get().removeWindow(id);
                case STAIR -> activeLevel.get().removeStaircase(id);
                case ROOM_OBJECT -> activeLevel.get().removeRoomObject(id);
                default -> false;
            };
        }
        if (removed) {
            synchronizeRoomsFromWalls(activeLevel.get());
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
        updateMouseCursor();
    }

    private void rebuildSelectionContextMenu() {
        selectionContextMenu.getItems().setAll(
                menuItem("Eigenschaften auf Auswahl anwenden", this::applyCurrentInputsToSelection, null),
                menuItem("Auswahl aufheben", this::clearSelection, null)
        );
        if (selectedSelections.stream().anyMatch(selection -> selection.kind() != RenderableKind.ROOM_VOLUME
                && selection.kind() != RenderableKind.ROOM_FLOOR
                && selection.kind() != RenderableKind.ROOM_CEILING)) {
            selectionContextMenu.getItems().add(menuItem("Auswahl löschen", this::deleteSelection, null));
        }
        if (selectedSelections.stream().anyMatch(this::isRotatableSelection)) {
            selectionContextMenu.getItems().addAll(
                    menuItem("Bauteile 90° im Uhrzeigersinn drehen", this::rotateSelectedComponentsClockwise, null),
                    menuItem("Bauteile 90° gegen den Uhrzeigersinn drehen", this::rotateSelectedComponentsCounterClockwise, null)
            );
        }
        if (selectedWalls().size() >= 3) {
            selectionContextMenu.getItems().add(menuItem("Raum erkennen", this::recognizeRoomFromSelectedWalls, null));
        }
    }

    private void recognizeRoomFromSelectedWalls() {
        List<Wall> walls = selectedWalls();
        if (walls.size() < 3) {
            draftLabel.setText("Raumerkennung benötigt mindestens drei ausgewählte Wände.");
            return;
        }
        List<Room> previousRooms = activeLevel.get().rooms();
        List<Room> recognizedRooms = autoRoomGenerationService.synchronizeFromSelectedWalls(
                activeLevel.get(),
                walls.stream().map(Wall::id).collect(java.util.stream.Collectors.toSet()),
                currentRoomDefaults()
        );
        if (recognizedRooms.equals(previousRooms)) {
            draftLabel.setText("Aus den ausgewählten Wänden konnte kein geschlossener Raum erkannt werden.");
            return;
        }
        rememberStateForUndo();
        activeLevel.get().replaceRooms(recognizedRooms);
        markThreeDDirty();
        updatePropertySectionVisibility();
        render();
        draftLabel.setText("Raum erkannt: " + recognizedRooms.size() + " Räume auf der aktiven Etage.");
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
                        syncLengthInput(wallThicknessField, wallThicknessUnit, wall.thickness(), LengthUnit.CENTIMETER);
                        syncLengthInput(wallHeightField, wallHeightUnit, wall.height(), LengthUnit.METER);
                    });
            case ROOM_VOLUME, ROOM_FLOOR, ROOM_CEILING -> activeLevel.get().rooms().stream()
                    .filter(room -> room.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(room -> {
                        roomNameField.setText(room.name());
                        syncLengthInput(roomHeightField, roomHeightUnit, room.roomHeight(), LengthUnit.METER);
                        syncLengthInput(floorThicknessField, floorThicknessUnit, room.floorThickness(), LengthUnit.CENTIMETER);
                        syncLengthInput(ceilingThicknessField, ceilingThicknessUnit, room.ceilingThickness(), LengthUnit.MILLIMETER);
                        if (room.slopedCeilingProfile().isPresent()) {
                            SlopedCeilingProfile profile = room.slopedCeilingProfile().orElseThrow();
                            slopedCeilingModeSelector.setValue("Mit Dachschräge");
                            slopedCeilingSideSelector.setValue(profile.lowSide());
                            syncLengthInput(kneeWallHeightField, kneeWallHeightUnit, profile.kneeWallHeight(), LengthUnit.METER);
                        } else {
                            slopedCeilingModeSelector.setValue("Ohne Dachschräge");
                        }
                    });
            case DOOR -> activeLevel.get().doors().stream()
                    .filter(door -> door.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(door -> {
                        syncLengthInput(doorWidthField, doorWidthUnit, door.width(), LengthUnit.METER);
                        syncLengthInput(doorHeightField, doorHeightUnit, door.height(), LengthUnit.METER);
                        syncLengthInput(thresholdField, thresholdUnit, door.thresholdHeight(), LengthUnit.CENTIMETER);
                    });
            case WINDOW -> activeLevel.get().windows().stream()
                    .filter(window -> window.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(window -> {
                        syncLengthInput(windowWidthField, windowWidthUnit, window.width(), LengthUnit.METER);
                        syncLengthInput(windowHeightField, windowHeightUnit, window.windowHeight(), LengthUnit.METER);
                        syncLengthInput(sillHeightField, sillHeightUnit, window.sillHeight(), LengthUnit.CENTIMETER);
                    });
            case STAIR -> activeLevel.get().staircases().stream()
                    .filter(stair -> stair.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(stair -> {
                        syncLengthInput(stairHeightField, stairHeightUnit, stair.totalHeight(), LengthUnit.METER);
                        stairStepsField.setText(Integer.toString(stair.stepCount()));
                        syncLengthInput(stairStartLandingField, stairStartLandingUnit, stair.startLandingWidth(), LengthUnit.CENTIMETER);
                        syncLengthInput(stairEndLandingField, stairEndLandingUnit, stair.endLandingWidth(), LengthUnit.CENTIMETER);
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
                            ? new Wall(
                            wall.id(),
                            wall.axis(),
                            currentWallThickness(),
                            currentWallHeight(),
                            currentWallHeight(),
                            currentWallHeight()
                    )
                            : wall)
                    .toList());
            case ROOM_VOLUME, ROOM_FLOOR, ROOM_CEILING -> activeLevel.get().replaceRooms(activeLevel.get().rooms().stream()
                    .map(room -> selectedIds().contains(room.id().toString())
                            ? new Room(room.id(), currentRoomName(), room.outline(), currentRoomHeight(), currentFloorThickness(), currentCeilingThickness(), currentSlopedCeilingProfile(), room.ceilingVertexHeights())
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
                            ? new Staircase(stair.id(), stair.stairType(), stair.firstCorner(), stair.oppositeCorner(), currentStairHeight(), currentStairSteps(), stair.rotationQuarterTurns(), currentStairStartLanding(), currentStairEndLanding())
                            : stair)
                    .toList());
            default -> {
            }
        }
        synchronizeRoomsFromWalls(activeLevel.get());
        markThreeDDirty();
        draftLabel.setText("Eigenschaften auf Auswahl angewendet.");
        render();
    }

    private void applyEndpointHeightToSelection() {
        if (selectedEndpointGroup == null) {
            return;
        }
        Length newHeight = currentEndpointHeight();
        rememberStateForUndo();
        activeLevel.get().replaceWalls(activeLevel.get().walls().stream()
                .map(wall -> {
                    boolean isStart = selectedEndpointGroup.startWallIds().contains(wall.id());
                    boolean isEnd = selectedEndpointGroup.endWallIds().contains(wall.id());
                    if (!isStart && !isEnd) {
                        return wall;
                    }
                    return wall.withEndpointHeights(
                            isStart ? newHeight : wall.startHeight(),
                            isEnd ? newHeight : wall.endHeight()
                    );
                })
                .toList());
        synchronizeRoomsFromWalls(activeLevel.get());
        syncEndpointHeightInputFromSelection();
        markThreeDDirty();
        updatePropertySectionVisibility();
        draftLabel.setText("Eckhöhe übernommen und Raumgeometrie aktualisiert.");
        render();
    }

    private void syncEndpointHeightInputFromSelection() {
        selectedEndpointHeight().ifPresent(height -> syncLengthInput(endpointHeightField, endpointHeightUnit, height, LengthUnit.METER));
    }

    private Optional<Length> selectedEndpointHeight() {
        if (selectedEndpointGroup == null) {
            return Optional.empty();
        }
        return activeLevel.get().walls().stream()
                .filter(wall -> selectedEndpointGroup.startWallIds().contains(wall.id()) || selectedEndpointGroup.endWallIds().contains(wall.id()))
                .findFirst()
                .map(wall -> selectedEndpointGroup.startWallIds().contains(wall.id()) ? wall.startHeight() : wall.endHeight());
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

    private void setLengthInput(TextField field, ComboBox<LengthUnit> unitSelector, Length length, LengthUnit unit) {
        updatingLengthInput = true;
        try {
            unitSelector.setValue(unit);
            field.setText(formatValue(length, unit, LENGTH_INPUT_DECIMALS));
        } finally {
            updatingLengthInput = false;
        }
    }

    private void syncLengthInput(TextField field, ComboBox<LengthUnit> unitSelector, Length length, LengthUnit fallbackUnit) {
        LengthUnit unit = Optional.ofNullable(unitSelector.getValue()).orElse(fallbackUnit);
        updatingLengthInput = true;
        try {
            if (unitSelector.getValue() == null) {
                unitSelector.setValue(unit);
            }
            field.setText(formatValue(length, unit, LENGTH_INPUT_DECIMALS));
        } finally {
            updatingLengthInput = false;
        }
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
        activeLevel.get().replaceStaircases(rotationResult.staircases());
        synchronizeRoomsFromWalls(activeLevel.get());
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
                .ifPresent(level -> {
                    activateLevel(level);
                    fitCurrentViewToContent();
                });
        selectSingle(selectionKey);
    }

    private void markThreeDDirty() {
        threeDDirty = true;
    }

    private void synchronizeRoomsFromWalls(Level level) {
        level.replaceRooms(autoRoomGenerationService.synchronize(level, currentRoomDefaults()));
    }

    private void prepareSelectionDrag(SelectionKey selectionKey, PlanPoint anchorPoint) {
        if (selectionKey == null || !selectedSelections.contains(selectionKey)) {
            return;
        }
        if (selectedSelections.stream().noneMatch(this::isTranslatableSelection)) {
            return;
        }
        selectionDragAnchor = anchorPoint;
        selectionDragBaseWalls = List.copyOf(activeLevel.get().walls());
        selectionDragBaseStaircases = List.copyOf(activeLevel.get().staircases());
        selectionDragBaseRoomObjects = List.copyOf(activeLevel.get().roomObjects());
        draftLabel.setText("Ausgewählte Wände, Treppen oder Objekte können jetzt parallel verschoben werden.");
    }

    private void translateSelectedComponents(PlanPoint snappedPoint) {
        double deltaX = snappedPoint.xMillimeters() - selectionDragAnchor.xMillimeters();
        double deltaY = snappedPoint.yMillimeters() - selectionDragAnchor.yMillimeters();
        if (snapToGuides.get() || snapToWalls.get()) {
            List<Wall> selectedWalls = selectionDragBaseWalls.stream()
                    .filter(wall -> selectedSelections.stream().anyMatch(selection -> selection.kind() == RenderableKind.WALL
                            && selection.elementId().equals(wall.id().toString())))
                    .toList();
            Set<UUID> selectedWallIds = selectedWalls.stream().map(Wall::id).collect(java.util.stream.Collectors.toSet());
            GuideSnapService.Translation translation = guideSnapService.snapWallTranslation(
                    selectedWalls,
                    deltaX,
                    deltaY,
                    currentAlignmentSnapTargets(selectedWallIds),
                    SNAP_TOLERANCE
            );
            deltaX = translation.deltaX();
            deltaY = translation.deltaY();
        }
        Level dragLevel = new Level(activeLevel.get().name());
        dragLevel.replaceWalls(selectionDragBaseWalls);
        dragLevel.replaceStaircases(selectionDragBaseStaircases);
        dragLevel.replaceRoomObjects(selectionDragBaseRoomObjects);
        SelectionTranslationService.TranslationResult translationResult = selectionTranslationService.translate(dragLevel, Set.copyOf(selectedSelections), deltaX, deltaY);
        if (!translationResult.changed()) {
            return;
        }
        activeLevel.get().replaceWalls(translationResult.walls());
        activeLevel.get().replaceStaircases(translationResult.staircases());
        activeLevel.get().replaceRoomObjects(translationResult.roomObjects());
        synchronizeRoomsFromWalls(activeLevel.get());
        markThreeDDirty();
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
                activeWorkspaceMode.get().name(),
                currentTool().name(),
                activeLevel.get().walls().size(),
                activeLevel.get().rooms().size(),
                activeLevel.get().doors().size(),
                activeLevel.get().windows().size(),
                activeLevel.get().staircases().size(),
                selectedSelections.size(),
                cadLibraryReferences.size(),
                threeDViewport.renderedBodyCount(),
                threeDViewport.hasVisibleSceneContent(),
                threeDViewport.cameraStatusText(),
                Optional.ofNullable(surfaceTypeSelector.getValue()).map(Enum::name).orElse(""),
                String.join(",", surfaceTypeSelector.getItems().stream().map(Enum::name).toList()),
                surfaceLayerTargetLabel.getText(),
                surfaceLayerSelectionHintLabel.getText(),
                surfaceLayerCoverageLabel.getText(),
                automationSelectedRoomMetrics(),
                draftLabel.getText(),
                zoom,
                offsetX,
                offsetY
        );
    }

    public void automationSetViewport(double zoomFactor, double newOffsetX, double newOffsetY) {
        zoom = twoDZoomRange.clamp(zoomFactor);
        offsetX = newOffsetX;
        offsetY = newOffsetY;
        updateStatus();
        render();
    }

    public WritableImage automationDrawingSnapshot() {
        ensureCanvasReady();
        return drawingCanvas.snapshot(null, null);
    }

    public void automationRememberUndoState() {
        rememberStateForUndo();
    }

    public void automationSetTool(String toolName) {
        String normalizedToolName = toolName.trim().toUpperCase(Locale.ROOT);
        if ("ROOM".equals(normalizedToolName)) {
            toolSelector.setValue(DrawingTool.EDIT);
            return;
        }
        toolSelector.setValue(DrawingTool.valueOf(normalizedToolName));
    }

    public void automationSelectLevel(String levelName) {
        availableLevels.stream()
                .filter(level -> level.name().equals(levelName))
                .findFirst()
                .ifPresentOrElse(this::activateLevel, () -> {
                    throw new IllegalArgumentException("Etage `" + levelName + "` ist nicht vorhanden.");
                });
    }

    public void automationSetWorkspace(String workspaceName) {
        selectWorkspaceMode(WorkspaceMode.valueOf(workspaceName.trim().toUpperCase(Locale.ROOT)), false);
    }

    public void automationSetSurfaceType(String surfaceTypeName) {
        SurfaceType targetType = SurfaceType.valueOf(surfaceTypeName.trim().toUpperCase(Locale.ROOT));
        if (!surfaceTypeSelector.getItems().contains(targetType)) {
            throw new IllegalArgumentException("Belagstyp `" + surfaceTypeName + "` passt nicht zur aktuellen Auswahl.");
        }
        surfaceTypeSelector.setValue(targetType);
        updatePropertySectionVisibility();
        render();
    }

    public void automationSelect(String kindName, int index, boolean toggle) {
        SelectionKey selectionKey = switch (kindName.trim().toUpperCase(Locale.ROOT)) {
            case "WALL" -> activeLevel.get().walls().stream()
                    .skip(index)
                    .findFirst()
                    .map(wall -> new SelectionKey(RenderableKind.WALL, activeLevel.get().name(), wall.id().toString()))
                    .orElseThrow(() -> new IllegalArgumentException("Wandindex `" + index + "` ist ungültig."));
            case "ROOM", "ROOM_VOLUME" -> activeLevel.get().rooms().stream()
                    .skip(index)
                    .findFirst()
                    .map(room -> new SelectionKey(RenderableKind.ROOM_VOLUME, activeLevel.get().name(), room.id().toString()))
                    .orElseThrow(() -> new IllegalArgumentException("Raumindex `" + index + "` ist ungültig."));
            case "DOOR" -> activeLevel.get().doors().stream()
                    .skip(index)
                    .findFirst()
                    .map(door -> new SelectionKey(RenderableKind.DOOR, activeLevel.get().name(), door.id().toString()))
                    .orElseThrow(() -> new IllegalArgumentException("Türindex `" + index + "` ist ungültig."));
            case "WINDOW" -> activeLevel.get().windows().stream()
                    .skip(index)
                    .findFirst()
                    .map(window -> new SelectionKey(RenderableKind.WINDOW, activeLevel.get().name(), window.id().toString()))
                    .orElseThrow(() -> new IllegalArgumentException("Fensterindex `" + index + "` ist ungültig."));
            case "STAIR" -> activeLevel.get().staircases().stream()
                    .skip(index)
                    .findFirst()
                    .map(stair -> new SelectionKey(RenderableKind.STAIR, activeLevel.get().name(), stair.id().toString()))
                    .orElseThrow(() -> new IllegalArgumentException("Treppenindex `" + index + "` ist ungültig."));
            case "OBJECT", "ROOM_OBJECT" -> activeLevel.get().roomObjects().stream()
                    .skip(index)
                    .findFirst()
                    .map(roomObject -> new SelectionKey(RenderableKind.ROOM_OBJECT, activeLevel.get().name(), roomObject.id().toString()))
                    .orElseThrow(() -> new IllegalArgumentException("Objektindex `" + index + "` ist ungültig."));
            default -> throw new IllegalArgumentException("Bauteilart `" + kindName + "` wird von der Automatisierung nicht unterstützt.");
        };
        updateSelection(selectionKey, toggle);
        render();
    }

    public void automationSelectSurfaceLayer(int index) {
        if (index < 0 || index >= surfaceLayerList.getItems().size()) {
            throw new IllegalArgumentException("Belagindex `" + index + "` ist ungültig.");
        }
        surfaceLayerList.getSelectionModel().select(index);
        syncInputsFromSelectedSurfaceLayer();
        render();
    }

    public void automationSetField(String fieldName, String value) {
        textFieldByName(fieldName).setText(value);
        updatePropertySectionVisibility();
        render();
    }

    public String automationFieldValue(String fieldName) {
        return textFieldByName(fieldName).getText();
    }

    public void automationSetUnit(String fieldName, String unitName) {
        unitSelectorByName(fieldName).setValue(LengthUnit.valueOf(unitName.trim().toUpperCase(Locale.ROOT)));
        render();
    }

    public String automationUnit(String fieldName) {
        return unitSelectorByName(fieldName).getValue().name();
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

    public void automationCanvasPress(double x, double y, MouseButton button) {
        ensureCanvasReady();
        handleMousePressed(mouseEvent(MouseEvent.MOUSE_PRESSED, x, y, button, false, false, false, true));
    }

    public void automationCanvasDragTo(double x, double y, MouseButton button) {
        handleMouseDragged(mouseEvent(MouseEvent.MOUSE_DRAGGED, x, y, button, false, false, false, true));
    }

    public void automationCanvasRelease(double x, double y, MouseButton button) {
        handleMouseReleased(mouseEvent(MouseEvent.MOUSE_RELEASED, x, y, button, false, false, false, false));
    }

    public String automationActiveEdgeHandle() {
        return activeEdgeHandle == null ? "" : activeEdgeHandle.kind().name();
    }

    public String automationEdgeHandleAtScreen(double x, double y) {
        return edgeResizeService.findHandle(
                        activeLevel.get(),
                        Set.copyOf(selectedSelections),
                        screenToWorld(x, y),
                        Length.ofMillimeters(8.0 / scale())
                )
                .map(handle -> handle.kind().name())
                .orElse("");
    }

    public String automationCursorAt(double x, double y, boolean altDown, boolean spaceDown) {
        lastMouseX = x;
        lastMouseY = y;
        altPressed = altDown;
        spacePressed = spaceDown;
        updateMouseCursor();
        Cursor cursor = drawingCanvas.getCursor();
        if (cursor == Cursor.H_RESIZE) return "H_RESIZE";
        if (cursor == Cursor.V_RESIZE) return "V_RESIZE";
        if (cursor == Cursor.OPEN_HAND) return "OPEN_HAND";
        if (cursor == Cursor.CLOSED_HAND) return "CLOSED_HAND";
        if (cursor == Cursor.MOVE) return "MOVE";
        if (cursor == Cursor.HAND) return "HAND";
        if (cursor == Cursor.CROSSHAIR) return "CROSSHAIR";
        return "DEFAULT";
    }

    public List<PlanPoint> automationEdgeHandleScreenPoints() {
        return edgeResizeService.handles(activeLevel.get(), Set.copyOf(selectedSelections)).stream()
                .map(handle -> new PlanPoint(
                        toScreenProjectedX(handle.position(), 0.0),
                        toScreenProjectedY(handle.position(), 0.0)
                ))
                .toList();
    }

    public WorkbenchAutomationSnapshot automationInvoke(String actionName, Path path) {
        WorkbenchAutomationSnapshot result = null;
        switch (actionName) {
            case "undo" -> undo();
            case "redo" -> redo();
            case "clearSelection" -> clearSelection();
            case "deleteSelection" -> deleteSelection();
            case "applySelectionProperties" -> applyCurrentInputsToSelection();
            case "applyEndpointHeight" -> applyEndpointHeightToSelection();
            case "recognizeRoomFromSelectedWalls" -> recognizeRoomFromSelectedWalls();
            case "toggleSurfaceLayerVisibility" -> toggleSurfaceLayerVisibility();
            case "addSurfaceLayer" -> addSurfaceLayer();
            case "updateSurfaceLayer" -> updateSurfaceLayer();
            case "rotateSelectedComponentsClockwise", "rotateSelectedStairsClockwise" -> rotateSelectedComponentsClockwise();
            case "rotateSelectedComponentsCounterClockwise", "rotateSelectedStairsCounterClockwise" -> rotateSelectedComponentsCounterClockwise();
            case "exportProjectDxf" -> exportProjectAsDxf(requirePath(path, actionName));
            case "importProjectDxf" -> importProjectFromDxf(requirePath(path, actionName));
            case "exportLevelDxf" -> exportCurrentLevel(requirePath(path, actionName));
            case "importLevelDxf" -> importLevel(requirePath(path, actionName));
            case "exportSurfaceMaterialReportMarkdown" -> exportSurfaceMaterialReportMarkdown(requirePath(path, actionName));
            case "importPartLibrary" -> importPartLibrary(requirePath(path, actionName));
            case "exportWorkbenchSnapshot" -> exportWorkbenchSnapshot(requirePath(path, actionName));
            case "exportThreeDSnapshot" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.exportSnapshot(requirePath(path, actionName));
            }
            case "exportSubSceneSnapshot" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.exportSubSceneSnapshot(requirePath(path, actionName));
            }
            case "threeDOrbitLeft" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.automationOrbit(-15.0, 0.0);
            }
            case "threeDOrbitRight" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.automationOrbit(15.0, 0.0);
            }
            case "threeDOrbitUp" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.automationOrbit(0.0, 8.0);
            }
            case "threeDOrbitDown" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.automationOrbit(0.0, -8.0);
            }
            case "threeDPanRight" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.automationPan(90.0, 0.0);
            }
            case "threeDPanLeft" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.automationPan(-90.0, 0.0);
            }
            case "threeDPanUp" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.automationPan(0.0, -60.0);
            }
            case "threeDPanDown" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.automationPan(0.0, 60.0);
            }
            case "threeDZoomIn" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.automationZoom(0.92);
            }
            case "threeDZoomOut" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.automationZoom(1.08);
            }
            case "threeDFit" -> {
                activeWorkspaceMode.set(WorkspaceMode.THREE_D);
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.automationFitToScene();
            }
            case "threeDReset" -> {
                activeWorkspaceMode.set(WorkspaceMode.THREE_D);
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                threeDViewport.resetToDefaultView();
            }
            case "threeDViewportReset" -> {
                refreshThreeDIfNeeded();
                threeDViewport.resetToDefaultView();
            }
            case "diagnose3D" -> {
                activateThreeDWorkspaceForSnapshot();
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                result = automationSnapshot();
                // Diagnose wird in die cameraStatus-Info-Zeile geschrieben, damit sie
                // vom Automation-Snapshot zurückgegeben werden kann, ohne neue Felder anzulegen.
                String diagnose = threeDViewport.diagnoseRenderState();
                draftLabel.setText("DIAGNOSE: " + diagnose);
            }
            case "setProjection3D" -> {
                activeWorkspaceMode.set(WorkspaceMode.THREE_D);
                updateWorkspaceMode();
                refreshThreeDIfNeeded();
                String mode = path != null ? path.toString() : "ORTHOGRAPHIC";
                threeDViewport.setProjectionMode(de.andreas.cadas.application.view.ProjectionMode.valueOf(mode));
            }
            case "clearProject" -> clearProjectWithoutDialog();
            default -> throw new IllegalArgumentException("Automatisierungsaktion `" + actionName + "` ist unbekannt.");
        }
        return result;
    }

    private void activateThreeDWorkspaceForSnapshot() {
        if (activeWorkspaceMode.get() == WorkspaceMode.TWO_D) {
            activeWorkspaceMode.set(WorkspaceMode.THREE_D);
        }
    }

    private void exportWorkbenchSnapshot(Path path) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            applyCss();
            layout();
            WritableImage image = snapshot(null, null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", path.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Workbench-Snapshot konnte nicht geschrieben werden.", exception);
        }
    }

    private String automationSelectedRoomMetrics() {
        return selectedSurfaceRoom()
                .map(room -> String.format(
                        Locale.GERMAN,
                        "%.2f m² | %.2f m³",
                        surfaceLayerEffectService.effectiveAreaSquareMeters(activeLevel.get(), room),
                        surfaceLayerEffectService.effectiveVolumeCubicMeters(activeLevel.get(), room)
                ))
                .orElse("");
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
        fitCurrentViewToContent();
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
            case "endpointHeight" -> endpointHeightField;
            case "surfaceLayerThickness" -> surfaceLayerThicknessField;
            case "surfaceTileWidth" -> surfaceTileWidthField;
            case "surfaceTileHeight" -> surfaceTileHeightField;
            case "surfaceLayoutOffset" -> surfaceLayoutOffsetField;
            case "surfaceMinimumOffset" -> surfaceMinimumOffsetField;
            case "surfaceMinimumEdgeWidth" -> surfaceMinimumEdgeWidthField;
            case "surfaceMinimumStartEndMargin" -> surfaceMinimumStartEndMarginField;
            case "surfaceJointWidth" -> surfaceJointWidthField;
            case "roomName" -> roomNameField;
            case "roomHeight" -> roomHeightField;
            case "floorThickness" -> floorThicknessField;
            case "ceilingThickness" -> ceilingThicknessField;
            case "kneeWallHeight" -> kneeWallHeightField;
            case "doorWidth" -> doorWidthField;
            case "doorHeight" -> doorHeightField;
            case "threshold" -> thresholdField;
            case "windowWidth" -> windowWidthField;
            case "windowHeight" -> windowHeightField;
            case "sillHeight" -> sillHeightField;
            case "stairHeight" -> stairHeightField;
            case "stairSteps" -> stairStepsField;
            case "stairStartLanding" -> stairStartLandingField;
            case "stairEndLanding" -> stairEndLandingField;
            default -> throw new IllegalArgumentException("Eingabefeld `" + fieldName + "` ist unbekannt.");
        };
    }

    private ComboBox<LengthUnit> unitSelectorByName(String fieldName) {
        return switch (fieldName) {
            case "grid" -> gridUnit;
            case "length" -> lengthUnit;
            case "wallThickness" -> wallThicknessUnit;
            case "wallHeight" -> wallHeightUnit;
            case "endpointHeight" -> endpointHeightUnit;
            case "stairStartLanding" -> stairStartLandingUnit;
            case "stairEndLanding" -> stairEndLandingUnit;
            case "surfaceLayerThickness" -> surfaceLayerThicknessUnit;
            case "surfaceTileWidth" -> surfaceTileWidthUnit;
            case "surfaceTileHeight" -> surfaceTileHeightUnit;
            case "surfaceLayoutOffset" -> surfaceLayoutOffsetUnit;
            case "surfaceMinimumOffset" -> surfaceMinimumOffsetUnit;
            case "surfaceMinimumEdgeWidth" -> surfaceMinimumEdgeWidthUnit;
            case "surfaceMinimumStartEndMargin" -> surfaceMinimumStartEndMarginUnit;
            case "surfaceJointWidth" -> surfaceJointWidthUnit;
            case "roomHeight" -> roomHeightUnit;
            case "floorThickness" -> floorThicknessUnit;
            case "ceilingThickness" -> ceilingThicknessUnit;
            case "kneeWallHeight" -> kneeWallHeightUnit;
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
                || selectionKey.kind() == RenderableKind.STAIR;
    }

    private boolean isTranslatableSelection(SelectionKey selectionKey) {
        return selectionKey.kind() == RenderableKind.WALL
                || selectionKey.kind() == RenderableKind.STAIR
                || selectionKey.kind() == RenderableKind.ROOM_OBJECT;
    }

    private record SurfaceSelectionContext(SurfaceType surfaceType, List<String> targetKeys, String label, String hint) {
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
