package de.schrell.cadas.application.layers;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

public final class UserSurfaceCoveringPresetLibrary {

    private static final String PRESET_EXTENSION = ".cadasbelag";

    private final Path libraryDirectory;

    public UserSurfaceCoveringPresetLibrary() {
        this(defaultLibraryDirectory());
    }

    public UserSurfaceCoveringPresetLibrary(Path libraryDirectory) {
        this.libraryDirectory = Objects.requireNonNull(libraryDirectory, "libraryDirectory darf nicht null sein.");
    }

    public Path libraryDirectory() {
        return libraryDirectory;
    }

    public List<SurfaceCoveringPreset> loadPresets() throws IOException {
        if (!Files.isDirectory(libraryDirectory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(libraryDirectory)) {
            return files
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(PRESET_EXTENSION))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(this::loadPresetUnchecked)
                    .sorted(Comparator.comparing(SurfaceCoveringPreset::name))
                    .toList();
        }
    }

    public List<Path> loadCadLibraries() throws IOException {
        if (!Files.isDirectory(libraryDirectory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(libraryDirectory)) {
            return files
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dwg"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    public boolean containsPresetName(String presetName) {
        return Files.exists(presetPath(presetName));
    }

    public SurfaceCoveringPreset savePreset(SurfaceCoveringPreset preset, boolean overwrite) throws IOException {
        Objects.requireNonNull(preset, "preset darf nicht null sein.");
        Files.createDirectories(libraryDirectory);
        Path target = presetPath(preset.name());
        if (Files.exists(target) && !overwrite) {
            throw new FileAlreadyExistsException(target.toString());
        }
        SurfaceCoveringPreset savedPreset = withSource(preset, target);
        Properties properties = new Properties();
        properties.setProperty("id", savedPreset.id());
        properties.setProperty("name", savedPreset.name());
        properties.setProperty("thicknessMm", Double.toString(savedPreset.thickness().toMillimeters()));
        properties.setProperty("tileWidthMm", Double.toString(savedPreset.tileWidth().toMillimeters()));
        properties.setProperty("tileHeightMm", Double.toString(savedPreset.tileHeight().toMillimeters()));
        properties.setProperty("layoutMode", savedPreset.layoutMode().name());
        properties.setProperty("offsetMm", Double.toString(savedPreset.offset().toMillimeters()));
        properties.setProperty("minimumOffsetMm", Double.toString(savedPreset.minimumOffset().toMillimeters()));
        properties.setProperty("minimumEdgeWidthMm", Double.toString(savedPreset.minimumEdgeWidth().toMillimeters()));
        properties.setProperty("minimumStartEndMarginMm", Double.toString(savedPreset.minimumStartEndMargin().toMillimeters()));
        properties.setProperty("jointWidthMm", Double.toString(savedPreset.jointWidth().toMillimeters()));
        properties.setProperty("cutRestriction", savedPreset.cutRestriction().name());
        properties.setProperty("coveringSource", savedPreset.coveringSource());
        properties.setProperty("originalCoveringSource", preset.coveringSource());
        try (OutputStream output = Files.newOutputStream(target)) {
            properties.store(output, "CADas-Belagspreset");
        }
        return savedPreset;
    }

    public Path copyCadLibrary(Path sourceFile, boolean overwrite) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile darf nicht null sein.");
        Files.createDirectories(libraryDirectory);
        Path source = sourceFile.toAbsolutePath().normalize();
        Path target = libraryDirectory.resolve(source.getFileName()).toAbsolutePath().normalize();
        if (Files.exists(target) && Files.isSameFile(source, target)) {
            return target;
        }
        if (!Files.exists(target) || overwrite) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
        copySidecarCatalogs(source, target, overwrite);
        return target;
    }

    private SurfaceCoveringPreset loadPresetUnchecked(Path path) {
        try {
            return loadPreset(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Belagspreset konnte nicht gelesen werden: " + path, exception);
        }
    }

    private SurfaceCoveringPreset loadPreset(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        String fallbackName = path.getFileName().toString().replace(PRESET_EXTENSION, "");
        return new SurfaceCoveringPreset(
                properties.getProperty("id", "user-" + sanitizedFileStem(fallbackName)),
                properties.getProperty("name", fallbackName),
                length(properties, "thicknessMm"),
                length(properties, "tileWidthMm"),
                length(properties, "tileHeightMm"),
                SurfaceLayoutMode.valueOf(properties.getProperty("layoutMode", SurfaceLayoutMode.AUTOMATIC.name())),
                length(properties, "offsetMm"),
                length(properties, "minimumOffsetMm"),
                length(properties, "minimumEdgeWidthMm"),
                length(properties, "minimumStartEndMarginMm"),
                length(properties, "jointWidthMm"),
                SurfaceCutRestriction.fromStoredValue(properties.getProperty("cutRestriction")),
                path.toAbsolutePath().normalize().toString()
        );
    }

    private Length length(Properties properties, String key) {
        String rawValue = properties.getProperty(key, "0").trim().replace(',', '.');
        return Length.ofMillimeters(Double.parseDouble(rawValue));
    }

    private SurfaceCoveringPreset withSource(SurfaceCoveringPreset preset, Path target) {
        String id = preset.id().isBlank() ? "user-" + sanitizedFileStem(preset.name()) : preset.id();
        return new SurfaceCoveringPreset(
                id,
                preset.name(),
                preset.thickness(),
                preset.tileWidth(),
                preset.tileHeight(),
                preset.layoutMode(),
                preset.offset(),
                preset.minimumOffset(),
                preset.minimumEdgeWidth(),
                preset.minimumStartEndMargin(),
                preset.jointWidth(),
                preset.cutRestriction(),
                target.toAbsolutePath().normalize().toString()
        );
    }

    private Path presetPath(String presetName) {
        return libraryDirectory.resolve(sanitizedFileStem(presetName) + PRESET_EXTENSION);
    }

    private String sanitizedFileStem(String name) {
        String normalized = name == null ? "" : name.trim().toLowerCase(Locale.GERMAN);
        StringBuilder builder = new StringBuilder();
        boolean previousWasSeparator = false;
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
                previousWasSeparator = false;
            } else if (!previousWasSeparator) {
                builder.append('-');
                previousWasSeparator = true;
            }
        }
        String result = builder.toString().replaceAll("^-+|-+$", "");
        return result.isBlank() ? "belag" : result;
    }

    private void copySidecarCatalogs(Path source, Path target, boolean overwrite) throws IOException {
        String fileName = source.getFileName().toString();
        copyCatalogIfPresent(source.resolveSibling(fileName + ".blocks"), target.resolveSibling(fileName + ".blocks"), overwrite);
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".dwg")) {
            String baseName = fileName.substring(0, fileName.length() - 4);
            copyCatalogIfPresent(source.resolveSibling(baseName + ".blocks"), target.resolveSibling(baseName + ".blocks"), overwrite);
        }
    }

    private void copyCatalogIfPresent(Path sourceCatalog, Path targetCatalog, boolean overwrite) throws IOException {
        if (!Files.isRegularFile(sourceCatalog)) {
            return;
        }
        if (!Files.exists(targetCatalog) || overwrite) {
            Files.copy(sourceCatalog, targetCatalog, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path defaultLibraryDirectory() {
        return Path.of(System.getProperty("user.home"), ".config", "CADas", "Belag");
    }
}
