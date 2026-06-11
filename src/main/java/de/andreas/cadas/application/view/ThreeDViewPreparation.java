package de.andreas.cadas.application.view;

public final class ThreeDViewPreparation {

    public CameraPose defaultPose() {
        return new CameraPose(ProjectionMode.PERSPECTIVE, 45.0, 30.0, 9_000.0, 0.0, 0.0, 0.0);
    }

    public CameraPose poseForAngles(ProjectionMode projectionMode, double azimuthDegrees, double elevationDegrees) {
        return new CameraPose(projectionMode, azimuthDegrees, elevationDegrees, 9_000.0, 0.0, 0.0, 0.0);
    }
}
