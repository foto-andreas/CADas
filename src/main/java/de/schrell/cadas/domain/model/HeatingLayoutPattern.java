package de.schrell.cadas.domain.model;

public enum HeatingLayoutPattern {
    MEANDER("Meander"),
    SPIRAL("Schnecke"),
    VARIO("Vario");

    private final String label;

    HeatingLayoutPattern(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
