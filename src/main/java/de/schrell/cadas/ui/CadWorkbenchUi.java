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

abstract class CadWorkbenchUi extends CadWorkbenchBase {

    void configureControls() {
        initializeUnitSelectors();
        levelSelector.setItems(availableLevels);
        levelSelector.setValue(activeLevel.get());
        toolSelector.getItems().setAll(java.util.Arrays.stream(DrawingTool.values())
                .filter(tool -> tool != DrawingTool.HEATING_MANIFOLD)
                .toList());
        toolSelector.setValue(DrawingTool.EDIT);
        floorExtensionTypeSelector.getItems().setAll(FloorExtensionType.values());
        floorExtensionTypeSelector.setValue(FloorExtensionType.BALCONY);
        floorExtensionPlacementSelector.getItems().setAll(FloorExtensionPlacement.values());
        floorExtensionPlacementSelector.setValue(FloorExtensionPlacement.EXTERIOR);
        initializePresetSelectors();
        initializeSurfaceLayerControls();
        initializeHeatingControls();
        initializeDwgLibraryControls();
        levelSelector.valueProperty().addListener((ignored, oldValue, newValue) -> {
            if (newValue != null) {
                activateLevel(newValue);
            }
        });

        applyFormTooltips();

        registerRenderListener(snapToGrid);
        registerRenderListener(showGrid);
        registerRenderListener(snapToEndpoints);
        registerRenderListener(showCompass);
        registerRenderListener(showDimensions);
        registerRenderListener(dimensionTextStyle);
        registerRenderListener(showAreaVolume);
        registerRenderListener(showTerrainInPlan);
        registerRenderListener(showHeatingCircuits);
        registerRenderListener(showVariothermCircles);
        registerRenderListener(showGuides);
        registerRenderListener(showGuideDistances);
        registerRenderListener(snapToGuides);
        registerRenderListener(snapToWalls);
        northAngleField.textProperty().addListener((ignored, oldValue, newValue) -> render());
        showRoomObjects.addListener((ignored, oldValue, newValue) -> {
            threeDViewport.setRoomObjectsVisible(newValue);
            markThreeDDirty();
            render();
        });
        activeView.addListener((ignored, oldValue, newValue) -> {
            fitCurrentViewToContent();
            render();
        });
        activeWorkspaceMode.addListener((ignored, oldValue, newValue) -> {
            updateWorkspaceMode();
            if (newValue == WorkspaceMode.THREE_D) {
                if (keepViewportOrbitPoseOnNextThreeDActivation) {
                    keepViewportOrbitPoseOnNextThreeDActivation = false;
                } else {
                    threeDViewport.activateOrbitView();
                }
                refreshThreeDIfNeeded();
            } else if (newValue == WorkspaceMode.INTERIOR) {
                refreshThreeDIfNeeded();
            }
            render();
        });
        toolSelector.valueProperty().addListener((ignored, oldValue, newValue) -> {
            storePropertySectionExpansionState(oldValue);
            restorePropertySectionExpansionState(newValue);
            updatePropertySectionVisibility();
            updateActionButtons();
            updateStatus();
            updateMouseCursor();
            render();
        });
        configureActionButtons();
        registerConfiguredDwgLibraries();
    }

    void configureLayout() {
        MenuBar menuBar = buildMenuBar();
        menuBar.setUseSystemMenuBar(true);
        // Im Fenster wird die MenuBar nicht dargestellt, damit sie keinen
        // Leerraum oberhalb der Werkzeugleiste erzeugt. Unter macOS wandert
        // das echte Menü in die Systemmenüleiste; auf anderen Plattformen
        // bleibt es sichtbar.
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            menuBar.setManaged(false);
            menuBar.setVisible(false);
        }
        ToolBar settingsBar = buildSettingsBar();
        ToolBar viewOptionsBar = buildViewOptionsBar();
        HBox viewBar = buildViewBar();
        VBox topArea = new VBox(8.0, menuBar, settingsBar, viewOptionsBar, viewBar);
        topArea.setPadding(new Insets(0, 0, 12, 0));
        setTop(topArea);

        Region rulerCorner = new Region();
        rulerCorner.setPrefSize(RULER_SIZE, RULER_SIZE);
        rulerCorner.setStyle("-fx-background-color: #e7decd;");
        BorderPane rulerHeader = new BorderPane();
        rulerHeader.setLeft(rulerCorner);
        rulerHeader.setCenter(horizontalRuler);
        drawingArea.setTop(rulerHeader);
        drawingArea.setLeft(verticalRuler);
        drawingArea.setCenter(new StackPane(drawingPane));
        drawingArea.setStyle("-fx-background-color: rgba(255,255,255,0.55); -fx-background-radius: 16;");
        workspacePane.getChildren().setAll(drawingArea, threeDViewport);
        SplitPane splitPane = new SplitPane(buildPropertyPane(), workspacePane);
        splitPane.setDividerPositions(0.22);
        setCenter(splitPane);

