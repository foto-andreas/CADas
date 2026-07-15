package de.schrell.cadas.application.reports;

import de.schrell.cadas.application.layers.SurfaceLayerEffectService;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.FloorExtensionPlacement;
import de.schrell.cadas.domain.model.FloorExtensionType;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.Staircase;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Berechnet anrechenbare Wohnflächen nach den in CADas abgebildeten Regeln der WoFlV.
 * Raumteile ab zwei Metern lichter Höhe zählen vollständig, Teile zwischen einem und zwei Metern zur Hälfte.
 * Bodenöffnungen und Treppen mit mehr als drei Steigungen zählen nicht. Außenbalkone werden mangels einer
 * gesonderten Qualitätsbewertung mit dem gesetzlichen Regelfaktor von 25 Prozent angesetzt.
 */
public final class ResidentialAreaService {

    private static final double ONE_METER_MILLIMETERS = 1_000.0;
    private static final double TWO_METERS_MILLIMETERS = 2_000.0;
    private static final double BALCONY_FACTOR = 0.25;
    private static final double CURVE_FLATNESS_MILLIMETERS = 0.1;
    private final SurfaceLayerEffectService effectService = new SurfaceLayerEffectService();

    /**
     * Berechnet die anrechenbare Fläche eines Raums ohne Stichprobenraster. Die nutzbare Grundfläche wird zuerst
     * geometrisch um Öffnungen und Treppen bereinigt und danach exakt an den beiden Höhenebenen geschnitten.
     */
    public double residentialAreaSquareMeters(Level level, Room room) {
        List<List<PlanPoint>> usablePolygons = polygons(usableGroundArea(level, room));
        if (usablePolygons.isEmpty()) {
            return 0.0;
        }
        double outerOrientation = usablePolygons.stream()
                .max(Comparator.comparingDouble(polygon -> Math.abs(signedArea(polygon))))
                .map(polygon -> Math.signum(signedArea(polygon)))
                .orElse(1.0);
        double surfaceReduction = effectService.floorLayerThicknessMillimeters(level, room)
                + effectService.ceilingLayerThicknessMillimeters(level, room);
        double weightedArea = 0.0;
        for (List<PlanPoint> polygon : usablePolygons) {
            // Java2D liefert Lochkonturen mit entgegengesetzter Laufrichtung; ihr Beitrag wird daher abgezogen.
            double orientationFactor = Math.signum(signedArea(polygon)) * outerOrientation;
            double atLeastOneMeter = room.areaSquareMetersAtMinimumHeight(
                    polygon,
                    ONE_METER_MILLIMETERS + surfaceReduction
            );
            double atLeastTwoMeters = room.areaSquareMetersAtMinimumHeight(
                    polygon,
                    TWO_METERS_MILLIMETERS + surfaceReduction
            );
            weightedArea += orientationFactor * (atLeastTwoMeters + 0.5 * (atLeastOneMeter - atLeastTwoMeters));
        }
        return Math.max(0.0, weightedArea);
    }

    /**
     * Liefert die Regelfläche eines außenliegenden Balkons. Andere Erweiterungen benötigen eine eigenständige
     * fachliche Einordnung und werden deshalb nicht stillschweigend als Wohnfläche angesetzt.
     */
    public double residentialAreaSquareMeters(FloorExtension extension) {
        if (extension.type() != FloorExtensionType.BALCONY
                || extension.placement() != FloorExtensionPlacement.EXTERIOR) {
            return 0.0;
        }
        return extension.widthMillimeters() * extension.depthMillimeters() / 1_000_000.0 * BALCONY_FACTOR;
    }

    private Area usableGroundArea(Level level, Room room) {
        Area usableArea = new Area(path(room.outline()));
        level.floorOpenings().stream()
                .filter(opening -> opening.roomId().equals(room.id()))
                .map(this::openingArea)
                .forEach(usableArea::subtract);
        level.staircases().stream()
                .filter(staircase -> staircase.stepCount() > 3)
                .map(this::staircaseArea)
                .forEach(usableArea::subtract);
        return usableArea;
    }

    private Area openingArea(FloorOpening opening) {
        if (opening.shape() == FloorOpeningShape.CIRCLE) {
            return new Area(new Ellipse2D.Double(
                    opening.minXMillimeters(),
                    opening.minYMillimeters(),
                    opening.width().toMillimeters(),
                    opening.depth().toMillimeters()
            ));
        }
        return new Area(new Rectangle2D.Double(
                opening.minXMillimeters(),
                opening.minYMillimeters(),
                opening.width().toMillimeters(),
                opening.depth().toMillimeters()
        ));
    }

    private Area staircaseArea(Staircase staircase) {
        return new Area(path(List.of(
                staircase.pointAtLocalPosition(0.0, 0.0),
                staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0.0),
                staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()),
                staircase.pointAtLocalPosition(0.0, staircase.heightMillimeters())
        )));
    }

    private Path2D path(List<PlanPoint> outline) {
        Path2D path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        path.moveTo(outline.getFirst().xMillimeters(), outline.getFirst().yMillimeters());
        outline.stream().skip(1).forEach(point -> path.lineTo(point.xMillimeters(), point.yMillimeters()));
        path.closePath();
        return path;
    }

    private List<List<PlanPoint>> polygons(Area area) {
        List<List<PlanPoint>> polygons = new ArrayList<>();
        List<PlanPoint> current = null;
        double[] coordinates = new double[6];
        PathIterator iterator = new FlatteningPathIterator(area.getPathIterator(null), CURVE_FLATNESS_MILLIMETERS);
        while (!iterator.isDone()) {
            int segment = iterator.currentSegment(coordinates);
            if (segment == PathIterator.SEG_MOVETO) {
                current = new ArrayList<>();
                polygons.add(current);
                current.add(new PlanPoint(coordinates[0], coordinates[1]));
            } else if (segment == PathIterator.SEG_LINETO && current != null) {
                current.add(new PlanPoint(coordinates[0], coordinates[1]));
            }
            iterator.next();
        }
        return polygons.stream()
                .map(this::withoutDuplicateClosingPoint)
                .filter(polygon -> polygon.size() >= 3)
                .toList();
    }

    private List<PlanPoint> withoutDuplicateClosingPoint(List<PlanPoint> polygon) {
        if (polygon.size() > 1
                && polygon.getFirst().distanceTo(polygon.getLast()).toMillimeters() < 0.000_001) {
            return List.copyOf(polygon.subList(0, polygon.size() - 1));
        }
        return List.copyOf(polygon);
    }

    private double signedArea(List<PlanPoint> polygon) {
        double sum = 0.0;
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint current = polygon.get(index);
            PlanPoint next = polygon.get((index + 1) % polygon.size());
            sum += current.xMillimeters() * next.yMillimeters() - next.xMillimeters() * current.yMillimeters();
        }
        return sum / 2.0;
    }
}
