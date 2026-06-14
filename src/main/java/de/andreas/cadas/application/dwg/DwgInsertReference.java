package de.andreas.cadas.application.dwg;

public record DwgInsertReference(
        String blockName,
        double xMillimeters,
        double yMillimeters,
        double scaleX,
        double scaleY,
        double rotationDegrees
) {
}
