package de.schrell.cadas.application.view;

/** Bündelt Szenenableitung, Grenzberechnung und anfänglichen Kamera-Fit für den Wechsel in die 3D-Ansicht. */
public final class ThreeDViewPreparation {

    public CameraPose defaultPose() {
        return new CameraPose(ProjectionMode.PERSPECTIVE, 45.0, 0.0, 9_000.0, 0.0, 0.0, 0.0);
    }

    public CameraPose poseForAngles(ProjectionMode projectionMode, double azimuthDegrees, double elevationDegrees) {
        return new CameraPose(projectionMode, azimuthDegrees, elevationDegrees, 9_000.0, 0.0, 0.0, 0.0);
    }
}
