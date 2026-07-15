package de.schrell.cadas.application.parts;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.StairType;

/** Benannte Vorgabe für Treppentyp, Laufmaße, Steigungen und Höhenlage. */
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
