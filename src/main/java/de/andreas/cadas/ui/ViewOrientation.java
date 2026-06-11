package de.andreas.cadas.ui;

public enum ViewOrientation {
    TOP("Oben", "⤒ Oben", "Grundriss in Draufsicht mit orthogonaler Projektion", 0.0, 90.0),
    BOTTOM("Unten", "⤓ Unten", "Grundriss in Untersicht mit orthogonaler Projektion", 0.0, -90.0),
    NORTH("Vorne", "↑", "Frontansicht mit orthogonaler Projektion", 0.0, 0.0),
    SOUTH("Hinten", "↓", "Rückansicht mit orthogonaler Projektion", 180.0, 0.0),
    EAST("Rechts", "→", "Rechte Seitenansicht mit orthogonaler Projektion", 90.0, 0.0),
    WEST("Links", "←", "Linke Seitenansicht mit orthogonaler Projektion", -90.0, 0.0);

    private final String label;
    private final String buttonLabel;
    private final String overlayDescription;
    private final double cameraAzimuthDegrees;
    private final double cameraElevationDegrees;

    ViewOrientation(
            String label,
            String buttonLabel,
            String overlayDescription,
            double cameraAzimuthDegrees,
            double cameraElevationDegrees
    ) {
        this.label = label;
        this.buttonLabel = buttonLabel;
        this.overlayDescription = overlayDescription;
        this.cameraAzimuthDegrees = cameraAzimuthDegrees;
        this.cameraElevationDegrees = cameraElevationDegrees;
    }

    public String label() {
        return label;
    }

    public String buttonLabel() {
        return buttonLabel;
    }

    public String overlayDescription() {
        return overlayDescription;
    }

    public double cameraAzimuthDegrees() {
        return cameraAzimuthDegrees;
    }

    public double cameraElevationDegrees() {
        return cameraElevationDegrees;
    }
}
