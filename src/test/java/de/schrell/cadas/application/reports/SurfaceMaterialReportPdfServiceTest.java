package de.schrell.cadas.application.reports;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class SurfaceMaterialReportPdfServiceTest {

    private final SurfaceMaterialListService reportService = new SurfaceMaterialListService();
    private final SurfaceMaterialReportPdfService pdfService = new SurfaceMaterialReportPdfService();

    @Test
    void exportiertMaterialberichtAlsPdf() throws Exception {
        SurfaceMaterialListService.SurfaceMaterialReport report = reportService.create(beispielProjekt());
        Path targetFile = Files.createTempFile("materialbericht-", ".pdf");
        Files.deleteIfExists(targetFile);

        pdfService.export(report, targetFile);

        assertStandardInhalt(targetFile, false);
        Files.deleteIfExists(targetFile);
    }

    @Test
    void exportiertAuchEinProjektOhneRäumeUndAusstattung() throws Exception {
        SurfaceMaterialListService.SurfaceMaterialReport report = reportService.create(
                ProjectModel.withDefaultLevel("Leerprojekt", "Erdgeschoss")
        );
        Path targetFile = Files.createTempFile("materialbericht-leer-", ".pdf");

        pdfService.export(report, targetFile);

        try (var document = Loader.loadPDF(targetFile.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Räume und Materialien - Leerprojekt"));
            assertTrue(text.contains("Keine Räume vorhanden."));
            assertTrue(text.contains("Keine Beläge vorhanden."));
            assertTrue(text.contains("Keine Flächenheizungen vorhanden."));
            assertTrue(text.contains("Keine Heizelemente vorhanden."));
        }
        Files.deleteIfExists(targetFile);
    }

    @Test
    void exportiertMaterialberichtMitRastergrafikenUndEtagenbild() throws Exception {
        SurfaceMaterialListService.SurfaceMaterialReport report = reportService.create(beispielProjekt());
        Path targetFile = Files.createTempFile("materialbericht-raster-", ".pdf");
        Files.deleteIfExists(targetFile);

        BufferedImage levelImage = new BufferedImage(640, 360, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D levelGraphics = levelImage.createGraphics();
        levelGraphics.fillRect(0, 0, 640, 360);
        levelGraphics.dispose();
        BufferedImage heatingImage = new BufferedImage(520, 420, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D heatingGraphics = heatingImage.createGraphics();
        heatingGraphics.fillRect(0, 0, 520, 420);
        heatingGraphics.dispose();

        pdfService.export(
                report,
                targetFile,
                new SurfaceMaterialReportPdfService.ExportAssets(
                        Map.of("Erdgeschoss", levelImage),
                        Map.of(SurfaceMaterialReportPdfService.materialLevelImageKey(report.materials().getFirst(), "Erdgeschoss"), levelImage),
                        Map.of(
                                SurfaceMaterialReportPdfService.heatingLevelImageKey("Erdgeschoss", "Fußboden"), heatingImage,
                                SurfaceMaterialReportPdfService.heatingLevelImageKey("Erdgeschoss", "Decke"), heatingImage
                        )
                )
        );

        assertStandardInhalt(targetFile, false);
        try (var document = Loader.loadPDF(targetFile.toFile())) {
            assertTrue(document.getNumberOfPages() >= 4);
            String text = new PDFTextStripper().getText(document);
            assertFalse(text.contains("Rasterzeichnungen Räume und Beläge"));
            assertFalse(text.contains("Heizflächen-Grafiken"));
            assertTrue(text.contains("Etagenübersicht Erdgeschoss"));
            assertTrue(text.contains("Belag Erdgeschoss / Parkett"));
            assertFalse(text.contains("Heizkreise Erdgeschoss / Fußboden"));
            assertTrue(text.contains("Heizflächen Fußboden - Erdgeschoss"));
            assertTrue(text.contains("Heizflächen Decke - Erdgeschoss"));
            assertTrue(text.indexOf("Heizflächen Fußboden - Erdgeschoss") > text.indexOf("Flächenheizungen"));
        }

        Files.deleteIfExists(targetFile);
    }

    private ProjectModel beispielProjekt() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4_000, 3_000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ).withHeatLoadWatts(900.0);
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
        SurfaceLayerStack ceilingStack = new SurfaceLayerStack(SurfaceType.CEILING, room.id().toString());
        ceilingStack.addLayer(SurfaceLayer.create(
                "Parkett",
                Length.of(15, LengthUnit.MILLIMETER),
                Length.of(80, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2, LengthUnit.MILLIMETER)
        ));
        project.primaryLevel().addSurfaceLayerStack(ceilingStack);
        project.primaryLevel().addRoomObject(RoomObject.create(
                "konvektor",
                "Konvektor",
                RoomObjectType.CUBOID,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(2_000, 1_500),
                Length.of(1, LengthUnit.METER),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                false,
                ""
        ).withHeatOutputWatts(500.0));
        project.primaryLevel().addHydronicHeating(HydronicHeating.create(
                room.id(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.MEANDER,
                Length.ofMillimeters(200),
                Length.ofMillimeters(16),
                Length.ofMillimeters(200_000),
                Length.ofMillimeters(150),
                room.outline().getFirst(),
                new PlanPoint(200, 0)
        ).withZones(List.of(HeatingZone.create("FBH 1", room.outline(), HeatingLayoutPattern.MEANDER))));
        return project;
    }

    private void assertStandardInhalt(Path targetFile, boolean expectSvgText) throws Exception {
        assertTrue(Files.exists(targetFile));
        assertTrue(Files.size(targetFile) > 100);
        byte[] bytes = Files.readAllBytes(targetFile);
        assertArrayEquals(new byte[]{'%', 'P', 'D', 'F'}, bytes.length >= 4
                ? java.util.Arrays.copyOf(bytes, 4)
                : new byte[0]);
        try (var document = Loader.loadPDF(targetFile.toFile())) {
            assertTrue(document.getNumberOfPages() >= 1);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Räume und Materialien - Haus"));
            assertTrue(text.contains("Räume und Wohnflächen nach WoFlV"));
            assertTrue(text.contains("Innenumfang"));
            assertTrue(text.contains("14,00 m"));
            assertTrue(text.contains("Zusammenfassung"));
            assertTrue(text.contains("Trennmerkmale bei mehrfachen Materialnamen"));
            assertTrue(text.contains("Heizlast"));
            assertTrue(text.contains("Flächenheizungen"));
            assertTrue(text.contains("Konvektor"));
            assertFalse(text.contains("Reststücke"));
            if (expectSvgText) {
                assertTrue(text.contains("Heizplan Erdgeschoss / Wohnen"));
                assertTrue(text.contains("V1"));
                assertTrue(text.contains("R1"));
            }
            assertTrue(text.contains("Beläge"));
        }
    }
}
