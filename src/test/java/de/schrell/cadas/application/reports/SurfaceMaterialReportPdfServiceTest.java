package de.schrell.cadas.application.reports;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SurfaceMaterialReportPdfServiceTest {

    private final SurfaceMaterialListService reportService = new SurfaceMaterialListService();
    private final SurfaceMaterialReportPdfService pdfService = new SurfaceMaterialReportPdfService();

    @Test
    void exportiertMaterialberichtAlsPdf() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4_000, 3_000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        project.primaryLevel().addRoom(room);
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        stack.addLayer(SurfaceLayer.create(
                "Parkett",
                Length.of(12, LengthUnit.MILLIMETER),
                Length.of(120, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                SurfaceLayoutMode.NONE,
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.of(2, LengthUnit.MILLIMETER),
                ""
        ));
        project.primaryLevel().addSurfaceLayerStack(stack);

        Path targetFile = Files.createTempFile("materialbericht-", ".pdf");
        Files.deleteIfExists(targetFile);

        pdfService.export(reportService.create(project), targetFile);

        assertTrue(Files.exists(targetFile));
        assertTrue(Files.size(targetFile) > 100);
        byte[] bytes = Files.readAllBytes(targetFile);
        assertArrayEquals(new byte[]{'%', 'P', 'D', 'F'}, bytes.length >= 4
                ? java.util.Arrays.copyOf(bytes, 4)
                : new byte[0]);

        Files.deleteIfExists(targetFile);
    }
}
