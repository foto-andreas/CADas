package de.andreas.cadas.application.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SlopedCeilingProfile;
import de.andreas.cadas.domain.model.SlopedCeilingSide;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;
import java.util.List;
import org.junit.jupiter.api.Test;

class AutoRoomGenerationServiceTest {

    private final AutoRoomGenerationService service = new AutoRoomGenerationService();

    @Test
    void leitetRechteckigenInnenraumAusWandinnenkantenAb() {
        Level level = new Level("Erdgeschoss");
        addLoop(level, List.of(
                new PlanPoint(0, 0),
                new PlanPoint(4000, 0),
                new PlanPoint(4000, 3000),
                new PlanPoint(0, 3000)
        ), Length.of(20, LengthUnit.CENTIMETER));

        List<Room> rooms = service.synchronize(level, defaults());

        assertEquals(1, rooms.size());
        Room room = rooms.getFirst();
        assertEquals(4, room.outline().size());
        assertEquals(100.0, room.minXMillimeters(), 0.001);
        assertEquals(3900.0, room.maxXMillimeters(), 0.001);
        assertEquals(100.0, room.minYMillimeters(), 0.001);
        assertEquals(2900.0, room.maxYMillimeters(), 0.001);
    }

    @Test
    void leitetPolygonalenRaumAusLfoermigemWandzugAb() {
        Level level = new Level("Erdgeschoss");
        addLoop(level, List.of(
                new PlanPoint(0, 0),
                new PlanPoint(5000, 0),
                new PlanPoint(5000, 1500),
                new PlanPoint(3000, 1500),
                new PlanPoint(3000, 4000),
                new PlanPoint(0, 4000)
        ), Length.of(20, LengthUnit.CENTIMETER));

        List<Room> rooms = service.synchronize(level, defaults());

        assertEquals(1, rooms.size());
        Room room = rooms.getFirst();
        assertTrue(room.outline().size() >= 6);
        assertEquals(13.24, room.areaSquareMeters(), 0.01);
    }

    @Test
    void behaeltRaumeigenschaftenBeimVerschiebenVonWaenden() {
        Level level = new Level("Dachgeschoss");
        addLoop(level, List.of(
                new PlanPoint(0, 0),
                new PlanPoint(4000, 0),
                new PlanPoint(4000, 3000),
                new PlanPoint(0, 3000)
        ), Length.of(20, LengthUnit.CENTIMETER));

        List<Room> firstRooms = service.synchronize(level, defaults());
        Room firstRoom = firstRooms.getFirst();
        level.replaceRooms(firstRooms);

        level.replaceWalls(List.of(
                level.walls().get(0),
                level.walls().get(1),
                new Wall(level.walls().get(2).id(), new PlanSegment(new PlanPoint(4500, 3000), new PlanPoint(0, 3000)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)),
                level.walls().get(3)
        ));

        List<Room> updatedRooms = service.synchronize(level, defaults());

        assertEquals(1, updatedRooms.size());
        assertEquals(firstRoom.id(), updatedRooms.getFirst().id());
        assertEquals("Dachraum", updatedRooms.getFirst().name());
        assertEquals(SlopedCeilingSide.NORTH, updatedRooms.getFirst().slopedCeilingProfile().orElseThrow().lowSide());
    }

