package de.andreas.cadas.application.layers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;

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
}

