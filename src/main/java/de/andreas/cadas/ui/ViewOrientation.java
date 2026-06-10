package de.andreas.cadas.ui;

public enum ViewOrientation {
    TOP("Oben", "Grundriss in Draufsicht mit orthogonaler Projektion"),
    BOTTOM("Unten", "Grundriss in Untersicht mit orthogonaler Projektion"),
    NORTH("Nord", "Nordansicht mit orthogonaler Projektion"),
    SOUTH("Süd", "Südansicht mit orthogonaler Projektion"),
    EAST("Ost", "Ostansicht mit orthogonaler Projektion"),
    WEST("West", "Westansicht mit orthogonaler Projektion");

    private final String label;
    private final String overlayDescription;

    ViewOrientation(String label, String overlayDescription) {
        this.label = label;
        this.overlayDescription = overlayDescription;
    }

    public String label() {
        return label;
    }

    public String overlayDescription() {
        return overlayDescription;
    }
}
