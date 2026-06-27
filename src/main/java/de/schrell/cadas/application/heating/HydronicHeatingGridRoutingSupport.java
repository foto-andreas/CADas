package de.schrell.cadas.application.heating;

import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Stellt das orthogonale Rasterrouting für Heizkreis-Anschlusswege bereit.
 */
final class HydronicHeatingGridRoutingSupport {

    private static final double EPSILON = 0.001;

    private HydronicHeatingGridRoutingSupport() {
    }

    record Bounds(double minX, double maxX, double minY, double maxY) {
    }

    static final class GeometryScope {

        private final List<List<PlanPoint>> polygons;
        private final Bounds bounds;

        GeometryScope(List<List<PlanPoint>> polygons) {
            this.polygons = polygons.stream().map(List::copyOf).toList();
            this.bounds = new Bounds(
                    polygons.stream().flatMap(List::stream).mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0),
                    polygons.stream().flatMap(List::stream).mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0),
                    polygons.stream().flatMap(List::stream).mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0),
                    polygons.stream().flatMap(List::stream).mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0)
            );
        }

        Bounds bounds() {
            return bounds;
        }

        boolean contains(PlanPoint point) {
            return polygons.stream().anyMatch(polygon -> containsPoint(polygon, point));
        }

        boolean containsSegment(PlanPoint start, PlanPoint end) {
            for (int step = 0; step <= 32; step++) {
                double ratio = step / 32.0;
                PlanPoint point = new PlanPoint(
                        start.xMillimeters() + (end.xMillimeters() - start.xMillimeters()) * ratio,
                        start.yMillimeters() + (end.yMillimeters() - start.yMillimeters()) * ratio
                );
                if (!contains(point)) {
                    return false;
                }
            }
            return true;
        }
    }

    record GridPoint(int ix, int iy) {
        PlanPoint toPlanPoint(double pitch) {
            return new PlanPoint(ix * pitch, iy * pitch);
        }
    }

    record GridEdge(GridPoint a, GridPoint b) {
        GridEdge {
            if (compare(b, a) < 0) {
                GridPoint swap = a;
                a = b;
                b = swap;
            }
        }

        private static int compare(GridPoint first, GridPoint second) {
            int x = Integer.compare(first.ix(), second.ix());
            return x != 0 ? x : Integer.compare(first.iy(), second.iy());
        }
    }

    static final class GridGraph {

        private final double pitch;
        private final Set<GridPoint> nodes;
        private final Map<GridPoint, List<GridPoint>> adjacency;

        private GridGraph(double pitch, Set<GridPoint> nodes, Map<GridPoint, List<GridPoint>> adjacency) {
            this.pitch = pitch;
            this.nodes = nodes;
            this.adjacency = adjacency;
        }

        static GridGraph create(GeometryScope scope, double pitch) {
            Bounds bounds = scope.bounds();
            int minX = (int) Math.floor(bounds.minX() / pitch) - 1;
            int maxX = (int) Math.ceil(bounds.maxX() / pitch) + 1;
            int minY = (int) Math.floor(bounds.minY() / pitch) - 1;
            int maxY = (int) Math.ceil(bounds.maxY() / pitch) + 1;
            Set<GridPoint> nodes = new LinkedHashSet<>();
            for (int ix = minX; ix <= maxX; ix++) {
                for (int iy = minY; iy <= maxY; iy++) {
                    GridPoint point = new GridPoint(ix, iy);
                    if (scope.contains(point.toPlanPoint(pitch))) {
                        nodes.add(point);
                    }
                }
            }
            Map<GridPoint, List<GridPoint>> adjacency = new HashMap<>();
            for (GridPoint node : nodes) {
                List<GridPoint> neighbors = new ArrayList<>();
                for (int[] delta : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    GridPoint neighbor = new GridPoint(node.ix() + delta[0], node.iy() + delta[1]);
                    if (nodes.contains(neighbor) && scope.containsSegment(node.toPlanPoint(pitch), neighbor.toPlanPoint(pitch))) {
                        neighbors.add(neighbor);
                    }
                }
                adjacency.put(node, neighbors);
            }
            return new GridGraph(pitch, nodes, adjacency);
        }

        Optional<GridPoint> nearest(PlanPoint point, GeometryScope scope, Set<GridPoint> forbiddenNodes) {
            GridPoint exact = new GridPoint(
                    (int) Math.round(point.xMillimeters() / pitch),
                    (int) Math.round(point.yMillimeters() / pitch)
            );
            if (nodes.contains(exact) && !forbiddenNodes.contains(exact) && scope.containsSegment(point, exact.toPlanPoint(pitch))) {
                return Optional.of(exact);
            }
            return nodes.stream()
                    .filter(candidate -> !forbiddenNodes.contains(candidate))
                    .filter(candidate -> scope.containsSegment(point, candidate.toPlanPoint(pitch)))
                    .min(Comparator.comparingDouble(candidate -> candidate.toPlanPoint(pitch).distanceTo(point).toMillimeters()));
        }

        List<GridPoint> shortestPath(GridPoint start, GridPoint goal, Set<GridEdge> blockedEdges, boolean blockTouchedNodes) {
            if (!nodes.contains(start) || !nodes.contains(goal)) {
                return List.of();
            }
            Set<GridPoint> blockedNodes = blockTouchedNodes ? blockedNodes(blockedEdges, start, goal) : Set.of();
            PriorityQueue<SearchNode> open = new PriorityQueue<>(Comparator.comparingDouble(SearchNode::score));
            Map<GridPoint, Double> distance = new HashMap<>();
            Map<GridPoint, GridPoint> previous = new HashMap<>();
            open.add(new SearchNode(start, heuristic(start, goal)));
            distance.put(start, 0.0);
            while (!open.isEmpty()) {
                GridPoint current = open.poll().point();
                if (current.equals(goal)) {
                    return compress(reconstruct(previous, current));
                }
                for (GridPoint neighbor : adjacency.getOrDefault(current, List.of())) {
                    GridEdge edge = new GridEdge(current, neighbor);
                    if (blockedEdges.contains(edge) || blockedNodes.contains(neighbor)) {
                        continue;
                    }
                    double turnPenalty = turnPenalty(previous.get(current), current, neighbor);
                    double candidateDistance = distance.get(current) + 1.0 + turnPenalty;
                    if (candidateDistance + EPSILON < distance.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                        distance.put(neighbor, candidateDistance);
                        previous.put(neighbor, current);
                        open.add(new SearchNode(neighbor, candidateDistance + heuristic(neighbor, goal)));
                    }
                }
            }
            return List.of();
        }

        Set<GridPoint> blockedNodes(Set<GridEdge> blockedEdges, GridPoint start, GridPoint goal) {
            Set<GridPoint> blockedNodes = new HashSet<>();
            for (GridEdge edge : blockedEdges) {
                blockedNodes.add(edge.a());
                blockedNodes.add(edge.b());
            }
            if (start != null) {
                blockedNodes.remove(start);
            }
            if (goal != null) {
                blockedNodes.remove(goal);
            }
            return blockedNodes;
        }

        private static List<GridPoint> reconstruct(Map<GridPoint, GridPoint> previous, GridPoint current) {
            ArrayDeque<GridPoint> points = new ArrayDeque<>();
            points.addFirst(current);
            while (previous.containsKey(current)) {
                current = previous.get(current);
                points.addFirst(current);
            }
            return List.copyOf(points);
        }

        private static List<GridPoint> compress(List<GridPoint> points) {
            if (points.size() < 3) {
                return points;
            }
            List<GridPoint> compressed = new ArrayList<>();
            compressed.add(points.getFirst());
            for (int index = 1; index + 1 < points.size(); index++) {
                GridPoint previous = compressed.getLast();
                GridPoint current = points.get(index);
                GridPoint next = points.get(index + 1);
                int dx1 = Integer.compare(current.ix() - previous.ix(), 0);
                int dy1 = Integer.compare(current.iy() - previous.iy(), 0);
                int dx2 = Integer.compare(next.ix() - current.ix(), 0);
                int dy2 = Integer.compare(next.iy() - current.iy(), 0);
                if (dx1 != dx2 || dy1 != dy2) {
                    compressed.add(current);
                }
            }
            compressed.add(points.getLast());
            return List.copyOf(compressed);
        }

        private double heuristic(GridPoint first, GridPoint second) {
            return Math.abs(first.ix() - second.ix()) + Math.abs(first.iy() - second.iy());
        }

        private double turnPenalty(GridPoint previous, GridPoint current, GridPoint next) {
            if (previous == null) {
                return 0.0;
            }
            int dx1 = Integer.compare(current.ix() - previous.ix(), 0);
            int dy1 = Integer.compare(current.iy() - previous.iy(), 0);
            int dx2 = Integer.compare(next.ix() - current.ix(), 0);
            int dy2 = Integer.compare(next.iy() - current.iy(), 0);
            return dx1 == dx2 && dy1 == dy2 ? 0.0 : 0.18;
        }

        private record SearchNode(GridPoint point, double score) {
        }
    }

    private static boolean containsPoint(List<PlanPoint> polygon, PlanPoint point) {
        boolean inside = false;
        int previousIndex = polygon.size() - 1;
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint current = polygon.get(index);
            PlanPoint previous = polygon.get(previousIndex);
            if (pointOnSegment(point, previous, current)) {
                return true;
            }
            boolean intersects = (current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters())
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / (previous.yMillimeters() - current.yMillimeters())
                    + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            previousIndex = index;
        }
        return inside;
    }

    private static boolean pointOnSegment(PlanPoint point, PlanPoint start, PlanPoint end) {
        double cross = orientation(start, end, point);
        if (Math.abs(cross) > EPSILON) {
            return false;
        }
        return point.xMillimeters() >= Math.min(start.xMillimeters(), end.xMillimeters()) - EPSILON
                && point.xMillimeters() <= Math.max(start.xMillimeters(), end.xMillimeters()) + EPSILON
                && point.yMillimeters() >= Math.min(start.yMillimeters(), end.yMillimeters()) - EPSILON
                && point.yMillimeters() <= Math.max(start.yMillimeters(), end.yMillimeters()) + EPSILON;
    }

    private static double orientation(PlanPoint first, PlanPoint second, PlanPoint third) {
        return (second.xMillimeters() - first.xMillimeters()) * (third.yMillimeters() - first.yMillimeters())
                - (second.yMillimeters() - first.yMillimeters()) * (third.xMillimeters() - first.xMillimeters());
    }
}
