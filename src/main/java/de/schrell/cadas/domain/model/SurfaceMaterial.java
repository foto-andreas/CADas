package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Zentrale, wiederverwendbare Definition eines Belags innerhalb einer Zeichnung.
 * Nutzungen behalten nur ihre flächenbezogenen Verlegewerte wie Sichtbarkeit und Startecke.
 */
public record SurfaceMaterial(
        UUID id,
        String name,
        Length thickness,
        Length tileWidth,
        Length tileHeight,
        SurfaceLayoutMode layoutMode,
        Length layoutOffset,
        Length minimumOffset,
        Length minimumEdgeWidth,
        Length minimumStartEndMargin,
        SurfaceLayoutMargins freeMargins,
        Length jointWidth,
        SurfaceCutRestriction cutRestriction,
        String coveringSource
) {

    public SurfaceMaterial {
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
        Objects.requireNonNull(jointWidth, "jointWidth darf nicht null sein.");
        Objects.requireNonNull(cutRestriction, "cutRestriction darf nicht null sein.");
        Objects.requireNonNull(coveringSource, "coveringSource darf nicht null sein.");
    }

    public static SurfaceMaterial fromLayer(SurfaceLayer layer) {
        Objects.requireNonNull(layer, "layer darf nicht null sein.");
        return new SurfaceMaterial(
                layer.materialId() == null ? UUID.randomUUID() : layer.materialId(),
                layer.name(),
                layer.thickness(),
                layer.tileWidth(),
                layer.tileHeight(),
                layer.layoutMode(),
                layer.layoutOffset(),
                layer.minimumOffset(),
                layer.minimumEdgeWidth(),
                layer.minimumStartEndMargin(),
                layer.freeMargins(),
                layer.jointWidth(),
                layer.cutRestriction(),
                layer.coveringSource()
        );
    }

    public SurfaceLayer applyTo(SurfaceLayer usage) {
        Objects.requireNonNull(usage, "usage darf nicht null sein.");
        return new SurfaceLayer(
                usage.id(),
                name,
                thickness,
                usage.visible(),
                tileWidth,
                tileHeight,
                layoutMode,
                layoutOffset,
                minimumOffset,
                minimumEdgeWidth,
                minimumStartEndMargin,
                freeMargins,
                usage.layoutAnchor(),
                usage.startRowTrim(),
                usage.startRowWidth(),
                jointWidth,
                cutRestriction,
                coveringSource,
                usage.layoutRotatedQuarterTurn(),
                id
        );
    }

    public SurfaceLayer createUsage() {
        return new SurfaceLayer(
                UUID.randomUUID(),
                name,
                thickness,
                true,
                tileWidth,
                tileHeight,
                layoutMode,
                layoutOffset,
                minimumOffset,
                minimumEdgeWidth,
                minimumStartEndMargin,
                freeMargins,
                SurfaceLayoutAnchor.AUTO,
                Length.zero(),
                Length.zero(),
                jointWidth,
                cutRestriction,
                coveringSource,
                false,
                id
        );
    }

    public SurfaceMaterial withId(UUID newId) {
        return new SurfaceMaterial(newId, name, thickness, tileWidth, tileHeight, layoutMode, layoutOffset,
                minimumOffset, minimumEdgeWidth, minimumStartEndMargin, freeMargins, jointWidth, cutRestriction,
                coveringSource);
    }

    public String legacyMigrationKey() {
        String normalizedSource = coveringSource.trim().toLowerCase(Locale.ROOT);
        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        return normalizedSource.isBlank() ? "name:" + normalizedName : "source:" + normalizedSource;
    }

    public String valueSignature() {
        return name + "|" + thickness.toMillimeters() + "|" + tileWidth.toMillimeters() + "|"
                + tileHeight.toMillimeters() + "|" + layoutMode + "|" + layoutOffset.toMillimeters() + "|"
                + minimumOffset.toMillimeters() + "|" + minimumEdgeWidth.toMillimeters() + "|"
                + minimumStartEndMargin.toMillimeters() + "|" + freeMargins + "|" + jointWidth.toMillimeters()
                + "|" + cutRestriction + "|" + coveringSource;
    }
}
