package de.schrell.cadas.domain.model;

/** Hauptrichtung, in der ein rechteckiger Belag über die Zielfläche fortgeschrieben wird. */
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
