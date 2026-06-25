package de.schrell.cadas.application.drawing;

import de.schrell.cadas.application.heating.HeatingCircuitRoutingService;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class HeatingZoneMirrorService {

    private final HeatingCircuitRoutingService heatingCircuitRoutingService = new HeatingCircuitRoutingService();

    public MirrorResult mirror(Level level, Set<SelectionKey> selections, boolean horizontally) {
        Set<String> selectedHeatingZones = selections.stream()
                .filter(selection -> selection.kind() == RenderableKind.HEATING_ZONE)
                .map(SelectionKey::elementId)
                .collect(Collectors.toSet());
        if (selectedHeatingZones.isEmpty()) {
            return new MirrorResult(level.hydronicHeatings(), false);
        }
        List<HydronicHeating> mirroredHeatings = level.hydronicHeatings().stream()
                .map(heating -> mirrorHeatingZones(heating, selectedHeatingZones, horizontally))
                .toList();
        return new MirrorResult(mirroredHeatings, true);
    }

    private HydronicHeating mirrorHeatingZones(
            HydronicHeating heating,
            Set<String> selectedHeatingZones,
            boolean horizontally
    ) {
        if (heating.zones().stream().noneMatch(zone -> selectedHeatingZones.contains(zone.id().toString()))) {
            return heating;
        }
        return heating.withZones(heating.zones().stream()
                .map(zone -> selectedHeatingZones.contains(zone.id().toString())
                        ? mirrorHeatingZone(heating, zone, horizontally)
                        : zone)
                .toList());
    }

    private HeatingZone mirrorHeatingZone(HydronicHeating heating, HeatingZone zone, boolean horizontally) {
        PlanPoint center = zone.routingStartPoint();
        HeatingZone mirrored = new HeatingZone(
                zone.id(),
                zone.name(),
                zone.outline().stream()
                        .map(point -> mirrorPoint(point, center, horizontally))
                        .toList(),
                zone.layoutPattern(),
                zone.flowInverted(),
                mirrorPoint(zone.supplyConnectionPoint(), center, horizontally),
                mirrorPoint(zone.returnConnectionPoint(), center, horizontally),
                zone.routingCommands(),
                zone.serpentineMiddleLine(),
                zone.heatOutputWattsPerSquareMeter(),
                zone.routingQuarterTurns(),
                zone.routingMirroredHorizontally(),
                zone.routingMirroredVertically()
        );
        mirrored = horizontally
                ? mirrored.withRoutingMirroredHorizontally()
                : mirrored.withRoutingMirroredVertically();
        return mirrored.hasRoutingCommands()
                ? heatingCircuitRoutingService.withRoutingCommands(
                        mirrored, heating, mirrored.routingCommands(), mirrored.serpentineMiddleLine()
                )
                : mirrored;
    }

    private PlanPoint mirrorPoint(PlanPoint point, PlanPoint center, boolean horizontally) {
        if (horizontally) {
            return new PlanPoint(
                    center.xMillimeters() * 2.0 - point.xMillimeters(),
                    point.yMillimeters()
            );
        }
        return new PlanPoint(
                point.xMillimeters(),
                center.yMillimeters() * 2.0 - point.yMillimeters()
        );
    }

    public record MirrorResult(List<HydronicHeating> hydronicHeatings, boolean changed) {
    }
}
