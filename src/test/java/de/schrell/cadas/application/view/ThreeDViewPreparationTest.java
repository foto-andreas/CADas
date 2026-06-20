package de.schrell.cadas.application.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThreeDViewPreparationTest {

    private final ThreeDViewPreparation preparation = new ThreeDViewPreparation();

    @Test
    void liefertEinenStartpunktFuerSpaetere3dAnsichten() {
        CameraPose pose = preparation.defaultPose();

        assertEquals(ProjectionMode.PERSPECTIVE, pose.projectionMode());
        assertEquals(45.0, pose.azimuthDegrees());
        assertEquals(0.0, pose.elevationDegrees());
    }

    @Test
    void kannGezielteKameraWinkelAlsAnsichtspresetAbleiten() {
        CameraPose pose = preparation.poseForAngles(ProjectionMode.PERSPECTIVE, 90.0, 0.0);

        assertEquals(ProjectionMode.PERSPECTIVE, pose.projectionMode());
        assertEquals(90.0, pose.azimuthDegrees());
        assertEquals(0.0, pose.elevationDegrees());
    }
}
