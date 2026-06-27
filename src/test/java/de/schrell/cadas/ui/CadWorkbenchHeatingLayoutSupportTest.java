package de.schrell.cadas.ui;

import de.schrell.cadas.application.heating.HeatingCircuitRoutingService;
import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CadWorkbenchHeatingLayoutSupportTest {

    @Test
    void ermitteltBetroffeneHeizungenAusAuswahl() {
        Level level = new Level("Erdgeschoss");
        HydronicHeating heating = heatingMitZone();
        level.addHydronicHeating(heating);
        Set<SelectionKey> selection = new LinkedHashSet<>();
        selection.add(new SelectionKey(RenderableKind.HEATING_ZONE, level.name(), heating.zones().getFirst().id().toString()));
        selection.add(new SelectionKey(RenderableKind.HEATING_MANIFOLD, level.name(), heating.id().toString()));

        Set<UUID> affected = CadWorkbenchHeatingLayoutSupport.affectedHeatingIds(selection, level);

        assertEquals(Set.of(heating.id()), affected);
    }

    @Test
    void findetHeizungZuHeizkreis() {
        Level level = new Level("Erdgeschoss");
        HydronicHeating heating = heatingMitZone();
        level.addHydronicHeating(heating);

        Optional<UUID> heatingId = CadWorkbenchHeatingLayoutSupport.heatingIdForZone(
                level,
                heating.zones().getFirst().id()
        );

        assertEquals(Optional.of(heating.id()), heatingId);
    }

    @Test
    void verwendetLayoutCacheWieder() {
        Map<UUID, HydronicHeatingLayoutService.PlanningResult> cache = new HashMap<>();
        HydronicHeatingLayoutService layoutService = new HydronicHeatingLayoutService();
        HydronicHeating heating = heatingOhneZonen();

        HydronicHeatingLayoutService.PlanningResult first = CadWorkbenchHeatingLayoutSupport.heatingLayouts(cache, layoutService, heating);
        HydronicHeatingLayoutService.PlanningResult second = CadWorkbenchHeatingLayoutSupport.heatingLayouts(cache, layoutService, heating);

        assertSame(first, second);
        assertEquals(1, cache.size());
    }

    @Test
    void berechnetDirtyLayoutsNeuUndLeertStatus() {
        Level level = new Level("Erdgeschoss");
        HydronicHeating heating = heatingOhneZonen();
        level.addHydronicHeating(heating);
        Map<UUID, HydronicHeatingLayoutService.PlanningResult> cache = new HashMap<>();
        Set<UUID> pendingRoutingRegeneration = new LinkedHashSet<>();
        Set<UUID> dirtyHeatings = new LinkedHashSet<>(Set.of(heating.id()));

        CadWorkbenchHeatingLayoutSupport.runHeatingLayoutRecalculation(
                level,
                cache,
                pendingRoutingRegeneration,
                dirtyHeatings,
                new HeatingCircuitRoutingService(),
                new HydronicHeatingLayoutService()
        );

        assertTrue(cache.containsKey(heating.id()));
        assertTrue(dirtyHeatings.isEmpty());
    }

    @Test
    void leertAlleHeizlayoutZwischenspeicher() {
        Map<UUID, HydronicHeatingLayoutService.PlanningResult> cache = new HashMap<>();
        Set<UUID> pendingRoutingRegeneration = new LinkedHashSet<>(Set.of(UUID.randomUUID()));
        Set<UUID> dirtyHeatings = new LinkedHashSet<>(Set.of(UUID.randomUUID()));
        HydronicHeating heating = heatingOhneZonen();
        cache.put(heating.id(), new HydronicHeatingLayoutService().layoutBestEffort(heating));

        CadWorkbenchHeatingLayoutSupport.clearHeatingLayoutCache(cache, pendingRoutingRegeneration, dirtyHeatings);

        assertTrue(cache.isEmpty());
        assertTrue(pendingRoutingRegeneration.isEmpty());
        assertTrue(dirtyHeatings.isEmpty());
    }

    @Test
    void meldetSauberenLayoutStatus() {
        Set<UUID> dirtyHeatings = new LinkedHashSet<>();

        assertFalse(CadWorkbenchHeatingLayoutSupport.isHeatingLayoutDirty(dirtyHeatings, heatingOhneZonen()));
    }

    private HydronicHeating heatingMitZone() {
        HeatingZone zone = HeatingZone.create("Heizkreis 1", java.util.List.of(
                new PlanPoint(0, 0),
                new PlanPoint(1_000, 0),
                new PlanPoint(1_000, 1_000),
                new PlanPoint(0, 1_000)
        ), HeatingLayoutPattern.MEANDER);
        return heatingOhneZonen().withZones(java.util.List.of(zone));
    }

    private HydronicHeating heatingOhneZonen() {
        UUID roomId = UUID.randomUUID();
        return new HydronicHeating(
                UUID.randomUUID(),
                roomId,
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.MEANDER,
                Length.ofMillimeters(100),
                Length.ofMillimeters(16),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(100, 0),
                java.util.List.of()
        );
    }
}
