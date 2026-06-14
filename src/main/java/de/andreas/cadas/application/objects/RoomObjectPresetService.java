package de.andreas.cadas.application.objects;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.model.RoomObjectShape;
import de.andreas.cadas.domain.model.RoomObjectType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public final class RoomObjectPresetService {

    private final Path objectDirectory;

    public RoomObjectPresetService() {
        this(defaultObjectDirectory());
    }

    public RoomObjectPresetService(Path objectDirectory) {
        this.objectDirectory = Objects.requireNonNull(objectDirectory, "objectDirectory darf nicht null sein.");
    }

    public List<RoomObjectPreset> presets() {
        return Stream.concat(defaults().stream(), loadDwgPresets().stream()).toList();
    }

    public List<RoomObjectPreset> defaults() {
        return List.of(
                preset("shower-rectangle", "Dusche Rechteck 90 x 90", RoomObjectType.SHOWER, RoomObjectShape.RECTANGLE, 90, 90, 200, false),
                preset("shower-half-round", "Dusche halbrund 90 x 90", RoomObjectType.SHOWER, RoomObjectShape.HALF_ROUND, 90, 90, 200, false),
                preset("shower-quarter-circle", "Dusche Viertelkreis 90 x 90", RoomObjectType.SHOWER, RoomObjectShape.QUARTER_CIRCLE, 90, 90, 200, false),
                preset("toilet", "Toilette 40 x 70", RoomObjectType.TOILET, RoomObjectShape.RECTANGLE, 40, 70, 80, false),
                preset("washbasin", "Waschbecken 60 x 50", RoomObjectType.WASHBASIN, RoomObjectShape.RECTANGLE, 60, 50, 85, false),
                preset("wall-cabinet", "Wandschrank 80 x 35", RoomObjectType.WALL_CABINET, RoomObjectShape.RECTANGLE, 80, 35, 200, true),
                preset("cabinet", "Schrank 80 x 60", RoomObjectType.CABINET, RoomObjectShape.RECTANGLE, 80, 60, 200, false),
                preset("table-rectangle", "Tisch Rechteck 160 x 90", RoomObjectType.TABLE, RoomObjectShape.RECTANGLE, 160, 90, 75, false),
                preset("table-oval", "Tisch oval 160 x 90", RoomObjectType.TABLE, RoomObjectShape.OVAL, 160, 90, 75, false),
                preset("table-round", "Tisch rund 110", RoomObjectType.TABLE, RoomObjectShape.CIRCLE, 110, 110, 75, false)
        );
    }

    public List<RoomObjectPreset> loadDwgPresets() {
        if (!Files.isDirectory(objectDirectory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(objectDirectory)) {
            return files
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dwg"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(this::dwgPreset)
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private RoomObjectPreset preset(String id, String name, RoomObjectType type, RoomObjectShape shape, double widthCentimeters, double depthCentimeters, double heightCentimeters, boolean cutsFloorCovering) {
        return new RoomObjectPreset(
                id,
                name,
                type,
                shape,
                Length.of(widthCentimeters, LengthUnit.CENTIMETER),
                Length.of(depthCentimeters, LengthUnit.CENTIMETER),
                Length.of(heightCentimeters, LengthUnit.CENTIMETER),
                cutsFloorCovering,
                ""
        );
    }

    private RoomObjectPreset dwgPreset(Path path) {
        String fileName = path.getFileName().toString();
        String baseName = fileName.substring(0, fileName.length() - 4);
        String normalizedId = baseName.toLowerCase(Locale.GERMAN).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "-").replaceAll("^-+|-+$", "");
        return new RoomObjectPreset(
                "dwg-" + (normalizedId.isBlank() ? "objekt" : normalizedId),
                "DWG-Objekt: " + baseName,
                RoomObjectType.DWG_REFERENCE,
                RoomObjectShape.RECTANGLE,
                Length.of(100, LengthUnit.CENTIMETER),
                Length.of(100, LengthUnit.CENTIMETER),
                Length.of(100, LengthUnit.CENTIMETER),
                false,
                path.toAbsolutePath().normalize().toString()
        );
    }

    private static Path defaultObjectDirectory() {
        return Path.of(System.getProperty("user.home"), ".config", "CADas", "Objekte");
    }
}
