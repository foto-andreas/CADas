package de.schrell.cadas.application.roof;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        List<Wall> walls = level.walls().stream()
                .map(wall -> updateWall(wall, selectedWall, room, kneeWallHeight, slopeWidth))
                .toList();
        SlopedCeilingProfile slope = new SlopedCeilingProfile(lowSide, kneeWallHeight, slopeWidth);
        List<Room> rooms = level.rooms().stream()
                .map(candidate -> candidate.id().equals(room.id()) ? withSlope(candidate, slope) : candidate)
                .toList();
        return new RoofSlopeResult(walls, rooms, room.id());
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

    private Wall updateWall(Wall wall, Wall selectedWall, Room room, Length kneeWallHeight, Length slopeWidth) {
        if (wall.id().equals(selectedWall.id())) {
            return new Wall(wall.id(), wall.axis(), wall.thickness(), kneeWallHeight);
        }
        ConnectedEnd connectedEnd = connectedEnd(wall, selectedWall);
        if (connectedEnd == ConnectedEnd.NONE || !extendsTowardsRoom(wall, connectedEnd, selectedWall, room)) {
            return wall;
        }
        double length = wall.axis().length().toMillimeters();
        double run = Math.min(length, slopeWidth.toMillimeters());
        Length high = room.roomHeight();
        List<WallProfilePoint> profile = new ArrayList<>();
        if (connectedEnd == ConnectedEnd.START) {
            profile.add(new WallProfilePoint(Length.zero(), kneeWallHeight));
            if (run < length - EPSILON) {
                profile.add(new WallProfilePoint(Length.ofMillimeters(run), high));
            }
            profile.add(new WallProfilePoint(Length.ofMillimeters(length), high));
        } else {
            profile.add(new WallProfilePoint(Length.zero(), high));
            if (run < length - EPSILON) {
                profile.add(new WallProfilePoint(Length.ofMillimeters(length - run), high));
            }
            profile.add(new WallProfilePoint(Length.ofMillimeters(length), kneeWallHeight));
        }
        return wall.withProfile(profile);
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
        return new Room(
                room.id(), room.name(), room.outline(), room.roomHeight(), room.floorThickness(),
                room.ceilingThickness(), slope, null
        );
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

    public record RoofSlopeResult(List<Wall> walls, List<Room> rooms, UUID roomId) {
    }
}
