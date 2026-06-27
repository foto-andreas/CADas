package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomObjectTest {

    @Test
    void berechnetBegrenzungFürFreienDrehwinkel() {
        RoomObject roomObject = roomObject(30.0, Length.ofMillimeters(1000), Length.ofMillimeters(500));

        assertEquals(1116.025, roomObject.footprintWidthMillimeters(), 0.001);
        assertEquals(933.013, roomObject.footprintDepthMillimeters(), 0.001);
    }

    @Test
    void normalisiertDrehwinkel() {
        assertEquals(345.0, roomObject(-15.0, Length.ofMillimeters(1000), Length.ofMillimeters(500)).rotationDegrees(), 0.001);
        assertEquals(90.0, roomObject(450.0, Length.ofMillimeters(1000), Length.ofMillimeters(500)).rotationDegrees(), 0.001);
    }

    @Test
    void weistUngültigeObjektmaßeZurück() {
        assertThrows(IllegalArgumentException.class, () -> roomObject(0.0, Length.zero(), Length.ofMillimeters(500)));
        assertThrows(IllegalArgumentException.class, () -> roomObject(0.0, Length.ofMillimeters(1000), Length.ofMillimeters(500))
                .withHeatOutputWatts(-1.0));
    }

    @Test
    void erhältPositiveUndNegativeBasishöhenBeiÄnderungen() {
        RoomObject roomObject = roomObject(15.0, Length.ofMillimeters(1000), Length.ofMillimeters(500))
                .withBaseElevation(Length.ofMillimeters(-250))
                .withHeatOutputWatts(850.0);

        assertEquals(-250.0, roomObject.baseElevation().toMillimeters(), 0.001);
        assertEquals(850.0, roomObject.heatOutputWatts(), 0.001);
        assertEquals(-250.0, roomObject.withRotationDegrees(30).baseElevation().toMillimeters(), 0.001);
        assertEquals(-250.0, roomObject.withVisibility(false).baseElevation().toMillimeters(), 0.001);
        assertEquals(850.0, roomObject.withBaseElevation(Length.zero()).heatOutputWatts(), 0.001);
    }

    private RoomObject roomObject(double rotationDegrees, Length width, Length depth) {
        return new RoomObject(
                UUID.randomUUID(),
                "test",
                "Testobjekt",
                RoomObjectType.TABLE,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(0, 0),
                width,
                depth,
                Length.ofMillimeters(750),
                rotationDegrees,
                RoomObjectMountingMode.STANDS_ON_COVERING,
                true,
                ""
        );
    }
}
