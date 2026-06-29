package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void bildetDrehungUndRichtungVerlustfreiAufDenLayoutAnkerAb() {
        SurfaceLayer layer = SurfaceLayer.create(
                "Platte",
                Length.of(18, LengthUnit.MILLIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(100, LengthUnit.CENTIMETER),
                Length.zero()
        ).withLayoutOrientation(SurfaceLayoutRotation.DEGREES_270, SurfaceLayoutDirection.RIGHT_TO_LEFT);

        assertTrue(layer.layoutRotatedQuarterTurn());
        assertEquals(SurfaceLayoutAnchor.MAX_X_MAX_Y, layer.layoutAnchor());
        assertEquals(SurfaceLayoutRotation.DEGREES_270, layer.layoutRotation());
        assertEquals(SurfaceLayoutDirection.RIGHT_TO_LEFT, layer.layoutDirection());
    }

    @Test
    void schaltetManuelleStarteckenImUhrzeigersinnWeiterUndZurueck() {
        assertEquals(SurfaceLayoutAnchor.MAX_X_MIN_Y, SurfaceLayoutAnchor.MIN_X_MIN_Y.nextManual());
        assertEquals(SurfaceLayoutAnchor.MAX_X_MAX_Y, SurfaceLayoutAnchor.MAX_X_MIN_Y.nextManual());
        assertEquals(SurfaceLayoutAnchor.MIN_X_MAX_Y, SurfaceLayoutAnchor.MAX_X_MAX_Y.nextManual());
        assertEquals(SurfaceLayoutAnchor.MIN_X_MIN_Y, SurfaceLayoutAnchor.MIN_X_MAX_Y.nextManual());

        assertEquals(SurfaceLayoutAnchor.MIN_X_MAX_Y, SurfaceLayoutAnchor.MIN_X_MIN_Y.previousManual());
        assertEquals(SurfaceLayoutAnchor.MAX_X_MAX_Y, SurfaceLayoutAnchor.MIN_X_MAX_Y.previousManual());
        assertEquals(SurfaceLayoutAnchor.MAX_X_MIN_Y, SurfaceLayoutAnchor.MAX_X_MAX_Y.previousManual());
        assertEquals(SurfaceLayoutAnchor.MIN_X_MIN_Y, SurfaceLayoutAnchor.MAX_X_MIN_Y.previousManual());
    }
}
