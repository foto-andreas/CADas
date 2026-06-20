package de.schrell.cadas.application.floor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import org.junit.jupiter.api.Test;

class FloorOpeningGeometryServiceTest {

    private final FloorOpeningGeometryService service = new FloorOpeningGeometryService();

    @Test
    void ziehtRechteckigeÖffnungExaktVomRaumAb() {
        Level level = new Level("Obergeschoss");
        Room room = room();
        level.addRoom(room);
        level.addFloorOpening(FloorOpening.create(
                room.id(), FloorOpeningShape.RECTANGLE, new PlanPoint(2_000, 2_000),
                Length.ofMillimeters(1_000), Length.ofMillimeters(2_000)
        ));

        assertEquals(14.0, service.floorAreaSquareMeters(level, room), 0.001);
        assertEquals(4, service.floorRectangles(level, room).size());
    }

    @Test
    void nähertRundeÖffnungMitFeinenFlächenstreifenAn() {
        Level level = new Level("Obergeschoss");
        Room room = room();
        level.addRoom(room);
        level.addFloorOpening(FloorOpening.create(
                room.id(), FloorOpeningShape.CIRCLE, new PlanPoint(2_000, 2_000),
                Length.ofMillimeters(1_000), Length.ofMillimeters(1_000)
        ));

        assertEquals(16.0 - Math.PI * 0.25, service.floorAreaSquareMeters(level, room), 0.002);
    }

    private Room room() {
        return Room.rectangular(
                "Raum", new PlanPoint(0, 0), new PlanPoint(4_000, 4_000),
                Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
        );
    }
}