    @Test
    void passtRaumkonturBeiVerschobenerEckeMitSchraegenWaendenAn() {
        Level level = new Level("Erdgeschoss");
        addLoop(level, List.of(
                new PlanPoint(0, 0),
                new PlanPoint(4000, 0),
                new PlanPoint(4000, 3000),
                new PlanPoint(0, 3000)
        ), Length.of(20, LengthUnit.CENTIMETER));

        level.replaceWalls(List.of(
                new Wall(level.walls().get(0).id(), new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4200, 200)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)),
                new Wall(level.walls().get(1).id(), new PlanSegment(new PlanPoint(4200, 200), new PlanPoint(4000, 3000)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)),
                level.walls().get(2),
                level.walls().get(3)
        ));

        List<Room> rooms = service.synchronize(level, defaults());

        assertEquals(1, rooms.size());
        assertTrue(rooms.getFirst().outline().size() >= 4);
        assertTrue(rooms.getFirst().maxXMillimeters() > 3900.0);
        assertTrue(rooms.getFirst().minYMillimeters() > 0.0);
    }

    @Test
    void leitetEckhoehenInPolygonaleRaumdeckeUndVolumenAb() {
        Level level = new Level("Dachgeschoss");
        level.addWall(new Wall(
                java.util.UUID.randomUUID(),
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(3.1, LengthUnit.METER),
                Length.of(2.4, LengthUnit.METER),
                Length.of(3.1, LengthUnit.METER)
        ));
        level.addWall(new Wall(
                java.util.UUID.randomUUID(),
                new PlanSegment(new PlanPoint(4000, 0), new PlanPoint(4000, 3000)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(3.1, LengthUnit.METER),
                Length.of(3.1, LengthUnit.METER),
                Length.of(3.1, LengthUnit.METER)
        ));
        level.addWall(new Wall(
                java.util.UUID.randomUUID(),
                new PlanSegment(new PlanPoint(4000, 3000), new PlanPoint(0, 3000)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(3.1, LengthUnit.METER),
                Length.of(3.1, LengthUnit.METER),
                Length.of(2.6, LengthUnit.METER)
        ));
        level.addWall(new Wall(
                java.util.UUID.randomUUID(),
                new PlanSegment(new PlanPoint(0, 3000), new PlanPoint(0, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.6, LengthUnit.METER),
                Length.of(2.6, LengthUnit.METER),
                Length.of(2.4, LengthUnit.METER)
        ));

        List<Room> rooms = service.synchronize(level, defaults());

        assertEquals(1, rooms.size());
        assertEquals(4, rooms.getFirst().ceilingVertexHeights().size());
        assertTrue(rooms.getFirst().minimumCeilingHeightMillimeters() > 2400.0);
        assertTrue(rooms.getFirst().minimumCeilingHeightMillimeters() < 2450.0);
        assertTrue(rooms.getFirst().maximumCeilingHeightMillimeters() > 3050.0);
        assertTrue(rooms.getFirst().maximumCeilingHeightMillimeters() <= 3100.0);
        assertTrue(rooms.getFirst().volumeCubicMeters() > 29.5);
        assertTrue(rooms.getFirst().volumeCubicMeters() < 30.1);
    }

    @Test
    void beruecksichtigtSichtbareWandInnenlagenBeiDerRaumkontur() {
        Level level = new Level("Erdgeschoss");
        addLoop(level, List.of(
                new PlanPoint(0, 0),
                new PlanPoint(4000, 0),
                new PlanPoint(4000, 3000),
                new PlanPoint(0, 3000)
        ), Length.of(20, LengthUnit.CENTIMETER));
        for (Wall wall : level.walls()) {
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, wall.id().toString());
            stack.addLayer(SurfaceLayer.create("Rigips", Length.of(2, LengthUnit.CENTIMETER), Length.of(125, LengthUnit.CENTIMETER), Length.of(200, LengthUnit.CENTIMETER), Length.zero()));
            level.addSurfaceLayerStack(stack);
        }

        List<Room> rooms = service.synchronize(level, defaults());

        assertEquals(1, rooms.size());
        assertEquals(120.0, rooms.getFirst().minXMillimeters(), 0.001);
        assertEquals(3880.0, rooms.getFirst().maxXMillimeters(), 0.001);
    }

    @Test
    void summiertMehrereSichtbareWandlagenIterativUndIgnoriertVersteckte() {
        Level level = new Level("Erdgeschoss");
        addLoop(level, List.of(
                new PlanPoint(0, 0),
                new PlanPoint(4000, 0),
                new PlanPoint(4000, 3000),
                new PlanPoint(0, 3000)
        ), Length.of(20, LengthUnit.CENTIMETER));
        for (Wall wall : level.walls()) {
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, wall.id().toString());
            stack.addLayer(SurfaceLayer.create("Rigips", Length.of(2, LengthUnit.CENTIMETER), Length.of(125, LengthUnit.CENTIMETER), Length.of(200, LengthUnit.CENTIMETER), Length.zero()));
            stack.addLayer(SurfaceLayer.create("Ausgleich", Length.of(1, LengthUnit.CENTIMETER), Length.of(125, LengthUnit.CENTIMETER), Length.of(200, LengthUnit.CENTIMETER), Length.zero()));
            stack.addLayer(SurfaceLayer.create("Verdeckt", Length.of(0.5, LengthUnit.CENTIMETER), Length.of(125, LengthUnit.CENTIMETER), Length.of(200, LengthUnit.CENTIMETER), Length.zero()).withVisibility(false));
            level.addSurfaceLayerStack(stack);
        }

        List<Room> rooms = service.synchronize(level, defaults());

        assertEquals(1, rooms.size());
        assertEquals(130.0, rooms.getFirst().minXMillimeters(), 0.001);
        assertEquals(3870.0, rooms.getFirst().maxXMillimeters(), 0.001);
        assertEquals(130.0, rooms.getFirst().minYMillimeters(), 0.001);
        assertEquals(2870.0, rooms.getFirst().maxYMillimeters(), 0.001);
    }

    private AutoRoomGenerationService.RoomDefaults defaults() {
        return new AutoRoomGenerationService.RoomDefaults(
                "Dachraum",
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                new SlopedCeilingProfile(SlopedCeilingSide.NORTH, Length.of(1.0, LengthUnit.METER))
        );
    }

    private void addLoop(Level level, List<PlanPoint> points, Length thickness) {
        for (int index = 0; index < points.size(); index++) {
            PlanPoint start = points.get(index);
            PlanPoint end = points.get((index + 1) % points.size());
            level.addWall(Wall.create(new PlanSegment(start, end), thickness, Length.of(2.8, LengthUnit.METER)));
        }
    }
}
