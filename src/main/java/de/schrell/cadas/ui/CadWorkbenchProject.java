package de.schrell.cadas.ui;

import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.formatSurfaceLayoutCorner;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.formatSurfaceLayoutDirection;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.normalizedSurfaceLayoutAnchor;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.startsAtMaximumX;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.startsAtMaximumY;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.surfaceLayoutRotatedQuarterTurn;
import static de.schrell.cadas.ui.CadWorkbenchSurfaceLayoutSupport.surfaceLayoutSelectionDirection;
import static de.schrell.cadas.ui.CadWorkbenchCoveringSourceSupport.extractDwgBlockName;
import static de.schrell.cadas.ui.CadWorkbenchCoveringSourceSupport.formatCoveringSourceLabel;

import de.schrell.cadas.application.drawing.DraftingConstraints;
import de.schrell.cadas.application.drawing.DraftingService;
import de.schrell.cadas.application.drawing.DimensionLabelOptions;
import de.schrell.cadas.application.drawing.DimensionLabelPlacementService;
import de.schrell.cadas.application.drawing.DimensionLabelService;
import de.schrell.cadas.application.drawing.DimensionLineLayoutService;
import de.schrell.cadas.application.drawing.DimensionTextStyle;
import de.schrell.cadas.application.drawing.DimensionStandard;
import de.schrell.cadas.application.drawing.EdgeResizeService;
import de.schrell.cadas.application.drawing.GuideSnapService;
import de.schrell.cadas.application.drawing.GuideSnapTargets;
import de.schrell.cadas.application.drawing.HeatingZoneMirrorService;
import de.schrell.cadas.application.drawing.WallIntersectionSplitService;
import de.schrell.cadas.application.exchange.ExchangeFileNameService;
import de.schrell.cadas.application.floor.FloorOpeningGeometryService;
import de.schrell.cadas.application.history.UndoRedoStack;
import de.schrell.cadas.application.help.HelpContentService;
import de.schrell.cadas.application.help.MarkdownNavigationService;
import de.schrell.cadas.application.heating.HeatingCircuitRoutingService;
import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
import de.schrell.cadas.application.heating.RoomHeatingOutputService;
import de.schrell.cadas.application.drawing.OpeningPlacementService;
import de.schrell.cadas.application.drawing.OrthogonalCorrectionService;
import de.schrell.cadas.application.drawing.QuarterTurnRotationService;
import de.schrell.cadas.application.drawing.SelectionQueryService;
import de.schrell.cadas.application.drawing.SelectionTranslationService;
import de.schrell.cadas.application.drawing.SnapService;
import de.schrell.cadas.application.drawing.TextBlockingBox;
import de.schrell.cadas.application.drawing.WallDimensionPlacementService;
import de.schrell.cadas.application.drawing.WallEditingService;
import de.schrell.cadas.application.drawing.WallDimensionService;
import de.schrell.cadas.application.drawing.WallSnapService;
import de.schrell.cadas.application.drawing.WallEndpointSelection;
import de.schrell.cadas.application.dwg.DwgBlockDefinition;
import de.schrell.cadas.application.dwg.DwgConversionAvailability;
import de.schrell.cadas.application.dwg.DwgLibraryAnalysis;
import de.schrell.cadas.application.dwg.DwgLibraryAnalyzer;
import de.schrell.cadas.application.exchange.LevelExchangeService;
import de.schrell.cadas.application.exchange.ProjectExchangeService;
import de.schrell.cadas.application.layers.SurfaceCoveringPreset;
import de.schrell.cadas.application.layers.SurfaceCoveringPresetService;
import de.schrell.cadas.application.layers.DwgBlockCatalogService;
import de.schrell.cadas.application.layers.SurfaceLayerEffectService;
import de.schrell.cadas.application.layers.SurfaceLayerConsistencyService;
import de.schrell.cadas.application.layers.SurfaceRectangleTileLayoutService;
import de.schrell.cadas.application.layers.TileLayoutRequest;
import de.schrell.cadas.application.layers.TileLayoutService;
import de.schrell.cadas.application.layers.TilePlacement;
import de.schrell.cadas.application.layers.UserSurfaceCoveringPresetLibrary;
import de.schrell.cadas.application.layers.WallSurfaceSideService;
import de.schrell.cadas.application.layers.WallSurfaceTargetKey;
import de.schrell.cadas.application.objects.RoomObjectPreset;
import de.schrell.cadas.application.objects.RoomObjectPresetService;
import de.schrell.cadas.application.parts.DoorPreset;
import de.schrell.cadas.application.parts.PartLibraryImportService;
import de.schrell.cadas.application.parts.StairPreset;
import de.schrell.cadas.application.parts.StandardPartLibrary;
import de.schrell.cadas.application.parts.StandardPartLibraryService;
import de.schrell.cadas.application.parts.WindowPreset;
import de.schrell.cadas.application.reports.MarkdownHtmlRenderer;
import de.schrell.cadas.application.reports.ConstructionDrawingPdfService;
import de.schrell.cadas.application.reports.SurfaceMaterialListService;
import de.schrell.cadas.application.roof.RoofSlopeWallService;
import de.schrell.cadas.application.roof.RoofWindowPlacementService;
import de.schrell.cadas.application.stairs.StairUnderbuildService;
import de.schrell.cadas.application.room.AutoRoomGenerationService;
import de.schrell.cadas.application.terrain.TerrainContourService;
import de.schrell.cadas.application.terrain.TerrainEditService;
import de.schrell.cadas.application.terrain.TerrainGeometryService;
import de.schrell.cadas.application.terrain.TerrainProfileService;
import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.application.view.WallSurfaceOpeningService;
import de.schrell.cadas.application.view.WallSurfaceOpeningService.WallSurfaceInterval;
import de.schrell.cadas.application.view.WallSurfaceOpeningService.WallSurfaceRectangle;
import de.schrell.cadas.application.view.WallSurfacePlanGeometryService;
import de.schrell.cadas.application.view.WallSurfacePlanGeometryService.WallSurfacePlanPolygon;
import de.schrell.cadas.domain.geometry.Angle;
import de.schrell.cadas.domain.geometry.Grid;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.FloorExtensionPlacement;
import de.schrell.cadas.domain.model.FloorExtensionType;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingRoutingLanguage;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoofWindow;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectHeatingType;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutAnchor;
import de.schrell.cadas.domain.model.SurfaceLayoutDirection;
import de.schrell.cadas.domain.model.SurfaceLayoutMargins;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;
import de.schrell.cadas.domain.model.WindowElement;
import de.schrell.cadas.infrastructure.dxf.DxfLevelExchangeService;
import de.schrell.cadas.infrastructure.dxf.DxfProjectExchangeService;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.embed.swing.SwingFXUtils;

