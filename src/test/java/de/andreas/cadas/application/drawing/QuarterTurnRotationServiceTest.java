package de.andreas.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.application.view.RenderableKind;
import de.andreas.cadas.application.view.SelectionKey;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.Wall;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuarterTurnRotationServiceTest {

    private final QuarterTurnRotationService service = new QuarterTurnRotationService();

    @Test
    void drehtWaendeRaeumeUndTreppenViertelweise() {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 2000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        Staircase staircase = Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(500, 500),
                new PlanPoint(1500, 3500),
                Length.of(2.8, LengthUnit.METER),
                16
        );
        level.addWall(wall);
        level.addRoom(room);
        level.addStaircase(staircase);

        QuarterTurnRotationService.RotationResult result = service.rotate(level, Set.of(
                new SelectionKey(RenderableKind.WALL, level.name(), wall.id().toString()),
                new SelectionKey(RenderableKind.ROOM_VOLUME, level.name(), room.id().toString()),
                new SelectionKey(RenderableKind.STAIR, level.name(), staircase.id().toString())
        ), true);

        assertTrue(result.changed());
        assertEquals(2000.0, result.walls().getFirst().axis().start().xMillimeters(), 0.001);
        assertEquals(2000.0, result.walls().getFirst().axis().end().xMillimeters(), 0.001);
        assertEquals(2000.0, result.rooms().getFirst().centerPoint().xMillimeters(), 0.001);
        assertEquals(1000.0, result.rooms().getFirst().centerPoint().yMillimeters(), 0.001);
        assertEquals(1, result.staircases().getFirst().rotationQuarterTurns());
    }
}
