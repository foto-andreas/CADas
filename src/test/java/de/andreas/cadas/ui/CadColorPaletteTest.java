package de.andreas.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class CadColorPaletteTest {

    @Test
    void wandfarbeHatAusreichendenKontrastZurMassschrift() {
        double contrast = contrastRatio(CadColorPalette.WALL, CadColorPalette.DIMENSION_TEXT);

        assertTrue(contrast >= 4.0, "Kontrast war nur " + contrast);
    }

    private double contrastRatio(Color first, Color second) {
        double lighter = Math.max(luminance(first), luminance(second));
        double darker = Math.min(luminance(first), luminance(second));
        return (lighter + 0.05) / (darker + 0.05);
    }

    private double luminance(Color color) {
        return 0.2126 * linear(color.getRed())
                + 0.7152 * linear(color.getGreen())
                + 0.0722 * linear(color.getBlue());
    }

    private double linear(double channel) {
        return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
