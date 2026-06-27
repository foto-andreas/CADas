package de.schrell.cadas.application.layers;

import de.schrell.cadas.application.room.OrthogonalPolygonDecompositionService.CellRectangle;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Legt Oberflächenbeläge für orthogonale Raumflächen mit durchgehendem globalem Raster aus.
 */
public final class SurfaceRectangleTileLayoutService {

    private static final double EPSILON = 0.001;
    private static final double MIN_RELEVANT_SCORE_RATIO = 0.1;

    /**
     * Erzeugt absolute Belagspositionen mit durchgehendem Raumraster über alle Teilrechtecke.
     */
    public List<PlacedSurfaceTile> tilesForRectangles(List<CellRectangle> rectangles, SurfaceLayer layer) {
        if (rectangles.isEmpty()) {
            return List.of();
        }
        double tileWidth = layer.effectiveTileWidth().toMillimeters();
        double tileHeight = layer.effectiveTileHeight().toMillimeters();
        if (tileWidth <= EPSILON || tileHeight <= EPSILON) {
            return List.of();
        }
        SurfaceBounds bounds = bounds(rectangles);
        AnchorPair anchors = bestAnchors(rectangles, layer, bounds);
        return clipTilesToRectangles(rectangles, layer, bounds, anchors.anchorX(), anchors.anchorY());
    }

    private AnchorPair bestAnchors(List<CellRectangle> rectangles, SurfaceLayer layer, SurfaceBounds bounds) {
        double tileWidth = layer.effectiveTileWidth().toMillimeters();
        double tileHeight = layer.effectiveTileHeight().toMillimeters();
        if (tileWidth <= EPSILON || tileHeight <= EPSILON) {
            return new AnchorPair(bounds.minX(), bounds.minY());
        }
        LayoutScore bestScore = null;
        double bestCandidate = bounds.minX();
        double bestAnchorY = bounds.minY();
        for (double yCandidate : anchorYCandidates(rectangles, layer, bounds)) {
            double normalizedY = candidateInBounds(yCandidate, bounds.minY(), tileHeight);
            for (double xCandidate : anchorCandidates(rectangles, layer, bounds, normalizedY)) {
                double normalizedX = candidateInBounds(xCandidate, bounds.minX(), tileWidth);
                LayoutScore score = score(rectangles, layer, bounds, normalizedX, normalizedY);
                if (bestScore == null || score.compareTo(bestScore) < 0) {
                    bestScore = score;
                    bestCandidate = normalizedX;
                    bestAnchorY = normalizedY;
                }
            }
        }
        return new AnchorPair(bestCandidate, bestAnchorY);
    }

    private List<Double> anchorCandidates(List<CellRectangle> rectangles, SurfaceLayer layer, SurfaceBounds bounds, double anchorY) {
        double tileWidth = layer.effectiveTileWidth().toMillimeters();
        List<Double> candidates = new ArrayList<>();
        candidates.add(bounds.minX());
        int firstRow = firstRelevantRow(bounds, anchorY, layer.effectiveTileHeight().toMillimeters());
        int lastRow = lastRelevantRow(bounds, anchorY, layer.effectiveTileHeight().toMillimeters());
        for (int row = firstRow; row <= lastRow; row++) {
            double rowOffset = rowOffset(layer, row);
            for (CellRectangle rectangle : rectangles) {
                candidates.add(rectangle.minX() + rowOffset);
                candidates.add(rectangle.maxX() - tileWidth + rowOffset);
            }
        }
        return candidates;
    }

    private List<Double> anchorYCandidates(List<CellRectangle> rectangles, SurfaceLayer layer, SurfaceBounds bounds) {
        double tileHeight = layer.effectiveTileHeight().toMillimeters();
        double surfaceHeight = bounds.maxY() - bounds.minY();
        List<Double> candidates = new ArrayList<>();
        candidates.add(bounds.minY() - boundedStartTrim(surfaceHeight, tileHeight, layer.minimumStartEndMargin().toMillimeters()));
        for (CellRectangle rectangle : rectangles) {
            candidates.add(rectangle.minY());
            candidates.add(rectangle.maxY() - tileHeight);
        }
        return candidates;
    }

