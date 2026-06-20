package de.andreas.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LevelTest {

    @Test
    void entferntMitWandAuchGebundeneÖffnungen() {
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
        level.addWall(removedWall);
        level.addWall(retainedWall);
        level.addDoor(removedDoor);
        level.addDoor(retainedDoor);
        level.addWindow(removedWindow);
        level.addWindow(retainedWindow);

        assertTrue(level.removeWall(removedWall.id()));

        assertEquals(1, level.walls().size());
        assertEquals(retainedDoor, level.doors().getFirst());
        assertEquals(retainedWindow, level.windows().getFirst());
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

    private Wall wallAt(double yMillimeters) {
        return Wall.create(
                new PlanSegment(new PlanPoint(0, yMillimeters), new PlanPoint(4_000, yMillimeters)),
                Length.ofMillimeters(175),
                Length.ofMillimeters(2_750)
        );
    }
}
