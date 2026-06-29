package de.schrell.cadas.ui;

import de.schrell.cadas.application.help.AboutInformation;
import de.schrell.cadas.application.help.MarkdownNavigationService.HelpSection;
import de.schrell.cadas.application.reports.ConstructionDrawingOptions;
import de.schrell.cadas.application.reports.ConstructionDrawingPdfService;
import de.schrell.cadas.application.reports.SurfaceMaterialListService;
import de.schrell.cadas.application.reports.SurfaceMaterialListService.SurfaceMaterialReport;
import de.schrell.cadas.application.reports.SurfaceMaterialReportPdfService;
import de.schrell.cadas.application.layers.SurfaceCoveringPresetService;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
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
import javafx.scene.control.TextField;
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

    private final CadWorkbench owner;
    private final SurfaceMaterialReportPdfService surfaceMaterialReportPdfService = new SurfaceMaterialReportPdfService();

    CadWorkbenchDocumentSupport(CadWorkbench owner) {
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

    private void showSurfaceMaterialReportWindow(SurfaceMaterialReport report, String renderedHtml) {
        WebView reportView = new WebView();
        reportView.getEngine().loadContent(renderedHtml);
        VBox.setVgrow(reportView, Priority.ALWAYS);
        Button exportButton = new Button("Markdown exportieren");
        exportButton.setOnAction(event -> exportSurfaceMaterialReportMarkdown(report));
        owner.applyTooltip(exportButton, "Exportiert genau diese Materialliste als Markdown-Datei.");
        Button exportPdfSvgButton = new Button("PDF exportieren (SVG)");
        exportPdfSvgButton.setOnAction(event -> exportSurfaceMaterialReportPdf(report));
        owner.applyTooltip(exportPdfSvgButton, "Exportiert diese Materialliste als PDF-Datei mit SVG-Heizplänen und zusätzlichen 2D-Etagenbildern aus der Workbench.");
        Button exportPdfRasterButton = new Button("PDF exportieren (Raster)");
        exportPdfRasterButton.setOnAction(event -> exportSurfaceMaterialReportPdfRaster(report));
        owner.applyTooltip(exportPdfRasterButton, "Exportiert diese Materialliste als PDF-Datei mit Raster-Heizplänen und zusätzlichen 2D-Etagenbildern aus der Workbench.");
        Button printButton = new Button("Drucken");
        printButton.setOnAction(event -> printSurfaceMaterialReport(reportView));
        owner.applyTooltip(printButton, "Druckt die gerenderte Materialliste so, wie sie in diesem Fenster angezeigt wird.");
        HBox actions = new HBox(8.0, printButton, exportPdfSvgButton, exportPdfRasterButton, exportButton);
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
        exportConstructionDrawingPdf(false);
    }

    void exportConstructionDrawingPdfRaster() {
        exportConstructionDrawingPdf(true);
    }

    private void exportConstructionDrawingPdf(boolean raster) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(raster
                ? "Gerasterte Bauzeichnung als PDF speichern"
                : "Maßstabgerechte Bauzeichnung als PDF speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Dateien", "*.pdf"));
        String projectName = owner.exchangeFileNameService.stripRepeatedExtension(Path.of(owner.project.name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(raster
                ? projectName + "_Bauzeichnung_Raster.pdf"
                : projectName + "_Bauzeichnung.pdf");
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
                    raster ? "Raster-Bauzeichnung wird exportiert" : "Bauzeichnung wird exportiert",
                    "Bauplan wird vorbereitet",
                    "PDF-Export fehlgeschlagen",
                    progress -> {
                        ConstructionDrawingPdfService.ExportAssets exportAssets = raster
                                ? createConstructionDrawingExportAssets(progress.range(0.0, 0.35))
                                : ConstructionDrawingPdfService.ExportAssets.empty();
                        owner.constructionDrawingPdfService.export(
                                owner.project,
                                target,
                                options,
                                exportAssets,
                                progress.range(raster ? 0.35 : 0.0, 1.0)::update
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
            Files.writeString(exportPath, report.toMarkdown());
            owner.draftLabel.setText("Materialliste exportiert: " + exportPath.getFileName());
        } catch (Exception exception) {
            owner.showOperationException("Materiallisten-Export fehlgeschlagen", exception);
        }
    }

    void exportSurfaceMaterialReportPdf() {
        exportSurfaceMaterialReportPdf((SurfaceMaterialReport) null);
    }

    void exportSurfaceMaterialReportPdf(SurfaceMaterialReport report) {
        exportSurfaceMaterialReportPdf(report, SurfaceMaterialReportPdfService.HeatingPlanGraphicVariant.SVG);
    }

    void exportSurfaceMaterialReportPdfRaster() {
        exportSurfaceMaterialReportPdf(null, SurfaceMaterialReportPdfService.HeatingPlanGraphicVariant.RASTERGRAFIK);
    }

    void exportSurfaceMaterialReportPdfRaster(SurfaceMaterialReport report) {
        exportSurfaceMaterialReportPdf(report, SurfaceMaterialReportPdfService.HeatingPlanGraphicVariant.RASTERGRAFIK);
    }

    void exportSurfaceMaterialReportPdf(SurfaceMaterialReport report, SurfaceMaterialReportPdfService.HeatingPlanGraphicVariant variant) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(variant == SurfaceMaterialReportPdfService.HeatingPlanGraphicVariant.RASTERGRAFIK
                ? "Materialliste als PDF mit Rastergrafiken speichern"
                : "Materialliste als PDF speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Dateien", "*.pdf"));
        String projectName = owner.exchangeFileNameService.stripRepeatedExtension(Path.of(owner.project.name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(variant == SurfaceMaterialReportPdfService.HeatingPlanGraphicVariant.RASTERGRAFIK
                ? projectName + "_Räume_und_Material_Raster.pdf"
                : projectName + "_Räume_und_Material.pdf");
        java.io.File file = fileChooser.showSaveDialog(owner.currentWindow());
        if (file == null) {
            return;
        }
        exportSurfaceMaterialReportPdf(report, file.toPath(), variant);
    }

    void exportSurfaceMaterialReportPdf(Path targetFile) {
        exportSurfaceMaterialReportPdf(null, targetFile);
    }

    void exportSurfaceMaterialReportPdf(SurfaceMaterialReport report, Path targetFile) {
        exportSurfaceMaterialReportPdf(report, targetFile, SurfaceMaterialReportPdfService.HeatingPlanGraphicVariant.SVG);
    }

    void exportSurfaceMaterialReportPdf(SurfaceMaterialReport report, Path targetFile, SurfaceMaterialReportPdfService.HeatingPlanGraphicVariant variant) {
        try {
            Path exportPath = owner.exchangeFileNameService.ensureSingleExtension(targetFile, ".pdf");
            runWithProgressDialog(
                    variant == SurfaceMaterialReportPdfService.HeatingPlanGraphicVariant.RASTERGRAFIK
                            ? "Materialliste mit Rastergrafiken wird exportiert"
                            : "Materialliste wird exportiert",
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
                                variant,
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
            SurfaceMaterialReportPdfService.HeatingPlanGraphicVariant variant,
            ProgressCallback progress
    ) throws Exception {
        List<MaterialRoomCapture> materialCaptures = report.materials().stream()
                .filter(material -> material.surfaceType() == SurfaceType.FLOOR)
                .flatMap(material -> material.roomEntries().stream()
                        .map(entry -> new MaterialRoomCapture(material, entry)))
                .toList();
        List<SurfaceMaterialListService.HeatingPlanSummary> heatingPlans = variant == SurfaceMaterialReportPdfService.HeatingPlanGraphicVariant.RASTERGRAFIK
                ? report.heatingPlans()
                : List.of();
        int totalSteps = Math.max(1, owner.project.levels().size() + materialCaptures.size() + heatingPlans.size());
        int completedSteps = 0;
        Map<String, BufferedImage> levelPlanImages = new LinkedHashMap<>();
        for (Level level : owner.project.levels()) {
            progress.update(completedSteps / (double) totalSteps, "2D-Ansicht " + level.name() + " wird erstellt");
            levelPlanImages.put(level.name(), captureLevelPlanImage(level.name()));
            completedSteps++;
        }
        Map<String, BufferedImage> heatingPlanImages = new LinkedHashMap<>();
        Map<String, BufferedImage> materialRoomImages = new LinkedHashMap<>();
        for (MaterialRoomCapture capture : materialCaptures) {
            progress.update(completedSteps / (double) totalSteps, "Raumgrafik " + capture.entry().roomName() + " wird erstellt");
            BufferedImage image = captureMaterialRoomImage(capture.material(), capture.entry());
            if (image != null) {
                materialRoomImages.put(
                        SurfaceMaterialReportPdfService.materialRoomImageKey(capture.material(), capture.entry()),
                        image
                );
            }
            completedSteps++;
        }
        if (variant == SurfaceMaterialReportPdfService.HeatingPlanGraphicVariant.RASTERGRAFIK) {
            for (SurfaceMaterialListService.HeatingPlanSummary plan : heatingPlans) {
                progress.update(completedSteps / (double) totalSteps, "Heizplan " + plan.roomName() + " wird erstellt");
                String imageKey = heatingPlanImageKey(plan);
                if (!heatingPlanImages.containsKey(imageKey)) {
                    heatingPlanImages.put(imageKey, captureHeatingPlanImage(plan));
                }
                completedSteps++;
            }
        }
        progress.update(1.0, "Materialgrafiken abgeschlossen");
        return new SurfaceMaterialReportPdfService.ExportAssets(variant, levelPlanImages, heatingPlanImages, materialRoomImages);
    }

    private BufferedImage captureLevelPlanImage(String levelName) throws Exception {
        return runOnFxThread(() -> SwingFXUtils.fromFXImage(owner.reportLevelSnapshot(levelName), null));
    }

    private BufferedImage captureFilteredLevelPlanImage(
            String levelName,
            Set<java.util.UUID> visibleLayerIds,
            boolean includeHydronicHeating
    ) throws Exception {
        return runOnFxThread(() -> SwingFXUtils.fromFXImage(
                owner.reportLevelSnapshot(levelName, visibleLayerIds, includeHydronicHeating),
                null
        ));
    }

    private BufferedImage captureHeatingPlanImage(SurfaceMaterialListService.HeatingPlanSummary plan) throws Exception {
        String levelName = plan.levelName();
        String roomName = plan.roomName();
        Level level = owner.project.levels().stream()
                .filter(candidate -> candidate.name().equals(levelName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Etage `" + levelName + "` ist unbekannt."));
        Room room = level.rooms().stream()
                .filter(candidate -> candidate.name().equals(roomName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Raum `" + roomName + "` ist unbekannt."));
        Set<java.util.UUID> visibleLayerIds = variothermLayerIds(level, room, plan.surfacePosition());
        return runOnFxThread(() -> SwingFXUtils.fromFXImage(
                owner.reportRoomSnapshot(level.name(), room.outline(), visibleLayerIds, !plan.objectBased()),
                null
        ));
    }

    private BufferedImage captureMaterialRoomImage(
            SurfaceMaterialListService.MaterialSummary material,
            SurfaceMaterialListService.MaterialRoomEntry entry
    ) throws Exception {
        Level level = owner.project.levels().stream()
                .filter(candidate -> candidate.name().equals(entry.levelName()))
                .findFirst()
                .orElse(null);
        if (level == null) {
            return null;
        }
        Room room = level.rooms().stream()
                .filter(candidate -> candidate.name().equals(entry.roomName()))
                .findFirst()
                .orElse(null);
        if (room == null) {
            return null;
        }
        Set<java.util.UUID> visibleLayerIds = materialLayerIds(level, room, material);
        if (visibleLayerIds.isEmpty()) {
            return null;
        }
        return runOnFxThread(() -> SwingFXUtils.fromFXImage(
                owner.reportRoomSnapshot(level.name(), room.outline(), visibleLayerIds, false),
                null
        ));
    }

    private ConstructionDrawingPdfService.ExportAssets createConstructionDrawingExportAssets(ProgressCallback progress) throws Exception {
        List<HeatingPlanCapture> heatingCaptures = new ArrayList<>();
        for (Level level : owner.project.levels()) {
            for (var surfacePosition : de.schrell.cadas.domain.model.HeatingSurfacePosition.values()) {
                if (level.hydronicHeatings().stream().noneMatch(heating -> heating.surfacePosition() == surfacePosition)) {
                    continue;
                }
                Set<java.util.UUID> visibleLayerIds = variothermLayerIds(level, surfacePosition == de.schrell.cadas.domain.model.HeatingSurfacePosition.CEILING
                        ? SurfaceType.CEILING
                        : SurfaceType.FLOOR);
                if (!visibleLayerIds.isEmpty()) {
                    heatingCaptures.add(new HeatingPlanCapture(level.name(), surfacePosition, visibleLayerIds));
                }
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
                    captureFilteredLevelPlanImage(capture.levelName(), capture.visibleLayerIds(), false)
            );
            completedSteps++;
        }
        progress.update(1.0, "Rastergrafiken abgeschlossen");
        return new ConstructionDrawingPdfService.ExportAssets(
                ConstructionDrawingPdfService.GraphicVariant.RASTERGRAFIK,
                levelPlanImages,
                heatingPlanImages
        );
    }

    private Set<java.util.UUID> variothermLayerIds(Level level, Room room, String surfacePosition) {
        SurfaceType surfaceType = "Decke".equals(surfacePosition) ? SurfaceType.CEILING : SurfaceType.FLOOR;
        var stack = level.findSurfaceLayerStack(surfaceType, room.id().toString());
        if (stack == null) {
            return Set.of();
        }
        return stack.layers().stream()
                .filter(layer -> SurfaceCoveringPresetService.VARIOTHERM_DRY_PANEL_SOURCE.equals(layer.coveringSource()))
                .map(de.schrell.cadas.domain.model.SurfaceLayer::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<java.util.UUID> variothermLayerIds(Level level, SurfaceType surfaceType) {
        return level.surfaceLayerStacks().stream()
                .filter(stack -> stack.surfaceType() == surfaceType)
                .flatMap(stack -> stack.layers().stream())
                .filter(layer -> SurfaceCoveringPresetService.VARIOTHERM_DRY_PANEL_SOURCE.equals(layer.coveringSource()))
                .map(de.schrell.cadas.domain.model.SurfaceLayer::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
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

    private String heatingPlanImageKey(SurfaceMaterialListService.HeatingPlanSummary plan) {
        return plan.levelName() + "\u0000" + plan.roomName() + "\u0000" + plan.surfacePosition();
    }

    private String constructionDrawingHeatingImageKey(String levelName, de.schrell.cadas.domain.model.HeatingSurfacePosition surfacePosition) {
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

    private record MaterialRoomCapture(
            SurfaceMaterialListService.MaterialSummary material,
            SurfaceMaterialListService.MaterialRoomEntry entry
    ) {
    }

    private record HeatingPlanCapture(
            String levelName,
            de.schrell.cadas.domain.model.HeatingSurfacePosition surfacePosition,
            Set<java.util.UUID> visibleLayerIds
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
