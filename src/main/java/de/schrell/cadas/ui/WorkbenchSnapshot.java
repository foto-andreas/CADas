package de.schrell.cadas.ui;

import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.domain.model.ProjectModel;
import java.util.List;
import java.util.Objects;

public record WorkbenchSnapshot(
        ProjectModel project,
        List<GuideLine> guideLines,
        String activeLevelName,
        List<SelectionKey> selectedSelections,
        SelectionKey primarySelection,
        double zoom,
        double offsetX,
        double offsetY
) {

    public WorkbenchSnapshot {
        Objects.requireNonNull(project, "project darf nicht null sein.");
        Objects.requireNonNull(guideLines, "guideLines darf nicht null sein.");
        Objects.requireNonNull(activeLevelName, "activeLevelName darf nicht null sein.");
        Objects.requireNonNull(selectedSelections, "selectedSelections darf nicht null sein.");
        guideLines = List.copyOf(guideLines);
        selectedSelections = List.copyOf(selectedSelections);
    }
}
