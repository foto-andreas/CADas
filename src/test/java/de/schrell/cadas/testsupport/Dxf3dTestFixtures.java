package de.schrell.cadas.testsupport;

public final class Dxf3dTestFixtures {

    private Dxf3dTestFixtures() {
    }

    public static String simpleSolidDxf() {
        String sat = """
                transform $-1 -1 1 0 0 0 1 0 0 0 1 0 0 0 1 no_rotate no_reflect no_shear #
                ellipse-curve $-1 -1 $-1 0 0 0 0 0 1 10 0 0 1 I I #
                ellipse-curve $-1 -1 $-1 0 0 20 0 0 1 10 0 0 1 I I #
                point $-1 -1 $-1 10 0 0 #
                point $-1 -1 $-1 10 0 20 #
                """;
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
