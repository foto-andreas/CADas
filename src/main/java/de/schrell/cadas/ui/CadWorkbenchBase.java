package de.schrell.cadas.ui;

import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.formatSurfaceLayoutCorner;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.formatSurfaceLayoutDirection;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.normalizedSurfaceLayoutAnchor;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.startsAtMaximumX;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.startsAtMaximumY;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.surfaceLayoutRotatedQuarterTurn;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.surfaceLayoutSelectionDirection;
import static de.schrell.cadas.ui.CadWorkbenchCoveringSourceSupport.extractDwgBlockName;
import static de.schrell.cadas.ui.CadWorkbenchCoveringSourceSupport.formatCoveringSourceLabel;

import de.schrell.cadas.application.drawing.DraftingConstraints;
import de.schrell.cadas.application.drawing.DraftingService;
import de.schrell.cadas.application.drawing.DimensionLabelOptions;
import de.schrell.cadas.application.drawing.DimensionLabelPlacementService;
import de.schrell.cadas.application.drawing.DimensionLabelService;
import de.schrell.cadas.application.drawing.DimensionLineLayoutService;
import de.schrell.cadas.application.drawing.DimensionTextStyle;
import de.schrell.cadas.application.drawing.DimensionStandard;
import de.schrell.cadas.application.drawing.EdgeResizeService;
import de.schrell.cadas.application.drawing.GuideSnapService;
import de.schrell.cadas.application.drawing.GuideSnapTargets;
import de.schrell.cadas.application.drawing.HeatingZoneMirrorService;
import de.schrell.cadas.application.drawing.WallIntersectionSplitService;
import de.schrell.cadas.application.exchange.ExchangeFileNameService;
import de.schrell.cadas.application.floor.FloorOpeningGeometryService;
import de.schrell.cadas.application.history.UndoRedoStack;
import de.schrell.cadas.application.help.HelpContentService;
import de.schrell.cadas.application.help.MarkdownNavigationService;
import de.schrell.cadas.application.heating.HeatingCircuitRoutingService;
import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
import de.schrell.cadas.application.heating.RoomHeatingOutputService;
import de.schrell.cadas.application.drawing.OpeningPlacementService;
import de.schrell.cadas.application.drawing.OrthogonalCorrectionService;
import de.schrell.cadas.application.drawing.QuarterTurnRotationService;
import de.schrell.cadas.application.drawing.SelectionQueryService;
import de.schrell.cadas.application.drawing.SelectionTranslationService;
import de.schrell.cadas.application.drawing.SnapService;
import de.schrell.cadas.application.drawing.TextBlockingBox;
import de.schrell.cadas.application.drawing.WallDimensionPlacementService;
import de.schrell.cadas.application.drawing.WallEditingService;
import de.schrell.cadas.application.drawing.WallDimensionService;
import de.schrell.cadas.application.drawing.WallSnapService;
import de.schrell.cadas.application.drawing.WallEndpointSelection;
import de.schrell.cadas.application.dwg.DwgBlockDefinition;
import de.schrell.cadas.application.dwg.DwgConversionAvailability;
import de.schrell.cadas.application.dwg.DwgLibraryAnalysis;
import de.schrell.cadas.application.dwg.DwgLibraryAnalyzer;
import de.schrell.cadas.application.exchange.LevelExchangeService;
import de.schrell.cadas.application.exchange.ProjectExchangeService;
import de.schrell.cadas.application.layers.SurfaceCoveringPreset;
import de.schrell.cadas.application.layers.SurfaceCoveringPresetService;
import de.schrell.cadas.application.layers.DwgBlockCatalogService;
import de.schrell.cadas.application.layers.SurfaceLayerEffectService;
import de.schrell.cadas.application.layers.SurfaceLayerConsistencyService;
import de.schrell.cadas.application.layers.SurfaceRectangleTileLayoutService;
import de.schrell.cadas.application.layers.TileLayoutRequest;
import de.schrell.cadas.application.layers.TileLayoutService;
import de.schrell.cadas.application.layers.TilePlacement;
import de.schrell.cadas.application.layers.UserSurfaceCoveringPresetLibrary;
import de.schrell.cadas.application.layers.WallSurfaceSideService;
import de.schrell.cadas.application.layers.WallSurfaceTargetKey;
import de.schrell.cadas.application.objects.RoomObjectPreset;
import de.schrell.cadas.application.objects.RoomObjectPresetService;
import de.schrell.cadas.application.parts.DoorPreset;
import de.schrell.cadas.application.parts.PartLibraryImportService;
import de.schrell.cadas.application.parts.StairPreset;
import de.schrell.cadas.application.parts.StandardPartLibrary;
import de.schrell.cadas.application.parts.StandardPartLibraryService;
import de.schrell.cadas.application.parts.WindowPreset;
import de.schrell.cadas.application.reports.MarkdownHtmlRenderer;
import de.schrell.cadas.application.reports.ConstructionDrawingPdfService;
import de.schrell.cadas.application.reports.SurfaceMaterialListService;
import de.schrell.cadas.application.roof.RoofSlopeWallService;
import de.schrell.cadas.application.roof.RoofWindowPlacementService;
import de.schrell.cadas.application.stairs.StairUnderbuildService;
import de.schrell.cadas.application.room.AutoRoomGenerationService;
import de.schrell.cadas.application.terrain.TerrainContourService;
import de.schrell.cadas.application.terrain.TerrainEditService;
import de.schrell.cadas.application.terrain.TerrainGeometryService;
import de.schrell.cadas.application.terrain.TerrainProfileService;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.application.view.WallSurfaceOpeningService;
import de.schrell.cadas.application.view.WallSurfaceOpeningService.WallSurfaceInterval;
import de.schrell.cadas.application.view.WallSurfaceOpeningService.WallSurfaceRectangle;
import de.schrell.cadas.application.view.WallSurfacePlanGeometryService;
import de.schrell.cadas.application.view.WallSurfacePlanGeometryService.WallSurfacePlanPolygon;
import de.schrell.cadas.domain.geometry.Angle;
import de.schrell.cadas.domain.geometry.Grid;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.FloorExtensionPlacement;
import de.schrell.cadas.domain.model.FloorExtensionType;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingRoutingLanguage;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoofWindow;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectHeatingType;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import de.schrell.cadas.domain.model.SurfaceLayoutDirection;
import de.schrell.cadas.domain.model.SurfaceLayoutMargins;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;
import de.schrell.cadas.domain.model.WindowElement;
import de.schrell.cadas.infrastructure.dxf.DxfLevelExchangeService;
import de.schrell.cadas.infrastructure.dxf.DxfProjectExchangeService;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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
import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.SnapshotParameters;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.embed.swing.SwingFXUtils;

