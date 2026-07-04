package de.schrell.cadas.ui;

import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.normalizedSurfaceLayoutAnchor;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.surfaceLayoutSelectionDirection;
import static de.schrell.cadas.ui.CadWorkbenchCoveringSourceSupport.extractDwgBlockName;
import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
import de.schrell.cadas.application.heating.RoomHeatingOutputService;
import de.schrell.cadas.application.dwg.DwgBlockDefinition;
import de.schrell.cadas.application.dwg.DwgLibraryAnalysis;
import de.schrell.cadas.application.layers.SurfaceCoveringPreset;
import de.schrell.cadas.application.objects.RoomObjectPreset;
import de.schrell.cadas.application.parts.DoorPreset;
import de.schrell.cadas.application.parts.StairPreset;
import de.schrell.cadas.application.parts.WindowPreset;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingRoutingLanguage;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObjectHeatingType;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.stage.Window;

abstract class CadWorkbenchSurfaceAndHeating extends CadWorkbenchProject {

    void updateCadLibrarySummary() {
        if (cadLibraryReferences.isEmpty()) {
            cadLibrarySummaryLabel.setText("Keine externen CAD-Bibliotheken registriert.");
            return;
        }
        cadLibrarySummaryLabel.setText(cadLibraryReferences.stream()
                .map(this::cadLibrarySummaryLine)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("Keine externen CAD-Bibliotheken registriert."));
    }

    String cadLibrarySummaryLine(Path path) {
        DwgLibraryAnalysis analysis = dwgAnalysesByPath.get(path.toAbsolutePath().normalize());
        if (analysis == null) {
            return "• " + path.getFileName();
        }
        if (!analysis.successful()) {
            return "• " + path.getFileName() + " | nicht lesbar: " + analysis.summary();
        }
        long usableBlocks = analysis.blocks().stream()
                .filter(DwgBlockDefinition::hasGeometry)
                .count();
        return "• " + path.getFileName() + " | " + usableBlocks + " Blöcke";
    }

    void registerDwgLibrary(Path sourceFile, boolean askBeforeOverwrite) {
        Path registeredFile = configuredCadLibraryPath(sourceFile, askBeforeOverwrite);
        Path normalizedRegisteredFile = registeredFile.toAbsolutePath().normalize();
        if (!cadLibraryReferences.contains(normalizedRegisteredFile)) {
            cadLibraryReferences.add(normalizedRegisteredFile);
        }
        DwgLibraryAnalysis analysis = analyzeDwgLibrary(normalizedRegisteredFile, false);
        SurfaceCoveringPreset dwgPreset = surfaceCoveringPresetService.fromDwg(normalizedRegisteredFile);
        registerSurfacePreset(dwgPreset);
        dwgBlockCatalogService.loadCatalog(normalizedRegisteredFile).forEach(blockName -> registerDwgBlockPreset(normalizedRegisteredFile, blockName));
        applyDwgBlockFilter();
        updateCadLibrarySummary();
        if (surfacePresetSelector.getValue() == null) {
            surfacePresetSelector.setValue(dwgPreset);
        }
        draftLabel.setText("DWG-Bibliothek geladen: " + normalizedRegisteredFile.getFileName() + " | " + analysis.summary());
    }

    DwgLibraryAnalysis analyzeDwgLibrary(Path sourceFile, boolean force) {
        Path normalizedSource = sourceFile.toAbsolutePath().normalize();
        if (!force && dwgAnalysesByPath.containsKey(normalizedSource)) {
            return dwgAnalysesByPath.get(normalizedSource);
        }
        DwgLibraryAnalysis analysis = dwgLibraryAnalyzer.analyze(normalizedSource);
        dwgAnalysesByPath.put(normalizedSource, analysis);
        dwgStatusLabel.setText(normalizedSource.getFileName() + ": " + analysis.summary());
        return analysis;
    }

    Path configuredCadLibraryPath(Path sourceFile, boolean askBeforeOverwrite) {
        boolean overwrite = askBeforeOverwrite && shouldOverwriteConfiguredCadLibrary(sourceFile);
        try {
            return userSurfacePresetLibrary.copyCadLibrary(sourceFile, overwrite);
        } catch (IOException exception) {
            showOperationException("DWG-Bibliothek konnte nicht in das Belagsverzeichnis übernommen werden", exception);
            return sourceFile.toAbsolutePath().normalize();
        }
    }

    boolean shouldOverwriteConfiguredCadLibrary(Path sourceFile) {
        Path targetFile = userSurfacePresetLibrary.libraryDirectory().resolve(sourceFile.getFileName()).toAbsolutePath().normalize();
        if (!Files.exists(targetFile) || isSameFile(sourceFile, targetFile)) {
            return false;
        }
        return confirmOverwrite(
                "DWG-Bibliothek überschreiben",
                "Die DWG-Bibliothek `" + sourceFile.getFileName() + "` ist im Belagsverzeichnis bereits vorhanden.",
                "Soll die vorhandene Datei durch die neu gewählte DWG ersetzt werden?"
        );
    }

    boolean isSameFile(Path first, Path second) {
        try {
            return Files.exists(first) && Files.exists(second) && Files.isSameFile(first, second);
        } catch (IOException exception) {
            return false;
        }
    }

    void addDwgBlockPreset() {
        String blockName = dwgBlockNameField.getText() == null ? "" : dwgBlockNameField.getText().trim();
        if (blockName.isBlank()) {
            draftLabel.setText("Bitte zuerst einen DWG-Blocknamen eintragen.");
            return;
        }
        Path dwgLibrary = currentDwgLibraryPath().orElse(null);
        if (dwgLibrary == null) {
            draftLabel.setText("Bitte zuerst eine DWG-Bibliothek laden oder ein DWG-Preset auswählen.");
            return;
        }
        SurfaceCoveringPreset preset = registerDwgBlockPreset(dwgLibrary, blockName);
        surfacePresetSelector.setValue(preset);
        draftLabel.setText("DWG-Block als Oberflächen-Preset registriert: " + blockName);
    }

