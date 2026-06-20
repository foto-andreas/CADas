package de.schrell.cadas.application.dwg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class AcisSatMeshTessellator {

    private static final int FULL_CURVE_SEGMENTS = 32;
    private static final double EPSILON = 1.0e-7;

    Optional<Dxf3dMesh> tessellate(String sat, int sourceSolidIndex) {
        List<SatRecord> records = records(sat);
        if (records.isEmpty()) {
            return Optional.empty();
        }
        Map<Integer, SatRecord> recordsById = new HashMap<>();
        records.forEach(record -> recordsById.put(record.id(), record));
        Transform3 transform = records.stream()
                .filter(record -> record.type().equals("transform"))
                .findFirst()
                .flatMap(this::parseTransform)
                .orElse(Transform3.identity());
        Topology topology = new Topology(records, recordsById, transform);
        List<Double> triangles = new ArrayList<>();
        records.stream()
                .filter(record -> record.type().equals("face"))
                .forEach(face -> triangles.addAll(tessellateFace(topology, face)));
        if (triangles.isEmpty()) {
            return Optional.empty();
        }
        double[] coordinates = triangles.stream().mapToDouble(Double::doubleValue).toArray();
        return Optional.of(new Dxf3dMesh(sourceSolidIndex, bounds(coordinates), coordinates));
    }

    private List<Double> tessellateFace(Topology topology, SatRecord face) {
        Optional<SatRecord> surface = topology.referenceOfType(face, type -> type.endsWith("-surface"));
        if (surface.isEmpty()) {
            return List.of();
        }
        List<List<Vector3>> loops = topology.records().stream()
                .filter(record -> record.type().equals("loop"))
                .filter(loop -> loop.references().contains(face.id()))
                .map(topology::loopPoints)
                .filter(points -> points.size() >= 3)
                .toList();
        if (loops.isEmpty()) {
            return List.of();
        }
        List<ProjectedPoint> polygon = loops.stream()
                .map(loop -> project(loop, projection(surface.orElseThrow(), topology.transform(), loop)))
                .max(Comparator.comparingDouble(points -> Math.abs(area(points))))
                .orElse(List.of());
        if (polygon.size() < 3) {
            return List.of();
        }
        List<Integer> indices = triangulate(polygon);
        boolean reversed = face.tokens().contains("reversed");
        List<Double> coordinates = new ArrayList<>(indices.size() * 3);
        for (int index = 0; index < indices.size(); index += 3) {
            int first = indices.get(index);
            int second = indices.get(index + (reversed ? 2 : 1));
            int third = indices.get(index + (reversed ? 1 : 2));
            add(coordinates, polygon.get(first).point());
            add(coordinates, polygon.get(second).point());
            add(coordinates, polygon.get(third).point());
        }
        return coordinates;
    }

    private List<ProjectedPoint> project(List<Vector3> points, Projection projection) {
        List<ProjectedPoint> projected = new ArrayList<>();
        Point2 previous = null;
        for (Vector3 point : points) {
            Point2 current = projection.project(point);
            if (previous != null) {
                current = projection.unwrap(previous, current);
            }
            if (projected.isEmpty() || !projected.getLast().point().near(point)) {
                projected.add(new ProjectedPoint(point, current));
                previous = current;
            }
        }
        if (projected.size() > 2 && projected.getFirst().point().near(projected.getLast().point())) {
            projected.removeLast();
        }
        removeCollinearPoints(projected);
        return projected;
    }

    private void removeCollinearPoints(List<ProjectedPoint> points) {
        boolean changed = true;
        while (changed && points.size() > 3) {
            changed = false;
            for (int index = 0; index < points.size(); index++) {
                Point2 previous = points.get(Math.floorMod(index - 1, points.size())).projection();
                Point2 current = points.get(index).projection();
                Point2 next = points.get((index + 1) % points.size()).projection();
                if (Math.abs(cross(previous, current, next)) <= EPSILON) {
                    points.remove(index);
                    changed = true;
                    break;
                }
            }
        }
    }

    private List<Integer> triangulate(List<ProjectedPoint> polygon) {
        List<Integer> remaining = new ArrayList<>();
        for (int index = 0; index < polygon.size(); index++) {
            remaining.add(index);
        }
        if (area(polygon) < 0.0) {
            java.util.Collections.reverse(remaining);
        }
        List<Integer> triangles = new ArrayList<>();
        while (remaining.size() > 3) {
            boolean earFound = false;
            for (int index = 0; index < remaining.size(); index++) {
                int previous = remaining.get(Math.floorMod(index - 1, remaining.size()));
                int current = remaining.get(index);
                int next = remaining.get((index + 1) % remaining.size());
                if (!isEar(polygon, remaining, previous, current, next)) {
                    continue;
                }
                triangles.add(previous);
                triangles.add(current);
                triangles.add(next);
                remaining.remove(index);
                earFound = true;
                break;
            }
            if (!earFound) {
                for (int index = 1; index + 1 < remaining.size(); index++) {
                    triangles.add(remaining.getFirst());
                    triangles.add(remaining.get(index));
                    triangles.add(remaining.get(index + 1));
                }
                remaining.clear();
                break;
            }
        }
        if (remaining.size() == 3) {
            triangles.addAll(remaining);
        }
        if (triangles.isEmpty() && polygon.size() >= 3) {
            for (int index = 1; index + 1 < polygon.size(); index++) {
                triangles.add(0);
                triangles.add(index);
                triangles.add(index + 1);
            }
        }
        return triangles;
    }

    private boolean isEar(List<ProjectedPoint> polygon, List<Integer> remaining, int previous, int current, int next) {
        Point2 a = polygon.get(previous).projection();
        Point2 b = polygon.get(current).projection();
        Point2 c = polygon.get(next).projection();
        if (cross(a, b, c) <= EPSILON) {
            return false;
        }
        for (int candidate : remaining) {
            if (candidate != previous && candidate != current && candidate != next
                    && insideTriangle(polygon.get(candidate).projection(), a, b, c)) {
                return false;
            }
        }
        return true;
    }

    private boolean insideTriangle(Point2 point, Point2 a, Point2 b, Point2 c) {
        double first = cross(a, b, point);
        double second = cross(b, c, point);
        double third = cross(c, a, point);
        return first >= -EPSILON && second >= -EPSILON && third >= -EPSILON;
    }

    private double area(List<ProjectedPoint> points) {
        double area = 0.0;
        for (int index = 0; index < points.size(); index++) {
            Point2 current = points.get(index).projection();
            Point2 next = points.get((index + 1) % points.size()).projection();
            area += current.x() * next.y() - next.x() * current.y();
        }
        return area / 2.0;
    }

    private double cross(Point2 a, Point2 b, Point2 c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }

    private Projection projection(SatRecord surface, Transform3 transform, List<Vector3> points) {
        List<Double> values = surface.geometryValues();
        if (surface.type().equals("plane-surface") && values.size() >= 9) {
            Vector3 origin = transform.point(vector(values, 0));
            Vector3 normal = transform.vector(vector(values, 3)).normalized();
            Vector3 uAxis = transform.vector(vector(values, 6)).normalized();
            Vector3 vAxis = normal.cross(uAxis).normalized();
            return Projection.planar(origin, uAxis, vAxis);
        }
        if (surface.type().equals("cone-surface") && values.size() >= 10) {
            Vector3 origin = transform.point(vector(values, 0));
            Vector3 axis = transform.vector(vector(values, 3)).normalized();
            Vector3 radial = transform.vector(vector(values, 6)).normalized();
            return Projection.angular(origin, axis, radial, axis.cross(radial).normalized(), 1);
        }
        if (surface.type().equals("torus-surface") && values.size() >= 11) {
            Vector3 origin = transform.point(vector(values, 0));
            Vector3 axis = transform.vector(vector(values, 3)).normalized();
            Vector3 radial = transform.vector(vector(values, 8)).normalized();
            double majorRadius = Math.abs(values.get(6) * transform.scale());
            return Projection.toroidal(origin, axis, radial, axis.cross(radial).normalized(), majorRadius);
        }
        return Projection.dominantAxis(points);
    }

    private Optional<Transform3> parseTransform(SatRecord transform) {
        List<Double> values = transform.numericValuesAfter(2);
        if (values.size() < 13) {
            return Optional.empty();
        }
        return Optional.of(new Transform3(
                values.subList(0, 9).stream().mapToDouble(Double::doubleValue).toArray(),
                vector(values, 9),
                values.get(12)
        ));
    }

    private List<SatRecord> records(String sat) {
        List<SatRecord> records = new ArrayList<>();
        int firstRecord = sat.indexOf("asmheader ");
        if (firstRecord < 0) {
            return List.of();
        }
        for (String recordText : sat.substring(firstRecord).split("#")) {
            List<String> tokens = tokens(recordText);
            if (tokens.isEmpty()) {
                continue;
            }
            int typeIndex = explicitRecordNumber(tokens.getFirst()) ? 1 : 0;
            if (typeIndex >= tokens.size() || !isRecordType(tokens.get(typeIndex))) {
                continue;
            }
            records.add(new SatRecord(
                    records.size(),
                    tokens.get(typeIndex).toLowerCase(Locale.ROOT),
                    tokens.subList(typeIndex + 1, tokens.size())
            ));
        }
        return records;
    }

    private List<String> tokens(String line) {
        String trimmed = line.trim();
        return trimmed.isEmpty() ? List.of() : List.of(trimmed.split("\\s+"));
    }

    private boolean explicitRecordNumber(String token) {
        return token.matches("-\\d+");
    }

    private boolean isRecordType(String token) {
        return token.matches("(?i)[a-z][a-z0-9_-]*") && !token.equalsIgnoreCase("forward") && !token.equalsIgnoreCase("reversed");
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

    private Vector3 vector(List<Double> values, int offset) {
        return new Vector3(values.get(offset), values.get(offset + 1), values.get(offset + 2));
    }

    private void add(List<Double> coordinates, Vector3 point) {
        coordinates.add(point.x());
        coordinates.add(point.y());
        coordinates.add(point.z());
    }

    private record Topology(List<SatRecord> records, Map<Integer, SatRecord> recordsById, Transform3 transform) {

        private Optional<SatRecord> referenceOfType(SatRecord source, java.util.function.Predicate<String> typeMatcher) {
            return source.references().stream()
                    .map(recordsById::get)
                    .filter(java.util.Objects::nonNull)
                    .filter(record -> typeMatcher.test(record.type()))
                    .findFirst();
        }

        private List<Vector3> loopPoints(SatRecord loop) {
            Optional<SatRecord> first = referenceOfType(loop, type -> type.equals("coedge"));
            if (first.isEmpty()) {
                return List.of();
            }
            List<Vector3> points = new ArrayList<>();
            Set<Integer> visited = new HashSet<>();
            SatRecord current = first.orElseThrow();
            while (visited.add(current.id())) {
                List<Vector3> segment = coedgePoints(current);
                appendConnected(points, segment);
                Optional<SatRecord> next = nextCoedge(current);
                if (next.isEmpty()) {
                    break;
                }
                current = next.orElseThrow();
            }
            return points;
        }

        private Optional<SatRecord> nextCoedge(SatRecord coedge) {
            if (coedge.tokens().size() <= 3) {
                return Optional.empty();
            }
            return coedge.referenceAt(3).map(recordsById::get).filter(record -> record.type().equals("coedge"));
        }

        private List<Vector3> coedgePoints(SatRecord coedge) {
            Optional<SatRecord> edge = referenceOfType(coedge, type -> type.equals("edge"));
            if (edge.isEmpty()) {
                return List.of();
            }
            List<Vector3> points = edgePoints(edge.orElseThrow());
            if (orientationAfterReference(coedge, edge.orElseThrow().id()).equals("reversed")) {
                java.util.Collections.reverse(points);
            }
            return points;
        }

        private List<Vector3> edgePoints(SatRecord edge) {
            if (edge.tokens().size() < 10) {
                return List.of();
            }
            Optional<Vector3> start = edge.referenceAt(3).flatMap(this::vertexPoint);
            Optional<Vector3> end = edge.referenceAt(5).flatMap(this::vertexPoint);
            Optional<SatRecord> curve = referenceOfType(edge, type -> type.endsWith("-curve"));
            if (start.isEmpty() || end.isEmpty() || curve.isEmpty()) {
                return List.of();
            }
            double startParameter = edge.doubleAt(4).orElse(0.0);
            double endParameter = edge.doubleAt(6).orElse(1.0);
            List<Vector3> sampled = sampleCurve(curve.orElseThrow(), startParameter, endParameter, start.orElseThrow(), end.orElseThrow());
            if (orientationAfterReference(edge, curve.orElseThrow().id()).equals("reversed")) {
                java.util.Collections.reverse(sampled);
            }
            return sampled;
        }

        private Optional<Vector3> vertexPoint(int vertexId) {
            SatRecord vertex = recordsById.get(vertexId);
            if (vertex == null || !vertex.type().equals("vertex")) {
                return Optional.empty();
            }
            return referenceOfType(vertex, type -> type.equals("point")).flatMap(this::point);
        }

        private Optional<Vector3> point(SatRecord point) {
            List<Double> values = point.geometryValues();
            return values.size() < 3 ? Optional.empty() : Optional.of(transform.point(new Vector3(values.get(0), values.get(1), values.get(2))));
        }

        private List<Vector3> sampleCurve(SatRecord curve, double startParameter, double endParameter, Vector3 start, Vector3 end) {
            if (curve.type().equals("intcurve-curve")) {
                List<Vector3> controlPoints = nurbsControlPoints(curve);
                if (controlPoints.size() >= 2) {
                    if (controlPoints.getFirst().distance(start) + controlPoints.getLast().distance(end)
                            > controlPoints.getFirst().distance(end) + controlPoints.getLast().distance(start)) {
                        java.util.Collections.reverse(controlPoints);
                    }
                    controlPoints.set(0, start);
                    controlPoints.set(controlPoints.size() - 1, end);
                    return controlPoints;
                }
            }
            if (!curve.type().equals("ellipse-curve")) {
                return new ArrayList<>(List.of(start, end));
            }
            List<Double> values = curve.geometryValues();
            if (values.size() < 10) {
                return new ArrayList<>(List.of(start, end));
            }
            Vector3 center = transform.point(vector(values, 0));
            Vector3 normal = vector(values, 3).normalized();
            Vector3 majorSource = vector(values, 6);
            double ratio = Math.abs(values.get(9));
            Vector3 minorSource = normal.cross(majorSource.normalized()).multiply(majorSource.length() * ratio);
            Vector3 major = transform.vector(majorSource);
            Vector3 minor = transform.vector(minorSource);
            double span = endParameter - startParameter;
            int segments = Math.max(2, (int) Math.ceil(Math.abs(span) / (Math.PI * 2.0) * FULL_CURVE_SEGMENTS));
            List<Vector3> points = new ArrayList<>(segments + 1);
            for (int index = 0; index <= segments; index++) {
                double parameter = startParameter + span * index / segments;
                points.add(center.add(major.multiply(Math.cos(parameter))).add(minor.multiply(Math.sin(parameter))));
            }
            points.set(0, start);
            points.set(points.size() - 1, end);
            return points;
        }

        private List<Vector3> nurbsControlPoints(SatRecord curve) {
            int nurbsIndex = -1;
            for (int index = 0; index < curve.tokens().size(); index++) {
                if (curve.tokens().get(index).equals("nurbs") || curve.tokens().get(index).equals("nubs")) {
                    nurbsIndex = index;
                    break;
                }
            }
            if (nurbsIndex < 0 || nurbsIndex + 3 >= curve.tokens().size()) {
                return List.of();
            }
            Optional<Integer> degree = integer(curve.tokens().get(nurbsIndex + 1));
            int openIndex = curve.tokens().subList(nurbsIndex + 2, curve.tokens().size()).indexOf("open");
            if (degree.isEmpty() || openIndex < 0) {
                return List.of();
            }
            openIndex += nurbsIndex + 2;
            Optional<Integer> spanCount = openIndex + 1 < curve.tokens().size()
                    ? integer(curve.tokens().get(openIndex + 1))
                    : Optional.empty();
            if (spanCount.isEmpty()) {
                return List.of();
            }
            int controlPointCount = spanCount.orElseThrow() + degree.orElseThrow() - 1;
            List<Double> values = new ArrayList<>();
            for (int index = openIndex + 2; index < curve.tokens().size(); index++) {
                if (curve.tokens().get(index).startsWith("null_")) {
                    break;
                }
                curve.doubleAt(index).ifPresent(values::add);
            }
            int coordinateCount = controlPointCount * 4;
            while (values.size() > coordinateCount && Math.abs(values.getLast()) <= EPSILON) {
                values.removeLast();
            }
            if (values.size() < coordinateCount) {
                return List.of();
            }
            int firstCoordinate = values.size() - coordinateCount;
            List<Vector3> points = new ArrayList<>(controlPointCount);
            for (int index = 0; index < controlPointCount; index++) {
                int offset = firstCoordinate + index * 4;
                points.add(transform.point(new Vector3(values.get(offset), values.get(offset + 1), values.get(offset + 2))));
            }
            return points;
        }

        private Optional<Integer> integer(String value) {
            try {
                return Optional.of(Integer.parseInt(value));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }

        private String orientationAfterReference(SatRecord record, int referenceId) {
            for (int index = 0; index + 1 < record.tokens().size(); index++) {
                if (record.referenceAt(index).orElse(-1) == referenceId) {
                    String orientation = record.tokens().get(index + 1);
                    if (orientation.equals("forward") || orientation.equals("reversed")) {
                        return orientation;
                    }
                }
            }
            return "forward";
        }

        private void appendConnected(List<Vector3> target, List<Vector3> segment) {
            if (segment.isEmpty()) {
                return;
            }
            if (!target.isEmpty() && !target.getLast().near(segment.getFirst()) && target.getLast().near(segment.getLast())) {
                java.util.Collections.reverse(segment);
            }
            int startIndex = !target.isEmpty() && target.getLast().near(segment.getFirst()) ? 1 : 0;
            for (int index = startIndex; index < segment.size(); index++) {
                target.add(segment.get(index));
            }
        }

        private Vector3 vector(List<Double> values, int offset) {
            return new Vector3(values.get(offset), values.get(offset + 1), values.get(offset + 2));
        }
    }

    private record SatRecord(int id, String type, List<String> tokens) {

        private SatRecord {
            tokens = List.copyOf(tokens);
        }

        private List<Integer> references() {
            return tokens.stream().filter(token -> token.matches("\\$-?\\d+"))
                    .map(token -> Integer.parseInt(token.substring(1)))
                    .filter(reference -> reference >= 0)
                    .toList();
        }

        private Optional<Integer> referenceAt(int index) {
            if (index < 0 || index >= tokens.size() || !tokens.get(index).matches("\\$-?\\d+")) {
                return Optional.empty();
            }
            int reference = Integer.parseInt(tokens.get(index).substring(1));
            return reference < 0 ? Optional.empty() : Optional.of(reference);
        }

        private Optional<Double> doubleAt(int index) {
            if (index < 0 || index >= tokens.size()) {
                return Optional.empty();
            }
            try {
                return Optional.of(Double.parseDouble(tokens.get(index)));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }

        private List<Double> geometryValues() {
            return numericValuesAfter(3);
        }

        private List<Double> numericValuesAfter(int skippedTokens) {
            List<Double> values = new ArrayList<>();
            for (int index = skippedTokens; index < tokens.size(); index++) {
                doubleAt(index).ifPresent(values::add);
            }
            return values;
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

        private double dot(Vector3 other) {
            return x * other.x + y * other.y + z * other.z;
        }

        private Vector3 cross(Vector3 other) {
            return new Vector3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x);
        }

        private double length() {
            return Math.sqrt(dot(this));
        }

        private Vector3 normalized() {
            double length = length();
            return length <= EPSILON ? this : multiply(1.0 / length);
        }

        private boolean near(Vector3 other) {
            return subtract(other).length() <= EPSILON;
        }

        private double distance(Vector3 other) {
            return subtract(other).length();
        }
    }

    private record Point2(double x, double y) {
    }

    private record ProjectedPoint(Vector3 point, Point2 projection) {
    }

    private interface Projection {

        Point2 project(Vector3 point);

        default Point2 unwrap(Point2 previous, Point2 current) {
            return current;
        }

        static Projection planar(Vector3 origin, Vector3 uAxis, Vector3 vAxis) {
            return point -> {
                Vector3 relative = point.subtract(origin);
                return new Point2(relative.dot(uAxis), relative.dot(vAxis));
            };
        }

        static Projection angular(Vector3 origin, Vector3 axis, Vector3 uAxis, Vector3 vAxis, int angularCoordinate) {
            return new Projection() {
                @Override
                public Point2 project(Vector3 point) {
                    Vector3 relative = point.subtract(origin);
                    double angle = Math.atan2(relative.dot(vAxis), relative.dot(uAxis));
                    return angularCoordinate == 0 ? new Point2(angle, relative.dot(axis)) : new Point2(relative.dot(axis), angle);
                }

                @Override
                public Point2 unwrap(Point2 previous, Point2 current) {
                    return angularCoordinate == 0
                            ? new Point2(unwrapAngle(previous.x(), current.x()), current.y())
                            : new Point2(current.x(), unwrapAngle(previous.y(), current.y()));
                }
            };
        }

        static Projection toroidal(Vector3 origin, Vector3 axis, Vector3 uAxis, Vector3 vAxis, double majorRadius) {
            return new Projection() {
                @Override
                public Point2 project(Vector3 point) {
                    Vector3 relative = point.subtract(origin);
                    double axial = relative.dot(axis);
                    Vector3 planar = relative.subtract(axis.multiply(axial));
                    double around = Math.atan2(planar.dot(vAxis), planar.dot(uAxis));
                    double section = Math.atan2(axial, planar.length() - majorRadius);
                    return new Point2(around, section);
                }

                @Override
                public Point2 unwrap(Point2 previous, Point2 current) {
                    return new Point2(unwrapAngle(previous.x(), current.x()), unwrapAngle(previous.y(), current.y()));
                }
            };
        }

        static Projection dominantAxis(List<Vector3> points) {
            Vector3 normal = new Vector3(0.0, 0.0, 0.0);
            for (int index = 0; index < points.size(); index++) {
                Vector3 current = points.get(index);
                Vector3 next = points.get((index + 1) % points.size());
                normal = normal.add(new Vector3(
                        (current.y() - next.y()) * (current.z() + next.z()),
                        (current.z() - next.z()) * (current.x() + next.x()),
                        (current.x() - next.x()) * (current.y() + next.y())
                ));
            }
            double x = Math.abs(normal.x());
            double y = Math.abs(normal.y());
            double z = Math.abs(normal.z());
            if (x >= y && x >= z) {
                return point -> new Point2(point.y(), point.z());
            }
            if (y >= z) {
                return point -> new Point2(point.x(), point.z());
            }
            return point -> new Point2(point.x(), point.y());
        }

        private static double unwrapAngle(double previous, double current) {
            double value = current;
            while (value - previous > Math.PI) value -= Math.PI * 2.0;
            while (value - previous < -Math.PI) value += Math.PI * 2.0;
            return value;
        }
    }
}
