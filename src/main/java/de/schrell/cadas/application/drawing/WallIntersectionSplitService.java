package de.schrell.cadas.application.drawing;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;
import de.schrell.cadas.domain.model.WindowElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Teilt ausgewählte Wände an einer Wandkreuzung auf und erhält dabei Öffnungen und Beläge.
 */
public final class WallIntersectionSplitService {

    private static final double EPSILON = 0.001;

    /**
     * Liefert eine aufteilbare Wandkreuzung am Kontextpunkt, falls mindestens eine ausgewählte Wand dort geteilt werden kann.
     */
    public Optional<SplitCandidate> findCandidate(
            Level level,
            List<UUID> selectedWallIds,
            PlanPoint contextPoint,
            Length tolerance
    ) {
        List<SplitPlan> plans = splitPlans(level, selectedWallIds, contextPoint, tolerance);
        if (plans.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SplitCandidate(
                plans.getFirst().intersection(),
                plans.stream().map(SplitPlan::wallId).toList()
        ));
    }

    /**
     * Teilt alle ausgewählten Wände, die am Kontextpunkt eine echte Kreuzung besitzen.
     */
    public SplitResult split(
            Level level,
            List<UUID> selectedWallIds,
            PlanPoint contextPoint,
            Length tolerance
    ) {
        List<SplitPlan> plans = splitPlans(level, selectedWallIds, contextPoint, tolerance);
        if (plans.isEmpty()) {
            throw new IllegalArgumentException("Am Kontextpunkt ist keine aufteilbare Wandkreuzung vorhanden.");
        }
        List<Wall> walls = new ArrayList<>(level.walls());
        List<Door> doors = new ArrayList<>(level.doors());
        List<WindowElement> windows = new ArrayList<>(level.windows());
        List<SurfaceLayerStack> surfaceLayerStacks = new ArrayList<>(level.surfaceLayerStacks());
        List<SplitWall> splits = new ArrayList<>();
        for (SplitPlan plan : plans) {
            int wallIndex = indexOfWall(walls, plan.wallId());
            if (wallIndex < 0) {
                continue;
            }
            Wall wall = walls.get(wallIndex);
            double splitOffset = wall.axis().projectedLength(plan.intersection()).toMillimeters();
            if (splitOffset <= EPSILON || splitOffset >= wall.axis().length().toMillimeters() - EPSILON) {
                continue;
            }
            verifyNoOpeningCrossesSplit(wall.id(), splitOffset, doors, windows);
            UUID secondWallId = UUID.randomUUID();
            SplitWallParts parts = splitWall(wall, plan.intersection(), splitOffset, secondWallId);
            walls.set(wallIndex, parts.firstWall());
            walls.add(wallIndex + 1, parts.secondWall());
            rebindOpeningsAfterSplit(wall.id(), secondWallId, splitOffset, doors, windows);
            duplicateSurfaceLayerStacks(surfaceLayerStacks, wall.id(), secondWallId);
            splits.add(new SplitWall(wall.id(), secondWallId, plan.intersection()));
        }
        return new SplitResult(
                List.copyOf(walls),
                List.copyOf(doors),
                List.copyOf(windows),
                List.copyOf(surfaceLayerStacks),
                List.copyOf(splits)
        );
    }

