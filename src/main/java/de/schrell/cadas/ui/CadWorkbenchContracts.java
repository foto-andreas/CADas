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

abstract class CadWorkbenchContracts extends BorderPane {

    static final double DIMENSION_LINE_BLOCKING_PADDING = 4.0;

    // Von den aufgeteilten Workbench-Bausteinen implementiert.
    abstract void configureControls();

    abstract void configureLayout();

    abstract ToolBar buildSettingsBar();

    abstract ToolBar buildViewOptionsBar();

    abstract HBox buildViewBar();

    abstract Button viewButton(String label, Runnable action, String tooltipText);

    abstract Button workspaceModeButton(WorkspaceMode workspaceMode);

    abstract String workspaceModeButtonStyle(boolean active);

    abstract void settingsBarStyling();

    abstract void updateWorkspaceMode();

    abstract void switchToThreeDWorkspaceFromViewport();

    abstract boolean activateInteriorViewForCurrentRoom();

    abstract void selectWorkspaceMode(WorkspaceMode workspaceMode, boolean showErrorDialog);

    abstract void showInteriorViewUnavailableError();

    abstract void editTerrainElevations();

    abstract boolean handleTerrainBandContextClick(MouseEvent event);

    abstract void editTerrainPoint(
            TerrainProfileService.ProjectedTerrainPoint projection,
            List<PlanPoint> contour
    );

    abstract void editExistingTerrainPoint(
            TerrainProfileService.ProjectedTerrainPoint projection,
            TerrainProfileService.ProjectedTerrainPoint existingSample,
            List<PlanPoint> contour
    );

    abstract void editTerrainPointDialog(
            TerrainProfileService.ProjectedTerrainPoint projection,
            TerrainProfileService.ProjectedTerrainPoint existingSample,
            List<PlanPoint> contour
    );

    abstract MenuBar buildMenuBar();

    abstract ScrollPane buildPropertyPane();

    abstract TitledPane createPropertySection(String title, Node... nodes);

    abstract boolean propertySectionExpandedState(DrawingTool tool, String title);

    abstract void storePropertySectionExpansionState(DrawingTool tool);

    abstract void restorePropertySectionExpansionState(DrawingTool tool);

    abstract VBox propertyRow(String label, Node... controls);

    abstract void configureActionButtons();

    abstract void updatePropertySectionVisibility();

    abstract boolean shouldShowSection(DrawingTool tool, RenderableKind... kinds);

    abstract boolean shouldShowRoomSection();

    abstract boolean shouldShowLayerSection();

    abstract String selectionSummary();

    abstract String selectionLabel(SelectionKey selection);

    abstract void updateActionButtons();

    abstract MenuItem menuItem(String label, Runnable action, KeyCombination accelerator);

    abstract MenuItem toolMenuItem(DrawingTool tool, KeyCode keyCode);

    abstract CheckMenuItem checkMenuItem(String label, BooleanProperty property);

    abstract <T> CheckMenuItem checkMenuItem(String label, ObjectProperty<T> property, T checkedValue, T uncheckedValue);

    abstract KeyCombination shortcutKey(KeyCode keyCode);

    abstract KeyCombination shortcutShiftKey(KeyCode keyCode);

    abstract void runGuardedAction(String actionLabel, Runnable action);

    abstract void showActionException(String actionLabel, Throwable throwable);

    abstract void showOperationException(String title, Throwable throwable);

    abstract void showHeatingCircuitRoutingWindow();

    abstract void showErrorDialog(String title, String header, String content, Throwable throwable);

    public abstract void handleUnhandledException(Throwable throwable);

    abstract HBox labelledNode(String label, Node node);

    abstract void initializeUnitSelectors();

    abstract void initializeUnitSelector(TextField field, ComboBox<LengthUnit> selector, LengthUnit defaultUnit);

    abstract void convertLengthInputOnUnitChange(TextField field, LengthUnit oldUnit, LengthUnit newUnit);

    abstract void initializePresetSelectors();

    abstract void initializeSurfaceLayerControls();

    abstract void initializeHeatingControls();

    abstract void initializeDwgLibraryControls();

    abstract void loadUserSurfacePresets();

    abstract void registerConfiguredDwgLibraries();

    abstract void registerConfiguredDwgLibraryReference(Path sourceFile);

    abstract <T> void selectFirstIfAvailable(ComboBox<T> selector, ObservableList<T> values);

    abstract void applyFormTooltips();

    abstract void registerRenderListener(BooleanProperty property);

    abstract <T> void registerRenderListener(ObjectProperty<T> property);

    abstract Button createActionButton(String label, String style, Runnable action, String tooltipText);

    abstract void configureCanvas();

    abstract void handleMousePressed(MouseEvent event);

    abstract SelectionKey editSelectionAt(PlanPoint editPoint, boolean cycleSelection);

    abstract Optional<SelectionKey> preferredEndpointWallSelection(PlanPoint editPoint, WallEndpointSelection endpointSelection);

    abstract void handleMouseDragged(MouseEvent event);

    abstract void handleMouseReleased(MouseEvent event);

    abstract SelectionKey contextSelectionAt(MouseEvent event);

    abstract void updateModifierState(KeyEvent event);

    abstract void handleGlobalShortcuts(KeyEvent event);

    abstract void requestApplicationExit();

    public abstract boolean confirmApplicationClose();

    abstract boolean hasUnsavedChanges();

    abstract void updateMouseCursor();

    abstract PointerCursorService.PointerTarget pointerTargetAtLastPosition();

    abstract void resizeCanvases();

    abstract void render();

    abstract List<PlanPoint> terrainContour();

    abstract void drawGuides(GraphicsContext graphics);

    abstract void drawGuideDistances(GraphicsContext graphics);

    abstract void drawGrid(GraphicsContext graphics);

    abstract void drawSelectionOverlay(GraphicsContext graphics);

    abstract void drawSelectionRectangle(GraphicsContext graphics);

    abstract boolean shouldStartSelectionRectangle(SelectionKey editSelection);

    abstract void drawSelectedOpening(GraphicsContext graphics, UUID wallId, Length offset, Length width);

