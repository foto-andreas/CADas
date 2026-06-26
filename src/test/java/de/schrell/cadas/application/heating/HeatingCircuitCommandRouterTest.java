package de.schrell.cadas.application.heating;

import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.CardinalDirection;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.LineSegment;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.QuarterArc;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingResult;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.Turn;
import de.schrell.cadas.domain.model.HeatingRoutingLanguage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HeatingCircuitCommandRouterTest {

    private final HeatingCircuitCommandRouter router = new HeatingCircuitCommandRouter();

    @Test
    void startetVorlaufNachObenUndRücklaufNachUnten() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "=-");

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
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "= \n -");

        Assertions.assertEquals(1, result.supplyPath().primitives().size());
        Assertions.assertEquals(1, result.returnPath().primitives().size());
    }

    @Test
    void meldetUngültigeBefehle() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> router.route(2_000.0, 3_000.0, 100.0, "A"));
    }

    @Test
    void tauschtVorlaufUndRücklaufFürInvertierteDarstellung() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "=-");
        RoutingResult inverted = result.withFlowInverted(true);

        Assertions.assertEquals(result.returnPath(), inverted.supplyPath());
        Assertions.assertEquals(result.supplyPath(), inverted.returnPath());
    }

    @Test
    void verschiebtRoutingErgebnisOhneKommandosemantikZuVerändern() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "=-");
        RoutingResult shifted = result.translatedBy(0.0, 50.0);

        Assertions.assertEquals(50.0, shifted.supplyPath().startPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(150.0, shifted.supplyPath().endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(-50.0, shifted.returnPath().endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(0.0, result.supplyPath().startPoint().yMillimeters(), 0.001);
    }

    @Test
    void drehtRoutingErgebnisUmNeunzigGradImUhrzeigersinn() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "=");
        RoutingResult rotated = result.rotatedClockwise();

        Assertions.assertEquals(100.0, rotated.supplyPath().endPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(0.0, rotated.supplyPath().endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(CardinalDirection.RIGHT, rotated.supplyPath().endDirection());
        Assertions.assertEquals(3_000.0, rotated.widthMillimeters(), 0.001);
        Assertions.assertEquals(2_000.0, rotated.heightMillimeters(), 0.001);
    }

    @Test
    void loeschtMitXDenLetztenSchrittJeRohrrolle() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "==X--x");

        Assertions.assertEquals(1, result.supplyPath().primitives().size());
        Assertions.assertEquals(1, result.returnPath().primitives().size());
        Assertions.assertEquals(100.0, result.supplyPath().endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(-100.0, result.returnPath().endPoint().yMillimeters(), 0.001);
    }

    @Test
    void spiegeltRoutingErgebnisHorizontalUndVertikal() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "R");
        RoutingResult horizontal = result.mirroredHorizontally();
        QuarterArc horizontalArc = (QuarterArc) horizontal.supplyPath().primitives().getFirst();
        RoutingResult vertical = result.mirroredVertically();
        QuarterArc verticalArc = (QuarterArc) vertical.supplyPath().primitives().getFirst();

        Assertions.assertEquals(-50.0, horizontal.supplyPath().endPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(50.0, horizontal.supplyPath().endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(Turn.LEFT, horizontalArc.turn());
        Assertions.assertEquals(50.0, vertical.supplyPath().endPoint().xMillimeters(), 0.001);
        Assertions.assertEquals(-50.0, vertical.supplyPath().endPoint().yMillimeters(), 0.001);
        Assertions.assertEquals(Turn.LEFT, verticalArc.turn());
    }

    @Test
    void erzeugtQuadratischenVarioRouterAusWachsenderDoppelspirale() {
        String commands = router.squareVarioCommands(1_300.0, 100.0);

        Assertions.assertEquals(
                modern("RRIRIIRIIIRIIIIRIIIIIRIIIIIIRIIIIIIIRIIIIIIIIRIIIIIIIIIRIIIIIIIIIIRIIIIIIIIIII"),
                filter(commands, true)
        );
        Assertions.assertEquals(
                modern("rririiriiiriiiiriiiiiriiiiiiriiiiiiiriiiiiiiiriiiiiiiiiriiiiiiiiiiriiiiiiiiiiiriiiiiiiiiiiiriiiiiiiiiiii"),
                filter(commands, false)
        );
        RoutingResult result = router.route(1_300.0, 1_300.0, 100.0, commands);
        Assertions.assertFalse(result.supplyPath().primitives().isEmpty());
        Assertions.assertFalse(result.returnPath().primitives().isEmpty());
    }

    @Test
    void skaliertQuadratischenVarioRouterAusSeitenlängeUndVerlegeabstand() {
        String commands = router.squareVarioCommands(2_000.0, 100.0);

        Assertions.assertTrue(filter(commands, true).contains(repeated(HeatingRoutingLanguage.SUPPLY_LINE, 18)));
        Assertions.assertTrue(filter(commands, false).contains(repeated(HeatingRoutingLanguage.RETURN_LINE, 19)));
        Assertions.assertDoesNotThrow(() -> router.route(2_000.0, 2_000.0, 100.0, commands));
    }

    @Test
    void erzeugtRechteckigenVarioRouterMitRandZuleitungen() {
        String commands = router.rectangularVarioCommands(2_000.0, 3_000.0, 100.0);
        String supply = filter(commands, true);
        String ret = filter(commands, false);

        Assertions.assertEquals(repeated(HeatingRoutingLanguage.SUPPLY_LINE, 5) + "RR", supply.substring(0, 7));
        Assertions.assertEquals(repeated(HeatingRoutingLanguage.RETURN_LINE, 5) + "rr", ret.substring(0, 7));
        Assertions.assertTrue(supply.contains(repeated(HeatingRoutingLanguage.SUPPLY_LINE, 27)));
        Assertions.assertTrue(ret.contains(repeated(HeatingRoutingLanguage.RETURN_LINE, 25)));
        Assertions.assertTrue(supply.endsWith(repeated(HeatingRoutingLanguage.SUPPLY_LINE, 28)));
        Assertions.assertTrue(ret.endsWith(repeated(HeatingRoutingLanguage.RETURN_LINE, 27)));
        Assertions.assertDoesNotThrow(() -> router.route(2_000.0, 3_000.0, 100.0, commands));
    }

    @Test
    void erzeugtRechteckigenVarioRouterMitSchlangenförmigerMittellinie() {
        String commands = router.rectangularVarioCommands(700.0, 1_600.0, 100.0, true);

        Assertions.assertTrue(commands.startsWith(
                modern("rLRRllrrLLRRllrrLLRRllrriIRr")
                        + repeated(HeatingRoutingLanguage.RETURN_LINE, 12) + "r"
                        + repeated(HeatingRoutingLanguage.SUPPLY_LINE, 12) + "R"
                        + repeated(HeatingRoutingLanguage.RETURN_LINE, 3) + "r"
                        + repeated(HeatingRoutingLanguage.SUPPLY_LINE, 3) + "R"
        ));
        Assertions.assertDoesNotThrow(() -> router.route(700.0, 1_600.0, 100.0, commands));
    }

    @Test
    void berechnetSchlangenförmigeVarioMittellinieAusRasterdifferenz() {
        String commands = router.rectangularVarioCommands(900.0, 1_600.0, 100.0, true);

        Assertions.assertTrue(commands.startsWith(modern("rLRRllrrLLRRllrrLLRRiIRr")));
        Assertions.assertTrue(commands.contains(repeated(HeatingRoutingLanguage.RETURN_LINE, 12)));
        Assertions.assertTrue(commands.contains(repeated(HeatingRoutingLanguage.SUPPLY_LINE, 10)));
        Assertions.assertDoesNotThrow(() -> router.route(900.0, 1_600.0, 100.0, commands));
    }

    @Test
    void rundetVarioSchlangeBeiUngerademRasterunterschiedAufVollständigeSchlangengruppe() {
        String commands = router.rectangularVarioCommands(2_100.0, 3_000.0, 100.0, true);

        Assertions.assertTrue(commands.startsWith(modern("rLRRllrrLLRRllrrLLRRllrriIRr")));
        Assertions.assertTrue(filter(commands, true).contains(repeated(HeatingRoutingLanguage.SUPPLY_LINE, 12)));
        Assertions.assertTrue(filter(commands, false).contains(repeated(HeatingRoutingLanguage.RETURN_LINE, 12)));
        Assertions.assertDoesNotThrow(() -> router.route(2_100.0, 3_000.0, 100.0, commands));
    }

    @Test
    void begrenztVarioSchlangeBeiUngeraderKurzerRasterseiteAufNutzbareBreite() {
        String commands = router.rectangularVarioCommands(1_100.0, 1_800.0, 100.0, true);

        Assertions.assertEquals(
                modern("LRRLLRRLLRRIRIIIIIIIIIIRIIIRIIIIIIIIIIIIRIIIIIRIIIIIIIIIIIIIIRIIIIIIIRIIIIIIIIIIIIIIIIRIIIIIIIIIRIIIIIIIIIIIIIIIII"),
                filter(commands, true)
        );
        Assertions.assertEquals(
                modern("rllrrllrririiiiiiiiiiriiiriiiiiiiiiiiiriiiiiriiiiiiiiiiiiiiriiiiiiiriiiiiiiiiiiiiiii"),
                filter(commands, false)
        );
        Assertions.assertDoesNotThrow(() -> router.route(1_100.0, 1_800.0, 100.0, commands));
    }

    @Test
    void verwendetOhneSchlangenSchalterDenStandardRouter() {
        Assertions.assertEquals(
                router.rectangularVarioCommands(2_000.0, 3_000.0, 100.0),
                router.rectangularVarioCommands(2_000.0, 3_000.0, 100.0, false)
        );
    }

    @Test
    void erzeugtMeanderRouterAusGespeichertenBeispielen() {
        assertMeanderPipes(
                500.0,
                500.0,
                modern("IIRRIIILLIIII"),
                modern("irriiilliiii")
        );
        assertMeanderPipes(
                600.0,
                800.0,
                modern("IIIIRRIIIIIIILLIIIIIIIRRIIIIIII"),
                modern("iiirriiiiiiilliiiiiiirriiiiiii")
        );
        assertMeanderPipes(
                800.0,
                1_300.0,
                modern("IIIIIIILLIIIIIIIIIIIIRRIIIIIIIIIIIILLIIIIIIIIIIIIRRIIIIIIIIIIII"),
                modern("iiiiilliiiiiiiiiiiirriiiiiiiiiiiilliiiiiiiiiiiirriiiiiiiiiiii")
        );

        String longSupply = repeated(HeatingRoutingLanguage.SUPPLY_LINE, 15)
                + ("LL" + repeated(HeatingRoutingLanguage.SUPPLY_LINE, 29) + "RR" + repeated(HeatingRoutingLanguage.SUPPLY_LINE, 29)).repeat(5);
        String longReturn = repeated(HeatingRoutingLanguage.RETURN_LINE, 14)
                + ("ll" + repeated(HeatingRoutingLanguage.RETURN_LINE, 29) + "rr" + repeated(HeatingRoutingLanguage.RETURN_LINE, 29)).repeat(5);
        assertMeanderPipes(2_000.0, 3_000.0, longSupply, longReturn);
    }

    @Test
    void meanderSchlangenSchalterErsetztMittlereGerade() {
        String commands = router.meanderCommands(2_000.0, 3_100.0, 100.0, true);
        String longLine = repeated(HeatingRoutingLanguage.SUPPLY_LINE, 30);
        String longReturnLine = repeated(HeatingRoutingLanguage.RETURN_LINE, 30);

        Assertions.assertEquals(
                modern("IL") + "RRLL".repeat(7) + modern("RRIR") + longLine
                        + ("LL" + longLine + "RR" + longLine).repeat(4)
                        + "LL" + longLine,
                filter(commands, true)
        );
        Assertions.assertEquals(
                "r" + "llrr".repeat(7) + modern("ir") + longReturnLine
                        + ("ll" + longReturnLine + "rr" + longReturnLine).repeat(4),
                filter(commands, false)
        );
        Assertions.assertDoesNotThrow(() -> router.route(2_000.0, 3_100.0, 100.0, commands));
    }

    @Test
    void gleichtMeanderMittelschlangeAnNormaleParallelreihenAn() {
        String commands = router.meanderCommands(2_000.0, 3_000.0, 100.0, true);

        Assertions.assertTrue(filter(commands, false).startsWith("r" + "llrr".repeat(7) + "lrr"));
        Assertions.assertTrue(filter(commands, true).startsWith("L" + "RRLL".repeat(6) + "RRL" + "RR"));
        Assertions.assertDoesNotThrow(() -> router.route(2_000.0, 3_000.0, 100.0, commands));
    }

    @Test
    void akzeptiertAlteIGeradeUndNormalisiertDieFeldgrenze() {
        RoutingResult result = router.route(2_000.0, 3_000.0, 100.0, "Ii+=-");

        Assertions.assertEquals(2, result.supplyPath().primitives().size());
        Assertions.assertEquals(2, result.returnPath().primitives().size());
        Assertions.assertEquals(1, result.fieldSupplyPrimitiveCount());
        Assertions.assertEquals(1, result.fieldReturnPrimitiveCount());
    }

    private void assertMeanderPipes(double widthMillimeters, double heightMillimeters, String supply, String ret) {
        String commands = router.meanderCommands(widthMillimeters, heightMillimeters, 100.0);

        Assertions.assertEquals(supply, filter(commands, true));
        Assertions.assertEquals(ret, filter(commands, false));
        Assertions.assertDoesNotThrow(() -> router.route(widthMillimeters, heightMillimeters, 100.0, commands));
    }

    private String repeated(char command, int count) {
        return String.valueOf(command).repeat(count);
    }

    private String filter(String commands, boolean supply) {
        StringBuilder filtered = new StringBuilder();
        for (int index = 0; index < commands.length(); index++) {
            char command = commands.charAt(index);
            if (supply ? HeatingRoutingLanguage.isSupplyCommand(command) : HeatingRoutingLanguage.isReturnCommand(command)) {
                filtered.append(command);
            }
        }
        return filtered.toString();
    }

    private String modern(String legacyCommands) {
        return HeatingRoutingLanguage.normalizeCommands(legacyCommands);
    }
}
