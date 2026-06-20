package de.schrell.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
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

    @Test
    void drehtAuchDieNiedrigeSeiteEinerDachschraegeMit() {
        Level level = new Level("Dachgeschoss");
        Room room = Room.rectangular(
                "Dachzimmer",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 2000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                new SlopedCeilingProfile(SlopedCeilingSide.NORTH, Length.of(1.0, LengthUnit.METER))
        );
        level.addRoom(room);

        QuarterTurnRotationService.RotationResult result = service.rotate(level, Set.of(
                new SelectionKey(RenderableKind.ROOM_VOLUME, level.name(), room.id().toString())
        ), true);

        assertEquals(SlopedCeilingSide.EAST, result.rooms().getFirst().slopedCeilingProfile().orElseThrow().lowSide());
    }

    @Test
    void drehtObjekteAusgehendVonFreiemWinkel() {
        Level level = new Level("Außenbereich");
        RoomObject roomObject = RoomObject.create(
                "tisch",
                "Tisch",
                RoomObjectType.TABLE,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(1000, 1000),
                Length.of(160, LengthUnit.CENTIMETER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(75, LengthUnit.CENTIMETER),
                25.0,
                RoomObjectMountingMode.STANDS_ON_COVERING,
                ""
        );
        level.addRoomObject(roomObject);

        QuarterTurnRotationService.RotationResult result = service.rotate(level, Set.of(
                new SelectionKey(RenderableKind.ROOM_OBJECT, level.name(), roomObject.id().toString())
        ), false);

        assertTrue(result.changed());
        assertEquals(295.0, result.roomObjects().getFirst().rotationDegrees(), 0.001);
    }
}
