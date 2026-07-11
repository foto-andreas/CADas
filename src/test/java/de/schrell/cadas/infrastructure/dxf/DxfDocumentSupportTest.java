package de.schrell.cadas.infrastructure.dxf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DxfDocumentSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void ersetztZieldateiAtomarUndRaeumtTemporaereDateiAuf() throws Exception {
        Path target = tempDir.resolve("neu").resolve("x");

        DxfDocumentSupport.writeAtomically(target, "erste Fassung");
        DxfDocumentSupport.writeAtomically(target, "vollständige neue Fassung");

        assertEquals("vollständige neue Fassung", Files.readString(target));
        try (var files = Files.list(target.getParent())) {
            assertEquals(java.util.List.of(target), files.toList());
        }
    }

    @Test
    void entferntTemporaereDateiAuchBeiFehlgeschlagenemErsetzen() throws Exception {
        Path targetDirectory = Files.createDirectory(tempDir.resolve("ziel"));

        assertThrows(IOException.class, () -> DxfDocumentSupport.writeAtomically(targetDirectory, "Inhalt"));

        try (var files = Files.list(tempDir)) {
            assertEquals(java.util.List.of(targetDirectory), files.toList());
        }
    }
}