abstract class CadWorkbenchBase extends CadWorkbenchContracts {

    static final double BASE_PIXELS_PER_MILLIMETER = 0.10;
    static final double RULER_SIZE = 32.0;
    static final Length DEFAULT_GRID = Length.of(1, LengthUnit.CENTIMETER);
    static final Length DEFAULT_WALL_THICKNESS = Length.of(17.5, LengthUnit.CENTIMETER);
    static final Length DEFAULT_WALL_HEIGHT = Length.of(2.75, LengthUnit.METER);
    static final Length DEFAULT_ROOM_HEIGHT = Length.of(2.60, LengthUnit.METER);
    static final Length DEFAULT_FLOOR_THICKNESS = Length.of(18, LengthUnit.CENTIMETER);
    static final Length DEFAULT_CEILING_THICKNESS = Length.of(1, LengthUnit.MILLIMETER);
    static final Length DEFAULT_DOOR_WIDTH = Length.of(1.01, LengthUnit.METER);
    static final Length DEFAULT_DOOR_HEIGHT = Length.of(2.01, LengthUnit.METER);
    static final Length DEFAULT_WINDOW_WIDTH = Length.of(1.20, LengthUnit.METER);
    static final Length DEFAULT_WINDOW_HEIGHT = Length.of(1.20, LengthUnit.METER);
    static final Length DEFAULT_WINDOW_SILL = Length.of(90, LengthUnit.CENTIMETER);
    static final Length DEFAULT_STAIR_HEIGHT = Length.of(2.80, LengthUnit.METER);
    static final Length SNAP_TOLERANCE = Length.of(12, LengthUnit.CENTIMETER);
    static final double POINTER_SELECTION_TOLERANCE_PIXELS = 8.0;
    static final double DEFAULT_HKV_PAIR_DISTANCE_MILLIMETERS = 50.0;
    static final double VARIOTHERM_DETAIL_MIN_SCREEN_SPACING = 28.0;
    static final int LENGTH_INPUT_DECIMALS = 3;
    static final Font DIMENSION_LABEL_FONT = Font.font("Menlo", 12);
    static final Font ROUTING_COMMAND_FONT = Font.font("Serif", 14);
    static final double DIMENSION_TEXT_AWAY_DISTANCE = 8.0;
    static final double DIMENSION_PARALLEL_TEXT_AWAY_DISTANCE = 14.0;
    static final double DIMENSION_TEXT_PADDING = 6.0;
    static final Color CANVAS_BACKGROUND = Color.web("#fcfaf5");
    static final Color TERRAIN_FILL_COLOR = Color.color(0.65, 0.49, 0.27, 0.16);
    static final Color TERRAIN_EDGE_COLOR = Color.web("#8a6337");
    static final Color TERRAIN_LABEL_COLOR = Color.web("#6f4e2c");
    static final Color TERRAIN_ELEVATION_COLOR = Color.web("#a67c46");

