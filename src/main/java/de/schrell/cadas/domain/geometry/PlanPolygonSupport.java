package de.schrell.cadas.domain.geometry;

import java.util.List;

/**
 * Bündelt allgemeine Polygon-Prüfungen für den Grundriss.
 */
public final class PlanPolygonSupport {

    private static final double DEFAULT_EPSILON = 0.001;

    private PlanPolygonSupport() {
    }

    public static boolean containsPoint(List<PlanPoint> polygon, PlanPoint point) {
        return containsPoint(polygon, point, DEFAULT_EPSILON);
    }

    public static boolean containsPoint(List<PlanPoint> polygon, PlanPoint point, double epsilon) {
        if (polygon.isEmpty()) {
            return false;
        }
        boolean inside = false;
        int previousIndex = polygon.size() - 1;
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint current = polygon.get(index);
            PlanPoint previous = polygon.get(previousIndex);
            if (pointOnSegment(point, previous, current, epsilon)) {
                return true;
            }
            boolean intersects = (current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters())
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / (previous.yMillimeters() - current.yMillimeters())
                    + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            previousIndex = index;
        }
        return inside;
    }

    private static boolean pointOnSegment(PlanPoint point, PlanPoint start, PlanPoint end, double epsilon) {
        double cross = orientation(start, end, point);
        if (Math.abs(cross) > epsilon) {
            return false;
        }
        return point.xMillimeters() >= Math.min(start.xMillimeters(), end.xMillimeters()) - epsilon
                && point.xMillimeters() <= Math.max(start.xMillimeters(), end.xMillimeters()) + epsilon
                && point.yMillimeters() >= Math.min(start.yMillimeters(), end.yMillimeters()) - epsilon
                && point.yMillimeters() <= Math.max(start.yMillimeters(), end.yMillimeters()) + epsilon;
    }

    private static double orientation(PlanPoint first, PlanPoint second, PlanPoint third) {
        return (second.xMillimeters() - first.xMillimeters()) * (third.yMillimeters() - first.yMillimeters())
                - (second.yMillimeters() - first.yMillimeters()) * (third.xMillimeters() - first.xMillimeters());
    }
}
