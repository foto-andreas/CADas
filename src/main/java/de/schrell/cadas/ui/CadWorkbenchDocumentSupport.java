package de.schrell.cadas.ui;

import de.schrell.cadas.application.help.AboutInformation;
import de.schrell.cadas.application.help.MarkdownNavigationService.HelpSection;
import de.schrell.cadas.application.reports.ConstructionDrawingOptions;
import de.schrell.cadas.application.reports.SurfaceMaterialListService.SurfaceMaterialReport;
import de.schrell.cadas.application.reports.SurfaceMaterialReportPdfService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Bündelt Dokument-, Hilfe- und Druckfenster der Workbench.
 */
final class CadWorkbenchDocumentSupport {

    private final CadWorkbench owner;
    private final SurfaceMaterialReportPdfService surfaceMaterialReportPdfService = new SurfaceMaterialReportPdfService();

    CadWorkbenchDocumentSupport(CadWorkbench owner) {
        this.owner = owner;
    }

    void showSurfaceMaterialReportWindow() {
        SurfaceMaterialReport report = owner.surfaceMaterialListService.create(owner.project);
        String markdown = report.toDisplayMarkdown();
        WebView reportView = new WebView();
        reportView.getEngine().loadContent(owner.markdownHtmlRenderer.renderDocument(markdown));
        VBox.setVgrow(reportView, Priority.ALWAYS);
        Button exportButton = new Button("Markdown exportieren");
        exportButton.setOnAction(event -> exportSurfaceMaterialReportMarkdown(report));
        owner.applyTooltip(exportButton, "Exportiert genau diese Materialliste als Markdown-Datei.");
        Button exportPdfButton = new Button("PDF exportieren");
        exportPdfButton.setOnAction(event -> exportSurfaceMaterialReportPdf(report));
        owner.applyTooltip(exportPdfButton, "Exportiert genau diese Materialliste als PDF-Datei ohne eingebettete SVG-Vorschauen.");
        Button printButton = new Button("Drucken");
        printButton.setOnAction(event -> printSurfaceMaterialReport(reportView));
        owner.applyTooltip(printButton, "Druckt die gerenderte Materialliste so, wie sie in diesem Fenster angezeigt wird.");
        HBox actions = new HBox(8.0, printButton, exportPdfButton, exportButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox container = new VBox(10.0, reportView, actions);
        container.setPadding(new Insets(12));
        Stage stage = new Stage();
        stage.setTitle("Räume und Materialien");
        Window ownerWindow = owner.currentWindow();
        if (ownerWindow != null) {
            stage.initOwner(ownerWindow);
        }
        stage.setScene(new Scene(container, 920, 680));
        stage.show();
    }

    void exportConstructionDrawingPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Maßstabgerechte Bauzeichnung als PDF speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Dateien", "*.pdf"));
        String projectName = owner.exchangeFileNameService.stripRepeatedExtension(Path.of(owner.project.name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(projectName + "_Bauzeichnung.pdf");
        java.io.File file = fileChooser.showSaveDialog(owner.currentWindow());
        if (file == null) {
            return;
        }
        try {
            Path target = owner.exchangeFileNameService.ensureSingleExtension(file.toPath(), ".pdf");
            ConstructionDrawingOptions options = new ConstructionDrawingOptions(
                    owner.currentDimensionLabelOptions(),
                    owner.showDimensions.get(),
                    owner.showAreaVolume.get()
            );
            owner.constructionDrawingPdfService.export(owner.project, target, options);
            owner.draftLabel.setText("Bauzeichnungs-PDF exportiert: " + target.getFileName());
        } catch (Exception exception) {
            owner.showOperationException("PDF-Export fehlgeschlagen", exception);
        }
    }

    void showHelpWindow() {
        showMarkdownWindow(
                owner.helpContentService.createMarkdown(),
                "CADas-Benutzerdokumentation",
                "Benutzerdokumentation",
                "Druckt die vollständige Benutzerdokumentation. Im Druckdialog kann auch ein PDF-Drucker gewählt werden."
        );
    }

    void showKeymapWindow() {
        showMarkdownWindow(
                owner.helpContentService.createKeymapMarkdown(),
                "CADas-Keymap und Mausbedienung",
                "Keymap und Mausbedienung",
                "Druckt die Tastaturkürzel und Mausbedienung. Im Druckdialog kann auch ein PDF-Drucker gewählt werden."
        );
    }

    void showThirdPartyLicensesWindow() {
        showMarkdownWindow(
                owner.helpContentService.createThirdPartyLicensesMarkdown(),
                "CADas-Drittanbieter-Lizenzen",
                "Drittanbieter-Lizenzen",
                "Druckt die automatisch erzeugte Liste aller Drittanbieter-Lizenzen."
        );
    }

    void showAboutDialog() {
        if (!owner.interactiveDialogsEnabled) {
            return;
        }
        AboutInformation information = AboutInformation.current();
        Alert alert = new Alert(Alert.AlertType.INFORMATION, information.detailText(), ButtonType.OK);
        alert.setTitle("Über CADas");
        alert.setHeaderText(information.applicationName());
        Window ownerWindow = owner.currentWindow();
        if (ownerWindow != null) {
            alert.initOwner(ownerWindow);
        }
        alert.showAndWait();
    }

    void showMarkdownWindow(String markdown, String windowTitle, String documentName, String printTooltip) {
        WebView view = new WebView();
        view.getEngine().loadContent(owner.markdownHtmlRenderer.renderDocument(markdown));
        VBox.setVgrow(view, Priority.ALWAYS);
        ComboBox<HelpSection> sectionSelector = new ComboBox<>();
        sectionSelector.getItems().setAll(owner.markdownNavigationService.sections(markdown));
        sectionSelector.setPromptText("Inhaltsverzeichnis");
        sectionSelector.setPrefWidth(300);
        sectionSelector.setOnAction(event -> Optional.ofNullable(sectionSelector.getValue())
                .ifPresent(section -> view.getEngine().executeScript(
                        "document.getElementById('" + section.anchor() + "').scrollIntoView({behavior:'smooth',block:'start'});"
                )));
        owner.applyTooltip(sectionSelector, "Listet alle Kapitel und Unterkapitel auf und springt direkt zum gewählten Abschnitt der Dokumentation.");
        TextField searchField = new TextField();
        searchField.setPromptText("Dokumentation durchsuchen");
        searchField.setPrefWidth(260);
        owner.applyTooltip(searchField, "Sucht im vollständigen Text der geöffneten Dokumentation. Mit Eingabe oder Weiter wird der nächste Treffer markiert.");
        Button previousButton = new Button("Zurück");
        previousButton.setOnAction(event -> findInWebView(view, searchField.getText(), true));
        owner.applyTooltip(previousButton, "Springt rückwärts zum vorherigen Treffer des eingegebenen Suchbegriffs.");
        Button nextButton = new Button("Weiter");
        nextButton.setOnAction(event -> findInWebView(view, searchField.getText(), false));
        owner.applyTooltip(nextButton, "Springt vorwärts zum nächsten Treffer des eingegebenen Suchbegriffs.");
        searchField.setOnAction(event -> findInWebView(view, searchField.getText(), false));
        HBox navigation = new HBox(8.0, sectionSelector, searchField, previousButton, nextButton);
        navigation.setAlignment(Pos.CENTER_LEFT);
        Button printButton = new Button("Drucken");
        printButton.setOnAction(event -> printWebView(view, documentName));
        owner.applyTooltip(printButton, printTooltip);
        HBox actions = new HBox(8.0, printButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox container = new VBox(10.0, navigation, view, actions);
        container.setPadding(new Insets(12));
        Stage stage = new Stage();
        stage.setTitle(windowTitle);
        Window ownerWindow = owner.currentWindow();
        if (ownerWindow != null) {
            stage.initOwner(ownerWindow);
        }
        stage.setScene(new Scene(container, 960, 760));
        stage.show();
    }

    void findInWebView(WebView view, String searchText, boolean backwards) {
        if (searchText == null || searchText.isBlank()) {
            return;
        }
        String escaped = searchText.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
        view.getEngine().executeScript("window.find('" + escaped + "',false," + backwards + ",true,false,true,false);");
    }

    void printSurfaceMaterialReport(WebView reportView) {
        printWebView(reportView, "Materialliste");
    }

    void printWebView(WebView reportView, String documentName) {
        PrinterJob printerJob = PrinterJob.createPrinterJob();
        if (printerJob == null) {
            owner.draftLabel.setText("Kein Drucker verfügbar.");
            return;
        }
        Window ownerWindow = owner.currentWindow();
        if (owner.interactiveDialogsEnabled && !printerJob.showPrintDialog(ownerWindow)) {
            owner.draftLabel.setText("Druck abgebrochen.");
            return;
        }
        reportView.getEngine().print(printerJob);
        printerJob.endJob();
        owner.draftLabel.setText(documentName + " an Drucker übergeben.");
    }

    void exportSurfaceMaterialReportMarkdown() {
        exportSurfaceMaterialReportMarkdown(owner.surfaceMaterialListService.create(owner.project));
    }

    void exportSurfaceMaterialReportMarkdown(SurfaceMaterialReport report) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Materialliste als Markdown speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown-Dateien", "*.md"));
        String projectName = owner.exchangeFileNameService.stripRepeatedExtension(Path.of(owner.project.name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(projectName + "_Räume_und_Material");
        java.io.File file = fileChooser.showSaveDialog(owner.currentWindow());
        if (file == null) {
            return;
        }
        exportSurfaceMaterialReportMarkdown(report, file.toPath());
    }

    void exportSurfaceMaterialReportMarkdown(Path targetFile) {
        exportSurfaceMaterialReportMarkdown(owner.surfaceMaterialListService.create(owner.project), targetFile);
    }

    void exportSurfaceMaterialReportMarkdown(SurfaceMaterialReport report, Path targetFile) {
        try {
            Path exportPath = owner.exchangeFileNameService.ensureSingleExtension(targetFile, ".md");
            Files.writeString(exportPath, report.toMarkdown());
            owner.draftLabel.setText("Materialliste exportiert: " + exportPath.getFileName());
        } catch (Exception exception) {
            owner.showOperationException("Materiallisten-Export fehlgeschlagen", exception);
        }
    }

    void exportSurfaceMaterialReportPdf() {
        exportSurfaceMaterialReportPdf(owner.surfaceMaterialListService.create(owner.project));
    }

    void exportSurfaceMaterialReportPdf(SurfaceMaterialReport report) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Materialliste als PDF speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Dateien", "*.pdf"));
        String projectName = owner.exchangeFileNameService.stripRepeatedExtension(Path.of(owner.project.name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(projectName + "_Räume_und_Material.pdf");
        java.io.File file = fileChooser.showSaveDialog(owner.currentWindow());
        if (file == null) {
            return;
        }
        exportSurfaceMaterialReportPdf(report, file.toPath());
    }

    void exportSurfaceMaterialReportPdf(Path targetFile) {
        exportSurfaceMaterialReportPdf(owner.surfaceMaterialListService.create(owner.project), targetFile);
    }

    void exportSurfaceMaterialReportPdf(SurfaceMaterialReport report, Path targetFile) {
        try {
            Path exportPath = owner.exchangeFileNameService.ensureSingleExtension(targetFile, ".pdf");
            surfaceMaterialReportPdfService.export(report, exportPath);
            owner.draftLabel.setText("Materialliste als PDF exportiert: " + exportPath.getFileName());
        } catch (Exception exception) {
            owner.showOperationException("Materiallisten-PDF-Export fehlgeschlagen", exception);
        }
    }
}
