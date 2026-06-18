package de.andreas.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Wall;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class WallSnapServiceTest {

    private final WallSnapService service = new WallSnapService();

    @Test
    void liefertAchsenAussenkantenUndEndkantenAlsFangziele() {
        Wall horizontal = wall(1_000, 2_000, 5_000, 2_000, 200);
        Wall vertical = wall(7_000, 500, 7_000, 3_500, 300);

        GuideSnapTargets targets = service.targets(List.of(horizontal, vertical), Set.of());

        assertTrue(targets.horizontalGuides().containsAll(List.of(1_900.0, 2_000.0, 2_100.0, 500.0, 3_500.0)));
        assertTrue(targets.verticalGuides().containsAll(List.of(1_000.0, 5_000.0, 6_850.0, 7_000.0, 7_150.0)));
    }

    @Test
    void schliesstVerschobeneWandAusDenEigenenFangzielenAus() {
        Wall selected = wall(0, 0, 4_000, 0, 200);

        GuideSnapTargets targets = service.targets(List.of(selected), Set.of(selected.id()));

        assertEquals(List.of(), targets.verticalGuides());
        assertEquals(List.of(), targets.horizontalGuides());
        assertFalse(targets.verticalGuides().contains(0.0));
    }

    private Wall wall(double startX, double startY, double endX, double endY, double thickness) {
        return Wall.create(
                new PlanSegment(new PlanPoint(startX, startY), new PlanPoint(endX, endY)),
                Length.ofMillimeters(thickness),
                Length.ofMillimeters(2_800)
        );
    }
}
