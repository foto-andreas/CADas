package de.andreas.cadas.ui;

public enum DrawingTool {
    EDIT("Bearbeiten", true),
    WALL("Wand", false),
    STAIR("Treppe", false),
    FLOOR_EXTENSION("Balkon/Empore", false),
    DOOR("Tür", true),
    WINDOW("Fenster", true),
    OBJECT("Objekt", true);

    private final String label;
    private final boolean pointTool;

    DrawingTool(String label, boolean pointTool) {
        this.label = label;
        this.pointTool = pointTool;
    }

    public String label() {
        return label;
    }

    public boolean isPointTool() {
        return pointTool;
    }

    @Override
    public String toString() {
        return label;
    }
}
