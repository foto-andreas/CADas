package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import de.schrell.cadas.domain.model.SurfaceLayoutMargins;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;

/** Vollständig validierte Eingabe für die Belegung einer rechteckigen Fläche mit einem Plattenraster. */
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
        SurfaceLayoutMargins freeMargins,
        SurfaceLayoutAnchor layoutAnchor,
        Length startRowTrim,
        Length startRowWidth
) {

    public static TileLayoutRequest withDefaults(
            Length surfaceWidth,
            Length surfaceHeight,
            Length tileWidth,
            Length tileHeight,
            SurfaceLayoutMode layoutMode,
            Length layoutOffset,
            Length minimumOffset,
            Length minimumEdgeWidth
    ) {
        return new TileLayoutRequest(
                surfaceWidth,
                surfaceHeight,
                tileWidth,
                tileHeight,
                layoutMode,
                layoutOffset,
                minimumOffset,
                minimumEdgeWidth,
                minimumEdgeWidth,
                SurfaceLayoutMargins.zero(),
                SurfaceLayoutAnchor.AUTO,
                Length.zero(),
                Length.zero()
        );
    }
}
