package de.andreas.cadas.application.room;

import de.andreas.cadas.application.layers.SurfaceLayerEffectService;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SlopedCeilingProfile;
import de.andreas.cadas.domain.model.Wall;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AutoRoomGenerationService {

    private static final double EPSILON = 0.001;
    private static final double MAX_ORTHOGONAL_DEVIATION_RATIO = Math.tan(Math.toRadians(0.5));
    private static final double OUTER_MARGIN = 1_000.0;
    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();

    public List<Room> synchronize(Level level, RoomDefaults defaults) {
        if (level.walls().isEmpty()) {
            return List.of();
        }
        List<DetectedRoom> loopRooms = detectRoomsFromWallLoops(level);
        if (!loopRooms.isEmpty()) {
            return matchWithExistingRooms(level, loopRooms, defaults);
        }
        List<WallRectangle> wallRectangles = wallRectangles(level);
        if (wallRectangles.size() == level.walls().size()) {
            List<DetectedRoom> detectedRooms = detectRooms(wallRectangles);
            if (!detectedRooms.isEmpty()) {
                return matchWithExistingRooms(level, detectedRooms, defaults);
            }
        }
        return List.of();
    }

    private List<WallRectangle> wallRectangles(Level level) {
        List<WallRectangle> rectangles = new ArrayList<>();
        for (Wall wall : level.walls()) {
            double deltaX = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
            double deltaY = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
            double halfThickness = wall.thickness().toMillimeters() / 2.0 + surfaceLayerEffectService.maximumWallInteriorThicknessMillimeters(level, wall);
            if (isNearlyHorizontal(deltaX, deltaY)) {
                double axisY = (wall.axis().start().yMillimeters() + wall.axis().end().yMillimeters()) / 2.0;
                rectangles.add(new WallRectangle(
                        Math.min(wall.axis().start().xMillimeters(), wall.axis().end().xMillimeters()),
                        Math.max(wall.axis().start().xMillimeters(), wall.axis().end().xMillimeters()),
                        axisY - halfThickness,
                        axisY + halfThickness
                ));
            } else if (isNearlyVertical(deltaX, deltaY)) {
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

    private boolean isNearlyHorizontal(double deltaX, double deltaY) {
        return Math.abs(deltaY) < EPSILON
                || Math.abs(deltaX) > EPSILON && Math.abs(deltaY / deltaX) <= MAX_ORTHOGONAL_DEVIATION_RATIO;
    }

    private boolean isNearlyVertical(double deltaX, double deltaY) {
        return Math.abs(deltaX) < EPSILON
                || Math.abs(deltaY) > EPSILON && Math.abs(deltaX / deltaY) <= MAX_ORTHOGONAL_DEVIATION_RATIO;
    }

    private List<DetectedRoom> detectRooms(List<WallRectangle> wallRectangles) {
        List<Double> xCoordinates = new ArrayList<>();
        List<Double> yCoordinates = new ArrayList<>();
        for (WallRectangle rectangle : wallRectangles) {
            xCoordinates.add(rectangle.minX());
            xCoordinates.add(rectangle.maxX());
            yCoordinates.add(rectangle.minY());
            yCoordinates.add(rectangle.maxY());
        }
        double minX = xCoordinates.stream().min(Double::compareTo).orElse(0.0) - OUTER_MARGIN;
        double maxX = xCoordinates.stream().max(Double::compareTo).orElse(0.0) + OUTER_MARGIN;
        double minY = yCoordinates.stream().min(Double::compareTo).orElse(0.0) - OUTER_MARGIN;
        double maxY = yCoordinates.stream().max(Double::compareTo).orElse(0.0) + OUTER_MARGIN;
        xCoordinates.add(minX);
        xCoordinates.add(maxX);
        yCoordinates.add(minY);
        yCoordinates.add(maxY);
        xCoordinates = xCoordinates.stream().distinct().sorted().toList();
        yCoordinates = yCoordinates.stream().distinct().sorted().toList();
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
        boolean[][] visited = new boolean[columns][rows];
        List<DetectedRoom> rooms = new ArrayList<>();
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                if (occupied[column][row] || visited[column][row]) {
                    continue;
                }
                Set<CellIndex> component = floodFill(column, row, occupied, visited);
                if (touchesBoundary(component, columns, rows)) {
                    continue;
                }
                List<PlanPoint> outline = buildOutline(component, xCoordinates, yCoordinates);
                if (outline.size() >= 3) {
                    rooms.add(new DetectedRoom(outline, List.of()));
                }
            }
        }
        return rooms;
    }

    private List<DetectedRoom> detectRoomsFromWallLoops(Level level) {
        List<Wall> walls = level.walls();
        Map<PointKey, List<WallConnection>> graph = buildGraph(walls);
        Set<UUID> visitedWalls = new HashSet<>();
        List<DetectedRoom> rooms = new ArrayList<>();
        for (Wall wall : walls) {
            if (visitedWalls.contains(wall.id())) {
                continue;
            }
            Set<UUID> componentWallIds = collectComponent(wall, graph, walls);
            visitedWalls.addAll(componentWallIds);
            List<Wall> componentWalls = walls.stream()
                    .filter(candidate -> componentWallIds.contains(candidate.id()))
                    .toList();
            if (componentWalls.size() < 3 || !formsSimpleLoop(componentWalls, graph)) {
                continue;
            }
            orderedLoop(componentWalls, graph)
                    .map(loop -> new DetectedRoom(offsetLoopToInnerContour(level, loop), List.of()))
                    .filter(detectedRoom -> detectedRoom.outline().size() >= 3)
                    .ifPresent(rooms::add);
        }
        return rooms;
    }

    private Set<CellIndex> floodFill(int startColumn, int startRow, boolean[][] occupied, boolean[][] visited) {
        int columns = occupied.length;
        int rows = occupied[0].length;
        Set<CellIndex> component = new LinkedHashSet<>();
        ArrayDeque<CellIndex> queue = new ArrayDeque<>();
        queue.add(new CellIndex(startColumn, startRow));
        visited[startColumn][startRow] = true;
        while (!queue.isEmpty()) {
            CellIndex current = queue.removeFirst();
            component.add(current);
            for (int[] offset : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nextColumn = current.column() + offset[0];
                int nextRow = current.row() + offset[1];
                if (nextColumn < 0 || nextColumn >= columns || nextRow < 0 || nextRow >= rows) {
                    continue;
                }
                if (occupied[nextColumn][nextRow] || visited[nextColumn][nextRow]) {
                    continue;
                }
                visited[nextColumn][nextRow] = true;
                queue.addLast(new CellIndex(nextColumn, nextRow));
            }
        }
        return component;
    }

    private boolean touchesBoundary(Set<CellIndex> component, int columns, int rows) {
        return component.stream().anyMatch(cell ->
                cell.column() == 0
                        || cell.row() == 0
                        || cell.column() == columns - 1
                        || cell.row() == rows - 1
        );
    }

    private List<PlanPoint> buildOutline(Set<CellIndex> component, List<Double> xCoordinates, List<Double> yCoordinates) {
        Set<CellIndex> componentLookup = new HashSet<>(component);
        Map<Vertex, Vertex> edges = new LinkedHashMap<>();
        for (CellIndex cell : component) {
            double minX = xCoordinates.get(cell.column());
            double maxX = xCoordinates.get(cell.column() + 1);
            double minY = yCoordinates.get(cell.row());
            double maxY = yCoordinates.get(cell.row() + 1);
            if (!componentLookup.contains(new CellIndex(cell.column(), cell.row() - 1))) {
                edges.put(new Vertex(minX, minY), new Vertex(maxX, minY));
            }
            if (!componentLookup.contains(new CellIndex(cell.column() + 1, cell.row()))) {
                edges.put(new Vertex(maxX, minY), new Vertex(maxX, maxY));
            }
            if (!componentLookup.contains(new CellIndex(cell.column(), cell.row() + 1))) {
                edges.put(new Vertex(maxX, maxY), new Vertex(minX, maxY));
            }
            if (!componentLookup.contains(new CellIndex(cell.column() - 1, cell.row()))) {
                edges.put(new Vertex(minX, maxY), new Vertex(minX, minY));
            }
        }
        if (edges.isEmpty()) {
            return List.of();
        }
        Vertex start = edges.keySet().iterator().next();
        List<PlanPoint> points = new ArrayList<>();
        points.add(start.toPoint());
        Vertex current = start;
        Set<Vertex> visited = new HashSet<>();
        while (edges.containsKey(current) && visited.add(current)) {
            Vertex next = edges.get(current);
            points.add(next.toPoint());
            current = next;
            if (current.equals(start)) {
                break;
            }
        }
        if (!points.isEmpty() && samePoint(points.getFirst(), points.getLast())) {
            points.removeLast();
        }
        return simplify(points);
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

    private boolean isCollinear(PlanPoint previous, PlanPoint current, PlanPoint next) {
        return (Math.abs(previous.xMillimeters() - current.xMillimeters()) < EPSILON
                && Math.abs(current.xMillimeters() - next.xMillimeters()) < EPSILON)
                || (Math.abs(previous.yMillimeters() - current.yMillimeters()) < EPSILON
                && Math.abs(current.yMillimeters() - next.yMillimeters()) < EPSILON);
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
            List<WallConnection> connections = graph.getOrDefault(currentKey, List.of());
            WallConnection nextConnection = null;
            for (WallConnection connection : connections) {
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
            PlanPoint from = currentKey.toPoint();
            PlanPoint to = nextConnection.otherEnd().toPoint();
            orderedEdges.add(new LoopEdge(nextWall, from, to));
            previousWallId = nextWall.id();
            currentKey = nextConnection.otherEnd();
            if (orderedEdges.size() > componentWalls.size() + 1) {
                return Optional.empty();
            }
        }
        return Optional.of(orderedEdges);
    }

    private List<PlanPoint> offsetLoopToInnerContour(Level level, List<LoopEdge> edges) {
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
            LoopEdge previous = edges.get((index - 1 + edges.size()) % edges.size());
            LoopEdge current = edges.get(index);
            OffsetLine previousLine = offsetLine(level, previous, orientation);
            OffsetLine currentLine = offsetLine(level, current, orientation);
            PlanPoint intersection = intersect(previousLine, currentLine)
                    .orElse(current.start());
            outline.add(intersection);
        }
        return simplify(outline);
    }

    private double signedArea(List<PlanPoint> points) {
        double area = 0.0;
        for (int index = 0; index < points.size(); index++) {
            PlanPoint current = points.get(index);
            PlanPoint next = points.get((index + 1) % points.size());
            area += current.xMillimeters() * next.yMillimeters() - next.xMillimeters() * current.yMillimeters();
        }
        return area / 2.0;
    }

    private OffsetLine offsetLine(Level level, LoopEdge edge, double orientation) {
        double dx = edge.end().xMillimeters() - edge.start().xMillimeters();
        double dy = edge.end().yMillimeters() - edge.start().yMillimeters();
        double length = Math.hypot(dx, dy);
        if (length < EPSILON) {
            return new OffsetLine(edge.start(), edge.end());
        }
        double normalX = -dy / length;
        double normalY = dx / length;
        if (orientation < 0.0) {
            normalX = -normalX;
            normalY = -normalY;
        }
        Room adjacentRoom = findRoomAtInnerWallSide(level, edge, normalX, normalY).orElse(null);
        double offset = edge.wall().thickness().toMillimeters() / 2.0
                + (adjacentRoom == null
                ? surfaceLayerEffectService.maximumWallInteriorThicknessMillimeters(level, edge.wall())
                : surfaceLayerEffectService.wallInteriorThicknessMillimeters(level, edge.wall(), adjacentRoom));
        return new OffsetLine(
                new PlanPoint(edge.start().xMillimeters() + normalX * offset, edge.start().yMillimeters() + normalY * offset),
                new PlanPoint(edge.end().xMillimeters() + normalX * offset, edge.end().yMillimeters() + normalY * offset)
        );
    }

    private Optional<Room> findRoomAtInnerWallSide(Level level, LoopEdge edge, double normalX, double normalY) {
        double probeDistance = edge.wall().thickness().toMillimeters() / 2.0 + 5.0;
        PlanPoint midpoint = new PlanPoint(
                (edge.start().xMillimeters() + edge.end().xMillimeters()) / 2.0,
                (edge.start().yMillimeters() + edge.end().yMillimeters()) / 2.0
        );
        PlanPoint probe = new PlanPoint(
                midpoint.xMillimeters() + normalX * probeDistance,
                midpoint.yMillimeters() + normalY * probeDistance
        );
        return level.rooms().stream()
                .filter(room -> containsPoint(room.outline(), probe))
                .findFirst();
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
        double intersectionX = (determinantFirst * (x3 - x4) - (x1 - x2) * determinantSecond) / denominator;
        double intersectionY = (determinantFirst * (y3 - y4) - (y1 - y2) * determinantSecond) / denominator;
        return Optional.of(new PlanPoint(intersectionX, intersectionY));
    }

    private List<Room> matchWithExistingRooms(Level level, List<DetectedRoom> detectedRooms, RoomDefaults defaults) {
        List<Room> existingRooms = level.rooms();
        List<Room> updatedRooms = new ArrayList<>();
        Set<UUID> matchedIds = new HashSet<>();
        int roomIndex = 1;
        for (DetectedRoom detectedRoom : detectedRooms) {
            List<Length> vertexHeights = detectedRoom.vertexHeights().isEmpty()
                    ? deriveVertexHeights(level, detectedRoom.outline(), defaults.roomHeight())
                    : detectedRoom.vertexHeights();
            Length derivedRoomHeight = Length.ofMillimeters(vertexHeights.stream().mapToDouble(Length::toMillimeters).max().orElse(defaults.roomHeight().toMillimeters()));
            Optional<Room> matchedRoom = existingRooms.stream()
                    .filter(room -> !matchedIds.contains(room.id()))
                    .filter(room -> containsPoint(detectedRoom.outline(), room.centerPoint()) || containsPoint(room.outline(), detectedRoom.centerPoint()))
                    .min(Comparator.comparingDouble(room -> room.centerPoint().distanceTo(detectedRoom.centerPoint()).toMillimeters()));
            Room room;
            if (matchedRoom.isPresent()) {
                Room previous = matchedRoom.orElseThrow();
                matchedIds.add(previous.id());
                room = new Room(
                        previous.id(),
                        previous.name(),
                        detectedRoom.outline(),
                        derivedRoomHeight,
                        previous.floorThickness(),
                        previous.ceilingThickness(),
                        hasVariableHeights(vertexHeights) ? null : previous.slopedCeiling(),
                        vertexHeights
                );
            } else {
                room = new Room(
                        UUID.randomUUID(),
                        defaults.generatedName(roomIndex++),
                        detectedRoom.outline(),
                        derivedRoomHeight,
                        defaults.floorThickness(),
                        defaults.ceilingThickness(),
                        hasVariableHeights(vertexHeights) ? null : defaults.slopedCeiling(),
                        vertexHeights
                );
            }
            updatedRooms.add(room);
        }
        return updatedRooms;
    }

    private boolean hasVariableHeights(List<Length> vertexHeights) {
        if (vertexHeights.isEmpty()) {
            return false;
        }
        double reference = vertexHeights.getFirst().toMillimeters();
        return vertexHeights.stream().anyMatch(length -> Math.abs(length.toMillimeters() - reference) > EPSILON);
    }

    private List<Length> deriveVertexHeights(Level level, List<PlanPoint> outline, Length defaultHeight) {
        if (outline.isEmpty()) {
            return List.of();
        }
        List<Length> heights = new ArrayList<>();
        for (PlanPoint point : outline) {
            double sum = 0.0;
            int count = 0;
            for (Wall wall : level.walls()) {
                double distance = wall.axis().distanceTo(point).toMillimeters();
                double tolerance = wall.thickness().toMillimeters() / 2.0
                        + surfaceLayerEffectService.maximumWallInteriorThicknessMillimeters(level, wall)
                        + 40.0;
                if (distance > tolerance) {
                    continue;
                }
                double offset = wall.axis().projectedLength(point).toMillimeters();
                sum += wall.heightAt(offset);
                count++;
            }
            if (count == 0) {
                heights.add(defaultHeight == null ? Length.zero() : defaultHeight);
            } else {
                heights.add(Length.ofMillimeters(sum / count));
            }
        }
        return List.copyOf(heights);
    }

    private boolean containsPoint(List<PlanPoint> outline, PlanPoint point) {
        boolean inside = false;
        int lastIndex = outline.size() - 1;
        for (int currentIndex = 0; currentIndex < outline.size(); currentIndex++) {
            PlanPoint current = outline.get(currentIndex);
            PlanPoint previous = outline.get(lastIndex);
            boolean intersects = (current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters())
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / (previous.yMillimeters() - current.yMillimeters())
                    + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            lastIndex = currentIndex;
        }
        return inside;
    }

    private boolean samePoint(PlanPoint first, PlanPoint second) {
        return Math.abs(first.xMillimeters() - second.xMillimeters()) < EPSILON
                && Math.abs(first.yMillimeters() - second.yMillimeters()) < EPSILON;
    }

    private record CellIndex(int column, int row) {
    }

    private record OffsetLine(PlanPoint start, PlanPoint end) {
    }

    private record Vertex(double x, double y) {

        PlanPoint toPoint() {
            return new PlanPoint(x, y);
        }
    }

    private record WallRectangle(double minX, double maxX, double minY, double maxY) {

        boolean contains(double x, double y) {
            return x > minX + EPSILON && x < maxX - EPSILON && y > minY + EPSILON && y < maxY - EPSILON;
        }
    }

    private record PointKey(long xMicrometers, long yMicrometers) {

        static PointKey of(PlanPoint point) {
            return new PointKey(Math.round(point.xMillimeters() * 1_000.0), Math.round(point.yMillimeters() * 1_000.0));
        }

        PlanPoint toPoint() {
            return new PlanPoint(xMicrometers / 1_000.0, yMicrometers / 1_000.0);
        }
    }

    private record WallConnection(UUID wallId, PointKey point, PointKey otherEnd) {
    }

    private record LoopEdge(Wall wall, PlanPoint start, PlanPoint end) {

        double startHeightMillimeters() {
            if (samePlanPoint(start, wall.axis().start())) {
                return wall.heightAtStart();
            }
            return wall.heightAtEnd();
        }

        private boolean samePlanPoint(PlanPoint first, PlanPoint second) {
            return Math.abs(first.xMillimeters() - second.xMillimeters()) < EPSILON
                    && Math.abs(first.yMillimeters() - second.yMillimeters()) < EPSILON;
        }
    }

    private record DetectedRoom(List<PlanPoint> outline, List<Length> vertexHeights) {

        PlanPoint centerPoint() {
            double sumX = 0.0;
            double sumY = 0.0;
            for (PlanPoint point : outline) {
                sumX += point.xMillimeters();
                sumY += point.yMillimeters();
            }
            return new PlanPoint(sumX / outline.size(), sumY / outline.size());
        }
    }

    public record RoomDefaults(
            String name,
            Length roomHeight,
            Length floorThickness,
            Length ceilingThickness,
            SlopedCeilingProfile slopedCeiling
    ) {

        public String generatedName(int roomIndex) {
            if (name == null || name.isBlank() || "Raum".equalsIgnoreCase(name.trim())) {
                return "Raum " + roomIndex;
            }
            return roomIndex == 1 ? name.trim() : name.trim() + " " + roomIndex;
        }
    }
}
