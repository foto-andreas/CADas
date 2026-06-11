package de.andreas.cadas.application.view;

public final class ThreeDCameraController {

    public CameraPose orbit(CameraPose pose, double deltaAzimuthDegrees, double deltaElevationDegrees) {
        double elevation = Math.max(-89.0, Math.min(89.0, pose.elevationDegrees() + deltaElevationDegrees));
        return new CameraPose(
                pose.projectionMode(),
                pose.azimuthDegrees() + deltaAzimuthDegrees,
                elevation,
                pose.distance(),
                pose.panX(),
                pose.panY(),
                pose.panZ()
        );
    }

    public CameraPose zoom(CameraPose pose, double factor) {
        double distance = Math.max(1_000.0, Math.min(200_000.0, pose.distance() * factor));
        return new CameraPose(
                pose.projectionMode(),
                pose.azimuthDegrees(),
                pose.elevationDegrees(),
                distance,
                pose.panX(),
                pose.panY(),
                pose.panZ()
        );
    }

    public CameraPose pan(CameraPose pose, double deltaScreenX, double deltaScreenY) {
        double azimuthRadians = Math.toRadians(pose.azimuthDegrees());
        double elevationRadians = Math.toRadians(pose.elevationDegrees());
        double movementFactor = pose.projectionMode() == ProjectionMode.ORTHOGRAPHIC
                ? 16.0
                : Math.max(8.0, pose.distance() / 600.0);

        double rightX = Math.cos(azimuthRadians);
        double rightZ = -Math.sin(azimuthRadians);
        double upX = Math.sin(azimuthRadians) * Math.sin(elevationRadians);
        double upY = Math.cos(elevationRadians);
        double upZ = Math.cos(azimuthRadians) * Math.sin(elevationRadians);

        double worldDeltaX = deltaScreenX * movementFactor * rightX - deltaScreenY * movementFactor * upX;
        double worldDeltaY = -deltaScreenY * movementFactor * upY;
        double worldDeltaZ = deltaScreenX * movementFactor * rightZ - deltaScreenY * movementFactor * upZ;

        return new CameraPose(
                pose.projectionMode(),
                pose.azimuthDegrees(),
                pose.elevationDegrees(),
                pose.distance(),
                pose.panX() + worldDeltaX,
                pose.panY() + worldDeltaY,
                pose.panZ() + worldDeltaZ
        );
    }

    public CameraPose switchProjection(CameraPose pose, ProjectionMode projectionMode) {
        return new CameraPose(
                projectionMode,
                pose.azimuthDegrees(),
                pose.elevationDegrees(),
                pose.distance(),
                pose.panX(),
                pose.panY(),
                pose.panZ()
        );
    }
}
