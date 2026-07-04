package de.schrell.cadas.ui;

import de.schrell.cadas.application.drawing.DraftingConstraints;
import de.schrell.cadas.application.drawing.EdgeResizeService;
import de.schrell.cadas.application.drawing.WallEndpointSelection;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.FloorExtensionPlacement;
import de.schrell.cadas.domain.model.FloorExtensionType;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.Cursor;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

abstract class CadWorkbenchInteraction extends CadWorkbenchUi {

    void handleMousePressed(MouseEvent event) {
        drawingCanvas.requestFocus();
        if (event.getButton() == MouseButton.SECONDARY && event.isAltDown()) {
            removeNearestGuide(screenToWorld(event.getX(), event.getY()));
            return;
        }

        if (event.getButton() == MouseButton.SECONDARY
                || event.getButton() == MouseButton.MIDDLE
                || event.getButton() == MouseButton.PRIMARY && spacePressed) {
            panning = true;
            panningMoved = false;
            pendingContextSelection = event.getButton() == MouseButton.SECONDARY && currentTool() == DrawingTool.EDIT
                    ? contextSelectionAt(event)
                    : null;
            pendingContextWorldPoint = pendingContextSelection == null
                    ? null
                    : screenToWorld(event.getX(), event.getY());
            panStartX = event.getX();
            panStartY = event.getY();
            panOriginX = offsetX;
            panOriginY = offsetY;
            updateMouseCursor();
            return;
        }

        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        if (!isDirectEditingView()) {
            render();
            draftLabel.setText("Direktes Zeichnen und Bearbeiten ist aktuell nur in der Draufsicht verfügbar.");
            return;
        }

        if (currentTool() == DrawingTool.EDIT) {
            PlanPoint rawEditPoint = screenToWorld(event.getX(), event.getY());
            activeEdgeHandle = edgeResizeService.findHandle(
                    activeLevel.get(),
                    Set.copyOf(selectedSelections),
                    rawEditPoint,
                    Length.ofMillimeters(8.0 / scale())
            ).orElse(null);
            if (activeEdgeHandle != null) {
                edgeResizeBaseWalls = List.copyOf(activeLevel.get().walls());
                edgeResizeBaseDoors = List.copyOf(activeLevel.get().doors());
                edgeResizeBaseWindows = List.copyOf(activeLevel.get().windows());
                edgeResizeBaseStaircases = List.copyOf(activeLevel.get().staircases());
                edgeResizeBaseFloorOpenings = List.copyOf(activeLevel.get().floorOpenings());
                edgeResizeBaseHeatingExclusionAreas = List.copyOf(activeLevel.get().heatingExclusionAreas());
                edgeResizeBaseHydronicHeatings = List.copyOf(activeLevel.get().hydronicHeatings());
                selectedEndpointGroup = null;
                selectionDragAnchor = null;
                openingDragId = null;
                historyCapturedForDrag = false;
                draftLabel.setText("Kanten-Handle ausgewählt: Ziehen verlängert oder kürzt das Bauteil entlang seiner Wandachse.");
                render();
                return;
            }
            // Für die Selektion wird der reine Klickpunkt verwendet, nicht das gerasterte Ergebnis.
            PlanPoint editPoint = rawEditPoint;
            selectedEndpointGroup = wallEditingService.findConnectedEndpoint(activeLevel.get().walls(), editPoint, pointerSelectionTolerance()).orElse(null);
            selectionDragAnchor = null;
            clearSelectionRectangle();
            selectionDragBaseWalls = List.of();
            selectionDragBaseStaircases = List.of();
            selectionDragBaseRoomObjects = List.of();
            selectionDragBaseFloorOpenings = List.of();
            selectionDragBaseHeatingExclusionAreas = List.of();
            selectionDragBaseHydronicHeatings = List.of();
            historyCapturedForDrag = false;
            if (selectedEndpointGroup != null) {
                syncEndpointHeightInputFromSelection();
                preferredEndpointWallSelection(editPoint, selectedEndpointGroup)
                        .ifPresent(selection -> updateSelection(
                                selection,
                                event.isShortcutDown() || event.isShiftDown()
                        ));
                draftLabel.setText("Wandecke ausgewählt. `Eckhöhe anwenden` setzt die Höhe auf alle verbundenen Wandenden.");
            } else {
                SelectionKey editSelection = editSelectionAt(editPoint, event.isAltDown());
                updateSelection(editSelection, event.isShortcutDown() || event.isShiftDown());
                if (shouldStartSelectionRectangle(editSelection)) {
                    selectionRectangleStart = editPoint;
                    selectionRectangleEnd = editPoint;
                    selectionRectangleToggle = event.isShortcutDown() || event.isShiftDown();
                } else {
                    clearSelectionRectangle();
                }
                prepareSelectionDrag(editSelection, editPoint);
                openingDragId = null;
                openingDragWallAxis = null;
                openingDragWidth = 0;
                openingDragOffsetDelta = 0;
                boolean onlyDoorOrWindow = selectedSelections.stream().allMatch(
                        s -> s.kind() == RenderableKind.DOOR || s.kind() == RenderableKind.WINDOW);
                if (onlyDoorOrWindow && editSelection != null && (editSelection.kind() == RenderableKind.DOOR || editSelection.kind() == RenderableKind.WINDOW)) {
                    UUID elementId = UUID.fromString(editSelection.elementId());
                    if (editSelection.kind() == RenderableKind.DOOR) {
                        activeLevel.get().doors().stream()
                                .filter(door -> door.id().equals(elementId))
                                .findFirst()
                                .ifPresent(door -> {
                                    Wall wall = activeLevel.get().findWall(door.wallId());
                                    openingDragId = door.id();
                                    openingDragWallAxis = wall.axis();
                                    openingDragWidth = door.width().toMillimeters();
                                    openingDragOffsetDelta = door.offsetFromStart().toMillimeters() - openingDragWallAxis.projectedLength(editPoint).toMillimeters();
                                });
                    } else {
                        activeLevel.get().windows().stream()
                                .filter(window -> window.id().equals(elementId))
                                .findFirst()
                                .ifPresent(window -> {
                                    Wall wall = activeLevel.get().findWall(window.wallId());
                                    openingDragId = window.id();
                                    openingDragWallAxis = wall.axis();
                                    openingDragWidth = window.width().toMillimeters();
                                    openingDragOffsetDelta = window.offsetFromStart().toMillimeters() - openingDragWallAxis.projectedLength(editPoint).toMillimeters();
                                });
                    }
                }
            }
            render();
            return;
        }

        DraftingConstraints constraints = currentConstraints(currentTool() == DrawingTool.WALL && !event.isShiftDown());
        PlanPoint rawStart = screenToWorld(event.getX(), event.getY());
        draftStart = currentTool() == DrawingTool.HEATING_ZONE_RECTANGLE
                ? rawStart
                : snapDrawingPoint(rawStart, constraints);
        previewSegment = new PlanSegment(draftStart, draftStart);
        if (currentTool() == DrawingTool.DOOR) {
            placeDoor(draftStart);
            draftStart = null;
            previewSegment = null;
        } else if (currentTool() == DrawingTool.WINDOW) {
            placeWindow(draftStart);
            draftStart = null;
            previewSegment = null;
        } else if (currentTool() == DrawingTool.ROOF_WINDOW) {
            placeRoofWindow(draftStart);
            draftStart = null;
            previewSegment = null;
        } else if (currentTool() == DrawingTool.OBJECT) {
            placeRoomObject(draftStart);
            draftStart = null;
            previewSegment = null;
        }
        render();
    }

