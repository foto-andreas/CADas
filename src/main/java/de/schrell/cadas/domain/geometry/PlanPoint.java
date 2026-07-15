package de.schrell.cadas.domain.geometry;

/**
 * Zweidimensionaler Weltpunkt eines Grundrisses; beide kartesischen Koordinaten sind Millimeterwerte.
 * Der Typ enthält bewusst keine Darstellungs- oder Zoomkoordinaten.
 */
public record PlanPoint(double xMillimeters, double yMillimeters) {

    public Length distanceTo(PlanPoint other) {
        return Length.ofMillimeters(Math.hypot(other.xMillimeters - xMillimeters, other.yMillimeters - yMillimeters));
    }

    public Angle angleTo(PlanPoint other) {
        return Angle.ofDegrees(Math.toDegrees(Math.atan2(other.yMillimeters - yMillimeters, other.xMillimeters - xMillimeters)));
    }
}
