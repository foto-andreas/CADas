package de.schrell.cadas.application.view;

public record SelectionKey(
        RenderableKind kind,
        String levelName,
        String elementId
) {
}