    private LayoutScore score(
            List<CellRectangle> rectangles,
            SurfaceLayer layer,
            SurfaceBounds bounds,
            double anchorX,
            double anchorY
    ) {
        double tileWidth = layer.effectiveTileWidth().toMillimeters();
        double tileHeight = layer.effectiveTileHeight().toMillimeters();
        double tileArea = tileWidth * tileHeight;
        int cutTiles = 0;
        double wasteArea = 0.0;
        double usedArea = 0.0;
        for (PlacedSurfaceTile tile : clipTilesToRectangles(rectangles, layer, bounds, anchorX, anchorY)) {
            double area = tile.width() * tile.height();
            if (area <= EPSILON) {
                continue;
            }
            usedArea += area;
            if (area + EPSILON < tileArea && isRelevantScorePiece(tile, tileWidth, tileHeight)) {
                cutTiles++;
                wasteArea += tileArea - area;
            }
        }
        double alignmentPenalty = alignmentPenalty(rectangles, layer, bounds, anchorX, anchorY, tileWidth, tileHeight);
        return new LayoutScore(
                cutTiles,
                wasteArea,
                alignmentPenalty,
                -usedArea,
                anchorDistance(anchorX, bounds.minX(), tileWidth) + anchorDistance(anchorY, bounds.minY(), tileHeight)
        );
    }

    private double alignmentPenalty(
            List<CellRectangle> rectangles,
            SurfaceLayer layer,
            SurfaceBounds bounds,
            double anchorX,
            double anchorY,
            double tileWidth,
            double tileHeight
    ) {
        double penalty = 0.0;
        int firstRow = firstRelevantRow(bounds, anchorY, tileHeight);
        int lastRow = lastRelevantRow(bounds, anchorY, tileHeight);
        for (CellRectangle rectangle : rectangles) {
            if (rectangle.width() + EPSILON < tileWidth) {
                continue;
            }
            double bestDistance = Double.POSITIVE_INFINITY;
            for (int row = firstRow; row <= lastRow; row++) {
                double rowStart = anchorY + row * tileHeight;
                double rowEnd = rowStart + tileHeight;
                if (rowEnd <= rectangle.minY() + EPSILON || rowStart >= rectangle.maxY() - EPSILON) {
                    continue;
                }
                double boundaryX = anchorX - rowOffset(layer, row);
                bestDistance = Math.min(bestDistance, boundaryDistance(rectangle.minX(), boundaryX, tileWidth));
            }
            if (bestDistance < Double.POSITIVE_INFINITY) {
                penalty += bestDistance / Math.max(rectangle.width(), EPSILON);
            }
        }
        return penalty;
    }

    private boolean isRelevantScorePiece(PlacedSurfaceTile tile, double tileWidth, double tileHeight) {
        double minimumRelevantSpan = Math.min(tileWidth, tileHeight) * MIN_RELEVANT_SCORE_RATIO;
        return tile.width() >= minimumRelevantSpan && tile.height() >= minimumRelevantSpan;
    }

    private double boundaryDistance(double value, double firstBoundary, double spacing) {
        double distance = modulo(value - firstBoundary, spacing);
        return Math.min(distance, Math.abs(spacing - distance));
    }

    private double anchorDistance(double anchorX, double minX, double tileWidth) {
        double distance = Math.abs(modulo(minX - anchorX, tileWidth));
        return Math.min(distance, Math.abs(tileWidth - distance));
    }

    private List<PlacedSurfaceTile> generateBoundingTiles(
            SurfaceBounds bounds,
            SurfaceLayer layer,
            double anchorX,
            double anchorY
    ) {
        double tileWidth = layer.effectiveTileWidth().toMillimeters();
        double tileHeight = layer.effectiveTileHeight().toMillimeters();
        List<PlacedSurfaceTile> tiles = new ArrayList<>();
        int firstRow = firstRelevantRow(bounds, anchorY, tileHeight);
        int lastRow = lastRelevantRow(bounds, anchorY, tileHeight);
        for (int row = firstRow; row <= lastRow; row++) {
            double y = anchorY + row * tileHeight;
            double clippedY = Math.max(bounds.minY(), y);
            double remainingHeight = Math.min(tileHeight - Math.max(0.0, bounds.minY() - y), bounds.maxY() - clippedY);
            if (remainingHeight <= EPSILON) {
                continue;
            }
            double rowOffset = rowOffset(layer, row);
            int firstColumn = firstRelevantColumn(bounds, anchorX, rowOffset, tileWidth);
            int lastColumn = lastRelevantColumn(bounds, anchorX, rowOffset, tileWidth);
            for (int column = firstColumn; column <= lastColumn; column++) {
                double x = anchorX - rowOffset + column * tileWidth;
                double clippedX = Math.max(bounds.minX(), x);
                double remainingWidth = Math.min(tileWidth - Math.max(0.0, bounds.minX() - x), bounds.maxX() - clippedX);
                if (remainingWidth <= EPSILON) {
                    continue;
                }
                tiles.add(new PlacedSurfaceTile(
                        column,
                        row,
                        clippedX,
                        clippedY,
                        remainingWidth,
                        remainingHeight
                ));
            }
        }
        return List.copyOf(tiles);
    }

