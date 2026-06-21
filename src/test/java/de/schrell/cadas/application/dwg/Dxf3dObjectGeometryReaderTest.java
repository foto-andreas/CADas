package de.schrell.cadas.application.dwg;

import static de.schrell.cadas.testsupport.Dxf3dTestFixtures.boundsOnlySolidDxf;
import static de.schrell.cadas.testsupport.Dxf3dTestFixtures.simpleSolidDxf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Dxf3dObjectGeometryReaderTest {

    @TempDir
    Path tempDir;

    private final Dxf3dObjectGeometryReader reader = new Dxf3dObjectGeometryReader();

    @Test
    void liestAcisKoerperMitMetrischenDreidimensionalenGrenzen() throws Exception {
        Path file = tempDir.resolve("zylinder.dxf");
        Files.writeString(file, simpleSolidDxf());

        Dxf3dObjectGeometry geometry = reader.read(file);

        assertEquals(1, geometry.sourceSolidCount());
        assertEquals(20.0, geometry.bounds().widthMillimeters(), 0.001);
        assertEquals(20.0, geometry.bounds().depthMillimeters(), 0.001);
        assertEquals(20.0, geometry.bounds().heightMillimeters(), 0.001);
        assertEquals(1, geometry.solidBounds().size());
        assertEquals(-10.0, geometry.solidBounds().getFirst().minXMillimeters(), 0.001);
        assertEquals(1, geometry.solidMeshes().size());
        assertEquals(62, geometry.solidMeshes().getFirst().triangleCount());
    }

    @Test
    void weistZweidimensionaleDxfDateiAlsObjektquelleZurueck() throws Exception {
        Path file = tempDir.resolve("leer.dxf");
        Files.writeString(file, "0\nSECTION\n2\nENTITIES\n0\nENDSEC\n0\nEOF\n");

        assertThrows(IllegalArgumentException.class, () -> reader.read(file));
    }

    @Test
    void verwendetBeiNichtAufloesbarerSatTopologieEinGefuelltesFallbackNetz() throws Exception {
        Path file = tempDir.resolve("fremdkoerper.dxf");
        Files.writeString(file, boundsOnlySolidDxf());

        Dxf3dObjectGeometry geometry = reader.read(file);

        assertEquals(1, geometry.solidMeshes().size());
        assertEquals(12, geometry.solidMeshes().getFirst().triangleCount());
    }

    @Test
    void übernimmtEchteDxfFarbeInDasNetzmaterial() throws Exception {
        Path file = tempDir.resolve("farbig.dxf");
        Files.writeString(file, simpleSolidDxf().replace("70\n1\n", "420\n1193046\n70\n1\n"));

        Dxf3dObjectGeometry geometry = reader.read(file);

        assertEquals("color:#123456", geometry.solidMeshes().getFirst().materialKey());
    }

}
