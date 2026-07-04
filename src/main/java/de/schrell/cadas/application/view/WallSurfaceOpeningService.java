package de.schrell.cadas.application.view;

import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;
import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

public final class WallSurfaceOpeningService {

    private static final double EPSILON = 0.001;
    private final WallSurfaceRoomIntervalService wallSurfaceRoomIntervalService = new WallSurfaceRoomIntervalService();

    public List<WallSurfaceRectangle> visibleRectangles(Level level, Wall wall) {
        return visibleRectangles(level, wall, null, null);
    }

    public List<WallSurfaceRectangle> visibleRectangles(Level level, Wall wall, double sideSign) {
        return visibleRectangles(level, wall, sideSign, null);
    }

    public List<WallSurfaceRectangle> visibleRectangles(Level level, Wall wall, double sideSign, UUID roomId) {
        return visibleRectangles(level, wall, Double.valueOf(sideSign < 0.0 ? -1.0 : 1.0), roomId);
    }

    private List<WallSurfaceRectangle> visibleRectangles(Level level, Wall wall, Double sideSign, UUID roomId) {
        double wallLength = wall.axis().length().toMillimeters();
        double wallHeight = wall.maximumHeightMillimeters();
        if (wallLength <= EPSILON || wallHeight <= EPSILON) {
            return List.of();
        }
        List<WallOpeningRectangle> openings = openingRectangles(level, wall);
        if (sideSign != null) {
            openings = withWallInterruptions(level, wall, sideSign, openings);
        }
        List<WallSurfaceRectangle> merged;
        if (openings.isEmpty()) {
            merged = List.of(new WallSurfaceRectangle(0.0, wallLength, 0.0, wallHeight));
        } else {
            TreeSet<Double> heightCuts = new TreeSet<>();
            heightCuts.add(0.0);
            heightCuts.add(wallHeight);
            for (WallOpeningRectangle opening : openings) {
                heightCuts.add(opening.lowerHeightMillimeters());
                heightCuts.add(opening.upperHeightMillimeters());
            }

            List<Double> heights = new ArrayList<>(heightCuts);
            List<WallSurfaceRectangle> bands = new ArrayList<>();
            for (int index = 0; index < heights.size() - 1; index++) {
                double lower = heights.get(index);
                double upper = heights.get(index + 1);
                if (upper - lower <= EPSILON) {
                    continue;
                }
                addVisibleBandRectangles(bands, openings, wallLength, lower, upper);
            }
            merged = mergeVertically(bands);
        }
        if (sideSign == null || roomId == null) {
            return merged;
        }
        return clipRectanglesToIntervals(merged, wallSurfaceRoomIntervalService.intervals(level, wall, roomId, sideSign));
    }

    public List<WallSurfaceInterval> visiblePlanIntervals(Level level, Wall wall) {
        return visiblePlanIntervals(level, wall, null, null);
    }

    public List<WallSurfaceInterval> visiblePlanIntervals(Level level, Wall wall, double sideSign) {
        return visiblePlanIntervals(level, wall, sideSign, null);
    }

    public List<WallSurfaceInterval> visiblePlanIntervals(Level level, Wall wall, double sideSign, UUID roomId) {
        return visiblePlanIntervals(level, wall, Double.valueOf(sideSign < 0.0 ? -1.0 : 1.0), roomId);
    }

