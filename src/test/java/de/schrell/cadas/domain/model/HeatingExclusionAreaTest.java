package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.PlanPoint;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeatingExclusionAreaTest {

    @Test
    void normalisiertRechteckkoordinatenUnabhängigVonZiehrichtung() {
        HeatingExclusionArea area = HeatingExclusionArea.create(
                UUID.randomUUID(),
                "Verteilerbereich",
                new PlanPoint(2_000, 1_500),
                new PlanPoint(800, 500)
        );

        assertEquals(800.0, area.minXMillimeters(), 0.001);
        assertEquals(2_000.0, area.maxXMillimeters(), 0.001);
        assertEquals(500.0, area.minYMillimeters(), 0.001);
        assertEquals(1_500.0, area.maxYMillimeters(), 0.001);
        assertEquals(new PlanPoint(1_400, 1_000), area.center());
        assertTrue(area.contains(new PlanPoint(1_000, 800)));
    }

    @Test
    void verschiebtBeideEckenGemeinsam() {
        HeatingExclusionArea area = HeatingExclusionArea.create(
                UUID.randomUUID(),
                "Sperre",
                new PlanPoint(100, 200),
                new PlanPoint(900, 700)
        );

        HeatingExclusionArea translated = area.translated(50, -80);

        assertEquals(new PlanPoint(150, 120), translated.firstCorner());
        assertEquals(new PlanPoint(950, 620), translated.oppositeCorner());
    }

    @Test
    void lehntLeereRechteckeAb() {
        assertThrows(IllegalArgumentException.class, () -> HeatingExclusionArea.create(
                UUID.randomUUID(),
                "Leer",
                new PlanPoint(100, 200),
                new PlanPoint(100, 700)
        ));
    }
}
