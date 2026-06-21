package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.Objects;
import java.util.UUID;

public record RoofWindow(
        UUID id,
        UUID roomId,
        PlanPoint center,
        Length width,
        Length depth,
        SlopedCeilingSide slopeSide
) {

    public RoofWindow {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(roomId, "roomId darf nicht null sein.");
        Objects.requireNonNull(center, "center darf nicht null sein.");
        Objects.requireNonNull(width, "width darf nicht null sein.");
        Objects.requireNonNull(depth, "depth darf nicht null sein.");
        Objects.requireNonNull(slopeSide, "slopeSide darf nicht null sein.");
        if (width.toMillimeters() <= 0.0 || depth.toMillimeters() <= 0.0) {
            throw new IllegalArgumentException("Dachfenstermaße müssen größer als null sein.");
        }
    }

    public static RoofWindow create(
            UUID roomId,
            PlanPoint center,
            Length width,
            Length depth,
            SlopedCeilingSide slopeSide
    ) {
        return new RoofWindow(UUID.randomUUID(), roomId, center, width, depth, slopeSide);
    }

    public boolean contains(PlanPoint point) {
        return Math.abs(point.xMillimeters() - center.xMillimeters()) <= width.toMillimeters() / 2.0
                && Math.abs(point.yMillimeters() - center.yMillimeters()) <= depth.toMillimeters() / 2.0;
    }
}
