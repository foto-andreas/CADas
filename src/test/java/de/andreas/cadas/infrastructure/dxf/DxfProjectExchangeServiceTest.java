package de.andreas.cadas.infrastructure.dxf;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        project.primaryLevel().addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(5000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
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
    }

    @Test
    void exportiertMetrischeHeaderUndProjektnamenFuerSpaeterenUiImport() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Importname", "Erdgeschoss");
        project.primaryLevel().addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(1500, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));

        Path file = tempDir.resolve("projekt-header.dxf");
        exchangeService.exportProject(project, file);
        String dxf = java.nio.file.Files.readString(file);
        ProjectModel imported = exchangeService.importProject(file, "Fallback");

        assertTrue(dxf.contains("$INSUNITS\n70\n4"));
        assertTrue(dxf.contains("$MEASUREMENT\n70\n1"));
        assertEquals("Importname", imported.name());
    }
}