    abstract void drawSelectedRoomObjectOutline(GraphicsContext graphics, RoomObject roomObject);

    abstract void drawSelectedFloorOpening(GraphicsContext graphics, FloorOpening opening);

    abstract void drawSelectedHeatingZone(GraphicsContext graphics, HeatingZone zone);

    abstract void drawSelectedHeatingManifold(GraphicsContext graphics, HydronicHeating heating);

    abstract void drawSelectedHeatingExclusionArea(GraphicsContext graphics, HeatingExclusionArea area);

    abstract void drawLowerLevel(GraphicsContext graphics);

    abstract void drawWalls(GraphicsContext graphics);

    abstract void drawWallDimensions(GraphicsContext graphics, List<TextBlockingBox> seedBlockers);

    abstract void drawReportInteriorRoomDimensions(GraphicsContext graphics, List<TextBlockingBox> seedBlockers);

    abstract Optional<RenderedInteriorRoomDimension> layoutReportInteriorRoomDimension(
            Room room,
            PlanSegment segment,
            List<TextBlockingBox> blockers
    );

    abstract RenderedInteriorRoomDimension renderInteriorRoomDimension(
            PlanSegment segment,
            double normalX,
            double normalY,
            double sideSign,
            double offsetMillimeters,
            String text,
            Text textMeasure
    );

    abstract PlanPoint offsetPoint(PlanPoint point, double unitX, double unitY, double offsetMillimeters);

    abstract void appendWallDimensionLabels(List<PendingWallDimensionLabel> pendingLabels, Wall wall, DimensionLabelOptions options);

    abstract RenderedWallDimensionLabel layoutWallDimensionLabel(PendingWallDimensionLabel pendingLabel, double normalOffset);

    abstract ScreenBounds dimensionScreenBounds(RenderedWallDimensionLabel rendered);

    abstract void drawWallSurfaceLayers(GraphicsContext graphics);

    abstract void drawWallSurfaceLayersInPlan(GraphicsContext graphics);

    abstract boolean isWallSurfaceType(SurfaceType surfaceType);

    abstract void drawWallSurfaceStackInPlan(GraphicsContext graphics, Wall wall, SurfaceLayerStack stack);

    abstract void drawWallSurfaceLayerInPlan(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayerStack stack,
            SurfaceLayer layer,
            int layerIndex,
            double centerOffset
    );

    abstract void fillWallSurfaceIntervalInPlan(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayerStack stack,
            SurfaceLayer layer,
            int layerIndex,
            double centerOffset,
            WallSurfaceInterval interval,
            boolean selected
    );

    abstract void drawWallSurfaceJointsInPlan(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayer layer,
            double centerOffset,
            List<WallSurfaceInterval> visibleIntervals,
            boolean selected
    );

    abstract boolean isVisiblePlanJoint(double jointPosition, List<WallSurfaceInterval> visibleIntervals);

    abstract void drawWallSurfaceLayersInElevation(GraphicsContext graphics);

    abstract void drawWallSurfaceStackInElevation(GraphicsContext graphics, Wall wall, SurfaceLayerStack stack);

    abstract void drawWallSurfaceLayerInElevation(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayerStack stack,
            SurfaceLayer layer,
            double sideSign
    );

    abstract void drawWallSurfaceJointsInElevation(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayer layer,
            double startX,
            double endX,
            List<WallSurfaceRectangle> visibleRectangles,
            boolean selected
    );

    abstract void drawClippedWallSurfaceJointsInElevation(
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
    );

    abstract PlanPoint wallOffsetPoint(Wall wall, double localDistance, double normalOffset);

    abstract double interpolateScreen(double start, double end, double ratio);

    abstract void drawRooms(GraphicsContext graphics);

    abstract HydronicHeatingLayoutService.PlanningResult heatingLayouts(HydronicHeating heating);

    abstract boolean isHeatingLayoutDirty(HydronicHeating heating);

    abstract void scheduleHeatingLayoutRecalculation();

    abstract void scheduleHeatingLayoutRecalculation(UUID heatingId);

    abstract void scheduleHeatingLayoutRecalculationForZone(UUID zoneId);

    abstract void runHeatingLayoutRecalculation();

    abstract void recomputeHeatingLayoutNow(UUID heatingId);

    abstract void clearHeatingLayoutCache();

    abstract void drawHydronicHeatings(GraphicsContext graphics);

    abstract boolean shouldDrawHydronicHeating(HydronicHeating heating);

    abstract void drawFloorOpenings(GraphicsContext graphics, Room room);

    abstract void drawHeatingExclusionAreas(GraphicsContext graphics, Room room);

    abstract void drawTerrainElevation(GraphicsContext graphics);

    abstract void drawTerrainPlanArea(GraphicsContext graphics);

    abstract void drawTerrainPlanMarkers(GraphicsContext graphics);

    abstract List<TextBlockingBox> drawRoomLabels(GraphicsContext graphics);

    abstract void drawRoomTileGrid(GraphicsContext graphics, Room room);

    abstract void drawSelectedHeatingVarioBackground(GraphicsContext graphics, Room room);

    abstract Optional<SurfaceLayer> firstVisibleSurfaceLayer(SurfaceLayerStack stack);

    abstract List<SurfaceLayer> visiblePlanSurfaceLayers(SurfaceLayerStack stack);

    abstract void drawRoomTileLayer(GraphicsContext graphics, Room room, SurfaceLayer layer, boolean highlighted);

    abstract void drawRoomTileLayer(GraphicsContext graphics, Room room, SurfaceLayer layer, boolean highlighted, boolean forceVariothermCircles);

    abstract void drawSurfaceLayerDirectionArrow(
            GraphicsContext graphics,
            SurfaceLayer layer,
            List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> tiles
    );

    abstract List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> orderedSurfaceTilesForDirectionArrow(
            SurfaceLayer layer,
            List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> tiles
    );

    abstract void strokeSurfaceLayerDirectionArrow(GraphicsContext graphics, Point2D start, Point2D end, Color color, double lineWidth);

    abstract double modulo(double value, double modulus);

    abstract List<TextBlockingBox> drawRoomLabel(GraphicsContext graphics, Room room, PlanPoint center);

