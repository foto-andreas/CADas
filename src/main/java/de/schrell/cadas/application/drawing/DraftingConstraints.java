package de.schrell.cadas.application.drawing;

import de.schrell.cadas.domain.geometry.Angle;
import de.schrell.cadas.domain.geometry.Grid;
import de.schrell.cadas.domain.geometry.Length;

import java.util.Optional;

/**
 * Bündelt die optionalen Längen-, Winkel- und Orthogonalvorgaben eines einzelnen Zeichenschritts.
 */
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