    private List<PlacedSurfaceTile> clipTilesToRectangles(
            List<CellRectangle> rectangles,
            SurfaceLayer layer,
            SurfaceBounds bounds,
            double anchorX,
            double anchorY
    ) {
        Map<TileKey, List<PlacedSurfaceTile>> piecesByTile = new LinkedHashMap<>();
        for (PlacedSurfaceTile tile : generateBoundingTiles(bounds, layer, anchorX, anchorY)) {
            for (CellRectangle rectangle : rectangles) {
                double clippedX = Math.max(tile.x(), rectangle.minX());
                double clippedY = Math.max(tile.y(), rectangle.minY());
                double clippedWidth = Math.min(tile.x() + tile.width(), rectangle.maxX()) - clippedX;
                double clippedHeight = Math.min(tile.y() + tile.height(), rectangle.maxY()) - clippedY;
                if (clippedWidth <= EPSILON || clippedHeight <= EPSILON) {
                    continue;
                }
                piecesByTile.computeIfAbsent(new TileKey(tile.column(), tile.row()), ignored -> new ArrayList<>())
                        .add(new PlacedSurfaceTile(tile.column(), tile.row(), clippedX, clippedY, clippedWidth, clippedHeight));
            }
        }
        List<PlacedSurfaceTile> mergedTiles = new ArrayList<>();
        for (List<PlacedSurfaceTile> pieces : piecesByTile.values()) {
            mergedTiles.addAll(mergeTouchingPieces(pieces));
        }
        mergedTiles.sort(Comparator.comparingInt(PlacedSurfaceTile::row)
                .thenComparingInt(PlacedSurfaceTile::column)
                .thenComparingDouble(PlacedSurfaceTile::y)
                .thenComparingDouble(PlacedSurfaceTile::x));
        return List.copyOf(mergedTiles);
    }

