package de.andreas.cadas.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class SurfaceLayerStack {

    private final UUID id;
    private final SurfaceType surfaceType;
    private final String targetKey;
    private final List<SurfaceLayer> layers = new ArrayList<>();

    public SurfaceLayerStack(SurfaceType surfaceType, String targetKey) {
        this(UUID.randomUUID(), surfaceType, targetKey);
    }

    public SurfaceLayerStack(UUID id, SurfaceType surfaceType, String targetKey) {
        this.id = Objects.requireNonNull(id, "id darf nicht null sein.");
        this.surfaceType = Objects.requireNonNull(surfaceType, "surfaceType darf nicht null sein.");
        this.targetKey = Objects.requireNonNull(targetKey, "targetKey darf nicht null sein.");
    }

    public UUID id() {
        return id;
    }

    public SurfaceType surfaceType() {
        return surfaceType;
    }

    public String targetKey() {
        return targetKey;
    }

    public List<SurfaceLayer> layers() {
        return List.copyOf(layers);
    }

    public void addLayer(SurfaceLayer layer) {
        layers.add(Objects.requireNonNull(layer, "layer darf nicht null sein."));
    }

    public void removeLayer(UUID layerId) {
        layers.removeIf(layer -> layer.id().equals(layerId));
    }

    public void renameLayer(UUID layerId, String newName) {
        replaceLayer(layerId, layer -> layer.rename(newName));
    }

    public void setVisibility(UUID layerId, boolean visible) {
        replaceLayer(layerId, layer -> layer.withVisibility(visible));
    }

    public void replaceLayer(UUID layerId, SurfaceLayer replacement) {
        replaceLayer(layerId, ignored -> replacement);
    }

    public void moveLayer(UUID layerId, int newIndex) {
        SurfaceLayer layer = layers.stream()
                .filter(candidate -> candidate.id().equals(layerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ebene nicht gefunden: " + layerId));
        layers.remove(layer);
        int boundedIndex = Math.max(0, Math.min(newIndex, layers.size()));
        layers.add(boundedIndex, layer);
    }

    private void replaceLayer(UUID layerId, java.util.function.Function<SurfaceLayer, SurfaceLayer> replacer) {
        for (int index = 0; index < layers.size(); index++) {
            SurfaceLayer layer = layers.get(index);
            if (layer.id().equals(layerId)) {
                layers.set(index, replacer.apply(layer));
                return;
            }
        }
        throw new IllegalArgumentException("Ebene nicht gefunden: " + layerId);
    }

    public SurfaceLayerStack copy() {
        SurfaceLayerStack copy = new SurfaceLayerStack(id, surfaceType, targetKey);
        copy.layers.addAll(layers);
        return copy;
    }
}
