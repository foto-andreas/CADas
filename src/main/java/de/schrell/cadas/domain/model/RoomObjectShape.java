package de.schrell.cadas.domain.model;

/** Geometrische Repräsentation eines Raumobjekts für Grundriss, Kollision und 3D-Ableitung. */
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
