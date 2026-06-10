package de.andreas.cadas.application.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThreeDCameraControllerTest {

    private final ThreeDCameraController controller = new ThreeDCameraController();

    @Test
    void orbitPanZoomUndProjektionBleibenKonsistent() {
        CameraPose pose = new CameraPose(ProjectionMode.ORTHOGRAPHIC, 45.0, 30.0, 10_000.0, 0.0, 0.0);

        pose = controller.orbit(pose, 15.0, -10.0);
        pose = controller.pan(pose, 250.0, -180.0);
        pose = controller.zoom(pose, 0.8);
        pose = controller.switchProjection(pose, ProjectionMode.PERSPECTIVE);

        assertEquals(ProjectionMode.PERSPECTIVE, pose.projectionMode());
        assertEquals(60.0, pose.azimuthDegrees());
        assertEquals(20.0, pose.elevationDegrees());
        assertEquals(8_000.0, pose.distance());
        assertEquals(250.0, pose.panX());
        assertEquals(-180.0, pose.panZ());
    }
}

