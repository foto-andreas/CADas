package de.schrell.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerrainTest {

    @Test
    void akzeptiertBeliebigePositiveUndNegativeEckhöhen() {
        Terrain terrain = new Terrain(List.of(
                new TerrainVertex(new PlanPoint(0, 0), Length.ofMillimeters(-300)),
                new TerrainVertex(new PlanPoint(4000, 0), Length.zero()),
                new TerrainVertex(new PlanPoint(4000, 3000), Length.ofMillimeters(500))
        ));

        assertTrue(terrain.configured());
    }

    @Test
    void weistUnvollständigeUndDoppelteEckenZurück() {
        assertThrows(IllegalArgumentException.class, () -> new Terrain(List.of(
                new TerrainVertex(new PlanPoint(0, 0), Length.zero())
        )));
        assertThrows(IllegalArgumentException.class, () -> new Terrain(List.of(
                new TerrainVertex(new PlanPoint(0, 0), Length.zero()),
                new TerrainVertex(new PlanPoint(1, 0), Length.zero()),
                new TerrainVertex(new PlanPoint(0, 0), Length.ofMillimeters(100))
        )));
        assertThrows(IllegalArgumentException.class, () -> new Terrain(List.of(
                new TerrainVertex(new PlanPoint(0, 0), Length.zero()),
                new TerrainVertex(new PlanPoint(1, 0), Length.zero()),
                new TerrainVertex(new PlanPoint(0, 1), Length.zero())
        ), Length.of(0, LengthUnit.CENTIMETER)));
    }
}
