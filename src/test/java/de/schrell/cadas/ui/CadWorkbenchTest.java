package de.schrell.cadas.ui;

import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;
import de.schrell.cadas.infrastructure.dxf.DxfProjectExchangeService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CadWorkbenchTest {

    @BeforeAll
    static void initialisiertJavaFxToolkit() {
        new JFXPanel();
    }

    @Test
    void startetMitEinemZentimeterRasterweite() throws Exception {
        CadWorkbench workbench = aufFxThread(CadWorkbench::new);

        Assertions.assertEquals("1", aufFxThread(() -> workbench.automationFieldValue("grid")));
        Assertions.assertEquals("CENTIMETER", aufFxThread(() -> workbench.automationUnit("grid")));
    }

    @Test
    void startetMitZentimeterFürAlleLängeneingaben() throws Exception {
        CadWorkbench workbench = aufFxThread(CadWorkbench::new);
        Map<String, String> expectedValues = Map.ofEntries(
                Map.entry("grid", "1"),
                Map.entry("length", ""),
                Map.entry("wallThickness", "17,5"),
                Map.entry("wallHeight", "275"),
                Map.entry("endpointHeight", "275"),
                Map.entry("roomHeight", "260"),
                Map.entry("floorThickness", "18"),
                Map.entry("ceilingThickness", "0,1"),
                Map.entry("kneeWallHeight", "100"),
                Map.entry("doorWidth", "101"),
                Map.entry("doorHeight", "201"),
                Map.entry("threshold", "0"),
                Map.entry("windowWidth", "120"),
                Map.entry("windowHeight", "120"),
                Map.entry("sillHeight", "90"),
                Map.entry("stairHeight", "280"),
                Map.entry("stairStartLanding", "0"),
                Map.entry("stairEndLanding", "0"),
                Map.entry("roomObjectWidth", "90"),
                Map.entry("roomObjectDepth", "90"),
                Map.entry("roomObjectHeight", "200"),
                Map.entry("roomObjectBaseElevation", "0"),
                Map.entry("floorExtensionThickness", "18"),
                Map.entry("surfaceLayerThickness", "1,2"),
                Map.entry("surfaceTileWidth", "60"),
                Map.entry("surfaceTileHeight", "30"),
                Map.entry("surfaceLayoutOffset", "0"),
                Map.entry("surfaceMinimumOffset", "10"),
                Map.entry("surfaceMinimumEdgeWidth", "8"),
                Map.entry("surfaceMinimumStartEndMargin", "8"),
                Map.entry("surfaceJointWidth", "0,2")
        );

        for (Map.Entry<String, String> entry : expectedValues.entrySet()) {
            Assertions.assertEquals("CENTIMETER", aufFxThread(() -> workbench.automationUnit(entry.getKey())), entry.getKey());
            Assertions.assertEquals(entry.getValue(), aufFxThread(() -> workbench.automationFieldValue(entry.getKey())), entry.getKey());
        }
        Assertions.assertEquals("0", aufFxThread(() -> workbench.automationFieldValue("roomObjectAngle")));
    }

    @Test
    void bodenklickSetztNeuenStandortDerInnenansicht() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Test", "Erdgeschoss");
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4_000, 3_000),
                Length.of(260, LengthUnit.CENTIMETER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        project.primaryLevel().addRoom(room);
        ThreeDViewport viewport = aufFxThread(() -> new ThreeDViewport(ignored -> { }, () -> { }));

        aufFxThread(() -> {
            viewport.activateInteriorView(project, project.primaryLevel(), room);
            Assertions.assertTrue(viewport.isInteriorFloorHit(RenderableKind.ROOM_FLOOR, 180.0));
            Assertions.assertTrue(viewport.isInteriorFloorHit(RenderableKind.SURFACE_LAYER, 180.0));
            Assertions.assertFalse(viewport.isInteriorFloorHit(RenderableKind.SURFACE_LAYER, 2_800.0));
            viewport.automationSelectInteriorFloorPoint(1_000.0, 800.0);
            return null;
        });

        PlanPoint eyePosition = aufFxThread(viewport::automationInteriorEyePosition);
        Assertions.assertEquals(1_000.0, eyePosition.xMillimeters(), 0.001);
        Assertions.assertEquals(800.0, eyePosition.yMillimeters(), 0.001);
    }

    @Test
    void bodenklickWechseltDurchTuerInNachbarraum() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Test", "Erdgeschoss");
        Room wohnen = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4_000, 3_000),
                Length.of(260, LengthUnit.CENTIMETER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        Room kueche = Room.rectangular(
                "Küche",
                new PlanPoint(4_000, 0),
                new PlanPoint(7_000, 3_000),
                Length.of(260, LengthUnit.CENTIMETER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        project.primaryLevel().addRoom(wohnen);
        project.primaryLevel().addRoom(kueche);
        ThreeDViewport viewport = aufFxThread(() -> new ThreeDViewport(ignored -> { }, () -> { }));

        aufFxThread(() -> {
            viewport.activateInteriorView(project, project.primaryLevel(), wohnen);
            return null;
        });

        // Klick in die Küche (Nachbarraum) versetzt den Standort dorthin.
        aufFxThread(() -> {
            viewport.automationClickInteriorFloorOfRoom(
                    kueche.id().toString(),
                    5_500.0,
                    1_500.0
            );
            return null;
        });

        PlanPoint eyePosition = aufFxThread(viewport::automationInteriorEyePosition);
        Assertions.assertEquals(5_500.0, eyePosition.xMillimeters(), 0.001);
        Assertions.assertEquals(1_500.0, eyePosition.yMillimeters(), 0.001);
    }

    @Test
    void tuerKlickWechseltInNachbarraum() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Test", "Erdgeschoss");
        var level = project.primaryLevel();
        // Gemeinsame Trennwand bei x=4_000 zwischen Wohnen und Küche.
        Wall trennwand = Wall.create(
                new PlanSegment(new PlanPoint(4_000, 0), new PlanPoint(4_000, 3_000)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(trennwand);
        Door tuer = Door.create(trennwand.id(), Length.of(1, LengthUnit.METER), Length.of(1, LengthUnit.METER), Length.of(2.01, LengthUnit.METER), Length.zero());
        level.addDoor(tuer);
        Room wohnen = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4_000, 3_000),
                Length.of(260, LengthUnit.CENTIMETER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        Room kueche = Room.rectangular(
                "Küche",
                new PlanPoint(4_000, 0),
                new PlanPoint(7_000, 3_000),
                Length.of(260, LengthUnit.CENTIMETER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(wohnen);
        level.addRoom(kueche);
        ThreeDViewport viewport = aufFxThread(() -> new ThreeDViewport(ignored -> { }, () -> { }));

        aufFxThread(() -> {
            viewport.activateInteriorView(project, level, wohnen);
            return null;
        });

        // Klick auf die Tür simuliert den Wechsel in den Nachbarräum.
        aufFxThread(() -> {
            viewport.automationClickDoorToNeighborRoom(tuer.id().toString());
            return null;
        });

        PlanPoint eyePosition = aufFxThread(viewport::automationInteriorEyePosition);
        // Nach dem Wechsel steht die Kamera im Zentrum der Küche (5_500, 1_500).
        Assertions.assertEquals(5_500.0, eyePosition.xMillimeters(), 0.001);
        Assertions.assertEquals(1_500.0, eyePosition.yMillimeters(), 0.001);
    }

    @Test
    void raumkontextÖffnetInnenansichtAmAngeklicktenStandort() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4_000, 3_000),
                Length.of(260, LengthUnit.CENTIMETER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );

        aufFxThread(() -> {
            workbench.automationAddRoom(room);
            workbench.automationSetTool("EDIT");
            workbench.automationSetViewport(1.0, 0.0, 0.0);
            workbench.automationPrepareSelectionContextMenu(100.0, 80.0);
            return null;
        });

        String menuLabel = "Innenansicht ab diesem Standort öffnen";
        Assertions.assertTrue(aufFxThread(workbench::automationSelectionContextMenuItems).contains(menuLabel));
        aufFxThread(() -> {
            workbench.automationInvokeSelectionContextMenuItem(menuLabel);
            return null;
        });

        Assertions.assertEquals("INTERIOR", aufFxThread(() -> workbench.automationSnapshot().workspaceMode()));
        PlanPoint eyePosition = aufFxThread(workbench::automationInteriorEyePosition);
        Assertions.assertEquals(1_000.0, eyePosition.xMillimeters(), 0.001);
        Assertions.assertEquals(800.0, eyePosition.yMillimeters(), 0.001);
    }

    @Test
    void ziehtBalkonAlsRechteckigeFußbodenplatteAuf() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetTool("FLOOR_EXTENSION");
            workbench.automationCanvasDrag(350, 280, 620, 460, javafx.scene.input.MouseButton.PRIMARY, true, false, false);
            return null;
        });

        Assertions.assertEquals(1, aufFxThread(workbench::automationFloorExtensionCount));
        Assertions.assertEquals(180, aufFxThread(() -> workbench.automationFloorExtension(0).slabThickness().toMillimeters()), 0.001);
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
    void beendenAusDateimenueFordertAppExitAn() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.automationDisableApplicationExit();
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        aufFxThread(() -> {
            VBox topArea = (VBox) workbench.getTop();
            MenuBar menuBar = (MenuBar) topArea.getChildren().getFirst();
            MenuItem beenden = menuBar.getMenus().stream()
                    .flatMap(menu -> menu.getItems().stream())
                    .filter(item -> "Beenden".equals(item.getText()))
                    .findFirst()
                    .orElseThrow();
            beenden.fire();
            return null;
        });

        Assertions.assertTrue(aufFxThread(workbench::automationExitRequested));
    }

    @Test
    void hilfemenueEnthaeltSichtbarenInfoDialog() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        Assertions.assertTrue(aufFxThread(() -> {
            VBox topArea = (VBox) workbench.getTop();
            MenuBar menuBar = (MenuBar) topArea.getChildren().getFirst();
            return menuBar.getMenus().stream()
                    .filter(menu -> "Hilfe".equals(menu.getText()))
                    .flatMap(menu -> menu.getItems().stream())
                    .anyMatch(item -> "Über CADas".equals(item.getText()));
        }));
    }

    @Test
    void abbruchVerhindertBeendenBeiUngesichertenÄnderungen() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.automationDisableApplicationExit();
            instanz.automationInvoke("clearProject", null);
            instanz.automationSetUnsavedChangesExitDecision(false);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationInvoke("exit", null);
            return null;
        });

        Assertions.assertTrue(aufFxThread(workbench::automationHasUnsavedChanges));
        Assertions.assertFalse(aufFxThread(workbench::automationExitRequested));
        Assertions.assertFalse(aufFxThread(workbench::confirmApplicationClose));
    }

    @Test
    void bewusstesVerwerfenErlaubtBeendenBeiUngesichertenÄnderungen() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.automationDisableApplicationExit();
            instanz.automationInvoke("clearProject", null);
            instanz.automationSetUnsavedChangesExitDecision(true);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationInvoke("exit", null);
            return null;
        });

        Assertions.assertTrue(aufFxThread(workbench::automationExitRequested));
    }

    @Test
    void gebäudesicherungUndRückgängigSetzenÄnderungsstatusKorrekt() throws Exception {
        Path exportDatei = Files.createTempFile("cadas-sicherungsstatus-", ".cadas");
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.automationInvoke("clearProject", null);
            return instanz;
        });

        Assertions.assertTrue(aufFxThread(workbench::automationHasUnsavedChanges));
        aufFxThread(() -> {
            workbench.automationInvoke("exportProjectDxf", exportDatei);
            workbench.automationInvoke("clearProject", null);
            return null;
        });
        Assertions.assertTrue(aufFxThread(workbench::automationHasUnsavedChanges));

        aufFxThread(() -> {
            workbench.automationInvoke("undo", null);
            return null;
        });

        Assertions.assertFalse(aufFxThread(workbench::automationHasUnsavedChanges));
    }

    @Test
    void menüeinträgeHabenEindeutigeTastaturkürzel() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        aufFxThread(() -> {
            VBox topArea = (VBox) workbench.getTop();
            MenuBar menuBar = (MenuBar) topArea.getChildren().getFirst();
            Map<String, String> labelsByShortcut = new HashMap<>();
            menuBar.getMenus().stream()
                    .flatMap(menu -> menu.getItems().stream())
                    .filter(item -> item.getAccelerator() != null)
                    .forEach(item -> {
                        String shortcut = item.getAccelerator().toString();
                        String existingLabel = labelsByShortcut.putIfAbsent(shortcut, item.getText());
                        Assertions.assertNull(
                                existingLabel,
                                () -> "Tastaturkürzel " + shortcut + " ist doppelt für `" + existingLabel + "` und `" + item.getText() + "` vergeben."
                        );
                    });
            return null;
        });
    }

    @Test
    void hilfemenueEnthaeltAutomatischeLizenzliste() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        Assertions.assertTrue(aufFxThread(() -> {
            VBox topArea = (VBox) workbench.getTop();
            MenuBar menuBar = (MenuBar) topArea.getChildren().getFirst();
            return menuBar.getMenus().stream()
                    .filter(menu -> "Hilfe".equals(menu.getText()))
                    .flatMap(menu -> menu.getItems().stream())
                    .map(MenuItem::getText)
                    .anyMatch("Drittanbieter-Lizenzen"::equals);
        }));
    }

    @Test
    void dateimenueBietetImportFürDreidimensionaleDxfObjekte() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        Assertions.assertTrue(aufFxThread(() -> {
            VBox topArea = (VBox) workbench.getTop();
            MenuBar menuBar = (MenuBar) topArea.getChildren().getFirst();
            return menuBar.getMenus().stream()
                    .filter(menu -> "Datei".equals(menu.getText()))
                    .flatMap(menu -> menu.getItems().stream())
                    .map(MenuItem::getText)
                    .anyMatch("3D-Objekt aus DXF laden"::equals);
        }));
    }

    @Test
    void rueckgaengigShortcutGreiftAuchVomTextfeldAus() throws Exception {
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
            workbench.automationTriggerShortcutOnField("roomName", KeyCode.Z, true, false);
            return null;
        });

        WorkbenchAutomationSnapshot nachShortcutUndo = aufFxThread(workbench::automationSnapshot);
        Assertions.assertEquals(1.85, nachShortcutUndo.zoom(), 0.0001);
        Assertions.assertEquals(320.0, nachShortcutUndo.offsetX(), 0.0001);
        Assertions.assertEquals(-145.0, nachShortcutUndo.offsetY(), 0.0001);
    }

    @Test
    void importfehlerWerdenImFehlerdialogFestgehalten() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.automationSetErrorDialogsEnabled(false);
            instanz.automationClearLastError();
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });
        Path fehlendeDatei = Path.of("gibt-es-nicht.dxf").toAbsolutePath();

        aufFxThread(() -> {
            workbench.automationInvoke("importProjectDxf", fehlendeDatei);
            return null;
        });

        Assertions.assertEquals("Gebäude-Laden fehlgeschlagen", aufFxThread(workbench::automationLastErrorTitle));
        Assertions.assertEquals("Gebäude-Laden fehlgeschlagen", aufFxThread(workbench::automationLastErrorHeader));
        Assertions.assertTrue(aufFxThread(workbench::automationLastErrorContent).contains("gibt-es-nicht.dxf"));
        Assertions.assertTrue(aufFxThread(workbench::automationLastErrorStackTrace).contains("gibt-es-nicht.dxf"));
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

        WorkbenchAutomationSnapshot bearbeiten = aufFxThread(workbench::automationSnapshot);
        Assertions.assertTrue(bearbeiten.statusText().contains("Linksklick wählt aus"));

        aufFxThread(() -> {
            workbench.automationSetTool("WALL");
            return null;
        });

        WorkbenchAutomationSnapshot wand = aufFxThread(workbench::automationSnapshot);
        Assertions.assertTrue(wand.statusText().contains("Linksklick startet"));
    }

    @Test
    void objektKannOhneRaumAußerhalbDesGebäudesPlatziertWerden() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationSetTool("OBJECT");
            instanz.automationCanvasClick(900, 600, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            return instanz;
        });

        Assertions.assertEquals(1, aufFxThread(workbench::automationRoomObjectCount));
        Assertions.assertTrue(aufFxThread(workbench::automationSnapshot).statusText().contains("innen oder außen"));
    }

    @Test
    void objektmaßeUndWinkelSindBeimPlatzierenUndBearbeitenEinstellbar() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationSetTool("OBJECT");
            instanz.automationSetField("roomObjectWidth", "120");
            instanz.automationSetField("roomObjectDepth", "80");
            instanz.automationSetField("roomObjectHeight", "240");
            instanz.automationSetField("roomObjectBaseElevation", "-15");
            instanz.automationSetField("roomObjectAngle", "37,5");
            instanz.automationCanvasClick(900, 600, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            return instanz;
        });

        RoomObject placed = aufFxThread(() -> workbench.automationRoomObject(0));
        Assertions.assertEquals(1200.0, placed.width().toMillimeters(), 0.001);
        Assertions.assertEquals(800.0, placed.depth().toMillimeters(), 0.001);
        Assertions.assertEquals(2400.0, placed.height().toMillimeters(), 0.001);
        Assertions.assertEquals(-150.0, placed.baseElevation().toMillimeters(), 0.001);
        Assertions.assertEquals(37.5, placed.rotationDegrees(), 0.001);

        aufFxThread(() -> {
            workbench.automationSetField("roomObjectWidth", "150");
            workbench.automationSetField("roomObjectAngle", "-15");
            workbench.automationSetField("roomObjectBaseElevation", "25");
            workbench.automationInvoke("applySelectionProperties", null);
            return null;
        });

        RoomObject edited = aufFxThread(() -> workbench.automationRoomObject(0));
        Assertions.assertEquals(1500.0, edited.width().toMillimeters(), 0.001);
        Assertions.assertEquals(345.0, edited.rotationDegrees(), 0.001);
        Assertions.assertEquals(250.0, edited.baseElevation().toMillimeters(), 0.001);
    }

    @Test
    void quaderHatFreiEinstellbareBezeichnungUndSichtbareBeschriftung() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationSelectRoomObjectPreset("custom-cuboid");
            instanz.automationSetField("roomObjectName", "Wärmepumpe");
            instanz.automationSetField("roomObjectWidth", "130");
            instanz.automationSetTool("OBJECT");
            instanz.automationCanvasClick(900, 600, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            return instanz;
        });

        RoomObject placed = aufFxThread(() -> workbench.automationRoomObject(0));
        Assertions.assertEquals(RoomObjectType.CUBOID, placed.type());
        Assertions.assertEquals("Wärmepumpe", placed.name());
        Assertions.assertEquals(1300.0, placed.width().toMillimeters(), 0.001);
        Assertions.assertNotNull(aufFxThread(workbench::automationDrawingSnapshot));

        aufFxThread(() -> {
            workbench.automationSetField("roomObjectName", "Außengerät");
            workbench.automationInvoke("applySelectionProperties", null);
            return null;
        });

        Assertions.assertEquals("Außengerät", aufFxThread(() -> workbench.automationRoomObject(0).name()));
    }

    @Test
    void innenansichtOhneRaumBleibtImAktivenArbeitsbereichUndMeldetDenGrund() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetWorkspace("INTERIOR");
            return null;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        Assertions.assertEquals("TWO_D", snapshot.workspaceMode());
        Assertions.assertTrue(snapshot.statusText().contains("braucht einen Raum"));
    }

    @Test
    void wandselektionBleibtInnerhalbDerBauteilkontur() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSelect("WALL", 0, false);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        WritableImage image = aufFxThread(workbench::automationDrawingSnapshot);
        int middleX = (int) Math.round(snapshot.offsetX() + 2_000.0 * 0.1 * snapshot.zoom());
        int minY = (int) image.getHeight();
        int maxY = -1;
        for (int y = 0; y < (int) image.getHeight(); y++) {
            var color = image.getPixelReader().getColor(middleX, y);
            if (color.getRed() > 0.75 && color.getGreen() > 0.35 && color.getGreen() < 0.65 && color.getBlue() < 0.3) {
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }

        Assertions.assertTrue(maxY >= minY, "Selektionsfarbe wurde nicht gerendert.");
        double erwarteteKonturbreite = 200.0 * 0.1 * snapshot.zoom();
        Assertions.assertTrue(maxY - minY + 1 <= Math.ceil(erwarteteKonturbreite) + 2.0);
    }

    @Test
    void bearbeitenZeigtPickkreiseFuerWandTuerUndFenster() throws Exception {
        Path projektDatei = erzeugeProjektMitPickpunktenAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            instanz.automationSetShowDimensions(false);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        WritableImage image = aufFxThread(workbench::automationDrawingSnapshot);

        assertPickkreis(image, snapshot, new PlanPoint(0, 0));
        assertPickkreis(image, snapshot, new PlanPoint(1_000, 0));
        assertPickkreis(image, snapshot, new PlanPoint(3_200, 0));
    }

    @Test
    void manuelleRaumerkennungTeiltRaumAnTKante() throws Exception {
        Path projektDatei = erzeugeProjektMitTrennwandAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("WALL", 0, false);
            for (int index = 1; index < 5; index++) {
                instanz.automationSelect("WALL", index, true);
            }
            instanz.automationInvoke("recognizeRoomFromSelectedWalls", null);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);

        Assertions.assertEquals(2, snapshot.roomCount());
        Assertions.assertTrue(snapshot.statusText().contains("Raum erkannt"));
    }

    @Test
    void verschiebtZweidimensionaleAnsichtAuchBeimZiehenAufRaum() throws Exception {
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
        WorkbenchAutomationSnapshot before = aufFxThread(workbench::automationSnapshot);
        double roomX = before.offsetX() + 2_000.0 * 0.1 * before.zoom();
        double roomY = before.offsetY() + 1_500.0 * 0.1 * before.zoom();

        aufFxThread(() -> {
            workbench.automationCanvasDrag(roomX, roomY, roomX + 90.0, roomY + 60.0, javafx.scene.input.MouseButton.SECONDARY, false, false, false);
            return null;
        });
        WorkbenchAutomationSnapshot after = aufFxThread(workbench::automationSnapshot);

        Assertions.assertEquals(before.offsetX() + 90.0, after.offsetX(), 0.001);
        Assertions.assertEquals(before.offsetY() + 60.0, after.offsetY(), 0.001);
    }

    @Test
    void tuerKantenHandleVerschiebtGenauDieGezogeneKanteMitRasterSnap() throws Exception {
        Path projektDatei = erzeugeProjektMitPickpunktenAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("DOOR", 0, false);
            return instanz;
        });
        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        PlanPoint startHandle = aufFxThread(() -> workbench.automationEdgeHandleScreenPoints().getFirst());
        double targetX = startHandle.xMillimeters() + 250.0 * 0.1 * snapshot.zoom();

        aufFxThread(() -> {
            Assertions.assertEquals("DOOR_START", workbench.automationEdgeHandleAtScreen(startHandle.xMillimeters(), startHandle.yMillimeters()));
            workbench.automationCanvasPress(startHandle.xMillimeters(), startHandle.yMillimeters(), javafx.scene.input.MouseButton.PRIMARY);
            Assertions.assertEquals("DOOR_START", workbench.automationActiveEdgeHandle());
            workbench.automationCanvasDragTo(targetX, startHandle.yMillimeters(), javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(targetX, startHandle.yMillimeters(), javafx.scene.input.MouseButton.PRIMARY);
            return null;
        });
        Path exportDatei = Files.createTempFile("cadas-handle-", ".dxf");
        aufFxThread(() -> {
            workbench.automationInvoke("exportProjectDxf", exportDatei);
            return null;
        });
        Door door = new DxfProjectExchangeService().importProject(exportDatei, "Handle").primaryLevel().doors().getFirst();

        Assertions.assertEquals(1_250.0, door.offsetFromStart().toMillimeters(), 0.001);
        Assertions.assertEquals(750.0, door.width().toMillimeters(), 0.001);
    }

    @Test
    void mauszeigerZeigtHandleUndSondertastenaktion() throws Exception {
        Path projektDatei = erzeugeProjektMitPickpunktenAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("DOOR", 0, false);
            return instanz;
        });
        PlanPoint handle = aufFxThread(() -> workbench.automationEdgeHandleScreenPoints().getFirst());

        Assertions.assertEquals("H_RESIZE", aufFxThread(() -> workbench.automationCursorAt(handle.xMillimeters(), handle.yMillimeters(), false, false)));
        Assertions.assertEquals("OPEN_HAND", aufFxThread(() -> workbench.automationCursorAt(handle.xMillimeters(), handle.yMillimeters(), false, true)));
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
    void dreiDViewportAnsichtWechseltAusInnenansichtZurOrbitAnsicht() throws Exception {
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

        WorkbenchAutomationSnapshot innen = aufFxThread(workbench::automationSnapshot);
        Assertions.assertTrue(innen.threeDCameraStatus().contains("3D Innenansicht:"));

        WorkbenchAutomationSnapshot orbit = aufFxThread(() -> {
            workbench.automationInvoke("threeDViewportReset", null);
            return workbench.automationSnapshot();
        });

        Assertions.assertTrue(orbit.threeDCameraStatus().contains("3D Ansicht:"));
        Assertions.assertFalse(orbit.threeDCameraStatus().contains("Innenansicht"));
    }

    @Test
    void arbeitsbereicheStellenVorherigeOrbitUndInnenansichtWiederHer() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetWorkspace("THREE_D");
            return instanz;
        });

        WorkbenchAutomationSnapshot orbitVorher = aufFxThread(() -> {
            workbench.automationInvoke("threeDOrbitRight", null);
            workbench.automationInvoke("threeDPanRight", null);
            return workbench.automationSnapshot();
        });

        WorkbenchAutomationSnapshot innenVorher = aufFxThread(() -> {
            workbench.automationSetWorkspace("INTERIOR");
            workbench.automationInvoke("threeDPanUp", null);
            workbench.automationInvoke("threeDZoomIn", null);
            workbench.automationInvoke("threeDOrbitRight", null);
            return workbench.automationSnapshot();
        });

        WorkbenchAutomationSnapshot innenWieder = aufFxThread(() -> {
            workbench.automationSetWorkspace("TWO_D");
            workbench.automationSetWorkspace("INTERIOR");
            return workbench.automationSnapshot();
        });
        Assertions.assertEquals(innenVorher.threeDCameraStatus(), innenWieder.threeDCameraStatus());

        WorkbenchAutomationSnapshot orbitWieder = aufFxThread(() -> {
            workbench.automationSetWorkspace("THREE_D");
            return workbench.automationSnapshot();
        });
        Assertions.assertEquals(orbitVorher.threeDCameraStatus(), orbitWieder.threeDCameraStatus());
    }

    @Test
    void workbenchZeigtNurNochDieIsoBemaessungAlsOption() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        aufFxThread(() -> {
            VBox topArea = (VBox) workbench.getTop();
            MenuBar menuBar = (MenuBar) topArea.getChildren().getFirst();
            ToolBar settingsBar = (ToolBar) topArea.getChildren().get(1);

            Assertions.assertTrue(settingsBar.getItems().stream()
                    .filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast)
                    .map(CheckBox::getText)
                    .anyMatch("ISO-Bemaßung"::equals));
            Assertions.assertFalse(settingsBar.getItems().stream()
                    .filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast)
                    .map(CheckBox::getText)
                    .anyMatch("ISO 7519"::equals));

            Menu optionenMenu = menuBar.getMenus().stream()
                    .filter(menu -> "Optionen".equals(menu.getText()))
                    .findFirst()
                    .orElseThrow();
            Assertions.assertTrue(optionenMenu.getItems().stream()
                    .map(MenuItem::getText)
                    .anyMatch("ISO-Bemaßung anzeigen"::equals));
            Assertions.assertFalse(optionenMenu.getItems().stream()
                    .map(MenuItem::getText)
                    .anyMatch("Bemaßung nach DIN EN ISO 7519 | 2025-01"::equals));
            return null;
        });
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

    @Test
    void materiallisteWirdAlsMarkdownExportiertUndNormalisiertExtension() throws Exception {
        Path projektDatei = erzeugeProjektMitInnenwandfliesenAlsDxf();
        Path exportPfad = Files.createTempDirectory("cadas-materialliste-").resolve("material.md.md");
        aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationInvoke("exportSurfaceMaterialReportMarkdown", exportPfad);
            return null;
        });

        Path normalisierterPfad = exportPfad.getParent().resolve("material.md");
        Assertions.assertTrue(Files.exists(normalisierterPfad));
        String markdown = Files.readString(normalisierterPfad);
        Assertions.assertTrue(markdown.contains("# Materialliste Beläge"));
        Assertions.assertTrue(markdown.contains("Fliese"));
        Assertions.assertTrue(markdown.contains("Komplexität pro Raum"));
        Assertions.assertTrue(markdown.contains("Schnitte"));
        Assertions.assertFalse(Files.exists(exportPfad));
    }

    @Test
    void selektiertMitReinemKlickpunktStattRasterpunkt() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationSetErrorDialogsEnabled(false);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetViewport(1.0, 240.0, 160.0);
            workbench.automationSetTool("WALL");
            workbench.automationSetUnit("grid", "MILLIMETER");
            workbench.automationSetField("grid", "10");
            workbench.automationCanvasDrag(240, 175, 340, 175, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            workbench.automationSetTool("EDIT");
            workbench.automationSetUnit("grid", "MILLIMETER");
            workbench.automationSetField("grid", "500");
            // Klick auf Weltkoordinate (500, 180): ohne Raster-Snap trifft er die Wandachse bei y = 150,
            // mit Raster-Snap würde der Punkt auf y = 0 geschnappt und die Wand verfehlen.
            workbench.automationCanvasClick(290, 178, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            return null;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        Assertions.assertEquals(1, snapshot.selectionCount());
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

    private Path erzeugeProjektMitPickpunktenAlsDxf() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Pickpunkte", "Erdgeschoss");
        var level = project.primaryLevel();
        Wall wall = Wall.create(new PlanSegment(new PlanPoint(0, 0), new PlanPoint(5_000, 0)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER));
        level.addWall(wall);
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(5_000, 0), new PlanPoint(5_000, 3_000)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        level.addDoor(Door.create(wall.id(), Length.of(1, LengthUnit.METER), Length.of(1, LengthUnit.METER), Length.of(2.01, LengthUnit.METER), Length.zero()));
        level.addWindow(WindowElement.create(wall.id(), Length.of(3.2, LengthUnit.METER), Length.of(1.2, LengthUnit.METER), Length.of(90, LengthUnit.CENTIMETER), Length.of(1.2, LengthUnit.METER)));
        Path datei = Files.createTempFile("cadas-pickpunkte-", ".dxf");
        new DxfProjectExchangeService().exportProject(project, datei);
        return datei;
    }

    private Path erzeugeProjektMitTrennwandAlsDxf() throws Exception {
        ProjectModel project = ProjectModel.withDefaultLevel("Raumerkennung", "Erdgeschoss");
        var level = project.primaryLevel();
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(0, 0), new PlanPoint(6_000, 0)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(6_000, 0), new PlanPoint(6_000, 4_000)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(6_000, 4_000), new PlanPoint(0, 4_000)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(0, 4_000), new PlanPoint(0, 0)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        level.addWall(Wall.create(new PlanSegment(new PlanPoint(3_000, 0), new PlanPoint(3_000, 4_000)), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        Path datei = Files.createTempFile("cadas-raumerkennung-", ".dxf");
        new DxfProjectExchangeService().exportProject(project, datei);
        return datei;
    }

    private void assertPickkreis(WritableImage image, WorkbenchAutomationSnapshot snapshot, PlanPoint point) {
        int centerX = (int) Math.round(snapshot.offsetX() + point.xMillimeters() * 0.1 * snapshot.zoom());
        int centerY = (int) Math.round(snapshot.offsetY() + point.yMillimeters() * 0.1 * snapshot.zoom());
        boolean darkOutlineFound = false;
        for (int x = Math.max(0, centerX - 7); x <= Math.min((int) image.getWidth() - 1, centerX + 7); x++) {
            for (int y = Math.max(0, centerY - 7); y <= Math.min((int) image.getHeight() - 1, centerY + 7); y++) {
                var color = image.getPixelReader().getColor(x, y);
                darkOutlineFound |= color.getRed() < 0.2 && color.getGreen() < 0.2 && color.getBlue() < 0.2;
            }
        }
        Assertions.assertTrue(darkOutlineFound, "Kein Pickkreis bei " + point + " gefunden.");
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
