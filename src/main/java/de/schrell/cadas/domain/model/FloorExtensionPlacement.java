package de.schrell.cadas.domain.model;

/** Legt fest, an welcher Seite einer Bezugskante ein ergänzender Bodenaufbau angeordnet wird. */
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
