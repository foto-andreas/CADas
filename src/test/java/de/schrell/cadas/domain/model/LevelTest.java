package de.schrell.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LevelTest {

    @Test
    void entferntMitWandAuchGebundeneÖffnungenUndBeläge() {
        Level level = new Level("Erdgeschoss");
        Wall removedWall = wallAt(0);
        Wall retainedWall = wallAt(2_000);
        Door removedDoor = Door.create(
                removedWall.id(),
                Length.ofMillimeters(500),
                Length.ofMillimeters(1_010),
                Length.ofMillimeters(2_010),
                Length.zero()
        );
        Door retainedDoor = Door.create(
                retainedWall.id(),
                Length.ofMillimeters(500),
                Length.ofMillimeters(1_010),
                Length.ofMillimeters(2_010),
                Length.zero()
        );
        WindowElement removedWindow = WindowElement.create(
                removedWall.id(),
                Length.ofMillimeters(1_800),
                Length.ofMillimeters(1_200),
                Length.ofMillimeters(900),
                Length.ofMillimeters(1_200)
        );
        WindowElement retainedWindow = WindowElement.create(
                retainedWall.id(),
                Length.ofMillimeters(1_800),
                Length.ofMillimeters(1_200),
                Length.ofMillimeters(900),
                Length.ofMillimeters(1_200)
        );
        SurfaceLayerStack removedExteriorStack = new SurfaceLayerStack(
                SurfaceType.WALL_EXTERIOR,
                removedWall.id().toString()
        );
        SurfaceLayerStack removedInteriorStack = new SurfaceLayerStack(
                SurfaceType.WALL_INTERIOR,
                removedWall.id() + "@" + UUID.randomUUID()
        );
        SurfaceLayerStack retainedWallStack = new SurfaceLayerStack(
                SurfaceType.WALL_INTERIOR,
                retainedWall.id().toString()
        );
        SurfaceLayerStack retainedFloorStack = new SurfaceLayerStack(
                SurfaceType.FLOOR,
                removedWall.id().toString()
        );
        level.addWall(removedWall);
        level.addWall(retainedWall);
        level.addDoor(removedDoor);
        level.addDoor(retainedDoor);
        level.addWindow(removedWindow);
        level.addWindow(retainedWindow);
        level.addSurfaceLayerStack(removedExteriorStack);
        level.addSurfaceLayerStack(removedInteriorStack);
        level.addSurfaceLayerStack(retainedWallStack);
        level.addSurfaceLayerStack(retainedFloorStack);

        assertTrue(level.removeWall(removedWall.id()));

        assertEquals(1, level.walls().size());
        assertEquals(retainedDoor, level.doors().getFirst());
        assertEquals(retainedWindow, level.windows().getFirst());
        assertFalse(level.surfaceLayerStacks().contains(removedExteriorStack));
        assertFalse(level.surfaceLayerStacks().contains(removedInteriorStack));
        assertTrue(level.surfaceLayerStacks().contains(retainedWallStack));
        assertTrue(level.surfaceLayerStacks().contains(retainedFloorStack));
        assertFalse(level.removeWall(UUID.randomUUID()));
    }

    @Test
    void entferntFußbodenerweiterungMitZugehörigemBelagsstapel() {
        Level level = new Level("Obergeschoss");
        FloorExtension floorExtension = FloorExtension.create(
                FloorExtensionType.BALCONY,
                FloorExtensionPlacement.EXTERIOR,
                new PlanPoint(0, 0),
                new PlanPoint(3_000, 1_500),
                Length.ofMillimeters(180)
        );
        SurfaceLayerStack extensionStack = new SurfaceLayerStack(SurfaceType.FLOOR, floorExtension.surfaceTargetKey());
        SurfaceLayerStack retainedStack = new SurfaceLayerStack(SurfaceType.FLOOR, "room:wohnen");
        level.addFloorExtension(floorExtension);
        level.addSurfaceLayerStack(extensionStack);
        level.addSurfaceLayerStack(retainedStack);

        assertTrue(level.removeFloorExtension(floorExtension.id()));

        assertTrue(level.floorExtensions().isEmpty());
        assertEquals(1, level.surfaceLayerStacks().size());
        assertEquals(retainedStack.id(), level.surfaceLayerStacks().getFirst().id());
        assertFalse(level.removeFloorExtension(UUID.randomUUID()));
    }

    @Test
    void verwaltetDachfensterUndÜbernimmtSieInKopien() {
        Level level = new Level("Dachgeschoss");
        RoofWindow first = RoofWindow.create(
                UUID.randomUUID(), new PlanPoint(1_000, 500),
                Length.ofMillimeters(800), Length.ofMillimeters(1_200), SlopedCeilingSide.NORTH
        );
        RoofWindow second = RoofWindow.create(
                UUID.randomUUID(), new PlanPoint(2_000, 500),
                Length.ofMillimeters(900), Length.ofMillimeters(1_100), SlopedCeilingSide.SOUTH
        );

        level.addRoofWindow(first);
        assertEquals(first, level.copy().roofWindows().getFirst());
        level.replaceRoofWindows(java.util.List.of(second));
        assertEquals(second, level.roofWindows().getFirst());
        assertTrue(level.removeRoofWindow(second.id()));
        assertFalse(level.removeRoofWindow(UUID.randomUUID()));
    }

    @Test
    void verwaltetJeRaumGetrennteBodenUndDeckenheizungen() {
        Level level = new Level("Erdgeschoss");
        Room room = Room.rectangular(
                "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 3_000),
                Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
        );
        level.addRoom(room);
        HydronicHeating floorHeating = heating(room.id(), HeatingSurfacePosition.FLOOR);
        HydronicHeating ceilingHeating = heating(room.id(), HeatingSurfacePosition.CEILING);

        level.addHydronicHeating(floorHeating);
        level.addHydronicHeating(ceilingHeating);

        assertEquals(floorHeating, level.findHydronicHeating(room.id(), HeatingSurfacePosition.FLOOR));
        assertEquals(ceilingHeating, level.copy().findHydronicHeating(room.id(), HeatingSurfacePosition.CEILING));
        assertThrows(IllegalArgumentException.class, () -> level.addHydronicHeating(
                heating(room.id(), HeatingSurfacePosition.FLOOR)
        ));
        assertTrue(level.removeHydronicHeating(floorHeating.id()));
        assertFalse(level.removeHydronicHeating(UUID.randomUUID()));
    }

    @Test
    void entferntHeizungenNichtMehrVorhandenerRäume() {
        Level level = new Level("Erdgeschoss");
        Room retainedRoom = Room.rectangular(
                "Wohnen", new PlanPoint(0, 0), new PlanPoint(4_000, 3_000),
                Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
        );
        Room removedRoom = Room.rectangular(
                "Küche", new PlanPoint(4_000, 0), new PlanPoint(7_000, 3_000),
                Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
        );
        level.addRoom(retainedRoom);
        level.addRoom(removedRoom);
        level.addHydronicHeating(heating(retainedRoom.id(), HeatingSurfacePosition.FLOOR));
        level.addHydronicHeating(heating(removedRoom.id(), HeatingSurfacePosition.FLOOR));

        level.replaceRooms(java.util.List.of(retainedRoom));

        assertEquals(1, level.hydronicHeatings().size());
        assertEquals(retainedRoom.id(), level.hydronicHeatings().getFirst().roomId());
    }

    private HydronicHeating heating(UUID roomId, HeatingSurfacePosition surfacePosition) {
        return HydronicHeating.create(
                roomId, surfacePosition, HeatingLayoutPattern.MEANDER,
                Length.ofMillimeters(150), Length.ofMillimeters(16), Length.ofMillimeters(80_000),
                Length.ofMillimeters(100), new PlanPoint(0, 0), new PlanPoint(100, 0)
        );
    }

    private Wall wallAt(double yMillimeters) {
        return Wall.create(
                new PlanSegment(new PlanPoint(0, yMillimeters), new PlanPoint(4_000, yMillimeters)),
                Length.ofMillimeters(175),
                Length.ofMillimeters(2_750)
        );
    }
}
