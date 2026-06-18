package de.andreas.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TwoDZoomRangeTest {

    private final TwoDZoomRange zoomRange = new TwoDZoomRange();

    @Test
    void erlaubtBisZuVierzigfacheVergroesserung() {
        assertEquals(20.0, zoomRange.clamp(20.0), 0.001);
        assertEquals(40.0, zoomRange.clamp(100.0), 0.001);
    }

    @Test
    void behaeltDieBisherigeMinimaleVergroesserung() {
        assertEquals(0.25, zoomRange.clamp(0.01), 0.001);
    }
}
