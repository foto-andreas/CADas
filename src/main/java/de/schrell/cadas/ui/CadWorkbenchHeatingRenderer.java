package de.schrell.cadas.ui;

import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Rendert Heizkreis-Überlagerungen der Workbench.
 */
final class CadWorkbenchHeatingRenderer {

    private static final Color FLOOR_HEATING_COLOR = Color.web("#c53b32");
    private static final Color CEILING_HEATING_COLOR = Color.web("#2878a8");
    private static final Color SELECTED_ZONE_COLOR = Color.web("#f2a900");
    private static final Color SUPPLY_COLOR = Color.web("#d33b32");
    private static final Color RETURN_COLOR = Color.web("#1f62d0");
    private static final Color START_MARKER_FILL = Color.web("#d00000");
    private static final Color START_MARKER_STROKE = Color.web("#ffffff");
    private static final Color CONNECTION_MARKER_FILL = Color.web("#fcfaf5");
    private static final double ZONE_STROKE_ALPHA = 0.55;
    private static final double ZONE_LINE_WIDTH = 1.0;
    private static final double SELECTED_ZONE_LINE_WIDTH = 2.2;
    private static final double START_MARKER_RADIUS = 4.0;
    private static final double SELECTED_START_MARKER_RADIUS = 5.0;
    private static final double START_MARKER_LINE_WIDTH = 1.0;
    private static final double SELECTED_START_MARKER_LINE_WIDTH = 1.6;
    private static final double CONNECTION_MARKER_RADIUS = 7.0;
    private static final double CONNECTION_MARKER_LINE_WIDTH = 2.0;
    private static final double ROUTING_MIN_TRIM = 0.001;

    private CadWorkbenchHeatingRenderer() {
    }

    static void drawHydronicHeatings(
            GraphicsContext graphics,
            List<HydronicHeating> heatings,
            String levelName,
            Set<SelectionKey> selectedSelections,
            Predicate<HydronicHeating> layoutDirty,
            Function<HydronicHeating, HydronicHeatingLayoutService.PlanningResult> layoutProvider,
            ToDoubleFunction<PlanPoint> screenXMapper,
            ToDoubleFunction<PlanPoint> screenYMapper,
            DoubleSupplier scaleSupplier
    ) {
        for (HydronicHeating heating : heatings) {
            Color color = heatingColor(heating);
            boolean dirty = layoutDirty.test(heating);
            Map<UUID, HydronicHeatingLayoutService.CircuitLayout> circuitsByZone = dirty
                    ? Map.of()
                    : layoutProvider.apply(heating).circuits().stream()
                    .collect(Collectors.toMap(HydronicHeatingLayoutService.CircuitLayout::zoneId, circuit -> circuit));
            graphics.setStroke(alphaColor(color));
            graphics.setLineWidth(ZONE_LINE_WIDTH);
            graphics.setLineDashes(5.0, 4.0);
            for (HeatingZone zone : heating.zones()) {
                boolean selected = isSelectedZone(selectedSelections, levelName, zone);
                graphics.setStroke(selected ? SELECTED_ZONE_COLOR : alphaColor(color));
                graphics.setLineWidth(selected ? SELECTED_ZONE_LINE_WIDTH : ZONE_LINE_WIDTH);
                double[] xPoints = zone.outline().stream().mapToDouble(screenXMapper).toArray();
                double[] yPoints = zone.outline().stream().mapToDouble(screenYMapper).toArray();
                graphics.strokePolygon(xPoints, yPoints, xPoints.length);
                HydronicHeatingLayoutService.CircuitLayout circuit = circuitsByZone.get(zone.id());
                if (circuit != null && !circuit.fieldSupplyPath().isEmpty() && !circuit.fieldReturnPath().isEmpty()) {
                    drawConnectionMarker(graphics, circuit.fieldSupplyPath().getFirst(), "V", SUPPLY_COLOR, screenXMapper, screenYMapper);
                    drawConnectionMarker(graphics, circuit.fieldReturnPath().getLast(), "R", RETURN_COLOR, screenXMapper, screenYMapper);
                }
            }
            graphics.setLineDashes();
            graphics.setLineWidth(Math.clamp(heating.pipeDiameter().toMillimeters() * scaleSupplier.getAsDouble(), 1.2, 5.0));
            if (!dirty) {
                for (HydronicHeatingLayoutService.CircuitLayout circuit : layoutProvider.apply(heating).circuits()) {
                    drawHeatingRolePath(graphics, circuit.fieldSupplyPath().reversed(), circuit.bendRadius().toMillimeters(),
                            SUPPLY_COLOR, screenXMapper, screenYMapper);
                    drawHeatingRolePath(graphics, circuit.fieldReturnPath(), circuit.bendRadius().toMillimeters(),
                            RETURN_COLOR, screenXMapper, screenYMapper);
                }
            }
            for (HeatingZone zone : heating.zones()) {
                drawRoutingStartMarker(
                        graphics,
                        zone.routingStartPoint(),
                        isSelectedZone(selectedSelections, levelName, zone),
                        screenXMapper,
                        screenYMapper
                );
            }
        }
    }

