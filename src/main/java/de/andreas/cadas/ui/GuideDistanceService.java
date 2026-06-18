package de.andreas.cadas.ui;

import de.andreas.cadas.domain.geometry.Length;

import java.util.Comparator;
import java.util.List;

final class GuideDistanceService {

    List<GuideDistance> distancesToParallelGuides(
            List<GuideLine> guideLines,
            GuideOrientation pendingOrientation,
            double pendingWorldMillimeters
    ) {
        return guideLines.stream()
                .filter(guideLine -> guideLine.orientation() == pendingOrientation)
                .map(guideLine -> new GuideDistance(
                        guideLine.worldMillimeters(),
                        Length.ofMillimeters(Math.abs(guideLine.worldMillimeters() - pendingWorldMillimeters))
                ))
                .filter(distance -> distance.distance().toMillimeters() > 0.001)
                .sorted(Comparator.comparingDouble(distance -> distance.distance().toMillimeters()))
                .toList();
    }

    record GuideDistance(double guideWorldMillimeters, Length distance) {
    }
}
