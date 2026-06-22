package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.Objects;
import java.util.UUID;

public record HeatingExclusionArea(
        UUID id,
        UUID roomId,
        String name,
        PlanPoint firstCorner,
        PlanPoint oppositeCorner
) {

    public HeatingExclusionArea {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(roomId, "roomId darf nicht null sein.");
        name = Objects.requireNonNull(name, "name darf nicht null sein.").trim();
        Objects.requireNonNull(firstCorner, "firstCorner darf nicht null sein.");
        Objects.requireNonNull(oppositeCorner, "oppositeCorner darf nicht null sein.");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Der Name der FBH-Sperrfläche darf nicht leer sein.");
        }
        if (widthMillimeters(firstCorner, oppositeCorner) <= 0.0
                || depthMillimeters(firstCorner, oppositeCorner) <= 0.0) {
            throw new IllegalArgumentException("FBH-Sperrflächen müssen eine positive Breite und Tiefe besitzen.");
        }
    }

    public static HeatingExclusionArea create(UUID roomId, String name, PlanPoint firstCorner, PlanPoint oppositeCorner) {
        return new HeatingExclusionArea(UUID.randomUUID(), roomId, name, firstCorner, oppositeCorner);
    }

    public double minXMillimeters() {
        return Math.min(firstCorner.xMillimeters(), oppositeCorner.xMillimeters());
    }

    public double maxXMillimeters() {
        return Math.max(firstCorner.xMillimeters(), oppositeCorner.xMillimeters());
    }

    public double minYMillimeters() {
        return Math.min(firstCorner.yMillimeters(), oppositeCorner.yMillimeters());
    }

    public double maxYMillimeters() {
        return Math.max(firstCorner.yMillimeters(), oppositeCorner.yMillimeters());
    }

    public double widthMillimeters() {
        return maxXMillimeters() - minXMillimeters();
    }

    public double depthMillimeters() {
        return maxYMillimeters() - minYMillimeters();
    }

    public PlanPoint center() {
        return new PlanPoint(
                (minXMillimeters() + maxXMillimeters()) / 2.0,
                (minYMillimeters() + maxYMillimeters()) / 2.0
        );
    }

    public boolean contains(PlanPoint point) {
        return point.xMillimeters() >= minXMillimeters()
                && point.xMillimeters() <= maxXMillimeters()
                && point.yMillimeters() >= minYMillimeters()
                && point.yMillimeters() <= maxYMillimeters();
    }

    public HeatingExclusionArea withCorners(PlanPoint newFirstCorner, PlanPoint newOppositeCorner) {
        return new HeatingExclusionArea(id, roomId, name, newFirstCorner, newOppositeCorner);
    }

    public HeatingExclusionArea withName(String newName) {
        return new HeatingExclusionArea(id, roomId, newName, firstCorner, oppositeCorner);
    }

    public HeatingExclusionArea translated(double deltaXMillimeters, double deltaYMillimeters) {
        return withCorners(
                translate(firstCorner, deltaXMillimeters, deltaYMillimeters),
                translate(oppositeCorner, deltaXMillimeters, deltaYMillimeters)
        );
    }

    private PlanPoint translate(PlanPoint point, double deltaXMillimeters, double deltaYMillimeters) {
        return new PlanPoint(
                point.xMillimeters() + deltaXMillimeters,
                point.yMillimeters() + deltaYMillimeters
        );
    }

    private static double widthMillimeters(PlanPoint firstCorner, PlanPoint oppositeCorner) {
        return Math.abs(oppositeCorner.xMillimeters() - firstCorner.xMillimeters());
    }

    private static double depthMillimeters(PlanPoint firstCorner, PlanPoint oppositeCorner) {
        return Math.abs(oppositeCorner.yMillimeters() - firstCorner.yMillimeters());
    }
}