    private List<WallSurfaceInterval> visiblePlanIntervals(Level level, Wall wall, Double sideSign, UUID roomId) {
        double wallLength = wall.axis().length().toMillimeters();
        if (wallLength <= EPSILON) {
            return List.of();
        }
        List<WallOpeningRectangle> openings = openingRectangles(level, wall);
        if (sideSign != null) {
            openings = withWallInterruptions(level, wall, sideSign, openings);
        }
        List<WallSurfaceInterval> intervals;
        if (openings.isEmpty()) {
            intervals = List.of(new WallSurfaceInterval(0.0, wallLength));
        } else {
            List<WallSurfaceInterval> mergedOpenings = mergeOpeningIntervals(openings);
            intervals = new ArrayList<>();
            double cursor = 0.0;
            for (WallSurfaceInterval opening : mergedOpenings) {
                if (opening.startMillimeters() - cursor > EPSILON) {
                    intervals.add(new WallSurfaceInterval(cursor, opening.startMillimeters()));
                }
                cursor = Math.max(cursor, opening.endMillimeters());
            }
            if (wallLength - cursor > EPSILON) {
                intervals.add(new WallSurfaceInterval(cursor, wallLength));
            }
        }
        if (sideSign == null || roomId == null) {
            return List.copyOf(intervals);
        }
        return intersectIntervals(intervals, wallSurfaceRoomIntervalService.intervals(level, wall, roomId, sideSign));
    }

    public List<WallOpeningRectangle> openingRectangles(Level level, Wall wall) {
        double wallLength = wall.axis().length().toMillimeters();
        double wallHeight = wall.maximumHeightMillimeters();
        if (wallLength <= EPSILON || wallHeight <= EPSILON) {
            return List.of();
        }
        List<WallOpeningRectangle> openings = new ArrayList<>();
        for (Door door : level.doors()) {
            if (!door.wallId().equals(wall.id())) {
                continue;
            }
            addOpening(openings, door.offsetFromStart().toMillimeters(), door.offsetFromStart().add(door.width()).toMillimeters(), 0.0, door.height().toMillimeters(), wallLength, wallHeight);
        }
        for (WindowElement window : level.windows()) {
            if (!window.wallId().equals(wall.id())) {
                continue;
            }
            addOpening(openings, window.offsetFromStart().toMillimeters(), window.offsetFromStart().add(window.width()).toMillimeters(), window.sillHeight().toMillimeters(), window.sillHeight().toMillimeters() + window.windowHeight().toMillimeters(), wallLength, wallHeight);
        }
        openings.sort(Comparator.comparingDouble(WallOpeningRectangle::startMillimeters)
                .thenComparingDouble(WallOpeningRectangle::lowerHeightMillimeters));
        return List.copyOf(openings);
    }

    private List<WallOpeningRectangle> withWallInterruptions(
            Level level,
            Wall wall,
            double sideSign,
            List<WallOpeningRectangle> openings
    ) {
        List<WallOpeningRectangle> allOpenings = new ArrayList<>(openings);
        addWallInterruptions(allOpenings, level, wall, sideSign);
        allOpenings.sort(Comparator.comparingDouble(WallOpeningRectangle::startMillimeters)
                .thenComparingDouble(WallOpeningRectangle::lowerHeightMillimeters));
        return List.copyOf(allOpenings);
    }

    private void addWallInterruptions(List<WallOpeningRectangle> openings, Level level, Wall wall, double sideSign) {
        double wallLength = wall.axis().length().toMillimeters();
        double wallHeight = wall.maximumHeightMillimeters();
        double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double length = Math.max(EPSILON, Math.hypot(dx, dy));
        double tangentX = dx / length;
        double tangentY = dy / length;
        double normalX = -tangentY;
        double normalY = tangentX;
        for (Wall candidate : level.walls()) {
            if (candidate.id().equals(wall.id())) {
                continue;
            }
            addWallInterruption(openings, wall, candidate, candidate.axis().start(), candidate.axis().end(), sideSign, tangentX, tangentY, normalX, normalY, wallLength, wallHeight);
            addWallInterruption(openings, wall, candidate, candidate.axis().end(), candidate.axis().start(), sideSign, tangentX, tangentY, normalX, normalY, wallLength, wallHeight);
        }
    }

