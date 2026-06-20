package de.schrell.cadas.application.view;

public enum ProjectionMode {
    ORTHOGRAPHIC("Orthografisch"),
    PERSPECTIVE("Perspektivisch");

    private final String displayName;

    ProjectionMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
