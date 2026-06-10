package de.andreas.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import org.junit.jupiter.api.Test;

class ProjectModelTest {

    @Test
    void erzeugtEinProjektMitStandardEtage() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");

        assertEquals("Haus", model.name());
        assertEquals(1, model.levels().size());
        assertEquals("Erdgeschoss", model.primaryLevel().name());
    }

    @Test
    void etagenSpeichernWaende() {
        Level level = new Level("Obergeschoss");
        level.addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(3000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));

        assertEquals(1, level.walls().size());
        assertEquals(3000.0, level.walls().getFirst().axis().length().toMillimeters(), 0.1);
    }

    @Test
    void projektKannWeitereEtagenAnlegen() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");

        Level level = model.createLevel("Obergeschoss");

        assertEquals(2, model.levels().size());
        assertEquals("Obergeschoss", level.name());
    }
}
