package de.schrell.cadas.application.view;

import java.util.List;

/** Unveränderliche, JavaFX-unabhängige 3D-Szene aus einfachen Quadern und allgemeinen Dreiecksmaschen. */
public record ThreeDSceneModel(List<RenderableBox> boxes, List<RenderableMesh> meshes) {

    public ThreeDSceneModel(List<RenderableBox> boxes) {
        this(boxes, List.of());
    }
}
