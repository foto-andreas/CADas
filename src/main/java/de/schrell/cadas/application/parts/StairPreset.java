package de.schrell.cadas.application.parts;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.StairType;

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