    SelectionKey editSelectionAt(PlanPoint editPoint, boolean cycleSelection) {
        List<SelectionKey> candidates = selectionQueryService.findSelections(activeLevel.get(), editPoint, pointerSelectionTolerance());
        if (candidates.isEmpty()) {
            return null;
        }
        if (!cycleSelection || candidates.size() == 1) {
            return candidates.getFirst();
        }
        SelectionKey currentSelection = selectedSelection.get();
        int currentIndex = candidates.indexOf(currentSelection);
        SelectionKey nextSelection = candidates.get((currentIndex + 1) % candidates.size());
        draftLabel.setText("Auswahl umgeschaltet: " + selectionLabel(nextSelection) + ".");
        return nextSelection;
    }

    Optional<SelectionKey> preferredEndpointWallSelection(PlanPoint editPoint, WallEndpointSelection endpointSelection) {
        Set<String> endpointWallIds = java.util.stream.Stream.concat(
                endpointSelection.startWallIds().stream(),
                endpointSelection.endWallIds().stream()
        ).map(UUID::toString).collect(java.util.stream.Collectors.toSet());
        Optional<SelectionKey> hitWall = selectionQueryService.findSelections(activeLevel.get(), editPoint, pointerSelectionTolerance()).stream()
                .filter(selection -> selection.kind() == RenderableKind.WALL)
                .filter(selection -> endpointWallIds.contains(selection.elementId()))
                .findFirst();
        if (hitWall.isPresent()) {
            return hitWall;
        }
        return activeLevel.get().walls().stream()
                .filter(wall -> endpointWallIds.contains(wall.id().toString()))
                .findFirst()
                .map(wall -> new SelectionKey(RenderableKind.WALL, activeLevel.get().name(), wall.id().toString()));
    }

