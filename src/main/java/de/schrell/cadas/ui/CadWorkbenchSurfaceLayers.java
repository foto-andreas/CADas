package de.schrell.cadas.ui;

import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.formatSurfaceLayoutCorner;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.formatSurfaceLayoutDirection;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.normalizedSurfaceLayoutAnchor;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.surfaceLayoutRotatedQuarterTurn;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.surfaceLayoutSelectionDirection;
import static de.schrell.cadas.ui.CadWorkbenchCoveringSourceSupport.formatCoveringSourceLabel;
import de.schrell.cadas.application.dwg.DwgBlockDefinition;
import de.schrell.cadas.application.layers.SurfaceCoveringPreset;
import de.schrell.cadas.application.layers.SurfaceCoveringPresetService;
import de.schrell.cadas.application.layers.TileLayoutRequest;
import de.schrell.cadas.application.layers.WallSurfaceTargetKey;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import de.schrell.cadas.domain.model.SurfaceLayoutDirection;
import de.schrell.cadas.domain.model.SurfaceLayoutMargins;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

abstract class CadWorkbenchSurfaceLayers extends CadWorkbenchSurfaceAndHeating {

    void refreshSurfaceLayerSection() {
        Optional<SurfaceSelectionContext> selectionContext = currentSurfaceSelectionContext();
        if (selectionContext.isEmpty()) {
            surfaceLayerTargetLabel.setText("Keine passende Belagsfläche ausgewählt.");
            surfaceLayerSelectionHintLabel.setText(currentSurfaceSelectionHint());
            surfaceLayerList.getItems().clear();
            surfaceLayerCoverageLabel.setText("Keine Ebenen ausgewählt.");
            updateActionButtons();
            return;
        }
        SurfaceSelectionContext context = selectionContext.get();
        surfaceLayerTargetLabel.setText(context.label());
        surfaceLayerSelectionHintLabel.setText(context.hint());
        Optional<SurfaceLayerStack> stack = currentDisplaySurfaceLayerStack();
        if (stack.isEmpty()) {
            surfaceLayerList.getItems().clear();
            surfaceLayerCoverageLabel.setText(context.targetKeys().size() > 1
                    ? "Ausgewählte Wände haben noch keine gemeinsame Belagsfolge."
                    : "Noch keine Ebene auf dieser Fläche.");
            updateActionButtons();
            return;
        }
        int previousSelection = surfaceLayerList.getSelectionModel().getSelectedIndex();
        surfaceLayerList.getItems().setAll(stack.get().layers().stream().map(this::describeSurfaceLayer).toList());
        if (surfaceLayerList.getItems().isEmpty()) {
            surfaceLayerList.getSelectionModel().clearSelection();
        } else if (previousSelection < 0) {
            surfaceLayerList.getSelectionModel().selectFirst();
        } else {
            surfaceLayerList.getSelectionModel().select(Math.min(previousSelection, surfaceLayerList.getItems().size() - 1));
        }
        syncInputsFromSelectedSurfaceLayer();
        updateActionButtons();
    }

    String describeSurfaceLayer(SurfaceLayer layer) {
        String visibility = layer.visible() ? "sichtbar" : "aus";
        int tileCount = estimatedTileCount(layer);
        String sourceLabel = formatCoveringSourceLabel(layer.coveringSource());
        String source = sourceLabel.isBlank() ? "" : " | Quelle: " + sourceLabel;
        return layer.name()
                + " | "
                + layer.thickness().format(LengthUnit.MILLIMETER, 1)
                + " | "
                + visibility
                + " | "
                + formatSurfaceLayoutCorner(layer.layoutAnchor())
                + " | "
                + formatSurfaceLayoutDirection(layer.layoutAnchor(), layer.layoutRotatedQuarterTurn())
                + " | "
                + tileCount
                + " Elemente | "
                + layer.cutRestriction().label()
                + source;
    }

    int estimatedTileCount(SurfaceLayer layer) {
        Optional<Room> room = selectedRoom();
        if (room.isEmpty() || currentSurfaceType() == SurfaceType.WALL_INTERIOR || currentSurfaceType() == SurfaceType.WALL_EXTERIOR) {
            return 0;
        }
        TileLayoutRequest request = new TileLayoutRequest(
                Length.ofMillimeters(room.get().widthMillimeters()),
                Length.ofMillimeters(room.get().depthMillimeters()),
                layer.effectiveTileWidth(),
                layer.effectiveTileHeight(),
                layer.layoutMode(),
                layer.layoutOffset(),
                layer.minimumOffset(),
                layer.minimumEdgeWidth(),
                layer.minimumStartEndMargin(),
                layer.freeMargins(),
                SurfaceLayoutAnchor.AUTO,
                Length.zero(),
                Length.zero()
        );
        return tileLayoutService.fillSurface(request).size();
    }

