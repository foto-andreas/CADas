package de.schrell.cadas.application.terrain;

import de.schrell.cadas.application.layers.SurfaceLayerEffectService;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;
import de.schrell.cadas.domain.model.Wall;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Leitet die äußeren Geländeecken aus der Außenwand-Schleife des Gebäudes ab.
 */
public final class TerrainCornerService {

    private static final double EPSILON = 0.001;
    private static final double MATCH_TOLERANCE = 250.0;
    private static final double OUTER_MARGIN = 1_000.0;
    private static final double PROBE_OFFSET = 20.0;
    private static final double MAX_ORTHOGONAL_DEVIATION_RATIO = Math.tan(Math.toRadians(0.5));

    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();

    public Terrain synchronize(Level lowestLevel, Terrain existingTerrain) {
        List<Wall> outerWalls = outerWalls(lowestLevel);
        List<PlanPoint> outline = outlineFromOuterWalls(lowestLevel, outerWalls);
        if (outline.size() < 3) {
            return Terrain.empty();
        }
        return new Terrain(outline.stream()
                .map(point -> new TerrainVertex(point, existingElevation(existingTerrain, point)))
                .toList(), existingTerrain.displayWidth());
    }

    private List<Wall> outerWalls(Level level) {
        GridData grid = buildGrid(level);
        if (grid == null) {
            return List.of();
        }
        List<Wall> outerWalls = new ArrayList<>();
        for (Wall wall : level.walls()) {
            if (!isNearlyOrthogonal(wall)) {
                continue;
            }
            double directionX = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
            double directionY = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
            double length = Math.hypot(directionX, directionY);
            if (length <= EPSILON) {
                continue;
            }
            double normalX = -directionY / length;
            double normalY = directionX / length;
            double probeDistance = wall.thickness().toMillimeters() / 2.0
                    + surfaceLayerEffectService.wallExteriorThicknessMillimeters(level, wall)
                    + PROBE_OFFSET;
            PlanPoint midpoint = new PlanPoint(
                    (wall.axis().start().xMillimeters() + wall.axis().end().xMillimeters()) / 2.0,
                    (wall.axis().start().yMillimeters() + wall.axis().end().yMillimeters()) / 2.0
            );
            boolean leftExterior = isExteriorAir(new PlanPoint(
                    midpoint.xMillimeters() + normalX * probeDistance,
                    midpoint.yMillimeters() + normalY * probeDistance
            ), grid);
            boolean rightExterior = isExteriorAir(new PlanPoint(
                    midpoint.xMillimeters() - normalX * probeDistance,
                    midpoint.yMillimeters() - normalY * probeDistance
            ), grid);
            if (leftExterior ^ rightExterior) {
                outerWalls.add(wall);
            }
        }
        return outerWalls;
    }

