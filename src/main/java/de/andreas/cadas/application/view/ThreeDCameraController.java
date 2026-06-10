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
                pose.panZ()
        );
    }

    public CameraPose pan(CameraPose pose, double deltaX, double deltaZ) {
        return new CameraPose(
                pose.projectionMode(),
                pose.azimuthDegrees(),
                pose.elevationDegrees(),
                pose.distance(),
                pose.panX() + deltaX,
                pose.panZ() + deltaZ
        );
    }

    public CameraPose switchProjection(CameraPose pose, ProjectionMode projectionMode) {
        return new CameraPose(
                projectionMode,
                pose.azimuthDegrees(),
                pose.elevationDegrees(),
                pose.distance(),
                pose.panX(),
                pose.panZ()
        );
    }
}

