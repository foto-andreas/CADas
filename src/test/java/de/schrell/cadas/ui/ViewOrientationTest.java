package de.schrell.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ViewOrientationTest {

    @Test
    void liefertFuerJedeAnsichtEineAussagekraeftigeOverlayBeschreibung() {
        for (ViewOrientation orientation : ViewOrientation.values()) {
            assertFalse(orientation.label().isBlank());
            assertFalse(orientation.buttonLabel().isBlank());
            assertFalse(orientation.overlayDescription().isBlank());
            assertTrue(orientation.overlayDescription().contains("orthogonaler Projektion"));
        }
    }

    @Test
    void drehtAnsichtenWieDieWorkbenchButtons() {
        assertEquals(ViewOrientation.WEST, ViewOrientation.TOP.rotateLeft());
        assertEquals(ViewOrientation.EAST, ViewOrientation.TOP.rotateRight());
        assertEquals(ViewOrientation.NORTH, ViewOrientation.TOP.rotateUp());
        assertEquals(ViewOrientation.SOUTH, ViewOrientation.TOP.rotateDown());
        assertEquals(ViewOrientation.SOUTH, ViewOrientation.WEST.rotateLeft());
        assertEquals(ViewOrientation.NORTH, ViewOrientation.WEST.rotateRight());
    }
}
