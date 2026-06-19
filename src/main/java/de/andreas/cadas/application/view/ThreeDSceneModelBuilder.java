package de.andreas.cadas.application.view;

import de.andreas.cadas.application.layers.SurfaceLayerEffectService;
import de.andreas.cadas.application.layers.TileLayoutRequest;
import de.andreas.cadas.application.layers.TileLayoutService;
import de.andreas.cadas.application.layers.TilePlacement;
import de.andreas.cadas.application.layers.WallSurfaceSideService;
import de.andreas.cadas.application.layers.WallSurfaceTargetKey;
import de.andreas.cadas.application.view.WallSurfaceOpeningService.WallSurfaceInterval;
import de.andreas.cadas.application.view.WallSurfaceOpeningService.WallSurfaceRectangle;
import de.andreas.cadas.application.view.WallSurfacePlanGeometryService.WallSurfacePlanPolygon;
import de.andreas.cadas.application.room.OrthogonalPolygonDecompositionService;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.FloorExtension;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.RoomObject;
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
    private static final double JOINT_HEIGHT = 2.5;
    private static final double JOINT_SURFACE_OFFSET = 2.0;
    private final OrthogonalPolygonDecompositionService decompositionService = new OrthogonalPolygonDecompositionService();
    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();
    private final WallSurfaceSideService wallSurfaceSideService = new WallSurfaceSideService();
    private final WallSurfaceOpeningService wallSurfaceOpeningService = new WallSurfaceOpeningService();
    private final WallSurfacePlanGeometryService wallSurfacePlanGeometryService = new WallSurfacePlanGeometryService();
    private final TileLayoutService tileLayoutService = new TileLayoutService();
    private final List<RenderableMesh> meshes = new ArrayList<>();

    public ThreeDSceneModel build(ProjectModel project, Set<String> visibleLevelNames, boolean renderSurfaceLayers) {
        return build(project, visibleLevelNames, renderSurfaceLayers, false);
    }

    public ThreeDSceneModel build(ProjectModel project, Set<String> visibleLevelNames, boolean renderSurfaceLayers, boolean surfaceRenderingMode) {
        return build(project, visibleLevelNames, renderSurfaceLayers, surfaceRenderingMode, true);
    }

    public ThreeDSceneModel build(ProjectModel project, Set<String> visibleLevelNames, boolean renderSurfaceLayers, boolean surfaceRenderingMode, boolean renderRoomObjects) {
        List<RenderableBox> boxes = new ArrayList<>();
        meshes.clear();
        Map<String, Double> levelBaseHeights = computeLevelBaseHeights(project.levels());

        for (Level level : project.levels()) {
            if (!visibleLevelNames.contains(level.name())) {
                continue;
            }
            double baseHeight = levelBaseHeights.getOrDefault(level.name(), 0.0);
            double floorOffset = maximumFloorThickness(level);
            double wallBaseHeight = baseHeight + floorOffset;
            buildRoomFloor(level, baseHeight);
            boxes.addAll(buildRoomFloorBodies(level, baseHeight));
            boxes.addAll(buildWallFloorSupports(level, baseHeight, floorOffset));
            boxes.addAll(buildRoomInteriors(level, baseHeight, renderSurfaceLayers, surfaceRenderingMode));
            boxes.addAll(buildWalls(level, wallBaseHeight, surfaceRenderingMode));
            if (renderSurfaceLayers) {
                boxes.addAll(buildWallSurfaceLayers(level, wallBaseHeight));
            }
            boxes.addAll(buildDoors(level, wallBaseHeight));
            boxes.addAll(buildWindows(level, wallBaseHeight));
            boxes.addAll(buildStairs(level, wallBaseHeight));
            boxes.addAll(buildFloorExtensions(level, baseHeight));
            if (renderSurfaceLayers) {
                boxes.addAll(buildFloorExtensionSurfaceLayers(level, baseHeight));
            }
            if (renderRoomObjects) {
                boxes.addAll(buildRoomObjects(level, wallBaseHeight));
            }
        }

        project.roof().ifPresent(roof -> boxes.addAll(buildRoof(project, roof, levelBaseHeights, visibleLevelNames)));
        return new ThreeDSceneModel(List.copyOf(boxes), List.copyOf(meshes));
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
        double objectHeight = level.roomObjects().stream().mapToDouble(roomObject -> roomObject.height().toMillimeters()).max().orElse(0.0);
        double roomHeight = level.rooms().stream()
                .mapToDouble(room -> surfaceLayerEffectService.effectiveMaximumCeilingHeightMillimeters(level, room)
                        + room.floorThickness().toMillimeters()
                        + surfaceLayerEffectService.floorLayerThicknessMillimeters(level, room)
                        + room.ceilingThickness().toMillimeters()
                        + surfaceLayerEffectService.ceilingLayerThicknessMillimeters(level, room))
                .max()
                .orElse(0.0);
        double stairHeight = level.staircases().stream().mapToDouble(staircase -> staircase.totalHeight().toMillimeters()).max().orElse(0.0);
        return Math.max(Math.max(wallHeight, objectHeight), Math.max(roomHeight, stairHeight));
    }

    private double maximumFloorThickness(Level level) {
        return level.rooms().stream()
                .mapToDouble(room -> room.floorThickness().toMillimeters())
                .max()
                .orElse(0.0);
    }

    private void buildRoomFloor(Level level, double baseHeight) {
        for (Room room : level.rooms()) {
            double floorThickness = room.floorThickness().toMillimeters();
            if (floorThickness <= 0.0) {
                continue;
            }
            addFlatSurfaceMesh(
                    new SelectionKey(RenderableKind.ROOM_FLOOR, level.name(), room.id().toString()),
                    level.name(),
                    RenderableKind.ROOM_FLOOR,
                    roomRectangles(room),
                    baseHeight,
                    floorThickness,
                    floorThickness,
                    "room-floor",
                    1.0
            );
        }
    }

    private List<RenderableBox> buildRoomFloorBodies(Level level, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (Room room : level.rooms()) {
            double floorThickness = room.floorThickness().toMillimeters();
            if (floorThickness <= 0.0) {
                continue;
            }
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

    private List<RenderableBox> buildWallFloorSupports(Level level, double baseHeight, double supportHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        if (supportHeight <= 0.0) {
            return boxes;
        }
        for (Wall wall : level.walls()) {
            double wallLength = wall.axis().length().toMillimeters();
            if (wallLength <= 0.0) {
                continue;
            }
            boxes.add(new RenderableBox(
                    new SelectionKey(RenderableKind.ROOM_FLOOR, level.name(), "wall-support-" + wall.id()),
                    level.name(),
                    RenderableKind.ROOM_FLOOR,
                    (wall.axis().start().xMillimeters() + wall.axis().end().xMillimeters()) / 2.0,
                    baseHeight + supportHeight / 2.0,
                    (wall.axis().start().yMillimeters() + wall.axis().end().yMillimeters()) / 2.0,
                    wallLength,
                    supportHeight,
                    wall.thickness().toMillimeters(),
                    RotationAxis.Y,
                    wall.axis().angle().degrees(),
                    "room-floor",
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
                buildFlatRoomCeilingMesh(level, room, baseHeight, rectangles, surfaceRenderingMode);
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

    private void buildFlatRoomCeilingMesh(Level level, Room room, double baseHeight, List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles, boolean surfaceRenderingMode) {
        double ceilingThickness = Math.max(0.1, room.ceilingThickness().toMillimeters());
        double baseY = baseHeight + room.floorThickness().toMillimeters() + room.maximumCeilingHeightMillimeters();
        addFlatSurfaceMesh(
                new SelectionKey(RenderableKind.ROOM_CEILING, level.name(), room.id().toString()),
                level.name(),
                RenderableKind.ROOM_CEILING,
                rectangles,
                baseY,
                0.0,
                ceilingThickness,
                "room-ceiling",
                surfaceRenderingMode ? 1.0 : 1.0
        );
    }

    private List<RenderableBox> buildFlatRoomVolume(Level level, Room room, double baseHeight, List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles) {
        List<RenderableBox> boxes = new ArrayList<>();
        double floorTop = baseHeight + surfaceLayerEffectService.effectiveFloorThickness(level, room).toMillimeters();
        double volumeHeight = Math.max(1.0, surfaceLayerEffectService.effectiveMaximumCeilingHeightMillimeters(level, room));
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
            boxes.addAll(buildFlatSurfaceLayerBodies(
                    level.name(),
                    layer.id().toString(),
                    rectangles,
                    currentHeight,
                    layer.thickness().toMillimeters()
            ));
            double layerTopHeight = currentHeight + layer.thickness().toMillimeters();
            addFlatSurfaceMesh(
                    new SelectionKey(RenderableKind.SURFACE_LAYER, level.name(), layer.id().toString()),
                    level.name(),
                    RenderableKind.SURFACE_LAYER,
                    rectangles,
                    currentHeight,
                    layer.thickness().toMillimeters(),
                    layer.thickness().toMillimeters(),
                    "surface-layer",
                    SURFACE_LAYER_OPACITY
            );
            boxes.addAll(buildJoints(level.name(), room, layer, rectangles, layerTopHeight));
            currentHeight = layerTopHeight;
        }
        return boxes;
    }

    private List<RenderableBox> buildFlatSurfaceLayerBodies(
            String levelName,
            String elementId,
            List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles,
            double baseHeight,
            double thickness
    ) {
        List<RenderableBox> boxes = new ArrayList<>();
        if (thickness <= 0.0) {
            return boxes;
        }
        for (OrthogonalPolygonDecompositionService.CellRectangle rectangle : rectangles) {
            boxes.add(new RenderableBox(
                    new SelectionKey(RenderableKind.SURFACE_LAYER, levelName, elementId),
                    levelName,
                    RenderableKind.SURFACE_LAYER,
                    rectangle.centerX(),
                    baseHeight + thickness / 2.0,
                    rectangle.centerY(),
                    rectangle.width(),
                    thickness,
                    rectangle.height(),
                    RotationAxis.Y,
                    0.0,
                    "surface-layer",
                    SURFACE_LAYER_OPACITY
            ));
        }
        return boxes;
    }

    private List<RenderableBox> buildJoints(String levelName, Room room, SurfaceLayer layer, List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles, double surfaceY) {
        List<RenderableBox> joints = new ArrayList<>();
        double jointWidthMm = layer.jointWidth().toMillimeters();
        if (jointWidthMm < 0.001) return joints;
        double jointHeight = JOINT_HEIGHT;
        double jointCenterY = surfaceY + JOINT_SURFACE_OFFSET + jointHeight / 2.0;
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
            for (OrthogonalPolygonDecompositionService.CellRectangle rect : rectangles) {
                double rx = rect.centerX() - rect.width() / 2.0;
                double rz = rect.centerY() - rect.height() / 2.0;
                double rxe = rx + rect.width();
                double rze = rz + rect.height();
                double jointX = Math.max(rx, tx);
                double jointXe = Math.min(rxe, tx + tw);
                double jointZ = ty + th - jointWidthMm / 2.0;
                double jointZe = jointZ + jointWidthMm;
                String hKey = String.format(java.util.Locale.US, "h:%.3f:%.3f:%.3f", jointZ, jointX, jointXe);
                if (jointX < jointXe && jointZ < jointZe && jointZ >= rz && jointZe <= rze && horizontalKeys.add(hKey)) {
                    joints.add(new RenderableBox(
                            new SelectionKey(RenderableKind.SURFACE_LAYER, levelName, layer.id().toString()),
                            levelName,
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
                double verticalJointX = tx + tw - jointWidthMm / 2.0;
                double verticalJointXe = verticalJointX + jointWidthMm;
                double verticalJointZ = Math.max(rz, ty);
                double verticalJointZe = Math.min(rze, ty + th);
                String vKey = String.format(java.util.Locale.US, "v:%.3f:%.3f:%.3f", verticalJointX, verticalJointZ, verticalJointZe);
                if (verticalJointX >= rx && verticalJointXe <= rxe && verticalJointZ < verticalJointZe && verticalKeys.add(vKey)) {
                    joints.add(new RenderableBox(
                            new SelectionKey(RenderableKind.SURFACE_LAYER, levelName, layer.id().toString()),
                            levelName,
                            RenderableKind.SURFACE_LAYER,
                            (verticalJointX + verticalJointXe) / 2.0,
                            jointCenterY,
                            (verticalJointZ + verticalJointZe) / 2.0,
                            verticalJointXe - verticalJointX,
                            jointHeight,
                            verticalJointZe - verticalJointZ,
                            RotationAxis.Y,
                            0.0,
                            "joint",
                            1.0
                    ));
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
                addSlopedSurfaceMesh(
                        new SelectionKey(RenderableKind.SURFACE_LAYER, level.name(), layer.id().toString()),
                        level.name(),
                        RenderableKind.SURFACE_LAYER,
                        room,
                        rectangles,
                        baseHeight + room.floorThickness().toMillimeters(),
                        -(offsetFromCeiling + layer.thickness().toMillimeters()),
                        layer.thickness().toMillimeters(),
                        "surface-layer",
                        SURFACE_LAYER_OPACITY
                );
            } else {
                double layerBottomHeight = baseHeight + room.floorThickness().toMillimeters() + room.roomHeight().toMillimeters() - offsetFromCeiling;
                addFlatSurfaceMesh(
                        new SelectionKey(RenderableKind.SURFACE_LAYER, level.name(), layer.id().toString()),
                        level.name(),
                        RenderableKind.SURFACE_LAYER,
                        rectangles,
                        layerBottomHeight - layer.thickness().toMillimeters(),
                        0.0,
                        layer.thickness().toMillimeters(),
                        "surface-layer",
                        SURFACE_LAYER_OPACITY
                );
                boxes.addAll(buildJoints(level.name(), room, layer, rectangles, layerBottomHeight - layer.thickness().toMillimeters()));
            }
            offsetFromCeiling += layer.thickness().toMillimeters();
        }
        return boxes;
    }

    private void addFlatSurfaceMesh(
            SelectionKey selectionKey,
            String levelName,
            RenderableKind kind,
            List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles,
            double baseY,
            double surfaceOffsetY,
            double height,
            String materialKey,
            double opacity
    ) {
        List<Float> vertexValues = new ArrayList<>();
        float surfaceY = (float) surfaceOffsetY;
        for (OrthogonalPolygonDecompositionService.CellRectangle rectangle : rectangles) {
            float x0 = (float) rectangle.minX();
            float x1 = (float) rectangle.maxX();
            float z0 = (float) rectangle.minY();
            float z1 = (float) rectangle.maxY();
            vertexValues.add(x0); vertexValues.add(surfaceY); vertexValues.add(z0);
            vertexValues.add(x1); vertexValues.add(surfaceY); vertexValues.add(z0);
            vertexValues.add(x1); vertexValues.add(surfaceY); vertexValues.add(z1);
            vertexValues.add(x0); vertexValues.add(surfaceY); vertexValues.add(z0);
            vertexValues.add(x1); vertexValues.add(surfaceY); vertexValues.add(z1);
            vertexValues.add(x0); vertexValues.add(surfaceY); vertexValues.add(z1);
        }
        if (vertexValues.isEmpty()) {
            return;
        }
        float[] points = new float[vertexValues.size()];
        for (int index = 0; index < vertexValues.size(); index++) {
            points[index] = vertexValues.get(index);
        }
        meshes.add(new RenderableMesh(
                selectionKey,
                levelName,
                kind,
                points,
                rectangles.size() * 2,
                baseY,
                height,
                materialKey,
                opacity
        ));
    }

    private void addSlopedSurfaceMesh(
            SelectionKey selectionKey,
            String levelName,
            RenderableKind kind,
            Room room,
            List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles,
            double baseY,
            double verticalOffsetY,
            double height,
            String materialKey,
            double opacity
    ) {
        List<Float> vertexValues = new ArrayList<>();
        for (OrthogonalPolygonDecompositionService.CellRectangle rectangle : rectangles) {
            double x0 = rectangle.minX();
            double x1 = rectangle.maxX();
            double z0 = rectangle.minY();
            double z1 = rectangle.maxY();
            double y00 = room.ceilingHeightAt(new PlanPoint(x0, z0)) + verticalOffsetY;
            double y10 = room.ceilingHeightAt(new PlanPoint(x1, z0)) + verticalOffsetY;
            double y11 = room.ceilingHeightAt(new PlanPoint(x1, z1)) + verticalOffsetY;
            double y01 = room.ceilingHeightAt(new PlanPoint(x0, z1)) + verticalOffsetY;
            addTriangle(vertexValues, x0, y00, z0, x1, y10, z0, x1, y11, z1);
            addTriangle(vertexValues, x0, y00, z0, x1, y11, z1, x0, y01, z1);
        }
        addMesh(selectionKey, levelName, kind, vertexValues, baseY, height, materialKey, opacity);
    }

    private void addMesh(
            SelectionKey selectionKey,
            String levelName,
            RenderableKind kind,
            List<Float> vertexValues,
            double baseY,
            double height,
            String materialKey,
            double opacity
    ) {
        if (vertexValues.isEmpty()) {
            return;
        }
        float[] points = new float[vertexValues.size()];
        for (int index = 0; index < vertexValues.size(); index++) {
            points[index] = vertexValues.get(index);
        }
        meshes.add(new RenderableMesh(
                selectionKey,
                levelName,
                kind,
                points,
                vertexValues.size() / 9,
                baseY,
                height,
                materialKey,
                opacity
        ));
    }

    private void addTriangle(
            List<Float> vertexValues,
            double ax,
            double ay,
            double az,
            double bx,
            double by,
            double bz,
            double cx,
            double cy,
            double cz
    ) {
        vertexValues.add((float) ax);
        vertexValues.add((float) ay);
        vertexValues.add((float) az);
        vertexValues.add((float) bx);
        vertexValues.add((float) by);
        vertexValues.add((float) bz);
        vertexValues.add((float) cx);
        vertexValues.add((float) cy);
        vertexValues.add((float) cz);
    }

    private void addQuad(
            List<Float> vertexValues,
            MeshPoint first,
            MeshPoint second,
            MeshPoint third,
            MeshPoint fourth
    ) {
        addTriangle(vertexValues, first.x(), first.y(), first.z(), second.x(), second.y(), second.z(), third.x(), third.y(), third.z());
        addTriangle(vertexValues, first.x(), first.y(), first.z(), third.x(), third.y(), third.z(), fourth.x(), fourth.y(), fourth.z());
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
                baseHeight + surfaceLayerEffectService.effectiveFloorThickness(level, room).toMillimeters(),
                0.0,
                "room-volume",
                ROOM_VOLUME_OPACITY,
                new SelectionKey(RenderableKind.ROOM_VOLUME, level.name(), room.id().toString())
        );
    }

    private List<RenderableBox> buildSlopedCeiling(Level level, Room room, double baseHeight, List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles, boolean surfaceRenderingMode) {
        addSlopedSurfaceMesh(
                new SelectionKey(RenderableKind.ROOM_CEILING, level.name(), room.id().toString()),
                level.name(),
                RenderableKind.ROOM_CEILING,
                room,
                rectangles,
                baseHeight + room.floorThickness().toMillimeters(),
                0.0,
                Math.max(0.1, room.ceilingThickness().toMillimeters()),
                "room-ceiling",
                surfaceRenderingMode ? 1.0 : 1.0
        );
        return List.of();
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
                        ? surfaceLayerEffectService.effectiveHeightAt(level, room, new PlanPoint(centerX, centerZ))
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
            List<OpeningRange> openings = openingsForWall(level, wall);
            double cursor = 0.0;
            for (OpeningRange opening : openings) {
                if (opening.startMillimeters() > cursor) {
                    boxes.addAll(segmentToBoxes(level.name(), wall, cursor, opening.startMillimeters(), baseHeight, 0.0, "wall", surfaceRenderingMode));
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
                boxes.addAll(segmentToBoxes(level.name(), wall, cursor, wallLength, baseHeight, 0.0, "wall", surfaceRenderingMode));
            }
        }
        return boxes;
    }

    private List<RenderableBox> buildWallSurfaceLayers(Level level, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (Wall wall : level.walls()) {
            level.surfaceLayerStacks().stream()
                    .filter(stack -> stack.surfaceType() == SurfaceType.WALL_INTERIOR || stack.surfaceType() == SurfaceType.WALL_EXTERIOR)
                    .filter(stack -> WallSurfaceTargetKey.matchesWall(stack.targetKey(), wall.id()))
                    .forEach(stack -> boxes.addAll(buildWallSurfaceLayerBoxes(level, level.name(), wall, stack, baseHeight)));
        }
        return boxes;
    }

    private List<RenderableBox> buildWallSurfaceLayerBoxes(Level level, String levelName, Wall wall, SurfaceLayerStack stack, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        double cumulativeThickness = wall.thickness().toMillimeters() / 2.0;
        WallSurfaceSideService.WallLayerSides wallLayerSides = wallSurfaceSideService.resolve(level, wall, stack.surfaceType(), stack.targetKey());
        for (int layerIndex = 0; layerIndex < stack.layers().size(); layerIndex++) {
            SurfaceLayer layer = stack.layers().get(layerIndex);
            if (!layer.visible()) {
                cumulativeThickness += layer.thickness().toMillimeters();
                continue;
            }
            double centerOffset = cumulativeThickness + layer.thickness().toMillimeters() / 2.0;
            if (wallLayerSides.positiveSide()) {
                int visibleLayerIndex = layerIndex;
                List<WallSurfaceRectangle> visibleRectangles = wallSurfaceOpeningService.visibleRectangles(level, wall, 1.0);
                visibleRectangles.forEach(rectangle -> wallSurfaceLayerBox(level, levelName, wall, stack, layer, visibleLayerIndex, rectangle, baseHeight, centerOffset).ifPresent(boxes::add));
                boxes.addAll(buildWallSurfaceJoints(level, levelName, wall, stack, layer, visibleLayerIndex, visibleRectangles, baseHeight, cumulativeThickness + layer.thickness().toMillimeters() + JOINT_SURFACE_OFFSET + JOINT_HEIGHT / 2.0));
            }
            if (wallLayerSides.negativeSide()) {
                int visibleLayerIndex = layerIndex;
                List<WallSurfaceRectangle> visibleRectangles = wallSurfaceOpeningService.visibleRectangles(level, wall, -1.0);
                visibleRectangles.forEach(rectangle -> wallSurfaceLayerBox(level, levelName, wall, stack, layer, visibleLayerIndex, rectangle, baseHeight, -centerOffset).ifPresent(boxes::add));
                boxes.addAll(buildWallSurfaceJoints(level, levelName, wall, stack, layer, visibleLayerIndex, visibleRectangles, baseHeight, -(cumulativeThickness + layer.thickness().toMillimeters() + JOINT_SURFACE_OFFSET + JOINT_HEIGHT / 2.0)));
            }
            cumulativeThickness += layer.thickness().toMillimeters();
        }
        return boxes;
    }

    private Optional<RenderableBox> wallSurfaceLayerBox(
            Level level,
            String levelName,
            Wall wall,
            SurfaceLayerStack stack,
            SurfaceLayer layer,
            int layerIndex,
            WallSurfaceRectangle rectangle,
            double baseHeight,
            double perpendicularOffset
    ) {
        WallSurfacePlanPolygon polygon = wallSurfacePlanGeometryService.surfacePolygon(
                level,
                wall,
                stack,
                layer,
                layerIndex,
                perpendicularOffset,
                new WallSurfaceInterval(rectangle.startMillimeters(), rectangle.endMillimeters())
        );
        if (wall.hasVariableTopHeight()) {
            addWallSurfaceLayerMesh(levelName, wall, layer, rectangle, baseHeight, polygon);
            return Optional.empty();
        }
        double width = polygon.points().get(0).distanceTo(polygon.points().get(1)).toMillimeters();
        PlanPoint center = polygonCenter(polygon);
        return Optional.of(new RenderableBox(
                new SelectionKey(RenderableKind.SURFACE_LAYER, levelName, layer.id().toString()),
                levelName,
                RenderableKind.SURFACE_LAYER,
                center.xMillimeters(),
                baseHeight + (rectangle.lowerHeightMillimeters() + rectangle.upperHeightMillimeters()) / 2.0,
                center.yMillimeters(),
                width,
                rectangle.heightMillimeters(),
                layer.thickness().toMillimeters(),
                RotationAxis.Y,
                wall.axis().angle().degrees(),
                "surface-layer",
                SURFACE_LAYER_OPACITY
        ));
    }

    private void addWallSurfaceLayerMesh(
            String levelName,
            Wall wall,
            SurfaceLayer layer,
            WallSurfaceRectangle rectangle,
            double baseHeight,
            WallSurfacePlanPolygon polygon
    ) {
        double lowerHeight = rectangle.lowerHeightMillimeters();
        PlanPoint startInnerPoint = polygon.points().get(0);
        PlanPoint endInnerPoint = polygon.points().get(1);
        PlanPoint endOuterPoint = polygon.points().get(2);
        PlanPoint startOuterPoint = polygon.points().get(3);
        double startInnerTop = wallSurfaceTopAt(wall, rectangle, startInnerPoint, lowerHeight);
        double endInnerTop = wallSurfaceTopAt(wall, rectangle, endInnerPoint, lowerHeight);
        double endOuterTop = wallSurfaceTopAt(wall, rectangle, endOuterPoint, lowerHeight);
        double startOuterTop = wallSurfaceTopAt(wall, rectangle, startOuterPoint, lowerHeight);
        double maximumTop = Math.max(Math.max(startInnerTop, endInnerTop), Math.max(startOuterTop, endOuterTop));
        if (maximumTop <= lowerHeight + 0.001) {
            return;
        }
        MeshPoint startInnerBottom = meshPoint(startInnerPoint, lowerHeight);
        MeshPoint startInnerTopPoint = meshPoint(startInnerPoint, startInnerTop);
        MeshPoint endInnerBottom = meshPoint(endInnerPoint, lowerHeight);
        MeshPoint endInnerTopPoint = meshPoint(endInnerPoint, endInnerTop);
        MeshPoint startOuterBottom = meshPoint(startOuterPoint, lowerHeight);
        MeshPoint startOuterTopPoint = meshPoint(startOuterPoint, startOuterTop);
        MeshPoint endOuterBottom = meshPoint(endOuterPoint, lowerHeight);
        MeshPoint endOuterTopPoint = meshPoint(endOuterPoint, endOuterTop);

        List<Float> vertexValues = new ArrayList<>();
        addQuad(vertexValues, startInnerBottom, endInnerBottom, endInnerTopPoint, startInnerTopPoint);
        addQuad(vertexValues, endOuterBottom, startOuterBottom, startOuterTopPoint, endOuterTopPoint);
        addQuad(vertexValues, startInnerTopPoint, endInnerTopPoint, endOuterTopPoint, startOuterTopPoint);
        addQuad(vertexValues, startOuterBottom, endOuterBottom, endInnerBottom, startInnerBottom);
        addQuad(vertexValues, startOuterBottom, startInnerBottom, startInnerTopPoint, startOuterTopPoint);
        addQuad(vertexValues, endInnerBottom, endOuterBottom, endOuterTopPoint, endInnerTopPoint);
        addMesh(
                new SelectionKey(RenderableKind.SURFACE_LAYER, levelName, layer.id().toString()),
                levelName,
                RenderableKind.SURFACE_LAYER,
                vertexValues,
                baseHeight,
                maximumTop,
                "surface-layer",
                SURFACE_LAYER_OPACITY
        );
    }

    private PlanPoint polygonCenter(WallSurfacePlanPolygon polygon) {
        double sumX = 0.0;
        double sumY = 0.0;
        for (PlanPoint point : polygon.points()) {
            sumX += point.xMillimeters();
            sumY += point.yMillimeters();
        }
        return new PlanPoint(sumX / polygon.points().size(), sumY / polygon.points().size());
    }

    private double wallSurfaceTopAt(Wall wall, WallSurfaceRectangle rectangle, PlanPoint point, double lowerHeight) {
        double localDistance = localDistanceOnWall(wall, point);
        return Math.max(lowerHeight, Math.min(rectangle.upperHeightMillimeters(), wall.heightAt(localDistance)));
    }

    private double localDistanceOnWall(Wall wall, PlanPoint point) {
        double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double length = Math.max(1.0, Math.hypot(dx, dy));
        double tangentX = dx / length;
        double tangentY = dy / length;
        return (point.xMillimeters() - wall.axis().start().xMillimeters()) * tangentX
                + (point.yMillimeters() - wall.axis().start().yMillimeters()) * tangentY;
    }

    private MeshPoint meshPoint(PlanPoint point, double height) {
        return new MeshPoint(point.xMillimeters(), height, point.yMillimeters());
    }

    private List<RenderableBox> buildWallSurfaceJoints(
            Level level,
            String levelName,
            Wall wall,
            SurfaceLayerStack stack,
            SurfaceLayer layer,
            int layerIndex,
            List<WallSurfaceRectangle> visibleRectangles,
            double baseHeight,
            double perpendicularOffset
    ) {
        List<RenderableBox> joints = new ArrayList<>();
        double wallLength = wall.axis().length().toMillimeters();
        double wallHeight = wall.maximumHeightMillimeters();
        double jointWidth = layer.jointWidth().toMillimeters();
        if (wallLength <= 0.0 || wallHeight <= 0.0 || jointWidth < 0.001 || visibleRectangles.isEmpty()) {
            return joints;
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
        List<TilePlacement> tiles = tileLayoutService.fillSurface(request);
        var horizontalKeys = new java.util.HashSet<String>();
        var verticalKeys = new java.util.HashSet<String>();
        for (TilePlacement tile : tiles) {
            double tileX = tile.xOffset().toMillimeters();
            double tileY = tile.yOffset().toMillimeters();
            double tileWidth = tile.width().toMillimeters();
            double tileHeight = tile.height().toMillimeters();
            addClippedWallSurfaceJoints(
                    joints,
                    horizontalKeys,
                    "h",
                    level,
                    levelName,
                    wall,
                    stack,
                    layer,
                    layerIndex,
                    visibleRectangles,
                    tileX,
                    tileX + tileWidth,
                    tileY + tileHeight - jointWidth,
                    tileY + tileHeight,
                    baseHeight,
                    perpendicularOffset
            );
            addClippedWallSurfaceJoints(
                    joints,
                    verticalKeys,
                    "v",
                    level,
                    levelName,
                    wall,
                    stack,
                    layer,
                    layerIndex,
                    visibleRectangles,
                    tileX + tileWidth - jointWidth,
                    tileX + tileWidth,
                    tileY,
                    tileY + tileHeight,
                    baseHeight,
                    perpendicularOffset
            );
        }
        return joints;
    }

    private void addClippedWallSurfaceJoints(
            List<RenderableBox> joints,
            Set<String> keys,
            String prefix,
            Level level,
            String levelName,
            Wall wall,
            SurfaceLayerStack stack,
            SurfaceLayer layer,
            int layerIndex,
            List<WallSurfaceRectangle> visibleRectangles,
            double localStartX,
            double localEndX,
            double localLowerY,
            double localUpperY,
            double baseHeight,
            double perpendicularOffset
    ) {
        for (WallSurfaceRectangle rectangle : visibleRectangles) {
            double clippedStartX = Math.max(localStartX, rectangle.startMillimeters());
            double clippedEndX = Math.min(localEndX, rectangle.endMillimeters());
            double clippedLowerY = Math.max(localLowerY, rectangle.lowerHeightMillimeters());
            double clippedUpperY = Math.min(localUpperY, rectangle.upperHeightMillimeters());
            clippedUpperY = Math.min(clippedUpperY, wall.heightAt((clippedStartX + clippedEndX) / 2.0));
            if (clippedEndX - clippedStartX <= 0.001 || clippedUpperY - clippedLowerY <= 0.001) {
                continue;
            }
            WallSurfaceInterval adjustedInterval = adjustedJointInterval(
                    level,
                    wall,
                    stack,
                    layer,
                    layerIndex,
                    perpendicularOffset,
                    prefix,
                    clippedStartX,
                    clippedEndX
            );
            double jointStartX = adjustedInterval.startMillimeters();
            double jointEndX = adjustedInterval.endMillimeters();
            String key = String.format(
                    java.util.Locale.US,
                    "%s:%.3f:%.3f:%.3f:%.3f",
                    prefix,
                    jointStartX,
                    jointEndX,
                    clippedLowerY,
                    clippedUpperY
            );
            if (!keys.add(key)) {
                continue;
            }
            joints.add(wallSurfaceJointBox(
                    levelName,
                    wall,
                    layer,
                    (jointStartX + jointEndX) / 2.0,
                    baseHeight + (clippedLowerY + clippedUpperY) / 2.0,
                    jointEndX - jointStartX,
                    clippedUpperY - clippedLowerY,
                    perpendicularOffset
            ));
        }
    }

    private WallSurfaceInterval adjustedJointInterval(
            Level level,
            Wall wall,
            SurfaceLayerStack stack,
            SurfaceLayer layer,
            int layerIndex,
            double perpendicularOffset,
            String prefix,
            double clippedStartX,
            double clippedEndX
    ) {
        if (!"h".equals(prefix)) {
            return new WallSurfaceInterval(clippedStartX, clippedEndX);
        }
        WallSurfacePlanPolygon polygon = wallSurfacePlanGeometryService.surfacePolygon(
                level,
                wall,
                stack,
                layer,
                layerIndex,
                perpendicularOffset,
                new WallSurfaceInterval(clippedStartX, clippedEndX)
        );
        return new WallSurfaceInterval(
                localDistanceOnWall(wall, polygon.points().get(0)),
                localDistanceOnWall(wall, polygon.points().get(1))
        );
    }

    private RenderableBox wallSurfaceJointBox(
            String levelName,
            Wall wall,
            SurfaceLayer layer,
            double localCenterX,
            double centerY,
            double width,
            double height,
            double perpendicularOffset
    ) {
        double wallLength = wall.axis().length().toMillimeters();
        PlanPoint wallPoint = wall.axis().pointAt(Length.ofMillimeters(localCenterX));
        double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double length = Math.max(1.0, Math.hypot(dx, dy));
        double normalX = -dy / length;
        double normalY = dx / length;
        return new RenderableBox(
                new SelectionKey(RenderableKind.SURFACE_LAYER, levelName, layer.id().toString()),
                levelName,
                RenderableKind.SURFACE_LAYER,
                wallPoint.xMillimeters() + normalX * perpendicularOffset,
                centerY,
                wallPoint.yMillimeters() + normalY * perpendicularOffset,
                width,
                height,
                JOINT_HEIGHT,
                RotationAxis.Y,
                wall.axis().angle().degrees(),
                "joint",
                1.0
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
        addWallSegmentMesh(levelName, wall, startMillimeters, endMillimeters, baseHeight, subtractBottomMillimeters, materialKey, surfaceRenderingMode ? 1.0 : 1.0);
        return List.of();
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

    private void addWallSegmentMesh(
            String levelName,
            Wall wall,
            double startMillimeters,
            double endMillimeters,
            double baseHeight,
            double subtractBottomMillimeters,
            String materialKey,
            double opacity
    ) {
        double startHeight = Math.max(0.0, wall.heightAt(startMillimeters) - subtractBottomMillimeters);
        double endHeight = Math.max(0.0, wall.heightAt(endMillimeters) - subtractBottomMillimeters);
        if (startHeight <= 0.001 && endHeight <= 0.001) {
            return;
        }
        double halfThickness = wall.thickness().toMillimeters() / 2.0;
        MeshPoint startLeftBottom = wallMeshPoint(wall, startMillimeters, -halfThickness, 0.0);
        MeshPoint startLeftTop = wallMeshPoint(wall, startMillimeters, -halfThickness, startHeight);
        MeshPoint startRightBottom = wallMeshPoint(wall, startMillimeters, halfThickness, 0.0);
        MeshPoint startRightTop = wallMeshPoint(wall, startMillimeters, halfThickness, startHeight);
        MeshPoint endLeftBottom = wallMeshPoint(wall, endMillimeters, -halfThickness, 0.0);
        MeshPoint endLeftTop = wallMeshPoint(wall, endMillimeters, -halfThickness, endHeight);
        MeshPoint endRightBottom = wallMeshPoint(wall, endMillimeters, halfThickness, 0.0);
        MeshPoint endRightTop = wallMeshPoint(wall, endMillimeters, halfThickness, endHeight);

        List<Float> vertexValues = new ArrayList<>();
        addQuad(vertexValues, startLeftBottom, endLeftBottom, endLeftTop, startLeftTop);
        addQuad(vertexValues, endRightBottom, startRightBottom, startRightTop, endRightTop);
        addQuad(vertexValues, startLeftTop, endLeftTop, endRightTop, startRightTop);
        addQuad(vertexValues, startRightBottom, endRightBottom, endLeftBottom, startLeftBottom);
        addQuad(vertexValues, startRightBottom, startLeftBottom, startLeftTop, startRightTop);
        addQuad(vertexValues, endLeftBottom, endRightBottom, endRightTop, endLeftTop);
        addMesh(
                new SelectionKey(RenderableKind.WALL, levelName, wall.id().toString()),
                levelName,
                RenderableKind.WALL,
                vertexValues,
                baseHeight,
                Math.max(startHeight, endHeight),
                materialKey,
                opacity
        );
    }

    private MeshPoint wallMeshPoint(Wall wall, double localDistance, double normalOffset, double height) {
        double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double length = Math.max(1.0, Math.hypot(dx, dy));
        double tangentX = dx / length;
        double tangentY = dy / length;
        double normalX = -tangentY;
        double normalY = tangentX;
        return new MeshPoint(
                wall.axis().start().xMillimeters() + tangentX * localDistance + normalX * normalOffset,
                height,
                wall.axis().start().yMillimeters() + tangentY * localDistance + normalY * normalOffset
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
        double startLanding = staircase.startLandingWidth().toMillimeters();
        double endLanding = staircase.endLandingWidth().toMillimeters();
        double regularRun = staircase.heightMillimeters() - startLanding - endLanding;
        double stepDepth = regularRun / staircase.regularStepCount();
        int heightIndex = 0;
        if (startLanding > 0) {
            boxes.add(stairBox(levelName, staircase, baseHeight + stepHeight / 2.0,
                    staircase.widthMillimeters() / 2.0, startLanding / 2.0,
                    staircase.widthMillimeters(), startLanding, 0.0));
            heightIndex++;
        }
        for (int step = 0; step < staircase.regularStepCount(); step++) {
            boxes.add(stairBox(
                    levelName,
                    staircase,
                    baseHeight + stepHeight * (heightIndex + step + 0.5),
                    staircase.widthMillimeters() / 2.0,
                    startLanding + stepDepth * step + stepDepth / 2.0,
                    staircase.widthMillimeters(),
                    stepDepth,
                    0.0
            ));
        }
        if (endLanding > 0) {
            boxes.add(stairBox(levelName, staircase,
                    baseHeight + staircase.totalHeight().toMillimeters() - stepHeight / 2.0,
                    staircase.widthMillimeters() / 2.0,
                    staircase.heightMillimeters() - endLanding / 2.0,
                    staircase.widthMillimeters(), endLanding, 0.0));
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

    private List<RenderableBox> buildRoomObjects(Level level, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (RoomObject roomObject : level.roomObjects()) {
            if (!roomObject.visible()) {
                continue;
            }
            double height = Math.max(1.0, roomObject.height().toMillimeters());
            boxes.add(new RenderableBox(
                    new SelectionKey(RenderableKind.ROOM_OBJECT, level.name(), roomObject.id().toString()),
                    level.name(),
                    RenderableKind.ROOM_OBJECT,
                    roomObject.center().xMillimeters(),
                    baseHeight + height / 2.0,
                    roomObject.center().yMillimeters(),
                    roomObject.width().toMillimeters(),
                    height,
                    roomObject.depth().toMillimeters(),
                    RotationAxis.Y,
                    roomObject.rotationQuarterTurns() * 90.0,
                    "room-object",
                    0.78
            ));
        }
        return boxes;
    }

    private List<RenderableBox> buildFloorExtensions(Level level, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (FloorExtension extension : level.floorExtensions()) {
            double thickness = extension.slabThickness().toMillimeters();
            boxes.add(new RenderableBox(
                    new SelectionKey(RenderableKind.FLOOR_EXTENSION, level.name(), extension.id().toString()),
                    level.name(),
                    RenderableKind.FLOOR_EXTENSION,
                    (extension.minX() + extension.maxX()) / 2.0,
                    baseHeight - thickness / 2.0,
                    (extension.minY() + extension.maxY()) / 2.0,
                    extension.widthMillimeters(),
                    thickness,
                    extension.depthMillimeters(),
                    RotationAxis.Y,
                    0.0,
                    extension.type() == de.andreas.cadas.domain.model.FloorExtensionType.BALCONY ? "balcony" : "gallery",
                    1.0
            ));
        }
        return boxes;
    }

    private List<RenderableBox> buildFloorExtensionSurfaceLayers(Level level, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (FloorExtension extension : level.floorExtensions()) {
            SurfaceLayerStack stack = level.findSurfaceLayerStack(SurfaceType.FLOOR, extension.surfaceTargetKey());
            if (stack == null) {
                continue;
            }
            double currentHeight = baseHeight;
            for (SurfaceLayer layer : stack.layers()) {
                if (!layer.visible() || layer.thickness().toMillimeters() <= 0) {
                    continue;
                }
                double thickness = layer.thickness().toMillimeters();
                boxes.add(new RenderableBox(
                        new SelectionKey(RenderableKind.SURFACE_LAYER, level.name(), layer.id().toString()),
                        level.name(), RenderableKind.SURFACE_LAYER,
                        (extension.minX() + extension.maxX()) / 2.0,
                        currentHeight + thickness / 2.0,
                        (extension.minY() + extension.maxY()) / 2.0,
                        extension.widthMillimeters(), thickness, extension.depthMillimeters(),
                        RotationAxis.Y, 0.0, "surface-layer", 1.0
                ));
                currentHeight += thickness;
            }
        }
        return boxes;
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

    private record MeshPoint(double x, double y, double z) {
    }
}
