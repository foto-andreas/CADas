package de.schrell.cadas.application.layers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.floor.FloorOpeningGeometryService;
import de.schrell.cadas.application.room.OrthogonalPolygonDecompositionService.CellRectangle;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.infrastructure.dxf.DxfProjectExchangeService;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class SurfaceRectangleTileLayoutServiceTest {

    private final SurfaceRectangleTileLayoutService service = new SurfaceRectangleTileLayoutService();
    private final FloorOpeningGeometryService floorOpeningGeometryService = new FloorOpeningGeometryService();

    @Test
    void beginntInTeilrechteckenMitVollerStartplatte() {
        SurfaceLayer layer = SurfaceLayer.create(
                "Vario 60x100",
                Length.ofMillimeters(18),
                Length.ofMillimeters(1_000),
                Length.ofMillimeters(600),
                SurfaceLayoutMode.NONE,
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.ofMillimeters(2),
                "Test"
        );

        List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> tiles = service.tilesForRectangles(
                List.of(
                        new CellRectangle(0.0, 3_000.0, 0.0, 1_000.0),
                        new CellRectangle(3_000.0, 4_000.0, 1_000.0, 3_000.0)
                ),
                layer
        );

        assertTrue(tiles.stream().anyMatch(tile ->
                        Math.abs(tile.x() - 3_000.0) < 0.001
                                && Math.abs(tile.y() - 1_000.0) < 0.001
                                && Math.abs(tile.width() - 1_000.0) < 0.001),
                "Das schmale Rechteck muss mit einer vollen Platte beginnen.");
    }

    @Test
    void richtetDenDachgeschossFlurAusKirepAmSchmalenFlurarmAus() throws Exception {
        Level level = new DxfProjectExchangeService()
                .importProject(Path.of("KIREP.cadas"), "KIREP")
                .levels().stream()
                .filter(candidate -> candidate.name().equals("Dachgeschoss"))
                .findFirst()
                .orElseThrow();
        Room flur = level.rooms().stream()
                .filter(room -> room.name().equals("Flur"))
                .findFirst()
                .orElseThrow();
        SurfaceLayerStack stack = level.findSurfaceLayerStack(SurfaceType.FLOOR, flur.id().toString());
        SurfaceLayer layer = stack.layers().getFirst();

        List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> tiles = service.tilesForRectangles(
                floorOpeningGeometryService.floorRectangles(level, flur),
                layer
        );

        assertTrue(tiles.stream().anyMatch(tile ->
                        Math.abs(tile.x() + 80.0) < 0.01
                                && Math.abs(tile.width() - 1_000.0) < 0.01),
                "Der schmale Flurarm soll bei 90°-Verlegung mit einer vollen 1000-mm-Platte starten.");
    }
}
