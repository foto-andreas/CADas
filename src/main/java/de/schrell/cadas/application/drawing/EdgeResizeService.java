package de.schrell.cadas.application.drawing;

import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

    public final class EdgeResizeService {

    private static final double MINIMUM_LENGTH = 1.0;
    private static final double MINIMUM_RECTANGLE_SIZE = 100.0;

    public Optional<EdgeHandle> findHandle(Level level, Set<SelectionKey> selections, PlanPoint point, Length tolerance) {
        return handles(level, selections).stream()
                .filter(handle -> handle.position().distanceTo(point).compareTo(tolerance) <= 0)
                .min(Comparator.comparingDouble(handle -> handle.position().distanceTo(point).toMillimeters()));
    }

    public List<EdgeHandle> handles(Level level, Set<SelectionKey> selections) {
        List<EdgeHandle> handles = new ArrayList<>();
        for (SelectionKey selection : selections) {
            UUID id = parseUuidOrNull(selection.elementId());
            if (id == null) {
                continue;
            }
            switch (selection.kind()) {
                case WALL -> level.walls().stream().filter(wall -> wall.id().equals(id)).findFirst().ifPresent(wall -> {
                    handles.add(new EdgeHandle(EdgeHandleKind.WALL_START, wall.id(), wall.id(), wall.axis().start()));
                    handles.add(new EdgeHandle(EdgeHandleKind.WALL_END, wall.id(), wall.id(), wall.axis().end()));
                });
                case DOOR -> level.doors().stream().filter(door -> door.id().equals(id)).findFirst().ifPresent(door -> {
                    Wall wall = level.findWall(door.wallId());
                    handles.add(new EdgeHandle(EdgeHandleKind.DOOR_START, door.id(), wall.id(), wall.axis().pointAt(door.offsetFromStart())));
                    handles.add(new EdgeHandle(EdgeHandleKind.DOOR_END, door.id(), wall.id(), wall.axis().pointAt(door.offsetFromStart().add(door.width()))));
                });
                case WINDOW -> level.windows().stream().filter(window -> window.id().equals(id)).findFirst().ifPresent(window -> {
                    Wall wall = level.findWall(window.wallId());
                    handles.add(new EdgeHandle(EdgeHandleKind.WINDOW_START, window.id(), wall.id(), wall.axis().pointAt(window.offsetFromStart())));
                    handles.add(new EdgeHandle(EdgeHandleKind.WINDOW_END, window.id(), wall.id(), wall.axis().pointAt(window.offsetFromStart().add(window.width()))));
                });
                case STAIR -> level.staircases().stream().filter(staircase -> staircase.id().equals(id)).findFirst().ifPresent(staircase -> {
                    addRectangleHandles(handles, RenderableKind.STAIR, staircase.id(), staircase.minX(), staircase.minY(), staircase.maxX(), staircase.maxY());
                });
                case FLOOR_OPENING -> level.floorOpenings().stream()
                        .filter(opening -> opening.id().equals(id))
                        .filter(opening -> opening.shape() == FloorOpeningShape.RECTANGLE)
                        .findFirst()
                        .ifPresent(opening -> addRectangleHandles(handles, RenderableKind.FLOOR_OPENING, opening.id(),
                                opening.minXMillimeters(), opening.minYMillimeters(),
                                opening.maxXMillimeters(), opening.maxYMillimeters()));
                case HEATING_EXCLUSION -> level.heatingExclusionAreas().stream().filter(area -> area.id().equals(id)).findFirst().ifPresent(area -> {
                    addRectangleHandles(handles, RenderableKind.HEATING_EXCLUSION, area.id(),
                            area.minXMillimeters(), area.minYMillimeters(), area.maxXMillimeters(), area.maxYMillimeters());
                });
                case HEATING_ZONE -> level.hydronicHeatings().stream()
                        .flatMap(heating -> heating.zones().stream())
                        .filter(zone -> zone.id().equals(id))
                        .findFirst()
                        .ifPresent(zone -> {
                            RectangleBounds bounds = bounds(zone.outline());
                            addRectangleHandles(handles, RenderableKind.HEATING_ZONE, zone.id(),
                                    bounds.minX(), bounds.minY(), bounds.maxX(), bounds.maxY());
                        });
                default -> {
                }
            }
        }
        return List.copyOf(handles);
    }

    private void addRectangleHandles(
            List<EdgeHandle> handles,
            RenderableKind elementKind,
            UUID elementId,
            double minX,
            double minY,
            double maxX,
            double maxY
    ) {
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        handles.add(new EdgeHandle(EdgeHandleKind.RECTANGLE_NORTH_WEST, elementKind, elementId, null, new PlanPoint(minX, minY)));
        handles.add(new EdgeHandle(EdgeHandleKind.RECTANGLE_NORTH, elementKind, elementId, null, new PlanPoint(centerX, minY)));
        handles.add(new EdgeHandle(EdgeHandleKind.RECTANGLE_NORTH_EAST, elementKind, elementId, null, new PlanPoint(maxX, minY)));
        handles.add(new EdgeHandle(EdgeHandleKind.RECTANGLE_EAST, elementKind, elementId, null, new PlanPoint(maxX, centerY)));
        handles.add(new EdgeHandle(EdgeHandleKind.RECTANGLE_SOUTH_EAST, elementKind, elementId, null, new PlanPoint(maxX, maxY)));
        handles.add(new EdgeHandle(EdgeHandleKind.RECTANGLE_SOUTH, elementKind, elementId, null, new PlanPoint(centerX, maxY)));
        handles.add(new EdgeHandle(EdgeHandleKind.RECTANGLE_SOUTH_WEST, elementKind, elementId, null, new PlanPoint(minX, maxY)));
        handles.add(new EdgeHandle(EdgeHandleKind.RECTANGLE_WEST, elementKind, elementId, null, new PlanPoint(minX, centerY)));
    }

    private static UUID parseUuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public ResizeResult resize(Level level, EdgeHandle handle, PlanPoint targetPoint) {
        return switch (handle.kind()) {
            case WALL_START, WALL_END -> resizeWall(level, handle, targetPoint);
            case DOOR_START, DOOR_END -> resizeDoor(level, handle, targetPoint);
            case WINDOW_START, WINDOW_END -> resizeWindow(level, handle, targetPoint);
            case STAIR_FIRST_CORNER, STAIR_OPPOSITE_CORNER -> resizeStaircase(level, handle, targetPoint);
            case RECTANGLE_NORTH_WEST, RECTANGLE_NORTH, RECTANGLE_NORTH_EAST, RECTANGLE_EAST,
                 RECTANGLE_SOUTH_EAST, RECTANGLE_SOUTH, RECTANGLE_SOUTH_WEST, RECTANGLE_WEST ->
                    resizeRectangle(level, handle, targetPoint);
        };
    }

    private ResizeResult resizeWall(Level level, EdgeHandle handle, PlanPoint targetPoint) {
        Wall wall = level.findWall(handle.hostWallId());
        Direction direction = direction(wall.axis());
        double oldLength = wall.axis().length().toMillimeters();
        double projected = projection(wall.axis().start(), targetPoint, direction);
        PlanSegment resizedAxis;
        double startShift;
        if (handle.kind() == EdgeHandleKind.WALL_START) {
            double maximumStart = minimumOpeningOffset(level, wall).orElse(oldLength - MINIMUM_LENGTH);
            startShift = clamp(projected, -Double.MAX_VALUE, maximumStart - MINIMUM_LENGTH);
            resizedAxis = new PlanSegment(pointAt(wall.axis().start(), direction, startShift), wall.axis().end());
        } else {
            double minimumEnd = maximumOpeningEnd(level, wall).orElse(MINIMUM_LENGTH);
            double newLength = clamp(projected, minimumEnd + MINIMUM_LENGTH, Double.MAX_VALUE);
            startShift = 0.0;
            resizedAxis = new PlanSegment(wall.axis().start(), pointAt(wall.axis().start(), direction, newLength));
        }
        Wall resizedWall = wall.withAxis(resizedAxis);
        List<Wall> walls = level.walls().stream().map(candidate -> candidate.id().equals(wall.id()) ? resizedWall : candidate).toList();
        List<Door> doors = level.doors().stream()
                .map(door -> door.wallId().equals(wall.id()) ? door.withOffset(Length.ofMillimeters(door.offsetFromStart().toMillimeters() - startShift)) : door)
                .toList();
        List<WindowElement> windows = level.windows().stream()
                .map(window -> window.wallId().equals(wall.id()) ? window.withOffset(Length.ofMillimeters(window.offsetFromStart().toMillimeters() - startShift)) : window)
                .toList();
        return new ResizeResult(walls, doors, windows, level.staircases(), level.floorOpenings(), level.heatingExclusionAreas(), level.hydronicHeatings());
    }

    private ResizeResult resizeDoor(Level level, EdgeHandle handle, PlanPoint targetPoint) {
        Door door = level.doors().stream().filter(candidate -> candidate.id().equals(handle.elementId())).findFirst().orElseThrow();
        Wall wall = level.findWall(door.wallId());
        double target = clamp(projectedLength(wall, targetPoint), 0.0, wall.axis().length().toMillimeters());
        double start = door.offsetFromStart().toMillimeters();
        double end = start + door.width().toMillimeters();
        Door resized = handle.kind() == EdgeHandleKind.DOOR_START
                ? new Door(door.id(), door.wallId(), Length.ofMillimeters(Math.min(target, end - MINIMUM_LENGTH)), Length.ofMillimeters(end - Math.min(target, end - MINIMUM_LENGTH)), door.height(), door.thresholdHeight())
                : new Door(door.id(), door.wallId(), door.offsetFromStart(), Length.ofMillimeters(Math.max(target, start + MINIMUM_LENGTH) - start), door.height(), door.thresholdHeight());
        return new ResizeResult(level.walls(), level.doors().stream().map(candidate -> candidate.id().equals(door.id()) ? resized : candidate).toList(),
                level.windows(), level.staircases(), level.floorOpenings(), level.heatingExclusionAreas(), level.hydronicHeatings());
    }

    private ResizeResult resizeWindow(Level level, EdgeHandle handle, PlanPoint targetPoint) {
        WindowElement window = level.windows().stream().filter(candidate -> candidate.id().equals(handle.elementId())).findFirst().orElseThrow();
        Wall wall = level.findWall(window.wallId());
        double target = clamp(projectedLength(wall, targetPoint), 0.0, wall.axis().length().toMillimeters());
        double start = window.offsetFromStart().toMillimeters();
        double end = start + window.width().toMillimeters();
        WindowElement resized = handle.kind() == EdgeHandleKind.WINDOW_START
                ? new WindowElement(window.id(), window.wallId(), Length.ofMillimeters(Math.min(target, end - MINIMUM_LENGTH)), Length.ofMillimeters(end - Math.min(target, end - MINIMUM_LENGTH)), window.sillHeight(), window.windowHeight())
                : new WindowElement(window.id(), window.wallId(), window.offsetFromStart(), Length.ofMillimeters(Math.max(target, start + MINIMUM_LENGTH) - start), window.sillHeight(), window.windowHeight());
        return new ResizeResult(level.walls(), level.doors(),
                level.windows().stream().map(candidate -> candidate.id().equals(window.id()) ? resized : candidate).toList(),
                level.staircases(), level.floorOpenings(), level.heatingExclusionAreas(), level.hydronicHeatings());
    }

    private ResizeResult resizeStaircase(Level level, EdgeHandle handle, PlanPoint targetPoint) {
        Staircase staircase = level.staircases().stream().filter(candidate -> candidate.id().equals(handle.elementId())).findFirst().orElseThrow();
        double minSize = 100.0;
        boolean draggingFirstCorner = handle.kind() == EdgeHandleKind.STAIR_FIRST_CORNER;
        PlanPoint fixedCorner = draggingFirstCorner ? staircase.oppositeCorner() : staircase.firstCorner();
        PlanPoint draggedCorner = draggingFirstCorner ? staircase.firstCorner() : staircase.oppositeCorner();
        double minX = Math.min(fixedCorner.xMillimeters(), draggedCorner.xMillimeters());
        double maxX = Math.max(fixedCorner.xMillimeters(), draggedCorner.xMillimeters());
        double minY = Math.min(fixedCorner.yMillimeters(), draggedCorner.yMillimeters());
        double maxY = Math.max(fixedCorner.yMillimeters(), draggedCorner.yMillimeters());
        double clampedX;
        double clampedY;
        if (draggingFirstCorner) {
            clampedX = fixedCorner.xMillimeters() >= draggedCorner.xMillimeters()
                    ? Math.min(targetPoint.xMillimeters(), fixedCorner.xMillimeters() - minSize)
                    : Math.max(targetPoint.xMillimeters(), fixedCorner.xMillimeters() + minSize);
            clampedY = fixedCorner.yMillimeters() >= draggedCorner.yMillimeters()
                    ? Math.min(targetPoint.yMillimeters(), fixedCorner.yMillimeters() - minSize)
                    : Math.max(targetPoint.yMillimeters(), fixedCorner.yMillimeters() + minSize);
        } else {
            clampedX = Math.max(targetPoint.xMillimeters(), minX + minSize);
            clampedY = Math.max(targetPoint.yMillimeters(), minY + minSize);
        }
        clampedX = Math.min(clampedX, maxX + 10_000_000.0);
        clampedY = Math.min(clampedY, maxY + 10_000_000.0);
        PlanPoint clampedTarget = new PlanPoint(clampedX, clampedY);
        Staircase resized = draggingFirstCorner
                ? new Staircase(staircase.id(), staircase.stairType(), clampedTarget, staircase.oppositeCorner(), staircase.totalHeight(), staircase.stepCount(), staircase.rotationQuarterTurns(), staircase.startLandingWidth(), staircase.endLandingWidth(), staircase.leftUnderbuildWidth(), staircase.rightUnderbuildWidth(), staircase.undersideThickness())
                : new Staircase(staircase.id(), staircase.stairType(), staircase.firstCorner(), clampedTarget, staircase.totalHeight(), staircase.stepCount(), staircase.rotationQuarterTurns(), staircase.startLandingWidth(), staircase.endLandingWidth(), staircase.leftUnderbuildWidth(), staircase.rightUnderbuildWidth(), staircase.undersideThickness());
        return new ResizeResult(level.walls(), level.doors(), level.windows(),
                level.staircases().stream().map(candidate -> candidate.id().equals(staircase.id()) ? resized : candidate).toList(),
                level.floorOpenings(), level.heatingExclusionAreas(), level.hydronicHeatings());
    }

    private ResizeResult resizeRectangle(Level level, EdgeHandle handle, PlanPoint targetPoint) {
        return switch (handle.elementKind()) {
            case STAIR -> resizeStaircaseRectangle(level, handle, targetPoint);
            case FLOOR_OPENING -> resizeFloorOpeningRectangle(level, handle, targetPoint);
            case HEATING_EXCLUSION -> resizeHeatingExclusionRectangle(level, handle, targetPoint);
            case HEATING_ZONE -> resizeHeatingZoneRectangle(level, handle, targetPoint);
            default -> throw new IllegalArgumentException("Bauteil kann nicht rechteckig geändert werden: " + handle.elementKind());
        };
    }

    private ResizeResult resizeStaircaseRectangle(Level level, EdgeHandle handle, PlanPoint targetPoint) {
        Staircase staircase = level.staircases().stream()
                .filter(candidate -> candidate.id().equals(handle.elementId()))
                .findFirst()
                .orElseThrow();
        RectangleBounds bounds = resizeBounds(
                new RectangleBounds(staircase.minX(), staircase.minY(), staircase.maxX(), staircase.maxY()),
                handle.kind(),
                targetPoint
        );
        PlanPoint firstCorner = preserveCornerOrientation(staircase.firstCorner(), staircase.oppositeCorner(), bounds);
        PlanPoint oppositeCorner = preserveCornerOrientation(staircase.oppositeCorner(), staircase.firstCorner(), bounds);
        Staircase resized = new Staircase(
                staircase.id(), staircase.stairType(), firstCorner, oppositeCorner,
                staircase.totalHeight(), staircase.stepCount(), staircase.rotationQuarterTurns(),
                staircase.startLandingWidth(), staircase.endLandingWidth(),
                staircase.leftUnderbuildWidth(), staircase.rightUnderbuildWidth(), staircase.undersideThickness()
        );
        return new ResizeResult(level.walls(), level.doors(), level.windows(),
                level.staircases().stream().map(candidate -> candidate.id().equals(staircase.id()) ? resized : candidate).toList(),
                level.floorOpenings(), level.heatingExclusionAreas(), level.hydronicHeatings());
    }

    private ResizeResult resizeFloorOpeningRectangle(Level level, EdgeHandle handle, PlanPoint targetPoint) {
        FloorOpening opening = level.floorOpenings().stream()
                .filter(candidate -> candidate.id().equals(handle.elementId()))
                .findFirst()
                .orElseThrow();
        RectangleBounds bounds = resizeBounds(
                new RectangleBounds(opening.minXMillimeters(), opening.minYMillimeters(),
                        opening.maxXMillimeters(), opening.maxYMillimeters()),
                handle.kind(),
                targetPoint
        );
        FloorOpening resized = new FloorOpening(
                opening.id(),
                opening.roomId(),
                opening.shape(),
                bounds.center(),
                Length.ofMillimeters(bounds.width()),
                Length.ofMillimeters(bounds.height())
        );
        return new ResizeResult(level.walls(), level.doors(), level.windows(), level.staircases(),
                level.floorOpenings().stream().map(candidate -> candidate.id().equals(opening.id()) ? resized : candidate).toList(),
                level.heatingExclusionAreas(), level.hydronicHeatings());
    }

    private ResizeResult resizeHeatingExclusionRectangle(Level level, EdgeHandle handle, PlanPoint targetPoint) {
        HeatingExclusionArea area = level.heatingExclusionAreas().stream()
                .filter(candidate -> candidate.id().equals(handle.elementId()))
                .findFirst()
                .orElseThrow();
        RectangleBounds bounds = resizeBounds(
                new RectangleBounds(area.minXMillimeters(), area.minYMillimeters(),
                        area.maxXMillimeters(), area.maxYMillimeters()),
                handle.kind(),
                targetPoint
        );
        HeatingExclusionArea resized = area.withCorners(
                preserveCornerOrientation(area.firstCorner(), area.oppositeCorner(), bounds),
                preserveCornerOrientation(area.oppositeCorner(), area.firstCorner(), bounds)
        );
        return new ResizeResult(level.walls(), level.doors(), level.windows(), level.staircases(),
                level.floorOpenings(),
                level.heatingExclusionAreas().stream().map(candidate -> candidate.id().equals(area.id()) ? resized : candidate).toList(),
                level.hydronicHeatings());
    }

    private ResizeResult resizeHeatingZoneRectangle(Level level, EdgeHandle handle, PlanPoint targetPoint) {
        HydronicHeating heating = level.hydronicHeatings().stream()
                .filter(candidate -> candidate.zones().stream().anyMatch(zone -> zone.id().equals(handle.elementId())))
                .findFirst()
                .orElseThrow();
        HeatingZone zone = heating.zones().stream()
                .filter(candidate -> candidate.id().equals(handle.elementId()))
                .findFirst()
                .orElseThrow();
        RectangleBounds bounds = resizeBounds(bounds(zone.outline()), handle.kind(), targetPoint);
        HeatingZone resized = zone.withOutline(rectanglePoints(bounds));
        HydronicHeating resizedHeating = heating.withZones(heating.zones().stream()
                .map(candidate -> candidate.id().equals(zone.id()) ? resized : candidate)
                .toList());
        return new ResizeResult(level.walls(), level.doors(), level.windows(), level.staircases(),
                level.floorOpenings(), level.heatingExclusionAreas(),
                level.hydronicHeatings().stream()
                        .map(candidate -> candidate.id().equals(heating.id()) ? resizedHeating : candidate)
                        .toList());
    }

    private RectangleBounds resizeBounds(RectangleBounds bounds, EdgeHandleKind handleKind, PlanPoint targetPoint) {
        double minX = bounds.minX();
        double maxX = bounds.maxX();
        double minY = bounds.minY();
        double maxY = bounds.maxY();
        if (movesWest(handleKind)) {
            minX = Math.min(targetPoint.xMillimeters(), maxX - MINIMUM_RECTANGLE_SIZE);
        }
        if (movesEast(handleKind)) {
            maxX = Math.max(targetPoint.xMillimeters(), minX + MINIMUM_RECTANGLE_SIZE);
        }
        if (movesNorth(handleKind)) {
            minY = Math.min(targetPoint.yMillimeters(), maxY - MINIMUM_RECTANGLE_SIZE);
        }
        if (movesSouth(handleKind)) {
            maxY = Math.max(targetPoint.yMillimeters(), minY + MINIMUM_RECTANGLE_SIZE);
        }
        return new RectangleBounds(minX, minY, maxX, maxY);
    }

    private PlanPoint preserveCornerOrientation(PlanPoint corner, PlanPoint oppositeCorner, RectangleBounds bounds) {
        double x = corner.xMillimeters() <= oppositeCorner.xMillimeters() ? bounds.minX() : bounds.maxX();
        double y = corner.yMillimeters() <= oppositeCorner.yMillimeters() ? bounds.minY() : bounds.maxY();
        return new PlanPoint(x, y);
    }

    private RectangleBounds bounds(List<PlanPoint> points) {
        return new RectangleBounds(
                points.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0)
        );
    }

    private List<PlanPoint> rectanglePoints(RectangleBounds bounds) {
        return List.of(
                new PlanPoint(bounds.minX(), bounds.minY()),
                new PlanPoint(bounds.maxX(), bounds.minY()),
                new PlanPoint(bounds.maxX(), bounds.maxY()),
                new PlanPoint(bounds.minX(), bounds.maxY())
        );
    }

    public static boolean isRectangleCorner(EdgeHandleKind kind) {
        return kind == EdgeHandleKind.RECTANGLE_NORTH_WEST
                || kind == EdgeHandleKind.RECTANGLE_NORTH_EAST
                || kind == EdgeHandleKind.RECTANGLE_SOUTH_EAST
                || kind == EdgeHandleKind.RECTANGLE_SOUTH_WEST
                || kind == EdgeHandleKind.STAIR_FIRST_CORNER
                || kind == EdgeHandleKind.STAIR_OPPOSITE_CORNER;
    }

    public static boolean isRectangleHorizontalResize(EdgeHandleKind kind) {
        return kind == EdgeHandleKind.RECTANGLE_EAST || kind == EdgeHandleKind.RECTANGLE_WEST;
    }

    public static boolean isRectangleVerticalResize(EdgeHandleKind kind) {
        return kind == EdgeHandleKind.RECTANGLE_NORTH || kind == EdgeHandleKind.RECTANGLE_SOUTH;
    }

    private boolean movesWest(EdgeHandleKind kind) {
        return kind == EdgeHandleKind.RECTANGLE_NORTH_WEST
                || kind == EdgeHandleKind.RECTANGLE_WEST
                || kind == EdgeHandleKind.RECTANGLE_SOUTH_WEST;
    }

    private boolean movesEast(EdgeHandleKind kind) {
        return kind == EdgeHandleKind.RECTANGLE_NORTH_EAST
                || kind == EdgeHandleKind.RECTANGLE_EAST
                || kind == EdgeHandleKind.RECTANGLE_SOUTH_EAST;
    }

    private boolean movesNorth(EdgeHandleKind kind) {
        return kind == EdgeHandleKind.RECTANGLE_NORTH_WEST
                || kind == EdgeHandleKind.RECTANGLE_NORTH
                || kind == EdgeHandleKind.RECTANGLE_NORTH_EAST;
    }

    private boolean movesSouth(EdgeHandleKind kind) {
        return kind == EdgeHandleKind.RECTANGLE_SOUTH_WEST
                || kind == EdgeHandleKind.RECTANGLE_SOUTH
                || kind == EdgeHandleKind.RECTANGLE_SOUTH_EAST;
    }

    private Optional<Double> minimumOpeningOffset(Level level, Wall wall) {
        return java.util.stream.Stream.concat(
                        level.doors().stream().filter(door -> door.wallId().equals(wall.id())).map(door -> door.offsetFromStart().toMillimeters()),
                        level.windows().stream().filter(window -> window.wallId().equals(wall.id())).map(window -> window.offsetFromStart().toMillimeters())
                )
                .min(Double::compare);
    }

    private Optional<Double> maximumOpeningEnd(Level level, Wall wall) {
        return java.util.stream.Stream.concat(
                        level.doors().stream().filter(door -> door.wallId().equals(wall.id())).map(door -> door.offsetFromStart().add(door.width()).toMillimeters()),
                        level.windows().stream().filter(window -> window.wallId().equals(wall.id())).map(window -> window.offsetFromStart().add(window.width()).toMillimeters())
                )
                .max(Double::compare);
    }

    private double projectedLength(Wall wall, PlanPoint point) {
        return wall.axis().projectedLength(point).toMillimeters();
    }

    private double projection(PlanPoint origin, PlanPoint point, Direction direction) {
        return (point.xMillimeters() - origin.xMillimeters()) * direction.x()
                + (point.yMillimeters() - origin.yMillimeters()) * direction.y();
    }

    private Direction direction(PlanSegment segment) {
        double length = segment.length().toMillimeters();
        return new Direction(
                (segment.end().xMillimeters() - segment.start().xMillimeters()) / length,
                (segment.end().yMillimeters() - segment.start().yMillimeters()) / length
        );
    }

    private PlanPoint pointAt(PlanPoint origin, Direction direction, double distance) {
        return new PlanPoint(origin.xMillimeters() + direction.x() * distance, origin.yMillimeters() + direction.y() * distance);
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record Direction(double x, double y) {
    }

    public enum EdgeHandleKind {
        WALL_START,
        WALL_END,
        DOOR_START,
        DOOR_END,
        WINDOW_START,
        WINDOW_END,
        STAIR_FIRST_CORNER,
        STAIR_OPPOSITE_CORNER,
        RECTANGLE_NORTH_WEST,
        RECTANGLE_NORTH,
        RECTANGLE_NORTH_EAST,
        RECTANGLE_EAST,
        RECTANGLE_SOUTH_EAST,
        RECTANGLE_SOUTH,
        RECTANGLE_SOUTH_WEST,
        RECTANGLE_WEST
    }

    private record RectangleBounds(double minX, double minY, double maxX, double maxY) {
        private double width() {
            return maxX - minX;
        }

        private double height() {
            return maxY - minY;
        }

        private PlanPoint center() {
            return new PlanPoint((minX + maxX) / 2.0, (minY + maxY) / 2.0);
        }
    }

    public record EdgeHandle(EdgeHandleKind kind, RenderableKind elementKind, UUID elementId, UUID hostWallId, PlanPoint position) {
        public EdgeHandle(EdgeHandleKind kind, UUID elementId, UUID hostWallId, PlanPoint position) {
            this(kind, elementKindFor(kind), elementId, hostWallId, position);
        }

        private static RenderableKind elementKindFor(EdgeHandleKind kind) {
            return switch (kind) {
                case WALL_START, WALL_END -> RenderableKind.WALL;
                case DOOR_START, DOOR_END -> RenderableKind.DOOR;
                case WINDOW_START, WINDOW_END -> RenderableKind.WINDOW;
                case STAIR_FIRST_CORNER, STAIR_OPPOSITE_CORNER -> RenderableKind.STAIR;
                default -> throw new IllegalArgumentException("Rechteck-Handles brauchen eine explizite Bauteilart.");
            };
        }
    }

    public record ResizeResult(
            List<Wall> walls,
            List<Door> doors,
            List<WindowElement> windows,
            List<Staircase> staircases,
            List<FloorOpening> floorOpenings,
            List<HeatingExclusionArea> heatingExclusionAreas,
            List<HydronicHeating> hydronicHeatings
    ) {
        public ResizeResult(List<Wall> walls, List<Door> doors, List<WindowElement> windows, List<Staircase> staircases) {
            this(walls, doors, windows, staircases, List.of(), List.of(), List.of());
        }

        public ResizeResult(
                List<Wall> walls,
                List<Door> doors,
                List<WindowElement> windows,
                List<Staircase> staircases,
                List<FloorOpening> floorOpenings,
                List<HeatingExclusionArea> heatingExclusionAreas
        ) {
            this(walls, doors, windows, staircases, floorOpenings, heatingExclusionAreas, List.of());
        }

        public ResizeResult {
            walls = List.copyOf(walls);
            doors = List.copyOf(doors);
            windows = List.copyOf(windows);
            staircases = List.copyOf(staircases);
            floorOpenings = List.copyOf(floorOpenings);
            heatingExclusionAreas = List.copyOf(heatingExclusionAreas);
            hydronicHeatings = List.copyOf(hydronicHeatings);
        }
    }
}