    final StandardPartLibrary partLibrary = new StandardPartLibraryService().load();
    final PartLibraryImportService partLibraryImportService = new PartLibraryImportService();
    final AutoRoomGenerationService autoRoomGenerationService = new AutoRoomGenerationService();
    final TerrainContourService terrainContourService = new TerrainContourService();
    final TerrainEditService terrainEditService = new TerrainEditService();
    final TerrainGeometryService terrainGeometryService = new TerrainGeometryService();
    final TerrainProfileService terrainProfileService = new TerrainProfileService();
    final HydronicHeatingLayoutService hydronicHeatingLayoutService = new HydronicHeatingLayoutService();
    final HeatingCircuitRoutingService heatingCircuitRoutingService = new HeatingCircuitRoutingService();
    final RoomHeatingOutputService roomHeatingOutputService = new RoomHeatingOutputService();
    final Map<UUID, HydronicHeatingLayoutService.PlanningResult> heatingLayoutCache = new HashMap<>();
    final Set<UUID> heatingZonesPendingRoutingRegeneration = new HashSet<>();
    final Set<UUID> heatingLayoutsDirty = new HashSet<>();
    final RoofSlopeWallService roofSlopeWallService = new RoofSlopeWallService();
    final RoofWindowPlacementService roofWindowPlacementService = new RoofWindowPlacementService();
    final StairUnderbuildService stairUnderbuildService = new StairUnderbuildService();
    final DraftingService draftingService = new DraftingService();
    final EdgeResizeService edgeResizeService = new EdgeResizeService();
    final SnapService snapService = new SnapService();
    final GuideSnapService guideSnapService = new GuideSnapService();
    final WallSnapService wallSnapService = new WallSnapService();
    final SelectionQueryService selectionQueryService = new SelectionQueryService();
    final ExchangeFileNameService exchangeFileNameService = new ExchangeFileNameService();
    final OpeningPlacementService openingPlacementService = new OpeningPlacementService();
    final WallEditingService wallEditingService = new WallEditingService();
    final WallDimensionService wallDimensionService = new WallDimensionService();
    final WallDimensionPlacementService wallDimensionPlacementService = new WallDimensionPlacementService();
    final DimensionLineLayoutService dimensionLineLayoutService = new DimensionLineLayoutService();
    final DimensionLabelService dimensionLabelService = new DimensionLabelService();
    final DimensionLabelPlacementService dimensionLabelPlacementService = new DimensionLabelPlacementService();
    final QuarterTurnRotationService quarterTurnRotationService = new QuarterTurnRotationService();
    final HeatingZoneMirrorService heatingZoneMirrorService = new HeatingZoneMirrorService();
    final OrthogonalCorrectionService orthogonalCorrectionService = new OrthogonalCorrectionService();
    final SelectionTranslationService selectionTranslationService = new SelectionTranslationService();
    final LevelExchangeService levelExchangeService = new DxfLevelExchangeService();
    final ProjectExchangeService projectExchangeService = new DxfProjectExchangeService();
    Path lastProjectSavePath;
    Path lastLevelSavePath;
    final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();
    final TileLayoutService tileLayoutService = new TileLayoutService();
    final SurfaceRectangleTileLayoutService surfaceRectangleTileLayoutService = new SurfaceRectangleTileLayoutService();
    final SurfaceLayerConsistencyService surfaceLayerConsistencyService = new SurfaceLayerConsistencyService();
    final WallSurfaceSideService wallSurfaceSideService = new WallSurfaceSideService();
    final WallSurfaceOpeningService wallSurfaceOpeningService = new WallSurfaceOpeningService();
    final WallSurfacePlanGeometryService wallSurfacePlanGeometryService = new WallSurfacePlanGeometryService();
    final WallIntersectionSplitService wallIntersectionSplitService = new WallIntersectionSplitService();
    final FloorOpeningGeometryService floorOpeningGeometryService = new FloorOpeningGeometryService();
    final GuideDistanceService guideDistanceService = new GuideDistanceService();
    final PointerCursorService pointerCursorService = new PointerCursorService();
    final TwoDZoomRange twoDZoomRange = new TwoDZoomRange();
    final SurfaceCoveringPresetService surfaceCoveringPresetService = new SurfaceCoveringPresetService();
    final UserSurfaceCoveringPresetLibrary userSurfacePresetLibrary = new UserSurfaceCoveringPresetLibrary();
    final SurfaceMaterialListService surfaceMaterialListService = new SurfaceMaterialListService();
    final ConstructionDrawingPdfService constructionDrawingPdfService = new ConstructionDrawingPdfService();
    final HelpContentService helpContentService = new HelpContentService();
    final MarkdownNavigationService markdownNavigationService = new MarkdownNavigationService();
    final MarkdownHtmlRenderer markdownHtmlRenderer = new MarkdownHtmlRenderer();
    final DwgBlockCatalogService dwgBlockCatalogService = new DwgBlockCatalogService();
    final RoomObjectPresetService roomObjectPresetService = new RoomObjectPresetService();
    final DwgLibraryAnalyzer dwgLibraryAnalyzer = new DwgLibraryAnalyzer();
    final CadWorkbenchDocumentSupport documentSupport = new CadWorkbenchDocumentSupport(this);
    final CadWorkbenchVariothermGrooveRenderer variothermGrooveRenderer = new CadWorkbenchVariothermGrooveRenderer();
    SurfaceType preferredRoomSurfaceType = SurfaceType.FLOOR;
    final ProjectModel project = ProjectModel.withDefaultLevel("Neues Projekt", "Erdgeschoss");

