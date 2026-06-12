package de.andreas.cadas.application.view;

public record ThreeDSceneBounds(
        double centerX,
        double centerY,
        double centerZ,
        double spanX,
        double spanY,
        double spanZ
) {

    public static ThreeDSceneBounds fromExtents(
            double minX,
            double maxX,
            double minY,
            double maxY,
            double minZ,
            double maxZ
    ) {
        return new ThreeDSceneBounds(
                (minX + maxX) / 2.0,
                (minY + maxY) / 2.0,
                (minZ + maxZ) / 2.0,
                Math.max(0.0, maxX - minX),
                Math.max(0.0, maxY - minY),
                Math.max(0.0, maxZ - minZ)
        );
    }

    public static ThreeDSceneBounds fallback(double spanX, double spanY, double spanZ) {
        return new ThreeDSceneBounds(0.0, 0.0, 0.0, spanX, spanY, spanZ);
    }

    public boolean hasContent() {
        return spanX > 0.0 || spanY > 0.0 || spanZ > 0.0;
    }

    public double horizontalSpan() {
        return Math.max(spanX, spanZ);
    }
}
