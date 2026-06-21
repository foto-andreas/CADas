package de.schrell.cadas.application.objects;

import static de.schrell.cadas.testsupport.Dxf3dTestFixtures.simpleSolidDxf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.dwg.DwgBlockDefinition;
import de.schrell.cadas.application.dwg.DwgBounds;
import de.schrell.cadas.application.dwg.DwgConversionAvailability;
import de.schrell.cadas.application.dwg.DwgConversionResult;
import de.schrell.cadas.application.dwg.DwgLibraryAnalyzer;
import de.schrell.cadas.application.dwg.DwgToDxfConverter;
import de.schrell.cadas.application.dwg.DwgUnit;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RoomObjectPresetServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void liefertFachlicheStandardobjekte() {
        RoomObjectPresetService service = new RoomObjectPresetService(tempDir);

        var presets = service.defaults();

        assertTrue(presets.stream().anyMatch(preset -> preset.type() == RoomObjectType.TOILET));
        assertTrue(presets.stream().anyMatch(preset -> preset.shape() == RoomObjectShape.HALF_ROUND));
        assertTrue(presets.stream().anyMatch(RoomObjectPreset::cutsFloorCovering));
        RoomObjectPreset cuboid = presets.stream().filter(preset -> preset.type() == RoomObjectType.CUBOID).findFirst().orElseThrow();
        assertEquals("custom-cuboid", cuboid.id());
        assertEquals(1000.0, cuboid.width().toMillimeters(), 0.001);
    }

    @Test
    void importiertDwgDateienAusObjektverzeichnisAlsPresets() throws Exception {
        Files.writeString(tempDir.resolve("Toilette.dwg"), "DWG");
        Files.writeString(tempDir.resolve("Notiz.txt"), "kein Objekt");
        RoomObjectPresetService service = new RoomObjectPresetService(tempDir, new DwgLibraryAnalyzer(new UnavailableConverter()));

        var presets = service.loadDwgPresets();

        assertEquals(1, presets.size());
        assertEquals("DWG-Objekt: Toilette", presets.getFirst().name());
        assertEquals(RoomObjectType.DWG_REFERENCE, presets.getFirst().type());
        assertTrue(presets.getFirst().source().endsWith("Toilette.dwg"));
    }

    @Test
    void leitetObjektPresetAusDwgBlockFootprintAb() {
        DwgBlockDefinition block = new DwgBlockDefinition(
                tempDir.resolve("Sanitär.dwg"),
                "Waschbecken",
                DwgUnit.MILLIMETER,
                0.0,
                0.0,
                new DwgBounds(-300.0, -250.0, 300.0, 250.0),
                List.of("Sanitär"),
                List.of("F1"),
                List.of(),
                1,
                0,
                List.of()
        );
        RoomObjectPresetService service = new RoomObjectPresetService(tempDir, new DwgLibraryAnalyzer(new UnavailableConverter()));

        RoomObjectPreset preset = service.fromDwgBlock(block, true);

        assertEquals("DWG-Objekt: Waschbecken", preset.name());
        assertEquals(RoomObjectType.DWG_REFERENCE, preset.type());
        assertEquals(RoomObjectShape.RECTANGLE, preset.shape());
        assertEquals(600.0, preset.width().toMillimeters(), 0.001);
        assertEquals(500.0, preset.depth().toMillimeters(), 0.001);
        assertTrue(preset.cutsFloorCovering());
        assertTrue(preset.source().endsWith("Sanitär.dwg#Waschbecken"));
    }

    @Test
    void importiertDreidimensionaleDxfDateiAlsSkalierbaresPreset() throws Exception {
        Path sourceDirectory = tempDir.resolve("Quelle");
        Path objectDirectory = tempDir.resolve("Objekte");
        Files.createDirectories(sourceDirectory);
        Path sourceFile = sourceDirectory.resolve("Wärmepumpe.dxf");
        Files.writeString(sourceFile, simpleSolidDxf());
        RoomObjectPresetService service = new RoomObjectPresetService(objectDirectory, new DwgLibraryAnalyzer(new UnavailableConverter()));

        RoomObjectPreset preset = service.importCad3dObject(sourceFile);

        assertEquals(RoomObjectType.DXF_3D_REFERENCE, preset.type());
        assertEquals(20.0, preset.width().toMillimeters(), 0.001);
        assertEquals(20.0, preset.depth().toMillimeters(), 0.001);
        assertEquals(20.0, preset.height().toMillimeters(), 0.001);
        assertTrue(Files.exists(objectDirectory.resolve("Wärmepumpe.dxf")));
        assertEquals(1, service.loadCad3dPresets().size());
    }

    @Test
    void importiertIfcDateiAlsSkalierbaresPreset() throws Exception {
        Path sourceFile = tempDir.resolve("Wärmepumpe.ifc");
        Files.writeString(sourceFile, """
                ISO-10303-21;
                DATA;
                #1=IFCSIUNIT(*,.LENGTHUNIT.,.MILLI.,.METRE.);
                #2=IFCCARTESIANPOINT((0.,0.,0.));
                #3=IFCCARTESIANPOINT((100.,0.,0.));
                #4=IFCCARTESIANPOINT((100.,50.,0.));
                #5=IFCPOLYLOOP((#2,#3,#4));
                #6=IFCFACEOUTERBOUND(#5,.T.);
                #7=IFCFACE((#6));
                #8=IFCCLOSEDSHELL((#7));
                #9=IFCFACETEDBREP(#8);
                ENDSEC;
                END-ISO-10303-21;
                """);
        Path objectDirectory = tempDir.resolve("Objekte");
        RoomObjectPresetService service = new RoomObjectPresetService(objectDirectory, new DwgLibraryAnalyzer(new UnavailableConverter()));

        RoomObjectPreset preset = service.importCad3dObject(sourceFile);

        assertEquals(RoomObjectType.IFC_3D_REFERENCE, preset.type());
        assertTrue(Files.exists(objectDirectory.resolve("Wärmepumpe.ifc")));
        assertTrue(service.loadCad3dPresets().stream().anyMatch(candidate -> candidate.type() == RoomObjectType.IFC_3D_REFERENCE));
    }

    @Test
    void importiertRfaMitBegleitgeometrieAlsEinPreset() throws Exception {
        Path sourceDirectory = tempDir.resolve("RfaQuelle");
        Path objectDirectory = tempDir.resolve("RfaObjekte");
        Files.createDirectories(sourceDirectory);
        Path rfa = sourceDirectory.resolve("Wärmepumpe.rfa");
        Files.writeString(rfa, "RFA");
        Files.writeString(sourceDirectory.resolve("Wärmepumpe.dxf"), simpleSolidDxf());
        RoomObjectPresetService service = new RoomObjectPresetService(objectDirectory, new DwgLibraryAnalyzer(new UnavailableConverter()));

        RoomObjectPreset preset = service.importCad3dObject(rfa);

        assertEquals(RoomObjectType.RFA_3D_REFERENCE, preset.type());
        assertEquals(20.0, preset.height().toMillimeters(), 0.001);
        assertTrue(Files.exists(objectDirectory.resolve("Wärmepumpe.rfa")));
        assertTrue(Files.exists(objectDirectory.resolve("Wärmepumpe.dxf")));
        assertEquals(1, service.loadCad3dPresets().size());
    }

    private static final class UnavailableConverter implements DwgToDxfConverter {

        @Override
        public DwgConversionAvailability availability() {
            return DwgConversionAvailability.unavailable("kein Testkonverter");
        }

        @Override
        public DwgConversionResult convert(Path dwgFile, Path targetDxfFile) throws IOException {
            throw new IOException("nicht verfügbar");
        }
    }
}
