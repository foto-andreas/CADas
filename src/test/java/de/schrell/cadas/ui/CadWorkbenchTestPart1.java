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
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import de.schrell.cadas.domain.model.SurfaceLayoutDirection;
import de.schrell.cadas.domain.model.SurfaceLayoutMargins;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceLayoutRotation;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ZoomEvent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CadWorkbenchTestPart1 extends CadWorkbenchTestBase {

    @Test
    void dokumentiertAlleMenüaktionenMitAusführlichenTooltips() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            return instanz;
        });

        aufFxThread(() -> {
            VBox topArea = (VBox) workbench.getTop();
            MenuBar menuBar = (MenuBar) topArea.getChildren().getFirst();
            List<MenuItem> menuItems = menuBar.getMenus().stream()
                    .flatMap(menu -> menu.getItems().stream())
                    .filter(item -> !(item instanceof SeparatorMenuItem))
                    .toList();
            Assertions.assertFalse(menuItems.isEmpty());
            for (MenuItem menuItem : menuItems) {
                Object tooltipText = menuItem.getProperties().get("cadas.tooltip");
                Assertions.assertInstanceOf(String.class, tooltipText, menuItem.getText());
                Assertions.assertTrue(((String) tooltipText).length() >= 40, menuItem.getText());
                Assertions.assertNotNull(menuItem.getGraphic(), menuItem.getText());
            }
            return null;
        });
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
                .contains("Diese Wand als Vorderseite setzen"));
        aufFxThread(() -> {
            workbench.automationInvokeSelectionContextMenuItem("Diese Wand als Vorderseite setzen");
            return null;
        });
        Assertions.assertEquals(0.0, aufFxThread(() -> workbench.project.frontAngle().degrees()), 0.001);
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
        Assertions.assertEquals(600.0, gedreht.tileWidth().toMillimeters(), 0.001);
        Assertions.assertEquals(1_000.0, gedreht.tileHeight().toMillimeters(), 0.001);
        Assertions.assertEquals(1_000.0, gedreht.effectiveTileWidth().toMillimeters(), 0.001);
        Assertions.assertEquals(600.0, gedreht.effectiveTileHeight().toMillimeters(), 0.001);
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
        Assertions.assertEquals(600.0, weitergeschaltet.tileWidth().toMillimeters(), 0.001);
        Assertions.assertEquals(1_000.0, weitergeschaltet.tileHeight().toMillimeters(), 0.001);
        Assertions.assertEquals(600.0, weitergeschaltet.effectiveTileWidth().toMillimeters(), 0.001);
        Assertions.assertEquals(1_000.0, weitergeschaltet.effectiveTileHeight().toMillimeters(), 0.001);
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
    void zweifingergesteZoomtUmGestenposition() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationSetViewport(1.0, 100.0, 80.0);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.drawingCanvas.getOnZoom().handle(new ZoomEvent(
                    ZoomEvent.ZOOM,
                    300.0,
                    250.0,
                    300.0,
                    250.0,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    1.5,
                    1.5,
                    new PickResult(workbench.drawingCanvas, 300.0, 250.0)
            ));
            return null;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        Assertions.assertEquals(1.5, snapshot.zoom(), 0.0001);
        Assertions.assertEquals(0.0, snapshot.offsetX(), 0.0001);
        Assertions.assertEquals(-5.0, snapshot.offsetY(), 0.0001);
    }

    @Test
    void zoomShortcutsGreifenAuchVomTextfeldAus() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationSetViewport(1.0, 100.0, 80.0);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.roomNameField.fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED,
                    "",
                    "+",
                    KeyCode.EQUALS,
                    true,
                    true,
                    false,
                    false
            ));
            return null;
        });
        Assertions.assertEquals(1.1, aufFxThread(workbench::automationSnapshot).zoom(), 0.0001);

        aufFxThread(() -> {
            workbench.roomNameField.fireEvent(new KeyEvent(
                    KeyEvent.KEY_PRESSED,
                    "",
                    "-",
                    KeyCode.MINUS,
                    false,
                    true,
                    false,
                    false
            ));
            return null;
        });
        Assertions.assertEquals(1.0, aufFxThread(workbench::automationSnapshot).zoom(), 0.0001);
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
}
