package de.andreas.cadas.application.drawing;

import de.andreas.cadas.application.layers.SurfaceLayerEffectService;
import de.andreas.cadas.application.layers.WallSurfaceSideService;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class WallDimensionPlacementService {

    private static final double EPSILON = 0.001;
    private final WallSurfaceSideService wallSurfaceSideService = new WallSurfaceSideService();
    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();

    public List<PlacedDimension> place(
            Level level,
            Wall wall,
            WallDimensionService.WallDimensions dimensions,
            double renderFactor,
            double baseOffset,
            double stepOffset
    ) {
        Objects.requireNonNull(level, "level darf nicht null sein.");
        Objects.requireNonNull(wall, "wall darf nicht null sein.");
        Objects.requireNonNull(dimensions, "dimensions darf nicht null sein.");
        if (renderFactor <= 0.0) {
            throw new IllegalArgumentException("renderFactor muss größer als 0 sein.");
        }
        if (baseOffset < 0.0) {
            throw new IllegalArgumentException("baseOffset darf nicht negativ sein.");
        }
        if (stepOffset < 0.0) {
            throw new IllegalArgumentException("stepOffset darf nicht negativ sein.");
        }
        List<DimensionEntry> entries = new ArrayList<>();
        for (WallDimensionService.SideDimension roomDimension : dimensions.roomDimensions()) {
            entries.add(new DimensionEntry(roomDimension, false));
        }
        dimensions.exteriorDimension().ifPresent(exteriorDimension -> entries.add(new DimensionEntry(exteriorDimension, true)));
        // Identische Maße (gleiche Länge, gleiche Position des Maßsegments) deduplizieren,
        // damit nebeneinander liegende Räume mit identischem Raummaß nicht doppelt gezeichnet werden.
        List<DimensionEntry> deduplicated = deduplicateEntries(entries);
        deduplicated.sort(Comparator
                .comparingDouble((DimensionEntry entry) -> entry.dimension().length().toMillimeters())
                .thenComparing(DimensionEntry::exterior));
        BoundaryPlacement boundaryPlacement = boundaryPlacement(
                level,
                wall,
                deduplicated.stream().map(entry -> entry.dimension().dimensionSegment()).toList()
        );
        double placementSideSign = boundaryPlacement.sideSign();
        double outsideBoundaryDistance = boundaryPlacement.distanceFromAxis();
        List<PlacedDimension> placements = new ArrayList<>();
        for (int index = 0; index < deduplicated.size(); index++) {
            DimensionEntry entry = deduplicated.get(index);
            placements.add(placeDimension(
                    wall,
                    entry.dimension(),
                    entry.exterior(),
                    placementSideSign,
                    outsideBoundaryDistance,
                    renderFactor,
                    baseOffset,
                    stepOffset,
                    index
            ));
        }
        return List.copyOf(placements);
    }

    public PlacedDimension placeAxisDimension(
            Level level,
            Wall wall,
            double renderFactor,
            double baseOffset
    ) {
        Objects.requireNonNull(level, "level darf nicht null sein.");
        Objects.requireNonNull(wall, "wall darf nicht null sein.");
        if (renderFactor <= 0.0) {
            throw new IllegalArgumentException("renderFactor muss größer als 0 sein.");
        }
        if (baseOffset < 0.0) {
            throw new IllegalArgumentException("baseOffset darf nicht negativ sein.");
        }
        BoundaryPlacement boundaryPlacement = boundaryPlacement(level, wall, List.of(wall.axis()));
        double placementSideSign = boundaryPlacement.sideSign();
        double outsideBoundaryDistance = boundaryPlacement.distanceFromAxis();
        double targetLineDistance = outsideBoundaryDistance * renderFactor + placementSideSign * baseOffset;
        return new PlacedDimension(null, false, targetLineDistance, targetLineDistance, placementSideSign, 0);
    }

    private PlacedDimension placeDimension(
            Wall wall,
            WallDimensionService.SideDimension dimension,
            boolean exterior,
            double placementSideSign,
            double outsideBoundaryDistance,
            double renderFactor,
            double baseOffset,
            double stepOffset,
            int stackIndex
    ) {
        // Die Maßlinie liegt außerhalb der Gebäudehülle auf der Platzierungsseite.
        // lineDistanceFromAxis ist der Abstand der Maßlinie von der Wandachse in Modellkoordinaten.
        double lineDistanceFromAxis = outsideBoundaryDistance * renderFactor
                + placementSideSign * (baseOffset + stackIndex * stepOffset);
        // normalOffset ist der Abstand der Maßlinie vom referenzierten Maßsegment
        // (das auf der Innen- oder Außenseite der Wand liegt). Dieser Wert kann
        // betragsmäßig groß sein, wenn das Segment weit von der Außenseite entfernt liegt.
        double dimensionDistance = signedNormalDistance(wall.axis(), dimension.dimensionSegment());
        double orientationSign = orientationSign(wall.axis(), dimension.dimensionSegment());
        double normalOffset = (lineDistanceFromAxis - dimensionDistance * renderFactor) * orientationSign;
        return new PlacedDimension(dimension, exterior, normalOffset, lineDistanceFromAxis, placementSideSign, stackIndex);
    }

    private BoundaryPlacement boundaryPlacement(Level level, Wall wall, List<PlanSegment> measuredSegments) {
        PlanPoint origin = midpoint(wall.axis());
        Direction normal = normal(wall.axis());
        Double nearestDistance = null;
        for (PlanSegment exteriorEdge : exteriorEdges(level)) {
            Double distance = intersectionDistance(origin, normal, exteriorEdge);
            if (distance != null && Math.abs(distance) > EPSILON
                    && (nearestDistance == null || Math.abs(distance) < Math.abs(nearestDistance))) {
                nearestDistance = distance;
            }
        }
        if (nearestDistance != null) {
            double sideSign = Math.copySign(1.0, nearestDistance);
            double clearedDistance = occupiedBoundaryDistance(level, wall.axis(), measuredSegments, sideSign, nearestDistance);
            return new BoundaryPlacement(clearedDistance, sideSign);
        }
        Envelope envelope = envelope(level, wall);
        double positiveDistance = envelope.boundaryDistance(wall.axis(), 1.0);
        double negativeDistance = envelope.boundaryDistance(wall.axis(), -1.0);
        return Math.abs(positiveDistance) < Math.abs(negativeDistance)
                ? new BoundaryPlacement(positiveDistance, 1.0)
                : new BoundaryPlacement(negativeDistance, -1.0);
    }

    private double occupiedBoundaryDistance(
            Level level,
            PlanSegment wallAxis,
            List<PlanSegment> measuredSegments,
            double sideSign,
            double initialDistance
    ) {
        Direction axisDirection = direction(wallAxis);
        double minimumProjection = measuredSegments.stream()
                .flatMap(segment -> java.util.stream.Stream.of(segment.start(), segment.end()))
                .mapToDouble(point -> projection(wallAxis.start(), point, axisDirection))
                .min()
                .orElse(0.0);
        double maximumProjection = measuredSegments.stream()
                .flatMap(segment -> java.util.stream.Stream.of(segment.start(), segment.end()))
                .mapToDouble(point -> projection(wallAxis.start(), point, axisDirection))
                .max()
                .orElse(wallAxis.length().toMillimeters());
        double boundaryDistance = initialDistance;
        for (Wall buildingWall : level.walls()) {
            List<PlanPoint> outline = wallOutline(buildingWall);
            for (int index = 0; index < outline.size(); index++) {
                PlanPoint start = outline.get(index);
                PlanPoint end = outline.get((index + 1) % outline.size());
                for (PlanPoint candidate : clippedEdgePoints(
                        wallAxis,
                        axisDirection,
                        start,
                        end,
                        minimumProjection,
                        maximumProjection
                )) {
                    double distance = signedNormalDistance(wallAxis, candidate);
                    boundaryDistance = sideSign > 0.0
                            ? Math.max(boundaryDistance, distance)
                            : Math.min(boundaryDistance, distance);
                }
            }
        }
        return boundaryDistance;
    }

    private List<PlanPoint> clippedEdgePoints(
            PlanSegment wallAxis,
            Direction axisDirection,
            PlanPoint start,
            PlanPoint end,
            double minimumProjection,
            double maximumProjection
    ) {
        double startProjection = projection(wallAxis.start(), start, axisDirection);
        double endProjection = projection(wallAxis.start(), end, axisDirection);
        List<PlanPoint> points = new ArrayList<>();
        if (within(startProjection, minimumProjection, maximumProjection)) {
            points.add(start);
        }
        if (within(endProjection, minimumProjection, maximumProjection)) {
            points.add(end);
        }
        double projectionDelta = endProjection - startProjection;
        if (Math.abs(projectionDelta) <= EPSILON) {
            return points;
        }
        for (double boundary : new double[]{minimumProjection, maximumProjection}) {
            double ratio = (boundary - startProjection) / projectionDelta;
            if (ratio > EPSILON && ratio < 1.0 - EPSILON) {
                points.add(interpolate(start, end, ratio));
            }
        }
        return points;
    }

    private boolean within(double value, double minimum, double maximum) {
        return value >= minimum - EPSILON && value <= maximum + EPSILON;
    }

    private PlanPoint interpolate(PlanPoint start, PlanPoint end, double ratio) {
        return new PlanPoint(
                start.xMillimeters() + (end.xMillimeters() - start.xMillimeters()) * ratio,
                start.yMillimeters() + (end.yMillimeters() - start.yMillimeters()) * ratio
        );
    }

    private double projection(PlanPoint origin, PlanPoint point, Direction direction) {
        return (point.xMillimeters() - origin.xMillimeters()) * direction.x()
                + (point.yMillimeters() - origin.yMillimeters()) * direction.y();
    }

    private List<PlanPoint> wallOutline(Wall wall) {
        Direction direction = direction(wall.axis());
        Direction normal = normal(wall.axis());
        double halfThickness = wall.thickness().toMillimeters() / 2.0;
        PlanPoint extendedStart = offsetPoint(wall.axis().start(), direction.x(), direction.y(), -halfThickness);
        PlanPoint extendedEnd = offsetPoint(wall.axis().end(), direction.x(), direction.y(), halfThickness);
        return List.of(
                offsetPoint(extendedStart, normal.x(), normal.y(), halfThickness),
                offsetPoint(extendedEnd, normal.x(), normal.y(), halfThickness),
                offsetPoint(extendedEnd, normal.x(), normal.y(), -halfThickness),
                offsetPoint(extendedStart, normal.x(), normal.y(), -halfThickness)
        );
    }

    private List<PlanSegment> exteriorEdges(Level level) {
        List<PlanSegment> edges = new ArrayList<>();
        for (Wall wall : level.walls()) {
            WallSurfaceSideService.WallLayerSides sides = wallSurfaceSideService.resolve(
                    level,
                    wall,
                    SurfaceType.WALL_EXTERIOR,
                    wall.id().toString()
            );
            if (sides.positiveSide() == sides.negativeSide()) {
                continue;
            }
            double sideSign = sides.positiveSide() ? 1.0 : -1.0;
            double offset = wall.thickness().toMillimeters() / 2.0
                    + surfaceLayerEffectService.wallExteriorThicknessMillimeters(level, wall);
            edges.add(shiftedSegment(wall.axis(), offset, sideSign));
        }
        return edges;
    }

    private PlanSegment shiftedSegment(PlanSegment segment, double offset, double sideSign) {
        Direction normal = normal(segment);
        return new PlanSegment(
                offsetPoint(segment.start(), normal.x(), normal.y(), offset * sideSign),
                offsetPoint(segment.end(), normal.x(), normal.y(), offset * sideSign)
        );
    }

    private Double intersectionDistance(PlanPoint origin, Direction rayDirection, PlanSegment segment) {
        double segmentX = segment.end().xMillimeters() - segment.start().xMillimeters();
        double segmentY = segment.end().yMillimeters() - segment.start().yMillimeters();
        double denominator = cross(rayDirection.x(), rayDirection.y(), segmentX, segmentY);
        if (Math.abs(denominator) <= EPSILON) {
            return null;
        }
        double originToStartX = segment.start().xMillimeters() - origin.xMillimeters();
        double originToStartY = segment.start().yMillimeters() - origin.yMillimeters();
        double segmentRatio = cross(originToStartX, originToStartY, rayDirection.x(), rayDirection.y()) / denominator;
        if (segmentRatio < -EPSILON || segmentRatio > 1.0 + EPSILON) {
            return null;
        }
        return cross(originToStartX, originToStartY, segmentX, segmentY) / denominator;
    }

    private double cross(double firstX, double firstY, double secondX, double secondY) {
        return firstX * secondY - firstY * secondX;
    }

    private double orientationSign(PlanSegment wallAxis, PlanSegment dimensionSegment) {
        Direction wallDirection = direction(wallAxis);
        Direction dimensionDirection = direction(dimensionSegment);
        double dotProduct = wallDirection.x() * dimensionDirection.x() + wallDirection.y() * dimensionDirection.y();
        return dotProduct < 0.0 ? -1.0 : 1.0;
    }

    private static double signedNormalDistance(PlanSegment axis, PlanSegment segment) {
        return signedNormalDistance(axis, midpoint(segment));
    }

    private static double signedNormalDistance(PlanSegment axis, PlanPoint point) {
        Direction direction = direction(axis);
        double deltaX = point.xMillimeters() - axis.start().xMillimeters();
        double deltaY = point.yMillimeters() - axis.start().yMillimeters();
        return -direction.y() * deltaX + direction.x() * deltaY;
    }

    private static PlanPoint midpoint(PlanSegment segment) {
        return new PlanPoint(
                (segment.start().xMillimeters() + segment.end().xMillimeters()) / 2.0,
                (segment.start().yMillimeters() + segment.end().yMillimeters()) / 2.0
        );
    }

    private static Direction direction(PlanSegment segment) {
        double length = Math.max(EPSILON, segment.length().toMillimeters());
        return new Direction(
                (segment.end().xMillimeters() - segment.start().xMillimeters()) / length,
                (segment.end().yMillimeters() - segment.start().yMillimeters()) / length
        );
    }

    private static Direction normal(PlanSegment segment) {
        Direction direction = direction(segment);
        return new Direction(-direction.y(), direction.x());
    }

    private record Direction(double x, double y) {
    }

    private record BoundaryPlacement(double distanceFromAxis, double sideSign) {
    }

    private static Envelope envelope(Level level, Wall wall) {
        List<PlanPoint> points = new ArrayList<>();
        appendWallOutline(points, wall);
        for (Wall levelWall : level.walls()) {
            if (!levelWall.id().equals(wall.id())) {
                appendWallOutline(points, levelWall);
            }
        }
        for (Room room : level.rooms()) {
            points.addAll(room.outline());
        }
        if (points.isEmpty()) {
            points.add(wall.axis().start());
            points.add(wall.axis().end());
        }
        return new Envelope(List.copyOf(points));
    }

    private static void appendWallOutline(List<PlanPoint> points, Wall wall) {
        Direction direction = direction(wall.axis());
        double normalX = -direction.y();
        double normalY = direction.x();
        double halfThickness = wall.thickness().toMillimeters() / 2.0;
        points.add(offsetPoint(wall.axis().start(), normalX, normalY, halfThickness));
        points.add(offsetPoint(wall.axis().end(), normalX, normalY, halfThickness));
        points.add(offsetPoint(wall.axis().start(), normalX, normalY, -halfThickness));
        points.add(offsetPoint(wall.axis().end(), normalX, normalY, -halfThickness));
    }

    private static PlanPoint offsetPoint(PlanPoint point, double normalX, double normalY, double distance) {
        return new PlanPoint(
                point.xMillimeters() + normalX * distance,
                point.yMillimeters() + normalY * distance
        );
    }

    private record Envelope(List<PlanPoint> points) {

        private double boundaryDistance(PlanSegment axis, double sideSign) {
            double distance = sideSign > 0.0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            for (PlanPoint point : points) {
                double current = signedNormalDistance(axis, point);
                distance = sideSign > 0.0
                        ? Math.max(distance, current)
                        : Math.min(distance, current);
            }
            return Double.isFinite(distance) ? distance : 0.0;
        }

    }

    /**
     * Entfernt Einträge mit identischem Maßsegment (gleiche Länge und gleiche
     * Start-/Endpunkte), sodass nebeneinander liegende identische Maße nicht
     * doppelt gezeichnet werden. Das Außenmaß hat dabei Vorrang vor Raummaßen
     * mit identischem Segment.
     */
    private List<DimensionEntry> deduplicateEntries(List<DimensionEntry> entries) {
        List<DimensionEntry> result = new ArrayList<>();
        for (DimensionEntry entry : entries) {
            boolean duplicate = result.stream().anyMatch(existing -> segmentsEqual(
                    existing.dimension().dimensionSegment(),
                    entry.dimension().dimensionSegment()
            ) && existing.exterior() == entry.exterior());
            if (!duplicate) {
                result.add(entry);
            }
        }
        return result;
    }

    private boolean segmentsEqual(PlanSegment first, PlanSegment second) {
        return Math.abs(first.length().toMillimeters() - second.length().toMillimeters()) <= EPSILON
                && pointsEqual(first.start(), second.start())
                && pointsEqual(first.end(), second.end());
    }

    private boolean pointsEqual(PlanPoint first, PlanPoint second) {
        return Math.abs(first.xMillimeters() - second.xMillimeters()) <= EPSILON
                && Math.abs(first.yMillimeters() - second.yMillimeters()) <= EPSILON;
    }

    private record DimensionEntry(
            WallDimensionService.SideDimension dimension,
            boolean exterior
    ) {
    }

    public record PlacedDimension(
            WallDimensionService.SideDimension dimension,
            boolean exterior,
            double normalOffset,
            double lineDistanceFromAxis,
            double placementSideSign,
            int stackIndex
    ) {
        public PlacedDimension {
            if (placementSideSign != -1.0 && placementSideSign != 1.0) {
                throw new IllegalArgumentException("placementSideSign muss -1 oder 1 sein.");
            }
            if (stackIndex < 0) {
                throw new IllegalArgumentException("stackIndex darf nicht negativ sein.");
            }
        }

    }
}
