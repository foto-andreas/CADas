package de.schrell.cadas.infrastructure.dxf;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayoutMargins;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;

import java.util.Locale;
import java.util.UUID;

/**
 * Übersetzt Oberflächenschichten einschließlich Verlegedaten in die versionierte CADas-DXF-Metadatenspur.
 */
final class SurfaceLayerMetadataCodec {

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int THICKNESS_INDEX = 2;
    private static final int VISIBLE_INDEX = 3;
    private static final int TILE_WIDTH_INDEX = 4;
    private static final int TILE_HEIGHT_INDEX = 5;
    private static final int LAYOUT_MODE_INDEX = 6;
    private static final int LAYOUT_OFFSET_INDEX = 7;
    private static final int MINIMUM_OFFSET_INDEX = 8;
    private static final int MINIMUM_EDGE_WIDTH_INDEX = 9;
    private static final int MINIMUM_START_END_MARGIN_INDEX = 10;
    private static final int JOINT_WIDTH_INDEX = 11;
    private static final int COVERING_SOURCE_INDEX = 12;
    private static final int CUT_RESTRICTION_INDEX = 13;
    private static final int LAYOUT_ROTATED_INDEX = 14;
    private static final int LAYOUT_ANCHOR_INDEX = 15;
    private static final int START_ROW_TRIM_INDEX = 16;
    private static final int START_ROW_WIDTH_INDEX = 17;
    private static final int FREE_MARGIN_LEFT_INDEX = 18;
    private static final int FREE_MARGIN_RIGHT_INDEX = 19;
    private static final int FREE_MARGIN_TOP_INDEX = 20;
    private static final int FREE_MARGIN_BOTTOM_INDEX = 21;
    private static final int MATERIAL_ID_INDEX = 22;

    private SurfaceLayerMetadataCodec() {
    }

    static String serializeLevel(SurfaceLayer layer) {
        return "SLL|" + serializedFields(layer);
    }

    static String serializeProject(String levelName, SurfaceLayer layer) {
        return String.format(
                Locale.US,
                "SLL|%s|%s",
                DxfMetadataCodec.encode(levelName),
                serializedFields(layer)
        );
    }

    static SurfaceLayer deserializeLevel(String[] parts, boolean encodedFields) {
        return deserialize(parts, 1, encodedFields);
    }

    static SurfaceLayer deserializeProject(String[] parts, boolean encodedFields) {
        return deserialize(parts, 2, encodedFields);
    }

    private static String serializedFields(SurfaceLayer layer) {
        return String.format(
                Locale.US,
                "%s|%s|%.3f|%s|%.3f|%.3f|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%s|%s|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%s",
                layer.id(),
                DxfMetadataCodec.encode(layer.name()),
                layer.thickness().toMillimeters(),
                layer.visible(),
                layer.tileWidth().toMillimeters(),
                layer.tileHeight().toMillimeters(),
                layer.layoutMode().name(),
                layer.layoutOffset().toMillimeters(),
                layer.minimumOffset().toMillimeters(),
                layer.minimumEdgeWidth().toMillimeters(),
                layer.minimumStartEndMargin().toMillimeters(),
                layer.jointWidth().toMillimeters(),
                DxfMetadataCodec.encode(layer.coveringSource()),
                layer.cutRestriction().name(),
                layer.layoutRotatedQuarterTurn(),
                layer.layoutAnchor().name(),
                layer.startRowTrim().toMillimeters(),
                layer.startRowWidth().toMillimeters(),
                layer.freeMargins().left().toMillimeters(),
                layer.freeMargins().right().toMillimeters(),
                layer.freeMargins().top().toMillimeters(),
                layer.freeMargins().bottom().toMillimeters(),
                layer.materialId() == null ? "" : layer.materialId()
        );
    }

