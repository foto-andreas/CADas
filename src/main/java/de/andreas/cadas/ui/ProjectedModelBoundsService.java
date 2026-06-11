package de.andreas.cadas.ui;

import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ProjectedModelBoundsService {

    private final ViewProjectionService projectionService = new ViewProjectionService();

    public Optional<ProjectedBounds> bounds(Level level, ViewOrientation orientation) {
        List<ViewProjectionService.ProjectedPoint> projectedPoints = new ArrayList<>();
        level.walls().forEach(wall -> collectWall(projectedPoints, wall, orientation));
        level.rooms().forEach(room -> collectRoom(projectedPoints, room, orientation));
        level.staircases().forEach(staircase -> collectStair(projectedPoints, staircase, orientation));
        level.doors().forEach(door -> collectDoor(projectedPoints, level, door, orientation));
        level.windows().forEach(window -> collectWindow(projectedPoints, level, window, orientation));
        if (projectedPoints.isEmpty()) {
            return Optional.empty();
        }
        double minHorizontal = projectedPoints.stream()
                .mapToDouble(ViewProjectionService.ProjectedPoint::horizontalMillimeters)
                .min()
                .orElse(0.0);
        double maxHorizontal = projectedPoints.stream()
                .mapToDouble(ViewProjectionService.ProjectedPoint::horizontalMillimeters)
                .max()
                .orElse(0.0);
        double minVertical = projectedPoints.stream()
                .mapToDouble(ViewProjectionService.ProjectedPoint::verticalMillimeters)
                .min()
                .orElse(0.0);
        double maxVertical = projectedPoints.stream()
                .mapToDouble(ViewProjectionService.ProjectedPoint::verticalMillimeters)
                .max()
                .orElse(0.0);
        return Optional.of(new ProjectedBounds(minHorizontal, maxHorizontal, minVertical, maxVertical));
    }

    private void collectWall(List<ViewProjectionService.ProjectedPoint> projectedPoints, Wall wall, ViewOrientation orientation) {
        collectPoint(projectedPoints, wall.axis().start(), 0.0, orientation);
        collectPoint(projectedPoints, wall.axis().end(), 0.0, orientation);
        collectPoint(projectedPoints, wall.axis().start(), wall.height().toMillimeters(), orientation);
        collectPoint(projectedPoints, wall.axis().end(), wall.height().toMillimeters(), orientation);
    }

    private void collectRoom(List<ViewProjectionService.ProjectedPoint> projectedPoints, Room room, ViewOrientation orientation) {
        for (PlanPoint point : room.outline()) {
            collectPoint(projectedPoints, point, 0.0, orientation);
            collectPoint(projectedPoints, point, room.ceilingHeightAt(point), orientation);
        }
    }

    private void collectStair(List<ViewProjectionService.ProjectedPoint> projectedPoints, Staircase staircase, ViewOrientation orientation) {
        List<PlanPoint> corners = List.of(
                staircase.pointAtLocalPosition(0, 0),
                staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0),
                staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()),
                staircase.pointAtLocalPosition(0, staircase.heightMillimeters())
        );
        for (PlanPoint point : corners) {
            collectPoint(projectedPoints, point, 0.0, orientation);
            collectPoint(projectedPoints, point, staircase.totalHeight().toMillimeters(), orientation);
        }
    }

    private void collectDoor(List<ViewProjectionService.ProjectedPoint> projectedPoints, Level level, Door door, ViewOrientation orientation) {
        Wall hostWall = level.findWall(door.wallId());
        PlanPoint start = hostWall.axis().pointAt(door.offsetFromStart());
        PlanPoint end = hostWall.axis().pointAt(door.offsetFromStart().add(door.width()));
        collectPoint(projectedPoints, start, door.thresholdHeight().toMillimeters(), orientation);
        collectPoint(projectedPoints, end, door.thresholdHeight().toMillimeters(), orientation);
        collectPoint(projectedPoints, start, door.thresholdHeight().toMillimeters() + door.height().toMillimeters(), orientation);
        collectPoint(projectedPoints, end, door.thresholdHeight().toMillimeters() + door.height().toMillimeters(), orientation);
    }

    private void collectWindow(List<ViewProjectionService.ProjectedPoint> projectedPoints, Level level, WindowElement window, ViewOrientation orientation) {
        Wall hostWall = level.findWall(window.wallId());
        PlanPoint start = hostWall.axis().pointAt(window.offsetFromStart());
        PlanPoint end = hostWall.axis().pointAt(window.offsetFromStart().add(window.width()));
        collectPoint(projectedPoints, start, window.sillHeight().toMillimeters(), orientation);
        collectPoint(projectedPoints, end, window.sillHeight().toMillimeters(), orientation);
        collectPoint(projectedPoints, start, window.sillHeight().toMillimeters() + window.windowHeight().toMillimeters(), orientation);
        collectPoint(projectedPoints, end, window.sillHeight().toMillimeters() + window.windowHeight().toMillimeters(), orientation);
    }

    private void collectPoint(List<ViewProjectionService.ProjectedPoint> projectedPoints, PlanPoint point, double heightMillimeters, ViewOrientation orientation) {
        projectedPoints.add(projectionService.project(point, heightMillimeters, orientation));
    }

    public record ProjectedBounds(
            double minHorizontalMillimeters,
            double maxHorizontalMillimeters,
            double minVerticalMillimeters,
            double maxVerticalMillimeters
    ) {

        public double widthMillimeters() {
            return maxHorizontalMillimeters - minHorizontalMillimeters;
        }

        public double heightMillimeters() {
            return maxVerticalMillimeters - minVerticalMillimeters;
        }

        public double centerHorizontalMillimeters() {
            return (minHorizontalMillimeters + maxHorizontalMillimeters) / 2.0;
        }

        public double centerVerticalMillimeters() {
            return (minVerticalMillimeters + maxVerticalMillimeters) / 2.0;
        }
    }
}
