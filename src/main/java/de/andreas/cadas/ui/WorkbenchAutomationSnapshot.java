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
        int threeDBodyCount,
        boolean threeDHasContent,
        String threeDCameraStatus,
        String surfaceType,
        String surfaceTypeOptions,
        String surfaceTargetLabel,
        String surfaceSelectionHint,
        String surfaceCoverageLabel,
        String statusText,
        double zoom,
        double offsetX,
        double offsetY
) {
}
