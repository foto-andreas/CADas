package de.schrell.cadas.application.parts;

import java.util.List;

/** Unveränderlicher Katalog der aktuell verfügbaren Tür-, Fenster- und Treppen-Presets. */
public record StandardPartLibrary(
        List<DoorPreset> doorPresets,
        List<WindowPreset> windowPresets,
        List<StairPreset> stairPresets
) {
}
