package de.schrell.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.application.stairs.StairUnderbuildService;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;

import java.util.List;
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

    @Test
    void verschiebtTreppenunterbauMitAusgewählterTreppe() {
        Level level = new Level("Erdgeschoss");
        Staircase staircase = new Staircase(
                java.util.UUID.randomUUID(), StairType.STRAIGHT,
                new PlanPoint(0, 0), new PlanPoint(1_200, 4_000),
                Length.ofMillimeters(2_800), 16, 0,
                Length.zero(), Length.zero(), Length.ofMillimeters(120), Length.zero(), Length.ofMillimeters(80)
        );
        level.addStaircase(staircase);
        StairUnderbuildService underbuildService = new StairUnderbuildService();
        level.replaceWalls(underbuildService.synchronize(level, staircase).walls());

        SelectionTranslationService.TranslationResult result = translationService.translate(
                level,
                Set.of(new SelectionKey(RenderableKind.STAIR, level.name(), staircase.id().toString())),
                200.0,
                300.0
        );

        assertEquals(200.0, result.walls().getFirst().axis().start().xMillimeters(), 0.001);
        assertEquals(300.0, result.walls().getFirst().axis().start().yMillimeters(), 0.001);
    }

    @Test
    void verschiebtAusgewählteBodenöffnung() {
        Level level = new Level("Obergeschoss");
        FloorOpening opening = FloorOpening.create(
                java.util.UUID.randomUUID(), FloorOpeningShape.RECTANGLE, new PlanPoint(1_000, 1_000),
                Length.ofMillimeters(800), Length.ofMillimeters(600)
        );
        level.addFloorOpening(opening);

        SelectionTranslationService.TranslationResult result = translationService.translate(
                level,
                Set.of(new SelectionKey(RenderableKind.FLOOR_OPENING, level.name(), opening.id().toString())),
                200.0,
                -100.0
        );

        assertEquals(new PlanPoint(1_200, 900), result.floorOpenings().getFirst().center());
    }

    @Test
    void verschiebtAusgewählteFbhSperrfläche() {
        Level level = new Level("Erdgeschoss");
        HeatingExclusionArea area = HeatingExclusionArea.create(
                java.util.UUID.randomUUID(),
                "Sperre",
                new PlanPoint(500, 600),
                new PlanPoint(1_500, 1_600)
        );
        level.addHeatingExclusionArea(area);

        SelectionTranslationService.TranslationResult result = translationService.translate(
                level,
                Set.of(new SelectionKey(RenderableKind.HEATING_EXCLUSION, level.name(), area.id().toString())),
                120.0,
                -80.0
        );

        assertEquals(new PlanPoint(620, 520), result.heatingExclusionAreas().getFirst().firstCorner());
        assertEquals(new PlanPoint(1_620, 1_520), result.heatingExclusionAreas().getFirst().oppositeCorner());
    }

    @Test
    void verschiebtAusgewähltenHeizkreis() {
        Level level = new Level("Erdgeschoss");
        HeatingZone zone = HeatingZone.create("HK 1", List.of(
                new PlanPoint(500, 600),
                new PlanPoint(1_500, 600),
                new PlanPoint(1_500, 1_600),
                new PlanPoint(500, 1_600)
        ));
        level.addHydronicHeating(HydronicHeating.create(
                java.util.UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.SPIRAL,
                Length.ofMillimeters(100),
                Length.ofMillimeters(11.6),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        ).withZones(List.of(zone)));

        SelectionTranslationService.TranslationResult result = translationService.translate(
                level,
                Set.of(new SelectionKey(RenderableKind.HEATING_ZONE, level.name(), zone.id().toString())),
                120.0,
                -80.0
        );

        assertTrue(result.changed());
        assertEquals(new PlanPoint(620, 520), result.hydronicHeatings().getFirst().zones().getFirst().outline().getFirst());
    }
}
