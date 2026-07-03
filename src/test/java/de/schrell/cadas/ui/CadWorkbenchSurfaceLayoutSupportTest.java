package de.schrell.cadas.ui;

import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import de.schrell.cadas.domain.model.SurfaceLayoutDirection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CadWorkbenchSurfaceLayoutSupportTest {

    @Test
    void normalisiertAutomatischeStarteckeAufUntenLinks() {
        assertEquals(
                SurfaceLayoutAnchor.MIN_X_MIN_Y,
                CadWorkbenchSurfaceLayoutSupport.normalizedSurfaceLayoutAnchor(SurfaceLayoutAnchor.AUTO)
        );
        assertEquals(
                SurfaceLayoutAnchor.MAX_X_MAX_Y,
                CadWorkbenchSurfaceLayoutSupport.normalizedSurfaceLayoutAnchor(SurfaceLayoutAnchor.MAX_X_MAX_Y)
        );
    }

    @Test
    void berechnetBelagsrichtungAusStarteckeUndDrehung() {
        assertEquals(
                SurfaceLayoutDirection.RIGHT_TO_LEFT,
                CadWorkbenchSurfaceLayoutSupport.surfaceLayoutSelectionDirection(SurfaceLayoutAnchor.MAX_X_MIN_Y, false)
        );
        assertEquals(
                SurfaceLayoutDirection.LEFT_TO_RIGHT,
                CadWorkbenchSurfaceLayoutSupport.surfaceLayoutSelectionDirection(SurfaceLayoutAnchor.MAX_X_MIN_Y, true)
        );
    }

    @Test
    void berechnetDrehungAusStarteckeUndRichtung() {
        assertFalse(CadWorkbenchSurfaceLayoutSupport.surfaceLayoutRotatedQuarterTurn(
                SurfaceLayoutAnchor.MIN_X_MIN_Y,
                SurfaceLayoutDirection.LEFT_TO_RIGHT
        ));
        assertTrue(CadWorkbenchSurfaceLayoutSupport.surfaceLayoutRotatedQuarterTurn(
                SurfaceLayoutAnchor.MIN_X_MIN_Y,
                SurfaceLayoutDirection.RIGHT_TO_LEFT
        ));
    }

    @Test
    void formatiertStarteckeUndRichtung() {
        assertEquals("Oben rechts", CadWorkbenchSurfaceLayoutSupport.formatSurfaceLayoutCorner(SurfaceLayoutAnchor.MAX_X_MAX_Y));
        assertEquals(
                "Rechts nach links",
                CadWorkbenchSurfaceLayoutSupport.formatSurfaceLayoutDirection(SurfaceLayoutAnchor.MIN_X_MIN_Y, true)
        );
    }
}
