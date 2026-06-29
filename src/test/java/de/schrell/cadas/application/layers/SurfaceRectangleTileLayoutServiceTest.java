package de.schrell.cadas.application.layers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.floor.FloorOpeningGeometryService;
import de.schrell.cadas.application.room.OrthogonalPolygonDecompositionService.CellRectangle;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.infrastructure.dxf.DxfProjectExchangeService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    void hältDasRasterBeiRechteckigenTeilungenDurchgehend() {
        SurfaceLayer layer = SurfaceLayer.create(
                "Dämmplatte 60x120",
                Length.ofMillimeters(18),
                Length.ofMillimeters(1_200),
                Length.ofMillimeters(600),
                SurfaceLayoutMode.FIXED,
                Length.ofMillimeters(200),
                Length.ofMillimeters(100),
                Length.ofMillimeters(100),
                Length.ofMillimeters(100),
                Length.ofMillimeters(2),
                "Test"
        );

        List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> full = service.tilesForRectangles(
                List.of(new CellRectangle(0.0, 4_000.0, 0.0, 3_000.0)),
                layer
        );
        List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> split = service.tilesForRectangles(
                List.of(
                        new CellRectangle(0.0, 1_900.0, 0.0, 3_000.0),
                        new CellRectangle(1_900.0, 4_000.0, 0.0, 3_000.0)
                ),
                layer
        );

        assertTrue(full.equals(split), "Eine reine Teilung des Raums darf das Belagsraster nicht neu starten.");
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

    @Test
    void hältDenFixenVersatzInGedrehterKi1SchlafzimmerbelegungInJederZweitenReihe() throws Exception {
        Level level = new DxfProjectExchangeService()
                .importProject(Path.of("KI1.cadas"), "KI1")
                .levels().stream()
                .filter(candidate -> candidate.name().equals("Erdgeschoss"))
                .findFirst()
                .orElseThrow();
        Room schlafzimmer = level.rooms().stream()
                .filter(room -> room.name().equals("Schlafzimmer"))
                .findFirst()
                .orElseThrow();
        SurfaceLayer layer = level.findSurfaceLayerStack(SurfaceType.FLOOR, schlafzimmer.id().toString()).layers().getFirst();

        assertTrue(layer.layoutRotatedQuarterTurn());
        assertEquals(SurfaceLayoutMode.FIXED, layer.layoutMode());
        assertEquals(300.0, layer.layoutOffset().toMillimeters(), 0.001);

        List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> tiles = service.tilesForRectangles(
                floorOpeningGeometryService.floorRectangles(level, schlafzimmer),
                layer
        );

        double ersteReihe = ersteReihenbreite(tiles, 0);
        double zweiteReihe = ersteReihenbreite(tiles, 1);
        double dritteReihe = ersteReihenbreite(tiles, 2);
        assertEquals(594.088, ersteReihe, 0.02);
        assertEquals(414.088, zweiteReihe, 0.02);
        assertEquals(594.088, dritteReihe, 0.02);
    }

    @Test
    void vermeidetInnenschnitteBeiNichtRotierbaremAussenschnittAnInnenverspruengen() {
        SurfaceLayer layer = SurfaceLayer.create(
                "Variotherm",
                Length.ofMillimeters(18),
                Length.ofMillimeters(600),
                Length.ofMillimeters(1_000),
                SurfaceLayoutMode.FIXED,
                Length.ofMillimeters(200),
                Length.ofMillimeters(100),
                Length.ofMillimeters(100),
                Length.ofMillimeters(100),
                Length.zero(),
                SurfaceCutRestriction.LAY_DIRECTION_OUTER_CUTS,
                "Test"
        );
        List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> tiles = service.tilesForRectangles(
                List.of(
                        new CellRectangle(1540.716, 4409.000, -6867.513, -3880.802),
                        new CellRectangle(1548.734, 4409.000, -3880.802, -3316.802),
                        new CellRectangle(301.000, 4409.000, -3316.802, -418.664)
                ),
                layer
        );

        Map<String, Long> tilesProGrundplatte = tiles.stream()
                .collect(Collectors.groupingBy(
                        tile -> tile.row() + ":" + tile.column(),
                        Collectors.counting()
                ));
        assertTrue(tilesProGrundplatte.values().stream().allMatch(anzahl -> anzahl == 1L),
                "Bei Außenschnitt-Regeln darf keine Grundplatte in mehrere, nicht zusammenhängende Teilstücke zerfallen.");
    }

    private double ersteReihenbreite(List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> tiles, int row) {
        return tiles.stream()
                .filter(tile -> tile.row() == row)
                .sorted(java.util.Comparator.comparingDouble(SurfaceRectangleTileLayoutService.PlacedSurfaceTile::x))
                .findFirst()
                .orElseThrow()
                .width();
    }
}
