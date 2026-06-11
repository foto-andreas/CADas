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
        String coveringSource
) {

    @Override
    public String toString() {
        return name;
    }
}