    void syncInputsFromSelectedSurfaceLayer() {
        SurfaceLayer selectedLayer = selectedSurfaceLayer().orElse(null);
        if (selectedLayer == null) {
            updateActionButtons();
            return;
        }
        surfaceLayerNameField.setText(selectedLayer.name());
        self().syncLengthInput(surfaceLayerThicknessField, surfaceLayerThicknessUnit, selectedLayer.thickness(), LengthUnit.CENTIMETER);
        self().syncLengthInput(surfaceTileWidthField, surfaceTileWidthUnit, selectedLayer.tileWidth(), LengthUnit.CENTIMETER);
        self().syncLengthInput(surfaceTileHeightField, surfaceTileHeightUnit, selectedLayer.tileHeight(), LengthUnit.CENTIMETER);
        SurfaceLayoutAnchor normalizedAnchor = normalizedSurfaceLayoutAnchor(selectedLayer.layoutAnchor());
        applySurfaceLayoutAnchorSelection(normalizedAnchor);
        surfaceLayoutDirectionSelector.setValue(surfaceLayoutSelectionDirection(normalizedAnchor, selectedLayer.layoutRotatedQuarterTurn()));
        surfaceLayoutModeSelector.setValue(selectedLayer.layoutMode());
        self().syncLengthInput(surfaceLayoutOffsetField, surfaceLayoutOffsetUnit, selectedLayer.layoutOffset(), LengthUnit.CENTIMETER);
        self().syncLengthInput(surfaceMinimumOffsetField, surfaceMinimumOffsetUnit, selectedLayer.minimumOffset(), LengthUnit.CENTIMETER);
        self().syncLengthInput(surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit, selectedLayer.minimumEdgeWidth(), LengthUnit.CENTIMETER);
        self().syncLengthInput(surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit, selectedLayer.minimumStartEndMargin(), LengthUnit.CENTIMETER);
        self().syncLengthInput(surfaceFreeMarginLeftField, surfaceFreeMarginLeftUnit, selectedLayer.freeMargins().left(), LengthUnit.CENTIMETER);
        self().syncLengthInput(surfaceFreeMarginRightField, surfaceFreeMarginRightUnit, selectedLayer.freeMargins().right(), LengthUnit.CENTIMETER);
        self().syncLengthInput(surfaceFreeMarginTopField, surfaceFreeMarginTopUnit, selectedLayer.freeMargins().top(), LengthUnit.CENTIMETER);
        self().syncLengthInput(surfaceFreeMarginBottomField, surfaceFreeMarginBottomUnit, selectedLayer.freeMargins().bottom(), LengthUnit.CENTIMETER);
        self().syncLengthInput(surfaceJointWidthField, surfaceJointWidthUnit, selectedLayer.jointWidth(), LengthUnit.CENTIMETER);
        surfaceCutRestrictionSelector.setValue(selectedLayer.cutRestriction());
        surfaceLayerCoverageLabel.setText(describeSurfaceLayer(selectedLayer));
        updateActionButtons();
    }

    void addSurfaceLayer() {
        Optional<SurfaceSelectionContext> selectionContext = currentSurfaceSelectionContext();
        if (selectionContext.isEmpty()) {
            showSurfaceLayerError("Belag kann nicht angelegt werden.", currentSurfaceSelectionHint());
            return;
        }
        if (!validateSurfaceLayerSelection(selectionContext.get())) {
            return;
        }
        self().rememberStateForUndo();
        for (String targetKey : selectionContext.get().targetKeys()) {
            SurfaceLayerStack stack = activeLevel.get().findSurfaceLayerStack(selectionContext.get().surfaceType(), targetKey);
            if (stack == null) {
                stack = new SurfaceLayerStack(selectionContext.get().surfaceType(), targetKey);
                activeLevel.get().addSurfaceLayerStack(stack);
            }
            stack.addLayer(buildSurfaceLayerFromInputs());
        }
        afterSurfaceLayerMutation(selectionContext.get().targetKeys().size() > 1
                ? "Belag auf ausgewählte Wände angewendet."
                : "Ebene hinzugefügt.");
    }

    void updateSurfaceLayer() {
        List<SurfaceLayerStack> stacks = currentSurfaceLayerStacks();
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        if (stacks.isEmpty() || selectedIndex < 0) {
            return;
        }
        self().rememberStateForUndo();
        for (SurfaceLayerStack stack : stacks) {
            SurfaceLayer selectedLayer = stack.layers().get(selectedIndex);
            replaceSurfaceLayer(stack, selectedLayer.id(), new SurfaceLayer(
                    selectedLayer.id(),
                    currentSurfaceLayerName(),
                    currentSurfaceLayerThickness(),
                    selectedLayer.visible(),
                    currentStoredSurfaceTileWidth(),
                    currentStoredSurfaceTileHeight(),
                    currentSurfaceLayoutMode(),
                    currentSurfaceLayoutOffset(),
                    currentSurfaceMinimumOffset(),
                    currentSurfaceMinimumEdgeWidth(),
                    currentSurfaceMinimumStartEndMargin(),
                    currentSurfaceFreeMargins(),
                    currentSurfaceLayoutAnchor(),
                    Length.zero(),
                    Length.zero(),
                    currentSurfaceJointWidth(),
                    currentSurfaceCutRestriction(),
                    currentSurfaceCoveringSource(),
                    currentSurfaceLayoutRotatedQuarterTurn()
            ));
        }
        afterSurfaceLayerMutation("Ebene aktualisiert.");
    }

