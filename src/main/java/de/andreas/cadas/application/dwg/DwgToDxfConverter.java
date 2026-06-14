package de.andreas.cadas.application.dwg;

import java.io.IOException;
import java.nio.file.Path;

public interface DwgToDxfConverter {

    DwgConversionAvailability availability();

    DwgConversionResult convert(Path dwgFile, Path targetDxfFile) throws IOException, InterruptedException;
}
