package de.andreas.cadas.domain.model;

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

