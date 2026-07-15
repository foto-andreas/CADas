package de.schrell.cadas.application.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UndoRedoStackTest {

    @Test
    void verwaltetRueckgaengigUndWiederherstellenInBeidenRichtungen() {
        UndoRedoStack<String> history = new UndoRedoStack<>();
        history.remember("eins");
        history.remember("zwei");

        String undoSnapshot = history.undo("drei").orElseThrow();
        String redoSnapshot = history.redo("zwei-aktuell").orElseThrow();

        assertEquals("zwei", undoSnapshot);
        assertEquals("drei", redoSnapshot);
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    void verwirftBeiErreichenDerGrenzeDieAeltestenSnapshots() {
        UndoRedoStack<Integer> history = new UndoRedoStack<>(3);
        for (int snapshot = 1; snapshot <= 5; snapshot++) {
            history.remember(snapshot);
        }

        assertEquals(5, history.undo(6).orElseThrow());
        assertEquals(4, history.undo(5).orElseThrow());
        assertEquals(3, history.undo(4).orElseThrow());
        assertTrue(history.undo(3).isEmpty());
    }

    @Test
    void lehntUnbrauchbareHistoriengroesseAb() {
        assertThrows(IllegalArgumentException.class, () -> new UndoRedoStack<>(0));
    }
}
