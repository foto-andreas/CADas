package de.andreas.cadas;

import de.andreas.cadas.ui.CadWorkbench;
import de.andreas.cadas.ui.AutomationBridgeServer;
import java.util.Objects;
import java.util.Optional;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public final class CadApplication extends Application {

    @Override
    public void start(Stage stage) {
        CadWorkbench workbench = new CadWorkbench();
        Optional<AutomationBridgeServer> automationBridge = AutomationBridgeServer.startIfEnabled(workbench);
        Scene scene = new Scene(workbench, 1600, 980);
        stage.setTitle("CADas - Gebäude-Grundrisse");
        stage.getIcons().add(new Image(Objects.requireNonNull(
                CadApplication.class.getResourceAsStream("/icons/cadas-icon.png"),
                "Anwendungsicon konnte nicht geladen werden."
        )));
        stage.setMinWidth(1200);
        stage.setMinHeight(760);
        stage.setOnCloseRequest(event -> automationBridge.ifPresent(AutomationBridgeServer::stop));
        stage.setScene(scene);
        stage.show();
    }
}
