package de.schrell.cadas.application.reports;

import de.schrell.cadas.application.drawing.DimensionLabelOptions;
import de.schrell.cadas.application.drawing.DimensionTextStyle;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.Wall;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructionDrawingPdfServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void exportiertGrundrissAufrisseUndRäumlicheAnsichtenMaßstabgerecht() throws Exception {
        ProjectModel project = sampleProject();
        Path target = tempDir.resolve("bauzeichnung.pdf");

        new ConstructionDrawingPdfService().export(project, target);

        assertTrue(Files.size(target) > 2_000);
        try (var document = Loader.loadPDF(target.toFile())) {
            assertEquals(3, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("2D-Grundriss"));
            assertTrue(text.contains("Seitenansichten - gesamtes Gebäude"));
            assertTrue(text.contains("3D-ISO"));
            assertTrue(text.contains(ConstructionDrawingPdfService.STANDARD));
            assertTrue(text.contains("M 1:"));
        }
    }

    @Test
    void exportiertGrundrisseEinzelnUndGebäudeansichtenGemeinsam() throws Exception {
        ProjectModel project = sampleProject();
        var upperLevel = project.createLevel("Obergeschoss");
        upperLevel.addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(6_000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.5, LengthUnit.METER)
        ));
        Path target = tempDir.resolve("mehrere-etagen.pdf");

        new ConstructionDrawingPdfService().export(project, target);

        try (var document = Loader.loadPDF(target.toFile())) {
            assertEquals(4, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("2D-Grundriss - Erdgeschoss"));
            assertTrue(text.contains("2D-Grundriss - Obergeschoss"));
            assertTrue(text.contains("3D-ISO - gesamtes Gebäude"));
            assertTrue(text.contains("Seitenansichten - gesamtes Gebäude"));
            assertFalse(text.contains("Seitenaufrisse - Erdgeschoss"));
            assertFalse(text.contains("Seitenaufrisse - Obergeschoss"));
        }
    }

    @Test
    void exportiertMitNurLängeOhneRaummaßVorsatz() throws Exception {
        ProjectModel project = sampleProject();
        Path target = tempDir.resolve("bauzeichnung_nurLaenge.pdf");
        ConstructionDrawingOptions options = new ConstructionDrawingOptions(
                DimensionLabelOptions.lengthOnly(), true, true
        );

        new ConstructionDrawingPdfService().export(project, target, options);

        try (var document = Loader.loadPDF(target.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertFalse(text.contains("Raummaß"), "Raummaß-Vorsatz darf bei LENGTH_ONLY nicht auftreten");
            assertFalse(text.contains("Außenmaß"), "Außenmaß-Vorsatz darf bei LENGTH_ONLY nicht auftreten");
        }
    }

    @Test
    void exportiertMitVollständigenTextenEnthältRaummassUndAussenmass() throws Exception {
        ProjectModel project = sampleProject();
        Path target = tempDir.resolve("bauzeichnung_voll.pdf");
        ConstructionDrawingOptions options = new ConstructionDrawingOptions(
                DimensionLabelOptions.full(), true, true
        );

        new ConstructionDrawingPdfService().export(project, target, options);

        try (var document = Loader.loadPDF(target.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Raummaß") || text.contains("Außenmaß"),
                    "Vollständiger Text muss Raum- oder Außenmaß-Vorsatz enthalten");
        }
    }

    @Test
    void optionenMitAbgeschalteterBemaßungZeigenKeineMaßtexte() throws Exception {
        ProjectModel project = sampleProject();
        Path target = tempDir.resolve("bauzeichnung_ohneBemaßung.pdf");
        ConstructionDrawingOptions options = new ConstructionDrawingOptions(
                DimensionLabelOptions.full(), false, true
        );

        new ConstructionDrawingPdfService().export(project, target, options);

        try (var document = Loader.loadPDF(target.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertFalse(text.contains("Raummaß"), "Bemaßung darf nicht auftreten, wenn showDimensions=false");
            assertFalse(text.contains("Außenmaß"), "Bemaßung darf nicht auftreten, wenn showDimensions=false");
        }
    }

    @Test
    void begrenztTiefenversatzDamitEineEtageNichtWieMehrereWirkt() {
        ProjectModel project = ProjectModel.withDefaultLevel("Langhaus", "Keller");
        project.primaryLevel().addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(20_000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.5, LengthUnit.METER)
        ));
        project.primaryLevel().addWall(Wall.create(
                new PlanSegment(new PlanPoint(20_000, 0), new PlanPoint(20_000, 10_000)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.5, LengthUnit.METER)
        ));
        ConstructionDrawingPdfService service = new ConstructionDrawingPdfService();

        double factor = service.spatialDepthFactor(project, Math.toRadians(45.0));
        double depthSpan = 20_000 * Math.sin(Math.toRadians(45.0)) + 10_000 * Math.cos(Math.toRadians(45.0));

        assertTrue(factor < 0.1);
        assertTrue(depthSpan * factor < 1_400.0);
    }

    private ProjectModel sampleProject() {
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
        return project;
    }
}
