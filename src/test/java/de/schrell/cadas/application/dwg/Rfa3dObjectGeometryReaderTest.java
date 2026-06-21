package de.schrell.cadas.application.dwg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static de.schrell.cadas.testsupport.Dxf3dTestFixtures.simpleSolidDxf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Rfa3dObjectGeometryReaderTest {

    @TempDir
    Path tempDir;

    private final Rfa3dObjectGeometryReader reader = new Rfa3dObjectGeometryReader();

    @Test
    void liestGleichnamigeDxfBegleitgeometrie() throws Exception {
        Path rfa = tempDir.resolve("Wärmepumpe.rfa");
        Files.writeString(rfa, "RFA");
        Files.writeString(tempDir.resolve("Wärmepumpe.dxf"), simpleSolidDxf());

        Dxf3dObjectGeometry geometry = reader.read(rfa);

        assertEquals(20.0, geometry.bounds().widthMillimeters(), 0.001);
        assertEquals("Wärmepumpe.dxf", reader.companionFile(rfa).orElseThrow().getFileName().toString());
    }

    @Test
    void verlangtEineGleichnamigeBegleitgeometrie() throws Exception {
        Path rfa = tempDir.resolve("OhneGeometrie.rfa");
        Files.writeString(rfa, "RFA");

        assertThrows(IllegalArgumentException.class, () -> reader.read(rfa));
    }
}
