package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;

import java.util.Objects;
import java.util.UUID;

public record SurfaceLayer(
        UUID id,
        String name,
        Length thickness,
        boolean visible,
        Length tileWidth,
        Length tileHeight,
        SurfaceLayoutMode layoutMode,
        Length layoutOffset,
        Length minimumOffset,
        Length minimumEdgeWidth,
        Length minimumStartEndMargin,
        Length jointWidth,
        SurfaceCutRestriction cutRestriction,
        String coveringSource
) {

    public SurfaceLayer(
            UUID id,
            String name,
            Length thickness,
            boolean visible,
            Length tileWidth,
            Length tileHeight,
            SurfaceLayoutMode layoutMode,
            Length layoutOffset,
            Length minimumOffset,
            Length minimumEdgeWidth,
            Length jointWidth,
            String coveringSource
    ) {
        this(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumEdgeWidth, jointWidth, SurfaceCutRestriction.fallback(), coveringSource);
    }

    public SurfaceLayer(
            UUID id,
            String name,
            Length thickness,
            boolean visible,
            Length tileWidth,
            Length tileHeight,
            SurfaceLayoutMode layoutMode,
            Length layoutOffset,
            Length minimumOffset,
            Length minimumEdgeWidth,
            Length minimumStartEndMargin,
            Length jointWidth,
            String coveringSource
    ) {
        this(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, jointWidth, SurfaceCutRestriction.fallback(), coveringSource);
    }

    public SurfaceLayer {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(name, "name darf nicht null sein.");
        Objects.requireNonNull(thickness, "thickness darf nicht null sein.");
        Objects.requireNonNull(tileWidth, "tileWidth darf nicht null sein.");
        Objects.requireNonNull(tileHeight, "tileHeight darf nicht null sein.");
        Objects.requireNonNull(layoutMode, "layoutMode darf nicht null sein.");
        Objects.requireNonNull(layoutOffset, "layoutOffset darf nicht null sein.");
        Objects.requireNonNull(minimumOffset, "minimumOffset darf nicht null sein.");
        Objects.requireNonNull(minimumEdgeWidth, "minimumEdgeWidth darf nicht null sein.");
        Objects.requireNonNull(minimumStartEndMargin, "minimumStartEndMargin darf nicht null sein.");
        Objects.requireNonNull(jointWidth, "jointWidth darf nicht null sein.");
        Objects.requireNonNull(cutRestriction, "cutRestriction darf nicht null sein.");
        Objects.requireNonNull(coveringSource, "coveringSource darf nicht null sein.");
    }

    public static SurfaceLayer create(
            String name,
            Length thickness,
            Length tileWidth,
            Length tileHeight,
            Length minimumOffset
    ) {
        return new SurfaceLayer(UUID.randomUUID(), name, thickness, true, tileWidth, tileHeight, SurfaceLayoutMode.AUTOMATIC, Length.zero(), minimumOffset, Length.zero(), Length.zero(), Length.ofMillimeters(2), SurfaceCutRestriction.fallback(), "");
    }

    public static SurfaceLayer create(
            String name,
            Length thickness,
            Length tileWidth,
            Length tileHeight,
            SurfaceLayoutMode layoutMode,
            Length layoutOffset,
            Length minimumOffset,
            Length minimumEdgeWidth,
            Length minimumStartEndMargin,
            Length jointWidth,
            String coveringSource
    ) {
        return create(name, thickness, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, jointWidth, SurfaceCutRestriction.fallback(), coveringSource);
    }

    public static SurfaceLayer create(
            String name,
            Length thickness,
            Length tileWidth,
            Length tileHeight,
            SurfaceLayoutMode layoutMode,
            Length layoutOffset,
            Length minimumOffset,
            Length minimumEdgeWidth,
            Length minimumStartEndMargin,
            Length jointWidth,
            SurfaceCutRestriction cutRestriction,
            String coveringSource
    ) {
        return new SurfaceLayer(UUID.randomUUID(), name, thickness, true, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, jointWidth, cutRestriction, coveringSource);
    }

    public SurfaceLayer rename(String newName) {
        return new SurfaceLayer(id, newName, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, jointWidth, cutRestriction, coveringSource);
    }

    public SurfaceLayer withVisibility(boolean newVisibility) {
        return new SurfaceLayer(id, name, thickness, newVisibility, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, jointWidth, cutRestriction, coveringSource);
    }

    public SurfaceLayer reconfigure(
            String newName,
            Length newThickness,
            Length newTileWidth,
            Length newTileHeight,
            SurfaceLayoutMode newLayoutMode,
            Length newLayoutOffset,
            Length newMinimumOffset,
            Length newMinimumEdgeWidth,
            Length newMinimumStartEndMargin,
            Length newJointWidth,
            SurfaceCutRestriction newCutRestriction,
            String newCoveringSource
    ) {
        return new SurfaceLayer(
                id,
                newName,
                newThickness,
                visible,
                newTileWidth,
                newTileHeight,
                newLayoutMode,
                newLayoutOffset,
                newMinimumOffset,
                newMinimumEdgeWidth,
                newMinimumStartEndMargin,
                newJointWidth,
                newCutRestriction,
                newCoveringSource
        );
    }

    public SurfaceLayer reconfigure(
            String newName,
            Length newThickness,
            Length newTileWidth,
            Length newTileHeight,
            SurfaceLayoutMode newLayoutMode,
            Length newLayoutOffset,
            Length newMinimumOffset,
            Length newMinimumEdgeWidth,
            Length newMinimumStartEndMargin,
            Length newJointWidth,
            String newCoveringSource
    ) {
        return reconfigure(
                newName,
                newThickness,
                newTileWidth,
                newTileHeight,
                newLayoutMode,
                newLayoutOffset,
                newMinimumOffset,
                newMinimumEdgeWidth,
                newMinimumStartEndMargin,
                newJointWidth,
                cutRestriction,
                newCoveringSource
        );
    }
}
