package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.Objects;

public record TerrainVertex(PlanPoint position, Length elevationAboveLowestFloor) {

    public TerrainVertex {
        Objects.requireNonNull(position, "position darf nicht null sein.");
        Objects.requireNonNull(elevationAboveLowestFloor, "elevationAboveLowestFloor darf nicht null sein.");
    }
}
