package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TileLayoutService {

    private static final int CACHE_SIZE = 128;
    private final Map<TileLayoutRequest, List<TilePlacement>> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<TileLayoutRequest, List<TilePlacement>> eldest) {
                    return size() > CACHE_SIZE;
                }
            }
    );

    public List<TilePlacement> fillSurface(TileLayoutRequest request) {
        List<TilePlacement> cached = cache.get(request);
        if (cached != null) {
            return cached;
        }
        List<TilePlacement> placements = new ArrayList<>();
        double surfaceWidth = request.surfaceWidth().toMillimeters();
        double surfaceHeight = request.surfaceHeight().toMillimeters();
        double tileWidth = request.tileWidth().toMillimeters();
        double tileHeight = request.tileHeight().toMillimeters();
        double minimumOffset = request.minimumOffset().toMillimeters();
        double minimumEdgeWidth = request.minimumEdgeWidth().toMillimeters();
        double minimumStartEndMargin = request.minimumStartEndMargin().toMillimeters();
        double layoutOffset = request.layoutOffset().toMillimeters();
        double leftMargin = request.freeMargins().left().toMillimeters();
        double rightMargin = request.freeMargins().right().toMillimeters();
        double topMargin = request.freeMargins().top().toMillimeters();
        double bottomMargin = request.freeMargins().bottom().toMillimeters();
        double availableWidth = Math.max(0.0, surfaceWidth - leftMargin - rightMargin);
        double availableHeight = Math.max(0.0, surfaceHeight - bottomMargin - topMargin);
        if (availableWidth <= 0.001 || availableHeight <= 0.001 || tileWidth <= 0.001 || tileHeight <= 0.001) {
            return List.of();
        }
        double rowStartTrim = startTrim(request, availableHeight, tileHeight, minimumStartEndMargin);

        int row = 0;
        List<TilePlacement> localPlacements = new ArrayList<>();
        for (double y = -rowStartTrim; y < availableHeight - 0.001; y += tileHeight, row++) {
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
            double remainingHeight = Math.min(tileHeight - Math.max(0.0, -y), availableHeight - clippedY);
            if (remainingHeight <= 0.0) {
                continue;
            }
            int column = 0;
            for (double x = -rowOffset; x < availableWidth - 0.001; x += tileWidth, column++) {
                double clippedX = Math.max(0.0, x);
                double remainingWidth = Math.min(tileWidth - Math.max(0.0, -x), availableWidth - clippedX);
                if (remainingWidth <= 0.0 || remainingHeight <= 0.0) {
                    continue;
                }
                localPlacements.add(new TilePlacement(
                        column,
                        row,
                        Length.ofMillimeters(clippedX + leftMargin),
                        Length.ofMillimeters(clippedY + bottomMargin),
                        Length.ofMillimeters(remainingWidth),
                        Length.ofMillimeters(remainingHeight)
                ));
            }
        }
        SurfaceLayoutAnchor layoutAnchor = request.layoutAnchor();
        if (layoutAnchor == SurfaceLayoutAnchor.AUTO) {
            placements.addAll(localPlacements);
        } else {
            for (TilePlacement placement : localPlacements) {
                placements.add(transformPlacement(placement, leftMargin, bottomMargin, availableWidth, availableHeight, layoutAnchor));
            }
        }
        List<TilePlacement> result = List.copyOf(placements);
        cache.put(request, result);
        return result;
    }

    private TilePlacement transformPlacement(
            TilePlacement placement,
            double leftMargin,
            double bottomMargin,
            double availableWidth,
            double availableHeight,
            SurfaceLayoutAnchor layoutAnchor
    ) {
        double localX = placement.xOffset().toMillimeters() - leftMargin;
        double localY = placement.yOffset().toMillimeters() - bottomMargin;
        double width = placement.width().toMillimeters();
        double height = placement.height().toMillimeters();
        double transformedX = switch (layoutAnchor) {
            case AUTO, MIN_X_MIN_Y, MIN_X_MAX_Y -> localX;
            case MAX_X_MIN_Y, MAX_X_MAX_Y -> availableWidth - localX - width;
        };
        double transformedY = switch (layoutAnchor) {
            case AUTO, MIN_X_MIN_Y, MAX_X_MIN_Y -> localY;
            case MAX_X_MAX_Y, MIN_X_MAX_Y -> availableHeight - localY - height;
        };
        return new TilePlacement(
                placement.column(),
                placement.row(),
                Length.ofMillimeters(transformedX + leftMargin),
                Length.ofMillimeters(transformedY + bottomMargin),
                Length.ofMillimeters(width),
                Length.ofMillimeters(height)
        );
    }

    private double startTrim(
            TileLayoutRequest request,
            double surfaceHeight,
            double tileHeight,
            double minimumStartEndMargin
    ) {
        double explicitWidth = clamp(request.startRowWidth().toMillimeters(), 0.0, tileHeight);
        if (explicitWidth > 0.001) {
            return Math.max(0.0, tileHeight - explicitWidth);
        }
        double explicitTrim = clamp(request.startRowTrim().toMillimeters(), 0.0, tileHeight);
        if (explicitTrim > 0.001 || request.layoutAnchor() != SurfaceLayoutAnchor.AUTO) {
            return explicitTrim;
        }
        return boundedStartTrim(surfaceHeight, tileHeight, minimumStartEndMargin);
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

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
