package de.schrell.cadas.application.floor;

import de.schrell.cadas.application.room.OrthogonalPolygonDecompositionService;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;

import java.util.ArrayList;
import java.util.List;

public final class FloorOpeningGeometryService {

    private static final int CIRCLE_SEGMENTS = 48;
    private final OrthogonalPolygonDecompositionService decompositionService = new OrthogonalPolygonDecompositionService();

    public List<OrthogonalPolygonDecompositionService.CellRectangle> floorRectangles(Level level, Room room) {
        return subtractOpenings(roomRectangles(room), openingsForRoom(level, room));
    }

    public List<OrthogonalPolygonDecompositionService.CellRectangle> ceilingRectangles(Room room, List<FloorOpening> openingsAbove) {
        return subtractOpenings(roomRectangles(room), openingsAbove);
    }

    public List<FloorOpening> openingsForRoom(Level level, Room room) {
        return level.floorOpenings().stream().filter(opening -> opening.roomId().equals(room.id())).toList();
    }

    public double floorAreaSquareMeters(Level level, Room room) {
        return floorRectangles(level, room).stream()
                .mapToDouble(rectangle -> rectangle.width() * rectangle.height())
                .sum() / 1_000_000.0;
    }

    private List<OrthogonalPolygonDecompositionService.CellRectangle> roomRectangles(Room room) {
        List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles = decompositionService.decompose(room.outline());
        if (!rectangles.isEmpty()) {
            return rectangles;
        }
        return List.of(new OrthogonalPolygonDecompositionService.CellRectangle(
                room.minXMillimeters(), room.maxXMillimeters(), room.minYMillimeters(), room.maxYMillimeters()
        ));
    }

    private List<OrthogonalPolygonDecompositionService.CellRectangle> subtractOpenings(
            List<OrthogonalPolygonDecompositionService.CellRectangle> source,
            List<FloorOpening> openings
    ) {
        List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles = List.copyOf(source);
        for (FloorOpening opening : openings) {
            for (Cutout cutout : cutouts(opening)) {
                rectangles = subtract(rectangles, cutout);
            }
        }
        return List.copyOf(rectangles);
    }

    private List<Cutout> cutouts(FloorOpening opening) {
        if (opening.shape() == FloorOpeningShape.RECTANGLE) {
            return List.of(new Cutout(
                    opening.minXMillimeters(), opening.maxXMillimeters(),
                    opening.minYMillimeters(), opening.maxYMillimeters()
            ));
        }
        List<Cutout> cutouts = new ArrayList<>();
        double radius = opening.width().toMillimeters() / 2.0;
        double sliceHeight = opening.depth().toMillimeters() / CIRCLE_SEGMENTS;
        for (int index = 0; index < CIRCLE_SEGMENTS; index++) {
            double minY = opening.minYMillimeters() + index * sliceHeight;
            double maxY = minY + sliceHeight;
            double sampleY = (minY + maxY) / 2.0 - opening.center().yMillimeters();
            double halfWidth = Math.sqrt(Math.max(0.0, radius * radius - sampleY * sampleY));
            cutouts.add(new Cutout(
                    opening.center().xMillimeters() - halfWidth,
                    opening.center().xMillimeters() + halfWidth,
                    minY,
                    maxY
            ));
        }
        return cutouts;
    }

    private List<OrthogonalPolygonDecompositionService.CellRectangle> subtract(
            List<OrthogonalPolygonDecompositionService.CellRectangle> source,
            Cutout cutout
    ) {
        List<OrthogonalPolygonDecompositionService.CellRectangle> result = new ArrayList<>();
        for (OrthogonalPolygonDecompositionService.CellRectangle rectangle : source) {
            double overlapMinX = Math.max(rectangle.minX(), cutout.minX());
            double overlapMaxX = Math.min(rectangle.maxX(), cutout.maxX());
            double overlapMinY = Math.max(rectangle.minY(), cutout.minY());
            double overlapMaxY = Math.min(rectangle.maxY(), cutout.maxY());
            if (overlapMaxX <= overlapMinX || overlapMaxY <= overlapMinY) {
                result.add(rectangle);
                continue;
            }
            addRectangle(result, rectangle.minX(), rectangle.maxX(), rectangle.minY(), overlapMinY);
            addRectangle(result, rectangle.minX(), rectangle.maxX(), overlapMaxY, rectangle.maxY());
            addRectangle(result, rectangle.minX(), overlapMinX, overlapMinY, overlapMaxY);
            addRectangle(result, overlapMaxX, rectangle.maxX(), overlapMinY, overlapMaxY);
        }
        return result;
    }

    private void addRectangle(
            List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles,
            double minX,
            double maxX,
            double minY,
            double maxY
    ) {
        if (maxX - minX > 0.001 && maxY - minY > 0.001) {
            rectangles.add(new OrthogonalPolygonDecompositionService.CellRectangle(minX, maxX, minY, maxY));
        }
    }

    private record Cutout(double minX, double maxX, double minY, double maxY) {
    }
}
