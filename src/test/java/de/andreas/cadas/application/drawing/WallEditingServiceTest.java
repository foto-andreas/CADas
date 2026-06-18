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

    @Test
    void waehltVonNahenEndpunktenDenTatsaechlichAngeklickten() {
        List<Wall> walls = List.of(
                Wall.create(new PlanSegment(new PlanPoint(0, 0), new PlanPoint(1000, 0)), Length.of(17.5, LengthUnit.CENTIMETER), Length.of(2.75, LengthUnit.METER)),
                Wall.create(new PlanSegment(new PlanPoint(0, 80), new PlanPoint(1000, 80)), Length.of(17.5, LengthUnit.CENTIMETER), Length.of(2.75, LengthUnit.METER))
        );

        WallEndpointSelection selection = wallEditingService
                .findConnectedEndpoint(walls, new PlanPoint(4, 75), Length.of(12, LengthUnit.CENTIMETER))
                .orElseThrow();

        assertEquals(new PlanPoint(0, 80), selection.anchorPoint());
        assertTrue(selection.startWallIds().contains(walls.get(1).id()));
    }

    @Test
    void verschiebtRechteckeckenOrthogonalUndHaeltAnschluesseVerbunden() {
        List<Wall> walls = List.of(
                wall(0, 0, 4000, 0),
                wall(4000, 0, 4000, 3000),
                wall(4000, 3000, 0, 3000),
                wall(0, 3000, 0, 0)
        );
        WallEndpointSelection selection = wallEditingService
                .findConnectedEndpoint(walls, new PlanPoint(0, 0), Length.of(1, LengthUnit.MILLIMETER))
                .orElseThrow();

        List<Wall> updatedWalls = wallEditingService.moveEndpointGroup(walls, selection, new PlanPoint(300, 400), true);

        assertEquals(new PlanPoint(300, 400), updatedWalls.get(0).axis().start());
        assertEquals(new PlanPoint(4000, 400), updatedWalls.get(0).axis().end());
        assertEquals(updatedWalls.get(0).axis().end(), updatedWalls.get(1).axis().start());
        assertEquals(new PlanPoint(300, 3000), updatedWalls.get(2).axis().end());
        assertEquals(updatedWalls.get(2).axis().end(), updatedWalls.get(3).axis().start());
        assertTrue(updatedWalls.stream().allMatch(this::isOrthogonal));
    }

    @Test
    void shiftFreigabeLaesstFreienWinkelZu() {
        List<Wall> walls = List.of(wall(0, 0, 4000, 0));
        WallEndpointSelection selection = wallEditingService
                .findConnectedEndpoint(walls, new PlanPoint(0, 0), Length.of(1, LengthUnit.MILLIMETER))
                .orElseThrow();

        List<Wall> updatedWalls = wallEditingService.moveEndpointGroup(walls, selection, new PlanPoint(300, 400), false);

        assertEquals(new PlanPoint(300, 400), updatedWalls.getFirst().axis().start());
        assertTrue(!isOrthogonal(updatedWalls.getFirst()));
    }

    private Wall wall(double startX, double startY, double endX, double endY) {
        return Wall.create(
                new PlanSegment(new PlanPoint(startX, startY), new PlanPoint(endX, endY)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
    }

    private boolean isOrthogonal(Wall wall) {
        double deltaX = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double deltaY = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        return Math.abs(deltaX) < 0.001 || Math.abs(deltaY) < 0.001;
    }
}
