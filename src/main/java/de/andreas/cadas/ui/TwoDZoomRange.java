package de.andreas.cadas.ui;

final class TwoDZoomRange {

    private static final double MINIMUM = 0.25;
    private static final double MAXIMUM = 40.0;

    double clamp(double zoom) {
        return Math.max(MINIMUM, Math.min(MAXIMUM, zoom));
    }
}
