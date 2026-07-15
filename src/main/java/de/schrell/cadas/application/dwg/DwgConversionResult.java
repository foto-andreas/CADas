package de.schrell.cadas.application.dwg;

import java.nio.file.Path;
import java.util.List;

/** Erfolgreiches Ergebnis eines externen DWG-Laufs mit eindeutiger DXF-Ausgabe und Prozessmeldungen. */
public record DwgConversionResult(Path dxfFile, String converterName, List<String> messages) {

    public DwgConversionResult {
        messages = List.copyOf(messages);
    }
}
