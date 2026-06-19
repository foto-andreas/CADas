package de.andreas.cadas.ui;

import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class UiErrorDialogs {

    private UiErrorDialogs() {
    }

    public static ErrorPresentation fromThrowable(String title, String header, String content, Throwable throwable) {
        String normalizedContent = normalizeContent(content, throwable);
        return new ErrorPresentation(title, header, normalizedContent, stackTrace(throwable));
    }

    public static void show(ErrorPresentation presentation, Window owner, boolean blocking) {
        if (!blocking) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.ERROR, presentation.content(), ButtonType.OK);
        alert.setTitle(presentation.title());
        alert.setHeaderText(presentation.header());
        alert.getDialogPane().setPrefWidth(720);
        if (!presentation.stackTrace().isBlank()) {
            alert.getDialogPane().setExpandableContent(expandableStackTrace(presentation.stackTrace()));
            alert.getDialogPane().setExpanded(false);
        }
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    public static String userMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unbekannter Fehler.";
        }
        Throwable rootCause = rootCause(throwable);
        String message = rootCause.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getMessage();
        }
        if (message == null || message.isBlank()) {
            return rootCause.getClass().getSimpleName();
        }
        return message;
    }

    private static VBox expandableStackTrace(String stackTrace) {
        Label label = new Label("Stacktrace");
        Button copyButton = new Button("Stacktrace kopieren");
        copyButton.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(stackTrace);
            Clipboard.getSystemClipboard().setContent(content);
        });
        HBox headerRow = new HBox(10.0, label, copyButton);
        TextArea stackTraceArea = new TextArea(stackTrace);
        stackTraceArea.setEditable(false);
        stackTraceArea.setWrapText(false);
        stackTraceArea.setPrefRowCount(18);
        VBox.setVgrow(stackTraceArea, Priority.ALWAYS);
        VBox container = new VBox(8.0, headerRow, stackTraceArea);
        container.setPadding(new Insets(8, 0, 0, 0));
        return container;
    }

    private static String normalizeContent(String content, Throwable throwable) {
        if (content != null && !content.isBlank()) {
            return content;
        }
        return userMessage(throwable);
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String stackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    public record ErrorPresentation(String title, String header, String content, String stackTrace) {

        public static ErrorPresentation empty() {
            return new ErrorPresentation("", "", "", "");
        }
    }
}
