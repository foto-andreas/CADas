package de.schrell.cadas.application.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void leitetÄußereEckenAusWandkörpernAbUndErhältHöhen() {
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

    @Test
    void erhältKonkaveGebäudeeckenBeiLForm() {
        Level level = new Level("Erdgeschoss");
        addWall(level, 0, 0, 3000, 0);
        addWall(level, 3000, 0, 3000, 1000);
        addWall(level, 3000, 1000, 1000, 1000);
        addWall(level, 1000, 1000, 1000, 3000);
        addWall(level, 1000, 3000, 0, 3000);
        addWall(level, 0, 3000, 0, 0);

        Terrain synchronizedTerrain = service.synchronize(level, Terrain.empty());

        assertEquals(6, synchronizedTerrain.vertices().size());
        assertEquals(new PlanPoint(1087.5, 1087.5), synchronizedTerrain.vertices().get(3).position());
    }

    @Test
    void berücksichtigtZusätzlicheKonkaveAußenkantenDesOberirdischenGrundrisses() {
        Level keller = new Level("Keller");
        addThinWall(keller, -7000, -10700, 1100, -10700);
        addThinWall(keller, 1100, -10700, 1100, -7000);
        addThinWall(keller, 1100, -7000, 4800, -7000);
        addThinWall(keller, 4800, -7000, 4800, 0);
        addThinWall(keller, 4800, 0, 0, 0);
        addThinWall(keller, 0, 0, 0, -3400);
        addThinWall(keller, 0, -3400, -7000, -3400);
        addThinWall(keller, -7000, -3400, -7000, -10700);

        Level erdgeschoss = new Level("Erdgeschoss");
        addThinWall(erdgeschoss, -7000, -10700, 1320, -10700);
        addThinWall(erdgeschoss, 1320, -10700, 1320, -7140);
        addThinWall(erdgeschoss, 1320, -7140, 4800, -7140);
        addThinWall(erdgeschoss, 4800, -7140, 4800, 0);
        addThinWall(erdgeschoss, 4800, 0, 0, 0);
        addThinWall(erdgeschoss, 0, 0, 0, -3630);
        addThinWall(erdgeschoss, 0, -3630, -7000, -3630);
        addThinWall(erdgeschoss, -7000, -3630, -7000, -10700);

        List<Level> terrainLevels = List.of(keller, erdgeschoss).stream()
                .filter(level -> !level.name().equals("Keller"))
                .toList();
        Terrain synchronizedTerrain = service.synchronize(terrainLevels, Terrain.empty());

        assertEquals(8, synchronizedTerrain.vertices().size());
        assertTrue(synchronizedTerrain.vertices().stream().anyMatch(vertex ->
                Math.abs(vertex.position().xMillimeters() - 1320.5) < 0.01
                        && Math.abs(vertex.position().yMillimeters() + 7140.5) < 0.01));
        assertTrue(synchronizedTerrain.vertices().stream().anyMatch(vertex ->
                Math.abs(vertex.position().xMillimeters() + 0.5) < 0.01
                        && Math.abs(vertex.position().yMillimeters() + 3629.5) < 0.01));
    }

    private void addWall(Level level, double x1, double y1, double x2, double y2) {
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(x1, y1), new PlanPoint(x2, y2)),
                Length.ofMillimeters(175),
                Length.ofMillimeters(2750)
        ));
    }

    private void addThinWall(Level level, double x1, double y1, double x2, double y2) {
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(x1, y1), new PlanPoint(x2, y2)),
                Length.ofMillimeters(1),
                Length.ofMillimeters(2750)
        ));
    }
}
