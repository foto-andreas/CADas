package de.schrell.cadas.domain.model;

/** Diskrete Drehung eines Oberflächenbelags relativ zur lokalen Fläche. */
public enum SurfaceLayoutRotation {
    DEGREES_0("0°", false, false),
    DEGREES_90("90°", true, false),
    DEGREES_180("180°", false, true),
    DEGREES_270("270°", true, true);

    private final String label;
    private final boolean rotatedQuarterTurn;
    private final boolean maximumYStart;

    SurfaceLayoutRotation(String label, boolean rotatedQuarterTurn, boolean maximumYStart) {
        this.label = label;
        this.rotatedQuarterTurn = rotatedQuarterTurn;
        this.maximumYStart = maximumYStart;
    }

    public String label() {
        return label;
    }

    public boolean rotatedQuarterTurn() {
        return rotatedQuarterTurn;
    }

    public boolean maximumYStart() {
        return maximumYStart;
    }
}
