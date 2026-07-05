package de.schrell.cadas.application.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StandardPartLibraryServiceTest {

    @Test
    void liefertStandardbibliothekFuerTuerenFensterUndTreppen() {
        StandardPartLibrary library = StandardPartLibraryService.load();

        assertTrue(library.doorPresets().size() >= 3);
        assertTrue(library.windowPresets().size() >= 3);
        assertTrue(library.stairPresets().size() >= 3);
        assertEquals("Gerade Treppe", library.stairPresets().getFirst().name());
    }
}
