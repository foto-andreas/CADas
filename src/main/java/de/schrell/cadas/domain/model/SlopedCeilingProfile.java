package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;

import java.util.Objects;

public record SlopedCeilingProfile(
        SlopedCeilingSide lowSide,
        Length kneeWallHeight
) {

    public SlopedCeilingProfile {
        Objects.requireNonNull(lowSide, "lowSide darf nicht null sein.");
        Objects.requireNonNull(kneeWallHeight, "kneeWallHeight darf nicht null sein.");
        if (kneeWallHeight.toMillimeters() < 0.0) {
            throw new IllegalArgumentException("Die Sockelhöhe der Dachschräge darf nicht negativ sein.");
        }
    }

    public SlopedCeilingProfile rotateClockwise() {
        return new SlopedCeilingProfile(lowSide.rotateClockwise(), kneeWallHeight);
    }

    public SlopedCeilingProfile rotateCounterClockwise() {
        return new SlopedCeilingProfile(lowSide.rotateCounterClockwise(), kneeWallHeight);
    }
}
