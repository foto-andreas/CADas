package de.schrell.cadas.domain.geometry;

import java.util.Locale;

/**
 * Repräsentiert einen auf den Bereich von einschließlich 0 bis ausschließlich 360 Grad normierten Winkel.
 * Negative und übervolle Eingaben bleiben dadurch für Drehungen äquivalent; die interne Einheit ist Grad.
 */
public final class Angle {

    private final double degrees;

    private Angle(double degrees) {
        this.degrees = normalize(degrees);
    }

    public static Angle ofDegrees(double degrees) {
        return new Angle(degrees);
    }

    public double degrees() {
        return degrees;
    }

    public double radians() {
        return Math.toRadians(degrees);
    }

    public String format() {
        return String.format(Locale.GERMAN, "%.1f°", degrees);
    }

    private static double normalize(double degrees) {
        double normalized = degrees % 360.0;
        if (normalized < 0.0) {
            normalized += 360.0;
        }
        return normalized;
    }
}
