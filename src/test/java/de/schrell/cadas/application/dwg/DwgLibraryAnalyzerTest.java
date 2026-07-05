package de.schrell.cadas.application.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DwgLibraryAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void analysiertKonvertierteDxfGeometrieUndMeldetKonverter() throws Exception {
        Path dwgFile = tempDir.resolve("teile.dwg");
        Files.writeString(dwgFile, "binäre Quelle wird im Test nicht gelesen");
        DwgLibraryAnalyzer analyzer = new DwgLibraryAnalyzer(testConverter());

        DwgLibraryAnalysis analysis = analyzer.analyze(dwgFile);

        assertTrue(analysis.successful());
        assertEquals("Testkonverter", analysis.converterName());
        assertTrue(analysis.summary().contains("1 nutzbare Blöcke"));
        assertEquals(800.0, analysis.blocks().getFirst().widthMillimeters(), 0.001);
        assertEquals(400.0, analysis.blocks().getFirst().heightMillimeters(), 0.001);
    }

    @Test
    void bleibtOhneExternenKonverterEhrlichNichtVerfügbar() {
        DwgLibraryAnalyzer analyzer = new DwgLibraryAnalyzer(
                ExternalDwgToDxfConverter.fromEnvironment(Map.of("PATH", ""), List.of())
        );

        DwgLibraryAnalysis analysis = analyzer.analyze(tempDir.resolve("teile.dwg"));

        assertFalse(analysis.successful());
        assertTrue(analysis.messages().getFirst().contains("Kein DWG-Konverter"));
    }

    private ExternalDwgToDxfConverter testConverter() throws Exception {
        Path executable = tempDir.resolve("dwg2dxf");
        Files.writeString(executable, """
                #!/bin/sh
                while [ "$#" -gt 0 ]; do
                  if [ "$1" = "-o" ]; then
                    shift
                    ziel="$1"
                  fi
                  shift
                done
                cat > "$ziel" <<'DXF'
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
                DXF
                echo ok
                """);
        executable.toFile().setExecutable(true);
        return new ExternalDwgToDxfConverter(new ExternalDwgToDxfConverter.Tool(
                "Testkonverter",
                executable,
                ExternalDwgToDxfConverter.ToolMode.DWG2DXF
        ));
    }
}
