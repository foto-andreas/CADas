package de.schrell.cadas.ui;

import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.startsAtMaximumX;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.startsAtMaximumY;
import de.schrell.cadas.application.drawing.DimensionLineLayoutService;
import de.schrell.cadas.application.drawing.DimensionStandard;
import de.schrell.cadas.application.drawing.EdgeResizeService;
import de.schrell.cadas.application.drawing.TextBlockingBox;
import de.schrell.cadas.application.layers.SurfaceCoveringPresetService;
import de.schrell.cadas.application.layers.SurfaceRectangleTileLayoutService;
import de.schrell.cadas.application.terrain.TerrainProfileService;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoofWindow;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;
import de.schrell.cadas.domain.model.WindowElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

abstract class CadWorkbenchRenderDetails extends CadWorkbenchRender {

    void drawTerrainPlanArea(GraphicsContext graphics) {
        if (!ViewProjectionService.isPlanView(activeView.get()) || !showTerrainInPlan.get()) {
            return;
        }
        List<PlanPoint> contour = terrainContour();
        List<PlanPoint> outerOutline = terrainGeometryService.outerOutline(contour, TerrainProfileService.BAND_WIDTH_MILLIMETERS);
        if (outerOutline.size() < 3) {
            return;
        }
        double[] outerXPoints = outerOutline.stream()
                .mapToDouble(point -> self().toScreenProjectedX(point, 0.0))
                .toArray();
        double[] outerYPoints = outerOutline.stream()
                .mapToDouble(point -> self().toScreenProjectedY(point, 0.0))
                .toArray();
        double[] innerXPoints = contour.stream()
                .mapToDouble(point -> self().toScreenProjectedX(point, 0.0))
                .toArray();
        double[] innerYPoints = contour.stream()
                .mapToDouble(point -> self().toScreenProjectedY(point, 0.0))
                .toArray();
        graphics.setFill(TERRAIN_FILL_COLOR);
        graphics.fillPolygon(outerXPoints, outerYPoints, outerXPoints.length);
        graphics.setFill(CANVAS_BACKGROUND);
        graphics.fillPolygon(innerXPoints, innerYPoints, innerXPoints.length);
    }

    void drawTerrainPlanMarkers(GraphicsContext graphics) {
        if (!ViewProjectionService.isPlanView(activeView.get()) || !showTerrainInPlan.get()) {
            return;
        }
        List<PlanPoint> contour = terrainContour();
        List<PlanPoint> outerOutline = terrainGeometryService.outerOutline(contour, TerrainProfileService.BAND_WIDTH_MILLIMETERS);
        if (contour.size() < 3 || outerOutline.size() < 3) {
            return;
        }
        graphics.setStroke(TERRAIN_EDGE_COLOR);
        graphics.setFill(TERRAIN_LABEL_COLOR);
        graphics.setLineWidth(2.0);
        for (int index = 0; index < contour.size(); index++) {
            PlanPoint point = contour.get(index);
            PlanPoint next = contour.get((index + 1) % contour.size());
            double x = self().toScreenProjectedX(point, 0.0);
            double y = self().toScreenProjectedY(point, 0.0);
            graphics.strokeLine(x, y,
                    self().toScreenProjectedX(next, 0.0),
                    self().toScreenProjectedY(next, 0.0));
            PlanPoint outerPoint = outerOutline.get(index);
            graphics.strokeLine(
                    self().toScreenProjectedX(outerPoint, 0.0),
                    self().toScreenProjectedY(outerPoint, 0.0),
                    self().toScreenProjectedX(outerOutline.get((index + 1) % outerOutline.size()), 0.0),
                    self().toScreenProjectedY(outerOutline.get((index + 1) % outerOutline.size()), 0.0)
            );
        }
        for (TerrainProfileService.ProjectedTerrainPoint sample : terrainProfileService.projectedSamples(project.terrain(), contour)) {
            double x = self().toScreenProjectedX(sample.bandPoint(), 0.0);
            double y = self().toScreenProjectedY(sample.bandPoint(), 0.0);
            graphics.fillOval(x - 4.0, y - 4.0, 8.0, 8.0);
            graphics.fillText(sample.elevation().format(LengthUnit.METER, 2), x + 7.0, y - 7.0);
        }
    }

    List<TextBlockingBox> drawRoomLabels(GraphicsContext graphics) {
        List<TextBlockingBox> blockers = new ArrayList<>();
        if (!ViewProjectionService.isPlanView(activeView.get()) || !showAreaVolume.get()) {
            return blockers;
        }
        for (Room room : activeLevel.get().rooms()) {
            PlanPoint center = roomLabelCenter(room);
            blockers.addAll(drawRoomLabel(graphics, room, center));
        }
        return blockers;
    }

    void drawRoomTileGrid(GraphicsContext graphics, Room room) {
        if (!ViewProjectionService.isPlanView(activeView.get())) {
            return;
        }
        SurfaceLayerStack stack = activeLevel.get().findSurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        if (stack == null || stack.layers().isEmpty()) {
            return;
        }
        List<SurfaceLayer> visibleLayers = visiblePlanSurfaceLayers(stack);
        if (visibleLayers.isEmpty()) {
            return;
        }
        if (reportSnapshotRestrictSurfaceLayers) {
            for (SurfaceLayer layer : visibleLayers) {
                drawRoomTileLayer(graphics, room, layer, false);
            }
            return;
        }
        SurfaceLayer highlightedLayer = stack.layers().stream()
                .filter(candidate -> self().isSelectedSurfaceLayer(stack, candidate))
                .findFirst()
                .orElse(null);
        SurfaceLayer baseLayer = visibleLayers.getFirst();
        if (baseLayer == null) {
            baseLayer = highlightedLayer;
        }
        if (baseLayer == null) {
            return;
        }
        boolean baseLayerSelected = highlightedLayer != null && highlightedLayer.id().equals(baseLayer.id());
        drawRoomTileLayer(graphics, room, baseLayer, baseLayerSelected);
        if (highlightedLayer != null && !baseLayerSelected) {
            drawRoomTileLayer(graphics, room, highlightedLayer, true);
        }
    }

    void drawSelectedHeatingVarioBackground(GraphicsContext graphics, Room room) {
        if (!ViewProjectionService.isPlanView(activeView.get()) || selectedSelections.isEmpty()) {
            return;
        }
        Set<UUID> drawnLayerIds = new HashSet<>();
        activeLevel.get().hydronicHeatings().stream()
                .filter(heating -> heating.roomId().equals(room.id()))
                .filter(this::hasSelectedHeatingZone)
                .forEach(heating -> drawSelectedHeatingVarioBackground(graphics, room, heating, drawnLayerIds));
    }

    private boolean hasSelectedHeatingZone(HydronicHeating heating) {
        return heating.zones().stream()
                .anyMatch(zone -> selectedSelections.contains(new SelectionKey(
                        RenderableKind.HEATING_ZONE,
                        activeLevel.get().name(),
                        zone.id().toString()
                )));
    }

