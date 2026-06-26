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
        return new HeatingZone(
                routed.id(),
                routed.name(),
                routed.outline(),
                routed.layoutPattern(),
                routed.flowInverted(),
                new PlanPoint(result.supplyPath().endPoint().xMillimeters(), result.supplyPath().endPoint().yMillimeters()),
                new PlanPoint(result.returnPath().endPoint().xMillimeters(), result.returnPath().endPoint().yMillimeters()),
                routed.routingStartPoint(),
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
        result = alignVerticallyToGrid(result, bounds.height(), spacingMillimeters);
        result = applyStoredTransform(result, zone);
        return result.translatedBy(bounds.centerX(), bounds.centerY());
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

    private RoutingResult alignVerticallyToGrid(RoutingResult result, double heightMillimeters, double spacingMillimeters) {
        if (result.supplyPath().primitives().isEmpty() && result.returnPath().primitives().isEmpty()) {
            return result;
        }
        Bounds bounds = routeBounds(result);
        double halfSpacing = spacingMillimeters / 2.0;
        double downOffset = -halfSpacing;
        double upOffset = halfSpacing;
        double offset = verticalAlignmentScore(bounds, heightMillimeters, downOffset)
                <= verticalAlignmentScore(bounds, heightMillimeters, upOffset)
                ? downOffset
                : upOffset;
        return result.translatedBy(0.0, offset);
    }

    private double verticalAlignmentScore(Bounds bounds, double heightMillimeters, double offsetMillimeters) {
        double bottom = -heightMillimeters / 2.0;
        double top = heightMillimeters / 2.0;
        double shiftedMinY = bounds.minY() + offsetMillimeters;
        double shiftedMaxY = bounds.maxY() + offsetMillimeters;
        double overflow = Math.max(0.0, bottom - shiftedMinY) + Math.max(0.0, shiftedMaxY - top);
        double edgeDistance = Math.min(Math.abs(shiftedMinY - bottom), Math.abs(top - shiftedMaxY));
        return overflow * 1_000.0 + edgeDistance;
    }

    private Bounds routeBounds(RoutingResult result) {
        return new Bounds(0.0, 0.0, 0.0, 0.0)
                .include(result.supplyPath())
                .include(result.returnPath());
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
        private Bounds include(PipePath path) {
            Bounds result = this;
            for (PipePrimitive primitive : path.primitives()) {
                result = result.include(primitive.startPoint()).include(primitive.endPoint());
                if (primitive instanceof QuarterArc arc) {
                    result = result.include(new RoutingPoint(
                            arc.centerPoint().xMillimeters() - arc.radiusMillimeters(),
                            arc.centerPoint().yMillimeters() - arc.radiusMillimeters()
                    )).include(new RoutingPoint(
                            arc.centerPoint().xMillimeters() + arc.radiusMillimeters(),
                            arc.centerPoint().yMillimeters() + arc.radiusMillimeters()
                    ));
                }
            }
            return result;
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
    }
}
