package de.schrell.cadas.application.stairs;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;
import de.schrell.cadas.domain.model.WindowElement;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class StairUnderbuildService {

    public UnderbuildResult synchronize(Level level, Staircase staircase) {
        validate(staircase);
        UUID leftWallId = wallId(staircase.id(), Side.LEFT);
        UUID rightWallId = wallId(staircase.id(), Side.RIGHT);
        List<Wall> walls = level.walls().stream()
                .filter(wall -> !wall.id().equals(leftWallId) && !wall.id().equals(rightWallId))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (staircase.leftUnderbuildWidth().toMillimeters() > 0.0) {
            walls.add(createWall(staircase, Side.LEFT, staircase.leftUnderbuildWidth()));
        }
        if (staircase.rightUnderbuildWidth().toMillimeters() > 0.0) {
            walls.add(createWall(staircase, Side.RIGHT, staircase.rightUnderbuildWidth()));
        }
        List<UUID> activeWallIds = walls.stream().map(Wall::id).toList();
        List<Door> doors = level.doors().stream().filter(door -> activeWallIds.contains(door.wallId())).toList();
        List<WindowElement> windows = level.windows().stream().filter(window -> activeWallIds.contains(window.wallId())).toList();
        return new UnderbuildResult(List.copyOf(walls), doors, windows);
    }

    public UUID wallId(UUID staircaseId, Side side) {
        return UUID.nameUUIDFromBytes(("CADas:Treppenunterbau:" + staircaseId + ":" + side.name())
                .getBytes(StandardCharsets.UTF_8));
    }

    public UnderbuildResult remove(Level level, UUID staircaseId) {
        List<UUID> generatedWallIds = List.of(wallId(staircaseId, Side.LEFT), wallId(staircaseId, Side.RIGHT));
        List<Wall> walls = level.walls().stream().filter(wall -> !generatedWallIds.contains(wall.id())).toList();
        List<Door> doors = level.doors().stream().filter(door -> !generatedWallIds.contains(door.wallId())).toList();
        List<WindowElement> windows = level.windows().stream().filter(window -> !generatedWallIds.contains(window.wallId())).toList();
        return new UnderbuildResult(walls, doors, windows);
    }

    private Wall createWall(Staircase staircase, Side side, Length width) {
        double localX = side == Side.LEFT ? 0.0 : staircase.widthMillimeters();
        PlanPoint start = staircase.pointAtLocalPosition(localX, 0.0);
        PlanPoint end = staircase.pointAtLocalPosition(localX, staircase.heightMillimeters());
        double endHeight = staircase.totalHeight().toMillimeters() - staircase.undersideThickness().toMillimeters();
        PlanSegment axis = new PlanSegment(start, end);
        return new Wall(
                wallId(staircase.id(), side),
                axis,
                width,
                Length.ofMillimeters(endHeight),
                Length.zero(),
                Length.ofMillimeters(endHeight),
                List.of(
                        new WallProfilePoint(Length.zero(), Length.zero()),
                        new WallProfilePoint(axis.length(), Length.ofMillimeters(endHeight))
                )
        );
    }

    private void validate(Staircase staircase) {
        boolean configured = staircase.leftUnderbuildWidth().toMillimeters() > 0.0
                || staircase.rightUnderbuildWidth().toMillimeters() > 0.0
                || staircase.undersideThickness().toMillimeters() > 0.0;
        if (configured && staircase.stairType() == StairType.SPIRAL) {
            throw new IllegalArgumentException("Wendeltreppen unterstützen keinen geraden Wandunterbau.");
        }
        if (staircase.leftUnderbuildWidth().add(staircase.rightUnderbuildWidth()).toMillimeters()
                >= staircase.widthMillimeters()) {
            throw new IllegalArgumentException("Die beiden Unterbauwände müssen zwischen sich eine freie Treppenbreite lassen.");
        }
        if (staircase.undersideThickness().compareTo(staircase.totalHeight()) >= 0) {
            throw new IllegalArgumentException("Die Dicke der Treppenuntersicht muss kleiner als die Treppenhöhe sein.");
        }
    }

    public enum Side {
        LEFT,
        RIGHT
    }

    public record UnderbuildResult(List<Wall> walls, List<Door> doors, List<WindowElement> windows) {
    }
}
