package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class WallSurfaceSideService {

    private static final double ROOM_TEST_OFFSET = 5.0;
    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();

    public WallLayerSides resolve(Level level, Wall wall, SurfaceType surfaceType, String targetKey) {
        Objects.requireNonNull(level, "level darf nicht null sein.");
        Objects.requireNonNull(wall, "wall darf nicht null sein.");
        Objects.requireNonNull(surfaceType, "surfaceType darf nicht null sein.");
        Objects.requireNonNull(targetKey, "targetKey darf nicht null sein.");
        return switch (surfaceType) {
            case WALL_INTERIOR -> resolveInterior(level, wall, targetKey);
            case WALL_EXTERIOR -> new WallLayerSides(!sideTouchesAnyRoom(level, wall, 1.0), !sideTouchesAnyRoom(level, wall, -1.0));
            default -> new WallLayerSides(true, true);
        };
    }

    public boolean hasInteriorSide(Level level, Wall wall, UUID roomId) {
        WallLayerSides sides = resolve(level, wall, SurfaceType.WALL_INTERIOR, WallSurfaceTargetKey.interior(wall.id(), roomId));
        return sides.positiveSide() || sides.negativeSide();
    }

    public boolean hasExteriorSide(Level level, Wall wall) {
        WallLayerSides sides = resolve(level, wall, SurfaceType.WALL_EXTERIOR, wall.id().toString());
        return sides.positiveSide() || sides.negativeSide();
    }

    private WallLayerSides resolveInterior(Level level, Wall wall, String targetKey) {
        double probeOffset = wall.thickness().toMillimeters() / 2.0
                + ROOM_TEST_OFFSET
                + surfaceLayerEffectService.maximumWallInteriorThicknessMillimeters(level, wall);
        return WallSurfaceTargetKey.roomId(targetKey)
                .map(roomId -> new WallLayerSides(
                        sideTouchesRoom(level.rooms(), wall, 1.0, roomId, probeOffset),
                        sideTouchesRoom(level.rooms(), wall, -1.0, roomId, probeOffset)
                ))
                .orElseGet(() -> new WallLayerSides(
                        sideTouchesRoom(level.rooms(), wall, 1.0, null, probeOffset),
                        sideTouchesRoom(level.rooms(), wall, -1.0, null, probeOffset)
                ));
    }

    private boolean sideTouchesAnyRoom(Level level, Wall wall, double sideSign) {
        double probeOffset = wall.thickness().toMillimeters() / 2.0
                + ROOM_TEST_OFFSET
                + surfaceLayerEffectService.maximumWallInteriorThicknessMillimeters(level, wall);
        return sideTouchesRoom(level.rooms(), wall, sideSign, null, probeOffset);
    }

    private boolean sideTouchesRoom(List<Room> rooms, Wall wall, double sideSign, UUID roomId, double probeOffset) {
        PlanPoint probePoint = probePoint(wall, sideSign, probeOffset);
        return rooms.stream()
                .filter(room -> roomId == null || room.id().equals(roomId))
                .anyMatch(room -> containsPoint(room.outline(), probePoint));
    }

    private PlanPoint probePoint(Wall wall, double sideSign, double probeOffset) {
        double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double length = Math.max(1.0, Math.hypot(dx, dy));
        double normalX = -dy / length;
        double normalY = dx / length;
        return new PlanPoint(
                (wall.axis().start().xMillimeters() + wall.axis().end().xMillimeters()) / 2.0 + normalX * probeOffset * sideSign,
                (wall.axis().start().yMillimeters() + wall.axis().end().yMillimeters()) / 2.0 + normalY * probeOffset * sideSign
        );
    }

    private boolean containsPoint(List<PlanPoint> outline, PlanPoint point) {
        boolean inside = false;
        int previousIndex = outline.size() - 1;
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint current = outline.get(index);
            PlanPoint previous = outline.get(previousIndex);
            boolean intersects = ((current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters()))
                    && (point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / ((previous.yMillimeters() - current.yMillimeters()) == 0.0 ? 1.0 : (previous.yMillimeters() - current.yMillimeters()))
                    + current.xMillimeters());
            if (intersects) {
                inside = !inside;
            }
            previousIndex = index;
        }
        return inside;
    }

    public record WallLayerSides(boolean positiveSide, boolean negativeSide) {
    }
}
