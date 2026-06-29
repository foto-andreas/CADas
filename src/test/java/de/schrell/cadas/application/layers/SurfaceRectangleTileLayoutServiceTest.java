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
import de.schrell.cadas.domain.model.SurfaceLayoutDirection;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceLayoutRotation;
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
    void beachtetDieGewaehlteStarteckeBeiRechteckigenBelaegen() {
        List<CellRectangle> rectangles = List.of(new CellRectangle(100.0, 950.0, 100.0, 1_100.0));
        SurfaceLayer leftToRight = SurfaceLayer.create(
                "Variotherm",
                Length.ofMillimeters(18),
                Length.ofMillimeters(600),
                Length.ofMillimeters(1_000),
                SurfaceLayoutMode.FIXED,
                Length.zero(),
                Length.zero(),
                Length.ofMillimeters(100),
                Length.ofMillimeters(100),
                Length.zero(),
                SurfaceCutRestriction.LAY_DIRECTION_OUTER_CUTS,
                "Test"
        ).withLayoutOrientation(SurfaceLayoutRotation.DEGREES_0, SurfaceLayoutDirection.LEFT_TO_RIGHT);
        SurfaceLayer rightToLeft = leftToRight.withLayoutOrientation(SurfaceLayoutRotation.DEGREES_0, SurfaceLayoutDirection.RIGHT_TO_LEFT);

        List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> leftTiles = service.tilesForRectangles(rectangles, leftToRight);
        List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> rightTiles = service.tilesForRectangles(rectangles, rightToLeft);

        assertTrue(leftTiles.stream().anyMatch(tile ->
                        Math.abs(tile.x() - 100.0) < 0.001
                                && Math.abs(tile.width() - 600.0) < 0.001),
                "Links-nach-rechts muss mit einer vollen Platte an Xmin beginnen.");
        assertTrue(leftTiles.stream().anyMatch(tile ->
                        Math.abs(tile.x() - 700.0) < 0.001
                                && Math.abs(tile.width() - 250.0) < 0.001),
                "Links-nach-rechts muss den Zuschnitt an Xmax lassen.");
        assertTrue(rightTiles.stream().anyMatch(tile ->
                        Math.abs(tile.x() - 100.0) < 0.001
                                && Math.abs(tile.width() - 250.0) < 0.001),
                "Rechts-nach-links muss den Zuschnitt an Xmin lassen.");
        assertTrue(rightTiles.stream().anyMatch(tile ->
                        Math.abs(tile.x() - 350.0) < 0.001
                                && Math.abs(tile.width() - 600.0) < 0.001),
                "Rechts-nach-links muss mit einer vollen Platte an Xmax beginnen.");
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
                        Math.abs(tile.x() + 80.0) <= 20.0
                                && Math.abs(tile.width() - 1_000.0) < 0.01),
                "Der schmale Flurarm soll bei 90°-Verlegung mit einer vollen 1000-mm-Platte starten.");
    }

    @Test
    void drehtDenFixenVersatzBeiNeunzigGradInDieNeueVerlegerichtungMit() {
        SurfaceLayer layer = SurfaceLayer.create(
                "Fliese",
                Length.ofMillimeters(10),
                Length.ofMillimeters(600),
                Length.ofMillimeters(1_000),
                SurfaceLayoutMode.FIXED,
                Length.ofMillimeters(300),
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.zero(),
                "Test"
        ).withLayoutRotatedQuarterTurn(true)
                .withLayoutAnchor(SurfaceLayoutAnchor.MIN_X_MIN_Y);

        List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> tiles = service.tilesForRectangles(
                List.of(new CellRectangle(0.0, 2_500.0, 0.0, 2_300.0)),
                layer
        );

        assertEquals(600.0, ersteSpaltenhoehe(tiles, 0), 0.001);
        assertEquals(300.0, ersteSpaltenhoehe(tiles, 1), 0.001);
        assertEquals(600.0, ersteSpaltenhoehe(tiles, 2), 0.001);
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

    private double ersteSpaltenhoehe(List<SurfaceRectangleTileLayoutService.PlacedSurfaceTile> tiles, int columnIndex) {
        List<Double> startspalten = tiles.stream()
                .map(SurfaceRectangleTileLayoutService.PlacedSurfaceTile::x)
                .distinct()
                .sorted()
                .toList();
        double startX = startspalten.get(columnIndex);
        return tiles.stream()
                .filter(tile -> Math.abs(tile.x() - startX) < 0.001)
                .sorted(java.util.Comparator.comparingDouble(SurfaceRectangleTileLayoutService.PlacedSurfaceTile::y))
                .findFirst()
                .orElseThrow()
                .height();
    }
}
