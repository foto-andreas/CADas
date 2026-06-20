package de.schrell.cadas.application.room;

import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OrthogonalPolygonDecompositionService {

    private static final double GRID_STEP = 200.0;

    public List<CellRectangle> decompose(List<PlanPoint> outline) {
        if (outline.size() < 3) {
            return List.of();
        }
        List<Double> xCoordinates = isOrthogonal(outline) ? orthogonalCoordinates(outline, true) : sampledCoordinates(outline, true);
        List<Double> yCoordinates = isOrthogonal(outline) ? orthogonalCoordinates(outline, false) : sampledCoordinates(outline, false);
        List<CellRectangle> rectangles = new ArrayList<>();
        for (int xIndex = 0; xIndex < xCoordinates.size() - 1; xIndex++) {
            for (int yIndex = 0; yIndex < yCoordinates.size() - 1; yIndex++) {
                double minX = xCoordinates.get(xIndex);
                double maxX = xCoordinates.get(xIndex + 1);
                double minY = yCoordinates.get(yIndex);
                double maxY = yCoordinates.get(yIndex + 1);
                PlanPoint center = new PlanPoint((minX + maxX) / 2.0, (minY + maxY) / 2.0);
                if (containsPoint(outline, center)) {
                    rectangles.add(new CellRectangle(minX, maxX, minY, maxY));
                }
            }
        }
        return mergeHorizontally(rectangles);
    }

    private boolean isOrthogonal(List<PlanPoint> outline) {
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint current = outline.get(index);
            PlanPoint next = outline.get((index + 1) % outline.size());
            if (Math.abs(current.xMillimeters() - next.xMillimeters()) >= 0.001
                    && Math.abs(current.yMillimeters() - next.yMillimeters()) >= 0.001) {
                return false;
            }
        }
        return true;
    }

    private List<Double> orthogonalCoordinates(List<PlanPoint> outline, boolean xAxis) {
        return outline.stream()
                .map(point -> xAxis ? point.xMillimeters() : point.yMillimeters())
                .distinct()
                .sorted()
                .toList();
    }

    private List<Double> sampledCoordinates(List<PlanPoint> outline, boolean xAxis) {
        double min = outline.stream().mapToDouble(point -> xAxis ? point.xMillimeters() : point.yMillimeters()).min().orElse(0.0);
        double max = outline.stream().mapToDouble(point -> xAxis ? point.xMillimeters() : point.yMillimeters()).max().orElse(0.0);
        List<Double> coordinates = new ArrayList<>();
        coordinates.add(min);
        double current = min;
        while (current + GRID_STEP < max) {
            current += GRID_STEP;
            coordinates.add(current);
        }
        coordinates.add(max);
        return coordinates.stream().distinct().sorted().toList();
    }

    private List<CellRectangle> mergeHorizontally(List<CellRectangle> rectangles) {
        List<CellRectangle> sorted = rectangles.stream()
                .sorted(Comparator
                        .comparingDouble(CellRectangle::minY)
                        .thenComparingDouble(CellRectangle::maxY)
                        .thenComparingDouble(CellRectangle::minX))
                .toList();
        List<CellRectangle> merged = new ArrayList<>();
        for (CellRectangle rectangle : sorted) {
            if (!merged.isEmpty()) {
                CellRectangle previous = merged.getLast();
                if (Math.abs(previous.minY() - rectangle.minY()) < 0.001
                        && Math.abs(previous.maxY() - rectangle.maxY()) < 0.001
                        && Math.abs(previous.maxX() - rectangle.minX()) < 0.001) {
                    merged.set(merged.size() - 1, new CellRectangle(previous.minX(), rectangle.maxX(), previous.minY(), previous.maxY()));
                    continue;
                }
            }
            merged.add(rectangle);
        }
        return mergeVertically(merged);
    }

    private List<CellRectangle> mergeVertically(List<CellRectangle> rectangles) {
        List<CellRectangle> remaining = new ArrayList<>(rectangles);
        boolean merged;
        do {
            merged = false;
            outer:
            for (int firstIndex = 0; firstIndex < remaining.size(); firstIndex++) {
                for (int secondIndex = firstIndex + 1; secondIndex < remaining.size(); secondIndex++) {
                    CellRectangle first = remaining.get(firstIndex);
                    CellRectangle second = remaining.get(secondIndex);
                    if (Math.abs(first.minX() - second.minX()) < 0.001
                            && Math.abs(first.maxX() - second.maxX()) < 0.001
                            && Math.abs(first.maxY() - second.minY()) < 0.001) {
                        remaining.set(firstIndex, new CellRectangle(first.minX(), first.maxX(), first.minY(), second.maxY()));
                        remaining.remove(secondIndex);
                        merged = true;
                        break outer;
                    }
                }
            }
        } while (merged);
        return remaining;
    }

    private boolean containsPoint(List<PlanPoint> outline, PlanPoint point) {
        boolean inside = false;
        int lastIndex = outline.size() - 1;
        for (int currentIndex = 0; currentIndex < outline.size(); currentIndex++) {
            PlanPoint current = outline.get(currentIndex);
            PlanPoint previous = outline.get(lastIndex);
            boolean intersects = (current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters())
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / (previous.yMillimeters() - current.yMillimeters())
                    + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            lastIndex = currentIndex;
        }
        return inside;
    }

    public record CellRectangle(double minX, double maxX, double minY, double maxY) {

        public double centerX() {
            return (minX + maxX) / 2.0;
        }

        public double centerY() {
            return (minY + maxY) / 2.0;
        }

        public double width() {
            return maxX - minX;
        }

        public double height() {
            return maxY - minY;
        }
    }
}