    void removeSurfaceLayer() {
        List<SurfaceLayerStack> stacks = currentSurfaceLayerStacks();
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        if (stacks.isEmpty() || selectedIndex < 0) {
            return;
        }
        self().rememberStateForUndo();
        for (SurfaceLayerStack stack : stacks) {
            SurfaceLayer selectedLayer = stack.layers().get(selectedIndex);
            stack.removeLayer(selectedLayer.id());
            if (stack.layers().isEmpty()) {
                activeLevel.get().removeSurfaceLayerStack(stack.id());
            }
        }
        afterSurfaceLayerMutation("Ebene entfernt.");
    }

    void toggleSurfaceLayerVisibility() {
        List<SurfaceLayerStack> stacks = currentSurfaceLayerStacks();
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        if (stacks.isEmpty() || selectedIndex < 0) {
            return;
        }
        self().rememberStateForUndo();
        for (SurfaceLayerStack stack : stacks) {
            SurfaceLayer selectedLayer = stack.layers().get(selectedIndex);
            stack.setVisibility(selectedLayer.id(), !selectedLayer.visible());
        }
        afterSurfaceLayerMutation("Ebenensichtbarkeit umgeschaltet.");
    }

    void moveSurfaceLayer(int direction) {
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        List<SurfaceLayerStack> stacks = currentSurfaceLayerStacks();
        if (stacks.isEmpty() || selectedIndex < 0) {
            return;
        }
        self().rememberStateForUndo();
        for (SurfaceLayerStack stack : stacks) {
            SurfaceLayer selectedLayer = stack.layers().get(selectedIndex);
            stack.moveLayer(selectedLayer.id(), selectedIndex + direction);
        }
        afterSurfaceLayerMutation("Ebenenreihenfolge geändert.");
        int newIndex = Math.max(0, Math.min(selectedIndex + direction, stacks.getFirst().layers().size() - 1));
        surfaceLayerList.getSelectionModel().select(newIndex);
    }

    void afterSurfaceLayerMutation(String message) {
        if (selectedSelections.stream().anyMatch(selection -> selection.kind() == RenderableKind.WALL
                || selection.kind() == RenderableKind.STAIR)) {
            self().synchronizeRoomsFromWalls(activeLevel.get());
        }
        self().markThreeDDirty();
        refreshSurfaceLayerSection();
        draftLabel.setText(message);
        render();
    }

    void repairSelectedSurfaceLayerLayout() {
        List<SurfaceLayerStack> stacks = currentSurfaceLayerStacks();
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        if (stacks.isEmpty() || selectedIndex < 0) {
            return;
        }
        boolean changed = false;
        for (SurfaceLayerStack stack : stacks) {
            SurfaceLayer selectedLayer = stack.layers().get(selectedIndex);
            SurfaceLayer repairedLayer = repairedSurfaceLayer(selectedLayer);
            if (repairedLayer.equals(selectedLayer)) {
                continue;
            }
            if (!changed) {
                self().rememberStateForUndo();
                changed = true;
            }
            replaceSurfaceLayer(stack, selectedLayer.id(), repairedLayer);
        }
        if (changed) {
            afterSurfaceLayerMutation("Belag-Verlegung repariert.");
            return;
        }
        draftLabel.setText("Belag erfüllt bereits die aktuellen Verlegeregeln.");
        render();
    }

    boolean selectedSurfaceLayerNeedsRepair() {
        return selectedSurfaceLayer().map(this::surfaceLayerNeedsRepair).orElse(false);
    }

    boolean surfaceLayerNeedsRepair(SurfaceLayer layer) {
        return layer.layoutAnchor() == SurfaceLayoutAnchor.AUTO
                || layer.startRowTrim().toMillimeters() > 0.001
                || layer.startRowWidth().toMillimeters() > 0.001
                || isVariothermSurfaceLayer(layer) && layer.cutRestriction() != SurfaceCutRestriction.LAY_DIRECTION_OUTER_CUTS;
    }

    SurfaceLayer repairedSurfaceLayer(SurfaceLayer layer) {
        SurfaceLayoutAnchor repairedAnchor = layer.layoutAnchor() == SurfaceLayoutAnchor.AUTO
                ? SurfaceLayer.anchorFor(layer.layoutRotation(), layer.layoutDirection())
                : layer.layoutAnchor();
        SurfaceCutRestriction repairedCutRestriction = isVariothermSurfaceLayer(layer)
                ? SurfaceCutRestriction.LAY_DIRECTION_OUTER_CUTS
                : layer.cutRestriction();
        return layer.reconfigure(
                layer.name(),
                layer.thickness(),
                layer.tileWidth(),
                layer.tileHeight(),
                layer.layoutMode(),
                layer.layoutOffset(),
                layer.minimumOffset(),
                layer.minimumEdgeWidth(),
                layer.minimumStartEndMargin(),
                layer.freeMargins(),
                repairedAnchor,
                Length.zero(),
                Length.zero(),
                layer.jointWidth(),
                repairedCutRestriction,
                layer.coveringSource(),
                layer.layoutRotatedQuarterTurn()
        );
    }