        HBox statusBar = new HBox(18.0, viewLabel, zoomLabel, cursorLabel, draftLabel);
        statusBar.setPadding(new Insets(10, 16, 0, 16));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-font-size: 12px; -fx-text-fill: #2f2a24;");
        setBottom(statusBar);
    }

    ToolBar buildSettingsBar() {
        Button addLevelButton = createActionButton(
                "Etage hinzufügen",
                null,
                this::createLevel,
                "Legt eine neue Etage für den aktuellen Grundriss an und wechselt direkt in diese Etage."
        );
        this.addLevelButton = addLevelButton;

        Button renameLevelButton = createActionButton(
                "Etage umbenennen",
                null,
                this::renameCurrentLevel,
                "Benennt die aktuell ausgewählte Etage um. Der Name muss innerhalb des Projekts eindeutig sein."
        );
        this.renameLevelButton = renameLevelButton;

        Button moveLevelUpButton = createActionButton(
                "Etage hoch",
                null,
                this::moveCurrentLevelUp,
                "Verschiebt die aktuell ausgewählte Etage eine Position nach oben in der Etagenreihenfolge."
        );
        this.moveLevelUpButton = moveLevelUpButton;

        Button moveLevelDownButton = createActionButton(
                "Etage runter",
                null,
                this::moveCurrentLevelDown,
                "Verschiebt die aktuell ausgewählte Etage eine Position nach unten in der Etagenreihenfolge."
        );
        this.moveLevelDownButton = moveLevelDownButton;

        Button terrainButton = createActionButton(
                "Gelände",
                null,
                this::editTerrainElevations,
                "Erklärt die Geländebearbeitung. Geländehöhen werden in der 2D-Ansicht per Rechtsklick im 40-cm-Geländeband außerhalb des Gebäudes gesetzt."
        );

        settingsBarStyling();
        return new ToolBar(
                labelledNode("Werkzeug", toolSelector),
                new Separator(Orientation.VERTICAL),
                labelledNode("Etage", levelSelector),
                addLevelButton,
                renameLevelButton,
                moveLevelUpButton,
                moveLevelDownButton,
                terrainButton,
                new Separator(Orientation.VERTICAL),
                undoButton,
                redoButton,
                deleteSelectionButton,
                clearSelectionButton
        );
    }

    ToolBar buildViewOptionsBar() {
        CheckBox gridBox = new CheckBox("Raster");
        gridBox.selectedProperty().bindBidirectional(showGrid);
        applyTooltip(gridBox, "Blendet das sichtbare Raster der 2D-Zeichenfläche ein oder aus, ohne den Raster-Snap zu verändern.");

        CheckBox snapRasterBox = new CheckBox("Raster-Snap");
        snapRasterBox.selectedProperty().bindBidirectional(snapToGrid);
        applyTooltip(snapRasterBox, "Aktiviert das magnetische Einrasten auf das konfigurierte Raster.");

        CheckBox snapPointsBox = new CheckBox("Punkt-Snap");
        snapPointsBox.selectedProperty().bindBidirectional(snapToEndpoints);
        applyTooltip(snapPointsBox, "Aktiviert das magnetische Einrasten auf vorhandene Linien-Endpunkte.");

        CheckBox guideDistancesBox = new CheckBox("Hilfslinienabstände");
        guideDistancesBox.selectedProperty().bindBidirectional(showGuideDistances);
        applyTooltip(guideDistancesBox, "Zeigt beim Herausziehen einer Hilfslinie die Abstände zu allen vorhandenen parallelen Hilfslinien an.");

        CheckBox snapGuidesBox = new CheckBox("Hilfslinien-Snap");
        snapGuidesBox.selectedProperty().bindBidirectional(snapToGuides);
        applyTooltip(snapGuidesBox, "Lässt Wände, Türen und Fenster mit Kanten oder Mittellinie magnetisch an sichtbaren Hilfslinien einrasten.");

        CheckBox snapWallsBox = new CheckBox("Wand-Snap");
        snapWallsBox.selectedProperty().bindBidirectional(snapToWalls);
        applyTooltip(snapWallsBox, "Lässt neue oder verschobene Wände an Achsen, Außenkanten und Endkanten anderer Wände einrasten.");

        CheckBox dimensionsBox = new CheckBox("ISO-Bemaßung");
        dimensionsBox.selectedProperty().bindBidirectional(showDimensions);
        applyTooltip(dimensionsBox, "Blendet die ISO-Bemaßung nach DIN EN ISO 7519 | 2025-01 mit Maß-, Maßhilfs- und Begrenzungslinien ein oder aus.");

        CheckBox dimensionTextPartsBox = new CheckBox("Erweiterte Maßtexte");
        dimensionTextPartsBox.setSelected(dimensionTextStyle.get() == DimensionTextStyle.FULL);
        dimensionTextPartsBox.selectedProperty().addListener((obs, wasFull, isFull) ->
                dimensionTextStyle.set(Boolean.TRUE.equals(isFull) ? DimensionTextStyle.FULL : DimensionTextStyle.LENGTH_ONLY));
        dimensionTextStyle.addListener((obs, oldStyle, newStyle) ->
                dimensionTextPartsBox.setSelected(newStyle == DimensionTextStyle.FULL));
        applyTooltip(dimensionTextPartsBox,
                "Bestimmt den Textanteil der Maßangaben in 2D-Ansicht und Bauzeichnung-PDF. " +
                "Aktiviert: vollständige Texte mit Raumname, Raummaß und Außenmaß-Vorsatz. " +
                "Deaktiviert: ausschließlich die nackte Länge, z. B. \"4,20 m\".");

        CheckBox objectsBox = new CheckBox("Objekte");
        objectsBox.selectedProperty().bindBidirectional(showRoomObjects);
        applyTooltip(objectsBox, "Blendet platzierte Raumobjekte gemeinsam in 2D, Innenansicht und 3D ein oder aus.");

        CheckBox terrainPlanBox = new CheckBox("Gelände 2D");
        terrainPlanBox.selectedProperty().bindBidirectional(showTerrainInPlan);
        applyTooltip(terrainPlanBox, "Blendet das Gelände in der 2D-Ansicht als Band außerhalb des Gebäudes ein oder aus. Seitenansichten und 3D bleiben davon unberührt.");

        CheckBox heatingCircuitsBox = new CheckBox("Heizkreise");
        heatingCircuitsBox.selectedProperty().bindBidirectional(showHeatingCircuits);
        applyTooltip(heatingCircuitsBox, "Blendet Heizkreisflächen, Vorlauf, Rücklauf, Anschlussmarker und Startpunkte global in der 2D-Ansicht ein oder aus. Planung, Auswahl und Materialauswertung bleiben unverändert.");

        CheckBox variothermCirclesBox = new CheckBox("Variotherm-Kreise");
        variothermCirclesBox.selectedProperty().bindBidirectional(showVariothermCircles);
        applyTooltip(variothermCirclesBox, "Blendet die Kreis-Markierungen der Variotherm-Trockenbauplatten global in der 2D-Ansicht ein oder aus. Die Belagsgeometrie und Mengenberechnung bleiben unverändert.");

        return new ToolBar(
                gridBox,
                snapRasterBox,
                snapPointsBox,
                guideDistancesBox,
                snapGuidesBox,
                snapWallsBox,
                new Separator(Orientation.VERTICAL),
                dimensionsBox,
                dimensionTextPartsBox,
                objectsBox,
                terrainPlanBox,
                heatingCircuitsBox,
                variothermCirclesBox
        );
    }

    HBox buildViewBar() {
        HBox box = new HBox(8.0);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(new Label("Arbeitsbereich:"));
        box.getChildren().add(workspaceModeButton(WorkspaceMode.TWO_D));
        box.getChildren().add(workspaceModeButton(WorkspaceMode.THREE_D));
        box.getChildren().add(workspaceModeButton(WorkspaceMode.INTERIOR));
        box.getChildren().add(new Separator(Orientation.VERTICAL));
        box.getChildren().add(new Label("2D-Ansichten:"));
        box.getChildren().add(viewButton("⤒ Oben", () -> activeView.set(ViewOrientation.TOP), "Schaltet auf die feste Draufsicht um."));
        box.getChildren().add(viewButton("⤓ Unten", () -> activeView.set(ViewOrientation.BOTTOM), "Schaltet auf die feste Untersicht um."));
        box.getChildren().add(viewButton("↑", () -> activeView.set(activeView.get().rotateUp()), "Dreht das Modell aus der aktuellen 2D-Ansicht nach oben."));
        box.getChildren().add(viewButton("↓", () -> activeView.set(activeView.get().rotateDown()), "Dreht das Modell aus der aktuellen 2D-Ansicht nach unten."));
        box.getChildren().add(viewButton("←", () -> activeView.set(activeView.get().rotateLeft()), "Dreht das Modell aus der aktuellen 2D-Ansicht nach links."));
        box.getChildren().add(viewButton("→", () -> activeView.set(activeView.get().rotateRight()), "Dreht das Modell aus der aktuellen 2D-Ansicht nach rechts."));
        box.getChildren().add(viewButton(
                "2D zentrieren",
                this::resetTwoDView,
                "Setzt Zoom und Verschiebung der Zeichenfläche auf die Startansicht zurück."
        ));
        return box;
    }

    Button viewButton(String label, Runnable action, String tooltipText) {
        Button button = new Button(label);
        button.setOnAction(event -> runGuardedAction(label, action));
        button.setStyle("-fx-background-radius: 999; -fx-padding: 8 14 8 14;");
        applyTooltip(button, tooltipText);
        return button;
    }

    Button workspaceModeButton(WorkspaceMode workspaceMode) {
        Button button = new Button(workspaceMode.label());
        button.setOnAction(event -> runGuardedAction(workspaceMode.label() + "-Arbeitsbereich", () -> selectWorkspaceMode(workspaceMode, true)));
        button.setStyle(workspaceModeButtonStyle(workspaceMode == activeWorkspaceMode.get()));
        activeWorkspaceMode.addListener((ignored, oldValue, newValue) ->
                button.setStyle(workspaceModeButtonStyle(workspaceMode == newValue)));
        applyTooltip(button, switch (workspaceMode) {
            case TWO_D -> "Zeigt die 2D-Zeichenfläche im großen Mittelbereich an.";
            case THREE_D -> "Zeigt die 3D-Orbitansicht im großen Mittelbereich an und spart Platz gegenüber der Parallelansicht.";
            case INTERIOR -> "Öffnet die 3D-Innenansicht im aktuell ausgewählten Raum oder im ersten Raum der aktiven Etage.";
        });
        return button;
    }

    String workspaceModeButtonStyle(boolean active) {
        return active
                ? "-fx-background-color: #4b6a88; -fx-text-fill: white; -fx-background-radius: 999; -fx-padding: 8 16 8 16;"
                : "-fx-background-radius: 999; -fx-padding: 8 16 8 16;";
    }

    void settingsBarStyling() {
        gridField.setPrefColumnCount(6);
        lengthField.setPrefColumnCount(6);
        angleField.setPrefColumnCount(6);
        northAngleField.setPrefColumnCount(6);
        wallThicknessField.setPrefColumnCount(6);
        wallHeightField.setPrefColumnCount(6);
        roomNameField.setPrefColumnCount(8);
        roomHeightField.setPrefColumnCount(6);
        floorThicknessField.setPrefColumnCount(6);
        ceilingThicknessField.setPrefColumnCount(6);
        endpointHeightField.setPrefColumnCount(6);
        kneeWallHeightField.setPrefColumnCount(6);
        doorWidthField.setPrefColumnCount(6);
        doorHeightField.setPrefColumnCount(6);
        thresholdField.setPrefColumnCount(6);
        windowWidthField.setPrefColumnCount(6);
        windowHeightField.setPrefColumnCount(6);
        sillHeightField.setPrefColumnCount(6);
        stairHeightField.setPrefColumnCount(6);
        stairStepsField.setPrefColumnCount(6);
        surfaceLayerThicknessField.setPrefColumnCount(6);
        surfaceTileWidthField.setPrefColumnCount(6);
        surfaceTileHeightField.setPrefColumnCount(6);
        surfaceLayoutOffsetField.setPrefColumnCount(6);
        surfaceMinimumOffsetField.setPrefColumnCount(6);
        surfaceMinimumEdgeWidthField.setPrefColumnCount(6);
        surfaceMinimumStartEndMarginField.setPrefColumnCount(6);
        surfaceJointWidthField.setPrefColumnCount(6);
        levelSelector.setPrefWidth(180);
        toolSelector.setPrefWidth(140);
        doorPresetSelector.setPrefWidth(190);
        windowPresetSelector.setPrefWidth(210);
        stairPresetSelector.setPrefWidth(190);
        roomObjectPresetSelector.setPrefWidth(210);
        roomObjectNameField.setPrefColumnCount(10);
        roomObjectWidthField.setPrefColumnCount(6);
        roomObjectDepthField.setPrefColumnCount(6);
        roomObjectHeightField.setPrefColumnCount(6);
        roomObjectHeatOutputField.setPrefColumnCount(6);
        roomObjectHeatingTypeSelector.setPrefWidth(170);
        roomObjectAngleField.setPrefColumnCount(6);
        dwgBlockNameField.setPrefColumnCount(14);
    }

    void updateWorkspaceMode() {
        boolean showTwoD = activeWorkspaceMode.get() == WorkspaceMode.TWO_D;
        drawingArea.setVisible(showTwoD);
        drawingArea.setManaged(showTwoD);
        threeDViewport.setVisible(!showTwoD);
        threeDViewport.setManaged(!showTwoD);
    }

    void switchToThreeDWorkspaceFromViewport() {
        if (activeWorkspaceMode.get() == WorkspaceMode.THREE_D) {
            return;
        }
        keepViewportOrbitPoseOnNextThreeDActivation = true;
        activeWorkspaceMode.set(WorkspaceMode.THREE_D);
    }

    boolean activateInteriorViewForCurrentRoom() {
        Optional<Room> targetRoom = selectedSurfaceRoom()
                .or(this::selectedRoom)
                .or(() -> activeLevel.get().rooms().stream().findFirst());
        if (targetRoom.isEmpty()) {
            draftLabel.setText("Innenansicht braucht einen Raum auf der aktiven Etage.");
            return false;
        }
        threeDViewport.activateInteriorView(project, activeLevel.get(), targetRoom.get());
        return true;
    }

    void selectWorkspaceMode(WorkspaceMode workspaceMode, boolean showErrorDialog) {
        if (workspaceMode == WorkspaceMode.INTERIOR && !activateInteriorViewForCurrentRoom()) {
            if (showErrorDialog) {
                showInteriorViewUnavailableError();
            }
            return;
        }
        activeWorkspaceMode.set(workspaceMode);
        updateWorkspaceMode();
        refreshThreeDIfNeeded();
    }

    void showInteriorViewUnavailableError() {
        draftLabel.setText("Innenansicht nicht verfügbar: Es wird ein Raum benötigt.");
        if (!interactiveDialogsEnabled) {
            return;
        }
        Alert alert = new Alert(
                Alert.AlertType.WARNING,
                "Auf der aktiven Etage ist kein Raum vorhanden. Schließe zuerst einen Wandzug, damit CADas einen Raum ableiten kann.",
                ButtonType.OK
        );
        alert.setTitle("Innenansicht nicht verfügbar");
        alert.setHeaderText("Für die Innenansicht wird ein Raum benötigt.");
        Window window = getScene() != null ? getScene().getWindow() : null;
        if (window != null) {
            alert.initOwner(window);
        }
        alert.showAndWait();
    }

    void editTerrainElevations() {
        if (!interactiveDialogsEnabled) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Gelände bearbeiten");
        alert.setHeaderText("Geländehöhen werden direkt im 2D-Geländeband gesetzt.");
        alert.setContentText("Blende `Gelände 2D` ein und setze die Höhe per Rechtsklick im 40-cm-Band außerhalb der Außenwände.");
        Window owner = currentWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    boolean handleTerrainBandContextClick(MouseEvent event) {
        if (!projectionService.isPlanView(activeView.get()) || !showTerrainInPlan.get()) {
            return false;
        }
        List<PlanPoint> contour = terrainContourService.contour(project);
        if (contour.size() < 3) {
            draftLabel.setText("Für das Gelände wird zuerst eine geschlossene Außenkontur benötigt.");
            return true;
        }
        PlanPoint clickPoint = screenToWorld(event.getX(), event.getY());
        Optional<TerrainProfileService.ProjectedTerrainPoint> existingSample = terrainEditService.existingSampleNear(
                project.terrain(),
                contour,
                clickPoint,
                TerrainEditService.EXISTING_POINT_SELECTION_TOLERANCE_MILLIMETERS
        );
        Optional<TerrainProfileService.ProjectedTerrainPoint> projection = existingSample.isPresent()
                ? existingSample
                : terrainEditService.resolveEditTarget(project.terrain(), contour, clickPoint);
        if (projection.isEmpty()) {
            return false;
        }
        if (existingSample.isPresent()) {
            editExistingTerrainPoint(projection.orElseThrow(), existingSample.orElseThrow(), contour);
        } else {
            editTerrainPoint(projection.orElseThrow(), contour);
        }
        return true;
    }

    void editTerrainPoint(
            TerrainProfileService.ProjectedTerrainPoint projection,
            List<PlanPoint> contour
    ) {
        editTerrainPointDialog(projection, null, contour);
    }

    void editExistingTerrainPoint(
            TerrainProfileService.ProjectedTerrainPoint projection,
            TerrainProfileService.ProjectedTerrainPoint existingSample,
            List<PlanPoint> contour
    ) {
        editTerrainPointDialog(projection, existingSample, contour);
    }

    void editTerrainPointDialog(
            TerrainProfileService.ProjectedTerrainPoint projection,
            TerrainProfileService.ProjectedTerrainPoint existingSample,
            List<PlanPoint> contour
    ) {
        if (!interactiveDialogsEnabled) {
            return;
        }
        Length currentElevation = existingSample == null
                ? terrainEditService.currentElevation(project.terrain(), contour, projection)
                : existingSample.elevation();
        TextField elevationField = new TextField(formatValue(
                currentElevation,
                LengthUnit.CENTIMETER,
                LENGTH_INPUT_DECIMALS
        ));
        elevationField.setPrefColumnCount(8);
        applyTooltip(elevationField, "Legt die Geländehöhe an diesem Punkt relativ zum Boden der untersten Etage in Zentimetern fest. Der Punkt wird entlang der Außenkontur gespeichert.");
        HBox row = new HBox(10.0,
                new Label(String.format(Locale.GERMAN, "Punkt bei %.2f / %.2f m", projection.bandPoint().xMillimeters() / 1_000.0, projection.bandPoint().yMillimeters() / 1_000.0)),
                elevationField,
                new Label("cm"));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Geländepunkt bearbeiten");
        dialog.setHeaderText("Höhe über dem Boden der untersten Etage");
        dialog.getDialogPane().setContent(row);
        ButtonType deleteButtonType = new ButtonType("Löschen", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.OK, ButtonType.CANCEL);
        Window owner = currentWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        javafx.scene.Node deleteButton = dialog.getDialogPane().lookupButton(deleteButtonType);
        deleteButton.setDisable(existingSample == null);
        applyTooltip(deleteButton, existingSample != null
                ? "Entfernt den gewählten gespeicherten Geländepunkt und interpoliert das Gelände aus den übrigen Punkten neu."
                : "Ist nur verfügbar, wenn ein bereits gespeicherter Geländepunkt ausgewählt wurde.");
        applyTooltip(dialog.getDialogPane().lookupButton(ButtonType.OK), "Übernimmt die Geländehöhe an diesem Bandpunkt und aktualisiert 2D, Seitenansichten, 3D und PDF.");
        applyTooltip(dialog.getDialogPane().lookupButton(ButtonType.CANCEL), "Verwirft die Eingabe und belässt das Gelände unverändert.");
        Optional<ButtonType> decision = dialog.showAndWait();
        if (decision.isEmpty() || ButtonType.CANCEL.equals(decision.orElseThrow())) {
            return;
        }
        rememberStateForUndo();
        if (deleteButtonType.equals(decision.orElseThrow())) {
            project.defineTerrain(terrainEditService.deletePoint(project.terrain(), contour, existingSample));
            draftLabel.setText("Geländepunkt entfernt.");
        } else {
            Length elevation = parseLength(elevationField, LengthUnit.CENTIMETER).orElse(currentElevation);
            if (existingSample == null) {
                project.defineTerrain(terrainEditService.upsertPoint(project.terrain(), contour, projection, elevation));
            } else {
                project.defineTerrain(terrainEditService.replacePoint(project.terrain(), contour, projection, existingSample, elevation));
            }
            draftLabel.setText("Geländepunkt aktualisiert.");
        }
        markThreeDDirty();
        render();
    }

    MenuBar buildMenuBar() {
        Menu dateiMenu = new Menu("Datei");
        dateiMenu.getItems().addAll(
                menuItem("Etage hinzufügen", this::createLevel, shortcutKey(KeyCode.N)),
                menuItem("Projekt leeren", this::clearProject, shortcutKey(KeyCode.L)),
                menuItem("Laden", this::importProjectFromDxf, shortcutShiftKey(KeyCode.I)),
                menuItem("Sichern", this::saveProject, shortcutKey(KeyCode.S)),
                menuItem("Sichern als ...", this::saveProjectAs, shortcutShiftKey(KeyCode.S)),
                menuItem("Etage laden", this::importLevel, null),
                menuItem("Etage sichern", this::saveCurrentLevel, null),
                menuItem("Etage sichern als ...", this::saveCurrentLevelAs, null),
                menuItem("Teilebibliothek laden", this::importPartLibrary, shortcutShiftKey(KeyCode.B)),
                menuItem("3D-Objekt aus DXF/IFC/RFA laden", this::importThreeDObject, null),
                menuItem("Beenden", this::requestApplicationExit, shortcutKey(KeyCode.Q))
        );

        Menu bearbeitenMenu = new Menu("Bearbeiten");
        bearbeitenMenu.getItems().addAll(
                menuItem("Rückgängig", this::undo, shortcutKey(KeyCode.Z)),
                menuItem("Wiederherstellen", this::redo, shortcutShiftKey(KeyCode.Z)),
                menuItem("Eigenschaften auf Auswahl anwenden", this::applyCurrentInputsToSelection, shortcutShiftKey(KeyCode.P)),
                menuItem("Auswahl löschen", this::deleteSelection, new KeyCodeCombination(KeyCode.DELETE)),
                menuItem("Auswahl aufheben", this::clearSelection, new KeyCodeCombination(KeyCode.ESCAPE))
        );

        Menu ansichtMenu = new Menu("Ansicht");
        ansichtMenu.getItems().addAll(
                menuItem("2D-Arbeitsbereich", () -> selectWorkspaceMode(WorkspaceMode.TWO_D, true), null),
                menuItem("3D-Arbeitsbereich", () -> selectWorkspaceMode(WorkspaceMode.THREE_D, true), null),
                menuItem("3D-Innenansicht", () -> selectWorkspaceMode(WorkspaceMode.INTERIOR, true), null),
                new SeparatorMenuItem()
        );
        for (ViewOrientation viewOrientation : ViewOrientation.values()) {
            ansichtMenu.getItems().add(menuItem(
                    "Zu " + viewOrientation.label(),
                    () -> activeView.set(viewOrientation),
                    null
            ));
        }
        ansichtMenu.getItems().addAll(
                menuItem("2D-Ansicht zentrieren", this::resetTwoDView, shortcutKey(KeyCode.DIGIT0)),
                menuItem("3D-Ansicht zentrieren", threeDViewport::centerCurrentView, shortcutShiftKey(KeyCode.DIGIT0))
        );

        Menu werkzeugMenu = new Menu("Werkzeuge");
        werkzeugMenu.getItems().addAll(
                toolMenuItem(DrawingTool.EDIT, KeyCode.E),
                toolMenuItem(DrawingTool.WALL, KeyCode.W),
                toolMenuItem(DrawingTool.STAIR, KeyCode.T),
                toolMenuItem(DrawingTool.FLOOR_EXTENSION, KeyCode.G),
                menuItem(DrawingTool.HEATING_ZONE_RECTANGLE.label(), () -> toolSelector.setValue(DrawingTool.HEATING_ZONE_RECTANGLE), null),
                menuItem(DrawingTool.HEATING_EXCLUSION_RECTANGLE.label(), () -> toolSelector.setValue(DrawingTool.HEATING_EXCLUSION_RECTANGLE), null),
                menuItem("Heizkreis-Router Vario testen", this::showHeatingCircuitRoutingWindow, null),
                toolMenuItem(DrawingTool.DOOR, KeyCode.D),
                toolMenuItem(DrawingTool.WINDOW, KeyCode.F),
                menuItem(DrawingTool.ROOF_WINDOW.label(), () -> toolSelector.setValue(DrawingTool.ROOF_WINDOW), null),
                toolMenuItem(DrawingTool.OBJECT, KeyCode.O),
                new SeparatorMenuItem(),
                menuItem("Geländehöhen bearbeiten", this::editTerrainElevations, null),
                menuItem("Ausgewählte Bauteile 90° rechts drehen", this::rotateSelectedComponentsClockwise, shortcutShiftKey(KeyCode.RIGHT)),
                menuItem("Ausgewählte Bauteile 90° links drehen", this::rotateSelectedComponentsCounterClockwise, shortcutShiftKey(KeyCode.LEFT)),
                menuItem("Ausgewählte Heizkreise horizontal spiegeln", () -> mirrorSelectedHeatingZones(true), null),
                menuItem("Ausgewählte Heizkreise vertikal spiegeln", () -> mirrorSelectedHeatingZones(false), null)
        );

        Menu optionenMenu = new Menu("Optionen");
        optionenMenu.getItems().addAll(
                checkMenuItem("Raster anzeigen", showGrid),
                checkMenuItem("Auf Raster einrasten", snapToGrid),
                checkMenuItem("Auf Punkte einrasten", snapToEndpoints),
                checkMenuItem("Hilfslinien anzeigen", showGuides),
                checkMenuItem("Hilfslinienabstände anzeigen", showGuideDistances),
                checkMenuItem("An Hilfslinien einrasten", snapToGuides),
                checkMenuItem("An anderen Wänden einrasten", snapToWalls),
                checkMenuItem("ISO-Bemaßung anzeigen", showDimensions),
                checkMenuItem("Erweiterte Maßtexte anzeigen", dimensionTextStyle, DimensionTextStyle.FULL, DimensionTextStyle.LENGTH_ONLY),
                checkMenuItem("Objekte anzeigen", showRoomObjects),
                checkMenuItem("Gelände in 2D anzeigen", showTerrainInPlan),
                checkMenuItem("Heizkreise anzeigen", showHeatingCircuits),
                checkMenuItem("Variotherm-Kreise anzeigen", showVariothermCircles),
                checkMenuItem("Fläche und Volumen anzeigen", showAreaVolume),
                checkMenuItem("Nordpfeil anzeigen", showCompass)
        );

        Menu berichteMenu = new Menu("Berichte");
        berichteMenu.getItems().addAll(
                menuItem("Bauzeichnung als PDF exportieren (Rastergrafik)", documentSupport::exportConstructionDrawingPdfRaster, null),
                menuItem("Räume und Materialien als PDF exportieren (Rastergrafik)", documentSupport::exportSurfaceMaterialReportPdfRaster, null),
                menuItem("Bauzeichnung als PDF exportieren", documentSupport::exportConstructionDrawingPdf, shortcutKey(KeyCode.P)),
                menuItem("Räume und Materialien anzeigen", documentSupport::showSurfaceMaterialReportWindow, null),
                menuItem("Räume und Materialien als PDF exportieren (SVG-Heizpläne)", documentSupport::exportSurfaceMaterialReportPdf, null),
                menuItem("Räume und Materialien als MD exportieren", documentSupport::exportSurfaceMaterialReportMarkdown, null)
        );

        Menu hilfeMenu = new Menu("Hilfe");
        hilfeMenu.getItems().addAll(
                menuItem("Über CADas", this::showAboutDialog, null),
                new SeparatorMenuItem(),
                menuItem("Benutzerdokumentation", documentSupport::showHelpWindow, new KeyCodeCombination(KeyCode.F1)),
                menuItem("Keymap und Mausbedienung", documentSupport::showKeymapWindow, null),
                menuItem("Drittanbieter-Lizenzen", documentSupport::showThirdPartyLicensesWindow, null)
        );

        MenuBar menuBar = new MenuBar(dateiMenu, bearbeitenMenu, ansichtMenu, werkzeugMenu, optionenMenu, berichteMenu, hilfeMenu);
        applyTooltip(menuBar, "Bietet Datei-, Bearbeitungs-, Ansichts- und Werkzeugfunktionen mit passenden Tastaturkürzeln an.");
        return menuBar;
    }

    ScrollPane buildPropertyPane() {
        selectionSummaryLabel.setWrapText(true);
        selectionSummaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        surfaceLayerTargetLabel.setWrapText(true);
        surfaceLayerTargetLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #5c5146;");
        surfaceLayerSelectionHintLabel.setWrapText(true);
        surfaceLayerSelectionHintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b6258;");
        surfaceLayerCoverageLabel.setWrapText(true);
        surfaceLayerCoverageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        cadLibrarySummaryLabel.setWrapText(true);
        cadLibrarySummaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        dwgStatusLabel.setWrapText(true);
        dwgStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        dwgBlockDetailLabel.setWrapText(true);
        dwgBlockDetailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        propertySections.getChildren().setAll(
                createPropertySection("Auswahl", selectionSummaryLabel, applySelectionPropertiesButton),
                createPropertySection(
                        "Zeichnen",
                        propertyRow("Rasterweite", gridField, gridUnit),
                        propertyRow("Länge", lengthField, lengthUnit),
                        propertyRow("Winkel", angleField),
                        propertyRow("Nordwinkel", northAngleField)
                ),
                createPropertySection(
                        "Wand",
                        propertyRow("Wandstärke", wallThicknessField, wallThicknessUnit),
                        propertyRow("Wandhöhe", wallHeightField, wallHeightUnit),
                        propertyRow("Eckhöhe", endpointHeightField, endpointHeightUnit),
                        applyEndpointHeightButton
                ),
                createPropertySection(
                        "Raum",
                        propertyRow("Name", roomNameField),
                        propertyRow("Raumhöhe", roomHeightField, roomHeightUnit),
                        propertyRow("Boden", floorThicknessField, floorThicknessUnit),
                        propertyRow("Decke", ceilingThicknessField, ceilingThicknessUnit),
                        propertyRow("Dachschrägen", roofSlopeManagementLabel)
                ),
                createPropertySection(
                        "Flächenheizung",
                        heatingSummaryLabel,
                        propertyRow("Fläche", heatingSurfacePositionSelector),
                        propertyRow("Verlegung", heatingLayoutPatternSelector),
                        propertyRow("Verlegeabstand", heatingPipeSpacingField, heatingPipeSpacingUnit),
                        propertyRow("Rohrdurchmesser", heatingPipeDiameterField, heatingPipeDiameterUnit),
                        propertyRow("Maximale Rohrlänge", heatingMaximumPipeLengthField, heatingMaximumPipeLengthUnit),
                        propertyRow("Wandabstand", heatingWallClearanceField, heatingWallClearanceUnit),
                        planHeatingButton,
                        heatingZoneList,
                        propertyRow("Name", heatingZoneNameField),
                        propertyRow("Verlegung", heatingZoneLayoutPatternSelector),
                        propertyRow("Rollen", heatingZoneFlowInvertedCheckBox),
                        propertyRow("Mittelschlange", heatingZoneSerpentineMiddleLineCheckBox),
                        propertyRow("W/m²", heatingZoneHeatOutputField),
                        propertyRow("Routing", heatingRoutingCommandArea),
                        autoRouteHeatingZoneOnResizeCheckBox,
                        applyHeatingZoneSettingsButton,
                        generateHeatingZoneRoutingButton
                ),
                createPropertySection(
                        "Tür",
                        propertyRow("Preset", doorPresetSelector),
                        propertyRow("Breite", doorWidthField, doorWidthUnit),
                        propertyRow("Höhe", doorHeightField, doorHeightUnit),
                        propertyRow("Schwelle", thresholdField, thresholdUnit)
                ),
                createPropertySection(
                        "Fenster",
                        propertyRow("Preset", windowPresetSelector),
                        propertyRow("Breite", windowWidthField, windowWidthUnit),
                        propertyRow("Höhe", windowHeightField, windowHeightUnit),
                        propertyRow("Brüstung", sillHeightField, sillHeightUnit)
                ),
                createPropertySection(
                        "Treppe",
                        propertyRow("Preset", stairPresetSelector),
                        propertyRow("Höhe", stairHeightField, stairHeightUnit),
                        propertyRow("Stufen inkl. Absätze", stairStepsField),
                        propertyRow("Absatz Anfang", stairStartLandingField, stairStartLandingUnit),
                        propertyRow("Absatz Ende", stairEndLandingField, stairEndLandingUnit),
                        propertyRow("Unterbau links", stairLeftUnderbuildField, stairLeftUnderbuildUnit),
                        propertyRow("Unterbau rechts", stairRightUnderbuildField, stairRightUnderbuildUnit),
                        propertyRow("Untersichtdicke", stairUndersideThicknessField, stairUndersideThicknessUnit)
                ),
                createPropertySection(
                        "Objekt",
                        propertyRow("Preset", roomObjectPresetSelector),
                        propertyRow("Bezeichnung", roomObjectNameField),
                        propertyRow("Breite", roomObjectWidthField, roomObjectWidthUnit),
                        propertyRow("Tiefe", roomObjectDepthField, roomObjectDepthUnit),
                        propertyRow("Höhe", roomObjectHeightField, roomObjectHeightUnit),
                        propertyRow("Wärmeleistung", roomObjectHeatOutputField),
                        propertyRow("Heizart", roomObjectHeatingTypeSelector),
                        propertyRow("Basishöhe", roomObjectBaseElevationField, roomObjectBaseElevationUnit),
                        propertyRow("Winkel", roomObjectAngleField)
                ),
                createPropertySection(
                        "Balkon/Empore",
                        propertyRow("Element", floorExtensionTypeSelector),
                        propertyRow("Lage", floorExtensionPlacementSelector),
                        propertyRow("Fußbodendicke", floorExtensionThicknessField, floorExtensionThicknessUnit)
                ),
                createPropertySection(
                        "Ebenen",
                        surfaceLayerTargetLabel,
                        surfaceLayerSelectionHintLabel,
                        propertyRow("Belagstyp", surfaceTypeSelector),
                        propertyRow("Preset", surfacePresetSelector),
                        surfaceLayerList,
                        surfaceLayerCoverageLabel,
                        new Separator(),
                        propertyRow("Name", surfaceLayerNameField, saveSurfacePresetButton),
                        propertyRow("Dicke", surfaceLayerThicknessField, surfaceLayerThicknessUnit),
                        propertyRow("Modulbreite", surfaceTileWidthField, surfaceTileWidthUnit),
                        propertyRow("Modulhöhe", surfaceTileHeightField, surfaceTileHeightUnit),
                        propertyRow("Startecke", surfaceLayoutCornerPreviousButton, surfaceLayoutCornerLabel, surfaceLayoutCornerNextButton),
                        propertyRow("Verlegerichtung", surfaceLayoutDirectionSelector),
                        propertyRow("Versatzmodus", surfaceLayoutModeSelector),
                        propertyRow("Versatz", surfaceLayoutOffsetField, surfaceLayoutOffsetUnit),
                        propertyRow("Mindestversatz", surfaceMinimumOffsetField, surfaceMinimumOffsetUnit),
                        propertyRow("Mindestrand links/rechts", surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit),
                        propertyRow("Mindestbreite Anfang/Ende", surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit),
                        propertyRow("Freirand links", surfaceFreeMarginLeftField, surfaceFreeMarginLeftUnit),
                        propertyRow("Freirand rechts", surfaceFreeMarginRightField, surfaceFreeMarginRightUnit),
                        propertyRow("Freirand oben", surfaceFreeMarginTopField, surfaceFreeMarginTopUnit),
                        propertyRow("Freirand unten", surfaceFreeMarginBottomField, surfaceFreeMarginBottomUnit),
                        propertyRow("Fugenbreite", surfaceJointWidthField, surfaceJointWidthUnit),
                        propertyRow("Schnittbeschränkung", surfaceCutRestrictionSelector),
                        propertyRow("DWG-Block", dwgBlockNameField),
                        new HBox(6.0, addSurfaceLayerButton, updateSurfaceLayerButton),
                        new HBox(6.0, removeSurfaceLayerButton, toggleSurfaceLayerVisibilityButton),
                        new HBox(6.0, moveSurfaceLayerUpButton, moveSurfaceLayerDownButton),
                        addDwgBlockPresetButton
                ),
                createPropertySection(
                        "CAD-Bibliotheken",
                        cadLibrarySummaryLabel,
                        dwgStatusLabel,
                        propertyRow("DWG-Suche", dwgBlockSearchField),
                        propertyRow("DWG-Block", dwgBlockSelector),
                        dwgPreviewCanvas,
                        dwgBlockDetailLabel,
                        propertyRow("Objektnutzung", dwgObjectFloorModeSelector),
                        new HBox(6.0, refreshDwgLibraryButton, addDwgBlockAsSurfaceButton, addDwgBlockAsObjectButton)
                )
        );
        propertySections.setPadding(new Insets(4, 0, 4, 0));
        restorePropertySectionExpansionState(currentTool());

        VBox container = new VBox(10.0, new Label("Eigenschaften"), propertySections);
        container.setPadding(new Insets(12));
        container.setStyle("-fx-background-color: rgba(255,255,255,0.62); -fx-background-radius: 16;");

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefWidth(290);
        scrollPane.setStyle("-fx-background-color: transparent;");
        applyTooltip(scrollPane, "Zeigt alle für Werkzeug oder Auswahl passenden Eigenschaften in einer permanent sichtbaren, vertikalen Liste an.");
        return scrollPane;
    }

    TitledPane createPropertySection(String title, Node... nodes) {
        VBox content = new VBox(8.0, nodes);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: rgba(242,236,226,0.92); -fx-background-radius: 0 0 12 12;");
        TitledPane pane = new TitledPane(title, content);
        pane.setAnimated(false);
        pane.setExpanded(propertySectionExpandedState(currentTool(), title));
        pane.setStyle("-fx-font-size: 13px;");
        pane.expandedProperty().addListener((ignored, oldExpanded, expanded) -> {
            if (!applyingPropertySectionExpansionState) {
                propertySectionExpandedByTool
                        .computeIfAbsent(currentTool(), ignoredTool -> new LinkedHashMap<>())
                        .put(title, expanded);
            }
        });
        applyTooltip(pane, "Klappt den Einstellungsbereich `" + title + "` ein oder aus. Der Zustand wird getrennt für jedes aktive Werkzeug gemerkt.");
        return pane;
    }

    boolean propertySectionExpandedState(DrawingTool tool, String title) {
        if (tool == null) {
            return true;
        }
        return propertySectionExpandedByTool
                .getOrDefault(tool, Map.of())
                .getOrDefault(title, true);
    }

    void storePropertySectionExpansionState(DrawingTool tool) {
        if (tool == null || propertySections.getChildren().isEmpty()) {
            return;
        }
        Map<String, Boolean> state = propertySectionExpandedByTool.computeIfAbsent(tool, ignored -> new LinkedHashMap<>());
        for (Node node : propertySections.getChildren()) {
            if (node instanceof TitledPane pane) {
                state.put(pane.getText(), pane.isExpanded());
            }
        }
    }

    void restorePropertySectionExpansionState(DrawingTool tool) {
        if (tool == null || propertySections.getChildren().isEmpty()) {
            return;
        }
        applyingPropertySectionExpansionState = true;
        try {
            for (Node node : propertySections.getChildren()) {
                if (node instanceof TitledPane pane) {
                    pane.setExpanded(propertySectionExpandedState(tool, pane.getText()));
                }
            }
        } finally {
            applyingPropertySectionExpansionState = false;
        }
    }

    VBox propertyRow(String label, Node... controls) {
        Label fieldLabel = new Label(label);
        fieldLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4d4135;");
        HBox controlRow = new HBox(6.0, controls);
        controlRow.setAlignment(Pos.CENTER_LEFT);
        return new VBox(4.0, fieldLabel, controlRow);
    }

    void configureActionButtons() {
        undoButton.setOnAction(event -> runGuardedAction("Rückgängig", this::undo));
        redoButton.setOnAction(event -> runGuardedAction("Wiederherstellen", this::redo));
        deleteSelectionButton.setOnAction(event -> runGuardedAction("Auswahl löschen", this::deleteSelection));
        clearSelectionButton.setOnAction(event -> runGuardedAction("Auswahl aufheben", this::clearSelection));
        applySelectionPropertiesButton.setOnAction(event -> runGuardedAction("Werte auf Auswahl anwenden", this::applyCurrentInputsToSelection));
        applyEndpointHeightButton.setOnAction(event -> runGuardedAction("Eckhöhe anwenden", this::applyEndpointHeightToSelection));
        addSurfaceLayerButton.setOnAction(event -> runGuardedAction("Ebene hinzufügen", this::addSurfaceLayer));
        updateSurfaceLayerButton.setOnAction(event -> runGuardedAction("Ebene aktualisieren", this::updateSurfaceLayer));
        removeSurfaceLayerButton.setOnAction(event -> runGuardedAction("Ebene entfernen", this::removeSurfaceLayer));
        toggleSurfaceLayerVisibilityButton.setOnAction(event -> runGuardedAction("Sichtbarkeit umschalten", this::toggleSurfaceLayerVisibility));
        surfaceLayoutCornerPreviousButton.setOnAction(event -> cycleSurfaceLayoutCorner(false));
        surfaceLayoutCornerNextButton.setOnAction(event -> cycleSurfaceLayoutCorner(true));
        moveSurfaceLayerUpButton.setOnAction(event -> runGuardedAction("Ebene nach oben", () -> moveSurfaceLayer(-1)));
        moveSurfaceLayerDownButton.setOnAction(event -> runGuardedAction("Ebene nach unten", () -> moveSurfaceLayer(1)));
        saveSurfacePresetButton.setOnAction(event -> runGuardedAction("Belagspreset speichern", this::saveCurrentSurfacePreset));
        addDwgBlockPresetButton.setOnAction(event -> runGuardedAction("DWG-Block hinzufügen", this::addDwgBlockPreset));
        refreshDwgLibraryButton.setOnAction(event -> runGuardedAction("DWG prüfen", this::refreshCurrentDwgLibraryAnalysis));
        addDwgBlockAsSurfaceButton.setOnAction(event -> runGuardedAction("DWG-Block als Belag", this::addSelectedDwgBlockAsSurfacePreset));
        addDwgBlockAsObjectButton.setOnAction(event -> runGuardedAction("DWG-Block als Objekt", this::addSelectedDwgBlockAsObjectPreset));
        planHeatingButton.setOnAction(event -> runGuardedAction("Heizkreise planen", this::planHydronicHeating));
        applyHeatingZoneSettingsButton.setOnAction(event -> runGuardedAction("Heizkreis übernehmen", this::applySelectedHeatingZoneSettings));
        generateHeatingZoneRoutingButton.setOnAction(event -> runGuardedAction("Heizkreis-Routing generieren", this::generateSelectedHeatingZoneRouting));
        applyHeatingRoutingCommandButton.setOnAction(event -> runGuardedAction("Heizkreis-Routing übernehmen", this::applySelectedHeatingZoneRouting));
        rebuildSelectionContextMenu();
        applyTooltip(undoButton, "Stellt den letzten fachlichen Bearbeitungsschritt des Projekts wieder her.");
        applyTooltip(redoButton, "Stellt einen zuvor rückgängig gemachten Bearbeitungsschritt erneut her.");
        applyTooltip(deleteSelectionButton, "Löscht das aktuell ausgewählte Bauteil aus der aktiven Etage.");
        applyTooltip(clearSelectionButton, "Hebt die aktuelle Auswahl auf und entfernt die Hervorhebung in 2D und 3D.");
        applyTooltip(applySelectionPropertiesButton, "Übernimmt die aktuell sichtbaren Eingabewerte auf alle passenden, ausgewählten Bauteile.");
        applyTooltip(applyEndpointHeightButton, "Übernimmt die eingetragene Höhe auf den aktuell ausgewählten Wand-Endpunkt und aktualisiert daraus die angrenzenden Räume.");
        applyTooltip(addSurfaceLayerButton, "Legt auf der aktuell ausgewählten Wand- oder Raumfläche eine neue Ebene mit den eingetragenen Maßen an.");
        applyTooltip(updateSurfaceLayerButton, "Übernimmt die aktuellen Ebenenwerte auf den in der Liste markierten Belag.");
        applyTooltip(removeSurfaceLayerButton, "Entfernt den in der Liste markierten Belag von der aktuell ausgewählten Fläche.");
        applyTooltip(toggleSurfaceLayerVisibilityButton, "Schaltet die Sichtbarkeit des markierten Belags um und passt Raumwirkung sowie 3D-Darstellung direkt an.");
        applyTooltip(surfaceLayoutCornerPreviousButton, "Schaltet die Startecke des Belags gegen den Uhrzeigersinn zur vorherigen Raumecke weiter. Die Verlegerichtung wird dabei passend auf die neue Ecke mitgeführt.");
        applyTooltip(surfaceLayoutCornerNextButton, "Schaltet die Startecke des Belags im Uhrzeigersinn zur nächsten Raumecke weiter. Die Verlegerichtung wird dabei passend auf die neue Ecke mitgeführt.");
        applyTooltip(moveSurfaceLayerUpButton, "Verschiebt den markierten Belag in der Stapelreihenfolge nach oben.");
        applyTooltip(moveSurfaceLayerDownButton, "Verschiebt den markierten Belag in der Stapelreihenfolge nach unten.");
        applyTooltip(saveSurfacePresetButton, "Speichert die aktuell eingetragenen Belagswerte als eigenes Preset unter `~/.config/CADas/Belag`, fragt vor dem Überschreiben nach und fügt das Preset der Auswahl hinzu.");
        applyTooltip(addDwgBlockPresetButton, "Registriert den manuell eingetragenen Blocknamen aus der aktuell ausgewählten DWG-Bibliothek als Belags-Preset. Wenn die DWG analysiert wurde, werden echte Blockmaße übernommen.");
        applyTooltip(refreshDwgLibraryButton, "Analysiert die aktuell geladene DWG-Bibliothek erneut über einen externen Konverter wie `dwg2dxf` oder `dwgread`.");
        applyTooltip(addDwgBlockAsSurfaceButton, "Übernimmt den ausgewählten DWG-Block mit echten Blockmaßen als Belags-Preset.");
        applyTooltip(addDwgBlockAsObjectButton, "Übernimmt den ausgewählten DWG-Block mit echtem Footprint als Objekt-Preset.");
        applyTooltip(planHeatingButton, "Die automatische Planung ganzer Räume ist vorübergehend deaktiviert. Heizkreise werden aktuell halbautomatisch als Rechtecke mit dem Werkzeug `Heizkreis` angelegt und danach direkt in der Zeichenfläche bearbeitet.");
        applyTooltip(applyHeatingZoneSettingsButton, "Übernimmt Name, Verlegung, Rollenorientierung, Mittelschlange, Heizleistung und Polygon-Eckpunkte aus der Eigenschaftenleiste auf den markierten Heizkreis.");
        applyTooltip(generateHeatingZoneRoutingButton, "Erzeugt für den markierten rechteckigen Heizkreis die gespeicherte FBH-Routing-Sprache neu. Vorhandene manuelle Korrekturen im Routing-String werden dabei ersetzt.");
        applyTooltip(applyHeatingRoutingCommandButton, "Übernimmt den Routing-Text auf den markierten Heizkreis. `=` und `R/L` steuern den Vorlauf, `-` und `r/l` den Rücklauf; `X` und `x` löschen jeweils den letzten Vorlauf- oder Rücklauf-Schritt. Bei einfacher Spiegelung werden zusätzlich `(` und `)` zu `L` und `R` sowie `8` und `9` zu `l` und `r`. Der rote Startpunkt markiert den tatsächlichen Routing-Start, nicht das Rechteck.");
        applyTooltip(cadLibrarySummaryLabel, "Listet registrierte externe CAD-Bibliotheken wie `.dwg` oder `.cadasparts` auf.");
        applyTooltip(dwgStatusLabel, "Zeigt, welcher externe DWG-Konverter gefunden wurde und ob die letzte Analyse erfolgreich war.");
        applyTooltip(dwgBlockSearchField, "Filtert die analysierten DWG-Blöcke nach Blockname, Layer oder Dateiname.");
        applyTooltip(dwgBlockSelector, "Wählt einen aus der DWG-Geometrie analysierten Block aus, der als Belag oder Objekt übernommen werden kann.");
        applyTooltip(dwgPreviewCanvas, "Zeigt eine maßstäbliche Draufsicht-Vorschau der aus dem DWG-Block abgeleiteten 2D-Grenzen.");
        applyTooltip(dwgBlockDetailLabel, "Zeigt Maße, Ursprung, Layer, Handles, Einheiten und Hinweise zum ausgewählten DWG-Block.");
        applyTooltip(dwgObjectFloorModeSelector, "Legt fest, ob das aus dem DWG-Block erzeugte Objekt auf dem Bodenbelag steht, den Bodenbelag ausschneidet oder wandmontiert ohne Bodenausschnitt geführt wird.");
    }

    void updatePropertySectionVisibility() {
        for (int index = 0; index < propertySections.getChildren().size(); index++) {
            Node node = propertySections.getChildren().get(index);
            boolean visible = switch (index) {
                case 0, 1 -> true;
                case 2 -> shouldShowSection(DrawingTool.WALL, RenderableKind.WALL);
                case 3 -> shouldShowRoomSection();
                case 4 -> selectedRoom().isPresent()
                        || currentTool() == DrawingTool.HEATING_ZONE_RECTANGLE
                        || currentTool() == DrawingTool.HEATING_MANIFOLD;
                case 5 -> shouldShowSection(DrawingTool.DOOR, RenderableKind.DOOR);
                case 6 -> shouldShowSection(DrawingTool.WINDOW, RenderableKind.WINDOW)
                        || shouldShowSection(DrawingTool.ROOF_WINDOW, RenderableKind.ROOF_WINDOW);
                case 7 -> shouldShowSection(DrawingTool.STAIR, RenderableKind.STAIR);
                case 8 -> shouldShowSection(DrawingTool.OBJECT, RenderableKind.ROOM_OBJECT);
                case 9 -> shouldShowSection(DrawingTool.FLOOR_EXTENSION, RenderableKind.FLOOR_EXTENSION);
                case 10 -> shouldShowLayerSection();
                default -> true;
            };
            node.setVisible(visible);
            node.setManaged(visible);
        }
        selectionSummaryLabel.setText(selectionSummary());
        refreshSurfaceTypeSelector();
        refreshSurfaceLayerSection();
        refreshHeatingSection();
    }

    boolean shouldShowSection(DrawingTool tool, RenderableKind... kinds) {
        if (currentTool() == tool) {
            return true;
        }
        if (selectedSelection.get() == null) {
            return false;
        }
        for (RenderableKind kind : kinds) {
            if (selectedSelection.get().kind() == kind) {
                return true;
            }
        }
        return false;
    }

    boolean shouldShowRoomSection() {
        if (currentTool() == DrawingTool.WALL) {
            return true;
        }
        if (selectedSelection.get() == null) {
            return false;
        }
        return selectedSelection.get().kind() == RenderableKind.ROOM_VOLUME
                || selectedSelection.get().kind() == RenderableKind.ROOM_FLOOR
                || selectedSelection.get().kind() == RenderableKind.ROOM_CEILING;
    }

    boolean shouldShowLayerSection() {
        if (selectedSelection.get() == null) {
            return false;
        }
        return selectedSelection.get().kind() == RenderableKind.WALL
                || selectedSelection.get().kind() == RenderableKind.ROOM_VOLUME
                || selectedSelection.get().kind() == RenderableKind.ROOM_FLOOR
                || selectedSelection.get().kind() == RenderableKind.ROOM_CEILING
                || selectedSelection.get().kind() == RenderableKind.FLOOR_EXTENSION;
    }

    String selectionSummary() {
        if (selectedEndpointGroup != null) {
            return "Ausgewählt: gemeinsame Wandecke auf Etage `" + activeLevel.get().name()
                    + "`. Die Eckhöhe wirkt auf alle verbundenen Wandenden und leitet daraus die Raumdecke neu ab.";
        }
        if (selectedSelections.isEmpty()) {
            return "Keine Auswahl. Wähle ein Bauteil im Werkzeug `Bearbeiten` aus oder nutze direkt die Werkzeuge in der Zeichenfläche.";
        }
        if (selectedSelections.size() > 1) {
            return selectedSelections.size() + " Bauteile ausgewählt. Änderungen über die sichtbaren Eigenschaften werden auf passende Auswahlen gemeinsam angewendet.";
        }
        return "Ausgewählt: " + selectionLabel(selectedSelection.get()) + " auf Etage `" + selectedSelection.get().levelName() + "`.";
    }

    String selectionLabel(SelectionKey selection) {
        return switch (selection.kind()) {
            case WALL -> "Wand";
            case ROOM_VOLUME, ROOM_FLOOR, ROOM_CEILING -> "automatisch abgeleiteter Raum";
            case DOOR -> "Tür";
            case WINDOW -> "Fenster";
            case ROOF_WINDOW -> "Dachfenster";
            case STAIR -> "Treppe";
            case ROOM_OBJECT -> "Objekt";
            case FLOOR_EXTENSION -> "Balkon/Empore";
            case FLOOR_OPENING -> "Bodenöffnung";
            case HEATING_ZONE -> "Heizkreis";
            case HEATING_MANIFOLD -> "HKV-Freifläche";
            case HEATING_EXCLUSION -> "FBH-Sperrfläche";
            default -> selection.kind().name();
        };
    }

    void updateActionButtons() {
        undoButton.setDisable(!history.canUndo());
        redoButton.setDisable(!history.canRedo());
        boolean hasSelection = !selectedSelections.isEmpty();
        boolean hasDeletableSelection = selectedSelections.stream().anyMatch(this::isDeletableSelection);
        deleteSelectionButton.setDisable(!hasDeletableSelection);
        clearSelectionButton.setDisable(!hasSelection && selectedEndpointGroup == null);
        applySelectionPropertiesButton.setDisable(!hasSelection);
        applyEndpointHeightButton.setDisable(selectedEndpointGroup == null);
        int currentIndex = availableLevels.indexOf(activeLevel.get());
        if (addLevelButton != null) {
            addLevelButton.setDisable(false);
        }
        if (renameLevelButton != null) {
            renameLevelButton.setDisable(activeLevel.get() == null);
        }
        if (moveLevelUpButton != null) {
            moveLevelUpButton.setDisable(currentIndex < 0 || currentIndex >= availableLevels.size() - 1);
        }
        if (moveLevelDownButton != null) {
            moveLevelDownButton.setDisable(currentIndex <= 0);
        }
        boolean hasSurfaceTarget = currentSurfaceSelectionContext().isPresent();
        boolean hasSurfaceSelection = selectedSurfaceLayer().isPresent();
        addSurfaceLayerButton.setDisable(!hasSurfaceTarget);
        updateSurfaceLayerButton.setDisable(!hasSurfaceTarget || !hasSurfaceSelection);
        removeSurfaceLayerButton.setDisable(!hasSurfaceTarget || !hasSurfaceSelection);
        toggleSurfaceLayerVisibilityButton.setDisable(!hasSurfaceTarget || !hasSurfaceSelection);
        moveSurfaceLayerUpButton.setDisable(!hasSurfaceTarget || !hasSurfaceSelection || surfaceLayerList.getSelectionModel().getSelectedIndex() <= 0);
        moveSurfaceLayerDownButton.setDisable(!hasSurfaceTarget || !hasSurfaceSelection || surfaceLayerList.getSelectionModel().getSelectedIndex() >= surfaceLayerList.getItems().size() - 1);
        boolean hasDwgBlock = Optional.ofNullable(dwgBlockSelector.getValue()).filter(DwgBlockDefinition::hasGeometry).isPresent();
        refreshDwgLibraryButton.setDisable(currentDwgLibraryPath().isEmpty());
        addDwgBlockAsSurfaceButton.setDisable(!hasDwgBlock);
        addDwgBlockAsObjectButton.setDisable(!hasDwgBlock);
        HydronicHeating selectedHeating = selectedHydronicHeating().orElse(null);
        int selectedZoneIndex = heatingZoneList.getSelectionModel().getSelectedIndex();
        boolean hasSelectedHeatingZone = selectedHeating != null && selectedZoneIndex >= 0;
        planHeatingButton.setDisable(true);
        applyHeatingZoneSettingsButton.setDisable(!hasSelectedHeatingZone);
        generateHeatingZoneRoutingButton.setDisable(!hasSelectedHeatingZone);
        applyHeatingRoutingCommandButton.setDisable(!hasSelectedHeatingZone);
        heatingZoneNameField.setDisable(!hasSelectedHeatingZone);
        heatingZoneLayoutPatternSelector.setDisable(!hasSelectedHeatingZone);
        heatingZoneFlowInvertedCheckBox.setDisable(!hasSelectedHeatingZone);
        heatingZoneSerpentineMiddleLineCheckBox.setDisable(!hasSelectedHeatingZone);
        heatingZoneHeatOutputField.setDisable(!hasSelectedHeatingZone);
        heatingZonePointArea.setDisable(!hasSelectedHeatingZone);
        heatingRoutingCommandArea.setDisable(!hasSelectedHeatingZone);
    }

    MenuItem menuItem(String label, Runnable action, KeyCombination accelerator) {
        MenuItem menuItem = new MenuItem(label);
        menuItem.setOnAction(event -> runGuardedAction(label, action));
        if (accelerator != null) {
            menuItem.setAccelerator(accelerator);
        }
        return menuItem;
    }

    MenuItem toolMenuItem(DrawingTool tool, KeyCode keyCode) {
        return menuItem(tool.label(), () -> toolSelector.setValue(tool), shortcutKey(keyCode));
    }

    CheckMenuItem checkMenuItem(String label, BooleanProperty property) {
        CheckMenuItem menuItem = new CheckMenuItem(label);
        menuItem.selectedProperty().bindBidirectional(property);
        return menuItem;
    }

    <T> CheckMenuItem checkMenuItem(String label, ObjectProperty<T> property, T checkedValue, T uncheckedValue) {
        CheckMenuItem menuItem = new CheckMenuItem(label);
        menuItem.setSelected(property.get() == checkedValue);
        menuItem.selectedProperty().addListener((obs, wasSelected, isSelected) ->
                property.set(Boolean.TRUE.equals(isSelected) ? checkedValue : uncheckedValue));
        property.addListener((obs, oldValue, newValue) ->
                menuItem.setSelected(newValue == checkedValue));
        return menuItem;
    }

    KeyCombination shortcutKey(KeyCode keyCode) {
        return new KeyCodeCombination(keyCode, KeyCombination.SHORTCUT_DOWN);
    }

    KeyCombination shortcutShiftKey(KeyCode keyCode) {
        return new KeyCodeCombination(keyCode, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    }

    void runGuardedAction(String actionLabel, Runnable action) {
        try {
            action.run();
        } catch (Exception exception) {
            showActionException(actionLabel, exception);
        }
    }

    void showActionException(String actionLabel, Throwable throwable) {
        String title = "Aktion fehlgeschlagen";
        String header = "Die Aktion `" + actionLabel + "` konnte nicht abgeschlossen werden.";
        String content = UiErrorDialogs.userMessage(throwable);
        draftLabel.setText(header + " " + content);
        showErrorDialog(title, header, content, throwable);
    }

    void showOperationException(String title, Throwable throwable) {
        String content = UiErrorDialogs.userMessage(throwable);
        draftLabel.setText(title + ": " + content);
        showErrorDialog(title, title, content, throwable);
    }

    void showHeatingCircuitRoutingWindow() {
        new HeatingCircuitRoutingWindow().show(currentWindow());
    }

    void showErrorDialog(String title, String header, String content, Throwable throwable) {
        lastErrorDialog = UiErrorDialogs.fromThrowable(title, header, content, throwable);
        UiErrorDialogs.show(lastErrorDialog, currentWindow(), interactiveDialogsEnabled);
    }

    public void handleUnhandledException(Throwable throwable) {
        showErrorDialog(
                "Unerwarteter Fehler",
                "CADas hat einen unerwarteten Fehler erkannt.",
                UiErrorDialogs.userMessage(throwable),
                throwable
        );
        draftLabel.setText("Unerwarteter Fehler: " + UiErrorDialogs.userMessage(throwable));
    }

    HBox labelledNode(String label, Node node) {
        HBox box = new HBox(6.0, new Label(label), node);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    void initializeUnitSelectors() {
        initializeUnitSelector(gridField, gridUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(lengthField, lengthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(wallThicknessField, wallThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(wallHeightField, wallHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(endpointHeightField, endpointHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(roomHeightField, roomHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(floorThicknessField, floorThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(ceilingThicknessField, ceilingThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(kneeWallHeightField, kneeWallHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(doorWidthField, doorWidthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(doorHeightField, doorHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(thresholdField, thresholdUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(windowWidthField, windowWidthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(windowHeightField, windowHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(sillHeightField, sillHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairHeightField, stairHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairStartLandingField, stairStartLandingUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairEndLandingField, stairEndLandingUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairLeftUnderbuildField, stairLeftUnderbuildUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairRightUnderbuildField, stairRightUnderbuildUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(stairUndersideThicknessField, stairUndersideThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(roomObjectWidthField, roomObjectWidthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(roomObjectDepthField, roomObjectDepthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(roomObjectHeightField, roomObjectHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(roomObjectBaseElevationField, roomObjectBaseElevationUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(floorExtensionThicknessField, floorExtensionThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingPipeSpacingField, heatingPipeSpacingUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingPipeDiameterField, heatingPipeDiameterUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingMaximumPipeLengthField, heatingMaximumPipeLengthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingWallClearanceField, heatingWallClearanceUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingSupplyXField, heatingSupplyXUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingSupplyYField, heatingSupplyYUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingReturnXField, heatingReturnXUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(heatingReturnYField, heatingReturnYUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceLayerThicknessField, surfaceLayerThicknessUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceTileWidthField, surfaceTileWidthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceTileHeightField, surfaceTileHeightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceLayoutOffsetField, surfaceLayoutOffsetUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceMinimumOffsetField, surfaceMinimumOffsetUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceMinimumEdgeWidthField, surfaceMinimumEdgeWidthUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceMinimumStartEndMarginField, surfaceMinimumStartEndMarginUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceFreeMarginLeftField, surfaceFreeMarginLeftUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceFreeMarginRightField, surfaceFreeMarginRightUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceFreeMarginTopField, surfaceFreeMarginTopUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceFreeMarginBottomField, surfaceFreeMarginBottomUnit, LengthUnit.CENTIMETER);
        initializeUnitSelector(surfaceJointWidthField, surfaceJointWidthUnit, LengthUnit.CENTIMETER);
    }

    void initializeUnitSelector(TextField field, ComboBox<LengthUnit> selector, LengthUnit defaultUnit) {
        selector.getItems().addAll(LengthUnit.values());
        selector.setValue(defaultUnit);
        selector.valueProperty().addListener((ignored, oldUnit, newUnit) -> convertLengthInputOnUnitChange(field, oldUnit, newUnit));
    }

    void convertLengthInputOnUnitChange(TextField field, LengthUnit oldUnit, LengthUnit newUnit) {
        if (updatingLengthInput || oldUnit == null || newUnit == null || oldUnit == newUnit) {
            return;
        }
        parseLength(field, oldUnit)
                .ifPresent(length -> field.setText(formatValue(length, newUnit, LENGTH_INPUT_DECIMALS)));
    }

    void initializePresetSelectors() {
        availableDoorPresets.setAll(partLibrary.doorPresets());
        availableWindowPresets.setAll(partLibrary.windowPresets());
        availableStairPresets.setAll(partLibrary.stairPresets());
        availableRoomObjectPresets.setAll(roomObjectPresetService.presets());
        roomObjectHeatingTypeSelector.getItems().setAll(RoomObjectHeatingType.values());
        roomObjectHeatingTypeSelector.setValue(RoomObjectHeatingType.NONE);
        doorPresetSelector.setItems(availableDoorPresets);
        windowPresetSelector.setItems(availableWindowPresets);
        stairPresetSelector.setItems(availableStairPresets);
        roomObjectPresetSelector.setItems(availableRoomObjectPresets);
        selectFirstIfAvailable(doorPresetSelector, availableDoorPresets);
        selectFirstIfAvailable(windowPresetSelector, availableWindowPresets);
        selectFirstIfAvailable(stairPresetSelector, availableStairPresets);
        selectFirstIfAvailable(roomObjectPresetSelector, availableRoomObjectPresets);
        applyDoorPreset(doorPresetSelector.getValue());
        applyWindowPreset(windowPresetSelector.getValue());
        applyStairPreset(stairPresetSelector.getValue());
        applyRoomObjectPreset(roomObjectPresetSelector.getValue());
        doorPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyDoorPreset(newValue));
        windowPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyWindowPreset(newValue));
        stairPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyStairPreset(newValue));
        roomObjectPresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applyRoomObjectPreset(newValue));
    }

    void initializeSurfaceLayerControls() {
        availableSurfacePresets.setAll(surfaceCoveringPresetService.defaults());
        loadUserSurfacePresets();
        surfacePresetSelector.setItems(availableSurfacePresets);
        if (!availableSurfacePresets.isEmpty()) {
            surfacePresetSelector.setValue(availableSurfacePresets.getFirst());
        }
        surfaceLayoutCornerPreviousButton.setFocusTraversable(false);
        surfaceLayoutCornerPreviousButton.setMinWidth(34.0);
        surfaceLayoutCornerNextButton.setFocusTraversable(false);
        surfaceLayoutCornerNextButton.setMinWidth(34.0);
        surfaceLayoutCornerLabel.setMinWidth(110.0);
        surfaceLayoutCornerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4d4135;");
        surfaceLayoutDirectionSelector.getItems().setAll(SurfaceLayoutDirection.values());
        surfaceLayoutDirectionSelector.setValue(SurfaceLayoutDirection.LEFT_TO_RIGHT);
        surfaceLayoutDirectionSelector.valueProperty().addListener((ignored, oldValue, newValue) -> syncSurfaceLayoutAnchorForDirection(newValue));
        surfaceLayoutModeSelector.getItems().setAll(SurfaceLayoutMode.values());
        surfaceLayoutModeSelector.setValue(SurfaceLayoutMode.AUTOMATIC);
        surfaceCutRestrictionSelector.getItems().setAll(SurfaceCutRestriction.values());
        surfaceCutRestrictionSelector.setValue(SurfaceCutRestriction.fallback());
        updateSurfaceLayoutCornerLabel();
        surfaceLayerList.setPrefHeight(120);
        surfaceLayerList.getSelectionModel().selectedIndexProperty().addListener((ignored, oldValue, newValue) -> {
            syncInputsFromSelectedSurfaceLayer();
            render();
        });
        surfaceTypeSelector.valueProperty().addListener((ignored, oldValue, newValue) -> {
            if (newValue == SurfaceType.FLOOR || newValue == SurfaceType.CEILING) {
                preferredRoomSurfaceType = newValue;
            }
            refreshSurfaceLayerSection();
        });
        surfacePresetSelector.valueProperty().addListener((ignored, oldValue, newValue) -> applySurfacePreset(newValue));
        refreshSurfaceTypeSelector();
        applySurfacePreset(surfacePresetSelector.getValue());
    }

    void initializeHeatingControls() {
        heatingSurfacePositionSelector.getItems().setAll(HeatingSurfacePosition.values());
        heatingSurfacePositionSelector.setValue(HeatingSurfacePosition.FLOOR);
        heatingLayoutPatternSelector.getItems().setAll(HeatingLayoutPattern.MEANDER, HeatingLayoutPattern.VARIO);
        heatingLayoutPatternSelector.setValue(HeatingLayoutPattern.VARIO);
        heatingZoneLayoutPatternSelector.getItems().setAll(HeatingLayoutPattern.MEANDER, HeatingLayoutPattern.VARIO);
        heatingZoneLayoutPatternSelector.setValue(HeatingLayoutPattern.VARIO);
        heatingZoneList.setPrefHeight(110.0);
        heatingZonePointArea.setPrefRowCount(6);
        heatingZonePointArea.setWrapText(true);
        heatingRoutingCommandArea.setPrefRowCount(4);
        heatingRoutingCommandArea.setFont(ROUTING_COMMAND_FONT);
        heatingRoutingCommandArea.setWrapText(true);
        autoRouteHeatingZoneOnResizeCheckBox.setSelected(autoRouteHeatingZoneOnResize.get());
        autoRouteHeatingZoneOnResizeCheckBox.selectedProperty().bindBidirectional(autoRouteHeatingZoneOnResize);
        heatingSummaryLabel.setWrapText(true);
        heatingSummaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5c5146;");
        heatingSurfacePositionSelector.valueProperty().addListener((ignored, oldValue, newValue) -> {
            refreshHeatingSection();
            render();
        });
        heatingRoutingCommandArea.textProperty().addListener((ignored, oldValue, newValue) -> {
            if (updatingHeatingRoutingInput) {
                return;
            }
            String normalizedDisplayText = normalizeRoutingEditorDisplayText(newValue);
            if (!Objects.equals(Optional.ofNullable(newValue).orElse(""), normalizedDisplayText)) {
                replaceTextPreservingCaretAndScroll(heatingRoutingCommandArea, normalizedDisplayText);
            }
            if (normalizedDisplayText.trim().isBlank()) {
                return;
            }
            runGuardedAction("Heizkreis-Routing übernehmen", this::applySelectedHeatingZoneRouting);
        });
        heatingZoneList.getSelectionModel().selectedIndexProperty().addListener((ignored, oldValue, newValue) -> {
            if (updatingHeatingZoneSelection) {
                return;
            }
            syncHeatingZoneSettingsInputs();
            syncHeatingRoutingCommandArea();
            updateActionButtons();
        });
    }

    void initializeDwgLibraryControls() {
        dwgBlockSelector.setItems(availableDwgBlocks);
        dwgBlockSelector.setPrefWidth(220);
        dwgBlockSearchField.setPromptText("Block, Layer oder Datei");
        dwgObjectFloorModeSelector.getItems().setAll(RoomObjectMountingMode.values());
        dwgObjectFloorModeSelector.setValue(RoomObjectMountingMode.STANDS_ON_COVERING);
        dwgBlockSearchField.textProperty().addListener((ignored, oldValue, newValue) -> applyDwgBlockFilter());
        dwgBlockSelector.valueProperty().addListener((ignored, oldValue, newValue) -> refreshDwgBlockPreviewAndDetails());
        DwgConversionAvailability availability = dwgLibraryAnalyzer.availability();
        dwgStatusLabel.setText(availability.message());
        drawEmptyDwgPreview("Keine DWG");
    }

    void loadUserSurfacePresets() {
        try {
            userSurfacePresetLibrary.loadPresets().forEach(this::registerSurfacePreset);
        } catch (RuntimeException | IOException exception) {
            showOperationException("Eigene Belagspresets konnten nicht geladen werden", exception);
        }
    }

    void registerConfiguredDwgLibraries() {
        try {
            userSurfacePresetLibrary.loadCadLibraries().forEach(this::registerConfiguredDwgLibraryReference);
        } catch (IOException exception) {
            showOperationException("Gespeicherte DWG-Bibliotheken konnten nicht geladen werden", exception);
        }
    }

    void registerConfiguredDwgLibraryReference(Path sourceFile) {
        Path normalizedSource = sourceFile.toAbsolutePath().normalize();
        if (!cadLibraryReferences.contains(normalizedSource)) {
            cadLibraryReferences.add(normalizedSource);
        }
        registerSurfacePreset(surfaceCoveringPresetService.fromDwg(normalizedSource));
        dwgBlockCatalogService.loadCatalog(normalizedSource)
                .forEach(blockName -> registerSurfacePreset(surfaceCoveringPresetService.fromDwgBlock(normalizedSource, blockName)));
        updateCadLibrarySummary();
    }

    <T> void selectFirstIfAvailable(ComboBox<T> selector, ObservableList<T> values) {
        if (!values.isEmpty()) {
            selector.setValue(values.getFirst());
        }
    }

    void applyFormTooltips() {
        applyTooltip(toolSelector, "Wählt das aktuelle Zeichenwerkzeug aus. Räume werden aus geschlossenen Wandzügen automatisch abgeleitet, im Werkzeug `Bearbeiten` ausgewählt und beim Zeichnen von Wänden über die sichtbaren Standardwerte links mitgesteuert.");
        applyTooltip(gridField, "Legt die Rasterweite für die Zeichenfläche fest. Werte werden mit der gewählten Einheit interpretiert.");
        applyTooltip(gridUnit, "Bestimmt die Einheit für die Rasterweite, damit Eingaben in Millimeter, Zentimeter oder Meter erfolgen können.");
        applyTooltip(lengthField, "Optionaler Längenwert für die gerade gezeichnete Wand. Wenn ein Wert eingetragen ist, wird die Wand auf diese Länge gesetzt.");
        applyTooltip(lengthUnit, "Bestimmt die Einheit für die manuelle Längeneingabe während des Zeichnens.");
        applyTooltip(angleField, "Optionaler Winkel in Grad für die aktuelle Wand. Ohne Eingabe bleibt der orthogonale 90°-Modus aktiv.");
        applyTooltip(northAngleField, "Definiert die Nordausrichtung des Gebäudes in Grad. Die Kompassanzeige richtet sich danach aus.");
        applyTooltip(wallThicknessField, "Definiert die Wandstärke für neu gezeichnete Wände.");
        applyTooltip(wallThicknessUnit, "Bestimmt die Einheit für die Wandstärke.");
        applyTooltip(wallHeightField, "Legt die Raum- beziehungsweise Wandhöhe für neu gezeichnete Wände fest.");
        applyTooltip(wallHeightUnit, "Bestimmt die Einheit für die Wandhöhe.");
        applyTooltip(endpointHeightField, "Legt die Höhe für den aktuell ausgewählten gemeinsamen Wand-Endpunkt fest. Daraus wird bei geschlossenen Wandzügen eine schräge Decke des betroffenen Raums abgeleitet.");
        applyTooltip(endpointHeightUnit, "Bestimmt die Einheit für die Endpunkthöhe einer ausgewählten Wandecke.");
        applyTooltip(roomNameField, "Legt den Namen für automatisch erkannte Räume oder für die aktuell ausgewählte Raumauswahl fest.");
        applyTooltip(roomHeightField, "Legt die lichte Raumhöhe für automatisch erkannte Räume oder die aktuell ausgewählte Raumauswahl fest.");
        applyTooltip(roomHeightUnit, "Bestimmt die Einheit für die Raumhöhe.");
        applyTooltip(floorThicknessField, "Legt die Boden- oder Fußbodenstärke für automatisch erkannte Räume oder die aktuell ausgewählte Raumauswahl fest.");
        applyTooltip(floorThicknessUnit, "Bestimmt die Einheit für die Bodenstärke.");
        applyTooltip(ceilingThicknessField, "Legt die Deckenstärke für automatisch erkannte Räume oder die aktuell ausgewählte Raumauswahl fest.");
        applyTooltip(ceilingThicknessUnit, "Bestimmt die Einheit für die Deckenstärke.");
        applyTooltip(kneeWallHeightField, "Legt die Sockel- beziehungsweise Kniestockhöhe der Dachschräge an der niedrigen Raumkante fest.");
        applyTooltip(kneeWallHeightUnit, "Bestimmt die Einheit für die Sockelhöhe der Dachschräge.");
        applyTooltip(roofSlopeManagementLabel, "Dachschrägen werden über das Kontextmenü ihrer niedrigen Wand erzeugt oder ersetzt. Jede Raumseite kann eine eigene Dachschräge besitzen.");
        applyTooltip(doorWidthField, "Legt die Breite der nächsten Tür fest.");
        applyTooltip(doorWidthUnit, "Bestimmt die Einheit für die Türbreite.");
        applyTooltip(doorHeightField, "Legt die Höhe der nächsten Tür fest.");
        applyTooltip(doorHeightUnit, "Bestimmt die Einheit für die Türhöhe.");
        applyTooltip(thresholdField, "Legt den Höhenversatz der Türschwelle für die nächste Tür fest.");
        applyTooltip(thresholdUnit, "Bestimmt die Einheit für die Türschwellenhöhe.");
        applyTooltip(doorPresetSelector, "Wählt eine Standardtür aus der internen Teilebibliothek und übernimmt deren Maße.");
        applyTooltip(windowWidthField, "Legt die Breite des nächsten Fensters fest.");
        applyTooltip(windowWidthUnit, "Bestimmt die Einheit für die Fensterbreite.");
        applyTooltip(windowHeightField, "Legt die Höhe des nächsten Fensters fest.");
        applyTooltip(windowHeightUnit, "Bestimmt die Einheit für die Fensterhöhe.");
        applyTooltip(sillHeightField, "Legt die Brüstungshöhe des nächsten Fensters fest.");
        applyTooltip(sillHeightUnit, "Bestimmt die Einheit für die Brüstungshöhe.");
        applyTooltip(windowPresetSelector, "Wählt ein Standardfenster aus der internen Teilebibliothek und übernimmt dessen Maße.");
        applyTooltip(stairPresetSelector, "Wählt eine Standardtreppe aus der internen Teilebibliothek und übernimmt Typ, Höhe und Stufenanzahl.");
        applyTooltip(stairHeightField, "Legt die Gesamthöhe der nächsten Treppe fest.");
        applyTooltip(stairHeightUnit, "Bestimmt die Einheit für die Treppenhöhe.");
        applyTooltip(stairStepsField, "Legt die gesamte Stufenanzahl fest. Konfigurierte Anfangs- und Endabsätze zählen jeweils als eine Stufe mit.");
        applyTooltip(stairStartLandingField, "Legt die Tiefe des ebenen Absatzes am Anfang der Treppe in Laufrichtung fest. Null deaktiviert den Absatz.");
        applyTooltip(stairStartLandingUnit, "Bestimmt die Einheit für die Tiefe des Anfangsabsatzes.");
        applyTooltip(stairEndLandingField, "Legt die Tiefe des ebenen Absatzes am Ende der Treppe in Laufrichtung fest. Null deaktiviert den Absatz.");
        applyTooltip(stairEndLandingUnit, "Bestimmt die Einheit für die Tiefe des Endabsatzes.");
        applyTooltip(stairLeftUnderbuildField, "Legt die Wandstärke des optionalen linken Treppenunterbaus fest. Null entfernt diese Unterbauwand.");
        applyTooltip(stairLeftUnderbuildUnit, "Bestimmt die Einheit für die Wandstärke des linken Treppenunterbaus.");
        applyTooltip(stairRightUnderbuildField, "Legt die Wandstärke des optionalen rechten Treppenunterbaus fest. Null entfernt diese Unterbauwand.");
        applyTooltip(stairRightUnderbuildUnit, "Bestimmt die Einheit für die Wandstärke des rechten Treppenunterbaus.");
        applyTooltip(stairUndersideThicknessField, "Legt die senkrechte Dicke der planen schrägen Untersicht unterhalb der Stufen fest. Null deaktiviert die Untersichtplatte.");
        applyTooltip(stairUndersideThicknessUnit, "Bestimmt die Einheit für die Dicke der planen schrägen Treppenuntersicht.");
        applyTooltip(floorExtensionTypeSelector, "Wählt, ob die rechteckige Erweiterung als Balkon oder Empore modelliert wird.");
        applyTooltip(floorExtensionPlacementSelector, "Kennzeichnet die Erweiterung als innen oder außen an die aktive Etage angehängt.");
        applyTooltip(floorExtensionThicknessField, "Legt die Dicke der tragenden rechteckigen Fußbodenplatte des Balkons oder der Empore fest.");
        applyTooltip(floorExtensionThicknessUnit, "Bestimmt die Einheit für die Fußbodendicke des Balkons oder der Empore.");
        applyTooltip(heatingSurfacePositionSelector, "Wählt unabhängig voneinander die Fußboden- oder Deckenheizung des markierten Raums. Für beide Flächen stehen dieselben Planungs- und Bearbeitungsfunktionen bereit.");
        applyTooltip(heatingLayoutPatternSelector, "Wählt die Start-Verlegeart für neu angelegte manuelle Heizkreise. Vario und Meander erzeugen gespeicherte FBH-Routing-Kommandos; alte Schneckenverlegung bleibt nur für Bestandsdateien erhalten.");
        applyTooltip(heatingPipeSpacingField, "Legt den Achsabstand benachbarter Rohrläufe fest. Der Kurvenradius wird automatisch als halber Verlegeabstand angesetzt.");
        applyTooltip(heatingPipeSpacingUnit, "Bestimmt die Einheit für den Verlegeabstand der Heizungsrohre.");
        applyTooltip(heatingPipeDiameterField, "Legt den Außendurchmesser des Heizungsrohrs fest. Er muss kleiner als der Verlegeabstand sein.");
        applyTooltip(heatingPipeDiameterUnit, "Bestimmt die Einheit für den Rohrdurchmesser.");
        applyTooltip(heatingMaximumPipeLengthField, "Begrenzt die gesamte Rohrlänge je Heizkreis einschließlich der Verbindung zum Vor- und Rücklauf. Größere Räume werden automatisch in mehrere Bereiche geteilt.");
        applyTooltip(heatingMaximumPipeLengthUnit, "Bestimmt die Einheit für die maximal zulässige Rohrlänge je Heizkreis.");
        applyTooltip(heatingWallClearanceField, "Legt den Mindestabstand der Rohrmitte von der Raumwand fest.");
        applyTooltip(heatingWallClearanceUnit, "Bestimmt die Einheit für den Mindestabstand zur Wand.");
        applyTooltip(heatingSupplyXField, "Legt die X-Koordinate des Vorlaufanschlusses am Verteiler im Koordinatensystem der Etage fest.");
        applyTooltip(heatingSupplyXUnit, "Bestimmt die Einheit für die X-Koordinate des Vorlaufanschlusses.");
        applyTooltip(heatingSupplyYField, "Legt die Y-Koordinate des Vorlaufanschlusses am Verteiler im Koordinatensystem der Etage fest.");
        applyTooltip(heatingSupplyYUnit, "Bestimmt die Einheit für die Y-Koordinate des Vorlaufanschlusses.");
        applyTooltip(heatingReturnXField, "Legt die X-Koordinate des Rücklaufanschlusses am Verteiler im Koordinatensystem der Etage fest.");
        applyTooltip(heatingReturnXUnit, "Bestimmt die Einheit für die X-Koordinate des Rücklaufanschlusses.");
        applyTooltip(heatingReturnYField, "Legt die Y-Koordinate des Rücklaufanschlusses am Verteiler im Koordinatensystem der Etage fest.");
        applyTooltip(heatingReturnYUnit, "Bestimmt die Einheit für die Y-Koordinate des Rücklaufanschlusses.");
        applyTooltip(heatingZoneList, "Listet die getrennten Heizkreise der gewählten Boden- oder Deckenfläche mit Routingart, HKL, Heizfläche und berechneter Heizleistung auf. Die darunterliegenden Eingaben beziehen sich immer auf den markierten Heizkreis.");
        applyTooltip(heatingZoneNameField, "Legt die sichtbare Bezeichnung des markierten Heizkreises fest.");
        applyTooltip(heatingZoneLayoutPatternSelector, "Legt fest, ob der markierte Heizkreis als Vario-Doppelspirale oder Meander neu generiert wird. Die Auswahl wirkt auf `Routing generieren` und auf das Übernehmen des Heizkreises.");
        applyTooltip(heatingZoneFlowInvertedCheckBox, "Tauscht beim markierten Heizkreis Vorlauf und Rücklauf, ohne die rote Startmarke vom tatsächlichen Routing-Beginn wegzubewegen.");
        applyTooltip(heatingZoneSerpentineMiddleLineCheckBox, "Aktiviert bei passenden rechteckigen Heizkreisen eine schlangenförmige Mittellinie für Vario- oder Meander-Routing.");
        applyTooltip(heatingZoneHeatOutputField, "Speichert die angenommene Heizleistung des markierten Heizkreises in Watt pro Quadratmeter für Übersicht, PDF und Materialliste.");
        applyTooltip(heatingZonePointArea, "Erfasst pro Zeile einen Polygon-Eckpunkt des markierten Heizkreises als `X; Y` in Zentimetern. Das Rechteck dient nur als Größenrahmen; nach dem Routing richtet CADas es an der äußeren Rohrkante aus.");
        applyTooltip(heatingRoutingCommandArea, "Zeigt und bearbeitet die Routing-Sprache des markierten Heizkreises. `=` und `-` verlängern Vorlauf und Rücklauf um eine Rasterlinie, `R/r` und `L/l` setzen Viertelkreise, `X/x` löschen den letzten Schritt. Bei einfacher Spiegelung werden zusätzlich `(` und `)` zu `L` und `R` sowie `8` und `9` zu `l` und `r`. Der rote Startpunkt bleibt an der tatsächlichen Startkante des Heizrohrs; das Rechteck ist nur der Größenrahmen.");
        applyTooltip(autoRouteHeatingZoneOnResizeCheckBox, "Legt fest, ob ein Heizkreis nach dem Ziehen seines Rechtecks automatisch neu geroutet wird. Ausgeschaltet bleiben die vorhandenen Routing-Befehle erhalten.");
        applyTooltip(heatingSummaryLabel, "Zeigt Fläche, Verlegeart, Anzahl der Heizkreise, gesamte HKL und die aufsummierte Heizleistung der gewählten Flächenheizung.");
        applyTooltip(roomObjectPresetSelector, "Wählt ein Objekt zum Platzieren aus und übernimmt dessen Standardmaße. DWG-Dateien unter `~/.config/CADas/Objekte` erscheinen hier zusätzlich als Objekt-Presets.");
        applyTooltip(roomObjectNameField, "Legt die sichtbare Bezeichnung eines neuen oder ausgewählten Objekts fest. Die Bezeichnung wird in der 2D-Draufsicht direkt am Objekt angezeigt.");
        applyTooltip(roomObjectWidthField, "Legt die Breite eines neuen oder ausgewählten Objekts fest.");
        applyTooltip(roomObjectWidthUnit, "Bestimmt die Einheit für die Objektbreite.");
        applyTooltip(roomObjectDepthField, "Legt die Tiefe eines neuen oder ausgewählten Objekts fest.");
        applyTooltip(roomObjectDepthUnit, "Bestimmt die Einheit für die Objekttiefe.");
        applyTooltip(roomObjectHeightField, "Legt die Höhe eines neuen oder ausgewählten Objekts fest.");
        applyTooltip(roomObjectHeightUnit, "Bestimmt die Einheit für die Objekthöhe.");
        applyTooltip(roomObjectHeatOutputField, "Legt die Wärmeleistung eines neuen oder ausgewählten Objekts in Watt fest. Der Wert wird je nach Heizart den Summen für FBH, DH, sonstige Flächenheizung oder Heizelemente zugeordnet.");
        applyTooltip(roomObjectHeatingTypeSelector, "Legt fest, ob die Wärmeleistung dieses Objekts als keine Heizung, Heizelement, FBH, DH oder sonstige Flächenheizung geführt wird. Diese Zuordnung steuert Raumwärmesummen, Materialliste und PDF.");
        applyTooltip(roomObjectBaseElevationField, "Legt die vertikale Lage der Objektbasis relativ zum Boden der aktiven Etage fest. Positive Werte heben das Objekt an, negative Werte versenken es.");
        applyTooltip(roomObjectBaseElevationUnit, "Bestimmt die Einheit für die positive oder negative Basishöhe des Objekts.");
        applyTooltip(roomObjectAngleField, "Legt den frei einstellbaren Drehwinkel eines neuen oder ausgewählten Objekts in Grad fest.");
        applyTooltip(surfaceTypeSelector, "Zeigt nur die Belagstypen an, die zur aktuellen Auswahl passen. Raum allein erlaubt Boden oder Decke, Raum plus Wand erlaubt Innenwand, Wand allein erlaubt Innenwand oder Außenwand; Innenwand ist dabei vorausgewählt.");
        applyTooltip(surfacePresetSelector, "Wählt einen Beispielbelag oder eine DWG-Referenz aus und übernimmt deren Standardwerte in die Ebenenfelder.");
        applyTooltip(surfaceLayerList, "Zeigt die Ebenen der aktuell ausgewählten Fläche in ihrer Stapelreihenfolge an.");
        applyTooltip(surfaceLayerNameField, "Legt den Namen der Ebene fest, etwa Fliese, Rigips, Dämmplatte oder eine DWG-Referenz.");
        applyTooltip(surfaceLayerThicknessField, "Legt die Dicke der Ebene fest. Innenwand- und Deckenbeläge wirken direkt auf Raumgeometrie und Volumen.");
        applyTooltip(surfaceLayerThicknessUnit, "Bestimmt die Einheit für die Dicke des ausgewählten Belags.");
        applyTooltip(surfaceTileWidthField, "Legt die Breite einer Fliese oder Platte für die Belegungsbasis fest.");
        applyTooltip(surfaceTileWidthUnit, "Bestimmt die Einheit für die Breite der Fliese oder Platte.");
        applyTooltip(surfaceTileHeightField, "Legt die Höhe beziehungsweise Länge einer Fliese oder Platte für die Belegungsbasis fest.");
        applyTooltip(surfaceTileHeightUnit, "Bestimmt die Einheit für die Höhe oder Länge des Belags.");
        applyTooltip(surfaceLayoutCornerLabel, "Zeigt die aktuell gewählte Startecke des Belags an. Von dieser Raumecke aus beginnt die erste Reihe.");
        applyTooltip(surfaceLayoutDirectionSelector, "Legt fest, auf welcher Raumseite die erste Reihe startet. Beim Umschalten bleibt die obere oder untere Startecke erhalten und wechselt nur auf die passende linke oder rechte Seite.");
        applyTooltip(surfaceLayoutModeSelector, "Bestimmt, ob ohne Versatz, mit automatischem Versatz oder mit festem Reihenversatz belegt wird.");
        applyTooltip(surfaceLayoutOffsetField, "Legt bei festem Versatz den Reihenversatz entlang der langen Modulkante fest. Der Wert wird von Reihe zu Reihe fortgeschrieben.");
        applyTooltip(surfaceLayoutOffsetUnit, "Bestimmt die Einheit für den festen Reihenversatz.");
        applyTooltip(surfaceMinimumOffsetField, "Legt den kleinsten zulässigen automatischen Versatz zwischen zwei Reihen fest.");
        applyTooltip(surfaceMinimumOffsetUnit, "Bestimmt die Einheit für den Mindestversatz.");
        applyTooltip(surfaceMinimumEdgeWidthField, "Legt die kleinste zulässige Restbreite links und rechts innerhalb einer Reihe fest.");
        applyTooltip(surfaceMinimumEdgeWidthUnit, "Bestimmt die Einheit für die seitliche Mindestbreite an den Rändern.");
        applyTooltip(surfaceMinimumStartEndMarginField, "Legt die kleinste zulässige Breite der Anfangs- und Endreihe in Verlegerichtung fest. Wenn die Endreihe zu schmal würde, wird die Anfangsreihe entsprechend beschnitten, bleibt aber direkt an der Wand.");
        applyTooltip(surfaceMinimumStartEndMarginUnit, "Bestimmt die Einheit für die Mindestbreite der Anfangs- und Endreihe.");
        applyTooltip(surfaceFreeMarginLeftField, "Lässt an der linken Außenkante dauerhaft einen freien Rand ohne zugeschnittene Streifen frei. Die Verlegung beginnt erst hinter diesem Rand.");
        applyTooltip(surfaceFreeMarginLeftUnit, "Bestimmt die Einheit für den freien linken Rand.");
        applyTooltip(surfaceFreeMarginRightField, "Lässt an der rechten Außenkante dauerhaft einen freien Rand ohne zugeschnittene Streifen frei. Die Verlegung endet vor diesem Rand.");
        applyTooltip(surfaceFreeMarginRightUnit, "Bestimmt die Einheit für den freien rechten Rand.");
        applyTooltip(surfaceFreeMarginTopField, "Lässt an der oberen Außenkante dauerhaft einen freien Rand ohne zugeschnittene Streifen frei. Die Verlegung endet vor diesem Rand.");
        applyTooltip(surfaceFreeMarginTopUnit, "Bestimmt die Einheit für den freien oberen Rand.");
        applyTooltip(surfaceFreeMarginBottomField, "Lässt an der unteren Außenkante dauerhaft einen freien Rand ohne zugeschnittene Streifen frei. Die Verlegung beginnt erst oberhalb dieses Randes.");
        applyTooltip(surfaceFreeMarginBottomUnit, "Bestimmt die Einheit für den freien unteren Rand.");
        applyTooltip(surfaceJointWidthField, "Legt die Breite der Fugen zwischen den Fliesen oder Platten fest.");
        applyTooltip(surfaceJointWidthUnit, "Bestimmt die Einheit für die Fugenbreite.");
        applyTooltip(surfaceCutRestrictionSelector, "Legt fest, ob Zuschnitte beliebig frei verwendet werden dürfen, ob Schnittkanten nur an Außenkanten liegen dürfen oder ob zusätzlich die Verlegerichtung ohne Drehung eingehalten werden muss.");
        applyTooltip(dwgBlockNameField, "Erfasst einen konkreten Blocknamen aus einer geladenen DWG-Bibliothek, damit daraus ein auswählbares Oberflächen-Preset wird.");
        applyTooltip(surfaceLayerTargetLabel, "Zeigt, auf welcher Wand- oder Raumfläche die aktuellen Ebenen bearbeitet werden.");
        applyTooltip(surfaceLayerSelectionHintLabel, "Erklärt, welche Kombination aus Raum- und Wandauswahl für den aktuell sichtbaren Belagstyp erforderlich ist.");
        applyTooltip(surfaceLayerCoverageLabel, "Zeigt eine Kurzbewertung der aktuellen Platten- oder Fliesenbelegung der markierten Ebene.");
        applyTooltip(levelSelector, "Wechselt zwischen den vorhandenen Etagen des aktuellen Projekts. Jede Etage besitzt ihren eigenen Wandbestand.");
    }

    void registerRenderListener(BooleanProperty property) {
        property.addListener((ignored, oldValue, newValue) -> render());
    }

    <T> void registerRenderListener(ObjectProperty<T> property) {
        property.addListener((ignored, oldValue, newValue) -> render());
    }

    Button createActionButton(String label, String style, Runnable action, String tooltipText) {
        Button button = new Button(label);
        button.setOnAction(event -> runGuardedAction(label, action));
        if (style != null) {
            button.setStyle(style);
        }
        applyTooltip(button, tooltipText);
        return button;
    }

    void configureCanvas() {
        horizontalRuler.setHeight(RULER_SIZE);
        verticalRuler.setWidth(RULER_SIZE);
        drawingCanvas.setFocusTraversable(true);

        drawingPane.widthProperty().addListener((ignored, oldValue, newValue) -> resizeCanvases());
        drawingPane.heightProperty().addListener((ignored, oldValue, newValue) -> resizeCanvases());
        horizontalRuler.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> startGuideDrag(GuideOrientation.HORIZONTAL, guideWorldPositionFromHorizontalRuler(event)));
        horizontalRuler.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> updateGuideDrag(GuideOrientation.HORIZONTAL, guideWorldPositionFromHorizontalRuler(event)));
        horizontalRuler.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> finishGuideDrag(GuideOrientation.HORIZONTAL, guideWorldPositionFromHorizontalRuler(event)));
        verticalRuler.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> startGuideDrag(GuideOrientation.VERTICAL, guideWorldPositionFromVerticalRuler(event)));
        verticalRuler.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> updateGuideDrag(GuideOrientation.VERTICAL, guideWorldPositionFromVerticalRuler(event)));
        verticalRuler.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> finishGuideDrag(GuideOrientation.VERTICAL, guideWorldPositionFromVerticalRuler(event)));

        drawingCanvas.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            lastCursor = screenToWorld(event.getX(), event.getY());
            altPressed = event.isAltDown();
            updateMouseCursor();
            updateStatus();
            render();
        });
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        drawingCanvas.addEventHandler(MouseEvent.MOUSE_EXITED, event -> drawingCanvas.setCursor(Cursor.DEFAULT));
        drawingCanvas.setOnScroll(event -> {
            double oldScale = scale();
            double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
            zoom = twoDZoomRange.clamp(zoom * zoomFactor);
            double newScale = scale();
            offsetX = event.getX() - ((event.getX() - offsetX) / oldScale) * newScale;
            offsetY = event.getY() - ((event.getY() - offsetY) / oldScale) * newScale;
            render();
        });
    }
}
