package de.schrell.cadas.application.layers;

import de.schrell.cadas.application.floor.FloorOpeningGeometryService;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;

import java.util.List;
import java.util.Objects;

public final class SurfaceLayerEffectService {

    private final FloorOpeningGeometryService floorOpeningGeometryService = new FloorOpeningGeometryService();

    public double visibleThicknessMillimeters(Level level, SurfaceType surfaceType, String targetKey) {
        return level.surfaceLayerStacks().stream()
                .filter(stack -> stack.surfaceType() == surfaceType)
                .filter(stack -> stack.targetKey().equals(targetKey))
                .findFirst()
                .map(SurfaceLayerStack::layers)
                .stream()
                .flatMap(List::stream)
                .filter(SurfaceLayer::visible)
                .mapToDouble(layer -> layer.thickness().toMillimeters())
                .sum();
    }

    public double wallInteriorThicknessMillimeters(Level level, Wall wall) {
        return maximumWallInteriorThicknessMillimeters(level, wall);
    }

    public double wallInteriorThicknessMillimeters(Level level, Wall wall, Room room) {
        Objects.requireNonNull(room, "room darf nicht null sein.");
        double roomSpecificThickness = level.surfaceLayerStacks().stream()
                .filter(stack -> stack.surfaceType() == SurfaceType.WALL_INTERIOR)
                .filter(stack -> WallSurfaceTargetKey.matchesWall(stack.targetKey(), wall.id()))
                .filter(stack -> WallSurfaceTargetKey.roomId(stack.targetKey()).map(room.id()::equals).orElse(false))
                .flatMap(stack -> stack.layers().stream())
                .filter(SurfaceLayer::visible)
                .mapToDouble(layer -> layer.thickness().toMillimeters())
                .sum();
        if (roomSpecificThickness > 0.0) {
            return roomSpecificThickness;
        }
        return visibleThicknessMillimeters(level, SurfaceType.WALL_INTERIOR, wall.id().toString());
    }

    public double maximumWallInteriorThicknessMillimeters(Level level, Wall wall) {
        double legacyThickness = visibleThicknessMillimeters(level, SurfaceType.WALL_INTERIOR, wall.id().toString());
        double roomSpecificMaximum = level.surfaceLayerStacks().stream()
                .filter(stack -> stack.surfaceType() == SurfaceType.WALL_INTERIOR)
                .filter(stack -> WallSurfaceTargetKey.matchesWall(stack.targetKey(), wall.id()))
                .mapToDouble(stack -> stack.layers().stream()
                        .filter(SurfaceLayer::visible)
                        .mapToDouble(layer -> layer.thickness().toMillimeters())
                        .sum())
                .max()
                .orElse(0.0);
        return Math.max(legacyThickness, roomSpecificMaximum);
    }

    public double wallExteriorThicknessMillimeters(Level level, Wall wall) {
        return visibleThicknessMillimeters(level, SurfaceType.WALL_EXTERIOR, wall.id().toString());
    }

    public double floorLayerThicknessMillimeters(Level level, Room room) {
        return visibleThicknessMillimeters(level, SurfaceType.FLOOR, room.id().toString());
    }

    public double ceilingLayerThicknessMillimeters(Level level, Room room) {
        return visibleThicknessMillimeters(level, SurfaceType.CEILING, room.id().toString());
    }

    public double effectiveHeightAt(Level level, Room room, PlanPoint point) {
        double effective = room.ceilingHeightAt(point) - floorLayerThicknessMillimeters(level, room) - ceilingLayerThicknessMillimeters(level, room);
        return Math.max(0.0, effective);
    }

    public double effectiveMinimumCeilingHeightMillimeters(Level level, Room room) {
        if (room.ceilingVertexHeightsProfile().isPresent()) {
            return room.outline().stream()
                    .mapToDouble(point -> effectiveHeightAt(level, room, point))
                    .min()
                    .orElse(0.0);
        }
        return Math.max(0.0, room.minimumCeilingHeightMillimeters() - floorLayerThicknessMillimeters(level, room) - ceilingLayerThicknessMillimeters(level, room));
    }

    public double effectiveMaximumCeilingHeightMillimeters(Level level, Room room) {
        if (room.ceilingVertexHeightsProfile().isPresent()) {
            return room.outline().stream()
                    .mapToDouble(point -> effectiveHeightAt(level, room, point))
                    .max()
                    .orElse(0.0);
        }
        return Math.max(0.0, room.maximumCeilingHeightMillimeters() - floorLayerThicknessMillimeters(level, room) - ceilingLayerThicknessMillimeters(level, room));
    }

    public double effectiveVolumeCubicMeters(Level level, Room room) {
        double heightReduction = floorLayerThicknessMillimeters(level, room)
                + ceilingLayerThicknessMillimeters(level, room);
        return floorOpeningGeometryService.floorRectangles(level, room).stream()
                .mapToDouble(rectangle -> room.volumeCubicMeters(List.of(
                        new PlanPoint(rectangle.minX(), rectangle.minY()),
                        new PlanPoint(rectangle.maxX(), rectangle.minY()),
                        new PlanPoint(rectangle.maxX(), rectangle.maxY()),
                        new PlanPoint(rectangle.minX(), rectangle.maxY())
                ), heightReduction))
                .sum();
    }

    public Length effectiveFloorThickness(Level level, Room room) {
        return Length.ofMillimeters(room.floorThickness().toMillimeters() + floorLayerThicknessMillimeters(level, room));
    }

    public double effectiveAreaSquareMeters(Level level, Room room) {
        return floorOpeningGeometryService.floorAreaSquareMeters(level, room);
    }

}
