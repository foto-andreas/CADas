package de.andreas.cadas.domain.model;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Room(
        UUID id,
        String name,
        List<PlanPoint> outline,
        Length roomHeight,
        Length floorThickness,
        Length ceilingThickness
) {

    public Room {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(name, "name darf nicht null sein.");
        Objects.requireNonNull(outline, "outline darf nicht null sein.");
        Objects.requireNonNull(roomHeight, "roomHeight darf nicht null sein.");
        Objects.requireNonNull(floorThickness, "floorThickness darf nicht null sein.");
        Objects.requireNonNull(ceilingThickness, "ceilingThickness darf nicht null sein.");
        if (outline.size() < 3) {
            throw new IllegalArgumentException("Ein Raum benötigt mindestens drei Eckpunkte.");
        }
        outline = List.copyOf(outline);
    }

    public static Room rectangular(
            String name,
            PlanPoint firstCorner,
            PlanPoint oppositeCorner,
            Length roomHeight,
            Length floorThickness,
            Length ceilingThickness
    ) {
        double minX = Math.min(firstCorner.xMillimeters(), oppositeCorner.xMillimeters());
        double maxX = Math.max(firstCorner.xMillimeters(), oppositeCorner.xMillimeters());
        double minY = Math.min(firstCorner.yMillimeters(), oppositeCorner.yMillimeters());
        double maxY = Math.max(firstCorner.yMillimeters(), oppositeCorner.yMillimeters());
        return new Room(
                UUID.randomUUID(),
                name,
                List.of(
                        new PlanPoint(minX, minY),
                        new PlanPoint(maxX, minY),
                        new PlanPoint(maxX, maxY),
                        new PlanPoint(minX, maxY)
                ),
                roomHeight,
                floorThickness,
                ceilingThickness
        );
    }

    public Length area() {
        double areaDouble = 0.0;
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint current = outline.get(index);
            PlanPoint next = outline.get((index + 1) % outline.size());
            areaDouble += current.xMillimeters() * next.yMillimeters() - next.xMillimeters() * current.yMillimeters();
        }
        return Length.ofMillimeters(Math.abs(areaDouble) / 2.0);
    }

    public double areaSquareMeters() {
        return area().toMillimeters() / 1_000_000.0;
    }

    public double volumeCubicMeters() {
        return areaSquareMeters() * roomHeight.toMillimeters() / 1000.0;
    }

    public PlanPoint centerPoint() {
        double sumX = 0.0;
        double sumY = 0.0;
        for (PlanPoint point : outline) {
            sumX += point.xMillimeters();
            sumY += point.yMillimeters();
        }
        return new PlanPoint(sumX / outline.size(), sumY / outline.size());
    }
}

