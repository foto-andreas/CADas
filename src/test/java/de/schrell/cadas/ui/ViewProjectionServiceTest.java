package de.schrell.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.PlanPoint;
import org.junit.jupiter.api.Test;

class ViewProjectionServiceTest {

    @Test
    void projiziertDraufsichtUndSeitenansichtenUnterschiedlich() {
        PlanPoint point = new PlanPoint(1200, 3400);

        ViewProjectionService.ProjectedPoint top = ViewProjectionService.project(point, 2600, ViewOrientation.TOP);
        ViewProjectionService.ProjectedPoint front = ViewProjectionService.project(point, 2600, ViewOrientation.NORTH);
        ViewProjectionService.ProjectedPoint right = ViewProjectionService.project(point, 2600, ViewOrientation.EAST);

        assertEquals(1200.0, top.horizontalMillimeters(), 0.001);
        assertEquals(3400.0, top.verticalMillimeters(), 0.001);
        assertEquals(1200.0, front.horizontalMillimeters(), 0.001);
        assertEquals(-2600.0, front.verticalMillimeters(), 0.001);
        assertEquals(3400.0, right.horizontalMillimeters(), 0.001);
        assertEquals(-2600.0, right.verticalMillimeters(), 0.001);
    }

    @Test
    void erkenntPlanansichten() {
        assertTrue(ViewProjectionService.isPlanView(ViewOrientation.TOP));
        assertTrue(ViewProjectionService.isPlanView(ViewOrientation.BOTTOM));
    }
}
