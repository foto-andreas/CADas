package de.andreas.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Angle;
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

    @Test
    void projektKannAufEineLeereEinzelEtageZurueckgesetztWerden() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        model.createLevel("Obergeschoss");
        model.defineRoof(new Roof(RoofType.SADDLE, Angle.ofDegrees(38), Length.of(40, LengthUnit.CENTIMETER), true));

        Level resetLevel = model.resetToSingleLevel("Neustart");

        assertEquals(1, model.levels().size());
        assertEquals("Neustart", resetLevel.name());
        assertFalse(model.roof().isPresent());
    }

    @Test
    void projektKannAusEinerMomentaufnahmeWiederhergestelltWerden() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        model.primaryLevel().addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(2000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        ));

        ProjectModel snapshot = model.copy();

        model.createLevel("Obergeschoss");
        model.primaryLevel().removeWall(model.primaryLevel().walls().getFirst().id());
        model.replaceWith(snapshot);

        assertEquals(1, model.levels().size());
        assertEquals(1, model.primaryLevel().walls().size());
        assertNotSame(snapshot.primaryLevel(), model.primaryLevel());
    }

    @Test
    void projektnameWirdBeimWiederherstellenMitUebernommen() {
        ProjectModel model = ProjectModel.withDefaultLevel("Alt", "Erdgeschoss");
        ProjectModel snapshot = ProjectModel.withDefaultLevel("Neu", "Import");

        model.replaceWith(snapshot);

        assertEquals("Neu", model.name());
        assertEquals("Import", model.primaryLevel().name());
    }

    @Test
    void etageKannUmbenanntWerden() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");

        model.renameLevel(model.primaryLevel(), "Keller");

        assertEquals("Keller", model.primaryLevel().name());
    }

    @Test
    void etagennameDarfNichtLeerSein() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");

        assertThrows(IllegalArgumentException.class, () -> model.renameLevel(model.primaryLevel(), "  "));
        assertEquals("Erdgeschoss", model.primaryLevel().name());
    }

    @Test
    void projektnameBleibtNachUngültigerUmbenennungErhalten() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");

        assertThrows(IllegalArgumentException.class, () -> model.rename("  "));

        assertEquals("Haus", model.name());
    }

    @Test
    void etagennameBleibtNachUngültigerDirekterUmbenennungErhalten() {
        Level level = new Level("Erdgeschoss");

        assertThrows(IllegalArgumentException.class, () -> level.rename("  "));

        assertEquals("Erdgeschoss", level.name());
    }

    @Test
    void etagennameMussEindeutigSein() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        model.createLevel("Obergeschoss");

        assertThrows(IllegalArgumentException.class, () -> model.renameLevel(model.primaryLevel(), "Obergeschoss"));
    }

    @Test
    void etageKannNachObenVerschobenWerden() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Level obergeschoss = model.createLevel("Obergeschoss");
        Level dachgeschoss = model.createLevel("Dachgeschoss");

        model.moveLevelUp(obergeschoss);

        assertEquals("Erdgeschoss", model.levels().get(0).name());
        assertEquals("Dachgeschoss", model.levels().get(1).name());
        assertEquals("Obergeschoss", model.levels().get(2).name());
    }

    @Test
    void etageKannNachUntenVerschobenWerden() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Level obergeschoss = model.createLevel("Obergeschoss");
        Level dachgeschoss = model.createLevel("Dachgeschoss");

        model.moveLevelDown(dachgeschoss);

        assertEquals("Erdgeschoss", model.levels().get(0).name());
        assertEquals("Dachgeschoss", model.levels().get(1).name());
        assertEquals("Obergeschoss", model.levels().get(2).name());
    }

    @Test
    void ersteEtageKannNichtNachUntenVerschobenWerden() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        model.createLevel("Obergeschoss");

        model.moveLevelDown(model.primaryLevel());

        assertEquals("Erdgeschoss", model.levels().get(0).name());
    }

    @Test
    void letzteEtageKannNichtNachObenVerschobenWerden() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Level obergeschoss = model.createLevel("Obergeschoss");

        model.moveLevelUp(obergeschoss);

        assertTrue(model.levels().indexOf(obergeschoss) == model.levels().size() - 1);
    }
}
