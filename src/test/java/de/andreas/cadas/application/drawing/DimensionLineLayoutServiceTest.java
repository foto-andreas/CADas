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
}
