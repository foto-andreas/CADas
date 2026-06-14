package de.andreas.cadas.domain.model;

public enum RoomObjectType {
    SHOWER("Dusche"),
    TOILET("Toilette"),
    WASHBASIN("Waschbecken"),
    WALL_CABINET("Wandschrank"),
    CABINET("Schrank"),
    TABLE("Tisch"),
    DWG_REFERENCE("DWG-Objekt");

    private final String label;

    RoomObjectType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
