package de.andreas.cadas.application.layers;

import de.andreas.cadas.domain.geometry.Length;

public record TilePlacement(
        int column,
        int row,
        Length xOffset,
        Length yOffset,
        Length width,
        Length height
) {
}

