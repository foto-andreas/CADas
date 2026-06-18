package de.andreas.cadas.application.drawing;

import de.andreas.cadas.application.layers.SurfaceLayerEffectService;
import de.andreas.cadas.application.layers.WallSurfaceSideService;
import de.andreas.cadas.application.layers.WallSurfaceTargetKey;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class WallDimensionService {

    private static final double EPSILON = 0.001;
    private static final double PARALLEL_TOLERANCE = Math.sin(Math.toRadians(0.5));
    private final WallSurfaceSideService wallSurfaceSideService = new WallSurfaceSideService();
    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();

    public WallDimensions dimensions(Level level, Wall wall) {
        Objects.requireNonNull(level, "level darf nicht null sein.");
        Objects.requireNonNull(wall, "wall darf nicht null sein.");
        List<SideDimension> roomDimensions = level.rooms().stream()
                .flatMap(room -> roomDimension(level, wall, room).stream())
                .sorted(Comparator.comparingDouble(SideDimension::sideSign).reversed())
                .toList();
        return new WallDimensions(roomDimensions, exteriorDimension(level, wall));
    }

    private Optional<SideDimension> roomDimension(Level level, Wall wall, Room room) {
        WallSurfaceSideService.WallLayerSides sides = wallSurfaceSideService.resolve(
                level,
                wall,
                SurfaceType.WALL_INTERIOR,
                WallSurfaceTargetKey.interior(wall.id(), room.id())
        );
        if (!sides.positiveSide() && !sides.negativeSide()) {
            return Optional.empty();
        }
        double maximumDistance = wall.thickness().toMillimeters() / 2.0
                + surfaceLayerEffectService.maximumWallInteriorThicknessMillimeters(level, wall)
                + 25.0;
        return outlineSegments(room).stream()
                .filter(segment -> isParallel(wall.axis(), segment))
                .filter(segment -> normalDistance(wall.axis(), segment) <= maximumDistance)
                .max(Comparator.comparingDouble(segment -> overlappingLength(wall.axis(), segment)))
                .filter(segment -> overlappingLength(wall.axis(), segment) > EPSILON)
                .map(segment -> new SideDimension(
                        room.name(),
                        Length.ofMillimeters(overlappingLength(wall.axis(), segment)),
                        sides.positiveSide() ? 1.0 : -1.0
                ));
    }

    private Optional<SideDimension> exteriorDimension(Level level, Wall wall) {
        WallSurfaceSideService.WallLayerSides sides = wallSurfaceSideService.resolve(
                level,
                wall,
                SurfaceType.WALL_EXTERIOR,
                wall.id().toString()
        );
        if (!sides.positiveSide() && !sides.negativeSide()) {
            return Optional.empty();
        }
        double length = wall.axis().length().toMillimeters()
                + endpointExtension(level, wall, wall.axis().start())
                + endpointExtension(level, wall, wall.axis().end());
        double sideSign = sides.positiveSide() && !sides.negativeSide() ? 1.0 : -1.0;
        return Optional.of(new SideDimension("Außen", Length.ofMillimeters(length), sideSign));
    }

    private double endpointExtension(Level level, Wall wall, PlanPoint endpoint) {
        return level.walls().stream()
                .filter(candidate -> !candidate.id().equals(wall.id()))
                .filter(candidate -> touches(candidate, endpoint))
                .mapToDouble(candidate -> extensionForAngle(wall, candidate))
                .max()
                .orElse(0.0);
    }

    private double extensionForAngle(Wall wall, Wall connectedWall) {
        Direction first = direction(wall.axis());
        Direction second = direction(connectedWall.axis());
        double sine = Math.abs(first.x() * second.y() - first.y() * second.x());
        if (sine < PARALLEL_TOLERANCE) {
            return 0.0;
        }
        return connectedWall.thickness().toMillimeters() / 2.0 / sine;
    }

    private boolean touches(Wall wall, PlanPoint point) {
        return wall.axis().start().distanceTo(point).toMillimeters() <= EPSILON
                || wall.axis().end().distanceTo(point).toMillimeters() <= EPSILON;
    }

    private List<PlanSegment> outlineSegments(Room room) {
        List<PlanSegment> segments = new ArrayList<>();
        for (int index = 0; index < room.outline().size(); index++) {
            segments.add(new PlanSegment(room.outline().get(index), room.outline().get((index + 1) % room.outline().size())));
        }
        return segments;
    }

    private boolean isParallel(PlanSegment first, PlanSegment second) {
        Direction firstDirection = direction(first);
        Direction secondDirection = direction(second);
        return Math.abs(firstDirection.x() * secondDirection.y() - firstDirection.y() * secondDirection.x()) <= PARALLEL_TOLERANCE;
    }

    private double normalDistance(PlanSegment wallAxis, PlanSegment roomEdge) {
        Direction direction = direction(wallAxis);
        PlanPoint midpoint = midpoint(roomEdge);
        double deltaX = midpoint.xMillimeters() - wallAxis.start().xMillimeters();
        double deltaY = midpoint.yMillimeters() - wallAxis.start().yMillimeters();
        return Math.abs(-direction.y() * deltaX + direction.x() * deltaY);
    }

    private double overlappingLength(PlanSegment wallAxis, PlanSegment roomEdge) {
        Direction direction = direction(wallAxis);
        double first = projection(wallAxis.start(), roomEdge.start(), direction);
        double second = projection(wallAxis.start(), roomEdge.end(), direction);
        double start = Math.max(0.0, Math.min(first, second));
        double end = Math.min(wallAxis.length().toMillimeters(), Math.max(first, second));
        return Math.max(0.0, end - start);
    }

    private double projection(PlanPoint origin, PlanPoint point, Direction direction) {
        return (point.xMillimeters() - origin.xMillimeters()) * direction.x()
                + (point.yMillimeters() - origin.yMillimeters()) * direction.y();
    }

    private PlanPoint midpoint(PlanSegment segment) {
        return new PlanPoint(
                (segment.start().xMillimeters() + segment.end().xMillimeters()) / 2.0,
                (segment.start().yMillimeters() + segment.end().yMillimeters()) / 2.0
        );
    }

    private Direction direction(PlanSegment segment) {
        double length = Math.max(EPSILON, segment.length().toMillimeters());
        return new Direction(
                (segment.end().xMillimeters() - segment.start().xMillimeters()) / length,
                (segment.end().yMillimeters() - segment.start().yMillimeters()) / length
        );
    }

    private record Direction(double x, double y) {
    }

    public record SideDimension(String name, Length length, double sideSign) {
        public SideDimension {
            Objects.requireNonNull(name, "name darf nicht null sein.");
            Objects.requireNonNull(length, "length darf nicht null sein.");
            if (sideSign != -1.0 && sideSign != 1.0) {
                throw new IllegalArgumentException("sideSign muss -1 oder 1 sein.");
            }
        }
    }

    public record WallDimensions(List<SideDimension> roomDimensions, Optional<SideDimension> exteriorDimension) {
        public WallDimensions {
            roomDimensions = List.copyOf(Objects.requireNonNull(roomDimensions, "roomDimensions darf nicht null sein."));
            Objects.requireNonNull(exteriorDimension, "exteriorDimension darf nicht null sein.");
        }
    }
}