    void handleMouseDragged(MouseEvent event) {
        if (panning) {
            panningMoved |= Math.hypot(event.getX() - panStartX, event.getY() - panStartY) > 3.0;
            offsetX = panOriginX + (event.getX() - panStartX);
            offsetY = panOriginY + (event.getY() - panStartY);
            render();
            updateMouseCursor();
            return;
        }

        if (draftStart == null) {
            if (selectionRectangleStart != null) {
                selectionRectangleEnd = screenToWorld(event.getX(), event.getY());
                render();
                return;
            }
            if (activeEdgeHandle != null) {
                if (!historyCapturedForDrag) {
                    rememberStateForUndo();
                    historyCapturedForDrag = true;
                }
                Level baseLevel = new Level(activeLevel.get().name());
                baseLevel.replaceWalls(edgeResizeBaseWalls);
                baseLevel.replaceDoors(edgeResizeBaseDoors);
                baseLevel.replaceWindows(edgeResizeBaseWindows);
                baseLevel.replaceStaircases(edgeResizeBaseStaircases);
                baseLevel.replaceFloorOpenings(edgeResizeBaseFloorOpenings);
                baseLevel.replaceHeatingExclusionAreas(edgeResizeBaseHeatingExclusionAreas);
                baseLevel.replaceHydronicHeatings(edgeResizeBaseHydronicHeatings);
                boolean isWallHandle = activeEdgeHandle.kind() == EdgeResizeService.EdgeHandleKind.WALL_START
                        || activeEdgeHandle.kind() == EdgeResizeService.EdgeHandleKind.WALL_END;
                Set<UUID> excludedWallIds = isWallHandle ? Set.of(activeEdgeHandle.hostWallId()) : Set.of();
                List<Wall> snapWalls = isWallHandle
                        ? edgeResizeBaseWalls.stream()
                                .filter(wall -> !wall.id().equals(activeEdgeHandle.hostWallId()))
                                .toList()
                        : edgeResizeBaseWalls;
                boolean heatingZoneHandle = isHeatingZoneHandle(activeEdgeHandle);
                PlanPoint rawPoint = screenToWorld(event.getX(), event.getY());
                PlanPoint resizePoint = heatingZoneHandle
                        ? rawPoint
                        : snapService.snap(
                                rawPoint,
                                currentConstraints(false),
                                snapWalls,
                                currentAlignmentSnapTargets(excludedWallIds)
                        );
                EdgeResizeService.ResizeOptions resizeOptions = heatingZoneHandle
                        ? new EdgeResizeService.ResizeOptions(
                                false,
                                snapToGrid.get() ? currentGrid() : null
                        )
                        : EdgeResizeService.ResizeOptions.defaults();
                EdgeResizeService.ResizeResult result = edgeResizeService.resize(baseLevel, activeEdgeHandle, resizePoint, resizeOptions);
                activeLevel.get().replaceWalls(result.walls());
                activeLevel.get().replaceDoors(result.doors());
                activeLevel.get().replaceWindows(result.windows());
                activeLevel.get().replaceStaircases(result.staircases());
                activeLevel.get().replaceFloorOpenings(result.floorOpenings());
                activeLevel.get().replaceHeatingExclusionAreas(result.heatingExclusionAreas());
                activeLevel.get().replaceHydronicHeatings(result.hydronicHeatings());
                List<Staircase> resizedStaircasesWithUnderbuild = result.staircases().stream()
                        .filter(staircase -> staircase.leftUnderbuildWidth().toMillimeters() > 0.0
                                || staircase.rightUnderbuildWidth().toMillimeters() > 0.0)
                        .toList();
                resizedStaircasesWithUnderbuild.forEach(this::synchronizeStairUnderbuild);
                if (isWallHandle || !resizedStaircasesWithUnderbuild.isEmpty()) {
                    previewRoomSynchronizationFromWalls(activeLevel.get());
                }
                markThreeDDirty();
                render();
                return;
            }
            if (selectedEndpointGroup != null) {
                if (!historyCapturedForDrag) {
                    rememberStateForUndo();
                    historyCapturedForDrag = true;
                }
                DraftingConstraints constraints = currentConstraints(false);
                Set<UUID> endpointWallIds = java.util.stream.Stream.concat(
                        selectedEndpointGroup.startWallIds().stream(),
                        selectedEndpointGroup.endWallIds().stream()
                ).collect(java.util.stream.Collectors.toSet());
                PlanPoint snappedPoint = snapService.snap(
                        screenToWorld(event.getX(), event.getY()),
                        constraints,
                        activeLevel.get().walls(),
                        currentAlignmentSnapTargets(endpointWallIds)
                );
                activeLevel.get().replaceWalls(wallEditingService.moveEndpointGroup(
                        activeLevel.get().walls(),
                        selectedEndpointGroup,
                        snappedPoint,
                        !event.isShiftDown()
                ));
                previewRoomSynchronizationFromWalls(activeLevel.get());
                markThreeDDirty();
                render();
            }
            if (openingDragId != null) {
                if (!historyCapturedForDrag) {
                    rememberStateForUndo();
                    historyCapturedForDrag = true;
                }
                DraftingConstraints constraints = currentConstraints(false);
                PlanPoint snappedPoint = snapService.snap(
                        screenToWorld(event.getX(), event.getY()),
                        constraints,
                        activeLevel.get().walls(),
                        currentGuideSnapTargets()
                );
                double wallLength = openingDragWallAxis.length().toMillimeters();
                double rawOffset = openingDragWallAxis.projectedLength(snappedPoint).toMillimeters() + openingDragOffsetDelta;
                double clampedOffset = Math.max(0.0, Math.min(wallLength - openingDragWidth, rawOffset));
                Wall openingWall = openingDragWall();
                Length newOffset = snapToGuides.get()
                        ? guideSnapService.snapOpeningOffset(
                                openingWall,
                                Length.ofMillimeters(clampedOffset),
                                Length.ofMillimeters(openingDragWidth),
                                currentGuideSnapTargets(),
                                SNAP_TOLERANCE
                        )
                        : Length.ofMillimeters(clampedOffset);
                activeLevel.get().replaceDoors(activeLevel.get().doors().stream()
                        .map(door -> door.id().equals(openingDragId) ? door.withOffset(newOffset) : door)
                        .toList());
                activeLevel.get().replaceWindows(activeLevel.get().windows().stream()
                        .map(window -> window.id().equals(openingDragId) ? window.withOffset(newOffset) : window)
                        .toList());
                previewRoomSynchronizationFromWalls(activeLevel.get());
                markThreeDDirty();
                render();
                return;
            }
            if (selectionDragAnchor != null) {
                if (!historyCapturedForDrag) {
                    rememberStateForUndo();
                    historyCapturedForDrag = true;
                }
                PlanPoint rawPoint = screenToWorld(event.getX(), event.getY());
                PlanPoint dragPoint = hasSelectedHeatingZone()
                        ? rawPoint
                        : snapService.snap(rawPoint, currentConstraints(false), activeLevel.get().walls());
                translateSelectedComponents(dragPoint);
                render();
            }
            return;
        }

        if (currentTool().isPointTool() && currentTool() != DrawingTool.HEATING_MANIFOLD) {
            return;
        }

        DraftingConstraints constraints = currentConstraints(currentTool() == DrawingTool.WALL && !event.isShiftDown());
        PlanPoint rawPoint = screenToWorld(event.getX(), event.getY());
        if (currentTool() == DrawingTool.HEATING_ZONE_RECTANGLE) {
            previewSegment = new PlanSegment(draftStart, rawPoint);
        } else {
            PlanPoint snappedPoint = snapDrawingPoint(rawPoint, constraints);
            previewSegment = draftingService.createSegment(draftStart, snappedPoint, constraints);
        }
        if (currentTool() != DrawingTool.HEATING_ZONE_RECTANGLE
                && (snapToGuides.get() || snapToWalls.get())
                && constraints.manualLength().isEmpty()
                && constraints.manualAngle().isEmpty()) {
            previewSegment = guideSnapService.snapWallSegment(
                    previewSegment,
                    currentWallThickness(),
                    currentAlignmentSnapTargets(Set.of()),
                    SNAP_TOLERANCE
            );
            draftStart = previewSegment.start();
        }
        lastCursor = previewSegment.end();
        updateStatus();
        render();
    }

