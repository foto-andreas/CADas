package de.schrell.cadas.ui;

import java.util.Set;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;

/**
 * Öffentliche JavaFX-Gesamtkomponente der CADas-Arbeitsfläche.
 * Die geerbten, paketinternen Schichten teilen den großen UI-Zustand nach Interaktion, Rendering,
 * Projektaktionen, Oberflächen und Automatisierung auf; diese Klasse stellt den stabilen Einstieg bereit.
 */
public final class CadWorkbench extends CadWorkbenchAutomation {

    public CadWorkbench() {
        // Oberer Bereich (Werkzeugleiste) soll bündig oben anliegen, daher
        // nur unten/links/rechts padding, oben 0.
        setPadding(new Insets(0, 12, 12, 12));
        setStyle("-fx-background-color: linear-gradient(to bottom, #f6f1e8, #ece5d8);");
        if (automationActive()) {
            interactiveDialogsEnabled = false;
        }

        configureControls();
        configureLayout();
        configureCanvas();
        threeDViewport.syncLevels(availableLevels, activeLevel.get().name());
        selectedSelection.addListener((ignored, oldValue, newValue) -> {
            threeDViewport.setSelectedSelection(newValue);
            threeDViewport.setSelectedSelections(Set.copyOf(selectedSelections));
            updatePropertySectionVisibility();
            updateActionButtons();
            syncInputsFromPrimarySelection();
            render();
        });
        sceneProperty().addListener((ignored, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.ESCAPE),
                        this::clearSelection
                );
                newScene.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), documentSupport::showHelpWindow);
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleGlobalShortcuts);
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::updateModifierState);
                newScene.addEventFilter(KeyEvent.KEY_RELEASED, this::updateModifierState);
            }
        });
        updatePropertySectionVisibility();
        updateActionButtons();
        updateWorkspaceMode();
        fitCurrentViewToContent();
        updateStatus();
        render();
    }
}
