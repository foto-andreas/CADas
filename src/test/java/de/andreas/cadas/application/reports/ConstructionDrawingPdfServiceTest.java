package de.andreas.cadas.application.reports;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Wall;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructionDrawingPdfServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void exportiertGrundrissAufrisseUndRäumlicheAnsichtenMaßstabgerecht() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Wohnhaus", "Erdgeschoss");
        project.primaryLevel().addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(8_000, 0)),
                Length.of(24, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));
        project.primaryLevel().addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(8_000, 5_000),
                Length.of(2.60, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        Path target = tempDir.resolve("bauzeichnung.pdf");

        new ConstructionDrawingPdfService().export(project, target);

        assertTrue(Files.size(target) > 2_000);
        try (var document = Loader.loadPDF(target.toFile())) {
            assertEquals(4, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("2D-Grundriss"));
            assertTrue(text.contains("Seitenaufrisse"));
            assertTrue(text.contains("3D-ISO"));
            assertTrue(text.contains("3D-Seitenansichten"));
            assertTrue(text.contains(ConstructionDrawingPdfService.STANDARD));
            assertTrue(text.contains("M 1:"));
        }
    }
}
