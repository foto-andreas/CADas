package de.andreas.cadas.application.drawing;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class WallEditingService {

    public Optional<WallEndpointSelection> findConnectedEndpoint(List<Wall> walls, PlanPoint clickPoint, Length tolerance) {
        for (Wall wall : walls) {
            if (wall.axis().start().distanceTo(clickPoint).compareTo(tolerance) <= 0) {
                return Optional.of(selectionForPoint(walls, wall.axis().start()));
            }
            if (wall.axis().end().distanceTo(clickPoint).compareTo(tolerance) <= 0) {
                return Optional.of(selectionForPoint(walls, wall.axis().end()));
            }
        }
        return Optional.empty();
    }

    public List<Wall> moveEndpointGroup(List<Wall> walls, WallEndpointSelection selection, PlanPoint newPoint) {
        List<Wall> updatedWalls = new ArrayList<>();
        for (Wall wall : walls) {
            PlanSegment axis = wall.axis();
            if (selection.startWallIds().contains(wall.id())) {
                axis = new PlanSegment(newPoint, axis.end());
            }
            if (selection.endWallIds().contains(wall.id())) {
                axis = new PlanSegment(axis.start(), newPoint);
            }
            updatedWalls.add(new Wall(wall.id(), axis, wall.thickness(), wall.height()));
        }
        return updatedWalls;
    }

    private WallEndpointSelection selectionForPoint(List<Wall> walls, PlanPoint anchorPoint) {
        List<UUID> startWallIds = new ArrayList<>();
        List<UUID> endWallIds = new ArrayList<>();
        for (Wall wall : walls) {
            if (samePoint(wall.axis().start(), anchorPoint)) {
                startWallIds.add(wall.id());
            }
            if (samePoint(wall.axis().end(), anchorPoint)) {
                endWallIds.add(wall.id());
            }
        }
        return new WallEndpointSelection(anchorPoint, List.copyOf(startWallIds), List.copyOf(endWallIds));
    }

    private boolean samePoint(PlanPoint first, PlanPoint second) {
        return Math.abs(first.xMillimeters() - second.xMillimeters()) < 0.001
                && Math.abs(first.yMillimeters() - second.yMillimeters()) < 0.001;
    }
}

