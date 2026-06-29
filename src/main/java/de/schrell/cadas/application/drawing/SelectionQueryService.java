package de.schrell.cadas.application.drawing;

import de.schrell.cadas.application.view.WallPlanOutlineService;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.RoofWindow;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SelectionQueryService {

    private final WallPlanOutlineService wallPlanOutlineService = new WallPlanOutlineService();

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
        selections.addAll(findHeatingZoneSelections(level, point));
        selections.addAll(findHeatingManifoldSelections(level, point));
        selections.addAll(findWallSelections(level, point, tolerance));
        selections.addAll(findRoomObjectSelections(level, point));
        selections.addAll(findRoomSelections(level, point));
        return List.copyOf(selections);
    }

    public List<SelectionKey> findSelectionsWithin(Level level, PlanPoint firstCorner, PlanPoint oppositeCorner) {
        RectangleSelectionBounds bounds = RectangleSelectionBounds.of(firstCorner, oppositeCorner);
        List<SelectionKey> selections = new ArrayList<>();
        selections.addAll(findDoorSelectionsWithin(level, bounds));
        selections.addAll(findWindowSelectionsWithin(level, bounds));
        selections.addAll(findRoofWindowSelectionsWithin(level, bounds));
        selections.addAll(findStairSelectionsWithin(level, bounds));
        selections.addAll(findFloorExtensionSelectionsWithin(level, bounds));
        selections.addAll(findFloorOpeningSelectionsWithin(level, bounds));
        selections.addAll(findHeatingExclusionSelectionsWithin(level, bounds));
        selections.addAll(findHeatingZoneSelectionsWithin(level, bounds));
        selections.addAll(findHeatingManifoldSelectionsWithin(level, bounds));
        selections.addAll(findWallSelectionsWithin(level, bounds));
        selections.addAll(findRoomObjectSelectionsWithin(level, bounds));
        selections.addAll(findRoomSelectionsWithin(level, bounds));
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

    private List<SelectionKey> findDoorSelectionsWithin(Level level, RectangleSelectionBounds bounds) {
        return level.doors().stream()
                .filter(door -> {
                    Wall wall = level.findWall(door.wallId());
                    return bounds.contains(wall.axis().pointAt(door.offsetFromStart()))
                            && bounds.contains(wall.axis().pointAt(door.offsetFromStart().add(door.width())));
                })
                .map(door -> new SelectionKey(RenderableKind.DOOR, level.name(), door.id().toString()))
                .toList();
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

    private List<SelectionKey> findWindowSelectionsWithin(Level level, RectangleSelectionBounds bounds) {
        return level.windows().stream()
                .filter(window -> {
                    Wall wall = level.findWall(window.wallId());
                    return bounds.contains(wall.axis().pointAt(window.offsetFromStart()))
                            && bounds.contains(wall.axis().pointAt(window.offsetFromStart().add(window.width())));
                })
                .map(window -> new SelectionKey(RenderableKind.WINDOW, level.name(), window.id().toString()))
                .toList();
    }

    private List<SelectionKey> findRoofWindowSelections(Level level, PlanPoint point) {
        return level.roofWindows().stream()
                .filter(roofWindow -> roofWindow.contains(point))
                .map(roofWindow -> new SelectionKey(RenderableKind.ROOF_WINDOW, level.name(), roofWindow.id().toString()))
                .toList();
    }

    private List<SelectionKey> findRoofWindowSelectionsWithin(Level level, RectangleSelectionBounds bounds) {
        return level.roofWindows().stream()
                .filter(roofWindow -> boundsWithin(
                        bounds,
                        roofWindow.center().xMillimeters() - roofWindow.width().toMillimeters() / 2.0,
                        roofWindow.center().yMillimeters() - roofWindow.depth().toMillimeters() / 2.0,
                        roofWindow.center().xMillimeters() + roofWindow.width().toMillimeters() / 2.0,
                        roofWindow.center().yMillimeters() + roofWindow.depth().toMillimeters() / 2.0
                ))
                .map(roofWindow -> new SelectionKey(RenderableKind.ROOF_WINDOW, level.name(), roofWindow.id().toString()))
                .toList();
    }

    private List<SelectionKey> findRoomSelections(Level level, PlanPoint point) {
        return level.rooms().stream()
                .filter(room -> containsPoint(room, point))
                .map(room -> new SelectionKey(RenderableKind.ROOM_VOLUME, level.name(), room.id().toString()))
                .toList();
    }

    private List<SelectionKey> findRoomSelectionsWithin(Level level, RectangleSelectionBounds bounds) {
        return level.rooms().stream()
                .filter(room -> outlineWithin(bounds, room.outline()))
                .map(room -> new SelectionKey(RenderableKind.ROOM_VOLUME, level.name(), room.id().toString()))
                .toList();
    }

    private List<SelectionKey> findRoomObjectSelections(Level level, PlanPoint point) {
        return level.roomObjects().stream()
                .filter(RoomObject::visible)
                .filter(roomObject -> roomObject.contains(point))
                .map(roomObject -> new SelectionKey(RenderableKind.ROOM_OBJECT, level.name(), roomObject.id().toString()))
                .toList();
    }

    private List<SelectionKey> findRoomObjectSelectionsWithin(Level level, RectangleSelectionBounds bounds) {
        return level.roomObjects().stream()
                .filter(RoomObject::visible)
                .filter(roomObject -> boundsWithin(
                        bounds,
                        roomObject.minXMillimeters(),
                        roomObject.minYMillimeters(),
                        roomObject.maxXMillimeters(),
                        roomObject.maxYMillimeters()
                ))
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

    private List<SelectionKey> findStairSelectionsWithin(Level level, RectangleSelectionBounds bounds) {
        return level.staircases().stream()
                .filter(staircase -> boundsWithin(bounds, staircase.minX(), staircase.minY(), staircase.maxX(), staircase.maxY()))
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

    private List<SelectionKey> findFloorExtensionSelectionsWithin(Level level, RectangleSelectionBounds bounds) {
        return level.floorExtensions().stream()
                .filter(extension -> boundsWithin(bounds, extension.minX(), extension.minY(), extension.maxX(), extension.maxY()))
                .map(extension -> new SelectionKey(RenderableKind.FLOOR_EXTENSION, level.name(), extension.id().toString()))
                .toList();
    }

    private List<SelectionKey> findFloorOpeningSelections(Level level, PlanPoint point) {
        return level.floorOpenings().stream()
                .filter(opening -> opening.contains(point))
                .map(opening -> new SelectionKey(RenderableKind.FLOOR_OPENING, level.name(), opening.id().toString()))
                .toList();
    }

    private List<SelectionKey> findFloorOpeningSelectionsWithin(Level level, RectangleSelectionBounds bounds) {
        return level.floorOpenings().stream()
                .filter(opening -> boundsWithin(
                        bounds,
                        opening.minXMillimeters(),
                        opening.minYMillimeters(),
                        opening.maxXMillimeters(),
                        opening.maxYMillimeters()
                ))
                .map(opening -> new SelectionKey(RenderableKind.FLOOR_OPENING, level.name(), opening.id().toString()))
                .toList();
    }

    private List<SelectionKey> findHeatingExclusionSelections(Level level, PlanPoint point) {
        return level.heatingExclusionAreas().stream()
                .filter(area -> area.contains(point))
                .map(area -> new SelectionKey(RenderableKind.HEATING_EXCLUSION, level.name(), area.id().toString()))
                .toList();
    }

    private List<SelectionKey> findHeatingExclusionSelectionsWithin(Level level, RectangleSelectionBounds bounds) {
        return level.heatingExclusionAreas().stream()
                .filter(area -> boundsWithin(
                        bounds,
                        area.minXMillimeters(),
                        area.minYMillimeters(),
                        area.maxXMillimeters(),
                        area.maxYMillimeters()
                ))
                .map(area -> new SelectionKey(RenderableKind.HEATING_EXCLUSION, level.name(), area.id().toString()))
                .toList();
    }

    private List<SelectionKey> findHeatingZoneSelections(Level level, PlanPoint point) {
        return level.hydronicHeatings().stream()
                .flatMap(heating -> heating.zones().stream())
                .filter(zone -> containsPoint(zone, point))
                .map(zone -> new SelectionKey(RenderableKind.HEATING_ZONE, level.name(), zone.id().toString()))
                .toList();
    }

    private List<SelectionKey> findHeatingZoneSelectionsWithin(Level level, RectangleSelectionBounds bounds) {
        return level.hydronicHeatings().stream()
                .flatMap(heating -> heating.zones().stream())
                .filter(zone -> outlineWithin(bounds, zone.outline()))
                .map(zone -> new SelectionKey(RenderableKind.HEATING_ZONE, level.name(), zone.id().toString()))
                .toList();
    }

    private List<SelectionKey> findHeatingManifoldSelections(Level level, PlanPoint point) {
        return level.hydronicHeatings().stream()
                .filter(heating -> containsManifoldArea(heating, point))
                .map(heating -> new SelectionKey(RenderableKind.HEATING_MANIFOLD, level.name(), heating.id().toString()))
                .toList();
    }

    private List<SelectionKey> findHeatingManifoldSelectionsWithin(Level level, RectangleSelectionBounds bounds) {
        return level.hydronicHeatings().stream()
                .filter(heating -> {
                    double centerX = (heating.supplyPoint().xMillimeters() + heating.returnPoint().xMillimeters()) / 2.0;
                    double centerY = (heating.supplyPoint().yMillimeters() + heating.returnPoint().yMillimeters()) / 2.0;
                    double halfWidth = heating.manifoldFreeAreaWidth().toMillimeters() / 2.0;
                    double halfDepth = heating.manifoldFreeAreaDepth().toMillimeters() / 2.0;
                    return boundsWithin(bounds, centerX - halfWidth, centerY - halfDepth, centerX + halfWidth, centerY + halfDepth);
                })
                .map(heating -> new SelectionKey(RenderableKind.HEATING_MANIFOLD, level.name(), heating.id().toString()))
                .toList();
    }

    private boolean containsManifoldArea(HydronicHeating heating, PlanPoint point) {
        double centerX = (heating.supplyPoint().xMillimeters() + heating.returnPoint().xMillimeters()) / 2.0;
        double centerY = (heating.supplyPoint().yMillimeters() + heating.returnPoint().yMillimeters()) / 2.0;
        double halfWidth = heating.manifoldFreeAreaWidth().toMillimeters() / 2.0;
        double halfDepth = heating.manifoldFreeAreaDepth().toMillimeters() / 2.0;
        return point.xMillimeters() >= centerX - halfWidth
                && point.xMillimeters() <= centerX + halfWidth
                && point.yMillimeters() >= centerY - halfDepth
                && point.yMillimeters() <= centerY + halfDepth;
    }

    private List<SelectionKey> findWallSelections(Level level, PlanPoint point, Length tolerance) {
        return level.walls().stream()
                .map(wall -> wallSelectionHit(wall, point))
                .filter(hit -> hit.distanceMillimeters() <= Math.max(
                        tolerance.toMillimeters(),
                        hit.wall().thickness().toMillimeters() / 2.0
                ))
                .sorted(Comparator.comparingDouble(WallSelectionHit::distanceMillimeters)
                        .thenComparing(Comparator.comparingDouble(WallSelectionHit::endpointClearanceMillimeters).reversed())
                        .thenComparing(hit -> hit.wall().id()))
                .map(hit -> new SelectionKey(RenderableKind.WALL, level.name(), hit.wall().id().toString()))
                .toList();
    }

    private List<SelectionKey> findWallSelectionsWithin(Level level, RectangleSelectionBounds bounds) {
        return level.walls().stream()
                .filter(wall -> outlineWithin(bounds, wallPlanOutlineService.outline(wall)))
                .map(wall -> new SelectionKey(RenderableKind.WALL, level.name(), wall.id().toString()))
                .toList();
    }

    private WallSelectionHit wallSelectionHit(Wall wall, PlanPoint point) {
        double projection = wall.axis().projectedLength(point).toMillimeters();
        double axisLength = wall.axis().length().toMillimeters();
        return new WallSelectionHit(
                wall,
                wall.axis().distanceTo(point).toMillimeters(),
                Math.min(projection, Math.max(0.0, axisLength - projection))
        );
    }

    private boolean containsPoint(Room room, PlanPoint point) {
        return containsPoint(room.outline(), point);
    }

    private boolean containsPoint(HeatingZone zone, PlanPoint point) {
        return containsPoint(zone.outline(), point);
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

    private boolean outlineWithin(RectangleSelectionBounds bounds, List<PlanPoint> outline) {
        return outline.stream().allMatch(bounds::contains);
    }

    private boolean boundsWithin(RectangleSelectionBounds bounds, double minX, double minY, double maxX, double maxY) {
        return bounds.contains(new PlanPoint(minX, minY))
                && bounds.contains(new PlanPoint(maxX, minY))
                && bounds.contains(new PlanPoint(maxX, maxY))
                && bounds.contains(new PlanPoint(minX, maxY));
    }

    private record WallSelectionHit(
            Wall wall,
            double distanceMillimeters,
            double endpointClearanceMillimeters
    ) {
    }

    private record RectangleSelectionBounds(double minX, double minY, double maxX, double maxY) {
        static RectangleSelectionBounds of(PlanPoint firstCorner, PlanPoint oppositeCorner) {
            return new RectangleSelectionBounds(
                    Math.min(firstCorner.xMillimeters(), oppositeCorner.xMillimeters()),
                    Math.min(firstCorner.yMillimeters(), oppositeCorner.yMillimeters()),
                    Math.max(firstCorner.xMillimeters(), oppositeCorner.xMillimeters()),
                    Math.max(firstCorner.yMillimeters(), oppositeCorner.yMillimeters())
            );
        }

        boolean contains(PlanPoint point) {
            return point.xMillimeters() >= minX
                    && point.xMillimeters() <= maxX
                    && point.yMillimeters() >= minY
                    && point.yMillimeters() <= maxY;
        }
    }
}
