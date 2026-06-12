package de.andreas.cadas.ui;

import de.andreas.cadas.application.view.CameraPose;
import de.andreas.cadas.application.view.ProjectionMode;
import de.andreas.cadas.application.view.RenderableBox;
import de.andreas.cadas.application.view.RotationAxis;
import de.andreas.cadas.application.view.SelectionKey;
import de.andreas.cadas.application.view.ThreeDCameraController;
import de.andreas.cadas.application.view.ThreeDSceneModel;
import de.andreas.cadas.application.view.ThreeDSceneModelBuilder;
import de.andreas.cadas.application.view.ThreeDViewPreparation;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
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
import javafx.scene.Camera;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public final class ThreeDViewport extends BorderPane {

    private static final double WORLD_SCALE = 0.08;
    private static final double MIN_CAMERA_DISTANCE_UNITS = 10.0;
    private static final double MAX_CAMERA_DISTANCE_UNITS = 50_000.0;
    private static final double DEFAULT_PERSPECTIVE_FOV_DEGREES = 38.0;
    private static final double FIT_PADDING_FACTOR = 1.4;

    private final ThreeDViewPreparation viewPreparation = new ThreeDViewPreparation();
    private final ThreeDSceneModelBuilder modelBuilder = new ThreeDSceneModelBuilder();
    private final ThreeDCameraController cameraController = new ThreeDCameraController();
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
    private SelectionKey selectedSelection;
    private Set<SelectionKey> selectedSelections = Set.of();
    private boolean fitToSceneRequested = true;
    private double sceneCenterX;
    private double sceneCenterZ;
    private double sceneSpanHorizontal;
    private double sceneHeightMax;
    private double sceneCenterY;
    private double dragStartX;
    private double dragStartY;
    private CameraPose dragStartPose;
    private MouseButton dragButton = MouseButton.NONE;
    private boolean sceneWasEmpty = true;
    private int lastRenderedBodyCount;
    private boolean surfaceRenderingMode;

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
        ThreeDSceneModel sceneModel = modelBuilder.build(
                project,
                visibleLevels,
                surfaceLayersCheckBox.isSelected(),
                surfaceRenderingCheckBox.isSelected()
        );
        rebuildScene(sceneModel);
        sceneHintLabel.setVisible(sceneModel.boxes().isEmpty());
        sceneHintLabel.setManaged(sceneModel.boxes().isEmpty());
        if (!sceneModel.boxes().isEmpty() && (fitToSceneRequested || sceneWasEmpty)) {
            fitCameraToScene();
        }
        sceneWasEmpty = sceneModel.boxes().isEmpty();
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

    public void applyViewOrientation(ViewOrientation viewOrientation) {
        cameraPose = viewPreparation.poseForAngles(
                projectionModeSelector.getValue(),
                viewOrientation.cameraAzimuthDegrees(),
                viewOrientation.cameraElevationDegrees()
        );
        fitToSceneRequested = true;
        if (currentProject != null) {
            refresh(currentProject);
            return;
        }
        updateCamera();
    }

    public void resetToCurrentOrientation() {
        ProjectionMode projectionMode = projectionModeSelector.getValue() == null
                ? ProjectionMode.PERSPECTIVE
                : projectionModeSelector.getValue();
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
            } else if (dragButton == MouseButton.SECONDARY) {
                cameraPose = cameraController.pan(dragStartPose, deltaX, deltaY);
            }
            fitToSceneRequested = false;
            updateCamera();
        });
        subScene.setOnMouseReleased(event -> dragButton = MouseButton.NONE);
        subScene.setOnScroll(event -> {
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
        isometricViewButton.setOnAction(event -> resetToCurrentOrientation());
        Button topViewButton = new Button("Oben");
        topViewButton.setOnAction(event -> applyViewOrientation(ViewOrientation.BOTTOM));
        Button bottomViewButton = new Button("Unten");
        bottomViewButton.setOnAction(event -> applyViewOrientation(ViewOrientation.TOP));
        Button frontViewButton = new Button("Vorne");
        frontViewButton.setOnAction(event -> applyViewOrientation(ViewOrientation.NORTH));
        Button backViewButton = new Button("Hinten");
        backViewButton.setOnAction(event -> applyViewOrientation(ViewOrientation.SOUTH));
        Button rightViewButton = new Button("Rechts");
        rightViewButton.setOnAction(event -> applyViewOrientation(ViewOrientation.EAST));
        Button leftViewButton = new Button("Links");
        leftViewButton.setOnAction(event -> applyViewOrientation(ViewOrientation.WEST));
        Button orbitLeftButton = new Button("←");
        orbitLeftButton.setOnAction(event -> nudgeOrbit(-15.0, 0.0));
        Button orbitRightButton = new Button("→");
        orbitRightButton.setOnAction(event -> nudgeOrbit(15.0, 0.0));
        Button orbitUpButton = new Button("↑");
        orbitUpButton.setOnAction(event -> nudgeOrbit(0.0, 8.0));
        Button orbitDownButton = new Button("↓");
        orbitDownButton.setOnAction(event -> nudgeOrbit(0.0, -8.0));
        Button fitSceneButton = new Button("Modell einpassen");
        fitSceneButton.setOnAction(event -> fitCameraToScene());
        applyTooltip(isometricViewButton, "Setzt auf die Standardansicht zurück (Azimut 45°, Elevation 0°, Abstand 9 m) – Höhe, Tiefe und Breite gleichzeitig sichtbar.");
        applyTooltip(topViewButton, "Wechselt die 3D-Kamera auf eine Draufsicht.");
        applyTooltip(bottomViewButton, "Wechselt die 3D-Kamera auf eine Untersicht.");
        applyTooltip(frontViewButton, "Wechselt die 3D-Kamera auf eine Vorderansicht.");
        applyTooltip(backViewButton, "Wechselt die 3D-Kamera auf eine Rückansicht.");
        applyTooltip(rightViewButton, "Wechselt die 3D-Kamera auf eine rechte Seitenansicht.");
        applyTooltip(leftViewButton, "Wechselt die 3D-Kamera auf eine linke Seitenansicht.");
        applyTooltip(orbitLeftButton, "Dreht die 3D-Kamera schrittweise nach links.");
        applyTooltip(orbitRightButton, "Dreht die 3D-Kamera schrittweise nach rechts.");
        applyTooltip(orbitUpButton, "Hebt die 3D-Kamera schrittweise an.");
        applyTooltip(orbitDownButton, "Senkt die 3D-Kamera schrittweise ab.");
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
        levelScrollPane.setPrefViewportHeight(56);
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
        lastRenderedBodyCount = sceneModel.boxes().size();
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
        modelGroup.getChildren().addAll(groupedByLevel.values());
        sceneStatsLabel.setText("3D-Szene: " + sceneModel.boxes().size() + " Körper");
        if (sceneModel.boxes().isEmpty()) {
            sceneCenterX = 0.0;
            sceneCenterZ = 0.0;
            sceneSpanHorizontal = 5_000.0 * WORLD_SCALE;
            sceneHeightMax = 3_000.0 * WORLD_SCALE;
            sceneCenterY = 0.0;
        } else {
            sceneCenterX = (minX + maxX) / 2.0;
            sceneCenterZ = (minZ + maxZ) / 2.0;
            sceneSpanHorizontal = Math.max(1_500.0 * WORLD_SCALE, Math.max(maxX - minX, maxZ - minZ));
            sceneHeightMax = Math.max(1_500.0 * WORLD_SCALE, maxY - minY);
            sceneCenterY = (minY + maxY) / 2.0;
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
        cameraPose = cameraController.pan(cameraPose, deltaScreenX, deltaScreenY);
        updateCamera();
    }

    public void automationZoom(double factor) {
        fitToSceneRequested = false;
        cameraPose = cameraController.zoom(cameraPose, factor);
        updateCamera();
    }

    public void automationFitToScene() {
        fitCameraToScene();
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
        sb.append("camDistMM=").append(cameraPose.distance()).append("|");
        sb.append("sceneSpan=").append(sceneSpanHorizontal).append("|");
        sb.append("sceneHeight=").append(sceneHeightMax).append("|");
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
        double widthUnits = Math.max(1.0, renderableBox.width() * WORLD_SCALE);
        double heightUnits = Math.max(1.0, renderableBox.height() * WORLD_SCALE);
        double depthUnits = Math.max(1.0, renderableBox.depth() * WORLD_SCALE);
        Box box = new Box(widthUnits, heightUnits, depthUnits);
        box.setTranslateX(renderableBox.centerX() * WORLD_SCALE);
        box.setTranslateY(-renderableBox.centerY() * WORLD_SCALE);
        box.setTranslateZ(renderableBox.centerZ() * WORLD_SCALE);
        box.setMaterial(material(renderableBox));
        box.setDrawMode(surfaceRenderingMode ? DrawMode.FILL : DrawMode.LINE);
        box.setCullFace(CullFace.NONE);
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

    private Rotate rotation(RotationAxis axis, double degrees) {
        return switch (axis) {
            case X -> new Rotate(degrees, Rotate.X_AXIS);
            case Y -> new Rotate(degrees, Rotate.Y_AXIS);
            case Z -> new Rotate(degrees, Rotate.Z_AXIS);
        };
    }

    private PhongMaterial material(RenderableBox renderableBox) {
        Color color = switch (renderableBox.materialKey()) {
            case "wall" -> Color.web("#5b738a");
            case "door" -> Color.web("#c88349");
            case "window" -> Color.web("#7ab9d6");
            case "room-floor" -> Color.web("#b89c7d");
            case "room-ceiling" -> Color.web("#d7d3c8");
            case "room-volume" -> Color.web("#d8c6aa");
            case "stair" -> Color.web("#7a6d60");
            case "roof" -> Color.web("#8e5f54");
            case "surface-layer" -> Color.web("#8e7b5e");
            default -> Color.web("#8c877f");
        };
        if ((selectedSelection != null && selectedSelection.equals(renderableBox.selectionKey()))
                || selectedSelections.contains(renderableBox.selectionKey())) {
            color = color.deriveColor(0, 1.0, 1.2, 1.0);
        }
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecularColor(color.brighter());
        return material;
    }

    private void updateCamera() {

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
                new Translate(-sceneCenterX, -sceneCenterY, sceneCenterZ)
        );
        orbitGroup.getTransforms().setAll(
                new Rotate(-cameraPose.azimuthDegrees(), Rotate.Y_AXIS),
                new Rotate(-cameraPose.elevationDegrees(), Rotate.X_AXIS)
        );
        double panXScene = cameraPose.panX();
        double panYScene = -cameraPose.panY();
        sceneGroup.getTransforms().setAll(new Translate(panXScene, panYScene, 0.0));

        double cameraOffset = Math.max(MIN_CAMERA_DISTANCE_UNITS, cameraPose.distance());
        perspectiveCamera.setNearClip(0.5);
        perspectiveCamera.setFarClip(MAX_CAMERA_DISTANCE_UNITS * 4.0);
        perspectiveCamera.getTransforms().setAll(new Translate(0.0, 0.0, -cameraOffset));
        boolean isOrthographic = cameraPose.projectionMode() == ProjectionMode.ORTHOGRAPHIC;
        perspectiveCamera.setFieldOfView(isOrthographic ? 5.0 : DEFAULT_PERSPECTIVE_FOV_DEGREES);
        subScene.setCamera(perspectiveCamera);

        if (fitToSceneRequested) {
            fitToSceneRequested = false;
            fitCameraToScene();
            return;
        }

        applyLightPositions(cameraPose.distance());
        cameraStatusLabel.setText(String.format(
                "3D Ansicht: %s | Azimut %.1f° | Elevation %.1f° | Abstand %.1f m | Versatz %.0f/%.0f mm | Szene %.0f mm",
                isOrthographic ? "orthografisch" : "perspektivisch",
                cameraPose.azimuthDegrees(),
                cameraPose.elevationDegrees(),
                cameraPose.distance() / 1000.0,
                cameraPose.panX(),
                cameraPose.panY(),
                sceneSpanHorizontal
        ));
    }

    private void nudgeOrbit(double azimuthDelta, double elevationDelta) {
        fitToSceneRequested = false;
        cameraPose = cameraController.orbit(cameraPose, azimuthDelta, elevationDelta);
        updateCamera();
    }

    private void fitCameraToScene() {
        if (sceneSpanHorizontal <= 0.0) {
            return;
        }
        // FOV setzen, BEVOR die Distanz berechnet wird. Dadurch arbeiten wir immer mit dem
        // korrekten FOV für den aktuellen Projektionsmodus, egal von wo fitCameraToScene
        // aufgerufen wird.
        boolean isOrthographic = cameraPose.projectionMode() == ProjectionMode.ORTHOGRAPHIC;
        double activeFovDegrees = isOrthographic ? 5.0 : DEFAULT_PERSPECTIVE_FOV_DEGREES;
        perspectiveCamera.setFieldOfView(activeFovDegrees);
        double halbeBildHoehe = Math.tan(Math.toRadians(activeFovDegrees / 2.0));
        double halbeBildBreite = halbeBildHoehe;
        double minHalbeBild = Math.min(halbeBildHoehe, halbeBildBreite);
        double modelHalfHorizontalUnits = sceneSpanHorizontal / 2.0;
        double horizontalAbstand = (modelHalfHorizontalUnits * FIT_PADDING_FACTOR) / minHalbeBild;
        double modelHeightUnits = sceneHeightMax;
        double heightAbstand = (modelHeightUnits * FIT_PADDING_FACTOR) / (2.0 * halbeBildHoehe);
        double benoetigterAbstandUnits = Math.max(horizontalAbstand, heightAbstand) + 20.0;
        double clamped = Math.max(MIN_CAMERA_DISTANCE_UNITS,
                Math.min(MAX_CAMERA_DISTANCE_UNITS, benoetigterAbstandUnits));
        cameraPose = new CameraPose(
                projectionModeSelector.getValue(),
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

    private void applyTooltip(Node node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(320);
        Tooltip.install(node, tooltip);
    }
}
