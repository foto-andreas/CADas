package de.schrell.cadas.application.heating;

import de.schrell.cadas.domain.geometry.PlanPolygonSupport;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectHeatingType;

import java.util.List;
import java.util.Objects;

/**
 * Ermittelt Heizleistungen pro Raum aus Heizkreisen und heizenden Objekten.
 */
public final class RoomHeatingOutputService {

    public RoomHeatTotals totals(Level level, Room room) {
        Objects.requireNonNull(level, "level darf nicht null sein.");
        Objects.requireNonNull(room, "room darf nicht null sein.");
        List<HeatingElementSummary> heatingObjects = heatingElements(level, room);
        double floorHeating = heatingOutput(level, room, HeatingSurfacePosition.FLOOR)
                + heatingObjects.stream()
                .filter(summary -> summary.heatingType().countsAsFloorHeating())
                .mapToDouble(HeatingElementSummary::heatOutputWatts)
                .sum();
        double ceilingHeating = heatingOutput(level, room, HeatingSurfacePosition.CEILING)
                + heatingObjects.stream()
                .filter(summary -> summary.heatingType().countsAsCeilingHeating())
                .mapToDouble(HeatingElementSummary::heatOutputWatts)
                .sum();
        double additionalSurfaceHeating = heatingObjects.stream()
                .filter(summary -> summary.heatingType().countsAsAdditionalSurfaceHeating())
                .mapToDouble(HeatingElementSummary::heatOutputWatts)
                .sum();
        double surfaceHeating = floorHeating + ceilingHeating + additionalSurfaceHeating;
        double heatingElements = heatingObjects.stream()
                .filter(summary -> summary.heatingType().countsAsHeatingElement())
                .mapToDouble(HeatingElementSummary::heatOutputWatts)
                .sum();
        return new RoomHeatTotals(
                floorHeating,
                ceilingHeating,
                additionalSurfaceHeating,
                surfaceHeating,
                heatingElements,
                surfaceHeating + heatingElements
        );
    }

    private double heatingOutput(Level level, Room room, HeatingSurfacePosition surfacePosition) {
        return level.hydronicHeatings().stream()
                .filter(heating -> heating.roomId().equals(room.id()))
                .filter(heating -> heating.surfacePosition() == surfacePosition)
                .flatMap(heating -> heating.zones().stream())
                .mapToDouble(HeatingZone::heatOutputWatts)
                .sum();
    }

    public List<HeatingElementSummary> heatingElements(Level level, Room room) {
        Objects.requireNonNull(level, "level darf nicht null sein.");
        Objects.requireNonNull(room, "room darf nicht null sein.");
        return level.roomObjects().stream()
                .filter(roomObject -> roomObject.heatOutputWatts() > 0.0)
                .filter(roomObject -> roomObject.heatingType().isHeated())
                .filter(roomObject -> PlanPolygonSupport.containsPoint(room.outline(), roomObject.center()))
                .map(this::summary)
                .toList();
    }

    private HeatingElementSummary summary(RoomObject roomObject) {
        return new HeatingElementSummary(
                roomObject.id().toString(),
                roomObject.name(),
                roomObject.type().toString(),
                roomObject.center(),
                roomObject.width().toMillimeters(),
                roomObject.depth().toMillimeters(),
                roomObject.rotationDegrees(),
                roomObject.heatingType(),
                roomObject.heatOutputWatts()
        );
    }

    public record RoomHeatTotals(
            double floorHeatingWatts,
            double ceilingHeatingWatts,
            double additionalSurfaceHeatingWatts,
            double surfaceHeatingWatts,
            double heatingElementWatts,
            double totalHeatOutputWatts
    ) {
    }

    public record HeatingElementSummary(
            String objectId,
            String objectName,
            String objectType,
            de.schrell.cadas.domain.geometry.PlanPoint center,
            double widthMillimeters,
            double depthMillimeters,
            double rotationDegrees,
            RoomObjectHeatingType heatingType,
            double heatOutputWatts
    ) {
    }
}
