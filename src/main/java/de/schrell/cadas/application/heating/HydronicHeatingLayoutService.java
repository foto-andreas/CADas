package de.schrell.cadas.application.heating;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Room;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class HydronicHeatingLayoutService {

    private static final double EPSILON = 0.001;
    private static final int MAXIMUM_ZONE_COUNT = 64;

    public PlanningResult suggest(Room room, HydronicHeating heating) {
        List<HeatingZone> zones = new ArrayList<>();
        zones.add(HeatingZone.create("Heizkreis 1", room.outline()));
        while (zones.size() < MAXIMUM_ZONE_COUNT) {
            Optional<HeatingZone> oversized = zones.stream()
                    .filter(zone -> layoutZone(heating, zone).pipeLength().compareTo(heating.maximumPipeLength()) > 0)
                    .max(Comparator.comparingDouble(HeatingZone::areaSquareMillimeters));
            if (oversized.isEmpty()) {
                break;
            }
            HeatingZone zone = oversized.orElseThrow();
            List<HeatingZone> split = split(zone);
            if (split.size() < 2) {
                break;
            }
            int index = zones.indexOf(zone);
            zones.remove(index);
            zones.addAll(index, split);
        }
        List<HeatingZone> namedZones = new ArrayList<>();
        for (int index = 0; index < zones.size(); index++) {
            namedZones.add(new HeatingZone(zones.get(index).id(), "Heizkreis " + (index + 1), zones.get(index).outline()));
        }
        HydronicHeating planned = heating.withZones(namedZones);
        return new PlanningResult(planned, layout(planned));
    }

    public List<CircuitLayout> layout(HydronicHeating heating) {
        return heating.zones().stream().map(zone -> layoutZone(heating, zone)).toList();
    }

    private CircuitLayout layoutZone(HydronicHeating heating, HeatingZone zone) {
        List<PlanPoint> path = heating.layoutPattern() == HeatingLayoutPattern.SPIRAL
                ? spiralPath(zone.outline(), heating.wallClearance().toMillimeters(), heating.pipeSpacing().toMillimeters())
                : meanderPath(zone.outline(), heating.wallClearance().toMillimeters(), heating.pipeSpacing().toMillimeters());
        if (path.isEmpty()) {
            path = List.of(centroid(zone.outline()));
        }
        List<PlanPoint> connectedPath = new ArrayList<>();
        connectedPath.add(heating.supplyPoint());
        connectedPath.addAll(path);
        connectedPath.add(heating.returnPoint());
        return new CircuitLayout(
                zone.id(), List.copyOf(connectedPath),
                Length.ofMillimeters(roundedLength(connectedPath, heating.bendRadius().toMillimeters())),
                heating.bendRadius()
        );
    }

    private List<PlanPoint> meanderPath(List<PlanPoint> polygon, double clearance, double spacing) {
        Bounds bounds = bounds(polygon);
        boolean horizontal = bounds.width() >= bounds.height();
        double minimumLane = (horizontal ? bounds.minY() : bounds.minX()) + clearance;
        double maximumLane = (horizontal ? bounds.maxY() : bounds.maxX()) - clearance;
        List<PlanPoint> path = new ArrayList<>();
        boolean reverse = false;
        for (double lane = minimumLane; lane <= maximumLane + EPSILON; lane += spacing) {
            List<Interval> intervals = scanlineIntervals(polygon, lane, horizontal);
            if (reverse) {
                intervals = intervals.reversed();
            }
            for (Interval interval : intervals) {
                double start = interval.start() + clearance;
                double end = interval.end() - clearance;
                if (end - start < spacing / 2.0) {
                    continue;
                }
                PlanPoint first = horizontal
                        ? new PlanPoint(reverse ? end : start, lane)
                        : new PlanPoint(lane, reverse ? end : start);
                PlanPoint second = horizontal
                        ? new PlanPoint(reverse ? start : end, lane)
                        : new PlanPoint(lane, reverse ? start : end);
                appendConnected(path, first, polygon);
                appendDistinct(path, second);
            }
            reverse = !reverse;
        }
        return List.copyOf(path);
    }

    private List<PlanPoint> spiralPath(List<PlanPoint> polygon, double clearance, double spacing) {
        List<List<PlanPoint>> contours = new ArrayList<>();
        for (double offset = clearance; contours.size() < 256; offset += spacing) {
            List<PlanPoint> contour = offsetPolygon(polygon, offset);
            if (contour.size() < 3 || Math.abs(signedDoubleArea(contour)) < spacing * spacing * 2.0) {
                break;
            }
            if (contour.stream().anyMatch(point -> !containsPoint(polygon, point))) {
                break;
            }
            contours.add(contour);
        }
        if (contours.size() < 2) {
            return meanderPath(polygon, clearance, spacing);
        }
        List<PlanPoint> path = new ArrayList<>();
        for (int index = 0; index < contours.size(); index += 2) {
            appendOpenContour(path, contours.get(index), false);
        }
        int returnIndex = contours.size() % 2 == 0 ? contours.size() - 1 : contours.size() - 2;
        for (int index = returnIndex; index >= 1; index -= 2) {
            appendOpenContour(path, contours.get(index), true);
        }
        return List.copyOf(path);
    }

    private void appendOpenContour(List<PlanPoint> path, List<PlanPoint> contour, boolean reverse) {
        List<PlanPoint> ordered = reverse ? contour.reversed() : contour;
        int startIndex = nearestIndex(ordered, path.isEmpty() ? centroid(contour) : path.getLast());
        for (int offset = 0; offset < ordered.size(); offset++) {
            appendDistinct(path, ordered.get((startIndex + offset) % ordered.size()));
        }
    }

    private int nearestIndex(List<PlanPoint> points, PlanPoint reference) {
        int nearestIndex = 0;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (int index = 0; index < points.size(); index++) {
            double distance = points.get(index).distanceTo(reference).toMillimeters();
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = index;
            }
        }
        return nearestIndex;
    }

    private List<PlanPoint> offsetPolygon(List<PlanPoint> polygon, double distance) {
        double orientation = Math.signum(signedDoubleArea(polygon));
        if (orientation == 0.0) {
            return List.of();
        }
        List<PlanPoint> result = new ArrayList<>();
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint previous = polygon.get((index - 1 + polygon.size()) % polygon.size());
            PlanPoint current = polygon.get(index);
            PlanPoint next = polygon.get((index + 1) % polygon.size());
            ShiftedLine first = shiftedLine(previous, current, distance, orientation);
            ShiftedLine second = shiftedLine(current, next, distance, orientation);
            intersect(first, second).ifPresent(result::add);
        }
        return simplifyPolygon(result);
    }

    private ShiftedLine shiftedLine(PlanPoint start, PlanPoint end, double distance, double orientation) {
        double deltaX = end.xMillimeters() - start.xMillimeters();
        double deltaY = end.yMillimeters() - start.yMillimeters();
        double length = Math.max(EPSILON, Math.hypot(deltaX, deltaY));
        double normalX = -deltaY / length * orientation;
        double normalY = deltaX / length * orientation;
        return new ShiftedLine(
                new PlanPoint(start.xMillimeters() + normalX * distance, start.yMillimeters() + normalY * distance),
                new PlanPoint(end.xMillimeters() + normalX * distance, end.yMillimeters() + normalY * distance)
        );
    }

    private Optional<PlanPoint> intersect(ShiftedLine first, ShiftedLine second) {
        double firstDeltaX = first.end().xMillimeters() - first.start().xMillimeters();
        double firstDeltaY = first.end().yMillimeters() - first.start().yMillimeters();
        double secondDeltaX = second.end().xMillimeters() - second.start().xMillimeters();
        double secondDeltaY = second.end().yMillimeters() - second.start().yMillimeters();
        double denominator = firstDeltaX * secondDeltaY - firstDeltaY * secondDeltaX;
        if (Math.abs(denominator) < EPSILON) {
            return Optional.empty();
        }
        double startDeltaX = second.start().xMillimeters() - first.start().xMillimeters();
        double startDeltaY = second.start().yMillimeters() - first.start().yMillimeters();
        double ratio = (startDeltaX * secondDeltaY - startDeltaY * secondDeltaX) / denominator;
        return Optional.of(new PlanPoint(
                first.start().xMillimeters() + ratio * firstDeltaX,
                first.start().yMillimeters() + ratio * firstDeltaY
        ));
    }

    private List<Interval> scanlineIntervals(List<PlanPoint> polygon, double lane, boolean horizontal) {
        List<Double> intersections = new ArrayList<>();
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint first = polygon.get(index);
            PlanPoint second = polygon.get((index + 1) % polygon.size());
            double firstCross = horizontal ? first.yMillimeters() : first.xMillimeters();
            double secondCross = horizontal ? second.yMillimeters() : second.xMillimeters();
            if ((firstCross > lane) == (secondCross > lane) || Math.abs(firstCross - secondCross) < EPSILON) {
                continue;
            }
            double ratio = (lane - firstCross) / (secondCross - firstCross);
            intersections.add(horizontal
                    ? first.xMillimeters() + ratio * (second.xMillimeters() - first.xMillimeters())
                    : first.yMillimeters() + ratio * (second.yMillimeters() - first.yMillimeters()));
        }
        intersections.sort(Double::compareTo);
        List<Interval> intervals = new ArrayList<>();
        for (int index = 0; index + 1 < intersections.size(); index += 2) {
            intervals.add(new Interval(intersections.get(index), intersections.get(index + 1)));
        }
        return intervals;
    }

    private void appendConnected(List<PlanPoint> path, PlanPoint target, List<PlanPoint> polygon) {
        if (path.isEmpty()) {
            path.add(target);
            return;
        }
        PlanPoint previous = path.getLast();
        PlanPoint firstCorner = new PlanPoint(previous.xMillimeters(), target.yMillimeters());
        PlanPoint secondCorner = new PlanPoint(target.xMillimeters(), previous.yMillimeters());
        if (segmentInside(previous, firstCorner, polygon) && segmentInside(firstCorner, target, polygon)) {
            appendDistinct(path, firstCorner);
        } else if (segmentInside(previous, secondCorner, polygon) && segmentInside(secondCorner, target, polygon)) {
            appendDistinct(path, secondCorner);
        }
        appendDistinct(path, target);
    }

    private boolean segmentInside(PlanPoint start, PlanPoint end, List<PlanPoint> polygon) {
        for (int step = 0; step <= 8; step++) {
            double ratio = step / 8.0;
            PlanPoint point = new PlanPoint(
                    start.xMillimeters() + (end.xMillimeters() - start.xMillimeters()) * ratio,
                    start.yMillimeters() + (end.yMillimeters() - start.yMillimeters()) * ratio
            );
            if (!containsPoint(polygon, point)) {
                return false;
            }
        }
        return true;
    }

    private List<HeatingZone> split(HeatingZone zone) {
        Bounds bounds = bounds(zone.outline());
        boolean splitVertically = bounds.width() >= bounds.height();
        double splitCoordinate = splitVertically
                ? (bounds.minX() + bounds.maxX()) / 2.0
                : (bounds.minY() + bounds.maxY()) / 2.0;
        List<PlanPoint> first = clip(zone.outline(), splitVertically, splitCoordinate, true);
        List<PlanPoint> second = clip(zone.outline(), splitVertically, splitCoordinate, false);
        if (first.size() < 3 || second.size() < 3) {
            return List.of(zone);
        }
        return List.of(
                HeatingZone.create(zone.name(), first),
                HeatingZone.create(zone.name(), second)
        );
    }

    private List<PlanPoint> clip(List<PlanPoint> polygon, boolean xAxis, double boundary, boolean keepLower) {
        List<PlanPoint> output = new ArrayList<>(polygon);
        List<PlanPoint> input = new ArrayList<>(output);
        output.clear();
        if (input.isEmpty()) {
            return List.of();
        }
        PlanPoint previous = input.getLast();
        for (PlanPoint current : input) {
            boolean currentInside = insideBoundary(current, xAxis, boundary, keepLower);
            boolean previousInside = insideBoundary(previous, xAxis, boundary, keepLower);
            if (currentInside != previousInside) {
                output.add(boundaryIntersection(previous, current, xAxis, boundary));
            }
            if (currentInside) {
                output.add(current);
            }
            previous = current;
        }
        return simplifyPolygon(output);
    }

    private boolean insideBoundary(PlanPoint point, boolean xAxis, double boundary, boolean keepLower) {
        double coordinate = xAxis ? point.xMillimeters() : point.yMillimeters();
        return keepLower ? coordinate <= boundary + EPSILON : coordinate >= boundary - EPSILON;
    }

    private PlanPoint boundaryIntersection(PlanPoint start, PlanPoint end, boolean xAxis, double boundary) {
        double startCoordinate = xAxis ? start.xMillimeters() : start.yMillimeters();
        double endCoordinate = xAxis ? end.xMillimeters() : end.yMillimeters();
        double ratio = Math.abs(endCoordinate - startCoordinate) < EPSILON
                ? 0.0
                : (boundary - startCoordinate) / (endCoordinate - startCoordinate);
        return new PlanPoint(
                xAxis ? boundary : start.xMillimeters() + ratio * (end.xMillimeters() - start.xMillimeters()),
                xAxis ? start.yMillimeters() + ratio * (end.yMillimeters() - start.yMillimeters()) : boundary
        );
    }

    private List<PlanPoint> simplifyPolygon(List<PlanPoint> points) {
        List<PlanPoint> distinct = new ArrayList<>();
        for (PlanPoint point : points) {
            if (distinct.isEmpty() || distinct.getLast().distanceTo(point).toMillimeters() > EPSILON) {
                distinct.add(point);
            }
        }
        if (distinct.size() > 1 && distinct.getFirst().distanceTo(distinct.getLast()).toMillimeters() <= EPSILON) {
            distinct.removeLast();
        }
        boolean changed = true;
        while (changed && distinct.size() >= 3) {
            changed = false;
            for (int index = 0; index < distinct.size(); index++) {
                PlanPoint previous = distinct.get((index - 1 + distinct.size()) % distinct.size());
                PlanPoint current = distinct.get(index);
                PlanPoint next = distinct.get((index + 1) % distinct.size());
                double cross = (current.xMillimeters() - previous.xMillimeters()) * (next.yMillimeters() - current.yMillimeters())
                        - (current.yMillimeters() - previous.yMillimeters()) * (next.xMillimeters() - current.xMillimeters());
                if (Math.abs(cross) < EPSILON) {
                    distinct.remove(index);
                    changed = true;
                    break;
                }
            }
        }
        return List.copyOf(distinct);
    }

    private double roundedLength(List<PlanPoint> path, double requestedRadius) {
        double length = 0.0;
        for (int index = 1; index < path.size(); index++) {
            length += path.get(index - 1).distanceTo(path.get(index)).toMillimeters();
        }
        for (int index = 1; index + 1 < path.size(); index++) {
            PlanPoint previous = path.get(index - 1);
            PlanPoint current = path.get(index);
            PlanPoint next = path.get(index + 1);
            double firstLength = previous.distanceTo(current).toMillimeters();
            double secondLength = current.distanceTo(next).toMillimeters();
            double radius = Math.min(requestedRadius, Math.min(firstLength, secondLength) / 2.0);
            double firstX = current.xMillimeters() - previous.xMillimeters();
            double firstY = current.yMillimeters() - previous.yMillimeters();
            double secondX = next.xMillimeters() - current.xMillimeters();
            double secondY = next.yMillimeters() - current.yMillimeters();
            double denominator = Math.max(EPSILON, firstLength * secondLength);
            double cosine = Math.max(-1.0, Math.min(1.0, (firstX * secondX + firstY * secondY) / denominator));
            double angle = Math.acos(cosine);
            if (angle > EPSILON && angle < Math.PI - EPSILON) {
                length += radius * angle - 2.0 * radius * Math.tan(angle / 2.0);
            }
        }
        return Math.max(0.0, length);
    }

    private boolean containsPoint(List<PlanPoint> polygon, PlanPoint point) {
        boolean inside = false;
        int previousIndex = polygon.size() - 1;
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint current = polygon.get(index);
            PlanPoint previous = polygon.get(previousIndex);
            if (pointOnSegment(point, previous, current)) {
                return true;
            }
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

    private boolean pointOnSegment(PlanPoint point, PlanPoint start, PlanPoint end) {
        double cross = (point.xMillimeters() - start.xMillimeters()) * (end.yMillimeters() - start.yMillimeters())
                - (point.yMillimeters() - start.yMillimeters()) * (end.xMillimeters() - start.xMillimeters());
        if (Math.abs(cross) > EPSILON) {
            return false;
        }
        return point.xMillimeters() >= Math.min(start.xMillimeters(), end.xMillimeters()) - EPSILON
                && point.xMillimeters() <= Math.max(start.xMillimeters(), end.xMillimeters()) + EPSILON
                && point.yMillimeters() >= Math.min(start.yMillimeters(), end.yMillimeters()) - EPSILON
                && point.yMillimeters() <= Math.max(start.yMillimeters(), end.yMillimeters()) + EPSILON;
    }

    private PlanPoint centroid(List<PlanPoint> polygon) {
        return new PlanPoint(
                polygon.stream().mapToDouble(PlanPoint::xMillimeters).average().orElse(0.0),
                polygon.stream().mapToDouble(PlanPoint::yMillimeters).average().orElse(0.0)
        );
    }

    private Bounds bounds(List<PlanPoint> polygon) {
        return new Bounds(
                polygon.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0),
                polygon.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0),
                polygon.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0),
                polygon.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0)
        );
    }

    private double signedDoubleArea(List<PlanPoint> polygon) {
        double area = 0.0;
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint current = polygon.get(index);
            PlanPoint next = polygon.get((index + 1) % polygon.size());
            area += current.xMillimeters() * next.yMillimeters() - next.xMillimeters() * current.yMillimeters();
        }
        return area;
    }

    private void appendDistinct(List<PlanPoint> points, PlanPoint point) {
        if (points.isEmpty() || points.getLast().distanceTo(point).toMillimeters() > EPSILON) {
            points.add(point);
        }
    }

    private record Bounds(double minX, double maxX, double minY, double maxY) {
        private double width() {
            return maxX - minX;
        }

        private double height() {
            return maxY - minY;
        }
    }

    private record Interval(double start, double end) {
    }

    private record ShiftedLine(PlanPoint start, PlanPoint end) {
    }

    public record CircuitLayout(UUID zoneId, List<PlanPoint> pipePath, Length pipeLength, Length bendRadius) {
        public CircuitLayout {
            pipePath = List.copyOf(pipePath);
        }
    }

    public record PlanningResult(HydronicHeating heating, List<CircuitLayout> circuits) {
        public PlanningResult {
            circuits = List.copyOf(circuits);
        }
    }
}
