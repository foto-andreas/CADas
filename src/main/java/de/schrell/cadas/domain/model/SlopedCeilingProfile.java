package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;

import java.util.Objects;

public record SlopedCeilingProfile(
        SlopedCeilingSide lowSide,
        Length kneeWallHeight,
        Length horizontalRun
) {

    public SlopedCeilingProfile {
        Objects.requireNonNull(lowSide, "lowSide darf nicht null sein.");
        Objects.requireNonNull(kneeWallHeight, "kneeWallHeight darf nicht null sein.");
        Objects.requireNonNull(horizontalRun, "horizontalRun darf nicht null sein.");
        if (kneeWallHeight.toMillimeters() < 0.0) {
            throw new IllegalArgumentException("Die Sockelhöhe der Dachschräge darf nicht negativ sein.");
        }
        if (horizontalRun.toMillimeters() < 0.0) {
            throw new IllegalArgumentException("Die Breite unterhalb der Dachschräge darf nicht negativ sein.");
        }
    }

    public SlopedCeilingProfile(SlopedCeilingSide lowSide, Length kneeWallHeight) {
        this(lowSide, kneeWallHeight, Length.zero());
    }

    public SlopedCeilingProfile rotateClockwise() {
        return new SlopedCeilingProfile(lowSide.rotateClockwise(), kneeWallHeight, horizontalRun);
    }

    public SlopedCeilingProfile rotateCounterClockwise() {
        return new SlopedCeilingProfile(lowSide.rotateCounterClockwise(), kneeWallHeight, horizontalRun);
    }
}
