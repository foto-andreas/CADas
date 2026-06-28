package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import de.schrell.cadas.domain.model.SurfaceLayoutDirection;
import de.schrell.cadas.domain.model.SurfaceLayoutMargins;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceLayoutRotation;
import de.schrell.cadas.domain.model.SurfaceLayer;

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
        SurfaceLayoutMargins freeMargins,
        SurfaceLayoutAnchor layoutAnchor,
        boolean layoutRotatedQuarterTurn,
        Length startRowTrim,
        Length startRowWidth,
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
        this(id, name, thickness, tileWidth, tileHeight, layoutMode, offset, minimumOffset, minimumEdgeWidth, minimumEdgeWidth, SurfaceLayoutMargins.zero(), SurfaceLayoutAnchor.AUTO, false, Length.zero(), Length.zero(), jointWidth, SurfaceCutRestriction.fallback(), coveringSource);
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
        this(id, name, thickness, tileWidth, tileHeight, layoutMode, offset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, SurfaceLayoutMargins.zero(), SurfaceLayoutAnchor.AUTO, false, Length.zero(), Length.zero(), jointWidth, SurfaceCutRestriction.fallback(), coveringSource);
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
            Length jointWidth,
            SurfaceCutRestriction cutRestriction,
            String coveringSource
    ) {
        this(id, name, thickness, tileWidth, tileHeight, layoutMode, offset, minimumOffset, minimumEdgeWidth, minimumEdgeWidth, SurfaceLayoutMargins.zero(), SurfaceLayoutAnchor.AUTO, false, Length.zero(), Length.zero(), jointWidth, cutRestriction, coveringSource);
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
            SurfaceCutRestriction cutRestriction,
            String coveringSource
    ) {
        this(id, name, thickness, tileWidth, tileHeight, layoutMode, offset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, SurfaceLayoutMargins.zero(), SurfaceLayoutAnchor.AUTO, false, Length.zero(), Length.zero(), jointWidth, cutRestriction, coveringSource);
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
            SurfaceLayoutAnchor layoutAnchor,
            Length startRowTrim,
            Length startRowWidth,
            Length jointWidth,
            String coveringSource
    ) {
        this(id, name, thickness, tileWidth, tileHeight, layoutMode, offset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, SurfaceLayoutMargins.zero(), layoutAnchor, false, startRowTrim, startRowWidth, jointWidth, SurfaceCutRestriction.fallback(), coveringSource);
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
            SurfaceLayoutAnchor layoutAnchor,
            Length startRowTrim,
            Length startRowWidth,
            Length jointWidth,
            SurfaceCutRestriction cutRestriction,
            String coveringSource
    ) {
        this(id, name, thickness, tileWidth, tileHeight, layoutMode, offset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, SurfaceLayoutMargins.zero(), layoutAnchor, false, startRowTrim, startRowWidth, jointWidth, cutRestriction, coveringSource);
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
            SurfaceLayoutMargins freeMargins,
            SurfaceLayoutAnchor layoutAnchor,
            boolean layoutRotatedQuarterTurn,
            Length startRowTrim,
            Length startRowWidth,
            Length jointWidth,
            String coveringSource
    ) {
        this(id, name, thickness, tileWidth, tileHeight, layoutMode, offset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, freeMargins, layoutAnchor, layoutRotatedQuarterTurn, startRowTrim, startRowWidth, jointWidth, SurfaceCutRestriction.fallback(), coveringSource);
    }

    public SurfaceLayoutRotation layoutRotation() {
        if (layoutRotatedQuarterTurn) {
            return layoutAnchor == SurfaceLayoutAnchor.MAX_X_MAX_Y || layoutAnchor == SurfaceLayoutAnchor.MIN_X_MAX_Y
                    ? SurfaceLayoutRotation.DEGREES_270
                    : SurfaceLayoutRotation.DEGREES_90;
        }
        return layoutAnchor == SurfaceLayoutAnchor.MAX_X_MAX_Y || layoutAnchor == SurfaceLayoutAnchor.MIN_X_MAX_Y
                ? SurfaceLayoutRotation.DEGREES_180
                : SurfaceLayoutRotation.DEGREES_0;
    }

    public SurfaceLayoutDirection layoutDirection() {
        return layoutAnchor == SurfaceLayoutAnchor.MAX_X_MIN_Y || layoutAnchor == SurfaceLayoutAnchor.MAX_X_MAX_Y
                ? SurfaceLayoutDirection.RIGHT_TO_LEFT
                : SurfaceLayoutDirection.LEFT_TO_RIGHT;
    }

    public SurfaceCoveringPreset withLayoutOrientation(SurfaceLayoutRotation rotation, SurfaceLayoutDirection direction) {
        return new SurfaceCoveringPreset(
                id,
                name,
                thickness,
                tileWidth,
                tileHeight,
                layoutMode,
                offset,
                minimumOffset,
                minimumEdgeWidth,
                minimumStartEndMargin,
                freeMargins,
                SurfaceLayer.anchorFor(rotation, direction),
                rotation.rotatedQuarterTurn(),
                startRowTrim,
                startRowWidth,
                jointWidth,
                cutRestriction,
                coveringSource
        );
    }

    @Override
    public String toString() {
        return name;
    }
}
