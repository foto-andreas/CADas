package de.andreas.cadas.ui;

import de.andreas.cadas.application.view.CameraPose;
import de.andreas.cadas.application.view.ProjectionMode;
import de.andreas.cadas.application.view.RenderableBox;
import de.andreas.cadas.application.view.RenderableMesh;
import de.andreas.cadas.application.view.RotationAxis;
import de.andreas.cadas.application.view.SelectionKey;
import de.andreas.cadas.application.view.ThreeDCameraController;
import de.andreas.cadas.application.view.ThreeDInteriorViewService;
import de.andreas.cadas.application.view.ThreeDInteriorViewService.InteriorViewTarget;
import de.andreas.cadas.application.view.ThreeDSceneBounds;
import de.andreas.cadas.application.view.ThreeDSceneFitService;
import de.andreas.cadas.application.view.ThreeDSceneModel;
import de.andreas.cadas.application.view.ThreeDSceneModelBuilder;
import de.andreas.cadas.application.view.ThreeDViewPreparation;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public final class ThreeDViewport extends BorderPane {

    private static final double WORLD_SCALE = 0.08;
    private static final double MIN_CAMERA_DISTANCE_UNITS = 10.0;
    private static final double MAX_CAMERA_DISTANCE_UNITS = 50_000.0;
    private static final double DEFAULT_PERSPECTIVE_FOV_DEGREES = 38.0;
    private static final double PERSPECTIVE_FIT_PADDING = 1.8;
    private static final double ORTHO_FOV_DEGREES = 15.0;
    private static final double ORTHO_FIT_PADDING = 1.4;
    private static final double INTERIOR_FOV_DEGREES = 64.0;
    private static final double INTERIOR_MIN_FOV_DEGREES = 28.0;
    private static final double INTERIOR_MAX_FOV_DEGREES = 115.0;
    private static final double INTERIOR_WALK_MILLIMETERS_PER_PIXEL = 8.0;
    private static final double MAX_INTERIOR_WALL_CLEARANCE_MILLIMETERS = 150.0;

    private final ThreeDViewPreparation viewPreparation = new ThreeDViewPreparation();
    private final ThreeDSceneModelBuilder modelBuilder = new ThreeDSceneModelBuilder();
    private final ThreeDCameraController cameraController = new ThreeDCameraController();
    private final ThreeDSceneFitService sceneFitService = new ThreeDSceneFitService();
    private final ThreeDInteriorViewService interiorViewService = new ThreeDInteriorViewService();
    private final PerspectiveCamera perspectiveCamera = new PerspectiveCamera(true);
    private final Group worldRoot = new Group();


    private final Group sceneGroup = new Group();
    private final Group orbitGroup = new Group();
    private final Group modelGroup = new Group();
    private final Group lightGroup = new Group();
    private final SubScene subScene = new SubScene(
            worldRoot,
            0.0,
            0.0,
            true,
            SceneAntialiasing.BALANCED
    );
    private final ComboBox<ProjectionMode> projectionModeSelector = new ComboBox<>();
    private final CheckBox surfaceLayersCheckBox = new CheckBox("3D Ebenen");
    private final CheckBox surfaceRenderingCheckBox = new CheckBox("Oberflächenrendering");
    private final FlowPane levelTogglePane = new FlowPane(8, 8);
    private final Label cameraStatusLabel = new Label();
    private final Label sceneStatsLabel = new Label("3D-Szene: 0 Körper");
    private final Label sceneHintLabel = new Label("Die 3D-Ansicht wird gefüllt, sobald auf der aktiven Etage Wände, Räume oder Treppen vorhanden sind.");
    private final Map<String, BooleanProperty> levelVisibility = new LinkedHashMap<>();
    private final Consumer<SelectionKey> selectionConsumer;

    private ProjectModel currentProject;
    private CameraPose cameraPose = viewPreparation.defaultPose();
    private ThreeDSceneBounds sceneBounds = ThreeDSceneBounds.fallback(
            5_000.0 * WORLD_SCALE,
            3_000.0 * WORLD_SCALE,
            5_000.0 * WORLD_SCALE
    );
    private SelectionKey selectedSelection;
    private Set<SelectionKey> selectedSelections = Set.of();
    private boolean fitToSceneRequested = true;
    private double dragStartX;
    private double dragStartY;
    private CameraPose dragStartPose;
    private MouseButton dragButton = MouseButton.NONE;
    private boolean sceneWasEmpty = true;
    private int lastRenderedBodyCount;
    private boolean surfaceRenderingMode;
    private CameraMode cameraMode = CameraMode.ORBIT;
    private InteriorViewTarget interiorTarget;
    private double interiorFieldOfViewDegrees = INTERIOR_FOV_DEGREES;
    private double interiorEyeXMillimeters;
    private double interiorEyeZMillimeters;
    private double dragStartInteriorEyeXMillimeters;
    private double dragStartInteriorEyeZMillimeters;

    public ThreeDViewport(Consumer<SelectionKey> selectionConsumer) {
        this.selectionConsumer = selectionConsumer;
        configureCameras();
        configureScene();
        configureControls();
        configureLayout();
        updateCamera();
    }

    public void syncLevels(List<Level> levels, String activeLevelName) {
        for (Level level : levels) {
            if (!levelVisibility.containsKey(level.name())) {
                levelVisibility.put(level.name(), new SimpleBooleanProperty(level.name().equals(activeLevelName) || levels.size() == 1));
            }
        }
        levelVisibility.keySet().removeIf(name -> levels.stream().noneMatch(level -> level.name().equals(name)));
        rebuildLevelTogglePane();
        if (currentProject != null) {
            refresh(currentProject);
        }
    }

    public void refresh(ProjectModel project) {
        currentProject = project;
        surfaceRenderingMode = surfaceRenderingCheckBox.isSelected();
        Set<String> visibleLevels = levelVisibility.entrySet().stream()
                .filter(entry -> entry.getValue().get())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (visibleLevels.isEmpty() && !project.levels().isEmpty()) {
            visibleLevels = Set.of(project.primaryLevel().name());
        }
        if (cameraMode == CameraMode.INTERIOR && interiorTarget != null) {
            visibleLevels = Set.of(interiorTarget.levelName());
        }
        ThreeDSceneModel sceneModel = modelBuilder.build(
                project,
                visibleLevels,
                surfaceLayersCheckBox.isSelected(),
                surfaceRenderingCheckBox.isSelected()
        );
        rebuildScene(sceneModel);
        boolean isEmpty = sceneModel.boxes().isEmpty() && sceneModel.meshes().isEmpty();
        sceneHintLabel.setVisible(isEmpty);
        sceneHintLabel.setManaged(isEmpty);
        if (!isEmpty && (fitToSceneRequested || sceneWasEmpty)) {
            fitCameraToScene();
        } else {
            updateCamera();
        }
        sceneWasEmpty = isEmpty;
    }

    public void setSelectedSelection(SelectionKey selectedSelection) {
        this.selectedSelection = selectedSelection;
        if (currentProject != null) {
            refresh(currentProject);
        }
    }

    public void setSelectedSelections(Set<SelectionKey> selectedSelections) {
        this.selectedSelections = Set.copyOf(selectedSelections);
        if (currentProject != null) {
            refresh(currentProject);
        }
    }

    public void applyViewPreset(ThreeDViewPreset viewPreset) {
        cameraMode = CameraMode.ORBIT;
        interiorTarget = null;
        cameraPose = viewPreparation.poseForAngles(
                currentProjectionMode(),
                viewPreset.cameraAzimuthDegrees(),
                viewPreset.cameraElevationDegrees()
        );
        fitToSceneRequested = true;
        if (currentProject != null) {
            refresh(currentProject);
            return;
        }
        updateCamera();
    }

    public void resetToDefaultView() {
        cameraMode = CameraMode.ORBIT;
        interiorTarget = null;
        ProjectionMode projectionMode = currentProjectionMode();
        CameraPose defaultPose = viewPreparation.defaultPose();
        cameraPose = new CameraPose(
                projectionMode,
                defaultPose.azimuthDegrees(),
                defaultPose.elevationDegrees(),
                defaultPose.distance(),
                0.0,
                0.0,
                0.0
        );
        fitToSceneRequested = true;
        if (currentProject != null) {
            refresh(currentProject);
            return;
        }
        updateCamera();
    }

    public void centerCurrentView() {
        if (cameraMode == CameraMode.INTERIOR) {
            resetInteriorPose();
            updateCamera();
            return;
        }
        fitCameraToScene();
    }

    public void resetElevationToZero() {
        cameraPose = new CameraPose(
                cameraPose.projectionMode(),
                cameraPose.azimuthDegrees(),
                0.0,
                cameraPose.distance(),
                cameraPose.panX(),
                cameraPose.panY(),
                cameraPose.panZ()
        );
        fitToSceneRequested = true;
        if (currentProject != null) {
            refresh(currentProject);
            return;
        }
        updateCamera();
    }

    public void activateOrbitView() {
        cameraMode = CameraMode.ORBIT;
        interiorTarget = null;
        fitToSceneRequested = true;
        if (currentProject != null) {
            refresh(currentProject);
            return;
        }
        updateCamera();
    }

    public void activateInteriorView(ProjectModel project, Level level, Room room) {
        currentProject = project;
        interiorTarget = interiorViewService.targetFor(project, level, room);
        cameraMode = CameraMode.INTERIOR;
        resetInteriorPose();
        projectionModeSelector.setValue(ProjectionMode.PERSPECTIVE);
        surfaceRenderingCheckBox.setSelected(true);
        fitToSceneRequested = false;
        refresh(project);
    }

    private void configureCameras() {
        perspectiveCamera.setNearClip(0.5);
        perspectiveCamera.setFarClip(MAX_CAMERA_DISTANCE_UNITS * 4.0);
        perspectiveCamera.setFieldOfView(DEFAULT_PERSPECTIVE_FOV_DEGREES);
    }

    private void configureScene() {
        worldRoot.getChildren().add(lightGroup);
        worldRoot.getChildren().add(sceneGroup);
        worldRoot.getChildren().add(perspectiveCamera);
        sceneGroup.getChildren().add(orbitGroup);
        orbitGroup.getChildren().add(modelGroup);

        AmbientLight ambient = new AmbientLight(Color.color(0.75, 0.75, 0.78));
        PointLight key = new PointLight(Color.color(0.95, 0.95, 1.0));
        PointLight fill = new PointLight(Color.color(0.6, 0.65, 0.7));
        lightGroup.getChildren().addAll(ambient, key, fill);
        applyLightPositions(120.0);

        subScene.setFill(Color.web("#1f2733"));
        subScene.setCamera(perspectiveCamera);
        subScene.setFocusTraversable(true);
        subScene.setOnMousePressed(event -> {
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
            dragStartPose = cameraPose;
            dragStartInteriorEyeXMillimeters = interiorEyeXMillimeters;
            dragStartInteriorEyeZMillimeters = interiorEyeZMillimeters;
            dragButton = event.getButton();
        });
        subScene.setOnMouseDragged(event -> {
            if (dragStartPose == null) {
                return;
            }
            double deltaX = event.getSceneX() - dragStartX;
            double deltaY = event.getSceneY() - dragStartY;
            if (dragButton == MouseButton.PRIMARY) {
                cameraPose = cameraController.orbit(dragStartPose, deltaX * 0.35, -deltaY * 0.35);
            } else if (dragButton == MouseButton.SECONDARY && cameraMode == CameraMode.INTERIOR) {
                moveInteriorCamera(deltaY);
            } else if (dragButton == MouseButton.SECONDARY && cameraMode == CameraMode.ORBIT) {
                cameraPose = cameraController.pan(dragStartPose, deltaX, deltaY);
            }
            fitToSceneRequested = false;
            updateCamera();
        });
        subScene.setOnMouseReleased(event -> dragButton = MouseButton.NONE);
        subScene.setOnScroll(event -> {
            if (cameraMode == CameraMode.INTERIOR) {
                zoomInteriorFieldOfView(event.getDeltaY() > 0 ? 0.92 : 1.08);
                return;
            }
            cameraPose = cameraController.zoom(cameraPose, event.getDeltaY() > 0 ? 0.92 : 1.08);
            fitToSceneRequested = false;
            updateCamera();
        });
    }

    private void applyLightPositions(double distanceUnits) {
        double lightSpan = Math.max(distanceUnits, 50.0);
        if (lightGroup.getChildren().size() >= 3) {
            Node first = lightGroup.getChildren().get(1);
            Node second = lightGroup.getChildren().get(2);
            if (first instanceof PointLight key) {
                key.setTranslateX(-lightSpan);
                key.setTranslateY(-lightSpan * 0.7);
                key.setTranslateZ(lightSpan * 0.5);
            }
            if (second instanceof PointLight fill) {
                fill.setTranslateX(lightSpan);
                fill.setTranslateY(lightSpan * 0.6);
                fill.setTranslateZ(lightSpan * 0.4);
            }
        }
    }

    private void configureControls() {
        projectionModeSelector.getItems().addAll(ProjectionMode.values());
        projectionModeSelector.setValue(cameraPose.projectionMode());
        projectionModeSelector.setPrefWidth(160);
        projectionModeSelector.valueProperty().addListener((ignored, oldValue, newValue) -> {
            if (newValue != null) {
                cameraPose = cameraController.switchProjection(cameraPose, newValue);
                fitToSceneRequested = true;
                updateCamera();
            }
        });
        surfaceLayersCheckBox.setSelected(true);
        surfaceLayersCheckBox.selectedProperty().addListener((ignored, oldValue, newValue) -> {
            if (currentProject != null) {
                refresh(currentProject);
            }
        });
        surfaceRenderingCheckBox.setSelected(true);
        surfaceRenderingCheckBox.selectedProperty().addListener((ignored, oldValue, newValue) -> {
            if (currentProject != null) {
                refresh(currentProject);
            }
        });
        applyTooltip(projectionModeSelector, "Schaltet die 3D-Kamera zwischen orthografischer und perspektivischer Projektion um.");
        applyTooltip(cameraStatusLabel, "Zeigt den aktuellen Zustand der 3D-Kamera mit Projektion, Winkel und Abstand an.");
        applyTooltip(sceneStatsLabel, "Zeigt an, wie viele 3D-Körper aktuell aus dem Fachmodell abgeleitet wurden.");
        levelTogglePane.setPadding(new Insets(6, 0, 0, 0));
    }

    private void configureLayout() {
        Label title = new Label("3D-Ansicht");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Button isometricViewButton = new Button("Iso");
        isometricViewButton.setOnAction(event -> resetToDefaultView());
        Button topViewButton = viewPresetButton(ThreeDViewPreset.TOP);
        Button bottomViewButton = viewPresetButton(ThreeDViewPreset.BOTTOM);
        Button frontViewButton = viewPresetButton(ThreeDViewPreset.FRONT);
        Button backViewButton = viewPresetButton(ThreeDViewPreset.BACK);
        Button rightViewButton = viewPresetButton(ThreeDViewPreset.RIGHT);
        Button leftViewButton = viewPresetButton(ThreeDViewPreset.LEFT);
        Button orbitLeftButton = new Button("←");
        orbitLeftButton.setOnAction(event -> nudgeOrbit(15.0, 0.0));
        Button orbitRightButton = new Button("→");
        orbitRightButton.setOnAction(event -> nudgeOrbit(-15.0, 0.0));
        Button orbitUpButton = new Button("↑");
        orbitUpButton.setOnAction(event -> nudgeOrbit(0.0, 8.0));
        Button orbitDownButton = new Button("↓");
        orbitDownButton.setOnAction(event -> nudgeOrbit(0.0, -8.0));
        Button fitSceneButton = new Button("Modell einpassen");
        fitSceneButton.setOnAction(event -> centerCurrentView());
        applyTooltip(isometricViewButton, "Setzt auf die räumliche Standardansicht zurück und passt das Modell in dieser Orientierung ein.");
        applyTooltip(orbitLeftButton, "Dreht die Orbitansicht schrittweise nach links; in der Innenansicht dreht sich nur der Blick am festen Kamerastandpunkt.");
        applyTooltip(orbitRightButton, "Dreht die Orbitansicht schrittweise nach rechts; in der Innenansicht dreht sich nur der Blick am festen Kamerastandpunkt.");
        applyTooltip(orbitUpButton, "Hebt die Orbitansicht schrittweise an; in der Innenansicht neigt sich nur der Blick am festen Kamerastandpunkt nach oben.");
        applyTooltip(orbitDownButton, "Senkt die Orbitansicht schrittweise ab; in der Innenansicht neigt sich nur der Blick am festen Kamerastandpunkt nach unten.");
        applyTooltip(fitSceneButton, "Richtet Kameraabstand und Mittelpunkt so aus, dass das aktuelle Modell vollständig sichtbar wird.");
        applyTooltip(surfaceLayersCheckBox, "Blendet zusätzliche Flächen-Ebenen als gestapelte 3D-Schichten ein oder aus.");
        applyTooltip(surfaceRenderingCheckBox, "Schaltet von der transparenten Modellansicht auf eine flächenbetonte Oberflächenansicht um.");

        HBox titleRow = new HBox(10.0, title, projectionModeSelector);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        FlowPane controlRow = new FlowPane(6.0, 6.0,
                isometricViewButton, topViewButton, bottomViewButton,
                frontViewButton, backViewButton, leftViewButton, rightViewButton,
                orbitUpButton, orbitDownButton, orbitLeftButton, orbitRightButton,
                fitSceneButton,
                surfaceLayersCheckBox, surfaceRenderingCheckBox
        );
        controlRow.setAlignment(Pos.CENTER_LEFT);
        controlRow.prefWrapLengthProperty().bind(widthProperty().subtract(40));

        VBox header = new VBox(4.0, titleRow, controlRow);
        header.setAlignment(Pos.CENTER_LEFT);

        ScrollPane levelScrollPane = new ScrollPane(levelTogglePane);
        levelScrollPane.setFitToWidth(true);
        levelScrollPane.setPrefViewportHeight(40);
        levelScrollPane.setStyle("-fx-background-color: transparent;");

        sceneHintLabel.setWrapText(true);
        sceneHintLabel.setMaxWidth(280);
        sceneHintLabel.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-padding: 12; -fx-background-radius: 12; -fx-text-fill: #5f5548;");
        StackPane sceneHolder = new StackPane(subScene, sceneHintLabel);
        sceneHolder.setMinSize(200, 200);
        BorderPane.setMargin(sceneHolder, new Insets(6, 0, 6, 0));
        subScene.widthProperty().bind(sceneHolder.widthProperty());
        subScene.heightProperty().bind(sceneHolder.heightProperty());
        VBox topArea = new VBox(6.0, header, levelScrollPane);
        setTop(topArea);
        setCenter(sceneHolder);
        VBox footer = new VBox(4.0, sceneStatsLabel, cameraStatusLabel);
        setBottom(footer);
        setPadding(new Insets(12, 0, 0, 12));
        setStyle("-fx-background-color: rgba(255,255,255,0.28); -fx-background-radius: 16;");
    }

    private void rebuildLevelTogglePane() {
        levelTogglePane.getChildren().clear();
        for (Map.Entry<String, BooleanProperty> entry : levelVisibility.entrySet()) {
            CheckBox checkBox = new CheckBox(entry.getKey());
            checkBox.selectedProperty().bindBidirectional(entry.getValue());
            checkBox.selectedProperty().addListener((ignored, oldValue, newValue) -> {
                if (currentProject != null) {
                    refresh(currentProject);
                }
            });
            applyTooltip(checkBox, "Blendet das Geschoss `" + entry.getKey() + "` in der 3D-Ansicht ein oder aus.");
            levelTogglePane.getChildren().add(checkBox);
        }
    }

    private void rebuildScene(ThreeDSceneModel sceneModel) {
        modelGroup.getChildren().clear();
        lastRenderedBodyCount = sceneModel.boxes().size() + sceneModel.meshes().size();
        Map<String, Group> groupedByLevel = new LinkedHashMap<>();
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (RenderableBox renderableBox : sceneModel.boxes()) {
            Group levelGroup = groupedByLevel.computeIfAbsent(renderableBox.levelName(), ignored -> new Group());
            levelGroup.getChildren().add(createNode(renderableBox));
            double hw = (renderableBox.width() * WORLD_SCALE) / 2.0;
            double hh = (renderableBox.height() * WORLD_SCALE) / 2.0;
            double hd = (renderableBox.depth() * WORLD_SCALE) / 2.0;
            double cx = renderableBox.centerX() * WORLD_SCALE;
            double cy = -renderableBox.centerY() * WORLD_SCALE;
            double cz = renderableBox.centerZ() * WORLD_SCALE;
            double angleRad = Math.toRadians(renderableBox.rotationDegrees());
            double absCos = Math.abs(Math.cos(angleRad));
            double absSin = Math.abs(Math.sin(angleRad));
            double halfExtX = hw;
            double halfExtY = hh;
            double halfExtZ = hd;
            switch (renderableBox.rotationAxis()) {
                case X -> {
                    halfExtX = hw;
                    halfExtY = absCos * hh + absSin * hd;
                    halfExtZ = absSin * hh + absCos * hd;
                }
                case Y -> {
                    halfExtX = absCos * hw + absSin * hd;
                    halfExtY = hh;
                    halfExtZ = absSin * hw + absCos * hd;
                }
                case Z -> {
                    halfExtX = absCos * hw + absSin * hh;
                    halfExtY = absSin * hw + absCos * hh;
                    halfExtZ = hd;
                }
            }
            minX = Math.min(minX, cx - halfExtX);
            maxX = Math.max(maxX, cx + halfExtX);
            minY = Math.min(minY, cy - halfExtY);
            maxY = Math.max(maxY, cy + halfExtY);
            minZ = Math.min(minZ, cz - halfExtZ);
            maxZ = Math.max(maxZ, cz + halfExtZ);
        }
        for (RenderableMesh rm : sceneModel.meshes()) {
            Group levelGroup = groupedByLevel.computeIfAbsent(rm.levelName(), ignored -> new Group());
            levelGroup.getChildren().add(createMeshNode(rm));
            float[] pts = rm.points();
            for (int i = 0; i < pts.length; i += 3) {
                double cx = pts[i] * WORLD_SCALE;
                double cy = -(rm.baseY() + pts[i + 1]) * WORLD_SCALE;
                double cz = pts[i + 2] * WORLD_SCALE;
                minX = Math.min(minX, cx);
                maxX = Math.max(maxX, cx);
                minY = Math.min(minY, cy);
                maxY = Math.max(maxY, cy);
                minZ = Math.min(minZ, cz);
                maxZ = Math.max(maxZ, cz);
            }
            double yBottom = -(rm.baseY() + rm.height()) * WORLD_SCALE;
            double yTop = -rm.baseY() * WORLD_SCALE;
            minY = Math.min(minY, Math.min(yBottom, yTop));
            maxY = Math.max(maxY, Math.max(yBottom, yTop));
        }
        modelGroup.getChildren().addAll(groupedByLevel.values());
        sceneStatsLabel.setText("3D-Szene: " + lastRenderedBodyCount + " Körper");
        if (sceneModel.boxes().isEmpty() && sceneModel.meshes().isEmpty()) {
            sceneBounds = ThreeDSceneBounds.fallback(
                    5_000.0 * WORLD_SCALE,
                    3_000.0 * WORLD_SCALE,
                    5_000.0 * WORLD_SCALE
            );
        } else {
            sceneBounds = ThreeDSceneBounds.fromExtents(
                    minX,
                    maxX,
                    minY,
                    maxY,
                    minZ,
                    maxZ
            );
        }
    }

    public int renderedBodyCount() {
        return lastRenderedBodyCount;
    }

    public boolean hasVisibleSceneContent() {
        return lastRenderedBodyCount > 0;
    }

    public String cameraStatusText() {
        return cameraStatusLabel.getText();
    }

    public SubScene subScene() {
        return subScene;
    }

    public void exportSnapshot(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Für den 3D-Snapshot wird ein Zielpfad benötigt.");
        }
        if (getWidth() <= 0 || getHeight() <= 0) {
            throw new IllegalStateException("Die 3D-Ansicht hat noch keine nutzbare Größe.");
        }
        fitCameraToScene();
        applyCss();
        layout();
        // Das BorderPane selbst snapshotten, damit der JavaFX-Renderer die SubScene
        // als zusammengesetzten Node korrekt in den 3D-Pfad einbezieht.
        WritableImage image = snapshot(null, null);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", path.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("3D-Snapshot konnte nicht geschrieben werden.", exception);
        }
    }

    public void exportSubSceneSnapshot(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Für den 3D-Snapshot wird ein Zielpfad benötigt.");
        }
        fitCameraToScene();
        WritableImage image = subScene.snapshot(null, null);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", path.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("3D-SubScene-Snapshot konnte nicht geschrieben werden.", exception);
        }
    }

    public void automationOrbit(double azimuthDelta, double elevationDelta) {
        nudgeOrbit(azimuthDelta, elevationDelta);
    }

    public void automationPan(double deltaScreenX, double deltaScreenY) {
        fitToSceneRequested = false;
        if (cameraMode == CameraMode.INTERIOR) {
            dragStartPose = cameraPose;
            dragStartInteriorEyeXMillimeters = interiorEyeXMillimeters;
            dragStartInteriorEyeZMillimeters = interiorEyeZMillimeters;
            moveInteriorCamera(deltaScreenY);
            updateCamera();
            return;
        }
        cameraPose = cameraController.pan(cameraPose, deltaScreenX, deltaScreenY);
        updateCamera();
    }

    public void automationZoom(double factor) {
        if (cameraMode == CameraMode.INTERIOR) {
            zoomInteriorFieldOfView(factor);
            return;
        }
        fitToSceneRequested = false;
        cameraPose = cameraController.zoom(cameraPose, factor);
        updateCamera();
    }

    public void automationFitToScene() {
        centerCurrentView();
    }

    public void setProjectionMode(ProjectionMode projectionMode) {
        projectionModeSelector.setValue(projectionMode);
        // setValue triggert den Listener, der fitCameraToScene automatisch ausführt.
    }

    public String diagnoseRenderState() {
        // Sammelt den vollständigen Renderzustand, um zu verstehen, warum die 3D-Boxen
        // möglicherweise nicht sichtbar sind. Liefert subScene/Boxen/Lichter/Kamera-
        // Eigenschaften als einzeiligen Text, der in den automationSnapshot eingebettet wird.
        StringBuilder sb = new StringBuilder();
        sb.append("subScene=").append(subScene.getWidth()).append("x").append(subScene.getHeight()).append("|");
        sb.append("subSceneStyleable=").append(subScene.isVisible()).append("|");
        sb.append("boxCount=").append(modelGroup.getChildren().size()).append("|");
        sb.append("groupCount=").append(countNodes(modelGroup)).append("|");
        sb.append("lights=");
        for (Node child : lightGroup.getChildren()) {
            sb.append(child.getClass().getSimpleName()).append("(");
            if (child instanceof AmbientLight ambient) {
                sb.append("color=").append(ambient.getColor()).append(",on=").append(ambient.isLightOn());
            } else if (child instanceof PointLight point) {
                sb.append("pos=(").append(point.getTranslateX()).append(",")
                        .append(point.getTranslateY()).append(",")
                        .append(point.getTranslateZ()).append("),on=").append(point.isLightOn());
            }
            sb.append(");");
        }
        sb.append("|");
        sb.append("cam=").append(subScene.getCamera().getClass().getSimpleName());
        if (subScene.getCamera() instanceof PerspectiveCamera pc) {
            sb.append("(fov=").append(pc.getFieldOfView())
                    .append(",near=").append(pc.getNearClip())
                    .append(",far=").append(pc.getFarClip()).append(")");
        }
        sb.append("|");
        sb.append("camPos=").append(subScene.getCamera().getTransforms()).append("|");
        sb.append("orbit=").append(orbitGroup.getTransforms()).append("|");
        sb.append("model=").append(modelGroup.getTransforms()).append("|");
        sb.append("scene=").append(sceneGroup.getTransforms()).append("|");
        sb.append("worldRoot=").append(worldRoot.getChildren().size()).append("|");
        sb.append("camDistMM=").append(worldUnitsToMillimeters(cameraPose.distance())).append("|");
        sb.append("sceneSpanMM=").append(worldUnitsToMillimeters(sceneBounds.horizontalSpan())).append("|");
        sb.append("sceneHeightMM=").append(worldUnitsToMillimeters(sceneBounds.spanY())).append("|");
        return sb.toString();
    }

    private int countNodes(javafx.scene.Parent parent) {
        int count = parent.getChildrenUnmodifiable().size();
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof javafx.scene.Parent p) {
                count += countNodes(p);
            }
        }
        return count;
    }

    private Node createNode(RenderableBox renderableBox) {
        // Größen und Positionen kommen aus dem Fachmodell in Millimetern und werden hier
        // mit WORLD_SCALE in JavaFX-Einheiten umgerechnet. So passen die Boxen zum
        // Kamera-Abstand in derselben Einheit.
        boolean joint = "joint".equals(renderableBox.materialKey());
        double widthUnits = renderableBox.width() * WORLD_SCALE;
        double heightUnits = renderableBox.height() * WORLD_SCALE;
        double depthUnits = renderableBox.depth() * WORLD_SCALE;
        if (!joint) {
            widthUnits = Math.max(1.0, widthUnits);
            heightUnits = Math.max(1.0, heightUnits);
            depthUnits = Math.max(1.0, depthUnits);
        }
        Box box = new Box(widthUnits, heightUnits, depthUnits);
        box.setTranslateX(renderableBox.centerX() * WORLD_SCALE);
        box.setTranslateY(-renderableBox.centerY() * WORLD_SCALE);
        box.setTranslateZ(renderableBox.centerZ() * WORLD_SCALE);
        box.setMaterial(material(renderableBox));
        box.setDrawMode(shouldRenderFilled(renderableBox) ? DrawMode.FILL : DrawMode.LINE);
        box.setCullFace(CullFace.NONE);
        box.setOpacity(renderableBox.opacity());
        box.getTransforms().add(rotation(renderableBox.rotationAxis(), renderableBox.rotationDegrees()));
        box.setUserData(renderableBox.selectionKey());
        box.setOnMouseClicked(event -> {
            if (renderableBox.selectionKey() != null) {
                selectionConsumer.accept(renderableBox.selectionKey());
                event.consume();
            }
        });
        return box;
    }

    private boolean shouldRenderFilled(RenderableBox renderableBox) {
        if (surfaceRenderingMode) {
            return true;
        }
        return "room-floor".equals(renderableBox.materialKey())
                || "joint".equals(renderableBox.materialKey());
    }

    private Rotate rotation(RotationAxis axis, double degrees) {
        return switch (axis) {
            case X -> new Rotate(degrees, Rotate.X_AXIS);
            case Y -> new Rotate(degrees, Rotate.Y_AXIS);
            case Z -> new Rotate(degrees, Rotate.Z_AXIS);
        };
    }

    private PhongMaterial material(RenderableBox renderableBox) {
        Color color = colorForKey(renderableBox.materialKey(), renderableBox.selectionKey());
        PhongMaterial material = new PhongMaterial(color);
        if ("joint".equals(renderableBox.materialKey())) {
            material.setDiffuseColor(color);
            material.setSpecularColor(Color.web("#222222"));
        } else {
            material.setSpecularColor(color.brighter());
        }
        return material;
    }

    private Color colorForKey(String materialKey, SelectionKey selectionKey) {
        boolean isSelected = (selectedSelection != null && selectedSelection.equals(selectionKey))
                || selectedSelections.contains(selectionKey);
        Color base = switch (materialKey) {
            case "wall" -> Color.web("#5b738a");
            case "door" -> Color.web("#c88349", 0.3);
            case "window" -> Color.web("#7ab9d6", 0.3);
            case "room-floor" -> Color.web("#b89c7d");
            case "room-ceiling" -> Color.web("#d7d3c8");
            case "room-volume" -> Color.web("#d8c6aa");
            case "stair" -> Color.web("#7a6d60");
            case "roof" -> Color.web("#8e5f54");
            case "surface-layer" -> Color.web("#8e7b5e");
            case "joint" -> Color.web("#1a1510");
            default -> Color.web("#8c877f");
        };
        if (isSelected) {
            base = base.deriveColor(0, 1.0, 1.2, 1.0);
        }
        return base;
    }

    private Node createMeshNode(RenderableMesh rm) {
        TriangleMesh mesh = new TriangleMesh();
        ObservableFloatArray meshPoints = mesh.getPoints();
        ObservableFloatArray texCoords = mesh.getTexCoords();
        ObservableIntegerArray faces = mesh.getFaces();
        ObservableIntegerArray smoothingGroups = mesh.getFaceSmoothingGroups();
        float[] src = rm.points();
        for (int i = 0; i < src.length; i += 3) {
            meshPoints.addAll(
                    src[i] * (float) WORLD_SCALE,
                    -src[i + 1] * (float) WORLD_SCALE,
                    src[i + 2] * (float) WORLD_SCALE
            );
        }
        int vertexCount = src.length / 3;
        for (int i = 0; i < vertexCount; i++) {
            texCoords.addAll(0f, 0f);
        }
        for (int i = 0; i < rm.faceCount(); i++) {
            int vi = i * 3;
            faces.addAll(vi, 0, vi + 1, 0, vi + 2, 0);
        }
        for (int i = 0; i < rm.faceCount(); i++) {
            smoothingGroups.addAll(0);
        }
        MeshView meshView = new MeshView(mesh);
        meshView.setTranslateY(-rm.baseY() * WORLD_SCALE);
        Color color = colorForKey(rm.materialKey(), rm.selectionKey());
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecularColor(color.brighter());
        meshView.setMaterial(material);
        meshView.setDrawMode(surfaceRenderingMode ? DrawMode.FILL : DrawMode.LINE);
        meshView.setCullFace(CullFace.NONE);
        meshView.setOpacity(rm.opacity());
        meshView.setUserData(rm.selectionKey());
        meshView.setOnMouseClicked(event -> {
            if (rm.selectionKey() != null) {
                selectionConsumer.accept(rm.selectionKey());
                event.consume();
            }
        });
        return meshView;
    }

    private void updateCamera() {
        if (cameraMode == CameraMode.INTERIOR && interiorTarget != null) {
            updateInteriorCamera();
            return;
        }

        // Das Modell wird im Ursprung zentriert (modelGroup). Die orbitGroup dreht
        // das zentrierte Modell um den Ursprung. Die sceneGroup verschiebt das Modell
        // (Panning). Die Kamera blickt fest entlang der +Z-Achse. Dadurch bleibt die
        // Drehachse immer im Modell-Mittelpunkt. JavaFX verwendet Y nach unten, daher
        // werden die Y-Koordinaten der Boxen in createNode und rebuildScene negiert.
        // Scale(1,1,-1) in modelGroup negiert die Z-Achse, damit die 3D-Draufsicht
        // (Elevation -90°) in der Y-Richtung mit dem 2D-Plan übereinstimmt. Der
        // 2D-Plan verwendet Plan-Y ohne Vorzeichenumkehr als Bildschirm-Y (Norden ist
        // unten), während die 3D-Szene Plan-Y als +Z mappt. Durch die Z-Negation wird
        // Norden (großes Plan-Y) in der Draufsicht korrekt unten dargestellt.
        modelGroup.getTransforms().setAll(
                new Scale(1.0, 1.0, -1.0),
                new Translate(-sceneBounds.centerX(), -sceneBounds.centerY(), sceneBounds.centerZ())
        );
        orbitGroup.getTransforms().setAll(
                new Rotate(-cameraPose.azimuthDegrees(), Rotate.Y_AXIS),
                new Rotate(-cameraPose.elevationDegrees(), Rotate.X_AXIS)
        );
        double panXScene = cameraPose.panX();
        double panYScene = -cameraPose.panY();
        sceneGroup.getTransforms().setAll(new Translate(panXScene, panYScene, 0.0));

        double cameraOffset = Math.max(MIN_CAMERA_DISTANCE_UNITS, cameraPose.distance());

        if (cameraPose.projectionMode() == ProjectionMode.ORTHOGRAPHIC) {
            perspectiveCamera.setFieldOfView(ORTHO_FOV_DEGREES);
        } else {
            perspectiveCamera.setFieldOfView(DEFAULT_PERSPECTIVE_FOV_DEGREES);
        }
        perspectiveCamera.getTransforms().setAll(new Translate(0.0, 0.0, -cameraOffset));
        subScene.setCamera(perspectiveCamera);

        if (fitToSceneRequested) {
            fitToSceneRequested = false;
            fitCameraToScene();
            return;
        }

        applyLightPositions(cameraPose.distance());
        String projName = cameraPose.projectionMode() == ProjectionMode.PERSPECTIVE
                ? "perspektivisch" : "orthografisch";
        cameraStatusLabel.setText(String.format(
                "3D Ansicht: %s | Azimut %.1f° | Elevation %.1f° | Abstand %.1f m | Versatz %.0f/%.0f mm | Szene %.0f mm",
                projName,
                cameraPose.azimuthDegrees(),
                cameraPose.elevationDegrees(),
                worldUnitsToMillimeters(cameraPose.distance()) / 1000.0,
                worldUnitsToMillimeters(cameraPose.panX()),
                worldUnitsToMillimeters(cameraPose.panY()),
                worldUnitsToMillimeters(sceneBounds.horizontalSpan())
        ));
    }

    private void updateInteriorCamera() {
        modelGroup.getTransforms().setAll(new Scale(1.0, 1.0, -1.0));
        orbitGroup.getTransforms().clear();
        sceneGroup.getTransforms().setAll();
        perspectiveCamera.setFieldOfView(interiorFieldOfViewDegrees);
        perspectiveCamera.getTransforms().setAll(
                new Translate(
                        interiorEyeXMillimeters * WORLD_SCALE,
                        -interiorTarget.eyeYMillimeters() * WORLD_SCALE,
                        -interiorEyeZMillimeters * WORLD_SCALE
                ),
                new Rotate(cameraPose.azimuthDegrees(), Rotate.Y_AXIS),
                new Rotate(cameraPose.elevationDegrees(), Rotate.X_AXIS)
        );
        subScene.setCamera(perspectiveCamera);
        applyLightPositions(260.0);
        cameraStatusLabel.setText(String.format(Locale.GERMAN,
                "3D Innenansicht: %s | Augenhöhe %.2f m | Position %.2f/%.2f m | Blick %.1f° / %.1f° | Sichtwinkel %.0f°",
                interiorTarget.roomName(),
                interiorTarget.eyeHeightAboveFloorMillimeters() / 1000.0,
                interiorEyeXMillimeters / 1000.0,
                interiorEyeZMillimeters / 1000.0,
                cameraPose.azimuthDegrees(),
                cameraPose.elevationDegrees(),
                interiorFieldOfViewDegrees
        ));
    }

    private void moveInteriorCamera(double deltaY) {
        if (interiorTarget == null || dragStartPose == null) {
            return;
        }
        double distanceMillimeters = -deltaY * INTERIOR_WALK_MILLIMETERS_PER_PIXEL;
        double azimuthRadians = Math.toRadians(dragStartPose.azimuthDegrees());
        double forwardX = Math.sin(azimuthRadians);
        double forwardZ = -Math.cos(azimuthRadians);
        PlanPoint bounded = boundedInteriorEyePosition(
                dragStartInteriorEyeXMillimeters,
                dragStartInteriorEyeZMillimeters,
                dragStartInteriorEyeXMillimeters + forwardX * distanceMillimeters,
                dragStartInteriorEyeZMillimeters + forwardZ * distanceMillimeters
        );
        interiorEyeXMillimeters = bounded.xMillimeters();
        interiorEyeZMillimeters = bounded.yMillimeters();
    }

    private void zoomInteriorFieldOfView(double factor) {
        fitToSceneRequested = false;
        interiorFieldOfViewDegrees = clamp(
                interiorFieldOfViewDegrees * factor,
                INTERIOR_MIN_FOV_DEGREES,
                INTERIOR_MAX_FOV_DEGREES
        );
        updateCamera();
    }

    private void nudgeOrbit(double azimuthDelta, double elevationDelta) {
        fitToSceneRequested = false;
        cameraPose = cameraController.orbit(cameraPose, azimuthDelta, elevationDelta);
        updateCamera();
    }

    private void fitCameraToScene() {
        if (cameraMode == CameraMode.INTERIOR) {
            updateCamera();
            return;
        }
        if (!sceneBounds.hasContent()) {
            return;
        }

        double fov = cameraPose.projectionMode() == ProjectionMode.ORTHOGRAPHIC
                ? ORTHO_FOV_DEGREES : DEFAULT_PERSPECTIVE_FOV_DEGREES;
        double padding = cameraPose.projectionMode() == ProjectionMode.ORTHOGRAPHIC
                ? ORTHO_FIT_PADDING : PERSPECTIVE_FIT_PADDING;
        double viewportWidth = subScene.getWidth() > 0.0 ? subScene.getWidth() : getWidth();
        double viewportHeight = subScene.getHeight() > 0.0 ? subScene.getHeight() : getHeight();
        double clamped = sceneFitService.calculateDistance(
                sceneBounds,
                cameraPose,
                viewportWidth,
                viewportHeight,
                fov,
                padding,
                20.0,
                MIN_CAMERA_DISTANCE_UNITS,
                MAX_CAMERA_DISTANCE_UNITS
        );

        cameraPose = new CameraPose(
                cameraPose.projectionMode(),
                cameraPose.azimuthDegrees(),
                cameraPose.elevationDegrees(),
                clamped,
                0.0,
                0.0,
                0.0
        );
        fitToSceneRequested = false;
        updateCamera();
    }

    private void resetInteriorPose() {
        interiorFieldOfViewDegrees = INTERIOR_FOV_DEGREES;
        if (interiorTarget != null) {
            interiorEyeXMillimeters = interiorTarget.eyeXMillimeters();
            interiorEyeZMillimeters = interiorTarget.eyeZMillimeters();
        }
        cameraPose = new CameraPose(
                ProjectionMode.PERSPECTIVE,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }

    private void applyTooltip(Node node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(320);
        Tooltip.install(node, tooltip);
    }

    private ProjectionMode currentProjectionMode() {
        return projectionModeSelector.getValue() == null
                ? cameraPose.projectionMode()
                : projectionModeSelector.getValue();
    }

    private Button viewPresetButton(ThreeDViewPreset viewPreset) {
        Button button = new Button(viewPreset.label());
        button.setOnAction(event -> applyViewPreset(viewPreset));
        applyTooltip(button, viewPreset.tooltip());
        return button;
    }

    private double worldUnitsToMillimeters(double worldUnits) {
        return worldUnits / WORLD_SCALE;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private PlanPoint boundedInteriorEyePosition(double startX, double startZ, double targetX, double targetZ) {
        if (isAllowedInteriorEyePosition(targetX, targetZ)) {
            return new PlanPoint(targetX, targetZ);
        }
        double low = 0.0;
        double high = 1.0;
        for (int index = 0; index < 24; index++) {
            double ratio = (low + high) / 2.0;
            double x = startX + (targetX - startX) * ratio;
            double z = startZ + (targetZ - startZ) * ratio;
            if (isAllowedInteriorEyePosition(x, z)) {
                low = ratio;
            } else {
                high = ratio;
            }
        }
        return new PlanPoint(
                startX + (targetX - startX) * low,
                startZ + (targetZ - startZ) * low
        );
    }

    private boolean isAllowedInteriorEyePosition(double xMillimeters, double zMillimeters) {
        if (interiorTarget == null || interiorTarget.roomOutline().size() < 3) {
            return true;
        }
        PlanPoint point = new PlanPoint(xMillimeters, zMillimeters);
        return isInsideRoom(point) && distanceToRoomBoundary(point) >= interiorWallClearanceMillimeters();
    }

    private boolean isInsideRoom(PlanPoint point) {
        List<PlanPoint> outline = interiorTarget.roomOutline();
        boolean inside = false;
        for (int current = 0, previous = outline.size() - 1; current < outline.size(); previous = current++) {
            PlanPoint a = outline.get(current);
            PlanPoint b = outline.get(previous);
            boolean crossesHorizontalRay = (a.yMillimeters() > point.yMillimeters()) != (b.yMillimeters() > point.yMillimeters());
            if (crossesHorizontalRay) {
                double intersectionX = (b.xMillimeters() - a.xMillimeters())
                        * (point.yMillimeters() - a.yMillimeters())
                        / (b.yMillimeters() - a.yMillimeters())
                        + a.xMillimeters();
                if (point.xMillimeters() < intersectionX) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    private double distanceToRoomBoundary(PlanPoint point) {
        List<PlanPoint> outline = interiorTarget.roomOutline();
        double minimumDistance = Double.POSITIVE_INFINITY;
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint start = outline.get(index);
            PlanPoint end = outline.get((index + 1) % outline.size());
            minimumDistance = Math.min(minimumDistance, distanceToSegment(point, start, end));
        }
        return minimumDistance;
    }

    private double distanceToSegment(PlanPoint point, PlanPoint start, PlanPoint end) {
        double dx = end.xMillimeters() - start.xMillimeters();
        double dy = end.yMillimeters() - start.yMillimeters();
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= 0.001) {
            return point.distanceTo(start).toMillimeters();
        }
        double ratio = clamp(
                ((point.xMillimeters() - start.xMillimeters()) * dx + (point.yMillimeters() - start.yMillimeters()) * dy) / lengthSquared,
                0.0,
                1.0
        );
        double closestX = start.xMillimeters() + ratio * dx;
        double closestY = start.yMillimeters() + ratio * dy;
        return Math.hypot(point.xMillimeters() - closestX, point.yMillimeters() - closestY);
    }

    private double interiorWallClearanceMillimeters() {
        List<PlanPoint> outline = interiorTarget.roomOutline();
        double minX = outline.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0);
        double maxX = outline.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0);
        double minY = outline.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0);
        double maxY = outline.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0);
        double smallerSpan = Math.min(maxX - minX, maxY - minY);
        return Math.max(20.0, Math.min(MAX_INTERIOR_WALL_CLEARANCE_MILLIMETERS, smallerSpan / 10.0));
    }

    private enum CameraMode {
        ORBIT,
        INTERIOR
    }
}
