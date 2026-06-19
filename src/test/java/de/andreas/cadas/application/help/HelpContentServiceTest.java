package de.andreas.cadas.application.help;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpContentServiceTest {

    @Test
    void beschreibtFunktionenUndMausSondertasten() {
        String markdown = new HelpContentService().createMarkdown();

        assertTrue(markdown.contains("Aktuelle Möglichkeiten"));
        assertTrue(markdown.contains("F1"));
        assertTrue(markdown.contains("Leertaste+Ziehen"));
        assertTrue(markdown.contains("Umschalt beim Wandzeichnen"));
        assertTrue(markdown.contains("Drucken"));
    }
}
