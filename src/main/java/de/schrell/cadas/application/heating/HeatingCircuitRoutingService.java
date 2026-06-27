package de.schrell.cadas.application.heating;

import de.schrell.cadas.application.heating.HeatingCircuitFieldBounds.Bounds;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingPoint;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingResult;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingRoutingLanguage;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;

import java.util.List;
import java.util.Objects;

public final class HeatingCircuitRoutingService {

    private static final double COORDINATE_EPSILON = 0.001;

    private final HeatingCircuitCommandRouter commandRouter = new HeatingCircuitCommandRouter();

    /**
     * Erzeugt das Routing eines Heizkreises neu aus dessen aktueller Geometrie.
     */
    public HeatingZone regenerate(HeatingZone zone, HydronicHeating heating) {
        Objects.requireNonNull(zone, "zone darf nicht null sein.");
        Objects.requireNonNull(heating, "heating darf nicht null sein.");
        OutlineBounds bounds = bounds(zone.outline());
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

    /**
     * Erzeugt ein neues Routing mit explizitem Layoutmuster für den bestehenden Heizkreis.
     */
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

    /**
     * Übernimmt manuelle Routing-Kommandos und synchronisiert daraus Startpunkt, Anschlüsse und Feldrechteck.
     */
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

    /**
     * Berechnet das platzierte Routing inklusive Drehung, Spiegelung und Startpunktverschiebung.
     */
    public RoutingResult placedRoutingResult(HeatingZone zone, HydronicHeating heating) {
        Objects.requireNonNull(zone, "zone darf nicht null sein.");
        Objects.requireNonNull(heating, "heating darf nicht null sein.");
        if (!zone.hasRoutingCommands()) {
            throw new IllegalArgumentException("Der Heizkreis besitzt keine Routing-Kommandos.");
        }
        OutlineBounds bounds = bounds(zone.outline());
        double spacingMillimeters = heating.pipeSpacing().toMillimeters();
        RoutingResult result = commandRouter.route(bounds.width(), bounds.height(), spacingMillimeters, zone.routingCommands())
                .withFlowInverted(zone.flowInverted());
        if (bounds.width() > bounds.height()) {
            result = result.rotatedClockwise();
        }
        result = applyStoredTransform(result, zone);
        return result.translatedBy(zone.routingStartPoint().xMillimeters(), zone.routingStartPoint().yMillimeters());
    }

    /**
     * Erzeugt die Standard-Kommandos für ein fachliches Heizkreis-Muster.
     */
    public String generatedCommands(
            HeatingLayoutPattern pattern,
            double widthMillimeters,
            double heightMillimeters,
            double spacingMillimeters,
            boolean serpentineMiddleLine
    ) {
        String commands = switch (manualPattern(pattern)) {
            case MEANDER -> commandRouter.meanderCommands(widthMillimeters, heightMillimeters, spacingMillimeters, serpentineMiddleLine);
            case VARIO -> commandRouter.rectangularVarioCommands(widthMillimeters, heightMillimeters, spacingMillimeters, serpentineMiddleLine);
            case SPIRAL -> throw new IllegalStateException("Schnecke wird für neue manuelle Heizkreise nicht mehr generiert.");
        };
        return HeatingRoutingLanguage.ensureConnectorSeparator(commands);
    }

    /**
     * Reduziert das fachliche Muster auf die manuell editierbaren Varianten.
     */
    public HeatingLayoutPattern manualPattern(HeatingLayoutPattern pattern) {
        return pattern == HeatingLayoutPattern.MEANDER ? HeatingLayoutPattern.MEANDER : HeatingLayoutPattern.VARIO;
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

    private List<PlanPoint> adjustedOutline(List<PlanPoint> outline, RoutingResult result, HydronicHeating heating) {
        if (!isAxisAlignedRectangle(outline)) {
            return outline;
        }
        Bounds routeBounds = HeatingCircuitFieldBounds.fieldBounds(result)
                .expanded(heating.pipeDiameter().toMillimeters() / 2.0);
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
            if (coordinates.stream().noneMatch(existing -> Math.abs(existing - coordinate) <= COORDINATE_EPSILON)) {
                coordinates.add(coordinate);
            }
        }
        return List.copyOf(coordinates);
    }

    private PlanPoint toPlanPoint(RoutingPoint point) {
        return new PlanPoint(point.xMillimeters(), point.yMillimeters());
    }

    private OutlineBounds bounds(List<PlanPoint> points) {
        return new OutlineBounds(
                points.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0)
        );
    }

    private record OutlineBounds(double minX, double minY, double maxX, double maxY) {
        private double width() {
            return maxX - minX;
        }

        private double height() {
            return maxY - minY;
        }
    }
}
