package de.schrell.cadas.ui;

import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingPoint;
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
            Assertions.assertEquals("Ix", window.automationProtocol());
            Assertions.assertEquals("I", window.automationCommands());

            window.automationUndo();
            Assertions.assertEquals("I", window.automationProtocol());
            Assertions.assertEquals("I", window.automationCommands());

            window.automationRedo();
            Assertions.assertEquals("Ix", window.automationProtocol());
            Assertions.assertEquals("I", window.automationCommands());
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

            Assertions.assertEquals("Ii", window.automationProtocol());
            Assertions.assertEquals("Ii", window.automationCommands());
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
            return null;
        });
    }

    @Test
    void drehtHeizkreisGemeinsamMitHintergrund() throws Exception {
        HeatingCircuitRoutingWindow window = aufFxThread(HeatingCircuitRoutingWindow::new);

        aufFxThread(() -> {
            window.automationInput("I");
            RoutingPoint before = window.automationSupplyEndPoint();
            window.automationRotateArea();
            RoutingPoint after = window.automationSupplyEndPoint();

            Assertions.assertEquals(0.0, before.xMillimeters(), 0.001);
            Assertions.assertTrue(Math.abs(after.xMillimeters()) > 0.001);
            return null;
        });
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
