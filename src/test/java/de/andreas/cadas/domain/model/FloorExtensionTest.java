package de.andreas.cadas.domain.model;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FloorExtensionTest {

    @Test
    void modelliertRechteckigeBalkonplatteMitBelagsziel() {
        FloorExtension balcony = FloorExtension.create(FloorExtensionType.BALCONY, FloorExtensionPlacement.EXTERIOR,
                new PlanPoint(4_000, 1_000), new PlanPoint(7_000, 2_500), Length.ofMillimeters(180));

        assertEquals(3_000, balcony.widthMillimeters(), 0.001);
        assertEquals(1_500, balcony.depthMillimeters(), 0.001);
        assertEquals(4, balcony.outline().size());
        assertEquals("floor-extension:" + balcony.id(), balcony.surfaceTargetKey());
    }

    @Test
    void weistLinienUndDickeNullZurück() {
        assertThrows(IllegalArgumentException.class, () -> FloorExtension.create(FloorExtensionType.GALLERY,
                FloorExtensionPlacement.INTERIOR, new PlanPoint(0, 0), new PlanPoint(0, 2_000), Length.ofMillimeters(0)));
    }
}
