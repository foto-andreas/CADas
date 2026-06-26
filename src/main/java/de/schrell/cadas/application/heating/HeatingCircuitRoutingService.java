package de.schrell.cadas.application.heating;

import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;

import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.PipePath;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.PipePrimitive;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.QuarterArc;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingPoint;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingResult;

import java.util.List;
import java.util.Objects;

public final class HeatingCircuitRoutingService {

    private static final int TRAILING_CONNECTOR_PRIMITIVES = 2;
    private final HeatingCircuitCommandRouter commandRouter = new HeatingCircuitCommandRouter();

    public HeatingZone regenerate(HeatingZone zone, HydronicHeating heating) {
        Objects.requireNonNull(zone, "zone darf nicht null sein.");
        Objects.requireNonNull(heating, "heating darf nicht null sein.");
        Bounds bounds = bounds(zone.outline());
        HeatingLayoutPattern pattern = manualPattern(zone.layoutPattern());
        String commands = generatedCommands(
                pattern,
                bounds.width(),
                bounds.height(),
                heating.pipeSpacing().toMillimeters(),
                zone.serpentineMiddleLine()
        );
        return withRoutingCommands(zone.withLayoutPattern(pattern), heating, commands, zone.serpentineMiddleLine());
    }

    public HeatingZone regenerateWithPattern(
            HeatingZone zone,
            HydronicHeating heating,
            HeatingLayoutPattern pattern,
            boolean serpentineMiddleLine
    ) {
        HeatingZone prepared = new HeatingZone(
                zone.id(),
                zone.name(),
                zone.outline(),
                manualPattern(pattern),
                zone.flowInverted(),
                zone.supplyConnectionPoint(),
                zone.returnConnectionPoint(),
                zone.routingStartPoint(),
                zone.routingCommands(),
                serpentineMiddleLine,
                zone.heatOutputWattsPerSquareMeter(),
                zone.routingQuarterTurns(),
                zone.routingMirroredHorizontally(),
                zone.routingMirroredVertically()
        );
        return regenerate(prepared, heating);
    }

    public HeatingZone withRoutingCommands(
            HeatingZone zone,
            HydronicHeating heating,
            String commands,
            boolean serpentineMiddleLine
    ) {
        HeatingZone routed = new HeatingZone(
                zone.id(),
                zone.name(),
                zone.outline(),
                manualPattern(zone.layoutPattern()),
                zone.flowInverted(),
                zone.supplyConnectionPoint(),
                zone.returnConnectionPoint(),
                zone.routingStartPoint(),
                commands,
                serpentineMiddleLine,
                zone.heatOutputWattsPerSquareMeter(),
                zone.routingQuarterTurns(),
                zone.routingMirroredHorizontally(),
                zone.routingMirroredVertically()
        );
        RoutingResult result = placedRoutingResult(routed, heating);
        PlanPoint routingStartPoint = toPlanPoint(result.supplyPath().startPoint());
        List<PlanPoint> adjustedOutline = adjustedOutline(routed.outline(), result, heating);
        return new HeatingZone(
                routed.id(),
                routed.name(),
                adjustedOutline,
                routed.layoutPattern(),
                routed.flowInverted(),
                toPlanPoint(result.supplyPath().endPoint()),
                toPlanPoint(result.returnPath().endPoint()),
                routingStartPoint,
                routed.routingCommands(),
                routed.serpentineMiddleLine(),
                routed.heatOutputWattsPerSquareMeter(),
                routed.routingQuarterTurns(),
                routed.routingMirroredHorizontally(),
                routed.routingMirroredVertically()
        );
    }

    public RoutingResult placedRoutingResult(HeatingZone zone, HydronicHeating heating) {
        Objects.requireNonNull(zone, "zone darf nicht null sein.");
        Objects.requireNonNull(heating, "heating darf nicht null sein.");
        if (!zone.hasRoutingCommands()) {
            throw new IllegalArgumentException("Der Heizkreis besitzt keine Routing-Kommandos.");
        }
        Bounds bounds = bounds(zone.outline());
        double spacingMillimeters = heating.pipeSpacing().toMillimeters();
        RoutingResult result = commandRouter.route(bounds.width(), bounds.height(), spacingMillimeters, zone.routingCommands())
                .withFlowInverted(zone.flowInverted());
        if (bounds.width() > bounds.height()) {
            result = result.rotatedClockwise();
        }
        result = applyStoredTransform(result, zone);
        return result.translatedBy(zone.routingStartPoint().xMillimeters(), zone.routingStartPoint().yMillimeters());
    }

