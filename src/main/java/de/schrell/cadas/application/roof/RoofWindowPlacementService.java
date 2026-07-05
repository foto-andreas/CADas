package de.schrell.cadas.application.roof;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPolygonSupport;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.RoofWindow;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;

import java.util.Comparator;
import java.util.Optional;

public final class RoofWindowPlacementService {

    public Optional<RoofWindow> place(Level level, PlanPoint point, Length width, Length depth) {
        return level.rooms().stream()
                .filter(room -> PlanPolygonSupport.containsPoint(room.outline(), point))
                .filter(room -> !room.slopedCeilingProfiles().isEmpty())
                .findFirst()
                .map(room -> RoofWindow.create(room.id(), point, width, depth, nearestSlope(room, point).lowSide()));
    }

    private SlopedCeilingProfile nearestSlope(Room room, PlanPoint point) {
        return room.slopedCeilingProfiles().stream()
                .min(Comparator.comparingDouble(profile -> room.ceilingHeightAt(point, profile)))
                .orElseThrow();
    }

}