    private void drawSelectedHeatingVarioBackground(
            GraphicsContext graphics,
            Room room,
            HydronicHeating heating,
            Set<UUID> drawnLayerIds
    ) {
        SurfaceLayerStack stack = activeLevel.get().findSurfaceLayerStack(surfaceType(heating.surfacePosition()), room.id().toString());
        if (stack == null) {
            return;
        }
        SurfaceLayer baseLayer = firstVisibleSurfaceLayer(stack).orElse(null);
        SurfaceLayer selectedLayer = stack.layers().stream()
                .filter(layer -> self().isSelectedSurfaceLayer(stack, layer))
                .findFirst()
                .orElse(null);
        stack.layers().stream()
                .filter(self()::isVisibleSurfaceLayer)
                .filter(SurfaceCoveringPresetService::isVariothermDryPanelLayer)
                .filter(layer -> drawnLayerIds.add(layer.id()))
                .filter(layer -> !alreadyDrawnWithCircles(layer, baseLayer, selectedLayer))
                .forEach(layer -> drawRoomTileLayer(graphics, room, layer, false, true));
    }

    private boolean alreadyDrawnWithCircles(SurfaceLayer layer, SurfaceLayer baseLayer, SurfaceLayer selectedLayer) {
        return showVariothermCircles.get()
                && (sameLayer(layer, baseLayer) || sameLayer(layer, selectedLayer));
    }

    private boolean sameLayer(SurfaceLayer first, SurfaceLayer second) {
        return first != null && second != null && first.id().equals(second.id());
    }

    private SurfaceType surfaceType(HeatingSurfacePosition surfacePosition) {
        return surfacePosition == HeatingSurfacePosition.CEILING ? SurfaceType.CEILING : SurfaceType.FLOOR;
    }

    Optional<SurfaceLayer> firstVisibleSurfaceLayer(SurfaceLayerStack stack) {
        return visiblePlanSurfaceLayers(stack).stream().findFirst();
    }

    List<SurfaceLayer> visiblePlanSurfaceLayers(SurfaceLayerStack stack) {
        return stack.layers().stream()
                .filter(self()::isVisibleSurfaceLayer)
                .filter(layer -> !reportSnapshotRestrictSurfaceLayers || reportSnapshotVisibleSurfaceLayerIds.contains(layer.id()))
                .toList();
    }

    void drawRoomTileLayer(GraphicsContext graphics, Room room, SurfaceLayer layer, boolean highlighted) {
        drawRoomTileLayer(graphics, room, layer, highlighted, false);
    }

    void drawRoomTileLayer(GraphicsContext graphics, Room room, SurfaceLayer layer, boolean highlighted, boolean forceVariothermCircles) {
        List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> tiles = surfaceRectangleTileLayoutService.tilesForRectangles(
                floorOpeningGeometryService.floorRectangles(activeLevel.get(), room),
                layer
        );
        if (tiles.isEmpty()) {
            return;
        }
        double jointPx = Math.max(0.4, layer.jointWidth().toMillimeters() * self().scale());
        graphics.save();
        graphics.beginPath();
        PlanPoint[] outline = room.outline().toArray(new PlanPoint[0]);
        graphics.moveTo(self().toScreenX(outline[0].xMillimeters()), self().toScreenY(outline[0].yMillimeters()));
        for (int i = 1; i < outline.length; i++) {
            graphics.lineTo(self().toScreenX(outline[i].xMillimeters()), self().toScreenY(outline[i].yMillimeters()));
        }
        graphics.closePath();
        graphics.clip();
        graphics.setFill(highlighted ? Color.color(0.80, 0.28, 0.08, 0.82) : Color.color(0.35, 0.25, 0.12, 0.55));
        var horizontalKeys = new java.util.HashSet<String>();
        var verticalKeys = new java.util.HashSet<String>();
        for (SurfaceRectangleTileLayoutService.PlacedSurfaceTile tile : tiles) {
            double tx = tile.x();
            double ty = tile.y();
            double tw = tile.width();
            double th = tile.height();
            String hKey = String.format(Locale.US, "h:%.3f:%.3f:%.3f", ty + th, tx, tx + tw);
            if (horizontalKeys.add(hKey)) {
                double screenX = self().toScreenX(tx);
                double screenY = self().toScreenY(ty + th - layer.jointWidth().toMillimeters() / 2.0);
                graphics.fillRect(screenX, screenY, tw * self().scale(), jointPx);
            }
            String vKey = String.format(Locale.US, "v:%.3f:%.3f:%.3f", tx + tw, ty, ty + th);
            if (verticalKeys.add(vKey)) {
                double screenX = self().toScreenX(tx + tw - layer.jointWidth().toMillimeters() / 2.0);
                double screenY = self().toScreenY(ty);
                graphics.fillRect(screenX, screenY, jointPx, th * self().scale());
            }
            if ((showVariothermCircles.get() || forceVariothermCircles)
                    && SurfaceCoveringPresetService.isVariothermDryPanelLayer(layer)
                    && self().scale() * SurfaceCoveringPresetService.VARIOTHERM_GROOVE_PITCH_MILLIMETERS
                    >= VARIOTHERM_DETAIL_MIN_SCREEN_SPACING) {
                variothermGrooveRenderer.drawPanelGrooves(graphics, tile, self().scale(), self()::toScreenX, self()::toScreenY);
            }
        }
        if (highlighted) {
            graphics.setStroke(Color.color(0.82, 0.28, 0.08, 0.95));
            graphics.setLineWidth(1.6);
            for (SurfaceRectangleTileLayoutService.PlacedSurfaceTile tile : tiles) {
                graphics.strokeRect(
                        self().toScreenX(tile.x()),
                        self().toScreenY(tile.y()),
                        tile.width() * self().scale(),
                    tile.height() * self().scale()
                );
            }
        }
        drawSurfaceLayerDirectionArrow(graphics, layer, tiles);
        graphics.restore();
    }

