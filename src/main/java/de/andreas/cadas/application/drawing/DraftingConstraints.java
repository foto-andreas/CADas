package de.andreas.cadas.application.drawing;

import de.andreas.cadas.domain.geometry.Angle;
import de.andreas.cadas.domain.geometry.Grid;
import de.andreas.cadas.domain.geometry.Length;
import java.util.Optional;

public record DraftingConstraints(
        boolean orthogonalMode,
        boolean snapToGrid,
        boolean snapToEndpoints,
        Grid grid,
        Length wallThickness,
        Length snapTolerance,
        Optional<Length> manualLength,
        Optional<Angle> manualAngle
) {
}

