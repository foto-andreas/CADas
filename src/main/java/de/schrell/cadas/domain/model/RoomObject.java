package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.Objects;
import java.util.UUID;

public record RoomObject(
        UUID id,
        String presetId,
        String name,
        RoomObjectType type,
        RoomObjectShape shape,
        PlanPoint center,
        Length width,
        Length depth,
        Length height,
        int rotationQuarterTurns,
        RoomObjectMountingMode mountingMode,
        boolean visible,
        String source
) {

    public RoomObject {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(presetId, "presetId darf nicht null sein.");
        Objects.requireNonNull(name, "name darf nicht null sein.");
        Objects.requireNonNull(type, "type darf nicht null sein.");
        Objects.requireNonNull(shape, "shape darf nicht null sein.");
        Objects.requireNonNull(center, "center darf nicht null sein.");
        Objects.requireNonNull(width, "width darf nicht null sein.");
        Objects.requireNonNull(depth, "depth darf nicht null sein.");
        Objects.requireNonNull(height, "height darf nicht null sein.");
        Objects.requireNonNull(mountingMode, "mountingMode darf nicht null sein.");
        Objects.requireNonNull(source, "source darf nicht null sein.");
        rotationQuarterTurns = Math.floorMod(rotationQuarterTurns, 4);
    }

    public RoomObject(
            UUID id,
            String presetId,
            String name,
            RoomObjectType type,
            RoomObjectShape shape,
            PlanPoint center,
            Length width,
            Length depth,
            Length height,
            int rotationQuarterTurns,
            boolean cutsFloorCovering,
            boolean visible,
            String source
    ) {
        this(
                id,
                presetId,
                name,
                type,
                shape,
                center,
                width,
                depth,
                height,
                rotationQuarterTurns,
                RoomObjectMountingMode.fromCutsFloorCovering(cutsFloorCovering),
                visible,
                source
        );
    }

    public static RoomObject create(
            String presetId,
            String name,
            RoomObjectType type,
            RoomObjectShape shape,
            PlanPoint center,
            Length width,
            Length depth,
            Length height,
            boolean cutsFloorCovering,
            String source
    ) {
        return create(
                presetId,
                name,
                type,
                shape,
                center,
                width,
                depth,
                height,
                RoomObjectMountingMode.fromCutsFloorCovering(cutsFloorCovering),
                source
        );
    }

    public static RoomObject create(
            String presetId,
            String name,
            RoomObjectType type,
            RoomObjectShape shape,
            PlanPoint center,
            Length width,
            Length depth,
            Length height,
            RoomObjectMountingMode mountingMode,
            String source
    ) {
        return new RoomObject(UUID.randomUUID(), presetId, name, type, shape, center, width, depth, height, 0, mountingMode, true, source);
    }

    public boolean cutsFloorCovering() {
        return mountingMode.cutsFloorCovering();
    }

    public double footprintWidthMillimeters() {
        return rotationQuarterTurns % 2 == 0 ? width.toMillimeters() : depth.toMillimeters();
    }

    public double footprintDepthMillimeters() {
        return rotationQuarterTurns % 2 == 0 ? depth.toMillimeters() : width.toMillimeters();
    }

    public double minXMillimeters() {
        return center.xMillimeters() - footprintWidthMillimeters() / 2.0;
    }

    public double maxXMillimeters() {
        return center.xMillimeters() + footprintWidthMillimeters() / 2.0;
    }

    public double minYMillimeters() {
        return center.yMillimeters() - footprintDepthMillimeters() / 2.0;
    }

    public double maxYMillimeters() {
        return center.yMillimeters() + footprintDepthMillimeters() / 2.0;
    }

    public RoomObject withVisibility(boolean newVisibility) {
        return new RoomObject(id, presetId, name, type, shape, center, width, depth, height, rotationQuarterTurns, mountingMode, newVisibility, source);
    }
}
