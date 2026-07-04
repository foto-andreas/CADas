package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.model.SurfaceLayoutMode;

final class SurfaceLayoutOffsetCalculator {

    private static final double EPSILON = 0.001;

    private SurfaceLayoutOffsetCalculator() {
    }

    static double rowOffset(
            double tileWidth,
            double tileHeight,
            SurfaceLayoutMode layoutMode,
            double layoutOffset,
            double minimumOffset,
            double minimumEdgeWidth,
            int relativeRow
    ) {
        double offsetReferenceWidth = Math.max(tileWidth, tileHeight);
        double requestedOffset = switch (layoutMode) {
            case NONE -> 0.0;
            case FIXED -> modulo(relativeRow * layoutOffset, offsetReferenceWidth);
            case AUTOMATIC -> relativeRow % 2 == 0 ? 0.0 : offsetReferenceWidth / 2.0;
        };
        return boundedOffset(requestedOffset, tileWidth, minimumOffset, minimumEdgeWidth);
    }

    private static double boundedOffset(double requestedOffset, double tileWidth, double minimumOffset, double minimumEdgeWidth) {
        if (tileWidth <= EPSILON) {
            return 0.0;
        }
        double lowerBound = Math.max(minimumOffset, minimumEdgeWidth);
        if (lowerBound <= EPSILON) {
            return Math.clamp(requestedOffset, 0.0, tileWidth);
        }
        double upperBound = tileWidth - lowerBound;
        if (upperBound < lowerBound) {
            return lowerBound;
        }
        return Math.clamp(requestedOffset, lowerBound, upperBound);
    }

    private static double modulo(double value, double divisor) {
        if (divisor <= EPSILON) {
            return 0.0;
        }
        double remainder = value % divisor;
        return remainder < 0.0 ? remainder + divisor : remainder;
    }
}