    abstract String roomMetricsText(Room room);

    abstract PlanPoint roomLabelCenter(Room room);

    abstract TextBlockingBox centeredTextBlockingBox(String text, Font font, double centerX, double y);

    abstract void drawRoomSlopeMarker(GraphicsContext graphics, Room room);

    abstract void drawRoomSlopeMarker(
            GraphicsContext graphics,
            Room room,
            SlopedCeilingProfile profile,
            int labelIndex
    );

    abstract void drawSlopeArrow(GraphicsContext graphics, PlanPoint arrowCenter, SlopedCeilingSide lowSide);

    abstract void drawDoors(GraphicsContext graphics);

    abstract void drawWindows(GraphicsContext graphics);

    abstract void drawRoofWindows(GraphicsContext graphics);

    abstract void drawRoofWindowOutline(GraphicsContext graphics, RoofWindow roofWindow);

    abstract void drawEditablePoints(GraphicsContext graphics);

    abstract void drawEdgeResizeHandles(GraphicsContext graphics);

    abstract void drawOpeningEditablePoints(GraphicsContext graphics, Wall wall, Length offset, Length width, Color color, boolean selected);

    abstract void drawEditablePoint(GraphicsContext graphics, PlanPoint point, Color color, double fillOpacity, double radius);

    abstract boolean samePlanPoint(PlanPoint first, PlanPoint second);

    abstract void drawRoomObjects(GraphicsContext graphics);

    abstract boolean shouldDrawRoomObject(RoomObject roomObject);

    abstract boolean isHeatingRoomObject(RoomObject roomObject);

    abstract void drawRoomObjectPlan(GraphicsContext graphics, RoomObject roomObject);

    abstract void drawFloorExtensions(GraphicsContext graphics);

    abstract void drawStaircases(GraphicsContext graphics);

    abstract void drawStraightStairTreads(GraphicsContext graphics, Staircase staircase);

    abstract void drawHalfTurnStair(GraphicsContext graphics, Staircase staircase);

    abstract void drawSwitchbackStair(GraphicsContext graphics, Staircase staircase);

    abstract void drawSpiralStair(GraphicsContext graphics, Staircase staircase);

    abstract void drawStairOutline(GraphicsContext graphics, Staircase staircase);

    abstract void strokeLocalRect(GraphicsContext graphics, Staircase staircase, double localX, double localY, double localWidth, double localHeight);

    abstract void strokeLocalLine(GraphicsContext graphics, Staircase staircase, double startLocalX, double startLocalY, double endLocalX, double endLocalY);

    abstract void drawWall(GraphicsContext graphics, PlanSegment segment, Length thickness, Color color, double widthFactor);

    abstract void drawWallElevation(GraphicsContext graphics, Wall wall, boolean selected);

    abstract void drawRoomElevation(GraphicsContext graphics, Room room);

    abstract boolean isSlopeVisibleInCurrentElevation(Room room);

    abstract void drawSlopedRoomElevation(GraphicsContext graphics, Room room, double left, double right, double floorY, double topY);

    abstract void drawPolygonalRoomElevation(GraphicsContext graphics, Room room, double floorY);

    abstract void addElevationSample(java.util.TreeMap<Long, Double> topProfile, PlanPoint point, double ceilingHeightMillimeters);

    abstract void drawStairElevation(GraphicsContext graphics, Staircase staircase);

    abstract void drawOpeningElevation(GraphicsContext graphics, PlanPoint openingStart, PlanPoint openingEnd, double baseHeightMillimeters, double openingHeightMillimeters, Color color);

    abstract void drawPreview(GraphicsContext graphics);

    abstract void drawHeatingManifoldPreviewMarkers(GraphicsContext graphics, double startX, double startY, double endX, double endY);

    abstract void drawDimensionLabel(GraphicsContext graphics, PlanSegment segment, String text);

    abstract void drawDimensionLabel(GraphicsContext graphics, PlanSegment segment, String text, double normalOffset);

    abstract DimensionStandard currentDimensionStandard();

    abstract void drawIsoDimensionLines(
            GraphicsContext graphics,
            DimensionLineLayoutService.DimensionLineLayout layout,
            double directionX,
            double directionY
    );

    abstract void drawViewOverlay(GraphicsContext graphics);

    abstract void drawCompass(GraphicsContext graphics);

    abstract void drawRulers();

    abstract void drawHorizontalRuler(GraphicsContext graphics);

    abstract void drawVerticalRuler(GraphicsContext graphics);

    abstract double chooseRulerStep();

    abstract String formatRuler(double worldMillimeters);

    abstract DraftingConstraints currentConstraints(boolean orthogonalMode);

    abstract Grid currentGrid();

    abstract Length pointerSelectionTolerance();

    abstract Length currentWallThickness();

    abstract Length currentWallHeight();

    abstract Length currentEndpointHeight();

    abstract String currentRoomName();

    abstract AutoRoomGenerationService.RoomDefaults currentRoomDefaults();

    abstract Length currentRoomHeight();

    abstract Length currentFloorThickness();

    abstract Length currentFloorExtensionThickness();

    abstract Length currentCeilingThickness();

    abstract Length currentDoorWidth();

    abstract Length currentDoorHeight();

    abstract Length currentThresholdHeight();

    abstract Length currentWindowWidth();

    abstract Length currentWindowHeight();

    abstract Length currentSillHeight();

    abstract StairType currentStairType();

    abstract Length currentStairHeight();

    abstract int currentStairSteps();

    abstract Length currentStairStartLanding();

    abstract Length currentStairEndLanding();

    abstract Length currentStairLeftUnderbuild();

    abstract Length currentStairRightUnderbuild();

    abstract Length currentStairUndersideThickness();

    abstract Length currentRoomObjectWidth(RoomObjectPreset preset);

    abstract String currentRoomObjectName(RoomObjectPreset preset);

    abstract Length currentRoomObjectDepth(RoomObjectPreset preset);

    abstract Length currentRoomObjectHeight(RoomObjectPreset preset);

    abstract double currentRoomObjectHeatOutputWatts(double fallback);

    abstract RoomObjectHeatingType currentRoomObjectHeatingType(RoomObjectHeatingType fallback);

