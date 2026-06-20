package de.schrell.cadas.application.terrain;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TerrainCornerService {

    private static final double MATCH_TOLERANCE = 0.1;

    public Terrain synchronize(Level lowestLevel, Terrain existingTerrain) {
        List<PlanPoint> points = lowestLevel.walls().stream()
                .flatMap(wall -> List.of(wall.axis().start(), wall.axis().end()).stream())
                .distinct()
                .toList();
        List<PlanPoint> hull = convexHull(points);
        if (hull.size() < 3) {
            return Terrain.empty();
        }
        return new Terrain(hull.stream()
                .map(point -> new TerrainVertex(point, existingElevation(existingTerrain, point)))
                .toList());
    }

    private Length existingElevation(Terrain terrain, PlanPoint point) {
        return terrain.vertices().stream()
                .filter(vertex -> distance(vertex.position(), point) <= MATCH_TOLERANCE)
                .map(TerrainVertex::elevationAboveLowestFloor)
                .findFirst()
                .orElse(Length.zero());
    }

    private List<PlanPoint> convexHull(List<PlanPoint> input) {
        List<PlanPoint> points = input.stream()
                .sorted(Comparator.comparingDouble(PlanPoint::xMillimeters).thenComparingDouble(PlanPoint::yMillimeters))
                .toList();
        if (points.size() < 3) {
            return points;
        }
        List<PlanPoint> lower = new ArrayList<>();
        for (PlanPoint point : points) {
            while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.getLast(), point) <= 0.0) {
                lower.removeLast();
            }
            lower.add(point);
        }
        List<PlanPoint> upper = new ArrayList<>();
        for (int index = points.size() - 1; index >= 0; index--) {
            PlanPoint point = points.get(index);
            while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.getLast(), point) <= 0.0) {
                upper.removeLast();
            }
            upper.add(point);
        }
        lower.removeLast();
        upper.removeLast();
        lower.addAll(upper);
        return List.copyOf(lower);
    }

    private double cross(PlanPoint first, PlanPoint second, PlanPoint third) {
        return (second.xMillimeters() - first.xMillimeters()) * (third.yMillimeters() - first.yMillimeters())
                - (second.yMillimeters() - first.yMillimeters()) * (third.xMillimeters() - first.xMillimeters());
    }

    private double distance(PlanPoint first, PlanPoint second) {
        return Math.hypot(first.xMillimeters() - second.xMillimeters(), first.yMillimeters() - second.yMillimeters());
    }
}
