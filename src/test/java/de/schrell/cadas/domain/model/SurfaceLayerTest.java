package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SurfaceLayerTest {

    @Test
    void tauschtEffektiveKachelmaßeBeiGedrehterVerlegerichtung() {
        SurfaceLayer layer = SurfaceLayer.create(
                "Platte",
                Length.of(18, LengthUnit.MILLIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(100, LengthUnit.CENTIMETER),
                Length.zero()
        ).withLayoutRotatedQuarterTurn(true);

        assertEquals(1_000.0, layer.effectiveTileWidth().toMillimeters(), 0.001);
        assertEquals(600.0, layer.effectiveTileHeight().toMillimeters(), 0.001);
    }
}
