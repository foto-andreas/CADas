package de.schrell.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThreeDViewPresetTest {

    @Test
    void bildetDraufUndUntersichtExplizitAufKamerawinkelAb() {
        assertEquals("Oben", ThreeDViewPreset.TOP.label());
        assertEquals(-90.0, ThreeDViewPreset.TOP.cameraElevationDegrees());
        assertEquals("Unten", ThreeDViewPreset.BOTTOM.label());
        assertEquals(90.0, ThreeDViewPreset.BOTTOM.cameraElevationDegrees());
    }

    @Test
    void rechteUndLinkeAnsichtNutzenDieGleicheDrehrichtungWieDieZweiDAnsichten() {
        assertEquals(90.0, ThreeDViewPreset.RIGHT.cameraAzimuthDegrees());
        assertEquals(-90.0, ThreeDViewPreset.LEFT.cameraAzimuthDegrees());
    }

    @Test
    void fassadenansichtenSchauenVonAussenAufDieGespeicherteVorderseite() {
        assertEquals(180.0, ThreeDViewport.viewAzimuthDegrees(ThreeDViewPreset.FRONT, 0.0), 0.001);
        assertEquals(0.0, ThreeDViewport.viewAzimuthDegrees(ThreeDViewPreset.BACK, 0.0), 0.001);
        assertEquals(270.0, ThreeDViewport.viewAzimuthDegrees(ThreeDViewPreset.RIGHT, 0.0), 0.001);
        assertEquals(90.0, ThreeDViewport.viewAzimuthDegrees(ThreeDViewPreset.LEFT, 0.0), 0.001);
        assertEquals(270.0, ThreeDViewport.viewAzimuthDegrees(ThreeDViewPreset.FRONT, 90.0), 0.001);
        assertEquals(0.0, ThreeDViewport.viewAzimuthDegrees(ThreeDViewPreset.TOP, 90.0), 0.001);
    }
}
