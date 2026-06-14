package de.andreas.cadas.application.objects;

import de.andreas.cadas.domain.geometry.Length;
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
        boolean cutsFloorCovering,
        String source
) {

    @Override
    public String toString() {
        return name;
    }
}
