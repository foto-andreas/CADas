package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.Objects;
import java.util.UUID;

public record RoomObject(
        UUID id,
        String presetId,
        String name,
        RoomObjectType type,
        RoomObjectShape shape,
        PlanPoint center,
        Length width,
        Length depth,
        Length height,
        double rotationDegrees,
        RoomObjectMountingMode mountingMode,
        boolean visible,
        String source,
        Length baseElevation,
        double heatOutputWatts
) {

    public RoomObject {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(presetId, "presetId darf nicht null sein.");
        Objects.requireNonNull(name, "name darf nicht null sein.");
        Objects.requireNonNull(type, "type darf nicht null sein.");
        Objects.requireNonNull(shape, "shape darf nicht null sein.");
        Objects.requireNonNull(center, "center darf nicht null sein.");
        Objects.requireNonNull(width, "width darf nicht null sein.");
        Objects.requireNonNull(depth, "depth darf nicht null sein.");
        Objects.requireNonNull(height, "height darf nicht null sein.");
        Objects.requireNonNull(mountingMode, "mountingMode darf nicht null sein.");
        Objects.requireNonNull(source, "source darf nicht null sein.");
        Objects.requireNonNull(baseElevation, "baseElevation darf nicht null sein.");
        if (width.toMillimeters() <= 0.0 || depth.toMillimeters() <= 0.0 || height.toMillimeters() <= 0.0) {
            throw new IllegalArgumentException("Objektmaße müssen größer als null sein.");
        }
        if (!Double.isFinite(rotationDegrees)) {
            throw new IllegalArgumentException("rotationDegrees muss endlich sein.");
        }
        if (!Double.isFinite(heatOutputWatts) || heatOutputWatts < 0.0) {
            throw new IllegalArgumentException("Die Wärmeleistung des Objekts darf nicht negativ sein.");
        }
        rotationDegrees = normalizeDegrees(rotationDegrees);
    }

    public RoomObject(
            UUID id,
            String presetId,
            String name,
            RoomObjectType type,
            RoomObjectShape shape,
            PlanPoint center,
            Length width,
            Length depth,
            Length height,
            double rotationDegrees,
            RoomObjectMountingMode mountingMode,
            boolean visible,
            String source
    ) {
        this(id, presetId, name, type, shape, center, width, depth, height, rotationDegrees, mountingMode, visible, source, Length.zero(), 0.0);
    }

    public RoomObject(
            UUID id,
            String presetId,
            String name,
            RoomObjectType type,
            RoomObjectShape shape,
            PlanPoint center,
            Length width,
            Length depth,
            Length height,
            double rotationDegrees,
            RoomObjectMountingMode mountingMode,
            boolean visible,
            String source,
            Length baseElevation
    ) {
        this(id, presetId, name, type, shape, center, width, depth, height, rotationDegrees, mountingMode, visible, source, baseElevation, 0.0);
    }

    public RoomObject(
            UUID id,
            String presetId,
            String name,
            RoomObjectType type,
            RoomObjectShape shape,
            PlanPoint center,
            Length width,
            Length depth,
            Length height,
            int rotationQuarterTurns,
            RoomObjectMountingMode mountingMode,
            boolean visible,
            String source
    ) {
        this(id, presetId, name, type, shape, center, width, depth, height, rotationQuarterTurns * 90.0, mountingMode, visible, source);
    }

    public RoomObject(
            UUID id,
            String presetId,
            String name,
            RoomObjectType type,
            RoomObjectShape shape,
            PlanPoint center,
            Length width,
            Length depth,
            Length height,
            int rotationQuarterTurns,
            boolean cutsFloorCovering,
            boolean visible,
            String source
    ) {
        this(
                id,
                presetId,
                name,
                type,
                shape,
                center,
                width,
                depth,
                height,
                rotationQuarterTurns * 90.0,
                RoomObjectMountingMode.fromCutsFloorCovering(cutsFloorCovering),
                visible,
                source
        );
    }

    public RoomObject(
            UUID id,
            String presetId,
            String name,
            RoomObjectType type,
            RoomObjectShape shape,
            PlanPoint center,
            Length width,
            Length depth,
            Length height,
            double rotationDegrees,
            boolean cutsFloorCovering,
            boolean visible,
            String source
    ) {
        this(
                id,
                presetId,
                name,
                type,
                shape,
                center,
                width,
                depth,
                height,
                rotationDegrees,
                RoomObjectMountingMode.fromCutsFloorCovering(cutsFloorCovering),
                visible,
                source
        );
    }

    public static RoomObject create(
            String presetId,
            String name,
            RoomObjectType type,
            RoomObjectShape shape,
            PlanPoint center,
            Length width,
            Length depth,
            Length height,
            boolean cutsFloorCovering,
            String source
    ) {
        return create(
                presetId,
                name,
                type,
                shape,
                center,
                width,
                depth,
                height,
                RoomObjectMountingMode.fromCutsFloorCovering(cutsFloorCovering),
                source
        );
    }

    public static RoomObject create(
            String presetId,
            String name,
            RoomObjectType type,
            RoomObjectShape shape,
            PlanPoint center,
            Length width,
            Length depth,
            Length height,
            RoomObjectMountingMode mountingMode,
            String source
    ) {
        return create(presetId, name, type, shape, center, width, depth, height, 0.0, mountingMode, source);
    }

    public static RoomObject create(
            String presetId,
            String name,
            RoomObjectType type,
            RoomObjectShape shape,
            PlanPoint center,
            Length width,
            Length depth,
            Length height,
            double rotationDegrees,
            RoomObjectMountingMode mountingMode,
            String source
    ) {
        return new RoomObject(UUID.randomUUID(), presetId, name, type, shape, center, width, depth, height, rotationDegrees, mountingMode, true, source);
    }

    public boolean cutsFloorCovering() {
        return mountingMode.cutsFloorCovering();
    }

    public double footprintWidthMillimeters() {
        double radians = Math.toRadians(rotationDegrees);
        return Math.abs(width.toMillimeters() * Math.cos(radians))
                + Math.abs(depth.toMillimeters() * Math.sin(radians));
    }

    public double footprintDepthMillimeters() {
        double radians = Math.toRadians(rotationDegrees);
        return Math.abs(width.toMillimeters() * Math.sin(radians))
                + Math.abs(depth.toMillimeters() * Math.cos(radians));
    }

    public double minXMillimeters() {
        return center.xMillimeters() - footprintWidthMillimeters() / 2.0;
    }

    public double maxXMillimeters() {
        return center.xMillimeters() + footprintWidthMillimeters() / 2.0;
    }

    public double minYMillimeters() {
        return center.yMillimeters() - footprintDepthMillimeters() / 2.0;
    }

    public double maxYMillimeters() {
        return center.yMillimeters() + footprintDepthMillimeters() / 2.0;
    }

    public RoomObject withVisibility(boolean newVisibility) {
        return new RoomObject(id, presetId, name, type, shape, center, width, depth, height, rotationDegrees, mountingMode, newVisibility, source, baseElevation, heatOutputWatts);
    }

    public RoomObject withRotationDegrees(double newRotationDegrees) {
        return new RoomObject(id, presetId, name, type, shape, center, width, depth, height, newRotationDegrees, mountingMode, visible, source, baseElevation, heatOutputWatts);
    }

    public RoomObject withBaseElevation(Length newBaseElevation) {
        return new RoomObject(id, presetId, name, type, shape, center, width, depth, height, rotationDegrees, mountingMode, visible, source, newBaseElevation, heatOutputWatts);
    }

    public RoomObject withHeatOutputWatts(double newHeatOutputWatts) {
        return new RoomObject(id, presetId, name, type, shape, center, width, depth, height, rotationDegrees, mountingMode, visible, source, baseElevation, newHeatOutputWatts);
    }

    private static double normalizeDegrees(double degrees) {
        double normalized = degrees % 360.0;
        return normalized < 0.0 ? normalized + 360.0 : normalized;
    }
}
