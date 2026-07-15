package de.schrell.cadas.application.view;

/** Stabiler Schlüssel, der einen 3D-Körper auf Typ, Etage und ID seines Fachobjekts zurückführt. */
public record SelectionKey(
        RenderableKind kind,
        String levelName,
        String elementId
) {
}