    private void addWallInterruption(
            List<WallOpeningRectangle> openings,
            Wall wall,
            Wall candidate,
            PlanPoint touchPoint,
            PlanPoint oppositePoint,
            double sideSign,
            double tangentX,
            double tangentY,
            double normalX,
            double normalY,
            double wallLength,
            double wallHeight
    ) {
        if (wall.axis().distanceTo(touchPoint).toMillimeters() > EPSILON) {
            return;
        }
        double localPosition = wall.axis().projectedLength(touchPoint).toMillimeters();
        if (localPosition <= EPSILON || localPosition >= wallLength - EPSILON) {
            return;
        }
        double candidateDx = oppositePoint.xMillimeters() - touchPoint.xMillimeters();
        double candidateDy = oppositePoint.yMillimeters() - touchPoint.yMillimeters();
        double candidateLength = Math.hypot(candidateDx, candidateDy);
        if (candidateLength <= EPSILON) {
            return;
        }
        double candidateTangentX = candidateDx / candidateLength;
        double candidateTangentY = candidateDy / candidateLength;
        double sideProjection = (candidateTangentX * normalX + candidateTangentY * normalY) * sideSign;
        if (sideProjection <= EPSILON) {
            return;
        }
        double sine = Math.abs(tangentX * candidateTangentY - tangentY * candidateTangentX);
        if (sine <= EPSILON) {
            return;
        }
        double halfWidth = candidate.thickness().toMillimeters() / 2.0 / sine;
        double candidateHeight = Math.min(wallHeight, candidate.heightAt(candidate.axis().projectedLength(touchPoint).toMillimeters()));
        addOpening(openings, localPosition - halfWidth, localPosition + halfWidth, 0.0, candidateHeight, wallLength, wallHeight);
    }

    private void addOpening(
            List<WallOpeningRectangle> openings,
            double start,
            double end,
            double lower,
            double upper,
            double wallLength,
            double wallHeight
    ) {
        double clippedStart = Math.clamp(start, 0.0, wallLength);
        double clippedEnd = Math.clamp(end, 0.0, wallLength);
        double clippedLower = Math.clamp(lower, 0.0, wallHeight);
        double clippedUpper = Math.clamp(upper, 0.0, wallHeight);
        if (clippedEnd - clippedStart > EPSILON && clippedUpper - clippedLower > EPSILON) {
            openings.add(new WallOpeningRectangle(clippedStart, clippedEnd, clippedLower, clippedUpper));
        }
    }

    private void addVisibleBandRectangles(
            List<WallSurfaceRectangle> rectangles,
            List<WallOpeningRectangle> openings,
            double wallLength,
            double lower,
            double upper
    ) {
        List<WallOpeningRectangle> blockingOpenings = openings.stream()
                .filter(opening -> opening.upperHeightMillimeters() > lower + EPSILON)
                .filter(opening -> opening.lowerHeightMillimeters() < upper - EPSILON)
                .sorted(Comparator.comparingDouble(WallOpeningRectangle::startMillimeters))
                .toList();
        double cursor = 0.0;
        for (WallOpeningRectangle opening : blockingOpenings) {
            if (opening.startMillimeters() - cursor > EPSILON) {
                rectangles.add(new WallSurfaceRectangle(cursor, opening.startMillimeters(), lower, upper));
            }
            cursor = Math.max(cursor, opening.endMillimeters());
        }
        if (wallLength - cursor > EPSILON) {
            rectangles.add(new WallSurfaceRectangle(cursor, wallLength, lower, upper));
        }
    }

    private List<WallSurfaceRectangle> mergeVertically(List<WallSurfaceRectangle> rectangles) {
        List<WallSurfaceRectangle> merged = new ArrayList<>();
        for (WallSurfaceRectangle rectangle : rectangles) {
            int mergeIndex = findVerticalMergeCandidate(merged, rectangle);
            if (mergeIndex >= 0) {
                WallSurfaceRectangle previous = merged.remove(mergeIndex);
                merged.add(new WallSurfaceRectangle(
                        previous.startMillimeters(),
                        previous.endMillimeters(),
                        previous.lowerHeightMillimeters(),
                        rectangle.upperHeightMillimeters()
                ));
            } else {
                merged.add(rectangle);
            }
        }
        return List.copyOf(merged);
    }

