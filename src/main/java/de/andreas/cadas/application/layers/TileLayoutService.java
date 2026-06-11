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

        int row = 0;
        for (double y = 0.0; y < surfaceHeight - 0.001; y += tileHeight, row++) {
            double rowOffset = switch (request.layoutMode()) {
                case NONE -> 0.0;
                case FIXED -> boundedOffset(request.layoutOffset().toMillimeters(), tileWidth, minimumEdgeWidth);
                case AUTOMATIC -> row % 2 == 0 ? 0.0 : boundedOffset(Math.max(minimumOffset, tileWidth / 2.0), tileWidth, minimumEdgeWidth);
            };
            int column = 0;
            for (double x = -rowOffset; x < surfaceWidth - 0.001; x += tileWidth, column++) {
                double clippedX = Math.max(0.0, x);
                double remainingWidth = Math.min(tileWidth - Math.max(0.0, -x), surfaceWidth - clippedX);
                double remainingHeight = Math.min(tileHeight, surfaceHeight - y);
                if (remainingWidth <= 0.0 || remainingHeight <= 0.0 || remainingWidth < Math.min(minimumEdgeWidth, tileWidth)) {
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

    private double boundedOffset(double requestedOffset, double tileWidth, double minimumEdgeWidth) {
        if (tileWidth <= 0.001) {
            return 0.0;
        }
        double bounded = Math.max(0.0, Math.min(requestedOffset, tileWidth));
        if (minimumEdgeWidth <= 0.001 || bounded <= 0.001) {
            return bounded;
        }
        double maximumOffset = Math.max(0.0, tileWidth - minimumEdgeWidth);
        return Math.max(minimumEdgeWidth, Math.min(bounded, maximumOffset));
    }
}
