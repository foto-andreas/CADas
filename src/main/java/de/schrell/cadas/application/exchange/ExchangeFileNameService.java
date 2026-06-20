package de.schrell.cadas.application.exchange;

import java.nio.file.Path;

public final class ExchangeFileNameService {

    public Path ensureSingleExtension(Path path, String extension) {
        Path parent = path.getParent();
        String normalizedFilename = stripRepeatedExtension(path.getFileName().toString(), extension) + extension;
        return parent == null ? Path.of(normalizedFilename) : parent.resolve(normalizedFilename);
    }

    public String stripRepeatedExtension(Path path, String extension) {
        return stripRepeatedExtension(path.getFileName().toString(), extension);
    }

    private String stripRepeatedExtension(String filename, String extension) {
        String result = filename;
        while (result.toLowerCase().endsWith(extension.toLowerCase())) {
            result = result.substring(0, result.length() - extension.length());
        }
        return result;
    }
}
