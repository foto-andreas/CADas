package de.andreas.cadas.domain.model;

import de.andreas.cadas.domain.geometry.Length;

import java.util.Objects;
import java.util.UUID;

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
    }

    public static Door create(UUID wallId, Length offsetFromStart, Length width, Length height, Length thresholdHeight) {
        return new Door(UUID.randomUUID(), wallId, offsetFromStart, width, height, thresholdHeight);
    }
}

