package de.schrell.cadas.application.heating;

import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.CardinalDirection;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.LineSegment;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.QuarterArc;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingResult;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.Turn;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HeatingCircuitCommandRouterTest {

    private final HeatingCircuitCommandRouter router = new HeatingCircuitCommandRouter();

    @Test
    void startetVorlaufNachObenUndRuecklaufNachUnten() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "Ii");

        LineSegment supply = (LineSegment) result.supplyPath().primitives().getFirst();
        LineSegment ret = (LineSegment) result.returnPath().primitives().getFirst();
        Assertions.assertEquals(0.0, supply.endPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(100.0, supply.endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(0.0, ret.endPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(-100.0, ret.endPoint().yMillimeters(), 0.001);
    }

    @Test
    void zeichnetRechteUndLinkeViertelkreiseMitHalbemVerlegeabstandAlsRadius() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "RrLl");

        QuarterArc supplyRight = (QuarterArc) result.supplyPath().primitives().getFirst();
        QuarterArc returnRight = (QuarterArc) result.returnPath().primitives().getFirst();
        QuarterArc supplyLeft = (QuarterArc) result.supplyPath().primitives().get(1);
        QuarterArc returnLeft = (QuarterArc) result.returnPath().primitives().get(1);

        Assertions.assertEquals(Turn.RIGHT, supplyRight.turn());
        Assertions.assertEquals(50.0, supplyRight.radiusMillimeters(), 0.001);
        Assertions.assertEquals(100.0, supplyRight.endPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(0.0, supplyRight.endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(CardinalDirection.RIGHT, supplyRight.endDirection());

        Assertions.assertEquals(Turn.RIGHT, returnRight.turn());
        Assertions.assertEquals(-100.0, returnRight.endPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(0.0, returnRight.endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(CardinalDirection.LEFT, returnRight.endDirection());

        Assertions.assertEquals(Turn.LEFT, supplyLeft.turn());
        Assertions.assertEquals(CardinalDirection.UP, supplyLeft.endDirection());
        Assertions.assertEquals(Turn.LEFT, returnLeft.turn());
        Assertions.assertEquals(CardinalDirection.DOWN, returnLeft.endDirection());
    }

    @Test
    void ignoriertLeerzeichenUndEnter() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "I \n i");

        Assertions.assertEquals(1, result.supplyPath().primitives().size());
        Assertions.assertEquals(1, result.returnPath().primitives().size());
    }

    @Test
    void meldetUngueltigeBefehle() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> router.route(2_000.0, 3_000.0, 100.0, "A"));
    }

    @Test
    void tauschtVorlaufUndRuecklaufFuerInvertierteDarstellung() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "Ii");
        RoutingResult inverted = result.withFlowInverted(true);

        Assertions.assertEquals(result.returnPath(), inverted.supplyPath());
        Assertions.assertEquals(result.supplyPath(), inverted.returnPath());
    }
}
