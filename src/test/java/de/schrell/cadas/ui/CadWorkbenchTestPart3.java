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
import de.schrell.cadas.domain.model.RoomObjectShape;
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
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

abstract class CadWorkbenchTestPart3 extends CadWorkbenchTestPart2 {

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
            ToolBar viewOptionsBar = (ToolBar) topArea.getChildren().get(2);

            Assertions.assertFalse(settingsBar.getItems().stream()
                    .anyMatch(CheckBox.class::isInstance));
            Assertions.assertTrue(viewOptionsBar.getItems().stream()
                    .filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast)
                    .map(CheckBox::getText)
                    .anyMatch("ISO-Bemaßung"::equals));
            Assertions.assertFalse(viewOptionsBar.getItems().stream()
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
            ToolBar viewOptionsBar = (ToolBar) topArea.getChildren().get(2);
            Assertions.assertTrue(viewOptionsBar.getItems().stream()
                    .filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast)
                    .map(CheckBox::getText)
                    .anyMatch("Raster"::equals));
            Assertions.assertTrue(viewOptionsBar.getItems().stream()
                    .filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast)
                    .map(CheckBox::getText)
                    .anyMatch("Raster-Snap"::equals));
            Assertions.assertTrue(viewOptionsBar.getItems().stream()
                    .filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast)
                    .map(CheckBox::getText)
                    .anyMatch("Gelände 2D"::equals));
            Assertions.assertTrue(viewOptionsBar.getItems().stream()
                    .filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast)
                    .map(CheckBox::getText)
                    .anyMatch("Heizkreise"::equals));
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
            Assertions.assertTrue(optionenMenu.getItems().stream()
                    .map(MenuItem::getText)
                    .anyMatch("Heizkreise anzeigen"::equals));
            return null;
        });
    }

    @Test
    void eigenschaftsbereicheMerkenKlappzustandProWerkzeug() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            return instanz;
        });

        aufFxThread(() -> {
            propertySection(workbench, "Zeichnen").setExpanded(false);
            workbench.automationSetTool("WALL");

            Assertions.assertTrue(propertySection(workbench, "Zeichnen").isExpanded());
            propertySection(workbench, "Wand").setExpanded(false);
            workbench.automationSetTool("EDIT");

            Assertions.assertFalse(propertySection(workbench, "Zeichnen").isExpanded());
            Assertions.assertTrue(propertySection(workbench, "Wand").isExpanded());
            workbench.automationSetTool("WALL");

            Assertions.assertTrue(propertySection(workbench, "Zeichnen").isExpanded());
            Assertions.assertFalse(propertySection(workbench, "Wand").isExpanded());
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

    @Test
    void rechtsklickNutztBereitsAusgewähltesElementUnterAnderenElementen() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            Room room = Room.rectangular(
                    "Wohnen",
                    new PlanPoint(100, 100),
                    new PlanPoint(3_900, 2_900),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            instanz.project.primaryLevel().addRoomObject(RoomObject.create(
                    "test-tisch",
                    "Tisch",
                    RoomObjectType.TABLE,
                    RoomObjectShape.RECTANGLE,
                    new PlanPoint(2_000, 1_500),
                    Length.ofMillimeters(800),
                    Length.ofMillimeters(600),
                    Length.ofMillimeters(750),
                    false,
                    "Test"
            ));
            instanz.automationSetTool("EDIT");
            instanz.automationSetViewport(1.0, 0.0, 0.0);
            instanz.automationSelect("ROOM", 0, false);
            return instanz;
        });

        aufFxThread(() -> {
            workbench.automationPrepareSelectionContextMenu(200.0, 150.0);
            return null;
        });

        Assertions.assertTrue(aufFxThread(workbench::automationSelectionContextMenuItems).contains("Raum umbenennen …"));
    }

    @Test
    void belagEbenenauswahlRendertSofortDieGewählteEbene() throws Exception {
        CadWorkbench workbench = aufFxThread(() -> {
            CadWorkbench instanz = new CadWorkbench();
            new Scene(instanz, 1200, 800);
            instanz.applyCss();
            instanz.layout();
            Room room = Room.rectangular(
                    "Bad",
                    new PlanPoint(100, 100),
                    new PlanPoint(3_900, 2_900),
                    Length.ofMillimeters(2_600),
                    Length.ofMillimeters(180),
                    Length.ofMillimeters(200)
            );
            instanz.project.primaryLevel().addRoom(room);
            SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
            stack.addLayer(SurfaceLayer.create(
                    "Oben",
                    Length.ofMillimeters(12),
                    Length.ofMillimeters(800),
                    Length.ofMillimeters(800),
                    Length.ofMillimeters(50)
            ));
            stack.addLayer(SurfaceLayer.create(
                    "Darunter",
                    Length.ofMillimeters(18),
                    Length.ofMillimeters(600),
                    Length.ofMillimeters(600),
                    Length.ofMillimeters(50)
            ));
            instanz.project.primaryLevel().addSurfaceLayerStack(stack);
            instanz.automationSetTool("EDIT");
            instanz.automationSetViewport(3.0, 20.0, 20.0);
            instanz.automationSelect("ROOM", 0, false);
            instanz.automationSetSurfaceType("FLOOR");
            instanz.automationSelectSurfaceLayer(0);
            return instanz;
        });

        WorkbenchAutomationSnapshot snapshot = aufFxThread(workbench::automationSnapshot);
        aufFxThread(() -> {
            workbench.surfaceLayerList.getSelectionModel().select(1);
            return null;
        });
        WritableImage image = aufFxThread(workbench::automationDrawingSnapshot);

        assertHervorgehobenerBelagImRaum(
                image,
                snapshot,
                new PlanPoint(300, 300),
                new PlanPoint(3_600, 2_600)
        );
    }
}
