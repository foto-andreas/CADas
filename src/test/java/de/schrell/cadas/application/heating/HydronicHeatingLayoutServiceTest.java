package de.schrell.cadas.application.heating;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Room;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HydronicHeatingLayoutServiceTest {

    private final HydronicHeatingLayoutService service = new HydronicHeatingLayoutService();

    @Test
    void verlegtMeanderZwischenVorlaufUndRücklauf() {
        Room room = rectangularRoom();
        HydronicHeating heating = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, 80_000)
                .withZones(List.of(HeatingZone.create("Heizkreis 1", room.outline())));

        HydronicHeatingLayoutService.CircuitLayout circuit = service.layout(heating).getFirst();

        assertEquals(heating.supplyPoint(), circuit.pipePath().getFirst());
        assertEquals(heating.returnPoint(), circuit.pipePath().getLast());
        assertEquals(100.0, circuit.bendRadius().toMillimeters(), 0.001);
        assertTrue(circuit.pipeLength().toMillimeters() > 40_000);
    }

    @Test
    void erzeugtFürSchneckeAnderenRohrverlauf() {
        Room room = rectangularRoom();
        HydronicHeating meander = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, 80_000)
                .withZones(List.of(HeatingZone.create("Heizkreis 1", room.outline())));
        HydronicHeating spiral = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.SPIRAL, 80_000)
                .withZones(meander.zones());

        List<PlanPoint> meanderPath = service.layout(meander).getFirst().pipePath();
        List<PlanPoint> spiralPath = service.layout(spiral).getFirst().pipePath();

        assertFalse(spiralPath.isEmpty());
        assertNotEquals(meanderPath, spiralPath);
    }

    @Test
    void teiltRaumBisZurMaximalenRohrlängeInHeizkreise() {
        Room room = rectangularRoom();
        HydronicHeating heating = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, 35_000);

        HydronicHeatingLayoutService.PlanningResult result = service.suggest(room, heating);

        assertTrue(result.heating().zones().size() > 1);
        assertEquals(result.heating().zones().size(), result.circuits().size());
        assertTrue(result.circuits().stream()
                .allMatch(circuit -> circuit.pipeLength().compareTo(heating.maximumPipeLength()) <= 0));
    }

    @Test
    void erhältLFormAlsVeränderbarenHeizbereich() {
        Room room = lShapedRoom();
        HydronicHeating heating = heating(room, HeatingSurfacePosition.CEILING, HeatingLayoutPattern.MEANDER, 200_000);

        HydronicHeatingLayoutService.PlanningResult result = service.suggest(room, heating);

        assertEquals(1, result.heating().zones().size());
        assertEquals(room.outline(), result.heating().zones().getFirst().outline());
        assertTrue(result.circuits().getFirst().pipePath().size() > 4);
    }

    @Test
    void plantBodenUndDeckeGeometrischGleich() {
        Room room = rectangularRoom();
        HydronicHeating floor = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.SPIRAL, 80_000);
        HydronicHeating ceiling = heating(room, HeatingSurfacePosition.CEILING, HeatingLayoutPattern.SPIRAL, 80_000);

        HydronicHeatingLayoutService.PlanningResult floorResult = service.suggest(room, floor);
        HydronicHeatingLayoutService.PlanningResult ceilingResult = service.suggest(room, ceiling);

        assertEquals(
                floorResult.circuits().stream().map(HydronicHeatingLayoutService.CircuitLayout::pipeLength).toList(),
                ceilingResult.circuits().stream().map(HydronicHeatingLayoutService.CircuitLayout::pipeLength).toList()
        );
        assertEquals(
                floorResult.circuits().stream().map(HydronicHeatingLayoutService.CircuitLayout::pipePath).toList(),
                ceilingResult.circuits().stream().map(HydronicHeatingLayoutService.CircuitLayout::pipePath).toList()
        );
    }

    private Room rectangularRoom() {
        return Room.rectangular(
                "Wohnen", new PlanPoint(0, 0), new PlanPoint(6_000, 4_000),
                Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
        );
    }

    private Room lShapedRoom() {
        return new Room(
                java.util.UUID.randomUUID(), "Wohnen", List.of(
                new PlanPoint(0, 0), new PlanPoint(6_000, 0), new PlanPoint(6_000, 2_000),
                new PlanPoint(3_000, 2_000), new PlanPoint(3_000, 5_000), new PlanPoint(0, 5_000)
        ), Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200), null);
    }

    private HydronicHeating heating(
            Room room,
            HeatingSurfacePosition position,
            HeatingLayoutPattern pattern,
            double maximumPipeLength
    ) {
        return HydronicHeating.create(
                room.id(), position, pattern, Length.ofMillimeters(200), Length.ofMillimeters(16),
                Length.ofMillimeters(maximumPipeLength), Length.ofMillimeters(150),
                new PlanPoint(100, 100), new PlanPoint(300, 100)
        );
    }
}
