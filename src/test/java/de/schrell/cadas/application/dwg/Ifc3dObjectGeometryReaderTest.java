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
}
