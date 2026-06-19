package de.andreas.cadas.application.drawing;

import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WallDimensionPlacementService {

    private static final double EPSILON = 0.001;

    public List<PlacedDimension> place(
            Wall wall,
            WallDimensionService.WallDimensions dimensions,
            double renderFactor,
            double baseOffset,
            double stepOffset
    ) {
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
        Map<Double, Integer> sideCounters = new HashMap<>();
        List<PlacedDimension> placements = new ArrayList<>();
        for (WallDimensionService.SideDimension roomDimension : dimensions.roomDimensions()) {
            placements.add(placeDimension(
                    wall,
                    roomDimension,
                    dimensions.exteriorDimension().orElse(null),
                    false,
                    renderFactor,
                    baseOffset,
                    stepOffset,
                    sideCounters
            ));
        }
        dimensions.exteriorDimension().ifPresent(exteriorDimension -> placements.add(placeDimension(
                wall,
                exteriorDimension,
                exteriorDimension,
                true,
                renderFactor,
                baseOffset,
                stepOffset,
                sideCounters
        )));
        return List.copyOf(placements);
    }

    private PlacedDimension placeDimension(
            Wall wall,
            WallDimensionService.SideDimension dimension,
            WallDimensionService.SideDimension referenceDimension,
            boolean exterior,
            double renderFactor,
            double baseOffset,
            double stepOffset,
            Map<Double, Integer> sideCounters
    ) {
        WallDimensionService.SideDimension placementReference = referenceDimension != null ? referenceDimension : dimension;
        double placementSideSign = placementReference.sideSign();
        int stackIndex = sideCounters.getOrDefault(placementSideSign, 0);
        sideCounters.put(placementSideSign, stackIndex + 1);
        double referenceDistance = signedNormalDistance(wall.axis(), placementReference.dimensionSegment());
        double dimensionDistance = signedNormalDistance(wall.axis(), dimension.dimensionSegment());
        double normalOffset = (referenceDistance - dimensionDistance) * renderFactor
                + placementSideSign * (baseOffset + stackIndex * stepOffset);
        double lineDistanceFromAxis = dimensionDistance * renderFactor + normalOffset;
        return new PlacedDimension(dimension, exterior, normalOffset, lineDistanceFromAxis, placementSideSign, stackIndex);
    }

    private double signedNormalDistance(PlanSegment axis, PlanSegment segment) {
        Direction direction = direction(axis);
        PlanPoint midpoint = midpoint(segment);
        double deltaX = midpoint.xMillimeters() - axis.start().xMillimeters();
        double deltaY = midpoint.yMillimeters() - axis.start().yMillimeters();
        return -direction.y() * deltaX + direction.x() * deltaY;
    }

    private PlanPoint midpoint(PlanSegment segment) {
        return new PlanPoint(
                (segment.start().xMillimeters() + segment.end().xMillimeters()) / 2.0,
                (segment.start().yMillimeters() + segment.end().yMillimeters()) / 2.0
        );
    }

    private Direction direction(PlanSegment segment) {
        double length = Math.max(EPSILON, segment.length().toMillimeters());
        return new Direction(
                (segment.end().xMillimeters() - segment.start().xMillimeters()) / length,
                (segment.end().yMillimeters() - segment.start().yMillimeters()) / length
        );
    }

    private record Direction(double x, double y) {
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
            Objects.requireNonNull(dimension, "dimension darf nicht null sein.");
            if (placementSideSign != -1.0 && placementSideSign != 1.0) {
                throw new IllegalArgumentException("placementSideSign muss -1 oder 1 sein.");
            }
            if (stackIndex < 0) {
                throw new IllegalArgumentException("stackIndex darf nicht negativ sein.");
            }
        }
    }
}
