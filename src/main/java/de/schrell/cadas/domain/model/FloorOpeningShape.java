package de.schrell.cadas.domain.model;

/** Beschreibt die geometrische Grundform einer Bodenöffnung und damit ihre Parametrisierung. */
public enum FloorOpeningShape {
    RECTANGLE("Rechteckig"),
    CIRCLE("Rund");

    private final String label;

    FloorOpeningShape(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
