package de.andreas.cadas.application.layers;

import de.andreas.cadas.domain.geometry.Length;

public record TileLayoutRequest(
        Length surfaceWidth,
        Length surfaceHeight,
        Length tileWidth,
        Length tileHeight,
        Length minimumOffset
) {
}