    final ObjectProperty<Level> activeLevel = new SimpleObjectProperty<>(project.primaryLevel());
    final ObjectProperty<ViewOrientation> activeView = new SimpleObjectProperty<>(ViewOrientation.TOP);
    final ObjectProperty<WorkspaceMode> activeWorkspaceMode = new SimpleObjectProperty<>(WorkspaceMode.TWO_D);
    final BooleanProperty snapToGrid = new SimpleBooleanProperty(true);
    final BooleanProperty showGrid = new SimpleBooleanProperty(true);
    final BooleanProperty snapToEndpoints = new SimpleBooleanProperty(true);
    final BooleanProperty showCompass = new SimpleBooleanProperty(true);
    final BooleanProperty showDimensions = new SimpleBooleanProperty(true);
    final ObjectProperty<DimensionTextStyle> dimensionTextStyle = new SimpleObjectProperty<>(DimensionTextStyle.LENGTH_ONLY);
    final BooleanProperty showAreaVolume = new SimpleBooleanProperty(true);
    final BooleanProperty showRoomObjects = new SimpleBooleanProperty(true);
    final BooleanProperty showTerrainInPlan = new SimpleBooleanProperty(true);
    final BooleanProperty showHeatingCircuits = new SimpleBooleanProperty(true);
    final BooleanProperty showVariothermCircles = new SimpleBooleanProperty(true);
    final BooleanProperty showGuides = new SimpleBooleanProperty(true);
    final BooleanProperty showGuideDistances = new SimpleBooleanProperty(true);
    final BooleanProperty snapToGuides = new SimpleBooleanProperty(true);
    final BooleanProperty snapToWalls = new SimpleBooleanProperty(true);
    final BooleanProperty autoRouteHeatingZoneOnResize = new SimpleBooleanProperty(true);

    final Canvas drawingCanvas = new Canvas();
    final Canvas horizontalRuler = new Canvas();
    final Canvas verticalRuler = new Canvas();
    final Pane drawingPane = new Pane(drawingCanvas);
    final BorderPane drawingArea = new BorderPane();
    final StackPane workspacePane = new StackPane();
    final ObservableList<Level> availableLevels = FXCollections.observableArrayList(project.levels());

