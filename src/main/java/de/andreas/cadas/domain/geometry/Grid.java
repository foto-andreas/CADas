package de.andreas.cadas.domain.geometry;

public record Grid(Length spacing) {

    public Grid {
        if (spacing.compareTo(Length.zero()) <= 0) {
            throw new IllegalArgumentException("Das Raster muss größer als 0 sein.");
        }
    }

    public PlanPoint snap(PlanPoint point) {
        double spacingMillimeters = spacing.toMillimeters();
        double snappedX = Math.round(point.xMillimeters() / spacingMillimeters) * spacingMillimeters;
        double snappedY = Math.round(point.yMillimeters() / spacingMillimeters) * spacingMillimeters;
        return new PlanPoint(snappedX, snappedY);
    }
}

