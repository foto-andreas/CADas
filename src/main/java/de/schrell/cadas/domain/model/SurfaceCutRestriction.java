package de.schrell.cadas.domain.model;

public enum SurfaceCutRestriction {
    FREE("frei", true),
    OUTER_CUTS_ROTATABLE("Schnitt nur außen", true),
    LAY_DIRECTION_OUTER_CUTS("Verlegerichtung, Schnitt nur außen", false);

    private final String label;
    private final boolean materialRotationAllowed;

    SurfaceCutRestriction(String label, boolean materialRotationAllowed) {
        this.label = label;
        this.materialRotationAllowed = materialRotationAllowed;
    }

    public String label() {
        return label;
    }

    public boolean allowsMaterialRotation() {
        return materialRotationAllowed;
    }

    public static SurfaceCutRestriction fallback() {
        return LAY_DIRECTION_OUTER_CUTS;
    }

    public static SurfaceCutRestriction fromStoredValue(String value) {
        if (value == null || value.isBlank()) {
            return fallback();
        }
        String normalized = value.trim();
        for (SurfaceCutRestriction restriction : values()) {
            if (restriction.name().equalsIgnoreCase(normalized) || restriction.label.equalsIgnoreCase(normalized)) {
                return restriction;
            }
        }
        return fallback();
    }

    @Override
    public String toString() {
        return label;
    }
}