abstract class CadWorkbenchProject extends CadWorkbenchRenderDetails {

    double chooseRulerStep() {
        double[] candidates = {100, 250, 500, 1000, 2000, 5000};
        for (double candidate : candidates) {
            if (candidate * scale() >= 60.0) {
                return candidate;
            }
        }
        return candidates[candidates.length - 1];
    }

    String formatRuler(double worldMillimeters) {
        return String.format(Locale.GERMAN, "%.1f m", worldMillimeters / 1000.0);
    }

    DraftingConstraints currentConstraints(boolean orthogonalMode) {
        return new DraftingConstraints(
                orthogonalMode,
                snapToGrid.get(),
                snapToEndpoints.get(),
                currentGrid(),
                currentWallThickness(),
                SNAP_TOLERANCE,
                parseLength(lengthField, lengthUnit.getValue()),
                parseAngle(angleField)
        );
    }

    Grid currentGrid() {
        return new Grid(parseLength(gridField, gridUnit.getValue()).orElse(DEFAULT_GRID));
    }

    Length pointerSelectionTolerance() {
        return Length.ofMillimeters(Math.min(
                SNAP_TOLERANCE.toMillimeters(),
                POINTER_SELECTION_TOLERANCE_PIXELS / Math.max(scale(), 0.0001)
        ));
    }

    Length currentWallThickness() {
        return parseLength(wallThicknessField, wallThicknessUnit.getValue()).orElse(DEFAULT_WALL_THICKNESS);
    }

    Length currentWallHeight() {
        return parseLength(wallHeightField, wallHeightUnit.getValue()).orElse(DEFAULT_WALL_HEIGHT);
    }

    Length currentEndpointHeight() {
        return parseLength(endpointHeightField, endpointHeightUnit.getValue()).orElse(currentWallHeight());
    }

    String currentRoomName() {
        String roomName = roomNameField.getText();
        if (roomName == null || roomName.isBlank()) {
            return "Raum";
        }
        return roomName.trim();
    }

    AutoRoomGenerationService.RoomDefaults currentRoomDefaults() {
        return new AutoRoomGenerationService.RoomDefaults(
                currentRoomName(),
                currentRoomHeight(),
                currentFloorThickness(),
                currentCeilingThickness(),
                null
        );
    }

    Length currentRoomHeight() {
        return parseLength(roomHeightField, roomHeightUnit.getValue()).orElse(DEFAULT_ROOM_HEIGHT);
    }

    Length currentFloorThickness() {
        return parseLength(floorThicknessField, floorThicknessUnit.getValue()).orElse(DEFAULT_FLOOR_THICKNESS);
    }

    Length currentFloorExtensionThickness() {
        return parseLength(floorExtensionThicknessField, floorExtensionThicknessUnit.getValue()).orElse(DEFAULT_FLOOR_THICKNESS);
    }

    Length currentCeilingThickness() {
        return parseLength(ceilingThicknessField, ceilingThicknessUnit.getValue()).orElse(DEFAULT_CEILING_THICKNESS);
    }

    Length currentDoorWidth() {
        return parseLength(doorWidthField, doorWidthUnit.getValue()).orElse(DEFAULT_DOOR_WIDTH);
    }

    Length currentDoorHeight() {
        return parseLength(doorHeightField, doorHeightUnit.getValue()).orElse(DEFAULT_DOOR_HEIGHT);
    }

    Length currentThresholdHeight() {
        return parseLength(thresholdField, thresholdUnit.getValue()).orElse(Length.zero());
    }

    Length currentWindowWidth() {
        return parseLength(windowWidthField, windowWidthUnit.getValue()).orElse(DEFAULT_WINDOW_WIDTH);
    }

    Length currentWindowHeight() {
        return parseLength(windowHeightField, windowHeightUnit.getValue()).orElse(DEFAULT_WINDOW_HEIGHT);
    }

    Length currentSillHeight() {
        return parseLength(sillHeightField, sillHeightUnit.getValue()).orElse(DEFAULT_WINDOW_SILL);
    }

    StairType currentStairType() {
        return Optional.ofNullable(stairPresetSelector.getValue())
                .map(StairPreset::stairType)
                .orElse(StairType.STRAIGHT);
    }

    Length currentStairHeight() {
        return parseLength(stairHeightField, stairHeightUnit.getValue()).orElse(DEFAULT_STAIR_HEIGHT);
    }

    int currentStairSteps() {
        try {
            return Math.max(1, Integer.parseInt(stairStepsField.getText()));
        } catch (NumberFormatException ignored) {
            return 16;
        }
    }

    Length currentStairStartLanding() {
        return parseLength(stairStartLandingField, stairStartLandingUnit.getValue()).orElse(Length.ofMillimeters(0));
    }

    Length currentStairEndLanding() {
        return parseLength(stairEndLandingField, stairEndLandingUnit.getValue()).orElse(Length.ofMillimeters(0));
    }

    Length currentStairLeftUnderbuild() {
        return parseLength(stairLeftUnderbuildField, stairLeftUnderbuildUnit.getValue()).orElse(Length.zero());
    }

    Length currentStairRightUnderbuild() {
        return parseLength(stairRightUnderbuildField, stairRightUnderbuildUnit.getValue()).orElse(Length.zero());
    }

    Length currentStairUndersideThickness() {
        return parseLength(stairUndersideThicknessField, stairUndersideThicknessUnit.getValue()).orElse(Length.zero());
    }

    Length currentRoomObjectWidth(RoomObjectPreset preset) {
        return positiveLength(roomObjectWidthField, roomObjectWidthUnit, preset.width());
    }

    String currentRoomObjectName(RoomObjectPreset preset) {
        String name = roomObjectNameField.getText();
        return name == null || name.isBlank() ? Optional.ofNullable(preset).map(RoomObjectPreset::name).orElse("Objekt") : name.trim();
    }

    Length currentRoomObjectDepth(RoomObjectPreset preset) {
        return positiveLength(roomObjectDepthField, roomObjectDepthUnit, preset.depth());
    }

    Length currentRoomObjectHeight(RoomObjectPreset preset) {
        return positiveLength(roomObjectHeightField, roomObjectHeightUnit, preset.height());
    }

