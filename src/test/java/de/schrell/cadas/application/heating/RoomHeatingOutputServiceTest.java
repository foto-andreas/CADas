package de.schrell.cadas.application.heating;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomHeatingOutputServiceTest {

    private final RoomHeatingOutputService service = new RoomHeatingOutputService();

    @Test
    void summiertHeizkreiseUndHeizelementeProRaum() {
        Level level = new Level("Erdgeschoss");
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4_000, 3_000),
                Length.ofMillimeters(2_600),
                Length.ofMillimeters(180),
                Length.ofMillimeters(200)
        );
        level.addRoom(room);
        HydronicHeating heating = HydronicHeating.create(
                room.id(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.MEANDER,
                Length.ofMillimeters(100),
                Length.ofMillimeters(16),
                Length.ofMillimeters(80_000),
                Length.ofMillimeters(100),
                new PlanPoint(0, 0),
                new PlanPoint(100, 0)
        ).withZones(List.of(
                HeatingZone.create("HK 1", List.of(
                        new PlanPoint(100, 100),
                        new PlanPoint(1_900, 100),
                        new PlanPoint(1_900, 1_400),
                        new PlanPoint(100, 1_400)
                ), HeatingLayoutPattern.MEANDER).withHeatOutputWattsPerSquareMeter(50.0)
        ));
        level.addHydronicHeating(heating);
        level.addRoomObject(new RoomObject(
                UUID.randomUUID(),
                "konvektor",
                "Konvektor",
                RoomObjectType.CUBOID,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(2_500, 1_500),
                Length.of(120, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                0.0,
                RoomObjectMountingMode.STANDS_ON_COVERING,
                true,
                "",
                Length.zero(),
                900.0
        ));

        RoomHeatingOutputService.RoomHeatTotals totals = service.totals(level, room);

        assertEquals(117.0, totals.surfaceHeatingWatts(), 0.2);
        assertEquals(900.0, totals.heatingElementWatts(), 0.001);
        assertEquals(1_017.0, totals.totalHeatOutputWatts(), 0.2);
        assertEquals(1, service.heatingElements(level, room).size());
    }
}