    SurfaceCoveringPreset registerDwgBlockPreset(Path sourceFile, String blockName) {
        SurfaceCoveringPreset preset = findAnalyzedDwgBlock(sourceFile, blockName)
                .map(surfaceCoveringPresetService::fromDwgBlock)
                .orElseGet(() -> surfaceCoveringPresetService.fromDwgBlock(sourceFile, blockName));
        registerSurfacePreset(preset);
        return availableSurfacePresets.stream()
                .filter(candidate -> candidate.coveringSource().equals(preset.coveringSource()))
                .findFirst()
                .orElse(preset);
    }

    Optional<DwgBlockDefinition> findAnalyzedDwgBlock(Path sourceFile, String blockName) {
        Path normalizedSource = sourceFile.toAbsolutePath().normalize();
        DwgLibraryAnalysis analysis = dwgAnalysesByPath.get(normalizedSource);
        if (analysis == null) {
            analysis = analyzeDwgLibrary(normalizedSource, false);
        }
        String normalizedBlockName = blockName == null ? "" : blockName.trim();
        return analysis.blocks().stream()
                .filter(block -> block.name().equalsIgnoreCase(normalizedBlockName))
                .findFirst();
    }

    void addSelectedDwgBlockAsSurfacePreset() {
        DwgBlockDefinition block = dwgBlockSelector.getValue();
        if (block == null || !block.hasGeometry()) {
            draftLabel.setText("Bitte zuerst einen DWG-Block mit auswertbarer Geometrie auswählen.");
            return;
        }
        SurfaceCoveringPreset preset = surfaceCoveringPresetService.fromDwgBlock(block);
        registerSurfacePreset(preset);
        surfacePresetSelector.setValue(preset);
        dwgBlockNameField.setText(block.name());
        applySurfacePreset(preset);
        draftLabel.setText("DWG-Block als Belag übernommen: " + block.name());
    }

    void addSelectedDwgBlockAsObjectPreset() {
        DwgBlockDefinition block = dwgBlockSelector.getValue();
        if (block == null || !block.hasGeometry()) {
            draftLabel.setText("Bitte zuerst einen DWG-Block mit auswertbarer Geometrie auswählen.");
            return;
        }
        RoomObjectMountingMode mountingMode = Optional.ofNullable(dwgObjectFloorModeSelector.getValue()).orElse(RoomObjectMountingMode.STANDS_ON_COVERING);
        RoomObjectPreset preset = roomObjectPresetService.fromDwgBlock(block, mountingMode);
        registerRoomObjectPreset(preset);
        roomObjectPresetSelector.setValue(preset);
        toolSelector.setValue(DrawingTool.OBJECT);
        draftLabel.setText("DWG-Block als Objekt übernommen: " + block.name());
    }

    void registerRoomObjectPreset(RoomObjectPreset preset) {
        for (int index = 0; index < availableRoomObjectPresets.size(); index++) {
            RoomObjectPreset existing = availableRoomObjectPresets.get(index);
            if (existing.source().equals(preset.source()) || existing.id().equals(preset.id())) {
                availableRoomObjectPresets.set(index, preset);
                return;
            }
        }
        availableRoomObjectPresets.add(preset);
    }

    void refreshCurrentDwgLibraryAnalysis() {
        Optional<Path> currentLibrary = currentDwgLibraryPath();
        if (currentLibrary.isEmpty()) {
            draftLabel.setText("Bitte zuerst eine DWG-Bibliothek laden oder auswählen.");
            return;
        }
        DwgLibraryAnalysis analysis = analyzeDwgLibrary(currentLibrary.get(), true);
        applyDwgBlockFilter();
        updateCadLibrarySummary();
        draftLabel.setText("DWG-Bibliothek geprüft: " + analysis.summary());
    }

    void applyDwgBlockFilter() {
        String filter = Optional.ofNullable(dwgBlockSearchField.getText()).orElse("").trim().toLowerCase(Locale.GERMAN);
        DwgBlockDefinition previousSelection = dwgBlockSelector.getValue();
        List<DwgBlockDefinition> filteredBlocks = dwgAnalysesByPath.values().stream()
                .flatMap(analysis -> analysis.blocks().stream())
                .filter(block -> blockMatchesFilter(block, filter))
                .toList();
        availableDwgBlocks.setAll(filteredBlocks);
        if (previousSelection != null && filteredBlocks.stream().anyMatch(block -> block.sourceReference().equals(previousSelection.sourceReference()))) {
            dwgBlockSelector.setValue(filteredBlocks.stream()
                    .filter(block -> block.sourceReference().equals(previousSelection.sourceReference()))
                    .findFirst()
                    .orElse(null));
        } else if (!filteredBlocks.isEmpty()) {
            dwgBlockSelector.setValue(filteredBlocks.getFirst());
        } else {
            dwgBlockSelector.setValue(null);
        }
        refreshDwgBlockPreviewAndDetails();
        updateActionButtons();
    }

    boolean blockMatchesFilter(DwgBlockDefinition block, String filter) {
        if (filter.isBlank()) {
            return true;
        }
        return block.name().toLowerCase(Locale.GERMAN).contains(filter)
                || block.sourceFile().getFileName().toString().toLowerCase(Locale.GERMAN).contains(filter)
                || block.layers().stream().anyMatch(layer -> layer.toLowerCase(Locale.GERMAN).contains(filter));
    }

    void refreshDwgBlockPreviewAndDetails() {
        DwgBlockDefinition block = dwgBlockSelector.getValue();
        if (block == null) {
            dwgBlockDetailLabel.setText("Kein DWG-Block ausgewählt.");
            drawEmptyDwgPreview("Kein Block");
            return;
        }
        drawDwgPreview(block);
        String layerText = block.layers().isEmpty() ? "keine Layer" : String.join(", ", block.layers());
        String warningText = block.warnings().isEmpty() ? "" : "\nHinweise: " + String.join(" ", block.warnings());
        dwgBlockDetailLabel.setText(String.format(
                Locale.GERMAN,
                "%s%nDatei: %s%nMaße: %.1f x %.1f mm%nUrsprung: %.1f / %.1f mm%nEinheit: %s%nLayer: %s%nElemente: %d | Handles: %d | Inserts: %d%s",
                block.name(),
                block.sourceFile().getFileName(),
                block.widthMillimeters(),
                block.heightMillimeters(),
                block.originXMillimeters(),
                block.originYMillimeters(),
                block.unit(),
                layerText,
                block.entityCount(),
                block.handles().size(),
                block.inserts().size(),
                warningText
        ));
    }

