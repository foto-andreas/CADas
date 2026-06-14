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
        assertEquals(6, material.requiredPieces());
        assertEquals(1.5, material.requiredMaterialAreaSquareMeters(), 0.001);
        assertEquals(2, material.cutCount());
        assertTrue(report.roomComplexities().getFirst().complexityScore() > 10.0);
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
        assertEquals(6, material.requiredPieces());
        assertTrue(material.cutCount() > 0);
        assertTrue(material.roomEntries().getFirst().surfaceDescription().contains("Innenwand"));
    }

    private SurfaceLayer layer(String name, Length tileWidth, Length tileHeight) {
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
                ""
        );
    }
}
