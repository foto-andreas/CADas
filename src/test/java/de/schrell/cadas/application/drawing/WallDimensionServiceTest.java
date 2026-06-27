package de.schrell.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.room.AutoRoomGenerationService;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.infrastructure.dxf.DxfProjectExchangeService;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WallDimensionServiceTest {

    private final WallDimensionService service = new WallDimensionService();

    @Test
    void ermitteltRaumUndAussenmassEinerGeschlossenenAussenwand() {
        Level level = new Level("Erdgeschoss");
        Wall selectedWall = addRectangle(level, 4_000, 3_000, 200).getFirst();
        synchronizeRooms(level);

        WallDimensionService.WallDimensions dimensions = service.dimensions(level, selectedWall);

        assertEquals(1, dimensions.roomDimensions().size());
        WallDimensionService.SideDimension roomDimension = dimensions.roomDimensions().getFirst();
        assertEquals(3_800.0, roomDimension.length().toMillimeters(), 0.001);
        assertEquals(1.0, roomDimension.sideSign());
        assertSegmentOnLine(roomDimension.dimensionSegment(), 100.0, 100.0, 3_900.0, 100.0);

        WallDimensionService.SideDimension exteriorDimension = dimensions.exteriorDimension().orElseThrow();
        assertEquals(4_200.0, exteriorDimension.length().toMillimeters(), 0.001);
        assertEquals(-1.0, exteriorDimension.sideSign());
        assertSegmentOnLine(exteriorDimension.dimensionSegment(), -100.0, -100.0, 4_100.0, -100.0);
    }

    @Test
    void zeigtBeiInnenwandBeideRaummasseAberKeinAussenmass() {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(2_000, 0), new PlanPoint(2_000, 3_000)),
                Length.ofMillimeters(100),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(wall);
        level.addRoom(Room.rectangular("Links", new PlanPoint(100, 100), new PlanPoint(1_950, 2_900), Length.of(2.6, LengthUnit.METER), Length.zero(), Length.zero()));
        level.addRoom(Room.rectangular("Rechts", new PlanPoint(2_050, 100), new PlanPoint(3_900, 2_900), Length.of(2.6, LengthUnit.METER), Length.zero(), Length.zero()));

        WallDimensionService.WallDimensions dimensions = service.dimensions(level, wall);

        assertEquals(2, dimensions.roomDimensions().size());
        assertTrue(dimensions.roomDimensions().stream().allMatch(dimension -> Math.abs(dimension.length().toMillimeters() - 2_800.0) < 0.001));
        assertTrue(dimensions.exteriorDimension().isEmpty());
    }

    @Test
    void ignoriertNahezuParalleleAnschlusswandBeimAussenmass() {
        Level level = new Level("Erdgeschoss");
        Wall selectedWall = wall(0, 0, 4_000, 0, 200);
        level.addWall(selectedWall);
        level.addWall(wall(4_000, 0, 6_000, -2, 200));
        level.addRoom(Room.rectangular(
                "Raum",
                new PlanPoint(0, -3_000),
                new PlanPoint(4_000, 0),
                Length.of(2.6, LengthUnit.METER),
                Length.zero(),
                Length.zero()
        ));

        WallDimensionService.WallDimensions dimensions = service.dimensions(level, selectedWall);

        WallDimensionService.SideDimension exteriorDimension = dimensions.exteriorDimension().orElseThrow();
        assertEquals(1.0, exteriorDimension.sideSign());
        assertEquals(4_000.0, exteriorDimension.length().toMillimeters(), 0.001);
        assertSegmentOnLine(exteriorDimension.dimensionSegment(), 0.0, 100.0, 4_000.0, 100.0);
    }

    @Test
    void begrenztKirepAussenmasseBeiFastParallelenDachgeschosswaenden() throws Exception {
        Level level = new DxfProjectExchangeService()
                .importProject(Path.of("KIREP.cadas"), "KIREP")
                .levels().stream()
                .filter(candidate -> candidate.name().equals("Dachgeschoss"))
                .findFirst()
                .orElseThrow();
        Wall firstWall = findWall(level, "c202572d-e23d-40c5-a999-9cba1c1086c2");
        Wall secondWall = findWall(level, "11d23ce0-c53d-47dd-8e86-86dde854dd7b");

        double firstExteriorLength = service.dimensions(level, firstWall).exteriorDimension().orElseThrow().length().toMillimeters();
        double secondExteriorLength = service.dimensions(level, secondWall).exteriorDimension().orElseThrow().length().toMillimeters();

        assertTrue(firstExteriorLength > 4_000.0 && firstExteriorLength < 6_000.0);
        assertTrue(secondExteriorLength > 1_500.0 && secondExteriorLength < 3_000.0);
    }

    private List<Wall> addRectangle(Level level, double width, double height, double thickness) {
        List<Wall> walls = List.of(
                wall(0, 0, width, 0, thickness),
                wall(width, 0, width, height, thickness),
                wall(width, height, 0, height, thickness),
                wall(0, height, 0, 0, thickness)
        );
        walls.forEach(level::addWall);
        return walls;
    }

    private Wall wall(double startX, double startY, double endX, double endY, double thickness) {
        return Wall.create(
                new PlanSegment(new PlanPoint(startX, startY), new PlanPoint(endX, endY)),
                Length.ofMillimeters(thickness),
                Length.of(2.8, LengthUnit.METER)
        );
    }

    private void synchronizeRooms(Level level) {
        AutoRoomGenerationService roomService = new AutoRoomGenerationService();
        level.replaceRooms(roomService.synchronize(level, new AutoRoomGenerationService.RoomDefaults(
                "Raum", Length.of(2.6, LengthUnit.METER), Length.zero(), Length.zero(), null
        )));
    }

    private Wall findWall(Level level, String wallId) {
        return level.walls().stream()
                .filter(candidate -> candidate.id().equals(UUID.fromString(wallId)))
                .findFirst()
                .orElseThrow();
    }

    private void assertSegmentOnLine(PlanSegment segment, double expectedStartX, double expectedStartY, double expectedEndX, double expectedEndY) {
        assertEquals(expectedStartX, segment.start().xMillimeters(), 0.001);
        assertEquals(expectedStartY, segment.start().yMillimeters(), 0.001);
        assertEquals(expectedEndX, segment.end().xMillimeters(), 0.001);
        assertEquals(expectedEndY, segment.end().yMillimeters(), 0.001);
    }
}