    abstract Length currentRoomObjectBaseElevation();

    abstract Length positiveLength(TextField field, ComboBox<LengthUnit> unitSelector, Length fallback);

    abstract double currentRoomObjectAngleDegrees();

    abstract Optional<Length> parseLength(TextField field, LengthUnit unit);

    abstract Optional<Angle> parseAngle(TextField field);

    abstract double currentNorthAngleDegrees();

    abstract void updateStatus();

    abstract String statusHintForCurrentTool();

    abstract void applyTooltip(javafx.scene.Node node, String text);

    abstract void createLevel();

    abstract void renameCurrentLevel();

    abstract void moveCurrentLevelUp();

    abstract void moveCurrentLevelDown();

    abstract void moveCurrentLevel(int direction);

    abstract DrawingTool currentTool();

    abstract PlanPoint snapDrawingPoint(PlanPoint point, DraftingConstraints constraints);

    abstract GuideSnapTargets currentGuideSnapTargets();

    abstract GuideSnapTargets currentAlignmentSnapTargets(Set<UUID> excludedWallIds);

    abstract Door snapDoorToGuides(Door door);

    abstract WindowElement snapWindowToGuides(WindowElement window);

    abstract Wall openingDragWall();

    abstract void placeDoor(PlanPoint clickPoint);

    abstract void placeWindow(PlanPoint clickPoint);

    abstract void placeRoofWindow(PlanPoint clickPoint);

    abstract void placeRoomObject(PlanPoint clickPoint);

    abstract void createFloorOpening(PlanSegment bounds, FloorOpeningShape shape);

    abstract void createHeatingExclusionArea(PlanSegment bounds);

    abstract void createHeatingZone(PlanSegment bounds);

    abstract void placeHydronicManifold(PlanPoint point);

    abstract void placeHydronicManifold(PlanSegment bounds);

    abstract Optional<ManifoldTarget> manifoldTarget(PlanPoint point);

    abstract Optional<Room> roomAt(PlanPoint point);

    abstract void startGuideDrag(GuideOrientation orientation, double worldMillimeters);

    abstract void updateGuideDrag(GuideOrientation orientation, double worldMillimeters);

    abstract void finishGuideDrag(GuideOrientation orientation, double worldMillimeters);

    abstract void removeNearestGuide(PlanPoint clickPoint);

    abstract double guideWorldPositionFromHorizontalRuler(MouseEvent event);

    abstract double guideWorldPositionFromVerticalRuler(MouseEvent event);

    abstract Point2D projectedPointInDrawingPane(MouseEvent event);

    abstract PlanPoint snapGuidePoint(Point2D point);

    abstract String formatGuidePosition(GuideOrientation orientation, double worldMillimeters);

    abstract void saveCurrentLevel();

    abstract void saveCurrentLevelAs();

    abstract void saveCurrentLevelTo(Path targetFile);

    abstract void exportCurrentLevel(Path targetFile);

    abstract void saveProject();

    abstract void saveProjectAs();

    abstract void exportProjectAsDxf(Path targetFile);

    abstract void confirmExportWritten(Path exportPath);

    public abstract void showAboutDialog();

    abstract void importLevel();

    abstract void importThreeDObject();

    abstract void importThreeDObject(Path sourceFile);

    abstract void importLevel(Path sourceFile);

    abstract void importProjectFromDxf();

    abstract void importProjectFromDxf(Path sourceFile);

    abstract FileChooser createCadasFileChooser();

    abstract String uniqueLevelName(String baseName);

    abstract boolean containsLevelName(String candidate);

    abstract void importPartLibrary();

    abstract FileChooser createPartLibraryFileChooser();

    abstract void importPartLibrary(Path sourceFile);

    abstract void updateCadLibrarySummary();

    abstract String cadLibrarySummaryLine(Path path);

    abstract void registerDwgLibrary(Path sourceFile, boolean askBeforeOverwrite);

    abstract DwgLibraryAnalysis analyzeDwgLibrary(Path sourceFile, boolean force);

    abstract Path configuredCadLibraryPath(Path sourceFile, boolean askBeforeOverwrite);

    abstract boolean shouldOverwriteConfiguredCadLibrary(Path sourceFile);

    abstract boolean isSameFile(Path first, Path second);

    abstract void addDwgBlockPreset();

    abstract SurfaceCoveringPreset registerDwgBlockPreset(Path sourceFile, String blockName);

    abstract Optional<DwgBlockDefinition> findAnalyzedDwgBlock(Path sourceFile, String blockName);

    abstract void addSelectedDwgBlockAsSurfacePreset();

    abstract void addSelectedDwgBlockAsObjectPreset();

    abstract void registerRoomObjectPreset(RoomObjectPreset preset);

    abstract void refreshCurrentDwgLibraryAnalysis();

    abstract void applyDwgBlockFilter();

    abstract boolean blockMatchesFilter(DwgBlockDefinition block, String filter);

    abstract void refreshDwgBlockPreviewAndDetails();

    abstract void drawEmptyDwgPreview(String text);

    abstract void drawDwgPreview(DwgBlockDefinition block);

    abstract void registerSurfacePreset(SurfaceCoveringPreset preset);

    abstract void saveCurrentSurfacePreset();

    abstract SurfaceCoveringPreset currentSurfacePresetFromInputs();

    abstract boolean confirmOverwrite(String title, String header, String content);

    abstract void applyDoorPreset(DoorPreset preset);

    abstract void applyWindowPreset(WindowPreset preset);

    abstract void applyStairPreset(StairPreset preset);

    abstract void applyRoomObjectPreset(RoomObjectPreset preset);

    abstract void applySurfacePreset(SurfaceCoveringPreset preset);

    abstract Optional<HydronicHeating> selectedHydronicHeating();

    abstract void refreshHeatingSection();

    abstract void syncHeatingRoutingCommandArea();

    abstract void syncHeatingRoutingCommandArea(HydronicHeating heating);

    abstract void syncHeatingZoneSettingsInputs();

    abstract void syncHeatingZoneSettingsInputs(HydronicHeating heating);

    abstract void replaceTextPreservingCaretAndScroll(TextArea textArea, String text);