    private RoutingResult applyStoredTransform(RoutingResult result, HeatingZone zone) {
        RoutingResult transformed = result;
        if (zone.routingMirroredHorizontally()) {
            transformed = transformed.mirroredHorizontally();
        }
        if (zone.routingMirroredVertically()) {
            transformed = transformed.mirroredVertically();
        }
        for (int index = 0; index < zone.routingQuarterTurns(); index++) {
            transformed = transformed.rotatedClockwise();
        }
        return transformed;
    }

    public String generatedCommands(
            HeatingLayoutPattern pattern,
            double widthMillimeters,
            double heightMillimeters,
            double spacingMillimeters,
            boolean serpentineMiddleLine
    ) {
        return switch (manualPattern(pattern)) {
            case MEANDER -> commandRouter.meanderCommands(widthMillimeters, heightMillimeters, spacingMillimeters, serpentineMiddleLine);
            case VARIO -> commandRouter.rectangularVarioCommands(widthMillimeters, heightMillimeters, spacingMillimeters, serpentineMiddleLine);
            case SPIRAL -> throw new IllegalStateException("Schnecke wird für neue manuelle Heizkreise nicht mehr generiert.");
        };
    }

    public HeatingLayoutPattern manualPattern(HeatingLayoutPattern pattern) {
        return pattern == HeatingLayoutPattern.MEANDER ? HeatingLayoutPattern.MEANDER : HeatingLayoutPattern.VARIO;
    }

    private Bounds routeBounds(RoutingResult result, int trailingPrimitivesToIgnore) {
Ï        return Bounds.from(result.supplyPath().startPoint())
                .include(result.supplyPath(), trailingPrimitivesToIgnore)
                .include(result.returnPath(), trailingPrimitivesToIgnore);
    }

    private List<PlanPoint> adjustedOutline(List<PlanPoint> outline, RoutingResult result, HydronicHeating heating) {
        if (!isAxisAlignedRectangle(outline)) {
            return outline;
        }
        Bounds routeBounds = routeBounds(result, TRAILING_CONNECTOR_PRIMITIVES).expanded(heating.pipeDiameter().toMillimeters() / 2.0);
        return List.of(
                new PlanPoint(routeBounds.minX(), routeBounds.minY()),
                new PlanPoint(routeBounds.maxX(), routeBounds.minY()),
                new PlanPoint(routeBounds.maxX(), routeBounds.maxY()),
                new PlanPoint(routeBounds.minX(), routeBounds.maxY())
        );
    }

    private boolean isAxisAlignedRectangle(List<PlanPoint> outline) {
        if (outline.size() != 4) {
            return false;
        }
        return distinctCoordinates(outline, true).size() == 2 && distinctCoordinates(outline, false).size() == 2;
    }

    private List<Double> distinctCoordinates(List<PlanPoint> outline, boolean xAxis) {
        java.util.ArrayList<Double> coordinates = new java.util.ArrayList<>();
        for (PlanPoint point : outline) {
            double coordinate = xAxis ? point.xMillimeters() : point.yMillimeters();
            if (coordinates.stream().noneMatch(existing -> Math.abs(existing - coordinate) <= 0.001)) {
                coordinates.add(coordinate);
            }
        }
        return List.copyOf(coordinates);
    }

    private PlanPoint toPlanPoint(RoutingPoint point) {
        return new PlanPoint(point.xMillimeters(), point.yMillimeters());
    }

    private PlanPoint nearestBoundaryPoint(Bounds bounds, RoutingPoint point) {
        double x = clamp(point.xMillimeters(), bounds.minX(), bounds.maxX());
        double y = clamp(point.yMillimeters(), bounds.minY(), bounds.maxY());
        double left = Math.abs(x - bounds.minX());
        double right = Math.abs(bounds.maxX() - x);
        double top = Math.abs(y - bounds.minY());
        double bottom = Math.abs(bounds.maxY() - y);
        double nearest = Math.min(Math.min(left, right), Math.min(top, bottom));
        if (nearest == left) {
            x = bounds.minX();
        } else if (nearest == right) {
            x = bounds.maxX();
        } else if (nearest == top) {
            y = bounds.minY();
        } else {
            y = bounds.maxY();
        }
        return new PlanPoint(x, y);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Bounds bounds(List<PlanPoint> points) {
        return new Bounds(
                points.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0)
        );
    }