    void drawSurfaceLayerDirectionArrow(
            GraphicsContext graphics,
            SurfaceLayer layer,
            List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> tiles
    ) {
        boolean verticalArrow = layer.layoutRotatedQuarterTurn();
        for (SurfaceRectangleTileLayoutService.PlacedSurfaceTile tile : orderedSurfaceTilesForDirectionArrow(layer, tiles)) {
            double tileX = self().toScreenX(tile.x());
            double tileY = self().toScreenY(tile.y());
            double tileWidth = tile.width() * self().scale();
            double tileHeight = tile.height() * self().scale();
            double primarySpan = verticalArrow ? tileHeight : tileWidth;
            double secondarySpan = verticalArrow ? tileWidth : tileHeight;
            if (primarySpan < 8.0 || secondarySpan < 4.5) {
                continue;
            }
            double padding = Math.max(1.5, Math.min(primarySpan * 0.16, secondarySpan * 0.22));
            Point2D start;
            Point2D end;
            double bubbleX;
            double bubbleY;
            double bubbleWidth;
            double bubbleHeight;
            if (verticalArrow) {
                double topY = tileY + padding;
                double bottomY = tileY + tileHeight - padding;
                if (bottomY - topY < 4.0) {
                    continue;
                }
                double centerX = tileX + tileWidth / 2.0;
                start = startsAtMaximumY(layer.layoutAnchor())
                        ? new Point2D(centerX, bottomY)
                        : new Point2D(centerX, topY);
                end = startsAtMaximumY(layer.layoutAnchor())
                        ? new Point2D(centerX, topY)
                        : new Point2D(centerX, bottomY);
                bubbleWidth = Math.max(6.0, Math.min(tileWidth - 1.0, 12.0));
                bubbleHeight = Math.max(8.0, Math.min(tileHeight - 1.0, Math.abs(end.getY() - start.getY()) + 2.0 * padding));
                bubbleX = centerX - bubbleWidth / 2.0;
                bubbleY = Math.max(tileY + 0.5, Math.min(start.getY(), end.getY()) - padding);
            } else {
                double leftX = tileX + padding;
                double rightX = tileX + tileWidth - padding;
                if (rightX - leftX < 4.0) {
                    continue;
                }
                double centerY = tileY + tileHeight / 2.0;
                start = startsAtMaximumX(layer.layoutAnchor())
                        ? new Point2D(rightX, centerY)
                        : new Point2D(leftX, centerY);
                end = startsAtMaximumX(layer.layoutAnchor())
                        ? new Point2D(leftX, centerY)
                        : new Point2D(rightX, centerY);
                bubbleWidth = Math.max(8.0, Math.min(tileWidth - 1.0, Math.abs(end.getX() - start.getX()) + 2.0 * padding));
                bubbleHeight = Math.max(6.0, Math.min(tileHeight - 1.0, 12.0));
                bubbleX = Math.max(tileX + 0.5, Math.min(start.getX(), end.getX()) - padding);
                bubbleY = centerY - bubbleHeight / 2.0;
            }
            graphics.setFill(Color.color(0.98, 0.97, 0.93, 0.82));
            graphics.fillRoundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, bubbleHeight, bubbleHeight);
            strokeSurfaceLayerDirectionArrow(graphics, start, end, Color.color(0.15, 0.12, 0.10, 0.95), Math.max(1.2, Math.min(2.2, Math.min(bubbleWidth, bubbleHeight) * 0.18)));
            return;
        }
    }

    List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> orderedSurfaceTilesForDirectionArrow(
            SurfaceLayer layer,
            List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> tiles
    ) {
        Comparator<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> rowComparator = Comparator.comparingDouble(tile ->
                startsAtMaximumY(layer.layoutAnchor()) ? -(tile.y() + tile.height()) : tile.y());
        Comparator<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> columnComparator = Comparator.comparingDouble(tile ->
                startsAtMaximumX(layer.layoutAnchor()) ? -(tile.x() + tile.width()) : tile.x());
        Comparator<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> startComparator = layer.layoutRotatedQuarterTurn()
                ? columnComparator.thenComparing(rowComparator)
                : rowComparator.thenComparing(columnComparator);
        return tiles.stream()
                .sorted(startComparator
                        .thenComparingDouble(SurfaceRectangleTileLayoutService.PlacedSurfaceTile::y)
                        .thenComparingDouble(SurfaceRectangleTileLayoutService.PlacedSurfaceTile::x))
                .toList();
    }

    void strokeSurfaceLayerDirectionArrow(GraphicsContext graphics, Point2D start, Point2D end, Color color, double lineWidth) {
        Point2D delta = end.subtract(start);
        double length = delta.magnitude();
        if (length < 1.0) {
            return;
        }
        Point2D unit = delta.normalize();
        Point2D normal = new Point2D(-unit.getY(), unit.getX());
        double headLength = Math.min(10.0, Math.max(3.5, length * 0.20));
        double headWidth = headLength * 0.55;
        Point2D headBase = end.subtract(unit.multiply(headLength));
        graphics.setStroke(color);
        graphics.setLineWidth(lineWidth);
        graphics.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
        graphics.strokeLine(
                end.getX(),
                end.getY(),
                headBase.getX() + normal.getX() * headWidth,
                headBase.getY() + normal.getY() * headWidth
        );
        graphics.strokeLine(
                end.getX(),
                end.getY(),
                headBase.getX() - normal.getX() * headWidth,
                headBase.getY() - normal.getY() * headWidth
        );
    }

    double modulo(double value, double modulus) {
        if (Math.abs(modulus) <= 0.001) {
            return 0.0;
        }
        double result = value % modulus;
        return result < 0.0 ? result + modulus : result;
    }

    List<TextBlockingBox> drawRoomLabel(GraphicsContext graphics, Room room, PlanPoint center) {
        List<TextBlockingBox> blockers = new ArrayList<>();
        double centerX = self().toScreenProjectedX(center, 0.0);
        double centerY = self().toScreenProjectedY(center, 0.0);
        graphics.save();
        graphics.setFill(Color.web("#5d4527"));
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setFont(Font.font("Menlo", 12));
        String name = room.name();
        graphics.fillText(name, centerX, centerY - 6);
        blockers.add(centeredTextBlockingBox(name, Font.font("Menlo", 12), centerX, centerY - 6));
        String areaVolume = roomMetricsText(room);
        graphics.setFont(Font.font("Menlo", 11));
        graphics.fillText(areaVolume, centerX, centerY + 12);
        blockers.add(centeredTextBlockingBox(areaVolume, Font.font("Menlo", 11), centerX, centerY + 12));
        graphics.restore();
        return blockers;
    }

    String roomMetricsText(Room room) {
        return String.format(
                Locale.GERMAN,
                "%.2f m² | %.2f m³ | U %.2f m",
                surfaceLayerEffectService.effectiveAreaSquareMeters(activeLevel.get(), room),
                surfaceLayerEffectService.effectiveVolumeCubicMeters(activeLevel.get(), room),
                de.schrell.cadas.domain.geometry.PlanPolygonSupport.perimeterMillimeters(room.outline()) / 1000.0
        );
    }

    PlanPoint roomLabelCenter(Room room) {
        PlanPoint center = room.areaCentroid();
        if (de.schrell.cadas.domain.geometry.PlanPolygonSupport.containsPoint(room.outline(), center)) {
            return center;
        }
        return new de.schrell.cadas.application.room.OrthogonalPolygonDecompositionService().decompose(room.outline()).stream()
                .max(Comparator.comparingDouble(rectangle -> rectangle.width() * rectangle.height()))
                .map(rectangle -> new PlanPoint(rectangle.centerX(), rectangle.centerY()))
                .orElse(center);
    }

    TextBlockingBox centeredTextBlockingBox(String text, Font font, double centerX, double y) {
        Text measure = new Text(text);
        measure.setFont(font);
        var bounds = measure.getLayoutBounds();
        double boxX = centerX - bounds.getWidth() / 2.0 - DIMENSION_TEXT_PADDING;
        double boxY = y + bounds.getMinY() - DIMENSION_TEXT_PADDING;
        return new TextBlockingBox(
                boxX,
                boxY,
                bounds.getWidth() + DIMENSION_TEXT_PADDING * 2.0,
                bounds.getHeight() + DIMENSION_TEXT_PADDING * 2.0
        );
    }

    void drawRoomSlopeMarker(GraphicsContext graphics, Room room) {
        if (!ViewProjectionService.isPlanView(activeView.get()) || room.slopedCeilingProfiles().isEmpty()) {
            return;
        }
        for (int index = 0; index < room.slopedCeilingProfiles().size(); index++) {
            drawRoomSlopeMarker(graphics, room, room.slopedCeilingProfiles().get(index), index);
        }
    }

    void drawRoomSlopeMarker(
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
                self().toScreenProjectedX(start, 0.0),
                self().toScreenProjectedY(start, 0.0),
                self().toScreenProjectedX(end, 0.0),
                self().toScreenProjectedY(end, 0.0)
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
                self().toScreenProjectedX(room.centerPoint(), 0.0) - 72,
                self().toScreenProjectedY(room.centerPoint(), 0.0) + 28 + labelIndex * 14.0
        );
    }

    void drawSlopeArrow(GraphicsContext graphics, PlanPoint arrowCenter, SlopedCeilingSide lowSide) {
        double arrowLength = 28.0;
        double startX = self().toScreenProjectedX(arrowCenter, 0.0);
        double startY = self().toScreenProjectedY(arrowCenter, 0.0);
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

    void drawDoors(GraphicsContext graphics) {
        for (Door door : activeLevel.get().doors()) {
            Wall hostWall = activeLevel.get().findWall(door.wallId());
            PlanPoint openingStart = hostWall.axis().pointAt(door.offsetFromStart());
            PlanPoint openingEnd = hostWall.axis().pointAt(door.offsetFromStart().add(door.width()));
            boolean selected = self().isSelected(RenderableKind.DOOR, door.id().toString());
            if (!ViewProjectionService.isPlanView(activeView.get())) {
                drawOpeningElevation(graphics, openingStart, openingEnd, door.thresholdHeight().toMillimeters(), door.height().toMillimeters(), selected ? Color.web("#f08f3c") : Color.web("#d66b2d"));
                continue;
            }
            graphics.save();
            graphics.setLineCap(javafx.scene.shape.StrokeLineCap.BUTT);
            graphics.setStroke(selected ? Color.web("#f08f3c") : Color.web("#d66b2d"));
            graphics.setLineWidth(Math.max(hostWall.thickness().toMillimeters() * self().scale() * 0.55, 3.0));
            graphics.strokeLine(
                    self().toScreenProjectedX(openingStart, 0.0),
                    self().toScreenProjectedY(openingStart, 0.0),
                    self().toScreenProjectedX(openingEnd, 0.0),
                    self().toScreenProjectedY(openingEnd, 0.0)
            );
            graphics.restore();
        }
    }

    void drawWindows(GraphicsContext graphics) {
        for (WindowElement window : activeLevel.get().windows()) {
            Wall hostWall = activeLevel.get().findWall(window.wallId());
            PlanPoint openingStart = hostWall.axis().pointAt(window.offsetFromStart());
            PlanPoint openingEnd = hostWall.axis().pointAt(window.offsetFromStart().add(window.width()));
            boolean selected = self().isSelected(RenderableKind.WINDOW, window.id().toString());
            if (!ViewProjectionService.isPlanView(activeView.get())) {
                drawOpeningElevation(graphics, openingStart, openingEnd, window.sillHeight().toMillimeters(), window.windowHeight().toMillimeters(), selected ? Color.web("#7bc8eb") : Color.web("#4da8da"));
                continue;
            }
            graphics.save();
            graphics.setLineCap(javafx.scene.shape.StrokeLineCap.BUTT);
            graphics.setStroke(selected ? Color.web("#7bc8eb") : Color.web("#4da8da"));
            graphics.setLineWidth(Math.max(hostWall.thickness().toMillimeters() * self().scale() * 0.35, 3.0));
            graphics.strokeLine(
                    self().toScreenProjectedX(openingStart, 0.0),
                    self().toScreenProjectedY(openingStart, 0.0),
                    self().toScreenProjectedX(openingEnd, 0.0),
                    self().toScreenProjectedY(openingEnd, 0.0)
            );
            graphics.restore();
        }
    }

    void drawRoofWindows(GraphicsContext graphics) {
        if (!ViewProjectionService.isPlanView(activeView.get())) {
            return;
        }
        for (RoofWindow roofWindow : activeLevel.get().roofWindows()) {
            boolean selected = self().isSelected(RenderableKind.ROOF_WINDOW, roofWindow.id().toString());
            graphics.save();
            graphics.setFill(selected ? Color.color(0.35, 0.72, 0.92, 0.42) : Color.color(0.35, 0.72, 0.92, 0.25));
            graphics.setStroke(selected ? Color.web("#d97f2f") : Color.web("#2c789f"));
            graphics.setLineWidth(selected ? 2.8 : 1.8);
            double x = self().toScreenProjectedX(new PlanPoint(
                    roofWindow.center().xMillimeters() - roofWindow.width().toMillimeters() / 2.0,
                    roofWindow.center().yMillimeters() - roofWindow.depth().toMillimeters() / 2.0
            ), 0.0);
            double y = self().toScreenProjectedY(new PlanPoint(
                    roofWindow.center().xMillimeters() - roofWindow.width().toMillimeters() / 2.0,
                    roofWindow.center().yMillimeters() - roofWindow.depth().toMillimeters() / 2.0
            ), 0.0);
            double width = roofWindow.width().toMillimeters() * self().scale();
            double depth = roofWindow.depth().toMillimeters() * self().scale();
            graphics.fillRect(x, y, width, depth);
            graphics.strokeRect(x, y, width, depth);
            graphics.strokeLine(x, y, x + width, y + depth);
            graphics.strokeLine(x + width, y, x, y + depth);
            graphics.restore();
        }
    }

    void drawRoofWindowOutline(GraphicsContext graphics, RoofWindow roofWindow) {
        double x = self().toScreenProjectedX(new PlanPoint(
                roofWindow.center().xMillimeters() - roofWindow.width().toMillimeters() / 2.0,
                roofWindow.center().yMillimeters() - roofWindow.depth().toMillimeters() / 2.0
        ), 0.0);
        double y = self().toScreenProjectedY(new PlanPoint(
                roofWindow.center().xMillimeters() - roofWindow.width().toMillimeters() / 2.0,
                roofWindow.center().yMillimeters() - roofWindow.depth().toMillimeters() / 2.0
        ), 0.0);
        graphics.strokeRect(x, y,
                roofWindow.width().toMillimeters() * self().scale(),
                roofWindow.depth().toMillimeters() * self().scale());
    }

    void drawEditablePoints(GraphicsContext graphics) {
        if (self().currentTool() != DrawingTool.EDIT || !ViewProjectionService.isPlanView(activeView.get())) {
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
            boolean selected = self().isSelected(RenderableKind.DOOR, door.id().toString());
            drawOpeningEditablePoints(graphics, wall, door.offsetFromStart(), door.width(), Color.web("#d66b2d"), selected);
        }
        for (WindowElement window : activeLevel.get().windows()) {
            Wall wall = activeLevel.get().findWall(window.wallId());
            boolean selected = self().isSelected(RenderableKind.WINDOW, window.id().toString());
            drawOpeningEditablePoints(graphics, wall, window.offsetFromStart(), window.width(), Color.web("#4da8da"), selected);
        }
    }

    void drawEdgeResizeHandles(GraphicsContext graphics) {
        if (self().currentTool() != DrawingTool.EDIT || !ViewProjectionService.isPlanView(activeView.get())) {
            return;
        }
        for (EdgeResizeService.EdgeHandle handle : edgeResizeService.handles(activeLevel.get(), Set.copyOf(selectedSelections))) {
            double x = self().toScreenProjectedX(handle.position(), 0.0);
            double y = self().toScreenProjectedY(handle.position(), 0.0);
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

    void drawOpeningEditablePoints(GraphicsContext graphics, Wall wall, Length offset, Length width, Color color, boolean selected) {
        drawEditablePoint(graphics, wall.axis().pointAt(offset), color, selected ? 0.52 : 0.24, selected ? 6.0 : 5.0);
        drawEditablePoint(graphics, wall.axis().pointAt(offset.add(width)), color, selected ? 0.52 : 0.24, selected ? 6.0 : 5.0);
    }

    void drawEditablePoint(GraphicsContext graphics, PlanPoint point, Color color, double fillOpacity, double radius) {
        double centerX = self().toScreenProjectedX(point, 0.0);
        double centerY = self().toScreenProjectedY(point, 0.0);
        graphics.save();
        graphics.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), fillOpacity));
        graphics.setStroke(Color.web("#201c18"));
        graphics.setLineWidth(1.2);
        graphics.fillOval(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0);
        graphics.strokeOval(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0);
        graphics.restore();
    }

    boolean samePlanPoint(PlanPoint first, PlanPoint second) {
        return first.distanceTo(second).toMillimeters() < 0.001;
    }

    void drawRoomObjects(GraphicsContext graphics) {
        if (!showRoomObjects.get()) {
            return;
        }
        for (RoomObject roomObject : activeLevel.get().roomObjects()) {
            if (!roomObject.visible()) {
                continue;
            }
            if (!shouldDrawRoomObject(roomObject)) {
                continue;
            }
            boolean selected = self().isSelected(RenderableKind.ROOM_OBJECT, roomObject.id().toString());
            graphics.setStroke(selected ? Color.web("#d97f2f") : Color.web("#356f62"));
            graphics.setFill(selected ? Color.color(0.85, 0.50, 0.18, 0.30) : Color.color(0.22, 0.44, 0.39, 0.22));
            graphics.setLineWidth(1.8);
            double width = roomObject.footprintWidthMillimeters();
            double depth = roomObject.footprintDepthMillimeters();
            if (ViewProjectionService.isPlanView(activeView.get())) {
                drawRoomObjectPlan(graphics, roomObject);
            } else {
                double x = self().toScreenProjectedX(roomObject.center(), 0.0) - width * self().scale() / 2.0;
                double y = self().toScreenProjectedY(roomObject.center(), roomObject.baseElevation().toMillimeters() + roomObject.height().toMillimeters());
                graphics.strokeRect(x, y, width * self().scale(), roomObject.height().toMillimeters() * self().scale());
            }
        }
    }

    boolean shouldDrawRoomObject(RoomObject roomObject) {
        if (!reportSnapshotFilterHeatingRoomObjects || !isHeatingRoomObject(roomObject)) {
            return true;
        }
        return reportSnapshotVisibleHeatingObjectTypes.contains(roomObject.heatingType());
    }

    boolean isHeatingRoomObject(RoomObject roomObject) {
        return roomObject.heatOutputWatts() > 0.0 && roomObject.heatingType().isHeated();
    }

    void drawRoomObjectPlan(GraphicsContext graphics, RoomObject roomObject) {
        double w = roomObject.width().toMillimeters() * self().scale();
        double h = roomObject.depth().toMillimeters() * self().scale();
        double x = -w / 2.0;
        double y = -h / 2.0;
        graphics.save();
        graphics.translate(self().toScreenX(roomObject.center().xMillimeters()), self().toScreenY(roomObject.center().yMillimeters()));
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
        graphics.save();
        graphics.setFill(Color.web("#183f37"));
        graphics.setFont(Font.font("Menlo", 11));
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.fillText(
                roomObjectPresetService.displayName(roomObject, availableRoomObjectPresets),
                self().toScreenX(roomObject.center().xMillimeters()),
                self().toScreenY(roomObject.center().yMillimeters()) + 4.0
        );
        graphics.restore();
    }

    void drawFloorExtensions(GraphicsContext graphics) {
        for (FloorExtension extension : activeLevel.get().floorExtensions()) {
            boolean selected = self().isSelected(RenderableKind.FLOOR_EXTENSION, extension.id().toString());
            graphics.save();
            graphics.setStroke(selected ? Color.web("#d97f2f") : Color.web("#806b4f"));
            graphics.setFill(selected ? Color.color(0.85, 0.50, 0.18, 0.28) : Color.color(0.58, 0.49, 0.36, 0.20));
            graphics.setLineWidth(1.8);
            if (ViewProjectionService.isPlanView(activeView.get())) {
                double x = self().toScreenX(extension.minX());
                double y = self().toScreenY(extension.minY());
                double width = extension.widthMillimeters() * self().scale();
                double depth = extension.depthMillimeters() * self().scale();
                graphics.fillRect(x, y, width, depth);
                graphics.strokeRect(x, y, width, depth);
                graphics.setFill(Color.web("#4b4034"));
                graphics.setFont(Font.font("Menlo", 11));
                graphics.fillText(extension.type().toString(), x + 8, y + 18);
            } else {
                double[] horizontals = extension.outline().stream().mapToDouble(point -> self().projectHorizontal(point, 0.0)).toArray();
                double min = java.util.Arrays.stream(horizontals).min().orElse(0.0);
                double max = java.util.Arrays.stream(horizontals).max().orElse(0.0);
                double left = self().toScreenHorizontal(min);
                double right = self().toScreenHorizontal(max);
                double floor = self().toScreenVertical(0.0);
                double bottom = self().toScreenVertical(extension.slabThickness().toMillimeters());
                graphics.fillRect(Math.min(left, right), Math.min(floor, bottom), Math.max(2, Math.abs(right - left)), Math.max(2, Math.abs(bottom - floor)));
                graphics.strokeRect(Math.min(left, right), Math.min(floor, bottom), Math.max(2, Math.abs(right - left)), Math.max(2, Math.abs(bottom - floor)));
            }
            graphics.restore();
        }
    }

    void drawStaircases(GraphicsContext graphics) {
        for (Staircase staircase : activeLevel.get().staircases()) {
            boolean selected = self().isSelected(RenderableKind.STAIR, staircase.id().toString());
            graphics.setStroke(selected ? Color.web("#8a6848") : Color.web("#5e503f"));
            graphics.setFill(selected ? Color.color(0.63, 0.47, 0.27, 0.24) : Color.color(0.52, 0.46, 0.37, 0.16));
            graphics.setLineWidth(2.0);
            if (!ViewProjectionService.isPlanView(activeView.get())) {
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

    void drawStraightStairTreads(GraphicsContext graphics, Staircase staircase) {
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

    void drawHalfTurnStair(GraphicsContext graphics, Staircase staircase) {
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

    void drawSwitchbackStair(GraphicsContext graphics, Staircase staircase) {
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

    void drawSpiralStair(GraphicsContext graphics, Staircase staircase) {
        graphics.strokeOval(
                self().toScreenProjectedX(new PlanPoint(staircase.minX(), staircase.minY()), 0.0),
                self().toScreenProjectedY(new PlanPoint(staircase.minX(), staircase.minY()), 0.0),
                staircase.widthMillimeters() * self().scale(),
                staircase.heightMillimeters() * self().scale()
        );
        graphics.strokeOval(
                self().toScreenProjectedX(new PlanPoint(staircase.minX() + staircase.widthMillimeters() * 0.25, staircase.minY() + staircase.heightMillimeters() * 0.25), 0.0),
                self().toScreenProjectedY(new PlanPoint(staircase.minX() + staircase.widthMillimeters() * 0.25, staircase.minY() + staircase.heightMillimeters() * 0.25), 0.0),
                staircase.widthMillimeters() * self().scale() * 0.5,
                staircase.heightMillimeters() * self().scale() * 0.5
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
                    self().toScreenProjectedX(center, 0.0),
                    self().toScreenProjectedY(center, 0.0),
                    self().toScreenProjectedX(outer, 0.0),
                    self().toScreenProjectedY(outer, 0.0)
            );
        }
    }

    void drawStairOutline(GraphicsContext graphics, Staircase staircase) {
        double[] xPoints = {
                self().toScreenProjectedX(staircase.pointAtLocalPosition(0, 0), 0.0),
                self().toScreenProjectedX(staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0), 0.0),
                self().toScreenProjectedX(staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()), 0.0),
                self().toScreenProjectedX(staircase.pointAtLocalPosition(0, staircase.heightMillimeters()), 0.0)
        };
        double[] yPoints = {
                self().toScreenProjectedY(staircase.pointAtLocalPosition(0, 0), 0.0),
                self().toScreenProjectedY(staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0), 0.0),
                self().toScreenProjectedY(staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()), 0.0),
                self().toScreenProjectedY(staircase.pointAtLocalPosition(0, staircase.heightMillimeters()), 0.0)
        };
        graphics.fillPolygon(xPoints, yPoints, 4);
        graphics.strokePolygon(xPoints, yPoints, 4);
    }

    void strokeLocalRect(GraphicsContext graphics, Staircase staircase, double localX, double localY, double localWidth, double localHeight) {
        strokeLocalLine(graphics, staircase, localX, localY, localX + localWidth, localY);
        strokeLocalLine(graphics, staircase, localX + localWidth, localY, localX + localWidth, localY + localHeight);
        strokeLocalLine(graphics, staircase, localX + localWidth, localY + localHeight, localX, localY + localHeight);
        strokeLocalLine(graphics, staircase, localX, localY + localHeight, localX, localY);
    }

    void strokeLocalLine(GraphicsContext graphics, Staircase staircase, double startLocalX, double startLocalY, double endLocalX, double endLocalY) {
        PlanPoint start = staircase.pointAtLocalPosition(startLocalX, startLocalY);
        PlanPoint end = staircase.pointAtLocalPosition(endLocalX, endLocalY);
        graphics.strokeLine(
                self().toScreenProjectedX(start, 0.0),
                self().toScreenProjectedY(start, 0.0),
                self().toScreenProjectedX(end, 0.0),
                self().toScreenProjectedY(end, 0.0)
        );
    }

    void drawWall(GraphicsContext graphics, PlanSegment segment, Length thickness, Color color, double widthFactor) {
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
                self().toScreenProjectedX(p1, 0.0),
                self().toScreenProjectedX(p2, 0.0),
                self().toScreenProjectedX(p3, 0.0),
                self().toScreenProjectedX(p4, 0.0)
        };
        double[] yPoints = {
                self().toScreenProjectedY(p1, 0.0),
                self().toScreenProjectedY(p2, 0.0),
                self().toScreenProjectedY(p3, 0.0),
                self().toScreenProjectedY(p4, 0.0)
        };
        graphics.setFill(color);
        graphics.fillPolygon(xPoints, yPoints, 4);
        graphics.setStroke(color);
        graphics.setLineWidth(1.0);
        graphics.strokePolygon(xPoints, yPoints, 4);
    }

    void drawWallElevation(GraphicsContext graphics, Wall wall, boolean selected) {
        List<WallProfilePoint> profile = wall.resolvedProfile();
        double[] xPoints = new double[profile.size() + 2];
        double[] yPoints = new double[profile.size() + 2];
        xPoints[0] = self().toScreenProjectedX(wall.axis().start(), 0.0);
        yPoints[0] = self().toScreenProjectedY(wall.axis().start(), 0.0);
        xPoints[1] = self().toScreenProjectedX(wall.axis().end(), 0.0);
        yPoints[1] = self().toScreenProjectedY(wall.axis().end(), 0.0);
        for (int index = 0; index < profile.size(); index++) {
            var profilePoint = profile.get(profile.size() - 1 - index);
            PlanPoint point = wall.axis().pointAt(profilePoint.offset());
            xPoints[index + 2] = self().toScreenProjectedX(point, profilePoint.height().toMillimeters());
            yPoints[index + 2] = self().toScreenProjectedY(point, profilePoint.height().toMillimeters());
        }
        graphics.setFill(selected ? Color.color(0.85, 0.57, 0.22, 0.24) : Color.color(0.23, 0.39, 0.54, 0.18));
        graphics.fillPolygon(xPoints, yPoints, profile.size() + 2);
        graphics.setStroke(selected ? Color.web("#d97f2f") : CadColorPalette.WALL);
        graphics.setLineWidth(2.0);
        graphics.strokePolygon(xPoints, yPoints, profile.size() + 2);
    }

    void drawRoomElevation(GraphicsContext graphics, Room room) {
        double minProjectedX = room.outline().stream()
                .mapToDouble(point -> self().projectHorizontal(point, 0.0))
                .min()
                .orElse(0.0);
        double maxProjectedX = room.outline().stream()
                .mapToDouble(point -> self().projectHorizontal(point, 0.0))
                .max()
                .orElse(0.0);
        double left = self().toScreenHorizontal(minProjectedX);
        double right = self().toScreenHorizontal(maxProjectedX);
        double floorY = self().toScreenVertical(0.0);
        double topY = self().toScreenVertical(-surfaceLayerEffectService.effectiveMaximumCeilingHeightMillimeters(activeLevel.get(), room));
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

    boolean isSlopeVisibleInCurrentElevation(Room room) {
        return switch (activeView.get()) {
            case EAST, WEST -> room.slopeVisibleInEastWestView();
            case NORTH, SOUTH -> room.slopeVisibleInNorthSouthView();
            default -> false;
        };
    }

    void drawSlopedRoomElevation(GraphicsContext graphics, Room room, double left, double right, double floorY, double topY) {
        if (room.ceilingVertexHeightsProfile().isPresent()) {
            drawPolygonalRoomElevation(graphics, room, floorY);
            return;
        }
        double lowY = self().toScreenVertical(-surfaceLayerEffectService.effectiveMinimumCeilingHeightMillimeters(activeLevel.get(), room));
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

    void drawPolygonalRoomElevation(GraphicsContext graphics, Room room, double floorY) {
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
            xPoints[pointIndex] = self().toScreenHorizontal(entry.getKey());
            yPoints[pointIndex] = self().toScreenVertical(-entry.getValue());
            pointIndex++;
        }
        List<Map.Entry<Long, Double>> entries = new ArrayList<>(topProfile.entrySet());
        for (int reverseIndex = entries.size() - 1; reverseIndex >= 0; reverseIndex--) {
            xPoints[pointIndex] = self().toScreenHorizontal(entries.get(reverseIndex).getKey());
            yPoints[pointIndex] = floorY;
            pointIndex++;
        }
        graphics.setFill(Color.color(0.77, 0.64, 0.45, 0.16));
        graphics.fillPolygon(xPoints, yPoints, xPoints.length);
        graphics.setStroke(Color.color(0.55, 0.43, 0.25, 0.72));
        graphics.setLineWidth(1.8);
        graphics.strokePolygon(xPoints, yPoints, xPoints.length);
    }

    void addElevationSample(java.util.TreeMap<Long, Double> topProfile, PlanPoint point, double ceilingHeightMillimeters) {
        long horizontal = Math.round(self().projectHorizontal(point, 0.0));
        topProfile.merge(horizontal, ceilingHeightMillimeters, Math::max);
    }

    void drawStairElevation(GraphicsContext graphics, Staircase staircase) {
        double[] projectedHorizontals = {
                self().projectHorizontal(staircase.pointAtLocalPosition(0, 0), 0.0),
                self().projectHorizontal(staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0), 0.0),
                self().projectHorizontal(staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()), 0.0),
                self().projectHorizontal(staircase.pointAtLocalPosition(0, staircase.heightMillimeters()), 0.0)
        };
        double minHorizontal = java.util.Arrays.stream(projectedHorizontals).min().orElse(0.0);
        double maxHorizontal = java.util.Arrays.stream(projectedHorizontals).max().orElse(0.0);
        double left = self().toScreenHorizontal(minHorizontal);
        double right = self().toScreenHorizontal(maxHorizontal);
        double floorY = self().toScreenVertical(0.0);
        double topY = self().toScreenVertical(-staircase.totalHeight().toMillimeters());
        graphics.fillRect(Math.min(left, right), Math.min(floorY, topY), Math.max(Math.abs(right - left), 3.0), Math.max(Math.abs(floorY - topY), 3.0));
        graphics.strokeRect(Math.min(left, right), Math.min(floorY, topY), Math.max(Math.abs(right - left), 3.0), Math.max(Math.abs(floorY - topY), 3.0));
    }

    void drawOpeningElevation(GraphicsContext graphics, PlanPoint openingStart, PlanPoint openingEnd, double baseHeightMillimeters, double openingHeightMillimeters, Color color) {
        double startX = self().toScreenProjectedX(openingStart, 0.0);
        double endX = self().toScreenProjectedX(openingEnd, 0.0);
        double bottomY = self().toScreenVertical(-baseHeightMillimeters);
        double topY = self().toScreenVertical(-(baseHeightMillimeters + openingHeightMillimeters));
        graphics.setStroke(color);
        graphics.setLineWidth(2.8);
        graphics.strokeRect(Math.min(startX, endX), Math.min(bottomY, topY), Math.max(Math.abs(endX - startX), 3.0), Math.max(Math.abs(bottomY - topY), 3.0));
    }

    void drawPreview(GraphicsContext graphics) {
        if (!ViewProjectionService.isPlanView(activeView.get())) {
            return;
        }
        double startX = Math.min(previewSegment.start().xMillimeters(), previewSegment.end().xMillimeters());
        double startY = Math.min(previewSegment.start().yMillimeters(), previewSegment.end().yMillimeters());
        double endX = Math.max(previewSegment.start().xMillimeters(), previewSegment.end().xMillimeters());
        double endY = Math.max(previewSegment.start().yMillimeters(), previewSegment.end().yMillimeters());
        if (self().currentTool() == DrawingTool.STAIR
                || self().currentTool() == DrawingTool.FLOOR_EXTENSION
                || self().currentTool() == DrawingTool.FLOOR_OPENING_RECTANGLE
                || self().currentTool() == DrawingTool.FLOOR_OPENING_CIRCLE
                || self().currentTool() == DrawingTool.HEATING_ZONE_RECTANGLE
                || self().currentTool() == DrawingTool.HEATING_EXCLUSION_RECTANGLE
                || self().currentTool() == DrawingTool.HEATING_MANIFOLD) {
            graphics.setFill(Color.color(0.45, 0.37, 0.29, 0.18));
            graphics.setStroke(Color.web("#7f6a55"));
            graphics.setLineWidth(2.0);
            double previewX = self().toScreenProjectedX(new PlanPoint(startX, startY), 0.0);
            double previewY = self().toScreenProjectedY(new PlanPoint(startX, startY), 0.0);
            double previewWidth = (endX - startX) * self().scale();
            double previewHeight = (endY - startY) * self().scale();
            if (self().currentTool() == DrawingTool.FLOOR_OPENING_CIRCLE) {
                double diameter = Math.min(previewWidth, previewHeight);
                graphics.fillOval(previewX, previewY, diameter, diameter);
                graphics.strokeOval(previewX, previewY, diameter, diameter);
            } else {
                graphics.fillRect(previewX, previewY, previewWidth, previewHeight);
                graphics.strokeRect(previewX, previewY, previewWidth, previewHeight);
            }
            if (self().currentTool() == DrawingTool.HEATING_MANIFOLD) {
                drawHeatingManifoldPreviewMarkers(graphics, startX, startY, endX, endY);
            }
        } else {
            drawWall(graphics, previewSegment, self().currentWallThickness(), Color.web("#c26d32"), 1.0);
        }
        drawDimensionLabel(
                graphics,
                previewSegment,
                previewSegment.length().format(LengthUnit.METER, 2) + " | " + previewSegment.angle().format()
        );
    }

    void drawHeatingManifoldPreviewMarkers(GraphicsContext graphics, double startX, double startY, double endX, double endY) {
        double centerX = (startX + endX) / 2.0;
        double centerY = (startY + endY) / 2.0;
        double width = endX - startX;
        double height = endY - startY;
        boolean horizontal = width >= height;
        double halfPitch = DEFAULT_HKV_PAIR_DISTANCE_MILLIMETERS / 2.0;
        PlanPoint supplyPoint = horizontal
                ? new PlanPoint(centerX - halfPitch, centerY)
                : new PlanPoint(centerX, centerY - halfPitch);
        PlanPoint returnPoint = horizontal
                ? new PlanPoint(centerX + halfPitch, centerY)
                : new PlanPoint(centerX, centerY + halfPitch);
        CadWorkbenchHeatingRenderer.drawConnectionMarker(
                graphics,
                supplyPoint,
                "V",
                Color.web("#1f62d0"),
                point -> self().toScreenProjectedX(point, 0.0),
                point -> self().toScreenProjectedY(point, 0.0)
        );
        CadWorkbenchHeatingRenderer.drawConnectionMarker(
                graphics,
                returnPoint,
                "R",
                Color.web("#d33b32"),
                point -> self().toScreenProjectedX(point, 0.0),
                point -> self().toScreenProjectedY(point, 0.0)
        );
    }

    void drawDimensionLabel(GraphicsContext graphics, PlanSegment segment, String text) {
        drawDimensionLabel(graphics, segment, text, 0.0);
    }

    void drawDimensionLabel(GraphicsContext graphics, PlanSegment segment, String text, double normalOffset) {
        double startX = self().toScreenProjectedX(segment.start(), 0.0);
        double startY = self().toScreenProjectedY(segment.start(), 0.0);
        double endX = self().toScreenProjectedX(segment.end(), 0.0);
        double endY = self().toScreenProjectedY(segment.end(), 0.0);
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

    DimensionStandard currentDimensionStandard() {
        return DimensionStandard.DIN_EN_ISO_7519_2025_01;
    }

    void drawIsoDimensionLines(
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

    void drawViewOverlay(GraphicsContext graphics) {
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

    void drawCompass(GraphicsContext graphics) {
        if (!ViewProjectionService.isPlanView(activeView.get())) {
            return;
        }
        double x = drawingCanvas.getWidth() - 78;
        double y = 34;
        CompassArrow arrow = compassArrow(x, y, compassDisplayAngleDegrees(self().currentNorthAngleDegrees(), activeView.get()));
        graphics.setStroke(Color.web("#4b6a88"));
        graphics.setFill(Color.web("#4b6a88"));
        graphics.setLineWidth(2);
        graphics.strokeOval(x - 18, y - 18, 36, 36);
        graphics.strokeLine(arrow.tail().getX(), arrow.tail().getY(), arrow.tip().getX(), arrow.tip().getY());
        graphics.strokeLine(arrow.tip().getX(), arrow.tip().getY(), arrow.leftWing().getX(), arrow.leftWing().getY());
        graphics.strokeLine(arrow.tip().getX(), arrow.tip().getY(), arrow.rightWing().getX(), arrow.rightWing().getY());
        Point2D label = compassLabelPosition(x, y, compassDisplayAngleDegrees(self().currentNorthAngleDegrees(), activeView.get()));
        graphics.fillText("N", label.getX() - 4, label.getY() + 4);
    }

    static CompassArrow compassArrow(double centerX, double centerY, double angleDegrees) {
        double angle = Math.toRadians(angleDegrees);
        double directionX = Math.sin(angle);
        double directionY = -Math.cos(angle);
        double perpendicularX = -directionY;
        double perpendicularY = directionX;
        double arrowLength = 14.0;
        double headBack = 6.0;
        double headSide = 4.0;
        Point2D tip = new Point2D(centerX + directionX * arrowLength, centerY + directionY * arrowLength);
        Point2D tail = new Point2D(centerX - directionX * arrowLength, centerY - directionY * arrowLength);
        Point2D leftWing = new Point2D(
                tip.getX() - directionX * headBack + perpendicularX * headSide,
                tip.getY() - directionY * headBack + perpendicularY * headSide
        );
        Point2D rightWing = new Point2D(
                tip.getX() - directionX * headBack - perpendicularX * headSide,
                tip.getY() - directionY * headBack - perpendicularY * headSide
        );
        return new CompassArrow(tail, tip, leftWing, rightWing);
    }

    static double compassDisplayAngleDegrees(double northAngleDegrees, ViewOrientation orientation) {
        return orientation.cameraAzimuthDegrees() - northAngleDegrees;
    }

    static Point2D compassLabelPosition(double centerX, double centerY, double angleDegrees) {
        double angle = Math.toRadians(angleDegrees);
        double labelDistance = 25.0;
        return new Point2D(
                centerX + Math.sin(angle) * labelDistance,
                centerY - Math.cos(angle) * labelDistance
        );
    }

    record CompassArrow(Point2D tail, Point2D tip, Point2D leftWing, Point2D rightWing) {
    }

    void drawRulers() {
        drawHorizontalRuler(horizontalRuler.getGraphicsContext2D());
        drawVerticalRuler(verticalRuler.getGraphicsContext2D());
    }

    void drawHorizontalRuler(GraphicsContext graphics) {
        graphics.setFill(Color.web("#e7decd"));
        graphics.fillRect(0, 0, horizontalRuler.getWidth(), horizontalRuler.getHeight());
        graphics.setStroke(Color.web("#7d7365"));
        graphics.setFill(Color.web("#4a433b"));
        graphics.setFont(Font.font("Menlo", 10));
        if (!ViewProjectionService.isPlanView(activeView.get())) {
            graphics.fillText("Achse", 6, 12);
            return;
        }

        double stepMillimeters = self().chooseRulerStep();
        double stepPixels = stepMillimeters * self().scale();
        double start = offsetX % stepPixels;
        double worldStart = -offsetX / self().scale();
        for (double x = start; x <= horizontalRuler.getWidth(); x += stepPixels) {
            graphics.strokeLine(x, horizontalRuler.getHeight(), x, horizontalRuler.getHeight() - 10);
            double worldX = worldStart + (x / self().scale());
            graphics.fillText(self().formatRuler(worldX), x + 3, 12);
        }
    }

    void drawVerticalRuler(GraphicsContext graphics) {
        graphics.setFill(Color.web("#e7decd"));
        graphics.fillRect(0, 0, verticalRuler.getWidth(), verticalRuler.getHeight());
        graphics.setStroke(Color.web("#7d7365"));
        graphics.setFill(Color.web("#4a433b"));
        graphics.setFont(Font.font("Menlo", 10));
        if (!ViewProjectionService.isPlanView(activeView.get())) {
            graphics.fillText("H", 6, 12);
            return;
        }

        double stepMillimeters = self().chooseRulerStep();
        double stepPixels = stepMillimeters * self().scale();
        double start = offsetY % stepPixels;
        double worldStart = -offsetY / self().scale();
        for (double y = start; y <= verticalRuler.getHeight(); y += stepPixels) {
            graphics.strokeLine(verticalRuler.getWidth(), y, verticalRuler.getWidth() - 10, y);
            double worldY = worldStart + (y / self().scale());
            graphics.fillText(self().formatRuler(worldY), 2, y - 3);
        }
    }
}
