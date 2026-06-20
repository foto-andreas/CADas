package de.schrell.cadas.application.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