    private record Bounds(double minX, double minY, double maxX, double maxY) {
        private static Bounds from(RoutingPoint point) {
            return new Bounds(
                    point.xMillimeters(),
                    point.yMillimeters(),
                    point.xMillimeters(),
                    point.yMillimeters()
            );
        }

        private Bounds include(PipePath path) {
            return include(path, 0);
        }

        private Bounds include(PipePath path, int trailingPrimitivesToIgnore) {
            if (trailingPrimitivesToIgnore <= 0 || path.primitives().size() <= trailingPrimitivesToIgnore) {
                Bounds result = include(path.startPoint()).include(path.endPoint());
                for (PipePrimitive primitive : path.primitives()) {
                    if (primitive instanceof QuarterArc arc) {
                        result = result.include(arc);
                    } else {
                        result = result.include(primitive.startPoint()).include(primitive.endPoint());
                    }
                }
                return result;
            }
            Bounds result = include(path.startPoint());
            int includedPrimitiveCount = path.primitives().size() - trailingPrimitivesToIgnore;
            for (int index = 0; index < includedPrimitiveCount; index++) {
                PipePrimitive primitive = path.primitives().get(index);
                if (primitive instanceof QuarterArc arc) {
                    result = result.include(arc);
                } else {
                    result = result.include(primitive.startPoint()).include(primitive.endPoint());
                }
            }
            return result;
        }

        private Bounds include(QuarterArc arc) {
            Bounds result = include(arc.startPoint()).include(arc.endPoint());
            double startAngle = normalizedAngle(arc.startPoint(), arc.centerPoint());
            double endAngle = normalizedAngle(arc.endPoint(), arc.centerPoint());
            for (double candidateAngle : new double[]{0.0, Math.PI / 2.0, Math.PI, Math.PI * 1.5}) {
                if (angleLiesOnArc(startAngle, endAngle, candidateAngle, arc.turn())) {
                    result = result.include(new RoutingPoint(
                            arc.centerPoint().xMillimeters() + Math.cos(candidateAngle) * arc.radiusMillimeters(),
                            arc.centerPoint().yMillimeters() + Math.sin(candidateAngle) * arc.radiusMillimeters()
                    ));
                }
            }
            return result;
        }

        private double normalizedAngle(RoutingPoint point, RoutingPoint centerPoint) {
            double angle = Math.atan2(
                    point.yMillimeters() - centerPoint.yMillimeters(),
                    point.xMillimeters() - centerPoint.xMillimeters()
            );
            return angle >= 0.0 ? angle : angle + Math.PI * 2.0;
        }

        private boolean angleLiesOnArc(double startAngle, double endAngle, double candidateAngle, HeatingCircuitCommandRouter.Turn turn) {
            double fullTurn = Math.PI * 2.0;
            double adjustedCandidate = candidateAngle;
            if (turn == HeatingCircuitCommandRouter.Turn.RIGHT) {
                double adjustedEnd = endAngle > startAngle ? endAngle - fullTurn : endAngle;
                if (adjustedCandidate > startAngle) {
                    adjustedCandidate -= fullTurn;
                }
                return adjustedCandidate <= startAngle + 0.000_001 && adjustedCandidate >= adjustedEnd - 0.000_001;
            }
            double adjustedEnd = endAngle < startAngle ? endAngle + fullTurn : endAngle;
            if (adjustedCandidate < startAngle) {
                adjustedCandidate += fullTurn;
            }
            return adjustedCandidate >= startAngle - 0.000_001 && adjustedCandidate <= adjustedEnd + 0.000_001;
        }

        private Bounds include(RoutingPoint point) {
            return new Bounds(
                    Math.min(minX, point.xMillimeters()),
                    Math.min(minY, point.yMillimeters()),
                    Math.max(maxX, point.xMillimeters()),
                    Math.max(maxY, point.yMillimeters())
            );
        }

        private double width() {
            return maxX - minX;
        }

        private double height() {
            return maxY - minY;
        }

        private double centerX() {
            return (minX + maxX) / 2.0;
        }

        private double centerY() {
            return (minY + maxY) / 2.0;
        }

        private Bounds expanded(double marginMillimeters) {
            if (marginMillimeters <= 0.0) {
                return this;
            }
            return new Bounds(
                    minX - marginMillimeters,
                    minY - marginMillimeters,
                    maxX + marginMillimeters,
                    maxY + marginMillimeters
            );
        }
    }
}