    void drawEmptyDwgPreview(String text) {
        GraphicsContext graphics = dwgPreviewCanvas.getGraphicsContext2D();
        graphics.setFill(Color.web("#f7f3eb"));
        graphics.fillRect(0, 0, dwgPreviewCanvas.getWidth(), dwgPreviewCanvas.getHeight());
        graphics.setStroke(Color.web("#b8ac9c"));
        graphics.strokeRect(0.5, 0.5, dwgPreviewCanvas.getWidth() - 1.0, dwgPreviewCanvas.getHeight() - 1.0);
        graphics.setFill(Color.web("#6b6258"));
        graphics.fillText(text, 12, dwgPreviewCanvas.getHeight() / 2.0);
    }

    void drawDwgPreview(DwgBlockDefinition block) {
        if (!block.hasGeometry()) {
            drawEmptyDwgPreview("Keine Geometrie");
            return;
        }
        GraphicsContext graphics = dwgPreviewCanvas.getGraphicsContext2D();
        double width = dwgPreviewCanvas.getWidth();
        double height = dwgPreviewCanvas.getHeight();
        graphics.setFill(Color.web("#f7f3eb"));
        graphics.fillRect(0, 0, width, height);
        graphics.setStroke(Color.web("#b8ac9c"));
        graphics.strokeRect(0.5, 0.5, width - 1.0, height - 1.0);

        DwgBlockDefinition safeBlock = block;
        double padding = 16.0;
        double scale = Math.min(
                (width - padding * 2.0) / Math.max(1.0, safeBlock.widthMillimeters()),
                (height - padding * 2.0) / Math.max(1.0, safeBlock.heightMillimeters())
        );
        double x = padding;
        double y = padding;
        double previewWidth = safeBlock.widthMillimeters() * scale;
        double previewHeight = safeBlock.heightMillimeters() * scale;
        double offsetX = x + (width - padding * 2.0 - previewWidth) / 2.0;
        double offsetY = y + (height - padding * 2.0 - previewHeight) / 2.0;
        graphics.setFill(Color.web("#d8c6aa"));
        graphics.fillRect(offsetX, offsetY, previewWidth, previewHeight);
        graphics.setStroke(Color.web("#2f2a24"));
        graphics.strokeRect(offsetX, offsetY, previewWidth, previewHeight);

        double originX = offsetX + (safeBlock.originXMillimeters() - safeBlock.bounds().minXMillimeters()) * scale;
        double originY = offsetY + previewHeight - (safeBlock.originYMillimeters() - safeBlock.bounds().minYMillimeters()) * scale;
        graphics.setStroke(Color.web("#b3412f"));
        graphics.strokeLine(originX - 5.0, originY, originX + 5.0, originY);
        graphics.strokeLine(originX, originY - 5.0, originX, originY + 5.0);
        graphics.setFill(Color.web("#2f2a24"));
        graphics.fillText(String.format(Locale.GERMAN, "%.0f x %.0f mm", safeBlock.widthMillimeters(), safeBlock.heightMillimeters()), 10.0, height - 10.0);
    }

    void registerSurfacePreset(SurfaceCoveringPreset preset) {
        for (int index = 0; index < availableSurfacePresets.size(); index++) {
            if (availableSurfacePresets.get(index).coveringSource().equals(preset.coveringSource())) {
                availableSurfacePresets.set(index, preset);
                return;
            }
        }
        availableSurfacePresets.add(preset);
    }

    void saveCurrentSurfacePreset() {
        SurfaceCoveringPreset preset = currentSurfacePresetFromInputs();
        boolean overwrite = false;
        if (userSurfacePresetLibrary.containsPresetName(preset.name())) {
            overwrite = confirmOverwrite(
                    "Belagspreset überschreiben",
                    "Der Belag `" + preset.name() + "` ist in der Benutzerbibliothek bereits vorhanden.",
                    "Soll das vorhandene Preset durch die aktuell eingetragenen Werte ersetzt werden?"
            );
            if (!overwrite) {
                draftLabel.setText("Belagspreset nicht gespeichert.");
                return;
            }
        }
        try {
            SurfaceCoveringPreset savedPreset = userSurfacePresetLibrary.savePreset(preset, overwrite);
            registerSurfacePreset(savedPreset);
            surfacePresetSelector.setValue(savedPreset);
            draftLabel.setText("Belagspreset gespeichert: " + savedPreset.name());
        } catch (FileAlreadyExistsException exception) {
            showOperationException("Belagspreset existiert bereits und wurde nicht überschrieben", exception);
        } catch (IOException exception) {
            showOperationException("Belagspreset konnte nicht gespeichert werden", exception);
        }
    }

    SurfaceCoveringPreset currentSurfacePresetFromInputs() {
        return new SurfaceCoveringPreset(
                "",
                currentSurfaceLayerName(),
                currentSurfaceLayerThickness(),
                currentStoredSurfaceTileWidth(),
                currentStoredSurfaceTileHeight(),
                currentSurfaceLayoutMode(),
                currentSurfaceLayoutOffset(),
                currentSurfaceMinimumOffset(),
                currentSurfaceMinimumEdgeWidth(),
                currentSurfaceMinimumStartEndMargin(),
                currentSurfaceFreeMargins(),
                currentSurfaceLayoutAnchor(),
                currentSurfaceLayoutRotatedQuarterTurn(),
                Length.zero(),
                Length.zero(),
                currentSurfaceJointWidth(),
                currentSurfaceCutRestriction(),
                currentSurfaceCoveringSource()
        );
    }

