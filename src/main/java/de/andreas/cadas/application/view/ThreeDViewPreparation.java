package de.andreas.cadas.application.view;

public final class ThreeDViewPreparation {

    public CameraPose defaultPose() {
        return new CameraPose(ProjectionMode.ORTHOGRAPHIC, 45.0, 30.0, 12_000.0, 0.0, 0.0);
    }
}
