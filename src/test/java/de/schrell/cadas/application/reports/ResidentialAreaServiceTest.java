package de.schrell.cadas.application.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;

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

    @Test
    void ziehtRechteckigeUndRundeBodenöffnungenVonDerWohnflächeAb() {
        Level level = new Level("Erdgeschoss");
        Room room = Room.rectangular(
                "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                Length.ofMillimeters(2_500), Length.zero(), Length.zero()
        );
        level.addRoom(room);
        level.addFloorOpening(FloorOpening.create(
                room.id(), FloorOpeningShape.RECTANGLE, new PlanPoint(1_000, 1_000),
                Length.ofMillimeters(1_000), Length.ofMillimeters(1_000)
        ));
        level.addFloorOpening(FloorOpening.create(
                room.id(), FloorOpeningShape.CIRCLE, new PlanPoint(3_000, 3_000),
                Length.ofMillimeters(1_000), Length.ofMillimeters(1_000)
        ));

        assertEquals(14.215, service.residentialAreaSquareMeters(level, room), 0.03);
    }

    @Test
    void schließtTreppenMitMehrAlsDreiSteigungenNachWohnflächenverordnungAus() {
        Level level = new Level("Erdgeschoss");
        Room room = Room.rectangular(
                "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                Length.ofMillimeters(2_500), Length.zero(), Length.zero()
        );
        level.addRoom(room);
        level.addStaircase(Staircase.create(
                StairType.STRAIGHT, new PlanPoint(0, 0), new PlanPoint(1_000, 4_000),
                Length.ofMillimeters(2_800), 16
        ));

        assertEquals(12.0, service.residentialAreaSquareMeters(level, room), 0.03);
    }

    private SurfaceLayerStack stack(Room room, SurfaceType type, Length thickness) {
        SurfaceLayerStack stack = new SurfaceLayerStack(type, room.id().toString());
        stack.addLayer(SurfaceLayer.create("Belag", thickness, Length.of(50, LengthUnit.CENTIMETER), Length.of(50, LengthUnit.CENTIMETER), Length.zero()));
        return stack;
    }
}
