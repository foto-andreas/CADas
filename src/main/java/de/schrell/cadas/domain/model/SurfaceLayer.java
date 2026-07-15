package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;

import java.util.Objects;
import java.util.UUID;

/**
 * Einzelne Materialschicht eines Oberflächenaufbaus einschließlich Dicke und Verlegedaten.
 * Konstruktion und Rekonfiguration validieren Maße, Fugen, Anker und Zuschnittsbeschränkungen gemeinsam.
 */
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
        SurfaceLayoutMargins freeMargins,
        SurfaceLayoutAnchor layoutAnchor,
        Length startRowTrim,
        Length startRowWidth,
        Length jointWidth,
        SurfaceCutRestriction cutRestriction,
        String coveringSource,
        boolean layoutRotatedQuarterTurn
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
        this(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumEdgeWidth, SurfaceLayoutMargins.zero(), SurfaceLayoutAnchor.AUTO, Length.zero(), Length.zero(), jointWidth, SurfaceCutRestriction.fallback(), coveringSource);
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
        this(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, SurfaceLayoutMargins.zero(), SurfaceLayoutAnchor.AUTO, Length.zero(), Length.zero(), jointWidth, SurfaceCutRestriction.fallback(), coveringSource);
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
            Length jointWidth,
            SurfaceCutRestriction cutRestriction,
            String coveringSource
    ) {
        this(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumEdgeWidth, SurfaceLayoutMargins.zero(), SurfaceLayoutAnchor.AUTO, Length.zero(), Length.zero(), jointWidth, cutRestriction, coveringSource, false);
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
            SurfaceCutRestriction cutRestriction,
            String coveringSource
    ) {
        this(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, SurfaceLayoutMargins.zero(), SurfaceLayoutAnchor.AUTO, Length.zero(), Length.zero(), jointWidth, cutRestriction, coveringSource, false);
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
            Length jointWidth,
            SurfaceCutRestriction cutRestriction,
            String coveringSource,
            boolean layoutRotatedQuarterTurn
    ) {
        this(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumEdgeWidth, SurfaceLayoutMargins.zero(), SurfaceLayoutAnchor.AUTO, Length.zero(), Length.zero(), jointWidth, cutRestriction, coveringSource, layoutRotatedQuarterTurn);
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
            SurfaceCutRestriction cutRestriction,
            String coveringSource,
            boolean layoutRotatedQuarterTurn
    ) {
        this(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, SurfaceLayoutMargins.zero(), SurfaceLayoutAnchor.AUTO, Length.zero(), Length.zero(), jointWidth, cutRestriction, coveringSource, layoutRotatedQuarterTurn);
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
            SurfaceLayoutMargins freeMargins,
            SurfaceLayoutAnchor layoutAnchor,
            Length startRowTrim,
            Length startRowWidth,
            Length jointWidth,
            SurfaceCutRestriction cutRestriction,
            String coveringSource
    ) {
        this(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, freeMargins, layoutAnchor, startRowTrim, startRowWidth, jointWidth, cutRestriction, coveringSource, false);
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
            SurfaceLayoutAnchor layoutAnchor,
            Length startRowTrim,
            Length startRowWidth,
            Length jointWidth,
            SurfaceCutRestriction cutRestriction,
            String coveringSource
    ) {
        this(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, SurfaceLayoutMargins.zero(), layoutAnchor, startRowTrim, startRowWidth, jointWidth, cutRestriction, coveringSource, false);
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
        Objects.requireNonNull(freeMargins, "freeMargins darf nicht null sein.");
        Objects.requireNonNull(layoutAnchor, "layoutAnchor darf nicht null sein.");
        Objects.requireNonNull(startRowTrim, "startRowTrim darf nicht null sein.");
        Objects.requireNonNull(startRowWidth, "startRowWidth darf nicht null sein.");
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
        return new SurfaceLayer(UUID.randomUUID(), name, thickness, true, tileWidth, tileHeight, SurfaceLayoutMode.AUTOMATIC, Length.zero(), minimumOffset, Length.zero(), Length.zero(), SurfaceLayoutMargins.zero(), SurfaceLayoutAnchor.AUTO, Length.zero(), Length.zero(), Length.ofMillimeters(2), SurfaceCutRestriction.fallback(), "", false);
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
        return new SurfaceLayer(UUID.randomUUID(), name, thickness, true, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, SurfaceLayoutMargins.zero(), SurfaceLayoutAnchor.AUTO, Length.zero(), Length.zero(), jointWidth, cutRestriction, coveringSource, false);
    }

    public SurfaceLayer rename(String newName) {
        return new SurfaceLayer(id, newName, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, freeMargins, layoutAnchor, startRowTrim, startRowWidth, jointWidth, cutRestriction, coveringSource, layoutRotatedQuarterTurn);
    }

    public SurfaceLayer withVisibility(boolean newVisibility) {
        return new SurfaceLayer(id, name, thickness, newVisibility, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, freeMargins, layoutAnchor, startRowTrim, startRowWidth, jointWidth, cutRestriction, coveringSource, layoutRotatedQuarterTurn);
    }

    public SurfaceLayer withFreeMargins(SurfaceLayoutMargins newFreeMargins) {
        return new SurfaceLayer(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, newFreeMargins, layoutAnchor, startRowTrim, startRowWidth, jointWidth, cutRestriction, coveringSource, layoutRotatedQuarterTurn);
    }

    public SurfaceLayer withLayoutRotatedQuarterTurn(boolean newLayoutRotatedQuarterTurn) {
        return new SurfaceLayer(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, freeMargins, layoutAnchor, startRowTrim, startRowWidth, jointWidth, cutRestriction, coveringSource, newLayoutRotatedQuarterTurn);
    }

    public SurfaceLayer withLayoutAnchor(SurfaceLayoutAnchor newLayoutAnchor) {
        return new SurfaceLayer(id, name, thickness, visible, tileWidth, tileHeight, layoutMode, layoutOffset, minimumOffset, minimumEdgeWidth, minimumStartEndMargin, freeMargins, newLayoutAnchor, startRowTrim, startRowWidth, jointWidth, cutRestriction, coveringSource, layoutRotatedQuarterTurn);
    }

    public Length effectiveTileWidth() {
        return layoutRotatedQuarterTurn ? tileHeight : tileWidth;
    }

    public Length effectiveTileHeight() {
        return layoutRotatedQuarterTurn ? tileWidth : tileHeight;
    }

    public SurfaceLayoutRotation layoutRotation() {
        if (layoutRotatedQuarterTurn) {
            return startsAtMaximumY(layoutAnchor) ? SurfaceLayoutRotation.DEGREES_270 : SurfaceLayoutRotation.DEGREES_90;
        }
        return startsAtMaximumY(layoutAnchor) ? SurfaceLayoutRotation.DEGREES_180 : SurfaceLayoutRotation.DEGREES_0;
    }

    public SurfaceLayoutDirection layoutDirection() {
        return startsAtMaximumX(layoutAnchor) ? SurfaceLayoutDirection.RIGHT_TO_LEFT : SurfaceLayoutDirection.LEFT_TO_RIGHT;
    }

    public SurfaceLayer withLayoutOrientation(SurfaceLayoutRotation rotation, SurfaceLayoutDirection direction) {
        return new SurfaceLayer(
                id,
                name,
                thickness,
                visible,
                tileWidth,
                tileHeight,
                layoutMode,
                layoutOffset,
                minimumOffset,
                minimumEdgeWidth,
                minimumStartEndMargin,
                freeMargins,
                anchorFor(rotation, direction),
                startRowTrim,
                startRowWidth,
                jointWidth,
                cutRestriction,
                coveringSource,
                rotation.rotatedQuarterTurn()
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
            SurfaceLayoutMargins newFreeMargins,
            SurfaceLayoutAnchor newLayoutAnchor,
            Length newStartRowTrim,
            Length newStartRowWidth,
            Length newJointWidth,
            SurfaceCutRestriction newCutRestriction,
            String newCoveringSource,
            boolean newLayoutRotatedQuarterTurn
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
                newFreeMargins,
                newLayoutAnchor,
                newStartRowTrim,
                newStartRowWidth,
                newJointWidth,
                newCutRestriction,
                newCoveringSource,
                newLayoutRotatedQuarterTurn
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
            SurfaceLayoutMargins newFreeMargins,
            SurfaceLayoutAnchor newLayoutAnchor,
            Length newStartRowTrim,
            Length newStartRowWidth,
            Length newJointWidth,
            SurfaceCutRestriction newCutRestriction,
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
                newFreeMargins,
                newLayoutAnchor,
                newStartRowTrim,
                newStartRowWidth,
                newJointWidth,
                newCutRestriction,
                newCoveringSource,
                layoutRotatedQuarterTurn
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
            SurfaceLayoutMargins newFreeMargins,
            SurfaceLayoutAnchor newLayoutAnchor,
            Length newStartRowTrim,
            Length newStartRowWidth,
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
                newFreeMargins,
                newLayoutAnchor,
                newStartRowTrim,
                newStartRowWidth,
                newJointWidth,
                cutRestriction,
                newCoveringSource,
                layoutRotatedQuarterTurn
        );
    }

    private static boolean startsAtMaximumX(SurfaceLayoutAnchor anchor) {
        return anchor == SurfaceLayoutAnchor.MAX_X_MIN_Y || anchor == SurfaceLayoutAnchor.MAX_X_MAX_Y;
    }

    private static boolean startsAtMaximumY(SurfaceLayoutAnchor anchor) {
        return anchor == SurfaceLayoutAnchor.MAX_X_MAX_Y || anchor == SurfaceLayoutAnchor.MIN_X_MAX_Y;
    }

    public static SurfaceLayoutAnchor anchorFor(SurfaceLayoutRotation rotation, SurfaceLayoutDirection direction) {
        boolean maximumX = direction == SurfaceLayoutDirection.RIGHT_TO_LEFT;
        if (rotation.maximumYStart()) {
            return maximumX ? SurfaceLayoutAnchor.MAX_X_MAX_Y : SurfaceLayoutAnchor.MIN_X_MAX_Y;
        }
        return maximumX ? SurfaceLayoutAnchor.MAX_X_MIN_Y : SurfaceLayoutAnchor.MIN_X_MIN_Y;
    }
}
