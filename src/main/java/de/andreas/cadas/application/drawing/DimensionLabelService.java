package de.andreas.cadas.application.drawing;

import de.andreas.cadas.domain.geometry.Length;

import java.util.Locale;

/**
 * Zentraler Aufbau der Maßtexte für 2D-Ansicht und Bauzeichnung-PDF.
 *
 * <p>Je nach {@link DimensionLabelOptions} wird entweder der vollständige Text
 * ("Küche: Raummaß 0,90 m" / "Außenmaß 4,20 m") oder nur die nackte Länge ("0,90 m")
 * geliefert. So bauen 2D und PDF die Maßtexte identisch auf.</p>
 */
public final class DimensionLabelService {

    /**
     * Baut den Maßtext für eine Raum- oder Außenmaßangabe.
     *
     * @param dimension die fachliche Seitendimension (enthält Raumname und Länge)
     * @param exterior  {@code true} für das Außenmaß, {@code false} für ein Raummaß
     * @param options   die aktiven Texteinstellungen
     * @return der formatierte Maßtext
     */
    public String label(WallDimensionService.SideDimension dimension, boolean exterior, DimensionLabelOptions options) {
        return label(dimension.name(), dimension.length(), exterior, options);
    }

    public String deduplicationKey(WallDimensionService.SideDimension dimension, boolean exterior) {
        if (exterior) {
            return "";
        }
        double deltaX = dimension.dimensionSegment().end().xMillimeters()
                - dimension.dimensionSegment().start().xMillimeters();
        double deltaY = dimension.dimensionSegment().end().yMillimeters()
                - dimension.dimensionSegment().start().yMillimeters();
        double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));
        double normalizedAngle = (angle % 180.0 + 180.0) % 180.0;
        long roundedLength = Math.round(dimension.length().toMillimeters() * 1_000.0);
        long roundedAngle = Math.round(normalizedAngle * 1_000.0);
        return dimension.sourceKey() + "|" + roundedLength + "|" + roundedAngle;
    }

    /**
     * Baut den Maßtext für eine beliebige Längenangabe (z. B. Achsmaß).
     *
     * @param name     Anzeigename (z. B. Raumname oder "Achsmaß")
     * @param length   die zu beschriftende Länge
     * @param exterior {@code true} für ein Außenmaß, {@code false} für ein Raummaß
     * @param options  die aktiven Texteinstellungen
     * @return der formatierte Maßtext
     */
    public String label(String name, Length length, boolean exterior, DimensionLabelOptions options) {
        if (!options.showsFullText()) {
            return formatLength(length);
        }
        if (exterior) {
            return "Außenmaß " + formatLength(length);
        }
        if (name == null || name.isBlank()) {
            return formatLength(length);
        }
        return name + ": Raummaß " + formatLength(length);
    }

    private String formatLength(Length length) {
        return String.format(Locale.GERMAN, "%.2f m", length.toMillimeters() / 1_000.0);
    }

    /**
     * Formatierhilfe für Aufrufer, die Längen außerhalb von SideDimension ausgeben
     * und immer dieselbe Formatierung nutzen wollen (z. B. Aufrisse im PDF).
     */
    public String formatMeters(double millimeters) {
        return String.format(Locale.GERMAN, "%.2f m", millimeters / 1_000.0);
    }
}
