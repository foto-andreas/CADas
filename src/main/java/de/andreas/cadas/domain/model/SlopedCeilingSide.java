package de.andreas.cadas.domain.model;

public enum SlopedCeilingSide {
    NORTH("Nordkante"),
    EAST("Ostkante"),
    SOUTH("Südkante"),
    WEST("Westkante");

    private final String label;

    SlopedCeilingSide(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public SlopedCeilingSide rotateClockwise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    public SlopedCeilingSide rotateCounterClockwise() {
        return switch (this) {
            case NORTH -> WEST;
            case WEST -> SOUTH;
            case SOUTH -> EAST;
            case EAST -> NORTH;
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
