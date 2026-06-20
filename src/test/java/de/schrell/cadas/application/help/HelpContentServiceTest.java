package de.schrell.cadas.application.help;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpContentServiceTest {

    @Test
    void benutzerdokumentationIstVollstaendig() {
        String markdown = new HelpContentService().createMarkdown();

        assertTrue(markdown.contains("# Benutzerdokumentation"));
        assertTrue(markdown.contains("Grundbedienung"));
        assertTrue(markdown.contains("Werkzeuge"));
        assertTrue(markdown.contains("Tastaturkürzel"));
        assertTrue(markdown.contains("Navigation in 2D"));
        assertTrue(markdown.contains("Navigation in 3D"));
        assertFalse(markdown.isBlank());
    }

    @Test
    void keymapEnthaeltKuerzelUndMausbedienung() {
        String markdown = new HelpContentService().createKeymapMarkdown();

        assertTrue(markdown.contains("Tastaturkürzel"));
        assertTrue(markdown.contains("Mausbedienung"));
        assertTrue(markdown.contains("F1"));
        assertTrue(markdown.contains("⌘/Strg+S"));
        assertTrue(markdown.contains("Leertaste + Ziehen"));
        assertTrue(markdown.contains("Umschalt beim Wandzeichnen"));
        assertTrue(markdown.contains("3D-Navigation"));
        assertTrue(markdown.contains("Drucken"));
    }

    @Test
    void drittanbieterLizenzenWerdenAusLaufzeitabhaengigkeitenErzeugt() {
        String markdown = new HelpContentService().createThirdPartyLicensesMarkdown();

        assertTrue(markdown.contains("# Drittanbieter-Lizenzen"));
        assertTrue(markdown.contains("commonmark-0.24.0.jar"));
        assertTrue(markdown.contains("pdfbox-3.0.7.jar"));
        assertTrue(markdown.contains("automatisch"));
    }
}