    final TextField gridField = new TextField("1");
    final ComboBox<LengthUnit> gridUnit = new ComboBox<>();
    final TextField lengthField = new TextField();
    final ComboBox<LengthUnit> lengthUnit = new ComboBox<>();
    final TextField angleField = new TextField();
    final TextField northAngleField = new TextField("0");
    final TextField wallThicknessField = new TextField("17,5");
    final ComboBox<LengthUnit> wallThicknessUnit = new ComboBox<>();
    final TextField wallHeightField = new TextField("275");
    final ComboBox<LengthUnit> wallHeightUnit = new ComboBox<>();
    final TextField endpointHeightField = new TextField("275");
    final ComboBox<LengthUnit> endpointHeightUnit = new ComboBox<>();
    final TextField roomNameField = new TextField("Raum");
    final TextField roomHeightField = new TextField("260");
    final ComboBox<LengthUnit> roomHeightUnit = new ComboBox<>();
    final TextField floorThicknessField = new TextField("18");
    final ComboBox<LengthUnit> floorThicknessUnit = new ComboBox<>();
    final TextField ceilingThicknessField = new TextField("0,1");
    final ComboBox<LengthUnit> ceilingThicknessUnit = new ComboBox<>();
    final TextField kneeWallHeightField = new TextField("100");
    final ComboBox<LengthUnit> kneeWallHeightUnit = new ComboBox<>();
    final Label roofSlopeManagementLabel = new Label("Wand-Kontextmenü");
    final TextField doorWidthField = new TextField("101");
    final ComboBox<LengthUnit> doorWidthUnit = new ComboBox<>();
    final TextField doorHeightField = new TextField("201");
    final ComboBox<LengthUnit> doorHeightUnit = new ComboBox<>();
    final TextField thresholdField = new TextField("0");
    final ComboBox<LengthUnit> thresholdUnit = new ComboBox<>();
    final TextField windowWidthField = new TextField("120");
    final ComboBox<LengthUnit> windowWidthUnit = new ComboBox<>();
    final TextField windowHeightField = new TextField("120");
    final ComboBox<LengthUnit> windowHeightUnit = new ComboBox<>();
    final TextField sillHeightField = new TextField("90");
    final ComboBox<LengthUnit> sillHeightUnit = new ComboBox<>();
    final ComboBox<DoorPreset> doorPresetSelector = new ComboBox<>();
    final ComboBox<WindowPreset> windowPresetSelector = new ComboBox<>();
    final ComboBox<StairPreset> stairPresetSelector = new ComboBox<>();
    final ComboBox<RoomObjectPreset> roomObjectPresetSelector = new ComboBox<>();
    final TextField roomObjectNameField = new TextField("Objekt");
    final TextField roomObjectWidthField = new TextField("90");
    final ComboBox<LengthUnit> roomObjectWidthUnit = new ComboBox<>();
    final TextField roomObjectDepthField = new TextField("90");
    final ComboBox<LengthUnit> roomObjectDepthUnit = new ComboBox<>();
    final TextField roomObjectHeightField = new TextField("200");
    final ComboBox<LengthUnit> roomObjectHeightUnit = new ComboBox<>();
    final TextField roomObjectHeatOutputField = new TextField("0");
    final ComboBox<RoomObjectHeatingType> roomObjectHeatingTypeSelector = new ComboBox<>();
    final TextField roomObjectBaseElevationField = new TextField("0");
    final ComboBox<LengthUnit> roomObjectBaseElevationUnit = new ComboBox<>();
    final TextField roomObjectAngleField = new TextField("0");
    final TextField stairHeightField = new TextField("280");
    final ComboBox<LengthUnit> stairHeightUnit = new ComboBox<>();
    final TextField stairStepsField = new TextField("16");
    final TextField stairStartLandingField = new TextField("0");
    final ComboBox<LengthUnit> stairStartLandingUnit = new ComboBox<>();
    final TextField stairEndLandingField = new TextField("0");
    final ComboBox<LengthUnit> stairEndLandingUnit = new ComboBox<>();
    final TextField stairLeftUnderbuildField = new TextField("0");
    final ComboBox<LengthUnit> stairLeftUnderbuildUnit = new ComboBox<>();
    final TextField stairRightUnderbuildField = new TextField("0");
    final ComboBox<LengthUnit> stairRightUnderbuildUnit = new ComboBox<>();
    final TextField stairUndersideThicknessField = new TextField("0");
    final ComboBox<LengthUnit> stairUndersideThicknessUnit = new ComboBox<>();
    final ComboBox<FloorExtensionType> floorExtensionTypeSelector = new ComboBox<>();
    final ComboBox<FloorExtensionPlacement> floorExtensionPlacementSelector = new ComboBox<>();
    final TextField floorExtensionThicknessField = new TextField("18");
    final ComboBox<LengthUnit> floorExtensionThicknessUnit = new ComboBox<>();
    final ComboBox<HeatingSurfacePosition> heatingSurfacePositionSelector = new ComboBox<>();
    final ComboBox<HeatingLayoutPattern> heatingLayoutPatternSelector = new ComboBox<>();
    final TextField heatingPipeSpacingField = new TextField("10");
    final ComboBox<LengthUnit> heatingPipeSpacingUnit = new ComboBox<>();
    final TextField heatingPipeDiameterField = new TextField("1,16");
    final ComboBox<LengthUnit> heatingPipeDiameterUnit = new ComboBox<>();
    final TextField heatingMaximumPipeLengthField = new TextField("8000");
    final ComboBox<LengthUnit> heatingMaximumPipeLengthUnit = new ComboBox<>();
    final TextField heatingWallClearanceField = new TextField("10");
    final ComboBox<LengthUnit> heatingWallClearanceUnit = new ComboBox<>();
    final TextField heatingSupplyXField = new TextField("0");
    final ComboBox<LengthUnit> heatingSupplyXUnit = new ComboBox<>();
    final TextField heatingSupplyYField = new TextField("0");
    final ComboBox<LengthUnit> heatingSupplyYUnit = new ComboBox<>();
    final TextField heatingReturnXField = new TextField("5");
    final ComboBox<LengthUnit> heatingReturnXUnit = new ComboBox<>();
    final TextField heatingReturnYField = new TextField("0");
    final ComboBox<LengthUnit> heatingReturnYUnit = new ComboBox<>();
    final ListView<String> heatingZoneList = new ListView<>();
    final TextField heatingZoneNameField = new TextField("Heizkreis");
    final ComboBox<HeatingLayoutPattern> heatingZoneLayoutPatternSelector = new ComboBox<>();
    final CheckBox heatingZoneFlowInvertedCheckBox = new CheckBox("Vorlauf und Rücklauf tauschen");
    final CheckBox heatingZoneSerpentineMiddleLineCheckBox = new CheckBox("Mittelschlange aktiv");
    final TextField heatingZoneHeatOutputField = new TextField("0");
    final TextArea heatingZonePointArea = new TextArea();
    final TextArea heatingRoutingCommandArea = new TextArea();
    final CheckBox autoRouteHeatingZoneOnResizeCheckBox = new CheckBox("Auto-Routing nach Rechteckänderung");
    final Label heatingSummaryLabel = new Label("Keine Heizfläche angelegt.");
    final ComboBox<SurfaceType> surfaceTypeSelector = new ComboBox<>();
    final ComboBox<SurfaceCoveringPreset> surfacePresetSelector = new ComboBox<>();
    final ListView<String> surfaceLayerList = new ListView<>();
    final TextField surfaceLayerNameField = new TextField("Belag");
    final TextField surfaceLayerThicknessField = new TextField("1,2");
    final ComboBox<LengthUnit> surfaceLayerThicknessUnit = new ComboBox<>();
    final TextField surfaceTileWidthField = new TextField("60");
    final ComboBox<LengthUnit> surfaceTileWidthUnit = new ComboBox<>();
    final TextField surfaceTileHeightField = new TextField("30");
    final ComboBox<LengthUnit> surfaceTileHeightUnit = new ComboBox<>();
    final Button surfaceLayoutCornerPreviousButton = new Button("←");
    final Label surfaceLayoutCornerLabel = new Label("Unten links");
    final Button surfaceLayoutCornerNextButton = new Button("→");
    final ComboBox<SurfaceLayoutDirection> surfaceLayoutDirectionSelector = new ComboBox<>();
    final ComboBox<SurfaceLayoutMode> surfaceLayoutModeSelector = new ComboBox<>();
    final TextField surfaceLayoutOffsetField = new TextField("0");
    final ComboBox<LengthUnit> surfaceLayoutOffsetUnit = new ComboBox<>();
    final TextField surfaceMinimumOffsetField = new TextField("10");
    final ComboBox<LengthUnit> surfaceMinimumOffsetUnit = new ComboBox<>();
    final TextField surfaceMinimumEdgeWidthField = new TextField("8");
    final ComboBox<LengthUnit> surfaceMinimumEdgeWidthUnit = new ComboBox<>();
    final TextField surfaceMinimumStartEndMarginField = new TextField("8");
    final ComboBox<LengthUnit> surfaceMinimumStartEndMarginUnit = new ComboBox<>();
    final TextField surfaceFreeMarginLeftField = new TextField("0");
    final ComboBox<LengthUnit> surfaceFreeMarginLeftUnit = new ComboBox<>();
    final TextField surfaceFreeMarginRightField = new TextField("0");
    final ComboBox<LengthUnit> surfaceFreeMarginRightUnit = new ComboBox<>();
    final TextField surfaceFreeMarginTopField = new TextField("0");
    final ComboBox<LengthUnit> surfaceFreeMarginTopUnit = new ComboBox<>();
    final TextField surfaceFreeMarginBottomField = new TextField("0");
    final ComboBox<LengthUnit> surfaceFreeMarginBottomUnit = new ComboBox<>();
    final TextField surfaceJointWidthField = new TextField("0,2");
    final ComboBox<LengthUnit> surfaceJointWidthUnit = new ComboBox<>();
    final ComboBox<SurfaceCutRestriction> surfaceCutRestrictionSelector = new ComboBox<>();
    final TextField dwgBlockNameField = new TextField();
    final TextField dwgBlockSearchField = new TextField();
    final ComboBox<DwgBlockDefinition> dwgBlockSelector = new ComboBox<>();
    final ComboBox<RoomObjectMountingMode> dwgObjectFloorModeSelector = new ComboBox<>();
    final Label dwgStatusLabel = new Label("Noch keine DWG-Bibliothek analysiert.");
    final Label dwgBlockDetailLabel = new Label("Kein DWG-Block ausgewählt.");
    final Canvas dwgPreviewCanvas = new Canvas(220, 150);
    final Label surfaceLayerTargetLabel = new Label("Keine Fläche ausgewählt.");
    final Label surfaceLayerSelectionHintLabel = new Label("Für Beläge zuerst eine passende Fläche auswählen.");
    final Label surfaceLayerCoverageLabel = new Label("Keine Ebenen ausgewählt.");
    final ObjectProperty<SurfaceLayoutAnchor> surfaceLayoutAnchorSelection = new SimpleObjectProperty<>(SurfaceLayoutAnchor.MIN_X_MIN_Y);
    final ComboBox<Level> levelSelector = new ComboBox<>();
    final ComboBox<DrawingTool> toolSelector = new ComboBox<>();
    final ObservableList<DoorPreset> availableDoorPresets = FXCollections.observableArrayList();
    final ObservableList<WindowPreset> availableWindowPresets = FXCollections.observableArrayList();
    final ObservableList<StairPreset> availableStairPresets = FXCollections.observableArrayList();
    final ObservableList<RoomObjectPreset> availableRoomObjectPresets = FXCollections.observableArrayList();
    final ObservableList<SurfaceCoveringPreset> availableSurfacePresets = FXCollections.observableArrayList();
    final ObservableList<DwgBlockDefinition> availableDwgBlocks = FXCollections.observableArrayList();
    final ThreeDViewport threeDViewport = new ThreeDViewport(this::handleThreeDSelection, this::switchToThreeDWorkspaceFromViewport);
    final ViewProjectionService projectionService = new ViewProjectionService();
    final ProjectedModelBoundsService projectedBoundsService = new ProjectedModelBoundsService();
    final UndoRedoStack<WorkbenchSnapshot> history = new UndoRedoStack<>();
    final VBox propertySections = new VBox(12.0);
    final Map<DrawingTool, Map<String, Boolean>> propertySectionExpandedByTool = new EnumMap<>(DrawingTool.class);
    boolean applyingPropertySectionExpansionState;
    final Label selectionSummaryLabel = new Label("Keine Auswahl");
    final Button undoButton = new Button("Rückgängig");
    final Button redoButton = new Button("Wiederherstellen");
    final Button deleteSelectionButton = new Button("Auswahl löschen");
    final Button clearSelectionButton = new Button("Auswahl aufheben");
    Button addLevelButton;
    Button renameLevelButton;
    Button moveLevelUpButton;
    Button moveLevelDownButton;
    final Button applySelectionPropertiesButton = new Button("Werte auf Auswahl anwenden");
    final Button applyEndpointHeightButton = new Button("Eckhöhe anwenden");
    final Button addSurfaceLayerButton = new Button("Ebene hinzufügen");
    final Button updateSurfaceLayerButton = new Button("Ebene aktualisieren");
    final Button removeSurfaceLayerButton = new Button("Ebene entfernen");
    final Button toggleSurfaceLayerVisibilityButton = new Button("Sichtbarkeit umschalten");
    final Button moveSurfaceLayerUpButton = new Button("Nach oben");
    final Button moveSurfaceLayerDownButton = new Button("Nach unten");
    final Button saveSurfacePresetButton = new Button("Speichern");
    final Button addDwgBlockPresetButton = new Button("DWG-Block hinzufügen");
    final Button refreshDwgLibraryButton = new Button("DWG prüfen");
    final Button addDwgBlockAsSurfaceButton = new Button("Als Belag");
    final Button addDwgBlockAsObjectButton = new Button("Als Objekt");
    final Button planHeatingButton = new Button("Raumplanung pausiert");
    final Button applyHeatingZoneSettingsButton = new Button("Heizkreis übernehmen");
    final Button generateHeatingZoneRoutingButton = new Button("Routing generieren");
    final Button applyHeatingRoutingCommandButton = new Button("Routing übernehmen");
    final ContextMenu selectionContextMenu = new ContextMenu();
    final Label cadLibrarySummaryLabel = new Label("Keine externen CAD-Bibliotheken registriert.");

