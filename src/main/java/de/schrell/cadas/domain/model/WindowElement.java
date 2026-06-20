package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;

import java.util.Objects;
import java.util.UUID;

public record WindowElement(
        UUID id,
        UUID wallId,
        Length offsetFromStart,
        Length width,
        Length sillHeight,
        Length windowHeight
) {

    public WindowElement {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(wallId, "wallId darf nicht null sein.");
        Objects.requireNonNull(offsetFromStart, "offsetFromStart darf nicht null sein.");
        Objects.requireNonNull(width, "width darf nicht null sein.");
        Objects.requireNonNull(sillHeight, "sillHeight darf nicht null sein.");
        Objects.requireNonNull(windowHeight, "windowHeight darf nicht null sein.");
    }

    public static WindowElement create(
            UUID wallId,
            Length offsetFromStart,
            Length width,
            Length sillHeight,
            Length windowHeight
    ) {
        return new WindowElement(UUID.randomUUID(), wallId, offsetFromStart, width, sillHeight, windowHeight);
    }

    public WindowElement withOffset(Length offset) {
        return new WindowElement(id, wallId, offset, width, sillHeight, windowHeight);
    }
}
