package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record HeatingZone(UUID id, String name, List<PlanPoint> outline) {

    public HeatingZone {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        name = Objects.requireNonNull(name, "name darf nicht null sein.").trim();
        Objects.requireNonNull(outline, "outline darf nicht null sein.");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Der Heizbereichsname darf nicht leer sein.");
        }
        if (outline.size() < 3) {
            throw new IllegalArgumentException("Ein Heizbereich braucht mindestens drei Eckpunkte.");
        }
        outline = List.copyOf(outline);
    }

    public static HeatingZone create(String name, List<PlanPoint> outline) {
        return new HeatingZone(UUID.randomUUID(), name, outline);
    }

    public double areaSquareMillimeters() {
        double doubleArea = 0.0;
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint current = outline.get(index);
            PlanPoint next = outline.get((index + 1) % outline.size());
            doubleArea += current.xMillimeters() * next.yMillimeters()
                    - next.xMillimeters() * current.yMillimeters();
        }
        return Math.abs(doubleArea) / 2.0;
    }

    public HeatingZone withOutline(List<PlanPoint> newOutline) {
        return new HeatingZone(id, name, newOutline);
    }
}
