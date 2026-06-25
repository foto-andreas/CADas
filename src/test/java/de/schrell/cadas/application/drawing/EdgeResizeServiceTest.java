package de.schrell.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import de.schrell.cadas.application.heating.HeatingCircuitRoutingService;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.Grid;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EdgeResizeServiceTest {

    private final EdgeResizeService service = new EdgeResizeService();

    @Test
    void verschiebtWandanfangUndHaeltOeffnungAnWeltposition() {
        Level level = level();
        Wall wall = level.walls().getFirst();
        Door door = level.doors().getFirst();
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.WALL_START,
                wall.id(),
                wall.id(),
                wall.axis().start()
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(-500, 0));

        assertEquals(-500.0, result.walls().getFirst().axis().start().xMillimeters(), 0.001);
        assertEquals(1_500.0, result.doors().getFirst().offsetFromStart().toMillimeters(), 0.001);
        assertEquals(1_000.0, result.doors().getFirst().width().toMillimeters(), 0.001);
    }

    @Test
    void verschiebtNurGezogeneTuerkante() {
        Level level = level();
        Door door = level.doors().getFirst();
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.DOOR_START,
                door.id(),
                door.wallId(),
                new PlanPoint(1_000, 0)
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(1_250, 80));

        assertEquals(1_250.0, result.doors().getFirst().offsetFromStart().toMillimeters(), 0.001);
        assertEquals(750.0, result.doors().getFirst().width().toMillimeters(), 0.001);
    }

    @Test
    void findetHandleNurAnSelektiertemElement() {
        Level level = level();
        Door door = level.doors().getFirst();

        EdgeResizeService.EdgeHandle handle = service.findHandle(
                level,
                Set.of(new SelectionKey(RenderableKind.DOOR, level.name(), door.id().toString())),
                new PlanPoint(2_010, 5),
                Length.ofMillimeters(20)
        ).orElseThrow();

        assertEquals(EdgeResizeService.EdgeHandleKind.DOOR_END, handle.kind());
    }

    @Test
    void liefertEckHandlesFuerSelektierteTreppe() {
        Level level = levelMitTreppe();
        Staircase staircase = level.staircases().getFirst();

        var handles = service.handles(level, Set.of(new SelectionKey(RenderableKind.STAIR, level.name(), staircase.id().toString())));

        assertEquals(8, handles.size());
    }

    @Test
    void liefertAchtHandlesFuerRechteckartigeFlaechen() {
        Level level = new Level("Obergeschoss");
        UUID roomId = UUID.randomUUID();
        FloorOpening opening = FloorOpening.create(
                roomId,
                FloorOpeningShape.RECTANGLE,
                new PlanPoint(1_000, 1_000),
                Length.ofMillimeters(800),
                Length.ofMillimeters(600)
        );
        HeatingExclusionArea area = HeatingExclusionArea.create(
                roomId,
                "Sperre",
                new PlanPoint(2_000, 500),
                new PlanPoint(3_000, 1_500)
        );
        level.addFloorOpening(opening);
        level.addHeatingExclusionArea(area);
        HeatingZone zone = HeatingZone.create("HK 1", rectangle(4_000, 500, 5_000, 1_500));
        level.addHydronicHeating(HydronicHeating.create(
                roomId,
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.SPIRAL,
                Length.ofMillimeters(100),
                Length.ofMillimeters(11.6),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        ).withZones(java.util.List.of(zone)));

        assertEquals(8, service.handles(level, Set.of(new SelectionKey(
                RenderableKind.FLOOR_OPENING, level.name(), opening.id().toString()
        ))).size());
        assertEquals(8, service.handles(level, Set.of(new SelectionKey(
                RenderableKind.HEATING_EXCLUSION, level.name(), area.id().toString()
        ))).size());
        assertEquals(8, service.handles(level, Set.of(new SelectionKey(
                RenderableKind.HEATING_ZONE, level.name(), zone.id().toString()
        ))).size());
    }

    @Test
    void aendertTreppeDurchZiehenDerErstenEcke() {
        Level level = levelMitTreppe();
        Staircase staircase = level.staircases().getFirst();
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.STAIR_FIRST_CORNER,
                staircase.id(),
                staircase.id(),
                staircase.firstCorner()
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(200, 300));

        Staircase resized = result.staircases().getFirst();
        assertEquals(200.0, resized.firstCorner().xMillimeters(), 0.001);
        assertEquals(300.0, resized.firstCorner().yMillimeters(), 0.001);
        assertEquals(staircase.oppositeCorner().xMillimeters(), resized.oppositeCorner().xMillimeters(), 0.001);
        assertEquals(staircase.oppositeCorner().yMillimeters(), resized.oppositeCorner().yMillimeters(), 0.001);
    }

    @Test
    void aendertTreppeDurchZiehenDerGegenueberliegendenEcke() {
        Level level = levelMitTreppe();
        Staircase staircase = level.staircases().getFirst();
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.STAIR_OPPOSITE_CORNER,
                staircase.id(),
                staircase.id(),
                staircase.oppositeCorner()
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(2_800, 1_700));

        Staircase resized = result.staircases().getFirst();
        assertEquals(staircase.firstCorner().xMillimeters(), resized.firstCorner().xMillimeters(), 0.001);
        assertEquals(staircase.firstCorner().yMillimeters(), resized.firstCorner().yMillimeters(), 0.001);
        assertEquals(2_800.0, resized.oppositeCorner().xMillimeters(), 0.001);
        assertEquals(1_700.0, resized.oppositeCorner().yMillimeters(), 0.001);
    }

    @Test
    void aendertRechteckigeBodenoeffnungMitSeitenHandle() {
        Level level = new Level("Obergeschoss");
        UUID roomId = UUID.randomUUID();
        FloorOpening opening = FloorOpening.create(
                roomId,
                FloorOpeningShape.RECTANGLE,
                new PlanPoint(1_000, 1_000),
                Length.ofMillimeters(800),
                Length.ofMillimeters(600)
        );
        level.addFloorOpening(opening);
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.RECTANGLE_EAST,
                RenderableKind.FLOOR_OPENING,
                opening.id(),
                null,
                new PlanPoint(opening.maxXMillimeters(), opening.center().yMillimeters())
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(1_700, 1_000));

        assertEquals(1_150.0, result.floorOpenings().getFirst().center().xMillimeters(), 0.001);
        assertEquals(1_100.0, result.floorOpenings().getFirst().width().toMillimeters(), 0.001);
        assertEquals(600.0, result.floorOpenings().getFirst().depth().toMillimeters(), 0.001);
    }

    @Test
    void aendertFbhSperrflaecheMitEckHandle() {
        Level level = new Level("Erdgeschoss");
        HeatingExclusionArea area = HeatingExclusionArea.create(
                UUID.randomUUID(),
                "Sperre",
                new PlanPoint(1_000, 1_000),
                new PlanPoint(2_000, 2_000)
        );
        level.addHeatingExclusionArea(area);
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.RECTANGLE_SOUTH_EAST,
                RenderableKind.HEATING_EXCLUSION,
                area.id(),
                null,
                area.oppositeCorner()
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(2_400, 2_300));

        assertEquals(1_400.0, result.heatingExclusionAreas().getFirst().widthMillimeters(), 0.001);
        assertEquals(1_300.0, result.heatingExclusionAreas().getFirst().depthMillimeters(), 0.001);
    }

    @Test
    void aendertHeizkreisMitSeitenHandle() {
        Level level = new Level("Erdgeschoss");
        HeatingZone zone = HeatingZone.create("HK 1", rectangle(1_000, 1_000, 2_000, 2_000));
        HydronicHeating heating = HydronicHeating.create(
                UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.SPIRAL,
                Length.ofMillimeters(100),
                Length.ofMillimeters(11.6),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        ).withZones(java.util.List.of(zone));
        level.addHydronicHeating(heating);
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.RECTANGLE_EAST,
                RenderableKind.HEATING_ZONE,
                zone.id(),
                null,
                new PlanPoint(2_000, 1_500)
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(2_400, 1_500));

        HeatingZone resized = result.hydronicHeatings().getFirst().zones().getFirst();
        assertEquals(1_400_000.0, resized.areaSquareMillimeters(), 0.001);
        assertEquals(2_400.0, resized.outline().get(1).xMillimeters(), 0.001);
    }

    @Test
    void regeneriertSprachroutingBeimAendernEinesHeizkreisRechtecks() {
        Level level = new Level("Erdgeschoss");
        HydronicHeating heating = HydronicHeating.create(
                UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.VARIO,
                Length.ofMillimeters(100),
                Length.ofMillimeters(11.6),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        );
        HeatingZone zone = new HeatingCircuitRoutingService().regenerate(new HeatingZone(
                UUID.randomUUID(),
                "HK 1",
                rectangle(1_000, 1_000, 3_000, 2_000),
                HeatingLayoutPattern.VARIO,
                false
        ), heating);
        level.addHydronicHeating(heating.withZones(java.util.List.of(zone)));
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.RECTANGLE_EAST,
                RenderableKind.HEATING_ZONE,
                zone.id(),
                null,
                new PlanPoint(3_000, 1_500)
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(4_000, 1_500));

        HeatingZone resized = result.hydronicHeatings().getFirst().zones().getFirst();
        assertEquals(3_000_000.0, resized.areaSquareMillimeters(), 0.001);
        assertNotEquals(zone.routingCommands(), resized.routingCommands());
        assertEquals(HeatingLayoutPattern.VARIO, resized.layoutPattern());
    }

    @Test
    void behaeltSprachroutingBeimAendernEinesHeizkreisRechtecksWennAutoRoutingAusIst() {
        Level level = new Level("Erdgeschoss");
        HydronicHeating heating = HydronicHeating.create(
                UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.VARIO,
                Length.ofMillimeters(100),
                Length.ofMillimeters(11.6),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        );
        HeatingZone zone = new HeatingCircuitRoutingService().regenerate(new HeatingZone(
                UUID.randomUUID(),
                "HK 1",
                rectangle(1_000, 1_000, 3_000, 2_000),
                HeatingLayoutPattern.VARIO,
                false
        ), heating);
        level.addHydronicHeating(heating.withZones(java.util.List.of(zone)));
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.RECTANGLE_EAST,
                RenderableKind.HEATING_ZONE,
                zone.id(),
                null,
                new PlanPoint(3_000, 1_500)
        );

        EdgeResizeService.ResizeResult result = service.resize(
                level,
                handle,
                new PlanPoint(4_000, 1_500),
                new EdgeResizeService.ResizeOptions(false, null)
        );

        HeatingZone resized = result.hydronicHeatings().getFirst().zones().getFirst();
        assertEquals(3_000_000.0, resized.areaSquareMillimeters(), 0.001);
        assertEquals(zone.routingCommands(), resized.routingCommands());
    }

    @Test
    void rastetHeizkreisRoutingStartBeimRechteckaendernAmRasterEin() {
        Level level = new Level("Erdgeschoss");
        HeatingZone zone = HeatingZone.create("HK 1", rectangle(1_000, 1_000, 2_000, 2_000));
        HydronicHeating heating = HydronicHeating.create(
                UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.SPIRAL,
                Length.ofMillimeters(100),
                Length.ofMillimeters(11.6),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        ).withZones(java.util.List.of(zone));
        level.addHydronicHeating(heating);
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.RECTANGLE_EAST,
                RenderableKind.HEATING_ZONE,
                zone.id(),
                null,
                new PlanPoint(2_000, 1_500)
        );

        EdgeResizeService.ResizeResult result = service.resize(
                level,
                handle,
                new PlanPoint(2_333, 1_500),
                new EdgeResizeService.ResizeOptions(false, new Grid(Length.ofMillimeters(100)))
        );

        HeatingZone resized = result.hydronicHeatings().getFirst().zones().getFirst();
        assertEquals(new PlanPoint(1_700, 1_500), resized.routingStartPoint());
        assertEquals(2_366.5, resized.outline().get(1).xMillimeters(), 0.001);
    }

    @Test
    void aendertHkvFreiflächeMitSeitenHandle() {
        Level level = new Level("Erdgeschoss");
        HydronicHeating heating = HydronicHeating.create(
                UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.SPIRAL,
                Length.ofMillimeters(100),
                Length.ofMillimeters(11.6),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(1_000, 1_000),
                new PlanPoint(1_050, 1_000)
        );
        level.addHydronicHeating(heating);
        EdgeResizeService.EdgeHandle handle = new EdgeResizeService.EdgeHandle(
                EdgeResizeService.EdgeHandleKind.RECTANGLE_EAST,
                RenderableKind.HEATING_MANIFOLD,
                heating.id(),
                null,
                new PlanPoint(1_325, 1_000)
        );

        EdgeResizeService.ResizeResult result = service.resize(level, handle, new PlanPoint(1_525, 1_000));

        HydronicHeating resized = result.hydronicHeatings().getFirst();
        assertEquals(800.0, resized.manifoldFreeAreaWidth().toMillimeters(), 0.001);
        assertEquals(100.0, resized.supplyPoint().xMillimeters() - heating.supplyPoint().xMillimeters(), 0.001);
    }

    private java.util.List<PlanPoint> rectangle(double minX, double minY, double maxX, double maxY) {
        return java.util.List.of(
                new PlanPoint(minX, minY),
                new PlanPoint(maxX, minY),
                new PlanPoint(maxX, maxY),
                new PlanPoint(minX, maxY)
        );
    }

    private Level level() {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(5_000, 0)),
                Length.ofMillimeters(200),
                Length.ofMillimeters(2_800)
        );
        level.addWall(wall);
        level.addDoor(Door.create(wall.id(), Length.ofMillimeters(1_000), Length.ofMillimeters(1_000), Length.ofMillimeters(2_010), Length.zero()));
        level.addWindow(WindowElement.create(wall.id(), Length.ofMillimeters(3_000), Length.ofMillimeters(1_000), Length.ofMillimeters(900), Length.ofMillimeters(1_200)));
        return level;
    }

    private Level levelMitTreppe() {
        Level level = new Level("Erdgeschoss");
        level.addStaircase(Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(1_000, 1_000),
                new PlanPoint(2_500, 2_000),
                Length.ofMillimeters(2_800),
                16
        ));
        return level;
    }
}
