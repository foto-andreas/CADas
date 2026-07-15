package de.schrell.cadas.application.view;

/**
 * Allgemeine triangulierte 3D-Fläche mit Weltkoordinaten, Material und optionalem Auswahlbezug.
 */
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
