package de.schrell.cadas.ui;

import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import de.schrell.cadas.domain.model.SurfaceLayoutDirection;

final class CadWorkbenchSurfaceLayoutSupport {

    private CadWorkbenchSurfaceLayoutSupport() {
    }

    static SurfaceLayoutAnchor normalizedSurfaceLayoutAnchor(SurfaceLayoutAnchor anchor) {
        if (anchor != null && anchor != SurfaceLayoutAnchor.AUTO) {
            return anchor;
        }
        return SurfaceLayoutAnchor.MIN_X_MIN_Y;
    }

    static String formatSurfaceLayoutCorner(SurfaceLayoutAnchor anchor) {
        return switch (anchor == null ? SurfaceLayoutAnchor.MIN_X_MIN_Y : anchor) {
            case AUTO -> "Automatisch";
            case MIN_X_MIN_Y -> "Unten links";
            case MAX_X_MIN_Y -> "Unten rechts";
            case MAX_X_MAX_Y -> "Oben rechts";
            case MIN_X_MAX_Y -> "Oben links";
        };
    }

    static SurfaceLayoutDirection surfaceLayoutSelectionDirection(
            SurfaceLayoutAnchor anchor,
            boolean rotatedQuarterTurn
    ) {
        return rotatedQuarterTurn == surfaceLayoutCornerParity(anchor)
                ? SurfaceLayoutDirection.LEFT_TO_RIGHT
                : SurfaceLayoutDirection.RIGHT_TO_LEFT;
    }

    static boolean surfaceLayoutRotatedQuarterTurn(SurfaceLayoutAnchor anchor, SurfaceLayoutDirection direction) {
        boolean clockwiseDirection = direction != SurfaceLayoutDirection.RIGHT_TO_LEFT;
        return clockwiseDirection
                ? surfaceLayoutCornerParity(anchor)
                : !surfaceLayoutCornerParity(anchor);
    }

    static boolean startsAtMaximumX(SurfaceLayoutAnchor anchor) {
        return anchor == SurfaceLayoutAnchor.MAX_X_MIN_Y || anchor == SurfaceLayoutAnchor.MAX_X_MAX_Y;
    }

    static boolean startsAtMaximumY(SurfaceLayoutAnchor anchor) {
        return anchor == SurfaceLayoutAnchor.MAX_X_MAX_Y || anchor == SurfaceLayoutAnchor.MIN_X_MAX_Y;
    }

    static String formatSurfaceLayoutDirection(SurfaceLayoutAnchor anchor, boolean rotatedQuarterTurn) {
        return surfaceLayoutSelectionDirection(anchor, rotatedQuarterTurn).label();
    }

    private static boolean surfaceLayoutCornerParity(SurfaceLayoutAnchor anchor) {
        SurfaceLayoutAnchor manualAnchor = normalizedSurfaceLayoutAnchor(anchor);
        return startsAtMaximumX(manualAnchor) ^ startsAtMaximumY(manualAnchor);
    }
}
