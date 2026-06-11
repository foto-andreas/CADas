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
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.Wall;

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
}
