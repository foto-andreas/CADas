package de.schrell.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WallTest {

    @Test
    void interpoliertPolygonalesHöhenprofilStückweise() {
        Wall wall = polygonWall();

        assertEquals(2_400.0, wall.heightAt(0), 0.001);
        assertEquals(2_750.0, wall.heightAt(500), 0.001);
        assertEquals(3_100.0, wall.heightAt(1_000), 0.001);
        assertEquals(3_100.0, wall.heightAt(3_000), 0.001);
        assertEquals(2_400.0, wall.minimumHeightMillimeters(), 0.001);
        assertEquals(3_100.0, wall.maximumHeightMillimeters(), 0.001);
        assertTrue(wall.hasPolygonalProfile());
    }

    @Test
    void skaliertProfilabständeBeimÄndernDerWandlänge() {
        Wall resized = polygonWall().withAxis(new PlanSegment(new PlanPoint(0, 0), new PlanPoint(8_000, 0)));

        assertEquals(2_000.0, resized.profile().get(1).offset().toMillimeters(), 0.001);
        assertEquals(8_000.0, resized.profile().getLast().offset().toMillimeters(), 0.001);
    }

    @Test
    void weistUnvollständigesProfilZurück() {
        assertThrows(IllegalArgumentException.class, () -> polygonWall().withProfile(List.of(
                new WallProfilePoint(Length.zero(), Length.ofMillimeters(2_400)),
                new WallProfilePoint(Length.ofMillimeters(3_000), Length.ofMillimeters(3_100))
        )));
    }

    private Wall polygonWall() {
        return new Wall(
                UUID.randomUUID(),
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4_000, 0)),
                Length.ofMillimeters(200),
                Length.ofMillimeters(3_100),
                Length.ofMillimeters(2_400),
                Length.ofMillimeters(3_100),
                List.of(
                        new WallProfilePoint(Length.zero(), Length.ofMillimeters(2_400)),
                        new WallProfilePoint(Length.ofMillimeters(1_000), Length.ofMillimeters(3_100)),
                        new WallProfilePoint(Length.ofMillimeters(4_000), Length.ofMillimeters(3_100))
                )
        );
    }
}
