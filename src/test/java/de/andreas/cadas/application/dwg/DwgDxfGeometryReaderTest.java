package de.andreas.cadas.application.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DwgDxfGeometryReaderTest {

    @TempDir
    Path tempDir;

    private final DwgDxfGeometryReader reader = new DwgDxfGeometryReader();

    @Test
    void liestEinheitenBlockgeometrieLayerHandlesUndRotierteInserts() throws Exception {
        Path dxfFile = tempDir.resolve("bibliothek.dxf");
        Path sourceFile = tempDir.resolve("bibliothek.dwg");
        Files.writeString(dxfFile, """
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
                5
                B1
                2
                PLATTE
                8
                0
                10
                100
                20
                50
                0
                LWPOLYLINE
                5
                E1
                8
                PLATTEN
                90
                4
                10
                100
                20
                50
                10
                1300
                20
                50
                10
                1300
                20
                650
                10
                100
                20
                650
                0
                ENDBLK
                5
                B2
                0
                BLOCK
                5
                B3
                2
                MOEBEL
                10
                0
                20
                0
                0
                INSERT
                5
                E2
                8
                EINBAU
                2
                PLATTE
                10
                2000
                20
                3000
                41
                1
                42
                1
                50
                90
                0
                ENDBLK
                5
                B4
                0
                ENDSEC
                0
                SECTION
                2
                ENTITIES
                0
                INSERT
                5
                E3
                8
                MODELL
                2
                PLATTE
                10
                500
                20
                600
                0
                LINE
                5
                E4
                8
                MODELL
                10
                0
                20
                0
                11
                100
                21
                200
                0
                ENDSEC
                0
                EOF
                """);

        DwgLibraryAnalysis analysis = reader.read(dxfFile, sourceFile, "Testkonverter", List.of("Konvertiert"));

        assertTrue(analysis.successful());
        assertEquals(DwgUnit.MILLIMETER, analysis.unit());
        DwgBlockDefinition plate = block(analysis, "PLATTE");
        assertEquals(1200.0, plate.widthMillimeters(), 0.001);
        assertEquals(600.0, plate.heightMillimeters(), 0.001);
        assertTrue(plate.layers().contains("PLATTEN"));
        assertTrue(plate.handles().contains("B1"));
        assertTrue(plate.handles().contains("E1"));
        DwgBlockDefinition rotatedInsert = block(analysis, "MOEBEL");
        assertEquals(600.0, rotatedInsert.widthMillimeters(), 0.001);
        assertEquals(1200.0, rotatedInsert.heightMillimeters(), 0.001);
        assertEquals(1, rotatedInsert.inserts().size());
        assertEquals(90.0, rotatedInsert.inserts().getFirst().rotationDegrees(), 0.001);
        DwgBlockDefinition modelSpace = block(analysis, "Modellbereich");
        assertTrue(modelSpace.hasGeometry());
        assertEquals(sourceFile.toAbsolutePath().normalize() + "#PLATTE", plate.sourceReference());
    }

    @Test
    void insertUrsprungVergrößertGrenzenNichtKünstlich() throws Exception {
        Path dxfFile = tempDir.resolve("ursprung.dxf");
        Path sourceFile = tempDir.resolve("ursprung.dwg");
        Files.writeString(dxfFile, """
                0
                SECTION
                2
                BLOCKS
                0
                BLOCK
                2
                PLATTE
                10
                -1000
                20
                -1000
                0
                LWPOLYLINE
                8
                PLATTEN
                10
                100
                20
                100
                10
                700
                20
                100
                10
                700
                20
                400
                10
                100
                20
                400
                0
                ENDBLK
                0
                BLOCK
                2
                EINSATZ
                0
                INSERT
                2
                PLATTE
                10
                0
                20
                0
                0
                ENDBLK
                0
                ENDSEC
                0
                EOF
                """);

        DwgLibraryAnalysis analysis = reader.read(dxfFile, sourceFile, "Testkonverter", List.of());

        DwgBlockDefinition insert = block(analysis, "EINSATZ");
        assertEquals(600.0, insert.widthMillimeters(), 0.001);
        assertEquals(300.0, insert.heightMillimeters(), 0.001);
    }

    @Test
    void dokumentiertEinheitenloseDateienAlsMillimeterAnnahme() throws Exception {
        Path dxfFile = tempDir.resolve("einheitenlos.dxf");
        Path sourceFile = tempDir.resolve("einheitenlos.dwg");
        Files.writeString(dxfFile, """
                0
                SECTION
                2
                ENTITIES
                0
                LINE
                8
                MODELL
                10
                0
                20
                0
                11
                42
                21
                24
                0
                ENDSEC
                0
                EOF
                """);

        DwgLibraryAnalysis analysis = reader.read(dxfFile, sourceFile, "Testkonverter", List.of());

        assertEquals(DwgUnit.UNITLESS, analysis.unit());
        assertTrue(analysis.messages().stream().anyMatch(message -> message.contains("als Millimeter interpretiert")));
        assertEquals(42.0, block(analysis, "Modellbereich").widthMillimeters(), 0.001);
    }

    private DwgBlockDefinition block(DwgLibraryAnalysis analysis, String name) {
        return analysis.blocks().stream()
                .filter(block -> block.name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
