package de.schrell.cadas.ui;

import de.schrell.cadas.application.heating.HeatingCircuitRoutingService;
import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanPolygonSupport;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Bündelt reine Heizkreis-Helferlogik für die Workbench.
 */
final class CadWorkbenchHeatingSupport {

    private static final double EPSILON = 0.001;

    private CadWorkbenchHeatingSupport() {
    }

    static String describeHeatingZone(
            HeatingZone zone,
            List<HydronicHeatingLayoutService.CircuitLayout> circuits
    ) {
        double pipeLength = circuits.stream()
                .filter(circuit -> circuit.zoneId().equals(zone.id()))
                .findFirst()
                .map(circuit -> circuit.pipeLength().toMillimeters())
                .orElse(0.0);
        String roleOrientation = zone.flowInverted() ? "invertiert" : "normal";
        String routingMode = zone.hasRoutingCommands() ? "Sprachrouting" : "Alt";
        return String.format(
                Locale.GERMAN,
                "%s · %s · %s · %s · HKL %.1f m · %.2f m² · %.0f W",
                zone.name(), zone.layoutPattern(), routingMode, roleOrientation,
                pipeLength / 1_000.0, zone.areaSquareMeters(), zone.heatOutputWatts()
        );
    }

    static String heatingWarning(HydronicHeatingLayoutService.ValidationReport report, boolean maximumExceeded) {
        List<String> warnings = new ArrayList<>();
        if (!report.valid()) {
            warnings.add(report.summary());
        }
        if (maximumExceeded) {
            warnings.add("Mindestens ein Heizkreis überschreitet die maximale Rohrlänge.");
        }
        if (warnings.isEmpty()) {
            return "";
        }
        return " · Warnung: " + String.join(" ", warnings);
    }

    static HydronicManifoldDefaults defaultHydronicManifold(Room room, double pairDistanceMillimeters) {
        HeatingZoneBounds bounds = heatingZoneBounds(room.outline());
        PlanPoint center = bounds.center();
        boolean horizontal = bounds.width() >= bounds.height();
        PlanPoint supplyPoint = horizontal
                ? new PlanPoint(center.xMillimeters() - pairDistanceMillimeters / 2.0, center.yMillimeters())
                : new PlanPoint(center.xMillimeters(), center.yMillimeters() - pairDistanceMillimeters / 2.0);
        PlanPoint returnPoint = horizontal
                ? new PlanPoint(center.xMillimeters() + pairDistanceMillimeters / 2.0, center.yMillimeters())
                : new PlanPoint(center.xMillimeters(), center.yMillimeters() + pairDistanceMillimeters / 2.0);
        return new HydronicManifoldDefaults(supplyPoint, returnPoint);
    }

    static HeatingZone defaultHeatingZone(
            Room room,
            HydronicHeating heating,
            HeatingCircuitRoutingService heatingCircuitRoutingService
    ) {
        HeatingZone zone = new HeatingZone(
                UUID.randomUUID(),
                "Heizkreis " + (heating.zones().size() + 1),
                defaultHeatingRectangle(room, heating),
                heatingCircuitRoutingService.manualPattern(heating.layoutPattern()),
                false
        );
        try {
            return heatingCircuitRoutingService.regenerate(zone, heating);
        } catch (RuntimeException ignored) {
            return zone;
        }
    }

    static List<PlanPoint> defaultHeatingRectangle(Room room, HydronicHeating heating) {
        HeatingZoneBounds roomBounds = heatingZoneBounds(room.outline());
        List<Double> xCoordinates = heatingSplitCoordinates(roomBounds, heating, true);
        List<Double> yCoordinates = heatingSplitCoordinates(roomBounds, heating, false);
        HeatingZoneBounds bestBounds = null;
        double bestArea = 0.0;
        for (int xIndex = 0; xIndex + 1 < xCoordinates.size(); xIndex++) {
            for (int yIndex = 0; yIndex + 1 < yCoordinates.size(); yIndex++) {
                HeatingZoneBounds candidate = new HeatingZoneBounds(
                        xCoordinates.get(xIndex),
                        yCoordinates.get(yIndex),
                        xCoordinates.get(xIndex + 1),
                        yCoordinates.get(yIndex + 1)
                );
                if (candidate.width() <= 0.0 || candidate.height() <= 0.0) {
                    continue;
                }
                PlanPoint center = candidate.center();
                if (!PlanPolygonSupport.containsPoint(room.outline(), center, EPSILON) || heating.zones().stream()
                        .anyMatch(zone -> PlanPolygonSupport.containsPoint(zone.outline(), center, EPSILON))) {
                    continue;
                }
                double area = candidate.area();
                if (area > bestArea) {
                    bestArea = area;
                    bestBounds = candidate;
                }
            }
        }
        if (bestBounds == null) {
            double inset = Math.min(
                    Math.max(100.0, heating.wallClearance().toMillimeters()),
                    Math.min(roomBounds.width(), roomBounds.height()) / 4.0
            );
            double maxX = roomBounds.minX() + Math.max(100.0, Math.min(2_000.0, roomBounds.width() - inset * 2.0));
            double maxY = roomBounds.minY() + Math.max(100.0, Math.min(2_000.0, roomBounds.height() - inset * 2.0));
            bestBounds = new HeatingZoneBounds(roomBounds.minX() + inset, roomBounds.minY() + inset, maxX, maxY);
        }
        return rectanglePoints(bestBounds);
    }

