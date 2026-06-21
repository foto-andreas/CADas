package de.schrell.cadas.application.dwg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class Rfa3dObjectGeometryReader {

    private final Dxf3dObjectGeometryReader dxfReader;
    private final Ifc3dObjectGeometryReader ifcReader;

    public Rfa3dObjectGeometryReader() {
        this(new Dxf3dObjectGeometryReader(), new Ifc3dObjectGeometryReader());
    }

    Rfa3dObjectGeometryReader(Dxf3dObjectGeometryReader dxfReader, Ifc3dObjectGeometryReader ifcReader) {
        this.dxfReader = Objects.requireNonNull(dxfReader, "dxfReader darf nicht null sein.");
        this.ifcReader = Objects.requireNonNull(ifcReader, "ifcReader darf nicht null sein.");
    }

    public Dxf3dObjectGeometry read(Path rfaFile) throws IOException {
        Path companion = companionFile(rfaFile).orElseThrow(() -> new IllegalArgumentException(
                "Zur RFA-Datei wird eine gleichnamige IFC- oder 3D-DXF-Begleitdatei benötigt."
        ));
        return extension(companion).equals("ifc") ? ifcReader.read(companion) : dxfReader.read(companion);
    }

    public Optional<Path> companionFile(Path rfaFile) throws IOException {
        Path normalized = Objects.requireNonNull(rfaFile, "rfaFile darf nicht null sein.").toAbsolutePath().normalize();
        Path directory = normalized.getParent();
        if (directory == null || !Files.isDirectory(directory)) {
            return Optional.empty();
        }
        String baseName = baseName(normalized.getFileName().toString());
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> baseName(path.getFileName().toString()).equalsIgnoreCase(baseName))
                    .filter(path -> List.of("ifc", "dxf").contains(extension(path)))
                    .sorted(Comparator.comparingInt(path -> extension(path).equals("ifc") ? 0 : 1))
                    .findFirst();
        }
    }

    private String baseName(String fileName) {
        int separator = fileName.lastIndexOf('.');
        return separator < 0 ? fileName : fileName.substring(0, separator);
    }

    private String extension(Path path) {
        String fileName = path.getFileName().toString();
        int separator = fileName.lastIndexOf('.');
        return separator < 0 ? "" : fileName.substring(separator + 1).toLowerCase(Locale.ROOT);
    }
}
