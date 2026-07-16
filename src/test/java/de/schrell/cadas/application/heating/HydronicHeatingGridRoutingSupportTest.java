package de.schrell.cadas.application.heating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.heating.HydronicHeatingGridRoutingSupport.GeometryScope;
import de.schrell.cadas.application.heating.HydronicHeatingGridRoutingSupport.GridEdge;
import de.schrell.cadas.application.heating.HydronicHeatingGridRoutingSupport.GridGraph;
import de.schrell.cadas.application.heating.HydronicHeatingGridRoutingSupport.GridPoint;
import de.schrell.cadas.domain.geometry.PlanPoint;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HydronicHeatingGridRoutingSupportTest {

    private static final double PITCH = 100.0;

    @Test
    void erkenntPunkteUndSegmenteInnerhalbMehrererTeilflächen() {
        GeometryScope scope = new GeometryScope(List.of(
                rectangle(0, 0, 400, 400),
                rectangle(600, 0, 800, 200)
        ));

        assertEquals(0.0, scope.bounds().minX());
        assertEquals(800.0, scope.bounds().maxX());
        assertTrue(scope.contains(new PlanPoint(200, 200)));
        assertTrue(scope.contains(new PlanPoint(700, 100)));
        assertFalse(scope.contains(new PlanPoint(500, 100)));
        assertTrue(scope.containsSegment(new PlanPoint(100, 100), new PlanPoint(300, 300)));
        assertFalse(scope.containsSegment(new PlanPoint(300, 100), new PlanPoint(700, 100)));
    }

    @Test
    void findetNächstenErreichbarenRasterpunktUndBeachtetSperren() {
        GeometryScope scope = new GeometryScope(List.of(rectangle(0, 0, 400, 400)));
        GridGraph graph = GridGraph.create(scope, PITCH);

        assertEquals(new GridPoint(1, 1), graph.nearest(new PlanPoint(110, 90), scope, Set.of()).orElseThrow());
        assertEquals(new GridPoint(1, 0), graph.nearest(
                new PlanPoint(110, 90), scope, Set.of(new GridPoint(1, 1))).orElseThrow());
        assertTrue(graph.nearest(new PlanPoint(-100, -100), scope, Set.of()).isEmpty());
    }

    @Test
    void komprimiertGeradeWegeUndUmgehtGesperrteKanten() {
        GeometryScope scope = new GeometryScope(List.of(rectangle(0, 0, 400, 400)));
        GridGraph graph = GridGraph.create(scope, PITCH);
        GridPoint start = new GridPoint(0, 0);
        GridPoint goal = new GridPoint(4, 0);

        assertEquals(List.of(start, goal), graph.shortestPath(start, goal, Set.of(), false));

        GridEdge blocked = new GridEdge(new GridPoint(2, 0), new GridPoint(1, 0));
        List<GridPoint> detour = graph.shortestPath(start, goal, Set.of(blocked), false);
        assertEquals(start, detour.getFirst());
        assertEquals(goal, detour.getLast());
        assertTrue(detour.size() >= 4);
        assertFalse(detour.stream().allMatch(point -> point.iy() == 0));
    }

    @Test
    void blockiertBerührteKnotenMitAusnahmeVonStartUndZiel() {
        GeometryScope scope = new GeometryScope(List.of(rectangle(0, 0, 400, 400)));
        GridGraph graph = GridGraph.create(scope, PITCH);
        GridPoint start = new GridPoint(0, 0);
        GridPoint middle = new GridPoint(1, 0);
        GridPoint goal = new GridPoint(2, 0);
        Set<GridEdge> usedEdges = Set.of(new GridEdge(start, middle), new GridEdge(middle, goal));

        assertEquals(Set.of(middle), graph.blockedNodes(usedEdges, start, goal));
        assertFalse(graph.shortestPath(start, goal, usedEdges, true).isEmpty());
        assertTrue(graph.shortestPath(new GridPoint(-1, 0), goal, Set.of(), false).isEmpty());
    }

    @Test
    void leereGeometrieErzeugtKeineRoute() {
        GeometryScope scope = new GeometryScope(List.of());
        GridGraph graph = GridGraph.create(scope, PITCH);

        assertEquals(0.0, scope.bounds().minX());
        assertFalse(scope.contains(new PlanPoint(0, 0)));
        assertTrue(graph.nearest(new PlanPoint(0, 0), scope, Set.of()).isEmpty());
        assertTrue(graph.shortestPath(new GridPoint(0, 0), new GridPoint(1, 0), Set.of(), false).isEmpty());
    }

    private List<PlanPoint> rectangle(double minX, double minY, double maxX, double maxY) {
        return List.of(
                new PlanPoint(minX, minY),
                new PlanPoint(maxX, minY),
                new PlanPoint(maxX, maxY),
                new PlanPoint(minX, maxY)
        );
    }
}
