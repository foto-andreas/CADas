package de.andreas.cadas.application.view;

import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Wall;

import java.util.Objects;

public final class ThreeDInteriorViewService {

    private static final double DEFAULT_EYE_HEIGHT_MILLIMETERS = 1_600.0;
    private static final double MIN_EYE_HEIGHT_MILLIMETERS = 300.0;
    private static final double CEILING_CLEARANCE_MILLIMETERS = 200.0;

    public InteriorViewTarget targetFor(ProjectModel project, Level level, Room room) {
        Objects.requireNonNull(project, "project darf nicht null sein.");
        Objects.requireNonNull(level, "level darf nicht null sein.");
        Objects.requireNonNull(room, "room darf nicht null sein.");
        double eyeHeight = eyeHeightFor(room);
        return new InteriorViewTarget(
                level.name(),
                room.name(),
                room.centerPoint().xMillimeters(),
                levelBaseHeightMillimeters(project, level.name()) + room.floorThickness().toMillimeters() + eyeHeight,
                room.centerPoint().yMillimeters(),
                eyeHeight
        );
    }

    private double eyeHeightFor(Room room) {
        double maximumHeight = Math.max(MIN_EYE_HEIGHT_MILLIMETERS, room.maximumCeilingHeightMillimeters() - CEILING_CLEARANCE_MILLIMETERS);
        return Math.min(DEFAULT_EYE_HEIGHT_MILLIMETERS, maximumHeight);
    }

    private double levelBaseHeightMillimeters(ProjectModel project, String levelName) {
        double baseHeight = 0.0;
        for (Level level : project.levels()) {
            if (level.name().equals(levelName)) {
                return baseHeight;
            }
            baseHeight += estimateLevelHeight(level);
        }
        return baseHeight;
    }

    private double estimateLevelHeight(Level level) {
        double wallHeight = level.walls().stream().mapToDouble(Wall::maximumHeightMillimeters).max().orElse(2_750.0);
        double roomHeight = level.rooms().stream()
                .mapToDouble(room -> room.maximumCeilingHeightMillimeters()
                        + room.floorThickness().toMillimeters()
                        + room.ceilingThickness().toMillimeters())
                .max()
                .orElse(0.0);
        double stairHeight = level.staircases().stream()
                .mapToDouble(staircase -> staircase.totalHeight().toMillimeters())
                .max()
                .orElse(0.0);
        return Math.max(wallHeight, Math.max(roomHeight, stairHeight));
    }

    public record InteriorViewTarget(
            String levelName,
            String roomName,
            double eyeXMillimeters,
            double eyeYMillimeters,
            double eyeZMillimeters,
            double eyeHeightAboveFloorMillimeters
    ) {
    }
}
