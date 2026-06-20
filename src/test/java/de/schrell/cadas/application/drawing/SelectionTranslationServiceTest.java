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
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;

import java.util.Set;

import org.junit.jupiter.api.Test;

class SelectionTranslationServiceTest {

    private final SelectionTranslationService translationService = new SelectionTranslationService();

    @Test
    void verschiebtAusgewaehlteWaendeUndTreppenParallel() {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        Staircase staircase = Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(500, 500),
                new PlanPoint(1500, 3000),
                Length.of(2.8, LengthUnit.METER),
                14
        );
        level.addWall(wall);
        level.addStaircase(staircase);

        SelectionTranslationService.TranslationResult result = translationService.translate(
                level,
                Set.of(
                        new SelectionKey(RenderableKind.WALL, level.name(), wall.id().toString()),
                        new SelectionKey(RenderableKind.STAIR, level.name(), staircase.id().toString())
                ),
                320.0,
                -180.0
        );

        assertTrue(result.changed());
        assertEquals(320.0, result.walls().getFirst().axis().start().xMillimeters(), 0.001);
        assertEquals(-180.0, result.walls().getFirst().axis().start().yMillimeters(), 0.001);
        assertEquals(820.0, result.staircases().getFirst().firstCorner().xMillimeters(), 0.001);
        assertEquals(320.0, result.staircases().getFirst().firstCorner().yMillimeters(), 0.001);
    }

    @Test
    void verschiebtAngrenzendeWandendenMitAusgewaehlterWand() {
        Level level = new Level("Erdgeschoss");
        Wall oben = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        Wall rechts = Wall.create(
                new PlanSegment(new PlanPoint(4000, 0), new PlanPoint(4000, 3000)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        Wall unten = Wall.create(
                new PlanSegment(new PlanPoint(4000, 3000), new PlanPoint(0, 3000)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        Wall links = Wall.create(
                new PlanSegment(new PlanPoint(0, 3000), new PlanPoint(0, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(oben);
        level.addWall(rechts);
        level.addWall(unten);
        level.addWall(links);

        SelectionTranslationService.TranslationResult result = translationService.translate(
                level,
                Set.of(new SelectionKey(RenderableKind.WALL, level.name(), oben.id().toString())),
                0.0,
                500.0
        );

        assertEquals(500.0, result.walls().get(0).axis().start().yMillimeters(), 0.001);
        assertEquals(500.0, result.walls().get(0).axis().end().yMillimeters(), 0.001);
        assertEquals(500.0, result.walls().get(1).axis().start().yMillimeters(), 0.001);
        assertEquals(3000.0, result.walls().get(1).axis().end().yMillimeters(), 0.001);
        assertEquals(3000.0, result.walls().get(2).axis().start().yMillimeters(), 0.001);
        assertEquals(3000.0, result.walls().get(2).axis().end().yMillimeters(), 0.001);
        assertEquals(3000.0, result.walls().get(3).axis().start().yMillimeters(), 0.001);
        assertEquals(500.0, result.walls().get(3).axis().end().yMillimeters(), 0.001);
    }

    @Test
    void verschiebtAusgewaehlteRaumobjekte() {
        Level level = new Level("Erdgeschoss");
        RoomObject toilet = RoomObject.create(
                "toilet",
                "Toilette",
                RoomObjectType.TOILET,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(500, 700),
                Length.of(40, LengthUnit.CENTIMETER),
                Length.of(70, LengthUnit.CENTIMETER),
                Length.of(80, LengthUnit.CENTIMETER),
                false,
                ""
        );
        level.addRoomObject(toilet);

        SelectionTranslationService.TranslationResult result = translationService.translate(
                level,
                Set.of(new SelectionKey(RenderableKind.ROOM_OBJECT, level.name(), toilet.id().toString())),
                200.0,
                -100.0
        );

        assertEquals(700.0, result.roomObjects().getFirst().center().xMillimeters(), 0.001);
        assertEquals(600.0, result.roomObjects().getFirst().center().yMillimeters(), 0.001);
    }
}
