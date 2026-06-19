package de.andreas.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.andreas.cadas.application.view.RenderableKind;
import de.andreas.cadas.application.view.SelectionKey;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import java.util.Set;

import org.junit.jupiter.api.Test;

class EdgeResizeServiceTest {

    private final EdgeResizeService service = new EdgeResizeService();

    @Test
    void verschiebtWandanfangUndHaeltOeffnungAnWeltposition() {
        Level level = level();
        Wall wall = level.walls().getFirst();
        Door door = level.doors().getFirst();
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.WALL_START,
                wall.id(),
                wall.id(),
                wall.axis().start()
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(-500, 0));

        assertEquals(-500.0, result.walls().getFirst().axis().start().xMillimeters(), 0.001);
        assertEquals(1_500.0, result.doors().getFirst().offsetFromStart().toMillimeters(), 0.001);
        assertEquals(1_000.0, result.doors().getFirst().width().toMillimeters(), 0.001);
    }

    @Test
    void verschiebtNurGezogeneTuerkante() {
        Level level = level();
        Door door = level.doors().getFirst();
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.DOOR_START,
                door.id(),
                door.wallId(),
                new PlanPoint(1_000, 0)
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(1_250, 80));

        assertEquals(1_250.0, result.doors().getFirst().offsetFromStart().toMillimeters(), 0.001);
        assertEquals(750.0, result.doors().getFirst().width().toMillimeters(), 0.001);
    }

    @Test
    void findetHandleNurAnSelektiertemElement() {
        Level level = level();
        Door door = level.doors().getFirst();

        EdgeResizeService.EdgeHandle handle = service.findHandle(
                level,
                Set.of(new SelectionKey(RenderableKind.DOOR, level.name(), door.id().toString())),
                new PlanPoint(2_010, 5),
                Length.ofMillimeters(20)
        ).orElseThrow();

        assertEquals(EdgeResizeService.EdgeHandleKind.DOOR_END, handle.kind());
    }

    @Test
    void liefertEckHandlesFuerSelektierteTreppe() {
        Level level = levelMitTreppe();
        Staircase staircase = level.staircases().getFirst();

        var handles = service.handles(level, Set.of(new SelectionKey(RenderableKind.STAIR, level.name(), staircase.id().toString())));

        assertEquals(2, handles.size());
    }

    @Test
    void aendertTreppeDurchZiehenDerErstenEcke() {
        Level level = levelMitTreppe();
        Staircase staircase = level.staircases().getFirst();
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.STAIR_FIRST_CORNER,
                staircase.id(),
                staircase.id(),
                staircase.firstCorner()
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(200, 300));

        Staircase resized = result.staircases().getFirst();
        assertEquals(200.0, resized.firstCorner().xMillimeters(), 0.001);
        assertEquals(300.0, resized.firstCorner().yMillimeters(), 0.001);
        assertEquals(staircase.oppositeCorner().xMillimeters(), resized.oppositeCorner().xMillimeters(), 0.001);
        assertEquals(staircase.oppositeCorner().yMillimeters(), resized.oppositeCorner().yMillimeters(), 0.001);
    }

    @Test
    void aendertTreppeDurchZiehenDerGegenueberliegendenEcke() {
        Level level = levelMitTreppe();
        Staircase staircase = level.staircases().getFirst();
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.STAIR_OPPOSITE_CORNER,
                staircase.id(),
                staircase.id(),
                staircase.oppositeCorner()
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(2_800, 1_700));

        Staircase resized = result.staircases().getFirst();
        assertEquals(staircase.firstCorner().xMillimeters(), resized.firstCorner().xMillimeters(), 0.001);
        assertEquals(staircase.firstCorner().yMillimeters(), resized.firstCorner().yMillimeters(), 0.001);
        assertEquals(2_800.0, resized.oppositeCorner().xMillimeters(), 0.001);
        assertEquals(1_700.0, resized.oppositeCorner().yMillimeters(), 0.001);
    }

    private Level level() {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(5_000, 0)),
                Length.ofMillimeters(200),
                Length.ofMillimeters(2_800)
        );
        level.addWall(wall);
        level.addDoor(Door.create(wall.id(), Length.ofMillimeters(1_000), Length.ofMillimeters(1_000), Length.ofMillimeters(2_010), Length.zero()));
        level.addWindow(WindowElement.create(wall.id(), Length.ofMillimeters(3_000), Length.ofMillimeters(1_000), Length.ofMillimeters(900), Length.ofMillimeters(1_200)));
        return level;
    }

    private Level levelMitTreppe() {
        Level level = new Level("Erdgeschoss");
        level.addStaircase(Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(1_000, 1_000),
                new PlanPoint(2_500, 2_000),
                Length.ofMillimeters(2_800),
                16
        ));
        return level;
    }
}
