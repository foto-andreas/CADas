package de.schrell.cadas.application.view;

import java.util.List;

public record ThreeDSceneModel(List<RenderableBox> boxes, List<RenderableMesh> meshes) {

    public ThreeDSceneModel(List<RenderableBox> boxes) {
        this(boxes, List.of());
    }
}
