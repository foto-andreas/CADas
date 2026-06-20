package de.schrell.cadas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PointerCursorServiceTest {

    private final PointerCursorService service = new PointerCursorService();

    @Test
    void priorisiertPanningUndKantenHandles() {
        assertEquals(PointerCursorService.CursorType.CLOSED_HAND, service.cursor(context(DrawingTool.EDIT, PointerCursorService.PointerTarget.HORIZONTAL_EDGE, true, false, false)));
        assertEquals(PointerCursorService.CursorType.HORIZONTAL_RESIZE, service.cursor(context(DrawingTool.EDIT, PointerCursorService.PointerTarget.HORIZONTAL_EDGE, false, false, false)));
        assertEquals(PointerCursorService.CursorType.VERTICAL_RESIZE, service.cursor(context(DrawingTool.EDIT, PointerCursorService.PointerTarget.VERTICAL_EDGE, false, false, false)));
    }

    @Test
    void unterscheidetPunkteElementeUndAltAuswahl() {
        assertEquals(PointerCursorService.CursorType.MOVE, service.cursor(context(DrawingTool.EDIT, PointerCursorService.PointerTarget.ENDPOINT, false, false, false)));
        assertEquals(PointerCursorService.CursorType.HAND, service.cursor(context(DrawingTool.EDIT, PointerCursorService.PointerTarget.ELEMENT, false, false, false)));
        assertEquals(PointerCursorService.CursorType.CROSSHAIR, service.cursor(context(DrawingTool.EDIT, PointerCursorService.PointerTarget.ELEMENT, false, false, true)));
    }

    @Test
    void zeigtMitLeertastePanningBereitsVorDemZiehenAn() {
        assertEquals(PointerCursorService.CursorType.OPEN_HAND, service.cursor(context(DrawingTool.WALL, PointerCursorService.PointerTarget.EMPTY, false, true, false)));
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
