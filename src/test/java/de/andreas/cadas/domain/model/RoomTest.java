package de.andreas.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;

import java.util.List;
import java.util.UUID;

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

    @Test
    void berechnetPolygonaleDeckenhoehenUndVolumenAusEckhoehen() {
        Room room = new Room(
                UUID.randomUUID(),
                "Ausbau",
                List.of(
                        new PlanPoint(0, 0),
                        new PlanPoint(4000, 0),
                        new PlanPoint(4000, 3000),
                        new PlanPoint(0, 3000)
                ),
                Length.of(3.1, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                null,
                List.of(
                        Length.of(2.4, LengthUnit.METER),
                        Length.of(2.8, LengthUnit.METER),
                        Length.of(3.1, LengthUnit.METER),
                        Length.of(2.7, LengthUnit.METER)
                )
        );

        assertEquals(12.0, room.areaSquareMeters(), 0.001);
        assertEquals(33.0, room.volumeCubicMeters(), 0.001);
        assertEquals(2400.0, room.minimumCeilingHeightMillimeters(), 0.001);
        assertEquals(3100.0, room.maximumCeilingHeightMillimeters(), 0.001);
        assertEquals(2750.0, room.ceilingHeightAt(room.centerPoint()), 0.001);
        assertEquals(0.0, room.slopeAngleDegrees(), 0.001);
        assertTrue(room.slopeVisibleInEastWestView());
        assertTrue(room.slopeVisibleInNorthSouthView());
    }

    @Test
    void berechnetDachschrägenFürAlleRaumseiten() {
        assertSlope(
                SlopedCeilingSide.NORTH,
                new PlanPoint(2000, 0),
                new PlanPoint(2000, 5000),
                19.7989,
                true,
                false
        );
        assertSlope(
                SlopedCeilingSide.SOUTH,
                new PlanPoint(2000, 5000),
                new PlanPoint(2000, 0),
                19.7989,
                true,
                false
        );
        assertSlope(
                SlopedCeilingSide.EAST,
                new PlanPoint(4000, 2500),
                new PlanPoint(0, 2500),
                24.2277,
                false,
                true
        );
        assertSlope(
                SlopedCeilingSide.WEST,
                new PlanPoint(0, 2500),
                new PlanPoint(4000, 2500),
                24.2277,
                false,
                true
        );
    }

    @Test
    void lehntUnvollständigeEckhöhenUndZuHoheKniestöckeAb() {
        List<PlanPoint> outline = List.of(
                new PlanPoint(0, 0),
                new PlanPoint(4000, 0),
                new PlanPoint(4000, 3000),
                new PlanPoint(0, 3000)
        );

        assertThrows(IllegalArgumentException.class, () -> new Room(
                UUID.randomUUID(),
                "Ausbau",
                outline,
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                null,
                List.of(Length.of(2.4, LengthUnit.METER))
        ));
        assertThrows(IllegalArgumentException.class, () -> new Room(
                UUID.randomUUID(),
                "Ausbau",
                outline,
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                new SlopedCeilingProfile(SlopedCeilingSide.NORTH, Length.of(2.7, LengthUnit.METER)),
                null
        ));
    }

    @Test
    void meldetBeiWaagerechterDeckeKeineSichtbareSchräge() {
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 5000),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER)
        );

        assertEquals(0.0, room.slopeAngleDegrees(), 0.001);
        assertFalse(room.slopeVisibleInEastWestView());
        assertFalse(room.slopeVisibleInNorthSouthView());
    }

    private void assertSlope(
            SlopedCeilingSide lowSide,
            PlanPoint lowPoint,
            PlanPoint highPoint,
            double expectedAngle,
            boolean visibleInEastWestView,
            boolean visibleInNorthSouthView
    ) {
        Room room = Room.rectangular(
                "Dachzimmer",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 5000),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                new SlopedCeilingProfile(lowSide, Length.of(1.0, LengthUnit.METER))
        );

        assertEquals(1000.0, room.ceilingHeightAt(lowPoint), 0.001);
        assertEquals(2800.0, room.ceilingHeightAt(highPoint), 0.001);
        assertEquals(expectedAngle, room.slopeAngleDegrees(), 0.001);
        assertEquals(visibleInEastWestView, room.slopeVisibleInEastWestView());
        assertEquals(visibleInNorthSouthView, room.slopeVisibleInNorthSouthView());
    }
}
