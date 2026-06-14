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

    @Test
    void innenansichtNutztDas3dFensterMitRaumkamera() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetWorkspace("INTERIOR");
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);

        Assertions.assertTrue(snapshot.threeDHasContent());
        Assertions.assertTrue(
                snapshot.threeDCameraStatus().contains("3D Innenansicht:"),
                "Status war: " + snapshot.threeDCameraStatus()
                        + " | Räume: " + snapshot.roomCount()
                        + " | Hinweis: " + snapshot.statusText()
        );
        Assertions.assertTrue(
                snapshot.threeDCameraStatus().contains("Augenhöhe 1,60 m"),
                "Status war: " + snapshot.threeDCameraStatus()
        );
        Assertions.assertTrue(
                snapshot.threeDCameraStatus().contains("Sichtwinkel 64°"),
                "Status war: " + snapshot.threeDCameraStatus()
        );

        aufFxThread(() -> {
            workbench.automationInvoke("threeDZoomIn", null);
            return null;
        });
        WorkbenchAutomationSnapshot gezoomt = aufFxThread(workbench::automationSnapshot);

        Assertions.assertTrue(
                gezoomt.threeDCameraStatus().contains("3D Innenansicht:")
                        && gezoomt.threeDCameraStatus().contains("Sichtwinkel 59°"),
                "Status war: " + gezoomt.threeDCameraStatus()
        );

        WorkbenchAutomationSnapshot bewegt = aufFxThread(() -> {
            workbench.automationInvoke("threeDPanUp", null);
            return workbench.automationSnapshot();
        });

        Assertions.assertTrue(
                bewegt.threeDCameraStatus().contains("3D Innenansicht:")
                        && bewegt.threeDCameraStatus().contains("Position 2,00/1,02 m"),
                "Status war: " + bewegt.threeDCameraStatus()
        );

        WorkbenchAutomationSnapshot nachSnapshot = aufFxThread(() -> {
            Path snapshotDatei = Files.createTempFile("cadas-innenkamera-", ".png");
            workbench.automationInvoke("exportSubSceneSnapshot", snapshotDatei);
            return workbench.automationSnapshot();
        });

        Assertions.assertTrue(
                nachSnapshot.threeDCameraStatus().contains("Position 2,00/1,02 m"),
                "Snapshot-Export darf die Innenposition nicht zurücksetzen: " + nachSnapshot.threeDCameraStatus()
        );

        WorkbenchAutomationSnapshot begrenzt = aufFxThread(() -> {
            for (int index = 0; index < 10; index++) {
                workbench.automationInvoke("threeDPanUp", null);
            }
            return workbench.automationSnapshot();
        });

        Assertions.assertTrue(
                begrenzt.threeDCameraStatus().contains("Position 2,00/0,25 m"),
                "Status war: " + begrenzt.threeDCameraStatus()
        );

        WorkbenchAutomationSnapshot weitwinkel = aufFxThread(() -> {
            for (int index = 0; index < 10; index++) {
                workbench.automationInvoke("threeDZoomOut", null);
            }
            return workbench.automationSnapshot();
        });

        Assertions.assertTrue(
                weitwinkel.threeDCameraStatus().contains("Sichtwinkel 115°"),
                "Status war: " + weitwinkel.threeDCameraStatus()
        );

        WorkbenchAutomationSnapshot gedreht = aufFxThread(() -> {
            workbench.automationInvoke("threeDOrbitRight", null);
            workbench.automationInvoke("diagnose3D", null);
            return workbench.automationSnapshot();
        });

        Assertions.assertTrue(
                gedreht.threeDCameraStatus().contains("3D Innenansicht:")
                        && gedreht.threeDCameraStatus().contains("Blick 15,0° / 0,0°"),
                "Status war: " + gedreht.threeDCameraStatus()
        );
        Assertions.assertTrue(
                gedreht.statusText().contains("camPos=[Translate"),
                "Kameratransform muss zuerst die feste Innenposition setzen: " + gedreht.statusText()
        );
    }

    @Test
    void einheitenwechselKonvertiertSichtbarenWertOhneLängenänderung() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        aufFxThread(() -> {
            Assertions.assertEquals("CENTIMETER", workbench.automationUnit("surfaceMinimumEdgeWidth"));
            Assertions.assertEquals("8", workbench.automationFieldValue("surfaceMinimumEdgeWidth"));

            workbench.automationSetUnit("surfaceMinimumEdgeWidth", "MILLIMETER");
            Assertions.assertEquals("MILLIMETER", workbench.automationUnit("surfaceMinimumEdgeWidth"));
            Assertions.assertEquals("80", workbench.automationFieldValue("surfaceMinimumEdgeWidth"));

            workbench.automationSetUnit("surfaceMinimumEdgeWidth", "METER");
            Assertions.assertEquals("METER", workbench.automationUnit("surfaceMinimumEdgeWidth"));
            Assertions.assertEquals("0,08", workbench.automationFieldValue("surfaceMinimumEdgeWidth"));

            workbench.automationSetUnit("surfaceMinimumEdgeWidth", "CENTIMETER");
            Assertions.assertEquals("CENTIMETER", workbench.automationUnit("surfaceMinimumEdgeWidth"));
            Assertions.assertEquals("8", workbench.automationFieldValue("surfaceMinimumEdgeWidth"));

            workbench.automationSetUnit("surfaceJointWidth", "CENTIMETER");
            Assertions.assertEquals("CENTIMETER", workbench.automationUnit("surfaceJointWidth"));
            Assertions.assertEquals("0,2", workbench.automationFieldValue("surfaceJointWidth"));
            return null;
        });
    }

    @Test
    void belagseinheitenBleibenNachUpdateInGewählterEinheitStabil() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("WALL", 0, false);
            instanz.automationSetUnit("surfaceMinimumEdgeWidth", "MILLIMETER");
            instanz.automationSetField("surfaceMinimumEdgeWidth", "92");
            instanz.automationInvoke("addSurfaceLayer", null);
            instanz.automationSelectSurfaceLayer(0);
            return instanz;
        });

        aufFxThread(() -> {
            Assertions.assertEquals("MILLIMETER", workbench.automationUnit("surfaceMinimumEdgeWidth"));
            Assertions.assertEquals("92", workbench.automationFieldValue("surfaceMinimumEdgeWidth"));
            workbench.automationInvoke("updateSurfaceLayer", null);
            Assertions.assertEquals("MILLIMETER", workbench.automationUnit("surfaceMinimumEdgeWidth"));
            Assertions.assertEquals("92", workbench.automationFieldValue("surfaceMinimumEdgeWidth"));
            return null;
        });

        Path exportDatei = Files.createTempFile("cadas-belagseinheit-", ".dxf");
        aufFxThread(() -> {
            workbench.automationInvoke("exportProjectDxf", exportDatei);
            return null;
        });

        SurfaceLayer importedLayer = new DxfProjectExchangeService()
                .importProject(exportDatei, "Einheitentest")
                .primaryLevel()
                .surfaceLayerStacks()
                .getFirst()
                .layers()
                .getFirst();
        Assertions.assertEquals(Length.of(92, LengthUnit.MILLIMETER), importedLayer.minimumEdgeWidth());
    }

    @Test
    void ausgewählteBauteileSynchronisierenWerteInAktuellerEinheit() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("WALL", 0, false);
            instanz.automationSetUnit("wallThickness", "MILLIMETER");
            instanz.automationSetField("wallThickness", "92");
            instanz.automationInvoke("applySelectionProperties", null);
            instanz.automationSelect("WALL", 0, false);
            return instanz;
        });

        aufFxThread(() -> {
            Assertions.assertEquals("MILLIMETER", workbench.automationUnit("wallThickness"));
            Assertions.assertEquals("92", workbench.automationFieldValue("wallThickness"));
            workbench.automationInvoke("applySelectionProperties", null);
            Assertions.assertEquals("MILLIMETER", workbench.automationUnit("wallThickness"));
            Assertions.assertEquals("92", workbench.automationFieldValue("wallThickness"));
            return null;
        });

        Path exportDatei = Files.createTempFile("cadas-wandeinheit-", ".dxf");
        aufFxThread(() -> {
            workbench.automationInvoke("exportProjectDxf", exportDatei);
            return null;
        });

        Wall importedWall = new DxfProjectExchangeService()
                .importProject(exportDatei, "Einheitentest")
                .primaryLevel()
                .walls()
                .getFirst();
        Assertions.assertEquals(Length.of(92, LengthUnit.MILLIMETER), importedWall.thickness());
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
