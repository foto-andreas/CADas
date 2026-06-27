package de.schrell.cadas.application.drawing;

import de.schrell.cadas.application.layers.SurfaceLayerEffectService;
import de.schrell.cadas.application.layers.WallSurfaceSideService;
import de.schrell.cadas.application.layers.WallSurfaceTargetKey;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;

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
                        segment.length(),
                        sides.positiveSide() ? 1.0 : -1.0,
                        segment,
                        "Raum:" + room.id()
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
        double sideSign = sides.positiveSide() && !sides.negativeSide() ? 1.0 : -1.0;
        PlanSegment segment = exteriorSegment(level, wall, sideSign);
        return Optional.of(new SideDimension("Außen", segment.length(), sideSign, segment, "Wand:" + wall.id()));
    }

    private PlanSegment exteriorSegment(Level level, Wall wall, double sideSign) {
        double offset = wall.thickness().toMillimeters() / 2.0
                + surfaceLayerEffectService.wallExteriorThicknessMillimeters(level, wall);
        PlanSegment shifted = shiftedSegment(wall.axis(), offset, sideSign);
        PlanPoint start = exteriorEndpoint(level, wall, wall.axis().start(), shifted, sideSign);
        PlanPoint end = exteriorEndpoint(level, wall, wall.axis().end(), shifted, sideSign);
        return new PlanSegment(start, end);
    }

    private PlanPoint exteriorEndpoint(Level level, Wall wall, PlanPoint axisEndpoint, PlanSegment shifted, double sideSign) {
        Direction direction = direction(wall.axis());
        double normalX = -direction.y() * sideSign;
        double normalY = direction.x() * sideSign;
        boolean isStart = axisEndpoint.distanceTo(wall.axis().start()).toMillimeters() <= EPSILON;
        PlanPoint best = isStart ? shifted.start() : shifted.end();
        double bestProjection = projection(wall.axis().start(), best, direction);
        PlanSegment extendedLine = new PlanSegment(
                new PlanPoint(best.xMillimeters() - direction.x() * 10_000_000.0, best.yMillimeters() - direction.y() * 10_000_000.0),
                new PlanPoint(best.xMillimeters() + direction.x() * 10_000_000.0, best.yMillimeters() + direction.y() * 10_000_000.0)
        );
        for (Wall connected : level.walls()) {
            if (connected.id().equals(wall.id()) || !touches(connected, axisEndpoint)) {
                continue;
            }
            // Fast kollineare Anschlusswände erzeugen numerisch instabile Fernschnitte.
            if (isParallel(wall.axis(), connected.axis())) {
                continue;
            }
            for (int connectedSide : new int[]{-1, 1}) {
                PlanSegment candidate = shiftedSegment(connected.axis(), connected.thickness().toMillimeters() / 2.0, connectedSide);
                Optional<PlanPoint> intersection = lineIntersection(extendedLine.start(), extendedLine.end(), candidate.start(), candidate.end());
                if (intersection.isPresent()) {
                    double projection = projection(wall.axis().start(), intersection.get(), direction);
                    double outward = (intersection.get().xMillimeters() - axisEndpoint.xMillimeters()) * normalX
                            + (intersection.get().yMillimeters() - axisEndpoint.yMillimeters()) * normalY;
                    if (outward < -EPSILON) {
                        continue;
                    }
                    boolean better = isStart
                            ? projection < bestProjection - EPSILON
                            : projection > bestProjection + EPSILON;
                    if (better) {
                        best = intersection.get();
                        bestProjection = projection;
                    }
                }
            }
        }
        return best;
    }

    private PlanSegment shiftedSegment(PlanSegment segment, double offset, double sideSign) {
        Direction direction = direction(segment);
        double normalX = -direction.y() * sideSign;
        double normalY = direction.x() * sideSign;
        return new PlanSegment(
                new PlanPoint(segment.start().xMillimeters() + normalX * offset, segment.start().yMillimeters() + normalY * offset),
                new PlanPoint(segment.end().xMillimeters() + normalX * offset, segment.end().yMillimeters() + normalY * offset)
        );
    }

    private Optional<PlanPoint> lineIntersection(PlanPoint firstStart, PlanPoint firstEnd, PlanPoint secondStart, PlanPoint secondEnd) {
        double x1 = firstStart.xMillimeters();
        double y1 = firstStart.yMillimeters();
        double x2 = firstEnd.xMillimeters();
        double y2 = firstEnd.yMillimeters();
        double x3 = secondStart.xMillimeters();
        double y3 = secondStart.yMillimeters();
        double x4 = secondEnd.xMillimeters();
        double y4 = secondEnd.yMillimeters();
        double denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denominator) < EPSILON) {
            return Optional.empty();
        }
        double determinantFirst = x1 * y2 - y1 * x2;
        double determinantSecond = x3 * y4 - y3 * x4;
        double intersectionX = (determinantFirst * (x3 - x4) - (x1 - x2) * determinantSecond) / denominator;
        double intersectionY = (determinantFirst * (y3 - y4) - (y1 - y2) * determinantSecond) / denominator;
        return Optional.of(new PlanPoint(intersectionX, intersectionY));
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

    public record SideDimension(String name, Length length, double sideSign, PlanSegment dimensionSegment, String sourceKey) {
        public SideDimension {
            Objects.requireNonNull(name, "name darf nicht null sein.");
            Objects.requireNonNull(length, "length darf nicht null sein.");
            Objects.requireNonNull(dimensionSegment, "dimensionSegment darf nicht null sein.");
            Objects.requireNonNull(sourceKey, "sourceKey darf nicht null sein.");
            if (sideSign != -1.0 && sideSign != 1.0) {
                throw new IllegalArgumentException("sideSign muss -1 oder 1 sein.");
            }
        }

        public SideDimension(String name, Length length, double sideSign, PlanSegment dimensionSegment) {
            this(name, length, sideSign, dimensionSegment, name);
        }
    }

    public record WallDimensions(List<SideDimension> roomDimensions, Optional<SideDimension> exteriorDimension) {
        public WallDimensions {
            roomDimensions = List.copyOf(Objects.requireNonNull(roomDimensions, "roomDimensions darf nicht null sein."));
            Objects.requireNonNull(exteriorDimension, "exteriorDimension darf nicht null sein.");
        }
    }
}
