package de.schrell.cadas.application.help;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AboutInformationTest {

    @Test
    void übernimmtBuildInformationenAusRessource() {
        Properties buildInformation = new Properties();
        buildInformation.setProperty("version", "9.8.7");
        buildInformation.setProperty("buildTimestamp", "2026-06-28T14:15:16Z");

        AboutInformation information = AboutInformation.current("1.4.0", buildInformation);

        assertEquals("CADas", information.applicationName());
        assertEquals("9.8.7", information.version());
        assertEquals("2026-06-28T14:15:16Z", information.buildTimestamp());
        assertTrue(information.detailText().contains("Build-Zeitpunkt 2026-06-28T14:15:16Z"));
        assertTrue(information.detailText().contains("CAD-Programm für Gebäude-Grundrisse"));
    }

    @Test
    void fälltOhneBuildRessourceAufLaufzeitversionZurück() {
        AboutInformation information = AboutInformation.current("1.4.0", new Properties());

        assertEquals("1.4.0", information.version());
        assertEquals("nicht verfügbar", information.buildTimestamp());
    }

    @Test
    void liefertVollständigeAnwendungsinformationen() {
        AboutInformation information = AboutInformation.current();

        assertEquals("CADas", information.applicationName());
        assertTrue(information.version().equals("Entwicklungsversion") || information.version().matches("\\d+\\.\\d+\\.\\d+"));
        assertTrue(!information.buildTimestamp().isBlank());
        assertTrue(information.detailText().contains("CAD-Programm für Gebäude-Grundrisse"));
    }
}
