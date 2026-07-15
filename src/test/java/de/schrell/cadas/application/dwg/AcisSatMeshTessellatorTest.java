package de.schrell.cadas.application.dwg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AcisSatMeshTessellatorTest {

    @Test
    void schneidetInnerenAcisLoopAusPlanarerFlaecheAus() {
        String sat = """
                asmheader $-1 -1 @13 227.0.0.65535 #
                face $-1 -1 $-1 $2 $3 $-1 $-1 $4 forward single #
                loop $-1 -1 $-1 $-1 $5 $1 #
                loop $-1 -1 $-1 $-1 $9 $1 #
                plane-surface $-1 -1 $-1 0 0 0 0 0 1 1 0 0 forward_v I I I I #
                coedge $-1 -1 $-1 $6 $8 $13 forward $1 $-1 #
                coedge $-1 -1 $-1 $7 $5 $14 forward $1 $-1 #
                coedge $-1 -1 $-1 $8 $6 $15 forward $1 $-1 #
                coedge $-1 -1 $-1 $5 $7 $16 forward $1 $-1 #
                coedge $-1 -1 $-1 $10 $12 $17 forward $1 $-1 #
                coedge $-1 -1 $-1 $11 $9 $18 forward $1 $-1 #
                coedge $-1 -1 $-1 $12 $10 $19 forward $1 $-1 #
                coedge $-1 -1 $-1 $9 $11 $20 forward $1 $-1 #
                edge $-1 -1 $-1 $21 0 $22 1 $29 forward @7 unknown #
                edge $-1 -1 $-1 $22 0 $23 1 $30 forward @7 unknown #
                edge $-1 -1 $-1 $23 0 $24 1 $31 forward @7 unknown #
                edge $-1 -1 $-1 $24 0 $21 1 $32 forward @7 unknown #
                edge $-1 -1 $-1 $25 0 $26 1 $33 forward @7 unknown #
                edge $-1 -1 $-1 $26 0 $27 1 $34 forward @7 unknown #
                edge $-1 -1 $-1 $27 0 $28 1 $35 forward @7 unknown #
                edge $-1 -1 $-1 $28 0 $25 1 $36 forward @7 unknown #
                vertex $-1 -1 $-1 $13 $37 #
                vertex $-1 -1 $-1 $14 $38 #
                vertex $-1 -1 $-1 $15 $39 #
                vertex $-1 -1 $-1 $16 $40 #
                vertex $-1 -1 $-1 $17 $41 #
                vertex $-1 -1 $-1 $18 $42 #
                vertex $-1 -1 $-1 $19 $43 #
                vertex $-1 -1 $-1 $20 $44 #
                straight-curve $-1 -1 $-1 0 0 0 1 0 0 I I #
                straight-curve $-1 -1 $-1 4 0 0 0 1 0 I I #
                straight-curve $-1 -1 $-1 4 4 0 -1 0 0 I I #
                straight-curve $-1 -1 $-1 0 4 0 0 -1 0 I I #
                straight-curve $-1 -1 $-1 1 1 0 0 1 0 I I #
                straight-curve $-1 -1 $-1 1 3 0 1 0 0 I I #
                straight-curve $-1 -1 $-1 3 3 0 0 -1 0 I I #
                straight-curve $-1 -1 $-1 3 1 0 -1 0 0 I I #
                point $-1 -1 $-1 0 0 0 #
                point $-1 -1 $-1 4 0 0 #
                point $-1 -1 $-1 4 4 0 #
                point $-1 -1 $-1 0 4 0 #
                point $-1 -1 $-1 1 1 0 #
                point $-1 -1 $-1 1 3 0 #
                point $-1 -1 $-1 3 3 0 #
                point $-1 -1 $-1 3 1 0 #
                """;

        Dxf3dMesh mesh = new AcisSatMeshTessellator().tessellate(sat, 0).orElseThrow();

        assertEquals(12.0, projectedArea(mesh), 0.000_001);
    }

    private double projectedArea(Dxf3dMesh mesh) {
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
