package de.schrell.cadas.application.reports;

import de.schrell.cadas.application.layers.SurfaceLayerEffectService;
import de.schrell.cadas.application.room.OrthogonalPolygonDecompositionService;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.Staircase;

import java.util.List;

public final class ResidentialAreaService {

    private static final double TARGET_SAMPLE_SIZE_MILLIMETERS = 50.0;
    private static final int MAXIMUM_SAMPLES_PER_AXIS = 200;
    private final SurfaceLayerEffectService effectService = new SurfaceLayerEffectService();
    private final OrthogonalPolygonDecompositionService decompositionService = new OrthogonalPolygonDecompositionService();

    public double residentialAreaSquareMeters(Level level, Room room) {
        List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles = decompositionService.decompose(room.outline());
        if (rectangles.isEmpty()) {
            rectangles = List.of(new OrthogonalPolygonDecompositionService.CellRectangle(
                    room.minXMillimeters(),
                    room.maxXMillimeters(),
                    room.minYMillimeters(),
                    room.maxYMillimeters()
            ));
        }
        double weightedAreaSquareMillimeters = 0.0;
        for (OrthogonalPolygonDecompositionService.CellRectangle rectangle : rectangles) {
            int columns = sampleCount(rectangle.width());
            int rows = sampleCount(rectangle.height());
            double sampleWidth = rectangle.width() / columns;
            double sampleHeight = rectangle.height() / rows;
            double sampleArea = sampleWidth * sampleHeight;
            for (int column = 0; column < columns; column++) {
                for (int row = 0; row < rows; row++) {
                    PlanPoint samplePoint = new PlanPoint(
                            rectangle.minX() + (column + 0.5) * sampleWidth,
                            rectangle.minY() + (row + 0.5) * sampleHeight
                    );
                    if (excludedGroundArea(level, room, samplePoint)) {
                        continue;
                    }
                    weightedAreaSquareMillimeters += sampleArea * heightFactor(effectService.effectiveHeightAt(level, room, samplePoint));
                }
            }
        }
        return weightedAreaSquareMillimeters / 1_000_000.0;
    }

    private boolean excludedGroundArea(Level level, Room room, PlanPoint point) {
        boolean floorOpening = level.floorOpenings().stream()
                .filter(opening -> opening.roomId().equals(room.id()))
                .anyMatch(opening -> opening.contains(point));
        if (floorOpening) {
            return true;
        }
        return level.staircases().stream()
                .filter(staircase -> staircase.stepCount() > 3)
                .anyMatch(staircase -> contains(staircase, point));
    }

    private boolean contains(Staircase staircase, PlanPoint point) {
        List<PlanPoint> outline = List.of(
                staircase.pointAtLocalPosition(0.0, 0.0),
                staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0.0),
                staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()),
                staircase.pointAtLocalPosition(0.0, staircase.heightMillimeters())
        );
        boolean inside = false;
        int previousIndex = outline.size() - 1;
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint current = outline.get(index);
            PlanPoint previous = outline.get(previousIndex);
            boolean intersects = (current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters())
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / (previous.yMillimeters() - current.yMillimeters())
                    + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            previousIndex = index;
        }
        return inside;
    }

    private int sampleCount(double lengthMillimeters) {
        return Math.max(1, Math.min(MAXIMUM_SAMPLES_PER_AXIS, (int) Math.ceil(lengthMillimeters / TARGET_SAMPLE_SIZE_MILLIMETERS)));
    }

    private double heightFactor(double heightMillimeters) {
        if (heightMillimeters >= 2_000.0) {
            return 1.0;
        }
        if (heightMillimeters >= 1_000.0) {
            return 0.5;
        }
        return 0.0;
    }
}
