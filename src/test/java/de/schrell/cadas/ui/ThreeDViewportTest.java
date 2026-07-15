package de.schrell.cadas.ui;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Wall;
import java.util.Set;
import java.util.concurrent.Callable;
import javafx.scene.Scene;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ThreeDViewportTest {

    @BeforeAll
    static void initialisiertJavaFxToolkit() {
        JavaFxTestSupport.initialisieren();
    }

    @Test
    void aktiviertNeueEtagenStandardmaessigInDer3dAnsicht() throws Exception {
        ThreeDViewport viewport = aufFxThread(() -> {
            ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
            project.primaryLevel().addWall(Wall.create(
                    new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4_000, 0)),
                    Length.of(20, LengthUnit.CENTIMETER),
                    Length.of(2.6, LengthUnit.METER)
            ));
            var obergeschoss = project.createLevel("Obergeschoss");
            obergeschoss.addWall(Wall.create(
                    new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4_000, 0)),
                    Length.of(20, LengthUnit.CENTIMETER),
                    Length.of(2.6, LengthUnit.METER)
            ));
            ThreeDViewport instanz = new ThreeDViewport(ignored -> { }, () -> { });
            new Scene(instanz, 900, 640);
            instanz.applyCss();
            instanz.layout();
            instanz.syncLevels(project.levels(), project.primaryLevel().name());
            instanz.refresh(project);
            int alleEtagen = instanz.renderedBodyCount();
            Assertions.assertTrue(alleEtagen >= 2, "Beide Etagen müssen initial sichtbar sein.");
            instanz.setVisibleLevels(Set.of(project.primaryLevel().name()));
            Assertions.assertTrue(instanz.renderedBodyCount() < alleEtagen,
                    "Nach Ausblendung der zweiten Etage muss die Körperzahl sinken.");
            return instanz;
        });

        Assertions.assertNotNull(viewport);
    }

    private static <T> T aufFxThread(Callable<T> aufgabe) throws Exception {
        return JavaFxTestSupport.aufFxThread(aufgabe);
    }
}
