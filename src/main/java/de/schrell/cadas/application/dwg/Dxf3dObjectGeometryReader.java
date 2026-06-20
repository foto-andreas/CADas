package de.schrell.cadas.application.dwg;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Dxf3dObjectGeometryReader {

    private static final Charset DXF_CHARSET = Charset.forName("windows-1252");
    private final AcisSatMeshTessellator meshTessellator = new AcisSatMeshTessellator();

    public Dxf3dObjectGeometry read(Path sourceFile) throws IOException {
        List<Pair> pairs = pairs(Files.readAllLines(sourceFile, DXF_CHARSET));
        DwgUnit unit = readUnit(pairs);
        Optional<Dxf3dBounds> headerBounds = readHeaderBounds(pairs);
        List<Dxf3dBounds> rawSolids = new ArrayList<>();
        List<Dxf3dMesh> rawMeshes = new ArrayList<>();
        int sourceSolidCount = 0;
        for (int index = 0; index < pairs.size(); index++) {
            if (!pairs.get(index).isType("3DSOLID")) {
                continue;
            }
            int sourceSolidIndex = sourceSolidCount++;
            List<Pair> entityPairs = new ArrayList<>();
            for (int entityIndex = index + 1; entityIndex < pairs.size() && pairs.get(entityIndex).code() != 0; entityIndex++) {
                entityPairs.add(pairs.get(entityIndex));
            }
            String sat = decryptedSat(entityPairs);
            Optional<Dxf3dBounds> bounds = solidBounds(sat);
            bounds.ifPresent(rawSolids::add);
            meshTessellator.tessellate(sat, sourceSolidIndex)
                    .or(() -> bounds.map(value -> Dxf3dMesh.box(sourceSolidIndex, value)))
                    .ifPresent(rawMeshes::add);
        }
        if (sourceSolidCount == 0) {
            throw new IllegalArgumentException("DXF-Datei enthält keine ACIS-3DSOLID-Körper.");
        }
        Dxf3dBounds drawingBounds = headerBounds.orElseGet(() -> rawSolids.stream()
                .reduce(Dxf3dBounds::include)
                .orElseThrow(() -> new IllegalArgumentException("3D-DXF-Grenzen konnten nicht bestimmt werden.")));
        List<Dxf3dBounds> normalizedSolids = rawSolids.stream()
                .filter(bounds -> bounds.intersects(drawingBounds))
                .map(bounds -> bounds.clampTo(drawingBounds))
                .toList();
        double factor = unit.millimetersPerDrawingUnit();
        Dxf3dBounds metricBounds = drawingBounds.scale(factor);
        List<Dxf3dBounds> metricSolids = normalizedSolids.stream().map(bounds -> bounds.scale(factor)).toList();
        List<Dxf3dMesh> metricMeshes = rawMeshes.stream()
                .map(mesh -> mesh.scale(factor))
                .toList();
        if (metricSolids.isEmpty()) {
            metricSolids = List.of(metricBounds);
        }
        if (metricMeshes.isEmpty()) {
            metricMeshes = List.of(Dxf3dMesh.box(0, metricBounds));
        }
        return new Dxf3dObjectGeometry(metricBounds, metricSolids, metricMeshes, sourceSolidCount);
    }

    private List<Pair> pairs(List<String> lines) {
        List<Pair> pairs = new ArrayList<>();
        for (int index = 0; index + 1 < lines.size(); index += 2) {
            try {
                pairs.add(new Pair(Integer.parseInt(lines.get(index).trim()), stripLineEnding(lines.get(index + 1))));
            } catch (NumberFormatException ignored) {
            }
        }
        return pairs;
    }

    private String stripLineEnding(String value) {
        return value.endsWith("\r") ? value.substring(0, value.length() - 1) : value;
    }

    private DwgUnit readUnit(List<Pair> pairs) {
        for (int index = 0; index + 1 < pairs.size(); index++) {
            if (pairs.get(index).code() == 9 && "$INSUNITS".equals(pairs.get(index).trimmedValue())) {
                return DwgUnit.fromRawHeaderValue(pairs.get(index + 1).trimmedValue());
            }
        }
        return DwgUnit.UNITLESS;
    }

    private Optional<Dxf3dBounds> readHeaderBounds(List<Pair> pairs) {
        Optional<Vector3> minimum = headerVector(pairs, "$EXTMIN");
        Optional<Vector3> maximum = headerVector(pairs, "$EXTMAX");
        if (minimum.isEmpty() || maximum.isEmpty()) {
            return Optional.empty();
        }
        Vector3 min = minimum.orElseThrow();
        Vector3 max = maximum.orElseThrow();
        return Optional.of(new Dxf3dBounds(
                Math.min(min.x(), max.x()),
                Math.min(min.y(), max.y()),
                Math.min(min.z(), max.z()),
                Math.max(min.x(), max.x()),
                Math.max(min.y(), max.y()),
                Math.max(min.z(), max.z())
        ));
    }

    private Optional<Vector3> headerVector(List<Pair> pairs, String variableName) {
        for (int index = 0; index < pairs.size(); index++) {
            if (pairs.get(index).code() != 9 || !variableName.equals(pairs.get(index).trimmedValue())) {
                continue;
            }
            Double x = null;
            Double y = null;
            Double z = 0.0;
            for (int valueIndex = index + 1; valueIndex < pairs.size() && pairs.get(valueIndex).code() != 9; valueIndex++) {
                Pair pair = pairs.get(valueIndex);
                if (pair.code() == 10) x = parseDouble(pair.trimmedValue()).orElse(null);
                if (pair.code() == 20) y = parseDouble(pair.trimmedValue()).orElse(null);
                if (pair.code() == 30) z = parseDouble(pair.trimmedValue()).orElse(null);
            }
            if (x != null && y != null && z != null) {
                return Optional.of(new Vector3(x, y, z));
            }
        }
        return Optional.empty();
    }

    private String decryptedSat(List<Pair> entityPairs) {
        StringBuilder encrypted = new StringBuilder();
        for (Pair pair : entityPairs) {
            if (pair.code() == 1 || pair.code() == 3) {
                encrypted.append(pair.value());
                if (pair.code() == 1) {
                    encrypted.append('\n');
                }
            }
        }
        if (encrypted.isEmpty()) {
            return "";
        }
        return decryptSat(encrypted.toString());
    }

    private Optional<Dxf3dBounds> solidBounds(String sat) {
        Transform3 transform = sat.lines()
                .filter(line -> line.startsWith("transform "))
                .findFirst()
                .flatMap(this::parseTransform)
                .orElse(Transform3.identity());
        MutableBounds bounds = new MutableBounds();
        sat.lines().forEach(line -> addGeometryLine(bounds, transform, line));
        return bounds.toBounds();
    }

    private String decryptSat(String encrypted) {
        StringBuilder decrypted = new StringBuilder(encrypted.length());
        for (int index = 0; index < encrypted.length(); index++) {
            char value = encrypted.charAt(index);
            if (value == '^' && index + 1 < encrypted.length() && encrypted.charAt(index + 1) == ' ') {
                decrypted.append('A');
                index++;
            } else if (value <= 32) {
                decrypted.append(value);
            } else {
                decrypted.append((char) (159 - value));
            }
        }
        return decrypted.toString();
    }

    private Optional<Transform3> parseTransform(String line) {
        List<Double> values = numericTokens(line);
        if (values.size() < 13) {
            return Optional.empty();
        }
        int offset = values.size() - 13;
        return Optional.of(new Transform3(
                values.subList(offset, offset + 9).stream().mapToDouble(Double::doubleValue).toArray(),
                new Vector3(values.get(offset + 9), values.get(offset + 10), values.get(offset + 11)),
                values.get(offset + 12)
        ));
    }

    private void addGeometryLine(MutableBounds bounds, Transform3 transform, String line) {
        if (line.startsWith("point ")) {
            List<Double> values = numericTokens(line);
            if (values.size() >= 3) {
                bounds.include(transform.point(vector(values, values.size() - 3)));
            }
            return;
        }
        if (line.startsWith("ellipse-curve ")) {
            List<Double> values = numericTokens(line);
            if (values.size() >= 10) {
                addEllipse(bounds, transform, values);
            }
            return;
        }
        if (line.startsWith("torus-surface ")) {
            List<Double> values = numericTokens(line);
            if (values.size() >= 8) {
                int offset = values.size() - 8;
                Vector3 center = transform.point(vector(values, offset));
                double radius = (Math.abs(values.get(values.size() - 1)) + Math.abs(values.get(values.size() - 2))) * Math.abs(transform.scale());
                bounds.includeCube(center, radius);
            }
        }
    }

    private void addEllipse(MutableBounds bounds, Transform3 transform, List<Double> values) {
        int offset = values.size() - 10;
        Vector3 center = transform.point(vector(values, offset));
        Vector3 normal = vector(values, offset + 3).normalized();
        Vector3 major = vector(values, offset + 6);
        double ratio = Math.abs(values.get(offset + 9));
        Vector3 minor = normal.cross(major.normalized()).multiply(major.length() * ratio);
        Vector3 transformedMajor = transform.vector(major);
        Vector3 transformedMinor = transform.vector(minor);
        Vector3 extent = new Vector3(
                Math.hypot(transformedMajor.x(), transformedMinor.x()),
                Math.hypot(transformedMajor.y(), transformedMinor.y()),
                Math.hypot(transformedMajor.z(), transformedMinor.z())
        );
        bounds.include(center.subtract(extent));
        bounds.include(center.add(extent));
    }

    private Vector3 vector(List<Double> values, int offset) {
        return new Vector3(values.get(offset), values.get(offset + 1), values.get(offset + 2));
    }

    private List<Double> numericTokens(String line) {
        List<Double> values = new ArrayList<>();
        for (String token : line.trim().split("\\s+")) {
            if (token.startsWith("$") || token.equals("#")) {
                continue;
            }
            parseDouble(token).ifPresent(values::add);
        }
        return values;
    }

    private Optional<Double> parseDouble(String value) {
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private record Pair(int code, String value) {
        private boolean isType(String type) {
            return code == 0 && type.equalsIgnoreCase(trimmedValue());
        }

        private String trimmedValue() {
            return value.trim();
        }
    }

    private record Transform3(double[] matrix, Vector3 translation, double scale) {
        private static Transform3 identity() {
            return new Transform3(new double[]{1, 0, 0, 0, 1, 0, 0, 0, 1}, new Vector3(0, 0, 0), 1.0);
        }

        private Vector3 point(Vector3 point) {
            return vector(point).add(translation);
        }

        private Vector3 vector(Vector3 vector) {
            return new Vector3(
                    (matrix[0] * vector.x() + matrix[3] * vector.y() + matrix[6] * vector.z()) * scale,
                    (matrix[1] * vector.x() + matrix[4] * vector.y() + matrix[7] * vector.z()) * scale,
                    (matrix[2] * vector.x() + matrix[5] * vector.y() + matrix[8] * vector.z()) * scale
            );
        }
    }

    private record Vector3(double x, double y, double z) {
        private Vector3 add(Vector3 other) {
            return new Vector3(x + other.x, y + other.y, z + other.z);
        }

        private Vector3 subtract(Vector3 other) {
            return new Vector3(x - other.x, y - other.y, z - other.z);
        }

        private Vector3 multiply(double factor) {
            return new Vector3(x * factor, y * factor, z * factor);
        }

        private double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }

        private Vector3 normalized() {
            double length = length();
            return length == 0.0 ? this : multiply(1.0 / length);
        }

        private Vector3 cross(Vector3 other) {
            return new Vector3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x);
        }
    }

    private static final class MutableBounds {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        private void include(Vector3 point) {
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            minZ = Math.min(minZ, point.z());
            maxX = Math.max(maxX, point.x());
            maxY = Math.max(maxY, point.y());
            maxZ = Math.max(maxZ, point.z());
        }

        private void includeCube(Vector3 center, double radius) {
            Vector3 extent = new Vector3(radius, radius, radius);
            include(center.subtract(extent));
            include(center.add(extent));
        }

        private Optional<Dxf3dBounds> toBounds() {
            if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(minZ)) {
                return Optional.empty();
            }
            return Optional.of(new Dxf3dBounds(minX, minY, minZ, maxX, maxY, maxZ));
        }
    }
}
