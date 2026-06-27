package de.schrell.cadas.application.heating;

import de.schrell.cadas.domain.geometry.PlanPolygonSupport;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;

import java.util.List;
import java.util.Objects;

/**
 * Ermittelt Heizleistungen pro Raum aus Heizkreisen und heizenden Objekten.
 */
public final class RoomHeatingOutputService {

    public RoomHeatTotals totals(Level level, Room room) {
        Objects.requireNonNull(level, "level darf nicht null sein.");
        Objects.requireNonNull(room, "room darf nicht null sein.");
        double surfaceHeating = level.hydronicHeatings().stream()
                .filter(heating -> heating.roomId().equals(room.id()))
                .flatMap(heating -> heating.zones().stream())
                .mapToDouble(HeatingZone::heatOutputWatts)
                .sum();
        double heatingElements = heatingElements(level, room).stream()
                .mapToDouble(HeatingElementSummary::heatOutputWatts)
                .sum();
        return new RoomHeatTotals(surfaceHeating, heatingElements, surfaceHeating + heatingElements);
    }

    public List<HeatingElementSummary> heatingElements(Level level, Room room) {
        Objects.requireNonNull(level, "level darf nicht null sein.");
        Objects.requireNonNull(room, "room darf nicht null sein.");
        return level.roomObjects().stream()
                .filter(roomObject -> roomObject.heatOutputWatts() > 0.0)
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
                roomObject.heatOutputWatts()
        );
    }

    public record RoomHeatTotals(
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
            double heatOutputWatts
    ) {
    }
}
