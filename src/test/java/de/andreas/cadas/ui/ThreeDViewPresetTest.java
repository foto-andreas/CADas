package de.andreas.cadas.ui;

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
}