    final Label zoomLabel = new Label();
    final Label cursorLabel = new Label();
    final Label draftLabel = new Label();
    final Label viewLabel = new Label();

    final ObservableList<GuideLine> guideLines = FXCollections.observableArrayList();
    final ObservableList<Path> cadLibraryReferences = FXCollections.observableArrayList();
    final Map<Path, DwgLibraryAnalysis> dwgAnalysesByPath = new LinkedHashMap<>();
    final LinkedHashSet<SelectionKey> selectedSelections = new LinkedHashSet<>();

    double zoom = 1.0;
    double offsetX = 240.0;
    double offsetY = 160.0;
    double panStartX;
    double panStartY;
    double panOriginX;
    double panOriginY;
    boolean panning;
    boolean panningMoved;
    SelectionKey pendingContextSelection;
    PlanPoint pendingContextWorldPoint;
    SelectionKey contextMenuSelection;
    PlanPoint contextMenuWorldPoint;
    boolean updatingLengthInput;
    boolean updatingHeatingRoutingInput;
    boolean updatingHeatingZoneSelection;
    PlanPoint draftStart;
    PlanSegment previewSegment;
    PlanPoint lastCursor = new PlanPoint(0.0, 0.0);
    WallEndpointSelection selectedEndpointGroup;
    final ObjectProperty<SelectionKey> selectedSelection = new SimpleObjectProperty<>();
    GuideOrientation pendingGuideOrientation;
    double pendingGuideWorldMillimeters;
    boolean threeDDirty = true;
    boolean keepViewportOrbitPoseOnNextThreeDActivation;
    boolean historyCapturedForDrag;
    PlanPoint selectionDragAnchor;
    PlanPoint selectionRectangleStart;
    PlanPoint selectionRectangleEnd;
    boolean selectionRectangleToggle;
    List<Wall> selectionDragBaseWalls = List.of();
    List<Staircase> selectionDragBaseStaircases = List.of();
    List<RoomObject> selectionDragBaseRoomObjects = List.of();
    List<FloorOpening> selectionDragBaseFloorOpenings = List.of();
    List<HeatingExclusionArea> selectionDragBaseHeatingExclusionAreas = List.of();
    List<HydronicHeating> selectionDragBaseHydronicHeatings = List.of();
    UUID openingDragId;
    PlanSegment openingDragWallAxis;
    double openingDragWidth;
    double openingDragOffsetDelta;
    EdgeResizeService.EdgeHandle activeEdgeHandle;
    List<Wall> edgeResizeBaseWalls = List.of();
    List<Door> edgeResizeBaseDoors = List.of();
    List<WindowElement> edgeResizeBaseWindows = List.of();
    List<Staircase> edgeResizeBaseStaircases = List.of();
    List<FloorOpening> edgeResizeBaseFloorOpenings = List.of();
    List<HeatingExclusionArea> edgeResizeBaseHeatingExclusionAreas = List.of();
    List<HydronicHeating> edgeResizeBaseHydronicHeatings = List.of();
    double lastMouseX;
    double lastMouseY;
    boolean altPressed;
    boolean spacePressed;
    // Steuert alle blockierenden UI-Dialoge (Fehler-, Bestätigungs-, Erfolgs- und Eingabedialoge).
    // Wird durch die Automatisierung deaktiviert, damit Tests nicht an Dialogen hängen bleiben.
    boolean interactiveDialogsEnabled = true;
    boolean applicationExitRequested;
    boolean applicationExitConfirmed;
    Boolean automatedUnsavedChangesExitDecision;
    long currentChangeRevision;
    long savedChangeRevision;
    long nextChangeRevision = 1;
    Runnable applicationExitAction = Platform::exit;
    UiErrorDialogs.ErrorPresentation lastErrorDialog = UiErrorDialogs.ErrorPresentation.empty();
    WarningPresentation lastWarningDialog = WarningPresentation.empty();
    int rememberedWarningCount;
    boolean reportSnapshotRestrictSurfaceLayers;
    Set<UUID> reportSnapshotVisibleSurfaceLayerIds = Set.of();
    boolean reportSnapshotHideHydronicHeatings;
    Set<HeatingSurfacePosition> reportSnapshotVisibleHydronicSurfacePositions = Set.of();
    boolean reportSnapshotFilterHeatingRoomObjects;
    Set<RoomObjectHeatingType> reportSnapshotVisibleHeatingObjectTypes = Set.of();
    boolean reportSnapshotInteriorRoomDimensionsOnly;
    ScreenBounds lastPlanDimensionScreenBounds;
    Level.RoomReplacementImpact pendingRoomSynchronizationImpact = emptyRoomSynchronizationImpact();















































