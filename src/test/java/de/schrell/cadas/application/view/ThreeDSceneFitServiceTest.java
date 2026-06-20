package de.schrell.cadas.application.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThreeDSceneFitServiceTest {

    private static final double FIELD_OF_VIEW = 15.0;
    private static final double PADDING = 1.4;
    private static final double SAFETY_MARGIN = 20.0;
    private static final double MIN_DISTANCE = 10.0;
    private static final double MAX_DISTANCE = 50_000.0;

    private final ThreeDSceneFitService service = new ThreeDSceneFitService();

    @Test
    void berechnetFitFuerVorderansichtMitTiefeAlsSicherheitsabstand() {
        ThreeDSceneBounds bounds = ThreeDSceneBounds.fallback(400.0, 120.0, 200.0);
        CameraPose pose = new CameraPose(ProjectionMode.ORTHOGRAPHIC, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

        double distance = service.calculateDistance(
                bounds,
                pose,
                1_200.0,
                800.0,
                FIELD_OF_VIEW,
                PADDING,
                SAFETY_MARGIN,
                MIN_DISTANCE,
                MAX_DISTANCE
        );

        double expected = Math.max(
                (200.0 * PADDING / horizontalHalfTangent(1_200.0, 800.0)) + 100.0,
                (60.0 * PADDING / verticalHalfTangent()) + 100.0
        ) + SAFETY_MARGIN;
        assertEquals(expected, distance, 0.001);
    }

    @Test
    void berechnetFuerObenUndUntenDenGleichenAbstand() {
        ThreeDSceneBounds bounds = ThreeDSceneBounds.fallback(420.0, 90.0, 260.0);
        CameraPose topPose = new CameraPose(ProjectionMode.ORTHOGRAPHIC, 0.0, -90.0, 0.0, 0.0, 0.0, 0.0);
        CameraPose bottomPose = new CameraPose(ProjectionMode.ORTHOGRAPHIC, 0.0, 90.0, 0.0, 0.0, 0.0, 0.0);

        double topDistance = service.calculateDistance(
                bounds,
                topPose,
                1_000.0,
                700.0,
                FIELD_OF_VIEW,
                PADDING,
                SAFETY_MARGIN,
                MIN_DISTANCE,
                MAX_DISTANCE
        );
        double bottomDistance = service.calculateDistance(
                bounds,
                bottomPose,
                1_000.0,
                700.0,
                FIELD_OF_VIEW,
                PADDING,
                SAFETY_MARGIN,
                MIN_DISTANCE,
                MAX_DISTANCE
        );

        double expected = Math.max(
                (210.0 * PADDING / horizontalHalfTangent(1_000.0, 700.0)) + 45.0,
                (130.0 * PADDING / verticalHalfTangent()) + 45.0
        ) + SAFETY_MARGIN;
        assertEquals(expected, topDistance, 0.001);
        assertEquals(expected, bottomDistance, 0.001);
    }

    @Test
    void breiterViewportBrauchtWenigerAbstand() {
        ThreeDSceneBounds bounds = ThreeDSceneBounds.fallback(500.0, 200.0, 300.0);
        CameraPose pose = new CameraPose(ProjectionMode.PERSPECTIVE, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

        double narrowDistance = service.calculateDistance(
                bounds,
                pose,
                700.0,
                700.0,
                38.0,
                1.8,
                SAFETY_MARGIN,
                MIN_DISTANCE,
                MAX_DISTANCE
        );
        double wideDistance = service.calculateDistance(
                bounds,
                pose,
                1_400.0,
                700.0,
                38.0,
                1.8,
                SAFETY_MARGIN,
                MIN_DISTANCE,
                MAX_DISTANCE
        );

        assertTrue(wideDistance < narrowDistance);
    }

    private double verticalHalfTangent() {
        return Math.tan(Math.toRadians(FIELD_OF_VIEW / 2.0));
    }

    private double horizontalHalfTangent(double viewportWidth, double viewportHeight) {
        return verticalHalfTangent() * (viewportWidth / viewportHeight);
    }
}
