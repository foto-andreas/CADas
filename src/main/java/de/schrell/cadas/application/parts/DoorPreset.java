package de.schrell.cadas.application.parts;

import de.schrell.cadas.domain.geometry.Length;

/** Benannte Vorgabe für Türbreite, Türhöhe und Öffnungsdarstellung. */
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