    private List<PlacedSurfaceTile> mergeTouchingPieces(List<PlacedSurfaceTile> pieces) {
        List<PlacedSurfaceTile> merged = new ArrayList<>(pieces);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int firstIndex = 0; firstIndex < merged.size() && !changed; firstIndex++) {
                for (int secondIndex = firstIndex + 1; secondIndex < merged.size(); secondIndex++) {
                    PlacedSurfaceTile combined = mergeIfTouching(merged.get(firstIndex), merged.get(secondIndex));
                    if (combined == null) {
                        continue;
                    }
                    merged.remove(secondIndex);
                    merged.remove(firstIndex);
                    merged.add(combined);
                    changed = true;
                    break;
                }
            }
        }
        return List.copyOf(merged);
    }

    private PlacedSurfaceTile mergeIfTouching(PlacedSurfaceTile first, PlacedSurfaceTile second) {
        if (sameValue(first.x(), second.x()) && sameValue(first.width(), second.width())) {
            double firstMaxY = first.y() + first.height();
            double secondMaxY = second.y() + second.height();
            if (sameValue(firstMaxY, second.y()) || sameValue(secondMaxY, first.y())) {
                double minY = Math.min(first.y(), second.y());
                double maxY = Math.max(firstMaxY, secondMaxY);
                return new PlacedSurfaceTile(first.column(), first.row(), first.x(), minY, first.width(), maxY - minY);
            }
        }
        if (sameValue(first.y(), second.y()) && sameValue(first.height(), second.height())) {
            double firstMaxX = first.x() + first.width();
            double secondMaxX = second.x() + second.width();
            if (sameValue(firstMaxX, second.x()) || sameValue(secondMaxX, first.x())) {
                double minX = Math.min(first.x(), second.x());
                double maxX = Math.max(firstMaxX, secondMaxX);
                return new PlacedSurfaceTile(first.column(), first.row(), minX, first.y(), maxX - minX, first.height());
            }
        }
        return null;
    }

    private boolean sameValue(double first, double second) {
        return Math.abs(first - second) <= EPSILON;
    }

    private int firstRelevantRow(SurfaceBounds bounds, double anchorY, double tileHeight) {
        return (int) Math.floor((bounds.minY() - anchorY) / tileHeight) - 1;
    }

    private int lastRelevantRow(SurfaceBounds bounds, double anchorY, double tileHeight) {
        return (int) Math.ceil((bounds.maxY() - anchorY) / tileHeight) + 1;
    }

    private int firstRelevantColumn(SurfaceBounds bounds, double anchorX, double rowOffset, double tileWidth) {
        return (int) Math.floor((bounds.minX() - (anchorX - rowOffset)) / tileWidth) - 1;
    }

    private int lastRelevantColumn(SurfaceBounds bounds, double anchorX, double rowOffset, double tileWidth) {
        return (int) Math.ceil((bounds.maxX() - (anchorX - rowOffset)) / tileWidth) + 1;
    }

    private double rowOffset(SurfaceLayer layer, int row) {
        double tileWidth = layer.effectiveTileWidth().toMillimeters();
        double minimumOffset = layer.minimumOffset().toMillimeters();
        double minimumEdgeWidth = layer.minimumEdgeWidth().toMillimeters();
        double requestedOffset = switch (layer.layoutMode()) {
            case NONE -> 0.0;
            case FIXED -> modulo(row * layer.layoutOffset().toMillimeters(), tileWidth);
            case AUTOMATIC -> row % 2 == 0 ? 0.0 : tileWidth / 2.0;
        };
        return boundedOffset(requestedOffset, tileWidth, minimumOffset, minimumEdgeWidth);
    }

    private double boundedStartTrim(double surfaceHeight, double tileHeight, double minimumStartEndMargin) {
        if (surfaceHeight <= EPSILON || tileHeight <= EPSILON || minimumStartEndMargin <= EPSILON) {
            return 0.0;
        }
        double trailingHeight = modulo(surfaceHeight, tileHeight);
        if (trailingHeight <= EPSILON || trailingHeight + EPSILON >= minimumStartEndMargin) {
            return 0.0;
        }
        double requiredTrim = minimumStartEndMargin - trailingHeight;
        double maximumTrim = Math.max(0.0, tileHeight - minimumStartEndMargin);
        return Math.min(requiredTrim, maximumTrim);
    }

    private double boundedOffset(double requestedOffset, double tileWidth, double minimumOffset, double minimumEdgeWidth) {
        if (tileWidth <= EPSILON) {
            return 0.0;
        }
        double lowerBound = Math.max(minimumOffset, minimumEdgeWidth);
        if (lowerBound <= EPSILON) {
            return Math.max(0.0, Math.min(requestedOffset, tileWidth));
        }
        double upperBound = tileWidth - lowerBound;
        if (upperBound < lowerBound) {
            return lowerBound;
        }
        return Math.max(lowerBound, Math.min(requestedOffset, upperBound));
    }

    private double candidateInBounds(double candidate, double minX, double tileWidth) {
        double normalized = minX - modulo(minX - candidate, tileWidth);
        if (normalized > minX + EPSILON) {
            return normalized - tileWidth;
        }
        return normalized;
    }

    private double modulo(double value, double divisor) {
        if (divisor <= EPSILON) {
            return 0.0;
        }
        double remainder = value % divisor;
        return remainder < 0.0 ? remainder + divisor : remainder;
    }

    private SurfaceBounds bounds(List<CellRectangle> rectangles) {
        double minX = rectangles.stream().mapToDouble(CellRectangle::minX).min().orElse(0.0);
        double maxX = rectangles.stream().mapToDouble(CellRectangle::maxX).max().orElse(0.0);
        double minY = rectangles.stream().mapToDouble(CellRectangle::minY).min().orElse(0.0);
        double maxY = rectangles.stream().mapToDouble(CellRectangle::maxY).max().orElse(0.0);
        return new SurfaceBounds(minX, maxX, minY, maxY);
    }

    public record PlacedSurfaceTile(
            int column,
            int row,
            double x,
            double y,
            double width,
            double height
    ) {
    }

    private record TileKey(int column, int row) {
    }

    private record AnchorPair(double anchorX, double anchorY) {
    }

    private record LayoutScore(
            int cutTiles,
            double wasteArea,
            double alignmentPenalty,
            double negativeUsedArea,
            double anchorDistance
    )
            implements Comparable<LayoutScore> {

        @Override
        public int compareTo(LayoutScore other) {
            int cutComparison = Integer.compare(cutTiles, other.cutTiles);
            if (cutComparison != 0) {
                return cutComparison;
            }
            int alignmentComparison = Double.compare(alignmentPenalty, other.alignmentPenalty);
            if (alignmentComparison != 0) {
                return alignmentComparison;
            }
            int wasteComparison = Double.compare(wasteArea, other.wasteArea);
            if (wasteComparison != 0) {
                return wasteComparison;
            }
            int areaComparison = Double.compare(negativeUsedArea, other.negativeUsedArea);
            if (areaComparison != 0) {
                return areaComparison;
            }
            return Double.compare(anchorDistance, other.anchorDistance);
        }
    }

    private record SurfaceBounds(double minX, double maxX, double minY, double maxY) {
    }
}
