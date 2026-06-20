package de.schrell.cadas.application.help;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AboutInformationTest {

    @Test
    void liefertVollständigeAnwendungsinformationen() {
        AboutInformation information = AboutInformation.current();

        assertEquals("CADas", information.applicationName());
        assertTrue(information.version().equals("Entwicklungsversion") || information.version().matches("\\d+\\.\\d+\\.\\d+"));
        assertTrue(information.detailText().contains("CAD-Programm für Gebäude-Grundrisse"));
    }
}
