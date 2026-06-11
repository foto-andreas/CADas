package de.andreas.cadas.ui;

import de.andreas.cadas.domain.geometry.PlanPoint;

public final class ViewProjectionService {

    public ProjectedPoint project(PlanPoint point, double heightMillimeters, ViewOrientation orientation) {
        return switch (orientation) {
            case TOP -> new ProjectedPoint(point.xMillimeters(), point.yMillimeters());
            case BOTTOM -> new ProjectedPoint(point.xMillimeters(), -point.yMillimeters());
            case NORTH -> new ProjectedPoint(point.xMillimeters(), -heightMillimeters);
            case SOUTH -> new ProjectedPoint(-point.xMillimeters(), -heightMillimeters);
            case EAST -> new ProjectedPoint(point.yMillimeters(), -heightMillimeters);
            case WEST -> new ProjectedPoint(-point.yMillimeters(), -heightMillimeters);
        };
    }

    public boolean isPlanView(ViewOrientation orientation) {
        return orientation == ViewOrientation.TOP || orientation == ViewOrientation.BOTTOM;
    }

    public record ProjectedPoint(double horizontalMillimeters, double verticalMillimeters) {
    }
}
