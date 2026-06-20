package de.schrell.cadas.application.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;

import org.junit.jupiter.api.Test;

class ResidentialAreaServiceTest {

    private final ResidentialAreaService service = new ResidentialAreaService();

    @Test
    void gewichtetDachschraegeNachWohnflaechenverordnung() {
        Level level = new Level("Dachgeschoss");
        Room room = Room.rectangular(
                "Studio",
                new PlanPoint(0, 0),
                new PlanPoint(4_000, 4_000),
                Length.of(3, LengthUnit.METER),
                Length.zero(),
                Length.zero(),
                new SlopedCeilingProfile(SlopedCeilingSide.WEST, Length.zero())
        );
        level.addRoom(room);

        assertEquals(8.0, service.residentialAreaSquareMeters(level, room), 0.06);
    }

    @Test
    void beruecksichtigtBodenUndDeckenbelagBeiDerLichtenHoehe() {
        Level level = new Level("Erdgeschoss");
        Room room = Room.rectangular(
                "Niedriger Raum",
                new PlanPoint(0, 0),
                new PlanPoint(2_000, 2_000),
                Length.of(2.1, LengthUnit.METER),
                Length.zero(),
                Length.zero()
        );
        level.addRoom(room);
        level.addSurfaceLayerStack(stack(room, SurfaceType.FLOOR, Length.of(6, LengthUnit.CENTIMETER)));
        level.addSurfaceLayerStack(stack(room, SurfaceType.CEILING, Length.of(6, LengthUnit.CENTIMETER)));

        assertEquals(2.0, service.residentialAreaSquareMeters(level, room), 0.001);
    }

    private SurfaceLayerStack stack(Room room, SurfaceType type, Length thickness) {
        SurfaceLayerStack stack = new SurfaceLayerStack(type, room.id().toString());
        stack.addLayer(SurfaceLayer.create("Belag", thickness, Length.of(50, LengthUnit.CENTIMETER), Length.of(50, LengthUnit.CENTIMETER), Length.zero()));
        return stack;
    }
}
