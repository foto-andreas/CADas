package de.andreas.cadas.domain.model;

import de.andreas.cadas.domain.geometry.Length;

import java.util.Objects;
import java.util.UUID;

public record SurfaceLayer(
        UUID id,
        String name,
        Length thickness,
        boolean visible,
        Length tileWidth,
        Length tileHeight,
        Length minimumOffset
) {

    public SurfaceLayer {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(name, "name darf nicht null sein.");
        Objects.requireNonNull(thickness, "thickness darf nicht null sein.");
        Objects.requireNonNull(tileWidth, "tileWidth darf nicht null sein.");
        Objects.requireNonNull(tileHeight, "tileHeight darf nicht null sein.");
        Objects.requireNonNull(minimumOffset, "minimumOffset darf nicht null sein.");
    }

    public static SurfaceLayer create(
            String name,
            Length thickness,
            Length tileWidth,
            Length tileHeight,
            Length minimumOffset
    ) {
        return new SurfaceLayer(UUID.randomUUID(), name, thickness, true, tileWidth, tileHeight, minimumOffset);
    }

    public SurfaceLayer rename(String newName) {
        return new SurfaceLayer(id, newName, thickness, visible, tileWidth, tileHeight, minimumOffset);
    }

    public SurfaceLayer withVisibility(boolean newVisibility) {
        return new SurfaceLayer(id, name, thickness, newVisibility, tileWidth, tileHeight, minimumOffset);
    }
}

