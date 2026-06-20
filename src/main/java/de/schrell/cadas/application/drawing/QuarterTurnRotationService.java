package de.schrell.cadas.application.drawing;

import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;

import java.util.List;
import java.util.Set;

public final class QuarterTurnRotationService {

    public RotationResult rotate(Level level, Set<SelectionKey> selections, boolean clockwise) {
        Set<String> selectedWalls = selectedIds(selections, RenderableKind.WALL);
        Set<String> selectedRooms = selectedIds(selections, RenderableKind.ROOM_VOLUME, RenderableKind.ROOM_FLOOR, RenderableKind.ROOM_CEILING);
        Set<String> selectedStairs = selectedIds(selections, RenderableKind.STAIR);
        Set<String> selectedRoomObjects = selectedIds(selections, RenderableKind.ROOM_OBJECT);

        List<Wall> rotatedWalls = level.walls().stream()
                .map(wall -> selectedWalls.contains(wall.id().toString()) ? rotateWall(wall, clockwise) : wall)
                .toList();
        List<Room> rotatedRooms = level.rooms().stream()
                .map(room -> selectedRooms.contains(room.id().toString()) ? rotateRoom(room, clockwise) : room)
                .toList();
        List<Staircase> rotatedStaircases = level.staircases().stream()
                .map(staircase -> selectedStairs.contains(staircase.id().toString())
                        ? (clockwise ? staircase.rotateClockwise() : staircase.rotateCounterClockwise())
                        : staircase)
                .toList();
        List<RoomObject> rotatedRoomObjects = level.roomObjects().stream()
                .map(roomObject -> selectedRoomObjects.contains(roomObject.id().toString())
                        ? roomObject.withRotationDegrees(roomObject.rotationDegrees() + (clockwise ? 90.0 : -90.0))
                        : roomObject)
                .toList();
        boolean changed = !selectedWalls.isEmpty() || !selectedRooms.isEmpty() || !selectedStairs.isEmpty() || !selectedRoomObjects.isEmpty();
        return new RotationResult(rotatedWalls, rotatedRooms, rotatedStaircases, rotatedRoomObjects, changed);
    }

    private Set<String> selectedIds(Set<SelectionKey> selections, RenderableKind... kinds) {
        Set<RenderableKind> allowedKinds = Set.of(kinds);
        return selections.stream()
                .filter(selection -> allowedKinds.contains(selection.kind()))
                .map(SelectionKey::elementId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Wall rotateWall(Wall wall, boolean clockwise) {
        PlanPoint center = midpoint(wall.axis().start(), wall.axis().end());
        return new Wall(
                wall.id(),
                new PlanSegment(
                        rotatePoint(wall.axis().start(), center, clockwise),
                        rotatePoint(wall.axis().end(), center, clockwise)
                ),
                wall.thickness(),
                wall.height(),
                wall.startHeight(),
                wall.endHeight()
        );
    }

    private Room rotateRoom(Room room, boolean clockwise) {
        PlanPoint center = room.centerPoint();
        return new Room(
                room.id(),
                room.name(),
                room.outline().stream()
                        .map(point -> rotatePoint(point, center, clockwise))
                        .toList(),
                room.roomHeight(),
                room.floorThickness(),
                room.ceilingThickness(),
                room.slopedCeilingProfile()
                        .map(profile -> clockwise ? profile.rotateClockwise() : profile.rotateCounterClockwise())
                        .orElse(null),
                room.ceilingVertexHeights()
        );
    }

    private PlanPoint midpoint(PlanPoint first, PlanPoint second) {
        return new PlanPoint(
                (first.xMillimeters() + second.xMillimeters()) / 2.0,
                (first.yMillimeters() + second.yMillimeters()) / 2.0
        );
    }

    private PlanPoint rotatePoint(PlanPoint point, PlanPoint center, boolean clockwise) {
        double dx = point.xMillimeters() - center.xMillimeters();
        double dy = point.yMillimeters() - center.yMillimeters();
        double rotatedX = clockwise ? dy : -dy;
        double rotatedY = clockwise ? -dx : dx;
        return new PlanPoint(center.xMillimeters() + rotatedX, center.yMillimeters() + rotatedY);
    }

    public record RotationResult(List<Wall> walls, List<Room> rooms, List<Staircase> staircases, List<RoomObject> roomObjects, boolean changed) {
    }
}
