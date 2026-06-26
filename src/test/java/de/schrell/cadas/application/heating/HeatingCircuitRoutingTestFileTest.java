package de.schrell.cadas.application.heating;

import de.schrell.cadas.domain.model.HeatingRoutingLanguage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HeatingCircuitRoutingTestFileTest {

    private static final Path TEST_DIRECTORY = Path.of("src/test/resources/heizkreise");
    private static final Set<String> VARIANTS = Set.of("Manuell", "Vario", "Meander");

    private final HeatingCircuitCommandRouter router = new HeatingCircuitCommandRouter();

    @Test
    void validiertGespeicherteHeizkreisTestdateien() throws IOException {
        if (!Files.isDirectory(TEST_DIRECTORY)) {
            return;
        }
        try (Stream<Path> files = Files.list(TEST_DIRECTORY)) {
            List<Path> testFiles = files
                    .filter(path -> path.getFileName().toString().endsWith(".cadasfbh"))
                    .sorted()
                    .toList();
            for (Path testFile : testFiles) {
                assertValidTestFile(testFile);
            }
        }
    }

    private void assertValidTestFile(Path testFile) throws IOException {
        Map<String, String> values = readValues(testFile);
        Assertions.assertEquals("cadas-fbh-routing-v1", require(values, "format", testFile));
        double widthMillimeters = centimeters(values, "breiteCm", testFile) * 10.0;
        double heightMillimeters = centimeters(values, "höheCm", testFile) * 10.0;
        double spacingMillimeters = centimeters(values, "verlegeabstandCm", testFile) * 10.0;
        String variant = require(values, "variante", testFile);
        Assertions.assertTrue(VARIANTS.contains(variant), testFile::toString);
        boolean serpentineMiddleLine = booleanValue(values, "schlangenMittellinie", testFile);
        booleanValue(values, "vorlaufRücklaufGetauscht", testFile);
        int rotationQuarterTurns = integer(values, "rotationViertel", testFile);
        Assertions.assertTrue(rotationQuarterTurns >= 0 && rotationQuarterTurns <= 3, testFile::toString);
        String commands = require(values, "kommandos", testFile);
        Assertions.assertFalse(commands.isBlank(), testFile::toString);
        Assertions.assertDoesNotThrow(() -> router.route(widthMillimeters, heightMillimeters, spacingMillimeters, commands));
        assertCanonicalCommandsMatch(values, commands, testFile);
        boolean generatorComparison = Boolean.parseBoolean(values.getOrDefault(
                "generatorVergleich",
                variant.equals("Manuell") ? "false" : "true"
        ));
        if (generatorComparison) {
            assertGeneratedCommandsMatchTestFile(
                    widthMillimeters,
                    heightMillimeters,
                    spacingMillimeters,
                    variant,
                    serpentineMiddleLine,
                    commands,
                    testFile
            );
        }
    }

    @Test
    void vergleichtKommandosRohrweiseUnabhängigVonDerEingabereihenfolge() {
        Assertions.assertEquals(canonicalCommands("=R-r"), canonicalCommands("IiRr"));
    }

    private void assertCanonicalCommandsMatch(Map<String, String> values, String commands, Path testFile) {
        String canonicalCommands = values.get("kanonischeKommandos");
        if (canonicalCommands == null) {
            return;
        }
        Assertions.assertEquals(
                canonicalCommands(commands),
                canonicalCommands(canonicalCommands),
                () -> "Kanonische Kommandos weichen ab in " + testFile
        );
    }

    private void assertGeneratedCommandsMatchTestFile(
            double widthMillimeters,
            double heightMillimeters,
            double spacingMillimeters,
            String variant,
            boolean serpentineMiddleLine,
            String commands,
            Path testFile
    ) {
        String generatedCommands = switch (variant) {
            case "Vario" -> router.rectangularVarioCommands(
                    widthMillimeters,
                    heightMillimeters,
                    spacingMillimeters,
                    serpentineMiddleLine
            );
            case "Meander" -> router.meanderCommands(
                    widthMillimeters,
                    heightMillimeters,
                    spacingMillimeters,
                    serpentineMiddleLine
            );
            case "Manuell" -> null;
            default -> throw new AssertionError("Unbekannte Variante `" + variant + "` in " + testFile);
        };
        if (generatedCommands == null) {
            return;
        }
        Assertions.assertEquals(
                pipeCommands(generatedCommands, true),
                pipeCommands(commands, true),
                () -> "Vorlauf-Kommandos weichen ab in " + testFile
        );
        Assertions.assertEquals(
                pipeCommands(generatedCommands, false),
                pipeCommands(commands, false),
                () -> "Rücklauf-Kommandos weichen ab in " + testFile
        );
    }

    private String pipeCommands(String commands, boolean supply) {
        StringBuilder result = new StringBuilder();
        String normalizedCommands = HeatingRoutingLanguage.normalizeCommands(commands);
        for (int index = 0; index < normalizedCommands.length(); index++) {
            char command = normalizedCommands.charAt(index);
            if (supply ? HeatingRoutingLanguage.isSupplyCommand(command) : HeatingRoutingLanguage.isReturnCommand(command)) {
                result.append(command);
            }
        }
        return result.toString();
    }

    private String canonicalCommands(String commands) {
        int separatorIndex = commands.indexOf('|');
        if (separatorIndex >= 0) {
            return pipeCommands(commands.substring(0, separatorIndex), true)
                    + "|"
                    + pipeCommands(commands.substring(separatorIndex + 1), false);
        }
        return pipeCommands(commands, true) + "|" + pipeCommands(commands, false);
    }

    private Map<String, String> readValues(Path testFile) throws IOException {
        Map<String, String> values = new HashMap<>();
        List<String> lines = Files.readAllLines(testFile, StandardCharsets.UTF_8);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            Assertions.assertTrue(separator > 0, () -> "Ungültige Zeile in " + testFile + ": " + line);
            values.put(line.substring(0, separator), line.substring(separator + 1));
        }
        return values;
    }

    private String require(Map<String, String> values, String key, Path testFile) {
        String value = values.get(key);
        Assertions.assertNotNull(value, () -> "Pflichtfeld `" + key + "` fehlt in " + testFile);
        return value;
    }

    private double centimeters(Map<String, String> values, String key, Path testFile) {
        try {
            double value = Double.parseDouble(require(values, key, testFile).replace(',', '.'));
            Assertions.assertTrue(value > 0.0, () -> "Feld `" + key + "` muss positiv sein in " + testFile);
            return value;
        } catch (NumberFormatException exception) {
            throw new AssertionError("Feld `" + key + "` ist keine Zentimeterzahl in " + testFile, exception);
        }
    }

    private boolean booleanValue(Map<String, String> values, String key, Path testFile) {
        String value = require(values, key, testFile);
        Assertions.assertTrue(value.equals("true") || value.equals("false"), () -> "Feld `" + key + "` ist kein Wahrheitswert in " + testFile);
        return Boolean.parseBoolean(value);
    }

    private int integer(Map<String, String> values, String key, Path testFile) {
        try {
            return Integer.parseInt(require(values, key, testFile));
        } catch (NumberFormatException exception) {
            throw new AssertionError("Feld `" + key + "` ist keine Ganzzahl in " + testFile, exception);
        }
    }
}
