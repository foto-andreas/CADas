package de.andreas.cadas.application.layers;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SurfaceLayerEffectService {

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
        if (room.ceilingVertexHeightsProfile().isPresent()) {
            List<Double> heights = new ArrayList<>();
            for (PlanPoint point : room.outline()) {
                heights.add(effectiveHeightAt(level, room, point));
            }
            double centerHeight = effectiveHeightAt(level, room, room.centerPoint());
            double volumeMillimeters = 0.0;
            for (int index = 0; index < room.outline().size(); index++) {
                PlanPoint current = room.outline().get(index);
                PlanPoint next = room.outline().get((index + 1) % room.outline().size());
                double triangleArea = triangleArea(room.centerPoint(), current, next);
                double averageHeight = (centerHeight + heights.get(index) + heights.get((index + 1) % heights.size())) / 3.0;
                volumeMillimeters += triangleArea * averageHeight;
            }
            return volumeMillimeters / 1_000_000_000.0;
        }
        double averageHeight = room.slopedCeilingProfile().isPresent()
                ? (effectiveMinimumCeilingHeightMillimeters(level, room) + effectiveMaximumCeilingHeightMillimeters(level, room)) / 2.0
                : effectiveMaximumCeilingHeightMillimeters(level, room);
        return effectiveAreaSquareMeters(level, room) * averageHeight / 1000.0;
    }

    public Length effectiveFloorThickness(Level level, Room room) {
        return Length.ofMillimeters(room.floorThickness().toMillimeters() + floorLayerThicknessMillimeters(level, room));
    }

    public double effectiveAreaSquareMeters(Level level, Room room) {
        return room.areaSquareMeters();
    }

    public double effectiveAverageHeightMillimeters(Level level, Room room) {
        double effectiveAreaSquareMeters = effectiveAreaSquareMeters(level, room);
        if (effectiveAreaSquareMeters <= 0.0) {
            return 0.0;
        }
        if (!room.hasVariableCeilingHeights()) {
            return effectiveMaximumCeilingHeightMillimeters(level, room);
        }
        double areaSquareMillimeters = effectiveAreaSquareMeters * 1_000_000.0;
        if (areaSquareMillimeters <= 0.0) {
            return 0.0;
        }
        return effectiveVolumeCubicMeters(level, room) * 1_000_000_000.0 / areaSquareMillimeters;
    }

    private double triangleArea(PlanPoint a, PlanPoint b, PlanPoint c) {
        return Math.abs(
                a.xMillimeters() * (b.yMillimeters() - c.yMillimeters())
                        + b.xMillimeters() * (c.yMillimeters() - a.yMillimeters())
                        + c.xMillimeters() * (a.yMillimeters() - b.yMillimeters())
        ) / 2.0;
    }
}
