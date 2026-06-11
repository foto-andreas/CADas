package de.andreas.cadas.application.room;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.andreas.cadas.domain.geometry.PlanPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrthogonalPolygonDecompositionServiceTest {

    private final OrthogonalPolygonDecompositionService service = new OrthogonalPolygonDecompositionService();

    @Test
    void zerlegtLfoermigePolygoneInMehrereRechtecke() {
        List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles = service.decompose(List.of(
                new PlanPoint(100, 100),
                new PlanPoint(4900, 100),
                new PlanPoint(4900, 1400),
                new PlanPoint(2900, 1400),
                new PlanPoint(2900, 3900),
                new PlanPoint(100, 3900)
        ));

        assertEquals(2, rectangles.size());
    }
}
