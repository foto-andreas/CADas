package de.schrell.cadas.ui;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;
import de.schrell.cadas.infrastructure.dxf.DxfProjectExchangeService;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

abstract class CadWorkbenchTestBase {

    @BeforeAll
    static void initialisiertJavaFxToolkit() {
        new JFXPanel();
    }

    Path erzeugeEinfachesProjektAlsDxf() throws Exception {
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

    Path erzeugeProjektMitPickpunktenAlsDxf() throws Exception {
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

    Path erzeugeProjektMitTrennwandAlsDxf() throws Exception {
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

    void assertPickkreis(WritableImage image, WorkbenchAutomationSnapshot snapshot, PlanPoint point) {
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

    TitledPane propertySection(CadWorkbench workbench, String title) {
        SplitPane splitPane = (SplitPane) workbench.getCenter();
        ScrollPane propertyPane = (ScrollPane) splitPane.getItems().getFirst();
        VBox container = (VBox) propertyPane.getContent();
        VBox sections = (VBox) container.getChildren().get(1);
        return sections.getChildren().stream()
                .filter(TitledPane.class::isInstance)
                .map(TitledPane.class::cast)
                .filter(section -> title.equals(section.getText()))
                .findFirst()
                .orElseThrow();
    }

    void assertHervorgehobenerBelagImRaum(
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

    int countVariothermCirclePixels(
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

    int countVariothermCirclePixels(WritableImage image) {
        int count = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
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

    HydronicHeating hydronicHeatingForReportTest(
            Room room,
            HeatingSurfacePosition surfacePosition,
            String zoneName,
            PlanPoint minPoint,
            PlanPoint maxPoint
    ) {
        HeatingZone zone = HeatingZone.create(zoneName, List.of(
                minPoint,
                new PlanPoint(maxPoint.xMillimeters(), minPoint.yMillimeters()),
                maxPoint,
                new PlanPoint(minPoint.xMillimeters(), maxPoint.yMillimeters())
        ), HeatingLayoutPattern.MEANDER);
        return HydronicHeating.create(
                room.id(),
                surfacePosition,
                HeatingLayoutPattern.MEANDER,
                Length.ofMillimeters(250),
                Length.ofMillimeters(16),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                minPoint,
                new PlanPoint(minPoint.xMillimeters() + 250, minPoint.yMillimeters())
        ).withZones(List.of(zone));
    }

    int countHeatingCircuitPixels(
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
                boolean heatingRed = color.getRed() > 0.70 && color.getGreen() < 0.35 && color.getBlue() < 0.35;
                boolean heatingBlue = color.getBlue() > 0.60 && color.getRed() < 0.35 && color.getGreen() < 0.50;
                if (heatingRed || heatingBlue) {
                    count++;
                }
            }
        }
        return count;
    }

    int countHeatingColorPixels(WritableImage image) {
        int count = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                var color = image.getPixelReader().getColor(x, y);
                boolean heatingRed = color.getRed() > 0.70 && color.getGreen() < 0.35 && color.getBlue() < 0.35;
                boolean heatingBlue = color.getBlue() > 0.60 && color.getRed() < 0.35 && color.getGreen() < 0.50;
                if (heatingRed || heatingBlue) {
                    count++;
                }
            }
        }
        return count;
    }

    int countDarkPixels(WritableImage image, int minX, int minY, int maxX, int maxY) {
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

    int countDarkPixels(
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

    int countLightPixels(
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

    int countNonBackgroundPixels(WritableImage image, int minX, int minY, int maxX, int maxY) {
        int count = 0;
        for (int x = Math.max(0, minX); x <= Math.min((int) image.getWidth() - 1, maxX); x++) {
            for (int y = Math.max(0, minY); y <= Math.min((int) image.getHeight() - 1, maxY); y++) {
                var color = image.getPixelReader().getColor(x, y);
                if (!isCanvasBackground(color)) {
                    count++;
                }
            }
        }
        return count;
    }

    boolean isCanvasBackground(javafx.scene.paint.Color color) {
        return Math.abs(color.getRed() - 0.988) <= 0.03
                && Math.abs(color.getGreen() - 0.980) <= 0.03
                && Math.abs(color.getBlue() - 0.961) <= 0.03;
    }

    Path erzeugeProjektMitInnenwandfliesenAlsDxf() throws Exception {
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

    static <T> T aufFxThread(Callable<T> aufgabe) throws Exception {
        FutureTask<T> task = new FutureTask<>(aufgabe);
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

    static TextArea heatingRoutingCommandArea(CadWorkbench workbench) {
        try {
            Field field = declaredField(CadWorkbench.class, "heatingRoutingCommandArea");
            field.setAccessible(true);
            return (TextArea) field.get(workbench);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Heizkreis-Textfeld konnte nicht gefunden werden.", exception);
        }
    }

    static void setBooleanProperty(CadWorkbench workbench, String fieldName, boolean value) {
        try {
            Field field = declaredField(CadWorkbench.class, fieldName);
            field.setAccessible(true);
            ((javafx.beans.property.BooleanProperty) field.get(workbench)).set(value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Boolesche Eigenschaft `" + fieldName + "` konnte nicht gefunden werden.", exception);
        }
    }

    static Field declaredField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

}
