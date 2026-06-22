package de.schrell.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.FloorExtensionPlacement;
import de.schrell.cadas.domain.model.FloorExtensionType;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;

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
    void findetFbhSperrflächeAlsEigenständigesElement() {
        Level level = new Level("EG");
        HeatingExclusionArea area = HeatingExclusionArea.create(
                java.util.UUID.randomUUID(),
                "Sperre",
                new PlanPoint(500, 500),
                new PlanPoint(1_500, 1_200)
        );
        level.addHeatingExclusionArea(area);

        var selections = selectionQueryService.findSelections(level, new PlanPoint(800, 900), Length.ofMillimeters(10));

        assertEquals(RenderableKind.HEATING_EXCLUSION, selections.getFirst().kind());
        assertEquals(area.id().toString(), selections.getFirst().elementId());
    }

    @Test
    void findetHeizkreisVorRaum() {
        Level level = new Level("EG");
        Room room = Room.rectangular(
                "Bad",
                new PlanPoint(0, 0),
                new PlanPoint(3_000, 3_000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        HeatingZone zone = HeatingZone.create("HK 1", List.of(
                new PlanPoint(500, 500),
                new PlanPoint(2_000, 500),
                new PlanPoint(2_000, 2_000),
                new PlanPoint(500, 2_000)
        ));
        level.addRoom(room);
        level.addHydronicHeating(HydronicHeating.create(
                room.id(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.SPIRAL,
                Length.ofMillimeters(100),
                Length.ofMillimeters(11.6),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        ).withZones(List.of(zone)));

        var selections = selectionQueryService.findSelections(level, new PlanPoint(1_000, 1_000), Length.ofMillimeters(10));

        assertEquals(RenderableKind.HEATING_ZONE, selections.getFirst().kind());
        assertEquals(zone.id().toString(), selections.getFirst().elementId());
        assertEquals(RenderableKind.ROOM_VOLUME, selections.get(1).kind());
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
