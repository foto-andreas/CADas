package de.schrell.cadas.application.roof;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;
import de.schrell.cadas.domain.model.WindowElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class RoofSlopeWallService {

    private static final double EPSILON = 0.001;
    private static final double CONNECTION_TOLERANCE = 10.0;

    public RoofSlopeResult apply(Level level, UUID selectedWallId, Length kneeWallHeight, Length slopeWidth) {
        Wall selectedWall = level.findWall(selectedWallId);
        Room room = adjacentRoom(level, selectedWall)
                .orElseThrow(() -> new IllegalArgumentException("An der gewählten Wand wurde kein angrenzender Raum gefunden."));
        if (slopeWidth.toMillimeters() <= 0.0) {
            throw new IllegalArgumentException("Die Breite unterhalb der Dachschräge muss größer als 0 sein.");
        }
        if (kneeWallHeight.toMillimeters() < 0.0 || kneeWallHeight.compareTo(room.roomHeight()) >= 0) {
            throw new IllegalArgumentException("Die Sockelhöhe muss unterhalb der Raumhöhe liegen.");
        }
        SlopedCeilingSide lowSide = lowSide(selectedWall, room);
        WallSplitResult splitResult = updateWalls(level, selectedWall, room, kneeWallHeight, slopeWidth);
        SlopedCeilingProfile slope = new SlopedCeilingProfile(lowSide, kneeWallHeight, slopeWidth);
        List<Room> rooms = level.rooms().stream()
                .map(candidate -> candidate.id().equals(room.id()) ? withSlope(candidate, slope) : candidate)
                .toList();
        return new RoofSlopeResult(
                splitResult.walls(), rooms, splitResult.doors(), splitResult.windows(),
                splitResult.surfaceLayerStacks(), room.id()
        );
    }

    private Optional<Room> adjacentRoom(Level level, Wall wall) {
        double maximumDistance = wall.thickness().toMillimeters() / 2.0 + 50.0;
        return level.rooms().stream()
                .map(room -> new RoomDistance(room, minimumParallelEdgeDistance(room, wall)))
                .filter(candidate -> candidate.distance() <= maximumDistance)
                .min(Comparator.comparingDouble(RoomDistance::distance))
                .map(RoomDistance::room);
    }

    private double minimumParallelEdgeDistance(Room room, Wall wall) {
        double wallDeltaX = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double wallDeltaY = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double wallLength = Math.max(EPSILON, Math.hypot(wallDeltaX, wallDeltaY));
        double minimumDistance = Double.POSITIVE_INFINITY;
        for (int index = 0; index < room.outline().size(); index++) {
            PlanPoint start = room.outline().get(index);
            PlanPoint end = room.outline().get((index + 1) % room.outline().size());
            double edgeDeltaX = end.xMillimeters() - start.xMillimeters();
            double edgeDeltaY = end.yMillimeters() - start.yMillimeters();
            double edgeLength = Math.max(EPSILON, Math.hypot(edgeDeltaX, edgeDeltaY));
            double cross = Math.abs(wallDeltaX * edgeDeltaY - wallDeltaY * edgeDeltaX) / (wallLength * edgeLength);
            if (cross > 0.01) {
                continue;
            }
            PlanPoint midpoint = new PlanPoint(
                    (start.xMillimeters() + end.xMillimeters()) / 2.0,
                    (start.yMillimeters() + end.yMillimeters()) / 2.0
            );
            minimumDistance = Math.min(minimumDistance, wall.axis().distanceTo(midpoint).toMillimeters());
        }
        return minimumDistance;
    }

    private WallSplitResult updateWalls(Level level, Wall selectedWall, Room room, Length kneeWallHeight, Length slopeWidth) {
        List<Wall> walls = new ArrayList<>();
        List<Door> doors = new ArrayList<>(level.doors());
        List<WindowElement> windows = new ArrayList<>(level.windows());
        Map<UUID, UUID> splitWallIds = new LinkedHashMap<>();
        for (Wall wall : level.walls()) {
            if (wall.id().equals(selectedWall.id())) {
                walls.add(new Wall(wall.id(), wall.axis(), wall.thickness(), kneeWallHeight));
                continue;
            }
            ConnectedEnd connectedEnd = connectedEnd(wall, selectedWall);
            if (connectedEnd == ConnectedEnd.NONE || !extendsTowardsRoom(wall, connectedEnd, selectedWall, room)) {
                walls.add(wall);
                continue;
            }
            splitSideWall(wall, connectedEnd, room.roomHeight(), kneeWallHeight, slopeWidth,
                    walls, doors, windows, splitWallIds);
        }
        return new WallSplitResult(
                List.copyOf(walls), List.copyOf(doors), List.copyOf(windows),
                splitSurfaceLayerStacks(level.surfaceLayerStacks(), splitWallIds)
        );
    }

    private void splitSideWall(
            Wall wall,
            ConnectedEnd connectedEnd,
            Length highHeight,
            Length kneeWallHeight,
            Length slopeWidth,
            List<Wall> walls,
            List<Door> doors,
            List<WindowElement> windows,
            Map<UUID, UUID> splitWallIds
    ) {
        double length = wall.axis().length().toMillimeters();
        double run = Math.min(length, slopeWidth.toMillimeters());
        if (run >= length - EPSILON) {
            walls.add(wall.withProfile(sideWallProfile(length, connectedEnd, highHeight, kneeWallHeight)));
            return;
        }
        double splitOffset = connectedEnd == ConnectedEnd.START ? run : length - run;
        verifyNoOpeningCrossesSplit(wall.id(), splitOffset, doors, windows);
        PlanPoint splitPoint = wall.axis().pointAt(Length.ofMillimeters(splitOffset));
        UUID secondWallId = UUID.randomUUID();
        Wall firstWall = new Wall(
                wall.id(),
                new PlanSegment(wall.axis().start(), splitPoint),
                wall.thickness(),
                connectedEnd == ConnectedEnd.START ? kneeWallHeight : highHeight
        );
        Wall secondWall = new Wall(
                secondWallId,
                new PlanSegment(splitPoint, wall.axis().end()),
                wall.thickness(),
                connectedEnd == ConnectedEnd.END ? kneeWallHeight : highHeight
        );
        if (connectedEnd == ConnectedEnd.START) {
            firstWall = firstWall.withProfile(sideWallProfile(splitOffset, connectedEnd, highHeight, kneeWallHeight));
        } else {
            secondWall = secondWall.withProfile(sideWallProfile(length - splitOffset, connectedEnd, highHeight, kneeWallHeight));
        }
        walls.add(firstWall);
        walls.add(secondWall);
        splitWallIds.put(wall.id(), secondWallId);
        rebindOpeningsAfterSplit(wall.id(), secondWallId, splitOffset, doors, windows);
    }

    private List<SurfaceLayerStack> splitSurfaceLayerStacks(
            List<SurfaceLayerStack> existingStacks,
            Map<UUID, UUID> splitWallIds
    ) {
        List<SurfaceLayerStack> stacks = new ArrayList<>(existingStacks);
        for (Map.Entry<UUID, UUID> split : splitWallIds.entrySet()) {
            String originalPrefix = split.getKey().toString();
            existingStacks.stream()
                    .filter(stack -> stack.surfaceType() == SurfaceType.WALL_INTERIOR
                            || stack.surfaceType() == SurfaceType.WALL_EXTERIOR)
                    .filter(stack -> stack.targetKey().equals(originalPrefix)
                            || stack.targetKey().startsWith(originalPrefix + "@"))
                    .map(stack -> duplicateStack(stack,
                            split.getValue() + stack.targetKey().substring(originalPrefix.length())))
                    .forEach(stacks::add);
        }
        return List.copyOf(stacks);
    }

    private SurfaceLayerStack duplicateStack(SurfaceLayerStack source, String targetKey) {
        SurfaceLayerStack duplicate = new SurfaceLayerStack(source.surfaceType(), targetKey);
        source.layers().forEach(duplicate::addLayer);
        return duplicate;
    }

    private List<WallProfilePoint> sideWallProfile(
            double length,
            ConnectedEnd connectedEnd,
            Length highHeight,
            Length kneeWallHeight
    ) {
        if (connectedEnd == ConnectedEnd.START) {
            return List.of(
                    new WallProfilePoint(Length.zero(), kneeWallHeight),
                    new WallProfilePoint(Length.ofMillimeters(length), highHeight)
            );
        }
        return List.of(
                new WallProfilePoint(Length.zero(), highHeight),
                new WallProfilePoint(Length.ofMillimeters(length), kneeWallHeight)
        );
    }

    private void verifyNoOpeningCrossesSplit(
            UUID wallId,
            double splitOffset,
            List<Door> doors,
            List<WindowElement> windows
    ) {
        boolean crossingDoor = doors.stream()
                .filter(door -> door.wallId().equals(wallId))
                .anyMatch(door -> crosses(door.offsetFromStart(), door.width(), splitOffset));
        boolean crossingWindow = windows.stream()
                .filter(window -> window.wallId().equals(wallId))
                .anyMatch(window -> crosses(window.offsetFromStart(), window.width(), splitOffset));
        if (crossingDoor || crossingWindow) {
            throw new IllegalArgumentException("Die Oberkante der Dachschräge darf keine Tür oder kein Fenster schneiden.");
        }
    }

    private boolean crosses(Length offset, Length width, double splitOffset) {
        return offset.toMillimeters() < splitOffset - EPSILON
                && offset.toMillimeters() + width.toMillimeters() > splitOffset + EPSILON;
    }

    private void rebindOpeningsAfterSplit(
            UUID firstWallId,
            UUID secondWallId,
            double splitOffset,
            List<Door> doors,
            List<WindowElement> windows
    ) {
        for (int index = 0; index < doors.size(); index++) {
            Door door = doors.get(index);
            if (door.wallId().equals(firstWallId) && door.offsetFromStart().toMillimeters() >= splitOffset - EPSILON) {
                doors.set(index, new Door(door.id(), secondWallId,
                        Length.ofMillimeters(door.offsetFromStart().toMillimeters() - splitOffset),
                        door.width(), door.height(), door.thresholdHeight()));
            }
        }
        for (int index = 0; index < windows.size(); index++) {
            WindowElement window = windows.get(index);
            if (window.wallId().equals(firstWallId) && window.offsetFromStart().toMillimeters() >= splitOffset - EPSILON) {
                windows.set(index, new WindowElement(window.id(), secondWallId,
                        Length.ofMillimeters(window.offsetFromStart().toMillimeters() - splitOffset),
                        window.width(), window.sillHeight(), window.windowHeight()));
            }
        }
    }

    private ConnectedEnd connectedEnd(Wall wall, Wall selectedWall) {
        if (near(wall.axis().start(), selectedWall.axis().start()) || near(wall.axis().start(), selectedWall.axis().end())) {
            return ConnectedEnd.START;
        }
        if (near(wall.axis().end(), selectedWall.axis().start()) || near(wall.axis().end(), selectedWall.axis().end())) {
            return ConnectedEnd.END;
        }
        return ConnectedEnd.NONE;
    }

    private boolean extendsTowardsRoom(Wall wall, ConnectedEnd connectedEnd, Wall selectedWall, Room room) {
        PlanPoint connected = connectedEnd == ConnectedEnd.START ? wall.axis().start() : wall.axis().end();
        PlanPoint other = connectedEnd == ConnectedEnd.START ? wall.axis().end() : wall.axis().start();
        PlanPoint wallMidpoint = new PlanPoint(
                (selectedWall.axis().start().xMillimeters() + selectedWall.axis().end().xMillimeters()) / 2.0,
                (selectedWall.axis().start().yMillimeters() + selectedWall.axis().end().yMillimeters()) / 2.0
        );
        double roomDirectionX = room.centerPoint().xMillimeters() - wallMidpoint.xMillimeters();
        double roomDirectionY = room.centerPoint().yMillimeters() - wallMidpoint.yMillimeters();
        double candidateDirectionX = other.xMillimeters() - connected.xMillimeters();
        double candidateDirectionY = other.yMillimeters() - connected.yMillimeters();
        return roomDirectionX * candidateDirectionX + roomDirectionY * candidateDirectionY > EPSILON;
    }

    private SlopedCeilingSide lowSide(Wall wall, Room room) {
        double deltaX = Math.abs(wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters());
        double deltaY = Math.abs(wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters());
        PlanPoint midpoint = new PlanPoint(
                (wall.axis().start().xMillimeters() + wall.axis().end().xMillimeters()) / 2.0,
                (wall.axis().start().yMillimeters() + wall.axis().end().yMillimeters()) / 2.0
        );
        if (deltaX >= deltaY) {
            return room.centerPoint().yMillimeters() >= midpoint.yMillimeters()
                    ? SlopedCeilingSide.NORTH
                    : SlopedCeilingSide.SOUTH;
        }
        return room.centerPoint().xMillimeters() <= midpoint.xMillimeters()
                ? SlopedCeilingSide.EAST
                : SlopedCeilingSide.WEST;
    }

    private Room withSlope(Room room, SlopedCeilingProfile slope) {
        List<SlopedCeilingProfile> slopes = new ArrayList<>(room.slopedCeilingProfiles().stream()
                .filter(existing -> existing.lowSide() != slope.lowSide())
                .toList());
        slopes.add(slope);
        return room.withSlopedCeilingProfiles(slopes);
    }

    private boolean near(PlanPoint first, PlanPoint second) {
        return first.distanceTo(second).toMillimeters() <= CONNECTION_TOLERANCE;
    }

    private enum ConnectedEnd {
        START,
        END,
        NONE
    }

    private record RoomDistance(Room room, double distance) {
    }

    private record WallSplitResult(
            List<Wall> walls,
            List<Door> doors,
            List<WindowElement> windows,
            List<SurfaceLayerStack> surfaceLayerStacks
    ) {
    }

    public record RoofSlopeResult(
            List<Wall> walls,
            List<Room> rooms,
            List<Door> doors,
            List<WindowElement> windows,
            List<SurfaceLayerStack> surfaceLayerStacks,
            UUID roomId
    ) {
    }
}
