package de.andreas.cadas.application.view;

public record CameraPose(
        ProjectionMode projectionMode,
        double azimuthDegrees,
        double elevationDegrees,
        double distance
) {
}

