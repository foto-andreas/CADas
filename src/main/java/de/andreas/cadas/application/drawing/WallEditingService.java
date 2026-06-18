package de.andreas.cadas.application.drawing;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class WallEditingService {

    public Optional<WallEndpointSelection> findConnectedEndpoint(List<Wall> walls, PlanPoint clickPoint, Length tolerance) {
        return walls.stream()
                .flatMap(wall -> java.util.stream.Stream.of(wall.axis().start(), wall.axis().end()))
                .filter(endpoint -> endpoint.distanceTo(clickPoint).compareTo(tolerance) <= 0)
                .min(Comparator.comparingDouble(endpoint -> endpoint.distanceTo(clickPoint).toMillimeters()))
                .map(endpoint -> selectionForPoint(walls, endpoint));
    }

    public List<Wall> moveEndpointGroup(List<Wall> walls, WallEndpointSelection selection, PlanPoint newPoint) {
        return moveEndpointGroup(walls, selection, newPoint, false);
    }

    public List<Wall> moveEndpointGroup(List<Wall> walls, WallEndpointSelection selection, PlanPoint newPoint, boolean keepOrthogonal) {
        if (keepOrthogonal) {
            return moveEndpointGroupOrthogonally(walls, selection, newPoint);
        }
        List<Wall> updatedWalls = new ArrayList<>();
        for (Wall wall : walls) {
            PlanSegment axis = wall.axis();
            if (selection.startWallIds().contains(wall.id())) {
                axis = new PlanSegment(newPoint, axis.end());
            }
            if (selection.endWallIds().contains(wall.id())) {
                axis = new PlanSegment(axis.start(), newPoint);
            }
            updatedWalls.add(new Wall(wall.id(), axis, wall.thickness(), wall.height(), wall.startHeight(), wall.endHeight()));
        }
        return updatedWalls;
    }

    private List<Wall> moveEndpointGroupOrthogonally(List<Wall> walls, WallEndpointSelection selection, PlanPoint newPoint) {
        Map<PlanPoint, MutablePoint> points = new LinkedHashMap<>();
        for (Wall wall : walls) {
            points.computeIfAbsent(wall.axis().start(), MutablePoint::new);
            points.computeIfAbsent(wall.axis().end(), MutablePoint::new);
        }
        MutablePoint selectedPoint = points.get(selection.anchorPoint());
        if (selectedPoint == null) {
            return moveEndpointGroup(walls, selection, newPoint, false);
        }
        selectedPoint.setX(newPoint.xMillimeters());
        selectedPoint.setY(newPoint.yMillimeters());

        boolean changed;
        do {
            changed = false;
            for (Wall wall : walls) {
                MutablePoint start = points.get(wall.axis().start());
                MutablePoint end = points.get(wall.axis().end());
                double deltaX = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
                double deltaY = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
                if (isNearlyHorizontal(deltaX, deltaY)) {
                    changed |= propagateY(start, end);
                } else if (isNearlyVertical(deltaX, deltaY)) {
                    changed |= propagateX(start, end);
                }
            }
        } while (changed);

        return walls.stream()
                .map(wall -> new Wall(
                        wall.id(),
                        new PlanSegment(points.get(wall.axis().start()).toPlanPoint(), points.get(wall.axis().end()).toPlanPoint()),
                        wall.thickness(),
                        wall.height(),
                        wall.startHeight(),
                        wall.endHeight()
                ))
                .toList();
    }

    private boolean propagateX(MutablePoint first, MutablePoint second) {
        if (first.xChanged && !second.xChanged) {
            return second.setX(first.x);
        }
        if (second.xChanged && !first.xChanged) {
            return first.setX(second.x);
        }
        return false;
    }

    private boolean propagateY(MutablePoint first, MutablePoint second) {
        if (first.yChanged && !second.yChanged) {
            return second.setY(first.y);
        }
        if (second.yChanged && !first.yChanged) {
            return first.setY(second.y);
        }
        return false;
    }

    private boolean isNearlyHorizontal(double deltaX, double deltaY) {
        return Math.abs(deltaX) > 0.001 && Math.abs(deltaY / deltaX) <= Math.tan(Math.toRadians(0.5));
    }

    private boolean isNearlyVertical(double deltaX, double deltaY) {
        return Math.abs(deltaY) > 0.001 && Math.abs(deltaX / deltaY) <= Math.tan(Math.toRadians(0.5));
    }

    private WallEndpointSelection selectionForPoint(List<Wall> walls, PlanPoint anchorPoint) {
        List<UUID> startWallIds = new ArrayList<>();
        List<UUID> endWallIds = new ArrayList<>();
        for (Wall wall : walls) {
            if (samePoint(wall.axis().start(), anchorPoint)) {
                startWallIds.add(wall.id());
            }
            if (samePoint(wall.axis().end(), anchorPoint)) {
                endWallIds.add(wall.id());
            }
        }
        return new WallEndpointSelection(anchorPoint, List.copyOf(startWallIds), List.copyOf(endWallIds));
    }

    private boolean samePoint(PlanPoint first, PlanPoint second) {
        return Math.abs(first.xMillimeters() - second.xMillimeters()) < 0.001
                && Math.abs(first.yMillimeters() - second.yMillimeters()) < 0.001;
    }

    private static final class MutablePoint {

        private double x;
        private double y;
        private boolean xChanged;
        private boolean yChanged;

        private MutablePoint(PlanPoint point) {
            x = point.xMillimeters();
            y = point.yMillimeters();
        }

        private boolean setX(double newX) {
            if (xChanged) {
                return false;
            }
            x = newX;
            xChanged = true;
            return true;
        }

        private boolean setY(double newY) {
            if (yChanged) {
                return false;
            }
            y = newY;
            yChanged = true;
            return true;
        }

        private PlanPoint toPlanPoint() {
            return new PlanPoint(x, y);
        }
    }
}
