package de.andreas.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;

import org.junit.jupiter.api.Test;

class SurfaceLayerStackTest {

    @Test
    void verwaltetReihenfolgeUndSichtbarkeitVonEbenen() {
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, "raum-eg-wohnen");
        SurfaceLayer estrich = SurfaceLayer.create("Estrich", Length.of(6, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.zero());
        SurfaceLayer fliese = SurfaceLayer.create("Fliese", Length.of(1.2, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.of(30, LengthUnit.CENTIMETER), Length.of(10, LengthUnit.CENTIMETER));
        stack.addLayer(estrich);
        stack.addLayer(fliese);

        stack.moveLayer(fliese.id(), 0);
        stack.setVisibility(estrich.id(), false);

        assertEquals("Fliese", stack.layers().getFirst().name());
        assertFalse(stack.layers().get(1).visible());
    }
}

