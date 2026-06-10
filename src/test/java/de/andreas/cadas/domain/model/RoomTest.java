package de.andreas.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;

import org.junit.jupiter.api.Test;

class RoomTest {

    @Test
    void berechnetRechteckigeFlaecheUndVolumen() {
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 5000),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );

        assertEquals(20.0, room.areaSquareMeters(), 0.001);
        assertEquals(50.0, room.volumeCubicMeters(), 0.001);
    }
}
