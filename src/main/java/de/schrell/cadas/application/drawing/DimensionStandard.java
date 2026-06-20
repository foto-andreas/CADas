package de.schrell.cadas.application.drawing;

public enum DimensionStandard {
    DIN_EN_ISO_7519_2025_01("DIN EN ISO 7519 | 2025-01");

    private final String label;

    DimensionStandard(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
