package de.andreas.cadas.application.drawing;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

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
                        hostWall.axis().projectedLength(clickPoint),
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
                        hostWall.axis().projectedLength(clickPoint),
                        width,
                        sillHeight,
                        windowHeight
                ));
    }

    private Optional<Wall> findHostWall(PlanPoint clickPoint, List<Wall> walls, Length snapTolerance) {
        return walls.stream()
                .filter(wall -> wall.axis().distanceTo(clickPoint).compareTo(snapTolerance) <= 0)
                .min(Comparator.comparingDouble(wall -> wall.axis().distanceTo(clickPoint).toMillimeters()));
    }
}