    abstract String normalizeRoutingEditorText(String text, HeatingZone zone);

    abstract String normalizeRoutingEditorDisplayText(String text);

    abstract Optional<HeatingZone> activeHeatingZoneForRoutingInput();

    abstract boolean usesMirroredRoutingAliases(HeatingZone zone);

    abstract String heatingUpdateMessage(HydronicHeating heating, String successPrefix);

    abstract void planHydronicHeating();

    abstract void planHydronicHeatingAutomatically();

    abstract String formatHeatingWarnings(List<HydronicHeatingLayoutService.ValidationIssue> warnings);

    abstract void showHeatingWarnings(List<HydronicHeatingLayoutService.ValidationIssue> warnings);

    abstract void showRoomSynchronizationWarning(Level.RoomReplacementImpact impact);

    abstract void rememberWarning(String title, String header, String content);

    abstract HydronicHeating heatingFromInputs(Room room, UUID heatingId);

    abstract Length requiredPositiveLength(TextField field, ComboBox<LengthUnit> unitSelector, String label);

    abstract Length requiredNonNegativeLength(TextField field, ComboBox<LengthUnit> unitSelector, String label);

    abstract void applySelectedHeatingZoneSettings();

    abstract HeatingZone heatingZoneDraft(
            HeatingZone baseZone,
            String routingCommands,
            HeatingLayoutPattern layoutPattern,
            boolean serpentineMiddleLine
    );

    abstract void generateSelectedHeatingZoneRouting();

    abstract void applySelectedHeatingZoneRouting();

    abstract boolean removeHeatingZoneById(UUID zoneId);

    abstract void applyHeatingZones(HydronicHeating heating, List<HeatingZone> zones, int selectedIndex);

    abstract boolean resetHydronicManifoldById(UUID heatingId);

    abstract void refreshSurfaceLayerSection();

    abstract String describeSurfaceLayer(SurfaceLayer layer);

    abstract int estimatedTileCount(SurfaceLayer layer);

    abstract void syncInputsFromSelectedSurfaceLayer();

    abstract void addSurfaceLayer();

    abstract void updateSurfaceLayer();

    abstract void removeSurfaceLayer();

    abstract void toggleSurfaceLayerVisibility();

    abstract void moveSurfaceLayer(int direction);

    abstract void afterSurfaceLayerMutation(String message);

    abstract void repairSelectedSurfaceLayerLayout();

    abstract boolean selectedSurfaceLayerNeedsRepair();

    abstract boolean surfaceLayerNeedsRepair(SurfaceLayer layer);

    abstract SurfaceLayer repairedSurfaceLayer(SurfaceLayer layer);

    abstract SurfaceLayer buildSurfaceLayerFromInputs();

    abstract String currentSurfaceLayerName();

    abstract Length currentSurfaceLayerThickness();

    abstract Length currentSurfaceTileWidth();

    abstract Length currentSurfaceTileHeight();

    abstract Length currentStoredSurfaceTileWidth();

    abstract Length currentStoredSurfaceTileHeight();

    abstract void cycleSurfaceLayoutCorner(boolean forward);

    abstract void syncSurfaceLayoutAnchorForDirection(SurfaceLayoutDirection direction);

    abstract void applySurfaceLayoutAnchorSelection(SurfaceLayoutAnchor anchor);

    abstract void updateSurfaceLayoutCornerLabel();

    abstract boolean applySurfaceLayoutOrientationToSelectedLayers();

    abstract SurfaceLayoutDirection currentSurfaceLayoutDirection();

    abstract boolean currentSurfaceLayoutRotatedQuarterTurn();

    abstract de.schrell.cadas.domain.model.SurfaceLayoutAnchor currentSurfaceLayoutAnchor();

    abstract SurfaceLayoutMode currentSurfaceLayoutMode();

    abstract Length currentSurfaceLayoutOffset();

    abstract Length currentSurfaceMinimumOffset();

    abstract Length currentSurfaceMinimumEdgeWidth();

    abstract Length currentSurfaceMinimumStartEndMargin();

    abstract SurfaceLayoutMargins currentSurfaceFreeMargins();

    abstract Length currentSurfaceJointWidth();

    abstract SurfaceCutRestriction currentSurfaceCutRestriction();

    abstract String currentSurfaceCoveringSource();

    abstract Optional<Path> currentDwgLibraryPath();

    abstract Optional<SurfaceLayer> selectedSurfaceLayer();

    abstract SurfaceType currentSurfaceType();

    abstract Optional<Room> selectedRoom();

    abstract Optional<HeatingZoneContext> selectedHeatingZoneContext();

    abstract Optional<HeatingContext> selectedHeatingContext();

    abstract Optional<HeatingZoneContext> contextHeatingZoneContext();

    abstract Optional<HeatingZoneContext> heatingZoneContext(UUID zoneId);

    abstract List<SurfaceType> availableSurfaceTypesForSelection();

    abstract void refreshSurfaceTypeSelector();

    abstract Optional<Room> selectedSurfaceRoom();

    abstract List<Wall> selectedWalls();

    abstract Optional<SurfaceSelectionContext> currentSurfaceSelectionContext();

    abstract Optional<FloorExtension> selectedFloorExtension();

    abstract String currentSurfaceSelectionHint();

    abstract Optional<SurfaceLayerStack> currentDisplaySurfaceLayerStack();

    abstract List<SurfaceLayerStack> currentSurfaceLayerStacks();

    abstract boolean isSelectedSurfaceLayer(SurfaceLayerStack stack, SurfaceLayer layer);

    abstract boolean isVisibleSurfaceLayer(SurfaceLayer layer);

    abstract boolean isVariothermSurfaceLayer(SurfaceLayer layer);

    abstract boolean validateSurfaceLayerSelection(SurfaceSelectionContext context);

    abstract List<String> interiorWallTargetKeys(List<Wall> walls);

    abstract void showSurfaceLayerError(String header, String content);

    abstract void replaceSurfaceLayer(SurfaceLayerStack stack, UUID layerId, SurfaceLayer replacement);

    abstract PlanPoint screenToWorld(double screenX, double screenY);

