package de.schrell.cadas.application.view;

import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Wall;

import java.util.List;
import java.util.Objects;

/**
 * Erzeugt den 2D-Grundrisskörper einer Wand inklusive bündiger Endverlängerung.
 */
public final class WallPlanOutlineService {

    public List<PlanPoint> outline(Wall wall) {
        Objects.requireNonNull(wall, "wall darf nicht null sein.");
        double deltaX = wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters();
        double deltaY = wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters();
        double length = Math.max(1.0, Math.hypot(deltaX, deltaY));
        double directionX = deltaX / length;
        double directionY = deltaY / length;
        double normalX = -directionY;
        double normalY = directionX;
        double halfThickness = wall.thickness().toMillimeters() / 2.0;
        PlanPoint extendedStart = offsetPoint(wall.axis().start(), directionX, directionY, -halfThickness);
        PlanPoint extendedEnd = offsetPoint(wall.axis().end(), directionX, directionY, halfThickness);
        return List.of(
                offsetPoint(extendedStart, normalX, normalY, halfThickness),
                offsetPoint(extendedEnd, normalX, normalY, halfThickness),
                offsetPoint(extendedEnd, normalX, normalY, -halfThickness),
                offsetPoint(extendedStart, normalX, normalY, -halfThickness)
        );
    }

    private PlanPoint offsetPoint(PlanPoint point, double directionX, double directionY, double distance) {
        return new PlanPoint(
                point.xMillimeters() + directionX * distance,
                point.yMillimeters() + directionY * distance
        );
    }
}
