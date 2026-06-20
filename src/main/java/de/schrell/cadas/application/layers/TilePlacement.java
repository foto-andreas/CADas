package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.geometry.Length;

public record TilePlacement(
        int column,
        int row,
        Length xOffset,
        Length yOffset,
        Length width,
        Length height
) {
}
