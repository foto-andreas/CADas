package de.schrell.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PointerCursorServiceTest {

    @Test
    void priorisiertPanningUndKantenHandles() {
        assertEquals(PointerCursorService.CursorType.CLOSED_HAND, PointerCursorService.cursor(context(DrawingTool.EDIT, PointerCursorService.PointerTarget.HORIZONTAL_EDGE, true, false, false)));
        assertEquals(PointerCursorService.CursorType.HORIZONTAL_RESIZE, PointerCursorService.cursor(context(DrawingTool.EDIT, PointerCursorService.PointerTarget.HORIZONTAL_EDGE, false, false, false)));
        assertEquals(PointerCursorService.CursorType.VERTICAL_RESIZE, PointerCursorService.cursor(context(DrawingTool.EDIT, PointerCursorService.PointerTarget.VERTICAL_EDGE, false, false, false)));
    }

    @Test
    void unterscheidetPunkteElementeUndAltAuswahl() {
        assertEquals(PointerCursorService.CursorType.MOVE, PointerCursorService.cursor(context(DrawingTool.EDIT, PointerCursorService.PointerTarget.ENDPOINT, false, false, false)));
        assertEquals(PointerCursorService.CursorType.HAND, PointerCursorService.cursor(context(DrawingTool.EDIT, PointerCursorService.PointerTarget.ELEMENT, false, false, false)));
        assertEquals(PointerCursorService.CursorType.CROSSHAIR, PointerCursorService.cursor(context(DrawingTool.EDIT, PointerCursorService.PointerTarget.ELEMENT, false, false, true)));
    }

    @Test
    void zeigtMitLeertastePanningBereitsVorDemZiehenAn() {
        assertEquals(PointerCursorService.CursorType.OPEN_HAND, PointerCursorService.cursor(context(DrawingTool.WALL, PointerCursorService.PointerTarget.EMPTY, false, true, false)));
    }

    private PointerCursorService.PointerContext context(
            DrawingTool tool,
            PointerCursorService.PointerTarget target,
            boolean panning,
            boolean spacePressed,
            boolean altPressed
    ) {
        return new PointerCursorService.PointerContext(tool, target, panning, spacePressed, altPressed);
    }
}
