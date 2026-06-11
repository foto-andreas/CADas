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

    @Test
    void berechnetVolumenUndHoehenFuerSchraegeDecke() {
        Room room = Room.rectangular(
                "Dachzimmer",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 5000),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                new SlopedCeilingProfile(
                        SlopedCeilingSide.NORTH,
                        Length.of(1.0, LengthUnit.METER)
                )
        );

        assertEquals(38.0, room.volumeCubicMeters(), 0.001);
        assertEquals(1000.0, room.minimumCeilingHeightMillimeters(), 0.001);
        assertEquals(2800.0, room.maximumCeilingHeightMillimeters(), 0.001);
        assertEquals(1900.0, room.ceilingHeightAt(new PlanPoint(2000, 2500)), 0.001);
    }
}
