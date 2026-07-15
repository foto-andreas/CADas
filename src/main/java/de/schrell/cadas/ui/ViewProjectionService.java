package de.schrell.cadas.ui;

import de.schrell.cadas.domain.geometry.PlanPoint;

/** Projiziert Grundriss- und Höhenkoordinaten in die lokale Ebene einer orthogonalen 2D-Ansicht. */
public final class ViewProjectionService {

    public static ProjectedPoint project(PlanPoint point, double heightMillimeters, ViewOrientation orientation) {
        return switch (orientation) {
            case TOP -> new ProjectedPoint(point.xMillimeters(), point.yMillimeters());
            case BOTTOM -> new ProjectedPoint(point.xMillimeters(), -point.yMillimeters());
            case NORTH -> new ProjectedPoint(point.xMillimeters(), -heightMillimeters);
            case SOUTH -> new ProjectedPoint(-point.xMillimeters(), -heightMillimeters);
            case EAST -> new ProjectedPoint(point.yMillimeters(), -heightMillimeters);
            case WEST -> new ProjectedPoint(-point.yMillimeters(), -heightMillimeters);
        };
    }

    public static boolean isPlanView(ViewOrientation orientation) {
        return orientation == ViewOrientation.TOP || orientation == ViewOrientation.BOTTOM;
    }

    public record ProjectedPoint(double horizontalMillimeters, double verticalMillimeters) {
    }
}
