package de.andreas.cadas.application.drawing;

import de.andreas.cadas.application.exchange.ProjectExchangeService;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.infrastructure.dxf.DxfProjectExchangeService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifiziert anhand der realen Beispieldatei P28_2.dxf, dass alle Bemaßungen
 * außerhalb des Gebäudes liegen, keine Maßlinien aufeinanderfallen und identische
 * Maße nicht doppelt auftreten. Der gleiche Algorithmus gilt für 2D und PDF.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class P28DimensionPlacementTest {

    private ProjectModel project;
    private Level level;
    private final WallDimensionService wallDimensionService = new WallDimensionService();
    private final WallDimensionPlacementService placementService = new WallDimensionPlacementService();

    @BeforeAll
    void ladeBeispieldatei() throws Exception {
        ProjectExchangeService exchange = new DxfProjectExchangeService();
        project = exchange.importProject(Path.of("src/test/resources/P28_2.dxf"), "P28_2");
        level = project.primaryLevel();
    }

    @Test
    void alleBemaßungenLiesenAusserhalbDesGebaeudes() {
        List<PlanPoint> gebaeudePunkte = sammleGebaeudePunkte();
        int verletzungen = 0;
        for (Wall wall : level.walls()) {
            WallDimensionService.WallDimensions dims = wallDimensionService.dimensions(level, wall);
            List<WallDimensionPlacementService.PlacedDimension> placements = placementService.place(
                    level, wall, dims, 1.0, 30.0, 16.0);
            for (WallDimensionPlacementService.PlacedDimension p : placements) {
                // Die Maßlinie wird an den Enden des referenzierten Segments in Normalenrichtung
                // um |normalOffset| verschoben. Prüfe, ob beide Enden außerhalb der Gebäudehülle liegen.
                PlanSegment seg = p.dimension() != null ? p.dimension().dimensionSegment() : wall.axis();
                PlanPoint linieStart = verschoben(seg.start(), wall.axis(), p.normalOffset());
                PlanPoint linieEnde = verschoben(seg.end(), wall.axis(), p.normalOffset());
                if (!istAusserhalb(linieStart, gebaeudePunkte, wall.axis()) ||
                    !istAusserhalb(linieEnde, gebaeudePunkte, wall.axis())) {
                    verletzungen++;
                    System.err.printf("VERLETZUNG Wand %s: Maß '%s' Linienpunkt inner-/an-Gebäude " +
                                    "(%.0f,%.0f) oder (%.0f,%.0f), normalOffset=%.1f sideSign=%.0f%n",
                            wall.id(),
                            p.dimension() != null ? p.dimension().name() : "Achsmaß",
                            linieStart.xMillimeters(), linieStart.yMillimeters(),
                            linieEnde.xMillimeters(), linieEnde.yMillimeters(),
                            p.normalOffset(), p.placementSideSign());
                }
            }
        }
        assertEquals(0, verletzungen,
                "Alle Maßlinien müssen außerhalb der Gebäudehülle liegen, " + verletzungen + " Verletzung(en)");
    }

    @Test
    void keineMasslinienFallenAufeinander() {
        for (Wall wall : level.walls()) {
            WallDimensionService.WallDimensions dims = wallDimensionService.dimensions(level, wall);
            List<WallDimensionPlacementService.PlacedDimension> placements = placementService.place(
                    level, wall, dims, 1.0, 30.0, 16.0);
            for (int i = 0; i < placements.size(); i++) {
                for (int j = i + 1; j < placements.size(); j++) {
                    double abstand = Math.abs(
                            placements.get(i).lineDistanceFromAxis() - placements.get(j).lineDistanceFromAxis());
                    assertTrue(abstand > 5.0,
                            "Maßlinien der Wand " + wall.id() + " dürfen nicht aufeinanderfallen (Abstand=" + abstand + ")");
                }
            }
        }
    }

    @Test
    void identischeMasseNichtDoppelt() {
        for (Wall wall : level.walls()) {
            WallDimensionService.WallDimensions dims = wallDimensionService.dimensions(level, wall);
            List<WallDimensionPlacementService.PlacedDimension> placements = placementService.place(
                    level, wall, dims, 1.0, 30.0, 16.0);
            for (int i = 0; i < placements.size(); i++) {
                for (int j = i + 1; j < placements.size(); j++) {
                    WallDimensionService.SideDimension a = placements.get(i).dimension();
                    WallDimensionService.SideDimension b = placements.get(j).dimension();
                    if (a == null || b == null) continue;
                    boolean gleicheLaenge = Math.abs(a.length().toMillimeters() - b.length().toMillimeters()) < 0.001;
                    boolean gleichesSegment = segmenteEqual(a.dimensionSegment(), b.dimensionSegment());
                    assertTrue(!(gleicheLaenge && gleichesSegment),
                            "Identische Maße an Wand " + wall.id() + " dürfen nicht doppelt auftreten");
                }
            }
        }
    }

    private List<PlanPoint> sammleGebaeudePunkte() {
        List<PlanPoint> punkte = new ArrayList<>();
        for (Wall wall : level.walls()) {
            double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
            double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
            double len = Math.max(0.001, Math.hypot(dx, dy));
            double nx = -dy / len;
            double ny = dx / len;
            double half = wall.thickness().toMillimeters() / 2.0;
            punkte.add(new PlanPoint(wall.axis().start().xMillimeters() + nx * half, wall.axis().start().yMillimeters() + ny * half));
            punkte.add(new PlanPoint(wall.axis().end().xMillimeters() + nx * half, wall.axis().end().yMillimeters() + ny * half));
            punkte.add(new PlanPoint(wall.axis().start().xMillimeters() - nx * half, wall.axis().start().yMillimeters() - ny * half));
            punkte.add(new PlanPoint(wall.axis().end().xMillimeters() - nx * half, wall.axis().end().yMillimeters() - ny * half));
        }
        for (Room room : level.rooms()) {
            punkte.addAll(room.outline());
        }
        return punkte;
    }

    private PlanPoint verschoben(PlanPoint punkt, PlanSegment achse, double normalOffset) {
        double dx = achse.end().xMillimeters() - achse.start().xMillimeters();
        double dy = achse.end().yMillimeters() - achse.start().yMillimeters();
        double len = Math.max(0.001, Math.hypot(dx, dy));
        double nx = -dy / len;
        double ny = dx / len;
        return new PlanPoint(
                punkt.xMillimeters() + nx * normalOffset,
                punkt.yMillimeters() + ny * normalOffset
        );
    }

    /**
     * Prüft, ob ein Punkt außerhalb der Gebäude-Outline liegt. "Außerhalb" bedeutet:
     * der signed-Normalenabstand zur Wandachse liegt betragsmäßig jenseits der
     * maximalen Gebäudeausdehnung auf der jeweiligen Seite.
     */
    private boolean istAusserhalb(PlanPoint punkt, List<PlanPoint> gebaeudePunkte, PlanSegment achse) {
        double dx = achse.end().xMillimeters() - achse.start().xMillimeters();
        double dy = achse.end().yMillimeters() - achse.start().yMillimeters();
        double len = Math.max(0.001, Math.hypot(dx, dy));
        double dirX = dx / len;
        double dirY = dy / len;
        double nx = -dirY;
        double ny = dirX;
        double punktAbstand = nx * (punkt.xMillimeters() - achse.start().xMillimeters())
                + ny * (punkt.yMillimeters() - achse.start().yMillimeters());
        double maxGebaeude = Double.NEGATIVE_INFINITY;
        double minGebaeude = Double.POSITIVE_INFINITY;
        for (PlanPoint gp : gebaeudePunkte) {
            double abstand = nx * (gp.xMillimeters() - achse.start().xMillimeters())
                    + ny * (gp.yMillimeters() - achse.start().yMillimeters());
            maxGebaeude = Math.max(maxGebaeude, abstand);
            minGebaeude = Math.min(minGebaeude, abstand);
        }
        // Der Punkt muss betragsmäßig außerhalb der Extrema liegen, mit etwas Toleranz.
        double toleranz = 1.0;
        return punktAbstand > maxGebaeude + toleranz || punktAbstand < minGebaeude - toleranz;
    }

    private boolean segmenteEqual(PlanSegment a, PlanSegment b) {
        return Math.abs(a.length().toMillimeters() - b.length().toMillimeters()) < 0.001
                && punkteEqual(a.start(), b.start())
                && punkteEqual(a.end(), b.end());
    }

    private boolean punkteEqual(PlanPoint a, PlanPoint b) {
        return Math.abs(a.xMillimeters() - b.xMillimeters()) < 0.001
                && Math.abs(a.yMillimeters() - b.yMillimeters()) < 0.001;
    }
}