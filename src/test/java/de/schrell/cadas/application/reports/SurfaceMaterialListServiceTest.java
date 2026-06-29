package de.schrell.cadas.application.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.layers.WallSurfaceTargetKey;
import de.schrell.cadas.application.reports.SurfaceMaterialListService.MaterialSummary;
import de.schrell.cadas.application.reports.SurfaceMaterialListService.SurfaceMaterialReport;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectHeatingType;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;
import de.schrell.cadas.domain.model.WindowElement;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SurfaceMaterialListServiceTest {

    private final SurfaceMaterialListService service = new SurfaceMaterialListService();

    @Test
    void listetRaeumeMitHoehenMassenFlaechenUndVolumenAuchOhneBelaege() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        project.primaryLevel().addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4_000, 3_000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));

        SurfaceMaterialReport report = service.create(project);

        assertEquals(1, report.rooms().size());
        assertEquals(12.0, report.rooms().getFirst().areaSquareMeters(), 0.001);
        assertEquals(14_000.0, report.rooms().getFirst().innerPerimeterMillimeters(), 0.001);
        assertEquals(31.2, report.rooms().getFirst().volumeCubicMeters(), 0.001);
        assertEquals(12.0, report.rooms().getFirst().residentialAreaSquareMeters(), 0.001);
        assertTrue(report.toMarkdown().contains("Räume und Mietflächen nach WoFlV"));
        assertTrue(report.toMarkdown().contains("### Erdgeschoss"));
        assertTrue(report.toMarkdown().contains("4,00 × 3,00 m"));
        assertTrue(report.toMarkdown().contains("Innenumfang"));
        assertTrue(report.toMarkdown().contains("14,00 m"));
    }

    @Test
    void listetAuchUnsichtbareEbenenInDerMaterialliste() {
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
        SurfaceLayer hiddenLayer = layer("Verdecktes Parkett", Length.of(120, LengthUnit.CENTIMETER), Length.of(20, LengthUnit.CENTIMETER));
        stack.addLayer(hiddenLayer);
        stack.setVisibility(hiddenLayer.id(), false);
        project.primaryLevel().addSurfaceLayerStack(stack);

        SurfaceMaterialReport report = service.create(project);

        assertEquals(1, report.materials().size());
        assertEquals("Verdecktes Parkett", report.materials().getFirst().name());
        assertTrue(report.toMarkdown().contains("Verdecktes Parkett"));
    }

    @Test
    void blendetHeizplanSvgNurImExportEin() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        project.primaryLevel().addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(2_000, 0)),
                Length.ofMillimeters(180),
                Length.ofMillimeters(2_600)
        ));
        Room room = Room.rectangular(
                "Bad",
                new PlanPoint(0, 0),
                new PlanPoint(2_000, 1_500),
                Length.ofMillimeters(2_600),
                Length.ofMillimeters(180),
                Length.ofMillimeters(200)
        );
        project.primaryLevel().addRoom(room);
        HydronicHeating heating = HydronicHeating.create(
                        room.id(),
                        HeatingSurfacePosition.FLOOR,
                        HeatingLayoutPattern.MEANDER,
                        Length.ofMillimeters(100),
                        Length.ofMillimeters(11.6),
                        Length.ofMillimeters(50_000),
                        Length.ofMillimeters(100),
                        new PlanPoint(0, 0),
                        new PlanPoint(50, 0)
                )
                .withZones(List.of(HeatingZone.create("FBH 1", List.of(
                        new PlanPoint(100, 100),
                        new PlanPoint(1_900, 100),
                        new PlanPoint(1_900, 1_400),
                        new PlanPoint(100, 1_400)
                ), HeatingLayoutPattern.SPIRAL).withHeatOutputWattsPerSquareMeter(45.0)));
        project.primaryLevel().addHydronicHeating(heating);
        project.primaryLevel().addHeatingExclusionArea(HeatingExclusionArea.create(
                room.id(),
                "Wanne",
                new PlanPoint(800, 500),
                new PlanPoint(1_200, 900)
        ));
        project.primaryLevel().addRoomObject(RoomObject.create(
                "konvektor",
                "Konvektor",
                RoomObjectType.CUBOID,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(1_500, 750),
                Length.of(120, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                false,
                ""
        ).withHeatOutputWatts(900.0));

        SurfaceMaterialReport report = service.create(project);

        assertEquals(2, report.heatingPlans().size());
        assertEquals(1, report.heatingElements().size());
        assertEquals("FBH 1", report.heatingPlans().getFirst().zoneName());
        assertEquals("Schnecke", report.heatingPlans().getFirst().layoutPattern());
        assertEquals(45.0, report.heatingPlans().getFirst().heatOutputWattsPerSquareMeter(), 0.001);
        assertEquals(105.3, report.heatingPlans().getFirst().heatOutputWatts(), 0.1);
        assertTrue(report.heatingPlans().stream().anyMatch(plan -> plan.objectBased() && plan.surfacePosition().equals("Heizelement")));
        assertEquals(900.0, report.heatingElements().getFirst().heatOutputWatts(), 0.001);
        assertEquals(105.3, report.rooms().getFirst().surfaceHeatingWatts(), 0.1);
        assertEquals(900.0, report.rooms().getFirst().heatingElementWatts(), 0.001);
        assertEquals(1_005.3, report.rooms().getFirst().totalHeatOutputWatts(), 0.1);
        assertTrue(report.heatingPlans().getFirst().svg().contains("<svg"));
        assertTrue(report.heatingPlans().getFirst().svg().contains("id=\"waende\""));
        assertTrue(report.heatingPlans().getFirst().svg().contains("id=\"sperrflaechen\""));
        assertTrue(report.heatingPlans().getFirst().svg().contains("fill=\"#f8dcd8\""));
        assertTrue(report.heatingPlans().getFirst().svg().contains("V1"));
        String exportMarkdown = report.toMarkdown();
        String displayMarkdown = report.toDisplayMarkdown();
        assertTrue(exportMarkdown.contains("## Flächenheizungen"));
        assertTrue(exportMarkdown.contains("## Heizelemente"));
        assertTrue(exportMarkdown.contains("| Raum | Objekt | Heizart | Leistung | Raum FBH | Raum DH | Raum Fläche | Raum Heizelemente | Raum gesamt |"));
        assertTrue(exportMarkdown.contains("### Heizplan Erdgeschoss / Bad / Fußboden"));
        assertTrue(exportMarkdown.contains("105 W"));
        assertTrue(exportMarkdown.contains("Konvektor"));
        assertTrue(exportMarkdown.contains("Heizelement"));
        assertTrue(exportMarkdown.contains("1005 W"));
        assertTrue(exportMarkdown.contains("<svg"));
        assertTrue(displayMarkdown.contains("## Flächenheizungen"));
        assertTrue(displayMarkdown.contains("105 W"));
        assertTrue(displayMarkdown.contains("1005 W"));
        assertFalse(displayMarkdown.contains("<svg"));
        assertFalse(displayMarkdown.contains("### Heizplan Erdgeschoss / Bad / Fußboden"));
    }

    @Test
    void listetAlsDeckenheizungMarkiertesObjektAuchBeiFlächenheizungen() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room room = Room.rectangular(
                "Bad",
                new PlanPoint(0, 0),
                new PlanPoint(2_000, 1_500),
                Length.ofMillimeters(2_600),
                Length.ofMillimeters(180),
                Length.ofMillimeters(200)
        );
        project.primaryLevel().addRoom(room);
        project.primaryLevel().addRoomObject(RoomObject.create(
                "deckensegel",
                "Deckensegel",
                RoomObjectType.CUBOID,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(1_000, 750),
                Length.of(120, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(10, LengthUnit.CENTIMETER),
                false,
                ""
        ).withHeatingType(RoomObjectHeatingType.CEILING_HEATING).withHeatOutputWatts(700.0));

        SurfaceMaterialReport report = service.create(project);

        assertEquals(1, report.heatingPlans().size());
        assertEquals(1, report.heatingElements().size());
        assertTrue(report.heatingPlans().getFirst().objectBased());
        assertEquals("Decke", report.heatingPlans().getFirst().surfacePosition());
        assertEquals("Raumobjekt", report.heatingPlans().getFirst().layoutPattern());
        assertEquals("Deckensegel", report.heatingPlans().getFirst().zoneName());
        assertEquals(700.0, report.heatingPlans().getFirst().heatOutputWatts(), 0.001);
        assertEquals(700.0, report.rooms().getFirst().ceilingHeatingWatts(), 0.001);
        assertEquals(700.0, report.rooms().getFirst().surfaceHeatingWatts(), 0.001);
        assertTrue(report.toMarkdown().contains("Deckensegel"));
        assertTrue(report.toMarkdown().contains("Raumobjekt"));
    }

    @Test
    void berechnetRechteckigenBodenOhneSchnitteAlsEinfacheVerlegung() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room room = Room.rectangular(
                "Bad",
                new PlanPoint(0, 0),
                new PlanPoint(1000, 1000),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        project.primaryLevel().addRoom(room);
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        stack.addLayer(layer("Fliese", Length.of(50, LengthUnit.CENTIMETER), Length.of(50, LengthUnit.CENTIMETER)));
        project.primaryLevel().addSurfaceLayerStack(stack);

        SurfaceMaterialReport report = service.create(project);

        MaterialSummary material = report.materials().getFirst();
        assertEquals(1.0, material.coveredAreaSquareMeters(), 0.001);
        assertEquals(4, material.requiredPieces());
        assertEquals(1.0, material.requiredMaterialAreaSquareMeters(), 0.001);
        assertEquals(0, material.cutCount());
        assertEquals(0.0, report.roomComplexities().getFirst().complexityScore(), 0.001);
        assertTrue(report.toMarkdown().contains("Komplexität pro Raum und Fläche"));
    }

    @Test
    void spartBodenöffnungInBodenbelagUndDeckenbelagDarunterAus() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room lowerRoom = Room.rectangular(
                "Unten", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
        );
        project.primaryLevel().addRoom(lowerRoom);
        SurfaceLayerStack ceilingStack = new SurfaceLayerStack(SurfaceType.CEILING, lowerRoom.id().toString());
        ceilingStack.addLayer(layer("Decke", Length.ofMillimeters(500), Length.ofMillimeters(500)));
        project.primaryLevel().addSurfaceLayerStack(ceilingStack);
        var upperLevel = project.createLevel("Obergeschoss");
        Room upperRoom = Room.rectangular(
                "Oben", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
        );
        upperLevel.addRoom(upperRoom);
        upperLevel.addFloorOpening(FloorOpening.create(
                upperRoom.id(), FloorOpeningShape.RECTANGLE, new PlanPoint(2_000, 2_000),
                Length.ofMillimeters(1_000), Length.ofMillimeters(2_000)
        ));
        SurfaceLayerStack floorStack = new SurfaceLayerStack(SurfaceType.FLOOR, upperRoom.id().toString());
        floorStack.addLayer(layer("Boden", Length.ofMillimeters(500), Length.ofMillimeters(500)));
        upperLevel.addSurfaceLayerStack(floorStack);

        SurfaceMaterialReport report = service.create(project);

        assertEquals(14.0, report.materials().stream()
                .filter(material -> material.name().equals("Boden"))
                .findFirst().orElseThrow().coveredAreaSquareMeters(), 0.001);
        assertEquals(14.0, report.materials().stream()
                .filter(material -> material.name().equals("Decke"))
                .findFirst().orElseThrow().coveredAreaSquareMeters(), 0.001);
    }

    @Test
    void bewertetKurzeRestkantenAlsKomplexer() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room room = Room.rectangular(
                "Küche",
                new PlanPoint(0, 0),
                new PlanPoint(1100, 1000),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        project.primaryLevel().addRoom(room);
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        stack.addLayer(layer("Fliese", Length.of(50, LengthUnit.CENTIMETER), Length.of(50, LengthUnit.CENTIMETER)));
        project.primaryLevel().addSurfaceLayerStack(stack);

        SurfaceMaterialReport report = service.create(project);

        MaterialSummary material = report.materials().getFirst();
        assertEquals(1.1, material.coveredAreaSquareMeters(), 0.001);
        assertEquals(5, material.requiredPieces());
        assertEquals(1.25, material.requiredMaterialAreaSquareMeters(), 0.001);
        assertEquals(2, material.cutCount());
        assertTrue(report.roomComplexities().getFirst().complexityScore() > 10.0);
    }

    @Test
    void führtBodenbelagInLFormMitDurchgehendemRasterÜberTeilrechteckeFort() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room room = new Room(
                UUID.randomUUID(),
                "Flur",
                List.of(
                        new PlanPoint(0, 0),
                        new PlanPoint(3_000, 0),
                        new PlanPoint(3_000, 1_000),
                        new PlanPoint(1_000, 1_000),
                        new PlanPoint(1_000, 3_000),
                        new PlanPoint(0, 3_000)
                ),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER),
                null
        );
        project.primaryLevel().addRoom(room);
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        stack.addLayer(layer("Vario", Length.ofMillimeters(1_000), Length.ofMillimeters(600)));
        project.primaryLevel().addSurfaceLayerStack(stack);

        SurfaceMaterialReport report = service.create(project);

        MaterialSummary material = report.materials().getFirst();
        assertEquals(5.0, material.coveredAreaSquareMeters(), 0.001);
        assertEquals(9, material.requiredPieces());
        assertEquals(2, material.cutCount());
    }

    @Test
    void verwendetReststueckeFuerWeitereZuschnitte() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room room = Room.rectangular(
                "Flur",
                new PlanPoint(0, 0),
                new PlanPoint(600, 1000),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        project.primaryLevel().addRoom(room);
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        stack.addLayer(layer("Fliese", Length.of(50, LengthUnit.CENTIMETER), Length.of(50, LengthUnit.CENTIMETER)));
        project.primaryLevel().addSurfaceLayerStack(stack);

        SurfaceMaterialReport report = service.create(project);

        MaterialSummary material = report.materials().getFirst();
        assertEquals(0.6, material.coveredAreaSquareMeters(), 0.001);
        assertEquals(3, material.requiredPieces());
        assertEquals(0.75, material.requiredMaterialAreaSquareMeters(), 0.001);
        assertEquals(2, material.fullPieces());
        assertEquals(2, material.cutPieces());
        assertEquals(2, material.cutCount());
    }

    @Test
    void ignoriertReststueckeUnterZweiZentimetern() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room room = Room.rectangular(
                "Bad",
                new PlanPoint(0, 0),
                new PlanPoint(500, 490),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        project.primaryLevel().addRoom(room);
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        stack.addLayer(layer("Fliese", Length.of(50, LengthUnit.CENTIMETER), Length.of(50, LengthUnit.CENTIMETER)));
        project.primaryLevel().addSurfaceLayerStack(stack);

        SurfaceMaterialReport report = service.create(project);

        MaterialSummary material = report.materials().getFirst();
        assertEquals(1, material.requiredPieces());
        assertTrue(material.restPieces().isEmpty());
        assertFalse(report.toMarkdown().contains("1,0 cm"));
    }

    @Test
    void verwendetReststueckeUeberRaumgrenzenHinweg() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room flur = Room.rectangular(
                "Flur",
                new PlanPoint(0, 0),
                new PlanPoint(600, 500),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        Room wc = Room.rectangular(
                "WC",
                new PlanPoint(1000, 0),
                new PlanPoint(1600, 500),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        project.primaryLevel().addRoom(flur);
        project.primaryLevel().addRoom(wc);
        SurfaceLayerStack flurStack = new SurfaceLayerStack(SurfaceType.FLOOR, flur.id().toString());
        flurStack.addLayer(layer("Fliese", Length.of(50, LengthUnit.CENTIMETER), Length.of(50, LengthUnit.CENTIMETER)));
        SurfaceLayerStack wcStack = new SurfaceLayerStack(SurfaceType.FLOOR, wc.id().toString());
        wcStack.addLayer(layer("Fliese", Length.of(50, LengthUnit.CENTIMETER), Length.of(50, LengthUnit.CENTIMETER)));
        project.primaryLevel().addSurfaceLayerStack(flurStack);
        project.primaryLevel().addSurfaceLayerStack(wcStack);

        SurfaceMaterialReport report = service.create(project);

        MaterialSummary material = report.materials().getFirst();
        assertEquals(0.6, material.coveredAreaSquareMeters(), 0.001);
        assertEquals(3, material.requiredPieces());
        assertEquals(0.75, material.requiredMaterialAreaSquareMeters(), 0.001);
        assertEquals(2, material.roomEntries().stream().mapToInt(entry -> entry.fullPieces()).sum());
        assertEquals(2, material.roomEntries().stream().mapToInt(entry -> entry.cutPieces()).sum());
    }

    @Test
    void nutztReststueckeGedrehtNurBeiPassenderSchnittbeschraenkung() {
        ProjectModel drehbaresProjekt = projektMitZweiZuschnittRaeumen(SurfaceCutRestriction.FREE);
        ProjectModel gerichtetesProjekt = projektMitZweiZuschnittRaeumen(SurfaceCutRestriction.LAY_DIRECTION_OUTER_CUTS);

        MaterialSummary drehbaresMaterial = service.create(drehbaresProjekt).materials().getFirst();
        MaterialSummary gerichtetesMaterial = service.create(gerichtetesProjekt).materials().getFirst();

        assertEquals(1, drehbaresMaterial.requiredPieces());
        assertEquals(2, gerichtetesMaterial.requiredPieces());
        assertTrue(drehbaresMaterial.values().contains("frei"));
        assertTrue(gerichtetesMaterial.values().contains("Verlegerichtung"));
    }

    @Test
    void gruppiertMaterialbedarfUnabhaengigVonDrehungUndRichtung() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room links = Room.rectangular(
                "Links",
                new PlanPoint(0, 0),
                new PlanPoint(1000, 600),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        Room rechts = Room.rectangular(
                "Rechts",
                new PlanPoint(1200, 0),
                new PlanPoint(2200, 600),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        project.primaryLevel().addRoom(links);
        project.primaryLevel().addRoom(rechts);
        SurfaceLayerStack linksStack = new SurfaceLayerStack(SurfaceType.FLOOR, links.id().toString());
        linksStack.addLayer(layer("Diele", Length.of(100, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER)));
        SurfaceLayerStack rechtsStack = new SurfaceLayerStack(SurfaceType.FLOOR, rechts.id().toString());
        rechtsStack.addLayer(layer("Diele", Length.of(100, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER))
                .withLayoutOrientation(
                        de.schrell.cadas.domain.model.SurfaceLayoutRotation.DEGREES_90,
                        de.schrell.cadas.domain.model.SurfaceLayoutDirection.RIGHT_TO_LEFT
                ));
        project.primaryLevel().addSurfaceLayerStack(linksStack);
        project.primaryLevel().addSurfaceLayerStack(rechtsStack);

        SurfaceMaterialReport report = service.create(project);

        MaterialSummary material = report.materials().getFirst();
        assertEquals(1, report.materials().size());
        assertEquals(3, material.requiredPieces());
        assertFalse(material.values().contains("Drehung"));
        assertFalse(material.values().contains("Richtung"));
    }

    @Test
    void ordnetZusammenfassungNachNameUndListetTrennmerkmaleVorHeizungen() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room a = Room.rectangular(
                "A",
                new PlanPoint(0, 0),
                new PlanPoint(1000, 1000),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        Room b = Room.rectangular(
                "B",
                new PlanPoint(1200, 0),
                new PlanPoint(2200, 1000),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        Room c = Room.rectangular(
                "C",
                new PlanPoint(2400, 0),
                new PlanPoint(3400, 1000),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        project.primaryLevel().addRoom(a);
        project.primaryLevel().addRoom(b);
        project.primaryLevel().addRoom(c);
        SurfaceLayerStack aStack = new SurfaceLayerStack(SurfaceType.FLOOR, a.id().toString());
        aStack.addLayer(layer("Zeta", Length.of(50, LengthUnit.CENTIMETER), Length.of(50, LengthUnit.CENTIMETER)));
        SurfaceLayerStack bStack = new SurfaceLayerStack(SurfaceType.FLOOR, b.id().toString());
        bStack.addLayer(layer("Alpha", Length.of(50, LengthUnit.CENTIMETER), Length.of(50, LengthUnit.CENTIMETER)));
        SurfaceLayerStack cStack = new SurfaceLayerStack(SurfaceType.FLOOR, c.id().toString());
        cStack.addLayer(SurfaceLayer.create(
                "Alpha",
                Length.of(12, LengthUnit.MILLIMETER),
                Length.of(50, LengthUnit.CENTIMETER),
                Length.of(50, LengthUnit.CENTIMETER),
                SurfaceLayoutMode.NONE,
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.of(2, LengthUnit.MILLIMETER),
                ""
        ));
        project.primaryLevel().addSurfaceLayerStack(aStack);
        project.primaryLevel().addSurfaceLayerStack(bStack);
        project.primaryLevel().addSurfaceLayerStack(cStack);

        String markdown = service.create(project).toMarkdown();
        int zusammenfassung = markdown.indexOf("## Zusammenfassung");
        int heizungen = markdown.indexOf("## Flächenheizungen");
        int alpha = markdown.indexOf("| Alpha |");
        int zeta = markdown.indexOf("| Zeta |");

        assertTrue(zusammenfassung >= 0 && heizungen > zusammenfassung);
        assertTrue(alpha >= 0 && zeta > alpha);
        assertTrue(markdown.contains("### Trennmerkmale bei mehrfachen Materialnamen"));
        assertTrue(markdown.contains("* Alpha:"));
        assertTrue(markdown.contains("Dicke"));
        assertTrue(markdown.contains("10 mm"));
        assertTrue(markdown.contains("12 mm"));
    }

    @Test
    void spartBodenbelagUnterBodenaussparendenObjektenAus() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room room = Room.rectangular(
                "Abstellraum",
                new PlanPoint(0, 0),
                new PlanPoint(1000, 1000),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        project.primaryLevel().addRoom(room);
        project.primaryLevel().addRoomObject(RoomObject.create(
                "wall-cabinet",
                "Wandschrank",
                RoomObjectType.WALL_CABINET,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(250, 500),
                Length.of(50, LengthUnit.CENTIMETER),
                Length.of(100, LengthUnit.CENTIMETER),
                Length.of(200, LengthUnit.CENTIMETER),
                true,
                ""
        ));
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        stack.addLayer(layer("Fliese", Length.of(50, LengthUnit.CENTIMETER), Length.of(50, LengthUnit.CENTIMETER)));
        project.primaryLevel().addSurfaceLayerStack(stack);

        SurfaceMaterialReport report = service.create(project);

        MaterialSummary material = report.materials().getFirst();
        assertEquals(0.5, material.coveredAreaSquareMeters(), 0.001);
        assertEquals(2, material.requiredPieces());
    }

    @Test
    void ziehtFensteroeffnungVonInnenwandMaterialAb() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(2000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2, LengthUnit.METER)
        );
        Room room = Room.rectangular(
                "Zimmer",
                new PlanPoint(0, 100),
                new PlanPoint(2000, 2100),
                Length.of(2, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        project.primaryLevel().addWall(wall);
        project.primaryLevel().addRoom(room);
        project.primaryLevel().addWindow(WindowElement.create(
                wall.id(),
                Length.of(50, LengthUnit.CENTIMETER),
                Length.of(100, LengthUnit.CENTIMETER),
                Length.of(50, LengthUnit.CENTIMETER),
                Length.of(100, LengthUnit.CENTIMETER)
        ));
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, WallSurfaceTargetKey.interior(wall.id(), room.id()));
        stack.addLayer(layer("Wandfliese", Length.of(100, LengthUnit.CENTIMETER), Length.of(100, LengthUnit.CENTIMETER)));
        project.primaryLevel().addSurfaceLayerStack(stack);

        SurfaceMaterialReport report = service.create(project);

        MaterialSummary material = report.materials().getFirst();
        assertEquals(3.0, material.coveredAreaSquareMeters(), 0.001);
        assertEquals(3, material.requiredPieces());
        assertTrue(material.cutCount() > 0);
        assertTrue(material.roomEntries().getFirst().surfaceDescription().contains("Innenwand"));
        assertTrue(report.toMarkdown().contains("Reststücke"));
    }

    @Test
    void trenntSockelUndSchraegeEinerInnenwandInMaterialUndRaumliste() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Dachgeschoss");
        Wall wall = new Wall(
                UUID.randomUUID(),
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.ofMillimeters(2000),
                Length.ofMillimeters(1000),
                Length.ofMillimeters(2000),
                List.of(
                        new WallProfilePoint(Length.zero(), Length.ofMillimeters(1000)),
                        new WallProfilePoint(Length.ofMillimeters(4000), Length.ofMillimeters(2000))
                )
        );
        Room room = Room.rectangular(
                "Studio",
                new PlanPoint(0, 100),
                new PlanPoint(4000, 2100),
                Length.ofMillimeters(2400),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        project.primaryLevel().addWall(wall);
        project.primaryLevel().addRoom(room);
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, WallSurfaceTargetKey.interior(wall.id(), room.id()));
        stack.addLayer(layer("Lehmputz", Length.of(100, LengthUnit.CENTIMETER), Length.of(100, LengthUnit.CENTIMETER)));
        project.primaryLevel().addSurfaceLayerStack(stack);

        SurfaceMaterialReport report = service.create(project);

        MaterialSummary material = report.materials().getFirst();
        assertEquals(6.0, material.coveredAreaSquareMeters(), 0.001);
        assertTrue(material.roomEntries().stream().anyMatch(entry ->
                entry.surfaceDescription().contains("Sockel") && Math.abs(entry.coveredAreaSquareMeters() - 4.0) < 0.001));
        assertTrue(material.roomEntries().stream().anyMatch(entry ->
                entry.surfaceDescription().contains("Schräge") && Math.abs(entry.coveredAreaSquareMeters() - 2.0) < 0.001));
        assertTrue(report.roomComplexities().stream().anyMatch(entry -> entry.surfaceDescription().contains("Sockel")));
        assertTrue(report.roomComplexities().stream().anyMatch(entry -> entry.surfaceDescription().contains("Schräge")));
        assertTrue(report.toMarkdown().contains("Sockel"));
        assertTrue(report.toMarkdown().contains("Schräge"));
    }

    private ProjectModel projektMitZweiZuschnittRaeumen(SurfaceCutRestriction cutRestriction) {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room links = Room.rectangular(
                "Links",
                new PlanPoint(0, 0),
                new PlanPoint(300, 200),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        Room rechts = Room.rectangular(
                "Rechts",
                new PlanPoint(500, 0),
                new PlanPoint(800, 200),
                Length.of(2.5, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        project.primaryLevel().addRoom(links);
        project.primaryLevel().addRoom(rechts);
        SurfaceLayerStack linksStack = new SurfaceLayerStack(SurfaceType.FLOOR, links.id().toString());
        linksStack.addLayer(layer("Platte", Length.of(50, LengthUnit.CENTIMETER), Length.of(30, LengthUnit.CENTIMETER), cutRestriction));
        SurfaceLayerStack rechtsStack = new SurfaceLayerStack(SurfaceType.FLOOR, rechts.id().toString());
        rechtsStack.addLayer(layer("Platte", Length.of(50, LengthUnit.CENTIMETER), Length.of(30, LengthUnit.CENTIMETER), cutRestriction));
        project.primaryLevel().addSurfaceLayerStack(linksStack);
        project.primaryLevel().addSurfaceLayerStack(rechtsStack);
        return project;
    }

    private SurfaceLayer layer(String name, Length tileWidth, Length tileHeight) {
        return layer(name, tileWidth, tileHeight, SurfaceCutRestriction.fallback());
    }

    private SurfaceLayer layer(String name, Length tileWidth, Length tileHeight, SurfaceCutRestriction cutRestriction) {
        return SurfaceLayer.create(
                name,
                Length.of(10, LengthUnit.MILLIMETER),
                tileWidth,
                tileHeight,
                SurfaceLayoutMode.NONE,
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.of(2, LengthUnit.MILLIMETER),
                cutRestriction,
                ""
        );
    }
}
