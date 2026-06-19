package de.andreas.cadas.application.drawing;

/**
 * Textstil für Maßangaben: entweder vollständiger Text mit Raum-/Außenmaß-Vorsatz
 * oder nur die nackte Längenangabe.
 */
public enum DimensionTextStyle {
    /** Vollständiger Text: "Raumname: Raummaß 4,00 m" bzw. "Außenmaß 4,20 m". */
    FULL,

    /** Nur die Länge: "4,00 m". */
    LENGTH_ONLY
}