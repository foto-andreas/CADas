package de.andreas.cadas.application.exchange;

import de.andreas.cadas.domain.model.Level;

import java.io.IOException;
import java.nio.file.Path;

public interface LevelExchangeService {

    void exportLevel(Level level, Path targetFile) throws IOException;

    Level importLevel(Path sourceFile, String levelName) throws IOException;
}

