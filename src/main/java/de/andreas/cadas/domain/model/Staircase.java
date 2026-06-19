package de.andreas.cadas.domain.model;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;

import java.util.Objects;
import java.util.UUID;

public record Staircase(
        UUID id,
        StairType stairType,
        PlanPoint firstCorner,
        PlanPoint oppositeCorner,
        Length totalHeight,
        int stepCount,
        int rotationQuarterTurns,
        Length startLandingWidth,
        Length endLandingWidth
) {

    public Staircase {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(stairType, "stairType darf nicht null sein.");
        Objects.requireNonNull(firstCorner, "firstCorner darf nicht null sein.");
        Objects.requireNonNull(oppositeCorner, "oppositeCorner darf nicht null sein.");
        Objects.requireNonNull(totalHeight, "totalHeight darf nicht null sein.");
        Objects.requireNonNull(startLandingWidth, "startLandingWidth darf nicht null sein.");
        Objects.requireNonNull(endLandingWidth, "endLandingWidth darf nicht null sein.");
        if (stepCount <= 0) {
            throw new IllegalArgumentException("stepCount muss größer als 0 sein.");
        }
        if (rotationQuarterTurns < 0 || rotationQuarterTurns > 3) {
            throw new IllegalArgumentException("rotationQuarterTurns muss zwischen 0 und 3 liegen.");
        }
        if (startLandingWidth.toMillimeters() < 0 || endLandingWidth.toMillimeters() < 0) {
            throw new IllegalArgumentException("Absatzbreiten dürfen nicht negativ sein.");
        }
        double runLength = Math.abs(oppositeCorner.yMillimeters() - firstCorner.yMillimeters());
        if (startLandingWidth.toMillimeters() + endLandingWidth.toMillimeters() >= runLength) {
            throw new IllegalArgumentException("Zwischen den Absätzen muss Platz für mindestens eine Stufe bleiben.");
        }
        int configuredLandingCount = (startLandingWidth.toMillimeters() > 0 ? 1 : 0)
                + (endLandingWidth.toMillimeters() > 0 ? 1 : 0);
        if (stepCount <= configuredLandingCount) {
            throw new IllegalArgumentException("Die Stufenanzahl muss die Absätze und mindestens eine Stufe enthalten.");
        }
    }

    public Staircase(
            UUID id,
            StairType stairType,
            PlanPoint firstCorner,
            PlanPoint oppositeCorner,
            Length totalHeight,
            int stepCount,
            int rotationQuarterTurns
    ) {
        this(id, stairType, firstCorner, oppositeCorner, totalHeight, stepCount, rotationQuarterTurns,
                Length.ofMillimeters(0), Length.ofMillimeters(0));
    }

    public static Staircase create(
            StairType stairType,
            PlanPoint firstCorner,
            PlanPoint oppositeCorner,
            Length totalHeight,
            int stepCount
    ) {
        return new Staircase(UUID.randomUUID(), stairType, firstCorner, oppositeCorner, totalHeight, stepCount, 0);
    }

    public static Staircase create(
            StairType stairType,
            PlanPoint firstCorner,
            PlanPoint oppositeCorner,
            Length totalHeight,
            int stepCount,
            Length startLandingWidth,
            Length endLandingWidth
    ) {
        return new Staircase(UUID.randomUUID(), stairType, firstCorner, oppositeCorner, totalHeight, stepCount, 0,
                startLandingWidth, endLandingWidth);
    }

    public double minX() {
        return Math.min(firstCorner.xMillimeters(), oppositeCorner.xMillimeters());
    }

    public double maxX() {
        return Math.max(firstCorner.xMillimeters(), oppositeCorner.xMillimeters());
    }

    public double minY() {
        return Math.min(firstCorner.yMillimeters(), oppositeCorner.yMillimeters());
    }

    public double maxY() {
        return Math.max(firstCorner.yMillimeters(), oppositeCorner.yMillimeters());
    }

    public double widthMillimeters() {
        return maxX() - minX();
    }

    public double heightMillimeters() {
        return maxY() - minY();
    }

    public int landingCount() {
        int count = 0;
        if (startLandingWidth.toMillimeters() > 0) {
            count++;
        }
        if (endLandingWidth.toMillimeters() > 0) {
            count++;
        }
        return count;
    }

    public int regularStepCount() {
        return stepCount - landingCount();
    }

    public PlanPoint pointAtLocalPosition(double localX, double localY) {
        double width = widthMillimeters();
        double height = heightMillimeters();
        return switch (rotationQuarterTurns) {
            case 1 -> new PlanPoint(minX() + height - localY, minY() + localX);
            case 2 -> new PlanPoint(minX() + width - localX, minY() + height - localY);
            case 3 -> new PlanPoint(minX() + localY, minY() + width - localX);
            default -> new PlanPoint(minX() + localX, minY() + localY);
        };
    }

    public double orientedWidth(double localWidth, double localHeight) {
        return rotationQuarterTurns % 2 == 0 ? localWidth : localHeight;
    }

    public double orientedHeight(double localWidth, double localHeight) {
        return rotationQuarterTurns % 2 == 0 ? localHeight : localWidth;
    }

    public Staircase rotateClockwise() {
        return new Staircase(id, stairType, firstCorner, oppositeCorner, totalHeight, stepCount,
                (rotationQuarterTurns + 1) % 4, startLandingWidth, endLandingWidth);
    }

    public Staircase rotateCounterClockwise() {
        return new Staircase(id, stairType, firstCorner, oppositeCorner, totalHeight, stepCount,
                (rotationQuarterTurns + 3) % 4, startLandingWidth, endLandingWidth);
    }
}
