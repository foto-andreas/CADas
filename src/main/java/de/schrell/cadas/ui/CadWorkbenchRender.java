package de.schrell.cadas.ui;

import de.schrell.cadas.application.drawing.DimensionLabelService;
import de.schrell.cadas.application.drawing.DimensionLineLayoutService;
import de.schrell.cadas.application.drawing.DimensionTextStyle;
import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
import de.schrell.cadas.application.drawing.TextBlockingBox;
import de.schrell.cadas.application.drawing.WallDimensionPlacementService;
import de.schrell.cadas.application.drawing.WallDimensionService;
import de.schrell.cadas.application.layers.TileLayoutRequest;
import de.schrell.cadas.application.layers.TilePlacement;
import de.schrell.cadas.application.layers.WallSurfaceSideService;
import de.schrell.cadas.application.layers.WallSurfaceTargetKey;
import de.schrell.cadas.application.terrain.TerrainProfileService;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.application.view.WallSurfaceOpeningService.WallSurfaceInterval;
import de.schrell.cadas.application.view.WallSurfaceOpeningService.WallSurfaceRectangle;
import de.schrell.cadas.application.view.WallSurfacePlanGeometryService.WallSurfacePlanPolygon;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

abstract class CadWorkbenchRender extends CadWorkbenchInteraction {

    void render() {
        GraphicsContext graphics = drawingCanvas.getGraphicsContext2D();
        graphics.setFill(CANVAS_BACKGROUND);
        graphics.fillRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());

        self().drawTerrainPlanArea(graphics);
        drawLowerLevel(graphics);

