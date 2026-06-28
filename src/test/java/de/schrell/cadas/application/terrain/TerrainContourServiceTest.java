package de.schrell.cadas.application.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerrainContourServiceTest {

    private final TerrainContourService service = new TerrainContourService();

    @Test
    void nutztGeländeeckenAlsFallbackWennNochKeineGebäudeaußenkonturExistiert() {
        ProjectModel project = ProjectModel.withDefaultLevel("Hanghaus", "Keller");
        project.defineTerrain(new Terrain(List.of(
                new TerrainVertex(new PlanPoint(0, 0), Length.ofMillimeters(-200)),
                new TerrainVertex(new PlanPoint(4000, 0), Length.zero()),
                new TerrainVertex(new PlanPoint(4000, 3000), Length.ofMillimeters(300)),
                new TerrainVertex(new PlanPoint(0, 3000), Length.ofMillimeters(100))
        )));

        assertEquals(List.of(
                new PlanPoint(0, 0),
                new PlanPoint(4000, 0),
                new PlanPoint(4000, 3000),
                new PlanPoint(0, 3000)
        ), service.contour(project));
    }
}