    void handleMouseReleased(MouseEvent event) {
        if (panning) {
            panning = false;
            if (!panningMoved && event.getButton() == MouseButton.SECONDARY) {
                if (pendingContextSelection != null) {
                    contextMenuSelection = pendingContextSelection;
                    contextMenuWorldPoint = pendingContextWorldPoint;
                    selectSingle(pendingContextSelection);
                    selectionContextMenu.show(drawingCanvas, event.getScreenX(), event.getScreenY());
                } else if (handleTerrainBandContextClick(event)) {
                    pendingContextSelection = null;
                    pendingContextWorldPoint = null;
                    updateMouseCursor();
                    return;
                }
            }
            pendingContextSelection = null;
            pendingContextWorldPoint = null;
            updateMouseCursor();
            return;
        }

        if (activeEdgeHandle != null) {
            EdgeResizeService.EdgeHandle releasedHandle = activeEdgeHandle;
            activeEdgeHandle = null;
            edgeResizeBaseWalls = List.of();
            edgeResizeBaseDoors = List.of();
            edgeResizeBaseWindows = List.of();
            edgeResizeBaseStaircases = List.of();
            edgeResizeBaseFloorOpenings = List.of();
            edgeResizeBaseHeatingExclusionAreas = List.of();
            edgeResizeBaseHydronicHeatings = List.of();
            boolean hadDrag = historyCapturedForDrag;
            historyCapturedForDrag = false;
            if (isHeatingZoneHandle(releasedHandle) || releasedHandle.elementKind() == RenderableKind.HEATING_MANIFOLD) {
                if (isHeatingZoneHandle(releasedHandle)) {
                    if (autoRouteHeatingZoneOnResize.get()) {
                        heatingZonesPendingRoutingRegeneration.add(releasedHandle.elementId());
                    }
                    scheduleHeatingLayoutRecalculationForZone(releasedHandle.elementId());
                } else {
                    scheduleHeatingLayoutRecalculation(releasedHandle.elementId());
                }
            }
            if (hadDrag) {
                flushPendingRoomSynchronizationWarning();
            }
            updatePropertySectionVisibility();
            updateActionButtons();
            render();
            return;
        }

        if (selectedEndpointGroup != null) {
            boolean hadDrag = historyCapturedForDrag;
            historyCapturedForDrag = false;
            if (hadDrag) {
                flushPendingRoomSynchronizationWarning();
            }
            updatePropertySectionVisibility();
            updateActionButtons();
            render();
            return;
        }

        if (openingDragId != null) {
            openingDragId = null;
            openingDragWallAxis = null;
            openingDragWidth = 0;
            openingDragOffsetDelta = 0;
            boolean hadDrag = historyCapturedForDrag;
            historyCapturedForDrag = false;
            if (hadDrag) {
                flushPendingRoomSynchronizationWarning();
            }
            updatePropertySectionVisibility();
            updateActionButtons();
            render();
            return;
        }

        if (selectionDragAnchor != null) {
            selectionDragAnchor = null;
            selectionDragBaseWalls = List.of();
            selectionDragBaseStaircases = List.of();
            selectionDragBaseRoomObjects = List.of();
            selectionDragBaseFloorOpenings = List.of();
            selectionDragBaseHeatingExclusionAreas = List.of();
            selectionDragBaseHydronicHeatings = List.of();
            boolean hadDrag = historyCapturedForDrag;
            historyCapturedForDrag = false;
            if (selectedSelections.stream().anyMatch(selection -> selection.kind() == RenderableKind.HEATING_ZONE
                    || selection.kind() == RenderableKind.HEATING_MANIFOLD)) {
                scheduleHeatingLayoutRecalculation();
            }
            if (hadDrag) {
                flushPendingRoomSynchronizationWarning();
            }
            updatePropertySectionVisibility();
            updateActionButtons();
            render();
            return;
        }

        if (selectionRectangleStart != null) {
            if (selectionRectangleEnd == null) {
                selectionRectangleEnd = selectionRectangleStart;
            }
            if (hasSelectionRectangleArea()) {
                applySelectionRectangle();
            } else {
                clearSelectionRectangle();
            }
            render();
            return;
        }

        if (event.getButton() != MouseButton.PRIMARY || draftStart == null || previewSegment == null) {
            return;
        }

        if (previewSegment.length().toMillimeters() > 1.0 || currentTool() == DrawingTool.HEATING_MANIFOLD) {
            if (currentTool() == DrawingTool.WALL) {
                rememberStateForUndo();
                Wall wall = Wall.create(previewSegment, currentWallThickness(), currentWallHeight());
                activeLevel.get().addWall(wall);
                synchronizeRoomsFromWalls(activeLevel.get());
                selectSingle(new SelectionKey(RenderableKind.WALL, activeLevel.get().name(), wall.id().toString()));
            } else if (currentTool() == DrawingTool.STAIR) {
                rememberStateForUndo();
                Staircase staircase = Staircase.create(
                        currentStairType(),
                        previewSegment.start(),
                        previewSegment.end(),
                        currentStairHeight(),
                        currentStairSteps(),
                        currentStairStartLanding(),
                        currentStairEndLanding(),
                        currentStairLeftUnderbuild(),
                        currentStairRightUnderbuild(),
                        currentStairUndersideThickness()
                );
                activeLevel.get().addStaircase(staircase);
                synchronizeStairUnderbuild(staircase);
                selectSingle(new SelectionKey(RenderableKind.STAIR, activeLevel.get().name(), staircase.id().toString()));
            } else if (currentTool() == DrawingTool.FLOOR_EXTENSION
                    && Math.abs(previewSegment.end().xMillimeters() - previewSegment.start().xMillimeters()) > 1.0
                    && Math.abs(previewSegment.end().yMillimeters() - previewSegment.start().yMillimeters()) > 1.0) {
                rememberStateForUndo();
                FloorExtension extension = FloorExtension.create(
                        Optional.ofNullable(floorExtensionTypeSelector.getValue()).orElse(FloorExtensionType.BALCONY),
                        Optional.ofNullable(floorExtensionPlacementSelector.getValue()).orElse(FloorExtensionPlacement.EXTERIOR),
                        previewSegment.start(),
                        previewSegment.end(),
                        currentFloorExtensionThickness()
                );
                activeLevel.get().addFloorExtension(extension);
                selectSingle(new SelectionKey(RenderableKind.FLOOR_EXTENSION, activeLevel.get().name(), extension.id().toString()));
            } else if ((currentTool() == DrawingTool.FLOOR_OPENING_RECTANGLE
                    || currentTool() == DrawingTool.FLOOR_OPENING_CIRCLE)
                    && Math.abs(previewSegment.end().xMillimeters() - previewSegment.start().xMillimeters()) > 1.0
                    && Math.abs(previewSegment.end().yMillimeters() - previewSegment.start().yMillimeters()) > 1.0) {
                createFloorOpening(previewSegment, currentTool() == DrawingTool.FLOOR_OPENING_CIRCLE
                        ? FloorOpeningShape.CIRCLE
                        : FloorOpeningShape.RECTANGLE);
            } else if (currentTool() == DrawingTool.HEATING_EXCLUSION_RECTANGLE
                    && Math.abs(previewSegment.end().xMillimeters() - previewSegment.start().xMillimeters()) > 1.0
                    && Math.abs(previewSegment.end().yMillimeters() - previewSegment.start().yMillimeters()) > 1.0) {
                createHeatingExclusionArea(previewSegment);
            } else if (currentTool() == DrawingTool.HEATING_ZONE_RECTANGLE
                    && Math.abs(previewSegment.end().xMillimeters() - previewSegment.start().xMillimeters()) > 1.0
                    && Math.abs(previewSegment.end().yMillimeters() - previewSegment.start().yMillimeters()) > 1.0) {
                createHeatingZone(previewSegment);
            } else if (currentTool() == DrawingTool.HEATING_MANIFOLD) {
                if (Math.abs(previewSegment.end().xMillimeters() - previewSegment.start().xMillimeters()) > 1.0
                        && Math.abs(previewSegment.end().yMillimeters() - previewSegment.start().yMillimeters()) > 1.0) {
                    placeHydronicManifold(previewSegment);
                } else {
                    placeHydronicManifold(draftStart);
                }
            }
            markThreeDDirty();
        }
        draftStart = null;
        previewSegment = null;
        render();
    }