    SurfaceLayer buildSurfaceLayerFromInputs() {
        return SurfaceLayer.create(
                currentSurfaceLayerName(),
                currentSurfaceLayerThickness(),
                currentStoredSurfaceTileWidth(),
                currentStoredSurfaceTileHeight(),
                currentSurfaceLayoutMode(),
                currentSurfaceLayoutOffset(),
                currentSurfaceMinimumOffset(),
                currentSurfaceMinimumEdgeWidth(),
                currentSurfaceMinimumStartEndMargin(),
                currentSurfaceJointWidth(),
                currentSurfaceCutRestriction(),
                currentSurfaceCoveringSource()
        ).withFreeMargins(currentSurfaceFreeMargins())
                .withLayoutAnchor(currentSurfaceLayoutAnchor())
                .withLayoutRotatedQuarterTurn(currentSurfaceLayoutRotatedQuarterTurn());
    }

    String currentSurfaceLayerName() {
        String name = surfaceLayerNameField.getText();
        return name == null || name.isBlank() ? "Belag" : name.trim();
    }

    Length currentSurfaceLayerThickness() {
        return parseLength(surfaceLayerThicknessField, surfaceLayerThicknessUnit.getValue()).orElse(Length.of(1.2, LengthUnit.CENTIMETER));
    }

    Length currentSurfaceTileWidth() {
        return parseLength(surfaceTileWidthField, surfaceTileWidthUnit.getValue()).orElse(Length.of(60, LengthUnit.CENTIMETER));
    }

    Length currentSurfaceTileHeight() {
        return parseLength(surfaceTileHeightField, surfaceTileHeightUnit.getValue()).orElse(Length.of(30, LengthUnit.CENTIMETER));
    }

    Length currentStoredSurfaceTileWidth() {
        return currentSurfaceTileWidth();
    }

    Length currentStoredSurfaceTileHeight() {
        return currentSurfaceTileHeight();
    }

    void cycleSurfaceLayoutCorner(boolean forward) {
        SurfaceLayoutAnchor currentAnchor = currentSurfaceLayoutAnchor();
        applySurfaceLayoutAnchorSelection(forward ? currentAnchor.nextManual() : currentAnchor.previousManual());
        if (applySurfaceLayoutOrientationToSelectedLayers()) {
            return;
        }
        render();
    }

    void syncSurfaceLayoutAnchorForDirection(SurfaceLayoutDirection direction) {
        if (direction == null) {
            return;
        }
        updateSurfaceLayoutCornerLabel();
    }

    void applySurfaceLayoutAnchorSelection(SurfaceLayoutAnchor anchor) {
        surfaceLayoutAnchorSelection.set(normalizedSurfaceLayoutAnchor(anchor));
        updateSurfaceLayoutCornerLabel();
    }

    void updateSurfaceLayoutCornerLabel() {
        surfaceLayoutCornerLabel.setText(formatSurfaceLayoutCorner(currentSurfaceLayoutAnchor()));
    }

    boolean applySurfaceLayoutOrientationToSelectedLayers() {
        List<SurfaceLayerStack> stacks = currentSurfaceLayerStacks();
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        if (stacks.isEmpty() || selectedIndex < 0) {
            return false;
        }
        self().rememberStateForUndo();
        for (SurfaceLayerStack stack : stacks) {
            SurfaceLayer selectedLayer = stack.layers().get(selectedIndex);
            replaceSurfaceLayer(
                    stack,
                    selectedLayer.id(),
                    selectedLayer.reconfigure(
                            selectedLayer.name(),
                            selectedLayer.thickness(),
                            selectedLayer.tileWidth(),
                            selectedLayer.tileHeight(),
                            selectedLayer.layoutMode(),
                            selectedLayer.layoutOffset(),
                            selectedLayer.minimumOffset(),
                            selectedLayer.minimumEdgeWidth(),
                            selectedLayer.minimumStartEndMargin(),
                            selectedLayer.freeMargins(),
                            currentSurfaceLayoutAnchor(),
                            selectedLayer.startRowTrim(),
                            selectedLayer.startRowWidth(),
                            selectedLayer.jointWidth(),
                            selectedLayer.cutRestriction(),
                            selectedLayer.coveringSource(),
                            currentSurfaceLayoutRotatedQuarterTurn()
                    )
            );
        }
        afterSurfaceLayerMutation("Belag-Startecke angepasst.");
        return true;
    }

    SurfaceLayoutDirection currentSurfaceLayoutDirection() {
        SurfaceLayoutDirection selection = surfaceLayoutDirectionSelector.getValue();
        if (selection != null) {
            return selection;
        }
        return SurfaceLayoutDirection.LEFT_TO_RIGHT;
    }

    boolean currentSurfaceLayoutRotatedQuarterTurn() {
        return surfaceLayoutRotatedQuarterTurn(currentSurfaceLayoutAnchor(), currentSurfaceLayoutDirection());
    }

