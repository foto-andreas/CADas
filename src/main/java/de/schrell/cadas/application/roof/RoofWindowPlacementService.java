package de.schrell.cadas.application.roof;

import de.schrell.cadas.domain.geometry.Length;
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
                .filter(room -> contains(room, point))
                .filter(room -> !room.slopedCeilingProfiles().isEmpty())
                .findFirst()
                .map(room -> RoofWindow.create(room.id(), point, width, depth, nearestSlope(room, point).lowSide()));
    }

    private SlopedCeilingProfile nearestSlope(Room room, PlanPoint point) {
        return room.slopedCeilingProfiles().stream()
                .min(Comparator.comparingDouble(profile -> room.ceilingHeightAt(point, profile)))
                .orElseThrow();
    }

    private boolean contains(Room room, PlanPoint point) {
        boolean inside = false;
        int previousIndex = room.outline().size() - 1;
        for (int index = 0; index < room.outline().size(); index++) {
            PlanPoint current = room.outline().get(index);
            PlanPoint previous = room.outline().get(previousIndex);
            boolean intersects = (current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters())
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / (previous.yMillimeters() - current.yMillimeters())
                    + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            previousIndex = index;
        }
        return inside;
    }
}