    SelectionKey contextSelectionAt(MouseEvent event) {
        PlanPoint editPoint = screenToWorld(event.getX(), event.getY());
        return contextSelectionAt(editPoint);
    }

    SelectionKey contextSelectionAt(PlanPoint editPoint) {
        List<SelectionKey> candidates = selectionQueryService.findSelections(activeLevel.get(), editPoint, pointerSelectionTolerance());
        SelectionKey currentSelection = selectedSelection.get();
        if (currentSelection != null && candidates.contains(currentSelection)) {
            return currentSelection;
        }
        return candidates.stream().findFirst().orElse(null);
    }

    void updateModifierState(KeyEvent event) {
        altPressed = event.isAltDown();
        if (event.getCode() == KeyCode.SPACE) {
            spacePressed = event.getEventType() == KeyEvent.KEY_PRESSED;
        }
        updateMouseCursor();
    }

    void handleGlobalShortcuts(KeyEvent event) {
        if (event.getEventType() != KeyEvent.KEY_PRESSED) {
            return;
        }
        if (!event.isShortcutDown() && event.getCode() == KeyCode.DELETE) {
            runGuardedAction("Auswahl löschen", this::deleteSelection);
            event.consume();
            return;
        }
        if (!event.isShortcutDown() && moveSelectionWithArrowKey(event.getCode())) {
            event.consume();
            return;
        }
        if (!event.isShortcutDown()) {
            return;
        }
        if (event.getCode() == KeyCode.Z && event.isShiftDown()) {
            runGuardedAction("Wiederherstellen", this::redo);
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.Z) {
            runGuardedAction("Rückgängig", this::undo);
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.Q) {
            runGuardedAction("Beenden", this::requestApplicationExit);
            event.consume();
        }
    }