    boolean confirmOverwrite(String title, String header, String content) {
        if (!interactiveDialogsEnabled) {
            // In der Automatisierung Overwrite ohne Nachfrage bestätigen, damit Aktionen nicht hängen.
            return true;
        }
        ButtonType overwriteButton = new ButtonType("Überschreiben", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, cancelButton, overwriteButton);
        alert.setTitle(title);
        alert.setHeaderText(header);
        Window owner = getScene() != null ? getScene().getWindow() : null;
        if (owner != null) {
            alert.initOwner(owner);
        }
        applyTooltip(alert.getDialogPane().lookupButton(overwriteButton),
                "Ersetzt die vorhandene Datei beziehungsweise das vorhandene Preset durch die neu gewählten Daten.");
        applyTooltip(alert.getDialogPane().lookupButton(cancelButton),
                "Bricht den Vorgang ab und lässt die vorhandenen Daten unverändert.");
        return alert.showAndWait()
                .filter(overwriteButton::equals)
                .isPresent();
    }

    void applyDoorPreset(DoorPreset preset) {
        if (preset == null) {
            return;
        }
        setLengthInput(doorWidthField, doorWidthUnit, preset.width(), LengthUnit.CENTIMETER);
        setLengthInput(doorHeightField, doorHeightUnit, preset.height(), LengthUnit.CENTIMETER);
        setLengthInput(thresholdField, thresholdUnit, preset.thresholdHeight(), LengthUnit.CENTIMETER);
    }

    void applyWindowPreset(WindowPreset preset) {
        if (preset == null) {
            return;
        }
        setLengthInput(windowWidthField, windowWidthUnit, preset.width(), LengthUnit.CENTIMETER);
        setLengthInput(windowHeightField, windowHeightUnit, preset.height(), LengthUnit.CENTIMETER);
        setLengthInput(sillHeightField, sillHeightUnit, preset.sillHeight(), LengthUnit.CENTIMETER);
    }

    void applyStairPreset(StairPreset preset) {
        if (preset == null) {
            return;
        }
        setLengthInput(stairHeightField, stairHeightUnit, preset.totalHeight(), LengthUnit.CENTIMETER);
        stairStepsField.setText(Integer.toString(preset.stepCount()));
    }

    void applyRoomObjectPreset(RoomObjectPreset preset) {
        if (preset == null) {
            return;
        }
        setLengthInput(roomObjectWidthField, roomObjectWidthUnit, preset.width(), LengthUnit.CENTIMETER);
        setLengthInput(roomObjectDepthField, roomObjectDepthUnit, preset.depth(), LengthUnit.CENTIMETER);
        setLengthInput(roomObjectHeightField, roomObjectHeightUnit, preset.height(), LengthUnit.CENTIMETER);
        roomObjectHeatOutputField.setText(formatNonNegativeDouble(preset.heatOutputWatts(), 1));
        roomObjectHeatingTypeSelector.setValue(preset.heatOutputWatts() > 0.0
                ? RoomObjectHeatingType.HEATING_ELEMENT
                : RoomObjectHeatingType.NONE);
        setLengthInput(roomObjectBaseElevationField, roomObjectBaseElevationUnit, Length.zero(), LengthUnit.CENTIMETER);
        roomObjectAngleField.setText("0");
    }

