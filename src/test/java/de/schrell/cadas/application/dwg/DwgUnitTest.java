package de.schrell.cadas.application.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DwgUnitTest {

    @Test
    void bildetDenVollstaendigenInsunitsWertebereichAb() {
        for (int code = 1; code <= 24; code++) {
            DwgUnit unit = DwgUnit.fromInsUnits(code);

            assertFalse(unit.assumed(), "INSUNITS " + code + " darf nicht als einheitenlos gelten.");
            assertTrue(unit.millimetersPerDrawingUnit() > 0.0);
        }
        assertEquals(1_000_000.0, DwgUnit.KILOMETER.millimetersPerDrawingUnit(), 0.0);
        assertEquals(0.000_001, DwgUnit.NANOMETER.millimetersPerDrawingUnit(), 0.0);
        assertEquals(304.800_609_601_219_2, DwgUnit.US_SURVEY_FOOT.millimetersPerDrawingUnit(), 0.0);
    }

    @Test
    void behandeltFehlendeUnbekannteUndKaputteHeaderwerteAlsEinheitenlos() {
        assertEquals(DwgUnit.UNITLESS, DwgUnit.fromRawHeaderValue(null));
        assertEquals(DwgUnit.UNITLESS, DwgUnit.fromRawHeaderValue(" "));
        assertEquals(DwgUnit.UNITLESS, DwgUnit.fromRawHeaderValue("keine Zahl"));
        assertEquals(DwgUnit.UNITLESS, DwgUnit.fromRawHeaderValue("25"));
        assertEquals(DwgUnit.PARSEC, DwgUnit.fromRawHeaderValue(" 20 "));
    }
}
