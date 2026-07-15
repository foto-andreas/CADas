package de.schrell.cadas.domain.geometry;

/**
 * Beschreibt das gleichmäßige Zeichenraster durch seinen strikt positiven Abstand.
 * Punkte werden in Millimetern auf das nächstgelegene ganzzahlige Vielfache dieses Abstands gerundet.
 */
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
