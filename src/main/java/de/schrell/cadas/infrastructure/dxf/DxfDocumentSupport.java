package de.schrell.cadas.infrastructure.dxf;

import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class DxfDocumentSupport {

    static final String BLOCK_DOOR = "CADAS_DOOR";
    static final String BLOCK_WINDOW = "CADAS_WINDOW";
    static final String BLOCK_STAIR = "CADAS_STAIR";

    private DxfDocumentSupport() {
    }

    static DxfWriteContext startDocument(StringBuilder dxf, Collection<String> layers, Collection<String> blockNames) {
        DxfWriteContext context = new DxfWriteContext(dxf);
        context.registerLayer("0");
        layers.forEach(context::registerLayer);
        context.registerBlockRecord("*Model_Space");
        context.registerBlockRecord("*Paper_Space");
        blockNames.forEach(context::registerBlockRecord);

        appendHeader(context);
        appendTables(context);
        appendBlocks(context);
        appendPair(dxf, 0, "SECTION");
        appendPair(dxf, 2, "ENTITIES");
        return context;
    }

    static void finishDocument(DxfWriteContext context) {
        appendPair(context.dxf(), 0, "ENDSEC");
        appendObjects(context);
        appendPair(context.dxf(), 0, "EOF");
    }

    static void appendModelSpaceEntityStart(DxfWriteContext context, String type, String layer) {
        appendPair(context.dxf(), 0, type);
        appendPair(context.dxf(), 5, context.nextHandle());
        appendPair(context.dxf(), 330, context.modelSpaceBlockRecordHandle());
        appendPair(context.dxf(), 100, "AcDbEntity");
        appendPair(context.dxf(), 8, layer);
        appendPair(context.dxf(), 67, 0);
        appendPair(context.dxf(), 410, "Model");
    }

    static void appendInsert(StringBuilder dxf, DxfWriteContext context, String layer, String blockName,
                             PlanPoint insertionPoint, double scaleX, double scaleY, double rotationDegrees) {
        appendModelSpaceEntityStart(context, "INSERT", layer);
        appendPair(dxf, 100, "AcDbBlockReference");
        appendPair(dxf, 2, blockName);
        appendPair(dxf, 10, insertionPoint.xMillimeters());
        appendPair(dxf, 20, insertionPoint.yMillimeters());
        appendPair(dxf, 30, 0.0);
        appendPair(dxf, 41, scaleX);
        appendPair(dxf, 42, scaleY);
        appendPair(dxf, 43, 1.0);
        appendPair(dxf, 50, rotationDegrees);
    }

    static void appendPair(StringBuilder dxf, int code, Object value) {
        dxf.append(code).append('\n').append(value).append('\n');
    }

    private static void appendHeader(DxfWriteContext context) {
        StringBuilder dxf = context.dxf();
        appendPair(dxf, 0, "SECTION");
        appendPair(dxf, 2, "HEADER");
        appendPair(dxf, 9, "$ACADVER");
        appendPair(dxf, 1, "AC1015");
        appendPair(dxf, 9, "$INSUNITS");
        appendPair(dxf, 70, 4);
        appendPair(dxf, 9, "$MEASUREMENT");
        appendPair(dxf, 70, 1);
        appendPair(dxf, 9, "$HANDSEED");
        appendPair(dxf, 5, "FFFF");
        appendPair(dxf, 9, "$CLAYER");
        appendPair(dxf, 8, "0");
        appendPair(dxf, 0, "ENDSEC");
    }

    private static void appendTables(DxfWriteContext context) {
        StringBuilder dxf = context.dxf();
        appendPair(dxf, 0, "SECTION");
        appendPair(dxf, 2, "TABLES");

        appendPair(dxf, 0, "TABLE");
        appendPair(dxf, 2, "LTYPE");
        appendPair(dxf, 70, 1);
        appendPair(dxf, 0, "LTYPE");
        appendPair(dxf, 5, context.nextHandle());
        appendPair(dxf, 100, "AcDbSymbolTableRecord");
        appendPair(dxf, 100, "AcDbLinetypeTableRecord");
        appendPair(dxf, 2, "CONTINUOUS");
        appendPair(dxf, 70, 0);
        appendPair(dxf, 3, "Solid line");
        appendPair(dxf, 72, 65);
        appendPair(dxf, 73, 0);
        appendPair(dxf, 40, 0.0);
        appendPair(dxf, 0, "ENDTAB");

        appendPair(dxf, 0, "TABLE");
        appendPair(dxf, 2, "LAYER");
        appendPair(dxf, 70, context.layers().size());
        for (String layer : context.layers()) {
            appendPair(dxf, 0, "LAYER");
            appendPair(dxf, 5, context.nextHandle());
            appendPair(dxf, 100, "AcDbSymbolTableRecord");
            appendPair(dxf, 100, "AcDbLayerTableRecord");
            appendPair(dxf, 2, layer);
            appendPair(dxf, 70, 0);
            appendPair(dxf, 62, layerColor(layer));
            appendPair(dxf, 6, "CONTINUOUS");
        }
        appendPair(dxf, 0, "ENDTAB");

        appendPair(dxf, 0, "TABLE");
        appendPair(dxf, 2, "STYLE");
        appendPair(dxf, 70, 1);
        appendPair(dxf, 0, "STYLE");
        appendPair(dxf, 5, context.nextHandle());
        appendPair(dxf, 100, "AcDbSymbolTableRecord");
        appendPair(dxf, 100, "AcDbTextStyleTableRecord");
        appendPair(dxf, 2, "Standard");
        appendPair(dxf, 70, 0);
        appendPair(dxf, 40, 0.0);
        appendPair(dxf, 41, 1.0);
        appendPair(dxf, 50, 0.0);
        appendPair(dxf, 71, 0);
        appendPair(dxf, 42, 2.5);
        appendPair(dxf, 3, "txt");
        appendPair(dxf, 4, "");
        appendPair(dxf, 0, "ENDTAB");

        appendPair(dxf, 0, "TABLE");
        appendPair(dxf, 2, "BLOCK_RECORD");
        appendPair(dxf, 70, context.blockRecordHandles().size());
        for (Map.Entry<String, String> entry : context.blockRecordHandles().entrySet()) {
            appendPair(dxf, 0, "BLOCK_RECORD");
            appendPair(dxf, 5, entry.getValue());
            appendPair(dxf, 100, "AcDbSymbolTableRecord");
            appendPair(dxf, 100, "AcDbBlockTableRecord");
            appendPair(dxf, 2, entry.getKey());
        }
        appendPair(dxf, 0, "ENDTAB");

        appendPair(dxf, 0, "ENDSEC");
    }

    private static void appendBlocks(DxfWriteContext context) {
        StringBuilder dxf = context.dxf();
        appendPair(dxf, 0, "SECTION");
        appendPair(dxf, 2, "BLOCKS");
        appendEmptyBlock(context, "*Model_Space");
        appendEmptyBlock(context, "*Paper_Space");
        if (context.blockRecordHandles().containsKey(BLOCK_DOOR)) {
            appendDoorBlock(context);
        }
        if (context.blockRecordHandles().containsKey(BLOCK_WINDOW)) {
            appendWindowBlock(context);
        }
        if (context.blockRecordHandles().containsKey(BLOCK_STAIR)) {
            appendStairBlock(context);
        }
        appendPair(dxf, 0, "ENDSEC");
    }

    private static void appendEmptyBlock(DxfWriteContext context, String blockName) {
        StringBuilder dxf = context.dxf();
        appendPair(dxf, 0, "BLOCK");
        appendPair(dxf, 5, context.nextHandle());
        appendPair(dxf, 330, context.blockRecordHandle(blockName));
        appendPair(dxf, 100, "AcDbEntity");
        appendPair(dxf, 8, "0");
        appendPair(dxf, 100, "AcDbBlockBegin");
        appendPair(dxf, 2, blockName);
        appendPair(dxf, 70, 0);
        appendPair(dxf, 10, 0.0);
        appendPair(dxf, 20, 0.0);
        appendPair(dxf, 30, 0.0);
        appendPair(dxf, 3, blockName);
        appendPair(dxf, 1, "");
        appendPair(dxf, 0, "ENDBLK");
        appendPair(dxf, 5, context.nextHandle());
        appendPair(dxf, 330, context.blockRecordHandle(blockName));
        appendPair(dxf, 100, "AcDbEntity");
        appendPair(dxf, 8, "0");
        appendPair(dxf, 100, "AcDbBlockEnd");
    }

    private static void appendDoorBlock(DxfWriteContext context) {
        StringBuilder dxf = context.dxf();
        appendBlockHeader(context, BLOCK_DOOR);
        appendBlockLine(context, new PlanPoint(0, 0), new PlanPoint(1000, 0));
        appendPair(dxf, 0, "ENDBLK");
        appendPair(dxf, 5, context.nextHandle());
        appendPair(dxf, 330, context.blockRecordHandle(BLOCK_DOOR));
        appendPair(dxf, 100, "AcDbEntity");
        appendPair(dxf, 8, "0");
        appendPair(dxf, 100, "AcDbBlockEnd");
    }

    private static void appendWindowBlock(DxfWriteContext context) {
        StringBuilder dxf = context.dxf();
        appendBlockHeader(context, BLOCK_WINDOW);
        appendBlockLine(context, new PlanPoint(0, 0), new PlanPoint(1000, 0));
        appendBlockLine(context, new PlanPoint(250, -60), new PlanPoint(250, 60));
        appendBlockLine(context, new PlanPoint(750, -60), new PlanPoint(750, 60));
        appendPair(dxf, 0, "ENDBLK");
        appendPair(dxf, 5, context.nextHandle());
        appendPair(dxf, 330, context.blockRecordHandle(BLOCK_WINDOW));
        appendPair(dxf, 100, "AcDbEntity");
        appendPair(dxf, 8, "0");
        appendPair(dxf, 100, "AcDbBlockEnd");
    }

    private static void appendStairBlock(DxfWriteContext context) {
        StringBuilder dxf = context.dxf();
        appendBlockHeader(context, BLOCK_STAIR);
        appendBlockPolyline(context, new PlanPoint(0, 0), new PlanPoint(1000, 0), new PlanPoint(1000, 1000), new PlanPoint(0, 1000));
        appendBlockLine(context, new PlanPoint(180, 200), new PlanPoint(820, 200));
        appendBlockLine(context, new PlanPoint(180, 400), new PlanPoint(820, 400));
        appendBlockLine(context, new PlanPoint(180, 600), new PlanPoint(820, 600));
        appendBlockLine(context, new PlanPoint(180, 800), new PlanPoint(820, 800));
        appendPair(dxf, 0, "ENDBLK");
        appendPair(dxf, 5, context.nextHandle());
        appendPair(dxf, 330, context.blockRecordHandle(BLOCK_STAIR));
        appendPair(dxf, 100, "AcDbEntity");
        appendPair(dxf, 8, "0");
        appendPair(dxf, 100, "AcDbBlockEnd");
    }

    private static void appendBlockHeader(DxfWriteContext context, String blockName) {
        StringBuilder dxf = context.dxf();
        appendPair(dxf, 0, "BLOCK");
        appendPair(dxf, 5, context.nextHandle());
        appendPair(dxf, 330, context.blockRecordHandle(blockName));
        appendPair(dxf, 100, "AcDbEntity");
        appendPair(dxf, 8, "0");
        appendPair(dxf, 100, "AcDbBlockBegin");
        appendPair(dxf, 2, blockName);
        appendPair(dxf, 70, 0);
        appendPair(dxf, 10, 0.0);
        appendPair(dxf, 20, 0.0);
        appendPair(dxf, 30, 0.0);
        appendPair(dxf, 3, blockName);
        appendPair(dxf, 1, "");
    }

    private static void appendBlockLine(DxfWriteContext context, PlanPoint start, PlanPoint end) {
        StringBuilder dxf = context.dxf();
        appendPair(dxf, 0, "LINE");
        appendPair(dxf, 5, context.nextHandle());
        appendPair(dxf, 100, "AcDbEntity");
        appendPair(dxf, 8, "0");
        appendPair(dxf, 100, "AcDbLine");
        appendPair(dxf, 10, start.xMillimeters());
        appendPair(dxf, 20, start.yMillimeters());
        appendPair(dxf, 30, 0.0);
        appendPair(dxf, 11, end.xMillimeters());
        appendPair(dxf, 21, end.yMillimeters());
        appendPair(dxf, 31, 0.0);
    }

    private static void appendBlockPolyline(DxfWriteContext context, PlanPoint... points) {
        StringBuilder dxf = context.dxf();
        appendPair(dxf, 0, "LWPOLYLINE");
        appendPair(dxf, 5, context.nextHandle());
        appendPair(dxf, 100, "AcDbEntity");
        appendPair(dxf, 8, "0");
        appendPair(dxf, 100, "AcDbPolyline");
        appendPair(dxf, 90, points.length);
        appendPair(dxf, 70, 1);
        for (PlanPoint point : points) {
            appendPair(dxf, 10, point.xMillimeters());
            appendPair(dxf, 20, point.yMillimeters());
        }
    }

    private static void appendObjects(DxfWriteContext context) {
        StringBuilder dxf = context.dxf();
        appendPair(dxf, 0, "SECTION");
        appendPair(dxf, 2, "OBJECTS");

        appendPair(dxf, 0, "DICTIONARY");
        appendPair(dxf, 5, context.rootDictionaryHandle());
        appendPair(dxf, 330, "0");
        appendPair(dxf, 100, "AcDbDictionary");
        appendPair(dxf, 281, 1);
        appendPair(dxf, 3, "ACAD_GROUP");
        appendPair(dxf, 350, context.groupDictionaryHandle());
        appendPair(dxf, 3, "ACAD_LAYOUT");
        appendPair(dxf, 350, context.layoutDictionaryHandle());

        appendPair(dxf, 0, "DICTIONARY");
        appendPair(dxf, 5, context.groupDictionaryHandle());
        appendPair(dxf, 330, context.rootDictionaryHandle());
        appendPair(dxf, 100, "AcDbDictionary");
        appendPair(dxf, 281, 1);

        appendPair(dxf, 0, "DICTIONARY");
        appendPair(dxf, 5, context.layoutDictionaryHandle());
        appendPair(dxf, 330, context.rootDictionaryHandle());
        appendPair(dxf, 100, "AcDbDictionary");
        appendPair(dxf, 281, 1);
        appendPair(dxf, 3, "Model");
        appendPair(dxf, 350, context.modelLayoutHandle());
        appendPair(dxf, 3, "Layout1");
        appendPair(dxf, 350, context.paperLayoutHandle());

        appendLayout(dxf, context.modelLayoutHandle(), context.modelSpaceBlockRecordHandle(), "Model", true);
        appendLayout(dxf, context.paperLayoutHandle(), context.paperSpaceBlockRecordHandle(), "Layout1", false);

        appendPair(dxf, 0, "ENDSEC");
    }

    private static void appendLayout(StringBuilder dxf, String handle, String blockRecordHandle, String layoutName, boolean modelSpace) {
        appendPair(dxf, 0, "LAYOUT");
        appendPair(dxf, 5, handle);
        appendPair(dxf, 330, blockRecordHandle);
        appendPair(dxf, 100, "AcDbPlotSettings");
        appendPair(dxf, 1, layoutName);
        appendPair(dxf, 100, "AcDbLayout");
        appendPair(dxf, 70, modelSpace ? 1 : 0);
        appendPair(dxf, 71, 0);
        appendPair(dxf, 10, 0.0);
        appendPair(dxf, 20, 0.0);
        appendPair(dxf, 11, 420.0);
        appendPair(dxf, 21, 297.0);
        appendPair(dxf, 12, 0.0);
        appendPair(dxf, 22, 0.0);
        appendPair(dxf, 32, 0.0);
        appendPair(dxf, 14, 420.0);
        appendPair(dxf, 24, 297.0);
        appendPair(dxf, 34, 0.0);
        appendPair(dxf, 15, 0.0);
        appendPair(dxf, 25, 0.0);
        appendPair(dxf, 35, 0.0);
        appendPair(dxf, 16, 1.0);
        appendPair(dxf, 26, 0.0);
        appendPair(dxf, 36, 0.0);
        appendPair(dxf, 17, 0.0);
        appendPair(dxf, 27, 0.0);
        appendPair(dxf, 37, 1.0);
        appendPair(dxf, 76, 0);
        appendPair(dxf, 330, blockRecordHandle);
    }

    private static int layerColor(String layer) {
        String upper = layer.toUpperCase(Locale.ROOT);
        if (upper.contains("WALL")) {
            return 5;
        }
        if (upper.contains("ROOM")) {
            return 30;
        }
        if (upper.contains("DOOR")) {
            return 1;
        }
        if (upper.contains("WINDOW")) {
            return 4;
        }
        if (upper.contains("STAIR")) {
            return 33;
        }
        if (upper.contains("META")) {
            return 8;
        }
        return 7;
    }

    static final class DxfWriteContext {
        private final StringBuilder dxf;
        private final Set<String> layers = new LinkedHashSet<>();
        private final Map<String, String> blockRecordHandles = new LinkedHashMap<>();
        private int nextHandle = 0x100;
        private final String rootDictionaryHandle = nextHandle();
        private final String groupDictionaryHandle = nextHandle();
        private final String layoutDictionaryHandle = nextHandle();
        private final String modelLayoutHandle = nextHandle();
        private final String paperLayoutHandle = nextHandle();

        private DxfWriteContext(StringBuilder dxf) {
            this.dxf = dxf;
        }

        StringBuilder dxf() {
            return dxf;
        }

        String nextHandle() {
            return Integer.toHexString(nextHandle++).toUpperCase(Locale.ROOT);
        }

        void registerLayer(String layer) {
            layers.add(layer);
        }

        Set<String> layers() {
            return layers;
        }

        void registerBlockRecord(String blockName) {
            blockRecordHandles.computeIfAbsent(blockName, ignored -> nextHandle());
        }

        Map<String, String> blockRecordHandles() {
            return blockRecordHandles;
        }

        String blockRecordHandle(String blockName) {
            return blockRecordHandles.get(blockName);
        }

        String modelSpaceBlockRecordHandle() {
            return blockRecordHandle("*Model_Space");
        }

        String paperSpaceBlockRecordHandle() {
            return blockRecordHandle("*Paper_Space");
        }

        String rootDictionaryHandle() {
            return rootDictionaryHandle;
        }

        String groupDictionaryHandle() {
            return groupDictionaryHandle;
        }

        String layoutDictionaryHandle() {
            return layoutDictionaryHandle;
        }

        String modelLayoutHandle() {
            return modelLayoutHandle;
        }

        String paperLayoutHandle() {
            return paperLayoutHandle;
        }
    }
}
