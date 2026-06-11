package de.andreas.cadas.application.layers;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.model.SurfaceLayoutMode;

public record TileLayoutRequest(
        Length surfaceWidth,
        Length surfaceHeight,
        Length tileWidth,
        Length tileHeight,
        SurfaceLayoutMode layoutMode,
        Length layoutOffset,
        Length minimumOffset,
        Length minimumEdgeWidth
) {

    public TileLayoutRequest(
            Length surfaceWidth,
            Length surfaceHeight,
            Length tileWidth,
            Length tileHeight,
            Length minimumOffset
    ) {
        this(surfaceWidth, surfaceHeight, tileWidth, tileHeight, SurfaceLayoutMode.AUTOMATIC, Length.zero(), minimumOffset, Length.zero());
    }
}
