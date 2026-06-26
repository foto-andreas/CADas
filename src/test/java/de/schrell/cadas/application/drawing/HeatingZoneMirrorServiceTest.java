package de.schrell.cadas.application.drawing;

import de.schrell.cadas.application.heating.HeatingCircuitRoutingService;
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

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeatingZoneMirrorServiceTest {

    private final HeatingZoneMirrorService service = new HeatingZoneMirrorService();

    @Test
    void spiegeltAusgewaehlteHeizkreiseHorizontalUndVertikal() {
        Level level = new Level("Erdgeschoss");
        HydronicHeating heating = heating();
        HeatingZone zone = new HeatingCircuitRoutingService().regenerate(new HeatingZone(
                java.util.UUID.randomUUID(),
                "HK 1",
                List.of(
                        new PlanPoint(1_000, 1_000),
                        new PlanPoint(3_000, 1_000),
                        new PlanPoint(3_000, 2_000),
                        new PlanPoint(1_000, 2_000)
                ),
                HeatingLayoutPattern.VARIO,
                false
        ), heating);
        level.addHydronicHeating(heating.withZones(List.of(zone)));
        PlanPoint routingStartPoint = zone.routingStartPoint();

        HeatingZoneMirrorService.MirrorResult horizontal = service.mirror(level, selection(level, zone), true);
        level.replaceHydronicHeatings(horizontal.hydronicHeatings());
        HeatingZoneMirrorService.MirrorResult vertical = service.mirror(level, selection(level, zone), false);

        HeatingZone mirrored = vertical.hydronicHeatings().getFirst().zones().getFirst();
        assertTrue(horizontal.changed());
        assertTrue(vertical.changed());
        assertEquals(2_858.0, mirrored.outline().getFirst().xMillimeters(), 0.001);
        assertEquals(2_058.0, mirrored.outline().getFirst().yMillimeters(), 0.001);
        assertEquals(routingStartPoint, mirrored.routingStartPoint());
        assertTrue(mirrored.routingQuarterTurns() == 2
                || mirrored.routingMirroredHorizontally()
                || mirrored.routingMirroredVertically());
    }

    private Set<SelectionKey> selection(Level level, HeatingZone zone) {
        return Set.of(new SelectionKey(RenderableKind.HEATING_ZONE, level.name(), zone.id().toString()));
    }

    private HydronicHeating heating() {
        return HydronicHeating.create(
                java.util.UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.VARIO,
                Length.ofMillimeters(100),
                Length.ofMillimeters(16),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        );
    }
}
