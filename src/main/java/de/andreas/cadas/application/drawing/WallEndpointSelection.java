package de.andreas.cadas.application.drawing;

import de.andreas.cadas.domain.geometry.PlanPoint;

import java.util.List;
import java.util.UUID;

public record WallEndpointSelection(
        PlanPoint anchorPoint,
        List<UUID> startWallIds,
        List<UUID> endWallIds
) {
}

