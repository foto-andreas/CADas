package de.andreas.cadas.application.view;

public record RenderableBox(
        SelectionKey selectionKey,
        String levelName,
        RenderableKind kind,
        double centerX,
        double centerY,
        double centerZ,
        double width,
        double height,
        double depth,
        RotationAxis rotationAxis,
        double rotationDegrees,
        String materialKey,
        double opacity
) {
}

