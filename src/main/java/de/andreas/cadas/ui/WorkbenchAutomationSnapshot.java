package de.andreas.cadas.ui;

public record WorkbenchAutomationSnapshot(
        String projectName,
        String activeLevel,
        String activeView,
        String activeTool,
        int wallCount,
        int roomCount,
        int doorCount,
        int windowCount,
        int stairCount,
        int selectionCount,
        int registeredCadLibraries,
        String statusText
) {
}