    static List<Double> heatingSplitCoordinates(HeatingZoneBounds roomBounds, HydronicHeating heating, boolean xAxis) {
        List<Double> coordinates = new ArrayList<>();
        coordinates.add(xAxis ? roomBounds.minX() : roomBounds.minY());
        coordinates.add(xAxis ? roomBounds.maxX() : roomBounds.maxY());
        for (HeatingZone zone : heating.zones()) {
            HeatingZoneBounds bounds = heatingZoneBounds(zone.outline());
            coordinates.add(xAxis ? bounds.minX() : bounds.minY());
            coordinates.add(xAxis ? bounds.maxX() : bounds.maxY());
        }
        coordinates.sort(Double::compareTo);
        List<Double> distinct = new ArrayList<>();
        for (double coordinate : coordinates) {
            if (distinct.isEmpty() || Math.abs(distinct.getLast() - coordinate) > EPSILON) {
                distinct.add(coordinate);
            }
        }
        return List.copyOf(distinct);
    }

    static HeatingZoneBounds heatingZoneBounds(List<PlanPoint> points) {
        return new HeatingZoneBounds(
                points.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0)
        );
    }

    static HeatingZoneBounds heatingZoneBounds(PlanSegment segment) {
        return new HeatingZoneBounds(
                Math.min(segment.start().xMillimeters(), segment.end().xMillimeters()),
                Math.min(segment.start().yMillimeters(), segment.end().yMillimeters()),
                Math.max(segment.start().xMillimeters(), segment.end().xMillimeters()),
                Math.max(segment.start().yMillimeters(), segment.end().yMillimeters())
        );
    }

    static List<PlanPoint> rectanglePoints(HeatingZoneBounds bounds) {
        return List.of(
                new PlanPoint(bounds.minX(), bounds.minY()),
                new PlanPoint(bounds.maxX(), bounds.minY()),
                new PlanPoint(bounds.maxX(), bounds.maxY()),
                new PlanPoint(bounds.minX(), bounds.maxY())
        );
    }

    static List<PlanPoint> parseHeatingZonePoints(String text) {
        List<PlanPoint> points = new ArrayList<>();
        for (String line : Optional.ofNullable(text).orElse("").lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            String[] coordinates = line.trim().split("\\s*;\\s*", 2);
            if (coordinates.length != 2) {
                throw new IllegalArgumentException("Jeder Eckpunkt benötigt X und Y, getrennt durch Semikolon.");
            }
            try {
                points.add(new PlanPoint(
                        Double.parseDouble(coordinates[0].replace(',', '.')) * 10.0,
                        Double.parseDouble(coordinates[1].replace(',', '.')) * 10.0
                ));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Ungültiger Heizbereichs-Eckpunkt: " + line, exception);
            }
        }
        if (points.size() < 3) {
            throw new IllegalArgumentException("Ein Heizbereich benötigt mindestens drei Eckpunkte.");
        }
        return List.copyOf(points);
    }

    static double parseNonNegativeDouble(String text, String label) {
        try {
            double value = Double.parseDouble(Optional.ofNullable(text).orElse("").trim().replace(',', '.'));
            if (!Double.isFinite(value) || value < 0.0) {
                throw new IllegalArgumentException(label + " darf nicht negativ sein.");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " ist keine gültige Zahl.", exception);
        }
    }

    static boolean canMerge(HeatingZoneBounds first, HeatingZoneBounds second) {
        boolean sameY = sameCoordinate(first.minY(), second.minY()) && sameCoordinate(first.maxY(), second.maxY());
        boolean sameX = sameCoordinate(first.minX(), second.minX()) && sameCoordinate(first.maxX(), second.maxX());
        boolean verticalNeighbor = sameY
                && (sameCoordinate(first.maxX(), second.minX()) || sameCoordinate(second.maxX(), first.minX()));
        boolean horizontalNeighbor = sameX
                && (sameCoordinate(first.maxY(), second.minY()) || sameCoordinate(second.maxY(), first.minY()));
        return verticalNeighbor || horizontalNeighbor;
    }

    static HeatingZoneBounds union(HeatingZoneBounds first, HeatingZoneBounds second) {
        return new HeatingZoneBounds(
                Math.min(first.minX(), second.minX()),
                Math.min(first.minY(), second.minY()),
                Math.max(first.maxX(), second.maxX()),
                Math.max(first.maxY(), second.maxY())
        );
    }

    private static boolean sameCoordinate(double first, double second) {
        return Math.abs(first - second) <= EPSILON;
    }

    record HydronicManifoldDefaults(PlanPoint supplyPoint, PlanPoint returnPoint) {
    }

    record HeatingZoneBounds(double minX, double minY, double maxX, double maxY) {
        double width() {
            return maxX - minX;
        }

        double height() {
            return maxY - minY;
        }

        double area() {
            return width() * height();
        }

        PlanPoint center() {
            return new PlanPoint((minX + maxX) / 2.0, (minY + maxY) / 2.0);
        }
    }
}
