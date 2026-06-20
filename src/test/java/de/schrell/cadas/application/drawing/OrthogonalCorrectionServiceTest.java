package de.schrell.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.Grid;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.Wall;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrthogonalCorrectionServiceTest {

    private final OrthogonalCorrectionService service = new OrthogonalCorrectionService();
    private final Grid grid = new Grid(Length.ofMillimeters(10));

    @Test
    void korrigiertWandBisZehnGradUndHältAnschlussZusammen() {
        Level level = new Level("Erdgeschoss");
        Wall selected = wall(0, 0, 997, 105);
        Wall connected = wall(997, 105, 1_000, 1_000);
        level.addWall(selected);
        level.addWall(connected);

        OrthogonalCorrectionService.CorrectionResult result = service.correct(
                level,
                Set.of(key(RenderableKind.WALL, selected.id().toString())),
                grid,
                10.0
        );

        assertTrue(result.changed());
        assertEquals(new PlanPoint(1_000, 0), result.walls().getFirst().axis().end());
        assertEquals(new PlanPoint(1_000, 0), result.walls().get(1).axis().start());
    }

    @Test
    void lässtGrößereAbweichungUnverändert() {
        Level level = new Level("Erdgeschoss");
        Wall selected = wall(0, 0, 1_000, 200);
        level.addWall(selected);

        OrthogonalCorrectionService.CorrectionResult result = service.correct(
                level,
                Set.of(key(RenderableKind.WALL, selected.id().toString())),
                grid,
                10.0
        );

        assertFalse(result.changed());
        assertEquals(selected, result.walls().getFirst());
    }

    @Test
    void korrigiertObjektwinkelAufNächsteNeunzigGrad() {
        Level level = new Level("Erdgeschoss");
        RoomObject roomObject = RoomObject.create(
                "quader", "Quader", RoomObjectType.CUBOID, RoomObjectShape.RECTANGLE,
                new PlanPoint(500, 500), Length.ofMillimeters(500), Length.ofMillimeters(300),
                Length.ofMillimeters(700), 84.0, RoomObjectMountingMode.STANDS_ON_COVERING, ""
        );
        level.addRoomObject(roomObject);

        OrthogonalCorrectionService.CorrectionResult result = service.correct(
                level,
                Set.of(key(RenderableKind.ROOM_OBJECT, roomObject.id().toString())),
                grid,
                10.0
        );

        assertTrue(result.changed());
        assertEquals(90.0, result.roomObjects().getFirst().rotationDegrees(), 0.001);
    }

    private Wall wall(double startX, double startY, double endX, double endY) {
        return Wall.create(
                new PlanSegment(new PlanPoint(startX, startY), new PlanPoint(endX, endY)),
                Length.ofMillimeters(200),
                Length.ofMillimeters(2_750)
        );
    }

    private SelectionKey key(RenderableKind kind, String elementId) {
        return new SelectionKey(kind, "Erdgeschoss", elementId);
    }
}
