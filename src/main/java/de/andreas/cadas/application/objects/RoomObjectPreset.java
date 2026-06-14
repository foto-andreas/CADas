package de.andreas.cadas.application.objects;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.model.RoomObjectMountingMode;
import de.andreas.cadas.domain.model.RoomObjectShape;
import de.andreas.cadas.domain.model.RoomObjectType;

public record RoomObjectPreset(
        String id,
        String name,
        RoomObjectType type,
        RoomObjectShape shape,
        Length width,
        Length depth,
        Length height,
        RoomObjectMountingMode mountingMode,
        String source
) {

    public RoomObjectPreset(
            String id,
            String name,
            RoomObjectType type,
            RoomObjectShape shape,
            Length width,
            Length depth,
            Length height,
            boolean cutsFloorCovering,
            String source
    ) {
        this(id, name, type, shape, width, depth, height, RoomObjectMountingMode.fromCutsFloorCovering(cutsFloorCovering), source);
    }

    public boolean cutsFloorCovering() {
        return mountingMode.cutsFloorCovering();
    }

    @Override
    public String toString() {
        return name;
    }
}
