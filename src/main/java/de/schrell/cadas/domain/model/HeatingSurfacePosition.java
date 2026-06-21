package de.schrell.cadas.domain.model;

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
