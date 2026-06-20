package de.schrell.cadas.domain.model;

import java.util.Locale;

public enum RoomObjectMountingMode {
    STANDS_ON_COVERING("steht auf Bodenbelag", false),
    CUTS_FLOOR_COVERING("Bodenbelag wird ausgespart", true),
    WALL_MOUNTED("wandmontiert", false);

    private final String label;
    private final boolean cutsFloorCovering;

    RoomObjectMountingMode(String label, boolean cutsFloorCovering) {
        this.label = label;
        this.cutsFloorCovering = cutsFloorCovering;
    }

    public boolean cutsFloorCovering() {
        return cutsFloorCovering;
    }

    public static RoomObjectMountingMode fromCutsFloorCovering(boolean cutsFloorCovering) {
        return cutsFloorCovering ? CUTS_FLOOR_COVERING : STANDS_ON_COVERING;
    }

    public static RoomObjectMountingMode fromStoredValue(String storedValue, boolean fallbackCutsFloorCovering) {
        if (storedValue == null || storedValue.isBlank()) {
            return fromCutsFloorCovering(fallbackCutsFloorCovering);
        }
        try {
            return RoomObjectMountingMode.valueOf(storedValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fromCutsFloorCovering(fallbackCutsFloorCovering);
        }
    }

    @Override
    public String toString() {
        return label;
    }
}
