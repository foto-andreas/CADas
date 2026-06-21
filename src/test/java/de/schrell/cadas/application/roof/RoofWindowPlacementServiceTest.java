package de.schrell.cadas.application.roof;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.RoofWindow;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoofWindowPlacementServiceTest {

    private final RoofWindowPlacementService service = new RoofWindowPlacementService();

    @Test
    void platziertDachfensterNurAufVorhandenerDachschräge() {
        Level level = new Level("Dachgeschoss");
        Room room = Room.withSlopedCeilings(
                UUID.randomUUID(), "Studio",
                List.of(new PlanPoint(0, 0), new PlanPoint(4_000, 0), new PlanPoint(4_000, 3_000), new PlanPoint(0, 3_000)),
                Length.ofMillimeters(2_800), Length.ofMillimeters(180), Length.ofMillimeters(200),
                List.of(new SlopedCeilingProfile(SlopedCeilingSide.NORTH, Length.ofMillimeters(1_000), Length.ofMillimeters(1_200))),
                null
        );
        level.addRoom(room);

        RoofWindow roofWindow = service.place(
                level, new PlanPoint(2_000, 600), Length.ofMillimeters(900), Length.ofMillimeters(1_200)
        ).orElseThrow();

        assertEquals(room.id(), roofWindow.roomId());
        assertEquals(SlopedCeilingSide.NORTH, roofWindow.slopeSide());
        assertTrue(service.place(level, new PlanPoint(5_000, 600), roofWindow.width(), roofWindow.depth()).isEmpty());
    }
}
