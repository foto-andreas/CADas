package de.schrell.cadas.application.drawing;

import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SelectionQueryService {

    public Optional<SelectionKey> findSelection(Level level, PlanPoint point, Length tolerance) {
        return findSelections(level, point, tolerance).stream().findFirst();
    }

    public List<SelectionKey> findSelections(Level level, PlanPoint point, Length tolerance) {
        List<SelectionKey> selections = new ArrayList<>();
        selections.addAll(findDoorSelections(level, point, tolerance));
        selections.addAll(findWindowSelections(level, point, tolerance));
        selections.addAll(findRoofWindowSelections(level, point));
        selections.addAll(findStairSelections(level, point));
        selections.addAll(findFloorExtensionSelections(level, point));
        selections.addAll(findFloorOpeningSelections(level, point));
        selections.addAll(findHeatingExclusionSelections(level, point));
        selections.addAll(findWallSelections(level, point, tolerance));
        selections.addAll(findRoomObjectSelections(level, point));
        selections.addAll(findRoomSelections(level, point));
        return List.copyOf(selections);
    }

    private List<SelectionKey> findDoorSelections(Level level, PlanPoint point, Length tolerance) {
        List<SelectionKey> selections = new ArrayList<>();
        for (Door door : level.doors()) {
            Wall wall = level.findWall(door.wallId());
            PlanSegment segment = new PlanSegment(
                    wall.axis().pointAt(door.offsetFromStart()),
                    wall.axis().pointAt(door.offsetFromStart().add(door.width()))
            );
            if (segment.distanceTo(point).compareTo(tolerance) <= 0) {
                selections.add(new SelectionKey(RenderableKind.DOOR, level.name(), door.id().toString()));
            }
        }
        return selections;
    }

    private List<SelectionKey> findWindowSelections(Level level, PlanPoint point, Length tolerance) {
        List<SelectionKey> selections = new ArrayList<>();
        for (WindowElement window : level.windows()) {
            Wall wall = level.findWall(window.wallId());
            PlanSegment segment = new PlanSegment(
                    wall.axis().pointAt(window.offsetFromStart()),
                    wall.axis().pointAt(window.offsetFromStart().add(window.width()))
            );
            if (segment.distanceTo(point).compareTo(tolerance) <= 0) {
                selections.add(new SelectionKey(RenderableKind.WINDOW, level.name(), window.id().toString()));
            }
        }
        return selections;
    }

    private List<SelectionKey> findRoofWindowSelections(Level level, PlanPoint point) {
        return level.roofWindows().stream()
                .filter(roofWindow -> roofWindow.contains(point))
                .map(roofWindow -> new SelectionKey(RenderableKind.ROOF_WINDOW, level.name(), roofWindow.id().toString()))
                .toList();
    }

    private List<SelectionKey> findRoomSelections(Level level, PlanPoint point) {
        return level.rooms().stream()
                .filter(room -> containsPoint(room, point))
                .map(room -> new SelectionKey(RenderableKind.ROOM_VOLUME, level.name(), room.id().toString()))
                .toList();
    }

    private List<SelectionKey> findRoomObjectSelections(Level level, PlanPoint point) {
        return level.roomObjects().stream()
                .filter(RoomObject::visible)
                .filter(roomObject -> point.xMillimeters() >= roomObject.minXMillimeters()
                        && point.xMillimeters() <= roomObject.maxXMillimeters()
                        && point.yMillimeters() >= roomObject.minYMillimeters()
                        && point.yMillimeters() <= roomObject.maxYMillimeters())
                .map(roomObject -> new SelectionKey(RenderableKind.ROOM_OBJECT, level.name(), roomObject.id().toString()))
                .toList();
    }

    private List<SelectionKey> findStairSelections(Level level, PlanPoint point) {
        return level.staircases().stream()
                .filter(staircase -> point.xMillimeters() >= staircase.minX()
                        && point.xMillimeters() <= staircase.maxX()
                        && point.yMillimeters() >= staircase.minY()
                        && point.yMillimeters() <= staircase.maxY())
                .map(staircase -> new SelectionKey(RenderableKind.STAIR, level.name(), staircase.id().toString()))
                .toList();
    }

    private List<SelectionKey> findFloorExtensionSelections(Level level, PlanPoint point) {
        return level.floorExtensions().stream()
                .filter(extension -> point.xMillimeters() >= extension.minX()
                        && point.xMillimeters() <= extension.maxX()
                        && point.yMillimeters() >= extension.minY()
                        && point.yMillimeters() <= extension.maxY())
                .map(extension -> new SelectionKey(RenderableKind.FLOOR_EXTENSION, level.name(), extension.id().toString()))
                .toList();
    }

    private List<SelectionKey> findFloorOpeningSelections(Level level, PlanPoint point) {
        return level.floorOpenings().stream()
                .filter(opening -> opening.contains(point))
                .map(opening -> new SelectionKey(RenderableKind.FLOOR_OPENING, level.name(), opening.id().toString()))
                .toList();
    }

    private List<SelectionKey> findHeatingExclusionSelections(Level level, PlanPoint point) {
        return level.heatingExclusionAreas().stream()
                .filter(area -> area.contains(point))
                .map(area -> new SelectionKey(RenderableKind.HEATING_EXCLUSION, level.name(), area.id().toString()))
                .toList();
    }

    private List<SelectionKey> findWallSelections(Level level, PlanPoint point, Length tolerance) {
        return level.walls().stream()
                .filter(wall -> wall.axis().distanceTo(point).toMillimeters() <= Math.max(tolerance.toMillimeters(), wall.thickness().toMillimeters() / 2.0))
                .sorted(Comparator.comparingDouble(wall -> wall.axis().distanceTo(point).toMillimeters()))
                .map(wall -> new SelectionKey(RenderableKind.WALL, level.name(), wall.id().toString()))
                .toList();
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
