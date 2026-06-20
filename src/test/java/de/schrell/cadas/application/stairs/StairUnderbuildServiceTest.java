package de.schrell.cadas.application.stairs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StairUnderbuildServiceTest {

    private final StairUnderbuildService service = new StairUnderbuildService();

    @Test
    void erzeugtBeidseitigeÖffnungsfähigePolygonwände() {
        Level level = new Level("Erdgeschoss");
        Staircase staircase = staircase(120, 150, 80);
        level.addStaircase(staircase);

        StairUnderbuildService.UnderbuildResult result = service.synchronize(level, staircase);

        assertEquals(2, result.walls().size());
        assertEquals(120.0, result.walls().getFirst().thickness().toMillimeters(), 0.001);
        assertEquals(0.0, result.walls().getFirst().heightAtStart(), 0.001);
        assertEquals(2_720.0, result.walls().getFirst().heightAtEnd(), 0.001);
        assertTrue(result.walls().stream().allMatch(wall -> wall.hasVariableTopHeight()));
    }

    @Test
    void erhältÖffnungenUndEntferntSieBeimDeaktivierenDerSeite() {
        Level level = new Level("Erdgeschoss");
        Staircase staircase = staircase(120, 0, 80);
        level.addStaircase(staircase);
        StairUnderbuildService.UnderbuildResult initial = service.synchronize(level, staircase);
        level.replaceWalls(initial.walls());
        Door door = Door.create(
                service.wallId(staircase.id(), StairUnderbuildService.Side.LEFT),
                Length.ofMillimeters(800), Length.ofMillimeters(900), Length.ofMillimeters(1_800), Length.zero()
        );
        level.addDoor(door);

        StairUnderbuildService.UnderbuildResult retained = service.synchronize(level, staircase);
        assertEquals(door.id(), retained.doors().getFirst().id());

        Staircase withoutWall = new Staircase(
                staircase.id(), staircase.stairType(), staircase.firstCorner(), staircase.oppositeCorner(),
                staircase.totalHeight(), staircase.stepCount(), staircase.rotationQuarterTurns(),
                staircase.startLandingWidth(), staircase.endLandingWidth(), Length.zero(), Length.zero(), staircase.undersideThickness()
        );
        StairUnderbuildService.UnderbuildResult removed = service.synchronize(level, withoutWall);
        assertTrue(removed.walls().isEmpty());
        assertFalse(removed.doors().stream().anyMatch(candidate -> candidate.id().equals(door.id())));
    }

    private Staircase staircase(double leftWidth, double rightWidth, double undersideThickness) {
        return new Staircase(
                UUID.randomUUID(), StairType.STRAIGHT,
                new PlanPoint(0, 0), new PlanPoint(1_200, 4_000),
                Length.ofMillimeters(2_800), 16, 0,
                Length.zero(), Length.zero(),
                Length.ofMillimeters(leftWidth), Length.ofMillimeters(rightWidth), Length.ofMillimeters(undersideThickness)
        );
    }
}
