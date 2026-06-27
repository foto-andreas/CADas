package de.schrell.cadas.domain.geometry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanPolygonSupportTest {

    @Test
    void erkenntInnerenPunktImRechteck() {
        assertTrue(PlanPolygonSupport.containsPoint(rechteck(), new PlanPoint(500, 400)));
    }

    @Test
    void behandeltRandUndEckeAlsEnthalten() {
        assertTrue(PlanPolygonSupport.containsPoint(rechteck(), new PlanPoint(0, 400)));
        assertTrue(PlanPolygonSupport.containsPoint(rechteck(), new PlanPoint(1_000, 800)));
    }

    @Test
    void lehntÄußerenPunktUndLeeresPolygonAb() {
        assertFalse(PlanPolygonSupport.containsPoint(rechteck(), new PlanPoint(1_100, 400)));
        assertFalse(PlanPolygonSupport.containsPoint(List.of(), new PlanPoint(0, 0)));
    }

    private List<PlanPoint> rechteck() {
        return List.of(
                new PlanPoint(0, 0),
                new PlanPoint(1_000, 0),
                new PlanPoint(1_000, 800),
                new PlanPoint(0, 800)
        );
    }
}
