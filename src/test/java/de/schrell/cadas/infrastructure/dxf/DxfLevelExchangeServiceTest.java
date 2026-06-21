package de.schrell.cadas.infrastructure.dxf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.FloorExtensionPlacement;
import de.schrell.cadas.domain.model.FloorExtensionType;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoofWindow;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;
import de.schrell.cadas.domain.model.WindowElement;

import java.nio.file.Path;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DxfLevelExchangeServiceTest {

    private final DxfLevelExchangeService exchangeService = new DxfLevelExchangeService();

    @TempDir
    Path tempDir;

    @Test
    void exportiertUndImportiertEinenLevelMitAllenGrundobjekten() throws Exception {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
        level.addWall(wall);
        level.addRoom(Room.rectangular(
                "Küche",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 3500),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        Door door = Door.create(
                wall.id(),
                Length.of(1.0, LengthUnit.METER),
                Length.of(1.01, LengthUnit.METER),
                Length.of(2.01, LengthUnit.METER),
                Length.zero()
        );
        level.addDoor(door);
        WindowElement window = WindowElement.create(
                wall.id(),
                Length.of(2.0, LengthUnit.METER),
                Length.of(1.2, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.2, LengthUnit.METER)
        );
        level.addWindow(window);
        level.addStaircase(new Staircase(
                java.util.UUID.randomUUID(),
                StairType.SWITCHBACK,
                new PlanPoint(500, 500),
                new PlanPoint(2200, 4200),
                Length.of(2.9, LengthUnit.METER),
                18,
                1,
                Length.of(80, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(12, LengthUnit.CENTIMETER),
                Length.of(15, LengthUnit.CENTIMETER),
                Length.of(8, LengthUnit.CENTIMETER)
        ));
        level.addFloorExtension(FloorExtension.create(FloorExtensionType.BALCONY, FloorExtensionPlacement.EXTERIOR,
                new PlanPoint(4_000, 0), new PlanPoint(7_000, 1_500), Length.ofMillimeters(180)));
        level.addFloorOpening(FloorOpening.create(
                level.rooms().getFirst().id(), FloorOpeningShape.RECTANGLE,
                new PlanPoint(2_000, 1_500), Length.ofMillimeters(1_000), Length.ofMillimeters(800)
        ));
        HydronicHeating ceilingHeating = HydronicHeating.create(
                level.rooms().getFirst().id(), HeatingSurfacePosition.CEILING, HeatingLayoutPattern.MEANDER,
                Length.ofMillimeters(200), Length.ofMillimeters(18), Length.ofMillimeters(75_000),
                Length.ofMillimeters(100), new PlanPoint(150, 200), new PlanPoint(350, 200)
        ).withZones(java.util.List.of(HeatingZone.create("L-Heizkreis", java.util.List.of(
                new PlanPoint(100, 100), new PlanPoint(3_800, 100), new PlanPoint(3_800, 1_600),
                new PlanPoint(2_000, 1_600), new PlanPoint(2_000, 3_300), new PlanPoint(100, 3_300)
        ))));
        level.addHydronicHeating(ceilingHeating);

        Path file = tempDir.resolve("grundriss.dxf");
        exchangeService.exportLevel(level, file);
        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(1, imported.walls().size());
        assertEquals(1, imported.rooms().size());
        assertEquals(1, imported.doors().size());
        assertEquals(1, imported.windows().size());
        assertEquals(1, imported.staircases().size());
        assertEquals(1, imported.floorExtensions().size());
        assertEquals(1, imported.floorOpenings().size());
        assertEquals(800.0, imported.floorOpenings().getFirst().depth().toMillimeters(), 0.001);
        assertEquals(FloorExtensionType.BALCONY, imported.floorExtensions().getFirst().type());
        assertEquals(180, imported.floorExtensions().getFirst().slabThickness().toMillimeters(), 0.001);
        assertEquals(door.id(), imported.doors().getFirst().id());
        assertEquals(window.id(), imported.windows().getFirst().id());
        assertEquals("Küche", imported.rooms().getFirst().name());
        assertEquals(14.0, imported.rooms().getFirst().areaSquareMeters(), 0.001);
        assertEquals(StairType.SWITCHBACK, imported.staircases().getFirst().stairType());
        assertEquals(1, imported.staircases().getFirst().rotationQuarterTurns());
        assertEquals(800, imported.staircases().getFirst().startLandingWidth().toMillimeters(), 0.001);
        assertEquals(600, imported.staircases().getFirst().endLandingWidth().toMillimeters(), 0.001);
        assertEquals(120, imported.staircases().getFirst().leftUnderbuildWidth().toMillimeters(), 0.001);
        assertEquals(150, imported.staircases().getFirst().rightUnderbuildWidth().toMillimeters(), 0.001);
        assertEquals(80, imported.staircases().getFirst().undersideThickness().toMillimeters(), 0.001);
        HydronicHeating importedHeating = imported.hydronicHeatings().getFirst();
        assertEquals(ceilingHeating.id(), importedHeating.id());
        assertEquals(HeatingSurfacePosition.CEILING, importedHeating.surfacePosition());
        assertEquals(18.0, importedHeating.pipeDiameter().toMillimeters(), 0.001);
        assertEquals("L-Heizkreis", importedHeating.zones().getFirst().name());
        assertEquals(6, importedHeating.zones().getFirst().outline().size());
    }

    @Test
    void importiertAlteOeffnungsMetadatenOhneObjektIds() throws Exception {
        java.util.UUID wallId = java.util.UUID.randomUUID();
        Path file = tempDir.resolve("alte-oeffnungen.dxf");
        Files.writeString(file, """
                0
                SECTION
                2
                ENTITIES
                0
                TEXT
                8
                CADAS_META
                1
                WALL|%s|175.000|2750.000|0.000|0.000|4000.000|0.000
                0
                TEXT
                8
                CADAS_META
                1
                DOOR|%s|1000.000|1010.000|2010.000|0.000
                0
                TEXT
                8
                CADAS_META
                1
                WINDOW|%s|2200.000|1200.000|900.000|1200.000
                0
                ENDSEC
                0
                EOF
                """.formatted(wallId, wallId, wallId));

        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(1, imported.walls().size());
        assertEquals(1, imported.doors().size());
        assertEquals(1, imported.windows().size());
        assertEquals(wallId, imported.doors().getFirst().wallId());
        assertEquals(wallId, imported.windows().getFirst().wallId());
    }

    @Test
    void exportiertUndImportiertOberflaechenStapel() throws Exception {
        Level level = new Level("Erdgeschoss");
        level.addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(5000, 4000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        var room = level.rooms().getFirst();
        SurfaceLayerStack floorStack = new SurfaceLayerStack(
                java.util.UUID.randomUUID(), SurfaceType.FLOOR, room.id().toString()
        );
        floorStack.addLayer(new SurfaceLayer(
                java.util.UUID.randomUUID(),
                "Fliese",
                Length.of(10, LengthUnit.MILLIMETER),
                true,
                Length.of(600, LengthUnit.MILLIMETER),
                Length.of(600, LengthUnit.MILLIMETER),
                SurfaceLayoutMode.NONE,
                Length.zero(),
                Length.of(10, LengthUnit.MILLIMETER),
                Length.of(20, LengthUnit.MILLIMETER),
                Length.of(20, LengthUnit.MILLIMETER),
                Length.of(2, LengthUnit.MILLIMETER),
                SurfaceCutRestriction.OUTER_CUTS_ROTATABLE,
                "Kacheln"
        ));
        floorStack.addLayer(new SurfaceLayer(
                java.util.UUID.randomUUID(),
                "Daemmung",
                Length.of(5, LengthUnit.MILLIMETER),
                true,
                Length.of(1200, LengthUnit.MILLIMETER),
                Length.of(600, LengthUnit.MILLIMETER),
                SurfaceLayoutMode.FIXED,
                Length.of(100, LengthUnit.MILLIMETER),
                Length.zero(),
                Length.zero(),
                Length.zero(),
                "Trittschalldaemmung"
        ));
        level.addSurfaceLayerStack(floorStack);

        SurfaceLayerStack ceilingStack = new SurfaceLayerStack(
                java.util.UUID.randomUUID(), SurfaceType.CEILING, room.id().toString()
        );
        ceilingStack.addLayer(new SurfaceLayer(
                java.util.UUID.randomUUID(),
                "PutZ",
                Length.of(15, LengthUnit.MILLIMETER),
                true,
                Length.of(1000, LengthUnit.MILLIMETER),
                Length.of(1000, LengthUnit.MILLIMETER),
                SurfaceLayoutMode.FIXED,
                Length.of(500, LengthUnit.MILLIMETER),
                Length.zero(),
                Length.zero(),
                Length.zero(),
                "Gipsputz"
        ));
        level.addSurfaceLayerStack(ceilingStack);
        level.addRoomObject(RoomObject.create(
                "wall-cabinet",
                "Wandschrank",
                RoomObjectType.WALL_CABINET,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(900, 800),
                Length.of(80, LengthUnit.CENTIMETER),
                Length.of(35, LengthUnit.CENTIMETER),
                Length.of(200, LengthUnit.CENTIMETER),
                37.5,
                RoomObjectMountingMode.CUTS_FLOOR_COVERING,
                "Standard"
        ).withBaseElevation(Length.of(25, LengthUnit.CENTIMETER)));

        Path file = tempDir.resolve("oberflaechen.dxf");
        exchangeService.exportLevel(level, file);
        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(2, imported.surfaceLayerStacks().size());
        var importedFloor = imported.surfaceLayerStacks().stream()
                .filter(s -> s.surfaceType() == SurfaceType.FLOOR)
                .findFirst().orElseThrow();
        assertEquals(2, importedFloor.layers().size());
        assertEquals("Fliese", importedFloor.layers().getFirst().name());
        assertEquals(600.0, importedFloor.layers().getFirst().tileWidth().toMillimeters(), 0.001);
        assertEquals(2.0, importedFloor.layers().getFirst().jointWidth().toMillimeters(), 0.001);
        assertEquals(SurfaceCutRestriction.OUTER_CUTS_ROTATABLE, importedFloor.layers().getFirst().cutRestriction());
        assertTrue(importedFloor.layers().getFirst().visible());
        assertEquals("Daemmung", importedFloor.layers().get(1).name());
        assertEquals(SurfaceLayoutMode.FIXED, importedFloor.layers().get(1).layoutMode());
        assertEquals(100.0, importedFloor.layers().get(1).layoutOffset().toMillimeters(), 0.001);

        var importedCeiling = imported.surfaceLayerStacks().stream()
                .filter(s -> s.surfaceType() == SurfaceType.CEILING)
                .findFirst().orElseThrow();
        assertEquals(1, importedCeiling.layers().size());
        assertEquals("PutZ", importedCeiling.layers().getFirst().name());
        assertEquals(SurfaceLayoutMode.FIXED, importedCeiling.layers().getFirst().layoutMode());
        assertEquals(500.0, importedCeiling.layers().getFirst().layoutOffset().toMillimeters(), 0.001);
        assertEquals(1, imported.roomObjects().size());
        assertEquals("Wandschrank", imported.roomObjects().getFirst().name());
        assertTrue(imported.roomObjects().getFirst().cutsFloorCovering());
        assertEquals(RoomObjectMountingMode.CUTS_FLOOR_COVERING, imported.roomObjects().getFirst().mountingMode());
        assertEquals(37.5, imported.roomObjects().getFirst().rotationDegrees(), 0.001);
        assertEquals(250.0, imported.roomObjects().getFirst().baseElevation().toMillimeters(), 0.001);
    }

    @Test
    void erhaeltSonderzeichenInMetadatenVerlustfrei() throws Exception {
        Level level = new Level("Sonderzeichen");
        Room room = Room.rectangular(
                "Bad | Küche/Flur ä\nNord",
                new PlanPoint(0, 0),
                new PlanPoint(3000, 2500),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(room);
        SurfaceLayerStack stack = new SurfaceLayerStack(
                java.util.UUID.randomUUID(),
                SurfaceType.FLOOR,
                "Raum/" + room.id() + "|Boden"
        );
        stack.addLayer(new SurfaceLayer(
                java.util.UUID.randomUUID(),
                "Belag | Eiche/hell",
                Length.of(12, LengthUnit.MILLIMETER),
                true,
                Length.of(600, LengthUnit.MILLIMETER),
                Length.of(300, LengthUnit.MILLIMETER),
                SurfaceLayoutMode.FIXED,
                Length.of(100, LengthUnit.MILLIMETER),
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.of(2, LengthUnit.MILLIMETER),
                "Quelle | DWG/Block"
        ));
        level.addSurfaceLayerStack(stack);

        Path file = tempDir.resolve("sonderzeichen.dxf");
        exchangeService.exportLevel(level, file);
        String dxf = Files.readString(file);
        Level imported = exchangeService.importLevel(file, "Import");

        assertTrue(dxf.contains("CADAS_DXF|4"));
        assertFalse(dxf.contains("Bad | Küche/Flur"));
        assertEquals("Bad | Küche/Flur ä\nNord", imported.rooms().getFirst().name());
        assertEquals("Raum/" + room.id() + "|Boden", imported.surfaceLayerStacks().getFirst().targetKey());
        assertEquals("Belag | Eiche/hell", imported.surfaceLayerStacks().getFirst().layers().getFirst().name());
        assertEquals("Quelle | DWG/Block", imported.surfaceLayerStacks().getFirst().layers().getFirst().coveringSource());
    }

    @Test
    void exportiertMetrischeHeaderUndModelSpaceKennung() throws Exception {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(1000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
        level.addWall(wall);
        level.addDoor(Door.create(
                wall.id(),
                Length.of(10, LengthUnit.CENTIMETER),
                Length.of(80, LengthUnit.CENTIMETER),
                Length.of(2.01, LengthUnit.METER),
                Length.zero()
        ));
        level.addWindow(WindowElement.create(
                wall.id(),
                Length.of(10, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.20, LengthUnit.METER)
        ));
        level.addStaircase(Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(0, 0),
                new PlanPoint(1000, 2000),
                Length.of(2.75, LengthUnit.METER),
                14
        ));

        Path file = tempDir.resolve("header.dxf");
        exchangeService.exportLevel(level, file);
        String dxf = Files.readString(file);

        assertTrue(dxf.contains("$INSUNITS\n70\n4"));
        assertTrue(dxf.contains("$MEASUREMENT\n70\n1"));
        assertTrue(dxf.contains("\n67\n0\n410\nModel\n"));
        assertTrue(dxf.contains("\n2\nTABLES\n"));
        assertTrue(dxf.contains("\n2\nBLOCKS\n"));
        assertTrue(dxf.contains("\n2\nOBJECTS\n"));
        assertTrue(dxf.contains("\n2\nLAYER\n"));
        assertTrue(dxf.contains("\n2\nBLOCK_RECORD\n"));
        assertTrue(dxf.contains("\n0\nINSERT\n"));
        assertTrue(dxf.contains("\n2\nCADAS_DOOR\n"));
        assertTrue(dxf.contains("\n2\nCADAS_WINDOW\n"));
        assertTrue(dxf.contains("\n2\nCADAS_STAIR\n"));
        assertTrue(dxf.contains("\n0\nDICTIONARY\n"));
        assertTrue(dxf.contains("\n0\nLAYOUT\n"));
    }

    @Test
    void importiertAlteObjektVierteldrehungAlsGradwert() throws Exception {
        Path file = tempDir.resolve("altes-objekt.dxf");
        Files.writeString(file, """
                0
                SECTION
                2
                ENTITIES
                0
                TEXT
                8
                CADAS_META
                1
                CADAS_DXF|3
                0
                TEXT
                8
                CADAS_META
                1
                OBJ|%s|test|Testobjekt|TABLE|RECTANGLE|100.000|200.000|1000.000|500.000|750.000|1|false|true||STANDS_ON_COVERING
                0
                ENDSEC
                0
                EOF
                """.formatted(java.util.UUID.randomUUID()));

        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(90.0, imported.roomObjects().getFirst().rotationDegrees(), 0.001);
    }

    @Test
    void verwendetBeimFallbackImportDieStandardDeckendickeVonEinemMillimeter() throws Exception {
        Path file = tempDir.resolve("fallback-raum.dxf");
        Files.writeString(file, """
                0
                SECTION
                2
                ENTITIES
                0
                LWPOLYLINE
                8
                ROOMS
                90
                4
                70
                1
                10
                0
                20
                0
                10
                4000
                20
                0
                10
                4000
                20
                3000
                10
                0
                20
                3000
                0
                ENDSEC
                0
                EOF
                """);

        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(1, imported.rooms().size());
        assertEquals(1.0, imported.rooms().getFirst().ceilingThickness().toMillimeters(), 0.001);
    }

    @Test
    void fallbackImportUeberspringtUnvollstaendigeOderKaputteGeometrie() throws Exception {
        Path file = tempDir.resolve("fallback-kaputt.dxf");
        Files.writeString(file, """
                0
                SECTION
                2
                ENTITIES
                0
                LINE
                8
                WALLS
                10
                defekt
                20
                0
                11
                1000
                21
                0
                0
                LWPOLYLINE
                8
                ROOMS
                90
                2
                70
                1
                10
                0
                20
                0
                10
                1000
                20
                0
                0
                ENDSEC
                0
                EOF
                """);

        Level imported = exchangeService.importLevel(file, "Import");

        assertTrue(imported.walls().isEmpty());
        assertTrue(imported.rooms().isEmpty());
    }

    @Test
    void exportiertUndImportiertRaeumeMitDachschraege() throws Exception {
        Level level = new Level("Dachgeschoss");
        level.addRoom(Room.withSlopedCeilings(
                java.util.UUID.randomUUID(),
                "Studio",
                java.util.List.of(
                        new PlanPoint(0, 0),
                        new PlanPoint(4000, 0),
                        new PlanPoint(4000, 3000),
                        new PlanPoint(0, 3000)
                ),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                java.util.List.of(
                        new SlopedCeilingProfile(
                                SlopedCeilingSide.SOUTH,
                                Length.of(1.1, LengthUnit.METER),
                                Length.of(1.4, LengthUnit.METER)
                        ),
                        new SlopedCeilingProfile(
                                SlopedCeilingSide.WEST,
                                Length.of(0.9, LengthUnit.METER),
                                Length.of(0.8, LengthUnit.METER)
                        )
                ),
                null
        ));

        Path file = tempDir.resolve("dachgeschoss.dxf");
        exchangeService.exportLevel(level, file);
        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(2, imported.rooms().getFirst().slopedCeilingProfiles().size());
        assertEquals(SlopedCeilingSide.SOUTH, imported.rooms().getFirst().slopedCeilingProfiles().getFirst().lowSide());
        assertEquals(1100.0, imported.rooms().getFirst().slopedCeilingProfiles().getFirst().kneeWallHeight().toMillimeters(), 0.001);
        assertEquals(1400.0, imported.rooms().getFirst().slopedCeilingProfiles().getFirst().horizontalRun().toMillimeters(), 0.001);
        assertEquals(SlopedCeilingSide.WEST, imported.rooms().getFirst().slopedCeilingProfiles().get(1).lowSide());
    }

    @Test
    void exportiertUndImportiertDachfenster() throws Exception {
        Level level = new Level("Dachgeschoss");
        java.util.UUID roomId = java.util.UUID.randomUUID();
        RoofWindow roofWindow = RoofWindow.create(
                roomId, new PlanPoint(2_000, 700),
                Length.ofMillimeters(900), Length.ofMillimeters(1_200), SlopedCeilingSide.NORTH
        );
        level.addRoofWindow(roofWindow);

        Path file = tempDir.resolve("dachfenster.dxf");
        exchangeService.exportLevel(level, file);
        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(roofWindow, imported.roofWindows().getFirst());
    }

    @Test
    void exportiertUndImportiertWandEndpunktHoehenUndPolygonaleDecken() throws Exception {
        Level level = new Level("Dachgeschoss");
        level.addWall(new Wall(
                java.util.UUID.randomUUID(),
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(3.1, LengthUnit.METER),
                Length.of(2.4, LengthUnit.METER),
                Length.of(3.1, LengthUnit.METER),
                java.util.List.of(
                        new WallProfilePoint(Length.zero(), Length.of(2.4, LengthUnit.METER)),
                        new WallProfilePoint(Length.of(1.5, LengthUnit.METER), Length.of(3.1, LengthUnit.METER)),
                        new WallProfilePoint(Length.of(4, LengthUnit.METER), Length.of(3.1, LengthUnit.METER))
                )
        ));
        level.addRoom(new Room(
                java.util.UUID.randomUUID(),
                "Ausbau",
                java.util.List.of(
                        new PlanPoint(0, 0),
                        new PlanPoint(4000, 0),
                        new PlanPoint(4000, 3000),
                        new PlanPoint(0, 3000)
                ),
                Length.of(3.1, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                null,
                java.util.List.of(
                        Length.of(2.4, LengthUnit.METER),
                        Length.of(3.1, LengthUnit.METER),
                        Length.of(3.1, LengthUnit.METER),
                        Length.of(2.6, LengthUnit.METER)
                )
        ));

        Path file = tempDir.resolve("decke.dxf");
        exchangeService.exportLevel(level, file);
        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(2400.0, imported.walls().getFirst().startHeight().toMillimeters(), 0.001);
        assertEquals(3100.0, imported.walls().getFirst().endHeight().toMillimeters(), 0.001);
        assertEquals(3, imported.walls().getFirst().profile().size());
        assertEquals(1500.0, imported.walls().getFirst().profile().get(1).offset().toMillimeters(), 0.001);
        assertEquals(4, imported.rooms().getFirst().ceilingVertexHeights().size());
        assertEquals(2600.0, imported.rooms().getFirst().ceilingVertexHeights().get(3).toMillimeters(), 0.001);
    }
}
