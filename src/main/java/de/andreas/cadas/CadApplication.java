package de.andreas.cadas;

import de.andreas.cadas.ui.CadWorkbench;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class CadApplication extends Application {

    @Override
    public void start(Stage stage) {
        CadWorkbench workbench = new CadWorkbench();
        Scene scene = new Scene(workbench, 1600, 980);
        stage.setTitle("CADas - Gebäude-Grundrisse");
        stage.setMinWidth(1200);
        stage.setMinHeight(760);
        stage.setScene(scene);
        stage.show();
    }
}

