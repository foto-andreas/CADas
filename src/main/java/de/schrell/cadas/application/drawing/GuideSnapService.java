package de.schrell.cadas.application.drawing;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Wall;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GuideSnapService {

    private static final double EPSILON = 0.001;

    public PlanPoint snapPoint(PlanPoint point, GuideSnapTargets targets, Length tolerance) {
        double x = nearestCoordinate(point.xMillimeters(), targets.verticalGuides(), tolerance).orElse(point.xMillimeters());
        double y = nearestCoordinate(point.yMillimeters(), targets.horizontalGuides(), tolerance).orElse(point.yMillimeters());
        return new PlanPoint(x, y);
    }

    public Length snapOpeningOffset(
            Wall wall,
            Length rawOffset,
            Length openingWidth,
            GuideSnapTargets targets,
            Length tolerance
    ) {
        double wallLength = wall.axis().length().toMillimeters();
        double width = openingWidth.toMillimeters();
        double offset = clamp(rawOffset.toMillimeters(), 0.0, Math.max(0.0, wallLength - width));
        Direction direction = direction(wall.axis());
        Optional<Double> bestDelta = java.util.stream.Stream.concat(
                        targets.verticalGuides().stream()
                                .filter(ignored -> Math.abs(direction.x()) > EPSILON)
                                .map(guide -> (guide - wall.axis().start().xMillimeters()) / direction.x()),
                        targets.horizontalGuides().stream()
                                .filter(ignored -> Math.abs(direction.y()) > EPSILON)
                                .map(guide -> (guide - wall.axis().start().yMillimeters()) / direction.y())
                )
                .flatMap(guideDistance -> java.util.stream.Stream.of(
                        guideDistance - offset,
                        guideDistance - (offset + width / 2.0),
                        guideDistance - (offset + width)
                ))
                .filter(delta -> Math.abs(delta) <= tolerance.toMillimeters())
                .min(Comparator.comparingDouble(Math::abs));
        double snappedOffset = bestDelta.map(delta -> offset + delta).orElse(offset);
        return Length.ofMillimeters(clamp(snappedOffset, 0.0, Math.max(0.0, wallLength - width)));
    }

    public PlanSegment snapWallSegment(
            PlanSegment segment,
            Length wallThickness,
            GuideSnapTargets targets,
            Length tolerance
    ) {
        double deltaX = segment.end().xMillimeters() - segment.start().xMillimeters();
        double deltaY = segment.end().yMillimeters() - segment.start().yMillimeters();
        double halfThickness = wallThickness.toMillimeters() / 2.0;
        if (Math.abs(deltaY) <= EPSILON && Math.abs(deltaX) > EPSILON) {
            double centerY = snapCenterOrEdge(segment.start().yMillimeters(), halfThickness, targets.horizontalGuides(), tolerance);
            double endX = nearestCoordinate(segment.end().xMillimeters(), targets.verticalGuides(), tolerance).orElse(segment.end().xMillimeters());
            return new PlanSegment(
                    new PlanPoint(segment.start().xMillimeters(), centerY),
                    new PlanPoint(endX, centerY)
            );
        }
        if (Math.abs(deltaX) <= EPSILON && Math.abs(deltaY) > EPSILON) {
            double centerX = snapCenterOrEdge(segment.start().xMillimeters(), halfThickness, targets.verticalGuides(), tolerance);
            double endY = nearestCoordinate(segment.end().yMillimeters(), targets.horizontalGuides(), tolerance).orElse(segment.end().yMillimeters());
            return new PlanSegment(
                    new PlanPoint(centerX, segment.start().yMillimeters()),
                    new PlanPoint(centerX, endY)
            );
        }
        return new PlanSegment(
                segment.start(),
                snapPoint(segment.end(), targets, tolerance)
        );
    }

    public Translation snapWallTranslation(
            List<Wall> walls,
            double deltaX,
            double deltaY,
            GuideSnapTargets targets,
            Length tolerance
    ) {
        List<PlanPoint> snapPoints = walls.stream().flatMap(wall -> wallSnapPoints(wall).stream()).toList();
        double snappedDeltaX = nearestTranslation(deltaX, snapPoints.stream().map(PlanPoint::xMillimeters).toList(), targets.verticalGuides(), tolerance)
                .orElse(deltaX);
        double snappedDeltaY = nearestTranslation(deltaY, snapPoints.stream().map(PlanPoint::yMillimeters).toList(), targets.horizontalGuides(), tolerance)
                .orElse(deltaY);
        return new Translation(snappedDeltaX, snappedDeltaY);
    }

    private List<PlanPoint> wallSnapPoints(Wall wall) {
        Direction tangent = direction(wall.axis());
        double normalX = -tangent.y();
        double normalY = tangent.x();
        double halfThickness = wall.thickness().toMillimeters() / 2.0;
        List<PlanPoint> points = new ArrayList<>();
        points.add(wall.axis().start());
        points.add(wall.axis().end());
        for (PlanPoint endpoint : List.of(wall.axis().start(), wall.axis().end())) {
            points.add(new PlanPoint(endpoint.xMillimeters() + normalX * halfThickness, endpoint.yMillimeters() + normalY * halfThickness));
            points.add(new PlanPoint(endpoint.xMillimeters() - normalX * halfThickness, endpoint.yMillimeters() - normalY * halfThickness));
        }
        return points;
    }

    private Optional<Double> nearestTranslation(
            double rawDelta,
            List<Double> featureCoordinates,
            List<Double> guides,
            Length tolerance
    ) {
        return featureCoordinates.stream()
                .flatMap(feature -> guides.stream().map(guide -> rawDelta + guide - (feature + rawDelta)))
                .filter(candidate -> Math.abs(candidate - rawDelta) <= tolerance.toMillimeters())
                .min(Comparator.comparingDouble(candidate -> Math.abs(candidate - rawDelta)));
    }

    private double snapCenterOrEdge(double center, double halfThickness, java.util.List<Double> guides, Length tolerance) {
        return guides.stream()
                .flatMap(guide -> java.util.stream.Stream.of(guide, guide - halfThickness, guide + halfThickness))
                .filter(candidate -> Math.abs(candidate - center) <= tolerance.toMillimeters())
                .min(Comparator.comparingDouble(candidate -> Math.abs(candidate - center)))
                .orElse(center);
    }

    private Optional<Double> nearestCoordinate(double coordinate, java.util.List<Double> guides, Length tolerance) {
        return guides.stream()
                .filter(guide -> Math.abs(guide - coordinate) <= tolerance.toMillimeters())
                .min(Comparator.comparingDouble(guide -> Math.abs(guide - coordinate)));
    }

    private Direction direction(PlanSegment segment) {
        double length = Math.max(EPSILON, segment.length().toMillimeters());
        return new Direction(
                (segment.end().xMillimeters() - segment.start().xMillimeters()) / length,
                (segment.end().yMillimeters() - segment.start().yMillimeters()) / length
        );
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record Direction(double x, double y) {
    }

    public record Translation(double deltaX, double deltaY) {
    }
}
