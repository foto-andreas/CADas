package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record HeatingZone(
        UUID id,
        String name,
        List<PlanPoint> outline,
        HeatingLayoutPattern layoutPattern,
        boolean flowInverted
) {

    public HeatingZone {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        name = Objects.requireNonNull(name, "name darf nicht null sein.").trim();
        Objects.requireNonNull(outline, "outline darf nicht null sein.");
        Objects.requireNonNull(layoutPattern, "layoutPattern darf nicht null sein.");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Der Heizbereichsname darf nicht leer sein.");
        }
        if (outline.size() < 3) {
            throw new IllegalArgumentException("Ein Heizbereich braucht mindestens drei Eckpunkte.");
        }
        outline = List.copyOf(outline);
        if (areaSquareMillimeters(outline) < 0.001) {
            throw new IllegalArgumentException("Ein Heizbereich muss eine positive Fläche besitzen.");
        }
    }

    public HeatingZone(UUID id, String name, List<PlanPoint> outline) {
        this(id, name, outline, HeatingLayoutPattern.MEANDER, false);
    }

    public static HeatingZone create(String name, List<PlanPoint> outline) {
        return create(name, outline, HeatingLayoutPattern.MEANDER);
    }

    public static HeatingZone create(String name, List<PlanPoint> outline, HeatingLayoutPattern layoutPattern) {
        return new HeatingZone(UUID.randomUUID(), name, outline, layoutPattern, false);
    }

    public double areaSquareMillimeters() {
        return areaSquareMillimeters(outline);
    }

    private static double areaSquareMillimeters(List<PlanPoint> points) {
        double doubleArea = 0.0;
        for (int index = 0; index < points.size(); index++) {
            PlanPoint current = points.get(index);
            PlanPoint next = points.get((index + 1) % points.size());
            doubleArea += current.xMillimeters() * next.yMillimeters()
                    - next.xMillimeters() * current.yMillimeters();
        }
        return Math.abs(doubleArea) / 2.0;
    }

    public HeatingZone withOutline(List<PlanPoint> newOutline) {
        return new HeatingZone(id, name, newOutline, layoutPattern, flowInverted);
    }

    public HeatingZone withName(String newName) {
        return new HeatingZone(id, newName, outline, layoutPattern, flowInverted);
    }

    public HeatingZone withLayoutPattern(HeatingLayoutPattern newLayoutPattern) {
        return new HeatingZone(id, name, outline, newLayoutPattern, flowInverted);
    }

    public HeatingZone withFlowInverted(boolean newFlowInverted) {
        return new HeatingZone(id, name, outline, layoutPattern, newFlowInverted);
    }
}
