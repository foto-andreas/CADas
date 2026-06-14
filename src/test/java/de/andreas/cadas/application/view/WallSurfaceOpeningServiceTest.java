package de.andreas.cadas.application.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.application.view.WallSurfaceOpeningService.WallOpeningRectangle;
import de.andreas.cadas.application.view.WallSurfaceOpeningService.WallSurfaceInterval;
import de.andreas.cadas.application.view.WallSurfaceOpeningService.WallSurfaceRectangle;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;
import java.util.List;
import org.junit.jupiter.api.Test;

class WallSurfaceOpeningServiceTest {

    private final WallSurfaceOpeningService service = new WallSurfaceOpeningService();

    @Test
    void bildetMaximaleSichtbareRechteckeUmTürenUndFenster() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(wall);
        level.addDoor(Door.create(
                wall.id(),
                Length.of(1.0, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(2.1, LengthUnit.METER),
                Length.zero()
        ));
        level.addWindow(WindowElement.create(
                wall.id(),
                Length.of(2.4, LengthUnit.METER),
                Length.of(1.0, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.2, LengthUnit.METER)
        ));

        List<WallSurfaceRectangle> rectangles = service.visibleRectangles(level, wall);
        List<WallOpeningRectangle> openings = service.openingRectangles(level, wall);

        assertEquals(5, rectangles.size());
        assertTrue(rectangles.stream().anyMatch(rectangle -> rectangleMatches(rectangle, 0.0, 1000.0, 0.0, 2100.0)));
        assertTrue(rectangles.stream().anyMatch(rectangle -> rectangleMatches(rectangle, 1900.0, 4000.0, 0.0, 900.0)));
        assertTrue(rectangles.stream().anyMatch(rectangle -> rectangleMatches(rectangle, 1900.0, 2400.0, 900.0, 2100.0)));
        assertTrue(rectangles.stream().anyMatch(rectangle -> rectangleMatches(rectangle, 3400.0, 4000.0, 900.0, 2100.0)));
        assertTrue(rectangles.stream().anyMatch(rectangle -> rectangleMatches(rectangle, 0.0, 4000.0, 2100.0, 2800.0)));
        assertFalse(rectangles.stream().anyMatch(rectangle ->
                openings.stream().anyMatch(opening -> overlaps(rectangle, opening))
        ));
    }

    @Test
    void draufsichtIntervalleSchneidenÖffnungenAus() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(wall);
        level.addDoor(Door.create(
                wall.id(),
                Length.of(1.0, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(2.1, LengthUnit.METER),
                Length.zero()
        ));
        level.addWindow(WindowElement.create(
                wall.id(),
                Length.of(2.4, LengthUnit.METER),
                Length.of(1.0, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.2, LengthUnit.METER)
        ));

        List<WallSurfaceInterval> intervals = service.visiblePlanIntervals(level, wall);

        assertEquals(3, intervals.size());
        assertTrue(intervals.stream().anyMatch(interval -> intervalMatches(interval, 0.0, 1000.0)));
        assertTrue(intervals.stream().anyMatch(interval -> intervalMatches(interval, 1900.0, 2400.0)));
        assertTrue(intervals.stream().anyMatch(interval -> intervalMatches(interval, 3400.0, 4000.0)));
    }

    @Test
    void draufsichtIntervalleUnterbrechenBelagAnEinbindenderInnenwandNurAufBetroffenerSeite() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        Wall exteriorWall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        Wall interiorWall = Wall.create(
                new PlanSegment(new PlanPoint(2000, 0), new PlanPoint(2000, 1500)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(exteriorWall);
        level.addWall(interiorWall);

        List<WallSurfaceInterval> interrupted = service.visiblePlanIntervals(level, exteriorWall, 1.0);
        List<WallSurfaceInterval> unaffected = service.visiblePlanIntervals(level, exteriorWall, -1.0);

        assertEquals(2, interrupted.size());
        assertTrue(interrupted.stream().anyMatch(interval -> intervalMatches(interval, 0.0, 1900.0)));
        assertTrue(interrupted.stream().anyMatch(interval -> intervalMatches(interval, 2100.0, 4000.0)));
        assertEquals(1, unaffected.size());
        assertTrue(unaffected.stream().anyMatch(interval -> intervalMatches(interval, 0.0, 4000.0)));
    }

    private boolean rectangleMatches(WallSurfaceRectangle rectangle, double start, double end, double lower, double upper) {
        return Math.abs(rectangle.startMillimeters() - start) < 0.001
                && Math.abs(rectangle.endMillimeters() - end) < 0.001
                && Math.abs(rectangle.lowerHeightMillimeters() - lower) < 0.001
                && Math.abs(rectangle.upperHeightMillimeters() - upper) < 0.001;
    }

    private boolean intervalMatches(WallSurfaceInterval interval, double start, double end) {
        return Math.abs(interval.startMillimeters() - start) < 0.001
                && Math.abs(interval.endMillimeters() - end) < 0.001;
    }

    private boolean overlaps(WallSurfaceRectangle rectangle, WallOpeningRectangle opening) {
        return rectangle.startMillimeters() < opening.endMillimeters() - 0.001
                && rectangle.endMillimeters() > opening.startMillimeters() + 0.001
                && rectangle.lowerHeightMillimeters() < opening.upperHeightMillimeters() - 0.001
                && rectangle.upperHeightMillimeters() > opening.lowerHeightMillimeters() + 0.001;
    }
}
