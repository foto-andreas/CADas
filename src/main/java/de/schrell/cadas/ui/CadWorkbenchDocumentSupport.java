package de.schrell.cadas.ui;

import de.schrell.cadas.application.help.AboutInformation;
import de.schrell.cadas.application.help.MarkdownNavigationService.HelpSection;
import de.schrell.cadas.application.layers.SurfaceCoveringPresetService;
import de.schrell.cadas.application.reports.ConstructionDrawingOptions;
import de.schrell.cadas.application.reports.ConstructionDrawingPdfService;
import de.schrell.cadas.application.reports.SurfaceMaterialListService;
import de.schrell.cadas.application.reports.SurfaceMaterialListService.SurfaceMaterialReport;
import de.schrell.cadas.application.reports.SurfaceMaterialReportPdfService;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObjectHeatingType;
import de.schrell.cadas.domain.model.SurfaceType;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Bündelt Dokument-, Hilfe- und Druckfenster der Workbench.
 */
final class CadWorkbenchDocumentSupport {

    private final CadWorkbenchBase owner;
    private final SurfaceMaterialReportPdfService surfaceMaterialReportPdfService = new SurfaceMaterialReportPdfService();
    private List<HeatingLoadField> openHeatingLoadFields = List.of();

    CadWorkbenchDocumentSupport(CadWorkbenchBase owner) {
        this.owner = owner;
    }

    void showSurfaceMaterialReportWindow() {
        runWithProgressDialog(
                "Materialliste wird erstellt",
                "Materialdaten werden gesammelt",
                "Materialliste konnte nicht erstellt werden",
                progress -> {
                    progress.update(0.15, "Materialdaten werden gesammelt");
                    SurfaceMaterialReport report = owner.surfaceMaterialListService.create(owner.project);
                    progress.update(0.8, "Ansicht wird vorbereitet");
                    return new MaterialReportWindowContent(
                            report,
                            owner.markdownHtmlRenderer.renderDocument(report.toDisplayMarkdown())
                    );
                },
                content -> showSurfaceMaterialReportWindow(content.report(), content.renderedHtml())
        );
    }

