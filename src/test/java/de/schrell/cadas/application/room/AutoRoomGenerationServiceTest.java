package de.schrell.cadas.application.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.application.layers.WallSurfaceTargetKey;
import java.util.List;
import java.util.Set;
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
    void erkenntRaumTrotzGeringerZeichnungsungenauigkeit() {
        Level level = new Level("Erdgeschoss");
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 1.25), new PlanPoint(8_000, 0)),
                Length.of(26, LengthUnit.CENTIMETER),
                Length.of(2.6, LengthUnit.METER)
        ));
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(8_000.5, 0), new PlanPoint(8_000, 6_000)),
                Length.of(26, LengthUnit.CENTIMETER),
                Length.of(2.6, LengthUnit.METER)
        ));
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(8_000, 6_000), new PlanPoint(0, 5_990)),
                Length.of(26, LengthUnit.CENTIMETER),
                Length.of(2.6, LengthUnit.METER)
        ));
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 5_990), new PlanPoint(0, 1.25)),
                Length.of(26, LengthUnit.CENTIMETER),
                Length.of(2.6, LengthUnit.METER)
        ));

        List<Room> rooms = service.synchronize(level, defaults());

        assertEquals(1, rooms.size());
        assertEquals(7_740.0, rooms.getFirst().widthMillimeters(), 0.01);
        assertEquals(5_739.82, rooms.getFirst().depthMillimeters(), 0.01);
    }

    @Test
    void erkenntRaumAusAneinandergereihtenWändenMitKleinenAnschlusslücken() {
        Level level = new Level("Erdgeschoss");
        level.addWall(wall(0, 0, 1_997, 0));
        level.addWall(wall(2_002, 0, 4_000, 0));
        level.addWall(wall(4_000, 0, 4_000, 3_000));
        level.addWall(wall(4_000, 3_000, 0, 3_000));
        level.addWall(wall(0, 3_000, 0, 0));

        List<Room> rooms = service.synchronize(level, defaults());

        assertEquals(1, rooms.size());
        assertEquals(3_800.0, rooms.getFirst().widthMillimeters(), 0.001);
        assertEquals(2_800.0, rooms.getFirst().depthMillimeters(), 0.001);
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
    void leitetEckhoehenAuchBeiSichtbarenInnenwandlagenAb() {
        Level level = new Level("Erdgeschoss");
        addLoop(level, List.of(
                new PlanPoint(0, 0),
                new PlanPoint(4000, 0),
                new PlanPoint(4000, 3000),
                new PlanPoint(0, 3000)
        ), Length.of(20, LengthUnit.CENTIMETER));
        level.replaceWalls(List.of(
                level.walls().get(0).withEndpointHeights(Length.of(3.2, LengthUnit.METER), Length.of(2.6, LengthUnit.METER)),
                level.walls().get(1).withEndpointHeights(Length.of(2.6, LengthUnit.METER), Length.of(2.6, LengthUnit.METER)),
                level.walls().get(2).withEndpointHeights(Length.of(2.6, LengthUnit.METER), Length.of(2.6, LengthUnit.METER)),
                level.walls().get(3).withEndpointHeights(Length.of(2.6, LengthUnit.METER), Length.of(3.2, LengthUnit.METER))
        ));
        for (Wall wall : level.walls()) {
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, wall.id().toString());
            stack.addLayer(SurfaceLayer.create(
                    "Dämmplatte",
                    Length.of(8, LengthUnit.CENTIMETER),
                    Length.of(120, LengthUnit.CENTIMETER),
                    Length.of(60, LengthUnit.CENTIMETER),
                    Length.zero()
            ));
            level.addSurfaceLayerStack(stack);
        }

        List<Room> rooms = service.synchronize(level, flatDefaults());

        assertEquals(1, rooms.size());
        Room room = rooms.getFirst();
        assertTrue(room.hasVariableCeilingHeights());
        assertTrue(room.maximumCeilingHeightMillimeters() > 3150.0);
        assertTrue(room.minimumCeilingHeightMillimeters() < 2650.0);
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

    @Test
    void beruecksichtigtRaumbezogeneInnenwandlagenBeimSichtbarkeitswechsel() {
        Level level = new Level("Erdgeschoss");
        addLoop(level, List.of(
                new PlanPoint(0, 0),
                new PlanPoint(4000, 0),
                new PlanPoint(4000, 3000),
                new PlanPoint(0, 3000)
        ), Length.of(20, LengthUnit.CENTIMETER));
        List<Room> initialRooms = service.synchronize(level, defaults());
        level.replaceRooms(initialRooms);
        Room room = level.rooms().getFirst();

        for (Wall wall : level.walls()) {
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, WallSurfaceTargetKey.interior(wall.id(), room.id()));
            stack.addLayer(SurfaceLayer.create("Dämmplatte", Length.of(4, LengthUnit.CENTIMETER), Length.of(120, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.zero()));
            level.addSurfaceLayerStack(stack);
        }

        List<Room> visibleRooms = service.synchronize(level, defaults());
        assertEquals(140.0, visibleRooms.getFirst().minXMillimeters(), 0.001);
        assertEquals(3860.0, visibleRooms.getFirst().maxXMillimeters(), 0.001);

        level.replaceRooms(visibleRooms);
        level.surfaceLayerStacks().forEach(stack -> stack.setVisibility(stack.layers().getFirst().id(), false));
        List<Room> hiddenRooms = service.synchronize(level, defaults());
        assertEquals(100.0, hiddenRooms.getFirst().minXMillimeters(), 0.001);
        assertEquals(3900.0, hiddenRooms.getFirst().maxXMillimeters(), 0.001);
    }

    @Test
    void leitetInneneckenOhneTreppenstufeZurWandmitteAb() {
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
        assertEquals(List.of(
                new PlanPoint(100.0, 100.0),
                new PlanPoint(4900.0, 100.0),
                new PlanPoint(4900.0, 1400.0),
                new PlanPoint(2900.0, 1400.0),
                new PlanPoint(2900.0, 3900.0),
                new PlanPoint(100.0, 3900.0)
        ), rooms.getFirst().outline());
    }

    @Test
    void teiltBestehendenRaumAnAutomatischErkanntenTKanten() {
        Level level = new Level("Erdgeschoss");
        addLoop(level, List.of(
                new PlanPoint(0, 0),
                new PlanPoint(6000, 0),
                new PlanPoint(6000, 4000),
                new PlanPoint(0, 4000)
        ), Length.of(20, LengthUnit.CENTIMETER));
        level.replaceRooms(service.synchronize(level, defaults()));
        java.util.UUID originalRoomId = level.rooms().getFirst().id();
        Wall partition = Wall.create(
                new PlanSegment(new PlanPoint(3000, 0), new PlanPoint(3000, 4000)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(partition);

        List<Room> rooms = service.synchronizeFromSelectedWalls(
                level,
                level.walls().stream().map(Wall::id).collect(java.util.stream.Collectors.toSet()),
                defaults()
        );

        assertEquals(2, rooms.size());
        assertEquals(1, rooms.stream().filter(room -> room.id().equals(originalRoomId)).count());
        assertTrue(rooms.stream().allMatch(room -> Math.abs(room.areaSquareMeters() - 10.64) < 0.02));
    }

    @Test
    void behaeltNichtVonDerAuswahlBetroffeneRaeume() {
        Level level = new Level("Erdgeschoss");
        addLoop(level, List.of(
                new PlanPoint(0, 0),
                new PlanPoint(4000, 0),
                new PlanPoint(4000, 3000),
                new PlanPoint(0, 3000)
        ), Length.of(20, LengthUnit.CENTIMETER));
        int firstLoopWallCount = level.walls().size();
        addLoop(level, List.of(
                new PlanPoint(6000, 0),
                new PlanPoint(10000, 0),
                new PlanPoint(10000, 3000),
                new PlanPoint(6000, 3000)
        ), Length.of(20, LengthUnit.CENTIMETER));
        level.replaceRooms(service.synchronizeFromSelectedWalls(
                level,
                level.walls().stream().map(Wall::id).collect(java.util.stream.Collectors.toSet()),
                defaults()
        ));
        Room unaffected = level.rooms().stream().max(java.util.Comparator.comparingDouble(Room::minXMillimeters)).orElseThrow();
        Set<java.util.UUID> firstLoopWallIds = level.walls().subList(0, firstLoopWallCount).stream()
                .map(Wall::id)
                .collect(java.util.stream.Collectors.toSet());

        List<Room> rooms = service.synchronizeFromSelectedWalls(level, firstLoopWallIds, defaults());

        assertEquals(2, rooms.size());
        assertTrue(rooms.stream().anyMatch(room -> room.id().equals(unaffected.id())));
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

    private AutoRoomGenerationService.RoomDefaults flatDefaults() {
        return new AutoRoomGenerationService.RoomDefaults(
                "Raum",
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER),
                null
        );
    }

    private void addLoop(Level level, List<PlanPoint> points, Length thickness) {
        for (int index = 0; index < points.size(); index++) {
            PlanPoint start = points.get(index);
            PlanPoint end = points.get((index + 1) % points.size());
            level.addWall(Wall.create(new PlanSegment(start, end), thickness, Length.of(2.8, LengthUnit.METER)));
        }
    }

    private Wall wall(double startX, double startY, double endX, double endY) {
        return Wall.create(
                new PlanSegment(new PlanPoint(startX, startY), new PlanPoint(endX, endY)),
                Length.ofMillimeters(200),
                Length.ofMillimeters(2_800)
        );
    }
}