        if (showGuides.get()) {
            drawGuides(graphics);
        }
        drawTerrainElevation(graphics);
        drawRooms(graphics);
        drawWalls(graphics);
        drawWallSurfaceLayers(graphics);
        self().drawStaircases(graphics);
        self().drawFloorExtensions(graphics);
        self().drawDoors(graphics);
        self().drawWindows(graphics);
        self().drawRoofWindows(graphics);
        self().drawRoomObjects(graphics);
        drawHydronicHeatings(graphics);
        // Raumtexte werden vor den Bemaßungen gerendert, damit ihre Sperrflächen
        // als Seed-Blocker für die kollisionsfreie Maßtext-Platzierung dienen.
        List<TextBlockingBox> roomLabelBlockers = self().drawRoomLabels(graphics);
        drawWallDimensions(graphics, roomLabelBlockers);
        self().drawTerrainPlanMarkers(graphics);
        drawGrid(graphics);
        drawSelectionOverlay(graphics);
        drawSelectionRectangle(graphics);
        self().drawEditablePoints(graphics);
        self().drawEdgeResizeHandles(graphics);
        if (previewSegment != null) {
            self().drawPreview(graphics);
        }
        self().drawViewOverlay(graphics);
        if (showCompass.get()) {
            self().drawCompass(graphics);
        }
        self().drawRulers();
        self().refreshThreeDIfNeeded();
        self().updateStatus();
    }

    List<PlanPoint> terrainContour() {
        return terrainContourService.contour(project);
    }

    void drawGuides(GraphicsContext graphics) {
        if (!ViewProjectionService.isPlanView(activeView.get())) {
            return;
        }
        graphics.setStroke(Color.color(0.73, 0.2, 0.2, 0.75));
        graphics.setLineWidth(1.2);
        for (GuideLine guideLine : guideLines) {
            if (guideLine.orientation() == GuideOrientation.VERTICAL) {
                double x = self().toScreenX(guideLine.worldMillimeters());
                graphics.strokeLine(x, 0, x, drawingCanvas.getHeight());
            } else {
                double y = self().toScreenY(guideLine.worldMillimeters());
                graphics.strokeLine(0, y, drawingCanvas.getWidth(), y);
            }
        }
        if (pendingGuideOrientation != null) {
            if (pendingGuideOrientation == GuideOrientation.VERTICAL) {
                double x = self().toScreenX(pendingGuideWorldMillimeters);
                graphics.strokeLine(x, 0, x, drawingCanvas.getHeight());
            } else {
                double y = self().toScreenY(pendingGuideWorldMillimeters);
                graphics.strokeLine(0, y, drawingCanvas.getWidth(), y);
            }
            drawGuideDistances(graphics);
        }
    }

    void drawGuideDistances(GraphicsContext graphics) {
        if (!showGuideDistances.get()) {
            return;
        }
        List<GuideDistanceService.GuideDistance> distances = GuideDistanceService.distancesToParallelGuides(
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
                graphics.fillText(text, self().toScreenX(midpoint) - 22.0, 20.0 + index % 4 * 16.0);
            } else {
                graphics.fillText(text, 10.0 + index % 3 * 76.0, self().toScreenY(midpoint) - 5.0);
            }
        }
    }

    void drawGrid(GraphicsContext graphics) {
        if (!ViewProjectionService.isPlanView(activeView.get()) || !showGrid.get()) {
            return;
        }
        double spacingMillimeters = self().currentGrid().spacing().toMillimeters();
        double spacingPixels = spacingMillimeters * self().scale();
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

    void drawSelectionOverlay(GraphicsContext graphics) {
        if (!ViewProjectionService.isPlanView(activeView.get()) || selectedSelections.isEmpty()) {
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
                        .ifPresent(wall -> self().drawWall(graphics, wall.axis(), wall.thickness(), Color.web("#d97f2f"), 1.0));
                case ROOM_FLOOR, ROOM_CEILING, ROOM_VOLUME -> activeLevel.get().rooms().stream()
                        .filter(room -> room.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(room -> graphics.strokePolygon(
                                room.outline().stream().mapToDouble(point -> self().toScreenProjectedX(point, 0.0)).toArray(),
                                room.outline().stream().mapToDouble(point -> self().toScreenProjectedY(point, 0.0)).toArray(),
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
                        .ifPresent(roofWindow -> self().drawRoofWindowOutline(graphics, roofWindow));
                case STAIR -> activeLevel.get().staircases().stream()
                        .filter(staircase -> staircase.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(staircase -> self().drawStairOutline(graphics, staircase));
                case ROOM_OBJECT -> activeLevel.get().roomObjects().stream()
                        .filter(roomObject -> roomObject.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(roomObject -> drawSelectedRoomObjectOutline(graphics, roomObject));
                case FLOOR_EXTENSION -> activeLevel.get().floorExtensions().stream()
                        .filter(extension -> extension.id().toString().equals(selection.elementId()))
                        .findFirst()
                        .ifPresent(extension -> graphics.strokeRect(
                                self().toScreenX(extension.minX()),
                                self().toScreenY(extension.minY()),
                                extension.widthMillimeters() * self().scale(),
                                extension.depthMillimeters() * self().scale()
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

    void drawSelectionRectangle(GraphicsContext graphics) {
        if (!ViewProjectionService.isPlanView(activeView.get())
                || selectionRectangleStart == null
                || selectionRectangleEnd == null
                || !self().hasSelectionRectangleArea()) {
            return;
        }
        double x = Math.min(self().toScreenX(selectionRectangleStart.xMillimeters()), self().toScreenX(selectionRectangleEnd.xMillimeters()));
        double y = Math.min(self().toScreenY(selectionRectangleStart.yMillimeters()), self().toScreenY(selectionRectangleEnd.yMillimeters()));
        double width = Math.abs(self().toScreenX(selectionRectangleEnd.xMillimeters()) - self().toScreenX(selectionRectangleStart.xMillimeters()));
        double height = Math.abs(self().toScreenY(selectionRectangleEnd.yMillimeters()) - self().toScreenY(selectionRectangleStart.yMillimeters()));
        graphics.save();
        graphics.setFill(Color.color(0.85, 0.5, 0.18, 0.12));
        graphics.fillRect(x, y, width, height);
        graphics.setStroke(Color.web("#d97f2f"));
        graphics.setLineDashes(8.0, 6.0);
        graphics.setLineWidth(1.5);
        graphics.strokeRect(x, y, width, height);
        graphics.restore();
    }

    boolean shouldStartSelectionRectangle(SelectionKey editSelection) {
        return editSelection == null
                || editSelection.kind() == RenderableKind.ROOM_VOLUME
                || editSelection.kind() == RenderableKind.ROOM_FLOOR
                || editSelection.kind() == RenderableKind.ROOM_CEILING;
    }

    void drawSelectedOpening(GraphicsContext graphics, UUID wallId, Length offset, Length width) {
        Wall wall = activeLevel.get().findWall(wallId);
        PlanPoint start = wall.axis().pointAt(offset);
        PlanPoint end = wall.axis().pointAt(offset.add(width));
        graphics.strokeLine(
                self().toScreenProjectedX(start, 0.0),
                self().toScreenProjectedY(start, 0.0),
                self().toScreenProjectedX(end, 0.0),
                self().toScreenProjectedY(end, 0.0)
        );
    }

    void drawSelectedRoomObjectOutline(GraphicsContext graphics, RoomObject roomObject) {
        double width = roomObject.width().toMillimeters() * self().scale();
        double depth = roomObject.depth().toMillimeters() * self().scale();
        graphics.save();
        graphics.translate(self().toScreenX(roomObject.center().xMillimeters()), self().toScreenY(roomObject.center().yMillimeters()));
        graphics.rotate(-roomObject.rotationDegrees());
        if (roomObject.shape() == RoomObjectShape.CIRCLE || roomObject.shape() == RoomObjectShape.OVAL) {
            graphics.strokeOval(-width / 2.0, -depth / 2.0, width, depth);
        } else {
            graphics.strokeRect(-width / 2.0, -depth / 2.0, width, depth);
        }
        graphics.restore();
    }

    void drawSelectedFloorOpening(GraphicsContext graphics, FloorOpening opening) {
        double x = self().toScreenProjectedX(new PlanPoint(opening.minXMillimeters(), opening.minYMillimeters()), 0.0);
        double y = self().toScreenProjectedY(new PlanPoint(opening.minXMillimeters(), opening.minYMillimeters()), 0.0);
        double width = opening.width().toMillimeters() * self().scale();
        double height = opening.depth().toMillimeters() * self().scale();
        if (opening.shape() == FloorOpeningShape.CIRCLE) {
            graphics.strokeOval(x, y, width, height);
        } else {
            graphics.strokeRect(x, y, width, height);
        }
    }

    void drawSelectedHeatingZone(GraphicsContext graphics, HeatingZone zone) {
        graphics.strokePolygon(
                zone.outline().stream().mapToDouble(point -> self().toScreenProjectedX(point, 0.0)).toArray(),
                zone.outline().stream().mapToDouble(point -> self().toScreenProjectedY(point, 0.0)).toArray(),
                zone.outline().size()
        );
    }

    void drawSelectedHeatingManifold(GraphicsContext graphics, HydronicHeating heating) {
        double centerX = (heating.supplyPoint().xMillimeters() + heating.returnPoint().xMillimeters()) / 2.0;
        double centerY = (heating.supplyPoint().yMillimeters() + heating.returnPoint().yMillimeters()) / 2.0;
        double minX = centerX - heating.manifoldFreeAreaWidth().toMillimeters() / 2.0;
        double minY = centerY - heating.manifoldFreeAreaDepth().toMillimeters() / 2.0;
        graphics.strokeRect(
                self().toScreenProjectedX(new PlanPoint(minX, minY), 0.0),
                self().toScreenProjectedY(new PlanPoint(minX, minY), 0.0),
                heating.manifoldFreeAreaWidth().toMillimeters() * self().scale(),
                heating.manifoldFreeAreaDepth().toMillimeters() * self().scale()
        );
    }

    void drawSelectedHeatingExclusionArea(GraphicsContext graphics, HeatingExclusionArea area) {
        graphics.strokeRect(
                self().toScreenProjectedX(new PlanPoint(area.minXMillimeters(), area.minYMillimeters()), 0.0),
                self().toScreenProjectedY(new PlanPoint(area.minXMillimeters(), area.minYMillimeters()), 0.0),
                area.widthMillimeters() * self().scale(),
                area.depthMillimeters() * self().scale()
        );
    }

    void drawLowerLevel(GraphicsContext graphics) {
        if (!ViewProjectionService.isPlanView(activeView.get())) return;
        int index = availableLevels.indexOf(activeLevel.get());
        if (index <= 0) return;
        Level lowerLevel = availableLevels.get(index - 1);
        graphics.setGlobalAlpha(0.2);
        Color gray = Color.gray(0.2);
        for (Room room : lowerLevel.rooms()) {
            double[] xPoints = room.outline().stream().mapToDouble(p -> self().toScreenProjectedX(p, 0.0)).toArray();
            double[] yPoints = room.outline().stream().mapToDouble(p -> self().toScreenProjectedY(p, 0.0)).toArray();
            graphics.setFill(gray);
            graphics.fillPolygon(xPoints, yPoints, xPoints.length);
            graphics.setStroke(gray);
            graphics.setLineWidth(2.0);
            graphics.strokePolygon(xPoints, yPoints, xPoints.length);
        }
        for (Wall wall : lowerLevel.walls()) {
            self().drawWall(graphics, wall.axis(), wall.thickness(), gray, 1.0);
        }
        graphics.setGlobalAlpha(1.0);
    }

    void drawWalls(GraphicsContext graphics) {
        graphics.setFill(Color.web("#2f2a24"));

        for (Wall wall : activeLevel.get().walls()) {
            boolean selected = self().isSelected(RenderableKind.WALL, wall.id().toString());
            if (ViewProjectionService.isPlanView(activeView.get())) {
                self().drawWall(graphics, wall.axis(), wall.thickness(), selected ? Color.web("#d97f2f") : CadColorPalette.WALL, 1.0);
            } else {
                self().drawWallElevation(graphics, wall, selected);
            }
        }
    }

    void drawWallDimensions(GraphicsContext graphics, List<TextBlockingBox> seedBlockers) {
        lastPlanDimensionScreenBounds = null;
        if (!showDimensions.get() || !ViewProjectionService.isPlanView(activeView.get())) {
            return;
        }
        if (reportSnapshotInteriorRoomDimensionsOnly) {
            drawReportInteriorRoomDimensions(graphics, seedBlockers);
            return;
        }
        DimensionTextStyle textStyle = currentDimensionTextStyle();
        List<PendingWallDimensionLabel> pendingLabels = new ArrayList<>();
        for (Wall wall : activeLevel.get().walls()) {
            appendWallDimensionLabels(pendingLabels, wall, textStyle);
        }
        List<RenderedWallDimensionLabel> placed = dimensionLabelPlacementService.place(
                pendingLabels,
                seedBlockers,
                this::layoutWallDimensionLabel
        );
        ScreenBounds dimensionBounds = null;
        for (RenderedWallDimensionLabel rendered : placed) {
            dimensionBounds = ScreenBounds.union(dimensionBounds, dimensionScreenBounds(rendered));
            self().drawIsoDimensionLines(graphics, rendered.layout(), rendered.directionX(), rendered.directionY());
            graphics.setFill(CadColorPalette.DIMENSION_TEXT);
            graphics.setFont(DIMENSION_LABEL_FONT);
            graphics.fillText(rendered.pending().text(), rendered.textX(), rendered.baselineY());
        }
        lastPlanDimensionScreenBounds = dimensionBounds;
    }

    void drawReportInteriorRoomDimensions(GraphicsContext graphics, List<TextBlockingBox> seedBlockers) {
        List<TextBlockingBox> blockers = new ArrayList<>(seedBlockers);
        ScreenBounds dimensionBounds = null;
        graphics.save();
        graphics.setFill(CadColorPalette.DIMENSION_TEXT);
        graphics.setStroke(CadColorPalette.DIMENSION_TEXT);
        graphics.setLineWidth(1.0);
        graphics.setFont(DIMENSION_LABEL_FONT);
        for (Room room : activeLevel.get().rooms()) {
            List<PlanPoint> outline = room.outline();
            for (int index = 0; index < outline.size(); index++) {
                PlanSegment segment = new PlanSegment(outline.get(index), outline.get((index + 1) % outline.size()));
                Optional<RenderedInteriorRoomDimension> rendered = layoutReportInteriorRoomDimension(room, segment, blockers);
                if (rendered.isEmpty()) {
                    continue;
                }
                RenderedInteriorRoomDimension dimension = rendered.orElseThrow();
                graphics.strokeLine(dimension.lineStartX(), dimension.lineStartY(), dimension.lineEndX(), dimension.lineEndY());
                graphics.fillText(dimension.text(), dimension.textX(), dimension.baselineY());
                blockers.add(dimension.blockingBox());
                dimensionBounds = ScreenBounds.union(dimensionBounds, ScreenBounds.from(dimension.blockingBox()));
            }
        }
        graphics.restore();
        lastPlanDimensionScreenBounds = dimensionBounds;
    }

    Optional<RenderedInteriorRoomDimension> layoutReportInteriorRoomDimension(
            Room room,
            PlanSegment segment,
            List<TextBlockingBox> blockers
    ) {
        double lengthMillimeters = segment.length().toMillimeters();
        if (lengthMillimeters < 500.0) {
            return Optional.empty();
        }
        double directionX = (segment.end().xMillimeters() - segment.start().xMillimeters()) / lengthMillimeters;
        double directionY = (segment.end().yMillimeters() - segment.start().yMillimeters()) / lengthMillimeters;
        double normalX = -directionY;
        double normalY = directionX;
        PlanPoint midpoint = new PlanPoint(
                (segment.start().xMillimeters() + segment.end().xMillimeters()) / 2.0,
                (segment.start().yMillimeters() + segment.end().yMillimeters()) / 2.0
        );
        double sideSign = de.schrell.cadas.domain.geometry.PlanPolygonSupport.containsPoint(room.outline(), offsetPoint(midpoint, normalX, normalY, 120.0))
                ? 1.0
                : -1.0;
        String text = String.format(Locale.GERMAN, "%.2f m", lengthMillimeters / 1000.0);
        Text textMeasure = new Text(text);
        textMeasure.setFont(DIMENSION_LABEL_FONT);
        double lineLengthPixels = lengthMillimeters * self().scale();
        if (textMeasure.getLayoutBounds().getWidth() > lineLengthPixels - 10.0) {
            return Optional.empty();
        }
        for (double offsetMillimeters : new double[]{140.0, 260.0, 380.0}) {
            PlanPoint labelCenter = offsetPoint(midpoint, normalX, normalY, sideSign * offsetMillimeters);
            if (!de.schrell.cadas.domain.geometry.PlanPolygonSupport.containsPoint(room.outline(), labelCenter)) {
                continue;
            }
            RenderedInteriorRoomDimension rendered = renderInteriorRoomDimension(segment, normalX, normalY, sideSign, offsetMillimeters, text, textMeasure);
            if (!TextBlockingBox.overlapsAny(rendered.blockingBox(), blockers)) {
                return Optional.of(rendered);
            }
        }
        return Optional.empty();
    }

    RenderedInteriorRoomDimension renderInteriorRoomDimension(
            PlanSegment segment,
            double normalX,
            double normalY,
            double sideSign,
            double offsetMillimeters,
            String text,
            Text textMeasure
    ) {
        PlanPoint lineStart = offsetPoint(segment.start(), normalX, normalY, sideSign * offsetMillimeters);
        PlanPoint lineEnd = offsetPoint(segment.end(), normalX, normalY, sideSign * offsetMillimeters);
        double lineStartX = self().toScreenProjectedX(lineStart, 0.0);
        double lineStartY = self().toScreenProjectedY(lineStart, 0.0);
        double lineEndX = self().toScreenProjectedX(lineEnd, 0.0);
        double lineEndY = self().toScreenProjectedY(lineEnd, 0.0);
        double centerX = (lineStartX + lineEndX) / 2.0;
        double centerY = (lineStartY + lineEndY) / 2.0;
        double textX = centerX - textMeasure.getLayoutBounds().getWidth() / 2.0;
        double baselineY = centerY - 3.0;
        TextBlockingBox blockingBox = new TextBlockingBox(
                textX + textMeasure.getLayoutBounds().getMinX() - DIMENSION_TEXT_PADDING,
                baselineY + textMeasure.getLayoutBounds().getMinY() - DIMENSION_TEXT_PADDING,
                textMeasure.getLayoutBounds().getWidth() + DIMENSION_TEXT_PADDING * 2.0,
                textMeasure.getLayoutBounds().getHeight() + DIMENSION_TEXT_PADDING * 2.0
        );
        return new RenderedInteriorRoomDimension(text, lineStartX, lineStartY, lineEndX, lineEndY, textX, baselineY, blockingBox);
    }

    PlanPoint offsetPoint(PlanPoint point, double unitX, double unitY, double offsetMillimeters) {
        return new PlanPoint(
                point.xMillimeters() + unitX * offsetMillimeters,
                point.yMillimeters() + unitY * offsetMillimeters
        );
    }

    void appendWallDimensionLabels(List<PendingWallDimensionLabel> pendingLabels, Wall wall, DimensionTextStyle textStyle) {
        WallDimensionService.WallDimensions dimensions = wallDimensionService.dimensions(activeLevel.get(), wall);
        double isoExtra = 12.0;
        double baseOffset = Math.max(wall.thickness().toMillimeters() * self().scale() / 2.0 + 16.0 + isoExtra, 28.0 + isoExtra);
        double stepOffset = 20.0 + isoExtra;
        for (WallDimensionPlacementService.PlacedDimension placement : wallDimensionPlacementService.place(
                activeLevel.get(),
                wall,
                dimensions,
                self().scale(),
                baseOffset,
                stepOffset
        )) {
            WallDimensionService.SideDimension dimension = placement.dimension();
            pendingLabels.add(new PendingWallDimensionLabel(
                    dimension.dimensionSegment(),
                    DimensionLabelService.label(dimension, placement.exterior(), textStyle),
                    placement.normalOffset(),
                    placement.lineDistanceFromAxis(),
                    Math.copySign(stepOffset, placement.normalOffset()),
                    dimension.length().toMillimeters(),
                    DimensionLabelService.deduplicationKey(dimension, placement.exterior())
            ));
        }
        if (dimensions.roomDimensions().isEmpty() && dimensions.exteriorDimension().isEmpty()) {
            WallDimensionPlacementService.PlacedDimension axisPlacement = wallDimensionPlacementService.placeAxisDimension(
                    activeLevel.get(),
                    wall,
                    self().scale(),
                    baseOffset
            );
            pendingLabels.add(new PendingWallDimensionLabel(
                    wall.axis(),
                    DimensionLabelService.label("Achsmaß", wall.axis().length(), false, textStyle),
                    axisPlacement.normalOffset(),
                    axisPlacement.lineDistanceFromAxis(),
                    Math.copySign(stepOffset, axisPlacement.normalOffset()),
                    wall.axis().length().toMillimeters(),
                    ""
            ));
        }
    }

    RenderedWallDimensionLabel layoutWallDimensionLabel(PendingWallDimensionLabel pendingLabel, double normalOffset) {
        PlanSegment segment = pendingLabel.segment();
        double startX = self().toScreenProjectedX(segment.start(), 0.0);
        double startY = self().toScreenProjectedY(segment.start(), 0.0);
        double endX = self().toScreenProjectedX(segment.end(), 0.0);
        double endY = self().toScreenProjectedY(segment.end(), 0.0);
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

    ScreenBounds dimensionScreenBounds(RenderedWallDimensionLabel rendered) {
        DimensionLineLayoutService.DimensionLineLayout layout = rendered.layout();
        ScreenBounds bounds = ScreenBounds.from(rendered.blockingBox());
        bounds = bounds.includeLine(layout.firstExtensionStartX(), layout.firstExtensionStartY(), layout.firstExtensionEndX(), layout.firstExtensionEndY(), 8.0);
        bounds = bounds.includeLine(layout.secondExtensionStartX(), layout.secondExtensionStartY(), layout.secondExtensionEndX(), layout.secondExtensionEndY(), 8.0);
        bounds = bounds.includeLine(layout.lineStartX(), layout.lineStartY(), layout.lineEndX(), layout.lineEndY(), 8.0);
        return bounds;
    }

    void drawWallSurfaceLayers(GraphicsContext graphics) {
        if (ViewProjectionService.isPlanView(activeView.get())) {
            drawWallSurfaceLayersInPlan(graphics);
        } else {
            drawWallSurfaceLayersInElevation(graphics);
        }
    }

    void drawWallSurfaceLayersInPlan(GraphicsContext graphics) {
        for (Wall wall : activeLevel.get().walls()) {
            activeLevel.get().surfaceLayerStacks().stream()
                    .filter(stack -> isWallSurfaceType(stack.surfaceType()))
                    .filter(stack -> WallSurfaceTargetKey.matchesWall(stack.targetKey(), wall.id()))
                    .forEach(stack -> drawWallSurfaceStackInPlan(graphics, wall, stack));
        }
    }

    boolean isWallSurfaceType(SurfaceType surfaceType) {
        return surfaceType == SurfaceType.WALL_INTERIOR || surfaceType == SurfaceType.WALL_EXTERIOR;
    }

    void drawWallSurfaceStackInPlan(GraphicsContext graphics, Wall wall, SurfaceLayerStack stack) {
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

    void drawWallSurfaceLayerInPlan(
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
        UUID roomId = stack.surfaceType() == SurfaceType.WALL_INTERIOR
                ? WallSurfaceTargetKey.roomId(stack.targetKey()).orElse(null)
                : null;
        List<WallSurfaceInterval> visibleIntervals = roomId == null
                ? wallSurfaceOpeningService.visiblePlanIntervals(activeLevel.get(), wall, sideSign)
                : wallSurfaceOpeningService.visiblePlanIntervals(activeLevel.get(), wall, sideSign, roomId);
        if (visibleIntervals.isEmpty()) {
            return;
        }
        boolean selected = self().isSelectedSurfaceLayer(stack, layer);
        graphics.save();
        graphics.setFill(selected ? Color.color(0.86, 0.48, 0.18, 0.88) : Color.color(0.72, 0.58, 0.34, 0.82));
        if (selected) {
            graphics.setStroke(Color.color(0.76, 0.28, 0.10, 0.96));
            graphics.setLineWidth(1.6);
        }
        for (WallSurfaceInterval interval : visibleIntervals) {
            fillWallSurfaceIntervalInPlan(graphics, wall, stack, layer, layerIndex, centerOffset, interval, selected);
        }
        drawWallSurfaceJointsInPlan(graphics, wall, layer, centerOffset, visibleIntervals, selected);
        graphics.restore();
    }

    void fillWallSurfaceIntervalInPlan(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayerStack stack,
            SurfaceLayer layer,
            int layerIndex,
            double centerOffset,
            WallSurfaceInterval interval,
            boolean selected
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
                polygon.points().stream().mapToDouble(point -> self().toScreenProjectedX(point, 0.0)).toArray(),
                polygon.points().stream().mapToDouble(point -> self().toScreenProjectedY(point, 0.0)).toArray(),
                polygon.points().size()
        );
        if (selected) {
            graphics.strokePolygon(
                    polygon.points().stream().mapToDouble(point -> self().toScreenProjectedX(point, 0.0)).toArray(),
                    polygon.points().stream().mapToDouble(point -> self().toScreenProjectedY(point, 0.0)).toArray(),
                    polygon.points().size()
            );
        }
    }

    void drawWallSurfaceJointsInPlan(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayer layer,
            double centerOffset,
            List<WallSurfaceInterval> visibleIntervals,
            boolean selected
    ) {
        double wallLength = wall.axis().length().toMillimeters();
        double jointWidth = layer.jointWidth().toMillimeters();
        if (jointWidth < 0.001 || layer.effectiveTileWidth().toMillimeters() * self().scale() < 14.0) {
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
                layer.minimumStartEndMargin(),
                layer.freeMargins(),
                SurfaceLayoutAnchor.AUTO,
                Length.zero(),
                Length.zero()
        );
        graphics.setStroke(selected ? Color.color(0.78, 0.24, 0.08, 0.92) : Color.color(0.20, 0.15, 0.09, 0.72));
        graphics.setLineWidth(Math.max(selected ? 1.2 : 0.8, jointWidth * self().scale()));
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
                    self().toScreenProjectedX(from, 0.0),
                    self().toScreenProjectedY(from, 0.0),
                    self().toScreenProjectedX(to, 0.0),
                    self().toScreenProjectedY(to, 0.0)
            );
        }
    }

    boolean isVisiblePlanJoint(double jointPosition, List<WallSurfaceInterval> visibleIntervals) {
        return visibleIntervals.stream()
                .anyMatch(interval -> jointPosition > interval.startMillimeters() + 0.001
                        && jointPosition < interval.endMillimeters() - 0.001);
    }

    void drawWallSurfaceLayersInElevation(GraphicsContext graphics) {
        for (Wall wall : activeLevel.get().walls()) {
            activeLevel.get().surfaceLayerStacks().stream()
                    .filter(stack -> isWallSurfaceType(stack.surfaceType()))
                    .filter(stack -> WallSurfaceTargetKey.matchesWall(stack.targetKey(), wall.id()))
                    .forEach(stack -> drawWallSurfaceStackInElevation(graphics, wall, stack));
        }
    }

    void drawWallSurfaceStackInElevation(GraphicsContext graphics, Wall wall, SurfaceLayerStack stack) {
        WallSurfaceSideService.WallLayerSides sides = wallSurfaceSideService.resolve(activeLevel.get(), wall, stack.surfaceType(), stack.targetKey());
        if (!sides.positiveSide() && !sides.negativeSide()) {
            return;
        }
        for (SurfaceLayer layer : stack.layers()) {
            if (layer.visible()) {
                if (sides.positiveSide()) {
                    drawWallSurfaceLayerInElevation(graphics, wall, stack, layer, 1.0);
                }
                if (sides.negativeSide()) {
                    drawWallSurfaceLayerInElevation(graphics, wall, stack, layer, -1.0);
                }
            }
        }
    }

    void drawWallSurfaceLayerInElevation(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayerStack stack,
            SurfaceLayer layer,
            double sideSign
    ) {
        double wallLength = wall.axis().length().toMillimeters();
        double startHorizontal = self().projectHorizontal(wall.axis().start(), 0.0);
        double endHorizontal = self().projectHorizontal(wall.axis().end(), 0.0);
        if (wallLength <= 0.0 || Math.abs(endHorizontal - startHorizontal) < 10.0) {
            return;
        }
        UUID roomId = stack.surfaceType() == SurfaceType.WALL_INTERIOR
                ? WallSurfaceTargetKey.roomId(stack.targetKey()).orElse(null)
                : null;
        List<WallSurfaceRectangle> visibleRectangles = roomId == null
                ? wallSurfaceOpeningService.visibleRectangles(activeLevel.get(), wall, sideSign)
                : wallSurfaceOpeningService.visibleRectangles(activeLevel.get(), wall, sideSign, roomId);
        if (visibleRectangles.isEmpty()) {
            return;
        }
        boolean selected = self().isSelectedSurfaceLayer(stack, layer);
        double startX = self().toScreenHorizontal(startHorizontal);
        double endX = self().toScreenHorizontal(endHorizontal);
        graphics.save();
        graphics.setFill(selected ? Color.color(0.86, 0.48, 0.18, 0.34) : Color.color(0.72, 0.58, 0.34, 0.26));
        graphics.setStroke(selected ? Color.color(0.76, 0.28, 0.10, 0.96) : Color.color(0.47, 0.36, 0.20, 0.80));
        graphics.setLineWidth(selected ? 1.8 : 1.2);
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
            double bottomY = self().toScreenVertical(-rectangle.lowerHeightMillimeters());
            double startTopY = self().toScreenVertical(-startTop);
            double endTopY = self().toScreenVertical(-endTop);
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
        drawWallSurfaceJointsInElevation(graphics, wall, layer, startX, endX, visibleRectangles, selected);
        graphics.restore();
    }

    void drawWallSurfaceJointsInElevation(
            GraphicsContext graphics,
            Wall wall,
            SurfaceLayer layer,
            double startX,
            double endX,
            List<WallSurfaceRectangle> visibleRectangles,
            boolean selected
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
                layer.minimumStartEndMargin(),
                layer.freeMargins(),
                SurfaceLayoutAnchor.AUTO,
                Length.zero(),
                Length.zero()
        );
        graphics.setStroke(selected ? Color.color(0.78, 0.24, 0.08, 0.92) : Color.color(0.16, 0.12, 0.08, 0.78));
        graphics.setLineWidth(Math.max(selected ? 1.1 : 0.7, jointWidth * self().scale()));
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

    void drawClippedWallSurfaceJointsInElevation(
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
            double centerY = self().toScreenVertical(-((clippedLowerY + clippedUpperY) / 2.0));
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
                        self().toScreenVertical(-clippedLowerY),
                        centerX,
                        self().toScreenVertical(-clippedUpperY)
                );
            }
        }
    }

    PlanPoint wallOffsetPoint(Wall wall, double localDistance, double normalOffset) {
        double wallLength = wall.axis().length().toMillimeters();
        PlanPoint axisPoint = wall.axis().pointAt(Length.ofMillimeters(Math.clamp(localDistance, 0.0, wallLength)));
        double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double length = Math.max(1.0, Math.hypot(dx, dy));
        return new PlanPoint(
                axisPoint.xMillimeters() - dy / length * normalOffset,
                axisPoint.yMillimeters() + dx / length * normalOffset
        );
    }

    double interpolateScreen(double start, double end, double ratio) {
        return start + (end - start) * Math.clamp(ratio, 0.0, 1.0);
    }

    void drawRooms(GraphicsContext graphics) {
        for (Room room : activeLevel.get().rooms()) {
            if (!ViewProjectionService.isPlanView(activeView.get())) {
                self().drawRoomElevation(graphics, room);
                continue;
            }
            double[] xPoints = room.outline().stream().mapToDouble(point -> self().toScreenProjectedX(point, 0.0)).toArray();
            double[] yPoints = room.outline().stream().mapToDouble(point -> self().toScreenProjectedY(point, 0.0)).toArray();
            boolean selected = self().isSelected(RenderableKind.ROOM_VOLUME, room.id().toString())
                    || self().isSelected(RenderableKind.ROOM_FLOOR, room.id().toString())
                    || self().isSelected(RenderableKind.ROOM_CEILING, room.id().toString());
            graphics.setFill(selected ? Color.color(0.87, 0.58, 0.24, 0.30) : Color.color(0.77, 0.64, 0.45, 0.22));
            graphics.fillPolygon(xPoints, yPoints, xPoints.length);
            graphics.setStroke(selected ? Color.color(0.78, 0.42, 0.14, 0.96) : Color.color(0.55, 0.43, 0.25, 0.8));
            graphics.setLineWidth(2.0);
            graphics.strokePolygon(xPoints, yPoints, xPoints.length);
            self().drawRoomSlopeMarker(graphics, room);
            self().drawRoomTileGrid(graphics, room);
            self().drawSelectedHeatingVarioBackground(graphics, room);
            drawFloorOpenings(graphics, room);
            drawHeatingExclusionAreas(graphics, room);
        }
    }

    HydronicHeatingLayoutService.PlanningResult heatingLayouts(HydronicHeating heating) {
        return CadWorkbenchHeatingLayoutSupport.heatingLayouts(
                heatingLayoutCache,
                hydronicHeatingLayoutService,
                heating
        );
    }

    boolean isHeatingLayoutDirty(HydronicHeating heating) {
        return CadWorkbenchHeatingLayoutSupport.isHeatingLayoutDirty(heatingLayoutsDirty, heating);
    }

    void scheduleHeatingLayoutRecalculation() {
        Set<UUID> affected = CadWorkbenchHeatingLayoutSupport.affectedHeatingIds(selectedSelections, activeLevel.get());
        if (affected.isEmpty()) {
            return;
        }
        heatingLayoutsDirty.addAll(affected);
        runHeatingLayoutRecalculation();
    }

    void scheduleHeatingLayoutRecalculation(UUID heatingId) {
        heatingLayoutsDirty.add(heatingId);
        runHeatingLayoutRecalculation();
    }

    void scheduleHeatingLayoutRecalculationForZone(UUID zoneId) {
        CadWorkbenchHeatingLayoutSupport.heatingIdForZone(activeLevel.get(), zoneId)
                .ifPresent(this::scheduleHeatingLayoutRecalculation);
    }

    void runHeatingLayoutRecalculation() {
        CadWorkbenchHeatingLayoutSupport.runHeatingLayoutRecalculation(
                activeLevel.get(),
                heatingLayoutCache,
                heatingZonesPendingRoutingRegeneration,
                heatingLayoutsDirty,
                heatingCircuitRoutingService,
                hydronicHeatingLayoutService
        );
        render();
    }

    void recomputeHeatingLayoutNow(UUID heatingId) {
        CadWorkbenchHeatingLayoutSupport.recomputeHeatingLayoutNow(
                heatingId,
                activeLevel.get(),
                heatingLayoutCache,
                heatingLayoutsDirty,
                hydronicHeatingLayoutService
        );
        render();
    }

    void clearHeatingLayoutCache() {
        CadWorkbenchHeatingLayoutSupport.clearHeatingLayoutCache(
                heatingLayoutCache,
                heatingZonesPendingRoutingRegeneration,
                heatingLayoutsDirty
        );
    }

    void drawHydronicHeatings(GraphicsContext graphics) {
        if (reportSnapshotHideHydronicHeatings || !showHeatingCircuits.get()) {
            return;
        }
        if (!ViewProjectionService.isPlanView(activeView.get())) {
            return;
        }
        List<HydronicHeating> visibleHeatings = activeLevel.get().hydronicHeatings().stream()
                .filter(this::shouldDrawHydronicHeating)
                .toList();
        CadWorkbenchHeatingRenderer.drawHydronicHeatings(
                graphics,
                visibleHeatings,
                activeLevel.get().name(),
                selectedSelections,
                this::isHeatingLayoutDirty,
                this::heatingLayouts,
                point -> self().toScreenProjectedX(point, 0.0),
                point -> self().toScreenProjectedY(point, 0.0),
                self()::scale
        );
    }

    boolean shouldDrawHydronicHeating(HydronicHeating heating) {
        return reportSnapshotVisibleHydronicSurfacePositions.isEmpty()
                || reportSnapshotVisibleHydronicSurfacePositions.contains(heating.surfacePosition());
    }

    void drawFloorOpenings(GraphicsContext graphics, Room room) {
        graphics.setFill(Color.web("#f6f1e8"));
        graphics.setLineWidth(2.0);
        for (FloorOpening opening : activeLevel.get().floorOpenings()) {
            if (!opening.roomId().equals(room.id())) {
                continue;
            }
            boolean selected = self().isSelected(RenderableKind.FLOOR_OPENING, opening.id().toString());
            graphics.setStroke(selected ? Color.web("#d97f2f") : Color.web("#8a5d32"));
            double x = self().toScreenProjectedX(new PlanPoint(opening.minXMillimeters(), opening.minYMillimeters()), 0.0);
            double y = self().toScreenProjectedY(new PlanPoint(opening.minXMillimeters(), opening.minYMillimeters()), 0.0);
            double width = opening.width().toMillimeters() * self().scale();
            double height = opening.depth().toMillimeters() * self().scale();
            if (opening.shape() == FloorOpeningShape.CIRCLE) {
                graphics.fillOval(x, y, width, height);
                graphics.strokeOval(x, y, width, height);
            } else {
                graphics.fillRect(x, y, width, height);
                graphics.strokeRect(x, y, width, height);
            }
        }
    }

    void drawHeatingExclusionAreas(GraphicsContext graphics, Room room) {
        for (HeatingExclusionArea area : activeLevel.get().heatingExclusionAreas()) {
            if (!area.roomId().equals(room.id())) {
                continue;
            }
            boolean selected = self().isSelected(RenderableKind.HEATING_EXCLUSION, area.id().toString());
            double x = self().toScreenProjectedX(new PlanPoint(area.minXMillimeters(), area.minYMillimeters()), 0.0);
            double y = self().toScreenProjectedY(new PlanPoint(area.minXMillimeters(), area.minYMillimeters()), 0.0);
            double width = area.widthMillimeters() * self().scale();
            double height = area.depthMillimeters() * self().scale();
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

    void drawTerrainElevation(GraphicsContext graphics) {
        if (ViewProjectionService.isPlanView(activeView.get())) {
            return;
        }
        List<TerrainProfileService.StripSample> strip = terrainProfileService.sampledStrip(project.terrain(), terrainContour());
        if (strip.size() < 2) {
            return;
        }
        java.util.TreeMap<Long, Double> profile = new java.util.TreeMap<>();
        for (TerrainProfileService.StripSample sample : strip) {
            long horizontal = Math.round(self().projectHorizontal(sample.outerPoint(), 0.0));
            profile.merge(horizontal, sample.elevationMillimeters(), Math::max);
        }
        if (profile.size() < 2) {
            return;
        }
        graphics.setStroke(TERRAIN_ELEVATION_COLOR);
        graphics.setLineWidth(2.4);
        Map.Entry<Long, Double> previous = null;
        for (Map.Entry<Long, Double> current : profile.entrySet()) {
            if (previous != null) {
                graphics.strokeLine(
                        self().toScreenHorizontal(previous.getKey()),
                        self().toScreenVertical(-previous.getValue()),
                        self().toScreenHorizontal(current.getKey()),
                        self().toScreenVertical(-current.getValue())
                );
            }
            previous = current;
        }
    }
}
