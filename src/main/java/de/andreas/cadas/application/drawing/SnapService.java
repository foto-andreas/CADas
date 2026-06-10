package de.andreas.cadas.application.drawing;

import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.model.Wall;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class SnapService {

    public PlanPoint snap(PlanPoint rawPoint, DraftingConstraints constraints, List<Wall> walls) {
        if (constraints.snapToEndpoints()) {
            Optional<PlanPoint> nearestEndpoint = walls.stream()
                    .flatMap(wall -> java.util.stream.Stream.of(wall.axis().start(), wall.axis().end()))
                    .filter(candidate -> candidate.distanceTo(rawPoint).compareTo(constraints.snapTolerance()) <= 0)
                    .min(Comparator.comparingDouble(candidate -> candidate.distanceTo(rawPoint).toMillimeters()));
            if (nearestEndpoint.isPresent()) {
                return nearestEndpoint.get();
            }
        }

        if (constraints.snapToGrid()) {
            return constraints.grid().snap(rawPoint);
        }
        return rawPoint;
    }
}

