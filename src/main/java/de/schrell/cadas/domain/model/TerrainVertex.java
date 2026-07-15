package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.Objects;

/**
 * Höhenstützpunkt des Geländes an einer Grundrissposition, bezogen auf die niedrigste Etage.
 */
public record TerrainVertex(PlanPoint position, Length elevationAboveLowestFloor) {

    public TerrainVertex {
        Objects.requireNonNull(position, "position darf nicht null sein.");
        Objects.requireNonNull(elevationAboveLowestFloor, "elevationAboveLowestFloor darf nicht null sein.");
    }
}
