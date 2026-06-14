package de.andreas.cadas.application.dwg;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExternalDwgToDxfConverterTest {

    @TempDir
    Path tempDir;

    @Test
    void meldetFehlendenKonverterVerständlich() {
        ExternalDwgToDxfConverter converter = ExternalDwgToDxfConverter.fromEnvironment(Map.of("PATH", ""));

        DwgConversionAvailability availability = converter.availability();

        assertFalse(availability.available());
        assertTrue(availability.message().contains("brew install libredwg"));
    }

    @Test
    void erkenntKonfiguriertenDwg2DxfKonverter() throws Exception {
        Path executable = tempDir.resolve("dwg2dxf");
        Files.writeString(executable, "#!/bin/sh\nexit 0\n");
        assertTrue(executable.toFile().setExecutable(true));

        ExternalDwgToDxfConverter converter = ExternalDwgToDxfConverter.fromEnvironment(Map.of(
                "PATH", "",
                "CADAS_DWG_CONVERTER", executable.toString()
        ));

        DwgConversionAvailability availability = converter.availability();

        assertTrue(availability.available());
        assertTrue(availability.converterName().contains("dwg2dxf"));
    }

    @Test
    void brichtHängendenKonverterOhneBlockierendesOutputLesenAb() throws Exception {
        Path executable = tempDir.resolve("dwg2dxf");
        Files.writeString(executable, "#!/bin/sh\necho gestartet\nsleep 5\n");
        assertTrue(executable.toFile().setExecutable(true));
        ExternalDwgToDxfConverter converter = new ExternalDwgToDxfConverter(
                new ExternalDwgToDxfConverter.Tool("Test dwg2dxf", executable, ExternalDwgToDxfConverter.ToolMode.DWG2DXF),
                Duration.ofMillis(100)
        );

        IOException exception = assertThrows(
                IOException.class,
                () -> converter.convert(tempDir.resolve("quelle.dwg"), tempDir.resolve("ziel.dxf"))
        );

        assertTrue(exception.getMessage().contains("100 ms"));
    }
}
