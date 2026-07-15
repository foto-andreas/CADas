package de.schrell.cadas.application.dwg;

import java.util.Optional;

/**
 * Ordnet den vollständigen Autodesk-Headerwert {@code $INSUNITS} einer CAD-Zeichnung einem
 * Millimeter-Skalierungsfaktor zu. Code 0 und unbekannte Werte sind dimensionslos; CADas muss für sie aus
 * Kompatibilitätsgründen Millimeter annehmen und kennzeichnet diese Annahme ausdrücklich.
 */
public enum DwgUnit {
    UNITLESS(0, "einheitenlos", 1.0, true),
    INCH(1, "Zoll", 25.4, false),
    FOOT(2, "Fuß", 304.8, false),
    MILE(3, "Meile", 1_609_344.0, false),
    MILLIMETER(4, "Millimeter", 1.0, false),
    CENTIMETER(5, "Zentimeter", 10.0, false),
    METER(6, "Meter", 1_000.0, false),
    KILOMETER(7, "Kilometer", 1_000_000.0, false),
    MICROINCH(8, "Mikrozoll", 0.000_025_4, false),
    MIL(9, "Mil", 0.0254, false),
    YARD(10, "Yard", 914.4, false),
    ANGSTROM(11, "Ångström", 0.000_000_1, false),
    NANOMETER(12, "Nanometer", 0.000_001, false),
    MICRON(13, "Mikrometer", 0.001, false),
    DECIMETER(14, "Dezimeter", 100.0, false),
    DECAMETER(15, "Dekameter", 10_000.0, false),
    HECTOMETER(16, "Hektometer", 100_000.0, false),
    GIGAMETER(17, "Gigameter", 1_000_000_000_000.0, false),
    ASTRONOMICAL_UNIT(18, "Astronomische Einheit", 149_597_870_700_000.0, false),
    LIGHT_YEAR(19, "Lichtjahr", 9.460_730_472_580_8E18, false),
    PARSEC(20, "Parsec", 3.085_677_581_491_367E19, false),
    US_SURVEY_FOOT(21, "US-Survey-Fuß", 304.800_609_601_219_2, false),
    US_SURVEY_INCH(22, "US-Survey-Zoll", 25.400_050_800_101_6, false),
    US_SURVEY_YARD(23, "US-Survey-Yard", 914.401_828_803_657_6, false),
    US_SURVEY_MILE(24, "US-Survey-Meile", 1_609_347.218_694_437_3, false);

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
