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
import javafx.scene.control.Button;
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
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public final class ThreeDViewport extends BorderPane {

    private static final double WORLD_SCALE = 0.08;

    private final ThreeDViewPreparation viewPreparation = new ThreeDViewPreparation();
    private final ThreeDSceneModelBuilder modelBuilder = new ThreeDSceneModelBuilder();
    private final ThreeDCameraController cameraController = new ThreeDCameraController();
    private final PerspectiveCamera perspectiveCamera = new PerspectiveCamera(false);
    private final ParallelCamera parallelCamera = new ParallelCamera();
    private final Group worldRoot = new Group();
    private final Group modelRoot = new Group();
    private final SubScene subScene = new SubScene(worldRoot, 520, 720, true, SceneAntialiasing.BALANCED);
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
    private double sceneCenterY;
    private double sceneCenterZ;
    private double sceneSpan;
    private double baseTranslateX;
    private double baseTranslateY;
    private double baseTranslateZ;
    private double dragStartX;
    private double dragStartY;
    private CameraPose dragStartPose;
    private MouseButton dragButton = MouseButton.NONE;
    private boolean sceneWasEmpty = true;
    private int lastRenderedBodyCount;

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
        subScene.widthProperty().addListener((ignored, oldValue, newValue) -> updateCamera());
        subScene.heightProperty().addListener((ignored, oldValue, newValue) -> updateCamera());
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
        surfaceRenderingCheckBox.setSelected(false);
        surfaceRenderingCheckBox.selectedProperty().addListener((ignored, oldValue, newValue) -> {
            if (currentProject != null) {
                refresh(currentProject);
            }
        });
        applyTooltip(projectionModeSelector, "Schaltet die 3D-Kamera zwischen orthografischer und perspektivischer Projektion um.");
        applyTooltip(surfaceLayersCheckBox, "Blendet zusätzliche Flächen-Ebenen als gestapelte 3D-Schichten ein oder aus.");
        applyTooltip(surfaceRenderingCheckBox, "Schaltet von der transparenten Modellansicht auf eine flächenbetonte Oberflächenansicht um, in der die Öffnungen freien Blick in den Innenraum lassen.");
        applyTooltip(cameraStatusLabel, "Zeigt den aktuellen Zustand der 3D-Kamera mit Projektion, Winkel und Abstand an.");
        applyTooltip(sceneStatsLabel, "Zeigt an, wie viele 3D-Körper aktuell aus dem Fachmodell abgeleitet wurden.");
        levelTogglePane.setPadding(new Insets(6, 0, 0, 0));
    }

    private void configureLayout() {
        Label title = new Label("3D-Ansicht");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label projectionLabel = new Label("Projektion");
        Label interactionHintLabel = new Label("Links drehen, rechts verschieben, Mausrad zoomen");
        interactionHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #685d52;");
        Button isometricViewButton = new Button("Iso");
        isometricViewButton.setOnAction(event -> resetToCurrentOrientation());
        Button topViewButton = new Button("Oben");
        topViewButton.setOnAction(event -> applyViewOrientation(ViewOrientation.TOP));
        Button frontViewButton = new Button("Vorne");
        frontViewButton.setOnAction(event -> applyViewOrientation(ViewOrientation.NORTH));
        Button rightViewButton = new Button("Rechts");
        rightViewButton.setOnAction(event -> applyViewOrientation(ViewOrientation.EAST));
        Button orbitLeftButton = new Button("⟲");
        orbitLeftButton.setOnAction(event -> nudgeOrbit(-15.0, 0.0));
        Button orbitRightButton = new Button("⟳");
        orbitRightButton.setOnAction(event -> nudgeOrbit(15.0, 0.0));
        Button orbitUpButton = new Button("↑");
        orbitUpButton.setOnAction(event -> nudgeOrbit(0.0, 8.0));
        Button orbitDownButton = new Button("↓");
        orbitDownButton.setOnAction(event -> nudgeOrbit(0.0, -8.0));
        Button fitSceneButton = new Button("Modell einpassen");
        fitSceneButton.setOnAction(event -> fitCameraToScene());
        Button resetViewButton = new Button("Ansicht zentrieren");
        resetViewButton.setOnAction(event -> resetToCurrentOrientation());
        resetViewButton.setStyle("-fx-background-color: #4b6a88; -fx-text-fill: white; -fx-background-radius: 999;");
        applyTooltip(resetViewButton, "Setzt die 3D-Kamera auf die Standardansicht zurück und richtet das Modell wieder übersichtlich aus.");
        applyTooltip(isometricViewButton, "Wechselt auf eine räumliche Standardperspektive, in der Höhe, Tiefe und Breite gleichzeitig sichtbar sind.");
        applyTooltip(topViewButton, "Wechselt die 3D-Kamera auf eine Draufsicht.");
        applyTooltip(frontViewButton, "Wechselt die 3D-Kamera auf eine Vorderansicht.");
        applyTooltip(rightViewButton, "Wechselt die 3D-Kamera auf eine rechte Seitenansicht.");
        applyTooltip(orbitLeftButton, "Dreht die 3D-Kamera schrittweise nach links.");
        applyTooltip(orbitRightButton, "Dreht die 3D-Kamera schrittweise nach rechts.");
        applyTooltip(orbitUpButton, "Hebt die 3D-Kamera schrittweise an.");
        applyTooltip(orbitDownButton, "Senkt die 3D-Kamera schrittweise ab.");
        applyTooltip(fitSceneButton, "Richtet Kameraabstand und Mittelpunkt so aus, dass das aktuelle Modell vollständig sichtbar wird.");
        applyTooltip(interactionHintLabel, "Erinnert an die direkte Maussteuerung der 3D-Ansicht: linke Maustaste dreht, rechte verschiebt und das Mausrad zoomt.");
        HBox titleRow = new HBox(10.0, title, interactionHintLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setFillHeight(true);

        FlowPane controlRow = new FlowPane(10.0, 10.0,
                projectionLabel,
                projectionModeSelector,
                isometricViewButton,
                topViewButton,
                frontViewButton,
                rightViewButton,
                orbitLeftButton,
                orbitRightButton,
                orbitUpButton,
                orbitDownButton,
                fitSceneButton,
                surfaceLayersCheckBox,
                surfaceRenderingCheckBox,
                resetViewButton
        );
        controlRow.setAlignment(Pos.CENTER_LEFT);
        controlRow.setPrefWrapLength(500);

        VBox header = new VBox(8.0, titleRow, controlRow);
        header.setAlignment(Pos.CENTER_LEFT);

        ScrollPane levelScrollPane = new ScrollPane(levelTogglePane);
        levelScrollPane.setFitToWidth(true);
        levelScrollPane.setPrefViewportHeight(72);
        levelScrollPane.setStyle("-fx-background-color: transparent;");

        sceneHintLabel.setWrapText(true);
        sceneHintLabel.setMaxWidth(280);
        sceneHintLabel.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-padding: 12; -fx-background-radius: 12; -fx-text-fill: #5f5548;");
        StackPane scenePane = new StackPane(subScene, sceneHintLabel);
        scenePane.setStyle("-fx-background-color: rgba(255,255,255,0.6); -fx-background-radius: 16;");
        subScene.widthProperty().bind(scenePane.widthProperty());
        subScene.heightProperty().bind(scenePane.heightProperty());

        BorderPane.setMargin(scenePane, new Insets(10, 0, 10, 0));
        VBox topArea = new VBox(10.0, header, levelScrollPane);
        setTop(topArea);
        setCenter(scenePane);
        VBox footer = new VBox(4.0, sceneStatsLabel, cameraStatusLabel);
        setBottom(footer);
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
            minX = Math.min(minX, renderableBox.centerX() - renderableBox.width() / 2.0);
            maxX = Math.max(maxX, renderableBox.centerX() + renderableBox.width() / 2.0);
            minY = Math.min(minY, renderableBox.centerY() - renderableBox.height() / 2.0);
            maxY = Math.max(maxY, renderableBox.centerY() + renderableBox.height() / 2.0);
            minZ = Math.min(minZ, renderableBox.centerZ() - renderableBox.depth() / 2.0);
            maxZ = Math.max(maxZ, renderableBox.centerZ() + renderableBox.depth() / 2.0);
        }
        modelRoot.getChildren().addAll(groupedByLevel.values());
        sceneStatsLabel.setText("3D-Szene: " + sceneModel.boxes().size() + " Körper");
        if (sceneModel.boxes().isEmpty()) {
            sceneCenterX = 0.0;
            sceneCenterY = 0.0;
            sceneCenterZ = 0.0;
            sceneSpan = 8_000.0;
            baseTranslateX = 0.0;
            baseTranslateY = 0.0;
            baseTranslateZ = 0.0;
        } else {
            sceneCenterX = (minX + maxX) / 2.0;
            sceneCenterY = (minY + maxY) / 2.0;
            sceneCenterZ = (minZ + maxZ) / 2.0;
            sceneSpan = Math.max(4_000.0, Math.max(maxY - minY, Math.max(maxX - minX, maxZ - minZ)));
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
        if ((selectedSelection != null && selectedSelection.equals(renderableBox.selectionKey()))
                || selectedSelections.contains(renderableBox.selectionKey())) {
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
        modelRoot.setTranslateX(baseTranslateX + cameraPose.panX() * WORLD_SCALE);
        modelRoot.setTranslateY(baseTranslateY + cameraPose.panY() * WORLD_SCALE);
        modelRoot.setTranslateZ(baseTranslateZ + cameraPose.panZ() * WORLD_SCALE);
        cameraStatusLabel.setText(String.format(
                "3D Kamera: %s | Azimut %.1f° | Elevation %.1f° | Abstand %.1f m | Szene %.0f mm",
                cameraPose.projectionMode() == ProjectionMode.ORTHOGRAPHIC ? "orthografisch" : "perspektivisch",
                cameraPose.azimuthDegrees(),
                cameraPose.elevationDegrees(),
                cameraPose.distance() / 1000.0,
                sceneSpan
        ));
    }

    private void nudgeOrbit(double azimuthDelta, double elevationDelta) {
        fitToSceneRequested = false;
        cameraPose = cameraController.orbit(cameraPose, azimuthDelta, elevationDelta);
        updateCamera();
    }

    private void fitCameraToScene() {
        baseTranslateX = -sceneCenterX * WORLD_SCALE;
        baseTranslateY = sceneCenterY * WORLD_SCALE;
        baseTranslateZ = -sceneCenterZ * WORLD_SCALE;
        cameraPose = new CameraPose(
                projectionModeSelector.getValue(),
                cameraPose.azimuthDegrees(),
                cameraPose.elevationDegrees(),
                Math.max(6_000.0, sceneSpan * 1.8),
                0.0,
                0.0,
                0.0
        );
        fitToSceneRequested = false;
        updateCamera();
    }

    private javafx.scene.transform.Transform[] cameraTransforms() {
        return new javafx.scene.transform.Transform[]{
                new Translate(0, 0, -cameraPose.distance() * WORLD_SCALE),
                new Rotate(-cameraPose.elevationDegrees(), Rotate.X_AXIS),
                new Rotate(-cameraPose.azimuthDegrees(), Rotate.Y_AXIS)
        };
    }

    private void applyTooltip(Node node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(320);
        Tooltip.install(node, tooltip);
    }

}
