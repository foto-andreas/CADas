package de.schrell.cadas.application.help;

import java.util.Optional;

public record AboutInformation(String applicationName, String version, String description) {

    public static AboutInformation current() {
        String implementationVersion = AboutInformation.class.getPackage().getImplementationVersion();
        return new AboutInformation(
                "CADas",
                Optional.ofNullable(implementationVersion).orElse("Entwicklungsversion"),
                "CAD-Programm für Gebäude-Grundrisse"
        );
    }

    public String detailText() {
        return "Version " + version + System.lineSeparator() + description;
    }
}
