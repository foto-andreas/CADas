package de.andreas.cadas.application.dwg;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record DwgLibraryAnalysis(
        Path sourceFile,
        boolean successful,
        String converterName,
        DwgUnit unit,
        List<DwgBlockDefinition> blocks,
        List<String> messages
) {

    public DwgLibraryAnalysis {
        Objects.requireNonNull(sourceFile, "sourceFile darf nicht null sein.");
        Objects.requireNonNull(converterName, "converterName darf nicht null sein.");
        Objects.requireNonNull(unit, "unit darf nicht null sein.");
        Objects.requireNonNull(blocks, "blocks darf nicht null sein.");
        Objects.requireNonNull(messages, "messages darf nicht null sein.");
        blocks = List.copyOf(blocks);
        messages = List.copyOf(messages);
    }

    public static DwgLibraryAnalysis unavailable(Path sourceFile, String message) {
        return new DwgLibraryAnalysis(sourceFile, false, "", DwgUnit.UNITLESS, List.of(), List.of(message));
    }

    public String summary() {
        if (!successful) {
            return messages.isEmpty() ? "DWG konnte nicht analysiert werden." : messages.getFirst();
        }
        long usableBlocks = blocks.stream().filter(DwgBlockDefinition::hasGeometry).count();
        return converterName + " | " + unit + " | " + usableBlocks + " nutzbare Blöcke";
    }
}
