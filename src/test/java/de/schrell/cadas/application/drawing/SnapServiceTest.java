package de.schrell.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.schrell.cadas.domain.geometry.Grid;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Wall;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class SnapServiceTest {

    private final SnapService snapService = new SnapService();

    @Test
    void snapptAufDasRasterWennKeinEndpunktInDerNaheLiegt() {
        PlanPoint snapped = snapService.snap(
                new PlanPoint(245, 255),
                constraints(true, true),
                List.of()
        );

        assertEquals(250.0, snapped.xMillimeters());
        assertEquals(250.0, snapped.yMillimeters());
    }

    @Test
    void bevorzugtVorhandeneEndpunkteVorDemRaster() {
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(1000, 1000), new PlanPoint(2000, 1000)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );

        PlanPoint snapped = snapService.snap(
                new PlanPoint(1930, 980),
                constraints(true, true),
                List.of(wall)
        );

        assertEquals(2000.0, snapped.xMillimeters());
        assertEquals(1000.0, snapped.yMillimeters());
    }

    private DraftingConstraints constraints(boolean snapToGrid, boolean snapToEndpoints) {
        return new DraftingConstraints(
                true,
                snapToGrid,
                snapToEndpoints,
                new Grid(Length.of(25, LengthUnit.CENTIMETER)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(12, LengthUnit.CENTIMETER),
                Optional.empty(),
                Optional.empty()
        );
    }
}
