package de.schrell.cadas.application.view;

/** Vollständiger, darstellungsunabhängiger Kamerazustand aus Lage, Drehung, Entfernung und Projektion. */
public record CameraPose(
        ProjectionMode projectionMode,
        double azimuthDegrees,
        double elevationDegrees,
        double distance,
        double panX,
        double panY,
        double panZ
) {
}
