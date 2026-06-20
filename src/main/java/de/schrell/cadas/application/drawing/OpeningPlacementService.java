package de.schrell.cadas.application.drawing;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class OpeningPlacementService {

    public Optional<Door> placeDoor(
            PlanPoint clickPoint,
            List<Wall> walls,
            Length width,
            Length height,
            Length thresholdHeight,
            Length snapTolerance
    ) {
        return findHostWall(clickPoint, walls, snapTolerance)
                .map(hostWall -> Door.create(
                        hostWall.id(),
                        centeredOffset(hostWall, clickPoint, width),
                        width,
                        height,
                        thresholdHeight
                ));
    }

    public Optional<WindowElement> placeWindow(
            PlanPoint clickPoint,
            List<Wall> walls,
            Length width,
            Length sillHeight,
            Length windowHeight,
            Length snapTolerance
    ) {
        return findHostWall(clickPoint, walls, snapTolerance)
                .map(hostWall -> WindowElement.create(
                        hostWall.id(),
                        centeredOffset(hostWall, clickPoint, width),
                        width,
                        sillHeight,
                        windowHeight
                ));
    }

    private Length centeredOffset(Wall wall, PlanPoint clickPoint, Length openingWidth) {
        double wallLength = wall.axis().length().toMillimeters();
        double width = openingWidth.toMillimeters();
        double centeredOffset = wall.axis().projectedLength(clickPoint).toMillimeters() - width / 2.0;
        double maximumOffset = Math.max(0.0, wallLength - width);
        return Length.ofMillimeters(Math.max(0.0, Math.min(centeredOffset, maximumOffset)));
    }

    private Optional<Wall> findHostWall(PlanPoint clickPoint, List<Wall> walls, Length snapTolerance) {
        return walls.stream()
                .filter(wall -> wall.axis().distanceTo(clickPoint).compareTo(snapTolerance) <= 0)
                .min(Comparator.comparingDouble(wall -> wall.axis().distanceTo(clickPoint).toMillimeters()));
    }
}
