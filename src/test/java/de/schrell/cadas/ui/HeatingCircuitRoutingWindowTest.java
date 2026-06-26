package de.schrell.cadas.ui;

import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingPoint;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingResult;
import de.schrell.cadas.domain.model.HeatingRoutingLanguage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HeatingCircuitRoutingWindowTest {

    @BeforeAll
    static void initialisiertJavaFxToolkit() {
        new JFXPanel();
    }

    @Test
    void machtProtokolleingabenRückgängigUndWiederher() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            window.automationInput("I");
            window.automationInput("a");
            Assertions.assertEquals("=x", window.automationProtocol());
            Assertions.assertEquals("=", window.automationCommands());

            window.automationUndo();
            Assertions.assertEquals("=", window.automationProtocol());
            Assertions.assertEquals("=", window.automationCommands());

            window.automationRedo();
            Assertions.assertEquals("=x", window.automationProtocol());
            Assertions.assertEquals("=", window.automationCommands());
            return null;
        });
    }

    @Test
    void neueEingabeVerwirftRedoStack() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            window.automationInput("I");
            window.automationInput("R");
            window.automationUndo();
            window.automationInput("i");
            window.automationRedo();

            Assertions.assertEquals("=-", window.automationProtocol());
            Assertions.assertEquals("=-", window.automationCommands());
            return null;
        });
    }

    @Test
    void rendertEditiertenKommandotextSofort() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            Assertions.assertTrue(window.automationProtocolEditable());

            window.automationSetProtocolText("I R\nzi");

            Assertions.assertEquals("=Rx-", window.automationProtocol());
            Assertions.assertEquals("=R-", window.automationCommands());
            return null;
        });
    }

    @Test
    void interpretiertGespiegelteKurvenAliaseImTestfensterSofort() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            window.automationSetSimpleMirrored(true);
            window.automationSetProtocolText("89()");

            Assertions.assertEquals("lrLR", window.automationProtocol());
            Assertions.assertEquals("lrLR", window.automationCommands());
            return null;
        });
    }

    @Test
    void zoomtImTestfensterEinUndAus() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            double startZoom = window.automationZoomFactor();
            window.automationZoomIn();
            Assertions.assertTrue(window.automationZoomFactor() > startZoom);
            window.automationZoomOut();
            Assertions.assertEquals(startZoom, window.automationZoomFactor(), 0.001);
            return null;
        });
    }

    @Test
    void erzeugtVarioAusAktuellenMaßen() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            window.automationGenerateVario();

            Assertions.assertEquals("200x300", window.automationAreaSizeText());
            Assertions.assertFalse(window.automationCommands().isBlank());
            Assertions.assertEquals(window.automationCommands(), window.automationProtocol());
            Assertions.assertTrue(window.automationCommands().endsWith("+"));
            return null;
        });
    }

    @Test
    void erzeugtVarioMitSchlangenförmigerMittellinie() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            window.automationSetAreaSize("70x160");
            window.automationGenerateVario();
            String standardCommands = window.automationCommands();

            window.automationSetSerpentineMiddleLine(true);
            window.automationGenerateVario();

            Assertions.assertNotEquals(standardCommands, window.automationCommands());
            Assertions.assertTrue(window.automationCommands().startsWith(
                    modern("rLRRllrrLLRRllrrLLRRllrriIRr"
                            + "iiiiiiiiiiiirIIIIIIIIIIIIR"
                            + "iiirIIIR")
            ));
            Assertions.assertTrue(window.automationCommands().endsWith("+"));
            return null;
        });
    }

    @Test
    void erzeugtMeanderAusAktuellenMaßen() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            window.automationGenerateMeander();

            Assertions.assertEquals("200x300", window.automationAreaSizeText());
            Assertions.assertFalse(window.automationCommands().isBlank());
            Assertions.assertEquals(window.automationCommands(), window.automationProtocol());
            Assertions.assertTrue(window.automationCommands().startsWith(modern("i".repeat(14) + "ll")));
            Assertions.assertTrue(window.automationCommands().endsWith("+"));
            return null;
        });
    }

    @Test
    void meanderSchlangenSchalterErsetztMittlereGerade() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            window.automationGenerateMeander();
            String standardCommands = window.automationCommands();

            window.automationSetSerpentineMiddleLine(true);
            window.automationGenerateMeander();

            Assertions.assertNotEquals(standardCommands, window.automationCommands());
            return null;
        });
    }

    @Test
    void sichertAktuellesRoutingAlsHeizkreisTestdatei() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            window.automationGenerateVario();
            Path testFile = window.automationSaveRoutingTestFile();
            Assertions.assertNotNull(testFile);
            try {
                String content = Files.readString(testFile, StandardCharsets.UTF_8);
                Assertions.assertTrue(content.contains("format=cadas-fbh-routing-v1"));
                Assertions.assertTrue(content.contains("breiteCm=200"));
                Assertions.assertTrue(content.contains("höheCm=300"));
                Assertions.assertTrue(content.contains("verlegeabstandCm=10"));
                Assertions.assertTrue(content.contains("variante=Vario"));
                Assertions.assertTrue(content.contains("kanonischeKommandos="));
                Assertions.assertTrue(content.contains("kommandos=" + window.automationCommands()));
            } finally {
                Files.deleteIfExists(testFile);
            }
            return null;
        });
    }

    @Test
    void verlängertUndKürztZuleitungsendenSegmentweise() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            window.automationInput("Ii");

            window.automationExtendSupply();
            Assertions.assertEquals("=-+=", window.automationCommands());
            Assertions.assertEquals(window.automationCommands(), window.automationProtocol());

            window.automationShortenSupply();
            Assertions.assertEquals("=-+", window.automationCommands());

            window.automationExtendReturn();
            Assertions.assertEquals("=-+-", window.automationCommands());

            window.automationShortenReturn();
            Assertions.assertEquals("=-+", window.automationCommands());
            return null;
        });
    }

    @Test
    void berechnetRenderBoundsFürSeparatorZuläufeOhneÜberstehendeEndsegmente() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);
        HeatingCircuitCommandRouter router = new HeatingCircuitCommandRouter();
        RoutingResult ii = router.route(2_000.0, 3_000.0, 100.0, "=-");
        RoutingResult separated = router.route(2_000.0, 3_000.0, 100.0, "=+-");

        aufFxThread(() -> {
            HeatingCircuitRoutingWindow.Bounds iiBounds = window.routeBounds(ii);
            HeatingCircuitRoutingWindow.Bounds separatedBounds = window.routeBounds(separated);

            Assertions.assertEquals(0.0, iiBounds.minX(), 0.001);
            Assertions.assertEquals(0.0, iiBounds.maxX(), 0.001);
            Assertions.assertEquals(-100.0, iiBounds.minY(), 0.001);
            Assertions.assertEquals(100.0, iiBounds.maxY(), 0.001);

            Assertions.assertEquals(0.0, separatedBounds.minX(), 0.001);
            Assertions.assertEquals(0.0, separatedBounds.maxX(), 0.001);
            Assertions.assertEquals(0.0, separatedBounds.minY(), 0.001);
            Assertions.assertEquals(100.0, separatedBounds.maxY(), 0.001);
            return null;
        });
    }

    @Test
    void verwendetPlusAlsFeldgrenzeUndNichtAlsEigenesSegment() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            window.automationGenerateVario();
            String fullRectangle = window.automationCommands();
            Assertions.assertTrue(fullRectangle.endsWith("+"));

            window.automationExtendSupply();
            window.automationExtendReturn();

            Assertions.assertEquals(fullRectangle + "=-", window.automationCommands());
            Assertions.assertEquals(window.automationCommands(), window.automationProtocol());
            return null;
        });
    }

    @Test
    void drehtHeizkreisGemeinsamMitHintergrund() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            window.automationInput("=");
            RoutingPoint before = window.automationSupplyEndPoint();
            window.automationRotateArea();
            RoutingPoint after = window.automationSupplyEndPoint();

            Assertions.assertEquals(0.0, before.xMillimeters(), 0.001);
            Assertions.assertTrue(Math.abs(after.xMillimeters()) > 0.001);
            return null;
        });
    }

    private String modern(String legacyCommands) {
        return HeatingRoutingLanguage.normalizeCommands(legacyCommands);
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
