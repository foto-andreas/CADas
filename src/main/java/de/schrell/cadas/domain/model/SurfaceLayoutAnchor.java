package de.schrell.cadas.domain.model;

/**
 * Beschreibt die feste Startecke des Belagsrasters relativ zu den
 * minimalen und maximalen Achsenkoordinaten der Zielgeometrie.
 */
public enum SurfaceLayoutAnchor {
    AUTO("Automatisch"),
    MIN_X_MIN_Y("Xmin / Ymin"),
    MAX_X_MIN_Y("Xmax / Ymin"),
    MAX_X_MAX_Y("Xmax / Ymax"),
    MIN_X_MAX_Y("Xmin / Ymax");

    private final String label;

    SurfaceLayoutAnchor(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public SurfaceLayoutAnchor next() {
        SurfaceLayoutAnchor[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
