package de.schrell.cadas.infrastructure.dxf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Angle;
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
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.Roof;
import de.schrell.cadas.domain.model.RoofType;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;
import de.schrell.cadas.domain.model.WindowElement;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DxfProjectExchangeServiceTest {

    private final DxfProjectExchangeService exchangeService = new DxfProjectExchangeService();

    @TempDir
    Path tempDir;

    @Test
    void exportiertUndImportiertMehrereEtagenUndDachInEinerDatei() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Wall wall = new Wall(
                java.util.UUID.randomUUID(),
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(5000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(3.1, LengthUnit.METER),
                Length.of(2.5, LengthUnit.METER),
                Length.of(3.1, LengthUnit.METER),
                java.util.List.of(
                        new WallProfilePoint(Length.zero(), Length.of(2.5, LengthUnit.METER)),
                        new WallProfilePoint(Length.of(3, LengthUnit.METER), Length.of(3.1, LengthUnit.METER)),
                        new WallProfilePoint(Length.of(5, LengthUnit.METER), Length.of(3.1, LengthUnit.METER))
                )
        );
        project.primaryLevel().addWall(wall);
        Door door = Door.create(
                wall.id(),
                Length.of(1.0, LengthUnit.METER),
                Length.of(1.01, LengthUnit.METER),
                Length.of(2.01, LengthUnit.METER),
                Length.zero()
        );
        project.primaryLevel().addDoor(door);
        WindowElement window = WindowElement.create(
                wall.id(),
                Length.of(2.5, LengthUnit.METER),
                Length.of(1.2, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.2, LengthUnit.METER)
        );
        project.primaryLevel().addWindow(window);
        project.primaryLevel().addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(5000, 4000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        project.primaryLevel().addFloorOpening(FloorOpening.create(
                project.primaryLevel().rooms().getFirst().id(), FloorOpeningShape.CIRCLE,
                new PlanPoint(2_000, 2_000), Length.ofMillimeters(1_000), Length.ofMillimeters(1_000)
        ));
        project.primaryLevel().addStaircase(new Staircase(
                java.util.UUID.randomUUID(),
                StairType.HALF_TURN,
                new PlanPoint(600, 500),
                new PlanPoint(2400, 4200),
                Length.of(2.9, LengthUnit.METER),
                18,
                0,
                Length.of(75, LengthUnit.CENTIMETER),
                Length.of(50, LengthUnit.CENTIMETER),
                Length.of(12, LengthUnit.CENTIMETER),
                Length.of(15, LengthUnit.CENTIMETER),
                Length.of(8, LengthUnit.CENTIMETER)
        ));
        project.primaryLevel().addFloorExtension(FloorExtension.create(FloorExtensionType.GALLERY, FloorExtensionPlacement.INTERIOR,
                new PlanPoint(3_000, 500), new PlanPoint(5_000, 2_000), Length.ofMillimeters(200)));
        var og = project.createLevel("Obergeschoss");
        og.addRoom(Room.rectangular(
                "Kind",
                new PlanPoint(500, 500),
                new PlanPoint(2500, 2500),
                Length.of(2.5, LengthUnit.METER),
                Length.of(16, LengthUnit.CENTIMETER),
                Length.of(18, LengthUnit.CENTIMETER)
        ));
        project.defineRoof(new Roof(RoofType.SADDLE, Angle.ofDegrees(38), Length.of(45, LengthUnit.CENTIMETER), true));

        Path file = tempDir.resolve("gebaeude.dxf");
        exchangeService.exportProject(project, file);
        String exportedDxf = Files.readString(file);
        assertTrue(exportedDxf.contains("FOPEN"));
        ProjectModel imported = exchangeService.importProject(file, "Import");

        assertEquals(2, imported.levels().size());
        assertEquals("Haus", imported.name());
        assertTrue(imported.roof().isPresent());
        assertEquals(1, imported.primaryLevel().staircases().size());
        assertEquals(1, imported.primaryLevel().floorExtensions().size());
        assertEquals(1, imported.primaryLevel().floorOpenings().size());
        assertEquals(FloorOpeningShape.CIRCLE, imported.primaryLevel().floorOpenings().getFirst().shape());
        assertEquals(FloorExtensionType.GALLERY, imported.primaryLevel().floorExtensions().getFirst().type());
        assertEquals(750, imported.primaryLevel().staircases().getFirst().startLandingWidth().toMillimeters(), 0.001);
        assertEquals(500, imported.primaryLevel().staircases().getFirst().endLandingWidth().toMillimeters(), 0.001);
        assertEquals(120, imported.primaryLevel().staircases().getFirst().leftUnderbuildWidth().toMillimeters(), 0.001);
        assertEquals(150, imported.primaryLevel().staircases().getFirst().rightUnderbuildWidth().toMillimeters(), 0.001);
        assertEquals(80, imported.primaryLevel().staircases().getFirst().undersideThickness().toMillimeters(), 0.001);
        assertEquals(door.id(), imported.primaryLevel().doors().getFirst().id());
        assertEquals(window.id(), imported.primaryLevel().windows().getFirst().id());
        assertEquals("Obergeschoss", imported.levels().get(1).name());
        assertEquals(2500.0, imported.primaryLevel().walls().getFirst().startHeight().toMillimeters(), 0.001);
        assertEquals(3100.0, imported.primaryLevel().walls().getFirst().endHeight().toMillimeters(), 0.001);
        assertEquals(3, imported.primaryLevel().walls().getFirst().profile().size());
        assertEquals(3000.0, imported.primaryLevel().walls().getFirst().profile().get(1).offset().toMillimeters(), 0.001);
    }

    @Test
    void schreibtExportierteDxfDateiAufDiePlatte() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        project.primaryLevel().addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(5_000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        ));
        Path file = tempDir.resolve("export-auf-platte.dxf");

        exchangeService.exportProject(project, file);

        assertTrue(Files.exists(file), "DXF-Datei wurde nicht auf die Platte geschrieben.");
        assertTrue(Files.size(file) > 0, "DXF-Datei ist leer.");
        String content = Files.readString(file);
        assertTrue(content.contains("EOF"), "DXF-Datei enthält kein EOF.");
        assertTrue(content.contains("PROJECT|Haus"), "DXF-Datei enthält keine Projekt-Metadaten.");
    }

    @Test
    void importiertAlteGebaeudeOeffnungenOhneObjektIds() throws Exception {
        java.util.UUID wallId = java.util.UUID.randomUUID();
        Path file = tempDir.resolve("alte-gebaeude-oeffnungen.dxf");
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
                PROJECT|Altbau
                0
                TEXT
                8
                CADAS_META
                1
                LEVEL|Erdgeschoss
                0
                TEXT
                8
                CADAS_META
                1
                WALL|Erdgeschoss|%s|175.000|2750.000|0.000|0.000|4000.000|0.000
                0
                TEXT
                8
                CADAS_META
                1
                DOOR|Erdgeschoss|%s|1000.000|1010.000|2010.000|0.000
                0
                TEXT
                8
                CADAS_META
                1
                WINDOW|Erdgeschoss|%s|2200.000|1200.000|900.000|1200.000
                0
                ENDSEC
                0
                EOF
                """.formatted(wallId, wallId, wallId));

        ProjectModel imported = exchangeService.importProject(file, "Fallback");

        assertEquals("Altbau", imported.name());
        assertEquals(1, imported.primaryLevel().doors().size());
        assertEquals(1, imported.primaryLevel().windows().size());
        assertEquals(wallId, imported.primaryLevel().doors().getFirst().wallId());
        assertEquals(wallId, imported.primaryLevel().windows().getFirst().wallId());
    }

    @Test
    void importiertGebaeudeOeffnungenReihenfolgeunabhaengigUndNurMitHostWand() throws Exception {
        java.util.UUID wallId = java.util.UUID.randomUUID();
        java.util.UUID missingWallId = java.util.UUID.randomUUID();
        java.util.UUID doorId = java.util.UUID.randomUUID();
        java.util.UUID windowId = java.util.UUID.randomUUID();
        Path file = tempDir.resolve("oeffnungen-reihenfolge.dxf");
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
                CADAS_DXF|2
                0
                TEXT
                8
                CADAS_META
                1
                PROJECT|Haus
                0
                TEXT
                8
                CADAS_META
                1
                LEVEL|Erdgeschoss
                0
                TEXT
                8
                CADAS_META
                1
                DOOR|Erdgeschoss|%s|%s|1000.000|1010.000|2010.000|0.000
                0
                TEXT
                8
                CADAS_META
                1
                WINDOW|Erdgeschoss|%s|%s|2200.000|1200.000|900.000|1200.000
                0
                TEXT
                8
                CADAS_META
                1
                WALL|Erdgeschoss|%s|175.000|2750.000|2750.000|2750.000|0.000|0.000|4000.000|0.000
                0
                ENDSEC
                0
                EOF
                """.formatted(doorId, wallId, windowId, missingWallId, wallId));

        ProjectModel imported = exchangeService.importProject(file, "Fallback");

        assertEquals(1, imported.primaryLevel().doors().size());
        assertEquals(doorId, imported.primaryLevel().doors().getFirst().id());
        assertTrue(imported.primaryLevel().windows().isEmpty());
    }

    @Test
    void exportiertUndImportiertOberflaechenStapelMitEtagen() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        project.primaryLevel().addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(5000, 4000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        var egRoom = project.primaryLevel().rooms().getFirst();
        SurfaceLayerStack egFloor = new SurfaceLayerStack(
                java.util.UUID.randomUUID(), SurfaceType.FLOOR, egRoom.id().toString()
        );
        egFloor.addLayer(new SurfaceLayer(
                java.util.UUID.randomUUID(),
                "Parkett",
                Length.of(14, LengthUnit.MILLIMETER),
                true,
                Length.of(500, LengthUnit.MILLIMETER),
                Length.of(500, LengthUnit.MILLIMETER),
                SurfaceLayoutMode.NONE,
                Length.zero(),
                Length.of(5, LengthUnit.MILLIMETER),
                Length.of(10, LengthUnit.MILLIMETER),
                Length.of(10, LengthUnit.MILLIMETER),
                Length.of(1, LengthUnit.MILLIMETER),
                SurfaceCutRestriction.LAY_DIRECTION_OUTER_CUTS,
                "Holz"
        ));
        project.primaryLevel().addSurfaceLayerStack(egFloor);
        project.primaryLevel().addRoomObject(RoomObject.create(
                "dwg-spiegel",
                "DWG-Spiegel",
                RoomObjectType.DWG_REFERENCE,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(1500, 1500),
                Length.of(80, LengthUnit.CENTIMETER),
                Length.of(5, LengthUnit.CENTIMETER),
                Length.of(120, LengthUnit.CENTIMETER),
                22.5,
                RoomObjectMountingMode.WALL_MOUNTED,
                "Spiegel.dwg#Block"
        ).withBaseElevation(Length.of(-15, LengthUnit.CENTIMETER)));
        project.defineTerrain(new Terrain(java.util.List.of(
                new TerrainVertex(new PlanPoint(0, 0), Length.ofMillimeters(-100)),
                new TerrainVertex(new PlanPoint(5000, 0), Length.ofMillimeters(200)),
                new TerrainVertex(new PlanPoint(5000, 4000), Length.ofMillimeters(600)),
                new TerrainVertex(new PlanPoint(0, 4000), Length.ofMillimeters(300))
        )));

        var og = project.createLevel("Obergeschoss");
        og.addRoom(Room.rectangular(
                "Kind",
                new PlanPoint(500, 500),
                new PlanPoint(2500, 2500),
                Length.of(2.5, LengthUnit.METER),
                Length.of(16, LengthUnit.CENTIMETER),
                Length.of(18, LengthUnit.CENTIMETER)
        ));
        var ogRoom = og.rooms().getFirst();
        SurfaceLayerStack ogCeiling = new SurfaceLayerStack(
                java.util.UUID.randomUUID(), SurfaceType.CEILING, ogRoom.id().toString()
        );
        ogCeiling.addLayer(new SurfaceLayer(
                java.util.UUID.randomUUID(),
                "Farbe",
                Length.of(2, LengthUnit.MILLIMETER),
                true,
                Length.of(1000, LengthUnit.MILLIMETER),
                Length.of(1000, LengthUnit.MILLIMETER),
                SurfaceLayoutMode.FIXED,
                Length.of(300, LengthUnit.MILLIMETER),
                Length.zero(),
                Length.zero(),
                Length.zero(),
                "Weiss"
        ));
        og.addSurfaceLayerStack(ogCeiling);

        Path file = tempDir.resolve("gebaeude-mit-ebenen.dxf");
        exchangeService.exportProject(project, file);
        ProjectModel imported = exchangeService.importProject(file, "Import");

        assertEquals(2, imported.levels().size());
        var importedEg = imported.levels().getFirst();
        assertEquals(1, importedEg.surfaceLayerStacks().size());
        assertEquals(SurfaceType.FLOOR, importedEg.surfaceLayerStacks().getFirst().surfaceType());
        assertEquals(1, importedEg.surfaceLayerStacks().getFirst().layers().size());
        assertEquals("Parkett", importedEg.surfaceLayerStacks().getFirst().layers().getFirst().name());
        assertEquals(1.0, importedEg.surfaceLayerStacks().getFirst().layers().getFirst().jointWidth().toMillimeters(), 0.001);
        assertEquals(SurfaceCutRestriction.LAY_DIRECTION_OUTER_CUTS, importedEg.surfaceLayerStacks().getFirst().layers().getFirst().cutRestriction());
        assertEquals(1, importedEg.roomObjects().size());
        assertEquals("DWG-Spiegel", importedEg.roomObjects().getFirst().name());
        assertEquals(RoomObjectMountingMode.WALL_MOUNTED, importedEg.roomObjects().getFirst().mountingMode());
        assertFalse(importedEg.roomObjects().getFirst().cutsFloorCovering());
        assertEquals(22.5, importedEg.roomObjects().getFirst().rotationDegrees(), 0.001);
        assertEquals(-150.0, importedEg.roomObjects().getFirst().baseElevation().toMillimeters(), 0.001);
        assertEquals(4, imported.terrain().vertices().size());
        assertEquals(600.0, imported.terrain().vertices().get(2).elevationAboveLowestFloor().toMillimeters(), 0.001);
        assertEquals(egRoom.id(), importedEg.rooms().getFirst().id(), "Raum-UUID muss im Rundlauf erhalten bleiben");

        var importedOg = imported.levels().get(1);
        assertEquals(1, importedOg.surfaceLayerStacks().size());
        assertEquals(SurfaceType.CEILING, importedOg.surfaceLayerStacks().getFirst().surfaceType());
        assertEquals("Farbe", importedOg.surfaceLayerStacks().getFirst().layers().getFirst().name());
        assertEquals(SurfaceLayoutMode.FIXED, importedOg.surfaceLayerStacks().getFirst().layers().getFirst().layoutMode());
        assertEquals(ogRoom.id(), importedOg.rooms().getFirst().id(), "Raum-UUID muss im Rundlauf erhalten bleiben");
    }

    @Test
    void exportiertMetrischeHeaderUndProjektnamenFuerSpaeterenUiImport() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Importname", "Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(1500, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
        project.primaryLevel().addWall(wall);
        project.primaryLevel().addStaircase(Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(500, 500),
                new PlanPoint(1500, 2500),
                Length.of(2.75, LengthUnit.METER),
                14
        ));

        Path file = tempDir.resolve("projekt-header.dxf");
        exchangeService.exportProject(project, file);
        String dxf = java.nio.file.Files.readString(file);
        ProjectModel imported = exchangeService.importProject(file, "Fallback");

        assertTrue(dxf.contains("$INSUNITS\n70\n4"));
        assertTrue(dxf.contains("$MEASUREMENT\n70\n1"));
        assertTrue(dxf.contains("\n2\nTABLES\n"));
        assertTrue(dxf.contains("\n2\nBLOCKS\n"));
        assertTrue(dxf.contains("\n2\nOBJECTS\n"));
        assertTrue(dxf.contains("\n0\nINSERT\n"));
        assertTrue(dxf.contains("\n2\nCADAS_STAIR\n"));
        assertEquals("Importname", imported.name());
    }

    @Test
    void erhaeltSonderzeichenInProjektEtageUndRaum() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus | Süd/West.dxf", "EG | Wohnen/Kochen");
        project.primaryLevel().addRoom(Room.rectangular(
                "Küche | Essen/Kind",
                new PlanPoint(0, 0),
                new PlanPoint(3500, 3000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));

        Path file = tempDir.resolve("projekt-sonderzeichen.dxf");
        exchangeService.exportProject(project, file);
        String dxf = Files.readString(file);
        ProjectModel imported = exchangeService.importProject(file, "Fallback");

        assertTrue(dxf.contains("CADAS_DXF|4"));
        assertFalse(dxf.contains("Haus | Süd/West"));
        assertEquals("Haus | Süd/West", imported.name());
        assertEquals("EG | Wohnen/Kochen", imported.primaryLevel().name());
        assertEquals("Küche | Essen/Kind", imported.primaryLevel().rooms().getFirst().name());
    }

    @Test
    void ignoriertBeschaedigteMetadatenEintraegeBeimProjektimport() throws Exception {
        Path file = tempDir.resolve("beschaedigte-metadaten.dxf");
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
                CADAS_DXF|2
                ABC
                wird ignoriert
                0
                TEXT
                8
                CADAS_META
                1
                PROJECT|Robustes+Haus
                0
                TEXT
                8
                CADAS_META
                1
                LEVEL|Erdgeschoss
                0
                TEXT
                8
                CADAS_META
                1
                WALL|Erdgeschoss
                0
                ENDSEC
                0
                EOF
                """);

        ProjectModel imported = exchangeService.importProject(file, "Fallback");

        assertEquals("Robustes Haus", imported.name());
        assertEquals(1, imported.levels().size());
        assertEquals("Erdgeschoss", imported.primaryLevel().name());
        assertTrue(imported.primaryLevel().walls().isEmpty());
    }

    @Test
    void vollstaendigerRundlaufMitOberflaechenEbenen() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "EG");
        var room = Room.rectangular("Wohnzimmer",
                new PlanPoint(0, 0), new PlanPoint(4000, 3000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER));
        project.primaryLevel().addRoom(room);
        SurfaceLayerStack floorStack = new SurfaceLayerStack(
                java.util.UUID.randomUUID(), SurfaceType.FLOOR, room.id().toString());
        floorStack.addLayer(new SurfaceLayer(
                java.util.UUID.randomUUID(), "Dielen", Length.of(12, LengthUnit.MILLIMETER), true,
                Length.of(600, LengthUnit.MILLIMETER), Length.of(120, LengthUnit.MILLIMETER),
                SurfaceLayoutMode.AUTOMATIC, Length.of(50, LengthUnit.MILLIMETER),
                Length.of(5, LengthUnit.MILLIMETER), Length.of(8, LengthUnit.MILLIMETER),
                Length.of(2, LengthUnit.MILLIMETER), "Eiche"));
        project.primaryLevel().addSurfaceLayerStack(floorStack);

        Path file = tempDir.resolve("rundlauf.dxf");
        exchangeService.exportProject(project, file);
        ProjectModel imported = exchangeService.importProject(file, "Import");

        var importedLevel = imported.primaryLevel();
        assertFalse(importedLevel.surfaceLayerStacks().isEmpty(), "Oberflächenstapel müssen importiert werden");
        var importedStack = importedLevel.surfaceLayerStacks().getFirst();
        assertEquals(room.id(), importedLevel.rooms().getFirst().id(), "Raum-UUID erhalten");
        assertEquals(room.id().toString(), importedStack.targetKey(), "SLS-targetKey muss Raum-UUID entsprechen");
        assertFalse(importedStack.layers().isEmpty());
        assertEquals("Dielen", importedStack.layers().getFirst().name());
    }
}
