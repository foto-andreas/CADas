package de.andreas.cadas.application.parts;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.model.StairType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PartLibraryImportService {

    public StandardPartLibrary importLibrary(Path sourceFile) throws IOException {
        List<DoorPreset> doorPresets = new ArrayList<>();
        List<WindowPreset> windowPresets = new ArrayList<>();
        List<StairPreset> stairPresets = new ArrayList<>();

        for (String line : Files.readAllLines(sourceFile)) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }
            String[] parts = trimmed.split(";");
            switch (parts[0]) {
                case "DOOR" -> doorPresets.add(new DoorPreset(
                        parts[1],
                        parts[2],
                        Length.of(Double.parseDouble(parts[3]), LengthUnit.MILLIMETER),
                        Length.of(Double.parseDouble(parts[4]), LengthUnit.MILLIMETER),
                        Length.of(Double.parseDouble(parts[5]), LengthUnit.MILLIMETER)
                ));
                case "WINDOW" -> windowPresets.add(new WindowPreset(
                        parts[1],
                        parts[2],
                        Length.of(Double.parseDouble(parts[3]), LengthUnit.MILLIMETER),
                        Length.of(Double.parseDouble(parts[4]), LengthUnit.MILLIMETER),
                        Length.of(Double.parseDouble(parts[5]), LengthUnit.MILLIMETER)
                ));
                case "STAIR" -> stairPresets.add(new StairPreset(
                        parts[1],
                        parts[2],
                        StairType.valueOf(parts[3]),
                        Length.of(Double.parseDouble(parts[4]), LengthUnit.MILLIMETER),
                        Integer.parseInt(parts[5])
                ));
                default -> throw new IllegalArgumentException("Unbekannter Bibliothekseintrag: " + parts[0]);
            }
        }

        return new StandardPartLibrary(List.copyOf(doorPresets), List.copyOf(windowPresets), List.copyOf(stairPresets));
    }
}
