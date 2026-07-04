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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

abstract class CadWorkbenchTestPart2 extends CadWorkbenchTestPart1 {

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
    void berichteMenueEnthaeltNurRasterBauzeichnung() throws Exception {
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
                    && !berichte.contains("Bauzeichnung als PDF exportieren (Rastergrafik)");
        }));
    }

    @Test
    void berichteMenueEnthaeltNurRasterMaterialPdf() throws Exception {
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
            return berichte.contains("Räume und Materialien als PDF exportieren")
                    && !berichte.contains("Räume und Materialien als PDF exportieren (SVG-Heizpläne)")
                    && !berichte.contains("Räume und Materialien als PDF exportieren (Rastergrafik)");
        }));
    }

    @Test
    void rasterExporteHabenShortcutTastaturkuerzel() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        Map<String, KeyCombination> shortcuts = aufFxThread(() -> {
            VBox topArea = (VBox) workbench.getTop();
            MenuBar menuBar = (MenuBar) topArea.getChildren().getFirst();
            Map<String, KeyCombination> result = new HashMap<>();
            menuBar.getMenus().stream()
                    .filter(menu -> "Berichte".equals(menu.getText()))
                    .flatMap(menu -> menu.getItems().stream())
                    .forEach(item -> result.put(item.getText(), item.getAccelerator()));
            return result;
        });

        Assertions.assertEquals(
                new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN),
                shortcuts.get("Bauzeichnung als PDF exportieren")
        );
        Assertions.assertEquals(
                new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN),
                shortcuts.get("Räume und Materialien als PDF exportieren")
        );
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
    void raumtextNutztFlaechenschwerpunktVorPunktmittel() throws Exception {
        Room room = new Room(
                java.util.UUID.randomUUID(),
                "Schrägraum",
                List.of(
                        new PlanPoint(0, 0),
                        new PlanPoint(4_000, 0),
                        new PlanPoint(4_000, 1_000),
                        new PlanPoint(0, 3_000)
                ),
                Length.ofMillimeters(2_600),
                Length.ofMillimeters(180),
                Length.ofMillimeters(200),
                (de.schrell.cadas.domain.model.SlopedCeilingProfile) null,
                null
        );
        CadWorkbench workbench = aufFxThread(CadWorkbench::new);

        PlanPoint labelCenter = aufFxThread(() -> workbench.roomLabelCenter(room));

        Assertions.assertEquals(room.areaCentroid().xMillimeters(), labelCenter.xMillimeters(), 0.001);
        Assertions.assertEquals(room.areaCentroid().yMillimeters(), labelCenter.yMillimeters(), 0.001);
    }

    @Test
    void nordwinkelAktualisiertKompassSofort() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            setBooleanProperty(instanz, "showGrid", false);
            instanz.showAreaVolume.set(false);
            instanz.render();
            return instanz;
        });

        WritableImage startImage = aufFxThread(workbench::automationDrawingSnapshot);
        int startRechts = countCompassPixels(startImage, 5, -12, 25, 12);
        int startOben = countCompassPixels(startImage, -12, -25, 12, -5);
        Assertions.assertTrue(startOben > startRechts, "Nordwinkel 0° richtet den Kompass nicht nach oben aus.");

        aufFxThread(() -> {
            workbench.northAngleField.setText("220");
            return null;
        });
        WritableImage image = aufFxThread(workbench::automationDrawingSnapshot);
        int rechtsUnten = countCompassPixels(image, 4, 4, 22, 22);
        int linksUnten = countCompassPixels(image, -22, 4, -4, 22);

        Assertions.assertTrue(rechtsUnten > linksUnten, "Nordwinkel 220° muss den Nordpfeil nach rechts unten ausrichten.");
        Assertions.assertEquals(220.0, workbench.project.northAngle().degrees(), 0.001);
    }

    @Test
    void kompassPfeilNutztBildschirmwinkelMitOstenBei90Grad() {
        var nord = CadWorkbenchRenderDetails.compassArrow(100, 100, 0);
        var ost = CadWorkbenchRenderDetails.compassArrow(100, 100, 90);
        var sued = CadWorkbenchRenderDetails.compassArrow(100, 100, 180);
        var west = CadWorkbenchRenderDetails.compassArrow(100, 100, 270);

        Assertions.assertEquals(100.0, nord.tip().getX(), 0.001);
        Assertions.assertEquals(86.0, nord.tip().getY(), 0.001);
        Assertions.assertEquals(114.0, ost.tip().getX(), 0.001);
        Assertions.assertEquals(100.0, ost.tip().getY(), 0.001);
        Assertions.assertEquals(100.0, sued.tip().getX(), 0.001);
        Assertions.assertEquals(114.0, sued.tip().getY(), 0.001);
        Assertions.assertEquals(86.0, west.tip().getX(), 0.001);
        Assertions.assertEquals(100.0, west.tip().getY(), 0.001);
        Assertions.assertTrue(ost.leftWing().getX() < ost.tip().getX());
        Assertions.assertTrue(ost.rightWing().getX() < ost.tip().getX());
    }

    @Test
    void nordwinkelIstPeilungDerPlanoberkante() {
        var pfeil = CadWorkbenchRenderDetails.compassArrow(
                100,
                100,
                CadWorkbenchRenderDetails.compassDisplayAngleDegrees(220, ViewOrientation.TOP)
        );
        var beschriftung = CadWorkbenchRenderDetails.compassLabelPosition(
                100,
                100,
                CadWorkbenchRenderDetails.compassDisplayAngleDegrees(220, ViewOrientation.TOP)
        );

        Assertions.assertTrue(pfeil.tip().getX() > 100.0, "Nordpfeil muss nach rechts zeigen.");
        Assertions.assertTrue(pfeil.tip().getY() > 100.0, "Nordpfeil muss nach unten zeigen.");
        Assertions.assertTrue(beschriftung.distance(100, 100) > 18.0, "Nord-N muss außerhalb des Kompasskreises liegen.");
    }

    @Test
    void dateiladenÜbernimmtGespeicherteNordrichtungInsFeld() throws Exception {
        Path projektDatei = Files.createTempFile("cadas-nordrichtung-", ".cadas");
        ProjectModel project = ProjectModel.withDefaultLevel("Nordhaus", "Erdgeschoss");
        project.defineNorthAngle(de.schrell.cadas.domain.geometry.Angle.ofDegrees(135));
        new DxfProjectExchangeService().exportProject(project, projektDatei);

        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.automationSetErrorDialogsEnabled(false);
            instanz.automationInvoke("importProjectDxf", projektDatei);
            return instanz;
        });

        Assertions.assertEquals(135.0, aufFxThread(workbench::currentNorthAngleDegrees), 0.001);
        Assertions.assertEquals(135.0, aufFxThread(() -> workbench.project.northAngle().degrees()), 0.001);
    }

    private int countCompassPixels(WritableImage image, int minDx, int minDy, int maxDx, int maxDy) {
        int centerX = (int) Math.round(image.getWidth() - 78.0);
        int centerY = 34;
        int count = 0;
        for (int x = centerX + minDx; x <= centerX + maxDx; x++) {
            for (int y = centerY + minDy; y <= centerY + maxDy; y++) {
                var color = image.getPixelReader().getColor(x, y);
                if (color.getRed() > 0.20 && color.getRed() < 0.40
                        && color.getGreen() > 0.32 && color.getGreen() < 0.50
                        && color.getBlue() > 0.45 && color.getBlue() < 0.65) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void rasterBauzeichnungLaesstGenugRandFuerIsoBemassung() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            var level = instanz.project.primaryLevel();
            level.addWall(Wall.create(new PlanSegment(new PlanPoint(0, 0), new PlanPoint(15_000, 0)), Length.ofMillimeters(200), Length.ofMillimeters(2_800)));
            level.addWall(Wall.create(new PlanSegment(new PlanPoint(15_000, 0), new PlanPoint(15_000, 3_500)), Length.ofMillimeters(200), Length.ofMillimeters(2_800)));
            level.addWall(Wall.create(new PlanSegment(new PlanPoint(15_000, 3_500), new PlanPoint(0, 3_500)), Length.ofMillimeters(200), Length.ofMillimeters(2_800)));
            level.addWall(Wall.create(new PlanSegment(new PlanPoint(0, 3_500), new PlanPoint(0, 0)), Length.ofMillimeters(200), Length.ofMillimeters(2_800)));
            level.addRoom(Room.rectangular(
                    "Wohnen",
                    new PlanPoint(100, 100),
                    new PlanPoint(14_900, 3_400),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            ));
            setBooleanProperty(instanz, "showGrid", false);
            setBooleanProperty(instanz, "showCompass", false);
            return instanz;
        });

        WritableImage image = aufFxThread(() -> workbench.reportLevelSnapshot("Erdgeschoss"));

        Assertions.assertTrue(countDarkPixels(image, 0, 0, (int) image.getWidth() - 1, (int) image.getHeight() - 1) > 100,
                "Die Raster-Bauzeichnung muss den bemaßten Grundriss enthalten.");
        int imageWidth = (int) image.getWidth();
        int imageHeight = (int) image.getHeight();
        Assertions.assertEquals(0, countNonBackgroundPixels(image, 0, 80, 15, imageHeight - 1),
                "Die Raster-Bauzeichnung schneidet links weiterhin Inhalt an.");
        Assertions.assertEquals(0, countNonBackgroundPixels(image, imageWidth - 16, 0, imageWidth - 1, imageHeight - 1),
                "Die Raster-Bauzeichnung schneidet rechts weiterhin Inhalt an.");
        Assertions.assertEquals(0, countNonBackgroundPixels(image, 0, imageHeight - 16, imageWidth - 1, imageHeight - 1),
                "Die Raster-Bauzeichnung schneidet unten weiterhin Inhalt an.");
    }

    @Test
    void pdfSnapshotsBlendenZeichenrasterUndHilfslinienAus() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            setBooleanProperty(instanz, "showCompass", false);
            instanz.automationPlaceGuide("VERTICAL", 0);
            instanz.automationPlaceGuide("HORIZONTAL", 0);
            return instanz;
        });

        WritableImage normaleAnsicht = aufFxThread(workbench::automationDrawingSnapshot);
        WritableImage pdfSnapshot = aufFxThread(() -> workbench.reportLevelSnapshot("Erdgeschoss"));

        int normaleHilfspixel = countNonBackgroundPixels(normaleAnsicht, 0, 180, (int) normaleAnsicht.getWidth() - 1, (int) normaleAnsicht.getHeight() - 1);
        int pdfHilfspixel = countNonBackgroundPixels(pdfSnapshot, 0, 180, (int) pdfSnapshot.getWidth() - 1, (int) pdfSnapshot.getHeight() - 1);

        Assertions.assertTrue(normaleHilfspixel > 200,
                "Die normale Ansicht muss Zeichen-Raster und Hilfslinien weiter anzeigen, Pixel: " + normaleHilfspixel);
        Assertions.assertEquals(0, pdfHilfspixel,
                "PDF-Snapshots dürfen kein Zeichen-Raster und keine Hilfslinien enthalten.");
        Assertions.assertTrue(aufFxThread(() -> workbench.showGrid.get() && workbench.showGuides.get()),
                "Die Anzeigeoptionen müssen nach dem PDF-Snapshot wiederhergestellt sein.");
    }

    @Test
    void materiallistenRasterUebersichtNutztDoppelteAufloesungUndStelltCanvasWiederHer() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.project.primaryLevel().addRoom(Room.rectangular(
                    "Wohnen",
                    new PlanPoint(100, 100),
                    new PlanPoint(4_100, 3_100),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            ));
            return instanz;
        });

        WritableImage vorher = aufFxThread(workbench::automationDrawingSnapshot);
        WritableImage report = aufFxThread(() -> workbench.reportMaterialOverviewSnapshot("Erdgeschoss"));
        WritableImage nachher = aufFxThread(workbench::automationDrawingSnapshot);

        Assertions.assertEquals(vorher.getWidth() * 2.0, report.getWidth(), 0.1);
        Assertions.assertEquals(vorher.getHeight() * 2.0, report.getHeight(), 0.1);
        Assertions.assertTrue(nachher.getWidth() < report.getWidth());
        Assertions.assertTrue(nachher.getHeight() < report.getHeight());
    }

    @Test
    void materiallistenRasterUebersichtVeraendertEigenschaftenBreiteNicht() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.project.primaryLevel().addRoom(Room.rectangular(
                    "Wohnen",
                    new PlanPoint(100, 100),
                    new PlanPoint(4_100, 3_100),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            ));
            return instanz;
        });
        double[] vorher = aufFxThread(() -> {
            SplitPane splitPane = (SplitPane) workbench.getCenter();
            splitPane.setDividerPositions(0.34);
            workbench.layout();
            return splitPane.getDividerPositions();
        });

        aufFxThread(() -> workbench.reportMaterialOverviewSnapshot("Erdgeschoss"));
        double[] nachher = aufFxThread(() -> ((SplitPane) workbench.getCenter()).getDividerPositions());

        Assertions.assertArrayEquals(vorher, nachher, 0.001);
    }

    @Test
    void eigenschaftenbereichStartetMitSiebzigProzentDerBisherigenBreite() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        double[] dividerPositions = aufFxThread(() -> ((SplitPane) workbench.getCenter()).getDividerPositions());

        Assertions.assertEquals(CadWorkbenchUi.INITIAL_PROPERTY_PANE_DIVIDER_POSITION, dividerPositions[0], 0.001);
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
    void variothermKreiseWerdenAbZoomEinsGezeichnet() throws Exception {
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
            instanz.automationSetViewport(0.95, 20.0, 20.0);
            return instanz;
        });

        WorkbenchAutomationSnapshot kleinerZoom = aufFxThread(workbench::automationSnapshot);
        WritableImage unterGrenze = aufFxThread(workbench::automationDrawingSnapshot);
        int ausgeblendeteKreisPixel = countVariothermCirclePixels(
                unterGrenze,
                kleinerZoom,
                new PlanPoint(200, 200),
                new PlanPoint(3_600, 2_500)
        );

        aufFxThread(() -> {
            workbench.automationSetViewport(1.0, 20.0, 20.0);
            return null;
        });
        WorkbenchAutomationSnapshot grenzZoom = aufFxThread(workbench::automationSnapshot);
        WritableImage abGrenze = aufFxThread(workbench::automationDrawingSnapshot);
        int sichtbareKreisPixel = countVariothermCirclePixels(
                abGrenze,
                grenzZoom,
                new PlanPoint(200, 200),
                new PlanPoint(3_600, 2_500)
        );

        Assertions.assertTrue(ausgeblendeteKreisPixel < 10, "Variotherm-Kreise werden schon unter Zoom 1,0 gezeichnet.");
        Assertions.assertTrue(sichtbareKreisPixel > 40, "Variotherm-Kreise werden ab Zoom 1,0 nicht gezeichnet.");
    }

    @Test
    void selektierterHeizkreisZeigtDarunterliegendeVariothermKreise() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.showAreaVolume.set(false);
            instanz.showVariothermCircles.set(false);
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
                    "Oberbelag",
                    Length.ofMillimeters(12),
                    Length.ofMillimeters(800),
                    Length.ofMillimeters(800),
                    Length.ofMillimeters(50)
            ));
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
            HydronicHeating heating = hydronicHeatingForReportTest(
                    room,
                    HeatingSurfacePosition.FLOOR,
                    "FBH 1",
                    new PlanPoint(600, 600),
                    new PlanPoint(3_400, 2_400)
            );
            instanz.project.primaryLevel().addHydronicHeating(heating);
            instanz.updateSelection(new de.schrell.cadas.application.view.SelectionKey(
                    RenderableKind.HEATING_ZONE,
                    instanz.activeLevel.get().name(),
                    heating.zones().getFirst().id().toString()
            ), false);
            instanz.automationSetViewport(3.0, 20.0, 20.0);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        WritableImage image = aufFxThread(workbench::automationDrawingSnapshot);
        int kreisPixel = countVariothermCirclePixels(
                image,
                snapshot,
                new PlanPoint(200, 200),
                new PlanPoint(3_600, 2_500)
        );

        Assertions.assertTrue(kreisPixel > 120, "Selektierter Heizkreis zeigt die darunterliegenden Variotherm-Kreise nicht.");
    }

    @Test
    void heizkreiseLassenSichGlobalAusblenden() throws Exception {
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
            HeatingZone zone = HeatingZone.create("Heizkreis 1", List.of(
                    new PlanPoint(600, 600),
                    new PlanPoint(3_400, 600),
                    new PlanPoint(3_400, 2_400),
                    new PlanPoint(600, 2_400)
            ), HeatingLayoutPattern.MEANDER);
            HydronicHeating heating = HydronicHeating.create(
                    room.id(),
                    HeatingSurfacePosition.FLOOR,
                    HeatingLayoutPattern.MEANDER,
                    Length.ofMillimeters(250),
                    Length.ofMillimeters(16),
                    Length.ofMillimeters(80_000),
                    Length.ofMillimeters(100),
                    new PlanPoint(900, 700),
                    new PlanPoint(1_150, 700)
            ).withZones(List.of(zone));
            instanz.project.primaryLevel().addHydronicHeating(heating);
            instanz.automationSetViewport(2.0, 20.0, 20.0);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        WritableImage mitHeizkreis = aufFxThread(workbench::automationDrawingSnapshot);
        int sichtbareHeizkreisPixel = countHeatingCircuitPixels(
                mitHeizkreis,
                snapshot,
                new PlanPoint(500, 500),
                new PlanPoint(3_500, 2_500)
        );

        aufFxThread(() -> {
            workbench.automationSetShowHeatingCircuits(false);
            return null;
        });
        WritableImage ohneHeizkreis = aufFxThread(workbench::automationDrawingSnapshot);
        int ausgeblendeteHeizkreisPixel = countHeatingCircuitPixels(
                ohneHeizkreis,
                snapshot,
                new PlanPoint(500, 500),
                new PlanPoint(3_500, 2_500)
        );

        Assertions.assertTrue(sichtbareHeizkreisPixel > 80, "Heizkreise wurden nicht sichtbar gezeichnet.");
        Assertions.assertTrue(ausgeblendeteHeizkreisPixel < sichtbareHeizkreisPixel / 10, "Heizkreise bleiben trotz globalem Abschalten sichtbar.");
    }

    @Test
    void reportGrundrissBlendetHeizkreiseAusUndHeizflaecheZeigtSie() throws Exception {
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
            HeatingZone zone = HeatingZone.create("Heizkreis 1", List.of(
                    new PlanPoint(600, 600),
                    new PlanPoint(3_400, 600),
                    new PlanPoint(3_400, 2_400),
                    new PlanPoint(600, 2_400)
            ), HeatingLayoutPattern.MEANDER);
            HydronicHeating heating = HydronicHeating.create(
                    room.id(),
                    HeatingSurfacePosition.FLOOR,
                    HeatingLayoutPattern.MEANDER,
                    Length.ofMillimeters(250),
                    Length.ofMillimeters(16),
                    Length.ofMillimeters(80_000),
                    Length.ofMillimeters(100),
                    new PlanPoint(900, 700),
                    new PlanPoint(1_150, 700)
            ).withZones(List.of(zone));
            instanz.project.primaryLevel().addHydronicHeating(heating);
            return instanz;
        });

        WritableImage grundriss = aufFxThread(() -> workbench.reportLevelSnapshot("Erdgeschoss"));
        WritableImage heizflaeche = aufFxThread(() -> workbench.reportLevelSnapshot("Erdgeschoss", Set.of(), true));

        int grundrissHeizpixel = countHeatingColorPixels(grundriss);
        int heizflaechenPixel = countHeatingColorPixels(heizflaeche);
        Assertions.assertTrue(heizflaechenPixel > grundrissHeizpixel * 20,
                "Die Heizflächen-Seite enthält keine sichtbaren Heizkreise: " + grundrissHeizpixel + " / " + heizflaechenPixel);
    }

    @Test
    void reportHeizflaecheFiltertHydronischeHeizkreiseNachFlaeche() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.showAreaVolume.set(false);
            Room room = Room.rectangular(
                    "Heizraum",
                    new PlanPoint(100, 100),
                    new PlanPoint(5_900, 3_900),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            instanz.project.primaryLevel().addHydronicHeating(hydronicHeatingForReportTest(
                    room,
                    HeatingSurfacePosition.FLOOR,
                    "FBH 1",
                    new PlanPoint(600, 600),
                    new PlanPoint(2_600, 1_800)
            ));
            instanz.project.primaryLevel().addHydronicHeating(hydronicHeatingForReportTest(
                    room,
                    HeatingSurfacePosition.CEILING,
                    "DH 1",
                    new PlanPoint(3_100, 1_800),
                    new PlanPoint(5_400, 3_300)
            ));
            return instanz;
        });

        WritableImage alleHeizkreise = aufFxThread(() -> workbench.reportLevelSnapshot("Erdgeschoss", Set.of(), true));
        WritableImage fbh = aufFxThread(() -> workbench.reportLevelSnapshot(
                "Erdgeschoss",
                Set.of(),
                true,
                Set.of(),
                Set.of(HeatingSurfacePosition.FLOOR)
        ));
        WritableImage dh = aufFxThread(() -> workbench.reportLevelSnapshot(
                "Erdgeschoss",
                Set.of(),
                true,
                Set.of(),
                Set.of(HeatingSurfacePosition.CEILING)
        ));

        int allePixel = countHeatingColorPixels(alleHeizkreise);
        int fbhPixel = countHeatingColorPixels(fbh);
        int dhPixel = countHeatingColorPixels(dh);
        Assertions.assertTrue(fbhPixel > 200, "FBH-Seite enthält keine sichtbaren Heizkreise.");
        Assertions.assertTrue(dhPixel > 200, "DH-Seite enthält keine sichtbaren Heizkreise.");
        Assertions.assertTrue(allePixel > fbhPixel + 200 && allePixel > dhPixel + 200,
                "Gefilterte Heizflächen-Seiten enthalten weiterhin fremde Heizkreise: " + allePixel + " / " + fbhPixel + " / " + dhPixel);
    }

    @Test
    void rasterHeizflaecheZeigtVariothermKreiseUnterOberbelag() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            instanz.showAreaVolume.set(false);
            instanz.showVariothermCircles.set(false);
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
                    "Oberbelag",
                    Length.ofMillimeters(12),
                    Length.ofMillimeters(800),
                    Length.ofMillimeters(800),
                    Length.ofMillimeters(50)
            ));
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
            instanz.project.primaryLevel().addHydronicHeating(hydronicHeatingForReportTest(
                    room,
                    HeatingSurfacePosition.FLOOR,
                    "FBH 1",
                    new PlanPoint(600, 600),
                    new PlanPoint(3_400, 2_400)
            ));
            return instanz;
        });

        UUID variothermLayerId = aufFxThread(() -> workbench.project.primaryLevel()
                .surfaceLayerStacks()
                .getFirst()
                .layers()
                .get(1)
                .id());
        WritableImage image = aufFxThread(() -> workbench.reportLevelSnapshot(
                "Erdgeschoss",
                Set.of(variothermLayerId),
                true,
                Set.of(),
                Set.of(HeatingSurfacePosition.FLOOR)
        ));
        int kreisPixel = countVariothermCirclePixels(image);

        Assertions.assertTrue(kreisPixel > 120, "Raster-Heizflächenbild zeigt die Variotherm-Kreise unter dem Oberbelag nicht.");
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
}
