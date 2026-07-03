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

abstract class CadWorkbenchAutomation extends CadWorkbenchSelectionAndContext {

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

    public void automationDeleteSelection() {
        deleteSelection();
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
        zones.add(CadWorkbenchHeatingSupport.defaultHeatingZone(room, heating, heatingCircuitRoutingService));
        applyHeatingZones(heating, zones, zones.size() - 1);
    }

    public void automationPrepareSelectionContextMenu(double screenX, double screenY) {
        contextMenuWorldPoint = screenToWorld(screenX, screenY);
        contextMenuSelection = selectionQueryService.findSelection(
                activeLevel.get(),
                contextMenuWorldPoint,
                pointerSelectionTolerance()
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

    WritableImage reportSnapshot(
            String levelName,
            List<PlanPoint> focusPoints,
            double paddingMillimeters,
            ReportSnapshotOptions options
    ) {
        WorkspaceMode previousWorkspace = activeWorkspaceMode.get();
        ViewOrientation previousView = activeView.get();
        Level previousLevel = activeLevel.get();
        double previousZoom = zoom;
        double previousOffsetX = offsetX;
        double previousOffsetY = offsetY;
        double[] previousDividerPositions = currentCenterDividerPositions();
        SelectionKey previousPrimarySelection = selectedSelection.get();
        List<SelectionKey> previousSelections = List.copyOf(selectedSelections);
        boolean previousRestrictSurfaceLayers = reportSnapshotRestrictSurfaceLayers;
        Set<UUID> previousVisibleSurfaceLayerIds = reportSnapshotVisibleSurfaceLayerIds;
        boolean previousHideHydronicHeatings = reportSnapshotHideHydronicHeatings;
        Set<HeatingSurfacePosition> previousVisibleHydronicSurfacePositions = reportSnapshotVisibleHydronicSurfacePositions;
        boolean previousFilterHeatingRoomObjects = reportSnapshotFilterHeatingRoomObjects;
        Set<RoomObjectHeatingType> previousVisibleHeatingObjectTypes = reportSnapshotVisibleHeatingObjectTypes;
        boolean previousInteriorRoomDimensionsOnly = reportSnapshotInteriorRoomDimensionsOnly;
        boolean previousShowDimensions = showDimensions.get();
        boolean previousShowAreaVolume = showAreaVolume.get();
        boolean previousShowHeatingCircuits = showHeatingCircuits.get();
        boolean previousShowVariothermCircles = showVariothermCircles.get();
        ensureCanvasReady();
        try {
            double renderScale = Math.max(1.0, options.renderScale());
            reportSnapshotRestrictSurfaceLayers = options.restrictSurfaceLayers();
            reportSnapshotVisibleSurfaceLayerIds = Set.copyOf(options.visibleSurfaceLayerIds());
            reportSnapshotHideHydronicHeatings = !options.includeHydronicHeating();
            reportSnapshotVisibleHydronicSurfacePositions = Set.copyOf(options.visibleHydronicSurfacePositions());
            reportSnapshotFilterHeatingRoomObjects = options.filterHeatingRoomObjects();
            reportSnapshotVisibleHeatingObjectTypes = Set.copyOf(options.visibleHeatingObjectTypes());
            reportSnapshotInteriorRoomDimensionsOnly = options.interiorRoomDimensionsOnly();
            showDimensions.set(options.includeDimensions());
            showAreaVolume.set(options.includeAreaVolume());
            showHeatingCircuits.set(options.includeHydronicHeating());
            showVariothermCircles.set(true);
            activeWorkspaceMode.set(WorkspaceMode.TWO_D);
            updateWorkspaceMode();
            activeView.set(ViewOrientation.TOP);
            activeLevel.set(resolveLevelForReport(levelName));
            clearSelection();
            if (focusPoints == null || focusPoints.isEmpty()) {
                fitCurrentReportViewToContent();
            } else {
                fitPlanViewToPoints(focusPoints, paddingMillimeters);
            }
            render();
            return reportCanvasSnapshot(renderScale);
        } finally {
            activeLevel.set(previousLevel);
            activeView.set(previousView);
            activeWorkspaceMode.set(previousWorkspace);
            zoom = previousZoom;
            offsetX = previousOffsetX;
            offsetY = previousOffsetY;
            selectedSelections.clear();
            selectedSelections.addAll(previousSelections);
            selectedSelection.set(previousPrimarySelection);
            reportSnapshotRestrictSurfaceLayers = previousRestrictSurfaceLayers;
            reportSnapshotVisibleSurfaceLayerIds = previousVisibleSurfaceLayerIds;
            reportSnapshotHideHydronicHeatings = previousHideHydronicHeatings;
            reportSnapshotVisibleHydronicSurfacePositions = previousVisibleHydronicSurfacePositions;
            reportSnapshotFilterHeatingRoomObjects = previousFilterHeatingRoomObjects;
            reportSnapshotVisibleHeatingObjectTypes = previousVisibleHeatingObjectTypes;
            reportSnapshotInteriorRoomDimensionsOnly = previousInteriorRoomDimensionsOnly;
            showDimensions.set(previousShowDimensions);
            showAreaVolume.set(previousShowAreaVolume);
            showHeatingCircuits.set(previousShowHeatingCircuits);
            showVariothermCircles.set(previousShowVariothermCircles);
            updateWorkspaceMode();
            restoreCenterDividerPositions(previousDividerPositions);
            render();
        }
    }

    double[] currentCenterDividerPositions() {
        return getCenter() instanceof SplitPane splitPane
                ? splitPane.getDividerPositions()
                : new double[0];
    }

    void restoreCenterDividerPositions(double[] dividerPositions) {
        if (dividerPositions.length == 0 || !(getCenter() instanceof SplitPane splitPane)) {
            return;
        }
        splitPane.setDividerPositions(dividerPositions);
    }

    WritableImage reportCanvasSnapshot(double renderScale) {
        if (renderScale <= 1.0) {
            return automationDrawingSnapshot();
        }
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setTransform(Transform.scale(renderScale, renderScale));
        return drawingCanvas.snapshot(parameters, null);
    }

    Level resolveLevelForReport(String levelName) {
        return availableLevels.stream()
                .filter(level -> level.name().equals(levelName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Etage `" + levelName + "` ist unbekannt."));
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

    public void automationSetShowVariothermCircles(boolean visible) {
        showVariothermCircles.set(visible);
    }

    public void automationSetShowHeatingCircuits(boolean visible) {
        showHeatingCircuits.set(visible);
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

    public void automationTriggerGlobalKey(KeyCode keyCode) {
        handleGlobalShortcuts(new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                keyCode,
                false,
                false,
                false,
                false
        ));
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

    public void automationSetSurfaceLayoutDirection(String directionName) {
        SurfaceLayoutDirection direction = SurfaceLayoutDirection.valueOf(directionName.trim().toUpperCase(Locale.ROOT));
        surfaceLayoutDirectionSelector.setValue(direction);
        render();
    }

    public String automationSurfaceLayoutDirection() {
        return Optional.ofNullable(surfaceLayoutDirectionSelector.getValue())
                .map(Enum::name)
                .orElse("");
    }

    public String automationSurfaceLayoutCornerLabel() {
        return surfaceLayoutCornerLabel.getText();
    }

    public void automationSelect(String kindName, int index, boolean toggle) {
        SelectionKey selectionKey = switch (kindName.trim().toUpperCase(Locale.ROOT)) {
            case "WALL" -> selectionKeyByIndex(activeLevel.get().walls(), index, RenderableKind.WALL, Wall::id, "Wandindex");
            case "ROOM", "ROOM_VOLUME" -> selectionKeyByIndex(activeLevel.get().rooms(), index, RenderableKind.ROOM_VOLUME, Room::id, "Raumindex");
            case "DOOR" -> selectionKeyByIndex(activeLevel.get().doors(), index, RenderableKind.DOOR, Door::id, "Türindex");
            case "WINDOW" -> selectionKeyByIndex(activeLevel.get().windows(), index, RenderableKind.WINDOW, WindowElement::id, "Fensterindex");
            case "STAIR" -> selectionKeyByIndex(activeLevel.get().staircases(), index, RenderableKind.STAIR, Staircase::id, "Treppenindex");
            case "OBJECT", "ROOM_OBJECT" -> selectionKeyByIndex(activeLevel.get().roomObjects(), index, RenderableKind.ROOM_OBJECT, RoomObject::id, "Objektindex");
            case "FLOOR_EXTENSION", "BALCONY", "GALLERY" -> selectionKeyByIndex(
                    activeLevel.get().floorExtensions(), index, RenderableKind.FLOOR_EXTENSION, FloorExtension::id, "Balkon-/Emporenindex"
            );
            case "FLOOR_OPENING" -> selectionKeyByIndex(
                    activeLevel.get().floorOpenings(), index, RenderableKind.FLOOR_OPENING, FloorOpening::id, "Bodenöffnungsindex"
            );
            case "HEATING_EXCLUSION", "HEATING_EXCLUSION_AREA" -> selectionKeyByIndex(
                    activeLevel.get().heatingExclusionAreas(), index, RenderableKind.HEATING_EXCLUSION, HeatingExclusionArea::id, "FBH-Sperrflächenindex"
            );
            case "HEATING_MANIFOLD", "HKV" -> selectionKeyByIndex(
                    activeLevel.get().hydronicHeatings(), index, RenderableKind.HEATING_MANIFOLD, HydronicHeating::id, "HKV-Index"
            );
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
        if ("roomObjectHeatingType".equals(fieldName)) {
            roomObjectHeatingTypeSelector.setValue(RoomObjectHeatingType.fromUserInput(value));
            updatePropertySectionVisibility();
            render();
            return;
        }
        textFieldByName(fieldName).setText(value);
        updatePropertySectionVisibility();
        render();
    }

    public void automationSetHeatingRoutingCommandAreaText(String text) {
        replaceTextPreservingCaretAndScroll(heatingRoutingCommandArea, text);
        render();
    }

    public void automationSetHeatingZonePointAreaText(String text) {
        heatingZonePointArea.setText(Optional.ofNullable(text).orElse(""));
        render();
    }

    public void automationSetHeatingRoutingCommandAreaCaretPosition(int position) {
        heatingRoutingCommandArea.positionCaret(Math.max(0, Math.min(position, heatingRoutingCommandArea.getLength())));
        render();
    }

    public void automationSetHeatingRoutingCommandAreaScrollTop(double scrollTop) {
        heatingRoutingCommandArea.setScrollTop(scrollTop);
        render();
    }

    public int automationHeatingRoutingCommandAreaCaretPosition() {
        return heatingRoutingCommandArea.getCaretPosition();
    }

    public double automationHeatingRoutingCommandAreaScrollTop() {
        return heatingRoutingCommandArea.getScrollTop();
    }

    public String automationHeatingRoutingCommandAreaText() {
        return heatingRoutingCommandArea.getText();
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

    public void automationSetErrorDialogsEnabled(boolean enabled) {
        interactiveDialogsEnabled = enabled;
    }

    public void automationClearLastError() {
        lastErrorDialog = UiErrorDialogs.ErrorPresentation.empty();
    }

    public void automationClearLastWarning() {
        lastWarningDialog = WarningPresentation.empty();
        rememberedWarningCount = 0;
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

    public String automationLastWarningTitle() {
        return lastWarningDialog.title();
    }

    public String automationLastWarningHeader() {
        return lastWarningDialog.header();
    }

    public String automationLastWarningContent() {
        return lastWarningDialog.content();
    }

    public int automationWarningCount() {
        return rememberedWarningCount;
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

    KeyEvent shortcutEvent(KeyCode keyCode, boolean shortcutDown, boolean shiftDown) {
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
            case "applyHeatingZoneSettings" -> applySelectedHeatingZoneSettings();
            case "applyHeatingRouting" -> applySelectedHeatingZoneRouting();
            case "generateHeatingRouting" -> generateSelectedHeatingZoneRouting();
            case "recognizeRoomFromSelectedWalls" -> recognizeRoomFromSelectedWalls();
            case "toggleSurfaceLayerVisibility" -> toggleSurfaceLayerVisibility();
            case "addSurfaceLayer" -> addSurfaceLayer();
            case "updateSurfaceLayer" -> updateSurfaceLayer();
            case "surfaceLayoutCornerPrevious" -> cycleSurfaceLayoutCorner(false);
            case "surfaceLayoutCornerNext" -> cycleSurfaceLayoutCorner(true);
            case "editTerrainElevations" -> editTerrainElevations();
            case "rotateSelectedComponentsClockwise", "rotateSelectedStairsClockwise" -> rotateSelectedComponentsClockwise();
            case "rotateSelectedComponentsCounterClockwise", "rotateSelectedStairsCounterClockwise" -> rotateSelectedComponentsCounterClockwise();
            case "mirrorSelectedHeatingZonesHorizontally" -> mirrorSelectedHeatingZones(true);
            case "mirrorSelectedHeatingZonesVertically" -> mirrorSelectedHeatingZones(false);
            case "exportProjectDxf" -> exportProjectAsDxf(requirePath(path, actionName));
            case "importProjectDxf" -> importProjectFromDxf(requirePath(path, actionName));
            case "exportLevelDxf" -> exportCurrentLevel(requirePath(path, actionName));
            case "importLevelDxf" -> importLevel(requirePath(path, actionName));
            case "exportSurfaceMaterialReportMarkdown" -> documentSupport.exportSurfaceMaterialReportMarkdown(requirePath(path, actionName));
            case "exportSurfaceMaterialReportPdf" -> documentSupport.exportSurfaceMaterialReportPdf(requirePath(path, actionName));
            case "importPartLibrary" -> importPartLibrary(requirePath(path, actionName));
            case "exportWorkbenchSnapshot" -> exportWorkbenchSnapshot(requirePath(path, actionName));
            case "exportThreeDSnapshot" -> runPreparedThreeDAction(false, () -> threeDViewport.exportSnapshot(requirePath(path, actionName)));
            case "exportSubSceneSnapshot" -> runPreparedThreeDAction(false, () -> threeDViewport.exportSubSceneSnapshot(requirePath(path, actionName)));
            case "threeDOrbitLeft" -> runPreparedThreeDAction(false, () -> threeDViewport.automationOrbit(-15.0, 0.0));
            case "threeDOrbitRight" -> runPreparedThreeDAction(false, () -> threeDViewport.automationOrbit(15.0, 0.0));
            case "threeDOrbitUp" -> runPreparedThreeDAction(false, () -> threeDViewport.automationOrbit(0.0, 8.0));
            case "threeDOrbitDown" -> runPreparedThreeDAction(false, () -> threeDViewport.automationOrbit(0.0, -8.0));
            case "threeDPanRight" -> runPreparedThreeDAction(false, () -> threeDViewport.automationPan(90.0, 0.0));
            case "threeDPanLeft" -> runPreparedThreeDAction(false, () -> threeDViewport.automationPan(-90.0, 0.0));
            case "threeDPanUp" -> runPreparedThreeDAction(false, () -> threeDViewport.automationPan(0.0, -60.0));
            case "threeDPanDown" -> runPreparedThreeDAction(false, () -> threeDViewport.automationPan(0.0, 60.0));
            case "threeDZoomIn" -> runPreparedThreeDAction(false, () -> threeDViewport.automationZoom(0.92));
            case "threeDZoomOut" -> runPreparedThreeDAction(false, () -> threeDViewport.automationZoom(1.08));
            case "threeDFit" -> runPreparedThreeDAction(true, threeDViewport::automationFitToScene);
            case "threeDReset" -> runPreparedThreeDAction(true, threeDViewport::resetToDefaultView);
            case "threeDViewportReset" -> runPreparedThreeDViewportAction(threeDViewport::resetToDefaultView);
            case "diagnose3D" -> {
                runPreparedThreeDAction(false, () -> {
                });
                result = automationSnapshot();
                // Diagnose wird in die cameraStatus-Info-Zeile geschrieben, damit sie
                // vom Automation-Snapshot zurückgegeben werden kann, ohne neue Felder anzulegen.
                String diagnose = threeDViewport.diagnoseRenderState();
                draftLabel.setText("DIAGNOSE: " + diagnose);
            }
            case "setProjection3D" -> runPreparedThreeDAction(true, () -> {
                String mode = path != null ? path.toString() : "ORTHOGRAPHIC";
                threeDViewport.setProjectionMode(de.schrell.cadas.application.view.ProjectionMode.valueOf(mode));
            });
            case "exit" -> requestApplicationExit();
            case "clearProject" -> clearProjectWithoutDialog();
            default -> throw new IllegalArgumentException("Automatisierungsaktion `" + actionName + "` ist unbekannt.");
        }
        return result;
    }

    <T> SelectionKey selectionKeyByIndex(
            List<T> elements,
            int index,
            RenderableKind kind,
            Function<T, UUID> idExtractor,
            String errorLabel
    ) {
        return elements.stream()
                .skip(index)
                .findFirst()
                .map(element -> new SelectionKey(kind, activeLevel.get().name(), idExtractor.apply(element).toString()))
                .orElseThrow(() -> new IllegalArgumentException(errorLabel + " `" + index + "` ist ungültig."));
    }

    void runPreparedThreeDAction(boolean forceThreeDWorkspace, Runnable action) {
        if (forceThreeDWorkspace) {
            activeWorkspaceMode.set(WorkspaceMode.THREE_D);
        } else {
            activateThreeDWorkspaceForSnapshot();
        }
        updateWorkspaceMode();
        refreshThreeDIfNeeded();
        action.run();
    }

    void runPreparedThreeDViewportAction(Runnable action) {
        refreshThreeDIfNeeded();
        action.run();
    }

    void activateThreeDWorkspaceForSnapshot() {
        if (activeWorkspaceMode.get() == WorkspaceMode.TWO_D) {
            activeWorkspaceMode.set(WorkspaceMode.THREE_D);
        }
    }

    void exportWorkbenchSnapshot(Path path) {
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

    String automationSelectedRoomMetrics() {
        return selectedSurfaceRoom()
                .map(this::roomMetricsText)
                .orElse("");
    }

    public void automationSetStatusText(String text) {
        draftLabel.setText(text);
    }

    void clearProjectWithoutDialog() {
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

    Path requirePath(Path path, String actionName) {
        if (path == null) {
            throw new IllegalArgumentException("Für `" + actionName + "` wird ein Parameter `path` benötigt.");
        }
        return path;
    }

    TextField textFieldByName(String fieldName) {
        return uiMember(fieldName + "Field", TextField.class, "Eingabefeld");
    }

    ComboBox<LengthUnit> unitSelectorByName(String fieldName) {
        return (ComboBox<LengthUnit>) uiMember(fieldName + "Unit", ComboBox.class, "Einheitenselektor");
    }

    <T> T uiMember(String memberName, Class<T> expectedType, String label) {
        try {
            Object value = findUiMemberField(memberName).get(this);
            if (!expectedType.isInstance(value)) {
                throw new IllegalArgumentException(label + " `" + memberName + "` hat einen unerwarteten Typ.");
            }
            return expectedType.cast(value);
        } catch (NoSuchFieldException exception) {
            throw new IllegalArgumentException(label + " `" + memberName.replaceAll("(Field|Unit)$", "") + "` ist unbekannt.", exception);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(label + " `" + memberName + "` konnte nicht gelesen werden.", exception);
        }
    }

    java.lang.reflect.Field findUiMemberField(String memberName) throws NoSuchFieldException {
        Class<?> currentType = getClass();
        while (currentType != null) {
            try {
                return currentType.getDeclaredField(memberName);
            } catch (NoSuchFieldException ignored) {
                currentType = currentType.getSuperclass();
            }
        }
        throw new NoSuchFieldException(memberName);
    }

    void ensureCanvasReady() {
        if (drawingCanvas.getWidth() <= 0 || drawingCanvas.getHeight() <= 0) {
            resizeCanvases();
        }
    }

    boolean isRotatableSelection(SelectionKey selectionKey) {
        return selectionKey.kind() == RenderableKind.WALL
                || selectionKey.kind() == RenderableKind.STAIR
                || selectionKey.kind() == RenderableKind.ROOM_OBJECT
                || selectionKey.kind() == RenderableKind.HEATING_ZONE
                || selectionKey.kind() == RenderableKind.HEATING_MANIFOLD
                || selectionKey.kind() == RenderableKind.FLOOR_OPENING;
    }

    boolean isTranslatableSelection(SelectionKey selectionKey) {
        return selectionKey.kind() == RenderableKind.WALL
                || selectionKey.kind() == RenderableKind.STAIR
                || selectionKey.kind() == RenderableKind.ROOM_OBJECT
                || selectionKey.kind() == RenderableKind.FLOOR_OPENING
                || selectionKey.kind() == RenderableKind.HEATING_ZONE
                || selectionKey.kind() == RenderableKind.HEATING_MANIFOLD
                || selectionKey.kind() == RenderableKind.HEATING_EXCLUSION;
    }

    MouseEvent mouseEvent(javafx.event.EventType<MouseEvent> type,
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
