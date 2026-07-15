package de.schrell.cadas.application.parts;

import de.schrell.cadas.domain.geometry.Length;

/** Benannte Vorgabe für Fensterbreite, Fensterhöhe und Brüstungshöhe. */
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
