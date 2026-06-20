package de.schrell.cadas.application.dwg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DwgLibraryAnalyzer {

    private final DwgToDxfConverter converter;
    private final DwgDxfGeometryReader geometryReader = new DwgDxfGeometryReader();

    public DwgLibraryAnalyzer() {
        this(new ExternalDwgToDxfConverter());
    }

    public DwgLibraryAnalyzer(DwgToDxfConverter converter) {
        this.converter = Objects.requireNonNull(converter, "converter darf nicht null sein.");
    }

    public DwgConversionAvailability availability() {
        return converter.availability();
    }

    public DwgLibraryAnalysis analyze(Path dwgFile) {
        DwgConversionAvailability availability = converter.availability();
        if (!availability.available()) {
            return DwgLibraryAnalysis.unavailable(dwgFile, availability.message());
        }
        Path temporaryDirectory = null;
        try {
            temporaryDirectory = Files.createTempDirectory("cadas-dwg-");
            Path dxfFile = temporaryDirectory.resolve(stripExtension(dwgFile.getFileName().toString()) + ".dxf");
            DwgConversionResult conversion = converter.convert(dwgFile, dxfFile);
            return geometryReader.read(conversion.dxfFile(), dwgFile, conversion.converterName(), conversion.messages());
        } catch (IOException exception) {
            return DwgLibraryAnalysis.unavailable(dwgFile, "DWG-Analyse fehlgeschlagen: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return DwgLibraryAnalysis.unavailable(dwgFile, "DWG-Analyse wurde unterbrochen.");
        } finally {
            deleteTemporaryDirectory(temporaryDirectory);
        }
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex <= 0 ? fileName : fileName.substring(0, dotIndex);
    }

    private void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        List<Path> paths = new ArrayList<>();
        try (var stream = Files.walk(directory)) {
            stream.forEach(paths::add);
        }
        for (int index = paths.size() - 1; index >= 0; index--) {
            Files.deleteIfExists(paths.get(index));
        }
    }

    private void deleteTemporaryDirectory(Path temporaryDirectory) {
        if (temporaryDirectory == null) {
            return;
        }
        try {
            deleteRecursively(temporaryDirectory);
        } catch (IOException ignored) {
            // Temporäre Konverterdateien dürfen die fachliche Diagnose nicht überdecken.
        }
    }
}
