package de.andreas.cadas.infrastructure.dxf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Angle;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Roof;
import de.andreas.cadas.domain.model.RoofType;
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceLayoutMode;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;
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
        project.primaryLevel().addWall(new Wall(
                java.util.UUID.randomUUID(),
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(5000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(3.1, LengthUnit.METER),
                Length.of(2.5, LengthUnit.METER),
                Length.of(3.1, LengthUnit.METER)
        ));
        project.primaryLevel().addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(5000, 4000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        project.primaryLevel().addStaircase(new Staircase(
                java.util.UUID.randomUUID(),
                StairType.HALF_TURN,
                new PlanPoint(600, 500),
                new PlanPoint(2400, 4200),
                Length.of(2.9, LengthUnit.METER),
                18,
                0
        ));
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
        ProjectModel imported = exchangeService.importProject(file, "Import");

        assertEquals(2, imported.levels().size());
        assertEquals("Haus", imported.name());
        assertTrue(imported.roof().isPresent());
        assertEquals(1, imported.primaryLevel().staircases().size());
        assertEquals("Obergeschoss", imported.levels().get(1).name());
        assertEquals(2500.0, imported.primaryLevel().walls().getFirst().startHeight().toMillimeters(), 0.001);
        assertEquals(3100.0, imported.primaryLevel().walls().getFirst().endHeight().toMillimeters(), 0.001);
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
                Length.of(1, LengthUnit.MILLIMETER),
                "Holz"
        ));
        project.primaryLevel().addSurfaceLayerStack(egFloor);

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
