package de.schrell.cadas.ui;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

final class WriteTargetDialog {

    private WriteTargetDialog() {
    }

    static Optional<Path> choose(CadWorkbenchBase owner, String title, String header, String initialFileName) {
        Dialog<Path> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        ButtonType writeButtonType = new ButtonType("Schreiben", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelButtonType, writeButtonType);
        Window ownerWindow = owner.currentWindow();
        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
        }

        TextField directoryField = new TextField(defaultDirectory(owner).toString());
        TextField fileNameField = new TextField(initialFileName);
        Button directoryButton = new Button("Ordner");
        directoryButton.setOnAction(event -> chooseDirectory(owner, directoryField));
        GridPane grid = new GridPane();
        grid.setHgap(8.0);
        grid.setVgap(8.0);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Ordner"), 0, 0);
        grid.add(directoryField, 1, 0);
        grid.add(directoryButton, 2, 0);
        grid.add(new Label("Dateiname"), 0, 1);
        grid.add(fileNameField, 1, 1, 2, 1);
        GridPane.setHgrow(directoryField, Priority.ALWAYS);
        GridPane.setHgrow(fileNameField, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(620);

        javafx.scene.Node writeButton = dialog.getDialogPane().lookupButton(writeButtonType);
        owner.applyTooltip(writeButton, "Schreibt die Datei in das angegebene Ziel. Falls sie bereits vorhanden ist, fragt CADas anschließend vor dem Überschreiben nach.");
        owner.applyTooltip(dialog.getDialogPane().lookupButton(cancelButtonType), "Bricht den Schreibvorgang ab und lässt vorhandene Dateien unverändert.");
        owner.applyTooltip(directoryButton, "Öffnet die Ordnerauswahl für den Zielordner.");
        owner.applyTooltip(directoryField, "Legt den Ordner fest, in den die Datei geschrieben wird.");
        owner.applyTooltip(fileNameField, "Legt den Dateinamen fest. Falls die Endung fehlt, ergänzt CADas sie passend zur Aktion.");
        Runnable updateButtonState = () -> writeButton.setDisable(
                directoryField.getText() == null || directoryField.getText().isBlank()
                        || fileNameField.getText() == null || fileNameField.getText().isBlank()
        );
        directoryField.textProperty().addListener((ignored, oldValue, newValue) -> updateButtonState.run());
        fileNameField.textProperty().addListener((ignored, oldValue, newValue) -> updateButtonState.run());
        updateButtonState.run();

        dialog.setResultConverter(buttonType -> {
            if (!writeButtonType.equals(buttonType)) {
                return null;
            }
            return targetPath(directoryField.getText(), fileNameField.getText()).orElse(null);
        });
        return dialog.showAndWait();
    }

    static boolean confirmOverwrite(CadWorkbenchBase owner, Path targetPath, String documentName) {
        if (!Files.exists(targetPath)) {
            return true;
        }
        return owner.confirmOverwrite(
                documentName + " überschreiben",
                "Die Datei `" + targetPath.getFileName() + "` ist bereits vorhanden.",
                "Soll die vorhandene Datei ersetzt werden?"
        );
    }

    private static void chooseDirectory(CadWorkbenchBase owner, TextField directoryField) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Zielordner wählen");
        targetPath(directoryField.getText(), "")
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .filter(Files::isDirectory)
                .ifPresent(path -> directoryChooser.setInitialDirectory(path.toFile()));
        java.io.File directory = directoryChooser.showDialog(owner.currentWindow());
        if (directory != null) {
            directoryField.setText(directory.toPath().toAbsolutePath().normalize().toString());
        }
    }

    private static Optional<Path> targetPath(String directory, String fileName) {
        try {
            return Optional.of(Path.of(directory.trim()).resolve(fileName.trim()));
        } catch (InvalidPathException exception) {
            return Optional.empty();
        }
    }

    private static Path defaultDirectory(CadWorkbenchBase owner) {
        return Optional.ofNullable(owner.lastProjectSavePath)
                .or(() -> Optional.ofNullable(owner.lastLevelSavePath))
                .map(Path::getParent)
                .filter(Objects::nonNull)
                .orElse(Path.of(System.getProperty("user.home")).toAbsolutePath().normalize());
    }
}
