package de.andreas.cadas.application.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DwgLibraryAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void analysiertKonvertierteDxfGeometrieUndMeldetKonverter() throws Exception {
        Path dwgFile = tempDir.resolve("teile.dwg");
        Files.writeString(dwgFile, "binäre Quelle wird im Test nicht gelesen");
        DwgLibraryAnalyzer analyzer = new DwgLibraryAnalyzer(new TestConverter(true));

        DwgLibraryAnalysis analysis = analyzer.analyze(dwgFile);

        assertTrue(analysis.successful());
        assertEquals("Testkonverter", analysis.converterName());
        assertTrue(analysis.summary().contains("1 nutzbare Blöcke"));
        assertEquals(800.0, analysis.blocks().getFirst().widthMillimeters(), 0.001);
        assertEquals(400.0, analysis.blocks().getFirst().heightMillimeters(), 0.001);
    }

    @Test
    void bleibtOhneExternenKonverterEhrlichNichtVerfügbar() {
        DwgLibraryAnalyzer analyzer = new DwgLibraryAnalyzer(new TestConverter(false));

        DwgLibraryAnalysis analysis = analyzer.analyze(tempDir.resolve("teile.dwg"));

        assertFalse(analysis.successful());
        assertTrue(analysis.messages().getFirst().contains("kein Testkonverter"));
    }

    private static final class TestConverter implements DwgToDxfConverter {

        private final boolean available;

        private TestConverter(boolean available) {
            this.available = available;
        }

        @Override
        public DwgConversionAvailability availability() {
            return available
                    ? DwgConversionAvailability.available("Testkonverter", "/tmp/testkonverter")
                    : DwgConversionAvailability.unavailable("kein Testkonverter verfügbar");
        }

        @Override
        public DwgConversionResult convert(Path dwgFile, Path targetDxfFile) throws IOException {
            Files.writeString(targetDxfFile, """
                    0
                    SECTION
                    2
                    HEADER
                    9
                    $INSUNITS
                    70
                    4
                    0
                    ENDSEC
                    0
                    SECTION
                    2
                    BLOCKS
                    0
                    BLOCK
                    2
                    TESTBLOCK
                    0
                    LINE
                    8
                    TEST
                    10
                    0
                    20
                    0
                    11
                    800
                    21
                    400
                    0
                    ENDBLK
                    0
                    ENDSEC
                    0
                    EOF
                    """);
            return new DwgConversionResult(targetDxfFile, "Testkonverter", List.of("ok"));
        }
    }
}
