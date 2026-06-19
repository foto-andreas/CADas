package de.andreas.cadas.application.drawing;

import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class WallDimensionPlacementService {

    private static final double EPSILON = 0.001;

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
        Envelope envelope = envelope(level, wall);
        double placementSideSign = resolvePlacementSideSign(wall, dimensions, envelope);
        double outsideBoundaryDistance = envelope.boundaryDistance(wall.axis(), placementSideSign);
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
        Envelope envelope = envelope(level, wall);
        double placementSideSign = resolvePlacementSideSign(
                wall,
                new WallDimensionService.WallDimensions(List.of(), java.util.Optional.empty()),
                envelope
        );
        double outsideBoundaryDistance = envelope.boundaryDistance(wall.axis(), placementSideSign);
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
        double normalOffset = lineDistanceFromAxis - dimensionDistance * renderFactor;
        return new PlacedDimension(dimension, exterior, normalOffset, lineDistanceFromAxis, placementSideSign, stackIndex);
    }

    private double resolvePlacementSideSign(
            Wall wall,
            WallDimensionService.WallDimensions dimensions,
            Envelope envelope
    ) {
        // Die Platzierungsseite wird grundsätzlich geometrisch über die Gebäudehülle
        // bestimmt, damit Maße — insbesondere das Außenmaß — immer außerhalb des
        // Gebäudes landen. Die sideSign aus WallDimensionService ist dafür nicht
        // verlässlich, weil sie auf der fehleranfälligen Wandseiten-Klassifikation
        // basiert und das Außenmaß sonst ins Gebäudeinnere gelegt werden kann.
        double positiveDistance = Math.abs(envelope.boundaryDistance(wall.axis(), 1.0));
        double negativeDistance = Math.abs(envelope.boundaryDistance(wall.axis(), -1.0));
        if (Math.abs(positiveDistance - negativeDistance) > EPSILON) {
            // Die Gebäudeaußenseite liegt dort, wo die Hülle am nächsten an der
            // Wandachse ist (betraglich kleinere Ausdehnung = Wand ist nah an
            // der Außenkante).
            return positiveDistance < negativeDistance ? 1.0 : -1.0;
        }
        double centerDistance = signedNormalDistance(wall.axis(), envelope.centerPoint());
        if (Math.abs(centerDistance) > EPSILON) {
            return centerDistance > 0.0 ? -1.0 : 1.0;
        }
        return -1.0;
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

    private record Direction(double x, double y) {
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

        private PlanPoint centerPoint() {
            double sumX = 0.0;
            double sumY = 0.0;
            for (PlanPoint point : points) {
                sumX += point.xMillimeters();
                sumY += point.yMillimeters();
            }
            return new PlanPoint(sumX / Math.max(1, points.size()), sumY / Math.max(1, points.size()));
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

        /**
         * Liefert den Abstandsbetrag der Maßlinie von der Wandachse in Modellkoordinaten.
         * Renderer nutzen {@link #placementSideSign()} um die Richtung in ihren
         * jeweiligen Koordinatensystemen korrekt zu interpretieren.
         */
        public double offsetMagnitude() {
            return Math.abs(normalOffset);
        }
    }
}
