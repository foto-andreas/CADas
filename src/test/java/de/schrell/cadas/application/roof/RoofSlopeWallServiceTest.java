package de.schrell.cadas.application.roof;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.room.AutoRoomGenerationService;
import de.schrell.cadas.application.room.AutoRoomGenerationService.RoomDefaults;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoofSlopeWallServiceTest {

    private final RoofSlopeWallService service = new RoofSlopeWallService();
    private final AutoRoomGenerationService roomService = new AutoRoomGenerationService();

    @Test
    void bautDachschrägeVonGewählterWandinnenkanteAutomatischAuf() {
        Level level = rectangularLevel();
        Wall lowWall = level.walls().getFirst();

        RoofSlopeWallService.RoofSlopeResult result = service.apply(
                level, lowWall.id(), Length.ofMillimeters(1_000), Length.ofMillimeters(1_200)
        );

        Wall updatedLowWall = result.walls().stream().filter(wall -> wall.id().equals(lowWall.id())).findFirst().orElseThrow();
        assertEquals(1_000.0, updatedLowWall.heightAtStart(), 0.001);
        assertEquals(1_000.0, updatedLowWall.heightAtEnd(), 0.001);
        assertEquals(6, result.walls().size());
        assertEquals(2, result.walls().stream().filter(Wall::hasPolygonalProfile).count());
        Room room = result.rooms().getFirst();
        assertEquals(SlopedCeilingSide.NORTH, room.slopedCeilingProfile().orElseThrow().lowSide());
        assertEquals(1_200.0, room.slopedCeilingProfile().orElseThrow().horizontalRun().toMillimeters(), 0.001);
        assertEquals(1_000.0, room.ceilingHeightAt(new PlanPoint(2_000, 100)), 0.001);
        assertEquals(2_800.0, room.ceilingHeightAt(new PlanPoint(2_000, 1_300)), 0.001);
        assertEquals(2_800.0, room.ceilingHeightAt(new PlanPoint(2_000, 2_000)), 0.001);
    }

    @Test
    void ergänztMehrereDachschrägenAnVerschiedenenRaumseiten() {
        Level level = rectangularLevel();
        Wall northWall = level.walls().getFirst();
        Wall southWall = level.walls().get(2);

        RoofSlopeWallService.RoofSlopeResult northResult = service.apply(
                level, northWall.id(), Length.ofMillimeters(1_000), Length.ofMillimeters(1_200)
        );
        level.replaceWalls(northResult.walls());
        level.replaceRooms(northResult.rooms());
        level.replaceDoors(northResult.doors());
        level.replaceWindows(northResult.windows());

        RoofSlopeWallService.RoofSlopeResult southResult = service.apply(
                level, southWall.id(), Length.ofMillimeters(900), Length.ofMillimeters(800)
        );

        Room room = southResult.rooms().getFirst();
        assertEquals(2, room.slopedCeilingProfiles().size());
        assertEquals(1_000.0, room.ceilingHeightAt(new PlanPoint(2_000, 0)), 0.001);
        assertEquals(900.0, room.ceilingHeightAt(new PlanPoint(2_000, 3_000)), 0.001);
        assertEquals(2_800.0, room.ceilingHeightAt(new PlanPoint(2_000, 1_500)), 0.001);
    }

    @Test
    void bindetFensterHinterDerWandteilungAnDasNeueSegment() {
        Level level = rectangularLevel();
        Wall sideWall = level.walls().get(1);
        WindowElement window = WindowElement.create(
                sideWall.id(), Length.ofMillimeters(1_800), Length.ofMillimeters(500),
                Length.ofMillimeters(900), Length.ofMillimeters(1_000)
        );
        level.addWindow(window);

        RoofSlopeWallService.RoofSlopeResult result = service.apply(
                level, level.walls().getFirst().id(), Length.ofMillimeters(1_000), Length.ofMillimeters(1_200)
        );

        WindowElement rebound = result.windows().getFirst();
        assertNotEquals(sideWall.id(), rebound.wallId());
        assertEquals(600.0, rebound.offsetFromStart().toMillimeters(), 0.001);
        assertTrue(result.walls().stream().anyMatch(wall -> wall.id().equals(rebound.wallId())));
    }

    @Test
    void erhältManuelleSchrägenbreiteBeiErneuterRaumableitung() {
        Level level = rectangularLevel();
        RoofSlopeWallService.RoofSlopeResult result = service.apply(
                level, level.walls().getFirst().id(), Length.ofMillimeters(1_000), Length.ofMillimeters(1_200)
        );
        level.replaceWalls(result.walls());
        level.replaceRooms(result.rooms());

        List<Room> synchronizedRooms = roomService.synchronize(level, defaults());

        assertTrue(synchronizedRooms.getFirst().ceilingVertexHeightsProfile().isEmpty());
        assertEquals(1_200.0, synchronizedRooms.getFirst().slopedCeilingProfile().orElseThrow().horizontalRun().toMillimeters(), 0.001);
    }

    private Level rectangularLevel() {
        Level level = new Level("Dachgeschoss");
        addWall(level, 0, 0, 4_000, 0);
        addWall(level, 4_000, 0, 4_000, 3_000);
        addWall(level, 4_000, 3_000, 0, 3_000);
        addWall(level, 0, 3_000, 0, 0);
        level.replaceRooms(roomService.synchronize(level, defaults()));
        return level;
    }

    private void addWall(Level level, double startX, double startY, double endX, double endY) {
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(startX, startY), new PlanPoint(endX, endY)),
                Length.ofMillimeters(200), Length.ofMillimeters(2_800)
        ));
    }

    private RoomDefaults defaults() {
        return new RoomDefaults(
                "Raum", Length.ofMillimeters(2_800), Length.ofMillimeters(180), Length.ofMillimeters(200), null
        );
    }
}
