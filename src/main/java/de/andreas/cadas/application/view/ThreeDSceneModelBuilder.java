package de.andreas.cadas.application.view;

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
    private static final double LEVEL_GAP = 600.0;

    public ThreeDSceneModel build(ProjectModel project, Set<String> visibleLevelNames, boolean renderSurfaceLayers) {
        List<RenderableBox> boxes = new ArrayList<>();
        Map<String, Double> levelBaseHeights = computeLevelBaseHeights(project.levels());

        for (Level level : project.levels()) {
            if (!visibleLevelNames.contains(level.name())) {
                continue;
            }
            double baseHeight = levelBaseHeights.getOrDefault(level.name(), 0.0);
            boxes.addAll(buildRooms(level, baseHeight, renderSurfaceLayers));
            boxes.addAll(buildWalls(level, baseHeight));
            boxes.addAll(buildDoors(level, baseHeight));
            boxes.addAll(buildWindows(level, baseHeight));
            boxes.addAll(buildStairs(level, baseHeight));
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
        double wallHeight = level.walls().stream().mapToDouble(wall -> wall.height().toMillimeters()).max().orElse(2750.0);
        double roomHeight = level.rooms().stream()
                .mapToDouble(room -> room.roomHeight().toMillimeters() + room.floorThickness().toMillimeters() + room.ceilingThickness().toMillimeters())
                .max()
                .orElse(0.0);
        double stairHeight = level.staircases().stream().mapToDouble(staircase -> staircase.totalHeight().toMillimeters()).max().orElse(0.0);
        return Math.max(wallHeight, Math.max(roomHeight, stairHeight));
    }

    private List<RenderableBox> buildRooms(Level level, double baseHeight, boolean renderSurfaceLayers) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (Room room : level.rooms()) {
            double[] bounds = bounds(room.outline());
            double centerX = (bounds[0] + bounds[1]) / 2.0;
            double centerZ = (bounds[2] + bounds[3]) / 2.0;
            double width = bounds[1] - bounds[0];
            double depth = bounds[3] - bounds[2];

            boxes.add(new RenderableBox(
                    new SelectionKey(RenderableKind.ROOM_FLOOR, level.name(), room.id().toString()),
                    level.name(),
                    RenderableKind.ROOM_FLOOR,
                    centerX,
                    baseHeight + room.floorThickness().toMillimeters() / 2.0,
                    centerZ,
                    width,
                    room.floorThickness().toMillimeters(),
                    depth,
                    RotationAxis.Y,
                    0.0,
                    "room-floor",
                    1.0
            ));
            boxes.add(new RenderableBox(
                    new SelectionKey(RenderableKind.ROOM_CEILING, level.name(), room.id().toString()),
                    level.name(),
                    RenderableKind.ROOM_CEILING,
                    centerX,
                    baseHeight + room.floorThickness().toMillimeters() + room.roomHeight().toMillimeters() + room.ceilingThickness().toMillimeters() / 2.0,
                    centerZ,
                    width,
                    room.ceilingThickness().toMillimeters(),
                    depth,
                    RotationAxis.Y,
                    0.0,
                    "room-ceiling",
                    1.0
            ));
            boxes.add(new RenderableBox(
                    new SelectionKey(RenderableKind.ROOM_VOLUME, level.name(), room.id().toString()),
                    level.name(),
                    RenderableKind.ROOM_VOLUME,
                    centerX,
                    baseHeight + room.floorThickness().toMillimeters() + room.roomHeight().toMillimeters() / 2.0,
                    centerZ,
                    Math.max(50.0, width - 20.0),
                    room.roomHeight().toMillimeters(),
                    Math.max(50.0, depth - 20.0),
                    RotationAxis.Y,
                    0.0,
                    "room-volume",
                    ROOM_VOLUME_OPACITY
            ));

            if (renderSurfaceLayers) {
                boxes.addAll(buildSurfaceLayers(level, room, bounds, baseHeight));
            }
        }
        return boxes;
    }

    private List<RenderableBox> buildSurfaceLayers(Level level, Room room, double[] bounds, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        double currentHeight = baseHeight;
        for (SurfaceLayerStack stack : level.surfaceLayerStacks()) {
            if (stack.surfaceType() != SurfaceType.FLOOR || !matchesRoom(stack, room)) {
                continue;
            }
            for (SurfaceLayer layer : stack.layers()) {
                if (!layer.visible()) {
                    currentHeight += layer.thickness().toMillimeters();
                    continue;
                }
                boxes.add(new RenderableBox(
                        new SelectionKey(RenderableKind.SURFACE_LAYER, level.name(), layer.id().toString()),
                        level.name(),
                        RenderableKind.SURFACE_LAYER,
                        (bounds[0] + bounds[1]) / 2.0,
                        currentHeight + layer.thickness().toMillimeters() / 2.0,
                        (bounds[2] + bounds[3]) / 2.0,
                        bounds[1] - bounds[0],
                        layer.thickness().toMillimeters(),
                        bounds[3] - bounds[2],
                        RotationAxis.Y,
                        0.0,
                        "surface-layer",
                        SURFACE_LAYER_OPACITY
                ));
                currentHeight += layer.thickness().toMillimeters();
            }
        }
        return boxes;
    }

    private boolean matchesRoom(SurfaceLayerStack stack, Room room) {
        return stack.targetKey().equals(room.id().toString())
                || stack.targetKey().equalsIgnoreCase(room.name())
                || stack.targetKey().contains(room.id().toString())
                || stack.targetKey().contains(room.name());
    }

    private List<RenderableBox> buildWalls(Level level, double baseHeight) {
        List<RenderableBox> boxes = new ArrayList<>();
        for (Wall wall : level.walls()) {
            List<OpeningRange> openings = openingsForWall(level, wall);
            double cursor = 0.0;
            for (OpeningRange opening : openings) {
                if (opening.startMillimeters() > cursor) {
                    boxes.add(segmentToBox(level.name(), wall, cursor, opening.startMillimeters(), baseHeight, 0.0, wall.height().toMillimeters(), "wall"));
                }
                if (opening.kind() == RenderableKind.DOOR) {
                    double upperHeight = Math.max(0.0, wall.height().toMillimeters() - opening.upperHeightMillimeters());
                    if (upperHeight > 0.0) {
                        boxes.add(segmentToBox(level.name(), wall, opening.startMillimeters(), opening.endMillimeters(), baseHeight + opening.upperHeightMillimeters(), 0.0, upperHeight, "wall"));
                    }
                } else if (opening.kind() == RenderableKind.WINDOW) {
                    if (opening.lowerHeightMillimeters() > 0.0) {
                        boxes.add(segmentToBox(level.name(), wall, opening.startMillimeters(), opening.endMillimeters(), baseHeight, 0.0, opening.lowerHeightMillimeters(), "wall"));
                    }
                    double upperBase = baseHeight + opening.upperHeightMillimeters();
                    double upperHeight = Math.max(0.0, wall.height().toMillimeters() - opening.upperHeightMillimeters());
                    if (upperHeight > 0.0) {
                        boxes.add(segmentToBox(level.name(), wall, opening.startMillimeters(), opening.endMillimeters(), upperBase, 0.0, upperHeight, "wall"));
                    }
                }
                cursor = Math.max(cursor, opening.endMillimeters());
            }
            if (cursor < wall.axis().length().toMillimeters()) {
                boxes.add(segmentToBox(level.name(), wall, cursor, wall.axis().length().toMillimeters(), baseHeight, 0.0, wall.height().toMillimeters(), "wall"));
            }
        }
        return boxes;
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

    private RenderableBox segmentToBox(
            String levelName,
            Wall wall,
            double startMillimeters,
            double endMillimeters,
            double baseHeight,
            double verticalOffset,
            double heightMillimeters,
            String materialKey
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
                baseHeight + verticalOffset + heightMillimeters / 2.0,
                (start.yMillimeters() + end.yMillimeters()) / 2.0,
                segmentLength,
                heightMillimeters,
                wall.thickness().toMillimeters(),
                RotationAxis.Y,
                wall.axis().angle().degrees(),
                materialKey,
                1.0
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
