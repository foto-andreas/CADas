package de.andreas.cadas.application.layers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;
import org.junit.jupiter.api.Test;

class WallSurfaceSideServiceTest {

    private final WallSurfaceSideService service = new WallSurfaceSideService();

    @Test
    void erkenntInnenwandseiteAuchNachVerschobenerRaumkanteDurchBelag() {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(wall);
        level.addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(100, 112),
                new PlanPoint(3900, 2900),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        ));
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, wall.id().toString());
        stack.addLayer(SurfaceLayer.create(
                "Fliese",
                Length.of(12, LengthUnit.MILLIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(30, LengthUnit.CENTIMETER),
                Length.of(2, LengthUnit.MILLIMETER)
        ));
        level.addSurfaceLayerStack(stack);

        WallSurfaceSideService.WallLayerSides sides = service.resolve(level, wall, SurfaceType.WALL_INTERIOR, wall.id().toString());

        assertTrue(sides.positiveSide());
        assertFalse(sides.negativeSide());

        WallSurfaceSideService.WallLayerSides exteriorSides = service.resolve(level, wall, SurfaceType.WALL_EXTERIOR, wall.id().toString());
        assertFalse(exteriorSides.positiveSide());
        assertTrue(exteriorSides.negativeSide());
    }
}
