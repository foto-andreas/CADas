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
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void alleBemaßungenLiegenAußerhalbDesGebäudes() {
        for (Wall wall : level.walls()) {
            WallDimensionService.WallDimensions dims = wallDimensionService.dimensions(level, wall);
            List<WallDimensionPlacementService.PlacedDimension> placements = placementService.place(
                    level, wall, dims, 1.0, 30.0, 16.0);
            for (WallDimensionPlacementService.PlacedDimension placement : placements) {
                PlanSegment segment = placement.dimension() != null
                        ? placement.dimension().dimensionSegment()
                        : wall.axis();
                PlanSegment dimensionLine = verschoben(segment, placement.normalOffset());
                for (int index = 0; index <= 20; index++) {
                    PlanPoint point = punktAufSegment(dimensionLine, index / 20.0);
                    assertFalse(istImGebäude(point), () -> "Maßlinie liegt im Gebäude: Wand " + wall.id()
                            + ", Achse " + wall.axis() + ", Maß " + placement.dimension().name()
                            + ", Segment " + segment + ", Linie " + dimensionLine
                            + ", Offset " + placement.normalOffset() + ", Achsabstand " + placement.lineDistanceFromAxis()
                            + ", Seite " + placement.placementSideSign() + ", Punkt " + point);
                }
            }
        }
    }

    @Test
    void keineMaßlinienFallenAufeinander() {
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
    void identischeMaßeNichtDoppelt() {
        for (Wall wall : level.walls()) {
            WallDimensionService.WallDimensions dims = wallDimensionService.dimensions(level, wall);
            List<WallDimensionPlacementService.PlacedDimension> placements = placementService.place(
                    level, wall, dims, 1.0, 30.0, 16.0);
            for (int i = 0; i < placements.size(); i++) {
                for (int j = i + 1; j < placements.size(); j++) {
                    WallDimensionService.SideDimension a = placements.get(i).dimension();
                    WallDimensionService.SideDimension b = placements.get(j).dimension();
                    if (a == null || b == null) continue;
                    boolean gleicheLänge = Math.abs(a.length().toMillimeters() - b.length().toMillimeters()) < 0.001;
                    boolean gleichesSegment = segmenteEqual(a.dimensionSegment(), b.dimensionSegment());
                    assertTrue(!(gleicheLänge && gleichesSegment),
                            "Identische Maße an Wand " + wall.id() + " dürfen nicht doppelt auftreten");
                }
            }
        }
    }

    private PlanSegment verschoben(PlanSegment segment, double normalOffset) {
        double dx = segment.end().xMillimeters() - segment.start().xMillimeters();
        double dy = segment.end().yMillimeters() - segment.start().yMillimeters();
        double len = Math.max(0.001, Math.hypot(dx, dy));
        double nx = -dy / len;
        double ny = dx / len;
        return new PlanSegment(
                new PlanPoint(segment.start().xMillimeters() + nx * normalOffset, segment.start().yMillimeters() + ny * normalOffset),
                new PlanPoint(segment.end().xMillimeters() + nx * normalOffset, segment.end().yMillimeters() + ny * normalOffset)
        );
    }

    private PlanPoint punktAufSegment(PlanSegment segment, double ratio) {
        return new PlanPoint(
                segment.start().xMillimeters() + (segment.end().xMillimeters() - segment.start().xMillimeters()) * ratio,
                segment.start().yMillimeters() + (segment.end().yMillimeters() - segment.start().yMillimeters()) * ratio
        );
    }

    private boolean istImGebäude(PlanPoint point) {
        if (level.rooms().stream().anyMatch(room -> enthält(room, point))) {
            return true;
        }
        return level.walls().stream().anyMatch(wall -> abstand(point, wall.axis()) < wall.thickness().toMillimeters() / 2.0 - 0.001);
    }

    private boolean enthält(Room room, PlanPoint point) {
        boolean inside = false;
        int previousIndex = room.outline().size() - 1;
        for (int index = 0; index < room.outline().size(); index++) {
            PlanPoint current = room.outline().get(index);
            PlanPoint previous = room.outline().get(previousIndex);
            boolean intersects = (current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters())
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / (previous.yMillimeters() - current.yMillimeters()) + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            previousIndex = index;
        }
        return inside;
    }

    private double abstand(PlanPoint point, PlanSegment segment) {
        double dx = segment.end().xMillimeters() - segment.start().xMillimeters();
        double dy = segment.end().yMillimeters() - segment.start().yMillimeters();
        double squaredLength = dx * dx + dy * dy;
        double ratio = squaredLength <= 0.001 ? 0.0 : ((point.xMillimeters() - segment.start().xMillimeters()) * dx
                + (point.yMillimeters() - segment.start().yMillimeters()) * dy) / squaredLength;
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        double nearestX = segment.start().xMillimeters() + ratio * dx;
        double nearestY = segment.start().yMillimeters() + ratio * dy;
        return Math.hypot(point.xMillimeters() - nearestX, point.yMillimeters() - nearestY);
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
