package de.schrell.cadas.application.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;
import de.schrell.cadas.domain.model.Wall;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerrainCornerServiceTest {

    private final TerrainCornerService service = new TerrainCornerService();

    @Test
    void leitetÄußereEckenAlsKonvexeHülleAbUndErhältHöhen() {
        Level level = new Level("Keller");
        addWall(level, 0, 0, 4000, 0);
        addWall(level, 4000, 0, 4000, 3000);
        addWall(level, 4000, 3000, 0, 3000);
        addWall(level, 0, 3000, 0, 0);
        addWall(level, 2000, 0, 2000, 3000);
        Terrain existing = new Terrain(List.of(
                new TerrainVertex(new PlanPoint(0, 0), Length.ofMillimeters(350)),
                new TerrainVertex(new PlanPoint(4000, 0), Length.zero()),
                new TerrainVertex(new PlanPoint(4000, 3000), Length.zero()),
                new TerrainVertex(new PlanPoint(0, 3000), Length.zero())
        ));

        Terrain synchronizedTerrain = service.synchronize(level, existing);

        assertEquals(4, synchronizedTerrain.vertices().size());
        assertEquals(350.0, synchronizedTerrain.vertices().getFirst().elevationAboveLowestFloor().toMillimeters(), 0.001);
    }

    private void addWall(Level level, double x1, double y1, double x2, double y2) {
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(x1, y1), new PlanPoint(x2, y2)),
                Length.ofMillimeters(175),
                Length.ofMillimeters(2750)
        ));
    }
}
