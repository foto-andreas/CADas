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

        int row = 0;
        for (double y = 0.0; y < surfaceHeight - 0.001; y += tileHeight, row++) {
            double rowOffset = row % 2 == 0 ? 0.0 : Math.min(tileWidth / 2.0, Math.max(minimumOffset, tileWidth / 2.0));
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
}

