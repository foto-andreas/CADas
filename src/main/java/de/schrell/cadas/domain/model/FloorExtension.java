package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record FloorExtension(
        UUID id,
        FloorExtensionType type,
        FloorExtensionPlacement placement,
        PlanPoint firstCorner,
        PlanPoint oppositeCorner,
        Length slabThickness
) {
    public FloorExtension {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(type, "type darf nicht null sein.");
        Objects.requireNonNull(placement, "placement darf nicht null sein.");
        Objects.requireNonNull(firstCorner, "firstCorner darf nicht null sein.");
        Objects.requireNonNull(oppositeCorner, "oppositeCorner darf nicht null sein.");
        Objects.requireNonNull(slabThickness, "slabThickness darf nicht null sein.");
        if (widthMillimeters(firstCorner, oppositeCorner) <= 0 || depthMillimeters(firstCorner, oppositeCorner) <= 0) {
            throw new IllegalArgumentException("Die rechteckige Grundfläche muss Breite und Tiefe besitzen.");
        }
        if (slabThickness.toMillimeters() <= 0) {
            throw new IllegalArgumentException("Die Fußbodendicke muss größer als 0 sein.");
        }
    }

    public static FloorExtension create(FloorExtensionType type, FloorExtensionPlacement placement,
                                        PlanPoint firstCorner, PlanPoint oppositeCorner, Length slabThickness) {
        return new FloorExtension(UUID.randomUUID(), type, placement, firstCorner, oppositeCorner, slabThickness);
    }

    public double minX() {
        return Math.min(firstCorner.xMillimeters(), oppositeCorner.xMillimeters());
    }

    public double minY() {
        return Math.min(firstCorner.yMillimeters(), oppositeCorner.yMillimeters());
    }

    public double maxX() {
        return Math.max(firstCorner.xMillimeters(), oppositeCorner.xMillimeters());
    }

    public double maxY() {
        return Math.max(firstCorner.yMillimeters(), oppositeCorner.yMillimeters());
    }

    public double widthMillimeters() {
        return widthMillimeters(firstCorner, oppositeCorner);
    }

    public double depthMillimeters() {
        return depthMillimeters(firstCorner, oppositeCorner);
    }

    public List<PlanPoint> outline() {
        return List.of(new PlanPoint(minX(), minY()), new PlanPoint(maxX(), minY()),
                new PlanPoint(maxX(), maxY()), new PlanPoint(minX(), maxY()));
    }

    public String surfaceTargetKey() {
        return "floor-extension:" + id;
    }

    private static double widthMillimeters(PlanPoint first, PlanPoint second) {
        return Math.abs(second.xMillimeters() - first.xMillimeters());
    }

    private static double depthMillimeters(PlanPoint first, PlanPoint second) {
        return Math.abs(second.yMillimeters() - first.yMillimeters());
    }
}
