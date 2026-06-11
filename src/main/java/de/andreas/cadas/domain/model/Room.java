package de.andreas.cadas.domain.model;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;

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
        SlopedCeilingProfile slopedCeiling
) {

    public Room {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(name, "name darf nicht null sein.");
        Objects.requireNonNull(outline, "outline darf nicht null sein.");
        Objects.requireNonNull(roomHeight, "roomHeight darf nicht null sein.");
        Objects.requireNonNull(floorThickness, "floorThickness darf nicht null sein.");
        Objects.requireNonNull(ceilingThickness, "ceilingThickness darf nicht null sein.");
        if (slopedCeiling != null && slopedCeiling.kneeWallHeight().toMillimeters() > roomHeight.toMillimeters()) {
            throw new IllegalArgumentException("Die Sockelhöhe der Dachschräge darf die lichte Raumhöhe nicht überschreiten.");
        }
        if (outline.size() < 3) {
            throw new IllegalArgumentException("Ein Raum benötigt mindestens drei Eckpunkte.");
        }
        outline = List.copyOf(outline);
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
                slopedCeiling
        );
    }

    public Optional<SlopedCeilingProfile> slopedCeilingProfile() {
        return Optional.ofNullable(slopedCeiling);
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
        double averageHeight = slopedCeilingProfile()
                .map(profile -> (profile.kneeWallHeight().toMillimeters() + roomHeight.toMillimeters()) / 2.0)
                .orElse(roomHeight.toMillimeters());
        return areaSquareMeters() * averageHeight / 1000.0;
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
        return slopedCeilingProfile()
                .map(profile -> profile.kneeWallHeight().toMillimeters())
                .orElse(roomHeight.toMillimeters());
    }

    public double maximumCeilingHeightMillimeters() {
        return roomHeight.toMillimeters();
    }

    public double slopeAngleDegrees() {
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
        return slopedCeiling != null
                && (slopedCeiling.lowSide() == SlopedCeilingSide.NORTH || slopedCeiling.lowSide() == SlopedCeilingSide.SOUTH);
    }

    public boolean slopeVisibleInNorthSouthView() {
        return slopedCeiling != null
                && (slopedCeiling.lowSide() == SlopedCeilingSide.EAST || slopedCeiling.lowSide() == SlopedCeilingSide.WEST);
    }

    private double runMillimeters(SlopedCeilingSide side) {
        return switch (side) {
            case NORTH, SOUTH -> depthMillimeters();
            case EAST, WEST -> widthMillimeters();
        };
    }

    private double distanceFromLowSide(PlanPoint point, SlopedCeilingSide side) {
        return switch (side) {
            case NORTH -> point.yMillimeters() - minYMillimeters();
            case SOUTH -> maxYMillimeters() - point.yMillimeters();
            case EAST -> maxXMillimeters() - point.xMillimeters();
            case WEST -> point.xMillimeters() - minXMillimeters();
        };
    }
}
