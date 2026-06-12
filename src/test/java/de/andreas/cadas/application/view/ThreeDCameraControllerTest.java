package de.andreas.cadas.application.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThreeDCameraControllerTest {

    private final ThreeDCameraController controller = new ThreeDCameraController();

    @Test
    void orbitPanZoomUndProjektionBleibenKonsistent() {
        CameraPose pose = new CameraPose(ProjectionMode.ORTHOGRAPHIC, 0.0, 0.0, 10_000.0, 0.0, 0.0, 0.0);

        pose = controller.pan(pose, 250.0, -180.0);
        pose = controller.orbit(pose, 15.0, -10.0);
        pose = controller.zoom(pose, 0.8);
        pose = controller.switchProjection(pose, ProjectionMode.PERSPECTIVE);

        assertEquals(ProjectionMode.PERSPECTIVE, pose.projectionMode());
        assertEquals(15.0, pose.azimuthDegrees());
        assertEquals(-10.0, pose.elevationDegrees());
        assertEquals(8_000.0, pose.distance());
        assertEquals(4_000.0, pose.panX());
        assertEquals(2_880.0, pose.panY());
        assertEquals(0.0, pose.panZ());
    }

    @Test
    void panBleibtVonOrbitwinkelnUnabhaengigUndArbeitetInBildschirmachsen() {
        CameraPose pose = new CameraPose(ProjectionMode.PERSPECTIVE, 47.0, 28.0, 12_000.0, 10.0, 20.0, 30.0);

        CameraPose verschoben = controller.pan(pose, 90.0, 40.0);

        assertEquals(1_810.0, verschoben.panX());
        assertEquals(-780.0, verschoben.panY());
        assertEquals(30.0, verschoben.panZ());
    }

    @Test
    void begrenztZoomAufSichereGrenzen() {
        CameraPose pose = new CameraPose(ProjectionMode.PERSPECTIVE, 0.0, 0.0, 10_000.0, 0.0, 0.0, 0.0);

        CameraPose starkGeneigt = controller.orbit(pose, 0.0, 140.0);
        CameraPose starkVerkleinert = controller.zoom(starkGeneigt, 0.0001);
        CameraPose starkVergroessert = controller.zoom(starkGeneigt, 1000.0);

        assertEquals(140.0, starkGeneigt.elevationDegrees());
        assertEquals(1_000.0, starkVerkleinert.distance());
        assertEquals(200_000.0, starkVergroessert.distance());
    }
}
