package de.andreas.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