    private static SurfaceLayer deserialize(String[] parts, int startIndex, boolean encodedFields) {
        int relativeLength = parts.length - startIndex;
        boolean hasMinimumStartEndMargin = relativeLength >= 13;
        boolean hasCutRestriction = relativeLength >= 14;
        boolean hasLayoutRotatedQuarterTurn = relativeLength >= 15;
        boolean hasLayoutAnchor = relativeLength >= 18;
        boolean hasFreeMargins = relativeLength >= 22;
        boolean hasMaterialId = relativeLength >= 23 && !parts[startIndex + MATERIAL_ID_INDEX].isBlank();
        SurfaceLayer layer = new SurfaceLayer(
                UUID.fromString(parts[startIndex + ID_INDEX]),
                DxfMetadataCodec.decode(parts[startIndex + NAME_INDEX], encodedFields),
                Length.ofMillimeters(parseDouble(parts[startIndex + THICKNESS_INDEX])),
                Boolean.parseBoolean(parts[startIndex + VISIBLE_INDEX]),
                Length.ofMillimeters(parseDouble(parts[startIndex + TILE_WIDTH_INDEX])),
                Length.ofMillimeters(parseDouble(parts[startIndex + TILE_HEIGHT_INDEX])),
                SurfaceLayoutMode.valueOf(parts[startIndex + LAYOUT_MODE_INDEX]),
                Length.ofMillimeters(parseDouble(parts[startIndex + LAYOUT_OFFSET_INDEX])),
                Length.ofMillimeters(parseDouble(parts[startIndex + MINIMUM_OFFSET_INDEX])),
                Length.ofMillimeters(parseDouble(parts[startIndex + MINIMUM_EDGE_WIDTH_INDEX])),
                Length.ofMillimeters(parseDouble(parts[startIndex + (hasMinimumStartEndMargin ? MINIMUM_START_END_MARGIN_INDEX : MINIMUM_EDGE_WIDTH_INDEX)])),
                new SurfaceLayoutMargins(
                        Length.ofMillimeters(hasFreeMargins ? parseDouble(parts[startIndex + FREE_MARGIN_LEFT_INDEX]) : 0.0),
                        Length.ofMillimeters(hasFreeMargins ? parseDouble(parts[startIndex + FREE_MARGIN_RIGHT_INDEX]) : 0.0),
                        Length.ofMillimeters(hasFreeMargins ? parseDouble(parts[startIndex + FREE_MARGIN_TOP_INDEX]) : 0.0),
                        Length.ofMillimeters(hasFreeMargins ? parseDouble(parts[startIndex + FREE_MARGIN_BOTTOM_INDEX]) : 0.0)
                ),
                hasLayoutAnchor ? SurfaceLayoutAnchor.valueOf(parts[startIndex + LAYOUT_ANCHOR_INDEX]) : SurfaceLayoutAnchor.AUTO,
                Length.ofMillimeters(hasLayoutAnchor ? parseDouble(parts[startIndex + START_ROW_TRIM_INDEX]) : 0.0),
                Length.ofMillimeters(hasLayoutAnchor ? parseDouble(parts[startIndex + START_ROW_WIDTH_INDEX]) : 0.0),
                Length.ofMillimeters(parseDouble(parts[startIndex + (hasMinimumStartEndMargin ? JOINT_WIDTH_INDEX : MINIMUM_START_END_MARGIN_INDEX)])),
                SurfaceCutRestriction.fromStoredValue(hasCutRestriction ? parts[startIndex + CUT_RESTRICTION_INDEX] : null),
                DxfMetadataCodec.decode(parts[startIndex + (hasMinimumStartEndMargin ? COVERING_SOURCE_INDEX : JOINT_WIDTH_INDEX)], encodedFields),
                hasLayoutRotatedQuarterTurn && Boolean.parseBoolean(parts[startIndex + LAYOUT_ROTATED_INDEX])
        );
        return hasMaterialId ? layer.withMaterialId(UUID.fromString(parts[startIndex + MATERIAL_ID_INDEX])) : layer;
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value);
    }
}
