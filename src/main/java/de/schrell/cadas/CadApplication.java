package de.schrell.cadas;

import de.schrell.cadas.ui.CadWorkbench;
import de.schrell.cadas.ui.AutomationBridgeServer;
import de.schrell.cadas.ui.UiErrorDialogs;
import java.awt.Desktop;
import java.util.Objects;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public final class CadApplication extends Application {

    @Override
    public void start(Stage stage) {
        try {
            CadWorkbench workbench = new CadWorkbench();
            Optional<AutomationBridgeServer> automationBridge = AutomationBridgeServer.startIfEnabled(workbench);
            installExceptionHandling(workbench);
            installDesktopHandlers(workbench);
            Scene scene = new Scene(workbench, 1600, 980);
            stage.setTitle("CADas - Gebäude-Grundrisse");
            stage.getIcons().add(new Image(Objects.requireNonNull(
                    CadApplication.class.getResourceAsStream("/icons/cadas-icon.png"),
                    "Anwendungsicon konnte nicht geladen werden."
            )));
            stage.setMinWidth(1200);
            stage.setMinHeight(760);
            stage.setOnCloseRequest(event -> {
                if (!workbench.confirmApplicationClose()) {
                    event.consume();
                    return;
                }
                automationBridge.ifPresent(AutomationBridgeServer::stop);
            });
            stage.setScene(scene);
            stage.show();
        } catch (RuntimeException exception) {
            UiErrorDialogs.show(
                    UiErrorDialogs.fromThrowable(
                            "CADas konnte nicht gestartet werden",
                            "Die Anwendung ist beim Start fehlgeschlagen.",
                            UiErrorDialogs.userMessage(exception),
                            exception
                    ),
                    stage,
                    true
            );
            throw exception;
        }
    }

    private void installExceptionHandling(CadWorkbench workbench) {
        Thread.UncaughtExceptionHandler handler = (thread, throwable) -> {
            Runnable showError = () -> workbench.handleUnhandledException(throwable);
            if (Platform.isFxApplicationThread()) {
                showError.run();
            } else {
                Platform.runLater(showError);
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler);
    }

    private void installDesktopHandlers(CadWorkbench workbench) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler(event -> Platform.runLater(workbench::showAboutDialog));
        }
    }
}
