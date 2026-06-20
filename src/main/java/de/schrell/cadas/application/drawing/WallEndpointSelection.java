package de.schrell.cadas.application.drawing;

import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.List;
import java.util.UUID;

public record WallEndpointSelection(
        PlanPoint anchorPoint,
        List<UUID> startWallIds,
        List<UUID> endWallIds
) {
}
