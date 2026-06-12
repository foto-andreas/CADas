package de.andreas.cadas.ui;

public enum ThreeDViewPreset {
    TOP("Oben", "Wechselt die 3D-Kamera auf eine Draufsicht.", 0.0, -90.0),
    BOTTOM("Unten", "Wechselt die 3D-Kamera auf eine Untersicht.", 0.0, 90.0),
    FRONT("Vorne", "Wechselt die 3D-Kamera auf eine Vorderansicht.", 0.0, 0.0),
    BACK("Hinten", "Wechselt die 3D-Kamera auf eine Rückansicht.", 180.0, 0.0),
    RIGHT("Rechts", "Wechselt die 3D-Kamera auf eine rechte Seitenansicht.", -90.0, 0.0),
    LEFT("Links", "Wechselt die 3D-Kamera auf eine linke Seitenansicht.", 90.0, 0.0);

    private final String label;
    private final String tooltip;
    private final double cameraAzimuthDegrees;
    private final double cameraElevationDegrees;

    ThreeDViewPreset(String label, String tooltip, double cameraAzimuthDegrees, double cameraElevationDegrees) {
        this.label = label;
        this.tooltip = tooltip;
        this.cameraAzimuthDegrees = cameraAzimuthDegrees;
        this.cameraElevationDegrees = cameraElevationDegrees;
    }

    public String label() {
        return label;
    }

    public String tooltip() {
        return tooltip;
    }

    public double cameraAzimuthDegrees() {
        return cameraAzimuthDegrees;
    }

    public double cameraElevationDegrees() {
        return cameraElevationDegrees;
    }
}