    abstract double toScreenX(double worldMillimeters);

    abstract double toScreenY(double worldMillimeters);

    abstract double toScreenProjectedX(PlanPoint point, double heightMillimeters);

    abstract double toScreenProjectedY(PlanPoint point, double heightMillimeters);

    abstract double toScreenHorizontal(double projectedMillimeters);

    abstract double toScreenVertical(double projectedMillimeters);

    abstract double projectHorizontal(PlanPoint point, double heightMillimeters);

    abstract double projectVertical(PlanPoint point, double heightMillimeters);

    abstract double scale();

    abstract boolean isDirectEditingView();

    abstract double clamp(double value, double min, double max);

    abstract void resetTwoDView();

    abstract void fitCurrentViewToContent();

    abstract void fitCurrentViewToContent(double horizontalPadding, double verticalPadding);

    abstract void fitCurrentReportViewToContent();

    abstract void fitRenderedReportBoundsIntoView();

    abstract Optional<ScreenBounds> currentReportScreenBounds();

    abstract void scaleViewAroundViewportCenter(double factor, double viewportWidth, double viewportHeight);

    abstract double viewportShift(double min, double max, double visibleMin, double visibleMax);

    abstract void fitPlanViewToPoints(List<PlanPoint> points, double paddingMillimeters);

    abstract void fitViewportToBounds(
            double viewportWidth,
            double viewportHeight,
            double contentWidthMillimeters,
            double contentHeightMillimeters,
            double centerHorizontalMillimeters,
            double centerVerticalMillimeters,
            double horizontalPadding,
            double verticalPadding
    );

    abstract void clearProject();

    abstract void undo();

    abstract void redo();

    abstract void rememberStateForUndo();

    abstract WorkbenchSnapshot captureSnapshot();

    abstract void restoreSnapshot(WorkbenchSnapshot snapshot);

    abstract void clearSelection();

    abstract void deleteSelection();

    abstract boolean confirmSelectionDeletion();

    abstract boolean isDeletableSelection(SelectionKey selection);

    abstract void applySelectionRectangle();

    abstract boolean hasSelectionRectangleArea();

    abstract void clearSelectionRectangle();

    abstract boolean removeStaircaseWithUnderbuild(UUID staircaseId);

    abstract void updateSelection(SelectionKey selectionKey, boolean toggleSelection);

    abstract void selectSingle(SelectionKey selectionKey);

    abstract void clearSelectionsInternal();

    abstract void syncSelectionState();

    abstract void rebuildSelectionContextMenu();

    abstract Optional<Room> contextMenuRoom();

    abstract void setContextHeatingZonePattern(HeatingLayoutPattern pattern);

    abstract void invertContextHeatingZone();

    abstract void setContextHeatingZoneSupplyConnection();

    abstract void setContextHeatingZoneReturnConnection();

    abstract void replaceHeatingZone(HeatingZoneContext context, HeatingZone replacement, String successPrefix);

    abstract Optional<HeatingZone> mergeableHeatingZone(HeatingZoneContext context);

    abstract void mergeContextHeatingZone();

    abstract void createRoofSlopeFromSelectedWall();

    abstract Optional<WallIntersectionSplitService.SplitCandidate> contextWallSplitCandidate();

    abstract void splitSelectedWallsAtContextIntersection();

    abstract List<UUID> contextWallSplitWallIds();

    abstract void openInteriorViewFromContextLocation();

    abstract void renameContextRoom();

    abstract void renameRoom(UUID roomId, String newName);

    abstract void recognizeRoomFromSelectedWalls();

    abstract void syncInputsFromPrimarySelection();

    abstract void applyCurrentInputsToSelection();

    abstract void applyEndpointHeightToSelection();

    abstract void syncEndpointHeightInputFromSelection();

    abstract Optional<Length> selectedEndpointHeight();

    abstract Set<String> selectedIds();

    abstract String formatValue(Length length, LengthUnit unit, int decimals);

    abstract String formatNonNegativeDouble(double value, int decimals);

    abstract void setLengthInput(TextField field, ComboBox<LengthUnit> unitSelector, Length length, LengthUnit unit);

    abstract void syncLengthInput(TextField field, ComboBox<LengthUnit> unitSelector, Length length, LengthUnit fallbackUnit);

    abstract void rotateSelectedComponentsClockwise();

    abstract void rotateSelectedComponentsCounterClockwise();

    abstract void rotateSelectedComponents(boolean clockwise);

    abstract void mirrorSelectedHeatingZones(boolean horizontally);

    abstract void activateLevel(Level level);

    abstract void handleThreeDSelection(SelectionKey selectionKey);

    abstract void markThreeDDirty();

    abstract void synchronizeRoomsFromWalls(Level level);

    abstract void previewRoomSynchronizationFromWalls(Level level);

    abstract Level.RoomReplacementImpact synchronizeRoomsFromWalls(Level level, boolean showWarning);

    abstract void flushPendingRoomSynchronizationWarning();

    abstract void synchronizeStairUnderbuild(Staircase staircase);

    abstract void prepareSelectionDrag(SelectionKey selectionKey, PlanPoint anchorPoint);

    abstract void translateSelectedComponents(PlanPoint snappedPoint);

    abstract boolean isHeatingZoneHandle(EdgeResizeService.EdgeHandle handle);

    abstract boolean hasSelectedHeatingZone();

    abstract Optional<HeatingZone> firstSelectedHeatingZone(List<HydronicHeating> heatings);

    abstract HeatingZone snapHeatingZoneRoutingStartIfNeeded(HeatingZone zone);

    abstract boolean moveSelectionWithArrowKey(KeyCode keyCode);

    abstract boolean moveSelectionByArrowKey(KeyCode keyCode);

    abstract void moveSelectedComponents(double deltaX, double deltaY);

    abstract TranslationDelta snapHeatingZoneTranslationToGrid(List<HydronicHeating> heatings, double deltaX, double deltaY);

    abstract boolean selectedTranslationAffectsRooms();

    abstract void correctSelectedComponentsOrthogonally();

    abstract void refreshThreeDIfNeeded();

    abstract boolean isSelected(RenderableKind kind, String elementId);

