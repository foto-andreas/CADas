package de.andreas.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.application.room.AutoRoomGenerationService;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Wall;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class WallDimensionPlacementServiceTest {

    private final WallDimensionService wallDimensionService = new WallDimensionService();
    private final WallDimensionPlacementService placementService = new WallDimensionPlacementService();

    @Test
    void ziehtInnenUndAussenmassEinerAussenwandGemeinsamNachAussen() {
        Level level = new Level("Erdgeschoss");
        Wall wall = addRectangle(level, 4_000, 3_000, 200).getFirst();
        synchronizeRooms(level);

        List<WallDimensionPlacementService.PlacedDimension> placements = placementService.place(
                level,
                wall,
                wallDimensionService.dimensions(level, wall),
                1.0,
                30.0,
                10.0
        );

        assertEquals(2, placements.size());
        WallDimensionPlacementService.PlacedDimension roomPlacement = placements.stream()
                .filter(placement -> !placement.exterior())
                .findFirst()
                .orElseThrow();
        WallDimensionPlacementService.PlacedDimension exteriorPlacement = placements.stream()
                .filter(WallDimensionPlacementService.PlacedDimension::exterior)
                .findFirst()
                .orElseThrow();
        assertEquals(-130.0, roomPlacement.lineDistanceFromAxis(), 0.001);
        assertEquals(-140.0, exteriorPlacement.lineDistanceFromAxis(), 0.001);
        assertTrue(roomPlacement.lineDistanceFromAxis() < 0.0);
        assertTrue(exteriorPlacement.lineDistanceFromAxis() < roomPlacement.lineDistanceFromAxis());
    }

    @Test
    void stapeltMehrereRaummaßeEinerAussenwandAufDerGemeinsamenAussenseite() {
        Level level = new Level("Erdgeschoss");
        Wall wall = wall(0, 0, 4_000, 0, 200);
        level.addWall(wall);
        WallDimensionService.WallDimensions dimensions = new WallDimensionService.WallDimensions(
                List.of(
                        new WallDimensionService.SideDimension(
                                "Küche",
                                Length.ofMillimeters(900),
                                1.0,
                                new PlanSegment(new PlanPoint(100, 100), new PlanPoint(1_000, 100))
                        ),
                        new WallDimensionService.SideDimension(
                                "Wohnen",
                                Length.ofMillimeters(1_850),
                                1.0,
                                new PlanSegment(new PlanPoint(2_050, 100), new PlanPoint(3_900, 100))
                        )
                ),
                Optional.of(new WallDimensionService.SideDimension(
                        "Außen",
                        Length.ofMillimeters(4_200),
                        -1.0,
                        new PlanSegment(new PlanPoint(-100, -100), new PlanPoint(4_100, -100))
                ))
        );

        List<WallDimensionPlacementService.PlacedDimension> placements = placementService.place(
                level,
                wall,
                dimensions,
                1.0,
                30.0,
                10.0
        );

        assertEquals(3, placements.size());
        assertEquals(900.0, placements.get(0).dimension().length().toMillimeters(), 0.001);
        assertEquals(1_850.0, placements.get(1).dimension().length().toMillimeters(), 0.001);
        assertEquals(4_200.0, placements.get(2).dimension().length().toMillimeters(), 0.001);
        assertEquals(-130.0, placements.get(0).lineDistanceFromAxis(), 0.001);
        assertEquals(-140.0, placements.get(1).lineDistanceFromAxis(), 0.001);
        assertEquals(-150.0, placements.get(2).lineDistanceFromAxis(), 0.001);
        assertTrue(placements.stream().allMatch(placement -> placement.lineDistanceFromAxis() < 0.0));
    }

    @Test
    void ziehtAuchInnenwandmaßeGemeinsamAußerhalbDesGebäudes() {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(2_000, 0), new PlanPoint(2_000, 3_000)),
                Length.ofMillimeters(100),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(wall);
        level.addRoom(Room.rectangular("Links", new PlanPoint(100, 100), new PlanPoint(1_950, 2_900), Length.of(2.6, LengthUnit.METER), Length.zero(), Length.zero()));
        level.addRoom(Room.rectangular("Rechts", new PlanPoint(2_050, 100), new PlanPoint(3_900, 2_900), Length.of(2.6, LengthUnit.METER), Length.zero(), Length.zero()));

        List<WallDimensionPlacementService.PlacedDimension> placements = placementService.place(
                level,
                wall,
                wallDimensionService.dimensions(level, wall),
                1.0,
                20.0,
                10.0
        );

        assertEquals(2, placements.size());
        assertTrue(Math.signum(placements.get(0).lineDistanceFromAxis()) == Math.signum(placements.get(1).lineDistanceFromAxis()));
        assertTrue(Math.abs(placements.get(0).lineDistanceFromAxis()) > 1_900.0);
        assertTrue(Math.abs(placements.get(1).lineDistanceFromAxis()) > Math.abs(placements.get(0).lineDistanceFromAxis()));
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
}
