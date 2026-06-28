package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
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
        Length minimumStartEndMargin,
        SurfaceLayoutAnchor layoutAnchor,
        Length startRowTrim,
        Length startRowWidth
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
        this(surfaceWidth, surfaceHeight, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumEdgeWidth, SurfaceLayoutAnchor.AUTO, Length.zero(), Length.zero());
    }

    public TileLayoutRequest(
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
        this(surfaceWidth, surfaceHeight, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, SurfaceLayoutAnchor.AUTO, Length.zero(), Length.zero());
    }

    public TileLayoutRequest(
            Length surfaceWidth,
            Length surfaceHeight,
            Length tileWidth,
            Length tileHeight,
            Length minimumOffset
    ) {
        this(surfaceWidth, surfaceHeight, tileWidth, tileHeight, SurfaceLayoutMode.AUTOMATIC, Length.zero(), minimumOffset, Length.zero(), Length.zero(), SurfaceLayoutAnchor.AUTO, Length.zero(), Length.zero());
    }
}
