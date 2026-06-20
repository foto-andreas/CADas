package de.schrell.cadas.application.objects;

import de.schrell.cadas.application.dwg.DwgBlockDefinition;
import de.schrell.cadas.application.dwg.DwgLibraryAnalyzer;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;

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
    private final DwgLibraryAnalyzer dwgLibraryAnalyzer;

    public RoomObjectPresetService() {
        this(defaultObjectDirectory());
    }

    public RoomObjectPresetService(Path objectDirectory) {
        this(objectDirectory, new DwgLibraryAnalyzer());
    }

    public RoomObjectPresetService(Path objectDirectory, DwgLibraryAnalyzer dwgLibraryAnalyzer) {
        this.objectDirectory = Objects.requireNonNull(objectDirectory, "objectDirectory darf nicht null sein.");
        this.dwgLibraryAnalyzer = Objects.requireNonNull(dwgLibraryAnalyzer, "dwgLibraryAnalyzer darf nicht null sein.");
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
                    .flatMap(path -> dwgPresets(path).stream())
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    public RoomObjectPreset fromDwgBlock(DwgBlockDefinition block, boolean cutsFloorCovering) {
        return fromDwgBlock(block, RoomObjectMountingMode.fromCutsFloorCovering(cutsFloorCovering));
    }

    public RoomObjectPreset fromDwgBlock(DwgBlockDefinition block, RoomObjectMountingMode mountingMode) {
        return new RoomObjectPreset(
                "dwg-" + normalizedId(block.sourceFile().getFileName().toString()) + "-" + normalizedId(block.name()),
                "DWG-Objekt: " + block.name(),
                RoomObjectType.DWG_REFERENCE,
                RoomObjectShape.RECTANGLE,
                Length.ofMillimeters(Math.max(1.0, block.widthMillimeters())),
                Length.ofMillimeters(Math.max(1.0, block.heightMillimeters())),
                Length.of(100, LengthUnit.CENTIMETER),
                mountingMode,
                block.sourceReference()
        );
    }

    private List<RoomObjectPreset> dwgPresets(Path path) {
        var analysis = dwgLibraryAnalyzer.analyze(path);
        List<RoomObjectPreset> blockPresets = analysis.blocks().stream()
                .filter(DwgBlockDefinition::hasGeometry)
                .map(block -> fromDwgBlock(block, false))
                .toList();
        return blockPresets.isEmpty() ? List.of(dwgPreset(path)) : blockPresets;
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
        String normalizedId = normalizedId(baseName);
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

    private String normalizedId(String value) {
        return value.toLowerCase(Locale.GERMAN).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "-").replaceAll("^-+|-+$", "");
    }

    private static Path defaultObjectDirectory() {
        return Path.of(System.getProperty("user.home"), ".config", "CADas", "Objekte");
    }
}