    de.schrell.cadas.domain.model.SurfaceLayoutAnchor currentSurfaceLayoutAnchor() {
        return Optional.ofNullable(surfaceLayoutAnchorSelection.get()).orElse(SurfaceLayoutAnchor.MIN_X_MIN_Y);
    }

    SurfaceLayoutMode currentSurfaceLayoutMode() {
        return Optional.ofNullable(surfaceLayoutModeSelector.getValue()).orElse(SurfaceLayoutMode.AUTOMATIC);
    }

    Length currentSurfaceLayoutOffset() {
        return parseLength(surfaceLayoutOffsetField, surfaceLayoutOffsetUnit.getValue()).orElse(Length.zero());
    }

    Length currentSurfaceMinimumOffset() {
        return parseLength(surfaceMinimumOffsetField, surfaceMinimumOffsetUnit.getValue()).orElse(Length.zero());
    }

    Length currentSurfaceMinimumEdgeWidth() {
        return parseLength(surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit.getValue()).orElse(Length.zero());
    }

    Length currentSurfaceMinimumStartEndMargin() {
        return parseLength(surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit.getValue()).orElse(Length.zero());
    }

    SurfaceLayoutMargins currentSurfaceFreeMargins() {
        return new SurfaceLayoutMargins(
                parseLength(surfaceFreeMarginLeftField, surfaceFreeMarginLeftUnit.getValue()).orElse(Length.zero()),
                parseLength(surfaceFreeMarginRightField, surfaceFreeMarginRightUnit.getValue()).orElse(Length.zero()),
                parseLength(surfaceFreeMarginTopField, surfaceFreeMarginTopUnit.getValue()).orElse(Length.zero()),
                parseLength(surfaceFreeMarginBottomField, surfaceFreeMarginBottomUnit.getValue()).orElse(Length.zero())
        );
    }

    Length currentSurfaceJointWidth() {
        return parseLength(surfaceJointWidthField, surfaceJointWidthUnit.getValue()).orElse(Length.ofMillimeters(2));
    }

    SurfaceCutRestriction currentSurfaceCutRestriction() {
        return Optional.ofNullable(surfaceCutRestrictionSelector.getValue()).orElse(SurfaceCutRestriction.fallback());
    }

    String currentSurfaceCoveringSource() {
        return Optional.ofNullable(surfacePresetSelector.getValue())
                .map(SurfaceCoveringPreset::coveringSource)
                .orElse("");
    }

    Optional<Path> currentDwgLibraryPath() {
        return Optional.ofNullable(dwgBlockSelector.getValue())
                .map(DwgBlockDefinition::sourceFile)
                .or(() -> Optional.ofNullable(surfacePresetSelector.getValue())
                .map(SurfaceCoveringPreset::coveringSource)
                .flatMap(CadWorkbenchCoveringSourceSupport::extractDwgLibraryPath))
                .or(() -> cadLibraryReferences.stream()
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dwg"))
                        .findFirst())
                .map(path -> path.toAbsolutePath().normalize());
    }

    Optional<SurfaceLayer> selectedSurfaceLayer() {
        SurfaceLayerStack stack = currentDisplaySurfaceLayerStack().orElse(null);
        int selectedIndex = surfaceLayerList.getSelectionModel().getSelectedIndex();
        if (stack == null || selectedIndex < 0 || selectedIndex >= stack.layers().size()) {
            return Optional.empty();
        }
        return Optional.of(stack.layers().get(selectedIndex));
    }

    SurfaceType currentSurfaceType() {
        List<SurfaceType> availableTypes = availableSurfaceTypesForSelection();
        if (availableTypes.isEmpty()) {
            return SurfaceType.WALL_INTERIOR;
        }
        SurfaceType selectedType = surfaceTypeSelector.getValue();
        if (selectedType != null && availableTypes.contains(selectedType)) {
            return selectedType;
        }
        if (availableTypes.contains(preferredRoomSurfaceType)) {
            return preferredRoomSurfaceType;
        }
        return availableTypes.getFirst();
    }

    Optional<Room> selectedRoom() {
        if (selectedSelection.get() == null) {
            return Optional.empty();
        }
        Optional<HeatingZoneContext> zoneContext = selectedHeatingZoneContext();
        if (zoneContext.isPresent()) {
            return Optional.of(zoneContext.orElseThrow().room());
        }
        Optional<HeatingContext> heatingContext = selectedHeatingContext();
        if (heatingContext.isPresent()) {
            return Optional.of(heatingContext.orElseThrow().room());
        }
        if (selectedSelection.get().kind() != RenderableKind.ROOM_VOLUME
                && selectedSelection.get().kind() != RenderableKind.ROOM_FLOOR
                && selectedSelection.get().kind() != RenderableKind.ROOM_CEILING) {
            return Optional.empty();
        }
        return activeLevel.get().rooms().stream()
                .filter(room -> room.id().toString().equals(selectedSelection.get().elementId()))
                .findFirst();
    }