    double currentRoomObjectHeatOutputWatts(double fallback) {
        String text = roomObjectHeatOutputField.getText();
        if (text == null || text.isBlank()) {
            return fallback;
        }
        try {
            double value = Double.parseDouble(text.replace(',', '.'));
            return Double.isFinite(value) && value >= 0.0 ? value : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    RoomObjectHeatingType currentRoomObjectHeatingType(RoomObjectHeatingType fallback) {
        return Optional.ofNullable(roomObjectHeatingTypeSelector.getValue()).orElse(fallback);
    }

    Length currentRoomObjectBaseElevation() {
        return parseLength(roomObjectBaseElevationField, roomObjectBaseElevationUnit.getValue()).orElse(Length.zero());
    }

    Length positiveLength(TextField field, ComboBox<LengthUnit> unitSelector, Length fallback) {
        return parseLength(field, unitSelector.getValue())
                .filter(length -> length.toMillimeters() > 0.0)
                .orElse(fallback);
    }

    double currentRoomObjectAngleDegrees() {
        return parseAngle(roomObjectAngleField).map(Angle::degrees).orElse(0.0);
    }

    Optional<Length> parseLength(TextField field, LengthUnit unit) {
        String text = field.getText();
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Length.of(Double.parseDouble(text.replace(',', '.')), unit));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    Optional<Angle> parseAngle(TextField field) {
        String text = field.getText();
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Angle.ofDegrees(Double.parseDouble(text.replace(',', '.'))));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    double currentNorthAngleDegrees() {
        return parseAngle(northAngleField).map(Angle::degrees).orElse(0.0);
    }

    void updateStatus() {
        viewLabel.setText("Arbeitsbereich: " + activeWorkspaceMode.get().label() + " | 2D-Ansicht: " + activeView.get().label() + " | Etage: " + activeLevel.get().name());
        zoomLabel.setText(String.format(Locale.GERMAN, "Zoom: %.2f x", zoom));
        cursorLabel.setText(String.format(Locale.GERMAN, "Cursor: %.2f m / %.2f m", lastCursor.xMillimeters() / 1000.0, lastCursor.yMillimeters() / 1000.0));
        if (previewSegment == null) {
            if (selectedEndpointGroup != null) {
                draftLabel.setText("Werkzeug: " + currentTool().label() + " | Wandecke ausgewählt: Ziehen verschiebt sie gemeinsam, `Eckhöhe anwenden` setzt ihre Höhe.");
            } else {
                draftLabel.setText(statusHintForCurrentTool());
            }
        } else {
            draftLabel.setText("Zeichnen: " + previewSegment.length().format(LengthUnit.METER, 2) + " | " + previewSegment.angle().format());
        }
    }

    String statusHintForCurrentTool() {
        return switch (currentTool()) {
            case EDIT -> "Werkzeug: Bearbeiten | Linksklick wählt aus, Alt+Linksklick schaltet überdeckte Treffer durch, Rechtsziehen verschiebt die Ansicht, Alt+Rechtsklick entfernt Hilfslinien.";
            case WALL -> "Werkzeug: Wand | Linksklick startet und beendet Wände, Shift erlaubt freie Winkel.";
            case DOOR -> "Werkzeug: Tür | Linksklick auf eine Wand platziert die Tür mit den aktuellen Maßen.";
            case WINDOW -> "Werkzeug: Fenster | Linksklick auf eine Wand platziert das Fenster mit den aktuellen Maßen.";
            case ROOF_WINDOW -> "Werkzeug: Dachfenster | Linksklick in einem Raum mit Dachschräge platziert das Dachfenster mit Fensterbreite und Fensterhöhe.";
            case STAIR -> "Werkzeug: Treppe | Rechteck aufziehen platziert die Treppe mit dem gewählten Preset.";
            case FLOOR_EXTENSION -> "Werkzeug: Balkon/Empore | Rechteck aufziehen fügt die Fußbodenplatte innen oder außen an die aktive Etage an.";
            case FLOOR_OPENING_RECTANGLE -> "Werkzeug: Bodenloch rechteckig | Rechteck innerhalb eines Raums aufziehen.";
            case FLOOR_OPENING_CIRCLE -> "Werkzeug: Bodenloch rund | Begrenzungsquadrat innerhalb eines Raums aufziehen.";
            case HEATING_ZONE_RECTANGLE -> "Werkzeug: Heizkreis | Rechteck innerhalb eines Raums aufziehen; Standard ist Schnecke, die Verlegung kann danach im Kontextmenü geändert werden.";
            case HEATING_MANIFOLD -> "Werkzeug: HKV | Linksklick oder Rechteck aufziehen platziert den Heizkreisverteiler; Vorlauf- und Rücklaufanschluss werden im aufgezogenen Kasten dargestellt.";
            case HEATING_EXCLUSION_RECTANGLE -> "Werkzeug: FBH-Sperrfläche | Rechteck innerhalb eines Raums aufziehen; der FBH-Layouter spart diese Fläche aus.";
            case OBJECT -> "Werkzeug: Objekt | Linksklick platziert das ausgewählte Objekt-Preset innen oder außen.";
        };
    }

    void applyTooltip(javafx.scene.Node node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(320);
        Tooltip.install(node, tooltip);
    }

    void createLevel() {
        String levelName = "";
        if (interactiveDialogsEnabled) {
            TextInputDialog dialog = new TextInputDialog("Etage " + (availableLevels.size() + 1));
            dialog.setTitle("Neue Etage");
            dialog.setHeaderText("Neue Etage anlegen");
            dialog.setContentText("Name der Etage:");
            dialog.getDialogPane().setPrefWidth(420);
            Window owner = getScene() != null ? getScene().getWindow() : null;
            if (owner != null) {
                dialog.initOwner(owner);
            }
            levelName = dialog.showAndWait()
                    .map(String::trim)
                    .filter(name -> !name.isBlank())
                    .orElse(null);
        }
        if (levelName == null) {
            return;
        }
        String finalLevelName = levelName.isBlank() ? "Etage " + (availableLevels.size() + 1) : levelName;
        rememberStateForUndo();
        Level level = project.createLevel(finalLevelName);
        availableLevels.add(level);
        activateLevel(level);
        fitCurrentViewToContent();
    }

    void renameCurrentLevel() {
        Level current = activeLevel.get();
        if (current == null) {
            return;
        }
        String newName = current.name();
        if (interactiveDialogsEnabled) {
            TextInputDialog dialog = new TextInputDialog(current.name());
            dialog.setTitle("Etage umbenennen");
            dialog.setHeaderText("Aktuelle Etage umbenennen");
            dialog.setContentText("Neuer Name der Etage:");
            dialog.getDialogPane().setPrefWidth(420);
            Window owner = getScene() != null ? getScene().getWindow() : null;
            if (owner != null) {
                dialog.initOwner(owner);
            }
            newName = dialog.showAndWait()
                    .map(String::trim)
                    .filter(name -> !name.isBlank())
                    .orElse(null);
        }
        if (newName == null) {
            return;
        }
        if (newName.equals(current.name())) {
            return;
        }
        rememberStateForUndo();
        try {
            project.renameLevel(current, newName);
        } catch (IllegalArgumentException exception) {
            UiErrorDialogs.show(
                    UiErrorDialogs.fromThrowable(
                            "Etage konnte nicht umbenannt werden",
                            "Der Name ist ungültig oder bereits vergeben.",
                            exception.getMessage(),
                            exception
                    ),
                    currentWindow(),
                    interactiveDialogsEnabled
            );
            return;
        }
        int index = availableLevels.indexOf(current);
        if (index >= 0) {
            availableLevels.set(index, current);
        }
        activateLevel(current);
    }

    void moveCurrentLevelUp() {
        moveCurrentLevel(-1);
    }

    void moveCurrentLevelDown() {
        moveCurrentLevel(1);
    }

    void moveCurrentLevel(int direction) {
        Level current = activeLevel.get();
        if (current == null) {
            return;
        }
        int currentIndex = availableLevels.indexOf(current);
        if (currentIndex < 0) {
            return;
        }
        int newIndex = currentIndex + direction;
        if (newIndex < 0 || newIndex >= availableLevels.size()) {
            return;
        }
        rememberStateForUndo();
        try {
            project.moveLevel(current, newIndex);
        } catch (IndexOutOfBoundsException exception) {
            UiErrorDialogs.show(
                    UiErrorDialogs.fromThrowable(
                            "Etage konnte nicht verschoben werden",
                            "Der neue Etage-Index liegt außerhalb des gültigen Bereichs.",
                            exception.getMessage(),
                            exception
                    ),
                    currentWindow(),
                    interactiveDialogsEnabled
            );
            return;
        }
        availableLevels.setAll(project.levels());
        activateLevel(current);
    }

    DrawingTool currentTool() {
        return Optional.ofNullable(toolSelector.getValue()).orElse(DrawingTool.WALL);
    }

    PlanPoint snapDrawingPoint(PlanPoint point, DraftingConstraints constraints) {
        GuideSnapTargets targets = currentTool() == DrawingTool.WALL || currentTool() == DrawingTool.EDIT
                ? currentAlignmentSnapTargets(Set.of())
                : currentGuideSnapTargets();
        return snapService.snap(point, constraints, activeLevel.get().walls(), targets);
    }

    GuideSnapTargets currentGuideSnapTargets() {
        if (!snapToGuides.get()) {
            return GuideSnapTargets.empty();
        }
        return new GuideSnapTargets(
                guideLines.stream()
                        .filter(guideLine -> guideLine.orientation() == GuideOrientation.VERTICAL)
                        .map(GuideLine::worldMillimeters)
                        .toList(),
                guideLines.stream()
                        .filter(guideLine -> guideLine.orientation() == GuideOrientation.HORIZONTAL)
                        .map(GuideLine::worldMillimeters)
                        .toList()
        );
    }

    GuideSnapTargets currentAlignmentSnapTargets(Set<UUID> excludedWallIds) {
        GuideSnapTargets guideTargets = currentGuideSnapTargets();
        GuideSnapTargets wallTargets = snapToWalls.get()
                ? wallSnapService.targets(activeLevel.get().walls(), excludedWallIds)
                : GuideSnapTargets.empty();
        return new GuideSnapTargets(
                java.util.stream.Stream.concat(guideTargets.verticalGuides().stream(), wallTargets.verticalGuides().stream()).distinct().toList(),
                java.util.stream.Stream.concat(guideTargets.horizontalGuides().stream(), wallTargets.horizontalGuides().stream()).distinct().toList()
        );
    }

    Door snapDoorToGuides(Door door) {
        if (!snapToGuides.get()) {
            return door;
        }
        Wall wall = activeLevel.get().findWall(door.wallId());
        return door.withOffset(guideSnapService.snapOpeningOffset(
                wall,
                door.offsetFromStart(),
                door.width(),
                currentGuideSnapTargets(),
                SNAP_TOLERANCE
        ));
    }

    WindowElement snapWindowToGuides(WindowElement window) {
        if (!snapToGuides.get()) {
            return window;
        }
        Wall wall = activeLevel.get().findWall(window.wallId());
        return window.withOffset(guideSnapService.snapOpeningOffset(
                wall,
                window.offsetFromStart(),
                window.width(),
                currentGuideSnapTargets(),
                SNAP_TOLERANCE
        ));
    }

    Wall openingDragWall() {
        Optional<UUID> wallId = activeLevel.get().doors().stream()
                .filter(door -> door.id().equals(openingDragId))
                .map(Door::wallId)
                .findFirst()
                .or(() -> activeLevel.get().windows().stream()
                        .filter(window -> window.id().equals(openingDragId))
                        .map(WindowElement::wallId)
                        .findFirst());
        return activeLevel.get().findWall(wallId.orElseThrow());
    }

    void placeDoor(PlanPoint clickPoint) {
        openingPlacementService.placeDoor(
                        clickPoint,
                        activeLevel.get().walls(),
                        currentDoorWidth(),
                currentDoorHeight(),
                currentThresholdHeight(),
                SNAP_TOLERANCE)
                .map(this::snapDoorToGuides)
                .ifPresent(door -> {
                    rememberStateForUndo();
                    activeLevel.get().addDoor(door);
                    selectSingle(new SelectionKey(RenderableKind.DOOR, activeLevel.get().name(), door.id().toString()));
                    markThreeDDirty();
                });
    }

    void placeWindow(PlanPoint clickPoint) {
        openingPlacementService.placeWindow(
                        clickPoint,
                        activeLevel.get().walls(),
                currentWindowWidth(),
                currentSillHeight(),
                currentWindowHeight(),
                SNAP_TOLERANCE)
                .map(this::snapWindowToGuides)
                .ifPresent(window -> {
                    rememberStateForUndo();
                    activeLevel.get().addWindow(window);
                    selectSingle(new SelectionKey(RenderableKind.WINDOW, activeLevel.get().name(), window.id().toString()));
                    markThreeDDirty();
                });
    }

    void placeRoofWindow(PlanPoint clickPoint) {
        roofWindowPlacementService.place(
                        activeLevel.get(), clickPoint, currentWindowWidth(), currentWindowHeight()
                )
                .ifPresentOrElse(roofWindow -> {
                    rememberStateForUndo();
                    activeLevel.get().addRoofWindow(roofWindow);
                    selectSingle(new SelectionKey(RenderableKind.ROOF_WINDOW, activeLevel.get().name(), roofWindow.id().toString()));
                    markThreeDDirty();
                    draftLabel.setText("Dachfenster auf Dachschräge platziert.");
                }, () -> draftLabel.setText("Dachfenster können nur innerhalb eines Raums mit Dachschräge platziert werden."));
    }

    void placeRoomObject(PlanPoint clickPoint) {
        RoomObjectPreset preset = roomObjectPresetSelector.getValue();
        if (preset == null) {
            draftLabel.setText("Kein Objekt-Preset ausgewählt.");
            return;
        }
        rememberStateForUndo();
        RoomObject roomObject = RoomObject.create(
                preset.id(),
                currentRoomObjectName(preset),
                preset.type(),
                preset.shape(),
                clickPoint,
                currentRoomObjectWidth(preset),
                currentRoomObjectDepth(preset),
                currentRoomObjectHeight(preset),
                currentRoomObjectAngleDegrees(),
                preset.mountingMode(),
                preset.source()
        )
                .withBaseElevation(currentRoomObjectBaseElevation())
                .withHeatingType(currentRoomObjectHeatingType(RoomObjectHeatingType.NONE))
                .withHeatOutputWatts(currentRoomObjectHeatOutputWatts(preset.heatOutputWatts()));
        activeLevel.get().addRoomObject(roomObject);
        selectSingle(new SelectionKey(RenderableKind.ROOM_OBJECT, activeLevel.get().name(), roomObject.id().toString()));
        markThreeDDirty();
    }

    void createFloorOpening(PlanSegment bounds, FloorOpeningShape shape) {
        double width = Math.abs(bounds.end().xMillimeters() - bounds.start().xMillimeters());
        double depth = Math.abs(bounds.end().yMillimeters() - bounds.start().yMillimeters());
        PlanPoint center = new PlanPoint(
                (bounds.start().xMillimeters() + bounds.end().xMillimeters()) / 2.0,
                (bounds.start().yMillimeters() + bounds.end().yMillimeters()) / 2.0
        );
        if (shape == FloorOpeningShape.CIRCLE) {
            double diameter = Math.min(width, depth);
            width = diameter;
            depth = diameter;
        }
        Optional<Room> room = roomAt(center);
        if (room.isEmpty()) {
            draftLabel.setText("Bodenöffnungen müssen mit ihrem Mittelpunkt in einem Raum liegen.");
            return;
        }
        rememberStateForUndo();
        FloorOpening opening = FloorOpening.create(
                room.orElseThrow().id(), shape, center,
                Length.ofMillimeters(width), Length.ofMillimeters(depth)
        );
        activeLevel.get().addFloorOpening(opening);
        selectSingle(new SelectionKey(RenderableKind.FLOOR_OPENING, activeLevel.get().name(), opening.id().toString()));
        markThreeDDirty();
        draftLabel.setText("Bodenöffnung erzeugt.");
    }

    void createHeatingExclusionArea(PlanSegment bounds) {
        PlanPoint center = new PlanPoint(
                (bounds.start().xMillimeters() + bounds.end().xMillimeters()) / 2.0,
                (bounds.start().yMillimeters() + bounds.end().yMillimeters()) / 2.0
        );
        Optional<Room> room = roomAt(center);
        if (room.isEmpty()) {
            draftLabel.setText("FBH-Sperrflächen müssen mit ihrem Mittelpunkt in einem Raum liegen.");
            return;
        }
        rememberStateForUndo();
        HeatingExclusionArea area = HeatingExclusionArea.create(
                room.orElseThrow().id(),
                "FBH-Sperrfläche " + (activeLevel.get().heatingExclusionAreas().size() + 1),
                bounds.start(),
                bounds.end()
        );
        activeLevel.get().addHeatingExclusionArea(area);
        selectSingle(new SelectionKey(RenderableKind.HEATING_EXCLUSION, activeLevel.get().name(), area.id().toString()));
        draftLabel.setText("FBH-Sperrfläche erzeugt.");
    }

    void createHeatingZone(PlanSegment bounds) {
        PlanPoint center = new PlanPoint(
                (bounds.start().xMillimeters() + bounds.end().xMillimeters()) / 2.0,
                (bounds.start().yMillimeters() + bounds.end().yMillimeters()) / 2.0
        );
        Room room = roomAt(center).orElse(null);
        if (room == null) {
            draftLabel.setText("Heizkreise müssen mit ihrem Mittelpunkt in einem Raum liegen.");
            return;
        }
        HeatingSurfacePosition surfacePosition = Optional.ofNullable(heatingSurfacePositionSelector.getValue())
                .orElse(HeatingSurfacePosition.FLOOR);
        HydronicHeating existing = activeLevel.get().findHydronicHeating(room.id(), surfacePosition);
        HydronicHeating heating;
        try {
            heating = existing == null
                    ? heatingFromInputs(room, UUID.randomUUID())
                    : existing;
        } catch (RuntimeException exception) {
            draftLabel.setText("Heizkreis nicht erzeugt: " + UiErrorDialogs.userMessage(exception));
            return;
        }
        List<HeatingZone> zones = new ArrayList<>(heating.zones());
        HeatingZone zone = new HeatingZone(
                UUID.randomUUID(),
                "Heizkreis " + (zones.size() + 1),
                CadWorkbenchHeatingSupport.rectanglePoints(CadWorkbenchHeatingSupport.heatingZoneBounds(bounds)),
                heatingCircuitRoutingService.manualPattern(Optional.ofNullable(heatingLayoutPatternSelector.getValue())
                        .orElse(HeatingLayoutPattern.VARIO)),
                false
        );
        try {
            zone = heatingCircuitRoutingService.regenerate(zone, heating);
            zone = snapHeatingZoneRoutingStartIfNeeded(zone);
        } catch (RuntimeException exception) {
            draftLabel.setText("Heizkreis nicht erzeugt: " + UiErrorDialogs.userMessage(exception));
            return;
        }
        zones.add(zone);
        HydronicHeating updatedHeating = heating.withZones(zones);
        rememberStateForUndo();
        if (existing == null) {
            activeLevel.get().addHydronicHeating(updatedHeating);
        } else {
            activeLevel.get().replaceHydronicHeating(updatedHeating);
        }
        selectSingle(new SelectionKey(RenderableKind.HEATING_ZONE, activeLevel.get().name(), zone.id().toString()));
        refreshHeatingSection();
        draftLabel.setText(heatingUpdateMessage(updatedHeating, "Heizkreis erzeugt."));
        recomputeHeatingLayoutNow(updatedHeating.id());
    }

    void placeHydronicManifold(PlanPoint point) {
        Optional<ManifoldTarget> target = manifoldTarget(point);
        if (target.isEmpty()) {
            draftLabel.setText("HKV braucht einen Raum oder eine ausgewählte Heizung.");
            return;
        }
        Room room = target.orElseThrow().room();
        HydronicHeating existing = target.orElseThrow().heating();
        PlanPoint returnPoint = new PlanPoint(
                point.xMillimeters() + DEFAULT_HKV_PAIR_DISTANCE_MILLIMETERS,
                point.yMillimeters()
        );
        setLengthInput(heatingSupplyXField, heatingSupplyXUnit, Length.ofMillimeters(point.xMillimeters()), LengthUnit.CENTIMETER);
        setLengthInput(heatingSupplyYField, heatingSupplyYUnit, Length.ofMillimeters(point.yMillimeters()), LengthUnit.CENTIMETER);
        setLengthInput(heatingReturnXField, heatingReturnXUnit, Length.ofMillimeters(returnPoint.xMillimeters()), LengthUnit.CENTIMETER);
        setLengthInput(heatingReturnYField, heatingReturnYUnit, Length.ofMillimeters(returnPoint.yMillimeters()), LengthUnit.CENTIMETER);
        HydronicHeating updated;
        try {
            HydronicHeating base = existing == null
                    ? heatingFromInputs(room, UUID.randomUUID())
                    : existing;
            updated = base.withManifold(point, returnPoint)
                    .withManifoldFreeArea(
                            HydronicHeating.DEFAULT_MANIFOLD_FREE_AREA_WIDTH,
                            HydronicHeating.DEFAULT_MANIFOLD_FREE_AREA_DEPTH
                    );
        } catch (RuntimeException exception) {
            draftLabel.setText("HKV nicht gesetzt: " + UiErrorDialogs.userMessage(exception));
            return;
        }
        rememberStateForUndo();
        if (existing == null) {
            activeLevel.get().addHydronicHeating(updated);
        } else {
            activeLevel.get().replaceHydronicHeating(updated);
        }
        selectSingle(new SelectionKey(RenderableKind.HEATING_MANIFOLD, activeLevel.get().name(), updated.id().toString()));
        refreshHeatingSection();
        draftLabel.setText("HKV gesetzt.");
        render();
    }

    void placeHydronicManifold(PlanSegment bounds) {
        CadWorkbenchHeatingSupport.HeatingZoneBounds freeArea = CadWorkbenchHeatingSupport.heatingZoneBounds(bounds);
        PlanPoint point = freeArea.center();
        Optional<ManifoldTarget> target = manifoldTarget(point);
        if (target.isEmpty()) {
            draftLabel.setText("HKV braucht einen Raum oder eine ausgewählte Heizung.");
            return;
        }
        Room room = target.orElseThrow().room();
        HydronicHeating existing = target.orElseThrow().heating();
        boolean horizontal = freeArea.width() >= freeArea.height();
        PlanPoint supplyPoint = horizontal
                ? new PlanPoint(point.xMillimeters() - DEFAULT_HKV_PAIR_DISTANCE_MILLIMETERS / 2.0, point.yMillimeters())
                : new PlanPoint(point.xMillimeters(), point.yMillimeters() - DEFAULT_HKV_PAIR_DISTANCE_MILLIMETERS / 2.0);
        PlanPoint returnPoint = horizontal
                ? new PlanPoint(point.xMillimeters() + DEFAULT_HKV_PAIR_DISTANCE_MILLIMETERS / 2.0, point.yMillimeters())
                : new PlanPoint(point.xMillimeters(), point.yMillimeters() + DEFAULT_HKV_PAIR_DISTANCE_MILLIMETERS / 2.0);
        setLengthInput(heatingSupplyXField, heatingSupplyXUnit, Length.ofMillimeters(supplyPoint.xMillimeters()), LengthUnit.CENTIMETER);
        setLengthInput(heatingSupplyYField, heatingSupplyYUnit, Length.ofMillimeters(supplyPoint.yMillimeters()), LengthUnit.CENTIMETER);
        setLengthInput(heatingReturnXField, heatingReturnXUnit, Length.ofMillimeters(returnPoint.xMillimeters()), LengthUnit.CENTIMETER);
        setLengthInput(heatingReturnYField, heatingReturnYUnit, Length.ofMillimeters(returnPoint.yMillimeters()), LengthUnit.CENTIMETER);
        HydronicHeating updated;
        try {
            HydronicHeating base = existing == null
                    ? heatingFromInputs(room, UUID.randomUUID())
                    : existing;
            updated = base.withManifold(supplyPoint, returnPoint)
                    .withManifoldFreeArea(
                            Length.ofMillimeters(freeArea.width()),
                            Length.ofMillimeters(freeArea.height())
                    );
        } catch (RuntimeException exception) {
            draftLabel.setText("HKV nicht gesetzt: " + UiErrorDialogs.userMessage(exception));
            return;
        }
        rememberStateForUndo();
        if (existing == null) {
            activeLevel.get().addHydronicHeating(updated);
        } else {
            activeLevel.get().replaceHydronicHeating(updated);
        }
        selectSingle(new SelectionKey(RenderableKind.HEATING_MANIFOLD, activeLevel.get().name(), updated.id().toString()));
        refreshHeatingSection();
        draftLabel.setText("HKV gesetzt.");
        render();
    }

    Optional<ManifoldTarget> manifoldTarget(PlanPoint point) {
        HeatingSurfacePosition surfacePosition = Optional.ofNullable(heatingSurfacePositionSelector.getValue())
                .orElse(HeatingSurfacePosition.FLOOR);
        Optional<Room> pointRoom = roomAt(point);
        if (pointRoom.isPresent()) {
            Room room = pointRoom.orElseThrow();
            return Optional.of(new ManifoldTarget(room, activeLevel.get().findHydronicHeating(room.id(), surfacePosition)));
        }
        Optional<HydronicHeating> selectedHeating = selectedHydronicHeating();
        if (selectedHeating.isPresent()) {
            HydronicHeating heating = selectedHeating.orElseThrow();
            return activeLevel.get().rooms().stream()
                    .filter(room -> room.id().equals(heating.roomId()))
                    .findFirst()
                    .map(room -> new ManifoldTarget(room, heating));
        }
        return selectedRoom()
                .map(room -> new ManifoldTarget(room, activeLevel.get().findHydronicHeating(room.id(), surfacePosition)));
    }

    Optional<Room> roomAt(PlanPoint point) {
        return selectionQueryService.findSelections(activeLevel.get(), point, SNAP_TOLERANCE).stream()
                .filter(selection -> selection.kind() == RenderableKind.ROOM_VOLUME)
                .findFirst()
                .flatMap(selection -> activeLevel.get().rooms().stream()
                        .filter(candidate -> candidate.id().toString().equals(selection.elementId()))
                        .findFirst());
    }

    void startGuideDrag(GuideOrientation orientation, double worldMillimeters) {
        if (!isDirectEditingView()) {
            render();
            draftLabel.setText("Hilfslinien lassen sich aktuell nur in der Draufsicht platzieren.");
            return;
        }
        if (pendingGuideOrientation == null) {
            rememberStateForUndo();
        }
        pendingGuideOrientation = orientation;
        pendingGuideWorldMillimeters = worldMillimeters;
        draftLabel.setText("Hilfslinie: " + formatGuidePosition(orientation, worldMillimeters));
        render();
    }

    void updateGuideDrag(GuideOrientation orientation, double worldMillimeters) {
        if (pendingGuideOrientation == orientation) {
            pendingGuideWorldMillimeters = worldMillimeters;
            draftLabel.setText("Hilfslinie: " + formatGuidePosition(orientation, worldMillimeters));
            render();
        }
    }

    void finishGuideDrag(GuideOrientation orientation, double worldMillimeters) {
        if (pendingGuideOrientation != orientation) {
            return;
        }
        guideLines.add(new GuideLine(orientation, worldMillimeters));
        pendingGuideOrientation = null;
        draftLabel.setText("Hilfslinie platziert: " + formatGuidePosition(orientation, worldMillimeters));
        render();
    }

    void removeNearestGuide(PlanPoint clickPoint) {
        guideDistanceService.nearestGuide(guideLines, clickPoint, SNAP_TOLERANCE)
                .ifPresent(guideLine -> {
                    rememberStateForUndo();
                    guideLines.remove(guideLine);
                });
        render();
    }

    double guideWorldPositionFromHorizontalRuler(MouseEvent event) {
        return snapGuidePoint(projectedPointInDrawingPane(event)).yMillimeters();
    }

    double guideWorldPositionFromVerticalRuler(MouseEvent event) {
        return snapGuidePoint(projectedPointInDrawingPane(event)).xMillimeters();
    }

    Point2D projectedPointInDrawingPane(MouseEvent event) {
        Point2D localPoint = drawingPane.sceneToLocal(event.getSceneX(), event.getSceneY());
        double x = clamp(localPoint.getX(), 0.0, drawingCanvas.getWidth());
        double y = clamp(localPoint.getY(), 0.0, drawingCanvas.getHeight());
        return new Point2D(x, y);
    }

    PlanPoint snapGuidePoint(Point2D point) {
        return snapService.snap(
                screenToWorld(point.getX(), point.getY()),
                currentConstraints(false),
                activeLevel.get().walls()
        );
    }

    String formatGuidePosition(GuideOrientation orientation, double worldMillimeters) {
        String axis = orientation == GuideOrientation.VERTICAL ? "X" : "Y";
        return axis + "=" + String.format(Locale.GERMAN, "%.2f m", worldMillimeters / 1000.0);
    }

    void saveCurrentLevel() {
        if (lastLevelSavePath != null) {
            saveCurrentLevelTo(lastLevelSavePath);
            return;
        }
        saveCurrentLevelAs();
    }

    void saveCurrentLevelAs() {
        FileChooser fileChooser = createCadasFileChooser();
        String levelName = exchangeFileNameService.stripRepeatedExtension(Path.of(activeLevel.get().name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(levelName);
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            return;
        }
        Path targetFile = file.toPath();
        if (!targetFile.getFileName().toString().contains(".")) {
            targetFile = exchangeFileNameService.ensureSingleExtension(targetFile, ".cadas");
        }
        String newLevelName = exchangeFileNameService.stripRepeatedExtension(targetFile.getFileName(), ".cadas");
        if (!newLevelName.isBlank() && !newLevelName.equals(activeLevel.get().name())) {
            rememberStateForUndo();
            project.renameLevel(activeLevel.get(), newLevelName);
            int index = availableLevels.indexOf(activeLevel.get());
            if (index >= 0) {
                availableLevels.set(index, activeLevel.get());
            }
        }
        saveCurrentLevelTo(targetFile);
    }

    void saveCurrentLevelTo(Path targetFile) {
        try {
            Path exportPath = targetFile.toAbsolutePath().normalize();
            levelExchangeService.exportLevel(activeLevel.get(), exportPath);
            lastLevelSavePath = exportPath;
            draftLabel.setText("Etage gesichert: " + exportPath.getFileName());
        } catch (Exception exception) {
            showOperationException("Etagen-Sicherung fehlgeschlagen", exception);
        }
    }

    void exportCurrentLevel(Path targetFile) {
        saveCurrentLevelTo(targetFile);
    }

    void saveProject() {
        if (lastProjectSavePath != null) {
            exportProjectAsDxf(lastProjectSavePath);
            return;
        }
        saveProjectAs();
    }

    void saveProjectAs() {
        FileChooser fileChooser = createCadasFileChooser();
        String projectName = exchangeFileNameService.stripRepeatedExtension(Path.of(project.name().replace(' ', '_')), ".cadas");
        fileChooser.setInitialFileName(projectName);
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            return;
        }
        Path targetFile = file.toPath();
        if (!targetFile.getFileName().toString().contains(".")) {
            targetFile = exchangeFileNameService.ensureSingleExtension(targetFile, ".cadas");
        }
        String newProjectName = exchangeFileNameService.stripRepeatedExtension(targetFile.getFileName(), ".cadas");
        if (!newProjectName.isBlank() && !newProjectName.equals(project.name())) {
            rememberStateForUndo();
            project.rename(newProjectName);
        }
        exportProjectAsDxf(targetFile);
    }

    void exportProjectAsDxf(Path targetFile) {
        try {
            Path exportPath = targetFile.toAbsolutePath().normalize();
            projectExchangeService.exportProject(project, exportPath);
            lastProjectSavePath = exportPath;
            savedChangeRevision = currentChangeRevision;
            confirmExportWritten(exportPath);
        } catch (Exception exception) {
            showOperationException("Gebäude-Sicherung fehlgeschlagen", exception);
        }
    }

    void confirmExportWritten(Path exportPath) {
        if (Files.exists(exportPath) && Files.isRegularFile(exportPath)) {
            if (interactiveDialogsEnabled) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Gebäude gesichert");
                alert.setHeaderText("Die Gebäude-Datei wurde erfolgreich gespeichert.");
                alert.setContentText(exportPath.toString());
                alert.getDialogPane().setPrefWidth(560);
                Window owner = getScene() != null ? getScene().getWindow() : null;
                if (owner != null) {
                    alert.initOwner(owner);
                }
                alert.showAndWait();
            }
            draftLabel.setText("Gebäude gesichert: " + exportPath.getFileName());
        } else {
            draftLabel.setText("Sicherung konnte nicht verifiziert werden: " + exportPath);
        }
    }

    public void showAboutDialog() {
        documentSupport.showAboutDialog();
    }

    void importLevel() {
        FileChooser fileChooser = createCadasFileChooser();
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        importLevel(file.toPath());
    }

    void importThreeDObject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("3D-Objekt aus DXF, IFC oder RFA laden");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "3D-CAD-Dateien", "*.dxf", "*.DXF", "*.ifc", "*.IFC", "*.rfa", "*.RFA"));
        Window window = currentWindow();
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        importThreeDObject(file.toPath());
    }

    void importThreeDObject(Path sourceFile) {
        try {
            Path targetFile = roomObjectPresetService.importTarget(sourceFile);
            if (Files.exists(targetFile)
                    && !isSameFile(sourceFile, targetFile)
                    && !confirmOverwrite(
                    "3D-CAD-Objekt überschreiben",
                    "Ein 3D-CAD-Objekt mit dem Namen `" + sourceFile.getFileName() + "` ist bereits registriert.",
                    "Soll die vorhandene Objektdatei ersetzt werden?"
            )) {
                return;
            }
            RoomObjectPreset preset = roomObjectPresetService.importCad3dObject(sourceFile);
            registerRoomObjectPreset(preset);
            roomObjectPresetSelector.setValue(preset);
            toolSelector.setValue(DrawingTool.OBJECT);
            draftLabel.setText("3D-CAD-Objekt geladen: " + sourceFile.getFileName());
        } catch (IOException | IllegalArgumentException exception) {
            showOperationException("3D-CAD-Import fehlgeschlagen", exception);
        }
    }

    void importLevel(Path sourceFile) {
        try {
            rememberStateForUndo();
            String levelName = uniqueLevelName(exchangeFileNameService.stripRepeatedExtension(sourceFile, ".cadas"));
            Level importedLevel = levelExchangeService.importLevel(sourceFile, levelName);
            importedLevel.replaceRooms(autoRoomGenerationService.synchronize(importedLevel, currentRoomDefaults()));
            project.addLevel(importedLevel);
            availableLevels.add(importedLevel);
            activateLevel(importedLevel);
            fitCurrentViewToContent();
            lastLevelSavePath = sourceFile.toAbsolutePath().normalize();
            draftLabel.setText("Etage geladen: " + sourceFile.getFileName());
        } catch (Exception exception) {
            showOperationException("Etagen-Laden fehlgeschlagen", exception);
        }
    }

    void importProjectFromDxf() {
        FileChooser fileChooser = createCadasFileChooser();
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        importProjectFromDxf(file.toPath());
    }

    void importProjectFromDxf(Path sourceFile) {
        try {
            rememberStateForUndo();
            String projectName = exchangeFileNameService.stripRepeatedExtension(sourceFile, ".cadas");
            ProjectModel importedProject = projectExchangeService.importProject(sourceFile, projectName);
            importedProject.levels().forEach(level -> level.replaceRooms(autoRoomGenerationService.synchronize(level, currentRoomDefaults())));
            project.replaceWith(importedProject);
            availableLevels.setAll(project.levels());
            guideLines.clear();
            clearSelectionsInternal();
            activateLevel(project.primaryLevel());
            clearHeatingLayoutCache();
            markThreeDDirty();
            fitCurrentViewToContent();
            lastProjectSavePath = sourceFile.toAbsolutePath().normalize();
            savedChangeRevision = currentChangeRevision;
            draftLabel.setText("Gebäude geladen: " + sourceFile.getFileName());
        } catch (Exception exception) {
            showOperationException("Gebäude-Laden fehlgeschlagen", exception);
        }
    }

    FileChooser createCadasFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("CADas-Gebäudedatei auswählen");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CADas-Gebäudedateien", "*.cadas"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DXF-Dateien", "*.dxf"));
        fileChooser.setInitialDirectory(Path.of(System.getProperty("user.home")).toFile());
        return fileChooser;
    }

    String uniqueLevelName(String baseName) {
        String candidate = baseName;
        int suffix = 2;
        while (containsLevelName(candidate)) {
            candidate = baseName + " " + suffix;
            suffix++;
        }
        return candidate;
    }

    boolean containsLevelName(String candidate) {
        return availableLevels.stream().anyMatch(level -> level.name().equalsIgnoreCase(candidate));
    }

    void importPartLibrary() {
        FileChooser fileChooser = createPartLibraryFileChooser();
        Window window = getScene() != null ? getScene().getWindow() : null;
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        importPartLibrary(file.toPath());
    }

    FileChooser createPartLibraryFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Teilebibliothek auswählen");
        fileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Unterstützte Bibliotheken", "*.cadasparts", "*.dwg", "*.DWG"),
                new FileChooser.ExtensionFilter("CADas Teilebibliothek", "*.cadasparts"),
                new FileChooser.ExtensionFilter("DWG-Bibliothek", "*.dwg", "*.DWG"),
                new FileChooser.ExtensionFilter("Alle Dateien", "*.*")
        );
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().getFirst());
        return fileChooser;
    }

    void importPartLibrary(Path sourceFile) {
        String fileName = sourceFile.getFileName().toString();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".dwg")) {
            registerDwgLibrary(sourceFile, true);
            return;
        }
        try {
            StandardPartLibrary importedLibrary = partLibraryImportService.importLibrary(sourceFile);
            availableDoorPresets.addAll(importedLibrary.doorPresets());
            availableWindowPresets.addAll(importedLibrary.windowPresets());
            availableStairPresets.addAll(importedLibrary.stairPresets());
            if (!cadLibraryReferences.contains(sourceFile)) {
                cadLibraryReferences.add(sourceFile);
            }
            updateCadLibrarySummary();
            if (!importedLibrary.doorPresets().isEmpty()) {
                doorPresetSelector.setValue(importedLibrary.doorPresets().getFirst());
            }
            if (!importedLibrary.windowPresets().isEmpty()) {
                windowPresetSelector.setValue(importedLibrary.windowPresets().getFirst());
            }
            if (!importedLibrary.stairPresets().isEmpty()) {
                stairPresetSelector.setValue(importedLibrary.stairPresets().getFirst());
            }
            draftLabel.setText("Teilebibliothek geladen: " + fileName);
        } catch (Exception exception) {
            showOperationException("Teilebibliothek fehlgeschlagen", exception);
        }
    }
}
