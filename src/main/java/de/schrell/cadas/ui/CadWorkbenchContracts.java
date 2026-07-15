package de.schrell.cadas.ui;

import de.schrell.cadas.application.drawing.DraftingConstraints;
import de.schrell.cadas.application.drawing.DimensionLabelPlacementService;
import de.schrell.cadas.application.drawing.DimensionLineLayoutService;
import de.schrell.cadas.application.drawing.DimensionTextStyle;
import de.schrell.cadas.application.drawing.EdgeResizeService;
import de.schrell.cadas.application.drawing.GuideSnapTargets;
import de.schrell.cadas.application.drawing.WallIntersectionSplitService;
import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
import de.schrell.cadas.application.drawing.TextBlockingBox;
import de.schrell.cadas.application.drawing.WallEndpointSelection;
import de.schrell.cadas.application.dwg.DwgBlockDefinition;
import de.schrell.cadas.application.dwg.DwgLibraryAnalysis;
import de.schrell.cadas.application.layers.SurfaceCoveringPreset;
import de.schrell.cadas.application.layers.SurfaceRectangleTileLayoutService;
import de.schrell.cadas.application.objects.RoomObjectPreset;
import de.schrell.cadas.application.parts.DoorPreset;
import de.schrell.cadas.application.parts.StairPreset;
import de.schrell.cadas.application.parts.WindowPreset;
import de.schrell.cadas.application.room.AutoRoomGenerationService;
import de.schrell.cadas.application.terrain.TerrainProfileService;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.application.view.WallSurfaceOpeningService.WallSurfaceInterval;
import de.schrell.cadas.application.view.WallSurfaceOpeningService.WallSurfaceRectangle;
import de.schrell.cadas.domain.geometry.Angle;
import de.schrell.cadas.domain.geometry.Grid;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoofWindow;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectHeatingType;
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
import de.schrell.cadas.domain.model.WindowElement;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javafx.collections.ObservableList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

abstract class CadWorkbenchContracts extends BorderPane {

    static final double DIMENSION_LINE_BLOCKING_PADDING = 4.0;

    CadWorkbench self() {
        return (CadWorkbench) this;
    }

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
