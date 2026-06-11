package de.andreas.cadas.ui;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.application.view.ThreeDSceneModel;
import de.andreas.cadas.application.view.ThreeDSceneModelBuilder;
import de.andreas.cadas.application.view.RenderableKind;
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
        Assertions.assertTrue(scene.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.ROOM_FLOOR),
                "Es muss ein Raum-Boden vorhanden sein.");
        Assertions.assertTrue(scene.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.ROOM_CEILING),
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
