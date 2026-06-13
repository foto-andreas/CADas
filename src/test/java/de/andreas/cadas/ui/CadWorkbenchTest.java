package de.andreas.cadas.ui;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.infrastructure.dxf.DxfProjectExchangeService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CadWorkbenchTest {

    @BeforeAll
    static void initialisiertJavaFxToolkit() {
        new JFXPanel();
    }

    @Test
    void undoUndWiederherstellenBehaltenZoomUndPosition() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetViewport(1.85, 320.0, -145.0);
            workbench.automationRememberUndoState();
            workbench.automationSetViewport(0.55, -20.0, 480.0);
            return null;
        });

        WorkbenchAutomationSnapshot vorUndo = aufFxThread(workbench::automationSnapshot);
        Assertions.assertEquals(0.55, vorUndo.zoom(), 0.0001);
        Assertions.assertEquals(-20.0, vorUndo.offsetX(), 0.0001);
        Assertions.assertEquals(480.0, vorUndo.offsetY(), 0.0001);

        aufFxThread(() -> {
            workbench.automationInvoke("undo", null);
            return null;
        });

        WorkbenchAutomationSnapshot nachUndo = aufFxThread(workbench::automationSnapshot);
        Assertions.assertEquals(1.85, nachUndo.zoom(), 0.0001);
        Assertions.assertEquals(320.0, nachUndo.offsetX(), 0.0001);
        Assertions.assertEquals(-145.0, nachUndo.offsetY(), 0.0001);

        aufFxThread(() -> {
            workbench.automationInvoke("redo", null);
            return null;
        });

        WorkbenchAutomationSnapshot nachRedo = aufFxThread(workbench::automationSnapshot);
        Assertions.assertEquals(0.55, nachRedo.zoom(), 0.0001);
        Assertions.assertEquals(-20.0, nachRedo.offsetX(), 0.0001);
        Assertions.assertEquals(480.0, nachRedo.offsetY(), 0.0001);
    }

    @Test
    void alterRaumWerkzeugAliasWechseltAufBearbeiten() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetTool("ROOM");
            return null;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        Assertions.assertEquals("EDIT", snapshot.activeTool());
    }

    @Test
    void statushinweisPasstZumAktivenWerkzeug() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        WorkbenchAutomationSnapshot wand = aufFxThread(workbench::automationSnapshot);
        Assertions.assertTrue(wand.statusText().contains("Linksklick startet"));

        aufFxThread(() -> {
            workbench.automationSetTool("EDIT");
            return null;
        });

        WorkbenchAutomationSnapshot bearbeiten = aufFxThread(workbench::automationSnapshot);
        Assertions.assertTrue(bearbeiten.statusText().contains("Linksklick wählt aus"));
    }

    @Test
    void belagsauswahlWechseltMitRaumUndWandSauberZwischenKontexten() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSelect("ROOM", 0, false);
            workbench.automationSetSurfaceType("CEILING");
            return null;
        });
        WorkbenchAutomationSnapshot nurRaum = aufFxThread(workbench::automationSnapshot);
        Assertions.assertEquals("CEILING", nurRaum.surfaceType());
        Assertions.assertEquals("FLOOR,CEILING", nurRaum.surfaceTypeOptions());

        aufFxThread(() -> {
            workbench.automationSelect("WALL", 0, true);
            return null;
        });
        WorkbenchAutomationSnapshot raumUndWand = aufFxThread(workbench::automationSnapshot);
        Assertions.assertEquals("WALL_INTERIOR", raumUndWand.surfaceType());
        Assertions.assertEquals("WALL_INTERIOR", raumUndWand.surfaceTypeOptions());

        aufFxThread(() -> {
            workbench.automationSelect("ROOM", 0, false);
            return null;
        });
        WorkbenchAutomationSnapshot wiederNurRaum = aufFxThread(workbench::automationSnapshot);
        Assertions.assertEquals("CEILING", wiederNurRaum.surfaceType());
        Assertions.assertEquals("FLOOR,CEILING", wiederNurRaum.surfaceTypeOptions());
    }

    @Test
    void belagsauswahlZeigtBeiNurWandNurAussenwandAn() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSelect("WALL", 0, false);
            return null;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        Assertions.assertEquals("WALL_EXTERIOR", snapshot.surfaceType());
        Assertions.assertEquals("WALL_EXTERIOR", snapshot.surfaceTypeOptions());
    }

    @Test
    void importierteInnenwandFliesenAktualisierenDie3dAnsichtMitFugen() throws Exception {
        Path projektDatei = erzeugeProjektMitInnenwandfliesenAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetWorkspace("THREE_D");
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);

        Assertions.assertTrue(
                snapshot.threeDBodyCount() > 40,
                "Innenwand-Fliesen müssen Fugen in die 3D-Szene bringen, Körperzahl war " + snapshot.threeDBodyCount() + "."
        );
    }

    private Path erzeugeEinfachesProjektAlsDxf() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Testhaus", "Erdgeschoss");
        var level = project.primaryLevel();
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(4000, 0), new PlanPoint(4000, 3000)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(4000, 3000), new PlanPoint(0, 3000)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(0, 3000), new PlanPoint(0, 0)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        level.addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(100, 100),
                new PlanPoint(3900, 2900),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        Path datei = Files.createTempFile("cadas-workbench-", ".dxf");
        new DxfProjectExchangeService().exportProject(project, datei);
        return datei;
    }

    private Path erzeugeProjektMitInnenwandfliesenAlsDxf() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Fliesentest", "Erdgeschoss");
        var level = project.primaryLevel();
        Wall gefliesteWand = Wall.create(new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER));
        level.addWall(gefliesteWand);
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(4000, 0), new PlanPoint(4000, 3000)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(4000, 3000), new PlanPoint(0, 3000)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(0, 3000), new PlanPoint(0, 0)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        level.addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(100, 100),
                new PlanPoint(3900, 2900),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, gefliesteWand.id().toString());
        stack.addLayer(SurfaceLayer.create(
                "Fliese",
                Length.of(12, LengthUnit.MILLIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(30, LengthUnit.CENTIMETER),
                Length.of(2, LengthUnit.MILLIMETER)
        ));
        level.addSurfaceLayerStack(stack);
        Path datei = Files.createTempFile("cadas-workbench-fliesen-", ".dxf");
        new DxfProjectExchangeService().exportProject(project, datei);
        return datei;
    }

    private static <T> T aufFxThread(FxCallable<T> aufgabe) throws Exception {
        FutureTask<T> task = new FutureTask<>(aufgabe::call);
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
        try {
            return task.get();
        } catch (ExecutionException exception) {
            Throwable ursache = exception.getCause();
            if (ursache instanceof Exception bekannteException) {
                throw bekannteException;
            }
            if (ursache instanceof Error fehler) {
                throw fehler;
            }
            throw new RuntimeException(ursache);
        }
    }

    @FunctionalInterface
    private interface FxCallable<T> {
        T call() throws Exception;
    }
}
