package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record Room(
        UUID id,
        String name,
        List<PlanPoint> outline,
        Length roomHeight,
        Length floorThickness,
        Length ceilingThickness,
        SlopedCeilingProfile slopedCeiling,
        List<Length> ceilingVertexHeights
) {

    public Room {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(name, "name darf nicht null sein.");
        Objects.requireNonNull(outline, "outline darf nicht null sein.");
        Objects.requireNonNull(roomHeight, "roomHeight darf nicht null sein.");
        Objects.requireNonNull(floorThickness, "floorThickness darf nicht null sein.");
        Objects.requireNonNull(ceilingThickness, "ceilingThickness darf nicht null sein.");
        if (ceilingVertexHeights != null && ceilingVertexHeights.size() != outline.size()) {
            throw new IllegalArgumentException("Deckenhöhen müssen zu allen Raum-Eckpunkten passen.");
        }
        if (slopedCeiling != null && slopedCeiling.kneeWallHeight().toMillimeters() > roomHeight.toMillimeters()) {
            throw new IllegalArgumentException("Die Sockelhöhe der Dachschräge darf die lichte Raumhöhe nicht überschreiten.");
        }
        if (outline.size() < 3) {
            throw new IllegalArgumentException("Ein Raum benötigt mindestens drei Eckpunkte.");
        }
        outline = List.copyOf(outline);
        ceilingVertexHeights = ceilingVertexHeights == null ? null : List.copyOf(ceilingVertexHeights);
    }

    public Room(
            UUID id,
            String name,
            List<PlanPoint> outline,
            Length roomHeight,
            Length floorThickness,
            Length ceilingThickness,
            SlopedCeilingProfile slopedCeiling
    ) {
        this(id, name, outline, roomHeight, floorThickness, ceilingThickness, slopedCeiling, null);
    }

    public static Room rectangular(
            String name,
            PlanPoint firstCorner,
            PlanPoint oppositeCorner,
            Length roomHeight,
            Length floorThickness,
            Length ceilingThickness
    ) {
        return rectangular(name, firstCorner, oppositeCorner, roomHeight, floorThickness, ceilingThickness, null);
    }

    public static Room rectangular(
            String name,
            PlanPoint firstCorner,
            PlanPoint oppositeCorner,
            Length roomHeight,
            Length floorThickness
    ) {
        return rectangular(name, firstCorner, oppositeCorner, roomHeight, floorThickness, Length.ofMillimeters(0.1), null);
    }

    public static Room rectangular(
            String name,
            PlanPoint firstCorner,
            PlanPoint oppositeCorner,
            Length roomHeight,
            Length floorThickness,
            Length ceilingThickness,
            SlopedCeilingProfile slopedCeiling
    ) {
        double minX = Math.min(firstCorner.xMillimeters(), oppositeCorner.xMillimeters());
        double maxX = Math.max(firstCorner.xMillimeters(), oppositeCorner.xMillimeters());
        double minY = Math.min(firstCorner.yMillimeters(), oppositeCorner.yMillimeters());
        double maxY = Math.max(firstCorner.yMillimeters(), oppositeCorner.yMillimeters());
        return new Room(
                UUID.randomUUID(),
                name,
                List.of(
                        new PlanPoint(minX, minY),
                        new PlanPoint(maxX, minY),
                        new PlanPoint(maxX, maxY),
                        new PlanPoint(minX, maxY)
                ),
                roomHeight,
                floorThickness,
                ceilingThickness,
                slopedCeiling,
                null
        );
    }

    public Optional<SlopedCeilingProfile> slopedCeilingProfile() {
        return Optional.ofNullable(slopedCeiling);
    }

    public Optional<List<Length>> ceilingVertexHeightsProfile() {
        return Optional.ofNullable(ceilingVertexHeights);
    }

    public Length area() {
        double areaDouble = 0.0;
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint current = outline.get(index);
            PlanPoint next = outline.get((index + 1) % outline.size());
            areaDouble += current.xMillimeters() * next.yMillimeters() - next.xMillimeters() * current.yMillimeters();
        }
        return Length.ofMillimeters(Math.abs(areaDouble) / 2.0);
    }

    public double areaSquareMeters() {
        return area().toMillimeters() / 1_000_000.0;
    }

    public double volumeCubicMeters() {
        if (ceilingVertexHeights != null && !ceilingVertexHeights.isEmpty()) {
            double centerHeight = ceilingHeightAt(centerPoint());
            double volumeMillimeters = 0.0;
            for (int index = 0; index < outline.size(); index++) {
                PlanPoint current = outline.get(index);
                PlanPoint next = outline.get((index + 1) % outline.size());
                double triangleArea = triangleArea(centerPoint(), current, next);
                double averageHeight = (centerHeight + ceilingVertexHeights.get(index).toMillimeters() + ceilingVertexHeights.get((index + 1) % ceilingVertexHeights.size()).toMillimeters()) / 3.0;
                volumeMillimeters += triangleArea * averageHeight;
            }
            return volumeMillimeters / 1_000_000_000.0;
        }
        double averageHeight = slopedCeiling == null
                ? roomHeight.toMillimeters()
                : ceilingHeightAt(areaCentroid());
        return areaSquareMeters() * averageHeight / 1000.0;
    }

    private PlanPoint areaCentroid() {
        double crossSum = 0.0;
        double weightedX = 0.0;
        double weightedY = 0.0;
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint current = outline.get(index);
            PlanPoint next = outline.get((index + 1) % outline.size());
            double cross = current.xMillimeters() * next.yMillimeters()
                    - next.xMillimeters() * current.yMillimeters();
            crossSum += cross;
            weightedX += (current.xMillimeters() + next.xMillimeters()) * cross;
            weightedY += (current.yMillimeters() + next.yMillimeters()) * cross;
        }
        if (Math.abs(crossSum) < 0.001) {
            return centerPoint();
        }
        return new PlanPoint(weightedX / (3.0 * crossSum), weightedY / (3.0 * crossSum));
    }

    public PlanPoint centerPoint() {
        double sumX = 0.0;
        double sumY = 0.0;
        for (PlanPoint point : outline) {
            sumX += point.xMillimeters();
            sumY += point.yMillimeters();
        }
        return new PlanPoint(sumX / outline.size(), sumY / outline.size());
    }

    public double minXMillimeters() {
        return outline.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0);
    }

    public double maxXMillimeters() {
        return outline.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0);
    }

    public double minYMillimeters() {
        return outline.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0);
    }

    public double maxYMillimeters() {
        return outline.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0);
    }

    public double widthMillimeters() {
        return maxXMillimeters() - minXMillimeters();
    }

    public double depthMillimeters() {
        return maxYMillimeters() - minYMillimeters();
    }

    public double ceilingHeightAt(PlanPoint point) {
        if (ceilingVertexHeights != null && ceilingVertexHeights.size() == outline.size()) {
            return interpolatedVertexHeight(point);
        }
        if (slopedCeiling == null) {
            return roomHeight.toMillimeters();
        }
        double lowHeight = slopedCeiling.kneeWallHeight().toMillimeters();
        double highHeight = roomHeight.toMillimeters();
        double run = runMillimeters(slopedCeiling.lowSide());
        if (run <= 1.0 || highHeight <= lowHeight) {
            return highHeight;
        }
        double distance = distanceFromLowSide(point, slopedCeiling.lowSide());
        double ratio = Math.max(0.0, Math.min(1.0, distance / run));
        return lowHeight + (highHeight - lowHeight) * ratio;
    }

    public double minimumCeilingHeightMillimeters() {
        if (ceilingVertexHeights != null && !ceilingVertexHeights.isEmpty()) {
            return ceilingVertexHeights.stream().mapToDouble(Length::toMillimeters).min().orElse(roomHeight.toMillimeters());
        }
        return slopedCeilingProfile()
                .map(profile -> profile.kneeWallHeight().toMillimeters())
                .orElse(roomHeight.toMillimeters());
    }

    public double maximumCeilingHeightMillimeters() {
        if (ceilingVertexHeights != null && !ceilingVertexHeights.isEmpty()) {
            return ceilingVertexHeights.stream().mapToDouble(Length::toMillimeters).max().orElse(roomHeight.toMillimeters());
        }
        return roomHeight.toMillimeters();
    }

    public double slopeAngleDegrees() {
        if (ceilingVertexHeights != null && !ceilingVertexHeights.isEmpty()) {
            return 0.0;
        }
        if (slopedCeiling == null) {
            return 0.0;
        }
        double rise = roomHeight.toMillimeters() - slopedCeiling.kneeWallHeight().toMillimeters();
        double run = runMillimeters(slopedCeiling.lowSide());
        if (rise <= 0.0 || run <= 1.0) {
            return 0.0;
        }
        return Math.toDegrees(Math.atan(rise / run));
    }

    public boolean slopeVisibleInEastWestView() {
        if (ceilingVertexHeights != null && !ceilingVertexHeights.isEmpty()) {
            return hasVariableCeilingHeights();
        }
        return slopedCeiling != null
                && (slopedCeiling.lowSide() == SlopedCeilingSide.NORTH || slopedCeiling.lowSide() == SlopedCeilingSide.SOUTH);
    }

    public boolean slopeVisibleInNorthSouthView() {
        if (ceilingVertexHeights != null && !ceilingVertexHeights.isEmpty()) {
            return hasVariableCeilingHeights();
        }
        return slopedCeiling != null
                && (slopedCeiling.lowSide() == SlopedCeilingSide.EAST || slopedCeiling.lowSide() == SlopedCeilingSide.WEST);
    }

    public boolean hasVariableCeilingHeights() {
        if (ceilingVertexHeights != null && ceilingVertexHeights.size() > 1) {
            double reference = ceilingVertexHeights.getFirst().toMillimeters();
            return ceilingVertexHeights.stream().anyMatch(length -> Math.abs(length.toMillimeters() - reference) > 0.001);
        }
        return slopedCeiling != null;
    }

    private double runMillimeters(SlopedCeilingSide side) {
        double roomRun = switch (side) {
            case NORTH, SOUTH -> depthMillimeters();
            case EAST, WEST -> widthMillimeters();
        };
        if (slopedCeiling == null || slopedCeiling.horizontalRun().toMillimeters() <= 0.0) {
            return roomRun;
        }
        return Math.min(roomRun, slopedCeiling.horizontalRun().toMillimeters());
    }

    private double distanceFromLowSide(PlanPoint point, SlopedCeilingSide side) {
        return switch (side) {
            case NORTH -> point.yMillimeters() - minYMillimeters();
            case SOUTH -> maxYMillimeters() - point.yMillimeters();
            case EAST -> maxXMillimeters() - point.xMillimeters();
            case WEST -> point.xMillimeters() - minXMillimeters();
        };
    }

    private double interpolatedVertexHeight(PlanPoint point) {
        PlanPoint center = centerPoint();
        double centerHeight = ceilingVertexHeights.stream().mapToDouble(Length::toMillimeters).average().orElse(roomHeight.toMillimeters());
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint first = outline.get(index);
            PlanPoint second = outline.get((index + 1) % outline.size());
            if (pointInsideTriangle(point, center, first, second)) {
                return barycentricHeight(
                        point,
                        center,
                        first,
                        second,
                        centerHeight,
                        ceilingVertexHeights.get(index).toMillimeters(),
                        ceilingVertexHeights.get((index + 1) % ceilingVertexHeights.size()).toMillimeters()
                );
            }
        }
        return centerHeight;
    }

    private boolean pointInsideTriangle(PlanPoint point, PlanPoint a, PlanPoint b, PlanPoint c) {
        double denominator = ((b.yMillimeters() - c.yMillimeters()) * (a.xMillimeters() - c.xMillimeters())
                + (c.xMillimeters() - b.xMillimeters()) * (a.yMillimeters() - c.yMillimeters()));
        if (Math.abs(denominator) < 0.001) {
            return false;
        }
        double alpha = ((b.yMillimeters() - c.yMillimeters()) * (point.xMillimeters() - c.xMillimeters())
                + (c.xMillimeters() - b.xMillimeters()) * (point.yMillimeters() - c.yMillimeters())) / denominator;
        double beta = ((c.yMillimeters() - a.yMillimeters()) * (point.xMillimeters() - c.xMillimeters())
                + (a.xMillimeters() - c.xMillimeters()) * (point.yMillimeters() - c.yMillimeters())) / denominator;
        double gamma = 1.0 - alpha - beta;
        return alpha >= -0.0001 && beta >= -0.0001 && gamma >= -0.0001;
    }

    private double barycentricHeight(PlanPoint point, PlanPoint a, PlanPoint b, PlanPoint c, double heightA, double heightB, double heightC) {
        double denominator = ((b.yMillimeters() - c.yMillimeters()) * (a.xMillimeters() - c.xMillimeters())
                + (c.xMillimeters() - b.xMillimeters()) * (a.yMillimeters() - c.yMillimeters()));
        if (Math.abs(denominator) < 0.001) {
            return heightA;
        }
        double alpha = ((b.yMillimeters() - c.yMillimeters()) * (point.xMillimeters() - c.xMillimeters())
                + (c.xMillimeters() - b.xMillimeters()) * (point.yMillimeters() - c.yMillimeters())) / denominator;
        double beta = ((c.yMillimeters() - a.yMillimeters()) * (point.xMillimeters() - c.xMillimeters())
                + (a.xMillimeters() - c.xMillimeters()) * (point.yMillimeters() - c.yMillimeters())) / denominator;
        double gamma = 1.0 - alpha - beta;
        return alpha * heightA + beta * heightB + gamma * heightC;
    }

    private double triangleArea(PlanPoint a, PlanPoint b, PlanPoint c) {
        return Math.abs(
                a.xMillimeters() * (b.yMillimeters() - c.yMillimeters())
                        + b.xMillimeters() * (c.yMillimeters() - a.yMillimeters())
                        + c.xMillimeters() * (a.yMillimeters() - b.yMillimeters())
        ) / 2.0;
    }
}