    void applySurfacePreset(SurfaceCoveringPreset preset) {
        if (preset == null) {
            return;
        }
        surfaceLayerNameField.setText(preset.name().replace("DWG-Referenz: ", "").replace("DWG-Block: ", ""));
        setLengthInput(surfaceLayerThicknessField, surfaceLayerThicknessUnit, preset.thickness(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceTileWidthField, surfaceTileWidthUnit, preset.tileWidth(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceTileHeightField, surfaceTileHeightUnit, preset.tileHeight(), LengthUnit.CENTIMETER);
        SurfaceLayoutAnchor normalizedAnchor = normalizedSurfaceLayoutAnchor(preset.layoutAnchor());
        applySurfaceLayoutAnchorSelection(normalizedAnchor);
        surfaceLayoutDirectionSelector.setValue(surfaceLayoutSelectionDirection(normalizedAnchor, preset.layoutRotatedQuarterTurn()));
        surfaceLayoutModeSelector.setValue(preset.layoutMode());
        setLengthInput(surfaceLayoutOffsetField, surfaceLayoutOffsetUnit, preset.offset(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceMinimumOffsetField, surfaceMinimumOffsetUnit, preset.minimumOffset(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit, preset.minimumEdgeWidth(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit, preset.minimumStartEndMargin(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceFreeMarginLeftField, surfaceFreeMarginLeftUnit, preset.freeMargins().left(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceFreeMarginRightField, surfaceFreeMarginRightUnit, preset.freeMargins().right(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceFreeMarginTopField, surfaceFreeMarginTopUnit, preset.freeMargins().top(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceFreeMarginBottomField, surfaceFreeMarginBottomUnit, preset.freeMargins().bottom(), LengthUnit.CENTIMETER);
        setLengthInput(surfaceJointWidthField, surfaceJointWidthUnit, preset.jointWidth(), LengthUnit.CENTIMETER);
        surfaceCutRestrictionSelector.setValue(preset.cutRestriction());
        dwgBlockNameField.setText(extractDwgBlockName(preset.coveringSource()).orElse(""));
    }

    Optional<HydronicHeating> selectedHydronicHeating() {
        Optional<HeatingZoneContext> zoneContext = selectedHeatingZoneContext();
        if (zoneContext.isPresent()) {
            return Optional.of(zoneContext.orElseThrow().heating());
        }
        Optional<HeatingContext> heatingContext = selectedHeatingContext();
        if (heatingContext.isPresent()) {
            return Optional.of(heatingContext.orElseThrow().heating());
        }
        Room room = selectedRoom().orElse(null);
        HeatingSurfacePosition surfacePosition = heatingSurfacePositionSelector.getValue();
        if (room == null || surfacePosition == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeLevel.get().findHydronicHeating(room.id(), surfacePosition));
    }

    void refreshHeatingSection() {
        Room room = selectedRoom().orElse(null);
        HydronicHeating heating = selectedHydronicHeating().orElse(null);
        if (room == null) {
            heatingSummaryLabel.setText("Für die Heizflächenplanung zuerst genau einen Raum auswählen.");
            heatingZoneList.getItems().clear();
            syncHeatingZoneSettingsInputs(null);
            syncHeatingRoutingCommandArea(null);
            updateActionButtons();
            return;
        }
        RoomHeatingOutputService.RoomHeatTotals roomHeatTotals = roomHeatingOutputService.totals(activeLevel.get(), room);
        if (heating == null) {
            heatingSummaryLabel.setText(String.format(
                    Locale.GERMAN,
                    "Für %s ist noch keine Heizung angelegt. FBH %.0f W · DH %.0f W · Fläche %.0f W · Heizelemente %.0f W · Raum gesamt %.0f W",
                    heatingSurfacePositionSelector.getValue(),
                    roomHeatTotals.floorHeatingWatts(),
                    roomHeatTotals.ceilingHeatingWatts(),
                    roomHeatTotals.additionalSurfaceHeatingWatts(),
                    roomHeatTotals.heatingElementWatts(),
                    roomHeatTotals.totalHeatOutputWatts()
            ));
            heatingZoneList.getItems().clear();
            syncHeatingZoneSettingsInputs(null);
            syncHeatingRoutingCommandArea(null);
            updateActionButtons();
            return;
        }
        if (heatingSurfacePositionSelector.getValue() != heating.surfacePosition()) {
            heatingSurfacePositionSelector.setValue(heating.surfacePosition());
        }
        heatingLayoutPatternSelector.setValue(heating.layoutPattern());
        syncLengthInput(heatingPipeSpacingField, heatingPipeSpacingUnit, heating.pipeSpacing(), LengthUnit.CENTIMETER);
        syncLengthInput(heatingPipeDiameterField, heatingPipeDiameterUnit, heating.pipeDiameter(), LengthUnit.CENTIMETER);
        syncLengthInput(heatingMaximumPipeLengthField, heatingMaximumPipeLengthUnit, heating.maximumPipeLength(), LengthUnit.CENTIMETER);
        syncLengthInput(heatingWallClearanceField, heatingWallClearanceUnit, heating.wallClearance(), LengthUnit.CENTIMETER);
        syncLengthInput(heatingSupplyXField, heatingSupplyXUnit, Length.ofMillimeters(heating.supplyPoint().xMillimeters()), LengthUnit.CENTIMETER);
        syncLengthInput(heatingSupplyYField, heatingSupplyYUnit, Length.ofMillimeters(heating.supplyPoint().yMillimeters()), LengthUnit.CENTIMETER);
        syncLengthInput(heatingReturnXField, heatingReturnXUnit, Length.ofMillimeters(heating.returnPoint().xMillimeters()), LengthUnit.CENTIMETER);
        syncLengthInput(heatingReturnYField, heatingReturnYUnit, Length.ofMillimeters(heating.returnPoint().yMillimeters()), LengthUnit.CENTIMETER);
        HydronicHeatingLayoutService.PlanningResult layoutResult = hydronicHeatingLayoutService.layoutBestEffort(heating);
        List<HydronicHeatingLayoutService.CircuitLayout> circuits = layoutResult.circuits();
        int selectedIndex = selectedHeatingZoneContext()
                .filter(context -> context.heating().id().equals(heating.id()))
                .map(HeatingZoneContext::zoneIndex)
                .orElse(heatingZoneList.getSelectionModel().getSelectedIndex());
        updatingHeatingZoneSelection = true;
        try {
            heatingZoneList.getItems().setAll(heating.zones().stream()
                    .map(zone -> CadWorkbenchHeatingSupport.describeHeatingZone(zone, circuits))
                    .toList());
            if (!heatingZoneList.getItems().isEmpty()) {
                heatingZoneList.getSelectionModel().select(Math.max(0, Math.min(selectedIndex, heatingZoneList.getItems().size() - 1)));
            }
        } finally {
            updatingHeatingZoneSelection = false;
        }
        syncHeatingZoneSettingsInputs(heating);
        syncHeatingRoutingCommandArea(heating);
        double totalLength = circuits.stream().mapToDouble(circuit -> circuit.pipeLength().toMillimeters()).sum();
        double totalArea = heating.zones().stream().mapToDouble(HeatingZone::areaSquareMeters).sum();
        double totalHeatOutput = heating.zones().stream().mapToDouble(HeatingZone::heatOutputWatts).sum();
        boolean maximumExceeded = circuits.stream()
                .anyMatch(circuit -> circuit.pipeLength().compareTo(heating.maximumPipeLength()) > 0);
        String warning = CadWorkbenchHeatingSupport.heatingWarning(layoutResult.validationReport(), maximumExceeded);
        heatingSummaryLabel.setText(String.format(
                Locale.GERMAN,
                "%s · Raumvorgabe %s · %d Heizkreis(e) · %.2f m² · %.1f m HKL · %.0f W FBH · %.0f W DH · %.0f W Fläche · %.0f W Heizelemente · %.0f W Raum gesamt%s",
                heating.surfacePosition(),
                heating.layoutPattern(),
                circuits.size(),
                totalArea,
                totalLength / 1_000.0,
                roomHeatTotals.floorHeatingWatts(),
                roomHeatTotals.ceilingHeatingWatts(),
                roomHeatTotals.additionalSurfaceHeatingWatts(),
                roomHeatTotals.heatingElementWatts(),
                roomHeatTotals.totalHeatOutputWatts(),
                warning
        ));
        updateActionButtons();
    }

    void syncHeatingRoutingCommandArea() {
        syncHeatingRoutingCommandArea(selectedHydronicHeating().orElse(null));
    }

    void syncHeatingRoutingCommandArea(HydronicHeating heating) {
        int selectedIndex = heatingZoneList.getSelectionModel().getSelectedIndex();
        String routingText = "";
        if (heating != null && selectedIndex >= 0 && selectedIndex < heating.zones().size()) {
            HeatingZone zone = heating.zones().get(selectedIndex);
            routingText = zone.routingCommands();
            if (Objects.equals(normalizeRoutingEditorText(heatingRoutingCommandArea.getText(), zone), routingText)) {
                return;
            }
        }
        replaceTextPreservingCaretAndScroll(heatingRoutingCommandArea, routingText);
    }

    void syncHeatingZoneSettingsInputs() {
        syncHeatingZoneSettingsInputs(selectedHydronicHeating().orElse(null));
    }

    void syncHeatingZoneSettingsInputs(HydronicHeating heating) {
        int selectedIndex = heatingZoneList.getSelectionModel().getSelectedIndex();
        if (heating == null || selectedIndex < 0 || selectedIndex >= heating.zones().size()) {
            heatingZoneNameField.setText("");
            heatingZoneLayoutPatternSelector.setValue(HeatingLayoutPattern.VARIO);
            heatingZoneFlowInvertedCheckBox.setSelected(false);
            heatingZoneSerpentineMiddleLineCheckBox.setSelected(false);
            heatingZoneHeatOutputField.setText("0");
            heatingZonePointArea.setText("");
            return;
        }
        HeatingZone zone = heating.zones().get(selectedIndex);
        heatingZoneNameField.setText(zone.name());
        heatingZoneLayoutPatternSelector.setValue(heatingCircuitRoutingService.manualPattern(zone.layoutPattern()));
        heatingZoneFlowInvertedCheckBox.setSelected(zone.flowInverted());
        heatingZoneSerpentineMiddleLineCheckBox.setSelected(zone.serpentineMiddleLine());
        heatingZoneHeatOutputField.setText(String.format(Locale.GERMAN, "%.1f", zone.heatOutputWattsPerSquareMeter()));
        heatingZonePointArea.setText(zone.outline().stream()
                .map(point -> String.format(Locale.GERMAN, "%.3f; %.3f", point.xMillimeters() / 10.0, point.yMillimeters() / 10.0))
                .collect(java.util.stream.Collectors.joining(System.lineSeparator())));
    }

    void replaceTextPreservingCaretAndScroll(TextArea textArea, String text) {
        String replacement = Optional.ofNullable(text).orElse("");
        String currentText = Optional.ofNullable(textArea.getText()).orElse("");
        if (Objects.equals(currentText, replacement)) {
            return;
        }
        int caretPosition = textArea.getCaretPosition();
        double scrollTop = textArea.getScrollTop();
        updatingHeatingRoutingInput = true;
        try {
            textArea.setText(replacement);
        } finally {
            updatingHeatingRoutingInput = false;
        }
        textArea.positionCaret(Math.min(caretPosition, textArea.getLength()));
        textArea.setScrollTop(scrollTop);
    }

    String normalizeRoutingEditorText(String text, HeatingZone zone) {
        return HeatingRoutingLanguage.stripWhitespaceAndNormalizeAliases(text, usesMirroredRoutingAliases(zone));
    }

    String normalizeRoutingEditorDisplayText(String text) {
        return HeatingRoutingLanguage.replaceEditorAliasesPreservingWhitespace(
                text,
                activeHeatingZoneForRoutingInput()
                        .map(this::usesMirroredRoutingAliases)
                        .orElse(false)
        );
    }

    Optional<HeatingZone> activeHeatingZoneForRoutingInput() {
        Optional<HeatingZoneContext> zoneContext = selectedHeatingZoneContext();
        if (zoneContext.isPresent()) {
            return Optional.of(zoneContext.orElseThrow().zone());
        }
        HydronicHeating heating = selectedHydronicHeating().orElse(null);
        int selectedIndex = heatingZoneList.getSelectionModel().getSelectedIndex();
        if (heating == null || selectedIndex < 0 || selectedIndex >= heating.zones().size()) {
            return Optional.empty();
        }
        return Optional.of(heating.zones().get(selectedIndex));
    }

    boolean usesMirroredRoutingAliases(HeatingZone zone) {
        return zone != null && HeatingRoutingLanguage.hasSimpleMirror(
                zone.routingMirroredHorizontally(),
                zone.routingMirroredVertically()
        );
    }

    String heatingUpdateMessage(HydronicHeating heating, String successPrefix) {
        HydronicHeatingLayoutService.PlanningResult layoutResult = hydronicHeatingLayoutService.layoutBestEffort(heating);
        boolean maximumExceeded = layoutResult.circuits().stream()
                .anyMatch(circuit -> circuit.pipeLength().compareTo(heating.maximumPipeLength()) > 0);
        String warning = CadWorkbenchHeatingSupport.heatingWarning(layoutResult.validationReport(), maximumExceeded);
        return successPrefix + (warning.isBlank() ? "" : " " + warning.strip());
    }

    void planHydronicHeating() {
        throw new IllegalStateException("Die automatische Planung ganzer Räume ist vorübergehend deaktiviert. Lege Heizkreise mit dem Werkzeug `Heizkreis` als Rechtecke an.");
    }

    void planHydronicHeatingAutomatically() {
        Room room = selectedRoom().orElseThrow(() -> new IllegalStateException("Für die Heizflächenplanung muss ein Raum ausgewählt sein."));
        HydronicHeating existing = selectedHydronicHeating().orElse(null);
        HydronicHeating unplanned = heatingFromInputs(room, existing == null ? UUID.randomUUID() : existing.id());
        HydronicHeatingLayoutService.PlanningResult result = hydronicHeatingLayoutService.suggest(
                room,
                unplanned,
                activeLevel.get().staircases(),
                activeLevel.get().floorOpenings(),
                activeLevel.get().heatingExclusionAreas()
        );
        if (!result.validationReport().valid()) {
            throw new IllegalArgumentException(result.validationReport().summary());
        }
        boolean maximumExceeded = result.circuits().stream()
                .anyMatch(circuit -> circuit.pipeLength().compareTo(result.heating().maximumPipeLength()) > 0);
        if (maximumExceeded) {
            throw new IllegalArgumentException("Die maximale Rohrlänge kann mit den gewählten Verteilerpunkten und Abständen nicht eingehalten werden.");
        }
        rememberStateForUndo();
        if (existing == null) {
            activeLevel.get().addHydronicHeating(result.heating());
        } else {
            activeLevel.get().replaceHydronicHeating(result.heating());
        }
        refreshHeatingSection();
        String warningText = formatHeatingWarnings(result.validationReport().warnings());
        draftLabel.setText(result.heating().zones().size() + " Heizkreis(e) für " + result.heating().surfacePosition() + " geplant." + warningText);
        showHeatingWarnings(result.validationReport().warnings());
        render();
    }

    String formatHeatingWarnings(List<HydronicHeatingLayoutService.ValidationIssue> warnings) {
        if (warnings.isEmpty()) {
            return "";
        }
        return " Warnung: " + warnings.stream()
                .map(HydronicHeatingLayoutService.ValidationIssue::message)
                .collect(java.util.stream.Collectors.joining(" "));
    }

    void showHeatingWarnings(List<HydronicHeatingLayoutService.ValidationIssue> warnings) {
        if (warnings.isEmpty() || !interactiveDialogsEnabled) {
            return;
        }
        Alert alert = new Alert(
                Alert.AlertType.WARNING,
                warnings.stream()
                        .map(HydronicHeatingLayoutService.ValidationIssue::message)
                        .collect(java.util.stream.Collectors.joining(System.lineSeparator())),
                ButtonType.OK
        );
        alert.setTitle("FBH-Planung mit Warnungen");
        alert.setHeaderText("CADas hat nur eine angepasste FBH-Planung erstellen können.");
        Window owner = currentWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    void showRoomSynchronizationWarning(Level.RoomReplacementImpact impact) {
        if (!impact.needsWarning()) {
            return;
        }
        List<String> lines = new ArrayList<>();
        if (impact.changedRoomCount() > 0) {
            lines.add(impact.changedRoomCount() + " Raum/Räume geometrisch angepasst.");
        }
        if (impact.removedRoomCount() > 0) {
            lines.add(impact.removedRoomCount() + " Raum/Räume entfallen.");
        }
        if (impact.addedRoomCount() > 0) {
            lines.add(impact.addedRoomCount() + " Raum/Räume neu erkannt.");
        }
        if (impact.removedHeatingCircuitCount() > 0) {
            lines.add(impact.removedHeatingCircuitCount() + " Heizkreis(e) entfernt.");
        } else if (impact.removedHydronicHeatingCount() > 0) {
            lines.add(impact.removedHydronicHeatingCount() + " Heizfläche(n) entfernt.");
        }
        if (impact.removedHeatingExclusionAreaCount() > 0) {
            lines.add(impact.removedHeatingExclusionAreaCount() + " FBH-Sperrfläche(n) entfernt.");
        }
        if (impact.removedSurfaceLayerCount() > 0) {
            lines.add(impact.removedSurfaceLayerCount() + " Belagsebene(n) entfernt.");
        }
        List<String> affectedRooms = new ArrayList<>();
        affectedRooms.addAll(impact.changedRoomNames());
        affectedRooms.addAll(impact.removedRoomNames());
        if (!affectedRooms.isEmpty()) {
            List<String> distinctRooms = affectedRooms.stream().distinct().toList();
            String preview = distinctRooms.stream().limit(3).collect(java.util.stream.Collectors.joining(", "));
            if (distinctRooms.size() > 3) {
                preview += " ...";
            }
            lines.add("Betroffene Räume: " + preview);
        }
        lines.add("Bitte Raumzuordnung, Heizflächen und Beläge direkt prüfen.");
        rememberWarning(
                "Räume und Zuordnungen geändert",
                "Durch die Bauteiländerung wurden Räume neu ausgewertet.",
                String.join(System.lineSeparator(), lines)
        );
    }

    void rememberWarning(String title, String header, String content) {
        lastWarningDialog = new WarningPresentation(title, header, content);
        rememberedWarningCount++;
        if (!interactiveDialogsEnabled) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.WARNING, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(header);
        Window owner = currentWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    HydronicHeating heatingFromInputs(Room room, UUID heatingId) {
        HeatingSurfacePosition surfacePosition = Optional.ofNullable(heatingSurfacePositionSelector.getValue())
                .orElse(HeatingSurfacePosition.FLOOR);
        HeatingLayoutPattern layoutPattern = Optional.ofNullable(heatingLayoutPatternSelector.getValue())
                .orElse(HeatingLayoutPattern.VARIO);
        CadWorkbenchHeatingSupport.HydronicManifoldDefaults manifoldDefaults = CadWorkbenchHeatingSupport.defaultHydronicManifold(
                room,
                DEFAULT_HKV_PAIR_DISTANCE_MILLIMETERS
        );
        return new HydronicHeating(
                heatingId, room.id(), surfacePosition, layoutPattern,
                requiredPositiveLength(heatingPipeSpacingField, heatingPipeSpacingUnit, "Verlegeabstand"),
                requiredPositiveLength(heatingPipeDiameterField, heatingPipeDiameterUnit, "Rohrdurchmesser"),
                requiredPositiveLength(heatingMaximumPipeLengthField, heatingMaximumPipeLengthUnit, "maximale Rohrlänge"),
                requiredNonNegativeLength(heatingWallClearanceField, heatingWallClearanceUnit, "Wandabstand"),
                manifoldDefaults.supplyPoint(),
                manifoldDefaults.returnPoint(),
                List.of()
        );
    }

    Length requiredPositiveLength(TextField field, ComboBox<LengthUnit> unitSelector, String label) {
        Length length = parseLength(field, unitSelector.getValue())
                .orElseThrow(() -> new IllegalArgumentException(label + " ist keine gültige Länge."));
        if (length.toMillimeters() <= 0.0) {
            throw new IllegalArgumentException(label + " muss größer als null sein.");
        }
        return length;
    }

    Length requiredNonNegativeLength(TextField field, ComboBox<LengthUnit> unitSelector, String label) {
        Length length = parseLength(field, unitSelector.getValue())
                .orElseThrow(() -> new IllegalArgumentException(label + " ist keine gültige Länge."));
        if (length.toMillimeters() < 0.0) {
            throw new IllegalArgumentException(label + " darf nicht negativ sein.");
        }
        return length;
    }

    void applySelectedHeatingZoneSettings() {
        HeatingZoneContext context = selectedHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Zuerst einen Heizkreis auswählen."));
        HeatingZone draft = heatingZoneDraft(
                context.zone(),
                heatingRoutingCommandArea.getText(),
                heatingZoneLayoutPatternSelector.getValue(),
                heatingZoneSerpentineMiddleLineCheckBox.isSelected()
        );
        HeatingZone replacement = draft.hasRoutingCommands()
                ? heatingCircuitRoutingService.withRoutingCommands(draft, context.heating(), draft.routingCommands(), draft.serpentineMiddleLine())
                : heatingCircuitRoutingService.regenerateWithPattern(
                draft,
                context.heating(),
                draft.layoutPattern(),
                draft.serpentineMiddleLine()
        );
        replacement = snapHeatingZoneRoutingStartIfNeeded(replacement);
        replaceHeatingZone(context, replacement, "Heizkreis aktualisiert.");
    }

    HeatingZone heatingZoneDraft(
            HeatingZone baseZone,
            String routingCommands,
            HeatingLayoutPattern layoutPattern,
            boolean serpentineMiddleLine
    ) {
        return new HeatingZone(
                baseZone.id(),
                heatingZoneNameField.getText(),
                CadWorkbenchHeatingSupport.parseHeatingZonePoints(heatingZonePointArea.getText()),
                heatingCircuitRoutingService.manualPattern(layoutPattern),
                heatingZoneFlowInvertedCheckBox.isSelected(),
                baseZone.supplyConnectionPoint(),
                baseZone.returnConnectionPoint(),
                baseZone.routingStartPoint(),
                normalizeRoutingEditorText(routingCommands, baseZone),
                serpentineMiddleLine,
                CadWorkbenchHeatingSupport.parseNonNegativeDouble(heatingZoneHeatOutputField.getText(), "Heizleistung pro m²"),
                baseZone.routingQuarterTurns(),
                baseZone.routingMirroredHorizontally(),
                baseZone.routingMirroredVertically()
        );
    }

    void generateSelectedHeatingZoneRouting() {
        HeatingZoneContext context = selectedHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Zuerst einen Heizkreis auswählen."));
        HeatingZone draft = heatingZoneDraft(
                context.zone(),
                heatingRoutingCommandArea.getText(),
                Optional.ofNullable(heatingZoneLayoutPatternSelector.getValue()).orElse(HeatingLayoutPattern.VARIO),
                heatingZoneSerpentineMiddleLineCheckBox.isSelected()
        );
        HeatingZone replacement = snapHeatingZoneRoutingStartIfNeeded(
                heatingCircuitRoutingService.regenerateWithPattern(
                        draft,
                        context.heating(),
                        draft.layoutPattern(),
                        draft.serpentineMiddleLine()
                )
        );
        replaceHeatingZone(context, replacement, "Routing für Heizkreis neu erzeugt.");
    }

    void applySelectedHeatingZoneRouting() {
        if (updatingHeatingRoutingInput) {
            return;
        }
        HeatingZoneContext context = selectedHeatingZoneContext()
                .orElseThrow(() -> new IllegalStateException("Zuerst einen Heizkreis auswählen."));
        String commands = normalizeRoutingEditorText(heatingRoutingCommandArea.getText(), context.zone());
        if (commands.isBlank()) {
            throw new IllegalArgumentException("Routing darf nicht leer sein.");
        }
        HeatingZone draft = heatingZoneDraft(
                context.zone(),
                commands,
                Optional.ofNullable(heatingZoneLayoutPatternSelector.getValue()).orElse(HeatingLayoutPattern.VARIO),
                heatingZoneSerpentineMiddleLineCheckBox.isSelected()
        );
        HeatingZone replacement = heatingCircuitRoutingService.withRoutingCommands(
                draft, context.heating(), commands, draft.serpentineMiddleLine()
        );
        replacement = snapHeatingZoneRoutingStartIfNeeded(replacement);
        replaceHeatingZone(context, replacement, "Routing für Heizkreis übernommen.");
    }

    boolean removeHeatingZoneById(UUID zoneId) {
        Optional<HeatingZoneContext> context = heatingZoneContext(zoneId);
        if (context.isEmpty()) {
            return false;
        }
        HeatingZoneContext heatingZoneContext = context.orElseThrow();
        List<HeatingZone> zones = new ArrayList<>(heatingZoneContext.heating().zones());
        zones.removeIf(zone -> zone.id().equals(zoneId));
        activeLevel.get().replaceHydronicHeating(heatingZoneContext.heating().withZones(zones));
        return true;
    }

    void applyHeatingZones(HydronicHeating heating, List<HeatingZone> zones, int selectedIndex) {
        HydronicHeating updatedHeating = heating.withZones(zones);
        rememberStateForUndo();
        activeLevel.get().replaceHydronicHeating(updatedHeating);
        refreshHeatingSection();
        if (selectedIndex >= 0) {
            heatingZoneList.getSelectionModel().select(selectedIndex);
        }
        draftLabel.setText(heatingUpdateMessage(updatedHeating, "Heizbereiche aktualisiert."));
        recomputeHeatingLayoutNow(heating.id());
    }

    boolean resetHydronicManifoldById(UUID heatingId) {
        HydronicHeating heating = activeLevel.get().hydronicHeatings().stream()
                .filter(candidate -> candidate.id().equals(heatingId))
                .findFirst()
                .orElse(null);
        if (heating == null) {
            return false;
        }
        Room room = activeLevel.get().rooms().stream()
                .filter(candidate -> candidate.id().equals(heating.roomId()))
                .findFirst()
                .orElse(null);
        if (room == null) {
            return false;
        }
        CadWorkbenchHeatingSupport.HydronicManifoldDefaults defaults = CadWorkbenchHeatingSupport.defaultHydronicManifold(
                room,
                DEFAULT_HKV_PAIR_DISTANCE_MILLIMETERS
        );
        activeLevel.get().replaceHydronicHeating(
                heating.withManifold(defaults.supplyPoint(), defaults.returnPoint())
                        .withManifoldFreeArea(
                                HydronicHeating.DEFAULT_MANIFOLD_FREE_AREA_WIDTH,
                                HydronicHeating.DEFAULT_MANIFOLD_FREE_AREA_DEPTH
                        )
        );
        return true;
    }
}
