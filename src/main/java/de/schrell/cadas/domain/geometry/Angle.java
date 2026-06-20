package de.schrell.cadas.domain.geometry;

import java.util.Locale;

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
