package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Angle;
import de.schrell.cadas.domain.geometry.Length;

import java.util.Objects;

public record Roof(
        RoofType roofType,
        Angle pitchAngle,
        Length overhang,
        boolean gutterEnabled
) {

    public Roof {
        Objects.requireNonNull(roofType, "roofType darf nicht null sein.");
        Objects.requireNonNull(pitchAngle, "pitchAngle darf nicht null sein.");
        Objects.requireNonNull(overhang, "overhang darf nicht null sein.");
    }
}
