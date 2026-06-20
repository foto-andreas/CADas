package de.schrell.cadas.application.drawing;

import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.Grid;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class OrthogonalCorrectionService {

    private static final double EPSILON = 0.001;

    public CorrectionResult correct(Level level, Set<SelectionKey> selections, Grid grid, double maximumDeviationDegrees) {
        if (maximumDeviationDegrees < 0.0 || maximumDeviationDegrees > 45.0) {
            throw new IllegalArgumentException("Die maximale Winkelabweichung muss zwischen 0° und 45° liegen.");
        }
        List<Wall> walls = new ArrayList<>(level.walls());
        boolean changed = false;
        for (SelectionKey selection : selections) {
            if (selection.kind() != RenderableKind.WALL) {
                continue;
            }
            int wallIndex = findWallIndex(walls, selection.elementId());
            if (wallIndex < 0) {
                continue;
            }
            Wall wall = walls.get(wallIndex);
            PlanPoint correctedEnd = correctedEnd(wall.axis(), grid, maximumDeviationDegrees);
            if (correctedEnd == null || samePoint(correctedEnd, wall.axis().end())) {
                continue;
            }
            PlanPoint previousEnd = wall.axis().end();
            walls = new ArrayList<>(walls.stream()
                    .map(candidate -> replaceEndpoint(candidate, previousEnd, correctedEnd))
                    .toList());
            changed = true;
        }
        List<RoomObject> roomObjects = level.roomObjects().stream()
                .map(roomObject -> {
                    if (!isSelected(selections, RenderableKind.ROOM_OBJECT, roomObject.id().toString())) {
                        return roomObject;
                    }
                    double correctedAngle = correctedAngle(roomObject.rotationDegrees(), maximumDeviationDegrees);
                    return Double.isNaN(correctedAngle) ? roomObject : roomObject.withRotationDegrees(correctedAngle);
                })
                .toList();
        changed |= !roomObjects.equals(level.roomObjects());
        return new CorrectionResult(List.copyOf(walls), roomObjects, changed);
    }

    private PlanPoint correctedEnd(PlanSegment axis, Grid grid, double maximumDeviationDegrees) {
        double deltaX = axis.end().xMillimeters() - axis.start().xMillimeters();
        double deltaY = axis.end().yMillimeters() - axis.start().yMillimeters();
        if (Math.hypot(deltaX, deltaY) < EPSILON) {
            return null;
        }
        double targetAngle = correctedAngle(Math.toDegrees(Math.atan2(deltaY, deltaX)), maximumDeviationDegrees);
        if (Double.isNaN(targetAngle)) {
            return null;
        }
        double spacing = grid.spacing().toMillimeters();
        int quarterTurn = Math.floorMod((int) Math.round(targetAngle / 90.0), 4);
        if (quarterTurn % 2 == 0) {
            double snappedX = Math.round(axis.end().xMillimeters() / spacing) * spacing;
            return new PlanPoint(snappedX, axis.start().yMillimeters());
        }
        double snappedY = Math.round(axis.end().yMillimeters() / spacing) * spacing;
        return new PlanPoint(axis.start().xMillimeters(), snappedY);
    }

    private double correctedAngle(double angleDegrees, double maximumDeviationDegrees) {
        double normalized = normalizeDegrees(angleDegrees);
        double target = Math.round(normalized / 90.0) * 90.0;
        double difference = Math.abs(normalized - target);
        difference = Math.min(difference, 360.0 - difference);
        return difference <= maximumDeviationDegrees + EPSILON ? normalizeDegrees(target) : Double.NaN;
    }

    private Wall replaceEndpoint(Wall wall, PlanPoint previousPoint, PlanPoint correctedPoint) {
        PlanPoint start = samePoint(wall.axis().start(), previousPoint) ? correctedPoint : wall.axis().start();
        PlanPoint end = samePoint(wall.axis().end(), previousPoint) ? correctedPoint : wall.axis().end();
        if (start == wall.axis().start() && end == wall.axis().end()) {
            return wall;
        }
        return wall.withAxis(new PlanSegment(start, end));
    }

    private int findWallIndex(List<Wall> walls, String elementId) {
        for (int index = 0; index < walls.size(); index++) {
            if (walls.get(index).id().toString().equals(elementId)) {
                return index;
            }
        }
        return -1;
    }

    private boolean isSelected(Set<SelectionKey> selections, RenderableKind kind, String elementId) {
        return selections.stream().anyMatch(selection -> selection.kind() == kind && selection.elementId().equals(elementId));
    }

    private boolean samePoint(PlanPoint first, PlanPoint second) {
        return Math.abs(first.xMillimeters() - second.xMillimeters()) <= EPSILON
                && Math.abs(first.yMillimeters() - second.yMillimeters()) <= EPSILON;
    }

    private double normalizeDegrees(double degrees) {
        double normalized = degrees % 360.0;
        return normalized < 0.0 ? normalized + 360.0 : normalized;
    }

    public record CorrectionResult(List<Wall> walls, List<RoomObject> roomObjects, boolean changed) {
    }
}
