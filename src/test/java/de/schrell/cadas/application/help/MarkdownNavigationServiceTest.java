package de.schrell.cadas.application.help;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MarkdownNavigationServiceTest {

    private final MarkdownNavigationService service = new MarkdownNavigationService();

    @Test
    void extrahiertKapitelUndUnterkapitelInDokumentreihenfolge() {
        var sections = service.sections("""
                # Hilfe
                ## Projekt
                ### **Sichern**
                Text
                #### `Randfall`
                """);

        assertEquals(3, sections.size());
        assertEquals("Projekt", sections.get(0).title());
        assertEquals("abschnitt-2", sections.get(1).anchor());
        assertEquals("  Sichern", sections.get(1).toString());
        assertEquals("    Randfall", sections.get(2).toString());
    }
}
