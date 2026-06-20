package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.geometry.Length;

import java.util.ArrayList;
import java.util.List;

public final class TileLayoutService {

    public List<TilePlacement> fillSurface(TileLayoutRequest request) {
        List<TilePlacement> placements = new ArrayList<>();
        double surfaceWidth = request.surfaceWidth().toMillimeters();
        double surfaceHeight = request.surfaceHeight().toMillimeters();
        double tileWidth = request.tileWidth().toMillimeters();
        double tileHeight = request.tileHeight().toMillimeters();
        double minimumOffset = request.minimumOffset().toMillimeters();
        double minimumEdgeWidth = request.minimumEdgeWidth().toMillimeters();
        double minimumStartEndMargin = request.minimumStartEndMargin().toMillimeters();
        double layoutOffset = request.layoutOffset().toMillimeters();
        double rowStartTrim = boundedStartTrim(surfaceHeight, tileHeight, minimumStartEndMargin);

        int row = 0;
        for (double y = -rowStartTrim; y < surfaceHeight - 0.001; y += tileHeight, row++) {
            double rowOffset = switch (request.layoutMode()) {
                case NONE -> 0.0;
                case FIXED -> boundedOffset(
                        (row * layoutOffset) % tileWidth,
                        tileWidth, minimumOffset, minimumEdgeWidth);
                case AUTOMATIC -> boundedOffset(
                        row % 2 == 0 ? 0.0 : tileWidth / 2.0,
                        tileWidth, minimumOffset, minimumEdgeWidth);
            };
            double clippedY = Math.max(0.0, y);
            double remainingHeight = Math.min(tileHeight - Math.max(0.0, -y), surfaceHeight - clippedY);
            if (remainingHeight <= 0.0) {
                continue;
            }
            int column = 0;
            for (double x = -rowOffset; x < surfaceWidth - 0.001; x += tileWidth, column++) {
                double clippedX = Math.max(0.0, x);
                double remainingWidth = Math.min(tileWidth - Math.max(0.0, -x), surfaceWidth - clippedX);
                if (remainingWidth <= 0.0 || remainingHeight <= 0.0) {
                    continue;
                }
                placements.add(new TilePlacement(
                        column,
                        row,
                        Length.ofMillimeters(clippedX),
                        Length.ofMillimeters(clippedY),
                        Length.ofMillimeters(remainingWidth),
                        Length.ofMillimeters(remainingHeight)
                ));
            }
        }
        return placements;
    }

    private double boundedStartTrim(double surfaceHeight, double tileHeight, double minimumStartEndMargin) {
        if (surfaceHeight <= 0.001 || tileHeight <= 0.001 || minimumStartEndMargin <= 0.001) {
            return 0.0;
        }
        double trailingHeight = surfaceHeight % tileHeight;
        if (trailingHeight <= 0.001 || trailingHeight + 0.001 >= minimumStartEndMargin) {
            return 0.0;
        }
        double requiredTrim = minimumStartEndMargin - trailingHeight;
        double maximumTrim = Math.max(0.0, tileHeight - minimumStartEndMargin);
        return Math.min(requiredTrim, maximumTrim);
    }

    private double boundedOffset(double requestedOffset, double tileWidth, double minimumOffset, double minimumEdgeWidth) {
        if (tileWidth <= 0.001) {
            return 0.0;
        }
        double lowerBound = Math.max(minimumOffset, minimumEdgeWidth);
        if (lowerBound <= 0.001) {
            return Math.max(0.0, Math.min(requestedOffset, tileWidth));
        }
        double upperBound = tileWidth - lowerBound;
        if (upperBound < lowerBound) {
            return lowerBound;
        }
        return Math.max(lowerBound, Math.min(requestedOffset, upperBound));
    }
}
