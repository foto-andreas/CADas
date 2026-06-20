package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;

public record TileLayoutRequest(
        Length surfaceWidth,
        Length surfaceHeight,
        Length tileWidth,
        Length tileHeight,
        SurfaceLayoutMode layoutMode,
        Length layoutOffset,
        Length minimumOffset,
        Length minimumEdgeWidth,
        Length minimumStartEndMargin
) {

    public TileLayoutRequest(
            Length surfaceWidth,
            Length surfaceHeight,
            Length tileWidth,
            Length tileHeight,
            SurfaceLayoutMode layoutMode,
            Length layoutOffset,
            Length minimumOffset,
            Length minimumEdgeWidth
    ) {
        this(surfaceWidth, surfaceHeight, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumEdgeWidth);
    }

    public TileLayoutRequest(
            Length surfaceWidth,
            Length surfaceHeight,
            Length tileWidth,
            Length tileHeight,
            Length minimumOffset
    ) {
        this(surfaceWidth, surfaceHeight, tileWidth, tileHeight, SurfaceLayoutMode.AUTOMATIC, Length.zero(), minimumOffset, Length.zero(), Length.zero());
    }
}
