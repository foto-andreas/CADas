package de.andreas.cadas.application.layers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DwgBlockCatalogService {

    public List<String> loadCatalog(Path dwgFile) {
        Set<String> blockNames = new LinkedHashSet<>();
        for (Path candidate : catalogCandidates(dwgFile)) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try {
                for (String line : Files.readAllLines(candidate)) {
                    String normalized = line.trim();
                    if (!normalized.isBlank() && !normalized.startsWith("#")) {
                        blockNames.add(normalized);
                    }
                }
            } catch (IOException ignored) {
                // Katalogdateien sind optional.
            }
        }
        return List.copyOf(blockNames);
    }

    private List<Path> catalogCandidates(Path dwgFile) {
        List<Path> candidates = new ArrayList<>();
        String fileName = dwgFile.getFileName().toString();
        candidates.add(dwgFile.resolveSibling(fileName + ".blocks"));
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".dwg")) {
            String baseName = fileName.substring(0, fileName.length() - 4);
            candidates.add(dwgFile.resolveSibling(baseName + ".blocks"));
        }
        return candidates;
    }
}
