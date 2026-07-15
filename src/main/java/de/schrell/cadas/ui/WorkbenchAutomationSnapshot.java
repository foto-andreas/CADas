package de.schrell.cadas.ui;

/** Reduzierter, threadsicher lesbarer Zustandsabzug für die lokale HTTP-Automatisierung. */
public record WorkbenchAutomationSnapshot(
        String projectName,
        String activeLevel,
        String activeView,
        String workspaceMode,
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
        String selectedRoomMetrics,
        String statusText,
        double zoom,
        double offsetX,
        double offsetY
) {
}