    Window currentWindow() {
        return getScene() != null ? getScene().getWindow() : null;
    }






















































    DimensionLabelOptions currentDimensionLabelOptions() {
        return new DimensionLabelOptions(dimensionTextStyle.get());
    }



































    /**
     * Zeichnet die Raumtexte und liefert ihre Sperrflächen zurück, damit
     * {@link #drawWallDimensions} die Maßtexte nicht über Raumangaben legt.
     */




































































































































































































































































































































































































    WritableImage reportLevelSnapshot(String levelName) {
        return reportSnapshot(
                levelName,
                null,
                0.0,
                new ReportSnapshotOptions(false, Set.of(), false, Set.of(), true, true, false, true, Set.of(), 2.0)
        );
    }

    WritableImage reportMaterialOverviewSnapshot(String levelName) {
        return reportSnapshot(
                levelName,
                null,
                0.0,
                new ReportSnapshotOptions(true, Set.of(), false, Set.of(), true, true, true, true, Set.of(), 2.0)
        );
    }

    WritableImage reportLevelSnapshot(String levelName, Set<UUID> visibleSurfaceLayerIds, boolean includeHydronicHeating) {
        return reportLevelSnapshot(levelName, visibleSurfaceLayerIds, includeHydronicHeating, Set.of());
    }

