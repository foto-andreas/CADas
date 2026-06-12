package de.andreas.cadas.application.layers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.model.SurfaceLayoutMode;

import java.util.List;

import org.junit.jupiter.api.Test;

class TileLayoutServiceTest {

    private final TileLayoutService tileLayoutService = new TileLayoutService();

    @Test
    void fuelltEineRechteckigeFlaecheMitVersatzreihen() {
        List<TilePlacement> placements = tileLayoutService.fillSurface(new TileLayoutRequest(
                Length.of(3, LengthUnit.METER),
                Length.of(2, LengthUnit.METER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(30, LengthUnit.CENTIMETER),
                Length.of(10, LengthUnit.CENTIMETER)
        ));

        assertFalse(placements.isEmpty());
        assertTrue(placements.stream().anyMatch(tile -> tile.row() == 1 && tile.xOffset().toMillimeters() > 0.0));
    }

    @Test
    void fixerVersatzErhoehtSichJeReiheModuloTileBreite() {
        double tileWidthMm = 600.0;
        List<TilePlacement> placements = tileLayoutService.fillSurface(new TileLayoutRequest(
                Length.of(3, LengthUnit.METER),
                Length.of(2, LengthUnit.METER),
                Length.ofMillimeters(tileWidthMm),
                Length.of(30, LengthUnit.CENTIMETER),
                SurfaceLayoutMode.FIXED,
                Length.of(10, LengthUnit.CENTIMETER),
                Length.zero(),
                Length.zero()
        ));

        assertFalse(placements.isEmpty());
        TilePlacement firstRow0 = placements.stream().filter(t -> t.row() == 0).findFirst().orElseThrow();
        TilePlacement firstRow1 = placements.stream().filter(t -> t.row() == 1).findFirst().orElseThrow();
        TilePlacement firstRow2 = placements.stream().filter(t -> t.row() == 2).findFirst().orElseThrow();
        double offsetRow0 = tileWidthMm - firstRow0.width().toMillimeters();
        double offsetRow1 = tileWidthMm - firstRow1.width().toMillimeters();
        double offsetRow2 = tileWidthMm - firstRow2.width().toMillimeters();
        assertEquals(100.0, offsetRow0, 0.01);
        assertEquals(200.0, offsetRow1, 0.01);
        assertEquals(300.0, offsetRow2, 0.01);
        assertTrue(offsetRow1 > offsetRow0);
        assertTrue(offsetRow2 > offsetRow1);
    }

    @Test
    void fixerVersatzModuloTileBreiteWickeltDurch() {
        double tileWidthMm = 600.0;
        List<TilePlacement> placements = tileLayoutService.fillSurface(new TileLayoutRequest(
                Length.of(3, LengthUnit.METER),
                Length.of(2, LengthUnit.METER),
                Length.ofMillimeters(tileWidthMm),
                Length.of(30, LengthUnit.CENTIMETER),
                SurfaceLayoutMode.FIXED,
                Length.of(50, LengthUnit.CENTIMETER),
                Length.zero(),
                Length.zero()
        ));

        assertFalse(placements.isEmpty());
        TilePlacement firstRow0 = placements.stream().filter(t -> t.row() == 0).findFirst().orElseThrow();
        TilePlacement firstRow1 = placements.stream().filter(t -> t.row() == 1).findFirst().orElseThrow();
        double offsetRow0 = tileWidthMm - firstRow0.width().toMillimeters();
        double offsetRow1 = tileWidthMm - firstRow1.width().toMillimeters();
        assertEquals(500.0, offsetRow0, 0.01);
        assertEquals(400.0, offsetRow1, 0.01);
    }

    @Test
    void minimumOffsetBegrenztAutoVersatzNachUnten() {
        double tileWidthMm = 600.0;
        double minimumOffsetMm = 400.0;
        List<TilePlacement> placements = tileLayoutService.fillSurface(new TileLayoutRequest(
                Length.of(3, LengthUnit.METER),
                Length.of(2, LengthUnit.METER),
                Length.ofMillimeters(tileWidthMm),
                Length.of(30, LengthUnit.CENTIMETER),
                SurfaceLayoutMode.AUTOMATIC,
                Length.zero(),
                Length.ofMillimeters(minimumOffsetMm),
                Length.zero()
        ));

        assertFalse(placements.isEmpty());
        TilePlacement firstRow1 = placements.stream().filter(t -> t.row() == 1).findFirst().orElseThrow();
        double ersteBreite = firstRow1.width().toMillimeters();
        double effektiverVersatz = tileWidthMm - ersteBreite;
        assertTrue(effektiverVersatz >= minimumOffsetMm,
                "Effektiver Versatz (" + effektiverVersatz + " mm) sollte >= minimumOffset (" + minimumOffsetMm + " mm) sein");
    }

    @Test
    void minimumEdgeWidthBegrenztVersatzNachObenUndUnten() {
        double tileWidthMm = 600.0;
        double minimumEdgeWidth = 100.0;
        List<TilePlacement> placements = tileLayoutService.fillSurface(new TileLayoutRequest(
                Length.of(3, LengthUnit.METER),
                Length.of(2, LengthUnit.METER),
                Length.ofMillimeters(tileWidthMm),
                Length.of(30, LengthUnit.CENTIMETER),
                SurfaceLayoutMode.AUTOMATIC,
                Length.zero(),
                Length.zero(),
                Length.ofMillimeters(minimumEdgeWidth)
        ));

        assertFalse(placements.isEmpty());
        for (TilePlacement tile : placements) {
            double rightEdgeX = tile.xOffset().toMillimeters() + tile.width().toMillimeters();
            assertTrue(rightEdgeX <= 3000.0 + 0.01);
        }
    }

    @Test
    void fixerVersatzRespektiertMinimumEdgeWidth() {
        double tileWidthMm = 600.0;
        double layoutOffsetMm = 50.0;
        double minimumEdgeWidth = 100.0;
        List<TilePlacement> placements = tileLayoutService.fillSurface(new TileLayoutRequest(
                Length.of(3, LengthUnit.METER),
                Length.of(2, LengthUnit.METER),
                Length.ofMillimeters(tileWidthMm),
                Length.of(30, LengthUnit.CENTIMETER),
                SurfaceLayoutMode.FIXED,
                Length.ofMillimeters(layoutOffsetMm),
                Length.zero(),
                Length.ofMillimeters(minimumEdgeWidth)
        ));

        assertFalse(placements.isEmpty());
        TilePlacement firstRow0 = placements.stream().filter(t -> t.row() == 0).findFirst().orElseThrow();
        double offsetRow0 = tileWidthMm - firstRow0.width().toMillimeters();
        assertTrue(offsetRow0 >= minimumEdgeWidth,
                "Effektiver Versatz (" + offsetRow0 + " mm) sollte >= minimumEdgeWidth (" + minimumEdgeWidth + " mm) sein");
    }
}
