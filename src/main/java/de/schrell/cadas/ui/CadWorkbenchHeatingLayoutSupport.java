package de.schrell.cadas.ui;

import de.schrell.cadas.application.heating.HeatingCircuitRoutingService;
import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Kapselt Cache- und Neuberechnungslogik für Heizkreis-Layouts.
 */
final class CadWorkbenchHeatingLayoutSupport {

    private CadWorkbenchHeatingLayoutSupport() {
    }

    static HydronicHeatingLayoutService.PlanningResult heatingLayouts(
            Map<UUID, HydronicHeatingLayoutService.PlanningResult> heatingLayoutCache,
            HydronicHeatingLayoutService hydronicHeatingLayoutService,
            HydronicHeating heating
    ) {
        HydronicHeatingLayoutService.PlanningResult cached = heatingLayoutCache.get(heating.id());
        if (cached != null) {
            return cached;
        }
        HydronicHeatingLayoutService.PlanningResult computed = hydronicHeatingLayoutService.layoutBestEffort(heating);
        heatingLayoutCache.put(heating.id(), computed);
        return computed;
    }

    static boolean isHeatingLayoutDirty(Set<UUID> heatingLayoutsDirty, HydronicHeating heating) {
        return heatingLayoutsDirty.contains(heating.id());
    }

    static Set<UUID> affectedHeatingIds(Set<SelectionKey> selectedSelections, Level level) {
        Set<UUID> affected = new HashSet<>();
        for (SelectionKey selection : selectedSelections) {
            switch (selection.kind()) {
                case HEATING_ZONE -> level.hydronicHeatings().stream()
                        .filter(heating -> heating.zones().stream()
                                .anyMatch(zone -> zone.id().toString().equals(selection.elementId())))
                        .findFirst()
                        .ifPresent(heating -> affected.add(heating.id()));
                case HEATING_MANIFOLD -> affected.add(UUID.fromString(selection.elementId()));
                default -> {
                }
            }
        }
        return affected;
    }

    static Optional<UUID> heatingIdForZone(Level level, UUID zoneId) {
        return level.hydronicHeatings().stream()
                .filter(heating -> heating.zones().stream().anyMatch(zone -> zone.id().equals(zoneId)))
                .map(HydronicHeating::id)
                .findFirst();
    }

    static void runHeatingLayoutRecalculation(
            Level level,
            Map<UUID, HydronicHeatingLayoutService.PlanningResult> heatingLayoutCache,
            Set<UUID> heatingZonesPendingRoutingRegeneration,
            Set<UUID> heatingLayoutsDirty,
            HeatingCircuitRoutingService heatingCircuitRoutingService,
            HydronicHeatingLayoutService hydronicHeatingLayoutService
    ) {
        Set<UUID> regeneratedHeatingIds = regeneratePendingHeatingZoneRouting(
                level,
                heatingZonesPendingRoutingRegeneration,
                heatingCircuitRoutingService
        );
        Set<UUID> toRecompute = new HashSet<>(heatingLayoutsDirty);
        toRecompute.addAll(regeneratedHeatingIds);
        for (UUID id : toRecompute) {
            level.hydronicHeatings().stream()
                    .filter(heating -> heating.id().equals(id))
                    .findFirst()
                    .ifPresent(heating -> heatingLayoutCache.put(id, hydronicHeatingLayoutService.layoutBestEffort(heating)));
        }
        heatingLayoutsDirty.clear();
    }

    static void recomputeHeatingLayoutNow(
            UUID heatingId,
            Level level,
            Map<UUID, HydronicHeatingLayoutService.PlanningResult> heatingLayoutCache,
            Set<UUID> heatingLayoutsDirty,
            HydronicHeatingLayoutService hydronicHeatingLayoutService
    ) {
        heatingLayoutsDirty.remove(heatingId);
        level.hydronicHeatings().stream()
                .filter(heating -> heating.id().equals(heatingId))
                .findFirst()
                .ifPresent(heating -> heatingLayoutCache.put(heatingId, hydronicHeatingLayoutService.layoutBestEffort(heating)));
    }

    static void clearHeatingLayoutCache(
            Map<UUID, HydronicHeatingLayoutService.PlanningResult> heatingLayoutCache,
            Set<UUID> heatingZonesPendingRoutingRegeneration,
            Set<UUID> heatingLayoutsDirty
    ) {
        heatingLayoutCache.clear();
        heatingZonesPendingRoutingRegeneration.clear();
        heatingLayoutsDirty.clear();
    }

    private static Set<UUID> regeneratePendingHeatingZoneRouting(
            Level level,
            Set<UUID> heatingZonesPendingRoutingRegeneration,
            HeatingCircuitRoutingService heatingCircuitRoutingService
    ) {
        Set<UUID> regeneratedHeatingIds = new HashSet<>();
        if (heatingZonesPendingRoutingRegeneration.isEmpty()) {
            return regeneratedHeatingIds;
        }
        for (HydronicHeating heating : List.copyOf(level.hydronicHeatings())) {
            List<HeatingZone> zones = new ArrayList<>(heating.zones());
            boolean changed = false;
            for (int index = 0; index < zones.size(); index++) {
                HeatingZone zone = zones.get(index);
                if (heatingZonesPendingRoutingRegeneration.contains(zone.id())) {
                    try {
                        zones.set(index, heatingCircuitRoutingService.regenerate(zone, heating));
                        changed = true;
                    } catch (RuntimeException exception) {
                        // Bei fehlgeschlagener Neugenerierung bleibt das bestehende Routing erhalten.
                    }
                }
            }
            if (changed) {
                level.replaceHydronicHeating(heating.withZones(zones));
                regeneratedHeatingIds.add(heating.id());
            }
        }
        heatingZonesPendingRoutingRegeneration.clear();
        return regeneratedHeatingIds;
    }
}
