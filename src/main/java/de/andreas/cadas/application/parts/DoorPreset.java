package de.andreas.cadas.application.parts;

import de.andreas.cadas.domain.geometry.Length;

public record DoorPreset(
        String id,
        String name,
        Length width,
        Length height,
        Length thresholdHeight
) {
    @Override
    public String toString() {
        return name;
    }
}

