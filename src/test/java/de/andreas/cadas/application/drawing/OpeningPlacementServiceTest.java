package de.andreas.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class OpeningPlacementServiceTest {

    private final OpeningPlacementService openingPlacementService = new OpeningPlacementService();

    @Test
    void platziertEineTürMittigAufDerNächstenWand() {
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );

        Optional<Door> door = openingPlacementService.placeDoor(
                new PlanPoint(1500, 40),
                List.of(wall),
                Length.of(1, LengthUnit.METER),
                Length.of(2.01, LengthUnit.METER),
                Length.zero(),
                Length.of(15, LengthUnit.CENTIMETER)
        );

        assertTrue(door.isPresent());
        assertEquals(1000.0, door.orElseThrow().offsetFromStart().toMillimeters(), 1.0);
    }

    @Test
    void platziertEinFensterMittigAufDerNächstenWand() {
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(0, 3000)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );

        Optional<WindowElement> window = openingPlacementService.placeWindow(
                new PlanPoint(30, 1200),
                List.of(wall),
                Length.of(1.20, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.20, LengthUnit.METER),
                Length.of(15, LengthUnit.CENTIMETER)
        );

        assertTrue(window.isPresent());
        assertEquals(600.0, window.orElseThrow().offsetFromStart().toMillimeters(), 1.0);
    }

    @Test
    void klemmtTürAmWandanfang() {
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );

        Optional<Door> door = openingPlacementService.placeDoor(
                new PlanPoint(200, 30),
                List.of(wall),
                Length.of(1, LengthUnit.METER),
                Length.of(2.01, LengthUnit.METER),
                Length.zero(),
                Length.of(15, LengthUnit.CENTIMETER)
        );

        assertTrue(door.isPresent());
        assertEquals(0.0, door.orElseThrow().offsetFromStart().toMillimeters(), 1.0);
    }

    @Test
    void klemmtFensterAmWandende() {
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );

        Optional<WindowElement> window = openingPlacementService.placeWindow(
                new PlanPoint(3800, 30),
                List.of(wall),
                Length.of(1.20, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.20, LengthUnit.METER),
                Length.of(15, LengthUnit.CENTIMETER)
        );

        assertTrue(window.isPresent());
        assertEquals(2800.0, window.orElseThrow().offsetFromStart().toMillimeters(), 1.0);
    }
}
