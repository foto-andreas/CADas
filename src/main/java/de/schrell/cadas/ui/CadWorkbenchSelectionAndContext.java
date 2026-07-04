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

abstract class CadWorkbenchSelectionAndContext extends CadWorkbenchSurfaceLayers {

    void resetTwoDView() {
        fitCurrentViewToContent();
        render();
    }

    void fitCurrentViewToContent() {
        fitCurrentViewToContent(
                projectionService.isPlanView(activeView.get()) ? 80.0 : 64.0,
                projectionService.isPlanView(activeView.get()) ? 96.0 : 72.0
        );
    }

    void fitCurrentViewToContent(double horizontalPadding, double verticalPadding) {
        double viewportWidth = Math.max(drawingPane.getWidth(), 640.0);
        double viewportHeight = Math.max(drawingPane.getHeight(), 420.0);
        projectedBoundsService.bounds(activeLevel.get(), activeView.get()).ifPresentOrElse(bounds -> {
            fitViewportToBounds(
                    viewportWidth,
                    viewportHeight,
                    bounds.widthMillimeters(),
                    bounds.heightMillimeters(),
                    bounds.centerHorizontalMillimeters(),
                    bounds.centerVerticalMillimeters(),
                    horizontalPadding,
                    verticalPadding
            );
        }, () -> {
            zoom = 1.0;
            offsetX = viewportWidth / 2.0;
            offsetY = viewportHeight / 2.0;
        });
    }

    void fitCurrentReportViewToContent() {
        if (projectionService.isPlanView(activeView.get()) && showDimensions.get()) {
            fitCurrentViewToContent(220.0, 240.0);
            fitRenderedReportBoundsIntoView();
            return;
        }
        fitCurrentViewToContent();
    }

    void fitRenderedReportBoundsIntoView() {
        double targetPadding = 42.0;
        for (int iteration = 0; iteration < 6; iteration++) {
            render();
            Optional<ScreenBounds> bounds = currentReportScreenBounds();
            if (bounds.isEmpty()) {
                return;
            }
            double viewportWidth = Math.max(drawingCanvas.getWidth(), 640.0);
            double viewportHeight = Math.max(drawingCanvas.getHeight(), 420.0);
            ScreenBounds visibleBounds = bounds.orElseThrow();
            double availableWidth = Math.max(220.0, viewportWidth - targetPadding * 2.0);
            double availableHeight = Math.max(180.0, viewportHeight - targetPadding * 2.0);
            double fitFactor = Math.min(availableWidth / Math.max(1.0, visibleBounds.width()), availableHeight / Math.max(1.0, visibleBounds.height()));
            if (fitFactor < 0.995) {
                scaleViewAroundViewportCenter(fitFactor, viewportWidth, viewportHeight);
                continue;
            }
            double shiftX = viewportShift(visibleBounds.minX(), visibleBounds.maxX(), targetPadding, viewportWidth - targetPadding);
            double shiftY = viewportShift(visibleBounds.minY(), visibleBounds.maxY(), targetPadding, viewportHeight - targetPadding);
            if (Math.abs(shiftX) <= 0.5 && Math.abs(shiftY) <= 0.5) {
                return;
            }
            offsetX += shiftX;
            offsetY += shiftY;
        }
    }

    Optional<ScreenBounds> currentReportScreenBounds() {
        Optional<ScreenBounds> contentBounds = projectedBoundsService.bounds(activeLevel.get(), activeView.get())
                .map(bounds -> new ScreenBounds(
                        toScreenHorizontal(bounds.minHorizontalMillimeters()),
                        toScreenHorizontal(bounds.maxHorizontalMillimeters()),
                        toScreenVertical(bounds.minVerticalMillimeters()),
                        toScreenVertical(bounds.maxVerticalMillimeters())
                ));
        return Optional.ofNullable(ScreenBounds.union(contentBounds.orElse(null), lastPlanDimensionScreenBounds));
    }

    void scaleViewAroundViewportCenter(double factor, double viewportWidth, double viewportHeight) {
        if (factor <= 0.0 || Math.abs(zoom) <= 0.001) {
            return;
        }
        double oldZoom = zoom;
        double newZoom = twoDZoomRange.clamp(zoom * factor);
        double effectiveFactor = newZoom / oldZoom;
        double centerX = viewportWidth / 2.0;
        double centerY = viewportHeight / 2.0;
        zoom = newZoom;
        offsetX = centerX + (offsetX - centerX) * effectiveFactor;
        offsetY = centerY + (offsetY - centerY) * effectiveFactor;
    }

    double viewportShift(double min, double max, double visibleMin, double visibleMax) {
        double shift = 0.0;
        if (min < visibleMin) {
            shift += visibleMin - min;
        }
        if (max + shift > visibleMax) {
            shift -= max + shift - visibleMax;
        }
        return shift;
    }

