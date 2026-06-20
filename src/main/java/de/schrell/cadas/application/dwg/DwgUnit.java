package de.schrell.cadas.application.dwg;

import java.util.Optional;

public enum DwgUnit {
    UNITLESS(0, "einheitenlos", 1.0, true),
    INCH(1, "Zoll", 25.4, false),
    FOOT(2, "Fuß", 304.8, false),
    MILE(3, "Meile", 1_609_344.0, false),
    MILLIMETER(4, "Millimeter", 1.0, false),
    CENTIMETER(5, "Zentimeter", 10.0, false),
    METER(6, "Meter", 1000.0, false);

    private final int insUnitsCode;
    private final String label;
    private final double millimetersPerDrawingUnit;
    private final boolean assumed;

    DwgUnit(int insUnitsCode, String label, double millimetersPerDrawingUnit, boolean assumed) {
        this.insUnitsCode = insUnitsCode;
        this.label = label;
        this.millimetersPerDrawingUnit = millimetersPerDrawingUnit;
        this.assumed = assumed;
    }

    public String label() {
        return label;
    }

    public double millimetersPerDrawingUnit() {
        return millimetersPerDrawingUnit;
    }

    public boolean assumed() {
        return assumed;
    }

    public static DwgUnit fromInsUnits(int code) {
        for (DwgUnit unit : values()) {
            if (unit.insUnitsCode == code) {
                return unit;
            }
        }
        return UNITLESS;
    }

    public static DwgUnit fromRawHeaderValue(String rawValue) {
        return Optional.ofNullable(rawValue)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException exception) {
                        return 0;
                    }
                })
                .map(DwgUnit::fromInsUnits)
                .orElse(UNITLESS);
    }

    @Override
    public String toString() {
        return assumed ? label + " (als mm angenommen)" : label;
    }
}
