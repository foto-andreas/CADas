package de.schrell.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.room.AutoRoomGenerationService;
import de.schrell.cadas.application.room.AutoRoomGenerationService.RoomDefaults;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.infrastructure.dxf.DxfProjectExchangeService;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WallIntersectionSplitServiceTest {

    private static final UUID KIREP_BAD_FLUR_WALL_ID = UUID.fromString("454f6c96-9ccb-47f7-9b5e-89f59892059b");
    private static final PlanPoint KIREP_BAD_FLUR_SPLIT_POINT = new PlanPoint(-2_210.0, -8_209.179);

    private final WallIntersectionSplitService service = new WallIntersectionSplitService();
    private final AutoRoomGenerationService roomService = new AutoRoomGenerationService();

    @Test
    void teiltWandAnKreuzungUndBindetTuerUndBelagAnDasNeueSegment() {
        Level level = new Level("Erdgeschoss");
        Wall horizontalWall = Wall.create(
                new PlanSegment(new PlanPoint(0, 1_000), new PlanPoint(4_000, 1_000)),
                Length.ofMillimeters(120),
                Length.ofMillimeters(2_400)
        );
        Wall verticalWall = Wall.create(
                new PlanSegment(new PlanPoint(2_000, 0), new PlanPoint(2_000, 2_000)),
                Length.ofMillimeters(120),
                Length.ofMillimeters(2_400)
        );
        level.addWall(horizontalWall);
        level.addWall(verticalWall);
        level.addDoor(Door.create(
                horizontalWall.id(),
                Length.ofMillimeters(2_500),
                Length.ofMillimeters(500),
                Length.ofMillimeters(2_010),
                Length.zero()
        ));
        UUID roomId = UUID.randomUUID();
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, horizontalWall.id() + "@" + roomId);
        stack.addLayer(SurfaceLayer.create(
                "Fliese",
                Length.ofMillimeters(10),
                Length.ofMillimeters(600),
                Length.ofMillimeters(300),
                SurfaceLayoutMode.NONE,
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.ofMillimeters(2),
                "Test"
        ));
        level.addSurfaceLayerStack(stack);

        WallIntersectionSplitService.SplitResult result = service.split(
                level,
                List.of(horizontalWall.id()),
                new PlanPoint(2_000, 1_000),
                Length.ofMillimeters(20)
        );

        Wall firstWall = result.walls().stream()
                .filter(wall -> wall.id().equals(horizontalWall.id()))
                .findFirst()
                .orElseThrow();
        WallIntersectionSplitService.SplitWall split = result.splits().getFirst();

        assertEquals(3, result.walls().size());
        assertEquals(2_000.0, firstWall.axis().end().xMillimeters(), 0.001);
        assertEquals(1_000.0, firstWall.axis().end().yMillimeters(), 0.001);
        assertTrue(result.doors().stream().anyMatch(door ->
                door.wallId().equals(split.secondWallId())
                        && Math.abs(door.offsetFromStart().toMillimeters() - 500.0) < 0.001));
        assertTrue(result.surfaceLayerStacks().stream().anyMatch(candidate ->
                candidate.targetKey().equals(horizontalWall.id() + "@" + roomId)));
        assertTrue(result.surfaceLayerStacks().stream().anyMatch(candidate ->
                candidate.targetKey().equals(split.secondWallId() + "@" + roomId)));
    }

    @Test
    void erhältImKirepDachgeschossDieRaumIdsBeimAufteilenDerBadFlurWand() throws Exception {
        Level level = new DxfProjectExchangeService()
                .importProject(Path.of("KIREP.cadas"), "KIREP")
                .levels().stream()
                .filter(candidate -> candidate.name().equals("Dachgeschoss"))
                .findFirst()
                .orElseThrow();
        Room flur = level.rooms().stream().filter(room -> room.name().equals("Flur")).findFirst().orElseThrow();
        Room bad = level.rooms().stream().filter(room -> room.name().equals("Bad")).findFirst().orElseThrow();
        Room ally = level.rooms().stream().filter(room -> room.name().equals("Ally")).findFirst().orElseThrow();

        WallIntersectionSplitService.SplitResult result = service.split(
                level,
                List.of(KIREP_BAD_FLUR_WALL_ID),
                KIREP_BAD_FLUR_SPLIT_POINT,
                Length.ofMillimeters(80)
        );
        WallIntersectionSplitService.SplitWall split = result.splits().getFirst();
        level.replaceWalls(result.walls());
        level.replaceDoors(result.doors());
        level.replaceWindows(result.windows());
        level.replaceSurfaceLayerStacks(result.surfaceLayerStacks());

        List<Room> synchronizedRooms = roomService.synchronize(level, defaults());

        assertTrue(synchronizedRooms.stream().anyMatch(room -> room.id().equals(flur.id()) && room.name().equals("Flur")));
        assertTrue(synchronizedRooms.stream().anyMatch(room -> room.id().equals(bad.id()) && room.name().equals("Bad")));
        assertTrue(synchronizedRooms.stream().anyMatch(room -> room.id().equals(ally.id()) && room.name().equals("Ally")));
        assertTrue(result.surfaceLayerStacks().stream().anyMatch(candidate ->
                candidate.targetKey().equals(KIREP_BAD_FLUR_WALL_ID + "@" + flur.id())));
        assertTrue(result.surfaceLayerStacks().stream().anyMatch(candidate ->
                candidate.targetKey().equals(split.secondWallId() + "@" + flur.id())));
        assertTrue(result.surfaceLayerStacks().stream().anyMatch(candidate ->
                candidate.targetKey().equals(split.secondWallId() + "@" + bad.id())));
    }

    private RoomDefaults defaults() {
        return new RoomDefaults(
                "Raum",
                Length.ofMillimeters(2_400),
                Length.ofMillimeters(180),
                Length.ofMillimeters(200),
                null
        );
    }
}
