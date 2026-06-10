package de.andreas.cadas.application.parts;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.model.StairType;

public record StairPreset(
        String id,
        String name,
        StairType stairType,
        Length totalHeight,
        int stepCount
) {
    @Override
    public String toString() {
        return name;
    }
}

