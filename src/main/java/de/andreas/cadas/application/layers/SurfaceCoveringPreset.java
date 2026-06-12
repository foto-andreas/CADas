package de.andreas.cadas.application.layers;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.model.SurfaceLayoutMode;

public record SurfaceCoveringPreset(
        String id,
        String name,
        Length thickness,
        Length tileWidth,
        Length tileHeight,
        SurfaceLayoutMode layoutMode,
        Length offset,
        Length minimumOffset,
        Length minimumEdgeWidth,
        Length minimumStartEndMargin,
        Length jointWidth,
        String coveringSource
) {

    public SurfaceCoveringPreset(
            String id,
            String name,
            Length thickness,
            Length tileWidth,
            Length tileHeight,
            SurfaceLayoutMode layoutMode,
            Length offset,
            Length minimumOffset,
            Length minimumEdgeWidth,
            Length jointWidth,
            String coveringSource
    ) {
        this(id, name, thickness, tileWidth, tileHeight, layoutMode, offset, minimumOffset, minimumEdgeWidth, minimumEdgeWidth, jointWidth, coveringSource);
    }

    @Override
    public String toString() {
        return name;
    }
}
