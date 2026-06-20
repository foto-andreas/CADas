package de.schrell.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.schrell.cadas.domain.geometry.Angle;
import de.schrell.cadas.domain.geometry.Grid;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DraftingServiceTest {

    private final DraftingService draftingService = new DraftingService();

    @Test
    void erzwingtOhneShiftDenOrthogonalenModus() {
        DraftingConstraints constraints = constraints(true, Optional.empty(), Optional.empty());

        PlanSegment segment = draftingService.createSegment(new PlanPoint(0, 0), new PlanPoint(1300, 700), constraints);

        assertEquals(0.0, segment.end().yMillimeters());
        assertEquals(1300.0, segment.end().xMillimeters());
    }

    @Test
    void uebernimmtFreieLaengeUndFreienWinkel() {
        DraftingConstraints constraints = constraints(false, Optional.of(Length.of(2, LengthUnit.METER)), Optional.of(Angle.ofDegrees(45)));

        PlanSegment segment = draftingService.createSegment(new PlanPoint(0, 0), new PlanPoint(500, 100), constraints);

        assertEquals(1414.21, segment.end().xMillimeters(), 0.2);
        assertEquals(1414.21, segment.end().yMillimeters(), 0.2);
    }

    @Test
    void uebernimmtNurDieManuelleLaengeBeiVorhandenerRichtung() {
        DraftingConstraints constraints = constraints(false, Optional.of(Length.of(3, LengthUnit.METER)), Optional.empty());

        PlanSegment segment = draftingService.createSegment(new PlanPoint(0, 0), new PlanPoint(1000, 1000), constraints);

        assertEquals(3000.0, segment.length().toMillimeters(), 0.2);
        assertEquals(45.0, segment.angle().degrees(), 0.2);
    }

    private DraftingConstraints constraints(boolean orthogonalMode, Optional<Length> length, Optional<Angle> angle) {
        return new DraftingConstraints(
                orthogonalMode,
                true,
                true,
                new Grid(Length.of(25, LengthUnit.CENTIMETER)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(12, LengthUnit.CENTIMETER),
                length,
                angle
        );
    }
}
