package de.andreas.cadas.application.room;

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
    private static final double OUTER_MARGIN = 1_000.0;

    public List<Room> synchronize(Level level, RoomDefaults defaults) {
        List<WallRectangle> wallRectangles = wallRectangles(level.walls());
        if (level.walls().isEmpty()) {
            return List.of();
        }
        if (wallRectangles.size() == level.walls().size()) {
            List<DetectedRoom> detectedRooms = detectRooms(wallRectangles);
            if (!detectedRooms.isEmpty()) {
                return matchWithExistingRooms(level.rooms(), detectedRooms, defaults);
            }
        }
        List<DetectedRoom> detectedRooms = detectRoomsFromWallLoops(level.walls());
        if (detectedRooms.isEmpty()) {
            return List.of();
        }
        return matchWithExistingRooms(level.rooms(), detectedRooms, defaults);
    }

    private List<WallRectangle> wallRectangles(List<Wall> walls) {
        List<WallRectangle> rectangles = new ArrayList<>();
        for (Wall wall : walls) {
            double deltaX = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
            double deltaY = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
            double halfThickness = wall.thickness().toMillimeters() / 2.0;
            if (Math.abs(deltaY) < EPSILON) {
                rectangles.add(new WallRectangle(
                        Math.min(wall.axis().start().xMillimeters(), wall.axis().end().xMillimeters()),
                        Math.max(wall.axis().start().xMillimeters(), wall.axis().end().xMillimeters()),
                        wall.axis().start().yMillimeters() - halfThickness,
                        wall.axis().start().yMillimeters() + halfThickness
                ));
            } else if (Math.abs(deltaX) < EPSILON) {
                rectangles.add(new WallRectangle(
                        wall.axis().start().xMillimeters() - halfThickness,
                        wall.axis().start().xMillimeters() + halfThickness,
                        Math.min(wall.axis().start().yMillimeters(), wall.axis().end().yMillimeters()),
                        Math.max(wall.axis().start().yMillimeters(), wall.axis().end().yMillimeters())
                ));
            }
        }
        return rectangles;
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
                    rooms.add(new DetectedRoom(outline));
                }
            }
        }
        return rooms;
    }

    private List<DetectedRoom> detectRoomsFromWallLoops(List<Wall> walls) {
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
                    .map(this::offsetLoopToInnerContour)
                    .filter(outline -> outline.size() >= 3)
                    .map(DetectedRoom::new)
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

    private List<PlanPoint> offsetLoopToInnerContour(List<LoopEdge> edges) {
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
            OffsetLine previousLine = offsetLine(previous, orientation);
            OffsetLine currentLine = offsetLine(current, orientation);
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

    private OffsetLine offsetLine(LoopEdge edge, double orientation) {
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
        double offset = edge.wall().thickness().toMillimeters() / 2.0;
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
        double intersectionX = (determinantFirst * (x3 - x4) - (x1 - x2) * determinantSecond) / denominator;
        double intersectionY = (determinantFirst * (y3 - y4) - (y1 - y2) * determinantSecond) / denominator;
        return Optional.of(new PlanPoint(intersectionX, intersectionY));
    }

    private List<Room> matchWithExistingRooms(List<Room> existingRooms, List<DetectedRoom> detectedRooms, RoomDefaults defaults) {
        List<Room> updatedRooms = new ArrayList<>();
        Set<UUID> matchedIds = new HashSet<>();
        int roomIndex = 1;
        for (DetectedRoom detectedRoom : detectedRooms) {
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
                        previous.roomHeight(),
                        previous.floorThickness(),
                        previous.ceilingThickness(),
                        previous.slopedCeiling()
                );
            } else {
                room = new Room(
                        UUID.randomUUID(),
                        defaults.generatedName(roomIndex++),
                        detectedRoom.outline(),
                        defaults.roomHeight(),
                        defaults.floorThickness(),
                        defaults.ceilingThickness(),
                        defaults.slopedCeiling()
                );
            }
            updatedRooms.add(room);
        }
        return updatedRooms;
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
    }

    private record DetectedRoom(List<PlanPoint> outline) {

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
