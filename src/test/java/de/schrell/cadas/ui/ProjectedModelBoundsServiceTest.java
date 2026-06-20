package de.schrell.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.Wall;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectedModelBoundsServiceTest {

    private final ProjectedModelBoundsService service = new ProjectedModelBoundsService();

    @Test
    void liefertGrundrissgrenzenInDerDraufsicht() {
        Level level = new Level("Erdgeschoss");
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));

        Optional<ProjectedModelBoundsService.ProjectedBounds> bounds = service.bounds(level, ViewOrientation.TOP);

        assertTrue(bounds.isPresent());
        assertEquals(0.0, bounds.orElseThrow().minHorizontalMillimeters(), 0.001);
        assertEquals(4000.0, bounds.orElseThrow().maxHorizontalMillimeters(), 0.001);
    }

    @Test
    void beruecksichtigtRaumhoehenInSeitenansichten() {
        Level level = new Level("Dachgeschoss");
        level.addRoom(Room.rectangular(
                "Dachzimmer",
                new PlanPoint(0, 0),
                new PlanPoint(3000, 5000),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                new SlopedCeilingProfile(SlopedCeilingSide.NORTH, Length.of(1.0, LengthUnit.METER))
        ));

        ProjectedModelBoundsService.ProjectedBounds bounds = service.bounds(level, ViewOrientation.EAST).orElseThrow();

        assertEquals(-2800.0, bounds.minVerticalMillimeters(), 0.001);
        assertEquals(0.0, bounds.maxVerticalMillimeters(), 0.001);
        assertEquals(0.0, bounds.minHorizontalMillimeters(), 0.001);
        assertEquals(5000.0, bounds.maxHorizontalMillimeters(), 0.001);
    }
}
