package de.schrell.cadas.application.view;

/** Achsenparalleler oder gedrehter Quader der 3D-Szene mit Material- und Auswahlbezug. */
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
