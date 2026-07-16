package de.schrell.cadas.application.terrain;

import de.schrell.cadas.application.layers.SurfaceLayerEffectService;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;
import de.schrell.cadas.domain.model.Wall;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Leitet die äußeren Geländeecken aus der Außenwand-Schleife des Gebäudes ab.
 */
public final class TerrainCornerService {

    private static final double EPSILON = 0.001;
    private static final double MATCH_TOLERANCE = 250.0;
    private static final double OUTER_MARGIN = 1_000.0;
    private static final double MAX_ORTHOGONAL_DEVIATION_RATIO = Math.tan(Math.toRadians(0.5));

    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();

    public Terrain synchronize(Level lowestLevel, Terrain existingTerrain) {
        return synchronize(List.of(lowestLevel), existingTerrain);
    }

    public Terrain synchronize(List<Level> levels, Terrain existingTerrain) {
        GridData grid = buildGrid(levels);
        List<PlanPoint> outline = grid == null ? List.of() : outlineFromGrid(grid);
        if (outline.size() < 3) {
            return Terrain.empty();
        }
        return new Terrain(outline.stream()
                .map(point -> new TerrainVertex(point, existingElevation(existingTerrain, point)))
                .toList(), existingTerrain.displayWidth());
    }

    private GridData buildGrid(List<Level> levels) {
        List<WallRectangle> wallRectangles = new ArrayList<>();
        int wallCount = 0;
        for (Level level : levels) {
            wallCount += level.walls().size();
            wallRectangles.addAll(wallRectangles(level));
        }
        if (wallRectangles.size() != wallCount) {
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
        return new GridData(xCoordinates, yCoordinates, occupied, floodFillExteriorAir(occupied));
    }

    private List<PlanPoint> outlineFromGrid(GridData grid) {
        Map<PointKey, List<PointKey>> graph = new LinkedHashMap<>();
        for (int column = 0; column < grid.occupied().length; column++) {
            for (int row = 0; row < grid.occupied()[0].length; row++) {
                if (!grid.occupied()[column][row]) {
                    continue;
                }
                addBoundaryEdgeIfExterior(graph, grid, column, row, Side.BOTTOM);
                addBoundaryEdgeIfExterior(graph, grid, column, row, Side.RIGHT);
                addBoundaryEdgeIfExterior(graph, grid, column, row, Side.TOP);
                addBoundaryEdgeIfExterior(graph, grid, column, row, Side.LEFT);
            }
        }
        if (graph.isEmpty()) {
            return List.of();
        }
        return extractLargestLoop(graph);
    }

    private void addBoundaryEdgeIfExterior(
            Map<PointKey, List<PointKey>> graph,
            GridData grid,
            int column,
            int row,
            Side side
    ) {
        int neighborColumn = column + side.columnOffset();
        int neighborRow = row + side.rowOffset();
        if (neighborColumn >= 0
                && neighborColumn < grid.occupied().length
                && neighborRow >= 0
                && neighborRow < grid.occupied()[0].length
                && !grid.exteriorAir()[neighborColumn][neighborRow]) {
            return;
        }
        BoundaryEdge edge = boundaryEdge(grid, column, row, side);
        graph.computeIfAbsent(edge.start(), ignored -> new ArrayList<>()).add(edge.end());
        graph.computeIfAbsent(edge.end(), ignored -> new ArrayList<>()).add(edge.start());
    }

    private BoundaryEdge boundaryEdge(GridData grid, int column, int row, Side side) {
        double minX = grid.xCoordinates().get(column);
        double maxX = grid.xCoordinates().get(column + 1);
        double minY = grid.yCoordinates().get(row);
        double maxY = grid.yCoordinates().get(row + 1);
        return switch (side) {
            case BOTTOM -> new BoundaryEdge(PointKey.of(new PlanPoint(minX, minY)), PointKey.of(new PlanPoint(maxX, minY)));
            case RIGHT -> new BoundaryEdge(PointKey.of(new PlanPoint(maxX, minY)), PointKey.of(new PlanPoint(maxX, maxY)));
            case TOP -> new BoundaryEdge(PointKey.of(new PlanPoint(maxX, maxY)), PointKey.of(new PlanPoint(minX, maxY)));
            case LEFT -> new BoundaryEdge(PointKey.of(new PlanPoint(minX, maxY)), PointKey.of(new PlanPoint(minX, minY)));
        };
    }

    private List<PlanPoint> extractLargestLoop(Map<PointKey, List<PointKey>> graph) {
        Set<EdgeKey> visitedEdges = new LinkedHashSet<>();
        List<PlanPoint> bestLoop = List.of();
        double bestArea = -1.0;
        for (Map.Entry<PointKey, List<PointKey>> entry : graph.entrySet()) {
            for (PointKey neighbor : entry.getValue()) {
                EdgeKey startEdge = EdgeKey.of(entry.getKey(), neighbor);
                if (visitedEdges.contains(startEdge)) {
                    continue;
                }
                List<PlanPoint> loop = traceLoop(graph, entry.getKey(), neighbor, visitedEdges);
                double area = Math.abs(signedArea(loop));
                if (loop.size() >= 3 && area > bestArea) {
                    bestArea = area;
                    bestLoop = simplify(loop);
                }
            }
        }
        return bestLoop;
    }

    private List<PlanPoint> traceLoop(
            Map<PointKey, List<PointKey>> graph,
            PointKey start,
            PointKey firstNeighbor,
            Set<EdgeKey> visitedEdges
    ) {
        List<PlanPoint> points = new ArrayList<>();
        PointKey previous = start;
        PointKey current = firstNeighbor;
        points.add(start.toPoint());
        visitedEdges.add(EdgeKey.of(start, current));
        while (true) {
            points.add(current.toPoint());
            List<PointKey> neighbors = graph.getOrDefault(current, List.of());
            PointKey next = null;
            for (PointKey candidate : neighbors) {
                if (!candidate.equals(previous)) {
                    next = candidate;
                    break;
                }
            }
            if (next == null) {
                return List.of();
            }
            if (next.equals(start)) {
                return points;
            }
            EdgeKey nextEdge = EdgeKey.of(current, next);
            if (visitedEdges.contains(nextEdge)) {
                return List.of();
            }
            visitedEdges.add(nextEdge);
            previous = current;
            current = next;
            if (points.size() > graph.size() + 1) {
                return List.of();
            }
        }
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
                        Math.min(wall.axis().start().xMillimeters(), wall.axis().end().xMillimeters()) - halfThickness,
                        Math.max(wall.axis().start().xMillimeters(), wall.axis().end().xMillimeters()) + halfThickness,
                        axisY - halfThickness,
                        axisY + halfThickness
                ));
            } else {
                double axisX = (wall.axis().start().xMillimeters() + wall.axis().end().xMillimeters()) / 2.0;
                rectangles.add(new WallRectangle(
                        axisX - halfThickness,
                        axisX + halfThickness,
                        Math.min(wall.axis().start().yMillimeters(), wall.axis().end().yMillimeters()) - halfThickness,
                        Math.max(wall.axis().start().yMillimeters(), wall.axis().end().yMillimeters()) + halfThickness
                ));
            }
        }
        return rectangles;
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

    private record GridData(List<Double> xCoordinates, List<Double> yCoordinates, boolean[][] occupied, boolean[][] exteriorAir) {
    }

    private record CellIndex(int column, int row) {
    }

    private record WallRectangle(double minX, double maxX, double minY, double maxY) {

        private boolean contains(double x, double y) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }
    }

    private record PointKey(long x, long y) implements Comparable<PointKey> {

        private static PointKey of(PlanPoint point) {
            return new PointKey(Math.round(point.xMillimeters() * 1000.0), Math.round(point.yMillimeters() * 1000.0));
        }

        private PlanPoint toPoint() {
            return new PlanPoint(x / 1000.0, y / 1000.0);
        }

        @Override
        public int compareTo(PointKey other) {
            int xComparison = Long.compare(x, other.x);
            if (xComparison != 0) {
                return xComparison;
            }
            return Long.compare(y, other.y);
        }
    }

    private record BoundaryEdge(PointKey start, PointKey end) {
    }

    private record EdgeKey(PointKey first, PointKey second) {

        private static EdgeKey of(PointKey first, PointKey second) {
            return first.compareTo(second) <= 0
                    ? new EdgeKey(first, second)
                    : new EdgeKey(second, first);
        }
    }

    private enum Side {
        BOTTOM(0, -1),
        RIGHT(1, 0),
        TOP(0, 1),
        LEFT(-1, 0);

        private final int columnOffset;
        private final int rowOffset;

        Side(int columnOffset, int rowOffset) {
            this.columnOffset = columnOffset;
            this.rowOffset = rowOffset;
        }

        private int columnOffset() {
            return columnOffset;
        }

        private int rowOffset() {
            return rowOffset;
        }
    }
}
