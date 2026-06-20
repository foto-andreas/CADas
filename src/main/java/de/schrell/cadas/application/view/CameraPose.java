package de.schrell.cadas.application.view;

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