    void showHeatingLoadWindow() {
        GridPane grid = new GridPane();
        grid.setHgap(10.0);
        grid.setVgap(8.0);
        grid.addRow(0, new Label("Etage"), new Label("Raum"), new Label("Heizlast W"));
        List<HeatingLoadField> fields = new ArrayList<>();
        int rowIndex = 1;
        for (Level level : owner.project.levels()) {
            for (Room room : level.rooms()) {
                TextField field = new TextField(formatWatts(room.heatLoadWatts()));
                field.setPrefColumnCount(8);
                owner.applyTooltip(field, "Legt die Heizlast dieses Raums in Watt fest. Leere Eingaben werden als 0 W gespeichert.");
                grid.addRow(rowIndex, new Label(level.name()), new Label(room.name()), field);
                fields.add(new HeatingLoadField(level, room, field));
                rowIndex++;
            }
        }
        openHeatingLoadFields = fields;
        if (fields.isEmpty()) {
            grid.add(new Label("Keine Räume vorhanden."), 0, rowIndex, 3, 1);
        }
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        Button saveButton = new Button("Speichern");
        owner.applyTooltip(saveButton, "Speichert die eingetragenen Heizlasten in Watt im aktuellen Projekt.");
        Button closeButton = new Button("Schließen");
        owner.applyTooltip(closeButton, "Schließt das Fenster ohne weitere Änderungen.");
        HBox actions = new HBox(8.0, saveButton, closeButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox container = new VBox(10.0, scrollPane, actions);
        container.setPadding(new Insets(12));
        Stage stage = new Stage();
        stage.setTitle("Heizlast");
        Window ownerWindow = owner.currentWindow();
        if (ownerWindow != null) {
            stage.initOwner(ownerWindow);
        }
        saveButton.setOnAction(event -> {
            if (commitHeatingLoads(fields)) {
                owner.draftLabel.setText("Heizlasten gespeichert.");
                stage.close();
            }
        });
        closeButton.setOnAction(event -> stage.close());
        stage.setOnHidden(event -> {
            if (openHeatingLoadFields == fields) {
                openHeatingLoadFields = List.of();
            }
        });
        stage.setScene(new Scene(container, 620, 520));
        stage.show();
    }

    boolean commitOpenHeatingLoads() {
        return openHeatingLoadFields.isEmpty() || commitHeatingLoads(openHeatingLoadFields);
    }

    private boolean commitHeatingLoads(List<HeatingLoadField> fields) {
        Map<java.util.UUID, Double> loadsByRoomId = new LinkedHashMap<>();
        for (HeatingLoadField field : fields) {
            try {
                double value = parseWatts(field.field().getText());
                loadsByRoomId.put(field.room().id(), value);
            } catch (IllegalArgumentException exception) {
                owner.draftLabel.setText("Heizlast für " + field.room().name() + " ist ungültig.");
                field.field().requestFocus();
                return false;
            }
        }
        boolean changed = owner.project.levels().stream()
                .flatMap(level -> level.rooms().stream())
                .anyMatch(room -> Double.compare(loadsByRoomId.getOrDefault(room.id(), room.heatLoadWatts()), room.heatLoadWatts()) != 0);
        if (!changed) {
            return true;
        }
        owner.rememberStateForUndo();
        for (Level level : owner.project.levels()) {
            level.replaceRooms(level.rooms().stream()
                    .map(room -> room.withHeatLoadWatts(loadsByRoomId.getOrDefault(room.id(), room.heatLoadWatts())))
                    .toList());
        }
        owner.render();
        return true;
    }

    private double parseWatts(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        double value = Double.parseDouble(text.replace(',', '.').trim());
        if (value < 0.0 || !Double.isFinite(value)) {
            throw new IllegalArgumentException("Heizlast ungültig.");
        }
        return value;
    }

    static String formatWatts(double watts) {
        return String.format(java.util.Locale.GERMAN, "%.0f", watts);
    }

    private void showSurfaceMaterialReportWindow(SurfaceMaterialReport report, String renderedHtml) {
        WebView reportView = new WebView();
        reportView.getEngine().loadContent(renderedHtml);
        VBox.setVgrow(reportView, Priority.ALWAYS);
        Button exportButton = new Button("Markdown exportieren");
        exportButton.setOnAction(event -> exportSurfaceMaterialReportMarkdown(report));
        owner.applyTooltip(exportButton, "Exportiert genau diese Materialliste als Markdown-Datei.");
        Button exportPdfButton = new Button("PDF exportieren");
        exportPdfButton.setOnAction(event -> exportSurfaceMaterialReportPdf(report));
        owner.applyTooltip(exportPdfButton, "Exportiert diese Materialliste als PDF-Datei mit Raster-Heizplänen und zusätzlichen 2D-Etagenbildern aus der Workbench.");
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
        fileChooser.setTitle("Gerasterte Bauzeichnung als PDF speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Dateien", "*.pdf"));
        String projectName = owner.exchangeFileNameService.stripRepeatedExtension(Path.of(owner.project.name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(projectName + "_Bauzeichnung_Raster.pdf");
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
            runWithProgressDialog(
                    "Raster-Bauzeichnung wird exportiert",
                    "Bauplan wird vorbereitet",
                    "PDF-Export fehlgeschlagen",
                    progress -> {
                        ConstructionDrawingPdfService.ExportAssets exportAssets = createConstructionDrawingExportAssets(
                                progress.range(0.0, 0.35)
                        );
                        owner.constructionDrawingPdfService.export(
                                owner.project,
                                target,
                                options,
                                exportAssets,
                                progress.range(0.35, 1.0)::update
                        );
                        return target;
                    },
                    exportPath -> owner.draftLabel.setText("Bauzeichnungs-PDF exportiert: " + exportPath.getFileName())
            );
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
            SurfaceMaterialReportPdfService.ExportAssets exportAssets = createExportAssets(
                    report,
                    (progress, message) -> {
                    }
            );
            Path imageDirectory = exportPath.resolveSibling(markdownImageDirectoryName(exportPath));
            Files.createDirectories(imageDirectory);
            Files.writeString(exportPath, rasterMarkdown(report, exportAssets, imageDirectory));
            owner.draftLabel.setText("Materialliste exportiert: " + exportPath.getFileName());
        } catch (Exception exception) {
            owner.showOperationException("Materiallisten-Export fehlgeschlagen", exception);
        }
    }

    private String rasterMarkdown(
            SurfaceMaterialReport report,
            SurfaceMaterialReportPdfService.ExportAssets exportAssets,
            Path imageDirectory
    ) throws Exception {
        StringBuilder markdown = new StringBuilder(report.toDisplayMarkdown());
        String imageDirectoryName = imageDirectory.getFileName().toString();
        markdown.append("\n## Rastergrafiken\n\n");
        appendImageLinks(markdown, "Etagenübersichten", exportAssets.levelPlanImages(), imageDirectory, imageDirectoryName, "Etage");
        appendMaterialLevelImageLinks(markdown, report, exportAssets, imageDirectory, imageDirectoryName);
        appendImageLinks(markdown, "Heizflächen", exportAssets.heatingLevelImages(), imageDirectory, imageDirectoryName, "Heizfläche");
        return markdown.toString();
    }

    private void appendMaterialLevelImageLinks(
            StringBuilder markdown,
            SurfaceMaterialReport report,
            SurfaceMaterialReportPdfService.ExportAssets exportAssets,
            Path imageDirectory,
            String imageDirectoryName
    ) throws Exception {
        Map<String, BufferedImage> images = new LinkedHashMap<>();
        for (SurfaceMaterialListService.MaterialSummary material : report.materials()) {
            for (String levelName : exportAssets.levelPlanImages().keySet()) {
                BufferedImage image = exportAssets.materialLevelImages().get(
                        SurfaceMaterialReportPdfService.materialLevelImageKey(material, levelName)
                );
                if (image != null) {
                    images.put(levelName + " / " + material.name(), image);
                }
            }
        }
        appendImageLinks(markdown, "Beläge", images, imageDirectory, imageDirectoryName, "Belag");
    }

    private void appendImageLinks(
            StringBuilder markdown,
            String title,
            Map<String, BufferedImage> images,
            Path imageDirectory,
            String imageDirectoryName,
            String filePrefix
    ) throws Exception {
        if (images.isEmpty()) {
            return;
        }
        markdown.append("### ").append(title).append("\n\n");
        int index = 1;
        for (Map.Entry<String, BufferedImage> entry : images.entrySet()) {
            String label = filePrefix + " " + entry.getKey();
            String fileName = safeFileName(filePrefix + "_" + index + "_" + entry.getKey()) + ".png";
            Path imagePath = imageDirectory.resolve(fileName);
            javax.imageio.ImageIO.write(entry.getValue(), "png", imagePath.toFile());
            markdown.append("![").append(label).append("](")
                    .append(imageDirectoryName).append("/")
                    .append(fileName)
                    .append(")\n\n");
            index++;
        }
    }

    private String markdownImageDirectoryName(Path exportPath) {
        String fileName = exportPath.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex < 0 ? fileName : fileName.substring(0, extensionIndex);
        return safeFileName(baseName) + "_Rasterbilder";
    }

    private String safeFileName(String rawName) {
        String safeName = rawName.replaceAll("[^\\p{L}\\p{N}._-]+", "_");
        return safeName.isBlank() ? "bild" : safeName;
    }

    void exportSurfaceMaterialReportPdf() {
        exportSurfaceMaterialReportPdf((SurfaceMaterialReport) null);
    }

    void exportSurfaceMaterialReportPdf(SurfaceMaterialReport report) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Materialliste als PDF mit Rastergrafiken speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Dateien", "*.pdf"));
        String projectName = owner.exchangeFileNameService.stripRepeatedExtension(Path.of(owner.project.name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(projectName + "_Räume_und_Material_Raster.pdf");
        java.io.File file = fileChooser.showSaveDialog(owner.currentWindow());
        if (file == null) {
            return;
        }
        exportSurfaceMaterialReportPdf(report, file.toPath());
    }

    void exportSurfaceMaterialReportPdf(Path targetFile) {
        exportSurfaceMaterialReportPdf(null, targetFile);
    }

    void exportSurfaceMaterialReportPdf(SurfaceMaterialReport report, Path targetFile) {
        try {
            Path exportPath = owner.exchangeFileNameService.ensureSingleExtension(targetFile, ".pdf");
            runWithProgressDialog(
                    "Materialliste mit Rastergrafiken wird exportiert",
                    "Materialliste wird vorbereitet",
                    "Materiallisten-PDF-Export fehlgeschlagen",
                    progress -> {
                        SurfaceMaterialReport effectiveReport = report;
                        double assetStart = 0.0;
                        if (effectiveReport == null) {
                            progress.update(0.08, "Materialdaten werden gesammelt");
                            effectiveReport = owner.surfaceMaterialListService.create(owner.project);
                            assetStart = 0.18;
                        }
                        SurfaceMaterialReportPdfService.ExportAssets exportAssets = createExportAssets(
                                effectiveReport,
                                progress.range(assetStart, 0.85)
                        );
                        progress.update(0.9, "Materiallisten-PDF wird geschrieben");
                        surfaceMaterialReportPdfService.export(effectiveReport, exportPath, exportAssets);
                        progress.update(1.0, "Materialliste abgeschlossen");
                        return exportPath;
                    },
                    path -> owner.draftLabel.setText("Materialliste als PDF exportiert: " + path.getFileName())
            );
        } catch (Exception exception) {
            owner.showOperationException("Materiallisten-PDF-Export fehlgeschlagen", exception);
        }
    }

    private SurfaceMaterialReportPdfService.ExportAssets createExportAssets(
            SurfaceMaterialReport report,
            ProgressCallback progress
    ) throws Exception {
        List<MaterialLevelCapture> materialLevelCaptures = report.materials().stream()
                .filter(material -> material.surfaceType() == SurfaceType.FLOOR)
                .flatMap(material -> material.roomEntries().stream()
                        .map(SurfaceMaterialListService.MaterialRoomEntry::levelName)
                        .distinct()
                        .map(levelName -> new MaterialLevelCapture(material, levelName)))
                .toList();
        List<HeatingPlanCapture> heatingLevelCaptures = materialHeatingLevelCaptures(report);
        int totalSteps = Math.max(1, owner.project.levels().size() + materialLevelCaptures.size() + heatingLevelCaptures.size());
        int completedSteps = 0;
        Map<String, BufferedImage> levelPlanImages = new LinkedHashMap<>();
        for (Level level : owner.project.levels()) {
            progress.update(completedSteps / (double) totalSteps, "2D-Ansicht " + level.name() + " wird erstellt");
            levelPlanImages.put(level.name(), captureMaterialOverviewImage(level.name()));
            completedSteps++;
        }
        Map<String, BufferedImage> materialLevelImages = new LinkedHashMap<>();
        Map<String, BufferedImage> heatingLevelImages = new LinkedHashMap<>();
        for (MaterialLevelCapture capture : materialLevelCaptures) {
            progress.update(completedSteps / (double) totalSteps, "Belag " + capture.material().name() + " – " + capture.levelName() + " wird gerastert");
            BufferedImage image = captureMaterialLevelImage(capture.material(), capture.levelName());
            if (image != null) {
                materialLevelImages.put(
                        SurfaceMaterialReportPdfService.materialLevelImageKey(capture.material(), capture.levelName()),
                        image
                );
            }
            completedSteps++;
        }
        for (HeatingPlanCapture capture : heatingLevelCaptures) {
            progress.update(completedSteps / (double) totalSteps, "Heizkreise " + capture.surfacePosition() + " – " + capture.levelName() + " werden gerastert");
            heatingLevelImages.put(
                    SurfaceMaterialReportPdfService.heatingLevelImageKey(capture.levelName(), capture.surfacePosition().toString()),
                    captureHeatingLevelImage(capture)
            );
            completedSteps++;
        }
        progress.update(1.0, "Materialgrafiken abgeschlossen");
        return new SurfaceMaterialReportPdfService.ExportAssets(
                levelPlanImages,
                materialLevelImages,
                heatingLevelImages
        );
    }

    private BufferedImage captureMaterialOverviewImage(String levelName) throws Exception {
        return runOnFxThread(() -> SwingFXUtils.fromFXImage(owner.reportMaterialOverviewSnapshot(levelName), null));
    }

    private BufferedImage captureLevelPlanImage(String levelName) throws Exception {
        return runOnFxThread(() -> SwingFXUtils.fromFXImage(owner.reportLevelSnapshot(levelName), null));
    }

    private List<HeatingPlanCapture> materialHeatingLevelCaptures(SurfaceMaterialReport report) {
        LinkedHashMap<String, HeatingPlanCapture> captures = new LinkedHashMap<>();
        for (SurfaceMaterialListService.HeatingPlanSummary plan : report.heatingPlans()) {
            Optional<HeatingSurfacePosition> surfacePosition = heatingSurfacePosition(plan.surfacePosition());
            if (surfacePosition.isEmpty()) {
                continue;
            }
            Level level = owner.project.levels().stream()
                    .filter(candidate -> candidate.name().equals(plan.levelName()))
                    .findFirst()
                    .orElse(null);
            if (level == null) {
                continue;
            }
            HeatingSurfacePosition position = surfacePosition.orElseThrow();
            Set<java.util.UUID> visibleLayerIds = variothermLayerIds(level, surfaceType(position));
            Set<RoomObjectHeatingType> visibleObjectTypes = heatingObjectTypes(position);
            boolean hasHydronicHeating = level.hydronicHeatings().stream().anyMatch(heating -> heating.surfacePosition() == position);
            if (visibleLayerIds.isEmpty() && !hasHydronicHeating && !hasHeatingObjects(level, visibleObjectTypes)) {
                continue;
            }
            captures.putIfAbsent(
                    constructionDrawingHeatingImageKey(level.name(), position),
                    new HeatingPlanCapture(level.name(), position, visibleLayerIds, visibleObjectTypes)
            );
        }
        return List.copyOf(captures.values());
    }

    private BufferedImage captureFilteredLevelPlanImage(
            String levelName,
            Set<java.util.UUID> visibleLayerIds,
            boolean includeHydronicHeating
    ) throws Exception {
        return captureFilteredLevelPlanImage(levelName, visibleLayerIds, includeHydronicHeating, Set.of());
    }

    private BufferedImage captureFilteredLevelPlanImage(
            String levelName,
            Set<java.util.UUID> visibleLayerIds,
            boolean includeHydronicHeating,
            Set<RoomObjectHeatingType> visibleHeatingObjectTypes
    ) throws Exception {
        return captureFilteredLevelPlanImage(levelName, visibleLayerIds, includeHydronicHeating, visibleHeatingObjectTypes, Set.of());
    }

    private BufferedImage captureFilteredLevelPlanImage(
            String levelName,
            Set<java.util.UUID> visibleLayerIds,
            boolean includeHydronicHeating,
            Set<RoomObjectHeatingType> visibleHeatingObjectTypes,
            Set<HeatingSurfacePosition> visibleHydronicSurfacePositions
    ) throws Exception {
        return runOnFxThread(() -> SwingFXUtils.fromFXImage(
                owner.reportLevelSnapshot(levelName, visibleLayerIds, includeHydronicHeating, visibleHeatingObjectTypes, visibleHydronicSurfacePositions),
                null
        ));
    }

    private BufferedImage captureHeatingLevelImage(HeatingPlanCapture capture) throws Exception {
        return captureFilteredLevelPlanImage(
                capture.levelName(),
                capture.visibleLayerIds(),
                true,
                capture.visibleHeatingObjectTypes(),
                Set.of(capture.surfacePosition())
        );
    }

    private BufferedImage captureMaterialLevelImage(
            SurfaceMaterialListService.MaterialSummary material,
            String levelName
    ) throws Exception {
        Level level = owner.project.levels().stream()
                .filter(candidate -> candidate.name().equals(levelName))
                .findFirst()
                .orElse(null);
        if (level == null) {
            return null;
        }
        Set<java.util.UUID> visibleLayerIds = material.roomEntries().stream()
                .filter(entry -> entry.levelName().equals(levelName))
                .map(SurfaceMaterialListService.MaterialRoomEntry::roomName)
                .map(roomName -> level.rooms().stream()
                        .filter(room -> room.name().equals(roomName))
                        .findFirst()
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .flatMap(room -> materialLayerIds(level, room, material).stream())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (visibleLayerIds.isEmpty()) {
            return null;
        }
        return captureFilteredLevelPlanImage(level.name(), visibleLayerIds, false);
    }

    private ConstructionDrawingPdfService.ExportAssets createConstructionDrawingExportAssets(ProgressCallback progress) throws Exception {
        List<HeatingPlanCapture> heatingCaptures = new ArrayList<>();
        for (Level level : owner.project.levels()) {
            for (HeatingSurfacePosition surfacePosition : HeatingSurfacePosition.values()) {
                Set<RoomObjectHeatingType> visibleObjectTypes = heatingObjectTypes(surfacePosition);
                boolean hasHydronicHeating = level.hydronicHeatings().stream().anyMatch(heating -> heating.surfacePosition() == surfacePosition);
                if (!hasHydronicHeating && !hasHeatingObjects(level, visibleObjectTypes)) {
                    continue;
                }
                Set<java.util.UUID> visibleLayerIds = variothermLayerIds(level, surfaceType(surfacePosition));
                heatingCaptures.add(new HeatingPlanCapture(level.name(), surfacePosition, visibleLayerIds, visibleObjectTypes));
            }
        }
        int totalSteps = Math.max(1, owner.project.levels().size() + heatingCaptures.size());
        int completedSteps = 0;
        Map<String, BufferedImage> levelPlanImages = new LinkedHashMap<>();
        for (Level level : owner.project.levels()) {
            progress.update(completedSteps / (double) totalSteps, "Bauplan " + level.name() + " wird gerastert");
            levelPlanImages.put(level.name(), captureLevelPlanImage(level.name()));
            completedSteps++;
        }
        Map<String, BufferedImage> heatingPlanImages = new LinkedHashMap<>();
        for (HeatingPlanCapture capture : heatingCaptures) {
            progress.update(completedSteps / (double) totalSteps, "Heizflächen " + capture.surfacePosition() + " – " + capture.levelName() + " werden gerastert");
            heatingPlanImages.put(
                    constructionDrawingHeatingImageKey(capture.levelName(), capture.surfacePosition()),
                    captureFilteredLevelPlanImage(
                            capture.levelName(),
                            capture.visibleLayerIds(),
                            true,
                            capture.visibleHeatingObjectTypes(),
                            Set.of(capture.surfacePosition())
                    )
            );
            completedSteps++;
        }
        progress.update(1.0, "Rastergrafiken abgeschlossen");
        return new ConstructionDrawingPdfService.ExportAssets(
                levelPlanImages,
                heatingPlanImages
        );
    }

    private Set<java.util.UUID> variothermLayerIds(Level level, SurfaceType surfaceType) {
        return level.surfaceLayerStacks().stream()
                .filter(stack -> stack.surfaceType() == surfaceType)
                .flatMap(stack -> stack.layers().stream())
                .filter(SurfaceCoveringPresetService::isVariothermDryPanelLayer)
                .map(de.schrell.cadas.domain.model.SurfaceLayer::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Optional<HeatingSurfacePosition> heatingSurfacePosition(String label) {
        if (HeatingSurfacePosition.FLOOR.toString().equals(label)) {
            return Optional.of(HeatingSurfacePosition.FLOOR);
        }
        if (HeatingSurfacePosition.CEILING.toString().equals(label)) {
            return Optional.of(HeatingSurfacePosition.CEILING);
        }
        return Optional.empty();
    }

    private SurfaceType surfaceType(HeatingSurfacePosition surfacePosition) {
        return surfacePosition == HeatingSurfacePosition.CEILING
                ? SurfaceType.CEILING
                : SurfaceType.FLOOR;
    }

    private Set<RoomObjectHeatingType> heatingObjectTypes(HeatingSurfacePosition surfacePosition) {
        return surfacePosition == HeatingSurfacePosition.CEILING
                ? Set.of(RoomObjectHeatingType.CEILING_HEATING)
                : Set.of(RoomObjectHeatingType.FLOOR_HEATING);
    }

    private boolean hasHeatingObjects(Level level, Set<RoomObjectHeatingType> heatingTypes) {
        return level.roomObjects().stream()
                .anyMatch(roomObject -> roomObject.visible()
                        && roomObject.heatOutputWatts() > 0.0
                        && heatingTypes.contains(roomObject.heatingType()));
    }

    private Set<java.util.UUID> materialLayerIds(Level level, Room room, SurfaceMaterialListService.MaterialSummary material) {
        var stack = level.findSurfaceLayerStack(material.surfaceType(), room.id().toString());
        if (stack == null) {
            return Set.of();
        }
        return stack.layers().stream()
                .filter(layer -> SurfaceMaterialListService.materialLookupKey(material.surfaceType(), layer).equals(material.lookupKey()))
                .map(de.schrell.cadas.domain.model.SurfaceLayer::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String constructionDrawingHeatingImageKey(String levelName, HeatingSurfacePosition surfacePosition) {
        return levelName + "\u0000" + surfacePosition.name();
    }

    private <T> void runWithProgressDialog(
            String title,
            String initialMessage,
            String errorTitle,
            BackgroundOperation<T> operation,
            java.util.function.Consumer<T> onSuccess
    ) {
        if (!owner.interactiveDialogsEnabled) {
            try {
                T result = operation.run((progress, message) -> {
                });
                onSuccess.accept(result);
            } catch (Exception exception) {
                owner.showOperationException(errorTitle, exception);
            }
            return;
        }
        ProgressBar progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(320.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        Label messageLabel = new Label(initialMessage);
        messageLabel.setWrapText(true);
        VBox container = new VBox(12.0, messageLabel, progressBar);
        container.setPadding(new Insets(16));
        Stage dialog = new Stage();
        dialog.setTitle(title);
        dialog.initModality(Modality.APPLICATION_MODAL);
        Window ownerWindow = owner.currentWindow();
        if (ownerWindow != null) {
            dialog.initOwner(ownerWindow);
        }
        dialog.setOnCloseRequest(event -> event.consume());
        dialog.setScene(new Scene(container, 380, 120));
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.run((progress, message) -> {
                    updateProgress(Math.max(0.0, Math.min(1.0, progress)), 1.0);
                    updateMessage(message);
                });
            }
        };
        task.progressProperty().addListener((ignored, oldValue, newValue) ->
                progressBar.setProgress(Math.max(0.0, newValue == null ? 0.0 : newValue.doubleValue())));
        task.messageProperty().addListener((ignored, oldValue, newValue) ->
                messageLabel.setText(newValue == null || newValue.isBlank() ? initialMessage : newValue));
        task.setOnSucceeded(event -> {
            dialog.close();
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            dialog.close();
            owner.showOperationException(errorTitle, task.getException());
        });
        Thread worker = new Thread(task, "CADas-Export");
        worker.setDaemon(true);
        worker.start();
        dialog.showAndWait();
    }

    private <T> T runOnFxThread(FxCallable<T> callable) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return callable.call();
        }
        FutureTask<T> task = new FutureTask<>(callable::call);
        Platform.runLater(task);
        try {
            return task.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception wrappedException) {
                throw wrappedException;
            }
            throw new IllegalStateException("JavaFX-Aufgabe ist fehlgeschlagen.", cause);
        }
    }

    private record MaterialReportWindowContent(SurfaceMaterialReport report, String renderedHtml) {
    }

    private record HeatingLoadField(Level level, Room room, TextField field) {
    }

    private record MaterialLevelCapture(
            SurfaceMaterialListService.MaterialSummary material,
            String levelName
    ) {
    }

    private record HeatingPlanCapture(
            String levelName,
            HeatingSurfacePosition surfacePosition,
            Set<java.util.UUID> visibleLayerIds,
            Set<RoomObjectHeatingType> visibleHeatingObjectTypes
    ) {
    }

    @FunctionalInterface
    private interface BackgroundOperation<T> {
        T run(ProgressCallback progress) throws Exception;
    }

    @FunctionalInterface
    private interface FxCallable<T> {
        T call() throws Exception;
    }

    @FunctionalInterface
    private interface ProgressCallback {
        void update(double progress, String message);

        default ProgressCallback range(double start, double end) {
            return (progress, message) -> {
                double bounded = Math.max(0.0, Math.min(1.0, progress));
                update(start + (end - start) * bounded, message);
            };
        }
    }
}
