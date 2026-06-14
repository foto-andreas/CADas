package de.andreas.cadas.application.dwg;

import java.nio.file.Path;
import java.util.List;

public record DwgConversionResult(Path dxfFile, String converterName, List<String> messages) {

    public DwgConversionResult {
        messages = List.copyOf(messages);
    }
}
