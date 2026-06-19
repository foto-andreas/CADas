package de.andreas.cadas.application.drawing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DimensionLineLayoutServiceTest {

    @Test
    void legtMaßlinieParallelMitSenkrechtenHilfslinienAn() {
        var layout = new DimensionLineLayoutService().layout(10, 20, 110, 20, 30);

        assertEquals(10, layout.lineStartX(), 0.001);
        assertEquals(50, layout.lineStartY(), 0.001);
        assertEquals(110, layout.lineEndX(), 0.001);
        assertEquals(50, layout.lineEndY(), 0.001);
        assertEquals(60, layout.textX(), 0.001);
        assertEquals(50, layout.textY(), 0.001);
    }

    @Test
    void verschiebtTextVonDerLinieWegInNormalenrichtung() {
        DimensionLineLayoutService service = new DimensionLineLayoutService();
        var layout = service.layout(10, 20, 110, 20, 30);

        // Horizontale Linie -> Normale zeigt nach unten (0,+1) für placementSideSign +1
        var delta = service.textOffsetAwayFromLine(layout, 1.0, 8.0);
        assertEquals(0.0, delta.deltaX(), 0.001);
        assertEquals(8.0, delta.deltaY(), 0.001);

        // Andere Seite: Normalenvektor dreht ins Negativ
        var deltaNeg = service.textOffsetAwayFromLine(layout, -1.0, 8.0);
        assertEquals(0.0, deltaNeg.deltaX(), 0.001);
        assertEquals(-8.0, deltaNeg.deltaY(), 0.001);
    }
}
