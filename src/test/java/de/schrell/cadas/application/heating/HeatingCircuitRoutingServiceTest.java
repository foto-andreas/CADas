package de.schrell.cadas.application.heating;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeatingCircuitRoutingServiceTest {

    private final HeatingCircuitRoutingService service = new HeatingCircuitRoutingService();

    @Test
    void speichertRohrstartUndRohrhuelleAmTatsaechlichenRouting() {
        HydronicHeating heating = HydronicHeating.create(
                UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.VARIO,
                Length.ofMillimeters(100),
                Length.ofMillimeters(16),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        );
        HeatingZone zone = new HeatingZone(
                UUID.randomUUID(),
                "HK 1",
                List.of(
                        new PlanPoint(0, 0),
                        new PlanPoint(500, 0),
                        new PlanPoint(500, 500),
                        new PlanPoint(0, 500)
                ),
                HeatingLayoutPattern.VARIO,
                false
        );

        HeatingZone routed = service.withRoutingCommands(zone, heating, "RR", false);

        assertEquals(new PlanPoint(250, 300), routed.routingStartPoint());
        assertEquals(new PlanPoint(350, 300), routed.supplyConnectionPoint());
        assertEquals(List.of(
                new PlanPoint(242, 292),
                new PlanPoint(358, 292),
                new PlanPoint(358, 358),
                new PlanPoint(242, 358)
        ), routed.outline());
    }

    @Test
    void verankertIiAmGesnapptenRoutingStartpunkt() {
        HydronicHeating heating = HydronicHeating.create(
                UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.VARIO,
                Length.ofMillimeters(100),
                Length.ofMillimeters(16),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        );
        HeatingZone zone = new HeatingZone(
                UUID.randomUUID(),
                "HK 1",
                List.of(
                        new PlanPoint(0, 0),
                        new PlanPoint(500, 0),
                        new PlanPoint(500, 500),
                        new PlanPoint(0, 500)
                ),
                HeatingLayoutPattern.VARIO,
                false,
                null,
                null,
                new PlanPoint(100, 100)
        );

        HeatingZone routed = service.withRoutingCommands(zone, heating, "Ii", false);

        assertEquals(new PlanPoint(100, 50), routed.routingStartPoint());
        assertEquals(new PlanPoint(100, 150), routed.supplyConnectionPoint());
        assertEquals(new PlanPoint(100, -50), routed.returnConnectionPoint());
        assertEquals(List.of(
                new PlanPoint(92, -58),
                new PlanPoint(108, -58),
                new PlanPoint(108, 158),
                new PlanPoint(92, 158)
        ), routed.outline());
    }

    @Test
    void ignoriertRuecklaufEndsegmenteBeimUmhuellendenHeizkreisrechteck() {
        HydronicHeating heating = HydronicHeating.create(
                UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.VARIO,
                Length.ofMillimeters(100),
                Length.ofMillimeters(16),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        );
        HeatingZone zone = new HeatingZone(
                UUID.randomUUID(),
                "HK 1",
                List.of(
                        new PlanPoint(0, 0),
                        new PlanPoint(500, 0),
                        new PlanPoint(500, 500),
                        new PlanPoint(0, 500)
                ),
                HeatingLayoutPattern.VARIO,
                false
        );

        HeatingZone routed = service.withRoutingCommands(zone, heating, "iIrr", false);

        assertEquals(List.of(
                new PlanPoint(242, 92),
                new PlanPoint(258, 92),
                new PlanPoint(258, 308),
                new PlanPoint(242, 308)
        ), routed.outline());
    }
}
