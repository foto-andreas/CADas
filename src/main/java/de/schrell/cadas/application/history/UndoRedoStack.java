package de.schrell.cadas.application.history;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

public final class UndoRedoStack<T> {

    private final Deque<T> undoStack = new ArrayDeque<>();
    private final Deque<T> redoStack = new ArrayDeque<>();

    public void remember(T snapshot) {
        undoStack.push(Objects.requireNonNull(snapshot, "snapshot darf nicht null sein."));
        redoStack.clear();
    }

    public Optional<T> undo(T currentSnapshot) {
        if (undoStack.isEmpty()) {
            return Optional.empty();
        }
        redoStack.push(Objects.requireNonNull(currentSnapshot, "currentSnapshot darf nicht null sein."));
        return Optional.of(undoStack.pop());
    }

    public Optional<T> redo(T currentSnapshot) {
        if (redoStack.isEmpty()) {
            return Optional.empty();
        }
        undoStack.push(Objects.requireNonNull(currentSnapshot, "currentSnapshot darf nicht null sein."));
        return Optional.of(redoStack.pop());
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
