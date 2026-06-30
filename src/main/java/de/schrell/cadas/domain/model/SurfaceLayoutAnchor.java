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

    public SurfaceLayoutAnchor nextManual() {
        return switch (this) {
            case AUTO, MIN_X_MIN_Y -> MIN_X_MAX_Y;
            case MIN_X_MAX_Y -> MAX_X_MAX_Y;
            case MAX_X_MAX_Y -> MAX_X_MIN_Y;
            case MAX_X_MIN_Y -> MIN_X_MIN_Y;
        };
    }

    public SurfaceLayoutAnchor previousManual() {
        return switch (this) {
            case AUTO, MIN_X_MIN_Y -> MAX_X_MIN_Y;
            case MAX_X_MIN_Y -> MAX_X_MAX_Y;
            case MAX_X_MAX_Y -> MIN_X_MAX_Y;
            case MIN_X_MAX_Y -> MIN_X_MIN_Y;
        };
    }
}
