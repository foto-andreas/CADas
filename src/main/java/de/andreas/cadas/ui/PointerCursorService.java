package de.andreas.cadas.ui;

final class PointerCursorService {

    CursorType cursor(PointerContext context) {
        if (context.panning()) {
            return CursorType.CLOSED_HAND;
        }
        if (context.spacePressed()) {
            return CursorType.OPEN_HAND;
        }
        if (context.target() == PointerTarget.HORIZONTAL_EDGE) {
            return CursorType.HORIZONTAL_RESIZE;
        }
        if (context.target() == PointerTarget.VERTICAL_EDGE) {
            return CursorType.VERTICAL_RESIZE;
        }
        if (context.tool() == DrawingTool.EDIT) {
            return switch (context.target()) {
                case ENDPOINT -> CursorType.MOVE;
                case ELEMENT -> context.altPressed() ? CursorType.CROSSHAIR : CursorType.HAND;
                default -> CursorType.DEFAULT;
            };
        }
        return CursorType.CROSSHAIR;
    }

    enum PointerTarget {
        EMPTY,
        ELEMENT,
        ENDPOINT,
        HORIZONTAL_EDGE,
        VERTICAL_EDGE
    }

    enum CursorType {
        DEFAULT,
        CROSSHAIR,
        HAND,
        OPEN_HAND,
        CLOSED_HAND,
        MOVE,
        HORIZONTAL_RESIZE,
        VERTICAL_RESIZE
    }

    record PointerContext(
            DrawingTool tool,
            PointerTarget target,
            boolean panning,
            boolean spacePressed,
            boolean altPressed
    ) {
    }
}
