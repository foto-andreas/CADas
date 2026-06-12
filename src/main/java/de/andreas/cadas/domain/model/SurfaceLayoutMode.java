package de.andreas.cadas.domain.model;

public enum SurfaceLayoutMode {
    NONE("Kein Versatz"),
    AUTOMATIC("Automatisch"),
    FIXED("Fest");

    private final String displayName;

    SurfaceLayoutMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
