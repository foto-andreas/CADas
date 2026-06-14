package de.andreas.cadas.application.dwg;

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
