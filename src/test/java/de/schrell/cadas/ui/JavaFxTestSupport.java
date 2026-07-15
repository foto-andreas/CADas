package de.schrell.cadas.ui;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

/**
 * Bündelt die technische JavaFX-Testausführung, damit alle Oberflächentests dieselben
 * Initialisierungs-, Fehler- und Zeitüberschreitungsregeln verwenden.
 */
final class JavaFxTestSupport {

    private static final long FX_TIMEOUT_SECONDS = 30;
    private static boolean initialisiert;

    private JavaFxTestSupport() {
    }

    /**
     * Startet das JavaFX-Toolkit pro Test-JVM genau einmal und hält es zwischen den Tests aktiv.
     */
    static synchronized void initialisieren() {
        if (initialisiert) {
            return;
        }
        new JFXPanel();
        Platform.setImplicitExit(false);
        initialisiert = true;
    }

    /**
     * Führt eine Aufgabe auf dem JavaFX-Anwendungsthread aus und reicht ihre ursprüngliche
     * Ausnahme weiter. Die Zeitgrenze verhindert, dass ein blockierter UI-Test den gesamten
     * Build unbegrenzt anhält.
     *
     * @param aufgabe auszuführende UI-Aufgabe
     * @param <T> Rückgabetyp der Aufgabe
     * @return Ergebnis der UI-Aufgabe
     * @throws Exception ursprüngliche Ausnahme oder Unterbrechung des wartenden Testthreads
     */
    static <T> T aufFxThread(Callable<T> aufgabe) throws Exception {
        FutureTask<T> task = new FutureTask<>(Objects.requireNonNull(aufgabe, "aufgabe"));
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
        try {
            return task.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            task.cancel(false);
            throw new AssertionError(
                    "JavaFX-Aufgabe wurde nicht innerhalb von " + FX_TIMEOUT_SECONDS + " Sekunden abgeschlossen.",
                    exception
            );
        } catch (InterruptedException exception) {
            task.cancel(false);
            Thread.currentThread().interrupt();
            throw exception;
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
}
