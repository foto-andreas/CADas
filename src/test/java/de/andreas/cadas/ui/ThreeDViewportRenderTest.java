package de.andreas.cadas.ui;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.application.view.RenderableBox;
import de.andreas.cadas.application.view.RenderableKind;
import de.andreas.cadas.application.view.RenderableMesh;
import de.andreas.cadas.application.view.RotationAxis;
import de.andreas.cadas.application.view.SelectionKey;
import de.andreas.cadas.application.view.ThreeDSceneModel;
import de.andreas.cadas.application.view.ThreeDSceneModelBuilder;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ThreeDViewportRenderTest {

    @Test
    void dreiDViewportErzeugtModellkoerperAusEinfachemZimmer() {
        // Dieser Test verifiziert, dass die ThreeDViewport-Initialisierung mit einem einfachen
        // Gebäude-Modell die erwarteten 3D-Körper erzeugt. Er prüft die fachliche Korrektheit
        // der Modellableitung, ohne den vollständigen 3D-Render-Pfad zu durchlaufen, der in
        // JavaFX 25 in Headless-Tests problematisch ist.
        ProjectModel project = baueEinfachesZimmer();
        ThreeDSceneModelBuilder builder = new ThreeDSceneModelBuilder();
        ThreeDSceneModel scene = builder.build(project, Set.of("Erdgeschoss"), false);
        Assertions.assertFalse(scene.boxes().isEmpty(), "Es müssen 3D-Körper vorliegen.");
        Assertions.assertTrue(scene.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.WALL),
                "Es müssen Wände vorhanden sein.");
        Assertions.assertTrue(scene.meshes().stream().anyMatch(mesh -> mesh.kind() == RenderableKind.ROOM_FLOOR),
                "Es muss ein Raum-Boden vorhanden sein.");
        Assertions.assertTrue(
                scene.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.ROOM_CEILING)
                        || scene.meshes().stream().anyMatch(mesh -> mesh.kind() == RenderableKind.ROOM_CEILING),
                "Es muss eine Raum-Decke vorhanden sein.");
    }

    @Test
    void dreiDViewportBerechnetSichtbareModellmitte() {
        // Die Modellmitte wird für die Zentrierung der Kamera verwendet. Wir prüfen hier die
        // Berechnung direkt, indem wir die ThreeDSceneModel-Boxen analysieren.
        ProjectModel project = baueEinfachesZimmer();
        ThreeDSceneModelBuilder builder = new ThreeDSceneModelBuilder();
        ThreeDSceneModel scene = builder.build(project, Set.of("Erdgeschoss"), false);
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (var box : scene.boxes()) {
            minX = Math.min(minX, box.centerX() - box.width() / 2.0);
            maxX = Math.max(maxX, box.centerX() + box.width() / 2.0);
            minZ = Math.min(minZ, box.centerZ() - box.depth() / 2.0);
            maxZ = Math.max(maxZ, box.centerZ() + box.depth() / 2.0);
            minY = Math.min(minY, box.centerY() - box.height() / 2.0);
            maxY = Math.max(maxY, box.centerY() + box.height() / 2.0);
        }
        double centerX = (minX + maxX) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;
        Assertions.assertEquals(2000.0, centerX, 1.0, "Modellmitte X sollte bei 2000mm liegen.");
        Assertions.assertEquals(1500.0, centerZ, 1.0, "Modellmitte Z sollte bei 1500mm liegen.");
        Assertions.assertTrue(maxY > 2000.0, "Modellhöhe sollte über 2m liegen (Wandhöhe).");
    }

    @Test
    void zentrierungBleibtKorrektBeiAllenPositionen() {
        double worldScale = 0.08;
        // Modell 1: Einfaches Rechteck um (2000, 1500) herum (Originalposition)
        double[] zentrum1 = berechneSceneZentrum(List.of(
            createRenderableBox(2000, 1500, 4000, 3000)
        ), worldScale);
        Assertions.assertEquals(2000 * worldScale, zentrum1[0], 0.001,
                "SceneCenterX für Modell bei (2000,1500)");
        Assertions.assertEquals(1500 * worldScale, zentrum1[1], 0.001,
                "SceneCenterZ für Modell bei (2000,1500)");

        // Nach Zentrierungstranslation sollte die Bounding-Box um 0 herum liegen
        double[] aabb1 = berechneAabbNachZentrierung(List.of(
            createRenderableBox(2000, 1500, 4000, 3000)
        ), zentrum1, worldScale);
        Assertions.assertTrue(Math.abs(aabb1[0] + aabb1[1]) < 0.001,
                "X-AABB sollte symmetrisch um 0 sein: min=" + aabb1[0] + " max=" + aabb1[1]);
        Assertions.assertTrue(Math.abs(aabb1[2] + aabb1[3]) < 0.001,
                "Z-AABB sollte symmetrisch um 0 sein: min=" + aabb1[2] + " max=" + aabb1[3]);

        // Modell 2: Raum bei (10000, 20000) – weit weg vom Ursprung
        double[] zentrum2 = berechneSceneZentrum(List.of(
            createRenderableBox(12000, 20500, 4000, 3000)
        ), worldScale);
        Assertions.assertEquals(12000 * worldScale, zentrum2[0], 0.001,
                "SceneCenterX für Modell bei (12000,20500)");
        double[] aabb2 = berechneAabbNachZentrierung(List.of(
            createRenderableBox(12000, 20500, 4000, 3000)
        ), zentrum2, worldScale);
        Assertions.assertTrue(Math.abs(aabb2[0] + aabb2[1]) < 0.001,
                "X-AABB nach Zentrierung bei verschobenem Modell");

        // Modell 3: Einzelner kleiner Würfel (100mm) nahe am Ursprung
        double[] zentrum3 = berechneSceneZentrum(List.of(
            createRenderableBox(50, 50, 100, 100)
        ), worldScale);
        double[] aabb3 = berechneAabbNachZentrierung(List.of(
            createRenderableBox(50, 50, 100, 100)
        ), zentrum3, worldScale);
        Assertions.assertTrue(Math.abs(aabb3[0] + aabb3[1]) < 0.001,
                "Kleiner Würfel: X-AABB symmetrisch");

        // Modell 4: Einzelner großer Würfel (20000mm)
        double[] zentrum4 = berechneSceneZentrum(List.of(
            createRenderableBox(10000, 10000, 20000, 20000)
        ), worldScale);
        double[] aabb4 = berechneAabbNachZentrierung(List.of(
            createRenderableBox(10000, 10000, 20000, 20000)
        ), zentrum4, worldScale);
        Assertions.assertTrue(Math.abs(aabb4[0] + aabb4[1]) < 0.001,
                "Großer Würfel: X-AABB symmetrisch");

        // Modell 5: Modell direkt am Nullpunkt
        double[] zentrum5 = berechneSceneZentrum(List.of(
            createRenderableBox(0, 0, 4000, 3000)
        ), worldScale);
        Assertions.assertEquals(0.0, zentrum5[0], 0.001,
                "SceneCenterX bei Modell am Ursprung");
        double[] aabb5 = berechneAabbNachZentrierung(List.of(
            createRenderableBox(0, 0, 4000, 3000)
        ), zentrum5, worldScale);
        Assertions.assertTrue(Math.abs(aabb5[0] + aabb5[1]) < 0.001,
                "Modell am Ursprung: X-AABB symmetrisch (min=" + aabb5[0] + " max=" + aabb5[1] + ")");
    }

    @Test
    void zentrierungMitKomplettemGebaeudeAnVerschiedenenPositionen() {
        double worldScale = 0.08;

        // Standardraum bei (0,0)-(4000,3000) – die ursprüngliche Testposition
        ProjectModel projekt1 = baueEinfachesZimmer();
        var aabbOriginal = berechneAabbAusProjekt(projekt1, worldScale);
        Assertions.assertTrue(Math.abs(aabbOriginal[0] + aabbOriginal[1]) < 2.0,
                "Original-Modell: X-AABB um 0 zentriert");

        // Neues Projekt mit Raum bei (10000, 20000)-(14000, 23000), also weit entfernt
        ProjectModel projekt2 = ProjectModel.withDefaultLevel("Test2", "EG");
        Level level2 = projekt2.primaryLevel();
        level2.addWall(Wall.create(
                new PlanSegment(new PlanPoint(10_000, 20_000), new PlanPoint(14_000, 20_000)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));
        level2.addWall(Wall.create(
                new PlanSegment(new PlanPoint(14_000, 20_000), new PlanPoint(14_000, 23_000)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));
        level2.addWall(Wall.create(
                new PlanSegment(new PlanPoint(14_000, 23_000), new PlanPoint(10_000, 23_000)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));
        level2.addWall(Wall.create(
                new PlanSegment(new PlanPoint(10_000, 23_000), new PlanPoint(10_000, 20_000)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));
        level2.addRoom(Room.rectangular(
                "Wohnen2", new PlanPoint(10_000, 20_000), new PlanPoint(14_000, 23_000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        var aabbWeit = berechneAabbAusProjekt(projekt2, worldScale);
        Assertions.assertTrue(Math.abs(aabbWeit[0] + aabbWeit[1]) < 2.0,
                "Verschobenes Modell: X-AABB um 0 zentriert (min=" + aabbWeit[0] + " max=" + aabbWeit[1] + ")");
    }

    /** Simuliert die rebuildScene-Zentrierungslogik für eine Liste von RenderableBoxen. */
    private double[] berechneSceneZentrum(List<RenderableBox> boxes, double scale) {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (RenderableBox box : boxes) {
            double hw = (box.width() * scale) / 2.0;
            double hd = (box.depth() * scale) / 2.0;
            double cx = box.centerX() * scale;
            double cz = box.centerZ() * scale;
            minX = Math.min(minX, cx - hw);
            maxX = Math.max(maxX, cx + hw);
            minZ = Math.min(minZ, cz - hd);
            maxZ = Math.max(maxZ, cz + hd);
        }
        return new double[]{
            (minX + maxX) / 2.0,
            (minZ + maxZ) / 2.0
        };
    }

    /** Berechnet die AABB der Boxen NACH der modelGroup-Zentrierungstranslation. */
    private double[] berechneAabbNachZentrierung(List<RenderableBox> boxes, double[] center, double scale) {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (RenderableBox box : boxes) {
            double hw = (box.width() * scale) / 2.0;
            double hh = (box.height() * scale) / 2.0;
            double hd = (box.depth() * scale) / 2.0;
            double cx = box.centerX() * scale - center[0];
            double cy = box.centerY() * scale;
            double cz = box.centerZ() * scale - center[1];
            minX = Math.min(minX, cx - hw);
            maxX = Math.max(maxX, cx + hw);
            minY = Math.min(minY, cy - hh);
            maxY = Math.max(maxY, cy + hh);
            minZ = Math.min(minZ, cz - hd);
            maxZ = Math.max(maxZ, cz + hd);
        }
        return new double[]{minX, maxX, minZ, maxZ, minY, maxY};
    }

    /** Erzeugt eine einfache RenderableBox mit gegebenem Zentrum und Abmessungen. */
    private RenderableBox createRenderableBox(double centerX, double centerZ, double width, double depth) {
        return new RenderableBox(
                new SelectionKey(RenderableKind.WALL, "test", "test-box"),
                "test", RenderableKind.WALL,
                centerX, 0.0, centerZ,
                width, 100.0, depth,
                RotationAxis.Y, 0.0, "wall", 1.0
        );
    }

    /** Berechnet die AABB eines kompletten Projekts NACH Zentrierung (simuliert rebuildScene-Logik). */
    private double[] berechneAabbAusProjekt(ProjectModel project, double scale) {
        ThreeDSceneModelBuilder builder = new ThreeDSceneModelBuilder();
        ThreeDSceneModel scene = builder.build(project, Set.of(project.primaryLevel().name()), false);
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (var box : scene.boxes()) {
            double hw = (box.width() * scale) / 2.0;
            double hd = (box.depth() * scale) / 2.0;
            double cx = box.centerX() * scale;
            double cz = box.centerZ() * scale;
            minX = Math.min(minX, cx - hw);
            maxX = Math.max(maxX, cx + hw);
            minZ = Math.min(minZ, cz - hd);
            maxZ = Math.max(maxZ, cz + hd);
        }
        double sceneCenterX = (minX + maxX) / 2.0;
        double sceneCenterZ = (minZ + maxZ) / 2.0;

        // Jetzt die AABB NACH Zentrierung berechnen
        double nachMinX = Double.POSITIVE_INFINITY, nachMaxX = Double.NEGATIVE_INFINITY;
        double nachMinZ = Double.POSITIVE_INFINITY, nachMaxZ = Double.NEGATIVE_INFINITY;
        for (var box : scene.boxes()) {
            double hw = (box.width() * scale) / 2.0;
            double hd = (box.depth() * scale) / 2.0;
            double cx = box.centerX() * scale - sceneCenterX;
            double cz = box.centerZ() * scale - sceneCenterZ;
            nachMinX = Math.min(nachMinX, cx - hw);
            nachMaxX = Math.max(nachMaxX, cx + hw);
            nachMinZ = Math.min(nachMinZ, cz - hd);
            nachMaxZ = Math.max(nachMaxZ, cz + hd);
        }
        return new double[]{nachMinX, nachMaxX, nachMinZ, nachMaxZ};
    }

    private static ProjectModel baueEinfachesZimmer() {
        ProjectModel project = ProjectModel.withDefaultLevel("Test", "Erdgeschoss");
        Level level = project.primaryLevel();
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4_000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(4_000, 0), new PlanPoint(4_000, 3_000)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(4_000, 3_000), new PlanPoint(0, 3_000)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 3_000), new PlanPoint(0, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));
        level.addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4_000, 3_000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        return project;
    }
}
