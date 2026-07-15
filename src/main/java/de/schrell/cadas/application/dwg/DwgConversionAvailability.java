package de.schrell.cadas.application.dwg;

/** Ergebnis der Konvertersuche mit Verfügbarkeit, gewähltem Programm und verständlicher Diagnose. */
public record DwgConversionAvailability(
        boolean available,
        String converterName,
        String executable,
        String message
) {

    public static DwgConversionAvailability available(String converterName, String executable) {
        return new DwgConversionAvailability(true, converterName, executable, "DWG-Konverter verfügbar: " + converterName);
    }

    public static DwgConversionAvailability unavailable(String message) {
        return new DwgConversionAvailability(false, "", "", message);
    }
}
