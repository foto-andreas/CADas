package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record HeatingZone(
        UUID id,
        String name,
        List<PlanPoint> outline,
        HeatingLayoutPattern layoutPattern,
        boolean flowInverted,
        PlanPoint supplyConnectionPoint,
        PlanPoint returnConnectionPoint,
        String routingCommands,
        boolean serpentineMiddleLine,
        double heatOutputWattsPerSquareMeter,
        int routingQuarterTurns,
        boolean routingMirroredHorizontally,
        boolean routingMirroredVertically
) {

    public HeatingZone {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        name = Objects.requireNonNull(name, "name darf nicht null sein.").trim();
        Objects.requireNonNull(outline, "outline darf nicht null sein.");
        Objects.requireNonNull(layoutPattern, "layoutPattern darf nicht null sein.");
        routingCommands = normalizeRoutingCommands(routingCommands);
        routingQuarterTurns = Math.floorMod(routingQuarterTurns, 4);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Der Heizbereichsname darf nicht leer sein.");
        }
        if (!Double.isFinite(heatOutputWattsPerSquareMeter) || heatOutputWattsPerSquareMeter < 0.0) {
            throw new IllegalArgumentException("Die Heizleistung pro Quadratmeter darf nicht negativ sein.");
        }
        if (outline.size() < 3) {
            throw new IllegalArgumentException("Ein Heizbereich braucht mindestens drei Eckpunkte.");
        }
        outline = List.copyOf(outline);
        supplyConnectionPoint = connectionOrDefault(supplyConnectionPoint, outline, true);
        returnConnectionPoint = connectionOrDefault(returnConnectionPoint, outline, false);
        if (areaSquareMillimeters(outline) < 0.001) {
            throw new IllegalArgumentException("Ein Heizbereich muss eine positive Fläche besitzen.");
        }
        if (!isPointOnBoundary(supplyConnectionPoint, outline)) {
            throw new IllegalArgumentException("Der Vorlaufanschluss muss auf dem Rand des Heizbereichs liegen.");
        }
        if (!isPointOnBoundary(returnConnectionPoint, outline)) {
            throw new IllegalArgumentException("Der Rücklaufanschluss muss auf dem Rand des Heizbereichs liegen.");
        }
    }

    public HeatingZone(
            UUID id,
            String name,
            List<PlanPoint> outline,
            HeatingLayoutPattern layoutPattern,
            boolean flowInverted,
            PlanPoint supplyConnectionPoint,
            PlanPoint returnConnectionPoint
    ) {
        this(id, name, outline, layoutPattern, flowInverted, supplyConnectionPoint, returnConnectionPoint, "", false, 0.0);
    }

    public HeatingZone(
            UUID id,
            String name,
            List<PlanPoint> outline,
            HeatingLayoutPattern layoutPattern,
            boolean flowInverted,
            PlanPoint supplyConnectionPoint,
            PlanPoint returnConnectionPoint,
            String routingCommands,
            boolean serpentineMiddleLine,
            double heatOutputWattsPerSquareMeter
    ) {
        this(id, name, outline, layoutPattern, flowInverted, supplyConnectionPoint, returnConnectionPoint,
                routingCommands, serpentineMiddleLine, heatOutputWattsPerSquareMeter, 0, false, false);
    }

    public HeatingZone(
            UUID id,
            String name,
            List<PlanPoint> outline,
            HeatingLayoutPattern layoutPattern,
            boolean flowInverted
    ) {
        this(id, name, outline, layoutPattern, flowInverted, null, null);
    }

    public HeatingZone(UUID id, String name, List<PlanPoint> outline) {
        this(id, name, outline, HeatingLayoutPattern.SPIRAL, false);
    }

    public static HeatingZone create(String name, List<PlanPoint> outline) {
        return create(name, outline, HeatingLayoutPattern.SPIRAL);
    }

    public static HeatingZone create(String name, List<PlanPoint> outline, HeatingLayoutPattern layoutPattern) {
        return new HeatingZone(UUID.randomUUID(), name, outline, layoutPattern, false);
    }

    public double areaSquareMillimeters() {
        return areaSquareMillimeters(outline);
    }

    public double areaSquareMeters() {
        return areaSquareMillimeters() / 1_000_000.0;
    }

    public double heatOutputWatts() {
        return areaSquareMeters() * heatOutputWattsPerSquareMeter;
    }

    public PlanPoint routingStartPoint() {
        double minX = outline.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0);
        double maxX = outline.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0);
        double minY = outline.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0);
        double maxY = outline.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0);
        return new PlanPoint((minX + maxX) / 2.0, (minY + maxY) / 2.0);
    }

    public boolean hasRoutingCommands() {
        return !routingCommands.isBlank();
    }

    private static double areaSquareMillimeters(List<PlanPoint> points) {
        double doubleArea = 0.0;
        for (int index = 0; index < points.size(); index++) {
            PlanPoint current = points.get(index);
            PlanPoint next = points.get((index + 1) % points.size());
            doubleArea += current.xMillimeters() * next.yMillimeters()
                    - next.xMillimeters() * current.yMillimeters();
        }
        return Math.abs(doubleArea) / 2.0;
    }

    public HeatingZone withOutline(List<PlanPoint> newOutline) {
        return new HeatingZone(
                id, name, newOutline, layoutPattern, flowInverted,
                isPointOnBoundary(supplyConnectionPoint, newOutline) ? supplyConnectionPoint : null,
                isPointOnBoundary(returnConnectionPoint, newOutline) ? returnConnectionPoint : null,
                routingCommands, serpentineMiddleLine, heatOutputWattsPerSquareMeter,
                routingQuarterTurns, routingMirroredHorizontally, routingMirroredVertically
        );
    }

    public HeatingZone withName(String newName) {
        return new HeatingZone(
                id, newName, outline, layoutPattern, flowInverted, supplyConnectionPoint, returnConnectionPoint,
                routingCommands, serpentineMiddleLine, heatOutputWattsPerSquareMeter,
                routingQuarterTurns, routingMirroredHorizontally, routingMirroredVertically
        );
    }

    public HeatingZone withLayoutPattern(HeatingLayoutPattern newLayoutPattern) {
        return new HeatingZone(
                id, name, outline, newLayoutPattern, flowInverted, supplyConnectionPoint, returnConnectionPoint,
                routingCommands, serpentineMiddleLine, heatOutputWattsPerSquareMeter,
                routingQuarterTurns, routingMirroredHorizontally, routingMirroredVertically
        );
    }

    public HeatingZone withFlowInverted(boolean newFlowInverted) {
        return new HeatingZone(
                id, name, outline, layoutPattern, newFlowInverted, supplyConnectionPoint, returnConnectionPoint,
                routingCommands, serpentineMiddleLine, heatOutputWattsPerSquareMeter,
                routingQuarterTurns, routingMirroredHorizontally, routingMirroredVertically
        );
    }

    public HeatingZone withSupplyConnectionPoint(PlanPoint point) {
        return new HeatingZone(
                id, name, outline, layoutPattern, flowInverted, point, returnConnectionPoint,
                routingCommands, serpentineMiddleLine, heatOutputWattsPerSquareMeter,
                routingQuarterTurns, routingMirroredHorizontally, routingMirroredVertically
        );
    }

    public HeatingZone withReturnConnectionPoint(PlanPoint point) {
        return new HeatingZone(
                id, name, outline, layoutPattern, flowInverted, supplyConnectionPoint, point,
                routingCommands, serpentineMiddleLine, heatOutputWattsPerSquareMeter,
                routingQuarterTurns, routingMirroredHorizontally, routingMirroredVertically
        );
    }

    public HeatingZone withRoutingCommands(String newRoutingCommands, boolean newSerpentineMiddleLine) {
        return new HeatingZone(
                id, name, outline, layoutPattern, flowInverted, supplyConnectionPoint, returnConnectionPoint,
                newRoutingCommands, newSerpentineMiddleLine, heatOutputWattsPerSquareMeter,
                routingQuarterTurns, routingMirroredHorizontally, routingMirroredVertically
        );
    }

    public HeatingZone withHeatOutputWattsPerSquareMeter(double newHeatOutputWattsPerSquareMeter) {
        return new HeatingZone(
                id, name, outline, layoutPattern, flowInverted, supplyConnectionPoint, returnConnectionPoint,
                routingCommands, serpentineMiddleLine, newHeatOutputWattsPerSquareMeter,
                routingQuarterTurns, routingMirroredHorizontally, routingMirroredVertically
        );
    }

    public HeatingZone translatedBy(double deltaXMillimeters, double deltaYMillimeters) {
        return new HeatingZone(
                id,
                name,
                outline.stream()
                        .map(point -> translatePoint(point, deltaXMillimeters, deltaYMillimeters))
                        .toList(),
                layoutPattern,
                flowInverted,
                translatePoint(supplyConnectionPoint, deltaXMillimeters, deltaYMillimeters),
                translatePoint(returnConnectionPoint, deltaXMillimeters, deltaYMillimeters),
                routingCommands,
                serpentineMiddleLine,
                heatOutputWattsPerSquareMeter,
                routingQuarterTurns,
                routingMirroredHorizontally,
                routingMirroredVertically
        );
    }

    public HeatingZone withRoutingTransform(
            int newRoutingQuarterTurns,
            boolean newRoutingMirroredHorizontally,
            boolean newRoutingMirroredVertically
    ) {
        return new HeatingZone(
                id, name, outline, layoutPattern, flowInverted, supplyConnectionPoint, returnConnectionPoint,
                routingCommands, serpentineMiddleLine, heatOutputWattsPerSquareMeter,
                newRoutingQuarterTurns, newRoutingMirroredHorizontally, newRoutingMirroredVertically
        );
    }

    public HeatingZone withRoutingRotated(boolean clockwise) {
        RoutingTransform transform = RoutingTransform.from(this)
                .then(clockwise ? RoutingTransform.clockwise() : RoutingTransform.counterClockwise());
        return withRoutingTransform(transform.quarterTurns(), transform.mirroredHorizontally(), transform.mirroredVertically());
    }

    public HeatingZone withRoutingMirroredHorizontally() {
        RoutingTransform transform = RoutingTransform.from(this).then(RoutingTransform.mirrorHorizontally());
        return withRoutingTransform(transform.quarterTurns(), transform.mirroredHorizontally(), transform.mirroredVertically());
    }

    public HeatingZone withRoutingMirroredVertically() {
        RoutingTransform transform = RoutingTransform.from(this).then(RoutingTransform.mirrorVertically());
        return withRoutingTransform(transform.quarterTurns(), transform.mirroredHorizontally(), transform.mirroredVertically());
    }

    public boolean hasCustomConnectionPoints() {
        return !samePoint(supplyConnectionPoint, defaultConnection(outline, true))
                || !samePoint(returnConnectionPoint, defaultConnection(outline, false));
    }

    private static PlanPoint connectionOrDefault(PlanPoint point, List<PlanPoint> outline, boolean supply) {
        if (point != null) {
            return point;
        }
        return defaultConnection(outline, supply);
    }

    private static PlanPoint defaultConnection(List<PlanPoint> outline, boolean supply) {
        int startIndex = supply ? 0 : 1 % outline.size();
        int endIndex = supply ? outline.size() - 1 : 2 % outline.size();
        PlanPoint start = outline.get(startIndex);
        PlanPoint end = outline.get(endIndex);
        return new PlanPoint(
                (start.xMillimeters() + end.xMillimeters()) / 2.0,
                (start.yMillimeters() + end.yMillimeters()) / 2.0
        );
    }

    private static boolean samePoint(PlanPoint first, PlanPoint second) {
        return Math.abs(first.xMillimeters() - second.xMillimeters()) <= 0.001
                && Math.abs(first.yMillimeters() - second.yMillimeters()) <= 0.001;
    }

    private static PlanPoint translatePoint(PlanPoint point, double deltaXMillimeters, double deltaYMillimeters) {
        return new PlanPoint(point.xMillimeters() + deltaXMillimeters, point.yMillimeters() + deltaYMillimeters);
    }

    private static String normalizeRoutingCommands(String commands) {
        if (commands == null || commands.isBlank()) {
            return "";
        }
        StringBuilder normalized = new StringBuilder();
        for (int index = 0; index < commands.length(); index++) {
            char character = commands.charAt(index);
            if (Character.isWhitespace(character)) {
                continue;
            }
            if (!isRoutingCommand(character)) {
                throw new IllegalArgumentException("Unbekannter Routing-Befehl `" + character + "`.");
            }
            normalized.append(character);
        }
        return normalized.toString();
    }

    private static boolean isRoutingCommand(char character) {
        return switch (character) {
            case 'I', 'i', 'R', 'r', 'L', 'l', 'X', 'x' -> true;
            default -> false;
        };
    }

    private static boolean isPointOnBoundary(PlanPoint point, List<PlanPoint> outline) {
        for (int index = 0; index < outline.size(); index++) {
            if (pointOnSegment(point, outline.get(index), outline.get((index + 1) % outline.size()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean pointOnSegment(PlanPoint point, PlanPoint start, PlanPoint end) {
        double cross = (end.xMillimeters() - start.xMillimeters()) * (point.yMillimeters() - start.yMillimeters())
                - (end.yMillimeters() - start.yMillimeters()) * (point.xMillimeters() - start.xMillimeters());
        if (Math.abs(cross) > 0.001) {
            return false;
        }
        return point.xMillimeters() >= Math.min(start.xMillimeters(), end.xMillimeters()) - 0.001
                && point.xMillimeters() <= Math.max(start.xMillimeters(), end.xMillimeters()) + 0.001
                && point.yMillimeters() >= Math.min(start.yMillimeters(), end.yMillimeters()) - 0.001
                && point.yMillimeters() <= Math.max(start.yMillimeters(), end.yMillimeters()) + 0.001;
    }

    private record RoutingTransform(int quarterTurns, boolean mirroredHorizontally, boolean mirroredVertically) {

        private static RoutingTransform from(HeatingZone zone) {
            return new RoutingTransform(
                    zone.routingQuarterTurns(),
                    zone.routingMirroredHorizontally(),
                    zone.routingMirroredVertically()
            );
        }

        private static RoutingTransform clockwise() {
            return new RoutingTransform(1, false, false);
        }

        private static RoutingTransform counterClockwise() {
            return new RoutingTransform(3, false, false);
        }

        private static RoutingTransform mirrorHorizontally() {
            return new RoutingTransform(0, true, false);
        }

        private static RoutingTransform mirrorVertically() {
            return new RoutingTransform(0, false, true);
        }

        private RoutingTransform then(RoutingTransform operation) {
            return fromMatrix(multiply(operation.matrix(), matrix()));
        }

        private int[] matrix() {
            int[] matrix = identity();
            if (mirroredHorizontally) {
                matrix = multiply(horizontalMirrorMatrix(), matrix);
            }
            if (mirroredVertically) {
                matrix = multiply(verticalMirrorMatrix(), matrix);
            }
            for (int index = 0; index < Math.floorMod(quarterTurns, 4); index++) {
                matrix = multiply(clockwiseMatrix(), matrix);
            }
            return matrix;
        }

        private static RoutingTransform fromMatrix(int[] target) {
            for (int turns = 0; turns < 4; turns++) {
                for (boolean horizontal : List.of(false, true)) {
                    for (boolean vertical : List.of(false, true)) {
                        RoutingTransform candidate = new RoutingTransform(turns, horizontal, vertical);
                        if (sameMatrix(candidate.matrix(), target)) {
                            return candidate;
                        }
                    }
                }
            }
            throw new IllegalStateException("Routing-Transformation ist nicht darstellbar.");
        }

        private static int[] identity() {
            return new int[]{1, 0, 0, 1};
        }

        private static int[] clockwiseMatrix() {
            return new int[]{0, 1, -1, 0};
        }

        private static int[] horizontalMirrorMatrix() {
            return new int[]{-1, 0, 0, 1};
        }

        private static int[] verticalMirrorMatrix() {
            return new int[]{1, 0, 0, -1};
        }

        private static int[] multiply(int[] left, int[] right) {
            return new int[]{
                    left[0] * right[0] + left[1] * right[2],
                    left[0] * right[1] + left[1] * right[3],
                    left[2] * right[0] + left[3] * right[2],
                    left[2] * right[1] + left[3] * right[3]
            };
        }

        private static boolean sameMatrix(int[] first, int[] second) {
            return first[0] == second[0] && first[1] == second[1]
                    && first[2] == second[2] && first[3] == second[3];
        }
    }
}
