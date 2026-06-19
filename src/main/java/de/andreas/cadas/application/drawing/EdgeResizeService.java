package de.andreas.cadas.application.drawing;

import de.andreas.cadas.application.view.RenderableKind;
import de.andreas.cadas.application.view.SelectionKey;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class EdgeResizeService {

    private static final double MINIMUM_LENGTH = 1.0;

    public Optional<EdgeHandle> findHandle(Level level, Set<SelectionKey> selections, PlanPoint point, Length tolerance) {
        return handles(level, selections).stream()
                .filter(handle -> handle.position().distanceTo(point).compareTo(tolerance) <= 0)
                .min(Comparator.comparingDouble(handle -> handle.position().distanceTo(point).toMillimeters()));
    }

    public List<EdgeHandle> handles(Level level, Set<SelectionKey> selections) {
        List<EdgeHandle> handles = new ArrayList<>();
        for (SelectionKey selection : selections) {
            UUID id = UUID.fromString(selection.elementId());
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
                default -> {
                }
            }
        }
        return List.copyOf(handles);
    }

    public ResizeResult resize(Level level, EdgeHandle handle, PlanPoint targetPoint) {
        return switch (handle.kind()) {
            case WALL_START, WALL_END -> resizeWall(level, handle, targetPoint);
            case DOOR_START, DOOR_END -> resizeDoor(level, handle, targetPoint);
            case WINDOW_START, WINDOW_END -> resizeWindow(level, handle, targetPoint);
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
        Wall resizedWall = new Wall(
                wall.id(),
                resizedAxis,
                wall.thickness(),
                wall.height(),
                wall.startHeight(),
                wall.endHeight()
        );
        List<Wall> walls = level.walls().stream().map(candidate -> candidate.id().equals(wall.id()) ? resizedWall : candidate).toList();
        List<Door> doors = level.doors().stream()
                .map(door -> door.wallId().equals(wall.id()) ? door.withOffset(Length.ofMillimeters(door.offsetFromStart().toMillimeters() - startShift)) : door)
                .toList();
        List<WindowElement> windows = level.windows().stream()
                .map(window -> window.wallId().equals(wall.id()) ? window.withOffset(Length.ofMillimeters(window.offsetFromStart().toMillimeters() - startShift)) : window)
                .toList();
        return new ResizeResult(walls, doors, windows);
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
        return new ResizeResult(level.walls(), level.doors().stream().map(candidate -> candidate.id().equals(door.id()) ? resized : candidate).toList(), level.windows());
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
        return new ResizeResult(level.walls(), level.doors(), level.windows().stream().map(candidate -> candidate.id().equals(window.id()) ? resized : candidate).toList());
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
        WINDOW_END
    }

    public record EdgeHandle(EdgeHandleKind kind, UUID elementId, UUID hostWallId, PlanPoint position) {
    }

    public record ResizeResult(List<Wall> walls, List<Door> doors, List<WindowElement> windows) {
        public ResizeResult {
            walls = List.copyOf(walls);
            doors = List.copyOf(doors);
            windows = List.copyOf(windows);
        }
    }
}
