package de.schrell.cadas.application.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerrainGeometryServiceTest {

    private final TerrainGeometryService service = new TerrainGeometryService();

    @Test
    void verschiebtRechteckigeKonturKonstantNachAußen() {
        Terrain terrain = new Terrain(List.of(
                new TerrainVertex(new PlanPoint(0, 0), Length.zero()),
                new TerrainVertex(new PlanPoint(4000, 0), Length.zero()),
                new TerrainVertex(new PlanPoint(4000, 3000), Length.zero()),
                new TerrainVertex(new PlanPoint(0, 3000), Length.zero())
        ), Length.ofMillimeters(500));

        List<PlanPoint> outline = service.outerOutline(terrain);

        assertEquals(List.of(
                new PlanPoint(-500, -500),
                new PlanPoint(4500, -500),
                new PlanPoint(4500, 3500),
                new PlanPoint(-500, 3500)
        ), outline);
    }

    @Test
    void verschiebtKonkaveKonturOhneDieEckeZuVerlieren() {
        Terrain terrain = new Terrain(List.of(
                new TerrainVertex(new PlanPoint(0, 0), Length.zero()),
                new TerrainVertex(new PlanPoint(3000, 0), Length.zero()),
                new TerrainVertex(new PlanPoint(3000, 1000), Length.zero()),
                new TerrainVertex(new PlanPoint(1000, 1000), Length.zero()),
                new TerrainVertex(new PlanPoint(1000, 3000), Length.zero()),
                new TerrainVertex(new PlanPoint(0, 3000), Length.zero())
        ), Length.ofMillimeters(500));

        List<PlanPoint> outline = service.outerOutline(terrain);

        assertEquals(List.of(
                new PlanPoint(-500, -500),
                new PlanPoint(3500, -500),
                new PlanPoint(3500, 1500),
                new PlanPoint(1500, 1500),
                new PlanPoint(1500, 3500),
                new PlanPoint(-500, 3500)
        ), outline);
    }
}
