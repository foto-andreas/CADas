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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.ParallelCamera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SubScene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.AmbientLight;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public final class ThreeDViewport extends BorderPane {

    private static final double WORLD_SCALE = 0.08;

    private final ThreeDSceneModelBuilder modelBuilder = new ThreeDSceneModelBuilder();
    private final ThreeDCameraController cameraController = new ThreeDCameraController();
    private final PerspectiveCamera perspectiveCamera = new PerspectiveCamera(true);
    private final ParallelCamera parallelCamera = new ParallelCamera();
    private final Group worldRoot = new Group();
    private final Group modelRoot = new Group();
    private final SubScene subScene = new SubScene(worldRoot, 520, 720, true, SceneAntialiasing.BALANCED);
    private final ComboBox<ProjectionMode> projectionModeSelector = new ComboBox<>();
    private final CheckBox surfaceLayersCheckBox = new CheckBox("3D Ebenen");
    private final FlowPane levelTogglePane = new FlowPane(8, 8);
    private final Label cameraStatusLabel = new Label();
    private final Map<String, BooleanProperty> levelVisibility = new LinkedHashMap<>();
    private final Consumer<SelectionKey> selectionConsumer;

    private ProjectModel currentProject;
    private CameraPose cameraPose = new ThreeDViewPreparation().defaultPose();
    private SelectionKey selectedSelection;
    private double dragStartX;
    private double dragStartY;
    private CameraPose dragStartPose;

    public ThreeDViewport(Consumer<SelectionKey> selectionConsumer) {
        this.selectionConsumer = selectionConsumer;
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
        Set<String> visibleLevels = levelVisibility.entrySet().stream()
                .filter(entry -> entry.getValue().get())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (visibleLevels.isEmpty() && !project.levels().isEmpty()) {
            visibleLevels = Set.of(project.primaryLevel().name());
        }
        ThreeDSceneModel sceneModel = modelBuilder.build(project, visibleLevels, surfaceLayersCheckBox.isSelected());
        rebuildScene(sceneModel);
    }

    public void setSelectedSelection(SelectionKey selectedSelection) {
        this.selectedSelection = selectedSelection;
        if (currentProject != null) {
            refresh(currentProject);
        }
    }

    private void configureScene() {
        modelRoot.setScaleX(WORLD_SCALE);
        modelRoot.setScaleY(-WORLD_SCALE);
        modelRoot.setScaleZ(WORLD_SCALE);
        worldRoot.getChildren().add(modelRoot);
        worldRoot.getChildren().add(new AmbientLight(Color.color(0.75, 0.75, 0.75)));
        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(-6_000);
        pointLight.setTranslateY(-8_000);
        pointLight.setTranslateZ(-10_000);
        worldRoot.getChildren().add(pointLight);
        subScene.setFill(Color.web("#f2eadf"));
        subScene.setFocusTraversable(true);
        subScene.setOnMousePressed(event -> {
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
            dragStartPose = cameraPose;
        });
        subScene.setOnMouseDragged(event -> {
            if (dragStartPose == null) {
                return;
            }
            double deltaX = event.getSceneX() - dragStartX;
            double deltaY = event.getSceneY() - dragStartY;
            if (event.getButton() == MouseButton.PRIMARY) {
                cameraPose = cameraController.orbit(dragStartPose, deltaX * 0.35, -deltaY * 0.35);
            } else if (event.getButton() == MouseButton.SECONDARY) {
                cameraPose = cameraController.pan(dragStartPose, deltaX * 22.0, -deltaY * 22.0);
            }
            updateCamera();
        });
        subScene.setOnScroll(event -> {
            cameraPose = cameraController.zoom(cameraPose, event.getDeltaY() > 0 ? 0.92 : 1.08);
            updateCamera();
        });
        subScene.widthProperty().addListener((ignored, oldValue, newValue) -> updateCamera());
        subScene.heightProperty().addListener((ignored, oldValue, newValue) -> updateCamera());
    }

    private void configureControls() {
        projectionModeSelector.getItems().addAll(ProjectionMode.values());
        projectionModeSelector.setValue(cameraPose.projectionMode());
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
        applyTooltip(projectionModeSelector, "Schaltet die 3D-Kamera zwischen orthografischer und perspektivischer Projektion um.");
        applyTooltip(surfaceLayersCheckBox, "Blendet zusätzliche Flächen-Ebenen als gestapelte 3D-Schichten ein oder aus.");
        applyTooltip(cameraStatusLabel, "Zeigt den aktuellen Zustand der 3D-Kamera mit Projektion, Winkel und Abstand an.");
        levelTogglePane.setPadding(new Insets(6, 0, 0, 0));
    }

    private void configureLayout() {
        Label title = new Label("3D-Ansicht");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        HBox header = new HBox(12.0, title, new Label("Projektion"), projectionModeSelector, surfaceLayersCheckBox);
        header.setAlignment(Pos.CENTER_LEFT);

        ScrollPane levelScrollPane = new ScrollPane(levelTogglePane);
        levelScrollPane.setFitToWidth(true);
        levelScrollPane.setPrefViewportHeight(88);
        levelScrollPane.setStyle("-fx-background-color: transparent;");

        StackPane scenePane = new StackPane(subScene);
        scenePane.setStyle("-fx-background-color: rgba(255,255,255,0.6); -fx-background-radius: 16;");
        subScene.widthProperty().bind(scenePane.widthProperty());
        subScene.heightProperty().bind(scenePane.heightProperty());

        BorderPane.setMargin(scenePane, new Insets(10, 0, 10, 0));
        setTop(new BorderPane(header, null, null, levelScrollPane, null));
        setCenter(scenePane);
        setBottom(cameraStatusLabel);
        setPadding(new Insets(0, 0, 0, 12));
        setPrefWidth(560);
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
        modelRoot.getChildren().clear();
        Map<String, Group> groupedByLevel = new LinkedHashMap<>();
        for (RenderableBox renderableBox : sceneModel.boxes()) {
            Group levelGroup = groupedByLevel.computeIfAbsent(renderableBox.levelName(), ignored -> {
                Group group = new Group();
                group.setCache(true);
                group.setCacheHint(CacheHint.SPEED);
                return group;
            });
            levelGroup.getChildren().add(createNode(renderableBox));
        }
        modelRoot.getChildren().addAll(groupedByLevel.values());
    }

    private Node createNode(RenderableBox renderableBox) {
        Box box = new Box(
                Math.max(1.0, renderableBox.width()),
                Math.max(1.0, renderableBox.height()),
                Math.max(1.0, renderableBox.depth())
        );
        box.setTranslateX(renderableBox.centerX());
        box.setTranslateY(renderableBox.centerY());
        box.setTranslateZ(renderableBox.centerZ());
        box.setMaterial(material(renderableBox));
        box.setOpacity(renderableBox.opacity());
        box.setDrawMode(DrawMode.FILL);
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
            case Y -> new Rotate(-degrees, Rotate.Y_AXIS);
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
        if (selectedSelection != null && selectedSelection.equals(renderableBox.selectionKey())) {
            color = color.deriveColor(0, 1.0, 1.2, 1.0);
        }
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecularColor(color.brighter());
        return material;
    }

    private void updateCamera() {
        projectionModeSelector.setValue(cameraPose.projectionMode());
        if (cameraPose.projectionMode() == ProjectionMode.PERSPECTIVE) {
            perspectiveCamera.setNearClip(0.1);
            perspectiveCamera.setFarClip(1_000_000);
            perspectiveCamera.setFieldOfView(35);
            perspectiveCamera.getTransforms().setAll(cameraTransforms());
            subScene.setCamera(perspectiveCamera);
        } else {
            parallelCamera.setNearClip(0.1);
            parallelCamera.setFarClip(1_000_000);
            parallelCamera.getTransforms().setAll(cameraTransforms());
            subScene.setCamera(parallelCamera);
        }
        modelRoot.setTranslateX(cameraPose.panX());
        modelRoot.setTranslateZ(cameraPose.panZ());
        cameraStatusLabel.setText(String.format(
                "3D Kamera: %s | Azimut %.1f° | Elevation %.1f° | Abstand %.1f m",
                cameraPose.projectionMode() == ProjectionMode.ORTHOGRAPHIC ? "orthografisch" : "perspektivisch",
                cameraPose.azimuthDegrees(),
                cameraPose.elevationDegrees(),
                cameraPose.distance() / 1000.0
        ));
    }

    private javafx.scene.transform.Transform[] cameraTransforms() {
        return new javafx.scene.transform.Transform[]{
                new Rotate(-cameraPose.elevationDegrees(), Rotate.X_AXIS),
                new Rotate(-cameraPose.azimuthDegrees(), Rotate.Y_AXIS),
                new Translate(0, -2_500 * WORLD_SCALE, -cameraPose.distance() * WORLD_SCALE)
        };
    }

    private void applyTooltip(Node node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(320);
        Tooltip.install(node, tooltip);
    }
}
