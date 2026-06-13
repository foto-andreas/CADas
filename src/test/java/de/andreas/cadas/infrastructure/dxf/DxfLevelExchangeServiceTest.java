package de.andreas.cadas.infrastructure.dxf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SlopedCeilingProfile;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceLayoutMode;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.SlopedCeilingSide;
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

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
        level.addDoor(Door.create(
                wall.id(),
                Length.of(1.0, LengthUnit.METER),
                Length.of(1.01, LengthUnit.METER),
                Length.of(2.01, LengthUnit.METER),
                Length.zero()
        ));
        level.addWindow(WindowElement.create(
                wall.id(),
                Length.of(2.0, LengthUnit.METER),
                Length.of(1.2, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.2, LengthUnit.METER)
        ));
        level.addStaircase(new Staircase(
                java.util.UUID.randomUUID(),
                StairType.SWITCHBACK,
                new PlanPoint(500, 500),
                new PlanPoint(2200, 4200),
                Length.of(2.9, LengthUnit.METER),
                18,
                1
        ));

        Path file = tempDir.resolve("grundriss.dxf");
        exchangeService.exportLevel(level, file);
        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(1, imported.walls().size());
        assertEquals(1, imported.rooms().size());
        assertEquals(1, imported.doors().size());
        assertEquals(1, imported.windows().size());
        assertEquals(1, imported.staircases().size());
        assertEquals("Küche", imported.rooms().getFirst().name());
        assertEquals(14.0, imported.rooms().getFirst().areaSquareMeters(), 0.001);
        assertEquals(StairType.SWITCHBACK, imported.staircases().getFirst().stairType());
        assertEquals(1, imported.staircases().getFirst().rotationQuarterTurns());
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
                Length.of(2, LengthUnit.MILLIMETER),
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

        assertTrue(dxf.contains("CADAS_DXF|2"));
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
        level.addRoom(Room.rectangular(
                "Studio",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 3000),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                new SlopedCeilingProfile(SlopedCeilingSide.SOUTH, Length.of(1.1, LengthUnit.METER))
        ));

        Path file = tempDir.resolve("dachgeschoss.dxf");
        exchangeService.exportLevel(level, file);
        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(SlopedCeilingSide.SOUTH, imported.rooms().getFirst().slopedCeilingProfile().orElseThrow().lowSide());
        assertEquals(1100.0, imported.rooms().getFirst().slopedCeilingProfile().orElseThrow().kneeWallHeight().toMillimeters(), 0.001);
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
                Length.of(3.1, LengthUnit.METER)
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
        assertEquals(4, imported.rooms().getFirst().ceilingVertexHeights().size());
        assertEquals(2600.0, imported.rooms().getFirst().ceilingVertexHeights().get(3).toMillimeters(), 0.001);
    }
}
