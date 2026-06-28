package de.schrell.cadas.domain.model;

public enum SurfaceLayoutDirection {
    LEFT_TO_RIGHT("Links nach rechts"),
    RIGHT_TO_LEFT("Rechts nach links");

    private final String label;

    SurfaceLayoutDirection(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
