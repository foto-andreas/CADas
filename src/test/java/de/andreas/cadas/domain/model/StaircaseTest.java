package de.andreas.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;

import org.junit.jupiter.api.Test;

class StaircaseTest {

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
}
