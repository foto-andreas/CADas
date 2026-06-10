package de.andreas.cadas.ui;

public enum ViewOrientation {
    TOP("Oben"),
    BOTTOM("Unten"),
    NORTH("Nord"),
    SOUTH("Süd"),
    EAST("Ost"),
    WEST("West");

    private final String label;

    ViewOrientation(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

