package de.schrell.cadas.application.layers;

import de.schrell.cadas.application.room.OrthogonalPolygonDecompositionService.CellRectangle;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.SurfaceLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Legt Oberflächenbeläge rechteckweise aus, damit L- und U-förmige Räume
 * an ihren tatsächlichen Teilflächen und nicht an der gesamten Bounding-Box ausgerichtet werden.
 */
public final class SurfaceRectangleTileLayoutService {

    private final TileLayoutService tileLayoutService = new TileLayoutService();

    /**
     * Erzeugt absolute Belagszuschnitte für alle übergebenen Teilrechtecke.
     */
    public List<PlacedSurfaceTile> tilesForRectangles(List<CellRectangle> rectangles, SurfaceLayer layer) {
        List<PlacedSurfaceTile> tiles = new ArrayList<>();
        for (CellRectangle rectangle : rectangles) {
            TileLayoutRequest request = new TileLayoutRequest(
                    Length.ofMillimeters(rectangle.width()),
                    Length.ofMillimeters(rectangle.height()),
                    layer.effectiveTileWidth(),
                    layer.effectiveTileHeight(),
                    layer.layoutMode(),
                    layer.layoutOffset(),
                    layer.minimumOffset(),
                    layer.minimumEdgeWidth(),
                    layer.minimumStartEndMargin()
            );
            for (TilePlacement tile : tileLayoutService.fillSurface(request)) {
                tiles.add(new PlacedSurfaceTile(
                        rectangle.minX() + tile.xOffset().toMillimeters(),
                        rectangle.minY() + tile.yOffset().toMillimeters(),
                        tile.width().toMillimeters(),
                        tile.height().toMillimeters()
                ));
            }
        }
        return List.copyOf(tiles);
    }

    public record PlacedSurfaceTile(
            double x,
            double y,
            double width,
            double height
    ) {
    }
}
