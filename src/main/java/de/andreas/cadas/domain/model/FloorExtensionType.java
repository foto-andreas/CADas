package de.andreas.cadas.domain.model;

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
