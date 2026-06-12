package de.andreas.cadas.application.view;

public record RenderableMesh(
        SelectionKey selectionKey,
        String levelName,
        RenderableKind kind,
        float[] points,
        int faceCount,
        double baseY,
        double height,
        String materialKey,
        double opacity
) {
}