    void requestApplicationExit() {
        if (!confirmApplicationClose()) {
            return;
        }
        applicationExitConfirmed = true;
        applicationExitRequested = true;
        Window window = currentWindow();
        if (window instanceof Stage stage) {
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
            if (stage.isShowing()) {
                stage.close();
            }
        } else if (window != null) {
            window.hide();
        }
        applicationExitAction.run();
    }

    public boolean confirmApplicationClose() {
        if (applicationExitConfirmed || !hasUnsavedChanges()) {
            return true;
        }
        if (automatedUnsavedChangesExitDecision != null) {
            return automatedUnsavedChangesExitDecision;
        }
        if (!interactiveDialogsEnabled) {
            return true;
        }

        ButtonType saveButton = new ButtonType("Sichern", ButtonBar.ButtonData.YES);
        ButtonType discardButton = new ButtonType("Ohne Sichern beenden", ButtonBar.ButtonData.NO);
        Alert alert = new Alert(Alert.AlertType.WARNING, "", saveButton, discardButton, ButtonType.CANCEL);
        alert.setTitle("Ungesicherte Änderungen");
        alert.setHeaderText("Änderungen vor dem Beenden sichern?");
        alert.setContentText("Das Projekt enthält Änderungen, die noch nicht gesichert wurden. Beim Beenden ohne Sichern gehen diese Änderungen verloren.");
        alert.getDialogPane().setPrefWidth(560);
        Window owner = currentWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        applyTooltip(alert.getDialogPane().lookupButton(saveButton),
                "Sichert das vollständige Gebäude und beendet CADas anschließend.");
        applyTooltip(alert.getDialogPane().lookupButton(discardButton),
                "Beendet CADas und verwirft alle Änderungen seit der letzten Gebäudesicherung.");
        applyTooltip(alert.getDialogPane().lookupButton(ButtonType.CANCEL),
                "Bricht das Beenden ab und kehrt zum aktuellen Projekt zurück.");

        Optional<ButtonType> decision = alert.showAndWait();
        if (decision.filter(saveButton::equals).isPresent()) {
            saveProject();
            return !hasUnsavedChanges();
        }
        return decision.filter(discardButton::equals).isPresent();
    }

