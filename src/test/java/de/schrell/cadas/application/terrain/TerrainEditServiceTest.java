package de.schrell.cadas.application.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;

import java.util.List;

import org.junit.jupiter.api.Test;

class TerrainEditServiceTest {

    private static final List<PlanPoint> CONTOUR = List.of(
            new PlanPoint(0, 0),
            new PlanPoint(4000, 0),
            new PlanPoint(4000, 3000),
            new PlanPoint(0, 3000)
    );

    private final TerrainEditService service = new TerrainEditService();

    @Test
    void findetVorhandenenGeländepunktNurInnerhalbDesZehnZentimeterUmkreises() {
        Terrain terrain = terrainMitStützpunkten();

        assertTrue(service.existingSampleNear(
                terrain,
                CONTOUR,
                new PlanPoint(2050, -190),
                TerrainEditService.EXISTING_POINT_SELECTION_TOLERANCE_MILLIMETERS
        ).isPresent());
        assertTrue(service.existingSampleNear(
                terrain,
                CONTOUR,
                new PlanPoint(2101, -200),
                TerrainEditService.EXISTING_POINT_SELECTION_TOLERANCE_MILLIMETERS
        ).isEmpty());
    }

    @Test
    void ersetztVorhandenenGeländepunktOhneDuplikat() {
        Terrain terrain = terrainMitStützpunkten();
        TerrainProfileService.ProjectedTerrainPoint vorhandenerPunkt = service.resolveEditTarget(
                terrain,
                CONTOUR,
                new PlanPoint(2050, -190)
        ).orElseThrow();

        Terrain aktualisiert = service.replacePoint(
                terrain,
                CONTOUR,
                vorhandenerPunkt,
                vorhandenerPunkt,
                Length.ofMillimeters(450)
        );

        assertEquals(3, aktualisiert.vertices().size());
        assertEquals(1, aktualisiert.vertices().stream()
                .filter(vertex -> vertex.position().equals(new PlanPoint(2000, 0)))
                .count());
        assertEquals(450.0, aktualisiert.vertices().stream()
                .filter(vertex -> vertex.position().equals(new PlanPoint(2000, 0)))
                .findFirst()
                .orElseThrow()
                .elevationAboveLowestFloor()
                .toMillimeters(), 0.001);
    }

    @Test
    void entferntGewähltenGeländepunktUndErhältDieAnzeigeBreite() {
        Terrain terrain = terrainMitStützpunkten();
        TerrainProfileService.ProjectedTerrainPoint vorhandenerPunkt = service.resolveEditTarget(
                terrain,
                CONTOUR,
                new PlanPoint(2050, -190)
        ).orElseThrow();

        Terrain aktualisiert = service.deletePoint(terrain, CONTOUR, vorhandenerPunkt);

        assertEquals(2, aktualisiert.vertices().size());
        assertEquals(1800.0, aktualisiert.displayWidth().toMillimeters(), 0.001);
        assertFalse(aktualisiert.vertices().stream().anyMatch(vertex -> vertex.position().equals(new PlanPoint(2000, 0))));
    }

    @Test
    void entferntAuchLeichtNebenDerKonturGespeichertenGeländepunkt() {
        Terrain terrain = new Terrain(List.of(
                new TerrainVertex(new PlanPoint(1000, 0), Length.ofMillimeters(100)),
                new TerrainVertex(new PlanPoint(2000, -20), Length.ofMillimeters(200)),
                new TerrainVertex(new PlanPoint(3000, 3000), Length.ofMillimeters(300))
        ), Length.ofMillimeters(1800));
        TerrainProfileService.ProjectedTerrainPoint vorhandenerPunkt = service.resolveEditTarget(
                terrain,
                CONTOUR,
                new PlanPoint(2050, -190)
        ).orElseThrow();

        Terrain aktualisiert = service.deletePoint(terrain, CONTOUR, vorhandenerPunkt);

        assertEquals(2, aktualisiert.vertices().size());
        assertFalse(aktualisiert.vertices().stream().anyMatch(vertex -> vertex.position().equals(new PlanPoint(2000, -20))));
    }

    private Terrain terrainMitStützpunkten() {
        return new Terrain(List.of(
                new TerrainVertex(new PlanPoint(1000, 0), Length.ofMillimeters(100)),
                new TerrainVertex(new PlanPoint(2000, 0), Length.ofMillimeters(200)),
                new TerrainVertex(new PlanPoint(3000, 3000), Length.ofMillimeters(300))
        ), Length.ofMillimeters(1800));
    }
}