    private GridData buildGrid(Level level) {
        List<WallRectangle> wallRectangles = wallRectangles(level);
        if (wallRectangles.size() != level.walls().size()) {
            return null;
        }
        List<Double> xCoordinates = new ArrayList<>();
        List<Double> yCoordinates = new ArrayList<>();
        for (WallRectangle rectangle : wallRectangles) {
            xCoordinates.add(rectangle.minX());
            xCoordinates.add(rectangle.maxX());
            yCoordinates.add(rectangle.minY());
            yCoordinates.add(rectangle.maxY());
        }
        xCoordinates.add(xCoordinates.stream().min(Double::compareTo).orElse(0.0) - OUTER_MARGIN);
        xCoordinates.add(xCoordinates.stream().max(Double::compareTo).orElse(0.0) + OUTER_MARGIN);
        yCoordinates.add(yCoordinates.stream().min(Double::compareTo).orElse(0.0) - OUTER_MARGIN);
        yCoordinates.add(yCoordinates.stream().max(Double::compareTo).orElse(0.0) + OUTER_MARGIN);
        xCoordinates = xCoordinates.stream().distinct().sorted().toList();
        yCoordinates = yCoordinates.stream().distinct().sorted().toList();
        if (xCoordinates.size() < 2 || yCoordinates.size() < 2) {
            return null;
        }
        int columns = xCoordinates.size() - 1;
        int rows = yCoordinates.size() - 1;
        boolean[][] occupied = new boolean[columns][rows];
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                double centerX = (xCoordinates.get(column) + xCoordinates.get(column + 1)) / 2.0;
                double centerY = (yCoordinates.get(row) + yCoordinates.get(row + 1)) / 2.0;
                occupied[column][row] = wallRectangles.stream().anyMatch(rectangle -> rectangle.contains(centerX, centerY));
            }
        }
        return new GridData(xCoordinates, yCoordinates, floodFillExteriorAir(occupied));
    }

    private boolean[][] floodFillExteriorAir(boolean[][] occupied) {
        int columns = occupied.length;
        int rows = occupied[0].length;
        boolean[][] exteriorAir = new boolean[columns][rows];
        ArrayDeque<CellIndex> queue = new ArrayDeque<>();
        for (int column = 0; column < columns; column++) {
            enqueueExteriorCell(column, 0, occupied, exteriorAir, queue);
            enqueueExteriorCell(column, rows - 1, occupied, exteriorAir, queue);
        }
        for (int row = 0; row < rows; row++) {
            enqueueExteriorCell(0, row, occupied, exteriorAir, queue);
            enqueueExteriorCell(columns - 1, row, occupied, exteriorAir, queue);
        }
        while (!queue.isEmpty()) {
            CellIndex current = queue.removeFirst();
            for (int[] offset : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                enqueueExteriorCell(current.column() + offset[0], current.row() + offset[1], occupied, exteriorAir, queue);
            }
        }
        return exteriorAir;
    }

    private void enqueueExteriorCell(
            int column,
            int row,
            boolean[][] occupied,
            boolean[][] exteriorAir,
            ArrayDeque<CellIndex> queue
    ) {
        if (column < 0 || column >= occupied.length || row < 0 || row >= occupied[0].length) {
            return;
        }
        if (occupied[column][row] || exteriorAir[column][row]) {
            return;
        }
        exteriorAir[column][row] = true;
        queue.addLast(new CellIndex(column, row));
    }

    private boolean isExteriorAir(PlanPoint point, GridData grid) {
        if (point.xMillimeters() < grid.xCoordinates().getFirst()
                || point.xMillimeters() > grid.xCoordinates().getLast()
                || point.yMillimeters() < grid.yCoordinates().getFirst()
                || point.yMillimeters() > grid.yCoordinates().getLast()) {
            return true;
        }
        int column = findCellIndex(grid.xCoordinates(), point.xMillimeters());
        int row = findCellIndex(grid.yCoordinates(), point.yMillimeters());
        return column < 0 || row < 0 || grid.exteriorAir()[column][row];
    }

    private int findCellIndex(List<Double> coordinates, double value) {
        for (int index = 0; index < coordinates.size() - 1; index++) {
            if (value >= coordinates.get(index) - EPSILON && value <= coordinates.get(index + 1) + EPSILON) {
                return index;
            }
        }
        return -1;
    }

    private List<WallRectangle> wallRectangles(Level level) {
        List<WallRectangle> rectangles = new ArrayList<>();
        for (Wall wall : level.walls()) {
            if (!isNearlyOrthogonal(wall)) {
                continue;
            }
            double halfThickness = wall.thickness().toMillimeters() / 2.0
                    + surfaceLayerEffectService.wallExteriorThicknessMillimeters(level, wall);
            double deltaX = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
            double deltaY = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
            if (isNearlyHorizontal(deltaX, deltaY)) {
                double axisY = (wall.axis().start().yMillimeters() + wall.axis().end().yMillimeters()) / 2.0;
                rectangles.add(new WallRectangle(
                        Math.min(wall.axis().start().xMillimeters(), wall.axis().end().xMillimeters()),
                        Math.max(wall.axis().start().xMillimeters(), wall.axis().end().xMillimeters()),
                        axisY - halfThickness,
                        axisY + halfThickness
                ));
            } else {
                double axisX = (wall.axis().start().xMillimeters() + wall.axis().end().xMillimeters()) / 2.0;
                rectangles.add(new WallRectangle(
                        axisX - halfThickness,
                        axisX + halfThickness,
                        Math.min(wall.axis().start().yMillimeters(), wall.axis().end().yMillimeters()),
                        Math.max(wall.axis().start().yMillimeters(), wall.axis().end().yMillimeters())
                ));
            }
        }
        return rectangles;
    }

    private List<PlanPoint> outlineFromOuterWalls(Level level, List<Wall> outerWalls) {
        if (outerWalls.size() < 3) {
            return List.of();
        }
        List<Wall> normalizedWalls = normalizeConnectedEndpoints(outerWalls);
        Map<PointKey, List<WallConnection>> graph = buildGraph(normalizedWalls);
        Set<UUID> visitedWalls = new HashSet<>();
        List<PlanPoint> bestOutline = List.of();
        double bestArea = -1.0;
        for (Wall wall : normalizedWalls) {
            if (visitedWalls.contains(wall.id())) {
                continue;
            }
            Set<UUID> componentWallIds = collectComponent(wall, graph, normalizedWalls);
            visitedWalls.addAll(componentWallIds);
            List<Wall> componentWalls = normalizedWalls.stream()
                    .filter(candidate -> componentWallIds.contains(candidate.id()))
                    .toList();
            if (componentWalls.size() < 3 || !formsSimpleLoop(componentWalls, graph)) {
                continue;
            }
            List<PlanPoint> outline = orderedLoop(componentWalls, graph)
                    .map(loop -> offsetLoopToOuterContour(level, loop))
                    .orElse(List.of());
            double area = Math.abs(signedArea(outline));
            if (outline.size() >= 3 && area > bestArea) {
                bestArea = area;
                bestOutline = outline;
            }
        }
        return bestOutline;
    }

    private List<Wall> normalizeConnectedEndpoints(List<Wall> walls) {
        List<PlanPoint> canonicalPoints = new ArrayList<>();
        return walls.stream()
                .map(wall -> wall.withAxis(new PlanSegment(
                        canonicalPoint(wall.axis().start(), canonicalPoints),
                        canonicalPoint(wall.axis().end(), canonicalPoints)
                )))
                .toList();
    }

    private PlanPoint canonicalPoint(PlanPoint point, List<PlanPoint> canonicalPoints) {
        return canonicalPoints.stream()
                .filter(candidate -> candidate.distanceTo(point).toMillimeters() <= 10.0)
                .min(Comparator.comparingDouble(candidate -> candidate.distanceTo(point).toMillimeters()))
                .orElseGet(() -> {
                    canonicalPoints.add(point);
                    return point;
                });
    }

    private Map<PointKey, List<WallConnection>> buildGraph(List<Wall> walls) {
        Map<PointKey, List<WallConnection>> graph = new LinkedHashMap<>();
        for (Wall wall : walls) {
            PointKey startKey = PointKey.of(wall.axis().start());
            PointKey endKey = PointKey.of(wall.axis().end());
            graph.computeIfAbsent(startKey, ignored -> new ArrayList<>())
                    .add(new WallConnection(wall.id(), startKey, endKey));
            graph.computeIfAbsent(endKey, ignored -> new ArrayList<>())
                    .add(new WallConnection(wall.id(), endKey, startKey));
        }
        return graph;
    }

    private Set<UUID> collectComponent(Wall startWall, Map<PointKey, List<WallConnection>> graph, List<Wall> walls) {
        Map<UUID, Wall> wallsById = new LinkedHashMap<>();
        walls.forEach(wall -> wallsById.put(wall.id(), wall));
        Set<UUID> component = new LinkedHashSet<>();
        ArrayDeque<UUID> queue = new ArrayDeque<>();
        queue.add(startWall.id());
        while (!queue.isEmpty()) {
            UUID wallId = queue.removeFirst();
            if (!component.add(wallId)) {
                continue;
            }
            Wall wall = wallsById.get(wallId);
            if (wall == null) {
                continue;
            }
            for (PointKey key : List.of(PointKey.of(wall.axis().start()), PointKey.of(wall.axis().end()))) {
                for (WallConnection connection : graph.getOrDefault(key, List.of())) {
                    if (!component.contains(connection.wallId())) {
                        queue.addLast(connection.wallId());
                    }
                }
            }
        }
        return component;
    }

    private boolean formsSimpleLoop(List<Wall> componentWalls, Map<PointKey, List<WallConnection>> graph) {
        for (Wall wall : componentWalls) {
            if (graph.getOrDefault(PointKey.of(wall.axis().start()), List.of()).size() != 2) {
                return false;
            }
            if (graph.getOrDefault(PointKey.of(wall.axis().end()), List.of()).size() != 2) {
                return false;
            }
        }
        return true;
    }

    private Optional<List<LoopEdge>> orderedLoop(List<Wall> componentWalls, Map<PointKey, List<WallConnection>> graph) {
        Map<UUID, Wall> wallsById = new LinkedHashMap<>();
        componentWalls.forEach(wall -> wallsById.put(wall.id(), wall));
        Wall startWall = componentWalls.getFirst();
        PointKey startKey = PointKey.of(startWall.axis().start());
        PointKey currentKey = PointKey.of(startWall.axis().end());
        UUID previousWallId = startWall.id();
        List<LoopEdge> orderedEdges = new ArrayList<>();
        orderedEdges.add(new LoopEdge(startWall, startWall.axis().start(), startWall.axis().end()));
        while (!currentKey.equals(startKey)) {
            WallConnection nextConnection = null;
            for (WallConnection connection : graph.getOrDefault(currentKey, List.of())) {
                if (!connection.wallId().equals(previousWallId)) {
                    nextConnection = connection;
                    break;
                }
            }
            if (nextConnection == null) {
                return Optional.empty();
            }
            Wall nextWall = wallsById.get(nextConnection.wallId());
            if (nextWall == null) {
                return Optional.empty();
            }
            orderedEdges.add(new LoopEdge(nextWall, currentKey.toPoint(), nextConnection.otherEnd().toPoint()));
            previousWallId = nextWall.id();
            currentKey = nextConnection.otherEnd();
            if (orderedEdges.size() > componentWalls.size() + 1) {
                return Optional.empty();
            }
        }
        return Optional.of(orderedEdges);
    }

    private List<PlanPoint> offsetLoopToOuterContour(Level level, List<LoopEdge> edges) {
        List<PlanPoint> axisPoints = new ArrayList<>();
        axisPoints.add(edges.getFirst().start());
        edges.forEach(edge -> axisPoints.add(edge.end()));
        if (samePoint(axisPoints.getFirst(), axisPoints.getLast())) {
            axisPoints.removeLast();
        }
        double orientation = signedArea(axisPoints);
        if (Math.abs(orientation) < EPSILON) {
            return List.of();
        }
        List<PlanPoint> outline = new ArrayList<>();
        for (int index = 0; index < edges.size(); index++) {
            OffsetLine previous = offsetLine(level, edges.get((index - 1 + edges.size()) % edges.size()), orientation);
            OffsetLine current = offsetLine(level, edges.get(index), orientation);
            outline.add(intersect(previous, current).orElse(current.start()));
        }
        return simplify(outline);
    }

    private OffsetLine offsetLine(Level level, LoopEdge edge, double orientation) {
        double deltaX = edge.end().xMillimeters() - edge.start().xMillimeters();
        double deltaY = edge.end().yMillimeters() - edge.start().yMillimeters();
        double length = Math.hypot(deltaX, deltaY);
        if (length <= EPSILON) {
            return new OffsetLine(edge.start(), edge.end());
        }
        double normalX = -deltaY / length;
        double normalY = deltaX / length;
        if (orientation > 0.0) {
            normalX = -normalX;
            normalY = -normalY;
        }
        double offset = edge.wall().thickness().toMillimeters() / 2.0
                + surfaceLayerEffectService.wallExteriorThicknessMillimeters(level, edge.wall());
        return new OffsetLine(
                new PlanPoint(edge.start().xMillimeters() + normalX * offset, edge.start().yMillimeters() + normalY * offset),
                new PlanPoint(edge.end().xMillimeters() + normalX * offset, edge.end().yMillimeters() + normalY * offset)
        );
    }

    private Optional<PlanPoint> intersect(OffsetLine first, OffsetLine second) {
        double x1 = first.start().xMillimeters();
        double y1 = first.start().yMillimeters();
        double x2 = first.end().xMillimeters();
        double y2 = first.end().yMillimeters();
        double x3 = second.start().xMillimeters();
        double y3 = second.start().yMillimeters();
        double x4 = second.end().xMillimeters();
        double y4 = second.end().yMillimeters();
        double denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denominator) < EPSILON) {
            return Optional.empty();
        }
        double determinantFirst = x1 * y2 - y1 * x2;
        double determinantSecond = x3 * y4 - y3 * x4;
        return Optional.of(new PlanPoint(
                (determinantFirst * (x3 - x4) - (x1 - x2) * determinantSecond) / denominator,
                (determinantFirst * (y3 - y4) - (y1 - y2) * determinantSecond) / denominator
        ));
    }

    private List<PlanPoint> simplify(List<PlanPoint> points) {
        if (points.size() < 3) {
            return points;
        }
        List<PlanPoint> simplified = new ArrayList<>(points);
        boolean changed;
        do {
            changed = false;
            for (int index = 0; index < simplified.size(); index++) {
                PlanPoint previous = simplified.get((index - 1 + simplified.size()) % simplified.size());
                PlanPoint current = simplified.get(index);
                PlanPoint next = simplified.get((index + 1) % simplified.size());
                if (isCollinear(previous, current, next)) {
                    simplified.remove(index);
                    changed = true;
                    break;
                }
            }
        } while (changed && simplified.size() >= 3);
        return simplified;
    }

    private double signedArea(List<PlanPoint> points) {
        if (points.size() < 3) {
            return 0.0;
        }
        double area = 0.0;
        for (int index = 0; index < points.size(); index++) {
            PlanPoint current = points.get(index);
            PlanPoint next = points.get((index + 1) % points.size());
            area += current.xMillimeters() * next.yMillimeters() - next.xMillimeters() * current.yMillimeters();
        }
        return area / 2.0;
    }

    private boolean isCollinear(PlanPoint previous, PlanPoint current, PlanPoint next) {
        return (Math.abs(previous.xMillimeters() - current.xMillimeters()) < EPSILON
                && Math.abs(current.xMillimeters() - next.xMillimeters()) < EPSILON)
                || (Math.abs(previous.yMillimeters() - current.yMillimeters()) < EPSILON
                && Math.abs(current.yMillimeters() - next.yMillimeters()) < EPSILON);
    }

    private boolean samePoint(PlanPoint first, PlanPoint second) {
        return Math.abs(first.xMillimeters() - second.xMillimeters()) < EPSILON
                && Math.abs(first.yMillimeters() - second.yMillimeters()) < EPSILON;
    }

    private boolean isNearlyOrthogonal(Wall wall) {
        double deltaX = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double deltaY = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        return isNearlyHorizontal(deltaX, deltaY) || isNearlyVertical(deltaX, deltaY);
    }

    private boolean isNearlyHorizontal(double deltaX, double deltaY) {
        return Math.abs(deltaY) < EPSILON
                || Math.abs(deltaX) > EPSILON && Math.abs(deltaY / deltaX) <= MAX_ORTHOGONAL_DEVIATION_RATIO;
    }

    private boolean isNearlyVertical(double deltaX, double deltaY) {
        return Math.abs(deltaX) < EPSILON
                || Math.abs(deltaY) > EPSILON && Math.abs(deltaX / deltaY) <= MAX_ORTHOGONAL_DEVIATION_RATIO;
    }

    private Length existingElevation(Terrain terrain, PlanPoint point) {
        return terrain.vertices().stream()
                .filter(vertex -> distance(vertex.position(), point) <= MATCH_TOLERANCE)
                .map(TerrainVertex::elevationAboveLowestFloor)
                .findFirst()
                .orElse(Length.zero());
    }

    private double distance(PlanPoint first, PlanPoint second) {
        return Math.hypot(first.xMillimeters() - second.xMillimeters(), first.yMillimeters() - second.yMillimeters());
    }

    private record GridData(List<Double> xCoordinates, List<Double> yCoordinates, boolean[][] exteriorAir) {
    }

    private record CellIndex(int column, int row) {
    }

    private record WallRectangle(double minX, double maxX, double minY, double maxY) {

        private boolean contains(double x, double y) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }
    }

    private record PointKey(long x, long y) {

        private static PointKey of(PlanPoint point) {
            return new PointKey(Math.round(point.xMillimeters() * 1000.0), Math.round(point.yMillimeters() * 1000.0));
        }

        private PlanPoint toPoint() {
            return new PlanPoint(x / 1000.0, y / 1000.0);
        }
    }

    private record WallConnection(UUID wallId, PointKey thisEnd, PointKey otherEnd) {
    }

    private record LoopEdge(Wall wall, PlanPoint start, PlanPoint end) {
    }

    private record OffsetLine(PlanPoint start, PlanPoint end) {
    }
}
