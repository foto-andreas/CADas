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
        boolean flowInverted,
        PlanPoint supplyConnectionPoint,
        PlanPoint returnConnectionPoint
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
        supplyConnectionPoint = connectionOrDefault(supplyConnectionPoint, outline, true);
        returnConnectionPoint = connectionOrDefault(returnConnectionPoint, outline, false);
        if (areaSquareMillimeters(outline) < 0.001) {
            throw new IllegalArgumentException("Ein Heizbereich muss eine positive Fläche besitzen.");
        }
        if (!isPointOnBoundary(supplyConnectionPoint, outline)) {
            throw new IllegalArgumentException("Der Vorlaufanschluss muss auf dem Rand des Heizbereichs liegen.");
        }
        if (!isPointOnBoundary(returnConnectionPoint, outline)) {
            throw new IllegalArgumentException("Der Rücklaufanschluss muss auf dem Rand des Heizbereichs liegen.");
        }
    }

    public HeatingZone(
            UUID id,
            String name,
            List<PlanPoint> outline,
            HeatingLayoutPattern layoutPattern,
            boolean flowInverted
    ) {
        this(id, name, outline, layoutPattern, flowInverted, null, null);
    }

    public HeatingZone(UUID id, String name, List<PlanPoint> outline) {
        this(id, name, outline, HeatingLayoutPattern.SPIRAL, false);
    }

    public static HeatingZone create(String name, List<PlanPoint> outline) {
        return create(name, outline, HeatingLayoutPattern.SPIRAL);
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
        return new HeatingZone(
                id, name, newOutline, layoutPattern, flowInverted,
                isPointOnBoundary(supplyConnectionPoint, newOutline) ? supplyConnectionPoint : null,
                isPointOnBoundary(returnConnectionPoint, newOutline) ? returnConnectionPoint : null
        );
    }

    public HeatingZone withName(String newName) {
        return new HeatingZone(id, newName, outline, layoutPattern, flowInverted, supplyConnectionPoint, returnConnectionPoint);
    }

    public HeatingZone withLayoutPattern(HeatingLayoutPattern newLayoutPattern) {
        return new HeatingZone(id, name, outline, newLayoutPattern, flowInverted, supplyConnectionPoint, returnConnectionPoint);
    }

    public HeatingZone withFlowInverted(boolean newFlowInverted) {
        return new HeatingZone(id, name, outline, layoutPattern, newFlowInverted, supplyConnectionPoint, returnConnectionPoint);
    }

    public HeatingZone withSupplyConnectionPoint(PlanPoint point) {
        return new HeatingZone(id, name, outline, layoutPattern, flowInverted, point, returnConnectionPoint);
    }

    public HeatingZone withReturnConnectionPoint(PlanPoint point) {
        return new HeatingZone(id, name, outline, layoutPattern, flowInverted, supplyConnectionPoint, point);
    }

    public boolean hasCustomConnectionPoints() {
        return !samePoint(supplyConnectionPoint, defaultConnection(outline, true))
                || !samePoint(returnConnectionPoint, defaultConnection(outline, false));
    }

    private static PlanPoint connectionOrDefault(PlanPoint point, List<PlanPoint> outline, boolean supply) {
        if (point != null) {
            return point;
        }
        return defaultConnection(outline, supply);
    }

    private static PlanPoint defaultConnection(List<PlanPoint> outline, boolean supply) {
        int startIndex = supply ? 0 : 1 % outline.size();
        int endIndex = supply ? outline.size() - 1 : 2 % outline.size();
        PlanPoint start = outline.get(startIndex);
        PlanPoint end = outline.get(endIndex);
        return new PlanPoint(
                (start.xMillimeters() + end.xMillimeters()) / 2.0,
                (start.yMillimeters() + end.yMillimeters()) / 2.0
        );
    }

    private static boolean samePoint(PlanPoint first, PlanPoint second) {
        return Math.abs(first.xMillimeters() - second.xMillimeters()) <= 0.001
                && Math.abs(first.yMillimeters() - second.yMillimeters()) <= 0.001;
    }

    private static boolean isPointOnBoundary(PlanPoint point, List<PlanPoint> outline) {
        for (int index = 0; index < outline.size(); index++) {
            if (pointOnSegment(point, outline.get(index), outline.get((index + 1) % outline.size()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean pointOnSegment(PlanPoint point, PlanPoint start, PlanPoint end) {
        double cross = (end.xMillimeters() - start.xMillimeters()) * (point.yMillimeters() - start.yMillimeters())
                - (end.yMillimeters() - start.yMillimeters()) * (point.xMillimeters() - start.xMillimeters());
        if (Math.abs(cross) > 0.001) {
            return false;
        }
        return point.xMillimeters() >= Math.min(start.xMillimeters(), end.xMillimeters()) - 0.001
                && point.xMillimeters() <= Math.max(start.xMillimeters(), end.xMillimeters()) + 0.001
                && point.yMillimeters() >= Math.min(start.yMillimeters(), end.yMillimeters()) - 0.001
                && point.yMillimeters() <= Math.max(start.yMillimeters(), end.yMillimeters()) + 0.001;
    }
}
