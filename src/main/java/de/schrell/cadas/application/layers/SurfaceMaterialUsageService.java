package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceMaterial;
import de.schrell.cadas.domain.model.SurfaceType;

import java.util.Objects;
import java.util.UUID;

/** Führt referenzbasierte Materialänderungen über alle passenden Oberflächennutzungen aus. */
public final class SurfaceMaterialUsageService {

    public enum InsertionPosition {
        BEFORE,
        AFTER
    }

    public int replace(
            ProjectModel project,
            UUID sourceMaterialId,
            SurfaceMaterial targetMaterial,
            SurfaceMaterialUsageScope scope,
            Level selectedLevel,
            UUID selectedRoomId
    ) {
        SurfaceMaterial target = registerMatchingMaterial(project, targetMaterial);
        int replacements = 0;
        for (Level level : project.levels()) {
            for (SurfaceLayerStack stack : level.surfaceLayerStacks()) {
                if (!matchesScope(level, stack, scope, selectedLevel, selectedRoomId)) {
                    continue;
                }
                for (SurfaceLayer layer : stack.layers()) {
                    if (sourceMaterialId.equals(layer.materialId())) {
                        stack.replaceLayer(layer.id(), target.applyTo(layer));
                        replacements++;
                    }
                }
            }
        }
        return replacements;
    }

    public int insert(
            ProjectModel project,
            UUID sourceMaterialId,
            SurfaceMaterial targetMaterial,
            InsertionPosition position,
            SurfaceMaterialUsageScope scope,
            Level selectedLevel,
            UUID selectedRoomId
    ) {
        SurfaceMaterial target = registerMatchingMaterial(project, targetMaterial);
        int additions = 0;
        for (Level level : project.levels()) {
            for (SurfaceLayerStack stack : level.surfaceLayerStacks()) {
                if (!matchesScope(level, stack, scope, selectedLevel, selectedRoomId)) {
                    continue;
                }
                for (int index = stack.layers().size() - 1; index >= 0; index--) {
                    if (sourceMaterialId.equals(stack.layers().get(index).materialId())) {
                        stack.insertLayer(position == InsertionPosition.BEFORE ? index : index + 1, target.createUsage());
                        additions++;
                    }
                }
            }
        }
        return additions;
    }

    public int remove(
            ProjectModel project,
            UUID materialId,
            SurfaceMaterialUsageScope scope,
            Level selectedLevel,
            UUID selectedRoomId
    ) {
        int removals = 0;
        for (Level level : project.levels()) {
            for (SurfaceLayerStack stack : level.surfaceLayerStacks()) {
                if (!matchesScope(level, stack, scope, selectedLevel, selectedRoomId)) {
                    continue;
                }
                for (SurfaceLayer layer : stack.layers()) {
                    if (materialId.equals(layer.materialId())) {
                        stack.removeLayer(layer.id());
                        removals++;
                    }
                }
            }
            level.surfaceLayerStacks().stream()
                    .filter(stack -> stack.layers().isEmpty())
                    .map(SurfaceLayerStack::id)
                    .toList()
                    .forEach(level::removeSurfaceLayerStack);
        }
        return removals;
    }

    public SurfaceMaterial registerMatchingMaterial(ProjectModel project, SurfaceMaterial candidate) {
        Objects.requireNonNull(project, "project darf nicht null sein.");
        Objects.requireNonNull(candidate, "candidate darf nicht null sein.");
        SurfaceMaterial existing = project.surfaceMaterials().stream()
                .filter(material -> sameMaterialOrigin(material, candidate))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            return project.registerSurfaceMaterial(candidate);
        }
        SurfaceMaterial updated = candidate.withId(existing.id());
        project.replaceSurfaceMaterial(updated);
        return updated;
    }

    private boolean matchesScope(
            Level level,
            SurfaceLayerStack stack,
            SurfaceMaterialUsageScope scope,
            Level selectedLevel,
            UUID selectedRoomId
    ) {
        if (scope == SurfaceMaterialUsageScope.ENTIRE_PROJECT) {
            return true;
        }
        if (selectedLevel == null || selectedRoomId == null || level != selectedLevel) {
            return false;
        }
        if (stack.surfaceType() == SurfaceType.FLOOR || stack.surfaceType() == SurfaceType.CEILING) {
            return selectedRoomId.toString().equals(stack.targetKey());
        }
        return stack.surfaceType() == SurfaceType.WALL_INTERIOR
                && WallSurfaceTargetKey.roomId(stack.targetKey()).filter(selectedRoomId::equals).isPresent();
    }

    private boolean sameMaterialOrigin(SurfaceMaterial first, SurfaceMaterial second) {
        if (!first.coveringSource().isBlank() || !second.coveringSource().isBlank()) {
            return first.coveringSource().equals(second.coveringSource());
        }
        return first.valueSignature().equals(second.valueSignature());
    }
}
