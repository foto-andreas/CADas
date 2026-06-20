package de.schrell.cadas.application.parts;

import de.schrell.cadas.domain.geometry.Length;

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
