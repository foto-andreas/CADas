package de.schrell.cadas.domain.model;

/** Legt fest, ob ein Belag automatisch, von einem Anker oder über eine manuelle Vorgabe angeordnet wird. */
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
