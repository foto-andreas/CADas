package de.andreas.cadas.infrastructure.dxf;

final class DxfDocumentSupport {

    private DxfDocumentSupport() {
    }

    static void appendStandardHeader(StringBuilder dxf) {
        appendPair(dxf, 0, "SECTION");
        appendPair(dxf, 2, "HEADER");
        appendPair(dxf, 9, "$ACADVER");
        appendPair(dxf, 1, "AC1015");
        appendPair(dxf, 9, "$INSUNITS");
        appendPair(dxf, 70, 4);
        appendPair(dxf, 9, "$MEASUREMENT");
        appendPair(dxf, 70, 1);
        appendPair(dxf, 0, "ENDSEC");
        appendPair(dxf, 0, "SECTION");
        appendPair(dxf, 2, "ENTITIES");
    }

    static void appendModelSpace(StringBuilder dxf, String layer) {
        appendPair(dxf, 8, layer);
        appendPair(dxf, 67, 0);
        appendPair(dxf, 410, "Model");
    }

    static void appendPair(StringBuilder dxf, int code, Object value) {
        dxf.append(code).append('\n').append(value).append('\n');
    }
}
