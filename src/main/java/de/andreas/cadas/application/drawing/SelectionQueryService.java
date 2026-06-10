package de.andreas.cadas.application.drawing;

import de.andreas.cadas.application.view.RenderableKind;
import de.andreas.cadas.application.view.SelectionKey;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import java.util.Comparator;
import java.util.Optional;

public final class SelectionQueryService {

    public Optional<SelectionKey> findSelection(Level level, PlanPoint point, Length tolerance) {
        return findDoorSelection(level, point, tolerance)
                .or(() -> findWindowSelection(level, point, tolerance))
                .or(() -> findStairSelection(level, point))
                .or(() -> findWallSelection(level, point, tolerance))
                .or(() -> findRoomSelection(level, point));
    }

    private Optional<SelectionKey> findDoorSelection(Level level, PlanPoint point, Length tolerance) {
        for (Door door : level.doors()) {
            Wall wall = level.findWall(door.wallId());
            PlanSegment segment = new PlanSegment(
                    wall.axis().pointAt(door.offsetFromStart()),
                    wall.axis().pointAt(door.offsetFromStart().add(door.width()))
            );
            if (segment.distanceTo(point).compareTo(tolerance) <= 0) {
                return Optional.of(new SelectionKey(RenderableKind.DOOR, level.name(), door.id().toString()));
            }
        }
        return Optional.empty();
    }

    private Optional<SelectionKey> findWindowSelection(Level level, PlanPoint point, Length tolerance) {
        for (WindowElement window : level.windows()) {
            Wall wall = level.findWall(window.wallId());
            PlanSegment segment = new PlanSegment(
                    wall.axis().pointAt(window.offsetFromStart()),
                    wall.axis().pointAt(window.offsetFromStart().add(window.width()))
            );
            if (segment.distanceTo(point).compareTo(tolerance) <= 0) {
                return Optional.of(new SelectionKey(RenderableKind.WINDOW, level.name(), window.id().toString()));
            }
        }
        return Optional.empty();
    }

    private Optional<SelectionKey> findRoomSelection(Level level, PlanPoint point) {
        return level.rooms().stream()
                .filter(room -> containsPoint(room, point))
                .findFirst()
                .map(room -> new SelectionKey(RenderableKind.ROOM_VOLUME, level.name(), room.id().toString()));
    }

    private Optional<SelectionKey> findStairSelection(Level level, PlanPoint point) {
        return level.staircases().stream()
                .filter(staircase -> point.xMillimeters() >= staircase.minX()
                        && point.xMillimeters() <= staircase.maxX()
                        && point.yMillimeters() >= staircase.minY()
                        && point.yMillimeters() <= staircase.maxY())
                .findFirst()
                .map(staircase -> new SelectionKey(RenderableKind.STAIR, level.name(), staircase.id().toString()));
    }

    private Optional<SelectionKey> findWallSelection(Level level, PlanPoint point, Length tolerance) {
        return level.walls().stream()
                .filter(wall -> wall.axis().distanceTo(point).compareTo(tolerance) <= 0)
                .min(Comparator.comparingDouble(wall -> wall.axis().distanceTo(point).toMillimeters()))
                .map(wall -> new SelectionKey(RenderableKind.WALL, level.name(), wall.id().toString()));
    }

    private boolean containsPoint(Room room, PlanPoint point) {
        boolean inside = false;
        int lastIndex = room.outline().size() - 1;
        for (int currentIndex = 0; currentIndex < room.outline().size(); currentIndex++) {
            PlanPoint current = room.outline().get(currentIndex);
            PlanPoint previous = room.outline().get(lastIndex);
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
}
