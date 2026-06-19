package de.andreas.cadas.domain.model;

public enum FloorExtensionPlacement {
    INTERIOR("Innen"),
    EXTERIOR("Außen");

    private final String label;

    FloorExtensionPlacement(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
