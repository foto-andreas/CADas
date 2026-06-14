package de.andreas.cadas.application.view;

import de.andreas.cadas.application.layers.WallSurfaceSideService;
import de.andreas.cadas.application.layers.WallSurfaceTargetKey;
import de.andreas.cadas.application.view.WallSurfaceOpeningService.WallSurfaceInterval;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class WallSurfacePlanGeometryService {

    private static final double EPSILON = 0.001;
    private static final double CORNER_DIRECTION_DOT_LIMIT = 0.5;

    private final WallSurfaceSideService wallSurfaceSideService = new WallSurfaceSideService();

    public WallSurfacePlanPolygon surfacePolygon(
            Level level,
            Wall wall,
            SurfaceLayerStack stack,
            SurfaceLayer layer,
            int layerIndex,
            double centerOffset,
            WallSurfaceInterval interval
    ) {
        Objects.requireNonNull(level, "level darf nicht null sein.");
        Objects.requireNonNull(wall, "wall darf nicht null sein.");
        Objects.requireNonNull(stack, "stack darf nicht null sein.");
        Objects.requireNonNull(layer, "layer darf nicht null sein.");
        Objects.requireNonNull(interval, "interval darf nicht null sein.");
        if (layerIndex < 0) {
            throw new IllegalArgumentException("layerIndex darf nicht negativ sein.");
        }
        double sideSign = centerOffset < 0.0 ? -1.0 : 1.0;
        double halfLayerThickness = layer.thickness().toMillimeters() / 2.0;
        double innerOffset = centerOffset - sideSign * halfLayerThickness;
        double outerOffset = centerOffset + sideSign * halfLayerThickness;
        double wallLength = wall.axis().length().toMillimeters();
        double startAdjustment = interval.startMillimeters() <= EPSILON
                ? endpointAdjustment(level, wall, stack, layerIndex, sideSign, WallEndpoint.START)
                : 0.0;
        double endAdjustment = interval.endMillimeters() >= wallLength - EPSILON
                ? endpointAdjustment(level, wall, stack, layerIndex, sideSign, WallEndpoint.END)
                : 0.0;
        return new WallSurfacePlanPolygon(List.of(
                wallOffsetPoint(wall, interval.startMillimeters() - startAdjustment, innerOffset),
                wallOffsetPoint(wall, interval.endMillimeters() + endAdjustment, innerOffset),
                wallOffsetPoint(wall, interval.endMillimeters() + endAdjustment, outerOffset),
                wallOffsetPoint(wall, interval.startMillimeters() - startAdjustment, outerOffset)
        ));
    }

    private double endpointAdjustment(
            Level level,
            Wall wall,
            SurfaceLayerStack stack,
            int layerIndex,
            double sideSign,
            WallEndpoint endpoint
    ) {
        PlanPoint corner = endpoint.point(wall);
        Direction ownSide = sideDirection(wall, sideSign);
        Direction ownExtension = endpoint.extensionDirection(wall);
        List<Double> adjustments = level.walls().stream()
                .filter(candidate -> !candidate.id().equals(wall.id()))
                .map(candidate -> adjustmentToConnectedSurface(level, candidate, stack, layerIndex, corner, ownSide, ownExtension))
                .filter(adjustment -> Math.abs(adjustment) > EPSILON)
                .toList();
        Optional<Double> extension = adjustments.stream()
                .filter(adjustment -> adjustment > 0.0)
                .max(Double::compare);
        if (extension.isPresent()) {
            return extension.get();
        }
        return adjustments.stream()
                .filter(adjustment -> adjustment < 0.0)
                .min(Double::compare)
                .orElse(0.0);
    }

    private double adjustmentToConnectedSurface(
            Level level,
            Wall candidate,
            SurfaceLayerStack sourceStack,
            int layerIndex,
            PlanPoint corner,
            Direction ownSide,
            Direction ownExtension
    ) {
        Optional<WallEndpoint> candidateEndpoint = endpointAt(candidate, corner);
        if (candidateEndpoint.isEmpty()) {
            return 0.0;
        }
        Direction candidateExtension = candidateEndpoint.get().extensionDirection(candidate);
        List<Double> targetSides = matchingTargetSides(level, candidate, sourceStack);
        double concaveExtension = level.surfaceLayerStacks().stream()
                .filter(candidateStack -> isCompatibleStack(sourceStack, candidateStack, candidate, layerIndex))
                .flatMapToDouble(candidateStack -> compatibleLayerSides(level, candidate, candidateStack).stream()
                        .filter(candidateSide -> isConcaveCorner(ownSide, ownExtension, candidateExtension, candidate, candidateSide))
                        .mapToDouble(candidateSide -> outerOffsetMagnitude(candidate, candidateStack, layerIndex)))
                .max()
                .orElse(0.0);
        if (concaveExtension > EPSILON) {
            return concaveExtension;
        }
        return targetSides.stream()
                .filter(candidateSide -> isConvexCorner(ownSide, ownExtension, candidateExtension, candidate, candidateSide))
                .mapToDouble(ignored -> -candidate.thickness().toMillimeters() / 2.0)
                .min()
                .orElse(0.0);
    }

    private List<Double> matchingTargetSides(Level level, Wall wall, SurfaceLayerStack sourceStack) {
        WallSurfaceSideService.WallLayerSides sides = wallSurfaceSideService.resolve(
                level,
                wall,
                sourceStack.surfaceType(),
                matchingTargetKey(sourceStack, wall)
        );
        if (sides.positiveSide() && sides.negativeSide()) {
            return List.of(1.0, -1.0);
        }
        if (sides.positiveSide()) {
            return List.of(1.0);
        }
        if (sides.negativeSide()) {
            return List.of(-1.0);
        }
        return List.of();
    }

    private String matchingTargetKey(SurfaceLayerStack sourceStack, Wall wall) {
        if (sourceStack.surfaceType() != SurfaceType.WALL_INTERIOR) {
            return wall.id().toString();
        }
        return WallSurfaceTargetKey.roomId(sourceStack.targetKey())
                .map(roomId -> WallSurfaceTargetKey.interior(wall.id(), roomId))
                .orElseGet(() -> wall.id().toString());
    }

    private List<Double> compatibleLayerSides(Level level, Wall wall, SurfaceLayerStack stack) {
        WallSurfaceSideService.WallLayerSides sides = wallSurfaceSideService.resolve(level, wall, stack.surfaceType(), stack.targetKey());
        if (sides.positiveSide() && sides.negativeSide()) {
            return List.of(1.0, -1.0);
        }
        if (sides.positiveSide()) {
            return List.of(1.0);
        }
        if (sides.negativeSide()) {
            return List.of(-1.0);
        }
        return List.of();
    }

    private boolean isCompatibleStack(SurfaceLayerStack sourceStack, SurfaceLayerStack candidateStack, Wall candidate, int layerIndex) {
        if (candidateStack.surfaceType() != sourceStack.surfaceType()
                || !WallSurfaceTargetKey.matchesWall(candidateStack.targetKey(), candidate.id())
                || candidateStack.layers().size() <= layerIndex
                || !candidateStack.layers().get(layerIndex).visible()
                || candidateStack.layers().get(layerIndex).thickness().toMillimeters() <= EPSILON) {
            return false;
        }
        if (sourceStack.surfaceType() != SurfaceType.WALL_INTERIOR) {
            return sourceStack.surfaceType() == SurfaceType.WALL_EXTERIOR;
        }
        Optional<UUID> sourceRoomId = WallSurfaceTargetKey.roomId(sourceStack.targetKey());
        Optional<UUID> candidateRoomId = WallSurfaceTargetKey.roomId(candidateStack.targetKey());
        return sourceRoomId.isPresent()
                ? candidateRoomId.map(sourceRoomId.get()::equals).orElse(false)
                : candidateRoomId.isEmpty();
    }

    private boolean isConcaveCorner(
            Direction ownSide,
            Direction ownExtension,
            Direction candidateExtension,
            Wall candidate,
            double candidateSideSign
    ) {
        Direction candidateSide = sideDirection(candidate, candidateSideSign);
        return ownExtension.dot(candidateSide) > CORNER_DIRECTION_DOT_LIMIT
                && candidateExtension.dot(ownSide) > CORNER_DIRECTION_DOT_LIMIT;
    }

    private boolean isConvexCorner(
            Direction ownSide,
            Direction ownExtension,
            Direction candidateExtension,
            Wall candidate,
            double candidateSideSign
    ) {
        Direction candidateSide = sideDirection(candidate, candidateSideSign);
        return ownExtension.dot(candidateSide) < -CORNER_DIRECTION_DOT_LIMIT
                && candidateExtension.dot(ownSide) < -CORNER_DIRECTION_DOT_LIMIT;
    }

    private Optional<WallEndpoint> endpointAt(Wall wall, PlanPoint point) {
        if (samePoint(wall.axis().start(), point)) {
            return Optional.of(WallEndpoint.START);
        }
        if (samePoint(wall.axis().end(), point)) {
            return Optional.of(WallEndpoint.END);
        }
        return Optional.empty();
    }

    private boolean samePoint(PlanPoint first, PlanPoint second) {
        return Math.abs(first.xMillimeters() - second.xMillimeters()) <= EPSILON
                && Math.abs(first.yMillimeters() - second.yMillimeters()) <= EPSILON;
    }

    private double outerOffsetMagnitude(Wall wall, SurfaceLayerStack stack, int layerIndex) {
        double thickness = wall.thickness().toMillimeters() / 2.0;
        for (int index = 0; index <= layerIndex && index < stack.layers().size(); index++) {
            thickness += stack.layers().get(index).thickness().toMillimeters();
        }
        return thickness;
    }

    private Direction sideDirection(Wall wall, double sideSign) {
        Direction tangent = tangent(wall);
        return new Direction(-tangent.y() * sideSign, tangent.x() * sideSign);
    }

    private PlanPoint wallOffsetPoint(Wall wall, double localDistance, double normalOffset) {
        Direction tangent = tangent(wall);
        double normalX = -tangent.y();
        double normalY = tangent.x();
        return new PlanPoint(
                wall.axis().start().xMillimeters() + tangent.x() * localDistance + normalX * normalOffset,
                wall.axis().start().yMillimeters() + tangent.y() * localDistance + normalY * normalOffset
        );
    }

    private Direction tangent(Wall wall) {
        double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double length = Math.max(EPSILON, Math.hypot(dx, dy));
        return new Direction(dx / length, dy / length);
    }

    private enum WallEndpoint {
        START {
            @Override
            PlanPoint point(Wall wall) {
                return wall.axis().start();
            }

            @Override
            Direction extensionDirection(Wall wall) {
                Direction tangent = tangentDirection(wall);
                return new Direction(-tangent.x(), -tangent.y());
            }
        },
        END {
            @Override
            PlanPoint point(Wall wall) {
                return wall.axis().end();
            }

            @Override
            Direction extensionDirection(Wall wall) {
                return tangentDirection(wall);
            }
        };

        abstract PlanPoint point(Wall wall);

        abstract Direction extensionDirection(Wall wall);

        private static Direction tangentDirection(Wall wall) {
            double dx = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
            double dy = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
            double length = Math.max(EPSILON, Math.hypot(dx, dy));
            return new Direction(dx / length, dy / length);
        }
    }

    private record Direction(double x, double y) {

        double dot(Direction other) {
            return x * other.x + y * other.y;
        }
    }

    public record WallSurfacePlanPolygon(List<PlanPoint> points) {

        public WallSurfacePlanPolygon {
            points = List.copyOf(Objects.requireNonNull(points, "points darf nicht null sein."));
            if (points.size() != 4) {
                throw new IllegalArgumentException("Ein Wandbelag in Draufsicht benötigt genau vier Eckpunkte.");
            }
        }
    }
}
