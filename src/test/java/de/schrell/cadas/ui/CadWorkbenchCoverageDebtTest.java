package de.schrell.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;

class CadWorkbenchCoverageDebtTest extends CadWorkbenchTestBase {

    @Test
    void exportiertMateriallisteUndBauzeichnungMitWorkbenchRasterbildern() throws Exception {
        Path directory = Files.createTempDirectory("cadas-rasterberichte-");
        Path materialPdf = directory.resolve("material.pdf");
        Path constructionPdf = directory.resolve("bauzeichnung.pdf");
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instance = new CadWorkbench();
            new Scene(instance, 1_200, 800);
            Level level = instance.project.primaryLevel();
            Room room = Room.rectangular(
                    "Wohnen",
                    new PlanPoint(0, 0),
                    new PlanPoint(4_000, 3_000),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            level.addRoom(room);
            SurfaceLayerStack floor = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
            floor.addLayer(SurfaceLayer.create(
                    "Bodenfliese",
                    Length.ofMillimeters(12),
                    Length.ofMillimeters(600),
                    Length.ofMillimeters(300),
                    Length.ofMillimeters(3)
            ));
            level.addSurfaceLayerStack(floor);
            HeatingZone zone = HeatingZone.create("Heizkreis", room.outline(), HeatingLayoutPattern.MEANDER);
            HydronicHeating heating = HydronicHeating.create(
                    room.id(),
                    HeatingSurfacePosition.FLOOR,
                    HeatingLayoutPattern.MEANDER,
                    Length.ofMillimeters(200),
                    Length.ofMillimeters(16),
                    Length.ofMillimeters(100_000),
                    Length.ofMillimeters(100),
                    new PlanPoint(100, 100),
                    new PlanPoint(300, 100)
            ).withZones(List.of(zone));
            level.addHydronicHeating(heating);
            instance.automationSetErrorDialogsEnabled(false);
            instance.applyCss();
            instance.layout();
            instance.documentSupport.exportSurfaceMaterialReportPdf(materialPdf);
            instance.documentSupport.exportConstructionDrawingPdf(constructionPdf);
            return instance;
        });

        assertTrue(Files.size(materialPdf) > 1_000);
        assertTrue(Files.size(constructionPdf) > 1_000);
        try (var material = Loader.loadPDF(materialPdf.toFile());
             var construction = Loader.loadPDF(constructionPdf.toFile())) {
            assertTrue(material.getNumberOfPages() >= 1);
            assertTrue(construction.getNumberOfPages() >= 1);
        }
        assertEquals("", aufFxThread(workbench::automationLastErrorTitle));
    }

    @Test
    void rendertTreppenWandbelägeDachschrägeUndGeländeInAllenAnsichten() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instance = new CadWorkbench();
            new Scene(instance, 1_200, 800);
            Level level = instance.project.primaryLevel();
            Wall south = addWall(level, 0, 0, 8_000, 0);
            addWall(level, 8_000, 0, 8_000, 6_000);
            addWall(level, 8_000, 6_000, 0, 6_000);
            addWall(level, 0, 6_000, 0, 0);
            level.addDoor(Door.create(
                    south.id(),
                    Length.ofMillimeters(800),
                    Length.ofMillimeters(1_000),
                    Length.ofMillimeters(2_010),
                    Length.zero()
            ));
            level.addWindow(WindowElement.create(
                    south.id(),
                    Length.ofMillimeters(3_000),
                    Length.ofMillimeters(1_400),
                    Length.ofMillimeters(900),
                    Length.ofMillimeters(1_200)
            ));
            Room room = Room.rectangular(
                    "Galerie",
                    new PlanPoint(100, 100),
                    new PlanPoint(7_900, 5_900),
                    Length.ofMillimeters(2_800),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200),
                    new SlopedCeilingProfile(
                            SlopedCeilingSide.WEST,
                            Length.ofMillimeters(900),
                            Length.ofMillimeters(2_000)
                    )
            );
            level.addRoom(room);
            SurfaceLayerStack wallCovering = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, south.id().toString());
            wallCovering.addLayer(SurfaceLayer.create(
                    "Wandfliese",
                    Length.ofMillimeters(10),
                    Length.ofMillimeters(300),
                    Length.ofMillimeters(200),
                    Length.ofMillimeters(2)
            ));
            level.addSurfaceLayerStack(wallCovering);
            level.addStaircase(stair(StairType.STRAIGHT, 500, 800));
            level.addStaircase(stair(StairType.HALF_TURN, 2_300, 800));
            level.addStaircase(stair(StairType.SWITCHBACK, 4_100, 800));
            level.addStaircase(stair(StairType.SPIRAL, 5_900, 800));
            instance.project.defineTerrain(new Terrain(List.of(
                    new TerrainVertex(new PlanPoint(-500, -500), Length.zero()),
                    new TerrainVertex(new PlanPoint(8_500, -500), Length.ofMillimeters(300)),
                    new TerrainVertex(new PlanPoint(8_500, 6_500), Length.ofMillimeters(700)),
                    new TerrainVertex(new PlanPoint(-500, 6_500), Length.ofMillimeters(100))
            ), Length.ofMillimeters(2_000)));
            Level upper = instance.project.createLevel("Obergeschoss");
            addWall(upper, 500, 500, 7_500, 500);
            instance.availableLevels.setAll(instance.project.levels());
            instance.activeLevel.set(level);
            instance.applyCss();
            instance.layout();
            return instance;
        });

        for (ViewOrientation orientation : ViewOrientation.values()) {
            WritableImage image = aufFxThread(() -> {
                workbench.activeView.set(orientation);
                workbench.render();
                return workbench.automationDrawingSnapshot();
            });
            assertTrue(countNonBackgroundPixels(
                    image, 0, 0, (int) image.getWidth() - 1, (int) image.getHeight() - 1
            ) > 100, "Leere Ansicht: " + orientation);
        }
    }

    @Test
    void exportiertUndImportiertEineEtageMitEindeutigenFolgenamen() throws Exception {
        Path levelFile = Files.createTempFile("cadas-etage-rundlauf-", ".cadas");
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instance = new CadWorkbench();
            new Scene(instance, 1_200, 800);
            addWall(instance.project.primaryLevel(), 0, 0, 4_000, 0);
            instance.automationSetErrorDialogsEnabled(false);
            instance.applyCss();
            instance.layout();
            instance.automationInvoke("exportLevelDxf", levelFile);
            return instance;
        });

        assertTrue(Files.size(levelFile) > 100);
        aufFxThread(() -> {
            workbench.automationInvoke("importLevelDxf", levelFile);
            workbench.automationInvoke("importLevelDxf", levelFile);
            return null;
        });

        List<String> levelNames = aufFxThread(() -> workbench.project.levels().stream()
                .map(Level::name)
                .toList());
        assertEquals(3, levelNames.size());
        assertEquals(3, levelNames.stream().map(String::toLowerCase).distinct().count());
        assertEquals(1, aufFxThread(() -> workbench.project.levels().get(1).walls().size()));
        assertEquals(1, aufFxThread(() -> workbench.project.levels().get(2).walls().size()));
    }

    private Wall addWall(Level level, double x1, double y1, double x2, double y2) {
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(x1, y1), new PlanPoint(x2, y2)),
                Length.ofMillimeters(200),
                Length.ofMillimeters(2_800)
        );
        level.addWall(wall);
        return wall;
    }

    private Staircase stair(StairType type, double x, double y) {
        return Staircase.create(
                type,
                new PlanPoint(x, y),
                new PlanPoint(x + 1_200, y + 4_000),
                Length.ofMillimeters(2_800),
                16
        );
    }
}
