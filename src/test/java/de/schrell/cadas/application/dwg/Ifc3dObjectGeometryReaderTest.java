package de.schrell.cadas.application.dwg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ifc3dObjectGeometryReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void liestBrepExtrusionUndOberflächenfarbe() throws Exception {
        Path file = tempDir.resolve("objekt.ifc");
        Files.writeString(file, """
                ISO-10303-21;
                DATA;
                #1=IFCSIUNIT(*,.LENGTHUNIT.,.MILLI.,.METRE.);
                #2=IFCCARTESIANPOINT((0.,0.,0.));
                #3=IFCCARTESIANPOINT((10.,0.,0.));
                #4=IFCCARTESIANPOINT((10.,10.,0.));
                #5=IFCCARTESIANPOINT((0.,10.,0.));
                #6=IFCPOLYLOOP((#2,#3,#4,#5));
                #7=IFCFACEOUTERBOUND(#6,.T.);
                #8=IFCFACE((#7));
                #9=IFCCLOSEDSHELL((#8));
                #10=IFCFACETEDBREP(#9);
                #20=IFCCOLOURRGB($,0.0705882353,0.2039215686,0.3372549020);
                #21=IFCSURFACESTYLERENDERING(#20,0.,$,$,$,$,$,$,.NOTDEFINED.);
                #22=IFCSURFACESTYLE('Farbe',.BOTH.,(#21));
                #23=IFCPRESENTATIONSTYLEASSIGNMENT((#22));
                #24=IFCSTYLEDITEM(#10,(#23),$);
                #30=IFCCARTESIANPOINT((20.,0.,0.));
                #31=IFCCARTESIANPOINT((25.,0.,0.));
                #32=IFCCARTESIANPOINT((25.,5.,0.));
                #33=IFCCARTESIANPOINT((20.,5.,0.));
                #34=IFCPOLYLINE((#30,#31,#32,#33,#30));
                #35=IFCARBITRARYCLOSEDPROFILEDEF(.AREA.,'Profil',#34);
                #36=IFCCARTESIANPOINT((0.,0.,0.));
                #37=IFCDIRECTION((0.,0.,1.));
                #38=IFCDIRECTION((1.,0.,0.));
                #39=IFCAXIS2PLACEMENT3D(#36,#37,#38);
                #40=IFCEXTRUDEDAREASOLID(#35,#39,#37,5.);
                ENDSEC;
                END-ISO-10303-21;
                """);

        Dxf3dObjectGeometry geometry = new Ifc3dObjectGeometryReader().read(file);

        assertEquals(2, geometry.solidMeshes().size());
        assertEquals("color:#123456", geometry.solidMeshes().getFirst().materialKey());
        assertTrue(geometry.solidMeshes().stream().mapToInt(Dxf3dMesh::triangleCount).sum() >= 14);
        assertEquals(25.0, geometry.bounds().maxXMillimeters(), 0.001);
        assertEquals(5.0, geometry.bounds().maxZMillimeters(), 0.001);
    }

    @Test
    void erhaeltKonkaveExtrusionskonturStattIhrerKonvexenHuelle() throws Exception {
        Path file = tempDir.resolve("konkav.ifc");
        Files.writeString(file, extrusionIfc("""
                #10=IFCCARTESIANPOINT((0.,0.));
                #11=IFCCARTESIANPOINT((2.,0.));
                #12=IFCCARTESIANPOINT((2.,1.));
                #13=IFCCARTESIANPOINT((1.,1.));
                #14=IFCCARTESIANPOINT((1.,2.));
                #15=IFCCARTESIANPOINT((0.,2.));
                #16=IFCPOLYLINE((#10,#11,#12,#13,#14,#15,#10));
                #17=IFCARBITRARYCLOSEDPROFILEDEF(.AREA.,'L-Profil',#16);
                """, 17));

        Dxf3dMesh mesh = new Ifc3dObjectGeometryReader().read(file).solidMeshes().getFirst();

        assertEquals(6.0, projectedHorizontalTriangleArea(mesh), 0.000_001);
    }

    @Test
    void schneidetInnenlochAusFacetedBrepFlaecheAus() throws Exception {
        Path file = tempDir.resolve("brep-loch.ifc");
        Files.writeString(file, """
                ISO-10303-21;
                DATA;
                #1=IFCSIUNIT(*,.LENGTHUNIT.,.MILLI.,.METRE.);
                #2=IFCCARTESIANPOINT((0.,0.,0.));
                #3=IFCCARTESIANPOINT((4.,0.,0.));
                #4=IFCCARTESIANPOINT((4.,4.,0.));
                #5=IFCCARTESIANPOINT((0.,4.,0.));
                #6=IFCPOLYLOOP((#2,#3,#4,#5));
                #7=IFCFACEOUTERBOUND(#6,.T.);
                #8=IFCCARTESIANPOINT((1.,1.,0.));
                #9=IFCCARTESIANPOINT((1.,3.,0.));
                #10=IFCCARTESIANPOINT((3.,3.,0.));
                #11=IFCCARTESIANPOINT((3.,1.,0.));
                #12=IFCPOLYLOOP((#8,#9,#10,#11));
                #13=IFCFACEBOUND(#12,.T.);
                #14=IFCFACE((#7,#13));
                #15=IFCCLOSEDSHELL((#14));
                #16=IFCFACETEDBREP(#15);
                ENDSEC;
                END-ISO-10303-21;
                """);

        Dxf3dMesh mesh = new Ifc3dObjectGeometryReader().read(file).solidMeshes().getFirst();

        assertEquals(12.0, projectedHorizontalTriangleArea(mesh), 0.000_001);
    }

    @Test
    void erzeugtBeiHohlkreisprofilFreieDeckelUndInnenmantel() throws Exception {
        Path file = tempDir.resolve("hohlkreis.ifc");
        Files.writeString(file, extrusionIfc("""
                #10=IFCCARTESIANPOINT((0.,0.));
                #11=IFCAXIS2PLACEMENT2D(#10,$);
                #12=IFCCIRCLEHOLLOWPROFILEDEF(.AREA.,'Rohr',#11,2.,1.);
                """, 12));

        Dxf3dMesh mesh = new Ifc3dObjectGeometryReader().read(file).solidMeshes().getFirst();
        double[] coordinates = mesh.triangleCoordinates();
        for (int index = 0; index < coordinates.length; index += 9) {
            if (coordinates[index + 2] != coordinates[index + 5]
                    || coordinates[index + 2] != coordinates[index + 8]) {
                continue;
            }
            double centerX = (coordinates[index] + coordinates[index + 3] + coordinates[index + 6]) / 3.0;
            double centerY = (coordinates[index + 1] + coordinates[index + 4] + coordinates[index + 7]) / 3.0;
            assertTrue(Math.hypot(centerX, centerY) >= 1.0 - 0.000_001);
        }
        assertTrue(mesh.triangleCount() > 128, "Innenmantel und gelochte Deckel müssen zusätzliche Dreiecke erzeugen.");
    }

    private String extrusionIfc(String profileEntities, int profileId) {
        return """
                ISO-10303-21;
                DATA;
                #1=IFCSIUNIT(*,.LENGTHUNIT.,.MILLI.,.METRE.);
                %s
                #30=IFCCARTESIANPOINT((0.,0.,0.));
                #31=IFCDIRECTION((0.,0.,1.));
                #32=IFCDIRECTION((1.,0.,0.));
                #33=IFCAXIS2PLACEMENT3D(#30,#31,#32);
                #34=IFCEXTRUDEDAREASOLID(#%d,#33,#31,1.);
                ENDSEC;
                END-ISO-10303-21;
                """.formatted(profileEntities, profileId);
    }

    private double projectedHorizontalTriangleArea(Dxf3dMesh mesh) {
        double area = 0.0;
        double[] coordinates = mesh.triangleCoordinates();
        for (int index = 0; index < coordinates.length; index += 9) {
            double x1 = coordinates[index];
            double y1 = coordinates[index + 1];
            double x2 = coordinates[index + 3];
            double y2 = coordinates[index + 4];
            double x3 = coordinates[index + 6];
            double y3 = coordinates[index + 7];
            area += Math.abs((x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1)) / 2.0;
        }
        return area;
    }
}
