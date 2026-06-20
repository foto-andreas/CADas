package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.Objects;
import java.util.UUID;

public record FloorOpening(
        UUID id,
        UUID roomId,
        FloorOpeningShape shape,
        PlanPoint center,
        Length width,
        Length depth
) {

    public FloorOpening {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(roomId, "roomId darf nicht null sein.");
        Objects.requireNonNull(shape, "shape darf nicht null sein.");
        Objects.requireNonNull(center, "center darf nicht null sein.");
        Objects.requireNonNull(width, "width darf nicht null sein.");
        Objects.requireNonNull(depth, "depth darf nicht null sein.");
        if (width.toMillimeters() <= 0.0 || depth.toMillimeters() <= 0.0) {
            throw new IllegalArgumentException("Bodenöffnungsmaße müssen größer als 0 sein.");
        }
        if (shape == FloorOpeningShape.CIRCLE) {
            double diameter = Math.min(width.toMillimeters(), depth.toMillimeters());
            width = Length.ofMillimeters(diameter);
            depth = Length.ofMillimeters(diameter);
        }
    }

    public static FloorOpening create(
            UUID roomId,
            FloorOpeningShape shape,
            PlanPoint center,
            Length width,
            Length depth
    ) {
        return new FloorOpening(UUID.randomUUID(), roomId, shape, center, width, depth);
    }

    public double minXMillimeters() {
        return center.xMillimeters() - width.toMillimeters() / 2.0;
    }

    public double maxXMillimeters() {
        return center.xMillimeters() + width.toMillimeters() / 2.0;
    }

    public double minYMillimeters() {
        return center.yMillimeters() - depth.toMillimeters() / 2.0;
    }

    public double maxYMillimeters() {
        return center.yMillimeters() + depth.toMillimeters() / 2.0;
    }

    public double areaSquareMeters() {
        if (shape == FloorOpeningShape.CIRCLE) {
            double radius = width.toMillimeters() / 2.0;
            return Math.PI * radius * radius / 1_000_000.0;
        }
        return width.toMillimeters() * depth.toMillimeters() / 1_000_000.0;
    }

    public boolean contains(PlanPoint point) {
        if (shape == FloorOpeningShape.CIRCLE) {
            double radius = width.toMillimeters() / 2.0;
            return center.distanceTo(point).toMillimeters() <= radius;
        }
        return point.xMillimeters() >= minXMillimeters()
                && point.xMillimeters() <= maxXMillimeters()
                && point.yMillimeters() >= minYMillimeters()
                && point.yMillimeters() <= maxYMillimeters();
    }

    public FloorOpening withCenter(PlanPoint newCenter) {
        return new FloorOpening(id, roomId, shape, newCenter, width, depth);
    }
}
