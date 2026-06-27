package de.schrell.cadas.application.terrain;

import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Erzeugt die sichtbare äußere Gelände-Kontur als konstantes Band um die
 * innere Gebäude-Kontur.
 */
public final class TerrainGeometryService {

    private static final double EPSILON = 0.001;

    public List<PlanPoint> outerOutline(Terrain terrain) {
        List<PlanPoint> innerOutline = terrain.vertices().stream()
                .map(vertex -> vertex.position())
                .toList();
        if (innerOutline.size() < 3) {
            return List.of();
        }
        double offset = terrain.displayWidth().toMillimeters();
        double signedArea = signedArea(innerOutline);
        if (Math.abs(signedArea) < EPSILON || offset <= EPSILON) {
            return innerOutline;
        }
        List<OffsetLine> offsetLines = new ArrayList<>();
        for (int index = 0; index < innerOutline.size(); index++) {
            PlanPoint start = innerOutline.get(index);
            PlanPoint end = innerOutline.get((index + 1) % innerOutline.size());
            offsetLines.add(offsetLine(start, end, signedArea, offset));
        }
        List<PlanPoint> result = new ArrayList<>();
        for (int index = 0; index < offsetLines.size(); index++) {
            OffsetLine previous = offsetLines.get((index - 1 + offsetLines.size()) % offsetLines.size());
            OffsetLine current = offsetLines.get(index);
            result.add(intersect(previous, current).orElse(current.start()));
        }
        return result;
    }

    private double signedArea(List<PlanPoint> points) {
        double area = 0.0;
        for (int index = 0; index < points.size(); index++) {
            PlanPoint current = points.get(index);
            PlanPoint next = points.get((index + 1) % points.size());
            area += current.xMillimeters() * next.yMillimeters() - next.xMillimeters() * current.yMillimeters();
        }
        return area / 2.0;
    }

    private OffsetLine offsetLine(PlanPoint start, PlanPoint end, double signedArea, double offset) {
        double deltaX = end.xMillimeters() - start.xMillimeters();
        double deltaY = end.yMillimeters() - start.yMillimeters();
        double length = Math.hypot(deltaX, deltaY);
        if (length <= EPSILON) {
            return new OffsetLine(start, end);
        }
        double normalX = signedArea > 0.0 ? deltaY / length : -deltaY / length;
        double normalY = signedArea > 0.0 ? -deltaX / length : deltaX / length;
        return new OffsetLine(
                new PlanPoint(start.xMillimeters() + normalX * offset, start.yMillimeters() + normalY * offset),
                new PlanPoint(end.xMillimeters() + normalX * offset, end.yMillimeters() + normalY * offset)
        );
    }

    private Optional<PlanPoint> intersect(OffsetLine first, OffsetLine second) {
        double x1 = first.start().xMillimeters();
        double y1 = first.start().yMillimeters();
        double x2 = first.end().xMillimeters();
        double y2 = first.end().yMillimeters();
        double x3 = second.start().xMillimeters();
        double y3 = second.start().yMillimeters();
        double x4 = second.end().xMillimeters();
        double y4 = second.end().yMillimeters();
        double denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denominator) <= EPSILON) {
            return Optional.empty();
        }
        double determinantFirst = x1 * y2 - y1 * x2;
        double determinantSecond = x3 * y4 - y3 * x4;
        return Optional.of(new PlanPoint(
                (determinantFirst * (x3 - x4) - (x1 - x2) * determinantSecond) / denominator,
                (determinantFirst * (y3 - y4) - (y1 - y2) * determinantSecond) / denominator
        ));
    }

    private record OffsetLine(PlanPoint start, PlanPoint end) {
    }
}
