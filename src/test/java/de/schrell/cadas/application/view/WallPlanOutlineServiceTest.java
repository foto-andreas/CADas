package de.schrell.cadas.application.view;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Wall;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WallPlanOutlineServiceTest {

    private final WallPlanOutlineService service = new WallPlanOutlineService();

    @Test
    void erzeugtBuednigenWandkoerperMitEndverlaengerung() {
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(1_000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.5, LengthUnit.METER)
        );

        List<PlanPoint> outline = service.outline(wall);

        assertEquals(List.of(
                new PlanPoint(-100, 100),
                new PlanPoint(1_100, 100),
                new PlanPoint(1_100, -100),
                new PlanPoint(-100, -100)
        ), outline);
    }
}
