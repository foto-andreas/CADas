package de.schrell.cadas.testsupport;

public final class Dxf3dTestFixtures {

    private Dxf3dTestFixtures() {
    }

    public static String simpleSolidDxf() {
        String sat = """
                20800 33 2 0
                16 Autodesk AutoCAD 20 ASM 227.0.0.65535 NT 0
                1 9.999999999999999547e-07 1.000000000000000036e-10
                asmheader $-1 -1 @13 227.0.0.65535 #
                body $-1 -1 $-1 $2 $-1 $3 #
                lump $-1 -1 $-1 $-1 $4 $1 #
                transform $-1 -1 1 0 0 0 1 0 0 0 1 0 0 0 1 no_rotate no_reflect no_shear #
                shell $-1 -1 $-1 $-1 $-1 $5 $-1 $2 #
                face $6 -1 $-1 $7 $8 $4 $-1 $9 forward single #
                persubent-acadSolidHistory-attrib $-1 -1 $-1 $-1 $5 1 1 1000000000 1001 #
                face $10 -1 $-1 $11 $12 $4 $-1 $13 forward single #
                loop $-1 -1 $-1 $-1 $14 $5 #
                cone-surface $-1 -1 $-1 0 0 0 0 0 1 -10 0 0 1 I I 0 1 10 forward I I I I #
                persubent-acadSolidHistory-attrib $-1 -1 $-1 $-1 $7 1 1 1000000000 1002 #
                face $15 -1 $-1 $-1 $16 $4 $-1 $17 reversed single #
                loop $-1 -1 $-1 $-1 $18 $7 #
                plane-surface $-1 -1 $-1 0 0 20 0 0 1 1 0 0 forward_v I I I I #
                coedge $-1 -1 $-1 $19 $20 $21 $22 forward $8 $-1 #
                persubent-acadSolidHistory-attrib $-1 -1 $-1 $-1 $11 1 1 1000000000 1003 #
                loop $-1 -1 $-1 $-1 $21 $11 #
                plane-surface $-1 -1 $-1 0 0 0 0 0 1 1 0 0 forward_v I I I I #
                coedge $-1 -1 $-1 $18 $18 $23 $24 forward $12 $-1 #
                coedge $-1 -1 $-1 $23 $14 $20 $25 forward $8 $-1 #
                coedge $-1 -1 $-1 $14 $23 $19 $25 reversed $8 $-1 #
                coedge $-1 -1 $-1 $21 $21 $14 $22 reversed $16 $-1 #
                edge $-1 -1 $-1 $26 -3.141592653589793 $26 3.141592653589793 $14 $27 forward @7 unknown #
                coedge $-1 -1 $-1 $20 $19 $18 $24 reversed $8 $-1 #
                edge $-1 -1 $-1 $28 -3.141592653589793 $28 3.141592653589793 $23 $29 forward @7 unknown #
                edge $-1 -1 $-1 $26 0 $28 20 $19 $30 forward @7 tangent #
                vertex $-1 -1 $-1 $22 $31 #
                ellipse-curve $-1 -1 $-1 0 0 0 0 0 1 -10 0 0 1 I I #
                vertex $-1 -1 $-1 $25 $32 #
                ellipse-curve $-1 -1 $-1 0 0 20 0 0 1 -10 0 0 1 I I #
                straight-curve $-1 -1 $-1 10 0 0 0 0 1 I I #
                point $-1 -1 $-1 10 0 0 #
                point $-1 -1 $-1 10 0 20 #
                """;
        return dxfWithSat(sat);
    }

    public static String boundsOnlySolidDxf() {
        return dxfWithSat("""
                transform $-1 -1 1 0 0 0 1 0 0 0 1 0 0 0 1 no_rotate no_reflect no_shear #
                ellipse-curve $-1 -1 $-1 0 0 0 0 0 1 10 0 0 1 I I #
                ellipse-curve $-1 -1 $-1 0 0 20 0 0 1 10 0 0 1 I I #
                point $-1 -1 $-1 10 0 0 #
                point $-1 -1 $-1 10 0 20 #
                """);
    }

    private static String dxfWithSat(String sat) {
        StringBuilder dxf = new StringBuilder("""
                0
                SECTION
                2
                HEADER
                9
                $INSUNITS
                70
                4
                9
                $EXTMIN
                10
                -10
                20
                -10
                30
                0
                9
                $EXTMAX
                10
                10
                20
                10
                30
                20
                0
                ENDSEC
                0
                SECTION
                2
                ENTITIES
                0
                3DSOLID
                70
                1
                """);
        sat.lines().forEach(line -> dxf.append("1\n").append(encryptSatLine(line)).append('\n'));
        return dxf.append("0\nENDSEC\n0\nEOF\n").toString();
    }

    private static String encryptSatLine(String line) {
        StringBuilder encrypted = new StringBuilder();
        for (char value : line.toCharArray()) {
            if (value == 'A') {
                encrypted.append("^ ");
            } else if (value <= 32) {
                encrypted.append(value);
            } else {
                encrypted.append((char) (159 - value));
            }
        }
        return encrypted.toString();
    }
}
