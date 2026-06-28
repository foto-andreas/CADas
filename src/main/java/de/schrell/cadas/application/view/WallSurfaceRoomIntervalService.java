package de.schrell.cadas.application.view;

import de.schrell.cadas.application.layers.SurfaceLayerEffectService;
import de.schrell.cadas.application.view.WallSurfaceOpeningService.WallSurfaceInterval;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ermittelt die Teilintervalle einer Wandseite, an denen ein konkreter Raum
 * tatsächlich anliegt.
 */
public final class WallSurfaceRoomIntervalService {

    private static final double EPSILON = 0.001;
    private static final double ROOM_PROBE_OFFSET = 5.0;

    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();

    public List<WallSurfaceInterval> intervals(Level level, Wall wall, UUID roomId, double sideSign) {
        Optional<Room> room = level.rooms().stream()
                .filter(candidate -> candidate.id().equals(roomId))
                .findFirst();
        if (room.isEmpty()) {
            return List.of();
        }
        double wallLength = wall.axis().length().toMillimeters();
        if (wallLength <= EPSILON) {
            return List.of();
        }
        double maxDistance = wall.thickness().toMillimeters() / 2.0
                + surfaceLayerEffectService.maximumWallInteriorThicknessMillimeters(level, wall)
                + ROOM_PROBE_OFFSET;
        List<WallSurfaceInterval> intervals = new ArrayList<>();
        List<PlanPoint> outline = room.get().outline();
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint start = outline.get(index);
            PlanPoint end = outline.get((index + 1) % outline.size());
            if (!isParallelToWall(wall, start, end)) {
                continue;
            }
            PlanPoint midpoint = new PlanPoint(
                    (start.xMillimeters() + end.xMillimeters()) / 2.0,
                    (start.yMillimeters() + end.yMillimeters()) / 2.0
            );
            if (wall.axis().distanceTo(midpoint).toMillimeters() > maxDistance + EPSILON) {
                continue;
            }
            if (!touchesRequestedSide(room.get(), wall, midpoint, sideSign)) {
                continue;
            }
            double localStart = projectedLengthOnWall(wall, start);
            double localEnd = projectedLengthOnWall(wall, end);
            double clippedStart = Math.max(0.0, Math.min(localStart, localEnd));
            double clippedEnd = Math.min(wallLength, Math.max(localStart, localEnd));
            if (clippedEnd - clippedStart > EPSILON) {
                intervals.add(new WallSurfaceInterval(clippedStart, clippedEnd));
            }
        }
        return merge(intervals);
    }

    private boolean isParallelToWall(Wall wall, PlanPoint start, PlanPoint end) {
        double wallDx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double wallDy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double edgeDx = end.xMillimeters() - start.xMillimeters();
        double edgeDy = end.yMillimeters() - start.yMillimeters();
        double wallLength = Math.hypot(wallDx, wallDy);
        double edgeLength = Math.hypot(edgeDx, edgeDy);
        if (wallLength <= EPSILON || edgeLength <= EPSILON) {
            return false;
        }
        double cross = Math.abs(wallDx * edgeDy - wallDy * edgeDx) / (wallLength * edgeLength);
        return cross <= 0.01;
    }

    private boolean touchesRequestedSide(Room room, Wall wall, PlanPoint midpoint, double sideSign) {
        double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double length = Math.max(EPSILON, Math.hypot(dx, dy));
        double normalX = -dy / length;
        double normalY = dx / length;
        PlanPoint probe = new PlanPoint(
                midpoint.xMillimeters() + normalX * ROOM_PROBE_OFFSET * sideSign,
                midpoint.yMillimeters() + normalY * ROOM_PROBE_OFFSET * sideSign
        );
        return containsPoint(room.outline(), probe);
    }

    private double projectedLengthOnWall(Wall wall, PlanPoint point) {
        double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double wallLength = Math.max(EPSILON, Math.hypot(dx, dy));
        return ((point.xMillimeters() - wall.axis().start().xMillimeters()) * dx
                + (point.yMillimeters() - wall.axis().start().yMillimeters()) * dy) / wallLength;
    }

    private List<WallSurfaceInterval> merge(List<WallSurfaceInterval> intervals) {
        if (intervals.isEmpty()) {
            return List.of();
        }
        List<WallSurfaceInterval> sorted = intervals.stream()
                .sorted(Comparator.comparingDouble(WallSurfaceInterval::startMillimeters))
                .toList();
        List<WallSurfaceInterval> merged = new ArrayList<>();
        WallSurfaceInterval current = sorted.getFirst();
        for (int index = 1; index < sorted.size(); index++) {
            WallSurfaceInterval next = sorted.get(index);
            if (next.startMillimeters() <= current.endMillimeters() + EPSILON) {
                current = new WallSurfaceInterval(
                        current.startMillimeters(),
                        Math.max(current.endMillimeters(), next.endMillimeters())
                );
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return List.copyOf(merged);
    }

    private boolean containsPoint(List<PlanPoint> outline, PlanPoint point) {
        boolean inside = false;
        int previousIndex = outline.size() - 1;
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint current = outline.get(index);
            PlanPoint previous = outline.get(previousIndex);
            boolean intersects = ((current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters()))
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / ((previous.yMillimeters() - current.yMillimeters()) == 0.0 ? 1.0 : (previous.yMillimeters() - current.yMillimeters()))
                    + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            previousIndex = index;
        }
        return inside;
    }
}
