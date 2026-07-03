package de.schrell.cadas.ui;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

final class CadWorkbenchCoveringSourceSupport {

    private CadWorkbenchCoveringSourceSupport() {
    }

    static Optional<Path> extractDwgLibraryPath(String coveringSource) {
        if (coveringSource == null || coveringSource.isBlank()) {
            return Optional.empty();
        }
        String pathPart = coveringSource.contains("#")
                ? coveringSource.substring(0, coveringSource.indexOf('#'))
                : coveringSource;
        if (!pathPart.toLowerCase(Locale.ROOT).endsWith(".dwg")) {
            return Optional.empty();
        }
        return Optional.of(Path.of(pathPart));
    }

    static Optional<String> extractDwgBlockName(String coveringSource) {
        if (coveringSource == null || !coveringSource.contains("#")) {
            return Optional.empty();
        }
        return Optional.of(coveringSource.substring(coveringSource.indexOf('#') + 1));
    }

    static String formatCoveringSourceLabel(String coveringSource) {
        if (coveringSource == null || coveringSource.isBlank()) {
            return "";
        }
        Optional<Path> dwgPath = extractDwgLibraryPath(coveringSource);
        if (dwgPath.isPresent()) {
            String fileName = dwgPath.get().getFileName().toString();
            return extractDwgBlockName(coveringSource)
                    .map(blockName -> fileName + " → " + blockName)
                    .orElse(fileName);
        }
        if (coveringSource.toLowerCase(Locale.ROOT).endsWith(".cadasbelag")) {
            String fileName = Path.of(coveringSource).getFileName().toString();
            return "Eigenes Preset: " + fileName.substring(0, fileName.length() - ".cadasbelag".length());
        }
        return coveringSource;
    }
}
