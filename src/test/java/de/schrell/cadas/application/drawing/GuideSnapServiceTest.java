package de.schrell.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Wall;

import java.util.List;

import org.junit.jupiter.api.Test;

class GuideSnapServiceTest {

    private final GuideSnapService service = new GuideSnapService();
    private final GuideSnapTargets targets = new GuideSnapTargets(List.of(2_000.0), List.of(500.0));

    @Test
    void rastetWandMitMittellinieOderAussenkanteAnHilfslinie() {
        PlanSegment centerSnapped = service.snapWallSegment(
                new PlanSegment(new PlanPoint(0, 470), new PlanPoint(1_950, 470)),
                Length.ofMillimeters(200),
                targets,
                Length.ofMillimeters(120)
        );
        PlanSegment edgeSnapped = service.snapWallSegment(
                new PlanSegment(new PlanPoint(0, 390), new PlanPoint(1_700, 390)),
                Length.ofMillimeters(200),
                targets,
                Length.ofMillimeters(120)
        );

        assertEquals(500.0, centerSnapped.start().yMillimeters(), 0.001);
        assertEquals(2_000.0, centerSnapped.end().xMillimeters(), 0.001);
        assertEquals(400.0, edgeSnapped.start().yMillimeters(), 0.001);
    }

    @Test
    void rastetOeffnungMitAnfangEndeOderMitteAnHilfslinie() {
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(5_000, 0)),
                Length.ofMillimeters(200),
                Length.ofMillimeters(2_800)
        );

        Length startSnapped = service.snapOpeningOffset(wall, Length.ofMillimeters(1_930), Length.ofMillimeters(1_000), targets, Length.ofMillimeters(120));
        Length centerSnapped = service.snapOpeningOffset(wall, Length.ofMillimeters(1_480), Length.ofMillimeters(1_000), targets, Length.ofMillimeters(120));
        Length endSnapped = service.snapOpeningOffset(wall, Length.ofMillimeters(1_050), Length.ofMillimeters(1_000), targets, Length.ofMillimeters(120));

        assertEquals(2_000.0, startSnapped.toMillimeters(), 0.001);
        assertEquals(1_500.0, centerSnapped.toMillimeters(), 0.001);
        assertEquals(1_000.0, endSnapped.toMillimeters(), 0.001);
    }

    @Test
    void rastetVerschobeneWandMitAussenkanteUndMittellinieAnHilfslinie() {
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(1_000, 0)),
                Length.ofMillimeters(200),
                Length.ofMillimeters(2_800)
        );

        GuideSnapService.Translation translation = service.snapWallTranslation(
                List.of(wall),
                950,
                390,
                targets,
                Length.ofMillimeters(120)
        );

        assertEquals(1_000.0, translation.deltaX(), 0.001);
        assertEquals(400.0, translation.deltaY(), 0.001);
    }
}
