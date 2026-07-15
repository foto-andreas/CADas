package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;

import java.util.Objects;
import java.util.UUID;

/**
 * Beschreibt eine Türöffnung relativ zu ihrer Host-Wand. Versatz, Breite und Höhe sind Millimeterwerte;
 * die ID bleibt über Bearbeitung, Persistenz und Auswahl stabil.
 */
public record Door(
        UUID id,
        UUID wallId,
        Length offsetFromStart,
        Length width,
        Length height,
        Length thresholdHeight
) {

    public Door {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(wallId, "wallId darf nicht null sein.");
        Objects.requireNonNull(offsetFromStart, "offsetFromStart darf nicht null sein.");
        Objects.requireNonNull(width, "width darf nicht null sein.");
        Objects.requireNonNull(height, "height darf nicht null sein.");
        Objects.requireNonNull(thresholdHeight, "thresholdHeight darf nicht null sein.");
        if (offsetFromStart.toMillimeters() < 0.0) {
            throw new IllegalArgumentException("Der Türabstand vom Wandanfang darf nicht negativ sein.");
        }
        if (width.toMillimeters() <= 0.0 || height.toMillimeters() <= 0.0) {
            throw new IllegalArgumentException("Türbreite und Türhöhe müssen größer als 0 sein.");
        }
        if (thresholdHeight.toMillimeters() < 0.0) {
            throw new IllegalArgumentException("Die Schwellenhöhe darf nicht negativ sein.");
        }
    }

    public static Door create(UUID wallId, Length offsetFromStart, Length width, Length height, Length thresholdHeight) {
        return new Door(UUID.randomUUID(), wallId, offsetFromStart, width, height, thresholdHeight);
    }

    public Door withOffset(Length offset) {
        return new Door(id, wallId, offset, width, height, thresholdHeight);
    }
}