    static void drawConnectionMarker(
            GraphicsContext graphics,
            PlanPoint point,
            String label,
            Color color,
            ToDoubleFunction<PlanPoint> screenXMapper,
            ToDoubleFunction<PlanPoint> screenYMapper
    ) {
        double x = screenXMapper.applyAsDouble(point);
        double y = screenYMapper.applyAsDouble(point);
        graphics.setFill(CONNECTION_MARKER_FILL);
        graphics.fillOval(x - CONNECTION_MARKER_RADIUS, y - CONNECTION_MARKER_RADIUS,
                CONNECTION_MARKER_RADIUS * 2.0, CONNECTION_MARKER_RADIUS * 2.0);
        graphics.setStroke(color);
        graphics.setLineWidth(CONNECTION_MARKER_LINE_WIDTH);
        graphics.strokeOval(x - CONNECTION_MARKER_RADIUS, y - CONNECTION_MARKER_RADIUS,
                CONNECTION_MARKER_RADIUS * 2.0, CONNECTION_MARKER_RADIUS * 2.0);
        graphics.setFill(color);
        graphics.fillText(label, x - 3.5, y + 4.0);
    }

    private static void drawRoutingStartMarker(
            GraphicsContext graphics,
            PlanPoint point,
            boolean selected,
            ToDoubleFunction<PlanPoint> screenXMapper,
            ToDoubleFunction<PlanPoint> screenYMapper
    ) {
        double radius = selected ? SELECTED_START_MARKER_RADIUS : START_MARKER_RADIUS;
        double x = screenXMapper.applyAsDouble(point);
        double y = screenYMapper.applyAsDouble(point);
        graphics.save();
        graphics.setLineDashes();
        graphics.setFill(START_MARKER_FILL);
        graphics.fillOval(x - radius, y - radius, radius * 2.0, radius * 2.0);
        graphics.setStroke(START_MARKER_STROKE);
        graphics.setLineWidth(selected ? SELECTED_START_MARKER_LINE_WIDTH : START_MARKER_LINE_WIDTH);
        graphics.strokeOval(x - radius, y - radius, radius * 2.0, radius * 2.0);
        graphics.restore();
    }

    private static void drawHeatingRolePath(
            GraphicsContext graphics,
            List<PlanPoint> path,
            double radiusMillimeters,
            Color color,
            ToDoubleFunction<PlanPoint> screenXMapper,
            ToDoubleFunction<PlanPoint> screenYMapper
    ) {
        graphics.setStroke(color);
        drawRoundedHeatingPath(graphics, path, radiusMillimeters, screenXMapper, screenYMapper);
    }

    private static void drawRoundedHeatingPath(
            GraphicsContext graphics,
            List<PlanPoint> path,
            double radiusMillimeters,
            ToDoubleFunction<PlanPoint> screenXMapper,
            ToDoubleFunction<PlanPoint> screenYMapper
    ) {
        if (path.size() < 2) {
            return;
        }
        graphics.beginPath();
        graphics.moveTo(screenXMapper.applyAsDouble(path.getFirst()), screenYMapper.applyAsDouble(path.getFirst()));
        for (int index = 1; index + 1 < path.size(); index++) {
            PlanPoint previous = path.get(index - 1);
            PlanPoint current = path.get(index);
            PlanPoint next = path.get(index + 1);
            double firstLength = previous.distanceTo(current).toMillimeters();
            double secondLength = current.distanceTo(next).toMillimeters();
            double trim = Math.min(radiusMillimeters, Math.min(firstLength, secondLength) / 2.0);
            if (trim <= ROUTING_MIN_TRIM) {
                graphics.lineTo(screenXMapper.applyAsDouble(current), screenYMapper.applyAsDouble(current));
                continue;
            }
            PlanPoint before = interpolateToward(current, previous, trim / firstLength);
            PlanPoint after = interpolateToward(current, next, trim / secondLength);
            graphics.lineTo(screenXMapper.applyAsDouble(before), screenYMapper.applyAsDouble(before));
            graphics.quadraticCurveTo(
                    screenXMapper.applyAsDouble(current), screenYMapper.applyAsDouble(current),
                    screenXMapper.applyAsDouble(after), screenYMapper.applyAsDouble(after)
            );
        }
        graphics.lineTo(screenXMapper.applyAsDouble(path.getLast()), screenYMapper.applyAsDouble(path.getLast()));
        graphics.stroke();
    }

    private static boolean isSelectedZone(Set<SelectionKey> selectedSelections, String levelName, HeatingZone zone) {
        return selectedSelections.contains(new SelectionKey(RenderableKind.HEATING_ZONE, levelName, zone.id().toString()));
    }

    private static Color heatingColor(HydronicHeating heating) {
        return heating.surfacePosition() == HeatingSurfacePosition.FLOOR ? FLOOR_HEATING_COLOR : CEILING_HEATING_COLOR;
    }

    private static Color alphaColor(Color color) {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(), ZONE_STROKE_ALPHA);
    }

    private static PlanPoint interpolateToward(PlanPoint start, PlanPoint target, double ratio) {
        return new PlanPoint(
                start.xMillimeters() + (target.xMillimeters() - start.xMillimeters()) * ratio,
                start.yMillimeters() + (target.yMillimeters() - start.yMillimeters()) * ratio
        );
    }
}
