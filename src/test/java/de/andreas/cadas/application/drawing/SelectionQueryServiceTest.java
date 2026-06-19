package de.andreas.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.application.view.SelectionKey;
import de.andreas.cadas.application.view.RenderableKind;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.FloorExtension;
import de.andreas.cadas.domain.model.FloorExtensionPlacement;
import de.andreas.cadas.domain.model.FloorExtensionType;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import java.util.List;

import org.junit.jupiter.api.Test;

class SelectionQueryServiceTest {

    private static final Length TOLERANCE = Length.of(12, LengthUnit.CENTIMETER);

    private final SelectionQueryService selectionQueryService = new SelectionQueryService();

    @Test
    void findetBalkonAlsEigenständigesElement() {
        Level level = new Level("EG");
        FloorExtension balcony = FloorExtension.create(FloorExtensionType.BALCONY, FloorExtensionPlacement.EXTERIOR,
                new PlanPoint(0, 0), new PlanPoint(2_000, 1_000), Length.ofMillimeters(180));
        level.addFloorExtension(balcony);

        var selections = selectionQueryService.findSelections(level, new PlanPoint(500, 500), Length.ofMillimeters(10));

        assertEquals(RenderableKind.FLOOR_EXTENSION, selections.getFirst().kind());
        assertEquals(balcony.id().toString(), selections.getFirst().elementId());
    }

    @Test
    void priorisiertOeffnungenVorRaeumenTreppenUndWaenden() {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(5_000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
        level.addWall(wall);
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(5_000, 4_000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(room);
        Door door = Door.create(
                wall.id(),
                Length.of(1.0, LengthUnit.METER),
                Length.of(1.0, LengthUnit.METER),
                Length.of(2.01, LengthUnit.METER),
                Length.zero()
        );
        level.addDoor(door);
        WindowElement window = WindowElement.create(
                wall.id(),
                Length.of(3.0, LengthUnit.METER),
                Length.of(1.2, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.2, LengthUnit.METER)
        );
        level.addWindow(window);
        Staircase staircase = Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(500, 500),
                new PlanPoint(1_500, 2_500),
                Length.of(2.8, LengthUnit.METER),
                14
        );
        level.addStaircase(staircase);

        assertEquals(
                RenderableKind.DOOR,
                selectionQueryService.findSelection(level, new PlanPoint(1_500, 0), TOLERANCE).orElseThrow().kind()
        );
        assertEquals(
                RenderableKind.WINDOW,
                selectionQueryService.findSelection(level, new PlanPoint(3_400, 0), TOLERANCE).orElseThrow().kind()
        );
        assertEquals(
                RenderableKind.ROOM_VOLUME,
                selectionQueryService.findSelection(level, new PlanPoint(4_500, 3_500), TOLERANCE).orElseThrow().kind()
        );
        assertEquals(
                RenderableKind.STAIR,
                selectionQueryService.findSelection(level, new PlanPoint(1_000, 1_500), TOLERANCE).orElseThrow().kind()
        );
        assertEquals(
                RenderableKind.WALL,
                selectionQueryService.findSelection(level, new PlanPoint(100, 0), TOLERANCE).orElseThrow().kind()
        );
    }

    @Test
    void liefertLeerWennKeinElementGetroffenWird() {
        Level level = new Level("Leeres Geschoss");

        assertTrue(selectionQueryService.findSelection(level, new PlanPoint(10_000, 10_000), TOLERANCE).isEmpty());
    }

    @Test
    void liefertAlleUebereinanderliegendenTrefferInPrioritaetsreihenfolge() {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(5_000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, -500),
                new PlanPoint(5_000, 2_500),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addWall(wall);
        level.addRoom(room);

        List<SelectionKey> selections = selectionQueryService.findSelections(level, new PlanPoint(2_500, 0), TOLERANCE);

        assertEquals(2, selections.size());
        assertEquals(RenderableKind.WALL, selections.getFirst().kind());
        assertEquals(RenderableKind.ROOM_VOLUME, selections.get(1).kind());
        assertEquals(selections.getFirst(), selectionQueryService.findSelection(level, new PlanPoint(2_500, 0), TOLERANCE).orElseThrow());
    }
}