    void fitPlanViewToPoints(List<PlanPoint> points, double paddingMillimeters) {
        if (points.isEmpty()) {
            fitCurrentViewToContent();
            return;
        }
        double minX = points.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0) - paddingMillimeters;
        double maxX = points.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0) + paddingMillimeters;
        double minY = points.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0) - paddingMillimeters;
        double maxY = points.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0) + paddingMillimeters;
        double viewportWidth = Math.max(drawingPane.getWidth(), 640.0);
        double viewportHeight = Math.max(drawingPane.getHeight(), 420.0);
        fitViewportToBounds(
                viewportWidth,
                viewportHeight,
                maxX - minX,
                maxY - minY,
                (minX + maxX) / 2.0,
                (minY + maxY) / 2.0,
                80.0,
                96.0
        );
    }

    void fitViewportToBounds(
            double viewportWidth,
            double viewportHeight,
            double contentWidthMillimeters,
            double contentHeightMillimeters,
            double centerHorizontalMillimeters,
            double centerVerticalMillimeters,
            double horizontalPadding,
            double verticalPadding
    ) {
        double contentWidth = Math.max(contentWidthMillimeters, 1_000.0);
        double contentHeight = Math.max(contentHeightMillimeters, 1_000.0);
        double availableWidth = Math.max(220.0, viewportWidth - horizontalPadding);
        double availableHeight = Math.max(180.0, viewportHeight - verticalPadding);
        double fitScale = Math.min(availableWidth / contentWidth, availableHeight / contentHeight);
        zoom = twoDZoomRange.clamp(fitScale / BASE_PIXELS_PER_MILLIMETER);
        offsetX = viewportWidth / 2.0 - centerHorizontalMillimeters * scale();
        offsetY = viewportHeight / 2.0 - centerVerticalMillimeters * scale();
    }

    void clearProject() {
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
        syncNorthAngleFieldFromProject();
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

    void undo() {
        history.undo(captureSnapshot())
                .ifPresentOrElse(snapshot -> {
                    restoreSnapshot(snapshot);
                    draftLabel.setText("Letzte Änderung rückgängig gemacht.");
                }, () -> draftLabel.setText("Kein weiterer Schritt zum Rückgängigmachen vorhanden."));
        updateActionButtons();
    }

    void redo() {
        history.redo(captureSnapshot())
                .ifPresentOrElse(snapshot -> {
                    restoreSnapshot(snapshot);
                    draftLabel.setText("Änderung wiederhergestellt.");
                }, () -> draftLabel.setText("Kein Schritt zum Wiederherstellen vorhanden."));
        updateActionButtons();
    }

    void rememberStateForUndo() {
        history.remember(captureSnapshot());
        currentChangeRevision = nextChangeRevision++;
        applicationExitConfirmed = false;
        updateActionButtons();
    }

    WorkbenchSnapshot captureSnapshot() {
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

    void restoreSnapshot(WorkbenchSnapshot snapshot) {
        project.replaceWith(snapshot.project());
        syncNorthAngleFieldFromProject();
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

    void clearSelection() {
        clearSelectionsInternal();
        selectedEndpointGroup = null;
        selectionDragAnchor = null;
        clearSelectionRectangle();
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

    void deleteSelection() {
        if (selectedSelections.isEmpty()) {
            return;
        }
        if (!confirmSelectionDeletion()) {
            draftLabel.setText("Löschen abgebrochen.");
            return;
        }
        rememberStateForUndo();
        boolean removed = false;
        boolean synchronizeRoomsAfterRemoval = false;
        for (SelectionKey selectionKey : List.copyOf(selectedSelections)) {
            UUID id = UUID.fromString(selectionKey.elementId());
            removed |= switch (selectionKey.kind()) {
                case WALL -> {
                    synchronizeRoomsAfterRemoval = true;
                    yield activeLevel.get().removeWall(id);
                }
                case ROOM_VOLUME, ROOM_FLOOR, ROOM_CEILING -> false;
                case DOOR -> activeLevel.get().removeDoor(id);
                case WINDOW -> activeLevel.get().removeWindow(id);
                case ROOF_WINDOW -> activeLevel.get().removeRoofWindow(id);
                case STAIR -> {
                    synchronizeRoomsAfterRemoval = true;
                    yield removeStaircaseWithUnderbuild(id);
                }
                case ROOM_OBJECT -> activeLevel.get().removeRoomObject(id);
                case FLOOR_EXTENSION -> activeLevel.get().removeFloorExtension(id);
                case FLOOR_OPENING -> activeLevel.get().removeFloorOpening(id);
                case HEATING_ZONE -> removeHeatingZoneById(id);
                case HEATING_MANIFOLD -> resetHydronicManifoldById(id);
                case HEATING_EXCLUSION -> activeLevel.get().removeHeatingExclusionArea(id);
                default -> false;
            };
        }
        if (removed) {
            if (synchronizeRoomsAfterRemoval) {
                synchronizeRoomsFromWalls(activeLevel.get());
            }
            clearSelectionsInternal();
            markThreeDDirty();
            clearHeatingLayoutCache();
            draftLabel.setText("Ausgewählte Bauteile gelöscht.");
            render();
            return;
        }
        draftLabel.setText("Auswahl konnte nicht gelöscht werden.");
        updateActionButtons();
    }

    boolean confirmSelectionDeletion() {
        if (!interactiveDialogsEnabled) {
            return true;
        }
        long deletableCount = selectedSelections.stream().filter(this::isDeletableSelection).count();
        String header = deletableCount == 1
                ? "Ausgewähltes Bauteil wirklich löschen?"
                : "Ausgewählte Bauteile wirklich löschen?";
        String content = deletableCount <= 1
                ? "Das ausgewählte Bauteil wird entfernt. Dieser Schritt kann über Rückgängig wiederhergestellt werden, solange der Verlauf erhalten bleibt."
                : "Die ausgewählten Bauteile werden entfernt. Dieser Schritt kann über Rückgängig wiederhergestellt werden, solange der Verlauf erhalten bleibt.";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle("Auswahl löschen");
        alert.setHeaderText(header);
        alert.getDialogPane().setPrefWidth(520);
        Window owner = currentWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        applyTooltip(alert.getDialogPane().lookupButton(ButtonType.OK),
                "Bestätigt das Löschen der aktuell ausgewählten Bauteile. Der Schritt bleibt über Rückgängig wiederherstellbar, solange der Verlauf verfügbar ist.");
        applyTooltip(alert.getDialogPane().lookupButton(ButtonType.CANCEL),
                "Bricht das Löschen ab und belässt alle ausgewählten Bauteile unverändert im Plan.");
        return alert.showAndWait()
                .filter(ButtonType.OK::equals)
                .isPresent();
    }

    boolean isDeletableSelection(SelectionKey selection) {
        return selection.kind() != RenderableKind.ROOM_VOLUME
                && selection.kind() != RenderableKind.ROOM_FLOOR
                && selection.kind() != RenderableKind.ROOM_CEILING;
    }

    void applySelectionRectangle() {
        List<SelectionKey> containedSelections = selectionQueryService.findSelectionsWithin(
                activeLevel.get(),
                selectionRectangleStart,
                selectionRectangleEnd
        );
        if (selectionRectangleToggle) {
            for (SelectionKey selection : containedSelections) {
                if (!selectedSelections.add(selection)) {
                    selectedSelections.remove(selection);
                }
            }
            selectedSelection.set(selectedSelections.stream().reduce((first, second) -> second).orElse(null));
        } else {
            selectedSelections.clear();
            selectedSelections.addAll(containedSelections);
            selectedSelection.set(containedSelections.isEmpty() ? null : containedSelections.getLast());
        }
        clearSelectionRectangle();
        syncSelectionState();
        if (selectedSelections.isEmpty()) {
            draftLabel.setText("Keine vollständig enthaltenen Bauteile im Auswahlrahmen gefunden.");
            return;
        }
        draftLabel.setText(selectedSelections.size() + " Bauteile per Auswahlrahmen markiert.");
    }

    boolean hasSelectionRectangleArea() {
        if (selectionRectangleStart == null || selectionRectangleEnd == null) {
            return false;
        }
        double widthPixels = Math.abs(selectionRectangleEnd.xMillimeters() - selectionRectangleStart.xMillimeters()) * scale();
        double heightPixels = Math.abs(selectionRectangleEnd.yMillimeters() - selectionRectangleStart.yMillimeters()) * scale();
        return widthPixels >= 4.0 && heightPixels >= 4.0;
    }

    void clearSelectionRectangle() {
        selectionRectangleStart = null;
        selectionRectangleEnd = null;
        selectionRectangleToggle = false;
    }

    boolean removeStaircaseWithUnderbuild(UUID staircaseId) {
        StairUnderbuildService.UnderbuildResult result = stairUnderbuildService.remove(activeLevel.get(), staircaseId);
        activeLevel.get().replaceWalls(result.walls());
        activeLevel.get().replaceDoors(result.doors());
        activeLevel.get().replaceWindows(result.windows());
        return activeLevel.get().removeStaircase(staircaseId);
    }

    void updateSelection(SelectionKey selectionKey, boolean toggleSelection) {
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

    void selectSingle(SelectionKey selectionKey) {
        selectedSelections.clear();
        if (selectionKey != null) {
            selectedSelections.add(selectionKey);
        }
        selectedSelection.set(selectionKey);
        syncSelectionState();
    }

    void clearSelectionsInternal() {
        selectedSelections.clear();
        selectedSelection.set(null);
        syncSelectionState();
    }

    void syncSelectionState() {
        threeDViewport.setSelectedSelections(Set.copyOf(selectedSelections));
        rebuildSelectionContextMenu();
        updatePropertySectionVisibility();
        updateActionButtons();
        updateMouseCursor();
    }

    void rebuildSelectionContextMenu() {
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
                    menuItem(
                            "Innenansicht ab diesem Standort öffnen",
                            this::openInteriorViewFromContextLocation,
                            null
                    )
            );
        }
        contextHeatingZoneContext().ifPresent(context -> {
            HeatingLayoutPattern targetPattern = context.zone().layoutPattern() == HeatingLayoutPattern.MEANDER
                    ? HeatingLayoutPattern.VARIO
                    : HeatingLayoutPattern.MEANDER;
            selectionContextMenu.getItems().addAll(
                    menuItem("Verlegung auf " + targetPattern + " umschalten", () -> setContextHeatingZonePattern(targetPattern), null),
                    menuItem("Routing neu generieren", this::generateSelectedHeatingZoneRouting, null),
                    menuItem("Vorlauf/Rücklauf im Heizkreis tauschen", this::invertContextHeatingZone, null),
                    menuItem("Heizkreis horizontal spiegeln", () -> mirrorSelectedHeatingZones(true), null),
                    menuItem("Heizkreis vertikal spiegeln", () -> mirrorSelectedHeatingZones(false), null),
                    menuItem("Vorlauf hier am Rand setzen", this::setContextHeatingZoneSupplyConnection, null),
                    menuItem("Rücklauf hier am Rand setzen", this::setContextHeatingZoneReturnConnection, null)
            );
            if (mergeableHeatingZone(context).isPresent()) {
                selectionContextMenu.getItems().add(menuItem("Mit angrenzendem Heizkreis verbinden", this::mergeContextHeatingZone, null));
            }
        });
        if (selectedSelections.stream().anyMatch(selection -> selection.kind() != RenderableKind.ROOM_VOLUME
                && selection.kind() != RenderableKind.ROOM_FLOOR
                && selection.kind() != RenderableKind.ROOM_CEILING)) {
            selectionContextMenu.getItems().add(menuItem("Auswahl löschen", this::deleteSelection, null));
        }
        if (selectedSurfaceLayerNeedsRepair()) {
            selectionContextMenu.getItems().add(menuItem("Belag-Verlegung reparieren", this::repairSelectedSurfaceLayerLayout, null));
        }
        if (selectedSelections.stream().anyMatch(this::isRotatableSelection)) {
            selectionContextMenu.getItems().addAll(
                    menuItem("Bauteile 90° im Uhrzeigersinn drehen", this::rotateSelectedComponentsClockwise, null),
                    menuItem("Bauteile 90° gegen den Uhrzeigersinn drehen", this::rotateSelectedComponentsCounterClockwise, null)
            );
        }
        if (selectedWalls().size() == 1) {
            selectionContextMenu.getItems().add(menuItem(
                    "Diese Wand als Vorderseite setzen",
                    this::setSelectedWallAsBuildingFront,
                    null
            ));
            selectionContextMenu.getItems().add(menuItem(
                    "Dachschräge aus Wand erzeugen …",
                    this::createRoofSlopeFromSelectedWall,
                    null
            ));
        }
        if (contextWallSplitCandidate().isPresent()) {
            selectionContextMenu.getItems().add(menuItem(
                    "Wand an Kreuzung aufteilen",
                    this::splitSelectedWallsAtContextIntersection,
                    null
            ));
        }
        if (selectedWalls().size() >= 3) {
            selectionContextMenu.getItems().add(menuItem("Raum erkennen", this::recognizeRoomFromSelectedWalls, null));
        }
    }

    Optional<Room> contextMenuRoom() {
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

    void setSelectedWallAsBuildingFront() {
        List<Wall> walls = selectedWalls();
        if (walls.size() != 1) {
            return;
        }
        rememberStateForUndo();
        project.defineFrontAngle(walls.getFirst().axis().angle());
        markThreeDDirty();
        refreshThreeDIfNeeded();
        render();
        draftLabel.setText("Vorderseite auf ausgewählte Wand gesetzt.");
    }

    void setContextHeatingZonePattern(HeatingLayoutPattern pattern) {
        HeatingZoneContext context = contextHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Kein Heizkreis ausgewählt."));
        HeatingZone replacement = heatingCircuitRoutingService.regenerateWithPattern(
                context.zone(), context.heating(), pattern, context.zone().serpentineMiddleLine()
        );
        replaceHeatingZone(context, replacement, "Verlegung für Heizkreis geändert.");
    }

    void invertContextHeatingZone() {
        HeatingZoneContext context = contextHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Kein Heizkreis ausgewählt."));
        HeatingZone inverted = context.zone().withFlowInverted(!context.zone().flowInverted());
        if (inverted.hasRoutingCommands()) {
            inverted = heatingCircuitRoutingService.withRoutingCommands(
                    inverted, context.heating(), inverted.routingCommands(), inverted.serpentineMiddleLine()
            );
        }
        replaceHeatingZone(context, inverted, "Vorlauf und Rücklauf im Heizkreis getauscht.");
    }

    void setContextHeatingZoneSupplyConnection() {
        HeatingZoneContext context = contextHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Kein Heizkreis ausgewählt."));
        replaceHeatingZone(
                context,
                context.zone().withSupplyConnectionPoint(CadWorkbenchHeatingSupport.nearestPointOnHeatingZoneBoundary(context.zone(), contextMenuWorldPoint)),
                "Vorlaufanschluss am Heizkreis gesetzt."
        );
    }

    void setContextHeatingZoneReturnConnection() {
        HeatingZoneContext context = contextHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Kein Heizkreis ausgewählt."));
        replaceHeatingZone(
                context,
                context.zone().withReturnConnectionPoint(CadWorkbenchHeatingSupport.nearestPointOnHeatingZoneBoundary(context.zone(), contextMenuWorldPoint)),
                "Rücklaufanschluss am Heizkreis gesetzt."
        );
    }

    void replaceHeatingZone(HeatingZoneContext context, HeatingZone replacement, String successPrefix) {
        List<HeatingZone> zones = new ArrayList<>(context.heating().zones());
        zones.set(context.zoneIndex(), replacement);
        HydronicHeating updatedHeating = context.heating().withZones(zones);
        rememberStateForUndo();
        activeLevel.get().replaceHydronicHeating(updatedHeating);
        selectSingle(new SelectionKey(RenderableKind.HEATING_ZONE, activeLevel.get().name(), replacement.id().toString()));
        refreshHeatingSection();
        draftLabel.setText(heatingUpdateMessage(updatedHeating, successPrefix));
        recomputeHeatingLayoutNow(context.heating().id());
    }

    Optional<HeatingZone> mergeableHeatingZone(HeatingZoneContext context) {
        CadWorkbenchHeatingSupport.HeatingZoneBounds bounds = CadWorkbenchHeatingSupport.heatingZoneBounds(context.zone().outline());
        return context.heating().zones().stream()
                .filter(zone -> !zone.id().equals(context.zone().id()))
                .filter(zone -> CadWorkbenchHeatingSupport.canMerge(bounds, CadWorkbenchHeatingSupport.heatingZoneBounds(zone.outline())))
                .findFirst();
    }

    void mergeContextHeatingZone() {
        HeatingZoneContext context = contextHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Kein Heizkreis ausgewählt."));
        HeatingZone neighbor = mergeableHeatingZone(context)
                .orElseThrow(() -> new IllegalStateException("Kein exakt angrenzender Rechteck-Heizkreis gefunden."));
        CadWorkbenchHeatingSupport.HeatingZoneBounds mergedBounds = CadWorkbenchHeatingSupport.union(
                CadWorkbenchHeatingSupport.heatingZoneBounds(context.zone().outline()),
                CadWorkbenchHeatingSupport.heatingZoneBounds(neighbor.outline())
        );
        HeatingZone merged = context.zone().withOutline(CadWorkbenchHeatingSupport.rectanglePoints(mergedBounds))
                .withName(context.zone().name() + "+" + neighbor.name());
        List<HeatingZone> zones = context.heating().zones().stream()
                .filter(zone -> !zone.id().equals(neighbor.id()))
                .map(zone -> zone.id().equals(context.zone().id()) ? merged : zone)
                .toList();
        HydronicHeating updatedHeating = context.heating().withZones(zones);
        rememberStateForUndo();
        activeLevel.get().replaceHydronicHeating(updatedHeating);
        selectSingle(new SelectionKey(RenderableKind.HEATING_ZONE, activeLevel.get().name(), merged.id().toString()));
        refreshHeatingSection();
        draftLabel.setText(heatingUpdateMessage(updatedHeating, "Angrenzende Heizkreise verbunden."));
        recomputeHeatingLayoutNow(context.heating().id());
    }

    void createRoofSlopeFromSelectedWall() {
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

    Optional<WallIntersectionSplitService.SplitCandidate> contextWallSplitCandidate() {
        if (contextMenuWorldPoint == null) {
            return Optional.empty();
        }
        return wallIntersectionSplitService.findCandidate(
                activeLevel.get(),
                contextWallSplitWallIds(),
                contextMenuWorldPoint,
                SNAP_TOLERANCE
        );
    }

    void splitSelectedWallsAtContextIntersection() {
        List<UUID> wallIds = contextWallSplitWallIds();
        if (contextMenuWorldPoint == null || wallIds.isEmpty()) {
            draftLabel.setText("Für die Wandteilung braucht es am Kontextpunkt eine aufteilbare Wandkreuzung.");
            return;
        }
        WallIntersectionSplitService.SplitResult result = wallIntersectionSplitService.split(
                activeLevel.get(),
                wallIds,
                contextMenuWorldPoint,
                SNAP_TOLERANCE
        );
        rememberStateForUndo();
        activeLevel.get().replaceWalls(result.walls());
        activeLevel.get().replaceDoors(result.doors());
        activeLevel.get().replaceWindows(result.windows());
        activeLevel.get().replaceSurfaceLayerStacks(result.surfaceLayerStacks());
        synchronizeRoomsFromWalls(activeLevel.get());
        markThreeDDirty();
        draftLabel.setText(result.splits().size() == 1
                ? "Wand an der Kreuzung aufgeteilt."
                : "Wände an der Kreuzung aufgeteilt.");
        render();
    }

    List<UUID> contextWallSplitWallIds() {
        if (contextMenuWorldPoint == null) {
            return List.of();
        }
        LinkedHashSet<UUID> wallIds = selectedWalls().stream()
                .map(Wall::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        selectionQueryService.findSelections(activeLevel.get(), contextMenuWorldPoint, SNAP_TOLERANCE).stream()
                .filter(selection -> selection.kind() == RenderableKind.WALL)
                .map(selection -> UUID.fromString(selection.elementId()))
                .forEach(wallIds::add);
        return List.copyOf(wallIds);
    }

    void openInteriorViewFromContextLocation() {
        Optional<Room> room = contextMenuRoom();
        if (room.isEmpty() || contextMenuWorldPoint == null) {
            draftLabel.setText("Innenansicht braucht einen Raum und einen Standort.");
            return;
        }
        threeDViewport.activateInteriorView(project, activeLevel.get(), room.orElseThrow(), contextMenuWorldPoint);
        activeWorkspaceMode.set(WorkspaceMode.INTERIOR);
        draftLabel.setText("Innenansicht am gewählten Raumstandort geöffnet.");
    }

    void renameContextRoom() {
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

    void renameRoom(UUID roomId, String newName) {
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

    void recognizeRoomFromSelectedWalls() {
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

    void syncInputsFromPrimarySelection() {
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
                        roomObjectHeatOutputField.setText(formatNonNegativeDouble(roomObject.heatOutputWatts(), 1));
                        roomObjectHeatingTypeSelector.setValue(roomObject.heatingType());
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

    void applyCurrentInputsToSelection() {
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
                            currentRoomObjectBaseElevation(),
                            currentRoomObjectHeatingType(roomObject.heatingType()),
                            currentRoomObjectHeatOutputWatts(roomObject.heatOutputWatts())
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

    void applyEndpointHeightToSelection() {
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

    void syncEndpointHeightInputFromSelection() {
        selectedEndpointHeight().ifPresent(height -> syncLengthInput(endpointHeightField, endpointHeightUnit, height, LengthUnit.CENTIMETER));
    }

    Optional<Length> selectedEndpointHeight() {
        if (selectedEndpointGroup == null) {
            return Optional.empty();
        }
        return activeLevel.get().walls().stream()
                .filter(wall -> selectedEndpointGroup.startWallIds().contains(wall.id()) || selectedEndpointGroup.endWallIds().contains(wall.id()))
                .findFirst()
                .map(wall -> selectedEndpointGroup.startWallIds().contains(wall.id()) ? wall.startHeight() : wall.endHeight());
    }

    Set<String> selectedIds() {
        return selectedSelections.stream()
                .map(SelectionKey::elementId)
                .collect(java.util.stream.Collectors.toSet());
    }

    String formatValue(Length length, LengthUnit unit, int decimals) {
        return length.format(unit, decimals)
                .replace(" " + unit.symbol(), "")
                .replace('.', ',');
    }

    String formatNonNegativeDouble(double value, int decimals) {
        String formatted = String.format(Locale.GERMAN, "%." + decimals + "f", value);
        return formatted.replaceAll(",0+$", "").replaceAll("(,\\d*?)0+$", "$1");
    }

    void setLengthInput(TextField field, ComboBox<LengthUnit> unitSelector, Length length, LengthUnit unit) {
        updatingLengthInput = true;
        try {
            unitSelector.setValue(unit);
            field.setText(formatValue(length, unit, LENGTH_INPUT_DECIMALS));
        } finally {
            updatingLengthInput = false;
        }
    }

    void syncLengthInput(TextField field, ComboBox<LengthUnit> unitSelector, Length length, LengthUnit fallbackUnit) {
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

    void rotateSelectedComponentsClockwise() {
        rotateSelectedComponents(true);
    }

    void rotateSelectedComponentsCounterClockwise() {
        rotateSelectedComponents(false);
    }

    void rotateSelectedComponents(boolean clockwise) {
        if (selectedSelections.stream().noneMatch(this::isRotatableSelection)) {
            return;
        }
        rememberStateForUndo();
        QuarterTurnRotationService.RotationResult rotationResult = quarterTurnRotationService.rotate(activeLevel.get(), Set.copyOf(selectedSelections), clockwise);
        activeLevel.get().replaceWalls(rotationResult.walls());
        activeLevel.get().replaceStaircases(rotationResult.staircases());
        activeLevel.get().replaceRoomObjects(rotationResult.roomObjects());
        activeLevel.get().replaceHydronicHeatings(rotationResult.hydronicHeatings());
        rotationResult.staircases().stream()
                .filter(staircase -> selectedIds().contains(staircase.id().toString()))
                .forEach(this::synchronizeStairUnderbuild);
        synchronizeRoomsFromWalls(activeLevel.get());
        markThreeDDirty();
        draftLabel.setText("Ausgewählte Bauteile gedreht.");
        if (rotationResult.hydronicHeatings().stream().anyMatch(heating -> !heating.zones().isEmpty())
                || selectedSelections.stream().anyMatch(selection -> selection.kind() == RenderableKind.HEATING_MANIFOLD)) {
            scheduleHeatingLayoutRecalculation();
        }
        render();
    }

    void mirrorSelectedHeatingZones(boolean horizontally) {
        if (selectedSelections.stream().noneMatch(selection -> selection.kind() == RenderableKind.HEATING_ZONE)) {
            return;
        }
        rememberStateForUndo();
        HeatingZoneMirrorService.MirrorResult mirrorResult = heatingZoneMirrorService.mirror(
                activeLevel.get(), Set.copyOf(selectedSelections), horizontally
        );
        if (!mirrorResult.changed()) {
            return;
        }
        activeLevel.get().replaceHydronicHeatings(mirrorResult.hydronicHeatings());
        refreshHeatingSection();
        markThreeDDirty();
        draftLabel.setText(horizontally
                ? "Ausgewählte Heizkreise horizontal gespiegelt."
                : "Ausgewählte Heizkreise vertikal gespiegelt.");
        scheduleHeatingLayoutRecalculation();
        render();
    }

    void activateLevel(Level level) {
        if (levelSelector.getValue() != level) {
            levelSelector.setValue(level);
            return;
        }
        activeLevel.set(level);
        threeDViewport.syncLevels(availableLevels, level.name());
        clearHeatingLayoutCache();
        markThreeDDirty();
        updatePropertySectionVisibility();
        updateActionButtons();
        render();
    }

    void handleThreeDSelection(SelectionKey selectionKey) {
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

    void markThreeDDirty() {
        threeDDirty = true;
    }

    void synchronizeRoomsFromWalls(Level level) {
        pendingRoomSynchronizationImpact = emptyRoomSynchronizationImpact();
        synchronizeRoomsFromWalls(level, true);
    }

    void previewRoomSynchronizationFromWalls(Level level) {
        pendingRoomSynchronizationImpact = synchronizeRoomsFromWalls(level, false);
    }

    Level.RoomReplacementImpact synchronizeRoomsFromWalls(Level level, boolean showWarning) {
        List<Room> synchronizedRooms = autoRoomGenerationService.synchronize(level, currentRoomDefaults());
        Level.RoomReplacementImpact impact = level.roomReplacementImpact(synchronizedRooms);
        level.replaceRooms(synchronizedRooms);
        if (showWarning) {
            showRoomSynchronizationWarning(impact);
        }
        return impact;
    }

    void flushPendingRoomSynchronizationWarning() {
        try {
            showRoomSynchronizationWarning(pendingRoomSynchronizationImpact);
        } finally {
            pendingRoomSynchronizationImpact = emptyRoomSynchronizationImpact();
        }
    }

    void synchronizeStairUnderbuild(Staircase staircase) {
        StairUnderbuildService.UnderbuildResult result = stairUnderbuildService.synchronize(activeLevel.get(), staircase);
        activeLevel.get().replaceWalls(result.walls());
        activeLevel.get().replaceDoors(result.doors());
        activeLevel.get().replaceWindows(result.windows());
    }

    void prepareSelectionDrag(SelectionKey selectionKey, PlanPoint anchorPoint) {
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

    void translateSelectedComponents(PlanPoint snappedPoint) {
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
        TranslationDelta snappedDelta = snapHeatingZoneTranslationToGrid(selectionDragBaseHydronicHeatings, deltaX, deltaY);
        deltaX = snappedDelta.deltaX();
        deltaY = snappedDelta.deltaY();
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
        if (selectedTranslationAffectsRooms()) {
            previewRoomSynchronizationFromWalls(activeLevel.get());
        }
        markThreeDDirty();
    }

    boolean isHeatingZoneHandle(EdgeResizeService.EdgeHandle handle) {
        return handle.elementKind() == RenderableKind.HEATING_ZONE;
    }

    boolean hasSelectedHeatingZone() {
        return selectedSelections.stream().anyMatch(selection -> selection.kind() == RenderableKind.HEATING_ZONE);
    }

    Optional<HeatingZone> firstSelectedHeatingZone(List<HydronicHeating> heatings) {
        return heatings.stream()
                .flatMap(heating -> heating.zones().stream())
                .filter(zone -> selectedSelections.stream().anyMatch(selection -> selection.kind() == RenderableKind.HEATING_ZONE
                        && selection.elementId().equals(zone.id().toString())))
                .findFirst();
    }

    HeatingZone snapHeatingZoneRoutingStartIfNeeded(HeatingZone zone) {
        if (!snapToGrid.get()) {
            return zone;
        }
        PlanPoint start = zone.routingStartPoint();
        PlanPoint snappedStart = currentGrid().snap(start);
        double deltaX = snappedStart.xMillimeters() - start.xMillimeters();
        double deltaY = snappedStart.yMillimeters() - start.yMillimeters();
        if (Math.abs(deltaX) <= 0.001 && Math.abs(deltaY) <= 0.001) {
            return zone;
        }
        return zone.translatedBy(deltaX, deltaY);
    }

    boolean moveSelectionWithArrowKey(KeyCode keyCode) {
        if (!drawingCanvas.isFocused()
                || activeWorkspaceMode.get() != WorkspaceMode.TWO_D
                || !isDirectEditingView()
                || currentTool() != DrawingTool.EDIT
                || selectedSelections.stream().noneMatch(this::isTranslatableSelection)) {
            return false;
        }
        return moveSelectionByArrowKey(keyCode);
    }

    boolean moveSelectionByArrowKey(KeyCode keyCode) {
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

    void moveSelectedComponents(double deltaX, double deltaY) {
        TranslationDelta snappedDelta = snapHeatingZoneTranslationToGrid(activeLevel.get().hydronicHeatings(), deltaX, deltaY);
        SelectionTranslationService.TranslationResult result = selectionTranslationService.translate(
                activeLevel.get(), Set.copyOf(selectedSelections), snappedDelta.deltaX(), snappedDelta.deltaY()
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
        if (selectedTranslationAffectsRooms()) {
            synchronizeRoomsFromWalls(activeLevel.get());
        }
        markThreeDDirty();
        draftLabel.setText("Auswahl um eine Rasterweite verschoben.");
        if (selectedSelections.stream().anyMatch(selection -> selection.kind() == RenderableKind.HEATING_ZONE
                || selection.kind() == RenderableKind.HEATING_MANIFOLD)) {
            scheduleHeatingLayoutRecalculation();
        }
        render();
    }

    TranslationDelta snapHeatingZoneTranslationToGrid(List<HydronicHeating> heatings, double deltaX, double deltaY) {
        if (!snapToGrid.get()) {
            return new TranslationDelta(deltaX, deltaY);
        }
        Optional<HeatingZone> heatingZone = firstSelectedHeatingZone(heatings);
        if (heatingZone.isEmpty()) {
            return new TranslationDelta(deltaX, deltaY);
        }
        PlanPoint start = heatingZone.orElseThrow().routingStartPoint();
        PlanPoint movedStart = new PlanPoint(start.xMillimeters() + deltaX, start.yMillimeters() + deltaY);
        PlanPoint snappedStart = currentGrid().snap(movedStart);
        return new TranslationDelta(
                deltaX + snappedStart.xMillimeters() - movedStart.xMillimeters(),
                deltaY + snappedStart.yMillimeters() - movedStart.yMillimeters()
        );
    }

    boolean selectedTranslationAffectsRooms() {
        return selectedSelections.stream().anyMatch(selection -> selection.kind() == RenderableKind.WALL
                || selection.kind() == RenderableKind.STAIR);
    }

    void correctSelectedComponentsOrthogonally() {
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

    void refreshThreeDIfNeeded() {
        if (!threeDDirty) {
            return;
        }
        threeDViewport.syncLevels(availableLevels, activeLevel.get().name());
        threeDViewport.refresh(project);
        threeDDirty = false;
    }

    boolean isSelected(RenderableKind kind, String elementId) {
        return selectedSelections.stream().anyMatch(selection ->
                selection.kind() == kind
                        && selection.levelName().equals(activeLevel.get().name())
                        && selection.elementId().equals(elementId)
        );
    }
}
