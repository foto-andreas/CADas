package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record HydronicHeating(
        UUID id,
        UUID roomId,
        HeatingSurfacePosition surfacePosition,
        HeatingLayoutPattern layoutPattern,
        Length pipeSpacing,
        Length pipeDiameter,
        Length maximumPipeLength,
        Length wallClearance,
        PlanPoint supplyPoint,
        PlanPoint returnPoint,
        List<HeatingZone> zones
) {

    public HydronicHeating {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(roomId, "roomId darf nicht null sein.");
        Objects.requireNonNull(surfacePosition, "surfacePosition darf nicht null sein.");
        Objects.requireNonNull(layoutPattern, "layoutPattern darf nicht null sein.");
        Objects.requireNonNull(pipeSpacing, "pipeSpacing darf nicht null sein.");
        Objects.requireNonNull(pipeDiameter, "pipeDiameter darf nicht null sein.");
        Objects.requireNonNull(maximumPipeLength, "maximumPipeLength darf nicht null sein.");
        Objects.requireNonNull(wallClearance, "wallClearance darf nicht null sein.");
        Objects.requireNonNull(supplyPoint, "supplyPoint darf nicht null sein.");
        Objects.requireNonNull(returnPoint, "returnPoint darf nicht null sein.");
        Objects.requireNonNull(zones, "zones darf nicht null sein.");
        if (pipeSpacing.toMillimeters() <= 0.0) {
            throw new IllegalArgumentException("Der Verlegeabstand muss größer als null sein.");
        }
        if (pipeDiameter.toMillimeters() <= 0.0 || pipeDiameter.compareTo(pipeSpacing) >= 0) {
            throw new IllegalArgumentException("Der Rohrdurchmesser muss positiv und kleiner als der Verlegeabstand sein.");
        }
        if (maximumPipeLength.toMillimeters() <= 0.0) {
            throw new IllegalArgumentException("Die maximale Rohrlänge muss größer als null sein.");
        }
        if (wallClearance.toMillimeters() < 0.0) {
            throw new IllegalArgumentException("Der Wandabstand darf nicht negativ sein.");
        }
        zones = List.copyOf(zones);
    }

    public static HydronicHeating create(
            UUID roomId,
            HeatingSurfacePosition surfacePosition,
            HeatingLayoutPattern layoutPattern,
            Length pipeSpacing,
            Length pipeDiameter,
            Length maximumPipeLength,
            Length wallClearance,
            PlanPoint supplyPoint,
            PlanPoint returnPoint
    ) {
        return new HydronicHeating(
                UUID.randomUUID(), roomId, surfacePosition, layoutPattern, pipeSpacing, pipeDiameter,
                maximumPipeLength, wallClearance, supplyPoint, returnPoint, List.of()
        );
    }

    public Length bendRadius() {
        return Length.ofMillimeters(pipeSpacing.toMillimeters() / 2.0);
    }

    public HydronicHeating withZones(List<HeatingZone> newZones) {
        return new HydronicHeating(
                id, roomId, surfacePosition, layoutPattern, pipeSpacing, pipeDiameter,
                maximumPipeLength, wallClearance, supplyPoint, returnPoint, newZones
        );
    }
}
