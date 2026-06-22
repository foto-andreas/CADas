package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HydronicHeatingTest {

    @Test
    void leitetKurvenradiusAusHalbemVerlegeabstandAb() {
        HydronicHeating heating = heating(HeatingSurfacePosition.FLOOR, Length.ofMillimeters(200));

        assertEquals(100.0, heating.bendRadius().toMillimeters(), 0.001);
    }

    @Test
    void unterstütztIdentischeKonfigurationFürBodenUndDecke() {
        HydronicHeating floorHeating = heating(HeatingSurfacePosition.FLOOR, Length.ofMillimeters(150));
        HydronicHeating ceilingHeating = heating(HeatingSurfacePosition.CEILING, Length.ofMillimeters(150));

        assertEquals(floorHeating.pipeSpacing(), ceilingHeating.pipeSpacing());
        assertEquals(floorHeating.pipeDiameter(), ceilingHeating.pipeDiameter());
        assertEquals(floorHeating.maximumPipeLength(), ceilingHeating.maximumPipeLength());
        assertEquals(floorHeating.wallClearance(), ceilingHeating.wallClearance());
    }

    @Test
    void berechnetFlächeNichtRechteckigerHeizbereiche() {
        HeatingZone zone = HeatingZone.create("L-Bereich", List.of(
                new PlanPoint(0, 0),
                new PlanPoint(4_000, 0),
                new PlanPoint(4_000, 2_000),
                new PlanPoint(2_000, 2_000),
                new PlanPoint(2_000, 4_000),
                new PlanPoint(0, 4_000)
        ));

        assertEquals(12_000_000.0, zone.areaSquareMillimeters(), 0.001);
    }

    @Test
    void neuerHeizkreisNutztSchneckeAlsStandard() {
        HeatingZone zone = HeatingZone.create("Standard", List.of(
                new PlanPoint(0, 0),
                new PlanPoint(1_000, 0),
                new PlanPoint(1_000, 1_000),
                new PlanPoint(0, 1_000)
        ));

        assertEquals(HeatingLayoutPattern.SPIRAL, zone.layoutPattern());
    }

    @Test
    void lehntUngültigeRohrparameterAb() {
        assertThrows(IllegalArgumentException.class, () -> new HydronicHeating(
                UUID.randomUUID(), UUID.randomUUID(), HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER,
                Length.ofMillimeters(100), Length.ofMillimeters(100), Length.ofMillimeters(80_000),
                Length.ofMillimeters(100), new PlanPoint(0, 0), new PlanPoint(100, 0), List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> HeatingZone.create("Leer", List.of(
                new PlanPoint(0, 0), new PlanPoint(1, 0)
        )));
        assertThrows(IllegalArgumentException.class, () -> HeatingZone.create("Ohne Fläche", List.of(
                new PlanPoint(0, 0), new PlanPoint(1, 0), new PlanPoint(2, 0)
        )));
    }

    private HydronicHeating heating(HeatingSurfacePosition position, Length spacing) {
        return HydronicHeating.create(
                UUID.randomUUID(), position, HeatingLayoutPattern.MEANDER, spacing,
                Length.ofMillimeters(16), Length.ofMillimeters(80_000), Length.ofMillimeters(100),
                new PlanPoint(0, 0), new PlanPoint(100, 0)
        );
    }
}
