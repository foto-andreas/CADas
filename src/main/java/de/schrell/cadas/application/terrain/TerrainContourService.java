package de.schrell.cadas.application.terrain;

import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Terrain;

import java.util.List;
import java.util.Locale;

/**
 * Liefert die bearbeitbare Gebäudeaußenkontur für das Gelände.
 */
public final class TerrainContourService {

    private final TerrainCornerService terrainCornerService = new TerrainCornerService();

    public List<PlanPoint> contour(ProjectModel project) {
        List<PlanPoint> levelContour = contour(project.levels());
        if (!levelContour.isEmpty()) {
            return levelContour;
        }
        return project.terrain().vertices().stream()
                .map(vertex -> vertex.position())
                .toList();
    }

    public List<PlanPoint> contour(List<Level> levels) {
        List<Level> sourceLevels = sourceLevels(levels);
        Terrain contour = terrainCornerService.synchronize(sourceLevels, Terrain.empty());
        return contour.vertices().stream()
                .map(vertex -> vertex.position())
                .toList();
    }

    public List<Level> sourceLevels(List<Level> levels) {
        List<Level> aboveGroundLevels = levels.stream()
                .filter(level -> !isBasementLevel(level))
                .toList();
        return aboveGroundLevels.isEmpty() ? levels : aboveGroundLevels;
    }

    private boolean isBasementLevel(Level level) {
        String normalizedName = level.name().trim().toLowerCase(Locale.GERMAN);
        return normalizedName.contains("keller")
                || normalizedName.startsWith("kg")
                || normalizedName.contains("souterrain");
    }
}
