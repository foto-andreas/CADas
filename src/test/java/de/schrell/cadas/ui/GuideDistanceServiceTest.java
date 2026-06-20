package de.schrell.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.List;

import org.junit.jupiter.api.Test;

class GuideDistanceServiceTest {

    private final GuideDistanceService service = new GuideDistanceService();

    @Test
    void liefertSortierteAbstaendeNurZuParallelenHilfslinien() {
        List<GuideLine> guides = List.of(
                new GuideLine(GuideOrientation.VERTICAL, 1_000),
                new GuideLine(GuideOrientation.HORIZONTAL, 1_500),
                new GuideLine(GuideOrientation.VERTICAL, 4_000)
        );

        List<GuideDistanceService.GuideDistance> distances = service.distancesToParallelGuides(
                guides,
                GuideOrientation.VERTICAL,
                2_500
        );

        assertEquals(2, distances.size());
        assertEquals(1_500.0, distances.getFirst().distance().toMillimeters(), 0.001);
        assertEquals(1_500.0, distances.get(1).distance().toMillimeters(), 0.001);
        assertEquals(1_000.0, distances.getFirst().guideWorldMillimeters(), 0.001);
    }

    @Test
    void ignoriertHilfslinieAnIdentischerPosition() {
        List<GuideDistanceService.GuideDistance> distances = service.distancesToParallelGuides(
                List.of(new GuideLine(GuideOrientation.HORIZONTAL, 800)),
                GuideOrientation.HORIZONTAL,
                800
        );

        assertEquals(List.of(), distances);
    }

    @Test
    void waehltUnabhaengigVonDerReihenfolgeDenKleinstenNormalabstand() {
        GuideLine weiterEntfernt = new GuideLine(GuideOrientation.VERTICAL, 0);
        GuideLine amNaechsten = new GuideLine(GuideOrientation.VERTICAL, 100);
        GuideLine horizontal = new GuideLine(GuideOrientation.HORIZONTAL, 1_000);

        GuideLine result = service.nearestGuide(
                List.of(weiterEntfernt, horizontal, amNaechsten),
                new PlanPoint(70, 100),
                Length.ofMillimeters(120)
        ).orElseThrow();

        assertEquals(amNaechsten, result);
    }
}
