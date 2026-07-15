package de.schrell.cadas.domain.model;

/** Klassifiziert zusätzliche Bodenbereiche nach ihrer baulichen Funktion und späteren Darstellung. */
public enum FloorExtensionType {
    BALCONY("Balkon"),
    GALLERY("Empore");

    private final String label;

    FloorExtensionType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
