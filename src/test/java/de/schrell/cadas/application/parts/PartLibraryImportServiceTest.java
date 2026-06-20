package de.schrell.cadas.application.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PartLibraryImportServiceTest {

    private final PartLibraryImportService importService = new PartLibraryImportService();

    @TempDir
    Path tempDir;

    @Test
    void importiertExterneTeilebibliothekAusTextdatei() throws Exception {
        Path file = tempDir.resolve("bibliothek.cadasparts");
        Files.writeString(file, """
                # Beispielbibliothek
                DOOR;door-custom;Partner Tür 98 x 210;980;2100;0
                WINDOW;window-custom;Partner Fenster 140 x 120;1400;1200;950
                STAIR;stair-custom;Partner Treppe;HALF_TURN;2900;17
                """);

        StandardPartLibrary library = importService.importLibrary(file);

        assertEquals(1, library.doorPresets().size());
        assertEquals(1, library.windowPresets().size());
        assertEquals(1, library.stairPresets().size());
        assertEquals("Partner Tür 98 x 210", library.doorPresets().getFirst().name());
    }
}
