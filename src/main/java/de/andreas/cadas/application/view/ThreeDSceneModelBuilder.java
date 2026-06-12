package de.andreas.cadas.application.view;

import de.andreas.cadas.application.layers.SurfaceLayerEffectService;
import de.andreas.cadas.application.layers.TileLayoutRequest;
import de.andreas.cadas.application.layers.TileLayoutService;
import de.andreas.cadas.application.layers.TilePlacement;
import de.andreas.cadas.application.room.OrthogonalPolygonDecompositionService;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Roof;
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ThreeDSceneModelBuilder {

    private static final double ROOM_VOLUME_OPACITY = 0.16;
    private static final double SURFACE_LAYER_OPACITY = 0.65;
    private static final double ROOF_THICKNESS = 80.0;
    private static final double DOOR_LEAF_DEPTH = 45.0;
    private static final double WINDOW_GLASS_DEPTH = 35.0;
    private static final double LEVEL_GAP = 0.0;
    private static final int MIN_SLOPE_SEGMENTS = 24;
    private final OrthogonalPolygonDecompositionService decompositionService = new OrthogonalPolygonDecompositionService();
    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();
    private final TileLayoutService tileLayoutService = new TileLayoutService();

    public ThreeDSceneModel build(ProjectModel project, Set<String> visibleLevelNames, boolean renderSurfaceLayers) {
        return build(project, visibleLevelNames, renderSurfaceLayers, false);
    }

    public ThreeDSceneModel build(ProjectModel project, Set<String> visibleLevelNames, boolean renderSurfaceLayers, boolean surfaceRenderingMode) {
        List<RenderableBox> boxes = new ArrayList<>();
        Map<String, Double> levelBaseHeights = computeLevelBaseHeights(project.levels());

        for (Level level : project.levels()) {
            if (!visibleLevelNames.contains(level.name())) {
                continue;
            }
            double baseHeight = levelBaseHeights.getOrDefault(level.name(), 0.0);
            double floorOffset = level.rooms().stream()
                    .mapToDouble(room -> room.floorThickness().toMillimeters())
                    .max()
                    .orElse(0.0);
            double wallBaseHeight = baseHeight + floorOffset;
            boxes.addAll(buildRoomFloor(level, baseHeight, surfaceRenderingMode));
            boxes.addAll(buildWallFoundations(level, baseHeight));
            boxes.addAll(buildWallCeilings(level, baseHeight));
            boxes.addAll(buildRoomInteriors(level, baseHeight, renderSurfaceLayers, surfaceRenderingMode));
            boxes.addAll(buildWalls(level, wallBaseHeight, surfaceRenderingMode));
            if (renderSurfaceLayers) {
                boxes.addAll(buildWallSurfaceLayers(level, wallBaseHeight));
            }
            boxes.addAll(buildDoors(level, wallBaseHeight));
            boxes.addAll(buildWindows(level, wallBaseHeight));
            boxes.addAll(buildStairs(level, wallBaseHeight));
        }

        project.roof().ifPresent(roof -> boxes.addAll(buildRoof(project, roof, levelBaseHeights, visibleLevelNames)));
        return new ThreeDSceneModel(List.copyOf(boxes));
    }

    private Map<String, Double> computeLevelBaseHeights(List<Level> levels) {
        Map<String, Double> baseHeights = new LinkedHashMap<>();
        double currentHeight = 0.0;
        for (Level level : levels) {
            baseHeights.put(level.name(), currentHeight);
            currentHeight += estimateLevelHeight(level) + LEVEL_GAP;
        }
        return baseHeights;
    }

    private double estimateLevelHeight(Level level) {
        double wallHeight = level.walls().stream().mapToDouble(Wall::maximumHeightMillimeters).max().orElse(2750.0);
        double roomHeight = level.rooms().stream()
                .mapToDouble(room -> surfaceLayerEffectService.effectiveMaximumCeilingHeightMillimeters(level, room)
                        + room.floorThickness().toMillimeters()
                        + surfaceLayerEffectService.floorLayerThicknessMillimeters(level, room)
                        + room.ceilingThickness().toMillimeters()
                        + surfaceLayerEffectService.ceilingLayerThicknessMillimeters(level, room))
                .max()
                .orElse(0.0);
        double stairHeight = level.staircases().stream().mapToDouble(staircase -> staircase.totalHeight().toMillimeters()).max().orElse(0.0);
        return Math.max(wallHeight, Math.max(roomHeight, stairHeight));
    }

    private List<RenderableBox> buildRoomFloor(Level level, double baseHeight, boolean surfaceRenderingMode) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (Room room : level.rooms()) {
            double floorThickness = room.floorThickness().toMillimeters();
            if (floorThickness <= 0.0) continue;
            for (OrthogonalPolygonDecompositionService.CellRectangle rectangle : roomRectangles(room)) {
                boxes.add(new RenderableBox(
                        new SelectionKey(RenderableKind.ROOM_FLOOR, level.name(), room.id().toString()),
                        level.name(),
                        RenderableKind.ROOM_FLOOR,
                        rectangle.centerX(),
                        baseHeight + floorThickness / 2.0,
                        rectangle.centerY(),
                        rectangle.width(),
                        floorThickness,
                        rectangle.height(),
                        RotationAxis.Y,
                        0.0,
                        "room-floor",
                        1.0
                ));
            }
        }
        return boxes;
    }

    private List<RenderableBox> buildWallFoundations(Level level, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        double floorThickness = level.rooms().stream()
                .mapToDouble(room -> room.floorThickness().toMillimeters())
                .max()
                .orElse(0.0);
        if (floorThickness <= 0.0) return boxes;
        double centerY = baseHeight + floorThickness / 2.0;
        for (Wall wall : level.walls()) {
            double wallLength = wall.axis().length().toMillimeters();
            double halfThickness = wall.thickness().toMillimeters() / 2.0;
            double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
            double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
            double ux = wallLength > 0.0 ? dx / wallLength : 0.0;
            double uy = wallLength > 0.0 ? dy / wallLength : 0.0;
            double midX = (wall.axis().start().xMillimeters() + wall.axis().end().xMillimeters()) / 2.0;
            double midZ = (wall.axis().start().yMillimeters() + wall.axis().end().yMillimeters()) / 2.0;
            boxes.add(new RenderableBox(
                    new SelectionKey(RenderableKind.ROOM_FLOOR, level.name(), "foundation-" + wall.id().toString()),
                    level.name(),
                    RenderableKind.ROOM_FLOOR,
                    midX + ux * halfThickness / 2.0,
                    centerY,
                    midZ + uy * halfThickness / 2.0,
                    wallLength + halfThickness,
                    floorThickness,
                    wall.thickness().toMillimeters(),
                    RotationAxis.Y,
                    wall.axis().angle().degrees(),
                    "room-floor",
                    1.0
            ));
        }
        return boxes;
    }

    private List<RenderableBox> buildWallCeilings(Level level, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        double ceilingBase = level.rooms().stream()
                .mapToDouble(room -> room.floorThickness().toMillimeters() + room.maximumCeilingHeightMillimeters())
                .max()
                .orElse(0.0);
        double ceilingHeight = level.rooms().stream()
                .mapToDouble(room -> room.ceilingThickness().toMillimeters())
                .max()
                .orElse(0.0);
        if (ceilingHeight <= 0.0) return boxes;
        double centerY = baseHeight + ceilingBase + ceilingHeight / 2.0;
        for (Wall wall : level.walls()) {
            double wallLength = wall.axis().length().toMillimeters();
            double halfThickness = wall.thickness().toMillimeters() / 2.0;
            double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
            double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
            double ux = wallLength > 0.0 ? dx / wallLength : 0.0;
            double uy = wallLength > 0.0 ? dy / wallLength : 0.0;
            double midX = (wall.axis().start().xMillimeters() + wall.axis().end().xMillimeters()) / 2.0;
            double midZ = (wall.axis().start().yMillimeters() + wall.axis().end().yMillimeters()) / 2.0;
            boxes.add(new RenderableBox(
                    new SelectionKey(RenderableKind.ROOM_CEILING, level.name(), "ceiling-" + wall.id().toString()),
                    level.name(),
                    RenderableKind.ROOM_CEILING,
                    midX + ux * halfThickness / 2.0,
                    centerY,
                    midZ + uy * halfThickness / 2.0,
                    wallLength + halfThickness,
                    ceilingHeight,
                    wall.thickness().toMillimeters(),
                    RotationAxis.Y,
                    wall.axis().angle().degrees(),
                    "room-ceiling",
                    1.0
            ));
        }
        return boxes;
    }

    private List<RenderableBox> buildRoomInteriors(Level level, double baseHeight, boolean renderSurfaceLayers, boolean surfaceRenderingMode) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (Room room : level.rooms()) {
            List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles = roomRectangles(room);
            if (room.hasVariableCeilingHeights()) {
                if (!surfaceRenderingMode) {
                    boxes.addAll(buildSlopedRoomVolume(level, room, baseHeight, rectangles));
                }
                boxes.addAll(buildSlopedCeiling(level, room, baseHeight, rectangles, surfaceRenderingMode));
            } else {
                boxes.addAll(buildFlatRoomCeiling(level, room, baseHeight, rectangles, surfaceRenderingMode));
                if (!surfaceRenderingMode) {
                    boxes.addAll(buildFlatRoomVolume(level, room, baseHeight, rectangles));
                }
            }

            if (renderSurfaceLayers) {
                boxes.addAll(buildSurfaceLayers(level, room, rectangles, baseHeight));
            }
        }
        return boxes;
    }

    private List<RenderableBox> buildFlatRoomCeiling(Level level, Room room, double baseHeight, List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles, boolean surfaceRenderingMode) {
        List<RenderableBox> boxes = new ArrayList<>();
        double ceilingThickness = Math.max(0.1, room.ceilingThickness().toMillimeters());
        for (OrthogonalPolygonDecompositionService.CellRectangle rectangle : rectangles) {
            boxes.add(new RenderableBox(
                    new SelectionKey(RenderableKind.ROOM_CEILING, level.name(), room.id().toString()),
                    level.name(),
                    RenderableKind.ROOM_CEILING,
                    rectangle.centerX(),
                    baseHeight + room.floorThickness().toMillimeters() + room.maximumCeilingHeightMillimeters() + ceilingThickness / 2.0,
                    rectangle.centerY(),
                    rectangle.width(),
                    ceilingThickness,
                    rectangle.height(),
                    RotationAxis.Y,
                    0.0,
                    "room-ceiling",
                    surfaceRenderingMode ? 1.0 : 1.0
            ));
        }
        return boxes;
    }

    private List<RenderableBox> buildFlatRoomVolume(Level level, Room room, double baseHeight, List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles) {
        List<RenderableBox> boxes = new ArrayList<>();
        double floorTop = baseHeight + room.floorThickness().toMillimeters();
        double ceilingBottom = floorTop + room.maximumCeilingHeightMillimeters() - room.ceilingThickness().toMillimeters();
        double volumeHeight = Math.max(1.0, ceilingBottom - floorTop);
        for (OrthogonalPolygonDecompositionService.CellRectangle rectangle : rectangles) {
            boxes.add(new RenderableBox(
                    new SelectionKey(RenderableKind.ROOM_VOLUME, level.name(), room.id().toString()),
                    level.name(),
                    RenderableKind.ROOM_VOLUME,
                    rectangle.centerX(),
                    floorTop + volumeHeight / 2.0,
                    rectangle.centerY(),
                    Math.max(40.0, rectangle.width() - 20.0),
                    volumeHeight,
                    Math.max(40.0, rectangle.height() - 20.0),
                    RotationAxis.Y,
                    0.0,
                    "room-volume",
                    ROOM_VOLUME_OPACITY
            ));
        }
        return boxes;
    }

    private List<RenderableBox> buildSurfaceLayers(Level level, Room room, List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (SurfaceLayerStack stack : level.surfaceLayerStacks()) {
            if (!matchesRoom(stack, room)) {
                continue;
            }
            if (stack.surfaceType() == SurfaceType.FLOOR) {
                boxes.addAll(buildFloorSurfaceLayers(level, room, rectangles, baseHeight, stack));
            } else if (stack.surfaceType() == SurfaceType.CEILING) {
                boxes.addAll(buildCeilingSurfaceLayers(level, room, rectangles, baseHeight, stack));
            }
        }
        return boxes;
    }

    private List<RenderableBox> buildFloorSurfaceLayers(Level level, Room room, List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles, double baseHeight, SurfaceLayerStack stack) {
        List<RenderableBox> boxes = new ArrayList<>();
        double currentHeight = baseHeight + room.floorThickness().toMillimeters();
        for (SurfaceLayer layer : stack.layers()) {
            if (!layer.visible()) {
                currentHeight += layer.thickness().toMillimeters();
                continue;
            }
            double layerTopHeight = currentHeight + layer.thickness().toMillimeters();
            for (OrthogonalPolygonDecompositionService.CellRectangle rectangle : rectangles) {
                boxes.add(new RenderableBox(
                        new SelectionKey(RenderableKind.SURFACE_LAYER, level.name(), layer.id().toString()),
                        level.name(),
                        RenderableKind.SURFACE_LAYER,
                        rectangle.centerX(),
                        currentHeight + layer.thickness().toMillimeters() / 2.0,
                        rectangle.centerY(),
                        rectangle.width(),
                        layer.thickness().toMillimeters(),
                        rectangle.height(),
                        RotationAxis.Y,
                        0.0,
                        "surface-layer",
                        SURFACE_LAYER_OPACITY
                ));
            }
            boxes.addAll(buildJoints(room, layer, rectangles, layerTopHeight));
            currentHeight = layerTopHeight;
        }
        return boxes;
    }

    private List<RenderableBox> buildJoints(Room room, SurfaceLayer layer, List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles, double topHeight) {
        List<RenderableBox> joints = new ArrayList<>();
        double jointWidthMm = layer.jointWidth().toMillimeters();
        if (jointWidthMm < 0.001) return joints;
        double jointHeight = 0.3;
        double jointCenterY = topHeight + jointHeight / 2.0;
        TileLayoutRequest request = new TileLayoutRequest(
                Length.ofMillimeters(room.widthMillimeters()),
                Length.ofMillimeters(room.depthMillimeters()),
                layer.tileWidth(),
                layer.tileHeight(),
                layer.layoutMode(),
                layer.layoutOffset(),
                layer.minimumOffset(),
                layer.minimumEdgeWidth()
        );
        List<TilePlacement> tiles = tileLayoutService.fillSurface(request);
        if (tiles.isEmpty()) return joints;
        double roomMinX = room.minXMillimeters();
        double roomMinY = room.minYMillimeters();
        var horizontalKeys = new java.util.HashSet<String>();
        var verticalKeys = new java.util.HashSet<String>();
        for (TilePlacement tile : tiles) {
            double tx = roomMinX + tile.xOffset().toMillimeters();
            double ty = roomMinY + tile.yOffset().toMillimeters();
            double tw = tile.width().toMillimeters();
            double th = tile.height().toMillimeters();
            String hKey = String.format(java.util.Locale.US, "h:%.3f", ty + th);
            if (horizontalKeys.add(hKey)) {
                for (OrthogonalPolygonDecompositionService.CellRectangle rect : rectangles) {
                    double rx = rect.centerX() - rect.width() / 2.0;
                    double rz = rect.centerY() - rect.height() / 2.0;
                    double rxe = rx + rect.width();
                    double rze = rz + rect.height();
                    double jointX = Math.max(rx, tx);
                    double jointXe = Math.min(rxe, tx + tw);
                    double jointZ = ty + th - jointWidthMm / 2.0;
                    double jointZe = jointZ + jointWidthMm;
                    if (jointX < jointXe && jointZ < jointZe && jointZ >= rz && jointZe <= rze) {
                        joints.add(new RenderableBox(
                                new SelectionKey(RenderableKind.SURFACE_LAYER, "", layer.id().toString()),
                                "",
                                RenderableKind.SURFACE_LAYER,
                                (jointX + jointXe) / 2.0,
                                jointCenterY,
                                (jointZ + jointZe) / 2.0,
                                jointXe - jointX,
                                jointHeight,
                                jointZe - jointZ,
                                RotationAxis.Y,
                                0.0,
                                "joint",
                                1.0
                        ));
                    }
                }
            }
            String vKey = String.format(java.util.Locale.US, "v:%.3f:%.3f", tx + tw, ty);
            if (verticalKeys.add(vKey)) {
                for (OrthogonalPolygonDecompositionService.CellRectangle rect : rectangles) {
                    double rx = rect.centerX() - rect.width() / 2.0;
                    double rz = rect.centerY() - rect.height() / 2.0;
                    double rxe = rx + rect.width();
                    double rze = rz + rect.height();
                    double jointX = tx + tw - jointWidthMm / 2.0;
                    double jointXe = jointX + jointWidthMm;
                    double jointZ = Math.max(rz, ty);
                    double jointZe = Math.min(rze, ty + th);
                    if (jointX >= rx && jointXe <= rxe && jointZ < jointZe) {
                        joints.add(new RenderableBox(
                                new SelectionKey(RenderableKind.SURFACE_LAYER, "", layer.id().toString()),
                                "",
                                RenderableKind.SURFACE_LAYER,
                                (jointX + jointXe) / 2.0,
                                jointCenterY,
                                (jointZ + jointZe) / 2.0,
                                jointXe - jointX,
                                jointHeight,
                                jointZe - jointZ,
                                RotationAxis.Y,
                                0.0,
                                "joint",
                                1.0
                        ));
                    }
                }
            }
        }
        return joints;
    }

    private List<RenderableBox> buildCeilingSurfaceLayers(Level level, Room room, List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles, double baseHeight, SurfaceLayerStack stack) {
        List<RenderableBox> boxes = new ArrayList<>();
        double offsetFromCeiling = 0.0;
        for (SurfaceLayer layer : stack.layers()) {
            if (!layer.visible()) {
                offsetFromCeiling += layer.thickness().toMillimeters();
                continue;
            }
            if (room.hasVariableCeilingHeights()) {
                boxes.addAll(buildSlopedRoomSlices(
                        level,
                        level.name(),
                        room,
                        rectangles,
                        baseHeight + room.floorThickness().toMillimeters() - offsetFromCeiling - layer.thickness().toMillimeters() / 2.0,
                        layer.thickness().toMillimeters(),
                        "surface-layer",
                        SURFACE_LAYER_OPACITY,
                        new SelectionKey(RenderableKind.SURFACE_LAYER, level.name(), layer.id().toString())
                ));
            } else {
                double layerBottomHeight = baseHeight + room.floorThickness().toMillimeters() + room.roomHeight().toMillimeters() - offsetFromCeiling;
                for (OrthogonalPolygonDecompositionService.CellRectangle rectangle : rectangles) {
                    boxes.add(new RenderableBox(
                            new SelectionKey(RenderableKind.SURFACE_LAYER, level.name(), layer.id().toString()),
                            level.name(),
                            RenderableKind.SURFACE_LAYER,
                            rectangle.centerX(),
                            layerBottomHeight - layer.thickness().toMillimeters() / 2.0,
                            rectangle.centerY(),
                            rectangle.width(),
                            layer.thickness().toMillimeters(),
                            rectangle.height(),
                            RotationAxis.Y,
                            0.0,
                            "surface-layer",
                            SURFACE_LAYER_OPACITY
                    ));
                }
                boxes.addAll(buildJoints(room, layer, rectangles, layerBottomHeight));
            }
            offsetFromCeiling += layer.thickness().toMillimeters();
        }
        return boxes;
    }

    private boolean matchesRoom(SurfaceLayerStack stack, Room room) {
        return stack.targetKey().equals(room.id().toString())
                || stack.targetKey().equalsIgnoreCase(room.name())
                || stack.targetKey().contains(room.id().toString())
                || stack.targetKey().contains(room.name());
    }

    private List<OrthogonalPolygonDecompositionService.CellRectangle> roomRectangles(Room room) {
        List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles = decompositionService.decompose(room.outline());
        if (!rectangles.isEmpty()) {
            return rectangles;
        }
        return List.of(new OrthogonalPolygonDecompositionService.CellRectangle(
                room.minXMillimeters(),
                room.maxXMillimeters(),
                room.minYMillimeters(),
                room.maxYMillimeters()
        ));
    }

    private List<RenderableBox> buildSlopedRoomVolume(Level level, Room room, double baseHeight, List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles) {
        return buildSlopedRoomSlices(
                level,
                level.name(),
                room,
                rectangles,
                baseHeight + room.floorThickness().toMillimeters(),
                0.0,
                "room-volume",
                ROOM_VOLUME_OPACITY,
                new SelectionKey(RenderableKind.ROOM_VOLUME, level.name(), room.id().toString())
        );
    }

    private List<RenderableBox> buildSlopedCeiling(Level level, Room room, double baseHeight, List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles, boolean surfaceRenderingMode) {
        return buildSlopedRoomSlices(
                level,
                level.name(),
                room,
                rectangles,
                baseHeight + room.floorThickness().toMillimeters(),
                Math.max(0.1, room.ceilingThickness().toMillimeters()),
                "room-ceiling",
                surfaceRenderingMode ? 1.0 : 1.0,
                new SelectionKey(RenderableKind.ROOM_CEILING, level.name(), room.id().toString())
        );
    }

    private List<RenderableBox> buildSlopedRoomSlices(
            Level level,
            String levelName,
            Room room,
            List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles,
            double baseHeight,
            double sliceThickness,
            String materialKey,
            double opacity,
            SelectionKey selectionKey
        ) {
        List<RenderableBox> boxes = new ArrayList<>();
        boolean variesAlongDepth = room.slopedCeilingProfile()
                .map(profile -> profile.lowSide() == de.andreas.cadas.domain.model.SlopedCeilingSide.NORTH
                        || profile.lowSide() == de.andreas.cadas.domain.model.SlopedCeilingSide.SOUTH)
                .orElse(room.depthMillimeters() >= room.widthMillimeters());
        for (OrthogonalPolygonDecompositionService.CellRectangle rectangle : rectangles) {
            int slopeSegments = Math.max(MIN_SLOPE_SEGMENTS, (int) Math.ceil((variesAlongDepth ? rectangle.height() : rectangle.width()) / 120.0));
            for (int index = 0; index < slopeSegments; index++) {
                double startRatio = (double) index / slopeSegments;
                double endRatio = (double) (index + 1) / slopeSegments;
                double centerX = rectangle.centerX();
                double centerZ = rectangle.centerY();
                double width = rectangle.width();
                double depth = rectangle.height();
                if (variesAlongDepth) {
                    double startY = rectangle.minY() + rectangle.height() * startRatio;
                    double endY = rectangle.minY() + rectangle.height() * endRatio;
                    centerZ = (startY + endY) / 2.0;
                    depth = Math.max(24.0, endY - startY);
                } else {
                    double startX = rectangle.minX() + rectangle.width() * startRatio;
                    double endX = rectangle.minX() + rectangle.width() * endRatio;
                    centerX = (startX + endX) / 2.0;
                    width = Math.max(24.0, endX - startX);
                }
                double ceilingHeightAtPoint = room.ceilingHeightAt(new PlanPoint(centerX, centerZ));
                double visibleHeight = "room-volume".equals(materialKey)
                        ? ceilingHeightAtPoint - room.ceilingThickness().toMillimeters()
                        : ceilingHeightAtPoint;
                double height = sliceThickness > 0.0 ? sliceThickness : Math.max(40.0, visibleHeight);
                double centerY = sliceThickness > 0.0
                        ? baseHeight + visibleHeight + sliceThickness / 2.0
                        : baseHeight + visibleHeight / 2.0;
                boxes.add(new RenderableBox(
                        selectionKey,
                        levelName,
                        materialKey.equals("room-volume") ? RenderableKind.ROOM_VOLUME :
                                materialKey.equals("room-ceiling") ? RenderableKind.ROOM_CEILING : RenderableKind.SURFACE_LAYER,
                        centerX,
                        centerY,
                        centerZ,
                        Math.max(24.0, width - 6.0),
                        height,
                        Math.max(24.0, depth - 6.0),
                        RotationAxis.Y,
                        0.0,
                        materialKey,
                        opacity
                ));
            }
        }
        return boxes;
    }

    private List<RenderableBox> buildWalls(Level level, double baseHeight, boolean surfaceRenderingMode) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (Wall wall : level.walls()) {
            double wallLength = wall.axis().length().toMillimeters();
            double halfThickness = wall.thickness().toMillimeters() / 2.0;
            List<OpeningRange> openings = openingsForWall(level, wall);
            double cursor = 0.0;
            for (OpeningRange opening : openings) {
                if (opening.startMillimeters() > cursor) {
                    double segStart = cursor == 0.0
                            ? Math.max(0.0, cursor - halfThickness)
                            : cursor;
                    boxes.addAll(segmentToBoxes(level.name(), wall, segStart, opening.startMillimeters(), baseHeight, 0.0, "wall", surfaceRenderingMode));
                }
                if (opening.kind() == RenderableKind.DOOR) {
                    double upperHeight = Math.max(0.0, wall.minimumHeightMillimeters() - opening.upperHeightMillimeters());
                    if (upperHeight > 0.0 || wall.hasVariableTopHeight()) {
                        boxes.addAll(segmentToBoxes(level.name(), wall, opening.startMillimeters(), opening.endMillimeters(), baseHeight + opening.upperHeightMillimeters(), opening.upperHeightMillimeters(), "wall", surfaceRenderingMode));
                    }
                } else if (opening.kind() == RenderableKind.WINDOW) {
                    if (opening.lowerHeightMillimeters() > 0.0) {
                        boxes.add(segmentToUniformBox(level.name(), wall, opening.startMillimeters(), opening.endMillimeters(), baseHeight, opening.lowerHeightMillimeters(), "wall", surfaceRenderingMode));
                    }
                    double upperBase = baseHeight + opening.upperHeightMillimeters();
                    double upperHeight = Math.max(0.0, wall.minimumHeightMillimeters() - opening.upperHeightMillimeters());
                    if (upperHeight > 0.0 || wall.hasVariableTopHeight()) {
                        boxes.addAll(segmentToBoxes(level.name(), wall, opening.startMillimeters(), opening.endMillimeters(), upperBase, opening.upperHeightMillimeters(), "wall", surfaceRenderingMode));
                    }
                }
                cursor = Math.max(cursor, opening.endMillimeters());
            }
            if (cursor < wallLength) {
                double segStart = cursor == 0.0
                        ? Math.max(0.0, cursor - halfThickness)
                        : cursor;
                double segEnd = wallLength + halfThickness;
                boxes.addAll(segmentToBoxes(level.name(), wall, segStart, segEnd, baseHeight, 0.0, "wall", surfaceRenderingMode));
            }
        }
        return boxes;
    }

    private List<RenderableBox> buildWallSurfaceLayers(Level level, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (Wall wall : level.walls()) {
            SurfaceLayerStack interior = level.findSurfaceLayerStack(SurfaceType.WALL_INTERIOR, wall.id().toString());
            if (interior != null) {
                boxes.addAll(buildWallSurfaceLayerBoxes(level.name(), wall, interior, baseHeight));
            }
            SurfaceLayerStack exterior = level.findSurfaceLayerStack(SurfaceType.WALL_EXTERIOR, wall.id().toString());
            if (exterior != null) {
                boxes.addAll(buildWallSurfaceLayerBoxes(level.name(), wall, exterior, baseHeight));
            }
        }
        return boxes;
    }

    private List<RenderableBox> buildWallSurfaceLayerBoxes(String levelName, Wall wall, SurfaceLayerStack stack, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        double cumulativeThickness = wall.thickness().toMillimeters() / 2.0;
        for (SurfaceLayer layer : stack.layers()) {
            if (!layer.visible()) {
                cumulativeThickness += layer.thickness().toMillimeters();
                continue;
            }
            double centerOffset = cumulativeThickness + layer.thickness().toMillimeters() / 2.0;
            boxes.add(wallSurfaceLayerBox(levelName, wall, layer, baseHeight, centerOffset));
            boxes.add(wallSurfaceLayerBox(levelName, wall, layer, baseHeight, -centerOffset));
            cumulativeThickness += layer.thickness().toMillimeters();
        }
        return boxes;
    }

    private RenderableBox wallSurfaceLayerBox(String levelName, Wall wall, SurfaceLayer layer, double baseHeight, double perpendicularOffset) {
        double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double length = Math.max(1.0, Math.hypot(dx, dy));
        double normalX = -dy / length;
        double normalY = dx / length;
        double centerX = (wall.axis().start().xMillimeters() + wall.axis().end().xMillimeters()) / 2.0 + normalX * perpendicularOffset;
        double centerZ = (wall.axis().start().yMillimeters() + wall.axis().end().yMillimeters()) / 2.0 + normalY * perpendicularOffset;
        return new RenderableBox(
                new SelectionKey(RenderableKind.SURFACE_LAYER, levelName, layer.id().toString()),
                levelName,
                RenderableKind.SURFACE_LAYER,
                centerX,
                baseHeight + wall.maximumHeightMillimeters() / 2.0,
                centerZ,
                wall.axis().length().toMillimeters(),
                wall.maximumHeightMillimeters(),
                layer.thickness().toMillimeters(),
                RotationAxis.Y,
                wall.axis().angle().degrees(),
                "surface-layer",
                SURFACE_LAYER_OPACITY
        );
    }

    private List<OpeningRange> openingsForWall(Level level, Wall wall) {
        List<OpeningRange> openings = new ArrayList<>();
        for (Door door : level.doors()) {
            if (!door.wallId().equals(wall.id())) {
                continue;
            }
            openings.add(new OpeningRange(
                    RenderableKind.DOOR,
                    door.offsetFromStart().toMillimeters(),
                    door.offsetFromStart().add(door.width()).toMillimeters(),
                    0.0,
                    door.height().toMillimeters()
            ));
        }
        for (WindowElement window : level.windows()) {
            if (!window.wallId().equals(wall.id())) {
                continue;
            }
            openings.add(new OpeningRange(
                    RenderableKind.WINDOW,
                    window.offsetFromStart().toMillimeters(),
                    window.offsetFromStart().add(window.width()).toMillimeters(),
                    window.sillHeight().toMillimeters(),
                    window.sillHeight().toMillimeters() + window.windowHeight().toMillimeters()
            ));
        }
        openings.sort(Comparator.comparingDouble(OpeningRange::startMillimeters));
        return openings;
    }

    private List<RenderableBox> segmentToBoxes(
            String levelName,
            Wall wall,
            double startMillimeters,
            double endMillimeters,
            double baseHeight,
            double subtractBottomMillimeters,
            String materialKey,
            boolean surfaceRenderingMode
    ) {
        if (!wall.hasVariableTopHeight()) {
            double heightMillimeters = Math.max(0.0, wall.heightAt((startMillimeters + endMillimeters) / 2.0) - subtractBottomMillimeters);
            if (heightMillimeters <= 0.0) {
                return List.of();
            }
            return List.of(segmentToUniformBox(levelName, wall, startMillimeters, endMillimeters, baseHeight, heightMillimeters, materialKey, surfaceRenderingMode));
        }
        List<RenderableBox> boxes = new ArrayList<>();
        double segmentLength = Math.max(1.0, endMillimeters - startMillimeters);
        int slices = Math.max(2, (int) Math.ceil(segmentLength / 400.0));
        for (int index = 0; index < slices; index++) {
            double sliceStart = startMillimeters + segmentLength * index / slices;
            double sliceEnd = startMillimeters + segmentLength * (index + 1) / slices;
            double heightMillimeters = Math.max(0.0, wall.heightAt((sliceStart + sliceEnd) / 2.0) - subtractBottomMillimeters);
            if (heightMillimeters <= 0.0) {
                continue;
            }
            boxes.add(segmentToUniformBox(levelName, wall, sliceStart, sliceEnd, baseHeight, heightMillimeters, materialKey, surfaceRenderingMode));
        }
        return boxes;
    }

    private RenderableBox segmentToUniformBox(
            String levelName,
            Wall wall,
            double startMillimeters,
            double endMillimeters,
            double baseHeight,
            double heightMillimeters,
            String materialKey,
            boolean surfaceRenderingMode
    ) {
        Length startLength = Length.ofMillimeters(startMillimeters);
        Length endLength = Length.ofMillimeters(endMillimeters);
        PlanPoint start = wall.axis().pointAt(startLength);
        PlanPoint end = wall.axis().pointAt(endLength);
        double segmentLength = Math.max(1.0, endMillimeters - startMillimeters);
        return new RenderableBox(
                new SelectionKey(RenderableKind.WALL, levelName, wall.id().toString()),
                levelName,
                RenderableKind.WALL,
                (start.xMillimeters() + end.xMillimeters()) / 2.0,
                baseHeight + heightMillimeters / 2.0,
                (start.yMillimeters() + end.yMillimeters()) / 2.0,
                segmentLength,
                heightMillimeters,
                wall.thickness().toMillimeters(),
                RotationAxis.Y,
                wall.axis().angle().degrees(),
                materialKey,
                surfaceRenderingMode ? 1.0 : 1.0
        );
    }

    private List<RenderableBox> buildDoors(Level level, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (Door door : level.doors()) {
            Wall wall = level.findWall(door.wallId());
            boxes.add(openingBox(level.name(), RenderableKind.DOOR, door.id().toString(), wall, door.offsetFromStart(), door.width(), baseHeight + door.height().toMillimeters() / 2.0, door.height().toMillimeters(), Math.min(DOOR_LEAF_DEPTH, wall.thickness().toMillimeters() * 0.6), "door", 0.95));
        }
        return boxes;
    }

    private List<RenderableBox> buildWindows(Level level, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (WindowElement window : level.windows()) {
            Wall wall = level.findWall(window.wallId());
            boxes.add(openingBox(level.name(), RenderableKind.WINDOW, window.id().toString(), wall, window.offsetFromStart(), window.width(), baseHeight + window.sillHeight().toMillimeters() + window.windowHeight().toMillimeters() / 2.0, window.windowHeight().toMillimeters(), Math.min(WINDOW_GLASS_DEPTH, wall.thickness().toMillimeters() * 0.45), "window", 0.7));
        }
        return boxes;
    }

    private RenderableBox openingBox(
            String levelName,
            RenderableKind kind,
            String elementId,
            Wall wall,
            Length offset,
            Length width,
            double centerY,
            double height,
            double depth,
            String materialKey,
            double opacity
    ) {
        PlanPoint start = wall.axis().pointAt(offset);
        PlanPoint end = wall.axis().pointAt(offset.add(width));
        return new RenderableBox(
                new SelectionKey(kind, levelName, elementId),
                levelName,
                kind,
                (start.xMillimeters() + end.xMillimeters()) / 2.0,
                centerY,
                (start.yMillimeters() + end.yMillimeters()) / 2.0,
                width.toMillimeters(),
                height,
                depth,
                RotationAxis.Y,
                wall.axis().angle().degrees(),
                materialKey,
                opacity
        );
    }

    private List<RenderableBox> buildStairs(Level level, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (Staircase staircase : level.staircases()) {
            switch (staircase.stairType()) {
                case STRAIGHT -> boxes.addAll(buildStraightStair(level.name(), staircase, baseHeight));
                case HALF_TURN -> boxes.addAll(buildHalfTurnStair(level.name(), staircase, baseHeight));
                case SWITCHBACK -> boxes.addAll(buildSwitchbackStair(level.name(), staircase, baseHeight));
                case SPIRAL -> boxes.addAll(buildSpiralStair(level.name(), staircase, baseHeight));
            }
        }
        return boxes;
    }

    private List<RenderableBox> buildStraightStair(String levelName, Staircase staircase, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        double stepHeight = staircase.totalHeight().toMillimeters() / staircase.stepCount();
        double stepDepth = staircase.heightMillimeters() / staircase.stepCount();
        for (int step = 0; step < staircase.stepCount(); step++) {
            boxes.add(stairBox(
                    levelName,
                    staircase,
                    baseHeight + stepHeight * (step + 0.5),
                    staircase.widthMillimeters() / 2.0,
                    stepDepth * step + stepDepth / 2.0,
                    staircase.widthMillimeters(),
                    stepDepth,
                    0.0
            ));
        }
        return boxes;
    }

    private List<RenderableBox> buildHalfTurnStair(String levelName, Staircase staircase, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        int firstFlightSteps = staircase.stepCount() / 2;
        int secondFlightSteps = staircase.stepCount() - firstFlightSteps;
        double stepHeight = staircase.totalHeight().toMillimeters() / staircase.stepCount();
        double totalWidth = staircase.widthMillimeters();
        double totalHeight = staircase.heightMillimeters();
        double landingDepth = totalHeight * 0.22;
        double flightDepth = (totalHeight - landingDepth) / 2.0;
        double flightWidth = totalWidth * 0.48;
        double firstStepDepth = flightDepth / Math.max(1, firstFlightSteps);
        for (int step = 0; step < firstFlightSteps; step++) {
            boxes.add(stairBox(levelName, staircase, baseHeight + stepHeight * (step + 0.5), flightWidth / 2.0, firstStepDepth * step + firstStepDepth / 2.0, flightWidth, firstStepDepth, 0.0));
        }
        boxes.add(stairBox(
                levelName,
                staircase,
                baseHeight + stepHeight * firstFlightSteps,
                totalWidth / 2.0,
                flightDepth + landingDepth / 2.0,
                totalWidth,
                landingDepth,
                0.0
        ));
        double secondStepDepth = flightDepth / Math.max(1, secondFlightSteps);
        for (int step = 0; step < secondFlightSteps; step++) {
            boxes.add(stairBox(
                    levelName,
                    staircase,
                    baseHeight + stepHeight * (firstFlightSteps + step + 0.5),
                    flightWidth + (totalWidth - flightWidth) / 2.0,
                    flightDepth + landingDepth + secondStepDepth * step + secondStepDepth / 2.0,
                    totalWidth - flightWidth,
                    secondStepDepth,
                    0.0
            ));
        }
        return boxes;
    }

    private List<RenderableBox> buildSwitchbackStair(String levelName, Staircase staircase, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        int firstFlightSteps = staircase.stepCount() / 2;
        int secondFlightSteps = staircase.stepCount() - firstFlightSteps;
        double stepHeight = staircase.totalHeight().toMillimeters() / staircase.stepCount();
        double totalWidth = staircase.widthMillimeters();
        double totalHeight = staircase.heightMillimeters();
        double turnZoneDepth = totalHeight * 0.18;
        double flightDepth = totalHeight - turnZoneDepth;
        double flightWidth = totalWidth / 2.0;
        double firstStepDepth = flightDepth / Math.max(1, firstFlightSteps);
        double secondStepDepth = flightDepth / Math.max(1, secondFlightSteps);
        for (int step = 0; step < firstFlightSteps; step++) {
            boxes.add(stairBox(levelName, staircase, baseHeight + stepHeight * (step + 0.5), flightWidth / 2.0, firstStepDepth * step + firstStepDepth / 2.0, flightWidth, firstStepDepth, 0.0));
        }
        boxes.add(stairBox(
                levelName,
                staircase,
                baseHeight + stepHeight * firstFlightSteps,
                totalWidth / 2.0,
                flightDepth + turnZoneDepth / 2.0,
                totalWidth,
                turnZoneDepth,
                0.0
        ));
        for (int step = 0; step < secondFlightSteps; step++) {
            boxes.add(stairBox(
                    levelName,
                    staircase,
                    baseHeight + stepHeight * (firstFlightSteps + step + 0.5),
                    flightWidth + flightWidth / 2.0,
                    flightDepth - secondStepDepth * step - secondStepDepth / 2.0,
                    flightWidth,
                    secondStepDepth,
                    180.0
            ));
        }
        return boxes;
    }

    private List<RenderableBox> buildSpiralStair(String levelName, Staircase staircase, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        double radius = Math.min(staircase.widthMillimeters(), staircase.heightMillimeters()) / 2.0;
        double stepHeight = staircase.totalHeight().toMillimeters() / staircase.stepCount();
        for (int step = 0; step < staircase.stepCount(); step++) {
            double angleDegrees = (360.0 / staircase.stepCount()) * step;
            double angleRadians = Math.toRadians(angleDegrees);
            boxes.add(stairBox(
                    levelName,
                    staircase,
                    baseHeight + stepHeight * (step + 0.5),
                    staircase.widthMillimeters() / 2.0 + Math.cos(angleRadians) * radius * 0.55,
                    staircase.heightMillimeters() / 2.0 + Math.sin(angleRadians) * radius * 0.55,
                    radius,
                    radius * 0.28,
                    angleDegrees
            ));
        }
        return boxes;
    }

    private RenderableBox stairBox(
            String levelName,
            Staircase staircase,
            double centerY,
            double localCenterX,
            double localCenterZ,
            double localWidth,
            double localDepth,
            double rotationDegrees
    ) {
        var center = staircase.pointAtLocalPosition(localCenterX, localCenterZ);
        return new RenderableBox(
                new SelectionKey(RenderableKind.STAIR, levelName, staircase.id().toString()),
                levelName,
                RenderableKind.STAIR,
                center.xMillimeters(),
                centerY,
                center.yMillimeters(),
                staircase.orientedWidth(localWidth, localDepth),
                staircase.totalHeight().toMillimeters() / staircase.stepCount(),
                staircase.orientedHeight(localWidth, localDepth),
                RotationAxis.Y,
                rotationDegrees + staircase.rotationQuarterTurns() * 90.0,
                "stair",
                1.0
        );
    }

    private List<RenderableBox> buildRoof(ProjectModel project, Roof roof, Map<String, Double> levelBaseHeights, Set<String> visibleLevelNames) {
        if (project.levels().stream().noneMatch(level -> visibleLevelNames.contains(level.name()))) {
            return List.of();
        }
        Footprint footprint = footprint(project.levels());
        if (footprint == null) {
            return List.of();
        }
        double topLevel = project.levels().stream()
                .filter(level -> visibleLevelNames.contains(level.name()))
                .mapToDouble(level -> levelBaseHeights.getOrDefault(level.name(), 0.0) + estimateLevelHeight(level))
                .max()
                .orElse(0.0);
        double overhang = roof.overhang().toMillimeters();
        double width = footprint.maxX - footprint.minX + overhang * 2.0;
        double depth = footprint.maxZ - footprint.minZ + overhang * 2.0;
        boolean ridgeAlongX = width >= depth;
        double span = ridgeAlongX ? depth / 2.0 : width / 2.0;
        double slopeLength = span / Math.cos(Math.toRadians(roof.pitchAngle().degrees()));
        double centerX = (footprint.minX + footprint.maxX) / 2.0;
        double centerZ = (footprint.minZ + footprint.maxZ) / 2.0;
        double rise = Math.tan(Math.toRadians(roof.pitchAngle().degrees())) * span;
        double baseY = topLevel + rise * 0.5 + ROOF_THICKNESS / 2.0;
        List<RenderableBox> boxes = new ArrayList<>();
        if (ridgeAlongX) {
            boxes.add(new RenderableBox(new SelectionKey(RenderableKind.ROOF, "Dach", "roof-left"), "Dach", RenderableKind.ROOF, centerX, baseY, centerZ - span / 2.0, width, ROOF_THICKNESS, slopeLength, RotationAxis.X, roof.pitchAngle().degrees(), "roof", 1.0));
            boxes.add(new RenderableBox(new SelectionKey(RenderableKind.ROOF, "Dach", "roof-right"), "Dach", RenderableKind.ROOF, centerX, baseY, centerZ + span / 2.0, width, ROOF_THICKNESS, slopeLength, RotationAxis.X, -roof.pitchAngle().degrees(), "roof", 1.0));
        } else {
            boxes.add(new RenderableBox(new SelectionKey(RenderableKind.ROOF, "Dach", "roof-left"), "Dach", RenderableKind.ROOF, centerX - span / 2.0, baseY, centerZ, slopeLength, ROOF_THICKNESS, depth, RotationAxis.Z, -roof.pitchAngle().degrees(), "roof", 1.0));
            boxes.add(new RenderableBox(new SelectionKey(RenderableKind.ROOF, "Dach", "roof-right"), "Dach", RenderableKind.ROOF, centerX + span / 2.0, baseY, centerZ, slopeLength, ROOF_THICKNESS, depth, RotationAxis.Z, roof.pitchAngle().degrees(), "roof", 1.0));
        }
        return boxes;
    }

    private Footprint footprint(List<Level> levels) {
        Double minX = null;
        Double maxX = null;
        Double minZ = null;
        Double maxZ = null;
        for (Level level : levels) {
            for (Wall wall : level.walls()) {
                minX = updateMin(minX, Math.min(wall.axis().start().xMillimeters(), wall.axis().end().xMillimeters()));
                maxX = updateMax(maxX, Math.max(wall.axis().start().xMillimeters(), wall.axis().end().xMillimeters()));
                minZ = updateMin(minZ, Math.min(wall.axis().start().yMillimeters(), wall.axis().end().yMillimeters()));
                maxZ = updateMax(maxZ, Math.max(wall.axis().start().yMillimeters(), wall.axis().end().yMillimeters()));
            }
            for (Room room : level.rooms()) {
                double[] bounds = bounds(room.outline());
                minX = updateMin(minX, bounds[0]);
                maxX = updateMax(maxX, bounds[1]);
                minZ = updateMin(minZ, bounds[2]);
                maxZ = updateMax(maxZ, bounds[3]);
            }
            for (Staircase staircase : level.staircases()) {
                minX = updateMin(minX, staircase.minX());
                maxX = updateMax(maxX, staircase.maxX());
                minZ = updateMin(minZ, staircase.minY());
                maxZ = updateMax(maxZ, staircase.maxY());
            }
        }
        if (minX == null || maxX == null || minZ == null || maxZ == null) {
            return null;
        }
        return new Footprint(minX, maxX, minZ, maxZ);
    }

    private double[] bounds(List<PlanPoint> points) {
        double minX = points.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0);
        double maxX = points.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0);
        double minZ = points.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0);
        double maxZ = points.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0);
        return new double[]{minX, maxX, minZ, maxZ};
    }

    private Double updateMin(Double current, double candidate) {
        return current == null ? candidate : Math.min(current, candidate);
    }

    private Double updateMax(Double current, double candidate) {
        return current == null ? candidate : Math.max(current, candidate);
    }

    private record OpeningRange(
            RenderableKind kind,
            double startMillimeters,
            double endMillimeters,
            double lowerHeightMillimeters,
            double upperHeightMillimeters
    ) {
    }

    private record Footprint(double minX, double maxX, double minZ, double maxZ) {
    }
}
