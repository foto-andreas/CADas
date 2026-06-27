package de.schrell.cadas.domain.model;

import java.util.Locale;

/**
 * Ordnet einem Objekt seine fachliche Heizart zu.
 */
public enum RoomObjectHeatingType {
    NONE("Keine Heizung"),
    HEATING_ELEMENT("Heizelement"),
    FLOOR_HEATING("FBH"),
    CEILING_HEATING("DH"),
    SURFACE_HEATING("Flächenheizung");

    private final String displayName;

    RoomObjectHeatingType(String displayName) {
        this.displayName = displayName;
    }

    public static RoomObjectHeatingType defaultForLegacyHeatOutput(double heatOutputWatts) {
        return heatOutputWatts > 0.0 ? HEATING_ELEMENT : NONE;
    }

    public static RoomObjectHeatingType fromStoredValue(String storedValue, double heatOutputWatts) {
        if (storedValue == null || storedValue.isBlank()) {
            return defaultForLegacyHeatOutput(heatOutputWatts);
        }
        return valueOf(storedValue.trim().toUpperCase(Locale.ROOT));
    }

    public static RoomObjectHeatingType fromUserInput(String input) {
        String normalized = input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "", "KEINE", "KEINE HEIZUNG", "NONE" -> NONE;
            case "HEIZELEMENT", "HEATING_ELEMENT" -> HEATING_ELEMENT;
            case "FBH", "FLOOR_HEATING" -> FLOOR_HEATING;
            case "DH", "CEILING_HEATING" -> CEILING_HEATING;
            case "FLÄCHENHEIZUNG", "FLAECHENHEIZUNG", "SURFACE_HEATING" -> SURFACE_HEATING;
            default -> valueOf(normalized);
        };
    }

    public boolean isHeated() {
        return this != NONE;
    }

    public boolean countsAsHeatingElement() {
        return this == HEATING_ELEMENT;
    }

    public boolean countsAsFloorHeating() {
        return this == FLOOR_HEATING;
    }

    public boolean countsAsCeilingHeating() {
        return this == CEILING_HEATING;
    }

    public boolean countsAsAdditionalSurfaceHeating() {
        return this == SURFACE_HEATING;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