    boolean hasUnsavedChanges() {
        return currentChangeRevision != savedChangeRevision;
    }

    void updateMouseCursor() {
        PointerCursorService.PointerTarget target = pointerTargetAtLastPosition();
        PointerCursorService.CursorType cursorType = pointerCursorService.cursor(new PointerCursorService.PointerContext(
                currentTool(),
                target,
                panning,
                spacePressed,
                altPressed
        ));
        drawingCanvas.setCursor(switch (cursorType) {
            case DEFAULT -> Cursor.DEFAULT;
            case CROSSHAIR -> Cursor.CROSSHAIR;
            case HAND -> Cursor.HAND;
            case OPEN_HAND -> Cursor.OPEN_HAND;
            case CLOSED_HAND -> Cursor.CLOSED_HAND;
            case MOVE -> Cursor.MOVE;
            case HORIZONTAL_RESIZE -> Cursor.H_RESIZE;
            case VERTICAL_RESIZE -> Cursor.V_RESIZE;
        });
    }

    PointerCursorService.PointerTarget pointerTargetAtLastPosition() {
        PlanPoint point = screenToWorld(lastMouseX, lastMouseY);
        Optional<EdgeResizeService.EdgeHandle> handle = edgeResizeService.findHandle(
                activeLevel.get(),
                Set.copyOf(selectedSelections),
                point,
                Length.ofMillimeters(8.0 / scale())
        );
        if (handle.isPresent()) {
            EdgeResizeService.EdgeHandle edgeHandle = handle.orElseThrow();
            if (EdgeResizeService.isRectangleCorner(edgeHandle.kind())) {
                return PointerCursorService.PointerTarget.RESIZE_CORNER;
            }
            if (EdgeResizeService.isRectangleHorizontalResize(edgeHandle.kind())) {
                return PointerCursorService.PointerTarget.HORIZONTAL_EDGE;
            }
            if (EdgeResizeService.isRectangleVerticalResize(edgeHandle.kind())) {
                return PointerCursorService.PointerTarget.VERTICAL_EDGE;
            }
            Wall wall = activeLevel.get().findWall(edgeHandle.hostWallId());
            double deltaX = Math.abs(wall.axis().end().xMillimeters() - wall.axis().start().xMillimeters());
            double deltaY = Math.abs(wall.axis().end().yMillimeters() - wall.axis().start().yMillimeters());
            return deltaX >= deltaY
                    ? PointerCursorService.PointerTarget.HORIZONTAL_EDGE
                    : PointerCursorService.PointerTarget.VERTICAL_EDGE;
        }
        if (currentTool() == DrawingTool.EDIT
                && wallEditingService.findConnectedEndpoint(activeLevel.get().walls(), point, Length.ofMillimeters(8.0 / scale())).isPresent()) {
            return PointerCursorService.PointerTarget.ENDPOINT;
        }
        if (currentTool() == DrawingTool.EDIT
                && selectionQueryService.findSelection(activeLevel.get(), point, Length.ofMillimeters(8.0 / scale())).isPresent()) {
            return PointerCursorService.PointerTarget.ELEMENT;
        }
        return PointerCursorService.PointerTarget.EMPTY;
    }

    void resizeCanvases() {
        double width = Math.max(drawingPane.getWidth(), 200.0);
        double height = Math.max(drawingPane.getHeight(), 200.0);
        drawingCanvas.setWidth(width);
        drawingCanvas.setHeight(height);
        horizontalRuler.setWidth(width);
        verticalRuler.setHeight(height);
        render();
    }
}
