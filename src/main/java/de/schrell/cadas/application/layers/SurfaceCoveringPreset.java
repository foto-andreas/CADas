package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;

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
        SurfaceCutRestriction cutRestriction,
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
        this(id, name, thickness, tileWidth, tileHeight, layoutMode, offset, minimumOffset, minimumEdgeWidth, minimumEdgeWidth, jointWidth, SurfaceCutRestriction.fallback(), coveringSource);
    }

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
            Length minimumStartEndMargin,
            Length jointWidth,
            String coveringSource
    ) {
        this(id, name, thickness, tileWidth, tileHeight, layoutMode, offset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, jointWidth, SurfaceCutRestriction.fallback(), coveringSource);
    }

    @Override
    public String toString() {
        return name;
    }
}
