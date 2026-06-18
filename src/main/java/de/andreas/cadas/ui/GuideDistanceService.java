package de.andreas.cadas.ui;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    Optional<GuideLine> nearestGuide(List<GuideLine> guideLines, PlanPoint clickPoint, Length tolerance) {
        return guideLines.stream()
                .filter(guideLine -> normalDistance(guideLine, clickPoint) <= tolerance.toMillimeters())
                .min(Comparator.comparingDouble(guideLine -> normalDistance(guideLine, clickPoint)));
    }

    private double normalDistance(GuideLine guideLine, PlanPoint point) {
        return guideLine.orientation() == GuideOrientation.VERTICAL
                ? Math.abs(guideLine.worldMillimeters() - point.xMillimeters())
                : Math.abs(guideLine.worldMillimeters() - point.yMillimeters());
    }

    record GuideDistance(double guideWorldMillimeters, Length distance) {
    }
}
