package de.andreas.cadas.application.drawing;

import java.util.Objects;

/**
 * Steuerung des Textanteils von Maßangaben in 2D-Ansicht und Bauzeichnung-PDF.
 *
 * <p>{@link #FULL} zeigt den vollständigen Text mit Raumname, Raummaß- bzw. Außenmaß-Vorsatz;
 * {@link #LENGTH_ONLY} zeigt ausschließlich die Länge.</p>
 */
public record DimensionLabelOptions(DimensionTextStyle textStyle) {

    public DimensionLabelOptions {
        Objects.requireNonNull(textStyle, "textStyle darf nicht null sein.");
    }

    public static DimensionLabelOptions full() {
        return new DimensionLabelOptions(DimensionTextStyle.FULL);
    }

    public static DimensionLabelOptions lengthOnly() {
        return new DimensionLabelOptions(DimensionTextStyle.LENGTH_ONLY);
    }

    public boolean showsFullText() {
        return textStyle == DimensionTextStyle.FULL;
    }
}