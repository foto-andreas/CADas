package de.schrell.cadas.application.parts;

import java.util.List;

public record StandardPartLibrary(
        List<DoorPreset> doorPresets,
        List<WindowPreset> windowPresets,
        List<StairPreset> stairPresets
) {
}
