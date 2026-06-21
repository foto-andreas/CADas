package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoofWindowTest {

    @Test
    void prüftGrundflächeUndPositiveMaße() {
        RoofWindow roofWindow = RoofWindow.create(
                UUID.randomUUID(), new PlanPoint(1_000, 1_000),
                Length.ofMillimeters(800), Length.ofMillimeters(1_200), SlopedCeilingSide.NORTH
        );

        assertTrue(roofWindow.contains(new PlanPoint(1_400, 1_600)));
        assertFalse(roofWindow.contains(new PlanPoint(1_401, 1_600)));
        assertThrows(IllegalArgumentException.class, () -> RoofWindow.create(
                UUID.randomUUID(), new PlanPoint(0, 0),
                Length.zero(), Length.ofMillimeters(1_000), SlopedCeilingSide.NORTH
        ));
    }
}
