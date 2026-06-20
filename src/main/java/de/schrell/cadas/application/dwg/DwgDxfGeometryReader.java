package de.schrell.cadas.application.dwg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class DwgDxfGeometryReader {

    private static final String MODEL_BLOCK_NAME = "Modellbereich";

    public DwgLibraryAnalysis read(Path dxfFile, Path sourceFile, String converterName, List<String> conversionMessages) throws IOException {
        List<Pair> pairs = pairs(Files.readAllLines(dxfFile));
        DwgUnit unit = readUnit(pairs);
        Map<String, RawBlock> rawBlocks = parseBlocks(pairs, unit);
        parseModelSpace(pairs, unit).ifPresent(model -> rawBlocks.put(model.name, model));
        List<DwgBlockDefinition> blocks = rawBlocks.values().stream()
                .filter(block -> !block.name.equals("*Model_Space") && !block.name.equals("*Paper_Space"))
                .map(block -> definition(sourceFile, unit, block, rawBlocks))
                .sorted(Comparator.comparing(DwgBlockDefinition::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<String> messages = new ArrayList<>(conversionMessages);
        if (unit.assumed()) {
            messages.add("DWG/DXF enthält keine metrische Einheit; Koordinaten wurden als Millimeter interpretiert.");
        }
        if (blocks.isEmpty()) {
            messages.add("Keine auswertbaren Blöcke oder Modellgeometrien gefunden.");
        }
        return new DwgLibraryAnalysis(sourceFile, true, converterName, unit, blocks, messages);
    }

    private List<Pair> pairs(List<String> lines) {
        List<Pair> pairs = new ArrayList<>();
        for (int index = 0; index + 1 < lines.size(); index += 2) {
            try {
                pairs.add(new Pair(Integer.parseInt(lines.get(index).trim()), lines.get(index + 1).trim()));
            } catch (NumberFormatException ignored) {
                // Kaputte DXF-Paare werden für die Bibliotheksanalyse übersprungen.
            }
        }
        return pairs;
    }

    private DwgUnit readUnit(List<Pair> pairs) {
        for (int index = 0; index < pairs.size() - 1; index++) {
            if (pairs.get(index).code == 9 && pairs.get(index).value.equals("$INSUNITS")) {
                return DwgUnit.fromRawHeaderValue(pairs.get(index + 1).value);
            }
        }
        return DwgUnit.UNITLESS;
    }

    private Map<String, RawBlock> parseBlocks(List<Pair> pairs, DwgUnit unit) {
        Map<String, RawBlock> blocks = new LinkedHashMap<>();
        String section = "";
        int index = 0;
        while (index < pairs.size()) {
            Pair pair = pairs.get(index);
            if (pair.isType("SECTION") && index + 1 < pairs.size() && pairs.get(index + 1).code == 2) {
                section = pairs.get(index + 1).value;
                index += 2;
                continue;
            }
            if (pair.isType("ENDSEC")) {
                section = "";
                index++;
                continue;
            }
            if ("BLOCKS".equals(section) && pair.isType("BLOCK")) {
                ParsedBlock parsedBlock = parseBlock(pairs, index, unit);
                blocks.put(parsedBlock.block().name, parsedBlock.block());
                index = parsedBlock.nextIndex();
                continue;
            }
            index++;
        }
        return blocks;
    }

    private Optional<RawBlock> parseModelSpace(List<Pair> pairs, DwgUnit unit) {
        RawBlock model = new RawBlock(MODEL_BLOCK_NAME, 0.0, 0.0);
        String section = "";
        int index = 0;
        while (index < pairs.size()) {
            Pair pair = pairs.get(index);
            if (pair.isType("SECTION") && index + 1 < pairs.size() && pairs.get(index + 1).code == 2) {
                section = pairs.get(index + 1).value;
                index += 2;
                continue;
            }
            if (pair.isType("ENDSEC")) {
                section = "";
                index++;
                continue;
            }
            if ("ENTITIES".equals(section) && pair.code == 0) {
                index = parseEntityIntoBlock(pairs, index, unit, model);
                continue;
            }
            index++;
        }
        return model.entityCount > 0 || model.bounds != null ? Optional.of(model) : Optional.empty();
    }

    private ParsedBlock parseBlock(List<Pair> pairs, int startIndex, DwgUnit unit) {
        int index = startIndex + 1;
        String name = "";
        double originX = 0.0;
        double originY = 0.0;
        String blockHandle = "";
        while (index < pairs.size() && pairs.get(index).code != 0) {
            Pair pair = pairs.get(index);
            if (pair.code == 2 && name.isBlank()) {
                name = pair.value;
            } else if (pair.code == 10) {
                originX = parseDouble(pair.value).orElse(0.0) * unit.millimetersPerDrawingUnit();
            } else if (pair.code == 20) {
                originY = parseDouble(pair.value).orElse(0.0) * unit.millimetersPerDrawingUnit();
            } else if (pair.code == 5) {
                blockHandle = pair.value;
            }
            index++;
        }
        RawBlock block = new RawBlock(name.isBlank() ? "Block" : name, originX, originY);
        if (!blockHandle.isBlank()) {
            block.handles.add(blockHandle);
        }
        while (index < pairs.size()) {
            Pair pair = pairs.get(index);
            if (pair.isType("ENDBLK")) {
                index++;
                while (index < pairs.size() && pairs.get(index).code != 0) {
                    Pair endPair = pairs.get(index);
                    if (endPair.code == 5) {
                        block.handles.add(endPair.value);
                    }
                    index++;
                }
                break;
            }
            if (pair.code == 0) {
                index = parseEntityIntoBlock(pairs, index, unit, block);
            } else {
                index++;
            }
        }
        return new ParsedBlock(block, index);
    }

    private int parseEntityIntoBlock(List<Pair> pairs, int startIndex, DwgUnit unit, RawBlock block) {
        String type = pairs.get(startIndex).value.toUpperCase(Locale.ROOT);
        if (type.equals("ENDSEC") || type.equals("ENDBLK") || type.equals("SECTION")) {
            return startIndex + 1;
        }
        List<Pair> entityPairs = new ArrayList<>();
        int index = startIndex + 1;
        while (index < pairs.size() && pairs.get(index).code != 0) {
            entityPairs.add(pairs.get(index));
            index++;
        }
        applyEntity(type, entityPairs, unit, block);
        return index;
    }

    private void applyEntity(String type, List<Pair> entityPairs, DwgUnit unit, RawBlock block) {
        block.entityCount++;
        first(entityPairs, 5).ifPresent(block.handles::add);
        first(entityPairs, 8).filter(layer -> !layer.isBlank()).ifPresent(block.layers::add);
        switch (type) {
            case "LINE" -> applyLine(entityPairs, unit, block);
            case "LWPOLYLINE", "VERTEX", "POINT" -> applyPointPairs(entityPairs, unit, block);
            case "CIRCLE", "ARC" -> applyCircle(entityPairs, unit, block);
            case "ELLIPSE" -> applyEllipse(entityPairs, unit, block);
            case "INSERT" -> applyInsert(entityPairs, unit, block);
            default -> block.unsupportedEntityCount++;
        }
    }

    private void applyLine(List<Pair> entityPairs, DwgUnit unit, RawBlock block) {
        Optional<Double> x1 = firstDouble(entityPairs, 10);
        Optional<Double> y1 = firstDouble(entityPairs, 20);
        Optional<Double> x2 = firstDouble(entityPairs, 11);
        Optional<Double> y2 = firstDouble(entityPairs, 21);
        if (x1.isPresent() && y1.isPresent() && x2.isPresent() && y2.isPresent()) {
            includePoint(block, x1.get(), y1.get(), unit);
            includePoint(block, x2.get(), y2.get(), unit);
        }
    }

    private void applyPointPairs(List<Pair> entityPairs, DwgUnit unit, RawBlock block) {
        List<String> xValues = values(entityPairs, 10);
        List<String> yValues = values(entityPairs, 20);
        int count = Math.min(xValues.size(), yValues.size());
        for (int index = 0; index < count; index++) {
            Optional<Double> x = parseDouble(xValues.get(index));
            Optional<Double> y = parseDouble(yValues.get(index));
            if (x.isPresent() && y.isPresent()) {
                includePoint(block, x.get(), y.get(), unit);
            }
        }
    }

    private void applyCircle(List<Pair> entityPairs, DwgUnit unit, RawBlock block) {
        Optional<Double> x = firstDouble(entityPairs, 10);
        Optional<Double> y = firstDouble(entityPairs, 20);
        Optional<Double> radius = firstDouble(entityPairs, 40);
        if (x.isPresent() && y.isPresent() && radius.isPresent()) {
            includeBounds(block, x.get() - radius.get(), y.get() - radius.get(), x.get() + radius.get(), y.get() + radius.get(), unit);
        }
    }

    private void applyEllipse(List<Pair> entityPairs, DwgUnit unit, RawBlock block) {
        Optional<Double> x = firstDouble(entityPairs, 10);
        Optional<Double> y = firstDouble(entityPairs, 20);
        Optional<Double> majorX = firstDouble(entityPairs, 11);
        Optional<Double> majorY = firstDouble(entityPairs, 21);
        Optional<Double> ratio = firstDouble(entityPairs, 40);
        if (x.isPresent() && y.isPresent() && majorX.isPresent() && majorY.isPresent()) {
            double majorRadius = Math.hypot(majorX.get(), majorY.get());
            double minorRadius = majorRadius * ratio.orElse(1.0);
            includeBounds(block, x.get() - majorRadius, y.get() - minorRadius, x.get() + majorRadius, y.get() + minorRadius, unit);
        }
    }

    private void applyInsert(List<Pair> entityPairs, DwgUnit unit, RawBlock block) {
        String blockName = first(entityPairs, 2).orElse("");
        Optional<Double> x = firstDouble(entityPairs, 10);
        Optional<Double> y = firstDouble(entityPairs, 20);
        if (blockName.isBlank() || x.isEmpty() || y.isEmpty()) {
            block.unsupportedEntityCount++;
            return;
        }
        double factor = unit.millimetersPerDrawingUnit();
        DwgInsertReference insert = new DwgInsertReference(
                blockName,
                x.get() * factor,
                y.get() * factor,
                firstDouble(entityPairs, 41).orElse(1.0),
                firstDouble(entityPairs, 42).orElse(1.0),
                firstDouble(entityPairs, 50).orElse(0.0)
        );
        block.inserts.add(insert);
    }

    private DwgBlockDefinition definition(Path sourceFile, DwgUnit unit, RawBlock block, Map<String, RawBlock> rawBlocks) {
        DwgBounds resolvedBounds = resolveBounds(block.name, rawBlocks, new LinkedHashSet<>()).orElse(null);
        List<String> warnings = new ArrayList<>();
        if (block.unsupportedEntityCount > 0) {
            warnings.add(block.unsupportedEntityCount + " nicht direkt auswertbare DXF-Elemente.");
        }
        if (resolvedBounds == null) {
            warnings.add("Keine 2D-Grenzen aus LINE, LWPOLYLINE, CIRCLE, ARC, ELLIPSE, POINT, VERTEX oder INSERT ableitbar.");
        }
        return new DwgBlockDefinition(
                sourceFile,
                block.name,
                unit,
                block.originXMillimeters,
                block.originYMillimeters,
                resolvedBounds,
                List.copyOf(block.layers),
                List.copyOf(block.handles),
                List.copyOf(block.inserts),
                block.entityCount,
                block.unsupportedEntityCount,
                warnings
        );
    }

    private Optional<DwgBounds> resolveBounds(String blockName, Map<String, RawBlock> rawBlocks, Set<String> stack) {
        RawBlock block = rawBlocks.get(blockName);
        if (block == null || stack.contains(blockName)) {
            return Optional.empty();
        }
        stack.add(blockName);
        DwgBounds bounds = block.bounds;
        for (DwgInsertReference insert : block.inserts) {
            RawBlock referencedBlock = rawBlocks.get(insert.blockName());
            if (referencedBlock == null) {
                continue;
            }
            Optional<DwgBounds> referencedBounds = resolveBounds(insert.blockName(), rawBlocks, stack);
            if (referencedBounds.isEmpty()) {
                continue;
            }
            DwgBounds transformed = transformBounds(referencedBounds.get(), referencedBlock, insert);
            bounds = union(bounds, transformed);
        }
        stack.remove(blockName);
        return Optional.ofNullable(bounds);
    }

    private DwgBounds transformBounds(DwgBounds bounds, RawBlock referencedBlock, DwgInsertReference insert) {
        double[][] corners = {
                {bounds.minXMillimeters(), bounds.minYMillimeters()},
                {bounds.minXMillimeters(), bounds.maxYMillimeters()},
                {bounds.maxXMillimeters(), bounds.minYMillimeters()},
                {bounds.maxXMillimeters(), bounds.maxYMillimeters()}
        };
        DwgBounds transformedBounds = null;
        double rotation = Math.toRadians(insert.rotationDegrees());
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        for (double[] corner : corners) {
            double localX = (corner[0] - referencedBlock.originXMillimeters) * insert.scaleX();
            double localY = (corner[1] - referencedBlock.originYMillimeters) * insert.scaleY();
            double rotatedX = localX * cos - localY * sin;
            double rotatedY = localX * sin + localY * cos;
            transformedBounds = union(transformedBounds, DwgBounds.point(insert.xMillimeters() + rotatedX, insert.yMillimeters() + rotatedY));
        }
        return transformedBounds;
    }

    private void includePoint(RawBlock block, double xDrawingUnits, double yDrawingUnits, DwgUnit unit) {
        double factor = unit.millimetersPerDrawingUnit();
        block.bounds = union(block.bounds, DwgBounds.point(xDrawingUnits * factor, yDrawingUnits * factor));
    }

    private void includeBounds(RawBlock block, double minX, double minY, double maxX, double maxY, DwgUnit unit) {
        DwgBounds.of(minX, minY, maxX, maxY, unit).ifPresent(bounds -> block.bounds = union(block.bounds, bounds));
    }

    private DwgBounds union(DwgBounds first, DwgBounds second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.include(second);
    }

    private Optional<String> first(List<Pair> pairs, int code) {
        return pairs.stream()
                .filter(pair -> pair.code == code)
                .map(Pair::value)
                .findFirst();
    }

    private List<String> values(List<Pair> pairs, int code) {
        return pairs.stream()
                .filter(pair -> pair.code == code)
                .map(Pair::value)
                .toList();
    }

    private Optional<Double> firstDouble(List<Pair> pairs, int code) {
        return first(pairs, code).flatMap(this::parseDouble);
    }

    private Optional<Double> parseDouble(String value) {
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private record Pair(int code, String value) {
        boolean isType(String expectedType) {
            return code == 0 && value.equalsIgnoreCase(expectedType);
        }
    }

    private record ParsedBlock(RawBlock block, int nextIndex) {
    }

    private static final class RawBlock {
        private final String name;
        private final double originXMillimeters;
        private final double originYMillimeters;
        private final Set<String> layers = new LinkedHashSet<>();
        private final Set<String> handles = new LinkedHashSet<>();
        private final List<DwgInsertReference> inserts = new ArrayList<>();
        private DwgBounds bounds;
        private int entityCount;
        private int unsupportedEntityCount;

        private RawBlock(String name, double originXMillimeters, double originYMillimeters) {
            this.name = name;
            this.originXMillimeters = originXMillimeters;
            this.originYMillimeters = originYMillimeters;
        }
    }
}
