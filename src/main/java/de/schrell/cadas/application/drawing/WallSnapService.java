package de.schrell.cadas.application.drawing;

import de.schrell.cadas.domain.model.Wall;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class WallSnapService {

    private static final double ORTHOGONAL_TOLERANCE = Math.tan(Math.toRadians(0.5));

    public GuideSnapTargets targets(List<Wall> walls, Set<UUID> excludedWallIds) {
        Set<Double> vertical = new LinkedHashSet<>();
        Set<Double> horizontal = new LinkedHashSet<>();
        walls.stream()
                .filter(wall -> !excludedWallIds.contains(wall.id()))
                .forEach(wall -> addTargets(wall, vertical, horizontal));
        return new GuideSnapTargets(List.copyOf(vertical), List.copyOf(horizontal));
    }

    private void addTargets(Wall wall, Set<Double> vertical, Set<Double> horizontal) {
        double deltaX = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double deltaY = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double halfThickness = wall.thickness().toMillimeters() / 2.0;
        if (isHorizontal(deltaX, deltaY)) {
            double axisY = (wall.axis().start().yMillimeters() + wall.axis().end().yMillimeters()) / 2.0;
            horizontal.add(axisY);
            horizontal.add(axisY - halfThickness);
            horizontal.add(axisY + halfThickness);
            vertical.add(wall.axis().start().xMillimeters());
            vertical.add(wall.axis().end().xMillimeters());
        } else if (isVertical(deltaX, deltaY)) {
            double axisX = (wall.axis().start().xMillimeters() + wall.axis().end().xMillimeters()) / 2.0;
            vertical.add(axisX);
            vertical.add(axisX - halfThickness);
            vertical.add(axisX + halfThickness);
            horizontal.add(wall.axis().start().yMillimeters());
            horizontal.add(wall.axis().end().yMillimeters());
        }
    }

    private boolean isHorizontal(double deltaX, double deltaY) {
        return Math.abs(deltaX) > 0.001 && Math.abs(deltaY / deltaX) <= ORTHOGONAL_TOLERANCE;
    }

    private boolean isVertical(double deltaX, double deltaY) {
        return Math.abs(deltaY) > 0.001 && Math.abs(deltaX / deltaY) <= ORTHOGONAL_TOLERANCE;
    }
}
