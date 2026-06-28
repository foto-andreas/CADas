package de.schrell.cadas.application.terrain;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Verwaltet das Setzen, Auswählen und Entfernen einzelner Geländestützpunkte
 * entlang der Gebäudeaußenkontur.
 */
public final class TerrainEditService {

    public static final double EXISTING_POINT_SELECTION_TOLERANCE_MILLIMETERS = 100.0;

    private final TerrainProfileService terrainProfileService = new TerrainProfileService();

    public Optional<TerrainProfileService.ProjectedTerrainPoint> resolveEditTarget(
            Terrain terrain,
            List<PlanPoint> contour,
            PlanPoint point
    ) {
        Optional<TerrainProfileService.ProjectedTerrainPoint> existingSample = existingSampleNear(
                terrain,
                contour,
                point,
                EXISTING_POINT_SELECTION_TOLERANCE_MILLIMETERS
        );
        if (existingSample.isPresent()) {
            return existingSample;
        }
        return terrainProfileService.projectToBand(point, contour);
    }

    public Optional<TerrainProfileService.ProjectedTerrainPoint> existingSampleNear(
            Terrain terrain,
            List<PlanPoint> contour,
            PlanPoint point,
            double toleranceMillimeters
    ) {
        return terrainProfileService.projectedSamples(terrain, contour).stream()
                .filter(sample -> sample.bandPoint().distanceTo(point).toMillimeters() <= toleranceMillimeters)
                .min(Comparator.comparingDouble(sample -> sample.bandPoint().distanceTo(point).toMillimeters()));
    }

    public Length currentElevation(
            Terrain terrain,
            List<PlanPoint> contour,
            TerrainProfileService.ProjectedTerrainPoint target
    ) {
        return Length.ofMillimeters(
                terrainProfileService.interpolatedElevationMillimeters(terrain, contour, target.contourDistance())
        );
    }

    public Terrain upsertPoint(
            Terrain terrain,
            List<PlanPoint> contour,
            TerrainProfileService.ProjectedTerrainPoint target,
            Length elevation
    ) {
        List<TerrainVertex> updatedVertices = new ArrayList<>(terrain.vertices());
        updatedVertices.add(new TerrainVertex(target.contourPoint(), elevation));
        return terrainWithSortedVertices(terrain.displayWidth(), contour, updatedVertices);
    }

    public Terrain replacePoint(
            Terrain terrain,
            List<PlanPoint> contour,
            TerrainProfileService.ProjectedTerrainPoint target,
            TerrainProfileService.ProjectedTerrainPoint existingSample,
            Length elevation
    ) {
        List<TerrainVertex> updatedVertices = new ArrayList<>(terrain.vertices());
        updatedVertices.removeIf(vertex -> vertex.position().equals(existingSample.contourPoint()));
        updatedVertices.add(new TerrainVertex(target.contourPoint(), elevation));
        return terrainWithSortedVertices(terrain.displayWidth(), contour, updatedVertices);
    }

    public Terrain deletePoint(
            Terrain terrain,
            List<PlanPoint> contour,
            TerrainProfileService.ProjectedTerrainPoint existingSample
    ) {
        List<TerrainVertex> updatedVertices = terrain.vertices().stream()
                .filter(vertex -> !vertex.position().equals(existingSample.contourPoint()))
                .toList();
        return terrainWithSortedVertices(terrain.displayWidth(), contour, updatedVertices);
    }

    private Terrain terrainWithSortedVertices(
            Length displayWidth,
            List<PlanPoint> contour,
            List<TerrainVertex> vertices
    ) {
        List<TerrainVertex> sortedVertices = vertices.stream()
                .sorted(Comparator.comparingDouble(vertex -> terrainProfileService.projectToContour(vertex.position(), contour)
                        .map(TerrainProfileService.ProjectedTerrainPoint::contourDistance)
                        .orElse(Double.MAX_VALUE)))
                .toList();
        return new Terrain(sortedVertices, displayWidth);
    }
}