    public abstract WorkbenchAutomationSnapshot automationSnapshot();

    public abstract void automationSetViewport(double zoomFactor, double newOffsetX, double newOffsetY);

    public abstract void automationAddRoom(Room room);

    public abstract Room automationRoom(int index);

    public abstract void automationPlanHydronicHeating(String surfacePosition, String layoutPattern);

    public abstract HydronicHeating automationHydronicHeating(int index);

    public abstract int automationHydronicHeatingCount();

    public abstract void automationDeleteSelection();

    public abstract void automationReplaceHeatingZone(int heatingIndex, int zoneIndex, String name, List<PlanPoint> outline);

    public abstract void automationReplaceHeatingZone(
            int heatingIndex,
            int zoneIndex,
            String name,
            List<PlanPoint> outline,
            String layoutPattern,
            boolean flowInverted
    );

    public abstract void automationRemoveHeatingZone(int heatingIndex, int zoneIndex);

    public abstract void automationAddHeatingZone(
            int heatingIndex,
            String name,
            List<PlanPoint> outline,
            String layoutPattern,
            boolean flowInverted
    );

    public abstract void automationAddDefaultHeatingZone(int heatingIndex);

    public abstract void automationPrepareSelectionContextMenu(double screenX, double screenY);

    public abstract List<String> automationSelectionContextMenuItems();

    public abstract void automationInvokeSelectionContextMenuItem(String label);

    public abstract void automationRenameContextRoom(String name);

    public abstract PlanPoint automationInteriorEyePosition();

    public abstract WritableImage automationDrawingSnapshot();

    abstract WritableImage reportSnapshot(
            String levelName,
            List<PlanPoint> focusPoints,
            double paddingMillimeters,
            ReportSnapshotOptions options
    );

    abstract double[] currentCenterDividerPositions();

    abstract void restoreCenterDividerPositions(double[] dividerPositions);

    abstract WritableImage reportCanvasSnapshot(double renderScale);

    abstract Level resolveLevelForReport(String levelName);

    public abstract void automationRememberUndoState();

    public abstract void automationSetTool(String toolName);

    public abstract void automationSelectRoomObjectPreset(String presetId);

    public abstract void automationSetShowDimensions(boolean visible);

    public abstract void automationSetShowVariothermCircles(boolean visible);

    public abstract void automationSetShowHeatingCircuits(boolean visible);

    public abstract int automationFloorExtensionCount();

    public abstract int automationFloorOpeningCount();

    public abstract int automationHeatingExclusionAreaCount();

    public abstract int automationRoofWindowCount();

    public abstract void automationPlaceRoofWindow(double worldXMillimeters, double worldYMillimeters);

    public abstract FloorOpening automationFloorOpening(int index);

    public abstract HeatingExclusionArea automationHeatingExclusionArea(int index);

    public abstract int automationRoomObjectCount();

    public abstract RoomObject automationRoomObject(int index);

    public abstract Wall automationWall(int index);

    public abstract boolean automationMoveSelectionWithArrowKey(KeyCode keyCode);

    public abstract void automationTriggerGlobalKey(KeyCode keyCode);

    public abstract FloorExtension automationFloorExtension(int index);

    public abstract void automationSelectLevel(String levelName);

    public abstract void automationSetWorkspace(String workspaceName);

    public abstract void automationSetSurfaceType(String surfaceTypeName);

    public abstract void automationSetSurfaceLayoutDirection(String directionName);

    public abstract String automationSurfaceLayoutDirection();

    public abstract String automationSurfaceLayoutCornerLabel();

    public abstract void automationSelect(String kindName, int index, boolean toggle);

    public abstract void automationSelectSurfaceLayer(int index);

    public abstract void automationSetField(String fieldName, String value);

    public abstract void automationSetHeatingRoutingCommandAreaText(String text);

    public abstract void automationSetHeatingZonePointAreaText(String text);

    public abstract void automationSetHeatingRoutingCommandAreaCaretPosition(int position);

    public abstract void automationSetHeatingRoutingCommandAreaScrollTop(double scrollTop);

    public abstract int automationHeatingRoutingCommandAreaCaretPosition();

    public abstract double automationHeatingRoutingCommandAreaScrollTop();

    public abstract String automationHeatingRoutingCommandAreaText();

    public abstract String automationFieldValue(String fieldName);

    public abstract void automationSetUnit(String fieldName, String unitName);

    public abstract String automationUnit(String fieldName);

    public abstract void automationPlaceGuide(String orientationName, double worldMillimeters);

    public abstract void automationCanvasClick(double x, double y, MouseButton button, boolean shiftDown, boolean shortcutDown, boolean altDown);

    public abstract void automationCanvasDrag(double fromX, double fromY, double toX, double toY, MouseButton button, boolean shiftDown, boolean shortcutDown, boolean altDown);

    public abstract void automationCanvasPress(double x, double y, MouseButton button);

    public abstract void automationCanvasDragTo(double x, double y, MouseButton button);

    public abstract void automationCanvasRelease(double x, double y, MouseButton button);

    public abstract String automationActiveEdgeHandle();

    public abstract String automationEdgeHandleAtScreen(double x, double y);

    public abstract String automationCursorAt(double x, double y, boolean altDown, boolean spaceDown);

    public abstract List<PlanPoint> automationEdgeHandleScreenPoints();

    public abstract void automationSetErrorDialogsEnabled(boolean enabled);

    public abstract void automationClearLastError();

    public abstract void automationClearLastWarning();

    public abstract String automationLastErrorTitle();

    public abstract String automationLastErrorHeader();

    public abstract String automationLastErrorContent();

    public abstract String automationLastErrorStackTrace();

    public abstract String automationLastWarningTitle();

    public abstract String automationLastWarningHeader();

    public abstract String automationLastWarningContent();

    public abstract int automationWarningCount();

    public abstract void automationDisableApplicationExit();

    public abstract boolean automationExitRequested();

    public abstract boolean automationHasUnsavedChanges();

    public abstract void automationSetUnsavedChangesExitDecision(boolean exitWithoutSaving);

    public abstract void automationTriggerShortcutOnField(String fieldName, KeyCode keyCode, boolean shortcutDown, boolean shiftDown);

