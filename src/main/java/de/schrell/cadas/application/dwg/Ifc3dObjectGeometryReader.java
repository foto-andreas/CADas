package de.schrell.cadas.application.dwg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Ifc3dObjectGeometryReader {

    private static final Pattern ENTITY_PATTERN = Pattern.compile("#(\\d+)\\s*=\\s*([A-Z0-9_]+)\\((.*)\\)\\s*;", Pattern.DOTALL);
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("#(\\d+)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[Ee][-+]?\\d+)?");
    private static final int CIRCLE_SEGMENTS = 32;

    public Dxf3dObjectGeometry read(Path sourceFile) throws IOException {
        Map<Integer, Entity> entities = entities(Files.readString(sourceFile, StandardCharsets.UTF_8));
        double unitFactor = lengthUnitFactor(entities);
        Map<Integer, String> materialKeys = materialKeys(entities);
        List<Dxf3dMesh> meshes = new ArrayList<>();
        int sourceIndex = 0;
        for (Map.Entry<Integer, Entity> entry : entities.entrySet()) {
            Entity entity = entry.getValue();
            Optional<double[]> coordinates = switch (entity.type()) {
                case "IFCFACETEDBREP" -> facetedBrep(entity, entities);
                case "IFCEXTRUDEDAREASOLID" -> extrudedAreaSolid(entity, entities);
                default -> Optional.empty();
            };
            if (coordinates.isEmpty()) {
                continue;
            }
            double[] metricCoordinates = Arrays.stream(coordinates.orElseThrow())
                    .map(coordinate -> coordinate * unitFactor)
                    .toArray();
            Dxf3dBounds bounds = bounds(metricCoordinates);
            meshes.add(new Dxf3dMesh(
                    sourceIndex++, bounds, metricCoordinates,
                    materialKeys.getOrDefault(entry.getKey(), "room-object")
            ));
        }
        if (meshes.isEmpty()) {
            throw new IllegalArgumentException("IFC-Datei enthält keine unterstützten 3D-Körper.");
        }
        Dxf3dBounds bounds = meshes.stream().map(Dxf3dMesh::bounds).reduce(Dxf3dBounds::include).orElseThrow();
        return new Dxf3dObjectGeometry(bounds, meshes.stream().map(Dxf3dMesh::bounds).toList(), meshes, meshes.size());
    }

    private Map<Integer, Entity> entities(String content) {
        Map<Integer, Entity> entities = new LinkedHashMap<>();
        StringBuilder statement = new StringBuilder();
        for (String line : content.lines().toList()) {
            String trimmed = line.trim();
            if (statement.isEmpty() && !trimmed.startsWith("#")) {
                continue;
            }
            statement.append(trimmed);
            if (!trimmed.endsWith(";")) {
                continue;
            }
            Matcher matcher = ENTITY_PATTERN.matcher(statement);
            if (matcher.matches()) {
                entities.put(Integer.parseInt(matcher.group(1)), new Entity(matcher.group(2), matcher.group(3)));
            }
            statement.setLength(0);
        }
        return entities;
    }

    private double lengthUnitFactor(Map<Integer, Entity> entities) {
        return entities.values().stream()
                .filter(entity -> entity.type().equals("IFCSIUNIT"))
                .filter(entity -> entity.arguments().contains(".LENGTHUNIT."))
                .map(Entity::arguments)
                .mapToDouble(arguments -> {
                    if (arguments.contains(".MILLI.")) return 1.0;
                    if (arguments.contains(".CENTI.")) return 10.0;
                    if (arguments.contains(".DECI.")) return 100.0;
                    return 1_000.0;
                })
                .findFirst()
                .orElse(1_000.0);
    }

    private Map<Integer, String> materialKeys(Map<Integer, Entity> entities) {
        Map<Integer, String> keys = new HashMap<>();
        for (Entity entity : entities.values()) {
            if (!entity.type().equals("IFCSTYLEDITEM")) {
                continue;
            }
            List<Integer> references = references(entity.arguments());
            if (references.size() < 2) {
                continue;
            }
            findColor(references.get(1), entities, new HashSet<>()).ifPresent(color -> keys.put(references.getFirst(), color));
        }
        return Map.copyOf(keys);
    }

    private Optional<String> findColor(int entityId, Map<Integer, Entity> entities, Set<Integer> visited) {
        if (!visited.add(entityId)) {
            return Optional.empty();
        }
        Entity entity = entities.get(entityId);
        if (entity == null) {
            return Optional.empty();
        }
        if (entity.type().equals("IFCCOLOURRGB")) {
            List<Double> values = numbers(entity.arguments());
            if (values.size() >= 3) {
                int red = colorChannel(values.get(values.size() - 3));
                int green = colorChannel(values.get(values.size() - 2));
                int blue = colorChannel(values.getLast());
                return Optional.of(String.format(Locale.ROOT, "color:#%02X%02X%02X", red, green, blue));
            }
        }
        return references(entity.arguments()).stream()
                .map(reference -> findColor(reference, entities, visited))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private int colorChannel(double value) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, value)) * 255.0);
    }

    private Optional<double[]> facetedBrep(Entity brep, Map<Integer, Entity> entities) {
        List<Integer> brepReferences = references(brep.arguments());
        if (brepReferences.isEmpty()) {
            return Optional.empty();
        }
        Entity shell = entities.get(brepReferences.getFirst());
        if (shell == null || !shell.type().equals("IFCCLOSEDSHELL")) {
            return Optional.empty();
        }
        List<Double> triangles = new ArrayList<>();
        for (int faceId : references(shell.arguments())) {
            Entity face = entities.get(faceId);
            if (face == null || !face.type().equals("IFCFACE")) {
                continue;
            }
            List<Integer> faceReferences = references(face.arguments());
            if (faceReferences.isEmpty()) {
                continue;
            }
            Entity bound = entities.get(faceReferences.getFirst());
            if (bound == null) {
                continue;
            }
            List<Integer> boundReferences = references(bound.arguments());
            if (boundReferences.isEmpty()) {
                continue;
            }
            Entity loop = entities.get(boundReferences.getFirst());
            if (loop == null || !loop.type().equals("IFCPOLYLOOP")) {
                continue;
            }
            List<Vector3> polygon = references(loop.arguments()).stream()
                    .map(entities::get)
                    .filter(point -> point != null && point.type().equals("IFCCARTESIANPOINT"))
                    .map(this::point)
                    .flatMap(Optional::stream)
                    .toList();
            appendPolygon(triangles, polygon);
        }
        return triangles.isEmpty() ? Optional.empty() : Optional.of(toArray(triangles));
    }

    private Optional<double[]> extrudedAreaSolid(Entity solid, Map<Integer, Entity> entities) {
        List<Integer> solidReferences = references(solid.arguments());
        List<Double> solidNumbers = numbers(solid.arguments());
        if (solidReferences.size() < 3 || solidNumbers.isEmpty()) {
            return Optional.empty();
        }
        Entity profile = entities.get(solidReferences.get(0));
        Entity placement = entities.get(solidReferences.get(1));
        Entity direction = entities.get(solidReferences.get(2));
        if (profile == null || placement == null || direction == null) {
            return Optional.empty();
        }
        List<Vector2> profilePoints = profilePoints(profile, entities);
        if (profilePoints.size() < 3) {
            return Optional.empty();
        }
        Transform transform = transform(placement, entities);
        Vector3 localDirection = direction(direction).orElse(new Vector3(0.0, 0.0, 1.0));
        Vector3 extrusion = transform.vector(localDirection).normalize().multiply(solidNumbers.getLast());
        List<Vector3> bottom = profilePoints.stream()
                .map(point -> transform.point(new Vector3(point.x(), point.y(), 0.0)))
                .toList();
        List<Vector3> top = bottom.stream().map(point -> point.add(extrusion)).toList();
        List<Double> triangles = new ArrayList<>();
        appendPolygon(triangles, bottom);
        List<Vector3> reversedTop = new ArrayList<>(top);
        java.util.Collections.reverse(reversedTop);
        appendPolygon(triangles, reversedTop);
        for (int index = 0; index < bottom.size(); index++) {
            int next = (index + 1) % bottom.size();
            appendTriangle(triangles, bottom.get(index), bottom.get(next), top.get(next));
            appendTriangle(triangles, bottom.get(index), top.get(next), top.get(index));
        }
        return Optional.of(toArray(triangles));
    }

    private List<Vector2> profilePoints(Entity profile, Map<Integer, Entity> entities) {
        if (profile.type().equals("IFCCIRCLEPROFILEDEF") || profile.type().equals("IFCCIRCLEHOLLOWPROFILEDEF")) {
            List<Double> values = numbers(profile.arguments());
            if (values.isEmpty()) {
                return List.of();
            }
            double radius = profile.type().equals("IFCCIRCLEHOLLOWPROFILEDEF") && values.size() >= 2
                    ? values.get(values.size() - 2)
                    : values.getLast();
            Vector2 center = profilePlacement(profile, entities);
            List<Vector2> points = new ArrayList<>();
            for (int index = 0; index < CIRCLE_SEGMENTS; index++) {
                double angle = Math.PI * 2.0 * index / CIRCLE_SEGMENTS;
                points.add(new Vector2(center.x() + Math.cos(angle) * radius, center.y() + Math.sin(angle) * radius));
            }
            return points;
        }
        List<Integer> profileReferences = references(profile.arguments());
        if (profileReferences.isEmpty()) {
            return List.of();
        }
        int curveReference = profileReferences.getLast();
        List<Vector2> points = reachablePoints(curveReference, entities, new HashSet<>()).stream()
                .map(point -> new Vector2(point.x(), point.y()))
                .distinct()
                .toList();
        return convexHull(points);
    }

    private Vector2 profilePlacement(Entity profile, Map<Integer, Entity> entities) {
        List<Integer> references = references(profile.arguments());
        if (references.isEmpty()) {
            return new Vector2(0.0, 0.0);
        }
        Entity placement = entities.get(references.getFirst());
        if (placement == null || !placement.type().equals("IFCAXIS2PLACEMENT2D")) {
            return new Vector2(0.0, 0.0);
        }
        List<Integer> placementReferences = references(placement.arguments());
        if (placementReferences.isEmpty()) {
            return new Vector2(0.0, 0.0);
        }
        return point(entities.get(placementReferences.getFirst()))
                .map(value -> new Vector2(value.x(), value.y()))
                .orElse(new Vector2(0.0, 0.0));
    }

    private List<Vector3> reachablePoints(int entityId, Map<Integer, Entity> entities, Set<Integer> visited) {
        if (!visited.add(entityId)) {
            return List.of();
        }
        Entity entity = entities.get(entityId);
        if (entity == null) {
            return List.of();
        }
        if (entity.type().equals("IFCCARTESIANPOINT")) {
            return point(entity).stream().toList();
        }
        return references(entity.arguments()).stream()
                .flatMap(reference -> reachablePoints(reference, entities, visited).stream())
                .toList();
    }

    private Transform transform(Entity placement, Map<Integer, Entity> entities) {
        List<Integer> placementReferences = references(placement.arguments());
        Vector3 origin = placementReferences.isEmpty()
                ? Vector3.ZERO
                : point(entities.get(placementReferences.getFirst())).orElse(Vector3.ZERO);
        Vector3 zAxis = placementReferences.size() >= 2
                ? direction(entities.get(placementReferences.get(1))).orElse(Vector3.Z)
                : Vector3.Z;
        Vector3 xAxis = placementReferences.size() >= 3
                ? direction(entities.get(placementReferences.get(2))).orElse(Vector3.X)
                : Vector3.X;
        zAxis = zAxis.normalize();
        xAxis = xAxis.normalize();
        Vector3 yAxis = zAxis.cross(xAxis).normalize();
        return new Transform(origin, xAxis, yAxis, zAxis);
    }

    private Optional<Vector3> point(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        List<Double> values = numbers(entity.arguments());
        if (values.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(new Vector3(values.get(0), values.get(1), values.size() >= 3 ? values.get(2) : 0.0));
    }

    private Optional<Vector3> direction(Entity entity) {
        return point(entity);
    }

    private List<Vector2> convexHull(List<Vector2> input) {
        List<Vector2> points = input.stream()
                .sorted(Comparator.comparingDouble(Vector2::x).thenComparingDouble(Vector2::y))
                .toList();
        if (points.size() < 3) {
            return points;
        }
        List<Vector2> lower = new ArrayList<>();
        for (Vector2 point : points) {
            while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.getLast(), point) <= 0.0) {
                lower.removeLast();
            }
            lower.add(point);
        }
        List<Vector2> upper = new ArrayList<>();
        for (int index = points.size() - 1; index >= 0; index--) {
            Vector2 point = points.get(index);
            while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.getLast(), point) <= 0.0) {
                upper.removeLast();
            }
            upper.add(point);
        }
        lower.removeLast();
        upper.removeLast();
        lower.addAll(upper);
        return List.copyOf(lower);
    }

    private double cross(Vector2 first, Vector2 second, Vector2 third) {
        return (second.x() - first.x()) * (third.y() - first.y())
                - (second.y() - first.y()) * (third.x() - first.x());
    }

    private void appendPolygon(List<Double> triangles, List<Vector3> polygon) {
        if (polygon.size() < 3) {
            return;
        }
        Vector3 first = polygon.getFirst();
        for (int index = 1; index + 1 < polygon.size(); index++) {
            appendTriangle(triangles, first, polygon.get(index), polygon.get(index + 1));
        }
    }

    private void appendTriangle(List<Double> triangles, Vector3 first, Vector3 second, Vector3 third) {
        appendPoint(triangles, first);
        appendPoint(triangles, second);
        appendPoint(triangles, third);
    }

    private void appendPoint(List<Double> coordinates, Vector3 point) {
        coordinates.add(point.x());
        coordinates.add(point.y());
        coordinates.add(point.z());
    }

    private double[] toArray(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private Dxf3dBounds bounds(double[] coordinates) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < coordinates.length; index += 3) {
            minX = Math.min(minX, coordinates[index]);
            minY = Math.min(minY, coordinates[index + 1]);
            minZ = Math.min(minZ, coordinates[index + 2]);
            maxX = Math.max(maxX, coordinates[index]);
            maxY = Math.max(maxY, coordinates[index + 1]);
            maxZ = Math.max(maxZ, coordinates[index + 2]);
        }
        return new Dxf3dBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private List<Integer> references(String value) {
        List<Integer> references = new ArrayList<>();
        Matcher matcher = REFERENCE_PATTERN.matcher(value);
        while (matcher.find()) {
            references.add(Integer.parseInt(matcher.group(1)));
        }
        return references;
    }

    private List<Double> numbers(String value) {
        String withoutReferences = REFERENCE_PATTERN.matcher(value).replaceAll("");
        List<Double> numbers = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(withoutReferences);
        while (matcher.find()) {
            numbers.add(Double.parseDouble(matcher.group()));
        }
        return numbers;
    }

    private record Entity(String type, String arguments) {
    }

    private record Vector2(double x, double y) {
    }

    private record Vector3(double x, double y, double z) {
        private static final Vector3 ZERO = new Vector3(0.0, 0.0, 0.0);
        private static final Vector3 X = new Vector3(1.0, 0.0, 0.0);
        private static final Vector3 Z = new Vector3(0.0, 0.0, 1.0);

        private Vector3 add(Vector3 other) {
            return new Vector3(x + other.x, y + other.y, z + other.z);
        }

        private Vector3 multiply(double factor) {
            return new Vector3(x * factor, y * factor, z * factor);
        }

        private Vector3 cross(Vector3 other) {
            return new Vector3(
                    y * other.z - z * other.y,
                    z * other.x - x * other.z,
                    x * other.y - y * other.x
            );
        }

        private Vector3 normalize() {
            double length = Math.sqrt(x * x + y * y + z * z);
            return length <= 0.000001 ? Z : new Vector3(x / length, y / length, z / length);
        }
    }

    private record Transform(Vector3 origin, Vector3 xAxis, Vector3 yAxis, Vector3 zAxis) {
        private Vector3 point(Vector3 local) {
            return origin.add(vector(local));
        }

        private Vector3 vector(Vector3 local) {
            return xAxis.multiply(local.x())
                    .add(yAxis.multiply(local.y()))
                    .add(zAxis.multiply(local.z()));
        }
    }
}
