package de.andreas.cadas.application.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.application.layers.WallSurfaceTargetKey;
import de.andreas.cadas.application.view.WallSurfaceOpeningService.WallSurfaceInterval;
import de.andreas.cadas.application.view.WallSurfacePlanGeometryService.WallSurfacePlanPolygon;
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

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WallSurfacePlanGeometryServiceTest {

    private final WallSurfacePlanGeometryService service = new WallSurfacePlanGeometryService();

    @Test
    void verlängertBelagAnKonkaverInneneckeBisZumNachbarbelag() {
        Level level = new Level("Erdgeschoss");
        Wall horizontal = wall(0, 0, -2000, 0);
        Wall vertical = wall(0, 1000, 0, 0);
        level.addWall(horizontal);
        level.addWall(vertical);
        Room room = room(List.of(
                new PlanPoint(-2000, -1500),
                new PlanPoint(1500, -1500),
                new PlanPoint(1500, 1000),
                new PlanPoint(0, 1000),
                new PlanPoint(0, 0),
                new PlanPoint(-2000, 0)
        ));
        level.addRoom(room);
        SurfaceLayerStack horizontalStack = addInteriorStack(level, horizontal, room);
        addInteriorStack(level, vertical, room);

        SurfaceLayer layer = horizontalStack.layers().getFirst();
        WallSurfacePlanPolygon polygon = service.surfacePolygon(
                level,
                horizontal,
                horizontalStack,
                layer,
                0,
                centerOffset(horizontal, layer),
                new WallSurfaceInterval(0, horizontal.axis().length().toMillimeters())
        );

        assertEquals(167.5, maxX(polygon), 0.001);
        assertTrue(polygon.points().stream().anyMatch(point -> point.xMillimeters() > 0.0));
    }

    @Test
    void kürztBelagAnKonvexerInneneckeBisZurNachbarwandkante() {
        Level level = new Level("Erdgeschoss");
        Wall horizontal = wall(0, 0, 2000, 0);
        Wall vertical = wall(0, 0, 0, 2000);
        level.addWall(horizontal);
        level.addWall(vertical);
        Room room = room(List.of(
                new PlanPoint(0, 0),
                new PlanPoint(2500, 0),
                new PlanPoint(2500, 2500),
                new PlanPoint(0, 2500)
        ));
        level.addRoom(room);
        SurfaceLayerStack horizontalStack = addInteriorStack(level, horizontal, room);
        addInteriorStack(level, vertical, room);

        SurfaceLayer layer = horizontalStack.layers().getFirst();
        WallSurfacePlanPolygon polygon = service.surfacePolygon(
                level,
                horizontal,
                horizontalStack,
                layer,
                0,
                centerOffset(horizontal, layer),
                new WallSurfaceInterval(0, horizontal.axis().length().toMillimeters())
        );

        assertEquals(87.5, minX(polygon), 0.001);
    }

    private Wall wall(double startX, double startY, double endX, double endY) {
        return Wall.create(
                new PlanSegment(new PlanPoint(startX, startY), new PlanPoint(endX, endY)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
    }

    private Room room(List<PlanPoint> outline) {
        return new Room(
                UUID.randomUUID(),
                "Raum",
                outline,
                Length.of(2.75, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER),
                null
        );
    }

    private SurfaceLayerStack addInteriorStack(Level level, Wall wall, Room room) {
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, WallSurfaceTargetKey.interior(wall.id(), room.id()));
        stack.addLayer(SurfaceLayer.create(
                "Dämmplatte",
                Length.of(8, LengthUnit.CENTIMETER),
                Length.of(120, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(2, LengthUnit.MILLIMETER)
        ));
        level.addSurfaceLayerStack(stack);
        return stack;
    }

    private double centerOffset(Wall wall, SurfaceLayer layer) {
        return wall.thickness().toMillimeters() / 2.0 + layer.thickness().toMillimeters() / 2.0;
    }

    private double minX(WallSurfacePlanPolygon polygon) {
        return polygon.points().stream()
                .mapToDouble(PlanPoint::xMillimeters)
                .min()
                .orElseThrow();
    }

    private double maxX(WallSurfacePlanPolygon polygon) {
        return polygon.points().stream()
                .mapToDouble(PlanPoint::xMillimeters)
                .max()
                .orElseThrow();
    }
}
