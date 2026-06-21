package de.schrell.cadas.application.reports;

import de.schrell.cadas.application.drawing.DimensionLabelOptions;
import de.schrell.cadas.application.drawing.DimensionTextStyle;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.ProjectModel;
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
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;

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
    void exportiertGetrennteHeizpläneFürEtageBodenUndDecke() throws Exception {
        ProjectModel project = sampleProject();
        Room groundFloorRoom = project.primaryLevel().rooms().getFirst();
        project.primaryLevel().addHydronicHeating(heating(
                groundFloorRoom, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, "FBH 1"
        ));
        project.primaryLevel().addHydronicHeating(heating(
                groundFloorRoom, HeatingSurfacePosition.CEILING, HeatingLayoutPattern.SPIRAL, "DH 1"
        ));
        var upperLevel = project.createLevel("Obergeschoss");
        Room upperRoom = Room.rectangular(
                "Kind", new PlanPoint(0, 0), new PlanPoint(4_000, 3_000),
                Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
        );
        upperLevel.addRoom(upperRoom);
        upperLevel.addHydronicHeating(heating(
                upperRoom, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, "FBH OG"
        ));
        Path target = tempDir.resolve("heizplaene.pdf");

        new ConstructionDrawingPdfService().export(project, target);

        try (var document = Loader.loadPDF(target.toFile())) {
            assertEquals(7, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Heizflächen Fußboden - Erdgeschoss"));
            assertTrue(text.contains("Heizflächen Decke - Erdgeschoss"));
            assertTrue(text.contains("Heizflächen Fußboden - Obergeschoss"));
            assertTrue(text.contains("FBH 1"));
            assertTrue(text.contains("DH 1"));
            assertTrue(text.contains("FBH OG"));
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
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(8_000, 0)),
                Length.of(24, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
        project.primaryLevel().addWall(wall);
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(8_000, 5_000),
                Length.of(2.60, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        project.primaryLevel().addRoom(room);
        project.primaryLevel().addDoor(Door.create(
                wall.id(), Length.ofMillimeters(800), Length.ofMillimeters(1_000),
                Length.ofMillimeters(2_010), Length.zero()
        ));
        project.primaryLevel().addWindow(WindowElement.create(
                wall.id(), Length.ofMillimeters(2_800), Length.ofMillimeters(1_200),
                Length.ofMillimeters(900), Length.ofMillimeters(1_200)
        ));
        project.primaryLevel().addStaircase(Staircase.create(
                StairType.STRAIGHT, new PlanPoint(500, 1_000), new PlanPoint(1_500, 3_500),
                Length.ofMillimeters(2_600), 13
        ));
        project.primaryLevel().addFloorExtension(FloorExtension.create(
                FloorExtensionType.BALCONY, FloorExtensionPlacement.EXTERIOR,
                new PlanPoint(8_000, 1_000), new PlanPoint(10_000, 3_000), Length.ofMillimeters(180)
        ));
        project.primaryLevel().addFloorOpening(FloorOpening.create(
                room.id(), FloorOpeningShape.RECTANGLE, new PlanPoint(6_000, 3_000),
                Length.ofMillimeters(1_000), Length.ofMillimeters(1_500)
        ));
        return project;
    }

    private HydronicHeating heating(
            Room room,
            HeatingSurfacePosition surfacePosition,
            HeatingLayoutPattern layoutPattern,
            String zoneName
    ) {
        return HydronicHeating.create(
                room.id(), surfacePosition, layoutPattern,
                Length.ofMillimeters(200), Length.ofMillimeters(16), Length.ofMillimeters(300_000),
                Length.ofMillimeters(150), room.outline().getFirst(), new PlanPoint(200, 0)
        ).withZones(java.util.List.of(HeatingZone.create(zoneName, room.outline())));
    }
}
