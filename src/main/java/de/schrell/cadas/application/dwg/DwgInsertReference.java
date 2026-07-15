package de.schrell.cadas.application.dwg;

/** Referenziert eine Blockeinfügung einschließlich Einfügepunkt, Skalierung und Drehung. */
public record DwgInsertReference(
        String blockName,
        double xMillimeters,
        double yMillimeters,
        double scaleX,
        double scaleY,
        double rotationDegrees
) {
}
