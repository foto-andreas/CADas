package de.schrell.cadas.quality;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PackageDocumentationTest {

    private static final Pattern TOP_LEVEL_TYPE = Pattern.compile(
            "(?m)^(?:public\\s+)?(?:(?:abstract|final|sealed|non-sealed)\\s+)?"
                    + "(?:class|interface|record|enum)\\s+[A-Za-z_$][A-Za-z0-9_$]*");

    @Test
    void jedesProduktionsUndTestpaketBesitztEineAusführlicheReadme() throws Exception {
        for (Path sourceRoot : List.of(Path.of("src/main/java"), Path.of("src/test/java"))) {
            for (Path packageDirectory : packageDirectories(sourceRoot)) {
                Path readme = packageDirectory.resolve("README.md");
                assertTrue(Files.isRegularFile(readme), () -> "Paketdokumentation fehlt: " + readme);
                assertTrue(Files.readString(readme).length() >= 120,
                        () -> "Paketdokumentation ist zu knapp: " + readme);
            }
        }
    }

    @Test
    void jedesProduktionspaketBesitztJavaPaketdokumentation() throws Exception {
        for (Path packageDirectory : packageDirectories(Path.of("src/main/java"))) {
            Path packageInfo = packageDirectory.resolve("package-info.java");
            assertTrue(Files.isRegularFile(packageInfo), () -> "package-info.java fehlt: " + packageInfo);
        }
    }

    @Test
    void jederProduktionstypErklärtSeineFachlicheVerantwortung() throws Exception {
        List<Path> undokumentierteTypen = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path sourceFile : files
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("module-info.java"))
                    .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                    .sorted()
                    .toList()) {
                String source = Files.readString(sourceFile);
                Matcher type = TOP_LEVEL_TYPE.matcher(source);
                if (!type.find() || !source.substring(0, type.start()).stripTrailing().endsWith("*/")) {
                    undokumentierteTypen.add(sourceFile);
                }
            }
        }
        assertTrue(undokumentierteTypen.isEmpty(),
                () -> "Fachliche Typdokumentation fehlt:\n" + String.join("\n", undokumentierteTypen.stream()
                        .map(Path::toString)
                        .toList()));
    }

    private List<Path> packageDirectories(Path sourceRoot) throws Exception {
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("module-info.java"))
                    .map(Path::getParent)
                    .distinct()
                    .sorted()
                    .toList();
        }
    }
}
