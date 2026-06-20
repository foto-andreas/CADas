package de.schrell.cadas.application.reports;

import de.schrell.cadas.application.drawing.DimensionLabelOptions;

import java.util.Objects;

/**
 * Übertragbare Anzeige-Einstellungen für den Bauzeichnungs-PDF-Export.
 *
 * <p>Spiegelt die Workbench-Einstellungen wider, damit der PDF-Export dieselbe
 * Darstellung wie die 2D-Ansicht erzeugt.</p>
 *
 * @param dimensionLabelOptions Textstil der Maßangaben (vollständig oder nur Länge)
 * @param showDimensions         {@code true} wenn ISO-Bemaßung gezeichnet werden soll
 * @param showAreaVolume         {@code true} wenn Raumtexte (Name, Fläche, Volumen) gezeichnet werden sollen
 */
public record ConstructionDrawingOptions(
        DimensionLabelOptions dimensionLabelOptions,
        boolean showDimensions,
        boolean showAreaVolume
) {

    public ConstructionDrawingOptions {
        Objects.requireNonNull(dimensionLabelOptions, "dimensionLabelOptions darf nicht null sein.");
    }

    public static ConstructionDrawingOptions defaults() {
        return new ConstructionDrawingOptions(DimensionLabelOptions.full(), true, true);
    }
}
