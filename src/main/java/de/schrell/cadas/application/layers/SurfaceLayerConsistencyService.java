package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;

import java.util.List;

public final class SurfaceLayerConsistencyService {

    public boolean haveEqualSequence(SurfaceLayerStack first, SurfaceLayerStack second) {
        return signature(first.layers()).equals(signature(second.layers()));
    }

    public String signature(List<SurfaceLayer> layers) {
        return layers.stream()
                .map(layer -> layer.name()
                        + ":" + layer.thickness().toMillimeters()
                        + ":" + layer.tileWidth().toMillimeters()
                        + ":" + layer.tileHeight().toMillimeters()
                        + ":" + layer.layoutMode()
                        + ":" + layer.layoutOffset().toMillimeters()
                        + ":" + layer.minimumOffset().toMillimeters()
                        + ":" + layer.minimumEdgeWidth().toMillimeters()
                        + ":" + layer.minimumStartEndMargin().toMillimeters()
                        + ":" + layer.layoutRotatedQuarterTurn()
                        + ":" + layer.coveringSource())
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
    }
}
