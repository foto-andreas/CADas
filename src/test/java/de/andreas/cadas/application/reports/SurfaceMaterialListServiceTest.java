package de.andreas.cadas.application.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.application.layers.WallSurfaceTargetKey;
import de.andreas.cadas.application.reports.SurfaceMaterialListService.MaterialSummary;
import de.andreas.cadas.application.reports.SurfaceMaterialListService.SurfaceMaterialReport;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SurfaceCutRestriction;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceLayoutMode;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import org.junit.jupiter.api.Test;

class SurfaceMaterialListServiceTest {

    private final SurfaceMaterialListService service = new SurfaceMaterialListService();

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
        assertTrue(report.toMarkdown().contains("Komplexität pro Raum"));
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