    WritableImage reportLevelSnapshot(
            String levelName,
            Set<UUID> visibleSurfaceLayerIds,
            boolean includeHydronicHeating,
            Set<RoomObjectHeatingType> visibleHeatingObjectTypes
    ) {
        return reportLevelSnapshot(levelName, visibleSurfaceLayerIds, includeHydronicHeating, visibleHeatingObjectTypes, Set.of());
    }

    WritableImage reportLevelSnapshot(
            String levelName,
            Set<UUID> visibleSurfaceLayerIds,
            boolean includeHydronicHeating,
            Set<RoomObjectHeatingType> visibleHeatingObjectTypes,
            Set<HeatingSurfacePosition> visibleHydronicSurfacePositions
    ) {
        return reportSnapshot(
                levelName,
                null,
                0.0,
                new ReportSnapshotOptions(true, visibleSurfaceLayerIds, includeHydronicHeating, visibleHydronicSurfacePositions, false, true, false, true, visibleHeatingObjectTypes, 2.0)
        );
    }

    WritableImage reportRoomSnapshot(String levelName, List<PlanPoint> focusPoints) {
        return reportSnapshot(levelName, List.copyOf(focusPoints), 280.0, ReportSnapshotOptions.defaults().hideHeatingRoomObjects().withRenderScale(2.0));
    }

    WritableImage reportRoomSnapshot(
            String levelName,
            List<PlanPoint> focusPoints,
            Set<UUID> visibleSurfaceLayerIds,
            boolean includeHydronicHeating
    ) {
        return reportSnapshot(
                levelName,
                List.copyOf(focusPoints),
                280.0,
                new ReportSnapshotOptions(true, visibleSurfaceLayerIds, includeHydronicHeating, Set.of(), false, true, false, true, Set.of(), 2.0)
        );
    }























































    // Erkennt Automatisierungs- bzw. Testumgebungen, in denen blockierende Dialoge vermieden werden müssen.
    static boolean automationActive() {
        return Boolean.parseBoolean(System.getProperty("cadas.automation.enabled", "false"))
                || "1".equals(System.getenv("CADAS_AUTOMATION"));
    }





























    @SuppressWarnings("unchecked")

    static Level.RoomReplacementImpact emptyRoomSynchronizationImpact() {
        return new Level.RoomReplacementImpact(0, List.of(), List.of(), 0, 0, 0, 0, 0);
    }
}
