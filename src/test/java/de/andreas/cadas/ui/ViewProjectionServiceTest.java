package de.andreas.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.PlanPoint;
import org.junit.jupiter.api.Test;

class ViewProjectionServiceTest {

    private final ViewProjectionService service = new ViewProjectionService();

    @Test
    void projiziertDraufsichtUndSeitenansichtenUnterschiedlich() {
        PlanPoint point = new PlanPoint(1200, 3400);

        ViewProjectionService.ProjectedPoint top = service.project(point, 2600, ViewOrientation.TOP);
        ViewProjectionService.ProjectedPoint front = service.project(point, 2600, ViewOrientation.NORTH);
        ViewProjectionService.ProjectedPoint right = service.project(point, 2600, ViewOrientation.EAST);

        assertEquals(1200.0, top.horizontalMillimeters(), 0.001);
        assertEquals(3400.0, top.verticalMillimeters(), 0.001);
        assertEquals(1200.0, front.horizontalMillimeters(), 0.001);
        assertEquals(-2600.0, front.verticalMillimeters(), 0.001);
        assertEquals(3400.0, right.horizontalMillimeters(), 0.001);
        assertEquals(-2600.0, right.verticalMillimeters(), 0.001);
    }

    @Test
    void erkenntPlanansichten() {
        assertTrue(service.isPlanView(ViewOrientation.TOP));
        assertTrue(service.isPlanView(ViewOrientation.BOTTOM));
    }
}
