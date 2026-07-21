package de.schrell.cadas.application.layers;

/** Geltungsbereich einer Materialoperation. */
public enum SurfaceMaterialUsageScope {
    ENTIRE_PROJECT("Gesamte Zeichnung"),
    SELECTED_ROOM("Ausgewählter Raum");

    private final String label;

    SurfaceMaterialUsageScope(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
