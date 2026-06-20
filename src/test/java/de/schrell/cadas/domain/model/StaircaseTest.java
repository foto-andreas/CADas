package de.schrell.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;

import org.junit.jupiter.api.Test;

class StaircaseTest {

    @Test
    void zähltKonfigurierteAbsätzeAlsStufen() {
        Staircase staircase = Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(0, 0),
                new PlanPoint(1_000, 4_000),
                Length.ofMillimeters(2_800),
                16,
                Length.ofMillimeters(800),
                Length.ofMillimeters(600)
        );

        assertEquals(2, staircase.landingCount());
        assertEquals(14, staircase.regularStepCount());
    }

    @Test
    void weistAbsätzeOhnePlatzFürStufenZurück() {
        assertThrows(IllegalArgumentException.class, () -> Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(0, 0),
                new PlanPoint(1_000, 1_400),
                Length.ofMillimeters(2_800),
                2,
                Length.ofMillimeters(800),
                Length.ofMillimeters(600)
        ));
    }

    @Test
    void erzeugtTreppenMitGeometrieUndStufenanzahl() {
        Staircase staircase = Staircase.create(
                StairType.HALF_TURN,
                new PlanPoint(0, 0),
                new PlanPoint(2200, 4200),
                Length.of(2.9, LengthUnit.METER),
                18
        );

        assertEquals(2200.0, staircase.widthMillimeters(), 0.1);
        assertEquals(4200.0, staircase.heightMillimeters(), 0.1);
        assertEquals(18, staircase.stepCount());
    }

    @Test
    void kannTreppenIn90GradSchrittenDrehen() {
        Staircase staircase = new Staircase(
                java.util.UUID.randomUUID(),
                StairType.SWITCHBACK,
                new PlanPoint(0, 0),
                new PlanPoint(2200, 4200),
                Length.of(2.9, LengthUnit.METER),
                18,
                0
        );

        Staircase rotated = staircase.rotateClockwise();

        assertEquals(1, rotated.rotationQuarterTurns());
        assertNotEquals(staircase.pointAtLocalPosition(2200, 0), rotated.pointAtLocalPosition(2200, 0));
    }
}
