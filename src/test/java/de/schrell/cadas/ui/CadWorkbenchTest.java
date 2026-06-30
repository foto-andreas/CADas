package de.schrell.cadas.ui;

import de.schrell.cadas.application.layers.SurfaceCoveringPresetService;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingRoutingLanguage;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectHeatingType;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import de.schrell.cadas.domain.model.SurfaceLayoutDirection;
import de.schrell.cadas.domain.model.SurfaceLayoutMargins;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceLayoutRotation;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;
import de.schrell.cadas.infrastructure.dxf.DxfProjectExchangeService;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TextArea;
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
                Map.entry("stairLeftUnderbuild", "0"),
                Map.entry("stairRightUnderbuild", "0"),
                Map.entry("stairUndersideThickness", "0"),
                Map.entry("roomObjectWidth", "90"),
                Map.entry("roomObjectDepth", "90"),
                Map.entry("roomObjectHeight", "200"),
                Map.entry("roomObjectBaseElevation", "0"),
                Map.entry("floorExtensionThickness", "18"),
                Map.entry("surfaceLayerThickness", "1,8"),
                Map.entry("surfaceTileWidth", "60"),
                Map.entry("surfaceTileHeight", "100"),
                Map.entry("surfaceLayoutOffset", "20"),
                Map.entry("surfaceMinimumOffset", "10"),
                Map.entry("surfaceMinimumEdgeWidth", "10"),
                Map.entry("surfaceMinimumStartEndMargin", "10"),
                Map.entry("surfaceJointWidth", "0"),
                Map.entry("heatingPipeSpacing", "10"),
                Map.entry("heatingPipeDiameter", "1,16"),
                Map.entry("heatingMaximumPipeLength", "8000"),
                Map.entry("heatingWallClearance", "10"),
                Map.entry("heatingSupplyX", "0"),
                Map.entry("heatingSupplyY", "0"),
                Map.entry("heatingReturnX", "5"),
                Map.entry("heatingReturnY", "0")
        );

        for (Map.Entry<String, String> entry : expectedValues.entrySet()) {
            Assertions.assertEquals("CENTIMETER", aufFxThread(() -> workbench.automationUnit(entry.getKey())), entry.getKey());
            Assertions.assertEquals(entry.getValue(), aufFxThread(() -> workbench.automationFieldValue(entry.getKey())), entry.getKey());
        }
        Assertions.assertEquals("0", aufFxThread(() -> workbench.automationFieldValue("roomObjectAngle")));
        Assertions.assertEquals("0", aufFxThread(() -> workbench.automationFieldValue("roomObjectHeatOutput")));
    }

    @Test
    void plantUndBearbeitetBodenUndDeckenheizungen() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instance = new CadWorkbench();
            Room room = Room.rectangular(
                    "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 3_000),
                    Length.ofMillimeters(2_600), Length.ofMillimeters(180), Length.ofMillimeters(200)
            );
            instance.automationAddRoom(room);
            instance.automationSelect("ROOM", 0, false);
            instance.automationSetField("heatingMaximumPipeLength", "20000");
            instance.automationPlanHydronicHeating("FLOOR", "MEANDER");
            instance.automationPlanHydronicHeating("CEILING", "SPIRAL");
            return instance;
        });

        Assertions.assertEquals(2, aufFxThread(workbench::automationHydronicHeatingCount));
        HydronicHeating floorHeating = aufFxThread(() -> workbench.automationHydronicHeating(0));
        HydronicHeating ceilingHeating = aufFxThread(() -> workbench.automationHydronicHeating(1));
        Assertions.assertEquals(HeatingSurfacePosition.FLOOR, floorHeating.surfacePosition());
        Assertions.assertEquals(HeatingLayoutPattern.MEANDER, floorHeating.layoutPattern());
        Assertions.assertEquals(HeatingSurfacePosition.CEILING, ceilingHeating.surfacePosition());
        Assertions.assertEquals(HeatingLayoutPattern.SPIRAL, ceilingHeating.layoutPattern());
        Assertions.assertTrue(floorHeating.zones().stream().allMatch(zone -> zone.layoutPattern() == HeatingLayoutPattern.MEANDER));
        Assertions.assertTrue(ceilingHeating.zones().stream().allMatch(zone -> zone.layoutPattern() == HeatingLayoutPattern.SPIRAL));

        aufFxThread(() -> {
            workbench.automationReplaceHeatingZone(
                    0,
                    0,
                    "L-Bereich",
                    java.util.List.of(
                            new PlanPoint(100, 100), new PlanPoint(3_900, 100), new PlanPoint(3_900, 1_500),
                            new PlanPoint(2_000, 1_500), new PlanPoint(2_000, 2_900), new PlanPoint(100, 2_900)
                    ),
                    "SPIRAL",
                    true
            );
            return null;
        });

        HydronicHeating editedHeating = aufFxThread(() -> workbench.automationHydronicHeating(0));
        Assertions.assertEquals("L-Bereich", editedHeating.zones().getFirst().name());
        Assertions.assertEquals(6, editedHeating.zones().getFirst().outline().size());
        Assertions.assertEquals(HeatingLayoutPattern.SPIRAL, editedHeating.zones().getFirst().layoutPattern());
        Assertions.assertTrue(editedHeating.zones().getFirst().flowInverted());

        int zoneCount = editedHeating.zones().size();
        aufFxThread(() -> {
            workbench.automationRemoveHeatingZone(0, 0);
            return null;
        });
        HydronicHeating afterDelete = aufFxThread(() -> workbench.automationHydronicHeating(0));
        Assertions.assertEquals(zoneCount - 1, afterDelete.zones().size());

        aufFxThread(() -> {
            workbench.automationAddDefaultHeatingZone(0);
            return null;
        });
        HydronicHeating afterAdd = aufFxThread(() -> workbench.automationHydronicHeating(0));
        Assertions.assertEquals(zoneCount, afterAdd.zones().size());
        Assertions.assertEquals(4, afterAdd.zones().getLast().outline().size());
    }

    @Test
    void plantHeizkreisOhneSichtbareHkvEinstellungen() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instance = new CadWorkbench();
            new Scene(instance, 1200, 800);
            instance.applyCss();
            instance.layout();
            Room room = Room.rectangular(
                    "Schlafen", new PlanPoint(0, 0), new PlanPoint(4_000, 3_000),
                    Length.ofMillimeters(2_600), Length.ofMillimeters(180), Length.ofMillimeters(200)
            );
            instance.automationAddRoom(room);
            instance.automationSetTool("EDIT");
            instance.automationSetViewport(1.0, 0.0, 0.0);
            instance.automationPrepareSelectionContextMenu(100.0, 80.0);
            return instance;
        });

        Assertions.assertFalse(aufFxThread(workbench::automationSelectionContextMenuItems).contains("HKV hier setzen"));
        aufFxThread(() -> {
            workbench.automationSetField("heatingMaximumPipeLength", "30000");
            workbench.automationPlanHydronicHeating("FLOOR", "MEANDER");
            return null;
        });

        HydronicHeating heating = aufFxThread(() -> workbench.automationHydronicHeating(0));
        Assertions.assertEquals(1_975.0, heating.supplyPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(1_500.0, heating.supplyPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(2_025.0, heating.returnPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(1_500.0, heating.returnPoint().yMillimeters(), 0.001);
    }

    @Test
    void loeschtHkvUeberAuswahl() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instance = new CadWorkbench();
            new Scene(instance, 1200, 800);
            instance.applyCss();
            instance.layout();
            Room room = Room.rectangular(
                    "Schlafen", new PlanPoint(0, 0), new PlanPoint(4_000, 3_000),
                    Length.ofMillimeters(2_600), Length.ofMillimeters(180), Length.ofMillimeters(200)
            );
            instance.automationAddRoom(room);
            instance.automationSetTool("EDIT");
            instance.automationSetViewport(1.0, 0.0, 0.0);
            instance.automationPrepareSelectionContextMenu(100.0, 80.0);
            return instance;
        });

        aufFxThread(() -> {
            workbench.automationSetTool("HEATING_MANIFOLD");
            workbench.automationCanvasPress(100, 80, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(100, 80, javafx.scene.input.MouseButton.PRIMARY);
            return null;
        });

        Assertions.assertEquals(1, aufFxThread(workbench::automationHydronicHeatingCount));
        aufFxThread(() -> {
            workbench.automationSelect("HKV", 0, false);
            workbench.automationDeleteSelection();
            return null;
        });
        Assertions.assertEquals(1, aufFxThread(workbench::automationHydronicHeatingCount));
    }

    @Test
    void kontextmenüBietetNeunzigGradKorrekturFürAuswahlen() throws Exception {
        CadWorkbench workbench = aufFxThread(CadWorkbench::new);

        Assertions.assertTrue(aufFxThread(workbench::automationSelectionContextMenuItems).contains("90°-Korrektur"));
    }

    @Test
    void wandkontextmenüBietetDachschrägenSchnellfunktion() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("WALL", 0, false);
            return instanz;
        });

        Assertions.assertTrue(aufFxThread(workbench::automationSelectionContextMenuItems)
                .contains("Dachschräge aus Wand erzeugen …"));
    }

    @Test
    void verschiebtAuswahlMitCursortasteUmEineRasterweite() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("WALL", 0, false);
            return instanz;
        });
        Wall before = aufFxThread(() -> workbench.automationWall(0));

        Assertions.assertTrue(aufFxThread(() -> workbench.automationMoveSelectionWithArrowKey(KeyCode.RIGHT)));

        Wall after = aufFxThread(() -> workbench.automationWall(0));
        Assertions.assertEquals(before.axis().start().xMillimeters() + 10.0, after.axis().start().xMillimeters(), 0.001);
        Assertions.assertEquals(before.axis().end().xMillimeters() + 10.0, after.axis().end().xMillimeters(), 0.001);
        Assertions.assertTrue(aufFxThread(workbench::automationHasUnsavedChanges));
    }

    @Test
    void rechteckauswahlMarkiertNurVollstaendigImRahmenLiegendeBauteile() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationSetViewport(1.0, 100.0, 100.0);
            instanz.automationSetTool("WALL");
            instanz.automationCanvasDrag(150, 150, 250, 150, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            instanz.automationCanvasDrag(220, 150, 420, 150, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            instanz.automationSetTool("EDIT");
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationCanvasDrag(140, 140, 260, 160, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            return null;
        });

        Assertions.assertEquals(1, aufFxThread(() -> workbench.automationSnapshot().selectionCount()));
        Wall beforeFirstWall = aufFxThread(() -> workbench.automationWall(0));
        Wall beforeSecondWall = aufFxThread(() -> workbench.automationWall(1));
        Assertions.assertTrue(aufFxThread(() -> workbench.automationMoveSelectionWithArrowKey(KeyCode.DOWN)));

        Wall firstWall = aufFxThread(() -> workbench.automationWall(0));
        Wall secondWall = aufFxThread(() -> workbench.automationWall(1));
        Assertions.assertEquals(10.0, firstWall.axis().start().yMillimeters() - beforeFirstWall.axis().start().yMillimeters(), 0.001);
        Assertions.assertEquals(10.0, firstWall.axis().end().yMillimeters() - beforeFirstWall.axis().end().yMillimeters(), 0.001);
        Assertions.assertEquals(0.0, secondWall.axis().start().yMillimeters() - beforeSecondWall.axis().start().yMillimeters(), 0.001);
        Assertions.assertEquals(0.0, secondWall.axis().end().yMillimeters() - beforeSecondWall.axis().end().yMillimeters(), 0.001);
    }

    @Test
    void rechteckauswahlStartetAuchBeimAufziehenAusEinemRaumHeraus() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Keller",
                    new PlanPoint(0, 0),
                    new PlanPoint(4_000, 3_000),
                    Length.ofMillimeters(2_500),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            ));
            instanz.automationSetViewport(1.0, 100.0, 100.0);
            instanz.automationSetTool("OBJECT");
            instanz.automationCanvasClick(250, 220, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            instanz.automationSetTool("EDIT");
            return instanz;
        });

        RoomObject before = aufFxThread(() -> workbench.automationRoomObject(0));
        aufFxThread(() -> {
            workbench.automationCanvasDrag(150, 150, 330, 280, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            return null;
        });

        Assertions.assertEquals(1, aufFxThread(() -> workbench.automationSnapshot().selectionCount()));
        Assertions.assertTrue(aufFxThread(() -> workbench.automationMoveSelectionWithArrowKey(KeyCode.RIGHT)));
        RoomObject after = aufFxThread(() -> workbench.automationRoomObject(0));
        Assertions.assertEquals(before.center().xMillimeters() + 10.0, after.center().xMillimeters(), 0.001);
        Assertions.assertEquals(before.center().yMillimeters(), after.center().yMillimeters(), 0.001);
    }

    @Test
    void deleteTasteLoeschtAusgewaehlteBauteileUeberDenGlobalenShortcutPfad() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationSetViewport(1.0, 100.0, 100.0);
            instanz.automationSetTool("WALL");
            instanz.automationCanvasDrag(150, 150, 250, 150, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("WALL", 0, false);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationTriggerGlobalKey(KeyCode.DELETE);
            return null;
        });

        Assertions.assertEquals(0, aufFxThread(() -> workbench.automationSnapshot().wallCount()));
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
    void raumkontextBenenntNurAngeklicktenRaumOhneMaßänderungUm() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Links", new PlanPoint(0, 0), new PlanPoint(2_000, 2_000),
                    Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
            ));
            instanz.automationAddRoom(Room.rectangular(
                    "Rechts", new PlanPoint(2_500, 0), new PlanPoint(4_500, 3_000),
                    Length.ofMillimeters(2_700), Length.ofMillimeters(220), Length.ofMillimeters(240)
            ));
            instanz.automationSetTool("EDIT");
            instanz.automationSetViewport(1.0, 0.0, 0.0);
            instanz.automationSelect("ROOM", 0, false);
            instanz.automationSelect("ROOM", 1, true);
            return instanz;
        });
        Room rightBefore = aufFxThread(() -> workbench.automationRoom(1));

        aufFxThread(() -> {
            workbench.automationPrepareSelectionContextMenu(300, 100);
            return null;
        });

        Assertions.assertEquals(1, aufFxThread(() -> workbench.automationSnapshot().selectionCount()));
        Assertions.assertTrue(aufFxThread(workbench::automationSelectionContextMenuItems).contains("Raum umbenennen …"));
        aufFxThread(() -> {
            workbench.automationRenameContextRoom("Arbeitszimmer");
            return null;
        });
        Room rightAfter = aufFxThread(() -> workbench.automationRoom(1));
        Assertions.assertEquals("Links", aufFxThread(() -> workbench.automationRoom(0).name()));
        Assertions.assertEquals("Arbeitszimmer", rightAfter.name());
        Assertions.assertEquals(rightBefore.outline(), rightAfter.outline());
        Assertions.assertEquals(rightBefore.roomHeight(), rightAfter.roomHeight());
        Assertions.assertEquals(rightBefore.floorThickness(), rightAfter.floorThickness());
        Assertions.assertEquals(rightBefore.ceilingThickness(), rightAfter.ceilingThickness());
    }

    @Test
    void ausgewaehlterRaumZeigtImMetriktextAuchDenInnenumfang() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.project.primaryLevel().addRoom(Room.rectangular(
                    "Wohnen",
                    new PlanPoint(0, 0),
                    new PlanPoint(4_000, 3_000),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            ));
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("ROOM", 0, false);
            return instanz;
        });

        Assertions.assertEquals("12,00 m² | 31,20 m³ | U 14,00 m", aufFxThread(() -> workbench.automationSnapshot().selectedRoomMetrics()));
    }

    @Test
    void belagkontextRepariertLegacyVariothermVerlegung() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            Room room = Room.rectangular(
                    "Heizraum",
                    new PlanPoint(0, 0),
                    new PlanPoint(4_000, 3_000),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
            stack.addLayer(new SurfaceLayer(
                    UUID.randomUUID(),
                    "Variotherm",
                    Length.of(18, LengthUnit.MILLIMETER),
                    true,
                    Length.of(60, LengthUnit.CENTIMETER),
                    Length.of(100, LengthUnit.CENTIMETER),
                    SurfaceLayoutMode.FIXED,
                    Length.of(20, LengthUnit.CENTIMETER),
                    Length.of(10, LengthUnit.CENTIMETER),
                    Length.of(10, LengthUnit.CENTIMETER),
                    Length.of(10, LengthUnit.CENTIMETER),
                    SurfaceLayoutMargins.zero(),
                    SurfaceLayoutAnchor.AUTO,
                    Length.ofMillimeters(120),
                    Length.ofMillimeters(880),
                    Length.zero(),
                    SurfaceCutRestriction.OUTER_CUTS_ROTATABLE,
                    SurfaceCoveringPresetService.VARIOTHERM_DRY_PANEL_SOURCE,
                    false
            ));
            instanz.project.primaryLevel().addSurfaceLayerStack(stack);
            instanz.automationSetTool("EDIT");
            instanz.automationSetViewport(1.0, 0.0, 0.0);
            instanz.automationSelect("ROOM", 0, false);
            instanz.automationSetSurfaceType("FLOOR");
            instanz.automationSelectSurfaceLayer(0);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationPrepareSelectionContextMenu(100.0, 80.0);
            return null;
        });

        Assertions.assertTrue(aufFxThread(workbench::automationSelectionContextMenuItems).contains("Belag-Verlegung reparieren"));
        aufFxThread(() -> {
            workbench.automationInvokeSelectionContextMenuItem("Belag-Verlegung reparieren");
            return null;
        });

        SurfaceLayer repaired = aufFxThread(() -> workbench.project.primaryLevel()
                .findSurfaceLayerStack(SurfaceType.FLOOR, workbench.automationRoom(0).id().toString())
                .layers()
                .getFirst());
        Assertions.assertEquals(SurfaceCutRestriction.LAY_DIRECTION_OUTER_CUTS, repaired.cutRestriction());
        Assertions.assertEquals(SurfaceLayoutAnchor.MIN_X_MIN_Y, repaired.layoutAnchor());
        Assertions.assertEquals(0.0, repaired.startRowTrim().toMillimeters(), 0.001);
        Assertions.assertEquals(0.0, repaired.startRowWidth().toMillimeters(), 0.001);
    }

    @Test
    void belagStarteckeLaesstSichPerPfeilenUndRichtungKonsistentUmschalten() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Wohnen",
                    new PlanPoint(0, 0),
                    new PlanPoint(4_000, 3_000),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            ));
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("ROOM", 0, false);
            instanz.automationSetSurfaceType("FLOOR");
            return instanz;
        });

        Assertions.assertEquals("Unten links", aufFxThread(workbench::automationSurfaceLayoutCornerLabel));
        Assertions.assertEquals("LEFT_TO_RIGHT", aufFxThread(workbench::automationSurfaceLayoutDirection));
        aufFxThread(() -> {
            workbench.automationInvoke("surfaceLayoutCornerNext", null);
            return null;
        });
        Assertions.assertEquals("Oben links", aufFxThread(workbench::automationSurfaceLayoutCornerLabel));
        Assertions.assertEquals("LEFT_TO_RIGHT", aufFxThread(workbench::automationSurfaceLayoutDirection));
        aufFxThread(() -> {
            workbench.automationInvoke("addSurfaceLayer", null);
            return null;
        });

        SurfaceLayer angelegt = aufFxThread(() -> workbench.project.primaryLevel()
                .findSurfaceLayerStack(SurfaceType.FLOOR, workbench.automationRoom(0).id().toString())
                .layers()
                .getFirst());
        Assertions.assertEquals(SurfaceLayoutAnchor.MIN_X_MAX_Y, angelegt.layoutAnchor());
        Assertions.assertTrue(angelegt.layoutRotatedQuarterTurn());

        aufFxThread(() -> {
            workbench.automationSelectSurfaceLayer(0);
            workbench.automationSetSurfaceLayoutDirection("RIGHT_TO_LEFT");
            workbench.automationInvoke("updateSurfaceLayer", null);
            return null;
        });

        Assertions.assertEquals("Oben links", aufFxThread(workbench::automationSurfaceLayoutCornerLabel));
        Assertions.assertEquals("RIGHT_TO_LEFT", aufFxThread(workbench::automationSurfaceLayoutDirection));
        SurfaceLayer ohneVierteldrehung = aufFxThread(() -> workbench.project.primaryLevel()
                .findSurfaceLayerStack(SurfaceType.FLOOR, workbench.automationRoom(0).id().toString())
                .layers()
                .getFirst());
        Assertions.assertEquals(SurfaceLayoutAnchor.MIN_X_MAX_Y, ohneVierteldrehung.layoutAnchor());
        Assertions.assertFalse(ohneVierteldrehung.layoutRotatedQuarterTurn());

        aufFxThread(() -> {
            workbench.automationInvoke("surfaceLayoutCornerNext", null);
            workbench.automationInvoke("updateSurfaceLayer", null);
            return null;
        });

        Assertions.assertEquals("Oben rechts", aufFxThread(workbench::automationSurfaceLayoutCornerLabel));
        Assertions.assertEquals("RIGHT_TO_LEFT", aufFxThread(workbench::automationSurfaceLayoutDirection));
        SurfaceLayer weitergeschaltet = aufFxThread(() -> workbench.project.primaryLevel()
                .findSurfaceLayerStack(SurfaceType.FLOOR, workbench.automationRoom(0).id().toString())
                .layers()
                .getFirst());
        Assertions.assertEquals(SurfaceLayoutAnchor.MAX_X_MAX_Y, weitergeschaltet.layoutAnchor());
        Assertions.assertTrue(weitergeschaltet.layoutRotatedQuarterTurn());
    }

    @Test
    void belagStarteckeDrehtMarkierteEbeneSofortUmNeunzigGradWeiter() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            Room room = Room.rectangular(
                    "Arbeiten",
                    new PlanPoint(0, 0),
                    new PlanPoint(4_000, 3_000),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
            stack.addLayer(SurfaceLayer.create(
                    "Platte",
                    Length.ofMillimeters(18),
                    Length.ofMillimeters(600),
                    Length.ofMillimeters(1_000),
                    SurfaceLayoutMode.AUTOMATIC,
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    SurfaceCutRestriction.FREE,
                    "Test"
            ));
            instanz.project.primaryLevel().addSurfaceLayerStack(stack);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("ROOM", 0, false);
            instanz.automationSetSurfaceType("FLOOR");
            instanz.automationSelectSurfaceLayer(0);
            return instanz;
        });

        Assertions.assertEquals("Unten links", aufFxThread(workbench::automationSurfaceLayoutCornerLabel));
        aufFxThread(() -> {
            workbench.automationInvoke("surfaceLayoutCornerNext", null);
            return null;
        });

        Assertions.assertEquals("Oben links", aufFxThread(workbench::automationSurfaceLayoutCornerLabel));
        SurfaceLayer gedreht = aufFxThread(() -> workbench.project.primaryLevel()
                .findSurfaceLayerStack(SurfaceType.FLOOR, workbench.automationRoom(0).id().toString())
                .layers()
                .getFirst());
        Assertions.assertEquals(SurfaceLayoutAnchor.MIN_X_MAX_Y, gedreht.layoutAnchor());
        Assertions.assertTrue(gedreht.layoutRotatedQuarterTurn());
    }

    @Test
    void belagStarteckeSchaltetVonObenLinksAufDieRechteWandWeiter() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            Room room = Room.rectangular(
                    "Arbeiten",
                    new PlanPoint(0, 0),
                    new PlanPoint(4_000, 3_000),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("ROOM", 0, false);
            instanz.automationSetSurfaceType("FLOOR");
            instanz.automationInvoke("surfaceLayoutCornerNext", null);
            instanz.automationInvoke("addSurfaceLayer", null);
            instanz.automationSelectSurfaceLayer(0);
            instanz.automationInvoke("surfaceLayoutCornerNext", null);
            return instanz;
        });

        Assertions.assertEquals("Oben rechts", aufFxThread(workbench::automationSurfaceLayoutCornerLabel));
        Assertions.assertEquals("LEFT_TO_RIGHT", aufFxThread(workbench::automationSurfaceLayoutDirection));
        SurfaceLayer weitergeschaltet = aufFxThread(() -> workbench.project.primaryLevel()
                .findSurfaceLayerStack(SurfaceType.FLOOR, workbench.automationRoom(0).id().toString())
                .layers()
                .getFirst());
        Assertions.assertEquals(SurfaceLayoutAnchor.MAX_X_MAX_Y, weitergeschaltet.layoutAnchor());
        Assertions.assertFalse(weitergeschaltet.layoutRotatedQuarterTurn());
    }

    @Test
    void gedrehteBelagsebeneWirdBeimAktualisierenAufSichtbareModulmaßeNormalisiert() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            Room room = Room.rectangular(
                    "Schlafen",
                    new PlanPoint(0, 0),
                    new PlanPoint(4_000, 3_000),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
            stack.addLayer(SurfaceLayer.create(
                    "Platte",
                    Length.ofMillimeters(18),
                    Length.ofMillimeters(600),
                    Length.ofMillimeters(1_000),
                    SurfaceLayoutMode.FIXED,
                    Length.ofMillimeters(200),
                    Length.ofMillimeters(100),
                    Length.ofMillimeters(100),
                    Length.ofMillimeters(100),
                    Length.zero(),
                    SurfaceCutRestriction.FREE,
                    "Test"
            ).withLayoutOrientation(SurfaceLayoutRotation.DEGREES_270, SurfaceLayoutDirection.RIGHT_TO_LEFT));
            instanz.project.primaryLevel().addSurfaceLayerStack(stack);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("ROOM", 0, false);
            instanz.automationSetSurfaceType("FLOOR");
            instanz.automationSelectSurfaceLayer(0);
            return instanz;
        });

        Assertions.assertEquals("Oben rechts", aufFxThread(workbench::automationSurfaceLayoutCornerLabel));
        aufFxThread(() -> {
            workbench.automationInvoke("updateSurfaceLayer", null);
            return null;
        });

        SurfaceLayer aktualisiert = aufFxThread(() -> workbench.project.primaryLevel()
                .findSurfaceLayerStack(SurfaceType.FLOOR, workbench.automationRoom(0).id().toString())
                .layers()
                .getFirst());
        Assertions.assertEquals(600.0, aktualisiert.tileWidth().toMillimeters(), 0.001);
        Assertions.assertEquals(1_000.0, aktualisiert.tileHeight().toMillimeters(), 0.001);
        Assertions.assertEquals(SurfaceLayoutAnchor.MAX_X_MAX_Y, aktualisiert.layoutAnchor());
        Assertions.assertTrue(aktualisiert.layoutRotatedQuarterTurn());
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
    void ziehtRechteckigeUndRundeBodenöffnungenImRaumAuf() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                    Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
            ));
            instanz.automationSetViewport(1.0, 0.0, 0.0);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetTool("FLOOR_OPENING_RECTANGLE");
            workbench.automationCanvasPress(100, 100, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasDragTo(200, 250, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(200, 250, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationSetTool("FLOOR_OPENING_CIRCLE");
            workbench.automationCanvasPress(250, 100, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasDragTo(350, 220, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(350, 220, javafx.scene.input.MouseButton.PRIMARY);
            return null;
        });

        Assertions.assertEquals(2, aufFxThread(workbench::automationFloorOpeningCount),
                aufFxThread(() -> workbench.automationSnapshot().statusText()));
        Assertions.assertEquals(FloorOpeningShape.RECTANGLE, aufFxThread(() -> workbench.automationFloorOpening(0).shape()));
        Assertions.assertEquals(1_000.0, aufFxThread(() -> workbench.automationFloorOpening(0).width().toMillimeters()), 0.001);
        Assertions.assertEquals(1_000.0, aufFxThread(() -> workbench.automationFloorOpening(1).width().toMillimeters()), 0.001);
        Assertions.assertEquals(1_000.0, aufFxThread(() -> workbench.automationFloorOpening(1).depth().toMillimeters()), 0.001);
    }

    @Test
    void ziehtFbhSperrflächeAufUndÄndertSieMitRechteckHandle() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                    Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
            ));
            instanz.automationSetViewport(1.0, 0.0, 0.0);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetTool("HEATING_EXCLUSION_RECTANGLE");
            workbench.automationCanvasPress(100, 100, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasDragTo(200, 250, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(200, 250, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationSetTool("EDIT");
            return null;
        });

        Assertions.assertEquals(1, aufFxThread(workbench::automationHeatingExclusionAreaCount));
        double vorherigeBreite = aufFxThread(() -> workbench.automationHeatingExclusionArea(0).widthMillimeters());
        List<PlanPoint> handles = aufFxThread(workbench::automationEdgeHandleScreenPoints);
        Assertions.assertEquals(8, handles.size());
        PlanPoint eastHandle = handles.get(3);

        aufFxThread(() -> {
            Assertions.assertEquals("RECTANGLE_EAST", workbench.automationEdgeHandleAtScreen(eastHandle.xMillimeters(), eastHandle.yMillimeters()));
            workbench.automationCanvasPress(eastHandle.xMillimeters(), eastHandle.yMillimeters(), javafx.scene.input.MouseButton.PRIMARY);
            Assertions.assertEquals("RECTANGLE_EAST", workbench.automationActiveEdgeHandle());
            workbench.automationCanvasDragTo(eastHandle.xMillimeters() + 1_000.0, eastHandle.yMillimeters(), javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(eastHandle.xMillimeters() + 1_000.0, eastHandle.yMillimeters(), javafx.scene.input.MouseButton.PRIMARY);
            return null;
        });

        Assertions.assertTrue(aufFxThread(() -> workbench.automationHeatingExclusionArea(0).widthMillimeters()) > vorherigeBreite);
    }

    @Test
    void ziehtHeizkreisRechteckMitGerastertemRoutingStartAuf() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                    Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
            ));
            instanz.automationSetViewport(1.0, 0.0, 0.0);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetTool("HEATING_ZONE_RECTANGLE");
            workbench.automationCanvasPress(50.3, 50.4, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasDragTo(180.8, 160.7, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(180.8, 160.7, javafx.scene.input.MouseButton.PRIMARY);
            return null;
        });

        Assertions.assertEquals(1, aufFxThread(workbench::automationHydronicHeatingCount),
                aufFxThread(() -> workbench.automationSnapshot().statusText()));
        HeatingZone zone = aufFxThread(() -> workbench.automationHydronicHeating(0).zones().getFirst());
        Assertions.assertEquals(new PlanPoint(1_160, 1_060), zone.routingStartPoint());
        Assertions.assertEquals(504.2, zone.outline().getFirst().xMillimeters(), 0.001);
    }

    @Test
    void übernimmtRoutingEingabenOhneDebounceBeimSchnellenLöschen() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                    Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
            ));
            instanz.automationSetViewport(1.0, 0.0, 0.0);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetTool("HEATING_ZONE_RECTANGLE");
            workbench.automationCanvasPress(50.3, 50.4, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasDragTo(180.8, 160.7, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(180.8, 160.7, javafx.scene.input.MouseButton.PRIMARY);
            return null;
        });

        aufFxThread(() -> {
            workbench.automationSetHeatingRoutingCommandAreaText("Ii");
            workbench.automationInvoke("applyHeatingRouting", null);
            heatingRoutingCommandArea(workbench).setText("I");
            return null;
        });

        Assertions.assertEquals("=", aufFxThread(() -> workbench.automationHydronicHeating(0).zones().getFirst().routingCommands()));
    }

    @Test
    void interpretiertGespiegelteKurvenAliaseImRoutingTextfeld() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                    Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
            ));
            instanz.automationSetViewport(1.0, 0.0, 0.0);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetTool("HEATING_ZONE_RECTANGLE");
            workbench.automationCanvasPress(50.3, 50.4, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasDragTo(180.8, 160.7, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(180.8, 160.7, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationInvoke("mirrorSelectedHeatingZonesHorizontally", null);
            heatingRoutingCommandArea(workbench).setText("89()");
            return null;
        });

        Assertions.assertEquals("lrLR", aufFxThread(() -> workbench.automationHydronicHeating(0).zones().getFirst().routingCommands()));
        Assertions.assertEquals("lrLR", aufFxThread(workbench::automationHeatingRoutingCommandAreaText));
    }

    @Test
    void behaeltCursorUndScrollpositionBeiMehrzeiligemSprachrouting() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                    Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
            ));
            instanz.automationSetViewport(1.0, 0.0, 0.0);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetTool("HEATING_ZONE_RECTANGLE");
            workbench.automationCanvasPress(50.3, 50.4, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasDragTo(180.8, 160.7, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(180.8, 160.7, javafx.scene.input.MouseButton.PRIMARY);
            return null;
        });

        String routingText = "II\nRR\nii\nrr\nI\ni\n";
        double scrollTop = aufFxThread(() -> {
            workbench.automationSetHeatingRoutingCommandAreaText(routingText);
            workbench.automationSetHeatingRoutingCommandAreaCaretPosition(routingText.length());
            workbench.automationSetHeatingRoutingCommandAreaScrollTop(120.0);
            return workbench.automationHeatingRoutingCommandAreaScrollTop();
        });

        aufFxThread(() -> {
            workbench.automationInvoke("applyHeatingRouting", null);
            return null;
        });

        Assertions.assertEquals(routingText.length(), aufFxThread(workbench::automationHeatingRoutingCommandAreaCaretPosition));
        Assertions.assertEquals(scrollTop, aufFxThread(workbench::automationHeatingRoutingCommandAreaScrollTop), 0.001);
        Assertions.assertEquals(
                HeatingRoutingLanguage.normalizeCommands(routingText),
                HeatingRoutingLanguage.stripWhitespaceAndNormalizeAliases(aufFxThread(workbench::automationHeatingRoutingCommandAreaText))
        );
    }

    @Test
    void setztHkvMitPunktwerkzeugImRaum() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                    Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
            ));
            instanz.automationSetViewport(1.0, 0.0, 0.0);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetTool("HEATING_MANIFOLD");
            workbench.automationCanvasPress(50, 70, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(50, 70, javafx.scene.input.MouseButton.PRIMARY);
            return null;
        });

        Assertions.assertEquals(1, aufFxThread(workbench::automationHydronicHeatingCount));
        HydronicHeating heating = aufFxThread(() -> workbench.automationHydronicHeating(0));
        Assertions.assertEquals(new PlanPoint(500, 700), heating.supplyPoint());
        Assertions.assertEquals(new PlanPoint(550, 700), heating.returnPoint());
        Assertions.assertEquals(1, aufFxThread(() -> workbench.automationSnapshot().selectionCount()));
    }

    @Test
    void setztHkvFreiAusserhalbDesAusgewaehltenRaums() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                    Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
            ));
            instanz.automationSelect("ROOM", 0, false);
            instanz.automationSetTool("HEATING_MANIFOLD");
            instanz.automationSetViewport(1.0, 0.0, 0.0);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationCanvasPress(520, 520, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(520, 520, javafx.scene.input.MouseButton.PRIMARY);
            return null;
        });

        HydronicHeating heating = aufFxThread(() -> workbench.automationHydronicHeating(0));
        Assertions.assertEquals(new PlanPoint(5_200, 5_200), heating.supplyPoint());
        Assertions.assertEquals(new PlanPoint(5_250, 5_200), heating.returnPoint());
    }

    @Test
    void ziehtVertikalesHkvRechteckImRaumAuf() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                    Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
            ));
            instanz.automationSetViewport(1.0, 0.0, 0.0);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationSetTool("HEATING_MANIFOLD");
            workbench.automationCanvasPress(50, 70, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasDragTo(110, 190, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasRelease(110, 190, javafx.scene.input.MouseButton.PRIMARY);
            return null;
        });

        HydronicHeating heating = aufFxThread(() -> workbench.automationHydronicHeating(0));
        Assertions.assertEquals(new PlanPoint(800, 1_275), heating.supplyPoint());
        Assertions.assertEquals(new PlanPoint(800, 1_325), heating.returnPoint());
        Assertions.assertEquals(600.0, heating.manifoldFreeAreaWidth().toMillimeters(), 0.001);
        Assertions.assertEquals(1_200.0, heating.manifoldFreeAreaDepth().toMillimeters(), 0.001);
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
    void dateimenueBietetImportFürDreidimensionaleDxfUndIfcObjekte() throws Exception {
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
                    .anyMatch("3D-Objekt aus DXF/IFC/RFA laden"::equals);
        }));
    }

    @Test
    void werkzeugmenueBietetVarioRouterTestfenster() throws Exception {
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
                    .filter(menu -> "Werkzeuge".equals(menu.getText()))
                    .flatMap(menu -> menu.getItems().stream())
                    .map(MenuItem::getText)
                    .anyMatch("Heizkreis-Router Vario testen"::equals);
        }));
    }

    @Test
    void berichteMenueEnthaeltBauzeichnungsExportStattDateimenue() throws Exception {
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
                    .filter(menu -> "Berichte".equals(menu.getText()))
                    .flatMap(menu -> menu.getItems().stream())
                    .map(MenuItem::getText)
                    .anyMatch("Bauzeichnung als PDF exportieren"::equals);
        }));
        Assertions.assertFalse(aufFxThread(() -> {
            VBox topArea = (VBox) workbench.getTop();
            MenuBar menuBar = (MenuBar) topArea.getChildren().getFirst();
            return menuBar.getMenus().stream()
                    .filter(menu -> "Datei".equals(menu.getText()))
                    .flatMap(menu -> menu.getItems().stream())
                    .map(MenuItem::getText)
                    .anyMatch("Bauzeichnung als PDF exportieren"::equals);
        }));
    }

    @Test
    void berichteMenueEnthaeltBeideBauzeichnungsVarianten() throws Exception {
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
            List<String> berichte = menuBar.getMenus().stream()
                    .filter(menu -> "Berichte".equals(menu.getText()))
                    .flatMap(menu -> menu.getItems().stream())
                    .map(MenuItem::getText)
                    .toList();
            return berichte.contains("Bauzeichnung als PDF exportieren")
                    && berichte.contains("Bauzeichnung als PDF exportieren (Rastergrafik)");
        }));
    }

    @Test
    void berichteMenueEnthaeltBeideMaterialPdfVarianten() throws Exception {
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
            List<String> berichte = menuBar.getMenus().stream()
                    .filter(menu -> "Berichte".equals(menu.getText()))
                    .flatMap(menu -> menu.getItems().stream())
                    .map(MenuItem::getText)
                    .toList();
            return berichte.contains("Räume und Materialien als PDF exportieren (SVG-Heizpläne)")
                    && berichte.contains("Räume und Materialien als PDF exportieren (Rastergrafik)");
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
    void warntBeiRaumneuabgleichÜberEntfernteHeizungUndBeläge() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.automationSetErrorDialogsEnabled(false);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("ROOM", 0, false);
            instanz.automationSetSurfaceType("FLOOR");
            instanz.automationInvoke("addSurfaceLayer", null);
            instanz.automationSetField("heatingMaximumPipeLength", "20000");
            instanz.automationPlanHydronicHeating("FLOOR", "MEANDER");
            instanz.automationClearLastWarning();
            instanz.automationSelect("WALL", 0, false);
            instanz.automationDeleteSelection();
            return instanz;
        });

        Assertions.assertEquals(0, aufFxThread(() -> workbench.automationSnapshot().roomCount()));
        Assertions.assertEquals(0, aufFxThread(workbench::automationHydronicHeatingCount));
        Assertions.assertEquals("Räume und Zuordnungen geändert", aufFxThread(workbench::automationLastWarningTitle));
        Assertions.assertEquals(
                "Durch die Bauteiländerung wurden Räume neu ausgewertet.",
                aufFxThread(workbench::automationLastWarningHeader)
        );
        Assertions.assertTrue(aufFxThread(workbench::automationLastWarningContent).contains("Raum/Räume entfallen"));
        Assertions.assertTrue(aufFxThread(workbench::automationLastWarningContent).contains("Heizkreis(e) entfernt"));
        Assertions.assertTrue(aufFxThread(workbench::automationLastWarningContent).contains("Belagsebene(n) entfernt"));
    }

    @Test
    void zeigtRaumneuabgleichWarnungBeimLöschenNurEinmal() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.automationSetErrorDialogsEnabled(false);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("ROOM", 0, false);
            instanz.automationSetSurfaceType("FLOOR");
            instanz.automationInvoke("addSurfaceLayer", null);
            instanz.automationSetField("heatingMaximumPipeLength", "20000");
            instanz.automationPlanHydronicHeating("FLOOR", "MEANDER");
            instanz.automationClearLastWarning();
            instanz.automationSelect("WALL", 0, false);
            instanz.automationDeleteSelection();
            return instanz;
        });

        Assertions.assertEquals(1, aufFxThread(workbench::automationWarningCount));
    }

    @Test
    void zeigtRaumneuabgleichWarnungBeimVerschiebenErstNachDemLoslassen() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.automationSetErrorDialogsEnabled(false);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            instanz.automationSetViewport(1.0, 100.0, 100.0);
            instanz.automationSelect("WALL", 0, false);
            instanz.automationClearLastWarning();
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationCanvasPress(300, 100, javafx.scene.input.MouseButton.PRIMARY);
            workbench.automationCanvasDragTo(300, 140, javafx.scene.input.MouseButton.PRIMARY);
            return null;
        });

        Assertions.assertEquals("", aufFxThread(workbench::automationLastWarningTitle));

        aufFxThread(() -> {
            workbench.automationCanvasRelease(300, 140, javafx.scene.input.MouseButton.PRIMARY);
            return null;
        });

        Assertions.assertEquals("Räume und Zuordnungen geändert", aufFxThread(workbench::automationLastWarningTitle));
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
    void dachfensterKannAufDachschrägePlatziertWerden() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.withSlopedCeilings(
                    java.util.UUID.randomUUID(), "Studio",
                    java.util.List.of(
                            new PlanPoint(0, 0), new PlanPoint(4_000, 0),
                            new PlanPoint(4_000, 3_000), new PlanPoint(0, 3_000)
                    ),
                    Length.ofMillimeters(2_800), Length.ofMillimeters(180), Length.ofMillimeters(200),
                    java.util.List.of(new de.schrell.cadas.domain.model.SlopedCeilingProfile(
                            de.schrell.cadas.domain.model.SlopedCeilingSide.NORTH,
                            Length.ofMillimeters(1_000), Length.ofMillimeters(1_200)
                    )), null
            ));
            instanz.automationSetTool("ROOF_WINDOW");
            instanz.automationPlaceRoofWindow(2_000, 600);
            return instanz;
        });

        Assertions.assertEquals(1, aufFxThread(workbench::automationRoofWindowCount),
                aufFxThread(workbench::automationSnapshot).statusText());
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
            instanz.automationSetField("roomObjectHeatOutput", "850");
            instanz.automationSetField("roomObjectHeatingType", "DH");
            instanz.automationSetField("roomObjectBaseElevation", "-15");
            instanz.automationSetField("roomObjectAngle", "37,5");
            instanz.automationCanvasClick(900, 600, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            return instanz;
        });

        RoomObject placed = aufFxThread(() -> workbench.automationRoomObject(0));
        Assertions.assertEquals(1200.0, placed.width().toMillimeters(), 0.001);
        Assertions.assertEquals(800.0, placed.depth().toMillimeters(), 0.001);
        Assertions.assertEquals(2400.0, placed.height().toMillimeters(), 0.001);
        Assertions.assertEquals(850.0, placed.heatOutputWatts(), 0.001);
        Assertions.assertEquals(RoomObjectHeatingType.CEILING_HEATING, placed.heatingType());
        Assertions.assertEquals(-150.0, placed.baseElevation().toMillimeters(), 0.001);
        Assertions.assertEquals(37.5, placed.rotationDegrees(), 0.001);

        aufFxThread(() -> {
            workbench.automationSetField("roomObjectWidth", "150");
            workbench.automationSetField("roomObjectHeatOutput", "1200");
            workbench.automationSetField("roomObjectHeatingType", "Flächenheizung");
            workbench.automationSetField("roomObjectAngle", "-15");
            workbench.automationSetField("roomObjectBaseElevation", "25");
            workbench.automationInvoke("applySelectionProperties", null);
            return null;
        });

        RoomObject edited = aufFxThread(() -> workbench.automationRoomObject(0));
        Assertions.assertEquals(1500.0, edited.width().toMillimeters(), 0.001);
        Assertions.assertEquals(1200.0, edited.heatOutputWatts(), 0.001);
        Assertions.assertEquals(RoomObjectHeatingType.SURFACE_HEATING, edited.heatingType());
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
    void rundesObjektZeigtSeineBezeichnungInDerDraufsicht() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationSelectRoomObjectPreset("table-round");
            instanz.automationSetField("roomObjectName", "Esstisch");
            instanz.automationSetTool("OBJECT");
            instanz.automationCanvasClick(600, 450, javafx.scene.input.MouseButton.PRIMARY, false, false, false);
            return instanz;
        });

        RoomObject placed = aufFxThread(() -> workbench.automationRoomObject(0));
        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        WritableImage image = aufFxThread(workbench::automationDrawingSnapshot);
        int centerX = (int) Math.round(snapshot.offsetX() + placed.center().xMillimeters() * 0.1 * snapshot.zoom());
        int centerY = (int) Math.round(snapshot.offsetY() + placed.center().yMillimeters() * 0.1 * snapshot.zoom());

        int darkPixels = countDarkPixels(image, centerX - 45, centerY - 16, centerX + 45, centerY + 16);
        Assertions.assertTrue(
                darkPixels > 3,
                "Die Objektbezeichnung wurde in der 2D-Ansicht nicht sichtbar gezeichnet. Dunkle Pixel: " + darkPixels
        );
    }

    @Test
    void lFoermigerRaumTextBleibtInnerhalbDesRaums() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.project.primaryLevel().addRoom(new Room(
                    java.util.UUID.randomUUID(),
                    "L-Raum",
                    List.of(
                            new PlanPoint(100, 100),
                            new PlanPoint(3_100, 100),
                            new PlanPoint(3_100, 1_100),
                            new PlanPoint(1_100, 1_100),
                            new PlanPoint(1_100, 3_100),
                            new PlanPoint(100, 3_100)
                    ),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200),
                    (de.schrell.cadas.domain.model.SlopedCeilingProfile) null,
                    null
            ));
            instanz.automationSetViewport(1.0, 60.0, 60.0);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        WritableImage image = aufFxThread(workbench::automationDrawingSnapshot);
        int notchMinX = (int) Math.round(snapshot.offsetX() + 1_250.0 * 0.1 * snapshot.zoom());
        int notchMinY = (int) Math.round(snapshot.offsetY() + 1_250.0 * 0.1 * snapshot.zoom());
        int notchMaxX = (int) Math.round(snapshot.offsetX() + 2_950.0 * 0.1 * snapshot.zoom());
        int notchMaxY = (int) Math.round(snapshot.offsetY() + 2_950.0 * 0.1 * snapshot.zoom());
        int topArmMinX = (int) Math.round(snapshot.offsetX() + 250.0 * 0.1 * snapshot.zoom());
        int topArmMinY = (int) Math.round(snapshot.offsetY() + 250.0 * 0.1 * snapshot.zoom());
        int topArmMaxX = (int) Math.round(snapshot.offsetX() + 2_950.0 * 0.1 * snapshot.zoom());
        int topArmMaxY = (int) Math.round(snapshot.offsetY() + 950.0 * 0.1 * snapshot.zoom());
        int leftArmMinX = (int) Math.round(snapshot.offsetX() + 250.0 * 0.1 * snapshot.zoom());
        int leftArmMinY = (int) Math.round(snapshot.offsetY() + 1_250.0 * 0.1 * snapshot.zoom());
        int leftArmMaxX = (int) Math.round(snapshot.offsetX() + 950.0 * 0.1 * snapshot.zoom());
        int leftArmMaxY = (int) Math.round(snapshot.offsetY() + 2_950.0 * 0.1 * snapshot.zoom());

        int notchPixels = countDarkPixels(image, notchMinX, notchMinY, notchMaxX, notchMaxY);
        int insidePixels = countDarkPixels(image, topArmMinX, topArmMinY, topArmMaxX, topArmMaxY)
                + countDarkPixels(image, leftArmMinX, leftArmMinY, leftArmMaxX, leftArmMaxY);

        Assertions.assertTrue(insidePixels > 4, "Die Rauminfo wurde in keinem Innenbereich gezeichnet. Innenpixel: " + insidePixels);
        Assertions.assertTrue(notchPixels < insidePixels, "Die Rauminfo landet noch im ausgesparten Bereich.");
    }

    @Test
    void rasterBauzeichnungLaesstGenugRandFuerIsoBemassung() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationAddRoom(Room.rectangular(
                    "Wohnen",
                    new PlanPoint(0, 0),
                    new PlanPoint(6_000, 4_000),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            ));
            setBooleanProperty(instanz, "showGrid", false);
            setBooleanProperty(instanz, "showCompass", false);
            return instanz;
        });

        WritableImage image = aufFxThread(() -> workbench.reportLevelSnapshot("Erdgeschoss"));

        Assertions.assertEquals(0, countBorderContentPixels(image, 16),
                "Die Raster-Bauzeichnung zeichnet ISO-Bemaßung noch bis an den Bildrand.");
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
    void belagsauswahlBevorzugtBeiNurWandInnenwandVorAussenwand() throws Exception {
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
        Assertions.assertEquals("WALL_INTERIOR", snapshot.surfaceType());
        Assertions.assertEquals("WALL_INTERIOR,WALL_EXTERIOR", snapshot.surfaceTypeOptions());
    }

    @Test
    void wandAlleinErlaubtDaemmplatteAlsInnenwandbelag() throws Exception {
        Path projektDatei = erzeugeEinfachesProjektAlsDxf();
        Path exportDatei = Files.createTempFile("cadas-innenwand-daemmplatte-", ".dxf");
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationSetTool("EDIT");
            instanz.automationSelect("WALL", 0, false);
            instanz.automationSetSurfaceType("WALL_INTERIOR");
            instanz.automationSetField("surfaceLayerName", "Dämmplatte");
            instanz.automationSetField("surfaceLayerThickness", "4");
            instanz.automationInvoke("addSurfaceLayer", null);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationInvoke("exportProjectDxf", exportDatei);
            return null;
        });

        SurfaceLayerStack stack = new DxfProjectExchangeService()
                .importProject(exportDatei, "Innenwanddämmung")
                .primaryLevel()
                .surfaceLayerStacks()
                .stream()
                .filter(candidate -> candidate.surfaceType() == SurfaceType.WALL_INTERIOR)
                .findFirst()
                .orElseThrow();

        Assertions.assertTrue(stack.targetKey().contains("@"));
        Assertions.assertEquals("Dämmplatte", stack.layers().getFirst().name());
        Assertions.assertEquals(40.0, stack.layers().getFirst().thickness().toMillimeters(), 0.001);
    }

    @Test
    void ausgeblendeteObersteBodenebeneZeichnetWeiterDieGewählteSichtbareEbene() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.showAreaVolume.set(false);
            Room room = Room.rectangular(
                    "Wohnen",
                    new PlanPoint(100, 100),
                    new PlanPoint(3900, 2900),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
            stack.addLayer(SurfaceLayer.create(
                    "Decklage",
                    Length.of(1.8, LengthUnit.CENTIMETER),
                    Length.ofMillimeters(1_500),
                    Length.ofMillimeters(1_000),
                    SurfaceLayoutMode.NONE,
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    Length.ofMillimeters(120),
                    ""
            ).withVisibility(false));
            stack.addLayer(SurfaceLayer.create(
                    "Traglage",
                    Length.of(1.8, LengthUnit.CENTIMETER),
                    Length.ofMillimeters(1_000),
                    Length.ofMillimeters(1_000),
                    SurfaceLayoutMode.NONE,
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    Length.ofMillimeters(200),
                    ""
            ));
            instanz.project.primaryLevel().addSurfaceLayerStack(stack);
            instanz.automationSetViewport(1.0, 150.0, 120.0);
            instanz.automationSelect("ROOM", 0, false);
            instanz.automationSelectSurfaceLayer(1);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        WritableImage image = aufFxThread(workbench::automationDrawingSnapshot);

        assertHervorgehobenerBelagImRaum(
                image,
                snapshot,
                new PlanPoint(150, 150),
                new PlanPoint(3_850, 2_850)
        );
    }

    @Test
    void gewaehlteAusgeblendeteBodenebeneBleibtTrotzSichtbarerDecklageHervorgehoben() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            Room room = Room.rectangular(
                    "Wohnen",
                    new PlanPoint(100, 100),
                    new PlanPoint(3900, 2900),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
            stack.addLayer(SurfaceLayer.create(
                    "Traglage",
                    Length.of(1.8, LengthUnit.CENTIMETER),
                    Length.ofMillimeters(1_000),
                    Length.ofMillimeters(1_000),
                    SurfaceLayoutMode.NONE,
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    Length.ofMillimeters(200),
                    ""
            ).withVisibility(false));
            stack.addLayer(SurfaceLayer.create(
                    "Decklage",
                    Length.of(1.8, LengthUnit.CENTIMETER),
                    Length.ofMillimeters(1_500),
                    Length.ofMillimeters(1_000),
                    SurfaceLayoutMode.NONE,
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    Length.ofMillimeters(120),
                    ""
            ));
            instanz.project.primaryLevel().addSurfaceLayerStack(stack);
            instanz.automationSetViewport(1.0, 150.0, 120.0);
            instanz.automationSelect("ROOM", 0, false);
            instanz.automationSelectSurfaceLayer(0);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        WritableImage image = aufFxThread(workbench::automationDrawingSnapshot);

        assertHervorgehobenerBelagImRaum(
                image,
                snapshot,
                new PlanPoint(150, 150),
                new PlanPoint(3_850, 2_850)
        );
    }

    @Test
    void bodenebeneZeigtVerlegerichtungImErstenElement() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            Room room = Room.rectangular(
                    "Wohnen",
                    new PlanPoint(100, 100),
                    new PlanPoint(3900, 2900),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
            stack.addLayer(SurfaceLayer.create(
                    "Belag",
                    Length.of(1.8, LengthUnit.CENTIMETER),
                    Length.ofMillimeters(1_000),
                    Length.ofMillimeters(1_000),
                    SurfaceLayoutMode.NONE,
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    Length.zero(),
                    Length.ofMillimeters(120),
                    ""
            ).withLayoutOrientation(SurfaceLayoutRotation.DEGREES_0, SurfaceLayoutDirection.RIGHT_TO_LEFT));
            instanz.project.primaryLevel().addSurfaceLayerStack(stack);
            instanz.automationSetViewport(2.5, 20.0, 20.0);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        WritableImage image = aufFxThread(workbench::automationDrawingSnapshot);

        int hellePfeilPixelRechts = countLightPixels(
                image,
                snapshot,
                new PlanPoint(3_180, 460),
                new PlanPoint(3_820, 740)
        );
        int hellePfeilPixelLinks = countLightPixels(
                image,
                snapshot,
                new PlanPoint(180, 460),
                new PlanPoint(820, 740)
        );

        Assertions.assertTrue(hellePfeilPixelRechts > 40, "Kein Richtungs-Pfeil im ersten Belagselement gefunden.");
        Assertions.assertTrue(hellePfeilPixelLinks > 0, "Die Belagdarstellung wurde im Gegenbereich nicht gezeichnet.");
    }

    @Test
    void variothermZeigtVerlegerichtungImErstenElement() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.showAreaVolume.set(false);
            Room room = Room.rectangular(
                    "Heizraum",
                    new PlanPoint(100, 100),
                    new PlanPoint(950, 1_100),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
            stack.addLayer(SurfaceLayer.create(
                    "Variotherm",
                    Length.of(18, LengthUnit.MILLIMETER),
                    Length.of(60, LengthUnit.CENTIMETER),
                    Length.of(100, LengthUnit.CENTIMETER),
                    SurfaceLayoutMode.FIXED,
                    Length.zero(),
                    Length.zero(),
                    Length.of(10, LengthUnit.CENTIMETER),
                    Length.of(10, LengthUnit.CENTIMETER),
                    Length.zero(),
                    SurfaceCutRestriction.LAY_DIRECTION_OUTER_CUTS,
                    SurfaceCoveringPresetService.VARIOTHERM_DRY_PANEL_SOURCE
            ).withLayoutOrientation(SurfaceLayoutRotation.DEGREES_0, SurfaceLayoutDirection.RIGHT_TO_LEFT));
            instanz.project.primaryLevel().addSurfaceLayerStack(stack);
            instanz.automationSetViewport(3.0, 30.0, 30.0);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        WritableImage image = aufFxThread(workbench::automationDrawingSnapshot);

        int hellePfeilPixelRechts = countLightPixels(
                image,
                snapshot,
                new PlanPoint(420, 520),
                new PlanPoint(820, 680)
        );
        int hellePfeilPixelLinks = countLightPixels(
                image,
                snapshot,
                new PlanPoint(120, 520),
                new PlanPoint(300, 680)
        );

        Assertions.assertTrue(hellePfeilPixelRechts > 40, "Die Verlegerichtung wird auf der Variotherm-Platte nicht sichtbar dargestellt.");
        Assertions.assertTrue(hellePfeilPixelLinks * 2 < hellePfeilPixelRechts, "Die Variotherm-Verlegerichtung liegt nicht auf dem ersten Element.");
    }

    @Test
    void variothermKreiseLassenSichGlobalAusblenden() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.showAreaVolume.set(false);
            Room room = Room.rectangular(
                    "Heizraum",
                    new PlanPoint(100, 100),
                    new PlanPoint(3_900, 2_900),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
            stack.addLayer(SurfaceLayer.create(
                    "Variotherm",
                    Length.of(18, LengthUnit.MILLIMETER),
                    Length.of(60, LengthUnit.CENTIMETER),
                    Length.of(100, LengthUnit.CENTIMETER),
                    SurfaceLayoutMode.FIXED,
                    Length.zero(),
                    Length.zero(),
                    Length.of(10, LengthUnit.CENTIMETER),
                    Length.of(10, LengthUnit.CENTIMETER),
                    Length.zero(),
                    SurfaceCoveringPresetService.VARIOTHERM_DRY_PANEL_SOURCE
            ));
            instanz.project.primaryLevel().addSurfaceLayerStack(stack);
            instanz.automationSetViewport(3.0, 20.0, 20.0);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        WritableImage mitKreisen = aufFxThread(workbench::automationDrawingSnapshot);
        int sichtbareKreisPixel = countVariothermCirclePixels(
                mitKreisen,
                snapshot,
                new PlanPoint(200, 200),
                new PlanPoint(3_600, 2_500)
        );

        aufFxThread(() -> {
            workbench.automationSetShowVariothermCircles(false);
            return null;
        });
        WritableImage ohneKreise = aufFxThread(workbench::automationDrawingSnapshot);
        int ausgeblendeteKreisPixel = countVariothermCirclePixels(
                ohneKreise,
                snapshot,
                new PlanPoint(200, 200),
                new PlanPoint(3_600, 2_500)
        );

        Assertions.assertTrue(sichtbareKreisPixel > 120, "Variotherm-Kreise wurden nicht sichtbar gezeichnet.");
        Assertions.assertTrue(ausgeblendeteKreisPixel < sichtbareKreisPixel / 5, "Variotherm-Kreise bleiben trotz globalem Abschalten sichtbar.");
    }

    @Test
    void variothermAussenschnittZeigtVolleKreiseAufDerInnenseite() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.showAreaVolume.set(false);
            Room room = Room.rectangular(
                    "Heizraum",
                    new PlanPoint(100, 100),
                    new PlanPoint(950, 1_100),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
            stack.addLayer(SurfaceLayer.create(
                    "Variotherm",
                    Length.of(18, LengthUnit.MILLIMETER),
                    Length.of(60, LengthUnit.CENTIMETER),
                    Length.of(100, LengthUnit.CENTIMETER),
                    SurfaceLayoutMode.FIXED,
                    Length.zero(),
                    Length.zero(),
                    Length.of(10, LengthUnit.CENTIMETER),
                    Length.of(10, LengthUnit.CENTIMETER),
                    Length.zero(),
                    SurfaceCutRestriction.LAY_DIRECTION_OUTER_CUTS,
                    SurfaceCoveringPresetService.VARIOTHERM_DRY_PANEL_SOURCE
            ).withLayoutOrientation(SurfaceLayoutRotation.DEGREES_0, SurfaceLayoutDirection.RIGHT_TO_LEFT));
            instanz.project.primaryLevel().addSurfaceLayerStack(stack);
            instanz.automationSetViewport(3.0, 30.0, 30.0);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        WritableImage image = aufFxThread(workbench::automationDrawingSnapshot);
        int aussenkante = countVariothermCirclePixels(
                image,
                snapshot,
                new PlanPoint(110, 180),
                new PlanPoint(125, 1_020)
        );
        int innenbereich = countVariothermCirclePixels(
                image,
                snapshot,
                new PlanPoint(210, 180),
                new PlanPoint(320, 1_020)
        );

        Assertions.assertTrue(innenbereich > 80, "Im inneren Bereich des Außenschnitts fehlen die vollen Variotherm-Kreise.");
        Assertions.assertTrue(aussenkante * 3 < innenbereich, "An der äußeren Schnittkante werden weiterhin zu viele volle Variotherm-Kreise gezeichnet.");
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
            Assertions.assertTrue(settingsBar.getItems().stream()
                    .filter(javafx.scene.control.Button.class::isInstance)
                    .map(javafx.scene.control.Button.class::cast)
                    .map(javafx.scene.control.Button::getText)
                    .anyMatch("Gelände"::equals));

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
    void rasterUndGeländeSindInDerAnsichtSchaltbar() throws Exception {
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
                    .anyMatch("Raster"::equals));
            Assertions.assertTrue(settingsBar.getItems().stream()
                    .filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast)
                    .map(CheckBox::getText)
                    .anyMatch("Raster-Snap"::equals));
            Assertions.assertTrue(settingsBar.getItems().stream()
                    .filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast)
                    .map(CheckBox::getText)
                    .anyMatch("Gelände 2D"::equals));
            Menu optionenMenu = menuBar.getMenus().stream()
                    .filter(menu -> "Optionen".equals(menu.getText()))
                    .findFirst()
                    .orElseThrow();
            Assertions.assertTrue(optionenMenu.getItems().stream()
                    .map(MenuItem::getText)
                    .anyMatch("Raster anzeigen"::equals));
            Assertions.assertTrue(optionenMenu.getItems().stream()
                    .map(MenuItem::getText)
                    .anyMatch("Gelände in 2D anzeigen"::equals));
            return null;
        });
    }

    @Test
    void geländeBearbeitenFälltNachDateiladenAufGespeichertesGeländeZurück() throws Exception {
        Path projektDatei = Files.createTempFile("cadas-gelaende-fallback", ".cadas");
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        project.primaryLevel().addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(3_000, 1_500)),
                Length.ofMillimeters(180),
                Length.ofMillimeters(2_600)
        ));
        project.defineTerrain(new Terrain(List.of(
                new TerrainVertex(new PlanPoint(0, 0), Length.ofMillimeters(0)),
                new TerrainVertex(new PlanPoint(4_000, 0), Length.ofMillimeters(200)),
                new TerrainVertex(new PlanPoint(4_000, 3_000), Length.ofMillimeters(400)),
                new TerrainVertex(new PlanPoint(0, 3_000), Length.ofMillimeters(100))
        ), Length.ofMillimeters(1_800)));
        new DxfProjectExchangeService().exportProject(project, projektDatei);

        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationSetErrorDialogsEnabled(false);
            instanz.automationInvoke("importProjectDxf", projektDatei);
            instanz.automationInvoke("editTerrainElevations", null);
            return instanz;
        });

        Assertions.assertFalse(aufFxThread(() -> workbench.automationSnapshot().statusText())
                .contains("Geländehöhen benötigen mindestens drei äußere Gebäudeecken."));
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
            Assertions.assertEquals("10", workbench.automationFieldValue("surfaceMinimumEdgeWidth"));

            workbench.automationSetUnit("surfaceMinimumEdgeWidth", "MILLIMETER");
            Assertions.assertEquals("MILLIMETER", workbench.automationUnit("surfaceMinimumEdgeWidth"));
            Assertions.assertEquals("100", workbench.automationFieldValue("surfaceMinimumEdgeWidth"));

            workbench.automationSetUnit("surfaceMinimumEdgeWidth", "METER");
            Assertions.assertEquals("METER", workbench.automationUnit("surfaceMinimumEdgeWidth"));
            Assertions.assertEquals("0,1", workbench.automationFieldValue("surfaceMinimumEdgeWidth"));

            workbench.automationSetUnit("surfaceMinimumEdgeWidth", "CENTIMETER");
            Assertions.assertEquals("CENTIMETER", workbench.automationUnit("surfaceMinimumEdgeWidth"));
            Assertions.assertEquals("10", workbench.automationFieldValue("surfaceMinimumEdgeWidth"));

            workbench.automationSetUnit("surfaceJointWidth", "CENTIMETER");
            Assertions.assertEquals("CENTIMETER", workbench.automationUnit("surfaceJointWidth"));
            Assertions.assertEquals("0", workbench.automationFieldValue("surfaceJointWidth"));
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
        Assertions.assertTrue(markdown.contains("Komplexität pro Raum und Fläche"));
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

    private void assertHervorgehobenerBelagImRaum(
            WritableImage image,
            WorkbenchAutomationSnapshot snapshot,
            PlanPoint minPoint,
            PlanPoint maxPoint
    ) {
        int minX = (int) Math.round(snapshot.offsetX() + minPoint.xMillimeters() * 0.1 * snapshot.zoom());
        int maxX = (int) Math.round(snapshot.offsetX() + maxPoint.xMillimeters() * 0.1 * snapshot.zoom());
        int minY = (int) Math.round(snapshot.offsetY() + minPoint.yMillimeters() * 0.1 * snapshot.zoom());
        int maxY = (int) Math.round(snapshot.offsetY() + maxPoint.yMillimeters() * 0.1 * snapshot.zoom());
        boolean highlightedJointFound = false;
        for (int x = Math.max(0, minX); x <= Math.min((int) image.getWidth() - 1, maxX); x++) {
            for (int y = Math.max(0, minY); y <= Math.min((int) image.getHeight() - 1, maxY); y++) {
                var color = image.getPixelReader().getColor(x, y);
                highlightedJointFound |= color.getRed() > 0.65
                        && color.getGreen() > 0.15
                        && color.getGreen() < 0.45
                        && color.getBlue() < 0.18;
            }
        }
        Assertions.assertTrue(highlightedJointFound, "Kein hervorgehobener Belag im Raumbereich gefunden.");
    }

    private int countVariothermCirclePixels(
            WritableImage image,
            WorkbenchAutomationSnapshot snapshot,
            PlanPoint minPoint,
            PlanPoint maxPoint
    ) {
        int minX = (int) Math.round(snapshot.offsetX() + minPoint.xMillimeters() * 0.1 * snapshot.zoom());
        int maxX = (int) Math.round(snapshot.offsetX() + maxPoint.xMillimeters() * 0.1 * snapshot.zoom());
        int minY = (int) Math.round(snapshot.offsetY() + minPoint.yMillimeters() * 0.1 * snapshot.zoom());
        int maxY = (int) Math.round(snapshot.offsetY() + maxPoint.yMillimeters() * 0.1 * snapshot.zoom());
        int count = 0;
        for (int x = Math.max(0, minX); x <= Math.min((int) image.getWidth() - 1, maxX); x++) {
            for (int y = Math.max(0, minY); y <= Math.min((int) image.getHeight() - 1, maxY); y++) {
                var color = image.getPixelReader().getColor(x, y);
                if (color.getBlue() > color.getRed() + 0.04
                        && color.getGreen() > color.getRed() + 0.02
                        && color.getBlue() > 0.32) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countDarkPixels(WritableImage image, int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int x = Math.max(0, minX); x <= Math.min((int) image.getWidth() - 1, maxX); x++) {
            for (int y = Math.max(0, minY); y <= Math.min((int) image.getHeight() - 1, maxY); y++) {
                var color = image.getPixelReader().getColor(x, y);
                if (color.getRed() < 0.55 && color.getGreen() < 0.50 && color.getBlue() < 0.40) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countDarkPixels(
            WritableImage image,
            WorkbenchAutomationSnapshot snapshot,
            PlanPoint minPoint,
            PlanPoint maxPoint
    ) {
        int minX = (int) Math.round(snapshot.offsetX() + minPoint.xMillimeters() * 0.1 * snapshot.zoom());
        int maxX = (int) Math.round(snapshot.offsetX() + maxPoint.xMillimeters() * 0.1 * snapshot.zoom());
        int minY = (int) Math.round(snapshot.offsetY() + minPoint.yMillimeters() * 0.1 * snapshot.zoom());
        int maxY = (int) Math.round(snapshot.offsetY() + maxPoint.yMillimeters() * 0.1 * snapshot.zoom());
        return countDarkPixels(image, minX, minY, maxX, maxY);
    }

    private int countLightPixels(
            WritableImage image,
            WorkbenchAutomationSnapshot snapshot,
            PlanPoint minPoint,
            PlanPoint maxPoint
    ) {
        int minX = (int) Math.round(snapshot.offsetX() + minPoint.xMillimeters() * 0.1 * snapshot.zoom());
        int maxX = (int) Math.round(snapshot.offsetX() + maxPoint.xMillimeters() * 0.1 * snapshot.zoom());
        int minY = (int) Math.round(snapshot.offsetY() + minPoint.yMillimeters() * 0.1 * snapshot.zoom());
        int maxY = (int) Math.round(snapshot.offsetY() + maxPoint.yMillimeters() * 0.1 * snapshot.zoom());
        int count = 0;
        for (int x = Math.max(0, minX); x <= Math.min((int) image.getWidth() - 1, maxX); x++) {
            for (int y = Math.max(0, minY); y <= Math.min((int) image.getHeight() - 1, maxY); y++) {
                var color = image.getPixelReader().getColor(x, y);
                if (color.getRed() > 0.88 && color.getGreen() > 0.86 && color.getBlue() > 0.80) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countBorderContentPixels(WritableImage image, int frameWidth) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int count = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean inBorder = x < frameWidth || y < frameWidth || x >= width - frameWidth || y >= height - frameWidth;
                if (!inBorder) {
                    continue;
                }
                var color = image.getPixelReader().getColor(x, y);
                if (Math.abs(color.getRed() - 0.988) > 0.03
                        || Math.abs(color.getGreen() - 0.980) > 0.03
                        || Math.abs(color.getBlue() - 0.961) > 0.03) {
                    count++;
                }
            }
        }
        return count;
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

    private static TextArea heatingRoutingCommandArea(CadWorkbench workbench) {
        try {
            Field field = CadWorkbench.class.getDeclaredField("heatingRoutingCommandArea");
            field.setAccessible(true);
            return (TextArea) field.get(workbench);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Heizkreis-Textfeld konnte nicht gefunden werden.", exception);
        }
    }

    private static void setBooleanProperty(CadWorkbench workbench, String fieldName, boolean value) {
        try {
            Field field = CadWorkbench.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            ((javafx.beans.property.BooleanProperty) field.get(workbench)).set(value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Boolesche Eigenschaft `" + fieldName + "` konnte nicht gefunden werden.", exception);
        }
    }

    @FunctionalInterface
    private interface FxCallable<T> {
        T call() throws Exception;
    }
}
