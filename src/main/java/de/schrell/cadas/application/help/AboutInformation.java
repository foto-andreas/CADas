package de.schrell.cadas.application.help;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

/** Unveränderliche, anzeigefertige Build- und Produktinformation für den Über-CADas-Dialog. */
public record AboutInformation(String applicationName, String version, String buildTimestamp, String description) {

    public static AboutInformation current() {
        return current(AboutInformation.class.getPackage().getImplementationVersion(), loadBuildInformation());
    }

    static AboutInformation current(String implementationVersion, Properties buildInformation) {
        return new AboutInformation(
                "CADas",
                property(buildInformation, "version")
                        .or(() -> Optional.ofNullable(implementationVersion))
                        .orElse("Entwicklungsversion"),
                property(buildInformation, "buildTimestamp").orElse("nicht verfügbar"),
                "CAD-Programm für Gebäude-Grundrisse"
        );
    }

    public String detailText() {
        return "Version " + version
                + System.lineSeparator()
                + "Build-Zeitpunkt " + buildTimestamp
                + System.lineSeparator()
                + description;
    }

    private static Properties loadBuildInformation() {
        Properties properties = new Properties();
        try (InputStream stream = AboutInformation.class.getResourceAsStream("/build-info.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException exception) {
            return new Properties();
        }
        return properties;
    }

    private static Optional<String> property(Properties properties, String key) {
        return Optional.ofNullable(properties.getProperty(key))
                .map(String::trim)
                .filter(value -> !value.isEmpty());
    }
}
