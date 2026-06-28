package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;

import java.util.Objects;

public record SurfaceLayoutMargins(
        Length left,
        Length right,
        Length top,
        Length bottom
) {

    public SurfaceLayoutMargins {
        Objects.requireNonNull(left, "left darf nicht null sein.");
        Objects.requireNonNull(right, "right darf nicht null sein.");
        Objects.requireNonNull(top, "top darf nicht null sein.");
        Objects.requireNonNull(bottom, "bottom darf nicht null sein.");
    }

    public static SurfaceLayoutMargins zero() {
        return new SurfaceLayoutMargins(Length.zero(), Length.zero(), Length.zero(), Length.zero());
    }
}