    abstract KeyEvent shortcutEvent(KeyCode keyCode, boolean shortcutDown, boolean shiftDown);

    public abstract WorkbenchAutomationSnapshot automationInvoke(String actionName, Path path);

    abstract <T> SelectionKey selectionKeyByIndex(
            List<T> elements,
            int index,
            RenderableKind kind,
            Function<T, UUID> idExtractor,
            String errorLabel
    );

    abstract void runPreparedThreeDAction(boolean forceThreeDWorkspace, Runnable action);

    abstract void runPreparedThreeDViewportAction(Runnable action);

    abstract void activateThreeDWorkspaceForSnapshot();

    abstract void exportWorkbenchSnapshot(Path path);

    abstract String automationSelectedRoomMetrics();

    public abstract void automationSetStatusText(String text);

    abstract void clearProjectWithoutDialog();

    abstract Path requirePath(Path path, String actionName);

    abstract TextField textFieldByName(String fieldName);

    abstract ComboBox<LengthUnit> unitSelectorByName(String fieldName);

    abstract <T> T uiMember(String memberName, Class<T> expectedType, String label);

    abstract java.lang.reflect.Field findUiMemberField(String memberName) throws NoSuchFieldException;

    abstract void ensureCanvasReady();

    abstract boolean isRotatableSelection(SelectionKey selectionKey);

    abstract boolean isTranslatableSelection(SelectionKey selectionKey);

    abstract MouseEvent mouseEvent(javafx.event.EventType<MouseEvent> type,
                                  double x,
                                  double y,
                                  MouseButton button,
                                  boolean shiftDown,
                                  boolean shortcutDown,
                                  boolean altDown,
                                  boolean buttonDown);

    enum WorkspaceMode {
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

    record HeatingZoneContext(
            Room room,
            HydronicHeating heating,
            HeatingZone zone,
            int zoneIndex
    ) {
    }

    record HeatingContext(
            Room room,
            HydronicHeating heating
    ) {
    }

    record ManifoldTarget(
            Room room,
            HydronicHeating heating
    ) {
    }

    record ReportSnapshotOptions(
            boolean restrictSurfaceLayers,
            Set<UUID> visibleSurfaceLayerIds,
            boolean includeHydronicHeating,
            Set<HeatingSurfacePosition> visibleHydronicSurfacePositions,
            boolean includeDimensions,
            boolean includeAreaVolume,
            boolean interiorRoomDimensionsOnly,
            boolean filterHeatingRoomObjects,
            Set<RoomObjectHeatingType> visibleHeatingObjectTypes,
            double renderScale
    ) {

        ReportSnapshotOptions {
            visibleSurfaceLayerIds = Set.copyOf(visibleSurfaceLayerIds);
            visibleHydronicSurfacePositions = Set.copyOf(visibleHydronicSurfacePositions);
            visibleHeatingObjectTypes = Set.copyOf(visibleHeatingObjectTypes);
            if (renderScale < 1.0) {
                throw new IllegalArgumentException("renderScale muss mindestens 1 sein.");
            }
        }

        static ReportSnapshotOptions defaults() {
            return new ReportSnapshotOptions(false, Set.of(), true, Set.of(), true, true, false, false, Set.of(), 1.0);
        }

        ReportSnapshotOptions hideHeatingRoomObjects() {
            return withVisibleHeatingObjectTypes(Set.of());
        }

        ReportSnapshotOptions withVisibleHeatingObjectTypes(Set<RoomObjectHeatingType> newVisibleHeatingObjectTypes) {
            return new ReportSnapshotOptions(
                    restrictSurfaceLayers,
                    visibleSurfaceLayerIds,
                    includeHydronicHeating,
                    visibleHydronicSurfacePositions,
                    includeDimensions,
                    includeAreaVolume,
                    interiorRoomDimensionsOnly,
                    true,
                    newVisibleHeatingObjectTypes,
                    renderScale
            );
        }

        ReportSnapshotOptions withRenderScale(double newRenderScale) {
            return new ReportSnapshotOptions(
                    restrictSurfaceLayers,
                    visibleSurfaceLayerIds,
                    includeHydronicHeating,
                    visibleHydronicSurfacePositions,
                    includeDimensions,
                    includeAreaVolume,
                    interiorRoomDimensionsOnly,
                    filterHeatingRoomObjects,
                    visibleHeatingObjectTypes,
                    newRenderScale
            );
        }
    }

    record PendingWallDimensionLabel(
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

    record RenderedWallDimensionLabel(
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

    record RenderedInteriorRoomDimension(
            String text,
            double lineStartX,
            double lineStartY,
            double lineEndX,
            double lineEndY,
            double textX,
            double baselineY,
            TextBlockingBox blockingBox
    ) {
    }

    record ScreenBounds(double minX, double maxX, double minY, double maxY) {

        static ScreenBounds from(TextBlockingBox box) {
            return new ScreenBounds(box.minX(), box.maxX(), box.minY(), box.maxY());
        }

        static ScreenBounds union(ScreenBounds first, ScreenBounds second) {
            if (first == null) {
                return second;
            }
            if (second == null) {
                return first;
            }
            return new ScreenBounds(
                    Math.min(first.minX(), second.minX()),
                    Math.max(first.maxX(), second.maxX()),
                    Math.min(first.minY(), second.minY()),
                    Math.max(first.maxY(), second.maxY())
            );
        }

        ScreenBounds includeLine(double startX, double startY, double endX, double endY, double padding) {
            return union(this, new ScreenBounds(
                    Math.min(startX, endX) - padding,
                    Math.max(startX, endX) + padding,
                    Math.min(startY, endY) - padding,
                    Math.max(startY, endY) + padding
            ));
        }

        double width() {
            return maxX - minX;
        }

        double height() {
            return maxY - minY;
        }
    }

    record SurfaceSelectionContext(SurfaceType surfaceType, List<String> targetKeys, String label, String hint) {
    }

    record TranslationDelta(double deltaX, double deltaY) {
    }

    record WarningPresentation(String title, String header, String content) {

        static WarningPresentation empty() {
            return new WarningPresentation("", "", "");
        }
    }
}
