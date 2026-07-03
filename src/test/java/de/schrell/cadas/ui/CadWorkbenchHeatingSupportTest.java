package de.schrell.cadas.ui;

import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingZone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CadWorkbenchHeatingSupportTest {

    @Test
    void findetNaechstenPunktAufHeizkreisRand() {
        HeatingZone zone = HeatingZone.create("Heizkreis", List.of(
                new PlanPoint(0, 0),
                new PlanPoint(1_000, 0),
                new PlanPoint(1_000, 1_000),
                new PlanPoint(0, 1_000)
        ), HeatingLayoutPattern.MEANDER);

        PlanPoint nearest = CadWorkbenchHeatingSupport.nearestPointOnHeatingZoneBoundary(zone, new PlanPoint(1_200, 400));

        assertEquals(1_000.0, nearest.xMillimeters(), 0.001);
        assertEquals(400.0, nearest.yMillimeters(), 0.001);
    }
}
