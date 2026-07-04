package de.schrell.cadas.application.reports;

import de.schrell.cadas.application.drawing.DimensionTextStyle;

import java.util.Objects;

/**
 * Übertragbare Anzeige-Einstellungen für den Bauzeichnungs-PDF-Export.
 *
 * <p>Spiegelt die Workbench-Einstellungen wider, damit der PDF-Export dieselbe
 * Darstellung wie die 2D-Ansicht erzeugt.</p>
 *
 * @param dimensionTextStyle Textstil der Maßangaben (vollständig oder nur Länge)
 * @param showDimensions     {@code true} wenn ISO-Bemaßung gezeichnet werden soll
 * @param showAreaVolume     {@code true} wenn Raumtexte (Name, Fläche, Volumen) gezeichnet werden sollen
 */
public record ConstructionDrawingOptions(
        DimensionTextStyle dimensionTextStyle,
        boolean showDimensions,
        boolean showAreaVolume
) {

    public ConstructionDrawingOptions {
        Objects.requireNonNull(dimensionTextStyle, "dimensionTextStyle darf nicht null sein.");
    }

    public static ConstructionDrawingOptions defaults() {
        return new ConstructionDrawingOptions(DimensionTextStyle.FULL, true, true);
    }
}
