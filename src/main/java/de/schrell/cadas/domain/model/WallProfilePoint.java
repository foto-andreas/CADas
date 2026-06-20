package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;

import java.util.Objects;

public record WallProfilePoint(Length offset, Length height) {

    public WallProfilePoint {
        Objects.requireNonNull(offset, "offset darf nicht null sein.");
        Objects.requireNonNull(height, "height darf nicht null sein.");
        if (offset.toMillimeters() < 0.0) {
            throw new IllegalArgumentException("Der Profilabstand darf nicht negativ sein.");
        }
        if (height.toMillimeters() < 0.0) {
            throw new IllegalArgumentException("Die Profilhöhe darf nicht negativ sein.");
        }
    }
}