    private List<WallSurfaceRectangle> clipRectanglesToIntervals(
            List<WallSurfaceRectangle> rectangles,
            List<WallSurfaceInterval> intervals
    ) {
        if (intervals.isEmpty()) {
            return List.of();
        }
        List<WallSurfaceRectangle> clipped = new ArrayList<>();
        for (WallSurfaceRectangle rectangle : rectangles) {
            for (WallSurfaceInterval interval : intervals) {
                double start = Math.max(rectangle.startMillimeters(), interval.startMillimeters());
                double end = Math.min(rectangle.endMillimeters(), interval.endMillimeters());
                if (end - start > EPSILON) {
                    clipped.add(new WallSurfaceRectangle(
                            start,
                            end,
                            rectangle.lowerHeightMillimeters(),
                            rectangle.upperHeightMillimeters()
                    ));
                }
            }
        }
        return List.copyOf(clipped);
    }

    private List<WallSurfaceInterval> intersectIntervals(
            List<WallSurfaceInterval> visibleIntervals,
            List<WallSurfaceInterval> allowedIntervals
    ) {
        if (allowedIntervals.isEmpty()) {
            return List.of();
        }
        List<WallSurfaceInterval> result = new ArrayList<>();
        for (WallSurfaceInterval visible : visibleIntervals) {
            for (WallSurfaceInterval allowed : allowedIntervals) {
                double start = Math.max(visible.startMillimeters(), allowed.startMillimeters());
                double end = Math.min(visible.endMillimeters(), allowed.endMillimeters());
                if (end - start > EPSILON) {
                    result.add(new WallSurfaceInterval(start, end));
                }
            }
        }
        return List.copyOf(result);
    }

    private int findVerticalMergeCandidate(List<WallSurfaceRectangle> rectangles, WallSurfaceRectangle rectangle) {
        for (int index = rectangles.size() - 1; index >= 0; index--) {
            if (rectangles.get(index).touchesVertically(rectangle)) {
                return index;
            }
        }
        return -1;
    }

    private List<WallSurfaceInterval> mergeOpeningIntervals(List<WallOpeningRectangle> openings) {
        List<WallSurfaceInterval> merged = new ArrayList<>();
        for (WallOpeningRectangle opening : openings.stream()
                .sorted(Comparator.comparingDouble(WallOpeningRectangle::startMillimeters))
                .toList()) {
            if (merged.isEmpty() || opening.startMillimeters() > merged.getLast().endMillimeters() + EPSILON) {
                merged.add(new WallSurfaceInterval(opening.startMillimeters(), opening.endMillimeters()));
                continue;
            }
            WallSurfaceInterval previous = merged.removeLast();
            merged.add(new WallSurfaceInterval(
                    previous.startMillimeters(),
                    Math.max(previous.endMillimeters(), opening.endMillimeters())
            ));
        }
        return merged;
    }

    public record WallSurfaceRectangle(
            double startMillimeters,
            double endMillimeters,
            double lowerHeightMillimeters,
            double upperHeightMillimeters
    ) {

        public double widthMillimeters() {
            return endMillimeters - startMillimeters;
        }

        public double heightMillimeters() {
            return upperHeightMillimeters - lowerHeightMillimeters;
        }

        boolean touchesVertically(WallSurfaceRectangle other) {
            return Math.abs(startMillimeters - other.startMillimeters) <= EPSILON
                    && Math.abs(endMillimeters - other.endMillimeters) <= EPSILON
                    && Math.abs(upperHeightMillimeters - other.lowerHeightMillimeters) <= EPSILON;
        }
    }

    public record WallSurfaceInterval(double startMillimeters, double endMillimeters) {
    }

    public record WallOpeningRectangle(
            double startMillimeters,
            double endMillimeters,
            double lowerHeightMillimeters,
            double upperHeightMillimeters
    ) {
    }
}