    private List<SplitPlan> splitPlans(
            Level level,
            List<UUID> selectedWallIds,
            PlanPoint contextPoint,
            Length tolerance
    ) {
        Objects.requireNonNull(level, "level darf nicht null sein.");
        Objects.requireNonNull(selectedWallIds, "selectedWallIds darf nicht null sein.");
        if (contextPoint == null || tolerance == null || selectedWallIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> distinctWallIds = new LinkedHashSet<>(selectedWallIds);
        List<Wall> walls = level.walls();
        List<SplitPlan> plans = new ArrayList<>();
        double toleranceMillimeters = tolerance.toMillimeters();
        for (UUID wallId : distinctWallIds) {
            Wall selectedWall = walls.stream()
                    .filter(candidate -> candidate.id().equals(wallId))
                    .findFirst()
                    .orElse(null);
            if (selectedWall == null) {
                continue;
            }
            walls.stream()
                    .filter(candidate -> !candidate.id().equals(selectedWall.id()))
                    .map(candidate -> intersection(selectedWall, candidate))
                    .flatMap(Optional::stream)
                    .filter(hit -> hit.point().distanceTo(contextPoint).toMillimeters() <= toleranceMillimeters + EPSILON)
                    .min(Comparator.comparingDouble(hit -> hit.point().distanceTo(contextPoint).toMillimeters()))
                    .map(hit -> new SplitPlan(selectedWall.id(), hit.point()))
                    .ifPresent(plans::add);
        }
        return List.copyOf(plans);
    }

    private Optional<IntersectionHit> intersection(Wall selectedWall, Wall otherWall) {
        PlanPoint firstStart = selectedWall.axis().start();
        PlanPoint firstEnd = selectedWall.axis().end();
        PlanPoint secondStart = otherWall.axis().start();
        PlanPoint secondEnd = otherWall.axis().end();
        double firstDeltaX = firstEnd.xMillimeters() - firstStart.xMillimeters();
        double firstDeltaY = firstEnd.yMillimeters() - firstStart.yMillimeters();
        double secondDeltaX = secondEnd.xMillimeters() - secondStart.xMillimeters();
        double secondDeltaY = secondEnd.yMillimeters() - secondStart.yMillimeters();
        double denominator = cross(firstDeltaX, firstDeltaY, secondDeltaX, secondDeltaY);
        if (Math.abs(denominator) <= EPSILON) {
            return Optional.empty();
        }
        double startDeltaX = secondStart.xMillimeters() - firstStart.xMillimeters();
        double startDeltaY = secondStart.yMillimeters() - firstStart.yMillimeters();
        double firstRatio = cross(startDeltaX, startDeltaY, secondDeltaX, secondDeltaY) / denominator;
        double secondRatio = cross(startDeltaX, startDeltaY, firstDeltaX, firstDeltaY) / denominator;
        if (firstRatio <= EPSILON || firstRatio >= 1.0 - EPSILON
                || secondRatio < -EPSILON || secondRatio > 1.0 + EPSILON) {
            return Optional.empty();
        }
        return Optional.of(new IntersectionHit(new PlanPoint(
                firstStart.xMillimeters() + firstDeltaX * firstRatio,
                firstStart.yMillimeters() + firstDeltaY * firstRatio
        )));
    }

    private double cross(double firstX, double firstY, double secondX, double secondY) {
        return firstX * secondY - firstY * secondX;
    }

    private int indexOfWall(List<Wall> walls, UUID wallId) {
        for (int index = 0; index < walls.size(); index++) {
            if (walls.get(index).id().equals(wallId)) {
                return index;
            }
        }
        return -1;
    }

    private SplitWallParts splitWall(Wall wall, PlanPoint splitPoint, double splitOffset, UUID secondWallId) {
        double startHeight = wall.heightAtStart();
        double splitHeight = wall.heightAt(splitOffset);
        double endHeight = wall.heightAtEnd();
        PlanSegment firstAxis = new PlanSegment(wall.axis().start(), splitPoint);
        PlanSegment secondAxis = new PlanSegment(splitPoint, wall.axis().end());
        if (!wall.hasPolygonalProfile()) {
            Wall firstWall = new Wall(
                    wall.id(),
                    firstAxis,
                    wall.thickness(),
                    Length.ofMillimeters(Math.max(startHeight, splitHeight)),
                    Length.ofMillimeters(startHeight),
                    Length.ofMillimeters(splitHeight)
            );
            Wall secondWall = new Wall(
                    secondWallId,
                    secondAxis,
                    wall.thickness(),
                    Length.ofMillimeters(Math.max(splitHeight, endHeight)),
                    Length.ofMillimeters(splitHeight),
                    Length.ofMillimeters(endHeight)
            );
            return new SplitWallParts(firstWall, secondWall);
        }
        ProfileParts profileParts = splitProfile(wall.profile(), splitOffset, wall.axis().length().toMillimeters(), splitHeight, endHeight);
        Wall firstWall = new Wall(
                wall.id(),
                firstAxis,
                wall.thickness(),
                wall.height(),
                Length.ofMillimeters(startHeight),
                Length.ofMillimeters(splitHeight),
                profileParts.firstProfile()
        );
        Wall secondWall = new Wall(
                secondWallId,
                secondAxis,
                wall.thickness(),
                wall.height(),
                Length.ofMillimeters(splitHeight),
                Length.ofMillimeters(endHeight),
                profileParts.secondProfile()
        );
        return new SplitWallParts(firstWall, secondWall);
    }

    private ProfileParts splitProfile(
            List<WallProfilePoint> profile,
            double splitOffset,
            double totalLength,
            double splitHeight,
            double endHeight
    ) {
        List<WallProfilePoint> firstProfile = new ArrayList<>();
        List<WallProfilePoint> secondProfile = new ArrayList<>();
        firstProfile.add(new WallProfilePoint(Length.zero(), profile.getFirst().height()));
        secondProfile.add(new WallProfilePoint(Length.zero(), Length.ofMillimeters(splitHeight)));
        for (int index = 1; index < profile.size() - 1; index++) {
            WallProfilePoint point = profile.get(index);
            double offset = point.offset().toMillimeters();
            if (offset < splitOffset - EPSILON) {
                firstProfile.add(point);
                continue;
            }
            if (offset > splitOffset + EPSILON) {
                secondProfile.add(new WallProfilePoint(
                        Length.ofMillimeters(offset - splitOffset),
                        point.height()
                ));
            }
        }
        firstProfile.add(new WallProfilePoint(Length.ofMillimeters(splitOffset), Length.ofMillimeters(splitHeight)));
        secondProfile.add(new WallProfilePoint(
                Length.ofMillimeters(totalLength - splitOffset),
                Length.ofMillimeters(endHeight)
        ));
        return new ProfileParts(List.copyOf(firstProfile), List.copyOf(secondProfile));
    }

    private void verifyNoOpeningCrossesSplit(
            UUID wallId,
            double splitOffset,
            List<Door> doors,
            List<WindowElement> windows
    ) {
        boolean crossingDoor = doors.stream()
                .filter(door -> door.wallId().equals(wallId))
                .anyMatch(door -> crosses(door.offsetFromStart(), door.width(), splitOffset));
        boolean crossingWindow = windows.stream()
                .filter(window -> window.wallId().equals(wallId))
                .anyMatch(window -> crosses(window.offsetFromStart(), window.width(), splitOffset));
        if (crossingDoor || crossingWindow) {
            throw new IllegalArgumentException("Die Wandteilung darf keine Tür oder kein Fenster schneiden.");
        }
    }

    private boolean crosses(Length offset, Length width, double splitOffset) {
        return offset.toMillimeters() < splitOffset - EPSILON
                && offset.toMillimeters() + width.toMillimeters() > splitOffset + EPSILON;
    }

    private void rebindOpeningsAfterSplit(
            UUID firstWallId,
            UUID secondWallId,
            double splitOffset,
            List<Door> doors,
            List<WindowElement> windows
    ) {
        for (int index = 0; index < doors.size(); index++) {
            Door door = doors.get(index);
            if (door.wallId().equals(firstWallId) && door.offsetFromStart().toMillimeters() >= splitOffset - EPSILON) {
                doors.set(index, new Door(
                        door.id(),
                        secondWallId,
                        Length.ofMillimeters(door.offsetFromStart().toMillimeters() - splitOffset),
                        door.width(),
                        door.height(),
                        door.thresholdHeight()
                ));
            }
        }
        for (int index = 0; index < windows.size(); index++) {
            WindowElement window = windows.get(index);
            if (window.wallId().equals(firstWallId) && window.offsetFromStart().toMillimeters() >= splitOffset - EPSILON) {
                windows.set(index, new WindowElement(
                        window.id(),
                        secondWallId,
                        Length.ofMillimeters(window.offsetFromStart().toMillimeters() - splitOffset),
                        window.width(),
                        window.sillHeight(),
                        window.windowHeight()
                ));
            }
        }
    }

    private void duplicateSurfaceLayerStacks(List<SurfaceLayerStack> stacks, UUID firstWallId, UUID secondWallId) {
        String firstTargetKey = firstWallId.toString();
        List<SurfaceLayerStack> duplicates = stacks.stream()
                .filter(stack -> stack.surfaceType() == SurfaceType.WALL_INTERIOR
                        || stack.surfaceType() == SurfaceType.WALL_EXTERIOR)
                .filter(stack -> stack.targetKey().equals(firstTargetKey)
                        || stack.targetKey().startsWith(firstTargetKey + "@"))
                .map(stack -> duplicateStack(
                        stack,
                        secondWallId + stack.targetKey().substring(firstTargetKey.length())
                ))
                .toList();
        stacks.addAll(duplicates);
    }

    private SurfaceLayerStack duplicateStack(SurfaceLayerStack source, String targetKey) {
        SurfaceLayerStack duplicate = new SurfaceLayerStack(source.surfaceType(), targetKey);
        for (SurfaceLayer layer : source.layers()) {
            duplicate.addLayer(layer);
        }
        return duplicate;
    }

    public record SplitCandidate(PlanPoint intersection, List<UUID> wallIds) {
    }

    public record SplitResult(
            List<Wall> walls,
            List<Door> doors,
            List<WindowElement> windows,
            List<SurfaceLayerStack> surfaceLayerStacks,
            List<SplitWall> splits
    ) {
    }

    public record SplitWall(UUID firstWallId, UUID secondWallId, PlanPoint intersection) {
    }

    private record SplitPlan(UUID wallId, PlanPoint intersection) {
    }

    private record IntersectionHit(PlanPoint point) {
    }

    private record SplitWallParts(Wall firstWall, Wall secondWall) {
    }

    private record ProfileParts(List<WallProfilePoint> firstProfile, List<WallProfilePoint> secondProfile) {
    }
}
