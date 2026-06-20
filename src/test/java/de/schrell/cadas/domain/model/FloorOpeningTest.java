package de.schrell.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FloorOpeningTest {

    @Test
    void normalisiertRundeÖffnungAufKleinerenDurchmesser() {
        FloorOpening opening = FloorOpening.create(
                UUID.randomUUID(), FloorOpeningShape.CIRCLE, new PlanPoint(1_000, 1_000),
                Length.ofMillimeters(1_200), Length.ofMillimeters(1_000)
        );

        assertEquals(1_000.0, opening.width().toMillimeters(), 0.001);
        assertEquals(1_000.0, opening.depth().toMillimeters(), 0.001);
        assertEquals(Math.PI * 0.25, opening.areaSquareMeters(), 0.001);
        assertTrue(opening.contains(new PlanPoint(1_400, 1_000)));
        assertFalse(opening.contains(new PlanPoint(1_600, 1_000)));
    }
}
