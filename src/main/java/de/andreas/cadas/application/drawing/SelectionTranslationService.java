package de.andreas.cadas.application.drawing;

import de.andreas.cadas.application.view.RenderableKind;
import de.andreas.cadas.application.view.SelectionKey;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.Wall;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class SelectionTranslationService {

    public TranslationResult translate(Level level, Set<SelectionKey> selections, double deltaXMillimeters, double deltaYMillimeters) {
        Set<String> selectedWalls = selectedIds(selections, RenderableKind.WALL);
        Set<String> selectedStairs = selectedIds(selections, RenderableKind.STAIR);
        List<Wall> translatedWalls = level.walls().stream()
                .map(wall -> selectedWalls.contains(wall.id().toString()) ? translateWall(wall, deltaXMillimeters, deltaYMillimeters) : wall)
                .toList();
        List<Staircase> translatedStairs = level.staircases().stream()
                .map(staircase -> selectedStairs.contains(staircase.id().toString()) ? translateStair(staircase, deltaXMillimeters, deltaYMillimeters) : staircase)
                .toList();
        boolean changed = !selectedWalls.isEmpty() || !selectedStairs.isEmpty();
        return new TranslationResult(translatedWalls, translatedStairs, changed);
    }

    private Set<String> selectedIds(Set<SelectionKey> selections, RenderableKind kind) {
        return selections.stream()
                .filter(selection -> selection.kind() == kind)
                .map(SelectionKey::elementId)
                .collect(Collectors.toSet());
    }

    private Wall translateWall(Wall wall, double deltaXMillimeters, double deltaYMillimeters) {
        return new Wall(
                wall.id(),
                new PlanSegment(
                        translatePoint(wall.axis().start(), deltaXMillimeters, deltaYMillimeters),
                        translatePoint(wall.axis().end(), deltaXMillimeters, deltaYMillimeters)
                ),
                wall.thickness(),
                wall.height(),
                wall.startHeight(),
                wall.endHeight()
        );
    }

    private Staircase translateStair(Staircase staircase, double deltaXMillimeters, double deltaYMillimeters) {
        return new Staircase(
                staircase.id(),
                staircase.stairType(),
                translatePoint(staircase.firstCorner(), deltaXMillimeters, deltaYMillimeters),
                translatePoint(staircase.oppositeCorner(), deltaXMillimeters, deltaYMillimeters),
                staircase.totalHeight(),
                staircase.stepCount(),
                staircase.rotationQuarterTurns()
        );
    }

    private PlanPoint translatePoint(PlanPoint point, double deltaXMillimeters, double deltaYMillimeters) {
        return new PlanPoint(point.xMillimeters() + deltaXMillimeters, point.yMillimeters() + deltaYMillimeters);
    }

    public record TranslationResult(List<Wall> walls, List<Staircase> staircases, boolean changed) {
    }
}
