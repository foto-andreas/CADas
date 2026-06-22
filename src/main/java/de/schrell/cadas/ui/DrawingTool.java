package de.schrell.cadas.ui;

public enum DrawingTool {
    EDIT("Bearbeiten", true),
    WALL("Wand", false),
    STAIR("Treppe", false),
    FLOOR_EXTENSION("Balkon/Empore", false),
    FLOOR_OPENING_RECTANGLE("Bodenloch rechteckig", false),
    FLOOR_OPENING_CIRCLE("Bodenloch rund", false),
    HEATING_ZONE_RECTANGLE("Heizkreis", false),
    HEATING_EXCLUSION_RECTANGLE("FBH-Sperrfläche", false),
    DOOR("Tür", true),
    WINDOW("Fenster", true),
    ROOF_WINDOW("Dachfenster", true),
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
