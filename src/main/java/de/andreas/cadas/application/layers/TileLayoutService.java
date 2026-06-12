package de.andreas.cadas.application.layers;

import de.andreas.cadas.domain.geometry.Length;

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
        double layoutOffset = request.layoutOffset().toMillimeters();

        int row = 0;
        for (double y = 0.0; y < surfaceHeight - 0.001; y += tileHeight, row++) {
            double rowOffset = switch (request.layoutMode()) {
                case NONE -> 0.0;
                case FIXED -> boundedOffset(
                        ((row + 1) * layoutOffset) % tileWidth,
                        tileWidth, minimumOffset, minimumEdgeWidth);
                case AUTOMATIC -> boundedOffset(
                        row % 2 == 0 ? 0.0 : tileWidth / 2.0,
                        tileWidth, minimumOffset, minimumEdgeWidth);
            };
            int column = 0;
            for (double x = -rowOffset; x < surfaceWidth - 0.001; x += tileWidth, column++) {
                double clippedX = Math.max(0.0, x);
                double remainingWidth = Math.min(tileWidth - Math.max(0.0, -x), surfaceWidth - clippedX);
                double remainingHeight = Math.min(tileHeight, surfaceHeight - y);
                if (remainingWidth <= 0.0 || remainingHeight <= 0.0) {
                    continue;
                }
                placements.add(new TilePlacement(
                        column,
                        row,
                        Length.ofMillimeters(clippedX),
                        Length.ofMillimeters(y),
                        Length.ofMillimeters(remainingWidth),
                        Length.ofMillimeters(remainingHeight)
                ));
            }
        }
        return placements;
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
