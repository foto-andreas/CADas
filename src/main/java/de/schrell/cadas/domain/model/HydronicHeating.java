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
        Length manifoldFreeAreaWidth,
        Length manifoldFreeAreaDepth,
        List<HeatingZone> zones
) {

    public static final Length DEFAULT_MANIFOLD_FREE_AREA_WIDTH = Length.ofMillimeters(600);
    public static final Length DEFAULT_MANIFOLD_FREE_AREA_DEPTH = Length.ofMillimeters(1_000);

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
        Objects.requireNonNull(manifoldFreeAreaWidth, "manifoldFreeAreaWidth darf nicht null sein.");
        Objects.requireNonNull(manifoldFreeAreaDepth, "manifoldFreeAreaDepth darf nicht null sein.");
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
        if (manifoldFreeAreaWidth.toMillimeters() <= 0.0 || manifoldFreeAreaDepth.toMillimeters() <= 0.0) {
            throw new IllegalArgumentException("Die HKV-Freifläche muss positive Maße besitzen.");
        }
        zones = List.copyOf(zones);
    }

    public HydronicHeating(
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
        this(id, roomId, surfacePosition, layoutPattern, pipeSpacing, pipeDiameter, maximumPipeLength,
                wallClearance, supplyPoint, returnPoint,
                DEFAULT_MANIFOLD_FREE_AREA_WIDTH, DEFAULT_MANIFOLD_FREE_AREA_DEPTH, zones);
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
                maximumPipeLength, wallClearance, supplyPoint, returnPoint,
                DEFAULT_MANIFOLD_FREE_AREA_WIDTH, DEFAULT_MANIFOLD_FREE_AREA_DEPTH, List.of()
        );
    }

    public Length bendRadius() {
        return Length.ofMillimeters(pipeSpacing.toMillimeters() / 2.0);
    }

    public HydronicHeating withZones(List<HeatingZone> newZones) {
        return new HydronicHeating(
                id, roomId, surfacePosition, layoutPattern, pipeSpacing, pipeDiameter,
                maximumPipeLength, wallClearance, supplyPoint, returnPoint,
                manifoldFreeAreaWidth, manifoldFreeAreaDepth, newZones
        );
    }

    public HydronicHeating withManifold(PlanPoint newSupplyPoint, PlanPoint newReturnPoint) {
        return new HydronicHeating(
                id, roomId, surfacePosition, layoutPattern, pipeSpacing, pipeDiameter,
                maximumPipeLength, wallClearance, newSupplyPoint, newReturnPoint,
                manifoldFreeAreaWidth, manifoldFreeAreaDepth, zones
        );
    }

    public HydronicHeating withManifoldFreeArea(Length newWidth, Length newDepth) {
        return new HydronicHeating(
                id, roomId, surfacePosition, layoutPattern, pipeSpacing, pipeDiameter,
                maximumPipeLength, wallClearance, supplyPoint, returnPoint,
                newWidth, newDepth, zones
        );
    }
}
