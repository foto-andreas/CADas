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
        int stepCount
) {

    public Staircase {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(stairType, "stairType darf nicht null sein.");
        Objects.requireNonNull(firstCorner, "firstCorner darf nicht null sein.");
        Objects.requireNonNull(oppositeCorner, "oppositeCorner darf nicht null sein.");
        Objects.requireNonNull(totalHeight, "totalHeight darf nicht null sein.");
        if (stepCount <= 0) {
            throw new IllegalArgumentException("stepCount muss größer als 0 sein.");
        }
    }

    public static Staircase create(
            StairType stairType,
            PlanPoint firstCorner,
            PlanPoint oppositeCorner,
            Length totalHeight,
            int stepCount
    ) {
        return new Staircase(UUID.randomUUID(), stairType, firstCorner, oppositeCorner, totalHeight, stepCount);
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
}

