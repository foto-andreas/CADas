package de.andreas.cadas.domain.model;

public enum RoomObjectShape {
    RECTANGLE("Rechteck"),
    HALF_ROUND("halbrund"),
    QUARTER_CIRCLE("viertelkreis"),
    OVAL("oval"),
    CIRCLE("rund");

    private final String label;

    RoomObjectShape(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
