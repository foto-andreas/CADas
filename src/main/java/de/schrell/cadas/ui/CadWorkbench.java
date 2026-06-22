package de.schrell.cadas.ui;

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
import de.schrell.cadas.application.exchange.ExchangeFileNameService;
import de.schrell.cadas.application.history.UndoRedoStack;
import de.schrell.cadas.application.help.HelpContentService;
import de.schrell.cadas.application.help.MarkdownNavigationService;
import de.schrell.cadas.application.help.MarkdownNavigationService.HelpSection;
import de.schrell.cadas.application.help.AboutInformation;
import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
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
import de.schrell.cadas.application.reports.ConstructionDrawingOptions;
import de.schrell.cadas.application.reports.ConstructionDrawingPdfService;
import de.schrell.cadas.application.reports.SurfaceMaterialListService;
import de.schrell.cadas.application.roof.RoofSlopeWallService;
import de.schrell.cadas.application.roof.RoofWindowPlacementService;
import de.schrell.cadas.application.stairs.StairUnderbuildService;
import de.schrell.cadas.application.room.AutoRoomGenerationService;
import de.schrell.cadas.application.terrain.TerrainCornerService;
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
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoofWindow;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;
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
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
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
    private static final Length DEFAULT_GRID = Length.of(1, LengthUnit.CENTIMETER);
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
    private static final double DEFAULT_HKV_PAIR_DISTANCE_MILLIMETERS = 50.0;
    private static final int LENGTH_INPUT_DECIMALS = 3;
    private static final Font DIMENSION_LABEL_FONT = Font.font("Menlo", 12);
    private static final double DIMENSION_TEXT_AWAY_DISTANCE = 8.0;
    private static final double DIMENSION_PARALLEL_TEXT_AWAY_DISTANCE = 14.0;
    private static final double DIMENSION_TEXT_PADDING = 6.0;
    private static final double DIMENSION_LINE_BLOCKING_PADDING = 4.0;

    private final StandardPartLibrary partLibrary = new StandardPartLibraryService().load();
    private final PartLibraryImportService partLibraryImportService = new PartLibraryImportService();
    private final AutoRoomGenerationService autoRoomGenerationService = new AutoRoomGenerationService();
    private final TerrainCornerService terrainCornerService = new TerrainCornerService();
    private final HydronicHeatingLayoutService hydronicHeatingLayoutService = new HydronicHeatingLayoutService();
    private final RoofSlopeWallService roofSlopeWallService = new RoofSlopeWallService();
    private final RoofWindowPlacementService roofWindowPlacementService = new RoofWindowPlacementService();
    private final StairUnderbuildService stairUnderbuildService = new StairUnderbuildService();
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
    private final WallDimensionPlacementService wallDimensionPlacementService = new WallDimensionPlacementService();
    private final DimensionLineLayoutService dimensionLineLayoutService = new DimensionLineLayoutService();
    private final DimensionLabelService dimensionLabelService = new DimensionLabelService();
    private final DimensionLabelPlacementService dimensionLabelPlacementService = new DimensionLabelPlacementService();
    private final QuarterTurnRotationService quarterTurnRotationService = new QuarterTurnRotationService();
    private final OrthogonalCorrectionService orthogonalCorrectionService = new OrthogonalCorrectionService();
    private final SelectionTranslationService selectionTranslationService = new SelectionTranslationService();
    private final LevelExchangeService levelExchangeService = new DxfLevelExchangeService();
    private final ProjectExchangeService projectExchangeService = new DxfProjectExchangeService();
    private Path lastProjectSavePath;
    private Path lastLevelSavePath;
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
    private final MarkdownNavigationService markdownNavigationService = new MarkdownNavigationService();
    private final MarkdownHtmlRenderer markdownHtmlRenderer = new MarkdownHtmlRenderer();
    private final DwgBlockCatalogService dwgBlockCatalogService = new DwgBlockCatalogService();
    private final RoomObjectPresetService roomObjectPresetService = new RoomObjectPresetService();
    private final DwgLibraryAnalyzer dwgLibraryAnalyzer = new DwgLibraryAnalyzer();
    private SurfaceType preferredRoomSurfaceType = SurfaceType.FLOOR;
    private final ProjectModel project = ProjectModel.withDefaultLevel("Neues Projekt", "Erdgeschoss");

    private final ObjectProperty<Level> activeLevel = new SimpleObjectProperty<>(project.primaryLevel());
    private final ObjectProperty<ViewOrientation> activeView = new SimpleObjectProperty<>(ViewOrientation.TOP);
    private final ObjectProperty<WorkspaceMode> activeWorkspaceMode = new SimpleObjectProperty<>(WorkspaceMode.TWO_D);
    private final BooleanProperty snapToGrid = new SimpleBooleanProperty(true);
    private final BooleanProperty snapToEndpoints = new SimpleBooleanProperty(true);
    private final BooleanProperty showCompass = new SimpleBooleanProperty(true);
    private final BooleanProperty showDimensions = new SimpleBooleanProperty(true);
    private final ObjectProperty<DimensionTextStyle> dimensionTextStyle = new SimpleObjectProperty<>(DimensionTextStyle.LENGTH_ONLY);
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

    private final TextField gridField = new TextField("1");
    private final ComboBox<LengthUnit> gridUnit = new ComboBox<>();
    private final TextField lengthField = new TextField();
    private final ComboBox<LengthUnit> lengthUnit = new ComboBox<>();
    private final TextField angleField = new TextField();
    private final TextField northAngleField = new TextField("0");
    private final TextField wallThicknessField = new TextField("17,5");
    private final ComboBox<LengthUnit> wallThicknessUnit = new ComboBox<>();
    private final TextField wallHeightField = new TextField("275");
    private final ComboBox<LengthUnit> wallHeightUnit = new ComboBox<>();
    private final TextField endpointHeightField = new TextField("275");
    private final ComboBox<LengthUnit> endpointHeightUnit = new ComboBox<>();
    private final TextField roomNameField = new TextField("Raum");
    private final TextField roomHeightField = new TextField("260");
    private final ComboBox<LengthUnit> roomHeightUnit = new ComboBox<>();
    private final TextField floorThicknessField = new TextField("18");
    private final ComboBox<LengthUnit> floorThicknessUnit = new ComboBox<>();
    private final TextField ceilingThicknessField = new TextField("0,1");
    private final ComboBox<LengthUnit> ceilingThicknessUnit = new ComboBox<>();
    private final TextField kneeWallHeightField = new TextField("100");
    private final ComboBox<LengthUnit> kneeWallHeightUnit = new ComboBox<>();
    private final Label roofSlopeManagementLabel = new Label("Wand-Kontextmenü");
    private final TextField doorWidthField = new TextField("101");
    private final ComboBox<LengthUnit> doorWidthUnit = new ComboBox<>();
    private final TextField doorHeightField = new TextField("201");
    private final ComboBox<LengthUnit> doorHeightUnit = new ComboBox<>();
    private final TextField thresholdField = new TextField("0");
    private final ComboBox<LengthUnit> thresholdUnit = new ComboBox<>();
    private final TextField windowWidthField = new TextField("120");
    private final ComboBox<LengthUnit> windowWidthUnit = new ComboBox<>();
    private final TextField windowHeightField = new TextField("120");
    private final ComboBox<LengthUnit> windowHeightUnit = new ComboBox<>();
    private final TextField sillHeightField = new TextField("90");
    private final ComboBox<LengthUnit> sillHeightUnit = new ComboBox<>();
    private final ComboBox<DoorPreset> doorPresetSelector = new ComboBox<>();
    private final ComboBox<WindowPreset> windowPresetSelector = new ComboBox<>();
    private final ComboBox<StairPreset> stairPresetSelector = new ComboBox<>();
    private final ComboBox<RoomObjectPreset> roomObjectPresetSelector = new ComboBox<>();
    private final TextField roomObjectNameField = new TextField("Objekt");
    private final TextField roomObjectWidthField = new TextField("90");
    private final ComboBox<LengthUnit> roomObjectWidthUnit = new ComboBox<>();
    private final TextField roomObjectDepthField = new TextField("90");
    private final ComboBox<LengthUnit> roomObjectDepthUnit = new ComboBox<>();
    private final TextField roomObjectHeightField = new TextField("200");
    private final ComboBox<LengthUnit> roomObjectHeightUnit = new ComboBox<>();
    private final TextField roomObjectBaseElevationField = new TextField("0");
    private final ComboBox<LengthUnit> roomObjectBaseElevationUnit = new ComboBox<>();
    private final TextField roomObjectAngleField = new TextField("0");
    private final TextField stairHeightField = new TextField("280");
    private final ComboBox<LengthUnit> stairHeightUnit = new ComboBox<>();
    private final TextField stairStepsField = new TextField("16");
    private final TextField stairStartLandingField = new TextField("0");
    private final ComboBox<LengthUnit> stairStartLandingUnit = new ComboBox<>();
    private final TextField stairEndLandingField = new TextField("0");
    private final ComboBox<LengthUnit> stairEndLandingUnit = new ComboBox<>();
    private final TextField stairLeftUnderbuildField = new TextField("0");
    private final ComboBox<LengthUnit> stairLeftUnderbuildUnit = new ComboBox<>();
    private final TextField stairRightUnderbuildField = new TextField("0");
    private final ComboBox<LengthUnit> stairRightUnderbuildUnit = new ComboBox<>();
    private final TextField stairUndersideThicknessField = new TextField("0");
    private final ComboBox<LengthUnit> stairUndersideThicknessUnit = new ComboBox<>();
    private final ComboBox<FloorExtensionType> floorExtensionTypeSelector = new ComboBox<>();
    private final ComboBox<FloorExtensionPlacement> floorExtensionPlacementSelector = new ComboBox<>();
    private final TextField floorExtensionThicknessField = new TextField("18");
    private final ComboBox<LengthUnit> floorExtensionThicknessUnit = new ComboBox<>();
    private final ComboBox<HeatingSurfacePosition> heatingSurfacePositionSelector = new ComboBox<>();
    private final ComboBox<HeatingLayoutPattern> heatingLayoutPatternSelector = new ComboBox<>();
    private final TextField heatingPipeSpacingField = new TextField("10");
    private final ComboBox<LengthUnit> heatingPipeSpacingUnit = new ComboBox<>();
    private final TextField heatingPipeDiameterField = new TextField("1,16");
    private final ComboBox<LengthUnit> heatingPipeDiameterUnit = new ComboBox<>();
    private final TextField heatingMaximumPipeLengthField = new TextField("8000");
    private final ComboBox<LengthUnit> heatingMaximumPipeLengthUnit = new ComboBox<>();
    private final TextField heatingWallClearanceField = new TextField("10");
    private final ComboBox<LengthUnit> heatingWallClearanceUnit = new ComboBox<>();
    private final TextField heatingSupplyXField = new TextField("0");
    private final ComboBox<LengthUnit> heatingSupplyXUnit = new ComboBox<>();
    private final TextField heatingSupplyYField = new TextField("0");
    private final ComboBox<LengthUnit> heatingSupplyYUnit = new ComboBox<>();
    private final TextField heatingReturnXField = new TextField("5");
    private final ComboBox<LengthUnit> heatingReturnXUnit = new ComboBox<>();
    private final TextField heatingReturnYField = new TextField("0");
    private final ComboBox<LengthUnit> heatingReturnYUnit = new ComboBox<>();
    private final ListView<String> heatingZoneList = new ListView<>();
    private final Label heatingSummaryLabel = new Label("Keine Heizfläche angelegt.");
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
    private final CheckBox surfaceRotateLayoutCheckBox = new CheckBox("Verlegerichtung um 90° drehen");
    private final ComboBox<SurfaceLayoutMode> surfaceLayoutModeSelector = new ComboBox<>();
    private final TextField surfaceLayoutOffsetField = new TextField("0");
    private final ComboBox<LengthUnit> surfaceLayoutOffsetUnit = new ComboBox<>();
    private final TextField surfaceMinimumOffsetField = new TextField("10");
    private final ComboBox<LengthUnit> surfaceMinimumOffsetUnit = new ComboBox<>();
    private final TextField surfaceMinimumEdgeWidthField = new TextField("8");
    private final ComboBox<LengthUnit> surfaceMinimumEdgeWidthUnit = new ComboBox<>();
    private final TextField surfaceMinimumStartEndMarginField = new TextField("8");
    private final ComboBox<LengthUnit> surfaceMinimumStartEndMarginUnit = new ComboBox<>();
    private final TextField surfaceJointWidthField = new TextField("0,2");
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
    private Button addLevelButton;
    private Button renameLevelButton;
    private Button moveLevelUpButton;
    private Button moveLevelDownButton;
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
    private final Button planHeatingButton = new Button("Raumplanung pausiert");
    private final Button addHeatingZoneButton = new Button("Rechteck hinzufügen");
    private final Button editHeatingZoneButton = new Button("Bereich bearbeiten");
    private final Button removeHeatingZoneButton = new Button("Bereich entfernen");
    private final Button removeHeatingButton = new Button("Heizung entfernen");
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
    private PlanPoint pendingContextWorldPoint;
    private SelectionKey contextMenuSelection;
    private PlanPoint contextMenuWorldPoint;
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
    private List<FloorOpening> selectionDragBaseFloorOpenings = List.of();
    private List<HeatingExclusionArea> selectionDragBaseHeatingExclusionAreas = List.of();
    private List<HydronicHeating> selectionDragBaseHydronicHeatings = List.of();
    private UUID openingDragId;
    private PlanSegment openingDragWallAxis;
    private double openingDragWidth;
    private double openingDragOffsetDelta;
    private EdgeResizeService.EdgeHandle activeEdgeHandle;
    private List<Wall> edgeResizeBaseWalls = List.of();
    private List<Door> edgeResizeBaseDoors = List.of();
    private List<WindowElement> edgeResizeBaseWindows = List.of();
    private List<Staircase> edgeResizeBaseStaircases = List.of();
    private List<FloorOpening> edgeResizeBaseFloorOpenings = List.of();
    private List<HeatingExclusionArea> edgeResizeBaseHeatingExclusionAreas = List.of();
    private List<HydronicHeating> edgeResizeBaseHydronicHeatings = List.of();
    private double lastMouseX;
    private double lastMouseY;
    private boolean altPressed;
    private boolean spacePressed;
    // Steuert alle blockierenden UI-Dialoge (Fehler-, Bestätigungs-, Erfolgs- und Eingabedialoge).
    // Wird durch die Automatisierung deaktiviert, damit Tests nicht an Dialogen hängen bleiben.
    private boolean interactiveDialogsEnabled = true;
    private boolean applicationExitRequested;
    private boolean applicationExitConfirmed;
    private Boolean automatedUnsavedChangesExitDecision;
    private long currentChangeRevision;
    private long savedChangeRevision;
    private long nextChangeRevision = 1;
    private Runnable applicationExitAction = Platform::exit;
    private UiErrorDialogs.ErrorPresentation lastErrorDialog = UiErrorDialogs.ErrorPresentation.empty();

    public CadWorkbench() {
        // Oberer Bereich (Werkzeugleiste) soll bündig oben anliegen, daher
        // nur unten/links/rechts padding, oben 0.
        setPadding(new Insets(0, 12, 12, 12));
        setStyle("-fx-background-color: linear-gradient(to bottom, #f6f1e8, #ece5d8);");
        if (automationActive()) {
            interactiveDialogsEnabled = false;
        }

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
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalShortcuts);
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
        levelSelector.setItems(availableLevels);
        levelSelector.setValue(activeLevel.get());
        toolSelector.getItems().addAll(DrawingTool.values());
        toolSelector.setValue(DrawingTool.EDIT);
        floorExtensionTypeSelector.getItems().setAll(FloorExtensionType.values());
        floorExtensionTypeSelector.setValue(FloorExtensionType.BALCONY);
        floorExtensionPlacementSelector.getItems().setAll(FloorExtensionPlacement.values());
        floorExtensionPlacementSelector.setValue(FloorExtensionPlacement.EXTERIOR);
        initializePresetSelectors();
        initializeSurfaceLayerControls();
        initializeHeatingControls();
        initializeDwgLibraryControls();
        levelSelector.valueProperty().addListener((ignored, oldValue, newValue) -> {
            if (newValue != null) {
                activateLevel(newValue);
            }
        });

        applyFormTooltips();

        registerRenderListener(snapToGrid);
        registerRenderListener(snapToEndpoints);
        registerRenderListener(showCompass);
        registerRenderListener(showDimensions);
        registerRenderListener(dimensionTextStyle);
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
        menuBar.setUseSystemMenuBar(true);
        // Im Fenster wird die MenuBar nicht dargestellt, damit sie keinen
        // Leerraum oberhalb der Werkzeugleiste erzeugt. Unter macOS wandert
        // das echte Menü in die Systemmenüleiste; auf anderen Plattformen
        // bleibt es sichtbar.
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            menuBar.setManaged(false);
            menuBar.setVisible(false);
        }
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

        CheckBox dimensionsBox = new CheckBox("ISO-Bemaßung");
        dimensionsBox.selectedProperty().bindBidirectional(showDimensions);
        applyTooltip(dimensionsBox, "Blendet die ISO-Bemaßung nach DIN EN ISO 7519 | 2025-01 mit Maß-, Maßhilfs- und Begrenzungslinien ein oder aus.");

        CheckBox dimensionTextPartsBox = new CheckBox("Erweiterte Maßtexte");
        dimensionTextPartsBox.setSelected(dimensionTextStyle.get() == DimensionTextStyle.FULL);
        dimensionTextPartsBox.selectedProperty().addListener((obs, wasFull, isFull) ->
                dimensionTextStyle.set(Boolean.TRUE.equals(isFull) ? DimensionTextStyle.FULL : DimensionTextStyle.LENGTH_ONLY));
        dimensionTextStyle.addListener((obs, oldStyle, newStyle) ->
                dimensionTextPartsBox.setSelected(newStyle == DimensionTextStyle.FULL));
        applyTooltip(dimensionTextPartsBox,
                "Bestimmt den Textanteil der Maßangaben in 2D-Ansicht und Bauzeichnung-PDF. " +
                "Aktiviert: vollständige Texte mit Raumname, Raummaß und Außenmaß-Vorsatz. " +
                "Deaktiviert: ausschließlich die nackte Länge, z. B. \"4,20 m\".");

        CheckBox objectsBox = new CheckBox("Objekte");
        objectsBox.selectedProperty().bindBidirectional(showRoomObjects);
        applyTooltip(objectsBox, "Blendet platzierte Raumobjekte gemeinsam in 2D, Innenansicht und 3D ein oder aus.");

        Button addLevelButton = createActionButton(
                "Etage hinzufügen",
                null,
                this::createLevel,
                "Legt eine neue Etage für den aktuellen Grundriss an und wechselt direkt in diese Etage."
        );
        this.addLevelButton = addLevelButton;

        Button renameLevelButton = createActionButton(
                "Etage umbenennen",
                null,
                this::renameCurrentLevel,
                "Benennt die aktuell ausgewählte Etage um. Der Name muss innerhalb des Projekts eindeutig sein."
        );
        this.renameLevelButton = renameLevelButton;

        Button moveLevelUpButton = createActionButton(
                "Etage hoch",
                null,
                this::moveCurrentLevelUp,
                "Verschiebt die aktuell ausgewählte Etage eine Position nach oben in der Etagenreihenfolge."
        );
        this.moveLevelUpButton = moveLevelUpButton;

        Button moveLevelDownButton = createActionButton(
                "Etage runter",
                null,
                this::moveCurrentLevelDown,
                "Verschiebt die aktuell ausgewählte Etage eine Position nach unten in der Etagenreihenfolge."
        );
        this.moveLevelDownButton = moveLevelDownButton;

        Button terrainButton = createActionButton(
                "Gelände",
                null,
                this::editTerrainElevations,
                "Öffnet die Geländehöhen aller äußeren Gebäudeecken relativ zum Boden der untersten Etage. Die Werte steuern Grundriss, Seitenansichten und 3D-Darstellung."
        );

        settingsBarStyling();
        return new ToolBar(
                labelledNode("Werkzeug", toolSelector),
                new Separator(Orientation.VERTICAL),
                labelledNode("Etage", levelSelector),
                addLevelButton,
                renameLevelButton,
                moveLevelUpButton,
                moveLevelDownButton,
                terrainButton,
                new Separator(Orientation.VERTICAL),
                undoButton,
                redoButton,
                deleteSelectionButton,
                clearSelectionButton,
                new Separator(Orientation.VERTICAL),
                snapRasterBox,
                snapPointsBox,
                guideDistancesBox,
                snapGuidesBox,
                snapWallsBox,
                new Separator(Orientation.VERTICAL),
                dimensionsBox,
                dimensionTextPartsBox,
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
        button.setOnAction(event -> runGuardedAction(label, action));
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
        button.setOnAction(event -> runGuardedAction(workspaceMode.label() + "-Arbeitsbereich", () -> selectWorkspaceMode(workspaceMode, true)));
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
        roomObjectNameField.setPrefColumnCount(10);
        roomObjectWidthField.setPrefColumnCount(6);
        roomObjectDepthField.setPrefColumnCount(6);
        roomObjectHeightField.setPrefColumnCount(6);
        roomObjectAngleField.setPrefColumnCount(6);
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
        draftLabel.setText("Innenansicht nicht verfügbar: Es wird ein Raum benötigt.");
        if (!interactiveDialogsEnabled) {
            return;
        }
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

    private void editTerrainElevations() {
        Terrain synchronizedTerrain = terrainCornerService.synchronize(project.primaryLevel(), project.terrain());
        if (!synchronizedTerrain.configured()) {
            draftLabel.setText("Geländehöhen benötigen mindestens drei äußere Gebäudeecken.");
            return;
        }
        if (!interactiveDialogsEnabled) {
            return;
        }
        List<TextField> elevationFields = new ArrayList<>();
        VBox rows = new VBox(8.0);
        for (int index = 0; index < synchronizedTerrain.vertices().size(); index++) {
            TerrainVertex vertex = synchronizedTerrain.vertices().get(index);
            TextField field = new TextField(formatValue(vertex.elevationAboveLowestFloor(), LengthUnit.CENTIMETER, LENGTH_INPUT_DECIMALS));
            field.setPrefColumnCount(8);
            applyTooltip(field, "Legt die Geländehöhe an dieser äußeren Gebäudeecke relativ zum Boden der untersten Etage in Zentimetern fest. Positive und negative Werte sind zulässig.");
            Label label = new Label(String.format(Locale.GERMAN, "Ecke %d bei %.2f / %.2f m", index + 1,
                    vertex.position().xMillimeters() / 1_000.0, vertex.position().yMillimeters() / 1_000.0));
            rows.getChildren().add(new HBox(10.0, label, field, new Label("cm")));
            elevationFields.add(field);
        }
        ScrollPane scrollPane = new ScrollPane(rows);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(Math.min(420.0, 52.0 * elevationFields.size()));
        applyTooltip(scrollPane, "Zeigt alle automatisch aus dem untersten Gebäudegrundriss abgeleiteten äußeren Geländeecken.");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Geländehöhen bearbeiten");
        dialog.setHeaderText("Höhe über dem Boden der untersten Etage");
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(560);
        Window owner = currentWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        applyTooltip(dialog.getDialogPane().lookupButton(ButtonType.OK), "Übernimmt alle Geländehöhen und aktualisiert 3D- sowie Seitenansichten.");
        applyTooltip(dialog.getDialogPane().lookupButton(ButtonType.CANCEL), "Verwirft die Eingaben und lässt das Gelände unverändert.");
        if (dialog.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }
        List<TerrainVertex> updatedVertices = new ArrayList<>();
        for (int index = 0; index < synchronizedTerrain.vertices().size(); index++) {
            TerrainVertex vertex = synchronizedTerrain.vertices().get(index);
            Length elevation = parseLength(elevationFields.get(index), LengthUnit.CENTIMETER)
                    .orElse(vertex.elevationAboveLowestFloor());
            updatedVertices.add(new TerrainVertex(vertex.position(), elevation));
        }
        rememberStateForUndo();
        project.defineTerrain(new Terrain(updatedVertices));
        markThreeDDirty();
        render();
    }

    private MenuBar buildMenuBar() {
        Menu dateiMenu = new Menu("Datei");
        dateiMenu.getItems().addAll(
                menuItem("Etage hinzufügen", this::createLevel, shortcutKey(KeyCode.N)),
                menuItem("Projekt leeren", this::clearProject, shortcutKey(KeyCode.L)),
                menuItem("Laden", this::importProjectFromDxf, shortcutShiftKey(KeyCode.I)),
                menuItem("Sichern", this::saveProject, shortcutKey(KeyCode.S)),
                menuItem("Sichern als ...", this::saveProjectAs, shortcutShiftKey(KeyCode.S)),
                menuItem("Etage laden", this::importLevel, null),
                menuItem("Etage sichern", this::saveCurrentLevel, null),
                menuItem("Etage sichern als ...", this::saveCurrentLevelAs, null),
                menuItem("Bauzeichnung als PDF exportieren", this::exportConstructionDrawingPdf, shortcutKey(KeyCode.P)),
                menuItem("Teilebibliothek laden", this::importPartLibrary, shortcutShiftKey(KeyCode.B)),
                menuItem("3D-Objekt aus DXF/IFC/RFA laden", this::importThreeDObject, null),
                menuItem("Beenden", this::requestApplicationExit, shortcutKey(KeyCode.Q))
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
                toolMenuItem(DrawingTool.FLOOR_EXTENSION, KeyCode.G),
                menuItem(DrawingTool.HEATING_ZONE_RECTANGLE.label(), () -> toolSelector.setValue(DrawingTool.HEATING_ZONE_RECTANGLE), null),
                menuItem(DrawingTool.HEATING_MANIFOLD.label(), () -> toolSelector.setValue(DrawingTool.HEATING_MANIFOLD), null),
                menuItem(DrawingTool.HEATING_EXCLUSION_RECTANGLE.label(), () -> toolSelector.setValue(DrawingTool.HEATING_EXCLUSION_RECTANGLE), null),
                menuItem("Heizkreis-Router Vario testen", this::showHeatingCircuitRoutingWindow, null),
                toolMenuItem(DrawingTool.DOOR, KeyCode.D),
                toolMenuItem(DrawingTool.WINDOW, KeyCode.F),
                menuItem(DrawingTool.ROOF_WINDOW.label(), () -> toolSelector.setValue(DrawingTool.ROOF_WINDOW), null),
                toolMenuItem(DrawingTool.OBJECT, KeyCode.O),
                new SeparatorMenuItem(),
                menuItem("Geländehöhen bearbeiten", this::editTerrainElevations, null),
                menuItem("Ausgewählte Bauteile 90° rechts drehen", this::rotateSelectedComponentsClockwise, shortcutShiftKey(KeyCode.RIGHT)),
                menuItem("Ausgewählte Bauteile 90° links drehen", this::rotateSelectedComponentsCounterClockwise, shortcutShiftKey(KeyCode.LEFT))
        );

        Menu optionenMenu = new Menu("Optionen");
        optionenMenu.getItems().addAll(
                checkMenuItem("Auf Raster einrasten", snapToGrid),
                checkMenuItem("Auf Punkte einrasten", snapToEndpoints),
                checkMenuItem("Hilfslinien anzeigen", showGuides),
                checkMenuItem("Hilfslinienabstände anzeigen", showGuideDistances),
                checkMenuItem("An Hilfslinien einrasten", snapToGuides),
                checkMenuItem("An anderen Wänden einrasten", snapToWalls),
                checkMenuItem("ISO-Bemaßung anzeigen", showDimensions),
                checkMenuItem("Erweiterte Maßtexte anzeigen", dimensionTextStyle, DimensionTextStyle.FULL, DimensionTextStyle.LENGTH_ONLY),
                checkMenuItem("Objekte anzeigen", showRoomObjects),
                checkMenuItem("Fläche und Volumen anzeigen", showAreaVolume),
                checkMenuItem("Nordpfeil anzeigen", showCompass)
        );

        Menu berichteMenu = new Menu("Berichte");
        berichteMenu.getItems().addAll(
                menuItem("Räume und Materialien anzeigen", this::showSurfaceMaterialReportWindow, null),
                menuItem("Räume und Materialien als MD exportieren", this::exportSurfaceMaterialReportMarkdown, null)
        );

        Menu hilfeMenu = new Menu("Hilfe");
        hilfeMenu.getItems().addAll(
                menuItem("Über CADas", this::showAboutDialog, null),
                new SeparatorMenuItem(),
                menuItem("Benutzerdokumentation", this::showHelpWindow, new KeyCodeCombination(KeyCode.F1)),
                menuItem("Keymap und Mausbedienung", this::showKeymapWindow, null),
                menuItem("Drittanbieter-Lizenzen", this::showThirdPartyLicensesWindow, null)
        );

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
                        propertyRow("Dachschrägen", roofSlopeManagementLabel)
                ),
                createPropertySection(
                        "Flächenheizung",
                        heatingSummaryLabel,
                        propertyRow("Fläche", heatingSurfacePositionSelector),
                        propertyRow("Verlegung", heatingLayoutPatternSelector),
                        propertyRow("Verlegeabstand", heatingPipeSpacingField, heatingPipeSpacingUnit),
                        propertyRow("Rohrdurchmesser", heatingPipeDiameterField, heatingPipeDiameterUnit),
                        propertyRow("Maximale Rohrlänge", heatingMaximumPipeLengthField, heatingMaximumPipeLengthUnit),
                        propertyRow("Wandabstand", heatingWallClearanceField, heatingWallClearanceUnit),
                        propertyRow("Vorlauf X", heatingSupplyXField, heatingSupplyXUnit),
                        propertyRow("Vorlauf Y", heatingSupplyYField, heatingSupplyYUnit),
                        propertyRow("Rücklauf X", heatingReturnXField, heatingReturnXUnit),
                        propertyRow("Rücklauf Y", heatingReturnYField, heatingReturnYUnit),
                        planHeatingButton,
                        heatingZoneList,
                        new HBox(6.0, addHeatingZoneButton, editHeatingZoneButton),
                        new HBox(6.0, removeHeatingZoneButton, removeHeatingButton)
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
                        propertyRow("Absatz Ende", stairEndLandingField, stairEndLandingUnit),
                        propertyRow("Unterbau links", stairLeftUnderbuildField, stairLeftUnderbuildUnit),
                        propertyRow("Unterbau rechts", stairRightUnderbuildField, stairRightUnderbuildUnit),
                        propertyRow("Untersichtdicke", stairUndersideThicknessField, stairUndersideThicknessUnit)
                ),
                createPropertySection(
                        "Objekt",
                        propertyRow("Preset", roomObjectPresetSelector),
                        propertyRow("Bezeichnung", roomObjectNameField),
                        propertyRow("Breite", roomObjectWidthField, roomObjectWidthUnit),
                        propertyRow("Tiefe", roomObjectDepthField, roomObjectDepthUnit),
                        propertyRow("Höhe", roomObjectHeightField, roomObjectHeightUnit),
                        propertyRow("Basishöhe", roomObjectBaseElevationField, roomObjectBaseElevationUnit),
                        propertyRow("Winkel", roomObjectAngleField)
                ),
                createPropertySection(
                        "Balkon/Empore",
                        propertyRow("Element", floorExtensionTypeSelector),
                        propertyRow("Lage", floorExtensionPlacementSelector),
                        propertyRow("Fußbodendicke", floorExtensionThicknessField, floorExtensionThicknessUnit)
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
                        surfaceRotateLayoutCheckBox,
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
        undoButton.setOnAction(event -> runGuardedAction("Rückgängig", this::undo));
        redoButton.setOnAction(event -> runGuardedAction("Wiederherstellen", this::redo));
        deleteSelectionButton.setOnAction(event -> runGuardedAction("Auswahl löschen", this::deleteSelection));
        clearSelectionButton.setOnAction(event -> runGuardedAction("Auswahl aufheben", this::clearSelection));
        applySelectionPropertiesButton.setOnAction(event -> runGuardedAction("Werte auf Auswahl anwenden", this::applyCurrentInputsToSelection));
        applyEndpointHeightButton.setOnAction(event -> runGuardedAction("Eckhöhe anwenden", this::applyEndpointHeightToSelection));
        addSurfaceLayerButton.setOnAction(event -> runGuardedAction("Ebene hinzufügen", this::addSurfaceLayer));
        updateSurfaceLayerButton.setOnAction(event -> runGuardedAction("Ebene aktualisieren", this::updateSurfaceLayer));
        removeSurfaceLayerButton.setOnAction(event -> runGuardedAction("Ebene entfernen", this::removeSurfaceLayer));
        toggleSurfaceLayerVisibilityButton.setOnAction(event -> runGuardedAction("Sichtbarkeit umschalten", this::toggleSurfaceLayerVisibility));
        moveSurfaceLayerUpButton.setOnAction(event -> runGuardedAction("Ebene nach oben", () -> moveSurfaceLayer(-1)));
        moveSurfaceLayerDownButton.setOnAction(event -> runGuardedAction("Ebene nach unten", () -> moveSurfaceLayer(1)));
        saveSurfacePresetButton.setOnAction(event -> runGuardedAction("Belagspreset speichern", this::saveCurrentSurfacePreset));
        addDwgBlockPresetButton.setOnAction(event -> runGuardedAction("DWG-Block hinzufügen", this::addDwgBlockPreset));
        refreshDwgLibraryButton.setOnAction(event -> runGuardedAction("DWG prüfen", this::refreshCurrentDwgLibraryAnalysis));
        addDwgBlockAsSurfaceButton.setOnAction(event -> runGuardedAction("DWG-Block als Belag", this::addSelectedDwgBlockAsSurfacePreset));
        addDwgBlockAsObjectButton.setOnAction(event -> runGuardedAction("DWG-Block als Objekt", this::addSelectedDwgBlockAsObjectPreset));
        planHeatingButton.setOnAction(event -> runGuardedAction("Heizkreise planen", this::planHydronicHeating));
        addHeatingZoneButton.setOnAction(event -> runGuardedAction("Heizbereich hinzufügen", () -> editHeatingZone(true)));
        editHeatingZoneButton.setOnAction(event -> runGuardedAction("Heizbereich bearbeiten", () -> editHeatingZone(false)));
        removeHeatingZoneButton.setOnAction(event -> runGuardedAction("Heizbereich entfernen", this::removeHeatingZone));
        removeHeatingButton.setOnAction(event -> runGuardedAction("Heizung entfernen", this::removeHydronicHeating));
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
        applyTooltip(planHeatingButton, "Die automatische Planung ganzer Räume ist vorübergehend deaktiviert. Heizkreise werden aktuell halbautomatisch als Rechtecke mit dem Werkzeug `Heizkreis` angelegt und danach direkt in der Zeichenfläche bearbeitet.");
        applyTooltip(addHeatingZoneButton, "Ergänzt einen neuen Heizkreis als rechteckiges Startfeld in einem freien Bereich des ausgewählten Raums. Das Rechteck kann im Dialog über Eckpunkte, Verlegeart und Rollenorientierung angepasst werden.");
        applyTooltip(editHeatingZoneButton, "Bearbeitet Name, Verlegeart, Rollenorientierung und sämtliche Eckpunkte des in der Liste markierten Heizbereichs. Danach wird dessen Rohrverlauf neu berechnet.");
        applyTooltip(removeHeatingZoneButton, "Entfernt den in der Liste markierten Heizbereich einschließlich seines berechneten Rohrverlaufs.");
        applyTooltip(removeHeatingButton, "Entfernt die Flächenheizung der ausgewählten Boden- oder Deckenfläche vollständig aus dem Raum.");
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
                case 4 -> selectedRoom().isPresent()
                        || currentTool() == DrawingTool.HEATING_ZONE_RECTANGLE
                        || currentTool() == DrawingTool.HEATING_MANIFOLD;
                case 5 -> shouldShowSection(DrawingTool.DOOR, RenderableKind.DOOR);
                case 6 -> shouldShowSection(DrawingTool.WINDOW, RenderableKind.WINDOW)
                        || shouldShowSection(DrawingTool.ROOF_WINDOW, RenderableKind.ROOF_WINDOW);
                case 7 -> shouldShowSection(DrawingTool.STAIR, RenderableKind.STAIR);
                case 8 -> shouldShowSection(DrawingTool.OBJECT, RenderableKind.ROOM_OBJECT);
                case 9 -> shouldShowSection(DrawingTool.FLOOR_EXTENSION, RenderableKind.FLOOR_EXTENSION);
                case 10 -> shouldShowLayerSection();
                default -> true;
            };
            node.setVisible(visible);
            node.setManaged(visible);
        }
        selectionSummaryLabel.setText(selectionSummary());
        refreshSurfaceTypeSelector();
        refreshSurfaceLayerSection();
        refreshHeatingSection();
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
                || selectedSelection.get().kind() == RenderableKind.ROOM_CEILING
                || selectedSelection.get().kind() == RenderableKind.FLOOR_EXTENSION;
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
            case ROOF_WINDOW -> "Dachfenster";
            case STAIR -> "Treppe";
            case ROOM_OBJECT -> "Objekt";
            case FLOOR_EXTENSION -> "Balkon/Empore";
            case FLOOR_OPENING -> "Bodenöffnung";
            case HEATING_ZONE -> "Heizkreis";
            case HEATING_MANIFOLD -> "HKV-Freifläche";
            case HEATING_EXCLUSION -> "FBH-Sperrfläche";
            default -> selection.kind().name();
        };
    }

    private void updateActionButtons() {
        undoButton.setDisable(!history.canUndo());
        redoButton.setDisable(!history.canRedo());
        boolean hasSelection = !selectedSelections.isEmpty();
        boolean hasDeletableSelection = selectedSelections.stream().anyMatch(selection -> selection.kind() != RenderableKind.ROOM_VOLUME
                && selection.kind() != RenderableKind.ROOM_FLOOR
                && selection.kind() != RenderableKind.ROOM_CEILING
                && selection.kind() != RenderableKind.HEATING_MANIFOLD);
        deleteSelectionButton.setDisable(!hasDeletableSelection);
        clearSelectionButton.setDisable(!hasSelection && selectedEndpointGroup == null);
        applySelectionPropertiesButton.setDisable(!hasSelection);
        applyEndpointHeightButton.setDisable(selectedEndpointGroup == null);
        int currentIndex = availableLevels.indexOf(activeLevel.get());
        if (addLevelButton != null) {
            addLevelButton.setDisable(false);
        }
        if (renameLevelButton != null) {
            renameLevelButton.setDisable(activeLevel.get() == null);
        }
        if (moveLevelUpButton != null) {
            moveLevelUpButton.setDisable(currentIndex < 0 || currentIndex >= availableLevels.size() - 1);
        }
        if (moveLevelDownButton != null) {
            moveLevelDownButton.setDisable(currentIndex <= 0);
        }
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
        HydronicHeating selectedHeating = selectedHydronicHeating().orElse(null);
        int selectedZoneIndex = heatingZoneList.getSelectionModel().getSelectedIndex();
        planHeatingButton.setDisable(true);
        addHeatingZoneButton.setDisable(selectedHeating == null);
        editHeatingZoneButton.setDisable(selectedHeating == null || selectedZoneIndex < 0);
        removeHeatingZoneButton.setDisable(selectedHeating == null || selectedZoneIndex < 0);
        removeHeatingButton.setDisable(selectedHeating == null);
    }

    private MenuItem menuItem(String label, Runnable action, KeyCombination accelerator) {
        MenuItem menuItem = new MenuItem(label);
        menuItem.setOnAction(event -> runGuardedAction(label, action));
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

    private <T> CheckMenuItem checkMenuItem(String label, ObjectProperty<T> property, T checkedValue, T uncheckedValue) {
        CheckMenuItem menuItem = new CheckMenuItem(label);
        menuItem.setSelected(property.get() == checkedValue);
        menuItem.selectedProperty().addListener((obs, wasSelected, isSelected) ->
                property.set(Boolean.TRUE.equals(isSelected) ? checkedValue : uncheckedValue));
        property.addListener((obs, oldValue, newValue) ->
                menuItem.setSelected(newValue == checkedValue));
        return menuItem;
    }

    private KeyCombination shortcutKey(KeyCode keyCode) {
        return new KeyCodeCombination(keyCode, KeyCombination.SHORTCUT_DOWN);
    }

    private KeyCombination shortcutShiftKey(KeyCode keyCode) {
        return new KeyCodeCombination(keyCode, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    }

    private void runGuardedAction(String actionLabel, Runnable action) {
        try {
            action.run();
        } catch (Exception exception) {
            showActionException(actionLabel, exception);
        }
    }

    private void showActionException(String actionLabel, Throwable throwable) {
        String title = "Aktion fehlgeschlagen";
        String header = "Die Aktion `" + actionLabel + "` konnte nicht abgeschlossen werden.";
        String content = UiErrorDialogs.userMessage(throwable);
        draftLabel.setText(header + " " + content);
        showErrorDialog(title, header, content, throwable);
    }

    private void showOperationException(String title, Throwable throwable) {
        String content = UiErrorDialogs.userMessage(throwable);
        draftLabel.setText(title + ": " + content);
        showErrorDialog(title, title, content, throwable);
    }

    private void showHeatingCircuitRoutingWindow() {
        new HeatingCircuitRoutingWindow().show(currentWindow());
    }

    private void showErrorDialog(String title, String header, String content, Throwable throwable) {
        lastErrorDialog = UiErrorDialogs.fromThrowable(title, header, content, throwable);
        UiErrorDialogs.show(lastErrorDialog, currentWindow(), interactiveDialogsEnabled);
    }

    private Window currentWindow() {
        return getScene() != null ? getScene().getWindow() : null;
    }

    public void handleUnhandledException(Throwable throwable) {
        showErrorDialog(
                "Unerwarteter Fehler",
                "CADas hat einen unerwarteten Fehler erkannt.",
                UiErrorDialogs.userMessage(throwable),
                throwable
        );
        draftLabel.setText("Unerwarteter Fehler: " + UiErrorDialogs.userMessage(throwable));
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
        initializeUnitSelector(wallHeightField, wallHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(endpointHeightField, endpointHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(roomHeightField, roomHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(floorThicknessField, floorThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(ceilingThicknessField, ceilingThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(kneeWallHeightField, kneeWallHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(doorWidthField, doorWidthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(doorHeightField, doorHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(thresholdField, thresholdUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(windowWidthField, windowWidthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(windowHeightField, windowHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(sillHeightField, sillHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairHeightField, stairHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairStartLandingField, stairStartLandingUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairEndLandingField, stairEndLandingUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairLeftUnderbuildField, stairLeftUnderbuildUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairRightUnderbuildField, stairRightUnderbuildUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairUndersideThicknessField, stairUndersideThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(roomObjectWidthField, roomObjectWidthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(roomObjectDepthField, roomObjectDepthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(roomObjectHeightField, roomObjectHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(roomObjectBaseElevationField, roomObjectBaseElevationUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(floorExtensionThicknessField, floorExtensionThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingPipeSpacingField, heatingPipeSpacingUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingPipeDiameterField, heatingPipeDiameterUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingMaximumPipeLengthField, heatingMaximumPipeLengthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingWallClearanceField, heatingWallClearanceUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingSupplyXField, heatingSupplyXUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingSupplyYField, heatingSupplyYUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingReturnXField, heatingReturnXUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingReturnYField, heatingReturnYUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceLayerThicknessField, surfaceLayerThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceTileWidthField, surfaceTileWidthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceTileHeightField, surfaceTileHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceLayoutOffsetField, surfaceLayoutOffsetUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceMinimumOffsetField, surfaceMinimumOffsetUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceJointWidthField, surfaceJointWidthUnit, LengthUnit.CENTIMETER);
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
        applyRoomObjectPreset(roomObjectPresetSelector.getValue());
        doorPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyDoorPreset(newValue));
        windowPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyWindowPreset(newValue));
        stairPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyStairPreset(newValue));
        roomObjectPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyRoomObjectPreset(newValue));
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

    private void initializeHeatingControls() {
        heatingSurfacePositionSelector.getItems().setAll(HeatingSurfacePosition.values());
        heatingSurfacePositionSelector.setValue(HeatingSurfacePosition.FLOOR);
        heatingLayoutPatternSelector.getItems().setAll(HeatingLayoutPattern.values());
        heatingLayoutPatternSelector.setValue(HeatingLayoutPattern.SPIRAL);
        heatingZoneList.setPrefHeight(110.0);
        heatingSummaryLabel.setWrapText(true);
        heatingSummaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        heatingSurfacePositionSelector.valueProperty().addListener((ignored, oldValue, newValue) -> {
            refreshHeatingSection();
            render();
        });
        heatingZoneList.getSelectionModel().selectedIndexProperty().addListener((ignored, oldValue, newValue) -> updateActionButtons());
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
            showOperationException("Eigene Belagspresets konnten nicht geladen werden", exception);
        }
    }

    private void registerConfiguredDwgLibraries() {
        try {
            userSurfacePresetLibrary.loadCadLibraries().forEach(this::registerConfiguredDwgLibraryReference);
        } catch (IOException exception) {
            showOperationException("Gespeicherte DWG-Bibliotheken konnten nicht geladen werden", exception);
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
        applyTooltip(kneeWallHeightField, "Legt die Sockel- beziehungsweise Kniestockhöhe der Dachschräge an der niedrigen Raumkante fest.");
        applyTooltip(kneeWallHeightUnit, "Bestimmt die Einheit für die Sockelhöhe der Dachschräge.");
        applyTooltip(roofSlopeManagementLabel, "Dachschrägen werden über das Kontextmenü ihrer niedrigen Wand erzeugt oder ersetzt. Jede Raumseite kann eine eigene Dachschräge besitzen.");
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
        applyTooltip(stairLeftUnderbuildField, "Legt die Wandstärke des optionalen linken Treppenunterbaus fest. Null entfernt diese Unterbauwand.");
        applyTooltip(stairLeftUnderbuildUnit, "Bestimmt die Einheit für die Wandstärke des linken Treppenunterbaus.");
        applyTooltip(stairRightUnderbuildField, "Legt die Wandstärke des optionalen rechten Treppenunterbaus fest. Null entfernt diese Unterbauwand.");
        applyTooltip(stairRightUnderbuildUnit, "Bestimmt die Einheit für die Wandstärke des rechten Treppenunterbaus.");
        applyTooltip(stairUndersideThicknessField, "Legt die senkrechte Dicke der planen schrägen Untersicht unterhalb der Stufen fest. Null deaktiviert die Untersichtplatte.");
        applyTooltip(stairUndersideThicknessUnit, "Bestimmt die Einheit für die Dicke der planen schrägen Treppenuntersicht.");
        applyTooltip(floorExtensionTypeSelector, "Wählt, ob die rechteckige Erweiterung als Balkon oder Empore modelliert wird.");
        applyTooltip(floorExtensionPlacementSelector, "Kennzeichnet die Erweiterung als innen oder außen an die aktive Etage angehängt.");
        applyTooltip(floorExtensionThicknessField, "Legt die Dicke der tragenden rechteckigen Fußbodenplatte des Balkons oder der Empore fest.");
        applyTooltip(floorExtensionThicknessUnit, "Bestimmt die Einheit für die Fußbodendicke des Balkons oder der Empore.");
        applyTooltip(heatingSurfacePositionSelector, "Wählt unabhängig voneinander die Fußboden- oder Deckenheizung des markierten Raums. Für beide Flächen stehen dieselben Planungs- und Bearbeitungsfunktionen bereit.");
        applyTooltip(heatingLayoutPatternSelector, "Wählt die Start-Verlegeart für neu angelegte Flächenheizungen. Vario wird zunächst zusätzlich im separaten Testfenster geprüft.");
        applyTooltip(heatingPipeSpacingField, "Legt den Achsabstand benachbarter Rohrläufe fest. Der Kurvenradius wird automatisch als halber Verlegeabstand angesetzt.");
        applyTooltip(heatingPipeSpacingUnit, "Bestimmt die Einheit für den Verlegeabstand der Heizungsrohre.");
        applyTooltip(heatingPipeDiameterField, "Legt den Außendurchmesser des Heizungsrohrs fest. Er muss kleiner als der Verlegeabstand sein.");
        applyTooltip(heatingPipeDiameterUnit, "Bestimmt die Einheit für den Rohrdurchmesser.");
        applyTooltip(heatingMaximumPipeLengthField, "Begrenzt die gesamte Rohrlänge je Heizkreis einschließlich der Verbindung zum Vor- und Rücklauf. Größere Räume werden automatisch in mehrere Bereiche geteilt.");
        applyTooltip(heatingMaximumPipeLengthUnit, "Bestimmt die Einheit für die maximal zulässige Rohrlänge je Heizkreis.");
        applyTooltip(heatingWallClearanceField, "Legt den Mindestabstand der Rohrmitte von der Raumwand fest.");
        applyTooltip(heatingWallClearanceUnit, "Bestimmt die Einheit für den Mindestabstand zur Wand.");
        applyTooltip(heatingSupplyXField, "Legt die X-Koordinate des Vorlaufanschlusses am Verteiler im Koordinatensystem der Etage fest.");
        applyTooltip(heatingSupplyXUnit, "Bestimmt die Einheit für die X-Koordinate des Vorlaufanschlusses.");
        applyTooltip(heatingSupplyYField, "Legt die Y-Koordinate des Vorlaufanschlusses am Verteiler im Koordinatensystem der Etage fest.");
        applyTooltip(heatingSupplyYUnit, "Bestimmt die Einheit für die Y-Koordinate des Vorlaufanschlusses.");
        applyTooltip(heatingReturnXField, "Legt die X-Koordinate des Rücklaufanschlusses am Verteiler im Koordinatensystem der Etage fest.");
        applyTooltip(heatingReturnXUnit, "Bestimmt die Einheit für die X-Koordinate des Rücklaufanschlusses.");
        applyTooltip(heatingReturnYField, "Legt die Y-Koordinate des Rücklaufanschlusses am Verteiler im Koordinatensystem der Etage fest.");
        applyTooltip(heatingReturnYUnit, "Bestimmt die Einheit für die Y-Koordinate des Rücklaufanschlusses.");
        applyTooltip(heatingZoneList, "Listet die getrennten Heizkreise der gewählten Boden- oder Deckenfläche mit berechneter Rohrlänge auf. Ein markierter Bereich kann als beliebiges Polygon bearbeitet werden.");
        applyTooltip(heatingSummaryLabel, "Zeigt Fläche, Verlegeart, Anzahl der Heizkreise und die gesamte berechnete Rohrlänge der gewählten Flächenheizung.");
        applyTooltip(roomObjectPresetSelector, "Wählt ein Objekt zum Platzieren aus und übernimmt dessen Standardmaße. DWG-Dateien unter `~/.config/CADas/Objekte` erscheinen hier zusätzlich als Objekt-Presets.");
        applyTooltip(roomObjectNameField, "Legt die sichtbare Bezeichnung eines neuen oder ausgewählten Objekts fest. Bei Quadern wird sie im Grundriss angezeigt.");
        applyTooltip(roomObjectWidthField, "Legt die Breite eines neuen oder ausgewählten Objekts fest.");
        applyTooltip(roomObjectWidthUnit, "Bestimmt die Einheit für die Objektbreite.");
        applyTooltip(roomObjectDepthField, "Legt die Tiefe eines neuen oder ausgewählten Objekts fest.");
        applyTooltip(roomObjectDepthUnit, "Bestimmt die Einheit für die Objekttiefe.");
        applyTooltip(roomObjectHeightField, "Legt die Höhe eines neuen oder ausgewählten Objekts fest.");
        applyTooltip(roomObjectHeightUnit, "Bestimmt die Einheit für die Objekthöhe.");
        applyTooltip(roomObjectBaseElevationField, "Legt die vertikale Lage der Objektbasis relativ zum Boden der aktiven Etage fest. Positive Werte heben das Objekt an, negative Werte versenken es.");
        applyTooltip(roomObjectBaseElevationUnit, "Bestimmt die Einheit für die positive oder negative Basishöhe des Objekts.");
        applyTooltip(roomObjectAngleField, "Legt den frei einstellbaren Drehwinkel eines neuen oder ausgewählten Objekts in Grad fest.");
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
        applyTooltip(surfaceRotateLayoutCheckBox, "Dreht die Verlegerichtung dieses Belags um 90 Grad. Die gespeicherten Modulmaße bleiben gleich, für Belegung, Darstellung und Materialberechnung werden Breite und Höhe vertauscht.");
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

    private <T> void registerRenderListener(ObjectProperty<T> property) {
        property.addListener((ignored, oldValue, newValue) -> render());
    }

    private Button createActionButton(String label, String style, Runnable action, String tooltipText) {
        Button button = new Button(label);
        button.setOnAction(event -> runGuardedAction(label, action));
        if (style != null) {
            button.setStyle(style);
        }
        applyTooltip(button, tooltipText);
        return button;
    }

    private void configureCanvas() {
        horizontalRuler.setHeight(RULER_SIZE);
        verticalRuler.setWidth(RULER_SIZE);
        drawingCanvas.setFocusTraversable(true);

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
        drawingCanvas.requestFocus();
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
            pendingContextWorldPoint = pendingContextSelection == null
                    ? null
                    : screenToWorld(event.getX(), event.getY());
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
                edgeResizeBaseStaircases = List.copyOf(activeLevel.get().staircases());
                edgeResizeBaseFloorOpenings = List.copyOf(activeLevel.get().floorOpenings());
                edgeResizeBaseHeatingExclusionAreas = List.copyOf(activeLevel.get().heatingExclusionAreas());
                edgeResizeBaseHydronicHeatings = List.copyOf(activeLevel.get().hydronicHeatings());
                selectedEndpointGroup = null;
                selectionDragAnchor = null;
                openingDragId = null;
                historyCapturedForDrag = false;
                draftLabel.setText("Kanten-Handle ausgewählt: Ziehen verlängert oder kürzt das Bauteil entlang seiner Wandachse.");
                render();
                return;
            }
            // Für die Selektion wird der reine Klickpunkt verwendet, nicht das gerasterte Ergebnis.
            PlanPoint editPoint = rawEditPoint;
            selectedEndpointGroup = wallEditingService.findConnectedEndpoint(activeLevel.get().walls(), editPoint, SNAP_TOLERANCE).orElse(null);
            selectionDragAnchor = null;
            selectionDragBaseWalls = List.of();
            selectionDragBaseStaircases = List.of();
            selectionDragBaseRoomObjects = List.of();
            selectionDragBaseFloorOpenings = List.of();
            selectionDragBaseHeatingExclusionAreas = List.of();
            selectionDragBaseHydronicHeatings = List.of();
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

        DraftingConstraints constraints = currentConstraints(currentTool() == DrawingTool.WALL && !event.isShiftDown());
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
        } else if (currentTool() == DrawingTool.ROOF_WINDOW) {
            placeRoofWindow(draftStart);
            draftStart = null;
            previewSegment = null;
        } else if (currentTool() == DrawingTool.OBJECT) {
            placeRoomObject(draftStart);
            draftStart = null;
            previewSegment = null;
        } else if (currentTool() == DrawingTool.HEATING_MANIFOLD) {
            placeHydronicManifold(draftStart);
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
                baseLevel.replaceStaircases(edgeResizeBaseStaircases);
                baseLevel.replaceFloorOpenings(edgeResizeBaseFloorOpenings);
                baseLevel.replaceHeatingExclusionAreas(edgeResizeBaseHeatingExclusionAreas);
                baseLevel.replaceHydronicHeatings(edgeResizeBaseHydronicHeatings);
                boolean isWallHandle = activeEdgeHandle.kind() == EdgeResizeService.EdgeHandleKind.WALL_START
                        || activeEdgeHandle.kind() == EdgeResizeService.EdgeHandleKind.WALL_END;
                Set<UUID> excludedWallIds = isWallHandle ? Set.of(activeEdgeHandle.hostWallId()) : Set.of();
                List<Wall> snapWalls = isWallHandle
                        ? edgeResizeBaseWalls.stream()
                                .filter(wall -> !wall.id().equals(activeEdgeHandle.hostWallId()))
                                .toList()
                        : edgeResizeBaseWalls;
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
                activeLevel.get().replaceStaircases(result.staircases());
                activeLevel.get().replaceFloorOpenings(result.floorOpenings());
                activeLevel.get().replaceHeatingExclusionAreas(result.heatingExclusionAreas());
                activeLevel.get().replaceHydronicHeatings(result.hydronicHeatings());
                List<Staircase> resizedStaircasesWithUnderbuild = result.staircases().stream()
                        .filter(staircase -> staircase.leftUnderbuildWidth().toMillimeters() > 0.0
                                || staircase.rightUnderbuildWidth().toMillimeters() > 0.0)
                        .toList();
                resizedStaircasesWithUnderbuild.forEach(this::synchronizeStairUnderbuild);
                if (isWallHandle || !resizedStaircasesWithUnderbuild.isEmpty()) {
                    synchronizeRoomsFromWalls(activeLevel.get());
                }
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

        DraftingConstraints constraints = currentConstraints(currentTool() == DrawingTool.WALL && !event.isShiftDown());
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
                contextMenuSelection = pendingContextSelection;
                contextMenuWorldPoint = pendingContextWorldPoint;
                selectSingle(pendingContextSelection);
                selectionContextMenu.show(drawingCanvas, event.getScreenX(), event.getScreenY());
            }
            pendingContextSelection = null;
            pendingContextWorldPoint = null;
            updateMouseCursor();
            return;
        }

        if (activeEdgeHandle != null) {
            activeEdgeHandle = null;
            edgeResizeBaseWalls = List.of();
            edgeResizeBaseDoors = List.of();
            edgeResizeBaseWindows = List.of();
            edgeResizeBaseStaircases = List.of();
            edgeResizeBaseFloorOpenings = List.of();
            edgeResizeBaseHeatingExclusionAreas = List.of();
            edgeResizeBaseHydronicHeatings = List.of();
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
            selectionDragBaseFloorOpenings = List.of();
            selectionDragBaseHeatingExclusionAreas = List.of();
            selectionDragBaseHydronicHeatings = List.of();
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
                        currentStairEndLanding(),
                        currentStairLeftUnderbuild(),
                        currentStairRightUnderbuild(),
                        currentStairUndersideThickness()
                );
                activeLevel.get().addStaircase(staircase);
                synchronizeStairUnderbuild(staircase);
                selectSingle(new SelectionKey(RenderableKind.STAIR, activeLevel.get().name(), staircase.id().toString()));
            } else if (currentTool() == DrawingTool.FLOOR_EXTENSION
                    && Math.abs(previewSegment.end().xMillimeters() - previewSegment.start().xMillimeters()) > 1.0
                    && Math.abs(previewSegment.end().yMillimeters() - previewSegment.start().yMillimeters()) > 1.0) {
                rememberStateForUndo();
                FloorExtension extension = FloorExtension.create(
                        Optional.ofNullable(floorExtensionTypeSelector.getValue()).orElse(FloorExtensionType.BALCONY),
                        Optional.ofNullable(floorExtensionPlacementSelector.getValue()).orElse(FloorExtensionPlacement.EXTERIOR),
                        previewSegment.start(),
                        previewSegment.end(),
                        currentFloorExtensionThickness()
                );
                activeLevel.get().addFloorExtension(extension);
                selectSingle(new SelectionKey(RenderableKind.FLOOR_EXTENSION, activeLevel.get().name(), extension.id().toString()));
            } else if ((currentTool() == DrawingTool.FLOOR_OPENING_RECTANGLE
                    || currentTool() == DrawingTool.FLOOR_OPENING_CIRCLE)
                    && Math.abs(previewSegment.end().xMillimeters() - previewSegment.start().xMillimeters()) > 1.0
                    && Math.abs(previewSegment.end().yMillimeters() - previewSegment.start().yMillimeters()) > 1.0) {
                createFloorOpening(previewSegment, currentTool() == DrawingTool.FLOOR_OPENING_CIRCLE
                        ? FloorOpeningShape.CIRCLE
                        : FloorOpeningShape.RECTANGLE);
            } else if (currentTool() == DrawingTool.HEATING_EXCLUSION_RECTANGLE
                    && Math.abs(previewSegment.end().xMillimeters() - previewSegment.start().xMillimeters()) > 1.0
                    && Math.abs(previewSegment.end().yMillimeters() - previewSegment.start().yMillimeters()) > 1.0) {
                createHeatingExclusionArea(previewSegment);
            } else if (currentTool() == DrawingTool.HEATING_ZONE_RECTANGLE
                    && Math.abs(previewSegment.end().xMillimeters() - previewSegment.start().xMillimeters()) > 1.0
                    && Math.abs(previewSegment.end().yMillimeters() - previewSegment.start().yMillimeters()) > 1.0) {
                createHeatingZone(previewSegment);
            }
            markThreeDDirty();
        }
        draftStart = null;
        previewSegment = null;
        render();
    }

    private SelectionKey contextSelectionAt(MouseEvent event) {
        PlanPoint editPoint = screenToWorld(event.getX(), event.getY());
        return selectionQueryService.findSelection(activeLevel.get(), editPoint, SNAP_TOLERANCE).orElse(null);
    }

    private void updateModifierState(KeyEvent event) {
        altPressed = event.isAltDown();
        if (event.getCode() == KeyCode.SPACE) {
            spacePressed = event.getEventType() == KeyEvent.KEY_PRESSED;
        }
        updateMouseCursor();
    }

    private void handleGlobalShortcuts(KeyEvent event) {
        if (event.getEventType() != KeyEvent.KEY_PRESSED) {
            return;
        }
        if (!event.isShortcutDown() && moveSelectionWithArrowKey(event.getCode())) {
            event.consume();
            return;
        }
        if (!event.isShortcutDown()) {
            return;
        }
        if (event.getCode() == KeyCode.Z && event.isShiftDown()) {
            runGuardedAction("Wiederherstellen", this::redo);
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.Z) {
            runGuardedAction("Rückgängig", this::undo);
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.Q) {
            runGuardedAction("Beenden", this::requestApplicationExit);
            event.consume();
        }
    }

    private void requestApplicationExit() {
        if (!confirmApplicationClose()) {
            return;
        }
        applicationExitConfirmed = true;
        applicationExitRequested = true;
        Window window = currentWindow();
        if (window instanceof Stage stage) {
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
            if (stage.isShowing()) {
                stage.close();
            }
        } else if (window != null) {
            window.hide();
        }
        applicationExitAction.run();
    }

    public boolean confirmApplicationClose() {
        if (applicationExitConfirmed || !hasUnsavedChanges()) {
            return true;
        }
        if (automatedUnsavedChangesExitDecision != null) {
            return automatedUnsavedChangesExitDecision;
        }
        if (!interactiveDialogsEnabled) {
            return true;
        }

        ButtonType saveButton = new ButtonType("Sichern", ButtonBar.ButtonData.YES);
        ButtonType discardButton = new ButtonType("Ohne Sichern beenden", ButtonBar.ButtonData.NO);
        Alert alert = new Alert(Alert.AlertType.WARNING, "", saveButton, discardButton, ButtonType.CANCEL);
        alert.setTitle("Ungesicherte Änderungen");
        alert.setHeaderText("Änderungen vor dem Beenden sichern?");
        alert.setContentText("Das Projekt enthält Änderungen, die noch nicht gesichert wurden. Beim Beenden ohne Sichern gehen diese Änderungen verloren.");
        alert.getDialogPane().setPrefWidth(560);
        Window owner = currentWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        applyTooltip(alert.getDialogPane().lookupButton(saveButton),
                "Sichert das vollständige Gebäude und beendet CADas anschließend.");
        applyTooltip(alert.getDialogPane().lookupButton(discardButton),
                "Beendet CADas und verwirft alle Änderungen seit der letzten Gebäudesicherung.");
        applyTooltip(alert.getDialogPane().lookupButton(ButtonType.CANCEL),
                "Bricht das Beenden ab und kehrt zum aktuellen Projekt zurück.");

        Optional<ButtonType> decision = alert.showAndWait();
        if (decision.filter(saveButton::equals).isPresent()) {
            saveProject();
            return !hasUnsavedChanges();
        }
        return decision.filter(discardButton::equals).isPresent();
    }

    private boolean hasUnsavedChanges() {
        return currentChangeRevision != savedChangeRevision;
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
            EdgeResizeService.EdgeHandle edgeHandle = handle.orElseThrow();
            if (EdgeResizeService.isRectangleCorner(edgeHandle.kind())) {
                return PointerCursorService.PointerTarget.RESIZE_CORNER;
            }
            if (EdgeResizeService.isRectangleHorizontalResize(edgeHandle.kind())) {
                return PointerCursorService.PointerTarget.HORIZONTAL_EDGE;
            }
            if (EdgeResizeService.isRectangleVerticalResize(edgeHandle.kind())) {
                return PointerCursorService.PointerTarget.VERTICAL_EDGE;
            }
            Wall wall = activeLevel.get().findWall(edgeHandle.hostWallId());
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

        drawTerrainPlanArea(graphics);
        drawLowerLevel(graphics);

        if (showGuides.get()) {
            drawGuides(graphics);
        }
        drawTerrainElevation(graphics);
        drawRooms(graphics);
        drawWalls(graphics);
        drawWallSurfaceLayers(graphics);
        drawStaircases(graphics);
        drawFloorExtensions(graphics);
        drawDoors(graphics);
        drawWindows(graphics);
        drawRoofWindows(graphics);
        drawRoomObjects(graphics);
        drawHydronicHeatings(graphics);
        // Raumtexte werden vor den Bemaßungen gerendert, damit ihre Sperrflächen
        // als Seed-Blocker für die kollisionsfreie Maßtext-Platzierung dienen.
        List<TextBlockingBox> roomLabelBlockers = drawRoomLabels(graphics);
        drawWallDimensions(graphics, roomLabelBlockers);
        drawTerrainPlanMarkers(graphics);
        drawGrid(graphics);
        drawSelectionOverlay(graphics);
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
        while (spacingPixels < 8.0) {
            spacingPixels *= 10.0;
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

    private void drawSelectionOverlay(GraphicsContext graphics) {
        if (!projectionService.isPlanView(activeView.get()) || selectedSelections.isEmpty()) {
            return;
        }
        graphics.save();
        graphics.setStroke(Color.web("#d97f2f"));
        graphics.setLineWidth(3.0);
        for (SelectionKey selection : selectedSelections) {
            switch (selection.kind()) {
                case WALL -> activeLevel.get().walls().stream()
                        .filter(wall -> wall.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(wall -> drawWall(graphics, wall.axis(), wall.thickness(), Color.web("#d97f2f"), 1.0));
                case ROOM_FLOOR, ROOM_CEILING, ROOM_VOLUME -> activeLevel.get().rooms().stream()
                        .filter(room -> room.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(room -> graphics.strokePolygon(
                                room.outline().stream().mapToDouble(point -> toScreenProjectedX(point, 0.0)).toArray(),
                                room.outline().stream().mapToDouble(point -> toScreenProjectedY(point, 0.0)).toArray(),
                                room.outline().size()
                        ));
                case DOOR -> activeLevel.get().doors().stream()
                        .filter(door -> door.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(door -> drawSelectedOpening(graphics, door.wallId(), door.offsetFromStart(), door.width()));
                case WINDOW -> activeLevel.get().windows().stream()
                        .filter(window -> window.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(window -> drawSelectedOpening(graphics, window.wallId(), window.offsetFromStart(), window.width()));
                case ROOF_WINDOW -> activeLevel.get().roofWindows().stream()
                        .filter(roofWindow -> roofWindow.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(roofWindow -> drawRoofWindowOutline(graphics, roofWindow));
                case STAIR -> activeLevel.get().staircases().stream()
                        .filter(staircase -> staircase.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(staircase -> drawStairOutline(graphics, staircase));
                case ROOM_OBJECT -> activeLevel.get().roomObjects().stream()
                        .filter(roomObject -> roomObject.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(roomObject -> drawSelectedRoomObjectOutline(graphics, roomObject));
                case FLOOR_EXTENSION -> activeLevel.get().floorExtensions().stream()
                        .filter(extension -> extension.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(extension -> graphics.strokeRect(
                                toScreenX(extension.minX()),
                                toScreenY(extension.minY()),
                                extension.widthMillimeters() * scale(),
                                extension.depthMillimeters() * scale()
                        ));
                case FLOOR_OPENING -> activeLevel.get().floorOpenings().stream()
                        .filter(opening -> opening.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(opening -> drawSelectedFloorOpening(graphics, opening));
                case HEATING_ZONE -> activeLevel.get().hydronicHeatings().stream()
                        .flatMap(heating -> heating.zones().stream())
                        .filter(zone -> zone.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(zone -> drawSelectedHeatingZone(graphics, zone));
                case HEATING_MANIFOLD -> activeLevel.get().hydronicHeatings().stream()
                        .filter(heating -> heating.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(heating -> drawSelectedHeatingManifold(graphics, heating));
                case HEATING_EXCLUSION -> activeLevel.get().heatingExclusionAreas().stream()
                        .filter(area -> area.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(area -> drawSelectedHeatingExclusionArea(graphics, area));
                default -> {
                }
            }
        }
        graphics.restore();
    }

    private void drawSelectedOpening(GraphicsContext graphics, UUID wallId, Length offset, Length width) {
        Wall wall = activeLevel.get().findWall(wallId);
        PlanPoint start = wall.axis().pointAt(offset);
        PlanPoint end = wall.axis().pointAt(offset.add(width));
        graphics.strokeLine(
                toScreenProjectedX(start, 0.0),
                toScreenProjectedY(start, 0.0),
                toScreenProjectedX(end, 0.0),
                toScreenProjectedY(end, 0.0)
        );
    }

    private void drawSelectedRoomObjectOutline(GraphicsContext graphics, RoomObject roomObject) {
        double width = roomObject.width().toMillimeters() * scale();
        double depth = roomObject.depth().toMillimeters() * scale();
        graphics.save();
        graphics.translate(toScreenX(roomObject.center().xMillimeters()), toScreenY(roomObject.center().yMillimeters()));
        graphics.rotate(-roomObject.rotationDegrees());
        if (roomObject.shape() == RoomObjectShape.CIRCLE || roomObject.shape() == RoomObjectShape.OVAL) {
            graphics.strokeOval(-width / 2.0, -depth / 2.0, width, depth);
        } else {
            graphics.strokeRect(-width / 2.0, -depth / 2.0, width, depth);
        }
        graphics.restore();
    }

    private void drawSelectedFloorOpening(GraphicsContext graphics, FloorOpening opening) {
        double x = toScreenProjectedX(new PlanPoint(opening.minXMillimeters(), opening.minYMillimeters()), 0.0);
        double y = toScreenProjectedY(new PlanPoint(opening.minXMillimeters(), opening.minYMillimeters()), 0.0);
        double width = opening.width().toMillimeters() * scale();
        double height = opening.depth().toMillimeters() * scale();
        if (opening.shape() == FloorOpeningShape.CIRCLE) {
            graphics.strokeOval(x, y, width, height);
        } else {
            graphics.strokeRect(x, y, width, height);
        }
    }

    private void drawSelectedHeatingZone(GraphicsContext graphics, HeatingZone zone) {
        graphics.strokePolygon(
                zone.outline().stream().mapToDouble(point -> toScreenProjectedX(point, 0.0)).toArray(),
                zone.outline().stream().mapToDouble(point -> toScreenProjectedY(point, 0.0)).toArray(),
                zone.outline().size()
        );
    }

    private void drawSelectedHeatingManifold(GraphicsContext graphics, HydronicHeating heating) {
        double centerX = (heating.supplyPoint().xMillimeters() + heating.returnPoint().xMillimeters()) / 2.0;
        double centerY = (heating.supplyPoint().yMillimeters() + heating.returnPoint().yMillimeters()) / 2.0;
        double minX = centerX - heating.manifoldFreeAreaWidth().toMillimeters() / 2.0;
        double minY = centerY - heating.manifoldFreeAreaDepth().toMillimeters() / 2.0;
        graphics.strokeRect(
                toScreenProjectedX(new PlanPoint(minX, minY), 0.0),
                toScreenProjectedY(new PlanPoint(minX, minY), 0.0),
                heating.manifoldFreeAreaWidth().toMillimeters() * scale(),
                heating.manifoldFreeAreaDepth().toMillimeters() * scale()
        );
    }

    private void drawSelectedHeatingExclusionArea(GraphicsContext graphics, HeatingExclusionArea area) {
        graphics.strokeRect(
                toScreenProjectedX(new PlanPoint(area.minXMillimeters(), area.minYMillimeters()), 0.0),
                toScreenProjectedY(new PlanPoint(area.minXMillimeters(), area.minYMillimeters()), 0.0),
                area.widthMillimeters() * scale(),
                area.depthMillimeters() * scale()
        );
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
        for (Wall wall : lowerLevel.walls()) {
            drawWall(graphics, wall.axis(), wall.thickness(), gray, 1.0);
        }
        graphics.setGlobalAlpha(1.0);
    }

    private void drawWalls(GraphicsContext graphics) {
        graphics.setFill(Color.web("#2f2a24"));

        for (Wall wall : activeLevel.get().walls()) {
            boolean selected = isSelected(RenderableKind.WALL, wall.id().toString());
            if (projectionService.isPlanView(activeView.get())) {
                drawWall(graphics, wall.axis(), wall.thickness(), selected ? Color.web("#d97f2f") : CadColorPalette.WALL, 1.0);
            } else {
                drawWallElevation(graphics, wall, selected);
            }
        }
    }

    private void drawWallDimensions(GraphicsContext graphics, List<TextBlockingBox> seedBlockers) {
        if (!showDimensions.get() || !projectionService.isPlanView(activeView.get())) {
            return;
        }
        DimensionLabelOptions options = currentDimensionLabelOptions();
        List<PendingWallDimensionLabel> pendingLabels = new ArrayList<>();
        for (Wall wall : activeLevel.get().walls()) {
            appendWallDimensionLabels(pendingLabels, wall, options);
        }
        List<RenderedWallDimensionLabel> placed = dimensionLabelPlacementService.place(
                pendingLabels,
                seedBlockers,
                this::layoutWallDimensionLabel
        );
        for (RenderedWallDimensionLabel rendered : placed) {
            drawIsoDimensionLines(graphics, rendered.layout(), rendered.directionX(), rendered.directionY());
            graphics.setFill(CadColorPalette.DIMENSION_TEXT);
            graphics.setFont(DIMENSION_LABEL_FONT);
            graphics.fillText(rendered.pending().text(), rendered.textX(), rendered.baselineY());
        }
    }

    private DimensionLabelOptions currentDimensionLabelOptions() {
        return new DimensionLabelOptions(dimensionTextStyle.get());
    }

    private void appendWallDimensionLabels(List<PendingWallDimensionLabel> pendingLabels, Wall wall, DimensionLabelOptions options) {
        WallDimensionService.WallDimensions dimensions = wallDimensionService.dimensions(activeLevel.get(), wall);
        double isoExtra = currentDimensionStandard() == DimensionStandard.DIN_EN_ISO_7519_2025_01 ? 12.0 : 0.0;
        double baseOffset = Math.max(wall.thickness().toMillimeters() * scale() / 2.0 + 16.0 + isoExtra, 28.0 + isoExtra);
        double stepOffset = 20.0 + isoExtra;
        for (WallDimensionPlacementService.PlacedDimension placement : wallDimensionPlacementService.place(
                activeLevel.get(),
                wall,
                dimensions,
                scale(),
                baseOffset,
                stepOffset
        )) {
            WallDimensionService.SideDimension dimension = placement.dimension();
            pendingLabels.add(new PendingWallDimensionLabel(
                    dimension.dimensionSegment(),
                    dimensionLabelService.label(dimension, placement.exterior(), options),
                    placement.normalOffset(),
                    placement.lineDistanceFromAxis(),
                    Math.copySign(stepOffset, placement.normalOffset()),
                    dimension.length().toMillimeters(),
                    dimensionLabelService.deduplicationKey(dimension, placement.exterior())
            ));
        }
        if (dimensions.roomDimensions().isEmpty() && dimensions.exteriorDimension().isEmpty()) {
            WallDimensionPlacementService.PlacedDimension axisPlacement = wallDimensionPlacementService.placeAxisDimension(
                    activeLevel.get(),
                    wall,
                    scale(),
                    baseOffset
            );
            pendingLabels.add(new PendingWallDimensionLabel(
                    wall.axis(),
                    dimensionLabelService.label("Achsmaß", wall.axis().length(), false, options),
                    axisPlacement.normalOffset(),
                    axisPlacement.lineDistanceFromAxis(),
                    Math.copySign(stepOffset, axisPlacement.normalOffset()),
                    wall.axis().length().toMillimeters(),
                    ""
            ));
        }
    }

    private RenderedWallDimensionLabel layoutWallDimensionLabel(PendingWallDimensionLabel pendingLabel, double normalOffset) {
        PlanSegment segment = pendingLabel.segment();
        double startX = toScreenProjectedX(segment.start(), 0.0);
        double startY = toScreenProjectedY(segment.start(), 0.0);
        double endX = toScreenProjectedX(segment.end(), 0.0);
        double endY = toScreenProjectedY(segment.end(), 0.0);
        double directionX = endX - startX;
        double directionY = endY - startY;
        double directionLength = Math.max(1.0, Math.hypot(directionX, directionY));
        double effectiveOffset = dimensionLineLayoutService.projectedNormalOffset(
                normalOffset,
                activeView.get() != ViewOrientation.TOP,
                24.0
        );
        double screenPlacementSign = Math.copySign(1.0, effectiveOffset);
        DimensionLineLayoutService.DimensionLineLayout layout = dimensionLineLayoutService.layout(startX, startY, endX, endY, effectiveOffset);
        double textAwayDistance = dimensionLineLayoutService.isParallelToHorizontalText(directionX, directionY)
                ? DIMENSION_PARALLEL_TEXT_AWAY_DISTANCE
                : DIMENSION_TEXT_AWAY_DISTANCE;
        // Text von der Maßlinie weg verschieben (in Bildschirm-Normalenrichtung der Platzierungsseite).
        DimensionLineLayoutService.TextDelta away = dimensionLineLayoutService.textOffsetAwayFromLine(
                layout, screenPlacementSign, textAwayDistance
        );
        Text textMeasure = new Text(pendingLabel.text());
        textMeasure.setFont(DIMENSION_LABEL_FONT);
        double textX = layout.textX() + away.deltaX();
        double baselineY = layout.textY() + away.deltaY();
        TextBlockingBox blockingBox = new TextBlockingBox(
                textX + textMeasure.getLayoutBounds().getMinX() - DIMENSION_TEXT_PADDING,
                baselineY + textMeasure.getLayoutBounds().getMinY() - DIMENSION_TEXT_PADDING,
                textMeasure.getLayoutBounds().getWidth() + DIMENSION_TEXT_PADDING * 2.0,
                textMeasure.getLayoutBounds().getHeight() + DIMENSION_TEXT_PADDING * 2.0
        );
        return new RenderedWallDimensionLabel(
                pendingLabel,
                layout,
                directionX / directionLength,
                directionY / directionLength,
                normalOffset,
                textX,
                baselineY,
                blockingBox
        );
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
        if (jointWidth < 0.001 || layer.effectiveTileWidth().toMillimeters() * scale() < 14.0) {
            return;
        }
        TileLayoutRequest request = new TileLayoutRequest(
                Length.ofMillimeters(wallLength),
                Length.ofMillimeters(wall.maximumHeightMillimeters()),
                layer.effectiveTileWidth(),
                layer.effectiveTileHeight(),
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
                layer.effectiveTileWidth(),
                layer.effectiveTileHeight(),
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
            drawRoomSlopeMarker(graphics, room);
            drawRoomTileGrid(graphics, room);
            drawFloorOpenings(graphics, room);
            drawHeatingExclusionAreas(graphics, room);
        }
    }

    private void drawHydronicHeatings(GraphicsContext graphics) {
        if (!projectionService.isPlanView(activeView.get())) {
            return;
        }
        for (HydronicHeating heating : activeLevel.get().hydronicHeatings()) {
            Color color = heating.surfacePosition() == HeatingSurfacePosition.FLOOR
                    ? Color.web("#c53b32")
                    : Color.web("#2878a8");
            drawHeatingManifold(graphics, heating);
            graphics.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.55));
            graphics.setLineWidth(1.0);
            graphics.setLineDashes(5.0, 4.0);
            for (HeatingZone zone : heating.zones()) {
                boolean selected = selectedSelections.contains(new SelectionKey(RenderableKind.HEATING_ZONE, activeLevel.get().name(), zone.id().toString()));
                graphics.setStroke(selected ? Color.web("#f2a900") : Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.55));
                graphics.setLineWidth(selected ? 2.2 : 1.0);
                double[] xPoints = zone.outline().stream().mapToDouble(point -> toScreenProjectedX(point, 0.0)).toArray();
                double[] yPoints = zone.outline().stream().mapToDouble(point -> toScreenProjectedY(point, 0.0)).toArray();
                graphics.strokePolygon(xPoints, yPoints, xPoints.length);
                drawHeatingConnectionMarker(graphics, zone.supplyConnectionPoint(), "V", Color.web("#1f62d0"));
                drawHeatingConnectionMarker(graphics, zone.returnConnectionPoint(), "R", Color.web("#d33b32"));
            }
            graphics.setLineDashes();
            graphics.setLineWidth(clamp(heating.pipeDiameter().toMillimeters() * scale(), 1.2, 5.0));
            for (HydronicHeatingLayoutService.CircuitLayout circuit : hydronicHeatingLayoutService.layoutBestEffort(heating).circuits()) {
                drawHeatingRolePath(graphics, circuit.supplyConnectorPath(), circuit.bendRadius().toMillimeters(), Color.web("#1f62d0"), true);
                drawHeatingRolePath(graphics, circuit.returnConnectorPath(), circuit.bendRadius().toMillimeters(), Color.web("#d33b32"), true);
                drawHeatingRolePath(graphics, circuit.fieldSupplyPath(), circuit.bendRadius().toMillimeters(), Color.web("#1f62d0"), false);
                drawHeatingRolePath(graphics, circuit.fieldReturnPath(), circuit.bendRadius().toMillimeters(), Color.web("#d33b32"), false);
                drawHeatingConnectionMarker(graphics, circuit.supplyPort(), "V", Color.web("#1f62d0"));
                drawHeatingConnectionMarker(graphics, circuit.returnPort(), "R", Color.web("#d33b32"));
            }
        }
    }

    private void drawHeatingManifold(GraphicsContext graphics, HydronicHeating heating) {
        double centerX = (heating.supplyPoint().xMillimeters() + heating.returnPoint().xMillimeters()) / 2.0;
        double centerY = (heating.supplyPoint().yMillimeters() + heating.returnPoint().yMillimeters()) / 2.0;
        double minX = centerX - heating.manifoldFreeAreaWidth().toMillimeters() / 2.0;
        double minY = centerY - heating.manifoldFreeAreaDepth().toMillimeters() / 2.0;
        boolean selected = selectedSelections.contains(new SelectionKey(RenderableKind.HEATING_MANIFOLD, activeLevel.get().name(), heating.id().toString()));
        graphics.save();
        graphics.setFill(Color.color(0.75, 0.78, 0.74, selected ? 0.22 : 0.12));
        graphics.setStroke(selected ? Color.web("#f2a900") : Color.web("#6b746b"));
        graphics.setLineWidth(selected ? 2.2 : 1.0);
        graphics.setLineDashes(6.0, 4.0);
        graphics.fillRect(
                toScreenProjectedX(new PlanPoint(minX, minY), 0.0),
                toScreenProjectedY(new PlanPoint(minX, minY), 0.0),
                heating.manifoldFreeAreaWidth().toMillimeters() * scale(),
                heating.manifoldFreeAreaDepth().toMillimeters() * scale()
        );
        graphics.strokeRect(
                toScreenProjectedX(new PlanPoint(minX, minY), 0.0),
                toScreenProjectedY(new PlanPoint(minX, minY), 0.0),
                heating.manifoldFreeAreaWidth().toMillimeters() * scale(),
                heating.manifoldFreeAreaDepth().toMillimeters() * scale()
        );
        graphics.setLineDashes();
        drawHeatingConnectionMarker(graphics, heating.supplyPoint(), "HKV V", Color.web("#1f62d0"));
        drawHeatingConnectionMarker(graphics, heating.returnPoint(), "HKV R", Color.web("#d33b32"));
        graphics.restore();
    }

    private void drawHeatingRolePath(GraphicsContext graphics, List<PlanPoint> path, double radiusMillimeters, Color color, boolean connector) {
        graphics.setStroke(color);
        graphics.setLineDashes(connector ? new double[]{6.0, 4.0} : new double[0]);
        drawRoundedHeatingPath(graphics, path, radiusMillimeters);
        graphics.setLineDashes();
    }

    private void drawRoundedHeatingPath(GraphicsContext graphics, List<PlanPoint> path, double radiusMillimeters) {
        if (path.size() < 2) {
            return;
        }
        graphics.beginPath();
        graphics.moveTo(toScreenProjectedX(path.getFirst(), 0.0), toScreenProjectedY(path.getFirst(), 0.0));
        for (int index = 1; index + 1 < path.size(); index++) {
            PlanPoint previous = path.get(index - 1);
            PlanPoint current = path.get(index);
            PlanPoint next = path.get(index + 1);
            double firstLength = previous.distanceTo(current).toMillimeters();
            double secondLength = current.distanceTo(next).toMillimeters();
            double trim = Math.min(radiusMillimeters, Math.min(firstLength, secondLength) / 2.0);
            if (trim <= 0.001) {
                graphics.lineTo(toScreenProjectedX(current, 0.0), toScreenProjectedY(current, 0.0));
                continue;
            }
            PlanPoint before = interpolateToward(current, previous, trim / firstLength);
            PlanPoint after = interpolateToward(current, next, trim / secondLength);
            graphics.lineTo(toScreenProjectedX(before, 0.0), toScreenProjectedY(before, 0.0));
            graphics.quadraticCurveTo(
                    toScreenProjectedX(current, 0.0), toScreenProjectedY(current, 0.0),
                    toScreenProjectedX(after, 0.0), toScreenProjectedY(after, 0.0)
            );
        }
        graphics.lineTo(toScreenProjectedX(path.getLast(), 0.0), toScreenProjectedY(path.getLast(), 0.0));
        graphics.stroke();
    }

    private PlanPoint interpolateToward(PlanPoint start, PlanPoint target, double ratio) {
        return new PlanPoint(
                start.xMillimeters() + (target.xMillimeters() - start.xMillimeters()) * ratio,
                start.yMillimeters() + (target.yMillimeters() - start.yMillimeters()) * ratio
        );
    }

    private void drawHeatingConnectionMarker(GraphicsContext graphics, PlanPoint point, String label, Color color) {
        double x = toScreenProjectedX(point, 0.0);
        double y = toScreenProjectedY(point, 0.0);
        graphics.setFill(Color.web("#fcfaf5"));
        graphics.fillOval(x - 7.0, y - 7.0, 14.0, 14.0);
        graphics.setStroke(color);
        graphics.setLineWidth(2.0);
        graphics.strokeOval(x - 7.0, y - 7.0, 14.0, 14.0);
        graphics.setFill(color);
        graphics.fillText(label, x - 3.5, y + 4.0);
    }

    private void drawFloorOpenings(GraphicsContext graphics, Room room) {
        graphics.setFill(Color.web("#f6f1e8"));
        graphics.setLineWidth(2.0);
        for (FloorOpening opening : activeLevel.get().floorOpenings()) {
            if (!opening.roomId().equals(room.id())) {
                continue;
            }
            boolean selected = isSelected(RenderableKind.FLOOR_OPENING, opening.id().toString());
            graphics.setStroke(selected ? Color.web("#d97f2f") : Color.web("#8a5d32"));
            double x = toScreenProjectedX(new PlanPoint(opening.minXMillimeters(), opening.minYMillimeters()), 0.0);
            double y = toScreenProjectedY(new PlanPoint(opening.minXMillimeters(), opening.minYMillimeters()), 0.0);
            double width = opening.width().toMillimeters() * scale();
            double height = opening.depth().toMillimeters() * scale();
            if (opening.shape() == FloorOpeningShape.CIRCLE) {
                graphics.fillOval(x, y, width, height);
                graphics.strokeOval(x, y, width, height);
            } else {
                graphics.fillRect(x, y, width, height);
                graphics.strokeRect(x, y, width, height);
            }
        }
    }

    private void drawHeatingExclusionAreas(GraphicsContext graphics, Room room) {
        for (HeatingExclusionArea area : activeLevel.get().heatingExclusionAreas()) {
            if (!area.roomId().equals(room.id())) {
                continue;
            }
            boolean selected = isSelected(RenderableKind.HEATING_EXCLUSION, area.id().toString());
            double x = toScreenProjectedX(new PlanPoint(area.minXMillimeters(), area.minYMillimeters()), 0.0);
            double y = toScreenProjectedY(new PlanPoint(area.minXMillimeters(), area.minYMillimeters()), 0.0);
            double width = area.widthMillimeters() * scale();
            double height = area.depthMillimeters() * scale();
            graphics.save();
            graphics.setFill(Color.color(0.82, 0.18, 0.12, selected ? 0.28 : 0.16));
            graphics.setStroke(selected ? Color.web("#d97f2f") : Color.web("#9f3028"));
            graphics.setLineWidth(selected ? 2.6 : 1.6);
            graphics.setLineDashes(8.0, 5.0);
            graphics.fillRect(x, y, width, height);
            graphics.strokeRect(x, y, width, height);
            graphics.setLineDashes();
            graphics.restore();
        }
    }

    private void drawTerrainElevation(GraphicsContext graphics) {
        if (projectionService.isPlanView(activeView.get()) || !project.terrain().configured()) {
            return;
        }
        java.util.TreeMap<Long, Double> profile = new java.util.TreeMap<>();
        for (TerrainVertex vertex : project.terrain().vertices()) {
            long horizontal = Math.round(projectHorizontal(vertex.position(), 0.0));
            profile.merge(horizontal, vertex.elevationAboveLowestFloor().toMillimeters(), Math::max);
        }
        if (profile.size() < 2) {
            return;
        }
        graphics.setStroke(Color.web("#a67c46"));
        graphics.setLineWidth(2.4);
        Map.Entry<Long, Double> previous = null;
        for (Map.Entry<Long, Double> current : profile.entrySet()) {
            if (previous != null) {
                graphics.strokeLine(
                        toScreenHorizontal(previous.getKey()),
                        toScreenVertical(-previous.getValue()),
                        toScreenHorizontal(current.getKey()),
                        toScreenVertical(-current.getValue())
                );
            }
            previous = current;
        }
    }

    private void drawTerrainPlanArea(GraphicsContext graphics) {
        if (!projectionService.isPlanView(activeView.get()) || !project.terrain().configured()) {
            return;
        }
        double[] xPoints = project.terrain().vertices().stream()
                .mapToDouble(vertex -> toScreenProjectedX(vertex.position(), 0.0))
                .toArray();
        double[] yPoints = project.terrain().vertices().stream()
                .mapToDouble(vertex -> toScreenProjectedY(vertex.position(), 0.0))
                .toArray();
        graphics.setFill(Color.color(0.65, 0.49, 0.27, 0.16));
        graphics.fillPolygon(xPoints, yPoints, xPoints.length);
    }

    private void drawTerrainPlanMarkers(GraphicsContext graphics) {
        if (!projectionService.isPlanView(activeView.get()) || !project.terrain().configured()) {
            return;
        }
        graphics.setStroke(Color.web("#8a6337"));
        graphics.setFill(Color.web("#6f4e2c"));
        graphics.setLineWidth(2.0);
        List<TerrainVertex> vertices = project.terrain().vertices();
        for (int index = 0; index < vertices.size(); index++) {
            TerrainVertex vertex = vertices.get(index);
            TerrainVertex next = vertices.get((index + 1) % vertices.size());
            double x = toScreenProjectedX(vertex.position(), 0.0);
            double y = toScreenProjectedY(vertex.position(), 0.0);
            graphics.strokeLine(x, y,
                    toScreenProjectedX(next.position(), 0.0),
                    toScreenProjectedY(next.position(), 0.0));
            graphics.fillOval(x - 4.0, y - 4.0, 8.0, 8.0);
            graphics.fillText(vertex.elevationAboveLowestFloor().format(LengthUnit.METER, 2), x + 7.0, y - 7.0);
        }
    }

    /**
     * Zeichnet die Raumtexte und liefert ihre Sperrflächen zurück, damit
     * {@link #drawWallDimensions} die Maßtexte nicht über Raumangaben legt.
     */
    private List<TextBlockingBox> drawRoomLabels(GraphicsContext graphics) {
        List<TextBlockingBox> blockers = new ArrayList<>();
        if (!projectionService.isPlanView(activeView.get()) || !showAreaVolume.get()) {
            return blockers;
        }
        for (Room room : activeLevel.get().rooms()) {
            PlanPoint center = room.centerPoint();
            blockers.addAll(drawRoomLabel(graphics, room, center));
        }
        return blockers;
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
                layer.effectiveTileWidth(),
                layer.effectiveTileHeight(),
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
            if (SurfaceCoveringPresetService.VARIOTHERM_DRY_PANEL_SOURCE.equals(layer.coveringSource())) {
                drawVariothermPanelGrooves(graphics, tx, ty, tw, th);
            }
        }
        graphics.restore();
    }

    private void drawVariothermPanelGrooves(GraphicsContext graphics, double tileX, double tileY, double tileWidth, double tileHeight) {
        double pitch = SurfaceCoveringPresetService.VARIOTHERM_GROOVE_PITCH_MILLIMETERS;
        double radius = Math.max(1.0, (pitch - SurfaceCoveringPresetService.VARIOTHERM_PIPE_DIAMETER_MILLIMETERS) / 2.0);
        graphics.setStroke(Color.color(0.18, 0.36, 0.44, 0.48));
        graphics.setLineWidth(Math.max(0.45, 1.2 * scale()));
        for (double x = tileX; x <= tileX + tileWidth + 0.001; x += pitch) {
            for (double y = tileY; y <= tileY + tileHeight + 0.001; y += pitch) {
                double screenRadius = radius * scale();
                graphics.strokeOval(
                        toScreenX(x - radius),
                        toScreenY(y - radius),
                        screenRadius * 2.0,
                        screenRadius * 2.0
                );
            }
        }
    }

    private List<TextBlockingBox> drawRoomLabel(GraphicsContext graphics, Room room, PlanPoint center) {
        List<TextBlockingBox> blockers = new ArrayList<>();
        double centerX = toScreenProjectedX(center, 0.0);
        double centerY = toScreenProjectedY(center, 0.0);
        graphics.setFill(Color.web("#5d4527"));
        graphics.setFont(Font.font("Menlo", 12));
        String name = room.name();
        graphics.fillText(name, centerX - 26, centerY - 6);
        blockers.add(textBlockingBox(name, Font.font("Menlo", 12), centerX - 26, centerY - 6));
        String areaVolume = String.format(
                Locale.GERMAN,
                "%.2f m² | %.2f m³",
                surfaceLayerEffectService.effectiveAreaSquareMeters(activeLevel.get(), room),
                surfaceLayerEffectService.effectiveVolumeCubicMeters(activeLevel.get(), room)
        );
        graphics.setFont(Font.font("Menlo", 11));
        graphics.fillText(areaVolume, centerX - 42, centerY + 12);
        blockers.add(textBlockingBox(areaVolume, Font.font("Menlo", 11), centerX - 42, centerY + 12));
        return blockers;
    }

    private TextBlockingBox textBlockingBox(String text, Font font, double x, double y) {
        Text measure = new Text(text);
        measure.setFont(font);
        var bounds = measure.getLayoutBounds();
        double boxX = x + bounds.getMinX() - DIMENSION_TEXT_PADDING;
        double boxY = y + bounds.getMinY() - DIMENSION_TEXT_PADDING;
        return new TextBlockingBox(
                boxX,
                boxY,
                bounds.getWidth() + DIMENSION_TEXT_PADDING * 2.0,
                bounds.getHeight() + DIMENSION_TEXT_PADDING * 2.0
        );
    }

    private void drawRoomSlopeMarker(GraphicsContext graphics, Room room) {
        if (!projectionService.isPlanView(activeView.get()) || room.slopedCeilingProfiles().isEmpty()) {
            return;
        }
        for (int index = 0; index < room.slopedCeilingProfiles().size(); index++) {
            drawRoomSlopeMarker(graphics, room, room.slopedCeilingProfiles().get(index), index);
        }
    }

    private void drawRoomSlopeMarker(
            GraphicsContext graphics,
            Room room,
            SlopedCeilingProfile profile,
            int labelIndex
    ) {
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
                        room.slopeAngleDegrees(profile)),
                toScreenProjectedX(room.centerPoint(), 0.0) - 72,
                toScreenProjectedY(room.centerPoint(), 0.0) + 28 + labelIndex * 14.0
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

    private void drawRoofWindows(GraphicsContext graphics) {
        if (!projectionService.isPlanView(activeView.get())) {
            return;
        }
        for (RoofWindow roofWindow : activeLevel.get().roofWindows()) {
            boolean selected = isSelected(RenderableKind.ROOF_WINDOW, roofWindow.id().toString());
            graphics.save();
            graphics.setFill(selected ? Color.color(0.35, 0.72, 0.92, 0.42) : Color.color(0.35, 0.72, 0.92, 0.25));
            graphics.setStroke(selected ? Color.web("#d97f2f") : Color.web("#2c789f"));
            graphics.setLineWidth(selected ? 2.8 : 1.8);
            double x = toScreenProjectedX(new PlanPoint(
                    roofWindow.center().xMillimeters() - roofWindow.width().toMillimeters() / 2.0,
                    roofWindow.center().yMillimeters() - roofWindow.depth().toMillimeters() / 2.0
            ), 0.0);
            double y = toScreenProjectedY(new PlanPoint(
                    roofWindow.center().xMillimeters() - roofWindow.width().toMillimeters() / 2.0,
                    roofWindow.center().yMillimeters() - roofWindow.depth().toMillimeters() / 2.0
            ), 0.0);
            double width = roofWindow.width().toMillimeters() * scale();
            double depth = roofWindow.depth().toMillimeters() * scale();
            graphics.fillRect(x, y, width, depth);
            graphics.strokeRect(x, y, width, depth);
            graphics.strokeLine(x, y, x + width, y + depth);
            graphics.strokeLine(x + width, y, x, y + depth);
            graphics.restore();
        }
    }

    private void drawRoofWindowOutline(GraphicsContext graphics, RoofWindow roofWindow) {
        double x = toScreenProjectedX(new PlanPoint(
                roofWindow.center().xMillimeters() - roofWindow.width().toMillimeters() / 2.0,
                roofWindow.center().yMillimeters() - roofWindow.depth().toMillimeters() / 2.0
        ), 0.0);
        double y = toScreenProjectedY(new PlanPoint(
                roofWindow.center().xMillimeters() - roofWindow.width().toMillimeters() / 2.0,
                roofWindow.center().yMillimeters() - roofWindow.depth().toMillimeters() / 2.0
        ), 0.0);
        graphics.strokeRect(x, y,
                roofWindow.width().toMillimeters() * scale(),
                roofWindow.depth().toMillimeters() * scale());
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
            boolean isWallHandle = handle.kind() == EdgeResizeService.EdgeHandleKind.WALL_START
                    || handle.kind() == EdgeResizeService.EdgeHandleKind.WALL_END;
            double size = active ? (isWallHandle ? 14.0 : 11.0) : (isWallHandle ? 11.0 : 9.0);
            graphics.save();
            graphics.setFill(active ? Color.web("#d97f2f") : Color.web("#fffaf1"));
            graphics.setStroke(Color.web("#201c18"));
            graphics.setLineWidth(1.4);
            if (isWallHandle) {
                double half = size / 2.0;
                graphics.fillPolygon(
                        new double[]{x, x + half, x, x - half},
                        new double[]{y - half, y, y + half, y}, 4);
                graphics.strokePolygon(
                        new double[]{x, x + half, x, x - half},
                        new double[]{y - half, y, y + half, y}, 4);
            } else {
                graphics.fillRect(x - size / 2.0, y - size / 2.0, size, size);
                graphics.strokeRect(x - size / 2.0, y - size / 2.0, size, size);
            }
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
                drawRoomObjectPlan(graphics, roomObject);
            } else {
                double x = toScreenProjectedX(roomObject.center(), 0.0) - width * scale() / 2.0;
                double y = toScreenProjectedY(roomObject.center(), roomObject.baseElevation().toMillimeters() + roomObject.height().toMillimeters());
                graphics.strokeRect(x, y, width * scale(), roomObject.height().toMillimeters() * scale());
            }
        }
    }

    private void drawRoomObjectPlan(GraphicsContext graphics, RoomObject roomObject) {
        double w = roomObject.width().toMillimeters() * scale();
        double h = roomObject.depth().toMillimeters() * scale();
        double x = -w / 2.0;
        double y = -h / 2.0;
        graphics.save();
        graphics.translate(toScreenX(roomObject.center().xMillimeters()), toScreenY(roomObject.center().yMillimeters()));
        graphics.rotate(-roomObject.rotationDegrees());
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
        graphics.restore();
        if (roomObject.type() == RoomObjectType.CUBOID && !roomObject.name().isBlank()) {
            graphics.save();
            graphics.setFill(Color.web("#183f37"));
            graphics.setFont(Font.font("Menlo", 11));
            graphics.setTextAlign(TextAlignment.CENTER);
            graphics.fillText(roomObject.name(), toScreenX(roomObject.center().xMillimeters()), toScreenY(roomObject.center().yMillimeters()) + 4.0);
            graphics.restore();
        }
    }

    private void drawFloorExtensions(GraphicsContext graphics) {
        for (FloorExtension extension : activeLevel.get().floorExtensions()) {
            boolean selected = isSelected(RenderableKind.FLOOR_EXTENSION, extension.id().toString());
            graphics.save();
            graphics.setStroke(selected ? Color.web("#d97f2f") : Color.web("#806b4f"));
            graphics.setFill(selected ? Color.color(0.85, 0.50, 0.18, 0.28) : Color.color(0.58, 0.49, 0.36, 0.20));
            graphics.setLineWidth(1.8);
            if (projectionService.isPlanView(activeView.get())) {
                double x = toScreenX(extension.minX());
                double y = toScreenY(extension.minY());
                double width = extension.widthMillimeters() * scale();
                double depth = extension.depthMillimeters() * scale();
                graphics.fillRect(x, y, width, depth);
                graphics.strokeRect(x, y, width, depth);
                graphics.setFill(Color.web("#4b4034"));
                graphics.setFont(Font.font("Menlo", 11));
                graphics.fillText(extension.type().toString(), x + 8, y + 18);
            } else {
                double[] horizontals = extension.outline().stream().mapToDouble(point -> projectHorizontal(point, 0.0)).toArray();
                double min = java.util.Arrays.stream(horizontals).min().orElse(0.0);
                double max = java.util.Arrays.stream(horizontals).max().orElse(0.0);
                double left = toScreenHorizontal(min);
                double right = toScreenHorizontal(max);
                double floor = toScreenVertical(0.0);
                double bottom = toScreenVertical(extension.slabThickness().toMillimeters());
                graphics.fillRect(Math.min(left, right), Math.min(floor, bottom), Math.max(2, Math.abs(right - left)), Math.max(2, Math.abs(bottom - floor)));
                graphics.strokeRect(Math.min(left, right), Math.min(floor, bottom), Math.max(2, Math.abs(right - left)), Math.max(2, Math.abs(bottom - floor)));
            }
            graphics.restore();
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
        double sx = segment.start().xMillimeters();
        double sy = segment.start().yMillimeters();
        double ex = segment.end().xMillimeters();
        double ey = segment.end().yMillimeters();
        double dx = ex - sx;
        double dy = ey - sy;
        double length = Math.hypot(dx, dy);
        if (length < 0.001) {
            return;
        }
        double nx = -dy / length;
        double ny = dx / length;
        double h = thickness.toMillimeters() / 2.0 * widthFactor;
        PlanPoint p1 = new PlanPoint(sx + nx * h, sy + ny * h);
        PlanPoint p2 = new PlanPoint(ex + nx * h, ey + ny * h);
        PlanPoint p3 = new PlanPoint(ex - nx * h, ey - ny * h);
        PlanPoint p4 = new PlanPoint(sx - nx * h, sy - ny * h);
        double[] xPoints = {
                toScreenProjectedX(p1, 0.0),
                toScreenProjectedX(p2, 0.0),
                toScreenProjectedX(p3, 0.0),
                toScreenProjectedX(p4, 0.0)
        };
        double[] yPoints = {
                toScreenProjectedY(p1, 0.0),
                toScreenProjectedY(p2, 0.0),
                toScreenProjectedY(p3, 0.0),
                toScreenProjectedY(p4, 0.0)
        };
        graphics.setFill(color);
        graphics.fillPolygon(xPoints, yPoints, 4);
        graphics.setStroke(color);
        graphics.setLineWidth(1.0);
        graphics.strokePolygon(xPoints, yPoints, 4);
    }

    private void drawWallElevation(GraphicsContext graphics, Wall wall, boolean selected) {
        List<WallProfilePoint> profile = wall.resolvedProfile();
        double[] xPoints = new double[profile.size() + 2];
        double[] yPoints = new double[profile.size() + 2];
        xPoints[0] = toScreenProjectedX(wall.axis().start(), 0.0);
        yPoints[0] = toScreenProjectedY(wall.axis().start(), 0.0);
        xPoints[1] = toScreenProjectedX(wall.axis().end(), 0.0);
        yPoints[1] = toScreenProjectedY(wall.axis().end(), 0.0);
        for (int index = 0; index < profile.size(); index++) {
            var profilePoint = profile.get(profile.size() - 1 - index);
            PlanPoint point = wall.axis().pointAt(profilePoint.offset());
            xPoints[index + 2] = toScreenProjectedX(point, profilePoint.height().toMillimeters());
            yPoints[index + 2] = toScreenProjectedY(point, profilePoint.height().toMillimeters());
        }
        graphics.setFill(selected ? Color.color(0.85, 0.57, 0.22, 0.24) : Color.color(0.23, 0.39, 0.54, 0.18));
        graphics.fillPolygon(xPoints, yPoints, profile.size() + 2);
        graphics.setStroke(selected ? Color.web("#d97f2f") : CadColorPalette.WALL);
        graphics.setLineWidth(2.0);
        graphics.strokePolygon(xPoints, yPoints, profile.size() + 2);
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
        if (currentTool() == DrawingTool.STAIR
                || currentTool() == DrawingTool.FLOOR_EXTENSION
                || currentTool() == DrawingTool.FLOOR_OPENING_RECTANGLE
                || currentTool() == DrawingTool.FLOOR_OPENING_CIRCLE
                || currentTool() == DrawingTool.HEATING_ZONE_RECTANGLE
                || currentTool() == DrawingTool.HEATING_EXCLUSION_RECTANGLE) {
            graphics.setFill(Color.color(0.45, 0.37, 0.29, 0.18));
            graphics.setStroke(Color.web("#7f6a55"));
            graphics.setLineWidth(2.0);
            double previewX = toScreenProjectedX(new PlanPoint(startX, startY), 0.0);
            double previewY = toScreenProjectedY(new PlanPoint(startX, startY), 0.0);
            double previewWidth = (endX - startX) * scale();
            double previewHeight = (endY - startY) * scale();
            if (currentTool() == DrawingTool.FLOOR_OPENING_CIRCLE) {
                double diameter = Math.min(previewWidth, previewHeight);
                graphics.fillOval(previewX, previewY, diameter, diameter);
                graphics.strokeOval(previewX, previewY, diameter, diameter);
            } else {
                graphics.fillRect(previewX, previewY, previewWidth, previewHeight);
                graphics.strokeRect(previewX, previewY, previewWidth, previewHeight);
            }
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
            DimensionLineLayoutService.TextDelta away = dimensionLineLayoutService.textOffsetAwayFromLine(
                    layout, -1.0, DIMENSION_TEXT_AWAY_DISTANCE
            );
            midX = layout.textX() + away.deltaX();
            midY = layout.textY() + away.deltaY();
        } else {
            midX += -directionY / directionLength * normalOffset;
            midY += directionX / directionLength * normalOffset;
        }
        graphics.setFill(CadColorPalette.DIMENSION_TEXT);
        graphics.setFont(DIMENSION_LABEL_FONT);
        graphics.fillText(text, midX, midY);
    }

    private DimensionStandard currentDimensionStandard() {
        return DimensionStandard.DIN_EN_ISO_7519_2025_01;
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
                null
        );
    }

    private Length currentRoomHeight() {
        return parseLength(roomHeightField, roomHeightUnit.getValue()).orElse(DEFAULT_ROOM_HEIGHT);
    }

    private Length currentFloorThickness() {
        return parseLength(floorThicknessField, floorThicknessUnit.getValue()).orElse(DEFAULT_FLOOR_THICKNESS);
    }

    private Length currentFloorExtensionThickness() {
        return parseLength(floorExtensionThicknessField, floorExtensionThicknessUnit.getValue()).orElse(DEFAULT_FLOOR_THICKNESS);
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

    private Length currentStairStartLanding() {
        return parseLength(stairStartLandingField, stairStartLandingUnit.getValue()).orElse(Length.ofMillimeters(0));
    }

    private Length currentStairEndLanding() {
        return parseLength(stairEndLandingField, stairEndLandingUnit.getValue()).orElse(Length.ofMillimeters(0));
    }

    private Length currentStairLeftUnderbuild() {
        return parseLength(stairLeftUnderbuildField, stairLeftUnderbuildUnit.getValue()).orElse(Length.zero());
    }

    private Length currentStairRightUnderbuild() {
        return parseLength(stairRightUnderbuildField, stairRightUnderbuildUnit.getValue()).orElse(Length.zero());
    }

    private Length currentStairUndersideThickness() {
        return parseLength(stairUndersideThicknessField, stairUndersideThicknessUnit.getValue()).orElse(Length.zero());
    }

    private Length currentRoomObjectWidth(RoomObjectPreset preset) {
        return positiveLength(roomObjectWidthField, roomObjectWidthUnit, preset.width());
    }

    private String currentRoomObjectName(RoomObjectPreset preset) {
        String name = roomObjectNameField.getText();
        return name == null || name.isBlank() ? Optional.ofNullable(preset).map(RoomObjectPreset::name).orElse("Objekt") : name.trim();
    }

    private Length currentRoomObjectDepth(RoomObjectPreset preset) {
        return positiveLength(roomObjectDepthField, roomObjectDepthUnit, preset.depth());
    }

    private Length currentRoomObjectHeight(RoomObjectPreset preset) {
        return positiveLength(roomObjectHeightField, roomObjectHeightUnit, preset.height());
    }

    private Length currentRoomObjectBaseElevation() {
        return parseLength(roomObjectBaseElevationField, roomObjectBaseElevationUnit.getValue()).orElse(Length.zero());
    }

    private Length positiveLength(TextField field, ComboBox<LengthUnit> unitSelector, Length fallback) {
        return parseLength(field, unitSelector.getValue())
                .filter(length -> length.toMillimeters() > 0.0)
                .orElse(fallback);
    }

    private double currentRoomObjectAngleDegrees() {
        return parseAngle(roomObjectAngleField).map(Angle::degrees).orElse(0.0);
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
            case ROOF_WINDOW -> "Werkzeug: Dachfenster | Linksklick in einem Raum mit Dachschräge platziert das Dachfenster mit Fensterbreite und Fensterhöhe.";
            case STAIR -> "Werkzeug: Treppe | Rechteck aufziehen platziert die Treppe mit dem gewählten Preset.";
            case FLOOR_EXTENSION -> "Werkzeug: Balkon/Empore | Rechteck aufziehen fügt die Fußbodenplatte innen oder außen an die aktive Etage an.";
            case FLOOR_OPENING_RECTANGLE -> "Werkzeug: Bodenloch rechteckig | Rechteck innerhalb eines Raums aufziehen.";
            case FLOOR_OPENING_CIRCLE -> "Werkzeug: Bodenloch rund | Begrenzungsquadrat innerhalb eines Raums aufziehen.";
            case HEATING_ZONE_RECTANGLE -> "Werkzeug: Heizkreis | Rechteck innerhalb eines Raums aufziehen; Standard ist Schnecke, die Verlegung kann danach im Kontextmenü geändert werden.";
            case HEATING_MANIFOLD -> "Werkzeug: HKV | Linksklick in einem Raum setzt den Verteiler für die gewählte Boden- oder Deckenheizung.";
            case HEATING_EXCLUSION_RECTANGLE -> "Werkzeug: FBH-Sperrfläche | Rechteck innerhalb eines Raums aufziehen; der FBH-Layouter spart diese Fläche aus.";
            case OBJECT -> "Werkzeug: Objekt | Linksklick platziert das ausgewählte Objekt-Preset innen oder außen.";
        };
    }

    private void applyTooltip(javafx.scene.Node node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(320);
        Tooltip.install(node, tooltip);
    }

    private void createLevel() {
        String levelName = "";
        if (interactiveDialogsEnabled) {
            TextInputDialog dialog = new TextInputDialog("Etage " + (availableLevels.size() + 1));
            dialog.setTitle("Neue Etage");
            dialog.setHeaderText("Neue Etage anlegen");
            dialog.setContentText("Name der Etage:");
            dialog.getDialogPane().setPrefWidth(420);
            Window owner = getScene() != null ? getScene().getWindow() : null;
            if (owner != null) {
                dialog.initOwner(owner);
            }
            levelName = dialog.showAndWait()
                    .map(String::trim)
                    .filter(name -> !name.isBlank())
                    .orElse(null);
        }
        if (levelName == null) {
            return;
        }
        String finalLevelName = levelName.isBlank() ? "Etage " + (availableLevels.size() + 1) : levelName;
        rememberStateForUndo();
        Level level = project.createLevel(finalLevelName);
        availableLevels.add(level);
        activateLevel(level);
        fitCurrentViewToContent();
    }

    private void renameCurrentLevel() {
        Level current = activeLevel.get();
        if (current == null) {
            return;
        }
        String newName = current.name();
        if (interactiveDialogsEnabled) {
            TextInputDialog dialog = new TextInputDialog(current.name());
            dialog.setTitle("Etage umbenennen");
            dialog.setHeaderText("Aktuelle Etage umbenennen");
            dialog.setContentText("Neuer Name der Etage:");
            dialog.getDialogPane().setPrefWidth(420);
            Window owner = getScene() != null ? getScene().getWindow() : null;
            if (owner != null) {
                dialog.initOwner(owner);
            }
            newName = dialog.showAndWait()
                    .map(String::trim)
                    .filter(name -> !name.isBlank())
                    .orElse(null);
        }
        if (newName == null) {
            return;
        }
        if (newName.equals(current.name())) {
            return;
        }
        rememberStateForUndo();
        try {
            project.renameLevel(current, newName);
        } catch (IllegalArgumentException exception) {
            UiErrorDialogs.show(
                    UiErrorDialogs.fromThrowable(
                            "Etage konnte nicht umbenannt werden",
                            "Der Name ist ungültig oder bereits vergeben.",
                            exception.getMessage(),
                            exception
                    ),
                    currentWindow(),
                    interactiveDialogsEnabled
            );
            return;
        }
        int index = availableLevels.indexOf(current);
        if (index >= 0) {
            availableLevels.set(index, current);
        }
        activateLevel(current);
    }

    private void moveCurrentLevelUp() {
        moveCurrentLevel(-1);
    }

    private void moveCurrentLevelDown() {
        moveCurrentLevel(1);
    }

    private void moveCurrentLevel(int direction) {
        Level current = activeLevel.get();
        if (current == null) {
            return;
        }
        int currentIndex = availableLevels.indexOf(current);
        if (currentIndex < 0) {
            return;
        }
        int newIndex = currentIndex + direction;
        if (newIndex < 0 || newIndex >= availableLevels.size()) {
            return;
        }
        rememberStateForUndo();
        try {
            project.moveLevel(current, newIndex);
        } catch (IndexOutOfBoundsException exception) {
            UiErrorDialogs.show(
                    UiErrorDialogs.fromThrowable(
                            "Etage konnte nicht verschoben werden",
                            "Der neue Etage-Index liegt außerhalb des gültigen Bereichs.",
                            exception.getMessage(),
                            exception
                    ),
                    currentWindow(),
                    interactiveDialogsEnabled
            );
            return;
        }
        availableLevels.setAll(project.levels());
        activateLevel(current);
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

    private void placeRoofWindow(PlanPoint clickPoint) {
        roofWindowPlacementService.place(
                        activeLevel.get(), clickPoint, currentWindowWidth(), currentWindowHeight()
                )
                .ifPresentOrElse(roofWindow -> {
                    rememberStateForUndo();
                    activeLevel.get().addRoofWindow(roofWindow);
                    selectSingle(new SelectionKey(RenderableKind.ROOF_WINDOW, activeLevel.get().name(), roofWindow.id().toString()));
                    markThreeDDirty();
                    draftLabel.setText("Dachfenster auf Dachschräge platziert.");
                }, () -> draftLabel.setText("Dachfenster können nur innerhalb eines Raums mit Dachschräge platziert werden."));
    }

    private void placeRoomObject(PlanPoint clickPoint) {
        RoomObjectPreset preset = roomObjectPresetSelector.getValue();
        if (preset == null) {
            draftLabel.setText("Kein Objekt-Preset ausgewählt.");
            return;
        }
        rememberStateForUndo();
        RoomObject roomObject = RoomObject.create(
                preset.id(),
                currentRoomObjectName(preset),
                preset.type(),
                preset.shape(),
                clickPoint,
                currentRoomObjectWidth(preset),
                currentRoomObjectDepth(preset),
                currentRoomObjectHeight(preset),
                currentRoomObjectAngleDegrees(),
                preset.mountingMode(),
                preset.source()
        ).withBaseElevation(currentRoomObjectBaseElevation());
        activeLevel.get().addRoomObject(roomObject);
        selectSingle(new SelectionKey(RenderableKind.ROOM_OBJECT, activeLevel.get().name(), roomObject.id().toString()));
        markThreeDDirty();
    }

    private void createFloorOpening(PlanSegment bounds, FloorOpeningShape shape) {
        double width = Math.abs(bounds.end().xMillimeters() - bounds.start().xMillimeters());
        double depth = Math.abs(bounds.end().yMillimeters() - bounds.start().yMillimeters());
        PlanPoint center = new PlanPoint(
                (bounds.start().xMillimeters() + bounds.end().xMillimeters()) / 2.0,
                (bounds.start().yMillimeters() + bounds.end().yMillimeters()) / 2.0
        );
        if (shape == FloorOpeningShape.CIRCLE) {
            double diameter = Math.min(width, depth);
            width = diameter;
            depth = diameter;
        }
        Optional<Room> room = roomAt(center);
        if (room.isEmpty()) {
            draftLabel.setText("Bodenöffnungen müssen mit ihrem Mittelpunkt in einem Raum liegen.");
            return;
        }
        rememberStateForUndo();
        FloorOpening opening = FloorOpening.create(
                room.orElseThrow().id(), shape, center,
                Length.ofMillimeters(width), Length.ofMillimeters(depth)
        );
        activeLevel.get().addFloorOpening(opening);
        selectSingle(new SelectionKey(RenderableKind.FLOOR_OPENING, activeLevel.get().name(), opening.id().toString()));
        markThreeDDirty();
        draftLabel.setText("Bodenöffnung erzeugt.");
    }

    private void createHeatingExclusionArea(PlanSegment bounds) {
        PlanPoint center = new PlanPoint(
                (bounds.start().xMillimeters() + bounds.end().xMillimeters()) / 2.0,
                (bounds.start().yMillimeters() + bounds.end().yMillimeters()) / 2.0
        );
        Optional<Room> room = roomAt(center);
        if (room.isEmpty()) {
            draftLabel.setText("FBH-Sperrflächen müssen mit ihrem Mittelpunkt in einem Raum liegen.");
            return;
        }
        rememberStateForUndo();
        HeatingExclusionArea area = HeatingExclusionArea.create(
                room.orElseThrow().id(),
                "FBH-Sperrfläche " + (activeLevel.get().heatingExclusionAreas().size() + 1),
                bounds.start(),
                bounds.end()
        );
        activeLevel.get().addHeatingExclusionArea(area);
        selectSingle(new SelectionKey(RenderableKind.HEATING_EXCLUSION, activeLevel.get().name(), area.id().toString()));
        draftLabel.setText("FBH-Sperrfläche erzeugt.");
    }

    private void createHeatingZone(PlanSegment bounds) {
        PlanPoint center = new PlanPoint(
                (bounds.start().xMillimeters() + bounds.end().xMillimeters()) / 2.0,
                (bounds.start().yMillimeters() + bounds.end().yMillimeters()) / 2.0
        );
        Room room = roomAt(center).orElse(null);
        if (room == null) {
            draftLabel.setText("Heizkreise müssen mit ihrem Mittelpunkt in einem Raum liegen.");
            return;
        }
        HeatingSurfacePosition surfacePosition = Optional.ofNullable(heatingSurfacePositionSelector.getValue())
                .orElse(HeatingSurfacePosition.FLOOR);
        HydronicHeating existing = activeLevel.get().findHydronicHeating(room.id(), surfacePosition);
        HydronicHeating heating;
        try {
            heating = existing == null
                    ? heatingFromInputs(room, UUID.randomUUID())
                    : existing;
        } catch (RuntimeException exception) {
            draftLabel.setText("Heizkreis nicht erzeugt: " + UiErrorDialogs.userMessage(exception));
            return;
        }
        List<HeatingZone> zones = new ArrayList<>(heating.zones());
        HeatingZone zone = new HeatingZone(
                UUID.randomUUID(),
                "Heizkreis " + (zones.size() + 1),
                rectanglePoints(heatingZoneBounds(bounds)),
                HeatingLayoutPattern.SPIRAL,
                false
        );
        zones.add(zone);
        HydronicHeating updatedHeating = heating.withZones(zones);
        try {
            hydronicHeatingLayoutService.validateZoneGeometry(room, updatedHeating);
        } catch (RuntimeException exception) {
            draftLabel.setText("Heizkreis nicht erzeugt: " + UiErrorDialogs.userMessage(exception));
            return;
        }
        rememberStateForUndo();
        if (existing == null) {
            activeLevel.get().addHydronicHeating(updatedHeating);
        } else {
            activeLevel.get().replaceHydronicHeating(updatedHeating);
        }
        selectSingle(new SelectionKey(RenderableKind.HEATING_ZONE, activeLevel.get().name(), zone.id().toString()));
        refreshHeatingSection();
        draftLabel.setText(heatingUpdateMessage(updatedHeating, "Heizkreis erzeugt."));
    }

    private void placeHydronicManifold(PlanPoint point) {
        Room room = roomAt(point).orElse(null);
        if (room == null) {
            draftLabel.setText("HKV muss in einem Raum gesetzt werden.");
            return;
        }
        PlanPoint returnPoint = new PlanPoint(
                point.xMillimeters() + DEFAULT_HKV_PAIR_DISTANCE_MILLIMETERS,
                point.yMillimeters()
        );
        setLengthInput(heatingSupplyXField, heatingSupplyXUnit, Length.ofMillimeters(point.xMillimeters()), LengthUnit.CENTIMETER);
        setLengthInput(heatingSupplyYField, heatingSupplyYUnit, Length.ofMillimeters(point.yMillimeters()), LengthUnit.CENTIMETER);
        setLengthInput(heatingReturnXField, heatingReturnXUnit, Length.ofMillimeters(returnPoint.xMillimeters()), LengthUnit.CENTIMETER);
        setLengthInput(heatingReturnYField, heatingReturnYUnit, Length.ofMillimeters(returnPoint.yMillimeters()), LengthUnit.CENTIMETER);
        HydronicHeating existing = activeLevel.get().findHydronicHeating(
                room.id(),
                Optional.ofNullable(heatingSurfacePositionSelector.getValue()).orElse(HeatingSurfacePosition.FLOOR)
        );
        HydronicHeating updated;
        try {
            updated = existing == null
                    ? heatingFromInputs(room, UUID.randomUUID())
                    : existing.withManifold(point, returnPoint);
        } catch (RuntimeException exception) {
            draftLabel.setText("HKV nicht gesetzt: " + UiErrorDialogs.userMessage(exception));
            return;
        }
        rememberStateForUndo();
        if (existing == null) {
            activeLevel.get().addHydronicHeating(updated);
        } else {
            activeLevel.get().replaceHydronicHeating(updated);
        }
        selectSingle(new SelectionKey(RenderableKind.HEATING_MANIFOLD, activeLevel.get().name(), updated.id().toString()));
        refreshHeatingSection();
        draftLabel.setText("HKV gesetzt.");
        render();
    }

    private Optional<Room> roomAt(PlanPoint point) {
        return selectionQueryService.findSelections(activeLevel.get(), point, SNAP_TOLERANCE).stream()
                .filter(selection -> selection.kind() == RenderableKind.ROOM_VOLUME)
                .findFirst()
                .flatMap(selection -> activeLevel.get().rooms().stream()
                        .filter(candidate -> candidate.id().toString().equals(selection.elementId()))
                        .findFirst());
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

    private void saveCurrentLevel() {
        if (lastLevelSavePath != null) {
            saveCurrentLevelTo(lastLevelSavePath);
            return;
        }
        saveCurrentLevelAs();
    }

    private void saveCurrentLevelAs() {
        FileChooser fileChooser = createCadasFileChooser();
        String levelName = exchangeFileNameService.stripRepeatedExtension(Path.of(activeLevel.get().name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(levelName);
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            return;
        }
        Path targetFile = file.toPath();
        if (!targetFile.getFileName().toString().contains(".")) {
            targetFile = exchangeFileNameService.ensureSingleExtension(targetFile, ".cadas");
        }
        String newLevelName = exchangeFileNameService.stripRepeatedExtension(targetFile.getFileName(), ".cadas");
        if (!newLevelName.isBlank() && !newLevelName.equals(activeLevel.get().name())) {
            rememberStateForUndo();
            project.renameLevel(activeLevel.get(), newLevelName);
            int index = availableLevels.indexOf(activeLevel.get());
            if (index >= 0) {
                availableLevels.set(index, activeLevel.get());
            }
        }
        saveCurrentLevelTo(targetFile);
    }

    private void saveCurrentLevelTo(Path targetFile) {
        try {
            Path exportPath = targetFile.toAbsolutePath().normalize();
            levelExchangeService.exportLevel(activeLevel.get(), exportPath);
            lastLevelSavePath = exportPath;
            draftLabel.setText("Etage gesichert: " + exportPath.getFileName());
        } catch (Exception exception) {
            showOperationException("Etagen-Sicherung fehlgeschlagen", exception);
        }
    }

    private void exportCurrentLevel(Path targetFile) {
        saveCurrentLevelTo(targetFile);
    }

    private void saveProject() {
        if (lastProjectSavePath != null) {
            exportProjectAsDxf(lastProjectSavePath);
            return;
        }
        saveProjectAs();
    }

    private void saveProjectAs() {
        FileChooser fileChooser = createCadasFileChooser();
        String projectName = exchangeFileNameService.stripRepeatedExtension(Path.of(project.name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(projectName);
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            return;
        }
        Path targetFile = file.toPath();
        if (!targetFile.getFileName().toString().contains(".")) {
            targetFile = exchangeFileNameService.ensureSingleExtension(targetFile, ".cadas");
        }
        String newProjectName = exchangeFileNameService.stripRepeatedExtension(targetFile.getFileName(), ".cadas");
        if (!newProjectName.isBlank() && !newProjectName.equals(project.name())) {
            rememberStateForUndo();
            project.rename(newProjectName);
        }
        exportProjectAsDxf(targetFile);
    }

    private void exportProjectAsDxf(Path targetFile) {
        try {
            Path exportPath = targetFile.toAbsolutePath().normalize();
            projectExchangeService.exportProject(project, exportPath);
            lastProjectSavePath = exportPath;
            savedChangeRevision = currentChangeRevision;
            confirmExportWritten(exportPath);
        } catch (Exception exception) {
            showOperationException("Gebäude-Sicherung fehlgeschlagen", exception);
        }
    }

    private void confirmExportWritten(Path exportPath) {
        if (Files.exists(exportPath) && Files.isRegularFile(exportPath)) {
            if (interactiveDialogsEnabled) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Gebäude gesichert");
                alert.setHeaderText("Die Gebäude-Datei wurde erfolgreich gespeichert.");
                alert.setContentText(exportPath.toString());
                alert.getDialogPane().setPrefWidth(560);
                Window owner = getScene() != null ? getScene().getWindow() : null;
                if (owner != null) {
                    alert.initOwner(owner);
                }
                alert.showAndWait();
            }
            draftLabel.setText("Gebäude gesichert: " + exportPath.getFileName());
        } else {
            draftLabel.setText("Sicherung konnte nicht verifiziert werden: " + exportPath);
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
        stage.setTitle("Räume und Materialien");
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
        String projectName = exchangeFileNameService.stripRepeatedExtension(Path.of(project.name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(projectName + "_Bauzeichnung.pdf");
        Window owner = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        try {
            Path target = exchangeFileNameService.ensureSingleExtension(file.toPath(), ".pdf");
            ConstructionDrawingOptions options = new ConstructionDrawingOptions(
                    currentDimensionLabelOptions(),
                    showDimensions.get(),
                    showAreaVolume.get()
            );
            constructionDrawingPdfService.export(project, target, options);
            draftLabel.setText("Bauzeichnungs-PDF exportiert: " + target.getFileName());
        } catch (Exception exception) {
            showOperationException("PDF-Export fehlgeschlagen", exception);
        }
    }

    private void showHelpWindow() {
        showMarkdownWindow(helpContentService.createMarkdown(), "CADas-Benutzerdokumentation", "Benutzerdokumentation", "Druckt die vollständige Benutzerdokumentation. Im Druckdialog kann auch ein PDF-Drucker gewählt werden.");
    }

    private void showKeymapWindow() {
        showMarkdownWindow(helpContentService.createKeymapMarkdown(), "CADas-Keymap und Mausbedienung", "Keymap und Mausbedienung", "Druckt die Tastaturkürzel und Mausbedienung. Im Druckdialog kann auch ein PDF-Drucker gewählt werden.");
    }

    private void showThirdPartyLicensesWindow() {
        showMarkdownWindow(helpContentService.createThirdPartyLicensesMarkdown(), "CADas-Drittanbieter-Lizenzen", "Drittanbieter-Lizenzen", "Druckt die automatisch erzeugte Liste aller Drittanbieter-Lizenzen.");
    }

    public void showAboutDialog() {
        if (!interactiveDialogsEnabled) {
            return;
        }
        AboutInformation information = AboutInformation.current();
        Alert alert = new Alert(Alert.AlertType.INFORMATION, information.detailText(), ButtonType.OK);
        alert.setTitle("Über CADas");
        alert.setHeaderText(information.applicationName());
        Window owner = currentWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private void showMarkdownWindow(String markdown, String windowTitle, String documentName, String printTooltip) {
        WebView view = new WebView();
        view.getEngine().loadContent(markdownHtmlRenderer.renderDocument(markdown));
        VBox.setVgrow(view, Priority.ALWAYS);
        ComboBox<HelpSection> sectionSelector = new ComboBox<>();
        sectionSelector.getItems().setAll(markdownNavigationService.sections(markdown));
        sectionSelector.setPromptText("Inhaltsverzeichnis");
        sectionSelector.setPrefWidth(300);
        sectionSelector.setOnAction(event -> Optional.ofNullable(sectionSelector.getValue())
                .ifPresent(section -> view.getEngine().executeScript(
                        "document.getElementById('" + section.anchor() + "').scrollIntoView({behavior:'smooth',block:'start'});"
                )));
        applyTooltip(sectionSelector, "Listet alle Kapitel und Unterkapitel auf und springt direkt zum gewählten Abschnitt der Dokumentation.");
        TextField searchField = new TextField();
        searchField.setPromptText("Dokumentation durchsuchen");
        searchField.setPrefWidth(260);
        applyTooltip(searchField, "Sucht im vollständigen Text der geöffneten Dokumentation. Mit Eingabe oder Weiter wird der nächste Treffer markiert.");
        Button previousButton = new Button("Zurück");
        previousButton.setOnAction(event -> findInWebView(view, searchField.getText(), true));
        applyTooltip(previousButton, "Springt rückwärts zum vorherigen Treffer des eingegebenen Suchbegriffs.");
        Button nextButton = new Button("Weiter");
        nextButton.setOnAction(event -> findInWebView(view, searchField.getText(), false));
        applyTooltip(nextButton, "Springt vorwärts zum nächsten Treffer des eingegebenen Suchbegriffs.");
        searchField.setOnAction(event -> findInWebView(view, searchField.getText(), false));
        HBox navigation = new HBox(8.0, sectionSelector, searchField, previousButton, nextButton);
        navigation.setAlignment(Pos.CENTER_LEFT);
        Button printButton = new Button("Drucken");
        printButton.setOnAction(event -> printWebView(view, documentName));
        applyTooltip(printButton, printTooltip);
        HBox actions = new HBox(8.0, printButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox container = new VBox(10.0, navigation, view, actions);
        container.setPadding(new Insets(12));
        Stage stage = new Stage();
        stage.setTitle(windowTitle);
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setScene(new Scene(container, 960, 760));
        stage.show();
    }

    private void findInWebView(WebView view, String searchText, boolean backwards) {
        if (searchText == null || searchText.isBlank()) {
            return;
        }
        String escaped = searchText.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
        view.getEngine().executeScript("window.find('" + escaped + "',false," + backwards + ",true,false,true,false);");
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
        if (interactiveDialogsEnabled && !printerJob.showPrintDialog(owner)) {
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
        String projectName = exchangeFileNameService.stripRepeatedExtension(Path.of(project.name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(projectName + "_Räume_und_Material");
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
            showOperationException("Materiallisten-Export fehlgeschlagen", exception);
        }
    }

    private void importLevel() {
        FileChooser fileChooser = createCadasFileChooser();
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        importLevel(file.toPath());
    }

    private void importThreeDObject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("3D-Objekt aus DXF, IFC oder RFA laden");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "3D-CAD-Dateien", "*.dxf", "*.DXF", "*.ifc", "*.IFC", "*.rfa", "*.RFA"));
        Window window = currentWindow();
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        importThreeDObject(file.toPath());
    }

    private void importThreeDObject(Path sourceFile) {
        try {
            Path targetFile = roomObjectPresetService.importTarget(sourceFile);
            if (Files.exists(targetFile)
                    && !isSameFile(sourceFile, targetFile)
                    && !confirmOverwrite(
                    "3D-CAD-Objekt überschreiben",
                    "Ein 3D-CAD-Objekt mit dem Namen `" + sourceFile.getFileName() + "` ist bereits registriert.",
                    "Soll die vorhandene Objektdatei ersetzt werden?"
            )) {
                return;
            }
            RoomObjectPreset preset = roomObjectPresetService.importCad3dObject(sourceFile);
            registerRoomObjectPreset(preset);
            roomObjectPresetSelector.setValue(preset);
            toolSelector.setValue(DrawingTool.OBJECT);
            draftLabel.setText("3D-CAD-Objekt geladen: " + sourceFile.getFileName());
        } catch (IOException | IllegalArgumentException exception) {
            showOperationException("3D-CAD-Import fehlgeschlagen", exception);
        }
    }

    private void importLevel(Path sourceFile) {
        try {
            rememberStateForUndo();
            String levelName = uniqueLevelName(exchangeFileNameService.stripRepeatedExtension(sourceFile, ".cadas"));
            Level importedLevel = levelExchangeService.importLevel(sourceFile, levelName);
            importedLevel.replaceRooms(autoRoomGenerationService.synchronize(importedLevel, currentRoomDefaults()));
            project.addLevel(importedLevel);
            availableLevels.add(importedLevel);
            activateLevel(importedLevel);
            fitCurrentViewToContent();
            lastLevelSavePath = sourceFile.toAbsolutePath().normalize();
            draftLabel.setText("Etage geladen: " + sourceFile.getFileName());
        } catch (Exception exception) {
            showOperationException("Etagen-Laden fehlgeschlagen", exception);
        }
    }

    private void importProjectFromDxf() {
        FileChooser fileChooser = createCadasFileChooser();
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
            String projectName = exchangeFileNameService.stripRepeatedExtension(sourceFile, ".cadas");
            ProjectModel importedProject = projectExchangeService.importProject(sourceFile, projectName);
            importedProject.levels().forEach(level -> level.replaceRooms(autoRoomGenerationService.synchronize(level, currentRoomDefaults())));
            project.replaceWith(importedProject);
            availableLevels.setAll(project.levels());
            guideLines.clear();
            clearSelectionsInternal();
            activateLevel(project.primaryLevel());
            markThreeDDirty();
            fitCurrentViewToContent();
            lastProjectSavePath = sourceFile.toAbsolutePath().normalize();
            savedChangeRevision = currentChangeRevision;
            draftLabel.setText("Gebäude geladen: " + sourceFile.getFileName());
        } catch (Exception exception) {
            showOperationException("Gebäude-Laden fehlgeschlagen", exception);
        }
    }

    private FileChooser createCadasFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("CADas-Gebäudedatei auswählen");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CADas-Gebäudedateien", "*.cadas"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DXF-Dateien", "*.dxf"));
        fileChooser.setInitialDirectory(Path.of(System.getProperty("user.home")).toFile());
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
            showOperationException("Teilebibliothek fehlgeschlagen", exception);
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
            showOperationException("DWG-Bibliothek konnte nicht in das Belagsverzeichnis übernommen werden", exception);
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
            showOperationException("Belagspreset existiert bereits und wurde nicht überschrieben", exception);
        } catch (IOException exception) {
            showOperationException("Belagspreset konnte nicht gespeichert werden", exception);
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
        if (!interactiveDialogsEnabled) {
            // In der Automatisierung Overwrite ohne Nachfrage bestätigen, damit Aktionen nicht hängen.
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(header);
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner != null) {
            alert.initOwner(owner);
        }
        return alert.showAndWait()
                .filter(ButtonType.OK::equals)
                .isPresent();
    }

    private void applyDoorPreset(DoorPreset preset) {
        if (preset == null) {
            return;
        }
        setLengthInput(doorWidthField, doorWidthUnit, preset.width(), LengthUnit.CENTIMETER);
        setLengthInput(doorHeightField, doorHeightUnit, preset.height(), LengthUnit.CENTIMETER);
        setLengthInput(thresholdField, thresholdUnit, preset.thresholdHeight(), LengthUnit.CENTIMETER);
    }

    private void applyWindowPreset(WindowPreset preset) {
        if (preset == null) {
            return;
        }
        setLengthInput(windowWidthField, windowWidthUnit, preset.width(), LengthUnit.CENTIMETER);
        setLengthInput(windowHeightField, windowHeightUnit, preset.height(), LengthUnit.CENTIMETER);
        setLengthInput(sillHeightField, sillHeightUnit, preset.sillHeight(), LengthUnit.CENTIMETER);
    }

    private void applyStairPreset(StairPreset preset) {
        if (preset == null) {
            return;
        }
        setLengthInput(stairHeightField, stairHeightUnit, preset.totalHeight(), LengthUnit.CENTIMETER);
        stairStepsField.setText(Integer.toString(preset.stepCount()));
    }

    private void applyRoomObjectPreset(RoomObjectPreset preset) {
        if (preset == null) {
            return;
        }
        roomObjectNameField.setText(preset.name());
        setLengthInput(roomObjectWidthField, roomObjectWidthUnit, preset.width(), LengthUnit.CENTIMETER);
        setLengthInput(roomObjectDepthField, roomObjectDepthUnit, preset.depth(), LengthUnit.CENTIMETER);
        setLengthInput(roomObjectHeightField, roomObjectHeightUnit, preset.height(), LengthUnit.CENTIMETER);
        setLengthInput(roomObjectBaseElevationField, roomObjectBaseElevationUnit, Length.zero(), LengthUnit.CENTIMETER);
        roomObjectAngleField.setText("0");
    }

    private void applySurfacePreset(SurfaceCoveringPreset preset) {
        if (preset == null) {
            return;
        }
        surfaceLayerNameField.setText(preset.name().replace("DWG-Referenz: ", "").replace("DWG-Block: ", ""));
        setLengthInput(surfaceLayerThicknessField, surfaceLayerThicknessUnit, preset.thickness(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceTileWidthField, surfaceTileWidthUnit, preset.tileWidth(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceTileHeightField, surfaceTileHeightUnit, preset.tileHeight(), LengthUnit.CENTIMETER);
        surfaceRotateLayoutCheckBox.setSelected(false);
        surfaceLayoutModeSelector.setValue(preset.layoutMode());
        setLengthInput(surfaceLayoutOffsetField, surfaceLayoutOffsetUnit, preset.offset(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceMinimumOffsetField, surfaceMinimumOffsetUnit, preset.minimumOffset(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit, preset.minimumEdgeWidth(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit, preset.minimumStartEndMargin(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceJointWidthField, surfaceJointWidthUnit, preset.jointWidth(), LengthUnit.CENTIMETER);
        surfaceCutRestrictionSelector.setValue(preset.cutRestriction());
        dwgBlockNameField.setText(extractDwgBlockName(preset.coveringSource()).orElse(""));
    }

    private Optional<HydronicHeating> selectedHydronicHeating() {
        Optional<HeatingZoneContext> zoneContext = selectedHeatingZoneContext();
        if (zoneContext.isPresent()) {
            return Optional.of(zoneContext.orElseThrow().heating());
        }
        Optional<HeatingContext> heatingContext = selectedHeatingContext();
        if (heatingContext.isPresent()) {
            return Optional.of(heatingContext.orElseThrow().heating());
        }
        Room room = selectedRoom().orElse(null);
        HeatingSurfacePosition surfacePosition = heatingSurfacePositionSelector.getValue();
        if (room == null || surfacePosition == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeLevel.get().findHydronicHeating(room.id(), surfacePosition));
    }

    private void refreshHeatingSection() {
        Room room = selectedRoom().orElse(null);
        HydronicHeating heating = selectedHydronicHeating().orElse(null);
        if (room == null) {
            heatingSummaryLabel.setText("Für die Heizflächenplanung zuerst genau einen Raum auswählen.");
            heatingZoneList.getItems().clear();
            updateActionButtons();
            return;
        }
        if (heating == null) {
            heatingSummaryLabel.setText("Für " + heatingSurfacePositionSelector.getValue() + " ist noch keine Heizung angelegt.");
            heatingZoneList.getItems().clear();
            updateActionButtons();
            return;
        }
        if (heatingSurfacePositionSelector.getValue() != heating.surfacePosition()) {
            heatingSurfacePositionSelector.setValue(heating.surfacePosition());
        }
        heatingLayoutPatternSelector.setValue(heating.layoutPattern());
        syncLengthInput(heatingPipeSpacingField, heatingPipeSpacingUnit, heating.pipeSpacing(), LengthUnit.CENTIMETER);
        syncLengthInput(heatingPipeDiameterField, heatingPipeDiameterUnit, heating.pipeDiameter(), LengthUnit.CENTIMETER);
        syncLengthInput(heatingMaximumPipeLengthField, heatingMaximumPipeLengthUnit, heating.maximumPipeLength(), LengthUnit.CENTIMETER);
        syncLengthInput(heatingWallClearanceField, heatingWallClearanceUnit, heating.wallClearance(), LengthUnit.CENTIMETER);
        syncLengthInput(heatingSupplyXField, heatingSupplyXUnit, Length.ofMillimeters(heating.supplyPoint().xMillimeters()), LengthUnit.CENTIMETER);
        syncLengthInput(heatingSupplyYField, heatingSupplyYUnit, Length.ofMillimeters(heating.supplyPoint().yMillimeters()), LengthUnit.CENTIMETER);
        syncLengthInput(heatingReturnXField, heatingReturnXUnit, Length.ofMillimeters(heating.returnPoint().xMillimeters()), LengthUnit.CENTIMETER);
        syncLengthInput(heatingReturnYField, heatingReturnYUnit, Length.ofMillimeters(heating.returnPoint().yMillimeters()), LengthUnit.CENTIMETER);
        HydronicHeatingLayoutService.PlanningResult layoutResult = hydronicHeatingLayoutService.layoutBestEffort(heating);
        List<HydronicHeatingLayoutService.CircuitLayout> circuits = layoutResult.circuits();
        int selectedIndex = selectedHeatingZoneContext()
                .filter(context -> context.heating().id().equals(heating.id()))
                .map(HeatingZoneContext::zoneIndex)
                .orElse(heatingZoneList.getSelectionModel().getSelectedIndex());
        heatingZoneList.getItems().setAll(heating.zones().stream()
                .map(zone -> describeHeatingZone(zone, circuits))
                .toList());
        if (!heatingZoneList.getItems().isEmpty()) {
            heatingZoneList.getSelectionModel().select(Math.max(0, Math.min(selectedIndex, heatingZoneList.getItems().size() - 1)));
        }
        double totalLength = circuits.stream().mapToDouble(circuit -> circuit.pipeLength().toMillimeters()).sum();
        boolean maximumExceeded = circuits.stream()
                .anyMatch(circuit -> circuit.pipeLength().compareTo(heating.maximumPipeLength()) > 0);
        String warning = heatingWarning(layoutResult.validationReport(), maximumExceeded);
        heatingSummaryLabel.setText(String.format(
                Locale.GERMAN,
                "%s · Raumvorgabe %s · %d Heizkreis(e) · %.1f m Rohr%s",
                heating.surfacePosition(), heating.layoutPattern(), circuits.size(), totalLength / 1_000.0, warning
        ));
        updateActionButtons();
    }

    private String describeHeatingZone(HeatingZone zone, List<HydronicHeatingLayoutService.CircuitLayout> circuits) {
        double pipeLength = circuits.stream()
                .filter(circuit -> circuit.zoneId().equals(zone.id()))
                .findFirst()
                .map(circuit -> circuit.pipeLength().toMillimeters())
                .orElse(0.0);
        String roleOrientation = zone.flowInverted() ? "invertiert" : "normal";
        return String.format(
                Locale.GERMAN,
                "%s · %s · %s · %.1f m · %.2f m²",
                zone.name(), zone.layoutPattern(), roleOrientation, pipeLength / 1_000.0, zone.areaSquareMillimeters() / 1_000_000.0
        );
    }

    private String heatingUpdateMessage(HydronicHeating heating, String successPrefix) {
        HydronicHeatingLayoutService.PlanningResult layoutResult = hydronicHeatingLayoutService.layoutBestEffort(heating);
        boolean maximumExceeded = layoutResult.circuits().stream()
                .anyMatch(circuit -> circuit.pipeLength().compareTo(heating.maximumPipeLength()) > 0);
        String warning = heatingWarning(layoutResult.validationReport(), maximumExceeded);
        return successPrefix + (warning.isBlank() ? "" : " " + warning.strip());
    }

    private String heatingWarning(HydronicHeatingLayoutService.ValidationReport report, boolean maximumExceeded) {
        List<String> warnings = new ArrayList<>();
        if (!report.valid()) {
            warnings.add(report.summary());
        }
        if (maximumExceeded) {
            warnings.add("Mindestens ein Heizkreis überschreitet die maximale Rohrlänge.");
        }
        if (warnings.isEmpty()) {
            return "";
        }
        return " · Warnung: " + String.join(" ", warnings);
    }

    private void planHydronicHeating() {
        throw new IllegalStateException("Die automatische Planung ganzer Räume ist vorübergehend deaktiviert. Lege Heizkreise mit dem Werkzeug `Heizkreis` als Rechtecke an.");
    }

    private void planHydronicHeatingAutomatically() {
        Room room = selectedRoom().orElseThrow(() -> new IllegalStateException("Für die Heizflächenplanung muss ein Raum ausgewählt sein."));
        HydronicHeating existing = selectedHydronicHeating().orElse(null);
        HydronicHeating unplanned = heatingFromInputs(room, existing == null ? UUID.randomUUID() : existing.id());
        HydronicHeatingLayoutService.PlanningResult result = hydronicHeatingLayoutService.suggest(
                room,
                unplanned,
                activeLevel.get().staircases(),
                activeLevel.get().floorOpenings(),
                activeLevel.get().heatingExclusionAreas()
        );
        if (!result.validationReport().valid()) {
            throw new IllegalArgumentException(result.validationReport().summary());
        }
        boolean maximumExceeded = result.circuits().stream()
                .anyMatch(circuit -> circuit.pipeLength().compareTo(result.heating().maximumPipeLength()) > 0);
        if (maximumExceeded) {
            throw new IllegalArgumentException("Die maximale Rohrlänge kann mit den gewählten Verteilerpunkten und Abständen nicht eingehalten werden.");
        }
        rememberStateForUndo();
        if (existing == null) {
            activeLevel.get().addHydronicHeating(result.heating());
        } else {
            activeLevel.get().replaceHydronicHeating(result.heating());
        }
        refreshHeatingSection();
        String warningText = formatHeatingWarnings(result.validationReport().warnings());
        draftLabel.setText(result.heating().zones().size() + " Heizkreis(e) für " + result.heating().surfacePosition() + " geplant." + warningText);
        showHeatingWarnings(result.validationReport().warnings());
        render();
    }

    private String formatHeatingWarnings(List<HydronicHeatingLayoutService.ValidationIssue> warnings) {
        if (warnings.isEmpty()) {
            return "";
        }
        return " Warnung: " + warnings.stream()
                .map(HydronicHeatingLayoutService.ValidationIssue::message)
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private void showHeatingWarnings(List<HydronicHeatingLayoutService.ValidationIssue> warnings) {
        if (warnings.isEmpty() || !interactiveDialogsEnabled) {
            return;
        }
        Alert alert = new Alert(
                Alert.AlertType.WARNING,
                warnings.stream()
                        .map(HydronicHeatingLayoutService.ValidationIssue::message)
                        .collect(java.util.stream.Collectors.joining(System.lineSeparator())),
                ButtonType.OK
        );
        alert.setTitle("FBH-Planung mit Warnungen");
        alert.setHeaderText("CADas hat nur eine angepasste FBH-Planung erstellen können.");
        Window owner = currentWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private HydronicHeating heatingFromInputs(Room room, UUID heatingId) {
        HeatingSurfacePosition surfacePosition = Optional.ofNullable(heatingSurfacePositionSelector.getValue())
                .orElse(HeatingSurfacePosition.FLOOR);
        HeatingLayoutPattern layoutPattern = Optional.ofNullable(heatingLayoutPatternSelector.getValue())
                .orElse(HeatingLayoutPattern.SPIRAL);
        return new HydronicHeating(
                heatingId, room.id(), surfacePosition, layoutPattern,
                requiredPositiveLength(heatingPipeSpacingField, heatingPipeSpacingUnit, "Verlegeabstand"),
                requiredPositiveLength(heatingPipeDiameterField, heatingPipeDiameterUnit, "Rohrdurchmesser"),
                requiredPositiveLength(heatingMaximumPipeLengthField, heatingMaximumPipeLengthUnit, "maximale Rohrlänge"),
                requiredNonNegativeLength(heatingWallClearanceField, heatingWallClearanceUnit, "Wandabstand"),
                new PlanPoint(
                        requiredCoordinate(heatingSupplyXField, heatingSupplyXUnit, "Vorlauf X"),
                        requiredCoordinate(heatingSupplyYField, heatingSupplyYUnit, "Vorlauf Y")
                ),
                new PlanPoint(
                        requiredCoordinate(heatingReturnXField, heatingReturnXUnit, "Rücklauf X"),
                        requiredCoordinate(heatingReturnYField, heatingReturnYUnit, "Rücklauf Y")
                ),
                List.of()
        );
    }

    private Length requiredPositiveLength(TextField field, ComboBox<LengthUnit> unitSelector, String label) {
        Length length = parseLength(field, unitSelector.getValue())
                .orElseThrow(() -> new IllegalArgumentException(label + " ist keine gültige Länge."));
        if (length.toMillimeters() <= 0.0) {
            throw new IllegalArgumentException(label + " muss größer als null sein.");
        }
        return length;
    }

    private Length requiredNonNegativeLength(TextField field, ComboBox<LengthUnit> unitSelector, String label) {
        Length length = parseLength(field, unitSelector.getValue())
                .orElseThrow(() -> new IllegalArgumentException(label + " ist keine gültige Länge."));
        if (length.toMillimeters() < 0.0) {
            throw new IllegalArgumentException(label + " darf nicht negativ sein.");
        }
        return length;
    }

    private double requiredCoordinate(TextField field, ComboBox<LengthUnit> unitSelector, String label) {
        return parseLength(field, unitSelector.getValue())
                .map(Length::toMillimeters)
                .orElseThrow(() -> new IllegalArgumentException(label + " ist keine gültige Koordinate."));
    }

    private HeatingZone defaultHeatingZone(Room room, HydronicHeating heating) {
        return new HeatingZone(
                UUID.randomUUID(),
                "Heizkreis " + (heating.zones().size() + 1),
                defaultHeatingRectangle(room, heating),
                heating.layoutPattern(),
                false
        );
    }

    private List<PlanPoint> defaultHeatingRectangle(Room room, HydronicHeating heating) {
        HeatingZoneBounds roomBounds = heatingZoneBounds(room.outline());
        List<Double> xCoordinates = heatingSplitCoordinates(roomBounds, heating, true);
        List<Double> yCoordinates = heatingSplitCoordinates(roomBounds, heating, false);
        HeatingZoneBounds bestBounds = null;
        double bestArea = 0.0;
        for (int xIndex = 0; xIndex + 1 < xCoordinates.size(); xIndex++) {
            for (int yIndex = 0; yIndex + 1 < yCoordinates.size(); yIndex++) {
                HeatingZoneBounds candidate = new HeatingZoneBounds(
                        xCoordinates.get(xIndex),
                        yCoordinates.get(yIndex),
                        xCoordinates.get(xIndex + 1),
                        yCoordinates.get(yIndex + 1)
                );
                if (candidate.width() <= 0.0 || candidate.height() <= 0.0) {
                    continue;
                }
                PlanPoint center = candidate.center();
                if (!containsHeatingPoint(room.outline(), center) || heating.zones().stream()
                        .anyMatch(zone -> containsHeatingPoint(zone.outline(), center))) {
                    continue;
                }
                double area = candidate.area();
                if (area > bestArea) {
                    bestArea = area;
                    bestBounds = candidate;
                }
            }
        }
        if (bestBounds == null) {
            double inset = Math.min(
                    Math.max(100.0, heating.wallClearance().toMillimeters()),
                    Math.min(roomBounds.width(), roomBounds.height()) / 4.0
            );
            double maxX = roomBounds.minX() + Math.max(100.0, Math.min(2_000.0, roomBounds.width() - inset * 2.0));
            double maxY = roomBounds.minY() + Math.max(100.0, Math.min(2_000.0, roomBounds.height() - inset * 2.0));
            bestBounds = new HeatingZoneBounds(roomBounds.minX() + inset, roomBounds.minY() + inset, maxX, maxY);
        }
        return rectanglePoints(bestBounds);
    }

    private List<Double> heatingSplitCoordinates(HeatingZoneBounds roomBounds, HydronicHeating heating, boolean xAxis) {
        List<Double> coordinates = new ArrayList<>();
        coordinates.add(xAxis ? roomBounds.minX() : roomBounds.minY());
        coordinates.add(xAxis ? roomBounds.maxX() : roomBounds.maxY());
        for (HeatingZone zone : heating.zones()) {
            HeatingZoneBounds bounds = heatingZoneBounds(zone.outline());
            coordinates.add(xAxis ? bounds.minX() : bounds.minY());
            coordinates.add(xAxis ? bounds.maxX() : bounds.maxY());
        }
        coordinates.sort(Double::compareTo);
        List<Double> distinct = new ArrayList<>();
        for (double coordinate : coordinates) {
            if (distinct.isEmpty() || Math.abs(distinct.getLast() - coordinate) > 0.001) {
                distinct.add(coordinate);
            }
        }
        return List.copyOf(distinct);
    }

    private HeatingZoneBounds heatingZoneBounds(List<PlanPoint> points) {
        return new HeatingZoneBounds(
                points.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0)
        );
    }

    private HeatingZoneBounds heatingZoneBounds(PlanSegment segment) {
        return new HeatingZoneBounds(
                Math.min(segment.start().xMillimeters(), segment.end().xMillimeters()),
                Math.min(segment.start().yMillimeters(), segment.end().yMillimeters()),
                Math.max(segment.start().xMillimeters(), segment.end().xMillimeters()),
                Math.max(segment.start().yMillimeters(), segment.end().yMillimeters())
        );
    }

    private List<PlanPoint> rectanglePoints(HeatingZoneBounds bounds) {
        return List.of(
                new PlanPoint(bounds.minX(), bounds.minY()),
                new PlanPoint(bounds.maxX(), bounds.minY()),
                new PlanPoint(bounds.maxX(), bounds.maxY()),
                new PlanPoint(bounds.minX(), bounds.maxY())
        );
    }

    private boolean containsHeatingPoint(List<PlanPoint> polygon, PlanPoint point) {
        boolean inside = false;
        int previousIndex = polygon.size() - 1;
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint current = polygon.get(index);
            PlanPoint previous = polygon.get(previousIndex);
            boolean intersects = (current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters())
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / (previous.yMillimeters() - current.yMillimeters())
                    + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            previousIndex = index;
        }
        return inside;
    }

    private void editHeatingZone(boolean createNewZone) {
        Room room = selectedRoom().orElseThrow(() -> new IllegalStateException("Für Heizbereiche muss ein Raum ausgewählt sein."));
        HydronicHeating heating = selectedHydronicHeating()
                .orElseThrow(() -> new IllegalStateException("Zuerst eine Heizung für die ausgewählte Fläche planen."));
        int selectedIndex = heatingZoneList.getSelectionModel().getSelectedIndex();
        if (!createNewZone && (selectedIndex < 0 || selectedIndex >= heating.zones().size())) {
            throw new IllegalStateException("Zuerst einen Heizbereich in der Liste auswählen.");
        }
        HeatingZone initialZone = createNewZone
                ? defaultHeatingZone(room, heating)
                : heating.zones().get(selectedIndex);
        editHeatingZoneDialog(initialZone).ifPresent(updatedZone -> {
            List<HeatingZone> zones = new ArrayList<>(heating.zones());
            if (createNewZone) {
                zones.add(updatedZone);
            } else {
                zones.set(selectedIndex, updatedZone);
            }
            applyHeatingZones(heating, zones, createNewZone ? zones.size() - 1 : selectedIndex);
        });
    }

    private Optional<HeatingZone> editHeatingZoneDialog(HeatingZone initialZone) {
        Dialog<HeatingZoneInput> dialog = new Dialog<>();
        dialog.setTitle("Heizbereich bearbeiten");
        dialog.setHeaderText("Name, Verlegeart, Rollenorientierung und Eckpunkte in Zentimetern");
        TextField nameField = new TextField(initialZone.name());
        ComboBox<HeatingLayoutPattern> layoutPatternSelector = new ComboBox<>();
        layoutPatternSelector.getItems().setAll(HeatingLayoutPattern.values());
        layoutPatternSelector.setValue(initialZone.layoutPattern());
        CheckBox flowInvertedCheckBox = new CheckBox("Vorlauf und Rücklauf im Heizkreis tauschen");
        flowInvertedCheckBox.setSelected(initialZone.flowInverted());
        TextArea pointArea = new TextArea(initialZone.outline().stream()
                .map(point -> String.format(Locale.GERMAN, "%.3f; %.3f", point.xMillimeters() / 10.0, point.yMillimeters() / 10.0))
                .collect(java.util.stream.Collectors.joining(System.lineSeparator())));
        pointArea.setPrefRowCount(10);
        applyTooltip(nameField, "Legt die eindeutige sichtbare Bezeichnung des Heizkreises fest.");
        applyTooltip(layoutPatternSelector, "Legt nur für diesen Heizkreis fest, ob die Rohre als Meander oder als bifilare Rechteck-Schnecke verlegt werden.");
        applyTooltip(flowInvertedCheckBox, "Dreht die Rollenorientierung dieses Heizkreises um, sodass der bisherige Rücklaufpfad als Vorlaufpfad beginnt und umgekehrt.");
        applyTooltip(pointArea, "Erfasst pro Zeile einen Polygon-Eckpunkt als `X; Y` in Zentimetern. Die Punkte werden umlaufend verbunden und erlauben Rechtecke, L-Formen sowie weitere Polygone.");
        dialog.getDialogPane().setContent(new VBox(
                8.0,
                propertyRow("Name", nameField),
                propertyRow("Verlegung", layoutPatternSelector),
                propertyRow("Rollen", flowInvertedCheckBox),
                propertyRow("Eckpunkte X; Y in cm", pointArea)
        ));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Window owner = currentWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setResultConverter(buttonType -> ButtonType.OK.equals(buttonType)
                ? new HeatingZoneInput(
                nameField.getText(),
                pointArea.getText(),
                layoutPatternSelector.getValue(),
                flowInvertedCheckBox.isSelected()
        )
                : null);
        return dialog.showAndWait().map(input -> new HeatingZone(
                initialZone.id(),
                input.name(),
                parseHeatingZonePoints(input.pointsText()),
                input.layoutPattern(),
                input.flowInverted()
        ));
    }

    private List<PlanPoint> parseHeatingZonePoints(String text) {
        List<PlanPoint> points = new ArrayList<>();
        for (String line : Optional.ofNullable(text).orElse("").lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            String[] coordinates = line.trim().split("\\s*;\\s*", 2);
            if (coordinates.length != 2) {
                throw new IllegalArgumentException("Jeder Eckpunkt benötigt X und Y, getrennt durch Semikolon.");
            }
            try {
                points.add(new PlanPoint(
                        Double.parseDouble(coordinates[0].replace(',', '.')) * 10.0,
                        Double.parseDouble(coordinates[1].replace(',', '.')) * 10.0
                ));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Ungültiger Heizbereichs-Eckpunkt: " + line, exception);
            }
        }
        if (points.size() < 3) {
            throw new IllegalArgumentException("Ein Heizbereich benötigt mindestens drei Eckpunkte.");
        }
        return List.copyOf(points);
    }

    private void removeHeatingZone() {
        HydronicHeating heating = selectedHydronicHeating()
                .orElseThrow(() -> new IllegalStateException("Keine Heizung ausgewählt."));
        int selectedIndex = heatingZoneList.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= heating.zones().size()) {
            throw new IllegalStateException("Zuerst einen Heizbereich in der Liste auswählen.");
        }
        List<HeatingZone> zones = new ArrayList<>(heating.zones());
        zones.remove(selectedIndex);
        applyHeatingZones(heating, zones, Math.min(selectedIndex, zones.size() - 1));
    }

    private boolean removeHeatingZoneById(UUID zoneId) {
        Optional<HeatingZoneContext> context = heatingZoneContext(zoneId);
        if (context.isEmpty()) {
            return false;
        }
        HeatingZoneContext heatingZoneContext = context.orElseThrow();
        List<HeatingZone> zones = new ArrayList<>(heatingZoneContext.heating().zones());
        zones.removeIf(zone -> zone.id().equals(zoneId));
        activeLevel.get().replaceHydronicHeating(heatingZoneContext.heating().withZones(zones));
        return true;
    }

    private void applyHeatingZones(HydronicHeating heating, List<HeatingZone> zones, int selectedIndex) {
        HydronicHeating updatedHeating = heating.withZones(zones);
        Room room = activeLevel.get().rooms().stream()
                .filter(candidate -> candidate.id().equals(updatedHeating.roomId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Kein Raum für die Heizung gefunden."));
        hydronicHeatingLayoutService.validateZoneGeometry(room, updatedHeating);
        rememberStateForUndo();
        activeLevel.get().replaceHydronicHeating(updatedHeating);
        refreshHeatingSection();
        if (selectedIndex >= 0) {
            heatingZoneList.getSelectionModel().select(selectedIndex);
        }
        draftLabel.setText(heatingUpdateMessage(updatedHeating, "Heizbereiche aktualisiert."));
        render();
    }

    private void removeHydronicHeating() {
        HydronicHeating heating = selectedHydronicHeating()
                .orElseThrow(() -> new IllegalStateException("Keine Heizung ausgewählt."));
        rememberStateForUndo();
        activeLevel.get().removeHydronicHeating(heating.id());
        refreshHeatingSection();
        draftLabel.setText("Heizung für " + heating.surfacePosition() + " entfernt.");
        render();
    }

    private record HeatingZoneInput(
            String name,
            String pointsText,
            HeatingLayoutPattern layoutPattern,
            boolean flowInverted
    ) {
    }

    private record HeatingZoneContext(
            Room room,
            HydronicHeating heating,
            HeatingZone zone,
            int zoneIndex
    ) {
    }

    private record HeatingContext(
            Room room,
            HydronicHeating heating
    ) {
    }

    private record HeatingZoneBounds(double minX, double minY, double maxX, double maxY) {
        private double width() {
            return maxX - minX;
        }

        private double height() {
            return maxY - minY;
        }

        private double area() {
            return width() * height();
        }

        private PlanPoint center() {
            return new PlanPoint((minX + maxX) / 2.0, (minY + maxY) / 2.0);
        }
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
        String direction = layer.layoutRotatedQuarterTurn() ? " | 90°" : "";
        return layer.name() + " | " + layer.thickness().format(LengthUnit.MILLIMETER, 1) + " | " + visibility + direction + " | " + tileCount + " Elemente | " + layer.cutRestriction().label() + source;
    }

    private int estimatedTileCount(SurfaceLayer layer) {
        Optional<Room> room = selectedRoom();
        if (room.isEmpty() || currentSurfaceType() == SurfaceType.WALL_INTERIOR || currentSurfaceType() == SurfaceType.WALL_EXTERIOR) {
            return 0;
        }
        TileLayoutRequest request = new TileLayoutRequest(
                Length.ofMillimeters(room.get().widthMillimeters()),
                Length.ofMillimeters(room.get().depthMillimeters()),
                layer.effectiveTileWidth(),
                layer.effectiveTileHeight(),
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
        surfaceRotateLayoutCheckBox.setSelected(selectedLayer.layoutRotatedQuarterTurn());
        surfaceLayoutModeSelector.setValue(selectedLayer.layoutMode());
        syncLengthInput(surfaceLayoutOffsetField, surfaceLayoutOffsetUnit, selectedLayer.layoutOffset(), LengthUnit.CENTIMETER);
        syncLengthInput(surfaceMinimumOffsetField, surfaceMinimumOffsetUnit, selectedLayer.minimumOffset(), LengthUnit.CENTIMETER);
        syncLengthInput(surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit, selectedLayer.minimumEdgeWidth(), LengthUnit.CENTIMETER);
        syncLengthInput(surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit, selectedLayer.minimumStartEndMargin(), LengthUnit.CENTIMETER);
        syncLengthInput(surfaceJointWidthField, surfaceJointWidthUnit, selectedLayer.jointWidth(), LengthUnit.CENTIMETER);
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
                    currentSurfaceCoveringSource(),
                    currentSurfaceLayoutRotatedQuarterTurn()
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
        if (selectedSelections.stream().anyMatch(selection -> selection.kind() == RenderableKind.WALL
                || selection.kind() == RenderableKind.STAIR)) {
            synchronizeRoomsFromWalls(activeLevel.get());
        }
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
        ).withLayoutRotatedQuarterTurn(currentSurfaceLayoutRotatedQuarterTurn());
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

    private boolean currentSurfaceLayoutRotatedQuarterTurn() {
        return surfaceRotateLayoutCheckBox.isSelected();
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

    private Optional<SurfaceLayer> selectedSurfaceLayer() {
        SurfaceLayerStack stack = currentDisplaySurfaceLayerStack().orElse(null);
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        if (stack == null || selectedIndex < 0 || selectedIndex >= stack.layers().size()) {
            return Optional.empty();
        }
        return Optional.of(stack.layers().get(selectedIndex));
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
        Optional<HeatingZoneContext> zoneContext = selectedHeatingZoneContext();
        if (zoneContext.isPresent()) {
            return Optional.of(zoneContext.orElseThrow().room());
        }
        Optional<HeatingContext> heatingContext = selectedHeatingContext();
        if (heatingContext.isPresent()) {
            return Optional.of(heatingContext.orElseThrow().room());
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

    private Optional<HeatingZoneContext> selectedHeatingZoneContext() {
        if (selectedSelection.get() == null || selectedSelection.get().kind() != RenderableKind.HEATING_ZONE) {
            return Optional.empty();
        }
        return heatingZoneContext(UUID.fromString(selectedSelection.get().elementId()));
    }

    private Optional<HeatingContext> selectedHeatingContext() {
        if (selectedSelection.get() == null || selectedSelection.get().kind() != RenderableKind.HEATING_MANIFOLD) {
            return Optional.empty();
        }
        UUID heatingId = UUID.fromString(selectedSelection.get().elementId());
        return activeLevel.get().hydronicHeatings().stream()
                .filter(heating -> heating.id().equals(heatingId))
                .findFirst()
                .flatMap(heating -> activeLevel.get().rooms().stream()
                        .filter(room -> room.id().equals(heating.roomId()))
                        .findFirst()
                        .map(room -> new HeatingContext(room, heating)));
    }

    private Optional<HeatingZoneContext> contextHeatingZoneContext() {
        if (contextMenuSelection == null || contextMenuSelection.kind() != RenderableKind.HEATING_ZONE) {
            return Optional.empty();
        }
        return heatingZoneContext(UUID.fromString(contextMenuSelection.elementId()));
    }

    private Optional<HeatingZoneContext> heatingZoneContext(UUID zoneId) {
        for (HydronicHeating heating : activeLevel.get().hydronicHeatings()) {
            for (int zoneIndex = 0; zoneIndex < heating.zones().size(); zoneIndex++) {
                HeatingZone zone = heating.zones().get(zoneIndex);
                if (zone.id().equals(zoneId)) {
                    int currentZoneIndex = zoneIndex;
                    return activeLevel.get().rooms().stream()
                            .filter(room -> room.id().equals(heating.roomId()))
                            .findFirst()
                            .map(room -> new HeatingZoneContext(room, heating, zone, currentZoneIndex));
                }
            }
        }
        return Optional.empty();
    }

    private List<SurfaceType> availableSurfaceTypesForSelection() {
        if (selectedFloorExtension().isPresent()) {
            return List.of(SurfaceType.FLOOR);
        }
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
        Optional<FloorExtension> floorExtension = selectedFloorExtension();
        if (floorExtension.isPresent()) {
            FloorExtension extension = floorExtension.orElseThrow();
            return Optional.of(new SurfaceSelectionContext(
                    SurfaceType.FLOOR,
                    List.of(extension.surfaceTargetKey()),
                    "Fläche: Oberseite " + extension.type(),
                    "Beläge liegen oberhalb der Fußbodenplatte des ausgewählten Elements."
            ));
        }
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

    private Optional<FloorExtension> selectedFloorExtension() {
        if (selectedSelections.size() != 1 || selectedSelection.get() == null
                || selectedSelection.get().kind() != RenderableKind.FLOOR_EXTENSION) {
            return Optional.empty();
        }
        return activeLevel.get().floorExtensions().stream()
                .filter(extension -> extension.id().toString().equals(selectedSelection.get().elementId()))
                .findFirst();
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
        draftLabel.setText(header + ": " + content);
        if (!interactiveDialogsEnabled) {
            return;
        }
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
        if (interactiveDialogsEnabled) {
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
            boolean bestaetigt = alert.showAndWait()
                    .filter(ButtonType.OK::equals)
                    .isPresent();
            if (!bestaetigt) {
                return;
            }
        }
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
    }

    private void undo() {
        history.undo(captureSnapshot())
                .ifPresentOrElse(snapshot -> {
                    restoreSnapshot(snapshot);
                    draftLabel.setText("Letzte Änderung rückgängig gemacht.");
                }, () -> draftLabel.setText("Kein weiterer Schritt zum Rückgängigmachen vorhanden."));
        updateActionButtons();
    }

    private void redo() {
        history.redo(captureSnapshot())
                .ifPresentOrElse(snapshot -> {
                    restoreSnapshot(snapshot);
                    draftLabel.setText("Änderung wiederhergestellt.");
                }, () -> draftLabel.setText("Kein Schritt zum Wiederherstellen vorhanden."));
        updateActionButtons();
    }

    private void rememberStateForUndo() {
        history.remember(captureSnapshot());
        currentChangeRevision = nextChangeRevision++;
        applicationExitConfirmed = false;
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
                offsetY,
                currentChangeRevision
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
        selectionDragBaseFloorOpenings = List.of();
        selectionDragBaseHeatingExclusionAreas = List.of();
        selectionDragBaseHydronicHeatings = List.of();
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
        currentChangeRevision = snapshot.changeRevision();
        applicationExitConfirmed = false;
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
        selectionDragBaseFloorOpenings = List.of();
        selectionDragBaseHeatingExclusionAreas = List.of();
        selectionDragBaseHydronicHeatings = List.of();
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
                case ROOF_WINDOW -> activeLevel.get().removeRoofWindow(id);
                case STAIR -> removeStaircaseWithUnderbuild(id);
                case ROOM_OBJECT -> activeLevel.get().removeRoomObject(id);
                case FLOOR_EXTENSION -> activeLevel.get().removeFloorExtension(id);
                case FLOOR_OPENING -> activeLevel.get().removeFloorOpening(id);
                case HEATING_ZONE -> removeHeatingZoneById(id);
                case HEATING_EXCLUSION -> activeLevel.get().removeHeatingExclusionArea(id);
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

    private boolean removeStaircaseWithUnderbuild(UUID staircaseId) {
        StairUnderbuildService.UnderbuildResult result = stairUnderbuildService.remove(activeLevel.get(), staircaseId);
        activeLevel.get().replaceWalls(result.walls());
        activeLevel.get().replaceDoors(result.doors());
        activeLevel.get().replaceWindows(result.windows());
        return activeLevel.get().removeStaircase(staircaseId);
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
                menuItem("90°-Korrektur", this::correctSelectedComponentsOrthogonally, null),
                menuItem("Auswahl aufheben", this::clearSelection, null)
        );
        if (selectedSelections.contains(contextMenuSelection)
                && contextMenuRoom().isPresent()
                && contextMenuWorldPoint != null) {
            selectionContextMenu.getItems().addAll(
                    menuItem("Raum umbenennen …", this::renameContextRoom, null),
                    menuItem("HKV hier setzen", this::setHydronicManifoldFromContextLocation, null),
                    menuItem(
                            "Innenansicht ab diesem Standort öffnen",
                            this::openInteriorViewFromContextLocation,
                            null
                    )
            );
        }
        contextHeatingZoneContext().ifPresent(context -> {
            HeatingLayoutPattern targetPattern = context.zone().layoutPattern() == HeatingLayoutPattern.SPIRAL
                    ? HeatingLayoutPattern.MEANDER
                    : HeatingLayoutPattern.SPIRAL;
            selectionContextMenu.getItems().addAll(
                    menuItem("Verlegung auf " + targetPattern + " umschalten", () -> setContextHeatingZonePattern(targetPattern), null),
                    menuItem("Vorlauf/Rücklauf im Heizkreis tauschen", this::invertContextHeatingZone, null),
                    menuItem("Vorlauf hier am Rand setzen", this::setContextHeatingZoneSupplyConnection, null),
                    menuItem("Rücklauf hier am Rand setzen", this::setContextHeatingZoneReturnConnection, null)
            );
            if (mergeableHeatingZone(context).isPresent()) {
                selectionContextMenu.getItems().add(menuItem("Mit angrenzendem Heizkreis verbinden", this::mergeContextHeatingZone, null));
            }
        });
        if (selectedSelections.stream().anyMatch(selection -> selection.kind() != RenderableKind.ROOM_VOLUME
                && selection.kind() != RenderableKind.ROOM_FLOOR
                && selection.kind() != RenderableKind.ROOM_CEILING
                && selection.kind() != RenderableKind.HEATING_MANIFOLD)) {
            selectionContextMenu.getItems().add(menuItem("Auswahl löschen", this::deleteSelection, null));
        }
        if (selectedSelections.stream().anyMatch(this::isRotatableSelection)) {
            selectionContextMenu.getItems().addAll(
                    menuItem("Bauteile 90° im Uhrzeigersinn drehen", this::rotateSelectedComponentsClockwise, null),
                    menuItem("Bauteile 90° gegen den Uhrzeigersinn drehen", this::rotateSelectedComponentsCounterClockwise, null)
            );
        }
        if (selectedWalls().size() == 1) {
            selectionContextMenu.getItems().add(menuItem(
                    "Dachschräge aus Wand erzeugen …",
                    this::createRoofSlopeFromSelectedWall,
                    null
            ));
        }
        if (selectedWalls().size() >= 3) {
            selectionContextMenu.getItems().add(menuItem("Raum erkennen", this::recognizeRoomFromSelectedWalls, null));
        }
    }

    private Optional<Room> contextMenuRoom() {
        if (contextMenuSelection == null
                || contextMenuSelection.kind() != RenderableKind.ROOM_VOLUME
                && contextMenuSelection.kind() != RenderableKind.ROOM_FLOOR
                && contextMenuSelection.kind() != RenderableKind.ROOM_CEILING) {
            return Optional.empty();
        }
        return activeLevel.get().rooms().stream()
                .filter(room -> room.id().toString().equals(contextMenuSelection.elementId()))
                .findFirst();
    }

    private void setContextHeatingZonePattern(HeatingLayoutPattern pattern) {
        HeatingZoneContext context = contextHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Kein Heizkreis ausgewählt."));
        replaceHeatingZone(context, context.zone().withLayoutPattern(pattern), "Verlegung für Heizkreis geändert.");
    }

    private void invertContextHeatingZone() {
        HeatingZoneContext context = contextHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Kein Heizkreis ausgewählt."));
        replaceHeatingZone(context, context.zone().withFlowInverted(!context.zone().flowInverted()), "Vorlauf und Rücklauf im Heizkreis getauscht.");
    }

    private void setContextHeatingZoneSupplyConnection() {
        HeatingZoneContext context = contextHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Kein Heizkreis ausgewählt."));
        replaceHeatingZone(
                context,
                context.zone().withSupplyConnectionPoint(nearestPointOnHeatingZoneBoundary(context.zone(), contextMenuWorldPoint)),
                "Vorlaufanschluss am Heizkreis gesetzt."
        );
    }

    private void setContextHeatingZoneReturnConnection() {
        HeatingZoneContext context = contextHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Kein Heizkreis ausgewählt."));
        replaceHeatingZone(
                context,
                context.zone().withReturnConnectionPoint(nearestPointOnHeatingZoneBoundary(context.zone(), contextMenuWorldPoint)),
                "Rücklaufanschluss am Heizkreis gesetzt."
        );
    }

    private PlanPoint nearestPointOnHeatingZoneBoundary(HeatingZone zone, PlanPoint point) {
        if (point == null) {
            throw new IllegalStateException("Kein Kontextpunkt vorhanden.");
        }
        PlanPoint nearest = zone.outline().getFirst();
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (int index = 0; index < zone.outline().size(); index++) {
            PlanPoint candidate = nearestPointOnSegment(
                    point,
                    zone.outline().get(index),
                    zone.outline().get((index + 1) % zone.outline().size())
            );
            double distance = candidate.distanceTo(point).toMillimeters();
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private PlanPoint nearestPointOnSegment(PlanPoint point, PlanPoint start, PlanPoint end) {
        double dx = end.xMillimeters() - start.xMillimeters();
        double dy = end.yMillimeters() - start.yMillimeters();
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= 0.001) {
            return start;
        }
        double ratio = ((point.xMillimeters() - start.xMillimeters()) * dx
                + (point.yMillimeters() - start.yMillimeters()) * dy) / lengthSquared;
        double clampedRatio = Math.max(0.0, Math.min(1.0, ratio));
        return new PlanPoint(
                start.xMillimeters() + dx * clampedRatio,
                start.yMillimeters() + dy * clampedRatio
        );
    }

    private void replaceHeatingZone(HeatingZoneContext context, HeatingZone replacement, String successPrefix) {
        List<HeatingZone> zones = new ArrayList<>(context.heating().zones());
        zones.set(context.zoneIndex(), replacement);
        HydronicHeating updatedHeating = context.heating().withZones(zones);
        hydronicHeatingLayoutService.validateZoneGeometry(context.room(), updatedHeating);
        rememberStateForUndo();
        activeLevel.get().replaceHydronicHeating(updatedHeating);
        selectSingle(new SelectionKey(RenderableKind.HEATING_ZONE, activeLevel.get().name(), replacement.id().toString()));
        refreshHeatingSection();
        draftLabel.setText(heatingUpdateMessage(updatedHeating, successPrefix));
        render();
    }

    private Optional<HeatingZone> mergeableHeatingZone(HeatingZoneContext context) {
        HeatingZoneBounds bounds = heatingZoneBounds(context.zone().outline());
        return context.heating().zones().stream()
                .filter(zone -> !zone.id().equals(context.zone().id()))
                .filter(zone -> canMerge(bounds, heatingZoneBounds(zone.outline())))
                .findFirst();
    }

    private void mergeContextHeatingZone() {
        HeatingZoneContext context = contextHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Kein Heizkreis ausgewählt."));
        HeatingZone neighbor = mergeableHeatingZone(context)
                .orElseThrow(() -> new IllegalStateException("Kein exakt angrenzender Rechteck-Heizkreis gefunden."));
        HeatingZoneBounds mergedBounds = union(heatingZoneBounds(context.zone().outline()), heatingZoneBounds(neighbor.outline()));
        HeatingZone merged = context.zone().withOutline(rectanglePoints(mergedBounds))
                .withName(context.zone().name() + "+" + neighbor.name());
        List<HeatingZone> zones = context.heating().zones().stream()
                .filter(zone -> !zone.id().equals(neighbor.id()))
                .map(zone -> zone.id().equals(context.zone().id()) ? merged : zone)
                .toList();
        HydronicHeating updatedHeating = context.heating().withZones(zones);
        hydronicHeatingLayoutService.validateZoneGeometry(context.room(), updatedHeating);
        rememberStateForUndo();
        activeLevel.get().replaceHydronicHeating(updatedHeating);
        selectSingle(new SelectionKey(RenderableKind.HEATING_ZONE, activeLevel.get().name(), merged.id().toString()));
        refreshHeatingSection();
        draftLabel.setText(heatingUpdateMessage(updatedHeating, "Angrenzende Heizkreise verbunden."));
        render();
    }

    private boolean canMerge(HeatingZoneBounds first, HeatingZoneBounds second) {
        boolean sameY = sameCoordinate(first.minY(), second.minY()) && sameCoordinate(first.maxY(), second.maxY());
        boolean sameX = sameCoordinate(first.minX(), second.minX()) && sameCoordinate(first.maxX(), second.maxX());
        boolean verticalNeighbor = sameY
                && (sameCoordinate(first.maxX(), second.minX()) || sameCoordinate(second.maxX(), first.minX()));
        boolean horizontalNeighbor = sameX
                && (sameCoordinate(first.maxY(), second.minY()) || sameCoordinate(second.maxY(), first.minY()));
        return verticalNeighbor || horizontalNeighbor;
    }

    private boolean sameCoordinate(double first, double second) {
        return Math.abs(first - second) <= 0.001;
    }

    private HeatingZoneBounds union(HeatingZoneBounds first, HeatingZoneBounds second) {
        return new HeatingZoneBounds(
                Math.min(first.minX(), second.minX()),
                Math.min(first.minY(), second.minY()),
                Math.max(first.maxX(), second.maxX()),
                Math.max(first.maxY(), second.maxY())
        );
    }

    private void createRoofSlopeFromSelectedWall() {
        List<Wall> walls = selectedWalls();
        if (walls.size() != 1 || !interactiveDialogsEnabled) {
            return;
        }
        Length currentKneeHeight = parseLength(this.kneeWallHeightField, kneeWallHeightUnit.getValue())
                .orElse(Length.of(1.0, LengthUnit.METER));
        TextField kneeHeightField = new TextField(formatValue(currentKneeHeight, LengthUnit.CENTIMETER, LENGTH_INPUT_DECIMALS));
        TextField slopeWidthField = new TextField("120");
        kneeHeightField.setPrefColumnCount(8);
        slopeWidthField.setPrefColumnCount(8);
        applyTooltip(kneeHeightField, "Legt die Sockel- beziehungsweise Kniestockhöhe an der Innenkante der ausgewählten Wand in Zentimetern fest.");
        applyTooltip(slopeWidthField, "Legt die horizontale Breite unterhalb der Dachschräge ab der Wandinnenkante in Zentimetern fest.");
        VBox content = new VBox(
                10.0,
                new HBox(10.0, new Label("Sockelhöhe"), kneeHeightField, new Label("cm")),
                new HBox(10.0, new Label("Breite unter Schräge"), slopeWidthField, new Label("cm"))
        );
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Dachschräge aus Wand erzeugen");
        dialog.setHeaderText("Dachschräge von der Wandinnenkante in den angrenzenden Raum aufbauen");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(580);
        Window owner = currentWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        applyTooltip(dialog.getDialogPane().lookupButton(ButtonType.OK), "Erzeugt oder ersetzt die Dachschräge an dieser Raumseite, passt die Eckhöhen an und teilt beide Seitenwände an der oberen Schrägenkante. Weitere Raumseiten können zusätzliche Dachschrägen erhalten.");
        applyTooltip(dialog.getDialogPane().lookupButton(ButtonType.CANCEL), "Schließt den Dialog, ohne Wände oder Raumdecke zu ändern.");
        if (dialog.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }
        Length kneeHeight = parseLength(kneeHeightField, LengthUnit.CENTIMETER)
                .orElseThrow(() -> new IllegalArgumentException("Die Sockelhöhe ist ungültig."));
        Length slopeWidth = parseLength(slopeWidthField, LengthUnit.CENTIMETER)
                .orElseThrow(() -> new IllegalArgumentException("Die Breite unterhalb der Dachschräge ist ungültig."));
        RoofSlopeWallService.RoofSlopeResult result = roofSlopeWallService.apply(
                activeLevel.get(), walls.getFirst().id(), kneeHeight, slopeWidth
        );
        rememberStateForUndo();
        activeLevel.get().replaceWalls(result.walls());
        activeLevel.get().replaceRooms(result.rooms());
        activeLevel.get().replaceDoors(result.doors());
        activeLevel.get().replaceWindows(result.windows());
        activeLevel.get().replaceSurfaceLayerStacks(result.surfaceLayerStacks());
        markThreeDDirty();
        draftLabel.setText("Dachschräge aus Wandinnenkante erzeugt.");
        render();
    }

    private void openInteriorViewFromContextLocation() {
        Optional<Room> room = contextMenuRoom();
        if (room.isEmpty() || contextMenuWorldPoint == null) {
            draftLabel.setText("Innenansicht braucht einen Raum und einen Standort.");
            return;
        }
        threeDViewport.activateInteriorView(project, activeLevel.get(), room.orElseThrow(), contextMenuWorldPoint);
        activeWorkspaceMode.set(WorkspaceMode.INTERIOR);
        draftLabel.setText("Innenansicht am gewählten Raumstandort geöffnet.");
    }

    private void setHydronicManifoldFromContextLocation() {
        Room room = contextMenuRoom().orElseThrow(() -> new IllegalStateException("Kein Raum im Kontextmenü ausgewählt."));
        if (contextMenuWorldPoint == null) {
            throw new IllegalStateException("Kein Standort im Kontextmenü vorhanden.");
        }
        PlanPoint supplyPoint = contextMenuWorldPoint;
        PlanPoint returnPoint = new PlanPoint(
                contextMenuWorldPoint.xMillimeters() + DEFAULT_HKV_PAIR_DISTANCE_MILLIMETERS,
                contextMenuWorldPoint.yMillimeters()
        );
        setLengthInput(heatingSupplyXField, heatingSupplyXUnit, Length.ofMillimeters(supplyPoint.xMillimeters()), LengthUnit.CENTIMETER);
        setLengthInput(heatingSupplyYField, heatingSupplyYUnit, Length.ofMillimeters(supplyPoint.yMillimeters()), LengthUnit.CENTIMETER);
        setLengthInput(heatingReturnXField, heatingReturnXUnit, Length.ofMillimeters(returnPoint.xMillimeters()), LengthUnit.CENTIMETER);
        setLengthInput(heatingReturnYField, heatingReturnYUnit, Length.ofMillimeters(returnPoint.yMillimeters()), LengthUnit.CENTIMETER);

        HydronicHeating existing = activeLevel.get().findHydronicHeating(
                room.id(),
                Optional.ofNullable(heatingSurfacePositionSelector.getValue()).orElse(HeatingSurfacePosition.FLOOR)
        );
        if (existing == null) {
            draftLabel.setText("HKV-Position gesetzt. Heizkreise können jetzt mit diesem Vor- und Rücklauf geplant werden.");
            return;
        }
        HydronicHeating updated = withHydronicManifold(existing, supplyPoint, returnPoint);
        HydronicHeatingLayoutService.ValidationReport report = hydronicHeatingLayoutService.validateLayout(updated);
        boolean maximumExceeded = report.valid() && hydronicHeatingLayoutService.layout(updated).stream()
                .anyMatch(circuit -> circuit.pipeLength().compareTo(updated.maximumPipeLength()) > 0);
        if (!report.valid() || maximumExceeded) {
            draftLabel.setText("HKV-Felder gesetzt. Die vorhandene Planung bleibt unverändert; bitte Heizkreise neu planen.");
            return;
        }
        rememberStateForUndo();
        activeLevel.get().replaceHydronicHeating(updated);
        refreshHeatingSection();
        draftLabel.setText("HKV-Position gesetzt und vorhandene Heizkreise neu angebunden.");
        render();
    }

    private HydronicHeating withHydronicManifold(HydronicHeating heating, PlanPoint supplyPoint, PlanPoint returnPoint) {
        return heating.withManifold(supplyPoint, returnPoint);
    }

    private void renameContextRoom() {
        Room room = contextMenuRoom().orElseThrow(() -> new IllegalStateException("Kein Raum im Kontextmenü ausgewählt."));
        if (!interactiveDialogsEnabled) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog(room.name());
        dialog.setTitle("Raum umbenennen");
        dialog.setHeaderText("Neuen Namen für den Raum eingeben");
        dialog.setContentText("Raumname:");
        dialog.getDialogPane().setPrefWidth(460);
        Window owner = currentWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        applyTooltip(dialog.getEditor(), "Legt ausschließlich den Namen des im Kontextmenü gewählten Raums fest; Maße und weitere Eigenschaften bleiben unverändert.");
        applyTooltip(dialog.getDialogPane().lookupButton(ButtonType.OK), "Übernimmt den neuen Namen nur für den gewählten Raum.");
        applyTooltip(dialog.getDialogPane().lookupButton(ButtonType.CANCEL), "Schließt den Dialog, ohne den Raumnamen zu ändern.");
        dialog.showAndWait().ifPresent(name -> renameRoom(room.id(), name));
    }

    private void renameRoom(UUID roomId, String newName) {
        Room currentRoom = activeLevel.get().rooms().stream()
                .filter(room -> room.id().equals(roomId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Raum nicht gefunden: " + roomId));
        Room renamedRoom = currentRoom.withName(newName);
        if (renamedRoom.name().equals(currentRoom.name())) {
            return;
        }
        rememberStateForUndo();
        activeLevel.get().replaceRooms(activeLevel.get().rooms().stream()
                .map(room -> room.id().equals(roomId) ? renamedRoom : room)
                .toList());
        roomNameField.setText(renamedRoom.name());
        markThreeDDirty();
        draftLabel.setText("Raum umbenannt: " + renamedRoom.name());
        render();
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
                        syncLengthInput(wallHeightField, wallHeightUnit, wall.height(), LengthUnit.CENTIMETER);
                    });
            case ROOM_VOLUME, ROOM_FLOOR, ROOM_CEILING -> activeLevel.get().rooms().stream()
                    .filter(room -> room.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(room -> {
                        roomNameField.setText(room.name());
                        syncLengthInput(roomHeightField, roomHeightUnit, room.roomHeight(), LengthUnit.CENTIMETER);
                        syncLengthInput(floorThicknessField, floorThicknessUnit, room.floorThickness(), LengthUnit.CENTIMETER);
                        syncLengthInput(ceilingThicknessField, ceilingThicknessUnit, room.ceilingThickness(), LengthUnit.CENTIMETER);
                        roofSlopeManagementLabel.setText(room.slopedCeilingProfiles().size() + " Dachschräge(n)");
                        if (room.slopedCeilingProfile().isPresent()) {
                            SlopedCeilingProfile profile = room.slopedCeilingProfile().orElseThrow();
                            syncLengthInput(kneeWallHeightField, kneeWallHeightUnit, profile.kneeWallHeight(), LengthUnit.CENTIMETER);
                        }
                    });
            case DOOR -> activeLevel.get().doors().stream()
                    .filter(door -> door.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(door -> {
                        syncLengthInput(doorWidthField, doorWidthUnit, door.width(), LengthUnit.CENTIMETER);
                        syncLengthInput(doorHeightField, doorHeightUnit, door.height(), LengthUnit.CENTIMETER);
                        syncLengthInput(thresholdField, thresholdUnit, door.thresholdHeight(), LengthUnit.CENTIMETER);
                    });
            case WINDOW -> activeLevel.get().windows().stream()
                    .filter(window -> window.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(window -> {
                        syncLengthInput(windowWidthField, windowWidthUnit, window.width(), LengthUnit.CENTIMETER);
                        syncLengthInput(windowHeightField, windowHeightUnit, window.windowHeight(), LengthUnit.CENTIMETER);
                        syncLengthInput(sillHeightField, sillHeightUnit, window.sillHeight(), LengthUnit.CENTIMETER);
                    });
            case ROOF_WINDOW -> activeLevel.get().roofWindows().stream()
                    .filter(roofWindow -> roofWindow.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(roofWindow -> {
                        syncLengthInput(windowWidthField, windowWidthUnit, roofWindow.width(), LengthUnit.CENTIMETER);
                        syncLengthInput(windowHeightField, windowHeightUnit, roofWindow.depth(), LengthUnit.CENTIMETER);
                    });
            case STAIR -> activeLevel.get().staircases().stream()
                    .filter(stair -> stair.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(stair -> {
                        syncLengthInput(stairHeightField, stairHeightUnit, stair.totalHeight(), LengthUnit.CENTIMETER);
                        stairStepsField.setText(Integer.toString(stair.stepCount()));
                        syncLengthInput(stairStartLandingField, stairStartLandingUnit, stair.startLandingWidth(), LengthUnit.CENTIMETER);
                        syncLengthInput(stairEndLandingField, stairEndLandingUnit, stair.endLandingWidth(), LengthUnit.CENTIMETER);
                        syncLengthInput(stairLeftUnderbuildField, stairLeftUnderbuildUnit, stair.leftUnderbuildWidth(), LengthUnit.CENTIMETER);
                        syncLengthInput(stairRightUnderbuildField, stairRightUnderbuildUnit, stair.rightUnderbuildWidth(), LengthUnit.CENTIMETER);
                        syncLengthInput(stairUndersideThicknessField, stairUndersideThicknessUnit, stair.undersideThickness(), LengthUnit.CENTIMETER);
                    });
            case ROOM_OBJECT -> activeLevel.get().roomObjects().stream()
                    .filter(roomObject -> roomObject.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(roomObject -> {
                        availableRoomObjectPresets.stream()
                                .filter(preset -> preset.id().equals(roomObject.presetId()))
                                .findFirst()
                                .ifPresent(roomObjectPresetSelector::setValue);
                        roomObjectNameField.setText(roomObject.name());
                        syncLengthInput(roomObjectWidthField, roomObjectWidthUnit, roomObject.width(), LengthUnit.CENTIMETER);
                        syncLengthInput(roomObjectDepthField, roomObjectDepthUnit, roomObject.depth(), LengthUnit.CENTIMETER);
                        syncLengthInput(roomObjectHeightField, roomObjectHeightUnit, roomObject.height(), LengthUnit.CENTIMETER);
                        syncLengthInput(roomObjectBaseElevationField, roomObjectBaseElevationUnit, roomObject.baseElevation(), LengthUnit.CENTIMETER);
                        roomObjectAngleField.setText(String.format(Locale.GERMAN, "%.2f", roomObject.rotationDegrees()));
                    });
            case FLOOR_EXTENSION -> activeLevel.get().floorExtensions().stream()
                    .filter(extension -> extension.id().toString().equals(selectedSelection.get().elementId()))
                    .findFirst()
                    .ifPresent(extension -> {
                        floorExtensionTypeSelector.setValue(extension.type());
                        floorExtensionPlacementSelector.setValue(extension.placement());
                        syncLengthInput(floorExtensionThicknessField, floorExtensionThicknessUnit, extension.slabThickness(), LengthUnit.CENTIMETER);
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
                            ? Room.withSlopedCeilings(room.id(), currentRoomName(), room.outline(), currentRoomHeight(), currentFloorThickness(), currentCeilingThickness(),
                            room.slopedCeilingProfiles(), room.ceilingVertexHeights())
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
            case ROOF_WINDOW -> activeLevel.get().replaceRoofWindows(activeLevel.get().roofWindows().stream()
                    .map(roofWindow -> selectedIds().contains(roofWindow.id().toString())
                            ? new RoofWindow(roofWindow.id(), roofWindow.roomId(), roofWindow.center(),
                            currentWindowWidth(), currentWindowHeight(), roofWindow.slopeSide())
                            : roofWindow)
                    .toList());
            case STAIR -> {
                List<Staircase> updatedStaircases = activeLevel.get().staircases().stream()
                        .map(stair -> selectedIds().contains(stair.id().toString())
                                ? new Staircase(
                                stair.id(), stair.stairType(), stair.firstCorner(), stair.oppositeCorner(),
                                currentStairHeight(), currentStairSteps(), stair.rotationQuarterTurns(),
                                currentStairStartLanding(), currentStairEndLanding(), currentStairLeftUnderbuild(),
                                currentStairRightUnderbuild(), currentStairUndersideThickness()
                        )
                                : stair)
                        .toList();
                activeLevel.get().replaceStaircases(updatedStaircases);
                updatedStaircases.stream()
                        .filter(stair -> selectedIds().contains(stair.id().toString()))
                        .forEach(this::synchronizeStairUnderbuild);
            }
            case ROOM_OBJECT -> activeLevel.get().replaceRoomObjects(activeLevel.get().roomObjects().stream()
                    .map(roomObject -> selectedIds().contains(roomObject.id().toString())
                            ? new RoomObject(
                            roomObject.id(),
                            roomObject.presetId(),
                            currentRoomObjectName(roomObjectPresetSelector.getValue()),
                            roomObject.type(),
                            roomObject.shape(),
                            roomObject.center(),
                            positiveLength(roomObjectWidthField, roomObjectWidthUnit, roomObject.width()),
                            positiveLength(roomObjectDepthField, roomObjectDepthUnit, roomObject.depth()),
                            positiveLength(roomObjectHeightField, roomObjectHeightUnit, roomObject.height()),
                            currentRoomObjectAngleDegrees(),
                            roomObject.mountingMode(),
                            roomObject.visible(),
                            roomObject.source(),
                            currentRoomObjectBaseElevation()
                    )
                            : roomObject)
                    .toList());
            case FLOOR_EXTENSION -> activeLevel.get().replaceFloorExtensions(activeLevel.get().floorExtensions().stream()
                    .map(extension -> selectedIds().contains(extension.id().toString())
                            ? new FloorExtension(extension.id(),
                            Optional.ofNullable(floorExtensionTypeSelector.getValue()).orElse(extension.type()),
                            Optional.ofNullable(floorExtensionPlacementSelector.getValue()).orElse(extension.placement()),
                            extension.firstCorner(), extension.oppositeCorner(), currentFloorExtensionThickness())
                            : extension)
                    .toList());
            default -> {
            }
        }
        if (selectedSelections.stream().anyMatch(selection -> selection.kind() == RenderableKind.WALL
                || selection.kind() == RenderableKind.STAIR)) {
            synchronizeRoomsFromWalls(activeLevel.get());
        }
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
        selectedEndpointHeight().ifPresent(height -> syncLengthInput(endpointHeightField, endpointHeightUnit, height, LengthUnit.CENTIMETER));
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
        activeLevel.get().replaceRoomObjects(rotationResult.roomObjects());
        rotationResult.staircases().stream()
                .filter(staircase -> selectedIds().contains(staircase.id().toString()))
                .forEach(this::synchronizeStairUnderbuild);
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

    private void synchronizeStairUnderbuild(Staircase staircase) {
        StairUnderbuildService.UnderbuildResult result = stairUnderbuildService.synchronize(activeLevel.get(), staircase);
        activeLevel.get().replaceWalls(result.walls());
        activeLevel.get().replaceDoors(result.doors());
        activeLevel.get().replaceWindows(result.windows());
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
        selectionDragBaseFloorOpenings = List.copyOf(activeLevel.get().floorOpenings());
        selectionDragBaseHeatingExclusionAreas = List.copyOf(activeLevel.get().heatingExclusionAreas());
        selectionDragBaseHydronicHeatings = List.copyOf(activeLevel.get().hydronicHeatings());
        draftLabel.setText("Ausgewählte Wände, Treppen, Objekte oder rechteckige Flächen können jetzt parallel verschoben werden.");
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
        dragLevel.replaceFloorOpenings(selectionDragBaseFloorOpenings);
        dragLevel.replaceHeatingExclusionAreas(selectionDragBaseHeatingExclusionAreas);
        dragLevel.replaceHydronicHeatings(selectionDragBaseHydronicHeatings);
        SelectionTranslationService.TranslationResult translationResult = selectionTranslationService.translate(dragLevel, Set.copyOf(selectedSelections), deltaX, deltaY);
        if (!translationResult.changed()) {
            return;
        }
        activeLevel.get().replaceWalls(translationResult.walls());
        activeLevel.get().replaceStaircases(translationResult.staircases());
        activeLevel.get().replaceRoomObjects(translationResult.roomObjects());
        activeLevel.get().replaceFloorOpenings(translationResult.floorOpenings());
        activeLevel.get().replaceHeatingExclusionAreas(translationResult.heatingExclusionAreas());
        activeLevel.get().replaceHydronicHeatings(translationResult.hydronicHeatings());
        synchronizeRoomsFromWalls(activeLevel.get());
        markThreeDDirty();
    }

    private boolean moveSelectionWithArrowKey(KeyCode keyCode) {
        if (!drawingCanvas.isFocused()
                || activeWorkspaceMode.get() != WorkspaceMode.TWO_D
                || !isDirectEditingView()
                || currentTool() != DrawingTool.EDIT
                || selectedSelections.stream().noneMatch(this::isTranslatableSelection)) {
            return false;
        }
        return moveSelectionByArrowKey(keyCode);
    }

    private boolean moveSelectionByArrowKey(KeyCode keyCode) {
        double spacing = currentGrid().spacing().toMillimeters();
        double deltaX = switch (keyCode) {
            case LEFT -> -spacing;
            case RIGHT -> spacing;
            default -> 0.0;
        };
        double deltaY = switch (keyCode) {
            case UP -> -spacing;
            case DOWN -> spacing;
            default -> 0.0;
        };
        if (deltaX == 0.0 && deltaY == 0.0) {
            return false;
        }
        moveSelectedComponents(deltaX, deltaY);
        return true;
    }

    private void moveSelectedComponents(double deltaX, double deltaY) {
        SelectionTranslationService.TranslationResult result = selectionTranslationService.translate(
                activeLevel.get(), Set.copyOf(selectedSelections), deltaX, deltaY
        );
        if (!result.changed()) {
            return;
        }
        rememberStateForUndo();
        activeLevel.get().replaceWalls(result.walls());
        activeLevel.get().replaceStaircases(result.staircases());
        activeLevel.get().replaceRoomObjects(result.roomObjects());
        activeLevel.get().replaceFloorOpenings(result.floorOpenings());
        activeLevel.get().replaceHeatingExclusionAreas(result.heatingExclusionAreas());
        activeLevel.get().replaceHydronicHeatings(result.hydronicHeatings());
        synchronizeRoomsFromWalls(activeLevel.get());
        markThreeDDirty();
        draftLabel.setText("Auswahl um eine Rasterweite verschoben.");
        render();
    }

    private void correctSelectedComponentsOrthogonally() {
        OrthogonalCorrectionService.CorrectionResult result = orthogonalCorrectionService.correct(
                activeLevel.get(), Set.copyOf(selectedSelections), currentGrid(), 10.0
        );
        if (!result.changed()) {
            draftLabel.setText("Keine Abweichung bis 10° zur 90°-Ausrichtung gefunden.");
            return;
        }
        rememberStateForUndo();
        activeLevel.get().replaceWalls(result.walls());
        activeLevel.get().replaceRoomObjects(result.roomObjects());
        synchronizeRoomsFromWalls(activeLevel.get());
        markThreeDDirty();
        draftLabel.setText("Auswahl auf 90° korrigiert und am Raster ausgerichtet.");
        render();
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

    public void automationAddRoom(Room room) {
        activeLevel.get().addRoom(room);
        markThreeDDirty();
        render();
    }

    public Room automationRoom(int index) {
        return activeLevel.get().rooms().get(index);
    }

    public void automationPlanHydronicHeating(String surfacePosition, String layoutPattern) {
        heatingSurfacePositionSelector.setValue(HeatingSurfacePosition.valueOf(surfacePosition.trim().toUpperCase(Locale.ROOT)));
        heatingLayoutPatternSelector.setValue(HeatingLayoutPattern.valueOf(layoutPattern.trim().toUpperCase(Locale.ROOT)));
        planHydronicHeatingAutomatically();
    }

    public HydronicHeating automationHydronicHeating(int index) {
        return activeLevel.get().hydronicHeatings().get(index);
    }

    public int automationHydronicHeatingCount() {
        return activeLevel.get().hydronicHeatings().size();
    }

    public void automationReplaceHeatingZone(int heatingIndex, int zoneIndex, String name, List<PlanPoint> outline) {
        HydronicHeating heating = activeLevel.get().hydronicHeatings().get(heatingIndex);
        List<HeatingZone> zones = new ArrayList<>(heating.zones());
        HeatingZone previous = zones.get(zoneIndex);
        zones.set(zoneIndex, new HeatingZone(previous.id(), name, outline, previous.layoutPattern(), previous.flowInverted()));
        applyHeatingZones(heating, zones, zoneIndex);
    }

    public void automationReplaceHeatingZone(
            int heatingIndex,
            int zoneIndex,
            String name,
            List<PlanPoint> outline,
            String layoutPattern,
            boolean flowInverted
    ) {
        HydronicHeating heating = activeLevel.get().hydronicHeatings().get(heatingIndex);
        List<HeatingZone> zones = new ArrayList<>(heating.zones());
        HeatingZone previous = zones.get(zoneIndex);
        zones.set(zoneIndex, new HeatingZone(
                previous.id(),
                name,
                outline,
                HeatingLayoutPattern.valueOf(layoutPattern.trim().toUpperCase(Locale.ROOT)),
                flowInverted
        ));
        applyHeatingZones(heating, zones, zoneIndex);
    }

    public void automationRemoveHeatingZone(int heatingIndex, int zoneIndex) {
        HydronicHeating heating = activeLevel.get().hydronicHeatings().get(heatingIndex);
        List<HeatingZone> zones = new ArrayList<>(heating.zones());
        zones.remove(zoneIndex);
        applyHeatingZones(heating, zones, Math.min(zoneIndex, zones.size() - 1));
    }

    public void automationAddHeatingZone(
            int heatingIndex,
            String name,
            List<PlanPoint> outline,
            String layoutPattern,
            boolean flowInverted
    ) {
        HydronicHeating heating = activeLevel.get().hydronicHeatings().get(heatingIndex);
        List<HeatingZone> zones = new ArrayList<>(heating.zones());
        zones.add(new HeatingZone(
                UUID.randomUUID(),
                name,
                outline,
                HeatingLayoutPattern.valueOf(layoutPattern.trim().toUpperCase(Locale.ROOT)),
                flowInverted
        ));
        applyHeatingZones(heating, zones, zones.size() - 1);
    }

    public void automationAddDefaultHeatingZone(int heatingIndex) {
        HydronicHeating heating = activeLevel.get().hydronicHeatings().get(heatingIndex);
        Room room = activeLevel.get().rooms().stream()
                .filter(candidate -> candidate.id().equals(heating.roomId()))
                .findFirst()
                .orElseThrow();
        List<HeatingZone> zones = new ArrayList<>(heating.zones());
        zones.add(defaultHeatingZone(room, heating));
        applyHeatingZones(heating, zones, zones.size() - 1);
    }

    public void automationPrepareSelectionContextMenu(double screenX, double screenY) {
        contextMenuWorldPoint = screenToWorld(screenX, screenY);
        contextMenuSelection = selectionQueryService.findSelection(
                activeLevel.get(),
                contextMenuWorldPoint,
                SNAP_TOLERANCE
        ).orElse(null);
        if (contextMenuSelection != null) {
            selectSingle(contextMenuSelection);
        } else {
            rebuildSelectionContextMenu();
        }
    }

    public List<String> automationSelectionContextMenuItems() {
        return selectionContextMenu.getItems().stream().map(MenuItem::getText).toList();
    }

    public void automationInvokeSelectionContextMenuItem(String label) {
        selectionContextMenu.getItems().stream()
                .filter(item -> label.equals(item.getText()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter Kontextmenüeintrag: " + label))
                .fire();
    }

    public void automationRenameContextRoom(String name) {
        Room room = contextMenuRoom().orElseThrow(() -> new IllegalStateException("Kein Raum im Kontextmenü ausgewählt."));
        renameRoom(room.id(), name);
    }

    public PlanPoint automationInteriorEyePosition() {
        return threeDViewport.automationInteriorEyePosition();
    }

    public WritableImage automationDrawingSnapshot() {
        ensureCanvasReady();
        return drawingCanvas.snapshot(null, null);
    }

    public void automationRememberUndoState() {
        history.remember(captureSnapshot());
        updateActionButtons();
    }

    public void automationSetTool(String toolName) {
        String normalizedToolName = toolName.trim().toUpperCase(Locale.ROOT);
        if ("ROOM".equals(normalizedToolName)) {
            toolSelector.setValue(DrawingTool.EDIT);
            return;
        }
        toolSelector.setValue(DrawingTool.valueOf(normalizedToolName));
    }

    public void automationSelectRoomObjectPreset(String presetId) {
        availableRoomObjectPresets.stream()
                .filter(preset -> preset.id().equals(presetId))
                .findFirst()
                .ifPresentOrElse(roomObjectPresetSelector::setValue, () -> {
                    throw new IllegalArgumentException("Objekt-Preset `" + presetId + "` ist unbekannt.");
                });
    }

    public void automationSetShowDimensions(boolean visible) {
        showDimensions.set(visible);
    }

    public int automationFloorExtensionCount() {
        return activeLevel.get().floorExtensions().size();
    }

    public int automationFloorOpeningCount() {
        return activeLevel.get().floorOpenings().size();
    }

    public int automationHeatingExclusionAreaCount() {
        return activeLevel.get().heatingExclusionAreas().size();
    }

    public int automationRoofWindowCount() {
        return activeLevel.get().roofWindows().size();
    }

    public void automationPlaceRoofWindow(double worldXMillimeters, double worldYMillimeters) {
        placeRoofWindow(new PlanPoint(worldXMillimeters, worldYMillimeters));
        render();
    }

    public FloorOpening automationFloorOpening(int index) {
        return activeLevel.get().floorOpenings().get(index);
    }

    public HeatingExclusionArea automationHeatingExclusionArea(int index) {
        return activeLevel.get().heatingExclusionAreas().get(index);
    }

    public int automationRoomObjectCount() {
        return activeLevel.get().roomObjects().size();
    }

    public RoomObject automationRoomObject(int index) {
        return activeLevel.get().roomObjects().get(index);
    }

    public Wall automationWall(int index) {
        return activeLevel.get().walls().get(index);
    }

    public boolean automationMoveSelectionWithArrowKey(KeyCode keyCode) {
        return moveSelectionByArrowKey(keyCode);
    }

    public FloorExtension automationFloorExtension(int index) {
        return activeLevel.get().floorExtensions().get(index);
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
            case "FLOOR_EXTENSION", "BALCONY", "GALLERY" -> activeLevel.get().floorExtensions().stream()
                    .skip(index)
                    .findFirst()
                    .map(extension -> new SelectionKey(RenderableKind.FLOOR_EXTENSION, activeLevel.get().name(), extension.id().toString()))
                    .orElseThrow(() -> new IllegalArgumentException("Balkon-/Emporenindex `" + index + "` ist ungültig."));
            case "FLOOR_OPENING" -> activeLevel.get().floorOpenings().stream()
                    .skip(index)
                    .findFirst()
                    .map(opening -> new SelectionKey(RenderableKind.FLOOR_OPENING, activeLevel.get().name(), opening.id().toString()))
                    .orElseThrow(() -> new IllegalArgumentException("Bodenöffnungsindex `" + index + "` ist ungültig."));
            case "HEATING_EXCLUSION", "HEATING_EXCLUSION_AREA" -> activeLevel.get().heatingExclusionAreas().stream()
                    .skip(index)
                    .findFirst()
                    .map(area -> new SelectionKey(RenderableKind.HEATING_EXCLUSION, activeLevel.get().name(), area.id().toString()))
                    .orElseThrow(() -> new IllegalArgumentException("FBH-Sperrflächenindex `" + index + "` ist ungültig."));
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
        drawingCanvas.fireEvent(mouseEvent(MouseEvent.MOUSE_PRESSED, fromX, fromY, button, shiftDown, shortcutDown, altDown, true));
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

    // Erkennt Automatisierungs- bzw. Testumgebungen, in denen blockierende Dialoge vermieden werden müssen.
    static boolean automationActive() {
        return Boolean.parseBoolean(System.getProperty("cadas.automation.enabled", "false"))
                || "1".equals(System.getenv("CADAS_AUTOMATION"));
    }

    public void automationSetErrorDialogsEnabled(boolean enabled) {
        interactiveDialogsEnabled = enabled;
    }

    public void automationClearLastError() {
        lastErrorDialog = UiErrorDialogs.ErrorPresentation.empty();
    }

    public String automationLastErrorTitle() {
        return lastErrorDialog.title();
    }

    public String automationLastErrorHeader() {
        return lastErrorDialog.header();
    }

    public String automationLastErrorContent() {
        return lastErrorDialog.content();
    }

    public String automationLastErrorStackTrace() {
        return lastErrorDialog.stackTrace();
    }

    public void automationDisableApplicationExit() {
        applicationExitAction = () -> {
        };
        applicationExitRequested = false;
    }

    public boolean automationExitRequested() {
        return applicationExitRequested;
    }

    public boolean automationHasUnsavedChanges() {
        return hasUnsavedChanges();
    }

    public void automationSetUnsavedChangesExitDecision(boolean exitWithoutSaving) {
        automatedUnsavedChangesExitDecision = exitWithoutSaving;
    }

    public void automationTriggerShortcutOnField(String fieldName, KeyCode keyCode, boolean shortcutDown, boolean shiftDown) {
        Event.fireEvent(textFieldByName(fieldName), shortcutEvent(keyCode, shortcutDown, shiftDown));
    }

    private KeyEvent shortcutEvent(KeyCode keyCode, boolean shortcutDown, boolean shiftDown) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, shiftDown, shortcutDown, false, shortcutDown);
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
                threeDViewport.setProjectionMode(de.schrell.cadas.application.view.ProjectionMode.valueOf(mode));
            }
            case "exit" -> requestApplicationExit();
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
            case "stairLeftUnderbuild" -> stairLeftUnderbuildField;
            case "stairRightUnderbuild" -> stairRightUnderbuildField;
            case "stairUndersideThickness" -> stairUndersideThicknessField;
            case "roomObjectName" -> roomObjectNameField;
            case "roomObjectWidth" -> roomObjectWidthField;
            case "roomObjectDepth" -> roomObjectDepthField;
            case "roomObjectHeight" -> roomObjectHeightField;
            case "roomObjectBaseElevation" -> roomObjectBaseElevationField;
            case "roomObjectAngle" -> roomObjectAngleField;
            case "floorExtensionThickness" -> floorExtensionThicknessField;
            case "heatingPipeSpacing" -> heatingPipeSpacingField;
            case "heatingPipeDiameter" -> heatingPipeDiameterField;
            case "heatingMaximumPipeLength" -> heatingMaximumPipeLengthField;
            case "heatingWallClearance" -> heatingWallClearanceField;
            case "heatingSupplyX" -> heatingSupplyXField;
            case "heatingSupplyY" -> heatingSupplyYField;
            case "heatingReturnX" -> heatingReturnXField;
            case "heatingReturnY" -> heatingReturnYField;
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
            case "stairLeftUnderbuild" -> stairLeftUnderbuildUnit;
            case "stairRightUnderbuild" -> stairRightUnderbuildUnit;
            case "stairUndersideThickness" -> stairUndersideThicknessUnit;
            case "roomObjectWidth" -> roomObjectWidthUnit;
            case "roomObjectDepth" -> roomObjectDepthUnit;
            case "roomObjectHeight" -> roomObjectHeightUnit;
            case "roomObjectBaseElevation" -> roomObjectBaseElevationUnit;
            case "floorExtensionThickness" -> floorExtensionThicknessUnit;
            case "heatingPipeSpacing" -> heatingPipeSpacingUnit;
            case "heatingPipeDiameter" -> heatingPipeDiameterUnit;
            case "heatingMaximumPipeLength" -> heatingMaximumPipeLengthUnit;
            case "heatingWallClearance" -> heatingWallClearanceUnit;
            case "heatingSupplyX" -> heatingSupplyXUnit;
            case "heatingSupplyY" -> heatingSupplyYUnit;
            case "heatingReturnX" -> heatingReturnXUnit;
            case "heatingReturnY" -> heatingReturnYUnit;
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
                || selectionKey.kind() == RenderableKind.STAIR
                || selectionKey.kind() == RenderableKind.ROOM_OBJECT
                || selectionKey.kind() == RenderableKind.FLOOR_OPENING;
    }

    private boolean isTranslatableSelection(SelectionKey selectionKey) {
        return selectionKey.kind() == RenderableKind.WALL
                || selectionKey.kind() == RenderableKind.STAIR
                || selectionKey.kind() == RenderableKind.ROOM_OBJECT
                || selectionKey.kind() == RenderableKind.FLOOR_OPENING
                || selectionKey.kind() == RenderableKind.HEATING_ZONE
                || selectionKey.kind() == RenderableKind.HEATING_MANIFOLD
                || selectionKey.kind() == RenderableKind.HEATING_EXCLUSION;
    }

    private record PendingWallDimensionLabel(
            PlanSegment segment,
            String text,
            double normalOffset,
            double lineDistanceFromAxis,
            double outwardStep,
            double dimensionLengthMillimeters,
            String deduplicationKey
    ) implements DimensionLabelPlacementService.PendingLabel {
        @Override
        public double initialNormalOffset() {
            return normalOffset;
        }
    }

    private record RenderedWallDimensionLabel(
            PendingWallDimensionLabel pending,
            DimensionLineLayoutService.DimensionLineLayout layout,
            double directionX,
            double directionY,
            double normalOffset,
            double textX,
            double baselineY,
            TextBlockingBox blockingBox
    ) implements DimensionLabelPlacementService.PlacedLabel {

        @Override
        public List<TextBlockingBox> blockingBoxes() {
            return List.of(
                    blockingBox,
                    TextBlockingBox.aroundLine(
                            layout.lineStartX(),
                            layout.lineStartY(),
                            layout.lineEndX(),
                            layout.lineEndY(),
                            DIMENSION_LINE_BLOCKING_PADDING
                    )
            );
        }
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
