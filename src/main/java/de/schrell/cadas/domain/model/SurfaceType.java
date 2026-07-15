package de.schrell.cadas.domain.model;

/** Identifiziert die bauliche Oberfläche, auf die ein Schichtaufbau oder Belag angewendet wird. */
public enum SurfaceType {
    FLOOR("Boden"),
    CEILING("Decke"),
    WALL_INTERIOR("Innenwand"),
    WALL_EXTERIOR("Außenwand"),
    ROOF("Dach");

    private final String displayName;

    SurfaceType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
