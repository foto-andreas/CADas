package de.schrell.cadas.application.view;

public final class ThreeDSceneFitService {

    public double calculateDistance(
            ThreeDSceneBounds sceneBounds,
            CameraPose cameraPose,
            double viewportWidth,
            double viewportHeight,
            double verticalFieldOfViewDegrees,
            double padding,
            double safetyMargin,
            double minimumDistance,
            double maximumDistance
    ) {
        if (sceneBounds == null || !sceneBounds.hasContent()) {
            return minimumDistance;
        }

        double safeViewportWidth = Math.max(1.0, viewportWidth);
        double safeViewportHeight = Math.max(1.0, viewportHeight);
        double aspectRatio = safeViewportWidth / safeViewportHeight;
        double halfVerticalTangent = Math.tan(Math.toRadians(verticalFieldOfViewDegrees / 2.0));
        double halfHorizontalTangent = halfVerticalTangent * aspectRatio;
        double halfSpanX = sceneBounds.spanX() / 2.0;
        double halfSpanY = sceneBounds.spanY() / 2.0;
        double halfSpanZ = sceneBounds.spanZ() / 2.0;
        double azimuthRadians = Math.toRadians(-cameraPose.azimuthDegrees());
        double elevationRadians = Math.toRadians(-cameraPose.elevationDegrees());
        double cosAzimuth = Math.cos(azimuthRadians);
        double sinAzimuth = Math.sin(azimuthRadians);
        double cosElevation = Math.cos(elevationRadians);
        double sinElevation = Math.sin(elevationRadians);

        double requiredDistance = minimumDistance;
        for (double xSign : new double[]{-1.0, 1.0}) {
            for (double ySign : new double[]{-1.0, 1.0}) {
                for (double zSign : new double[]{-1.0, 1.0}) {
                    double x = xSign * halfSpanX;
                    double y = ySign * halfSpanY;
                    double z = -zSign * halfSpanZ;

                    double rotatedX = (x * cosAzimuth) + (z * sinAzimuth);
                    double rotatedZAfterY = (-x * sinAzimuth) + (z * cosAzimuth);
                    double rotatedY = (y * cosElevation) - (rotatedZAfterY * sinElevation);
                    double rotatedZ = (y * sinElevation) + (rotatedZAfterY * cosElevation);

                    double horizontalDistance = (Math.abs(rotatedX) * padding / halfHorizontalTangent) - rotatedZ;
                    double verticalDistance = (Math.abs(rotatedY) * padding / halfVerticalTangent) - rotatedZ;
                    requiredDistance = Math.max(requiredDistance, Math.max(horizontalDistance, verticalDistance));
                }
            }
        }

        double paddedDistance = requiredDistance + safetyMargin;
        return Math.max(minimumDistance, Math.min(maximumDistance, paddedDistance));
    }
}