    Optional<HeatingZoneContext> selectedHeatingZoneContext() {
        if (selectedSelection.get() == null || selectedSelection.get().kind() != RenderableKind.HEATING_ZONE) {
            return Optional.empty();
        }
        return heatingZoneContext(UUID.fromString(selectedSelection.get().elementId()));
    }

    Optional<HeatingContext> selectedHeatingContext() {
        if (selectedSelection.get() == null || selectedSelection.get().kind() != RenderableKind.HEATING_MANIFOLD) {
            return Optional.empty();
        }
        UUID heatingId = UUID.fromString(selectedSelection.get().elementId());
        return activeLevel.get().hydronicHeatings().stream()
                .filter(heating -> heating.id().equals(heatingId))
                .findFirst()
                .flatMap(heating -> activeLevel.get().rooms().stream()
                        .filter(room -> room.id().equals(heating.roomId()))
                        .findFirst()
                        .map(room -> new HeatingContext(room, heating)));
    }

    Optional<HeatingZoneContext> contextHeatingZoneContext() {
        if (contextMenuSelection == null || contextMenuSelection.kind() != RenderableKind.HEATING_ZONE) {
            return Optional.empty();
        }
        return heatingZoneContext(UUID.fromString(contextMenuSelection.elementId()));
    }

    Optional<HeatingZoneContext> heatingZoneContext(UUID zoneId) {
        for (HydronicHeating heating : activeLevel.get().hydronicHeatings()) {
            for (int zoneIndex = 0; zoneIndex < heating.zones().size(); zoneIndex++) {
                HeatingZone zone = heating.zones().get(zoneIndex);
                if (zone.id().equals(zoneId)) {
                    int currentZoneIndex = zoneIndex;
                    return activeLevel.get().rooms().stream()
                            .filter(room -> room.id().equals(heating.roomId()))
                            .findFirst()
                            .map(room -> new HeatingZoneContext(room, heating, zone, currentZoneIndex));
                }
            }
        }
        return Optional.empty();
    }

    List<SurfaceType> availableSurfaceTypesForSelection() {
        if (selectedFloorExtension().isPresent()) {
            return List.of(SurfaceType.FLOOR);
        }
        List<Wall> walls = selectedWalls();
        boolean hasWalls = !walls.isEmpty();
        boolean hasSingleRoom = selectedSurfaceRoom().isPresent();
        if (hasWalls && hasSingleRoom) {
            return List.of(SurfaceType.WALL_INTERIOR);
        }
        if (hasWalls) {
            boolean hasExteriorSide = walls.stream().anyMatch(wall -> wallSurfaceSideService.hasExteriorSide(activeLevel.get(), wall));
            boolean hasInteriorSide = walls.stream().anyMatch(wall -> !interiorWallTargetKeys(List.of(wall)).isEmpty());
            if (hasExteriorSide && hasInteriorSide) {
                return List.of(SurfaceType.WALL_INTERIOR, SurfaceType.WALL_EXTERIOR);
            }
            if (hasInteriorSide) {
                return List.of(SurfaceType.WALL_INTERIOR);
            }
            return List.of(SurfaceType.WALL_EXTERIOR);
        }
        if (hasSingleRoom) {
            return List.of(SurfaceType.FLOOR, SurfaceType.CEILING);
        }
        return List.of();
    }

    void refreshSurfaceTypeSelector() {
        List<SurfaceType> availableTypes = availableSurfaceTypesForSelection();
        SurfaceType currentValue = surfaceTypeSelector.getValue();
        if (!surfaceTypeSelector.getItems().equals(availableTypes)) {
            surfaceTypeSelector.getItems().setAll(availableTypes);
        }
        SurfaceType preferredType = availableTypes.contains(preferredRoomSurfaceType)
                ? preferredRoomSurfaceType
                : availableTypes.stream().findFirst().orElse(null);
        SurfaceType nextValue = currentValue != null && availableTypes.contains(currentValue) ? currentValue : preferredType;
        if (surfaceTypeSelector.getValue() != nextValue) {
            surfaceTypeSelector.setValue(nextValue);
        }
        surfaceTypeSelector.setDisable(availableTypes.size() <= 1);
    }

    Optional<Room> selectedSurfaceRoom() {
        List<String> roomIds = selectedSelections.stream()
                .filter(selection -> selection.kind() == RenderableKind.ROOM_VOLUME
                        || selection.kind() == RenderableKind.ROOM_FLOOR
                        || selection.kind() == RenderableKind.ROOM_CEILING)
                .map(SelectionKey::elementId)
                .distinct()
                .toList();
        if (roomIds.size() != 1) {
            return Optional.empty();
        }
        return activeLevel.get().rooms().stream()
                .filter(room -> room.id().toString().equals(roomIds.getFirst()))
                .findFirst();
    }

