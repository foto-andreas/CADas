package de.andreas.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Wall;

import java.util.List;

import org.junit.jupiter.api.Test;

class WallEditingServiceTest {

    private final WallEditingService wallEditingService = new WallEditingService();

    @Test
    void findetVerbundeneEndpunkteMehrererWaende() {
        List<Wall> walls = List.of(
                Wall.create(new PlanSegment(new PlanPoint(0, 0), new PlanPoint(2000, 0)), Length.of(17.5, LengthUnit.CENTIMETER), Length.of(2.75, LengthUnit.METER)),
                Wall.create(new PlanSegment(new PlanPoint(0, 0), new PlanPoint(0, 1500)), Length.of(17.5, LengthUnit.CENTIMETER), Length.of(2.75, LengthUnit.METER))
        );

        var selection = wallEditingService.findConnectedEndpoint(walls, new PlanPoint(40, 20), Length.of(10, LengthUnit.CENTIMETER));

        assertTrue(selection.isPresent());
        assertEquals(2, selection.orElseThrow().startWallIds().size());
    }

    @Test
    void verschiebtAlleVerknuepftenEndpunkteGemeinsam() {
        List<Wall> walls = List.of(
                Wall.create(new PlanSegment(new PlanPoint(0, 0), new PlanPoint(2000, 0)), Length.of(17.5, LengthUnit.CENTIMETER), Length.of(2.75, LengthUnit.METER)),
                Wall.create(new PlanSegment(new PlanPoint(0, 0), new PlanPoint(0, 1500)), Length.of(17.5, LengthUnit.CENTIMETER), Length.of(2.75, LengthUnit.METER))
        );

        WallEndpointSelection selection = wallEditingService.findConnectedEndpoint(walls, new PlanPoint(0, 0), Length.of(1, LengthUnit.CENTIMETER)).orElseThrow();
        List<Wall> updatedWalls = wallEditingService.moveEndpointGroup(walls, selection, new PlanPoint(300, 400));

        assertEquals(300.0, updatedWalls.getFirst().axis().start().xMillimeters(), 0.1);
        assertEquals(400.0, updatedWalls.getFirst().axis().start().yMillimeters(), 0.1);
        assertEquals(300.0, updatedWalls.get(1).axis().start().xMillimeters(), 0.1);
        assertEquals(400.0, updatedWalls.get(1).axis().start().yMillimeters(), 0.1);
    }

    @Test
    void bleibtAuchBeiKleinteiligenEckenStabil() {
        List<Wall> walls = List.of(
                Wall.create(new PlanSegment(new PlanPoint(0, 0), new PlanPoint(50, 0)), Length.of(17.5, LengthUnit.CENTIMETER), Length.of(2.75, LengthUnit.METER)),
                Wall.create(new PlanSegment(new PlanPoint(50, 0), new PlanPoint(50, 75)), Length.of(17.5, LengthUnit.CENTIMETER), Length.of(2.75, LengthUnit.METER)),
                Wall.create(new PlanSegment(new PlanPoint(50, 75), new PlanPoint(400, 75)), Length.of(17.5, LengthUnit.CENTIMETER), Length.of(2.75, LengthUnit.METER))
        );

        WallEndpointSelection selection = wallEditingService.findConnectedEndpoint(walls, new PlanPoint(50, 0), Length.of(4, LengthUnit.CENTIMETER)).orElseThrow();
        List<Wall> updatedWalls = wallEditingService.moveEndpointGroup(walls, selection, new PlanPoint(60, 10));

        assertEquals(60.0, updatedWalls.getFirst().axis().end().xMillimeters(), 0.1);
        assertEquals(10.0, updatedWalls.getFirst().axis().end().yMillimeters(), 0.1);
        assertEquals(60.0, updatedWalls.get(1).axis().start().xMillimeters(), 0.1);
        assertEquals(10.0, updatedWalls.get(1).axis().start().yMillimeters(), 0.1);
    }
}
