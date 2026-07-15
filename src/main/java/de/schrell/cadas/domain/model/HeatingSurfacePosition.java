package de.schrell.cadas.domain.model;

/** Ordnet eine Heizfläche dem Boden oder der Decke zu und steuert Höhenlage und Darstellung. */
public enum HeatingSurfacePosition {
    FLOOR("Fußboden"),
    CEILING("Decke");

    private final String label;

    HeatingSurfacePosition(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