    List<Wall> selectedWalls() {
        Set<String> wallIds = selectedSelections.stream()
                .filter(selection -> selection.kind() == RenderableKind.WALL)
                .map(SelectionKey::elementId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (wallIds.isEmpty()) {
            return List.of();
        }
        return activeLevel.get().walls().stream()
                .filter(wall -> wallIds.contains(wall.id().toString()))
                .toList();
    }

    Optional<SurfaceSelectionContext> currentSurfaceSelectionContext() {
        SurfaceType surfaceType = currentSurfaceType();
        Optional<FloorExtension> floorExtension = selectedFloorExtension();
        if (floorExtension.isPresent()) {
            FloorExtension extension = floorExtension.orElseThrow();
            return Optional.of(new SurfaceSelectionContext(
                    SurfaceType.FLOOR,
                    List.of(extension.surfaceTargetKey()),
                    "Fläche: Oberseite " + extension.type(),
                    "Beläge liegen oberhalb der Fußbodenplatte des ausgewählten Elements."
            ));
        }
        if (surfaceType == SurfaceType.WALL_INTERIOR || surfaceType == SurfaceType.WALL_EXTERIOR) {
            List<Wall> walls = selectedWalls();
            if (walls.isEmpty()) {
                return Optional.empty();
            }
            if (surfaceType == SurfaceType.WALL_INTERIOR) {
                Optional<Room> room = selectedSurfaceRoom();
                if (room.isPresent()) {
                    return Optional.of(new SurfaceSelectionContext(
                            surfaceType,
                            walls.stream().map(wall -> WallSurfaceTargetKey.interior(wall.id(), room.get().id())).toList(),
                            "Fläche: Innenwand auf Raum `" + room.get().name() + "` und " + walls.size() + " Wand/Wände",
                            "Innenwand-Beläge werden aus dem ausgewählten Raum auf die angrenzende Wandseite gelegt."
                    ));
                }
                List<String> targetKeys = interiorWallTargetKeys(walls);
                if (targetKeys.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(new SurfaceSelectionContext(
                        surfaceType,
                        targetKeys,
                        "Fläche: Innenwand auf " + walls.size() + " Wand/Wände",
                        "Innenwand-Beläge werden auf alle angrenzenden Raumseiten der ausgewählten Wände gelegt."
                ));
            }
            if (selectedSurfaceRoom().isPresent()) {
                return Optional.empty();
            }
            return Optional.of(new SurfaceSelectionContext(
                    surfaceType,
                    walls.stream().map(wall -> wall.id().toString()).toList(),
                    "Fläche: Außenwand auf " + walls.size() + " Wand/Wände",
                    "Außenwand-Beläge werden nur auf raumfreie Wandseiten gelegt."
            ));
        }
        Optional<Room> room = selectedSurfaceRoom();
        if (room.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SurfaceSelectionContext(
                surfaceType,
                List.of(room.get().id().toString()),
                "Fläche: " + (surfaceType == SurfaceType.CEILING ? "Decke" : "Boden") + " auf Raum `" + room.get().name() + "`",
                surfaceType == SurfaceType.CEILING
                        ? "Deckenbeläge wirken auf die Unterseite der Raumdecke."
                        : "Bodenbeläge liegen oberhalb des Rohbodens innerhalb des ausgewählten Raums."
        ));
    }

    Optional<FloorExtension> selectedFloorExtension() {
        if (selectedSelections.size() != 1 || selectedSelection.get() == null
                || selectedSelection.get().kind() != RenderableKind.FLOOR_EXTENSION) {
            return Optional.empty();
        }
        return activeLevel.get().floorExtensions().stream()
                .filter(extension -> extension.id().toString().equals(selectedSelection.get().elementId()))
                .findFirst();
    }

    String currentSurfaceSelectionHint() {
        return switch (currentSurfaceType()) {
            case WALL_INTERIOR -> "Für Innenwand-Beläge Raum und Wand auswählen oder eine Wand mit angrenzendem Raum direkt wählen.";
            case WALL_EXTERIOR -> "Für Außenwand-Beläge eine oder mehrere Wände ohne Raumauswahl auswählen.";
            case FLOOR, CEILING -> "Für Boden- oder Deckenbeläge genau einen Raum auswählen.";
            default -> "Keine passende Fläche ausgewählt.";
        };
    }

    Optional<SurfaceLayerStack> currentDisplaySurfaceLayerStack() {
        List<SurfaceLayerStack> stacks = currentSurfaceLayerStacks();
        if (stacks.isEmpty()) {
            return Optional.empty();
        }
        if (stacks.size() == 1) {
            return Optional.of(stacks.getFirst());
        }
        SurfaceLayerStack reference = stacks.getFirst();
        boolean allSelectedWallsCovered = currentSurfaceSelectionContext()
                .map(context -> context.targetKeys().size() == stacks.size())
                .orElse(false);
        if (!allSelectedWallsCovered) {
            return Optional.empty();
        }
        boolean equalSequence = stacks.stream()
                .skip(1)
                .allMatch(stack -> surfaceLayerConsistencyService.haveEqualSequence(reference, stack));
        return equalSequence ? Optional.of(reference) : Optional.empty();
    }

    List<SurfaceLayerStack> currentSurfaceLayerStacks() {
        return currentSurfaceSelectionContext()
                .map(context -> context.targetKeys().stream()
                        .map(targetKey -> activeLevel.get().findSurfaceLayerStack(context.surfaceType(), targetKey))
                        .filter(stack -> stack != null)
                        .toList())
                .orElseGet(List::of);
    }

    boolean isSelectedSurfaceLayer(SurfaceLayerStack stack, SurfaceLayer layer) {
        SurfaceLayerStack selectedStack = currentDisplaySurfaceLayerStack().orElse(null);
        SurfaceLayer selectedLayer = selectedSurfaceLayer().orElse(null);
        return selectedStack != null
                && selectedLayer != null
                && selectedStack.surfaceType() == stack.surfaceType()
                && selectedStack.targetKey().equals(stack.targetKey())
                && selectedLayer.id().equals(layer.id());
    }

    boolean isVisibleSurfaceLayer(SurfaceLayer layer) {
        return layer.visible() && layer.thickness().toMillimeters() > 0.0;
    }

    boolean isVariothermSurfaceLayer(SurfaceLayer layer) {
        return SurfaceCoveringPresetService.VARIOTHERM_DRY_PANEL_SOURCE.equals(layer.coveringSource());
    }

    boolean validateSurfaceLayerSelection(SurfaceSelectionContext context) {
        if (context.surfaceType() == SurfaceType.WALL_INTERIOR) {
            Optional<Room> room = selectedSurfaceRoom();
            boolean invalidWallSelected = room
                    .map(selectedRoom -> selectedWalls().stream()
                            .anyMatch(wall -> !wallSurfaceSideService.hasInteriorSide(activeLevel.get(), wall, selectedRoom.id())))
                    .orElseGet(() -> selectedWalls().stream()
                            .anyMatch(wall -> interiorWallTargetKeys(List.of(wall)).isEmpty()));
            if (invalidWallSelected) {
                showSurfaceLayerError(
                        "Innenwand-Belag kann nicht angelegt werden.",
                        "Der ausgewählte Raum grenzt nicht an alle ausgewählten Wände."
                );
                return false;
            }
        }
        if (context.surfaceType() == SurfaceType.WALL_EXTERIOR) {
            boolean invalidWallSelected = selectedWalls().stream()
                    .anyMatch(wall -> !wallSurfaceSideService.hasExteriorSide(activeLevel.get(), wall));
            if (invalidWallSelected) {
                showSurfaceLayerError(
                        "Außenwand-Belag kann nicht angelegt werden.",
                        "Mindestens eine ausgewählte Wand hat keine raumfreie Seite. Wähle für Innenwände zusätzlich den passenden Raum aus."
                );
                return false;
            }
        }
        return true;
    }

    List<String> interiorWallTargetKeys(List<Wall> walls) {
        return walls.stream()
                .flatMap(wall -> activeLevel.get().rooms().stream()
                        .filter(room -> wallSurfaceSideService.hasInteriorSide(activeLevel.get(), wall, room.id()))
                        .map(room -> WallSurfaceTargetKey.interior(wall.id(), room.id())))
                .distinct()
                .toList();
    }

    void showSurfaceLayerError(String header, String content) {
        draftLabel.setText(header + ": " + content);
        if (!interactiveDialogsEnabled) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
        alert.setTitle("Belag");
        alert.setHeaderText(header);
        alert.getDialogPane().setPrefWidth(520);
        Window window = getScene() != null ? getScene().getWindow() : null;
        if (window != null) {
            alert.initOwner(window);
        }
        alert.showAndWait();
    }

    void replaceSurfaceLayer(SurfaceLayerStack stack, UUID layerId, SurfaceLayer replacement) {
        stack.replaceLayer(layerId, replacement);
    }

    PlanPoint screenToWorld(double screenX, double screenY) {
        return new PlanPoint((screenX - offsetX) / scale(), (screenY - offsetY) / scale());
    }

    double toScreenX(double worldMillimeters) {
        return offsetX + worldMillimeters * scale();
    }

    double toScreenY(double worldMillimeters) {
        return offsetY + worldMillimeters * scale();
    }

    double toScreenProjectedX(PlanPoint point, double heightMillimeters) {
        return toScreenHorizontal(projectHorizontal(point, heightMillimeters));
    }

    double toScreenProjectedY(PlanPoint point, double heightMillimeters) {
        return toScreenVertical(projectVertical(point, heightMillimeters));
    }

    double toScreenHorizontal(double projectedMillimeters) {
        return offsetX + projectedMillimeters * scale();
    }

    double toScreenVertical(double projectedMillimeters) {
        return offsetY + projectedMillimeters * scale();
    }

    double projectHorizontal(PlanPoint point, double heightMillimeters) {
        return ViewProjectionService.project(point, heightMillimeters, activeView.get()).horizontalMillimeters();
    }

    double projectVertical(PlanPoint point, double heightMillimeters) {
        return ViewProjectionService.project(point, heightMillimeters, activeView.get()).verticalMillimeters();
    }

    double scale() {
        return BASE_PIXELS_PER_MILLIMETER * zoom;
    }

    boolean isDirectEditingView() {
        return activeView.get() == ViewOrientation.TOP;
    }

}
