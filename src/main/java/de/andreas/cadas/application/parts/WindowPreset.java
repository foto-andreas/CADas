package de.andreas.cadas.application.parts;

import de.andreas.cadas.domain.geometry.Length;

public record WindowPreset(
        String id,
        String name,
        Length width,
        Length height,
        Length sillHeight
) {
    @Override
    public String toString() {
        return name;
    }
}

