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
    void startetVorlaufNachObenUndRücklaufNachUnten() {
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
        Assertions.assertEquals(50.0, supplyRight.endPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(50.0, supplyRight.endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(CardinalDirection.RIGHT, supplyRight.endDirection());

        Assertions.assertEquals(Turn.RIGHT, returnRight.turn());
        Assertions.assertEquals(-50.0, returnRight.endPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(-50.0, returnRight.endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(CardinalDirection.LEFT, returnRight.endDirection());

        Assertions.assertEquals(Turn.LEFT, supplyLeft.turn());
        Assertions.assertEquals(100.0, supplyLeft.endPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(100.0, supplyLeft.endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(CardinalDirection.UP, supplyLeft.endDirection());
        Assertions.assertEquals(Turn.LEFT, returnLeft.turn());
        Assertions.assertEquals(-100.0, returnLeft.endPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(-100.0, returnLeft.endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(CardinalDirection.DOWN, returnLeft.endDirection());
    }

    @Test
    void ignoriertLeerzeichenUndEnter() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "I \n i");

        Assertions.assertEquals(1, result.supplyPath().primitives().size());
        Assertions.assertEquals(1, result.returnPath().primitives().size());
    }

    @Test
    void meldetUngültigeBefehle() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> router.route(2_000.0, 3_000.0, 100.0, "A"));
    }

    @Test
    void tauschtVorlaufUndRücklaufFürInvertierteDarstellung() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "Ii");
        RoutingResult inverted = result.withFlowInverted(true);

        Assertions.assertEquals(result.returnPath(), inverted.supplyPath());
        Assertions.assertEquals(result.supplyPath(), inverted.returnPath());
    }

    @Test
    void verschiebtRoutingErgebnisOhneKommandosemantikZuVerändern() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "Ii");
        RoutingResult shifted = result.translatedBy(0.0, 50.0);

        Assertions.assertEquals(50.0, shifted.supplyPath().startPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(150.0, shifted.supplyPath().endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(-50.0, shifted.returnPath().endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(0.0, result.supplyPath().startPoint().yMillimeters(), 0.001);
    }

    @Test
    void drehtRoutingErgebnisUmNeunzigGradImUhrzeigersinn() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "I");
        RoutingResult rotated = result.rotatedClockwise();

        Assertions.assertEquals(100.0, rotated.supplyPath().endPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(0.0, rotated.supplyPath().endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(CardinalDirection.RIGHT, rotated.supplyPath().endDirection());
        Assertions.assertEquals(3_000.0, rotated.widthMillimeters(), 0.001);
        Assertions.assertEquals(2_000.0, rotated.heightMillimeters(), 0.001);
    }

    @Test
    void erzeugtQuadratischenVarioRouterAusWachsenderDoppelspirale() {
        String commands = router.squareVarioCommands(1_300.0, 100.0);

        Assertions.assertEquals(
                "RRIRIIRIIIRIIIIRIIIIIRIIIIIIRIIIIIIIRIIIIIIIIRIIIIIIIIIRIIIIIIIIIIRIIIIIIIIIII",
                filter(commands, true)
        );
        Assertions.assertEquals(
                "rririiriiiriiiiriiiiiriiiiiiriiiiiiiriiiiiiiiriiiiiiiiiriiiiiiiiiiriiiiiiiiiiiriiiiiiiiiiiiriiiiiiiiiiii",
                filter(commands, false)
        );
        RoutingResult result = router.route(1_300.0, 1_300.0, 100.0, commands);
        Assertions.assertFalse(result.supplyPath().primitives().isEmpty());
        Assertions.assertFalse(result.returnPath().primitives().isEmpty());
    }

    @Test
    void skaliertQuadratischenVarioRouterAusSeitenlängeUndVerlegeabstand() {
        String commands = router.squareVarioCommands(2_000.0, 100.0);

        Assertions.assertTrue(filter(commands, true).contains("IIIIIIIIIIIIIIIIII"));
        Assertions.assertTrue(filter(commands, false).contains("iiiiiiiiiiiiiiiiiii"));
        Assertions.assertDoesNotThrow(() -> router.route(2_000.0, 2_000.0, 100.0, commands));
    }

    @Test
    void erzeugtRechteckigenVarioRouterMitGekürztenEndläufen() {
        String commands = router.rectangularVarioCommands(2_000.0, 3_000.0, 100.0);
        String supply = filter(commands, true);
        String ret = filter(commands, false);

        Assertions.assertEquals(repeated('I', 5) + "RR", supply.substring(0, 7));
        Assertions.assertEquals(repeated('i', 5) + "rr", ret.substring(0, 7));
        Assertions.assertTrue(supply.contains(repeated('I', 27)));
        Assertions.assertTrue(ret.contains(repeated('i', 25)));
        Assertions.assertTrue(supply.endsWith(repeated('I', 18) + "R"));
        Assertions.assertTrue(ret.endsWith(repeated('i', 16) + "r"));
        Assertions.assertFalse(supply.endsWith(repeated('I', 28)));
        Assertions.assertFalse(ret.endsWith(repeated('i', 27)));
        Assertions.assertDoesNotThrow(() -> router.route(2_000.0, 3_000.0, 100.0, commands));
    }

    private String repeated(char command, int count) {
        return String.valueOf(command).repeat(count);
    }

    private String filter(String commands, boolean supply) {
        StringBuilder filtered = new StringBuilder();
        for (int index = 0; index < commands.length(); index++) {
            char command = commands.charAt(index);
            if (Character.isUpperCase(command) == supply) {
                filtered.append(command);
            }
        }
        return filtered.toString();
    }
}
