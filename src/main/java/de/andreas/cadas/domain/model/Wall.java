package de.andreas.cadas.domain.model;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanSegment;

import java.util.Objects;
import java.util.UUID;

public record Wall(UUID id, PlanSegment axis, Length thickness, Length height, Length startHeight, Length endHeight) {

    public Wall {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(axis, "axis darf nicht null sein.");
        Objects.requireNonNull(thickness, "thickness darf nicht null sein.");
        Objects.requireNonNull(height, "height darf nicht null sein.");
        Objects.requireNonNull(startHeight, "startHeight darf nicht null sein.");
        Objects.requireNonNull(endHeight, "endHeight darf nicht null sein.");
    }

    public Wall(UUID id, PlanSegment axis, Length thickness, Length height) {
        this(id, axis, thickness, height, height, height);
    }

    public static Wall create(PlanSegment axis, Length thickness, Length height) {
        return new Wall(UUID.randomUUID(), axis, thickness, height);
    }

    public double minimumHeightMillimeters() {
        return Math.min(startHeight.toMillimeters(), endHeight.toMillimeters());
    }

    public double maximumHeightMillimeters() {
        return Math.max(startHeight.toMillimeters(), endHeight.toMillimeters());
    }

    public double heightAt(double offsetMillimeters) {
        double totalLength = axis.length().toMillimeters();
        if (totalLength <= 0.001) {
            return startHeight.toMillimeters();
        }
        double ratio = Math.max(0.0, Math.min(1.0, offsetMillimeters / totalLength));
        return startHeight.toMillimeters() + (endHeight.toMillimeters() - startHeight.toMillimeters()) * ratio;
    }

    public double heightAtStart() {
        return startHeight.toMillimeters();
    }

    public double heightAtEnd() {
        return endHeight.toMillimeters();
    }

    public boolean hasVariableTopHeight() {
        return Math.abs(startHeight.toMillimeters() - endHeight.toMillimeters()) > 0.001;
    }

    public Wall withEndpointHeights(Length newStartHeight, Length newEndHeight) {
        double maximumHeight = Math.max(newStartHeight.toMillimeters(), newEndHeight.toMillimeters());
        return new Wall(id, axis, thickness, Length.ofMillimeters(maximumHeight), newStartHeight, newEndHeight);
    }
}
